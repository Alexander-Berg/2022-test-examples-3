import fs from 'fs/promises';
import fsOld from 'fs';
import path from 'path';

// @ts-ignore
import scanFs from '@discoveryjs/scan-fs';
import {Volume} from 'memfs';

export default class MemoryFs {
    protected created = false;

    protected volume = new Volume({});

    constructor(protected basedir: string) {}

    // eslint-disable-next-line class-methods-use-this
    get fs(): typeof fsOld {
        return this.volume as unknown as typeof fsOld;
    }

    async init(): Promise<void> {
        if (this.created) {
            return;
        }

        this.created = true;

        const dump = (await scanFs({basedir: this.basedir})) as Array<{
            path: string;
        }> & {
            symlinks: Array<{path: string; realpath: string}>;
        };

        const res = dump.reduce((all, current) => {
            if (
                current.path.endsWith('/package.json') ||
                current.path === 'package.json'
            ) {
                all[current.path] = fs.readFile(
                    path.resolve(this.basedir, current.path),
                    'utf8',
                );
            }
            return all;
        }, {} as Record<string, Promise<string>>);

        const resProcessed: Record<string, string> = {};

        await Promise.all(
            Object.entries(res).map(async ([key, value]) => {
                resProcessed[key] = await value;
            }),
        );

        this.volume.fromJSON(resProcessed, this.basedir);

        await Promise.all(
            Object.values(dump.symlinks).map(
                async ({path: cursorFile, realpath: cursorReal}) => {
                    try {
                        const cursorRealAbs = path.resolve(
                            this.basedir,
                            cursorReal,
                        );

                        this.volume.mkdirpSync(path.dirname(cursorReal));
                        this.volume.mkdirpSync(path.dirname(cursorFile));

                        // eslint-disable-next-line no-await-in-loop
                        if ((await fs.lstat(cursorRealAbs)).isSymbolicLink()) {
                            // eslint-disable-next-line no-await-in-loop
                            const realpath = await fs.realpath(cursorRealAbs);
                            this.volume.symlinkSync(
                                realpath,
                                cursorReal,
                                // eslint-disable-next-line no-await-in-loop
                                (await fs.stat(realpath)).isDirectory()
                                    ? 'dir'
                                    : 'file',
                            );
                        }
                        // eslint-disable-next-line no-await-in-loop
                        this.volume.symlinkSync(
                            cursorReal,
                            cursorFile,
                            // eslint-disable-next-line no-await-in-loop
                            (await fs.stat(cursorRealAbs)).isDirectory()
                                ? 'dir'
                                : 'file',
                        );
                        // eslint-disable-next-line no-empty
                    } catch {}
                },
            ),
        );
    }
}
