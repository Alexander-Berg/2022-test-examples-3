import fs from 'fs';

// @ts-ignore в этой версии enhanced-resolve еще нет опубликованных тайпингов
import enhancedResolve, {CachedInputFileSystem} from 'enhanced-resolve';

import {iterator} from '../registry';

// todo: мержить с настройками вебпака
// todo: завести отдеьный тип дескрипторов под это
const platform =
    process.env.APP_PLATFORM ||
    process.env.PLATFORM ||
    process.env.BLUE_PLATFORM;
const project = process.env.PROJECT;
const mainFiles = [
    project && platform && `index.${project}.${platform}`,
    project && `index.${project}`,
    platform && `index.${platform}`,
    'index',
].filter(Boolean);
const modules = ['node_modules'];

if (platform) {
    modules.push(process.cwd());
}

export default (fileSystem: typeof fs): any =>
    enhancedResolve.create.sync({
        plugins: [
            {
                apply(resolver: any) {
                    resolver
                        .getHook('existing-directory')
                        .tap('MyResolverPlugin', (request: any) => {
                            const pkg = request.descriptionFileData;

                            for (const descriptor of iterator()) {
                                if (
                                    descriptor.type === 'packageJSON' &&
                                    descriptor.data.name === pkg.name
                                ) {
                                    request.descriptionFileData =
                                        descriptor.handler(pkg);
                                }
                            }
                        });
                },
            },
        ],
        mainFiles,
        modules,
        mainFields: ['main', 'browser'],
        extensions: ['.js', '.json', '.jsx', '.ts', '.tsx', '.css', '.styl'],
        unsafeCache: true,
        fileSystem: new CachedInputFileSystem(fileSystem, 4000),
    });
