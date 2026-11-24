#!/bin/sh
case "$RUNTIME_DB_PASSWORD" in ''|*[!a-fA-F0-9]*) echo "Invalid database password format" >&2; exit 1;; esac
[ "${#RUNTIME_DB_PASSWORD}" -ge 32 ] || exit 1
MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot <<SQL
CREATE USER 'seal_runtime'@'%' IDENTIFIED BY '$RUNTIME_DB_PASSWORD';
GRANT SELECT, INSERT, UPDATE ON seal_card.seal_card TO 'seal_runtime'@'%';
GRANT SELECT, INSERT, UPDATE ON seal_card.seal_card_version TO 'seal_runtime'@'%';
GRANT SELECT, INSERT, UPDATE ON seal_card.seal_card_account TO 'seal_runtime'@'%';
GRANT SELECT, INSERT, UPDATE ON seal_card.seal_card_image TO 'seal_runtime'@'%';
GRANT SELECT, INSERT, UPDATE ON seal_card.seal_upload_task TO 'seal_runtime'@'%';
GRANT SELECT, INSERT ON seal_card.seal_card_audit_log TO 'seal_runtime'@'%';
SQL
