/* eslint-disable */
import { ConfigOverride } from './defaults';

const config: ConfigOverride = {
    app: {
        developerConsolePath: 'https://dialogs.test.voicetech.yandex.ru/developer',
        storePath: 'https://dialogs.test.voicetech.yandex.ru/store',
        publicRootPath: 'https://dialogs.test.voicetech.yandex.ru',

        db: {
            longStackTraces: true,
        },

        publicApi: {
            imageUploadSizeLimit: 1 * 1024 * 1024,
        },

        nlu: {
            enabled: true,
        },

        maxSkillsPerUser: 10,
        inflectActivationPhrases: true,
        checkActivationPhrasesCommonness: true,
        scheduler: {
            updatePumpkinConcurrency: 1,
        },
    },

    db: {
        options: {
            dialectOptions: {
                ssl: {
                    // ca: require('fs').readFileSync('/root/.postgresql/root.crt', 'utf8'),
                    rejectUnauthorized: true,
                },
            } as any,
        },
    },

    mailer: {
        host: 'outbound-relay.yandex.net',
    },

    tvmtool: {
        host: 'http://localhost:1',
        permittedApps: {
            wizard: 2009743, // WIZARD-10883
            idm: 2001602,
        },
    },

    pumpkin: {
        apiKey: 'pumpkin/testing/getSkill.json',
        storeKeyPrefix: 'pumpkin/testing',
    },

    s3: {
        accessKeyId: 'X3hNjeh3YlmHfBGcWtqD',
    },

    saas: {
        kvSaaS: {
            ferrymanUrl: 'http://alice-paskills-testing.ferryman.n.yandex-team.ru',
            ytUploadDir: '//home/paskills/ferryman/testing',
            prefix: '1',
            serviceName: 'alice_paskills_testing',
            ctype: 'prestable',
            uploadYtProxy: 'arnold',
        },
    },

    pg: {
        acquireTimeoutMillis: 60000,
    },
    qloud: {
        environment: 'testing',
    },
};

export default config;
