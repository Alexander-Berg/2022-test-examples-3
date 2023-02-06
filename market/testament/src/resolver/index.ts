import * as fs from 'fs';

import MemoryFs from '../memoryFs';
import {makePackageJSONDescriptor, PackageJSON, register} from './registry';
import createResolver from './helpers/createResolver';

type Options = {
    basedir: string;
    defaultResolver: (request: string, options: Options) => string;
};

const fsCustomResolver = createResolver(fs);
let memoryFs: MemoryFs;
let memFsCustomResolver: any;
let initPromise: Promise<void> | void;

module.exports = (request: string, options: Options) => {
    try {
        try {
            return memFsCustomResolver(options.basedir, request);
        } catch (e) {
            return fsCustomResolver(options.basedir, request);
        }
    } catch (e) {
        return options.defaultResolver(request, options);
    }
};

module.exports.init = async (
    basedir = process.env.TESTAMENT_MEMFS_BASEDIR ?? process.cwd(),
) => {
    if (initPromise) {
        await initPromise;
        return;
    }

    memoryFs = new MemoryFs(basedir);
    initPromise = memoryFs.init();
    await initPromise;
    memFsCustomResolver = createResolver(memoryFs.fs);

    register(
        makePackageJSONDescriptor(
            '@yandex-market/apiary',
            (pkg: PackageJSON) => {
                // @ts-ignore
                if (process.env.TESTAMENT_RENDER_PROCESS) {
                    return pkg;
                }

                return {
                    ...pkg,
                    main: pkg.browser,
                };
            },
        ),
    );
};

module.exports.register = register;
