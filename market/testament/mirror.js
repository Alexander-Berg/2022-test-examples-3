// @flow
import path from 'path';
// flowlint-next-line untyped-import: off
import JestLayer from '@yandex-market/testament/mirror/layers/jest';
// flowlint-next-line untyped-import: off
import MandrelLayer from '@yandex-market/testament/mirror/layers/mandrel';
// flowlint-next-line untyped-import: off
import KadavrLayer from '@yandex-market/testament/mirror/layers/kadavr';
// flowlint-next-line untyped-import: off
import ApiaryLayer from '@yandex-market/testament/mirror/layers/apiary';
// flowlint-next-line untyped-import: off
import Mirror from '@yandex-market/testament/mirror';

export type makeMirrorFn =
    ((testPath: string, jestObject: typeof jest) => Promise<Mirror>)
    & ((config: MirrorConfig) => Promise<Mirror>);

export type KadavrConfig = {
    host: string;
    port: number;
    skipLayer?: boolean;
    asLibrary?: boolean;
};

export type MandrelConfig = {
    resourcesPath: string,
    // $FlowFixMe
    envConfig: Object,
    showResourceLog?: boolean,
};

export type MirrorConfig = {
    jest: { testFilename: string, jestObject: typeof jest },
    mandrel?: MandrelConfig,
    kadavr?: $Shape<KadavrConfig>,
};

export async function makeMirror(config: MirrorConfig): Promise<Mirror> {
    const {skipLayer = false, ...kadavr} = config.kadavr || {};

    let kadavrLayer;
    const mirror = new Mirror();
    const jestLayer = new JestLayer(config.jest.testFilename, config.jest.jestObject);
    const mandrelLayer = new MandrelLayer(config.mandrel);

    if (!skipLayer) {
        const kadavrConfig = {
            host: 'kadavr2.vs.market.yandex.net',
            port: 80,
            // $FlowFixMe
            ...kadavr,
        };

        kadavrLayer = new KadavrLayer(kadavrConfig);
    }
    const apiaryLayer = new ApiaryLayer();

    await mirror.registerRuntime(jestLayer);
    await mirror.registerLayer(mandrelLayer);
    if (!skipLayer) {
        await mirror.registerLayer(kadavrLayer);
    }
    await mirror.registerLayer(apiaryLayer);

    return mirror;
}

export const makeMirrorDesktop: makeMirrorFn = async function (...args) {
    let config: MirrorConfig;

    // $FlowIgnore
    const {servant: desktopServant} = require('@self/root/market/platform.desktop/configs/current/node'); // eslint-disable-line global-require

    if (typeof args[0] === 'string') {
        // $FlowFixMe
        config = ({jest: {testFilename: args[0], jestObject: args[1] || jest}}: MirrorConfig);
    } else {
        config = args[0];
        if (!config.jest.jestObject) {
            config.jest.jestObject = jest;
        }
    }

    const mandrelConfig = {
        resourcesPath: path.dirname(require.resolve('@self/root/market/platform.desktop/app/resource')),
        envConfig: desktopServant,
        // $FlowFixMe
        ...config.mandrel,
    };

    return makeMirror({
        jest: config.jest,
        kadavr: config.kadavr,
        mandrel: mandrelConfig,
    });
};

export const makeMirrorTouch: makeMirrorFn = async function (...args) {
    let config: MirrorConfig;

    // $FlowIgnore
    const {servant: touchServant} = require('@self/root/market/platform.touch/configs/current/node');// eslint-disable-line global-require

    if (typeof args[0] === 'string') {
        // $FlowFixMe
        config = ({jest: {testFilename: args[0], jestObject: args[1] || jest}}: MirrorConfig);
    } else {
        config = args[0];
        if (!config.jest.jestObject) {
            config.jest.jestObject = jest;
        }
    }

    const mandrelConfig = {
        resourcesPath: path.dirname(require.resolve('@self/root/market/platform.touch/app/resource')),
        envConfig: touchServant,
        // $FlowFixMe
        ...config.mandrel,
    };

    return makeMirror({jest: config.jest, kadavr: config.kadavr, mandrel: mandrelConfig});
};
