import react, { reactCompilerPreset } from '@vitejs/plugin-react'
import babel from '@rolldown/plugin-babel'
import process from 'node:process'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react(), babel({ presets: [reactCompilerPreset()] })],
  server: { proxy: { '/api': { target: process.env.API_TARGET || 'http://localhost:8080', changeOrigin: false } } },
  preview: { proxy: { '/api': { target: process.env.API_TARGET || 'http://localhost:8080', changeOrigin: false } } },
})
