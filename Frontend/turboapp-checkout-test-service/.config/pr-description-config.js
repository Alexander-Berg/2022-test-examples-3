module.exports = {
    service: 'turboapp-checkout-test-service',
    '@title': 'Чекаут: Тестовый Магазин',
    '@iconUrl': 'https://yastat.net/s3/tap/checkout/images/favicon.ico',
    pipeLinks: [
        {
            label: 'Бета',
            url: 'https://checkout-test-service-pr-{{github_payload.pull_request.number}}.tap-tst.yandex.ru/',
            qr: true,
        },
    ],
};
