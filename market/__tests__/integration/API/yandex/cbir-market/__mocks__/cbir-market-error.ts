module.exports = {
    host: 'https://yandex.ru',
    route: '/images/api/v1/cbir/market?req_id=12345&url=http%3A%2F%2Furl.ru&lr=Moscow&relev=relev&cbird=115',
    allowUnmocked: false,
    response: {
        data: {
            cbir_id: '1970537/JnFDt23nIFap53O0Dc2IKQ8888',
            images: [],
        },
        reqId: '12345',
        status: 'error',
        errors: ['ащипка 101', 'другая ащипка'],
    },
};
