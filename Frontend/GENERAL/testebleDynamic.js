const { Preset } = require('@yandex-int/kotik');
const { presets } = require('@yandex-int/archon-renderer-devserver-command');
const { cacheFilename } = require('@yandex-int/frontend-kotik/middlewares/cache-filename');
const injectAssetsMiddlewareFactory = require('@yandex-int/frontend-kotik/middlewares/inject-assets');
const injectDynamicHermioneAsset = require('../middlewares/inject-dynamic-hermione-asset');
const cachePath = require('../middlewares/cache-path');

module.exports = () => {
    const preset = new Preset(presets['serp-apphost-frontend-dynamic'].middlewares);
    const injectAssetsMiddleware = injectAssetsMiddlewareFactory({
        assetsPath: [__dirname, '..', 'middlewares', 'test-assets'],
    });

    preset
        .addFirst({ name: 'cacheFilename', fn: cacheFilename([
            'reqid',
            'suggest_reqid',
            'tpid',
            'rnd',
            'testRunId',
            'session_info',
            'st',
            'yu',
            'expFlags',
            'flags',
        ]) })
        .addAfter('render', { name: 'injectDynamicAssets', fn: injectDynamicHermioneAsset })
        .addAfter('injectDynamicAssets', injectAssetsMiddleware)
        .addAfter('cookieParser', { name: 'messengerCachePath', fn: cachePath() });

    return preset;
};
