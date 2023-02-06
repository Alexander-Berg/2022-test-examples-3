import { ConfigOverride } from './defaults';
import BUILD_ID from '../build-id';

const testingConfig: ConfigOverride = {
    api: {
        url: 'https://paskills.test.voicetech.yandex.net/api',
    },

    app: {
        storeUrl: 'https://dialogs.test.voicetech.yandex.ru/store',
        oauthClientId: '049d7f40d1d54dd78399b405d5d75b15',

        publicApi: {
            rateLimit: {
                windowMs: 6 * 1000,
                max: 30,
            },
        },

        links: {
            documentation: 'https://zamulla-d-docapi-10667-d-9.tech-unstable.yandex.ru',
        },

        deepLinkUrl: 'https://dialogs.test.voicetech.yandex.ru/s',
        assetsRoot: `https://yastatic.net/s3/dialogs/dev-console/${BUILD_ID}`,

        displayDeepLinks: true,
        displayDonationPage: true,
        displayMonitoringPage: true,
        displaySmartHomeMonitoringPage: true,
        shareBaseUrl: 'https://dialogs.test.voicetech.yandex.ru/share',
    },

    tvmtool: {
        host: 'http://localhost:1',
    },
};

module.exports = testingConfig;
