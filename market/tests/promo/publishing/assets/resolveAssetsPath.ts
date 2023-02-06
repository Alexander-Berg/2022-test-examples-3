import * as path from 'path';

export function resolveAssetPath(filename) {
  return path.join(__dirname, `./${filename}`);
}
