var stubs = require('@yandex-int/gemini-serp-stubs');

module.exports = {
    type: 'snippet',
    data_stub: {
        num: 0,
        snippets: {
            main: {
                template: 'generic',
                // jscs:disable maximumLineLength
                headline: 'Лекарства, предметы гигиены, спортивное питание, медицинские изделия и приборы и др. Адреса пунктов доставки.',
                // jscs:enable maximumLineLength
                passages: [],
                type: 'yaca'
            },
            post: [
                {
                    type: 'adresa',
                    items: [
                        {
                            numitems: 1,
                            company_id: 1094780958,
                            map_longitude: 60,
                            map_latitude: 56,
                            metro: 'Площадь 1905 года',
                            address: 'Екатеринбург, ул. Аппаратная, 7',
                            time: 'ежедневно, круглосуточно',
                            phone: '+7 (343) 379-09-41',
                            name: 'Катрен',
                            local_region_id: 54
                        }
                    ],
                    counter_prefix: '/snippet/adress_button/',
                    where: {
                        span_arrayref: stubs.constant([0, 0])
                    },
                    what: 'аптека',
                    types: {
                        kind: 'snippets'
                    }
                }
            ]
        },
        doctitle: '«\u0007[Аптека\u0007].ру» — интернет-\u0007[аптека\u0007]',
        url: 'http://apteka.ru/',
        host: 'apteka.ru',
        green_url: '\u0007[apteka\u0007].ru'
    }
};
