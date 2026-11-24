import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    strictPort: true,
    watch: { usePolling: true, interval: 500 },
    proxy: { '/api': { target: process.env.VITE_API_PROXY || 'http://backend:8080', changeOrigin: true } },
  },
})
