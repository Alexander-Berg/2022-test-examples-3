import path from 'path';
import zlib from 'zlib';
import { promises as fs } from 'fs';
import yaml from 'js-yaml';

async function readDirectory(
    dirPath: string,
    rootDir: string = dirPath,
    ignore: RegExp[] = [/.*\.DS_Store/],
): Promise<object> {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let result: Record<string, any> = {};
    const items = await fs.readdir(dirPath);
    for (const item of items) {
        let itemPath = path.join(dirPath, item);
        if (ignore.some((regexp) => regexp.test(itemPath))) {
            continue;
        }
        if ((await fs.lstat(itemPath)).isDirectory()) {
            result = { ...result, ...(await readDirectory(itemPath, rootDir, ignore)) };
        } else {
            let contents = await fs.readFile(itemPath);
            try {
                if (itemPath.endsWith('.json')) {
                    result[path.relative(rootDir, itemPath)] = JSON.parse(contents.toString());
                } else if (itemPath.endsWith('.json.gz')) {
                    result[path.relative(rootDir, itemPath)] = JSON.parse(
                        zlib.gunzipSync(contents).toString(),
                    );
                } else if (itemPath.endsWith('.yml') || itemPath.endsWith('.yaml')) {
                    result[path.relative(rootDir, itemPath)] = yaml.load(contents.toString());
                } else {
                    throw new Error('Unknown format');
                }
            } catch {
                result[path.relative(rootDir, itemPath)] = contents.toString().trim();
            }
        }
    }
    return result;
}

export { readDirectory };
