module.exports = {
    tvm: {
        destinations: [
            'mayak_back_tvm',
            239, // 'blackbox-mimino'
        ],
    },

    passport: {
        host: 'https://passport.yandex.ru',
    },

    blackbox: {
        api: 'blackbox-mimino.yandex.net',
        getServiceTicket: (req) => {
            if (req.tvm && req.tvm.tickets && req.tvm.tickets['blackbox-mimino']) {
                return req.tvm.tickets['blackbox-mimino'].ticket;
            }
        },
    },

    mayak: {
        host: 'https://tst.mayak.yandex.ru',
    },
};
