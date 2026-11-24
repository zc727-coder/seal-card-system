"""Build a clean source archive and an offline deployment directory."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def sha256(path):
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda: stream.read(4 * 1024 * 1024), b''):
            digest.update(chunk)
    return digest.hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('output', type=Path)
    parser.add_argument('--docker', default=shutil.which('docker'))
    args = parser.parse_args()
    if not args.docker:
        raise RuntimeError('Docker CLI must be available')
    target = args.output.resolve()
    target.mkdir(parents=True, exist_ok=False)
    runtime = target / 'seal-card-system'
    runtime.mkdir()
    files = ['README.md', 'compose.release.yaml', 'docs/sql/001_create_tables.sql',
             'infra/mysql/02-runtime-user.sh', 'scripts/install.ps1', 'scripts/install.sh']
    for name in files:
        destination = runtime / name
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(ROOT / name, destination)
    shutil.copyfile(ROOT / '.env.release.example', runtime / '.env.example')
    (runtime / 'app').mkdir()
    shutil.copyfile(ROOT / 'backend/target/seal-card-service.jar', runtime / 'app/seal-card-service.jar')
    # Copy the static assets from the exact web image that is being distributed.
    container = subprocess.check_output([args.docker, 'create', 'seal-card-web:1.0.0'], text=True).strip()
    try:
        (runtime / 'frontend').mkdir()
        subprocess.run([args.docker, 'cp', container + ':/usr/share/nginx/html', str(runtime / 'frontend/dist')], check=True)
    finally:
        subprocess.run([args.docker, 'rm', container], check=True, stdout=subprocess.DEVNULL)
    subprocess.run([args.docker, 'save', '-o', str(runtime / 'images.tar'),
                    'seal-card-service:1.0.0', 'seal-card-web:1.0.0', 'seal-card-mysql:8.4.13'], check=True)
    entries = sorted(p for p in runtime.rglob('*') if p.is_file())
    (runtime / 'SHA256SUMS').write_text(''.join(sha256(p) + '  ' + p.relative_to(runtime).as_posix() + '\n' for p in entries), encoding='utf-8')
    # Include product source only; no workstation credentials, build reports or business data.
    candidates = [ROOT / 'README.md', ROOT / 'compose.release.yaml', ROOT / '.env.release.example', ROOT / '.dockerignore']
    for name in ('backend/src/main', 'frontend/src', 'frontend/public', 'infra/runtime', 'infra/mysql', 'docs/sql'):
        directory = ROOT / name
        if directory.exists():
            candidates += [p for p in directory.rglob('*') if p.is_file()]
    candidates += [ROOT / name for name in ('backend/pom.xml', 'backend/README.md', 'frontend/README.md',
                   'frontend/package.json', 'frontend/package-lock.json', 'frontend/index.html', 'frontend/vite.config.js',
                   'scripts/install.ps1', 'scripts/install.sh', 'scripts/export-release.py')]
    with zipfile.ZipFile(target / 'seal-card-system-source-1.0.0.zip', 'w', zipfile.ZIP_DEFLATED) as archive:
        for p in sorted(set(candidates)):
            if p.is_file():
                archive.write(p, 'seal-card-system/' + p.relative_to(ROOT).as_posix())
    print('Offline deployment directory and source archive prepared at ' + str(target))


if __name__ == '__main__':
    main()
