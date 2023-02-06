const loggerStream = require('@yandex-int/yandex-logger/streams/default');

module.exports = {
    frontend: {
        endpoint: 'https://testing.events.spec-promo.yandex.{tld}/',
    },

    takeout: {
        tvmId: 2009783,
    },

    logger: {
        name: 'events',
        streams: [
            {
                level: process.env.LOG_LEVEL || 'debug',
                stream: loggerStream({
                    line: {
                        template: [
                            '{{date}}',
                            '{{levelName}}',
                            '{{msg}}',
                            '{{#err}}\n{{err.stack}}{{/err}}',
                        ].join(' '),
                    },
                }),
            },
        ],
    },

    db: {
        uri: 'postgres://api:api@127.0.0.1:5432/eventsapi',
        options: {
            logging: false,
            dialectOptions: {
                ssl: {
                    require: true,
                    rejectUnauthorized: false,
                },
            },
        },
        dialectOptions: {
            ssl: {
                require: true,
                rejectUnauthorized: false,
            },
        },
    },

    sender: {
        host: 'https://test.sender.yandex-team.ru',
        accountSlug: 'ya.events',
        mailIds: {
            external: {
                registrationConfirmed: 'SXH5KK43-VD4',
                confirmationEmail: '8S98NOM3-Q741',
            },
            internal: {
                registrationConfirmed: '007YAI33-ROX1',
                confirmationEmail: '8QU6BI33-657',
            },
            registrationCreateError: 'SBELQJ33-WVL',
        },
    },
};
