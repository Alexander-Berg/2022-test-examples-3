module.exports = {
    servant: {
        report: {
            host: 'ps.tst.vs.market.yandex.net',
        },
        checkouter: {
            host: 'checkouter.sand.tst.vs.market.yandex.net',
        },
        checkouterPayment: {
            host: 'checkouter.sand.tst.vs.market.yandex.net',
        },
        checkout: {
            host: 'checkouter.sand.tst.vs.market.yandex.net',
        },
    },

    checkouter: {
        /**
         * Контекст для market-preview
         * @see https://nda.ya.ru/3S38sd
         */
        environment: 'CHECK_ORDER',
    },
};
