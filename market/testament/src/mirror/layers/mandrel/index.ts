/* eslint-disable no-return-assign, no-console, global-require, @typescript-eslint/explicit-module-boundary-types,
 @typescript-eslint/no-var-requires, @typescript-eslint/no-non-null-assertion */

import Layer from '../../layer';
import type {InitContextArg} from './contextHelpers';
import JestLayer from '../jest';
import {resolveScriptPath} from '../../../utils/relativePath';
import {MandrelWorker} from './worker';

export type Config = {
    resourcesPath: string;
    // eslint-disable-next-line @typescript-eslint/ban-types
    envConfig: Object;
    showResourceLog?: boolean;
    // eslint-disable-next-line @typescript-eslint/ban-types
    defaultRoutes?: Object[];
};

// eslint-disable-next-line @typescript-eslint/ban-types
export default class MandrelLayer extends Layer<{}, MandrelWorker> {
    static ID = 'mandrel';

    #config: Config | null = null;

    constructor(config: Config | null = null) {
        super(MandrelLayer.ID, resolveScriptPath(__filename, 'worker.js'));
        this.#config = config;
    }

    // eslint-disable-next-line class-methods-use-this,@typescript-eslint/ban-types
    getMethods(): {} {
        return {};
    }

    async init(): Promise<void> {
        const jestLayer = this.getMirror()?.getLayer<JestLayer>(JestLayer.ID);
        await jestLayer?.backend.runCode(
            (
                initHelpersModule,
                resourcesPath,
                envConfig,
                showResourceLog,
                defaultRoutes,
            ) => {
                jest.unmock('@yandex-market/mandrel/bcm/base/MarketContext.js');
                jest.unmock('bcm2/base/backends-config');

                const {
                    initTransport,
                    initRouter,
                    initCache,
                    initLogger,
                    // eslint-disable-next-line import/no-unresolved
                } = require(initHelpersModule);
                initLogger();
                initCache();
                initRouter(defaultRoutes);

                if (resourcesPath && envConfig) {
                    initTransport(resourcesPath, envConfig);
                }

                if (!showResourceLog) {
                    const Resource = require('resource');
                    const {
                        ServiceResource,
                    } = require('@yandex-market/mandrel/resource');
                    jest.spyOn(
                        Resource.prototype,
                        'logRequest',
                    ).mockImplementation(() => false);
                    jest.spyOn(
                        ServiceResource.prototype,
                        'logRequest',
                    ).mockImplementation(() => false);
                }
            },
            [
                resolveScriptPath(__filename, './initHelpers'),
                this.#config?.resourcesPath,
                this.#config?.envConfig,
                this.#config?.showResourceLog,
                this.#config?.defaultRoutes,
            ] as const,
        );
        await jestLayer?.backend.doMock(
            '@yandex-market/mandrel/mockedContext',
            // @ts-ignore
            () => () => getBackend('mandrel').getContext(), // eslint-disable-line no-undef
            {virtual: true},
        );
    }

    async initContext(params?: InitContextArg): Promise<void> {
        await this.worker.initContext(params);
    }
}
