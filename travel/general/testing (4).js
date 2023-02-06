/*
 * Конфиг для тестинга
 */
import csp from '@yandex-int/csp-presets-pack';

import {ASSESSORS_SUGGEST_URL} from '../../common/lib/suggests/constants';

import defaultConfig from './default';

const cspDefault = defaultConfig.csp.policies;

module.exports = {
    blablacar: {
        requestOptions: {
            protocol: 'http:',
            timeout: 10000,
            rejectUnauthorized: false,
            hostname:
                process.env.BACKEND_BLABLACAR_HOST ||
                'testing.blablacar.rasp.yandex.net',
        },
    },

    bunker: {
        hostname: 'bunker-api-dot.yandex.net',
    },

    jaeger: {
        host: 'https://jaeger.yt.yandex-team.ru/travel-test/search',
    },

    startrek: {
        options: {
            host: 'st-api.test.yandex-team.ru',
        },
        components: [44959],
    },

    blackbox: {
        api: 'blackbox-mimino.yandex.net',
    },

    mail: {
        host: 'zealot.yandex.ru:32500',
    },

    suggests: {
        url:
            process.env.SUGGESTS_URL ||
            'https://testing.suggests.rasp.common.yandex.net/',
    },

    csp: {
        policies: {
            [csp.SCRIPT]: [].concat(cspDefault[csp.SCRIPT], [
                'testing.suggests.rasp.common.yandex.net',
                'suggests.cloud.tst.rasp.yandex.net',
                process.env.ASSESSORS_SUGGEST_URL || ASSESSORS_SUGGEST_URL,
            ]),
            [csp.IMG]: [].concat(cspDefault[csp.IMG], [
                'work.admin-test.rasp.yandex.ru',
                'work.admin-test.rasp.yandex-team.ru',
                'rasp-test-media-bucket.s3.yandex.net',
            ]),
            [csp.CONNECT]: [].concat(cspDefault[csp.CONNECT], [
                'testing.suggests.rasp.common.yandex.net',
                'suggests.cloud.tst.rasp.yandex.net',
                process.env.ASSESSORS_SUGGEST_URL || ASSESSORS_SUGGEST_URL,
                'oauth.yandex-team.ru',
                'passport.yandex-team.ru',
            ]),
        },
    },

    experiments: {
        __webvisor: {
            type: Boolean,
            denied: true,
        },
    },
};
