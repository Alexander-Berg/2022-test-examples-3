/**
 * Настройки для расширенного окружения blackbox-mimino FAPI
 * В этом окружении тестинг FAPI может авторизовывать приложения по продовым oAuth токенам
 */
module.exports = {
    isTest: true,
    tvm: {
        env: 'market_front_blue-testing-mimino',
    },
    servant: {
        passport: {
            host: 'blackbox-mimino.yandex.net',
        },
        blackbox: {
            host: 'blackbox-mimino.yandex.net',
        },
        ocrm: {
            host: 'ow.tst.market.yandex-team.ru',
            consumer: 'market_front_blue-testing-mimino',
        },
    },
};
