// Copies the Capacitor runtime and the plugin's browser bundle into www/vendor,
// so the demo page needs no bundler.
import { copyFileSync, mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const out = join(root, 'www', 'vendor');
mkdirSync(out, { recursive: true });

const files = [
  ['node_modules/@capacitor/core/dist/capacitor.js', 'capacitor.js'],
  ['node_modules/capacitor-odc-home-widget/dist/plugin.js', 'odc-home-widget.js'],
];
for (const [from, to] of files) {
  copyFileSync(join(root, from), join(out, to));
  console.log(`copied ${from} -> www/vendor/${to}`);
}
