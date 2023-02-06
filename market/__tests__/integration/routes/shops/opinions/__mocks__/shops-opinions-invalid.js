module.exports = {
    host: 'https://api.content.market.yandex.ru',
    route: /\/v[0-9.]+\/shops\/[0-9]+\/opinions/,
    response: {
        status: 'iPetyaSatus',
        context: {
            region: {
                id: 225,
                name: 'Россия',
                type: 'COUNTRY',
                childCount: 10,
                country: { id: 225, name: 'Россия', type: 'COUNTRY', childCount: 10 },
            },
            currency: { id: 'RUR', name: 'руб.' },
            id: '1577108854393/97884cafcc8e515972cb09455f9a0500',
            time: '2019-12-23T16:47:34.397+03:00',
            marketUrl: 'https://market.yandex.ru?pp=1002',
        },
        errors: [{ message: 'Shop 123 not found' }],
    },
};
