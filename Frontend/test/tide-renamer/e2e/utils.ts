import path from 'path';
import fse from 'fs-extra';

async function readDirectory(dirPath: string, rootDir?: string): Promise<object> {
    if (!rootDir) rootDir = dirPath;
    let result = {};
    const items = await fse.readdir(dirPath);
    for (const item of items) {
        let itemPath = path.join(dirPath, item);
        if ((await fse.lstat(itemPath)).isDirectory()) {
            result = { ...result, ...(await readDirectory(itemPath, rootDir)) };
        } else {
            let contents = (await fse.readFile(itemPath)).toString();
            result[path.relative(rootDir, itemPath)] = contents;
            try {
                result[path.relative(rootDir, itemPath)] = JSON.parse(contents);
            } catch {}
        }
    }
    return result;
}

export { readDirectory };
