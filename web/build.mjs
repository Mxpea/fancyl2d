import { build } from 'esbuild';
import { copyFile, mkdir } from 'node:fs/promises';

await mkdir('dist/assets', { recursive: true });
await build({ entryPoints: ['main.js'], bundle: true, splitting: true, format: 'esm',
  outdir: 'dist/assets', minify: true, target: 'chrome100', legalComments: 'inline' });
await copyFile('index.html', 'dist/index.html');
await copyFile('page.css', 'dist/assets/page.css');
