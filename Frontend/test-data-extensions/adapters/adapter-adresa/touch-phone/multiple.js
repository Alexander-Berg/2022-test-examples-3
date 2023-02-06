var stubs = require('@yandex-int/gemini-serp-stubs');

module.exports = {
    type: 'snippet',
    data_stub: {
        num: 0,
        snippets: {
            main: {
                template: 'generic',
                // jscs:disable maximumLineLength
                headline: 'Продажа лекарств, лечебной косметики, биологически активных добавок, медицинских приборов. Система подбора лекарств, цены. Возможность онлайн-бронирования лекарственных средств. Адреса аптек.',
                // jscs:enable maximumLineLength
                passages: [],
                type: 'yaca'
            },
            post: [
                {
                    type: 'adresa',
                    items: [
                        {
                            numitems: 2,
                            url: 'http://zhivika.ru',
                            metro: 'Геологическая',
                            time: 'сб 8:00–22:00; вс 10:00–20:00; пн-пт 8:00–22:00',
                            map_longitude: 60,
                            map_latitude: 56,
                            company_id: 1243633548,
                            name: 'Живика',
                            address: 'Екатеринбург, ул. Вайнера, 60',
                            phone: '+7 (343) 287-41-79',
                            local_region_id: 54
                        },
                        {
                            map_longitude: 60,
                            map_latitude: 56,
                            company_id: 1129679728,
                            name: 'Живика',
                            address: 'Екатеринбург, ул. Свердлова, 66а',
                            metro: 'Геологическая',
                            time: 'ежедневно, 8:00–22:00',
                            phone: '+7 (343) 354-32-99'
                        }
                    ],
                    counter_prefix: '/snippet/adress_button/',
                    where: {
                        name_in: stubs.constant('в Екатеринбурге'),
                        name: stubs.constant('Екатеринбург'),
                        span_arrayref: stubs.constant([0, 0])
                    },
                    what: 'аптека',
                    types: {
                        kind: 'snippets'
                    }
                }
            ]
        },
        doctitle: 'Интернет-\u0007[аптека\u0007] Живика - \u0007[Екатеринбург\u0007]',
        url: 'http://Zhivika.ru/',
        host: 'Zhivika.ru',
        green_url: 'Zhivika.ru'
    }
};
