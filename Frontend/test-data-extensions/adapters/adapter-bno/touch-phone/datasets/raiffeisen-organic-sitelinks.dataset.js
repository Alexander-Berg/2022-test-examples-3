module.exports = function() {
    return {
        url: { url: 'http://yandex.ru' },
        path: [{
            text: 'raiffeisen.ru',
            url: 'raiffeisen.ru'
        }],
        marker: 'мобильная версия',
        title: { text: '«\u0007[Райффайзенбанк\u0007]» — банковская группа - \u0007[Москва\u0007]' },
        text: { text: 'Услуги для частных и корпоративных клиентов, малого бизнеса, финансовых институтов. ' +
            'Тарифы и условия обслуживания. Онлайн-заявки. Адреса, телефоны, режим работы \u0007[банка\u0007] и ' +
            'филиалов. Список банкоматов.'
        },
        sitelinks: {
            items: [
                {
                    title: 'Интернет-банк',
                    desc: 'Raiffeisen CONNECT - обеспечивает высокую надежность...',
                    url: 'http://connect.raiffeisen.ru/'
                },
                {
                    title: '\u0007[Отделения\u0007] и банкоматы',
                    desc: 'Денежные средства \u0007[доступны по карте\u0007] мгновенно (для карт...',
                    url: 'http://www.raiffeisen.ru/retail/'
                },
                {
                    title: 'Курсы валют',
                    desc: 'Обмен валюты по выгодному курсу в банке Райффайзенбанк...',
                    url: 'http://www.raiffeisen.ru/currency_rates/'
                },
                {
                    title: 'Скидки для Вас',
                    desc: 'Представляем вашему вниманию новые акции для наших...',
                    url: 'http://www.raiffeisen.ru/special_offers/'
                }
            ]
        }
    };
};
