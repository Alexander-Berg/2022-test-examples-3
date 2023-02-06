import path from 'path';

export default function (filePath: string): string {
    return path.relative(process.cwd(), filePath);
}

export function resolveScriptPath(from: string, moduleName: string): string {
    const src = path.resolve(__dirname, '..');
    const relSrc = path.relative(src, from);
    const relativeFrom = path.resolve(from, '..', moduleName);
    const distPath = path.resolve(src, '../dist', relSrc, '..', moduleName);
    const paths = [relativeFrom, distPath];

    for (const item of paths) {
        try {
            require.resolve(item);
            return item;
            // eslint-disable-next-line no-empty
        } catch {}
    }

    throw new Error('Resolve module failed');
}
