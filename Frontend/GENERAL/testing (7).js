module.exports = {
    frontend: {
        endpoint: 'https://testing.events.spec-promo.yandex.{tld}/',
    },

    takeout: {
        tvmId: 2009783,
    },

    db: {
        options: {
            dialectOptions: {
                ssl: true,
                keepAlive: true,
            },

            /**
             * Выставляем время соединения с базой на 5 секунд, чтобы не выбрасывалась ошибка
             * @see https://github.com/sequelize/sequelize/issues/7884#issuecomment-313853545
             */
            pool: {
                acquire: 5000,
            },
        },
    },

    cors: {
        origin: [
            'https://swagger-ui.yandex-team.ru',
            'https://swagger-editor.common.yandex.ru',
            'https://testing-events-admin-v2.common-int.yandex-team.ru',
            'https://testing.events.common.yandex.ru',
            'https://localhost.msup.yandex.ru:8082',
            process.env.LPC_HOST || 'https://develop.lpc-stage.yandex-team.ru',
        ],
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
            error: 'WCYCYFN3-LFP1',
        },
    },
};
