/* eslint-disable no-console, global-require, @typescript-eslint/no-var-requires */

// eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
export function initTransport(resourcesPath: string, envConfig: any): void {
    const url = require('url');
    const resource = require('@yandex-market/mandrel/resource').default;
    const {backendsConfig} = require('bcm2');

    resource.setPath(resourcesPath);
    resource.setEnvConfig(envConfig);

    const remapBackend: {[key: string]: string} = {passport: 'blackbox'};
    const remaped = new Set();

    Object.entries(envConfig).forEach((item: any) => {
        const backend: string = item[0];
        const backendConfig: {[key: string]: any} = item[1];
        const bcmConfig = {
            pathname: backendConfig.path,
            traceId: backendConfig.data && backendConfig.data.traceServiceId,
            settings: backendConfig.data,
            changeable: backendConfig.changeable,
            timeout: backendConfig.timeout,
            host: '',
        };
        if (backendConfig.host) {
            bcmConfig.host = url.format({
                hostname: backendConfig.host,
                protocol: backendConfig.protocol || 'http:',
                port: backendConfig.port,
            });
        }
        if (!remaped.has(backend)) {
            backendsConfig.setup({[backend]: bcmConfig});
        }
        if (remapBackend[backend]) {
            remaped.add(remapBackend[backend]);
            backendsConfig.setup({[remapBackend[backend]]: bcmConfig});
        }
    });
}

// eslint-disable-next-line @typescript-eslint/ban-types
export function initRouter(defaultRoutes?: Object[]): void {
    const Susanin = require('susanin');
    const stout = require('@yandex-market/stout');
    const {
        createRouteStub,
    } = require('@yandex-market/mandrel/specUtils/stoutTestHelpers');

    if (defaultRoutes) {
        stout.createRouter(defaultRoutes);
    } else {
        stout.router = new Susanin();
        stout.router.buildURL = () => 'https://ya.ru';
        stout.router.findFirst = () => [createRouteStub(), {}];
    }
}

export function initCache(): void {
    // eslint-disable-next-line import/no-unresolved
    const cachelib = require('@yandex-market/cache');
    cachelib.setup({});
}

export function initLogger(): void {
    // eslint-disable-next-line no-restricted-modules
    const logger = require('@yandex-market/logger');
    logger.setup(
        (
            lvl: 'error' | 'warn' | 'info' | 'debug' | 'trace',
            ...args: any[]
        ) => {
            switch (lvl) {
                case 'error':
                    console.error(...args);
                    break;
                case 'warn':
                    console.warn(...args);
                    break;
                case 'info':
                    console.info(...args);
                    break;
                case 'debug':
                    console.debug(...args);
                    break;
                case 'trace':
                    console.trace(...args);
                    break;
                default:
                    console.log(...args);
            }
        },
        'trace',
    );
}
