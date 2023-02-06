import fs from 'fs';

import NodeEnvironment from 'jest-environment-node';
import {Config} from '@jest/types';
import {readConfig} from 'jest-config';
// @ts-ignore
import sourcemapSupport from 'source-map-support';

import Runtime from '../../../runtime';

// eslint-disable-next-line @typescript-eslint/no-var-requires
const {init: initResolver} = require('../../../resolver');

export type RuntimeDescriptor = {
    runtime: Runtime;
    environment: NodeEnvironment;
};

export default async function createRuntimeDescriptor(
    filename: string,
    packageRootOrConfig: Config.Path,
    argv: Config.Argv,
): Promise<RuntimeDescriptor> {
    await initResolver();

    const {projectConfig, globalConfig} = await readConfig(
        argv,
        packageRootOrConfig,
    );
    const cacheFS = {};
    const environment = new NodeEnvironment(projectConfig);
    environment.global.console = console;
    const context = await Runtime.createContext(projectConfig, {
        ...globalConfig,
        maxWorkers: 1,
    });

    const runtime = new Runtime(
        projectConfig,
        environment,
        context.resolver,
        cacheFS,
        globalConfig,
        filename,
    );

    for (const path of projectConfig.setupFiles) {
        runtime.requireModule(path);
    }

    const sourcemapOptions = {
        environment: 'node',
        handleUncaughtExceptions: false,
        retrieveSourceMap: (source: string) => {
            const sourceMapSource = runtime.getSourceMaps()[source];

            if (sourceMapSource) {
                try {
                    return {
                        map: JSON.parse(
                            fs.readFileSync(sourceMapSource, 'utf8'),
                        ),
                        url: source,
                    };
                } catch {
                    return null;
                }
            }
            return null;
        },
    };

    // @ts-ignore
    runtime
        .requireInternalModule(
            require.resolve('source-map-support'),
            'source-map-support',
        )
        .install(sourcemapOptions);

    sourcemapSupport.install(sourcemapOptions);

    return {runtime, environment};
}
