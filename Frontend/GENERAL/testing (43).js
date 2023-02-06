const merge = require('deepmerge');

const defaultConfig = require('./defaults');
const cspPresets = require('@yandex-int/csp-presets-pack');

const STATIC_HOST = 'frontend-test.s3.mds.yandex.net';

module.exports = merge(defaultConfig, {
    app: {
        domains: 'ru',
    },

    hosts: {
        quasar: 'testing.quasar.yandex.ru',
        billing: 'testing.quasar.yandex.ru',
        iot: 'iot-beta.quasar.yandex.ru',
        dialogs: 'priemka.dialogs.alice.yandex.ru',
    },

    srcrwr: {
        IOT__USER_INFO: 'SHARED__IOT_USER_INFO_BETA',
    },

    cspPresets: [
        {
            [cspPresets.SCRIPT]: [STATIC_HOST],
            [cspPresets.STYLE]: [STATIC_HOST],
            [cspPresets.FONT]: [STATIC_HOST],
            [cspPresets.IMG]: [STATIC_HOST],
            [cspPresets.MEDIA]: [STATIC_HOST],
        },

        ...defaultConfig.cspPresets,
    ],
});
