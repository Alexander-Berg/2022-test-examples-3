var stubs = require('@yandex-int/gemini-serp-stubs');

module.exports = {
    type: 'snippet',
    data_stub:   {
        num: 0,
        snippets: {
            full: {
                biz: [
                  {
                    url: 'https://yandex.ru/maps/?oid=1240226014&ol=biz&source=wizgeo',
                    name: 'Центр психологии и тренинга Марии Минаковой',
                    mobileUrl: 'https://yandex.ru/maps/?oid=1240226014&ol=biz&source=wizgeo',
                    id: '1240226014'
                  }
                ],
                photo: stubs.imageUrlStub(100, 100, { color: '30aaca', format: 'png', patternSize: 20 }),
                region: {},
                region_ll: [
                  '37.620393',
                  '55.753960'
                ],
                region_spn: [
                  '0.641442',
                  '0.466439'
                ],
                subtype: null,
                applicable: 1,
                url: {
                  link: '',
                  canonical: '',
                  cut_www: null,
                  query_string: '',
                  __package: 'YxWeb::Util::Url',
                  path: '/maps/',
                  hostname: 'yandex.ru',
                  scheme: 'https',
                  port: null,
                  __is_plain: 1,
                  anchor: null
                },
                serp_info: {
                  template: 'maps',
                  format: 'json',
                  type: 'maps',
                  flat: 1,
                  counter_prefix: '/snippet/maps/'
                },
                metro: [
                  {
                    color: '7f0000',
                    distance: '436.859',
                    name: 'Парк культуры'
                  },
                  {
                    color: 'cc0000',
                    distance: '590.962',
                    name: 'Парк культуры'
                  },
                  {
                    color: 'cc0000',
                    distance: '838.121',
                    name: 'Фрунзенская'
                  }
                ],
                map_box_latitude: '0.004633',
                template: 'maps',
                data: {},
                map_latitude: '55.734065',
                type: 'maps',
                accuracy: '1',
                types: {
                  kind: 'wizard',
                  all: [
                    'snippets',
                    'maps'
                  ],
                  main: 'maps'
                },
                geoid: 213,
                map_longitude: '37.586365',
                na_karte_chego: 'Москвы',
                counter_prefix: '/snippet/maps/',
                map_box_longitude: '0.00821',
                city: 'Москва',
                kind: 'house',
                text: 'Улица Льва Толстого, 19',
                highlightedTitle: 'Улица Льва Толстого, 19 на карте Москвы',
                panorama: '#',
                slot_rank: 0,
                slot: 'full',
                country: 'Россия',
                path: 'Россия, Москва, улица Льва Толстого, 19',
                crossroad: null
            }
        },
        doctitle: 'maps.yandex.ru/geo_wizard',
        url_parts: {},
        size: 0,
        is_recent: 0,
        url: 'http://maps.yandex.ru/geo_wizard',
        green_url: 'maps.yandex.ru/geo_wizard',
        host: 'maps.yandex.ru',
        favicon_domain: 'maps.yandex.ru',
        markers: {
          Rule: 'Vertical/ToponymWizard_maps',
          WizardPos: '0'
        }
    }
};
