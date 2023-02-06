import fs from 'fs';
import { join } from 'path';
import { AppConfig } from 'yandex-cfg';

import {
    COMPETITION_IMPORT,
    CONTEST_DELETE,
    CONTEST_GROUP_START_TIME,
    MONITOR_COUNTER_MODE,
    PROBLEM_GENERATORS,
    PROBLEM_VALIDATORS,
} from 'common/features';

import testingCsp from 'configs/csp/testing';

const ca = [fs.readFileSync(join(__dirname, 'certs/YandexInternalRootCA.crt'))];

const config: AppConfig = {
    mongo: {
        options: {
            sslCA: ca,
        },
    },

    features: {
        [COMPETITION_IMPORT]: true,
        [PROBLEM_GENERATORS]: true,
        [PROBLEM_VALIDATORS]: true,
        [CONTEST_DELETE]: true,
        [CONTEST_GROUP_START_TIME]: true,
        [MONITOR_COUNTER_MODE]: true,
    },

    api: {
        // prettier-ignore
        hostname: process.env.BACKEND_HOST || 'backend-test.contest.yandex.net',
        pathname: '/api/private/admin/v1/',
        port: 80,
        protocol: 'http',
        timeout: 30000,
        retry: 2,
    },

    blackbox: {
        api: 'blackbox-test.yandex.net',
    },

    bunker: {
        api: 'http://bunker-api-dot.yandex.net/v1',
        version: 'latest',
    },

    participantUI: {
        host: 'https://contest.test.yandex.ru',
    },

    avatars: {
        host: 'https://avatars.mdst.yandex.net',
    },

    passport: {
        host: 'https://passport-test.yandex.ru',
    },

    renewSession: {
        type: 'passport-test',
    },

    csp: {
        presets: testingCsp,
    },

    httpLangdetect: {
        server: 'http://langdetect-test.qloud.yandex.ru',
    },

    httpUatraits: {
        server: 'http://uatraits-test.qloud.yandex.ru',
    },

    tvm: {
        serverUrl: 'http://localhost:2',
    },

    static: {
        hash: process.env.APP_COMMIT_ID,
    },

    s3: {
        credentials: {
            endpoint: 'http://s3.mdst.yandex.net',
        },
    },

    adminOldHost: process.env.ADMIN_OLD_HOST || 'https://contest.test.yandex.ru/admin',

    accessFormUrl: {
        ru: 'https://forms.yandex.ru/surveys/10017621.052e5968cb7bd0784f3ea555b7922146ce31e346/',
        en: 'https://forms.yandex.ru/surveys/10019398.61d4e3c2bba1e717567ae4f5c7b65e86297eef75/',
    },

    domains: {
        main: 'admin.contest.test.yandex.ru',
        beta: 'beta.admin-test.contest.yandex.ru',
    },
};

module.exports = config;
