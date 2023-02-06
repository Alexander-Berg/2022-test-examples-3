'use strict';

module.exports = {
    type: 'snippet',
    request_text: 'погода в москве',
    data_stub: {
        supplementary: {
            generic: [
                {
                    passages: [],
                    links: {},
                    headline_src: null,
                    headline: '',
                    type: 'generic',
                    applicable: 1,
                    by_link: '',
                    is_generic: 1,
                    attrs: {},
                    template: 'generic',
                    counter_prefix: '/snippet/generic/',
                    passage_attrs: [],
                    types: {
                        kind: 'snippets',
                        main: 'generic',
                        all: ['snippets', 'generic'],
                        extra: []
                    }
                }
            ],
            domain_link: [
                {
                    green_tail: 'moscow',
                    green_domain: 'pogoda.yandex.ru',
                    domain_href: 'http://pogoda.yandex.ru/'
                }
            ]
        },
        favicon_domain: 'pogoda.yandex.ru',
        green_url: 'pogoda.yandex.ru/moscow',
        url: 'http://pogoda.yandex.ru/moscow',
        server_descr: 'WEATHER_PROXY',
        markers: {
            WizardPos: '0',
            Rule: 'Vertical/wizweather'
        },
        doctitle: 'pogoda.yandex.ru/moscow',
        url_parts: {
            hostname: 'pogoda.yandex.ru',
            scheme: 'http',
            cut_www: null,
            anchor: null,
            link: '/moscow',
            __is_plain: 1,
            query_string: '',
            path: '/moscow',
            __package: 'YxWeb::Util::Url',
            port: null,
            canonical: 'http://pogoda.yandex.ru/moscow'
        },
        host: 'pogoda.yandex.ru',
        num: '0',
        _markers: [],
        mime: '',
        signed_saved_copy_url: 'http://hghltd.yandex.net/yandbtm',
        is_recent: '1',
        snippets: {
            full: {
                slot: 'full',
                city: {
                    id: '213',
                    path: ['10000', '10001', '225', '3', '1'],
                    name: {
                        ru: {
                            genitive: 'Москвы',
                            locative: 'Москве',
                            nominative: 'Москва'
                        }
                    },
                    preposition: {
                        ru: 'в'
                    }
                },
                cityid: 'moscow',
                type: 'weather',
                city_disambiguation: [],
                slot_rank: 0,
                data: {},
                applicable: 1,
                forecast: [
                    {
                        hours: [
                            {
                                _fallback_prec: false,
                                prec_prob: 10,
                                soil_temp: 18,
                                _fallback_temp: false,
                                pressure_pa: 1000,
                                uv_index: 0,
                                hour_ts: 1502139600,
                                soil_moisture: 0.26,
                                prec_mm: 0,
                                wind_dir: 'nw',
                                feels_like_color: 'f6f3d6',
                                pressure_mm: 750,
                                condition: 'overcast',
                                icon: 'ovc',
                                wind_speed: 0.9,
                                temp_color: 'f7f3d3',
                                wind_gust: 4,
                                hour: '0',
                                humidity: 71,
                                temp: 17,
                                feels_like: 16,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 10,
                                soil_temp: 18,
                                _fallback_temp: false,
                                pressure_pa: 1002,
                                uv_index: 0,
                                hour_ts: 1502143200,
                                soil_moisture: 0.26,
                                prec_mm: 0,
                                wind_dir: 'nw',
                                feels_like_color: 'f6f3d6',
                                pressure_mm: 751,
                                condition: 'overcast',
                                icon: 'ovc',
                                wind_speed: 0.8,
                                temp_color: 'f7f3d3',
                                wind_gust: 3.8,
                                hour: '1',
                                humidity: 73,
                                temp: 17,
                                feels_like: 16,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 10,
                                soil_temp: 16,
                                _fallback_temp: false,
                                pressure_pa: 1002,
                                uv_index: 0,
                                hour_ts: 1502146800,
                                soil_moisture: 0.26,
                                prec_mm: 0,
                                wind_dir: 'w',
                                feels_like_color: 'f6f3d6',
                                pressure_mm: 751,
                                condition: 'overcast',
                                icon: 'ovc',
                                wind_speed: 0.7,
                                temp_color: 'f6f3d6',
                                wind_gust: 3.5,
                                hour: '2',
                                humidity: 79,
                                temp: 15,
                                feels_like: 15,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 16,
                                _fallback_temp: false,
                                pressure_pa: 1002,
                                uv_index: 0,
                                hour_ts: 1502150400,
                                soil_moisture: 0.26,
                                prec_mm: 0,
                                wind_dir: 's',
                                feels_like_color: 'f6f3d6',
                                pressure_mm: 751,
                                condition: 'clear',
                                icon: 'skc_n',
                                wind_speed: 0,
                                temp_color: 'f6f3d6',
                                wind_gust: 1.9,
                                hour: '3',
                                humidity: 82,
                                temp: 15,
                                feels_like: 15,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 10,
                                soil_temp: 16,
                                _fallback_temp: false,
                                pressure_pa: 1002,
                                uv_index: 0,
                                hour_ts: 1502154000,
                                soil_moisture: 0.26,
                                prec_mm: 0,
                                wind_dir: 'nw',
                                feels_like_color: 'f6f3d6',
                                pressure_mm: 751,
                                condition: 'overcast',
                                icon: 'ovc',
                                wind_speed: 0.6,
                                temp_color: 'f6f3d6',
                                wind_gust: 2.8,
                                hour: '4',
                                humidity: 77,
                                temp: 15,
                                feels_like: 15,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 16,
                                _fallback_temp: false,
                                pressure_pa: 1002,
                                uv_index: 0,
                                hour_ts: 1502157600,
                                soil_moisture: 0.26,
                                prec_mm: 0,
                                wind_dir: 'nw',
                                feels_like_color: 'f5f2d9',
                                pressure_mm: 751,
                                condition: 'overcast',
                                icon: 'ovc',
                                wind_speed: 0.9,
                                temp_color: 'f5f2d9',
                                wind_gust: 4.1,
                                hour: '5',
                                humidity: 79,
                                temp: 14,
                                feels_like: 14,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 16,
                                _fallback_temp: false,
                                pressure_pa: 1002,
                                uv_index: 0,
                                hour_ts: 1502161200,
                                soil_moisture: 0.26,
                                prec_mm: 0,
                                wind_dir: 'nw',
                                feels_like_color: 'f5f2d9',
                                pressure_mm: 751,
                                condition: 'overcast',
                                icon: 'ovc',
                                wind_speed: 1.2,
                                temp_color: 'f5f2d9',
                                wind_gust: 4.3,
                                hour: '6',
                                humidity: 80,
                                temp: 14,
                                feels_like: 14,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 16,
                                _fallback_temp: false,
                                pressure_pa: 1002,
                                uv_index: 0,
                                hour_ts: 1502164800,
                                soil_moisture: 0.26,
                                prec_mm: 0,
                                wind_dir: 'nw',
                                feels_like_color: 'f6f3d6',
                                pressure_mm: 751,
                                condition: 'overcast',
                                icon: 'ovc',
                                wind_speed: 1.3,
                                temp_color: 'f6f3d6',
                                wind_gust: 2.9,
                                hour: '7',
                                humidity: 76,
                                temp: 15,
                                feels_like: 15,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 18,
                                _fallback_temp: false,
                                pressure_pa: 1002,
                                uv_index: 2,
                                hour_ts: 1502168400,
                                soil_moisture: 0.26,
                                prec_mm: 0,
                                wind_dir: 'nw',
                                feels_like_color: 'f6f3d6',
                                pressure_mm: 751,
                                condition: 'overcast',
                                icon: 'ovc',
                                wind_speed: 1.5,
                                temp_color: 'f6f3d6',
                                wind_gust: 2.3,
                                hour: '8',
                                humidity: 73,
                                temp: 16,
                                feels_like: 16,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 18,
                                _fallback_temp: false,
                                pressure_pa: 1003,
                                uv_index: 2,
                                hour_ts: 1502172000,
                                soil_moisture: 0.26,
                                prec_mm: 0,
                                wind_dir: 's',
                                feels_like_color: 'f8f4d0',
                                pressure_mm: 752,
                                condition: 'clear',
                                icon: 'skc_d',
                                wind_speed: 0,
                                temp_color: 'f8f4d0',
                                wind_gust: 1.2,
                                hour: '9',
                                humidity: 66,
                                temp: 19,
                                feels_like: 20,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 18,
                                _fallback_temp: false,
                                pressure_pa: 1003,
                                uv_index: 2,
                                hour_ts: 1502175600,
                                soil_moisture: 0.26,
                                prec_mm: 0,
                                wind_dir: 'nw',
                                feels_like_color: 'f8f4d0',
                                pressure_mm: 752,
                                condition: 'clear',
                                icon: 'skc_d',
                                wind_speed: 0.8,
                                temp_color: 'f8f4d0',
                                wind_gust: 1.5,
                                hour: '10',
                                humidity: 59,
                                temp: 20,
                                feels_like: 19,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 21,
                                _fallback_temp: false,
                                pressure_pa: 1003,
                                uv_index: 6,
                                hour_ts: 1502179200,
                                soil_moisture: 0.25,
                                prec_mm: 0,
                                wind_dir: 'nw',
                                feels_like_color: 'f8f1c8',
                                pressure_mm: 752,
                                condition: 'clear',
                                icon: 'skc_d',
                                wind_speed: 1.6,
                                temp_color: 'f8f1c8',
                                wind_gust: 2.9,
                                hour: '11',
                                humidity: 53,
                                temp: 21,
                                feels_like: 21,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 21,
                                _fallback_temp: false,
                                pressure_pa: 1003,
                                uv_index: 6,
                                hour_ts: 1502182800,
                                soil_moisture: 0.25,
                                prec_mm: 0,
                                wind_dir: 'nw',
                                feels_like_color: 'f8f1c8',
                                pressure_mm: 752,
                                condition: 'clear',
                                icon: 'skc_d',
                                wind_speed: 2.5,
                                temp_color: 'f8f1c8',
                                wind_gust: 3.4,
                                hour: '12',
                                humidity: 47,
                                temp: 22,
                                feels_like: 21,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 21,
                                _fallback_temp: false,
                                pressure_pa: 1003,
                                uv_index: 6,
                                hour_ts: 1502186400,
                                soil_moisture: 0.25,
                                prec_mm: 0,
                                wind_dir: 'nw',
                                feels_like_color: 'f8f1c8',
                                pressure_mm: 752,
                                condition: 'clear',
                                icon: 'skc_d',
                                wind_speed: 2.7,
                                temp_color: 'f9eec0',
                                wind_gust: 3.4,
                                hour: '13',
                                humidity: 47,
                                temp: 23,
                                feels_like: 22,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 22,
                                _fallback_temp: false,
                                pressure_pa: 1003,
                                uv_index: 4,
                                hour_ts: 1502190000,
                                soil_moisture: 0.25,
                                prec_mm: 0,
                                wind_dir: 'nw',
                                feels_like_color: 'f8f1c8',
                                pressure_mm: 752,
                                condition: 'clear',
                                icon: 'skc_d',
                                wind_speed: 2.7,
                                temp_color: 'f9eec0',
                                wind_gust: 3.6,
                                hour: '14',
                                humidity: 39,
                                temp: 23,
                                feels_like: 21,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 22,
                                _fallback_temp: false,
                                pressure_pa: 1003,
                                uv_index: 4,
                                hour_ts: 1502193600,
                                soil_moisture: 0.25,
                                prec_mm: 0,
                                wind_dir: 'nw',
                                feels_like_color: 'f8f1c8',
                                pressure_mm: 752,
                                condition: 'clear',
                                icon: 'skc_d',
                                wind_speed: 2.7,
                                temp_color: 'f9eec0',
                                wind_gust: 3.4,
                                hour: '15',
                                humidity: 44,
                                temp: 23,
                                feels_like: 22,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 22,
                                _fallback_temp: false,
                                pressure_pa: 1003,
                                uv_index: 4,
                                hour_ts: 1502197200,
                                soil_moisture: 0.25,
                                prec_mm: 0,
                                wind_dir: 'nw',
                                feels_like_color: 'f8f1c8',
                                pressure_mm: 752,
                                condition: 'clear',
                                icon: 'skc_d',
                                wind_speed: 2.7,
                                temp_color: 'f9eec0',
                                wind_gust: 2.8,
                                hour: '16',
                                humidity: 37,
                                temp: 23,
                                feels_like: 21,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 21,
                                _fallback_temp: false,
                                pressure_pa: 1004,
                                uv_index: 1,
                                hour_ts: 1502200800,
                                soil_moisture: 0.24,
                                prec_mm: 0,
                                condition: 'clear',
                                _nowcast: true,
                                feels_like_color: 'f8f1c8',
                                pressure_mm: 753,
                                wind_dir: 'n',
                                icon: 'skc_d',
                                wind_speed: 2.6,
                                temp_color: 'f9eec0',
                                wind_gust: 3.6,
                                hour: '17',
                                humidity: 41,
                                temp: 23,
                                feels_like: 22,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 21,
                                _fallback_temp: false,
                                pressure_pa: 1004,
                                uv_index: 1,
                                hour_ts: 1502204400,
                                soil_moisture: 0.24,
                                prec_mm: 0,
                                condition: 'clear',
                                _nowcast: true,
                                feels_like_color: 'f8f1c8',
                                pressure_mm: 753,
                                wind_dir: 'n',
                                icon: 'skc_d',
                                wind_speed: 2.5,
                                temp_color: 'f9eec0',
                                wind_gust: 4.1,
                                hour: '18',
                                humidity: 46,
                                temp: 23,
                                feels_like: 22,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 21,
                                _fallback_temp: false,
                                pressure_pa: 1004,
                                uv_index: 1,
                                hour_ts: 1502208000,
                                soil_moisture: 0.24,
                                prec_mm: 0,
                                condition: 'clear',
                                _nowcast: true,
                                feels_like_color: 'f8f1c8',
                                pressure_mm: 753,
                                wind_dir: 'n',
                                icon: 'skc_d',
                                wind_speed: 2.3,
                                temp_color: 'f9eec0',
                                wind_gust: 4.1,
                                hour: '19',
                                humidity: 45,
                                temp: 23,
                                feels_like: 21,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 19,
                                _fallback_temp: false,
                                pressure_pa: 1004,
                                uv_index: 0,
                                hour_ts: 1502211600,
                                soil_moisture: 0.24,
                                prec_mm: 0,
                                wind_dir: 'n',
                                feels_like_color: 'f8f1c8',
                                pressure_mm: 753,
                                condition: 'clear',
                                icon: 'skc_d',
                                wind_speed: 1.8,
                                temp_color: 'f8f1c8',
                                wind_gust: 4.9,
                                hour: '20',
                                humidity: 56,
                                temp: 21,
                                feels_like: 21,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 19,
                                _fallback_temp: false,
                                pressure_pa: 1006,
                                uv_index: 0,
                                hour_ts: 1502215200,
                                soil_moisture: 0.24,
                                prec_mm: 0,
                                wind_dir: 'n',
                                feels_like_color: 'f8f4d0',
                                pressure_mm: 754,
                                condition: 'clear',
                                icon: 'skc_n',
                                wind_speed: 1.4,
                                temp_color: 'f8f4d0',
                                wind_gust: 7.1,
                                hour: '21',
                                humidity: 61,
                                temp: 20,
                                feels_like: 19,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 19,
                                _fallback_temp: false,
                                pressure_pa: 1006,
                                uv_index: 0,
                                hour_ts: 1502218800,
                                soil_moisture: 0.24,
                                prec_mm: 0,
                                wind_dir: 'n',
                                feels_like_color: 'f8f4d0',
                                pressure_mm: 754,
                                condition: 'clear',
                                icon: 'skc_n',
                                wind_speed: 1.3,
                                temp_color: 'f8f4d0',
                                wind_gust: 6.8,
                                hour: '22',
                                humidity: 64,
                                temp: 19,
                                feels_like: 19,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 18,
                                _fallback_temp: false,
                                pressure_pa: 1007,
                                uv_index: 0,
                                hour_ts: 1502222400,
                                soil_moisture: 0.24,
                                prec_mm: 0,
                                wind_dir: 'n',
                                feels_like_color: 'f7f3d3',
                                pressure_mm: 755,
                                condition: 'clear',
                                icon: 'skc_n',
                                wind_speed: 1.2,
                                temp_color: 'f7f3d3',
                                wind_gust: 5,
                                hour: '23',
                                humidity: 74,
                                temp: 18,
                                feels_like: 18,
                                prec_period: 60
                            }
                        ],
                        date: '2017-08-07T21:00:00.000Z',
                        parts: [
                            {
                                temperature_to: 17,
                                wind_speed: 0.9,
                                temperature_from: 14,
                                temperature_min: 14,
                                temp_avg: 16,
                                weather_type: 'Облачно с прояснениями',
                                humidity: 77,
                                pressure: {
                                    content: 751,
                                    units: 'mm'
                                },
                                temperature_max: 17,
                                'temperature-data': {
                                    to: 17,
                                    from: 14
                                },
                                'image-v3': {
                                    content: 'bkn_n'
                                },
                                type: 'night',
                                weather_condition: {
                                    code: 'cloudy'
                                }
                            },
                            {
                                temperature_to: 21,
                                wind_speed: 1.6,
                                temperature_from: 14,
                                temperature_min: 14,
                                temp_avg: 18,
                                weather_type: 'Облачно с прояснениями',
                                humidity: 68,
                                pressure: {
                                    content: 752,
                                    units: 'mm'
                                },
                                temperature_max: 21,
                                'temperature-data': {
                                    to: 21,
                                    from: 14
                                },
                                'image-v3': {
                                    content: 'bkn_d'
                                },
                                type: 'morning',
                                weather_condition: {
                                    code: 'cloudy'
                                }
                            },
                            {
                                temperature_to: 23,
                                wind_speed: 2.7,
                                temperature_from: 22,
                                temperature_min: 22,
                                temp_avg: 23,
                                weather_type: 'Ясно',
                                humidity: 43,
                                pressure: {
                                    content: 752,
                                    units: 'mm'
                                },
                                temperature_max: 23,
                                'temperature-data': {
                                    to: 23,
                                    from: 22
                                },
                                'image-v3': {
                                    content: 'skc_d'
                                },
                                type: 'day',
                                weather_condition: {
                                    code: 'clear'
                                }
                            },
                            {
                                temperature_to: 23,
                                wind_speed: 2.5,
                                temperature_from: 18,
                                temperature_min: 18,
                                temp_avg: 21,
                                weather_type: 'Ясно',
                                humidity: 58,
                                pressure: {
                                    content: 754,
                                    units: 'mm'
                                },
                                temperature_max: 23,
                                'temperature-data': {
                                    to: 23,
                                    from: 18
                                },
                                'image-v3': {
                                    content: 'skc_d'
                                },
                                type: 'evening',
                                weather_condition: {
                                    code: 'clear'
                                }
                            },
                            {
                                wind_speed: 1.2,
                                temperature: 14,
                                weather_type: 'Ясно',
                                image: {
                                    content: 'skc_n_+14'
                                },
                                humidity: 88,
                                pressure: {
                                    content: 756,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 14,
                                        bgcolor: 'f5f2d9'
                                    }
                                },
                                'image-v2': {
                                    content: 'skc_n_+14'
                                },
                                'image-v3': {
                                    content: 'skc_n'
                                },
                                type: 'night_short',
                                weather_condition: {
                                    code: 'clear'
                                }
                            },
                            {
                                wind_speed: 2.7,
                                temperature: 23,
                                temperature_from: 16,
                                temperature_min: 16,
                                image: {
                                    content: 'skc_d_+24'
                                },
                                weather_type: 'Ясно',
                                humidity: 50,
                                pressure: {
                                    content: 752,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 24,
                                        bgcolor: 'f9eec0'
                                    },
                                    from: 16
                                },
                                'image-v2': {
                                    content: 'skc_d_+24'
                                },
                                'image-v3': {
                                    content: 'skc_d'
                                },
                                type: 'day_short',
                                weather_condition: {
                                    code: 'clear'
                                }
                            }
                        ],
                        type: 'today',
                        current_part: 2
                    },
                    {
                        hours: [
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 18,
                                _fallback_temp: false,
                                pressure_pa: 1007,
                                uv_index: 0,
                                hour_ts: 1502226000,
                                soil_moisture: 0.24,
                                prec_mm: 0,
                                wind_dir: 'n',
                                feels_like_color: 'f7f3d3',
                                pressure_mm: 755,
                                condition: 'clear',
                                icon: 'skc_n',
                                wind_speed: 1.1,
                                temp_color: 'f7f3d3',
                                wind_gust: 5.4,
                                hour: '0',
                                humidity: 77,
                                temp: 17,
                                feels_like: 18,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 18,
                                _fallback_temp: false,
                                pressure_pa: 1007,
                                uv_index: 0,
                                hour_ts: 1502229600,
                                soil_moisture: 0.24,
                                prec_mm: 0,
                                wind_dir: 'ne',
                                feels_like_color: 'f6f3d6',
                                pressure_mm: 755,
                                condition: 'clear',
                                icon: 'skc_n',
                                wind_speed: 1.1,
                                temp_color: 'f6f3d6',
                                wind_gust: 4.8,
                                hour: '1',
                                humidity: 85,
                                temp: 16,
                                feels_like: 16,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 17,
                                _fallback_temp: false,
                                pressure_pa: 1008,
                                uv_index: 0,
                                hour_ts: 1502233200,
                                soil_moisture: 0.24,
                                prec_mm: 0,
                                wind_dir: 'ne',
                                feels_like_color: 'f6f3d6',
                                pressure_mm: 756,
                                condition: 'clear',
                                icon: 'skc_n',
                                wind_speed: 1,
                                temp_color: 'f6f3d6',
                                wind_gust: 2.8,
                                hour: '2',
                                humidity: 89,
                                temp: 15,
                                feels_like: 16,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 17,
                                _fallback_temp: false,
                                pressure_pa: 1008,
                                uv_index: 0,
                                hour_ts: 1502236800,
                                soil_moisture: 0.24,
                                prec_mm: 0,
                                wind_dir: 'n',
                                feels_like_color: 'f6f3d6',
                                pressure_mm: 756,
                                condition: 'clear',
                                icon: 'skc_n',
                                wind_speed: 0.9,
                                temp_color: 'f6f3d6',
                                wind_gust: 4.6,
                                hour: '3',
                                humidity: 89,
                                temp: 15,
                                feels_like: 16,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 17,
                                _fallback_temp: false,
                                pressure_pa: 1008,
                                uv_index: 0,
                                hour_ts: 1502240400,
                                soil_moisture: 0.24,
                                prec_mm: 0,
                                wind_dir: 'ne',
                                feels_like_color: 'f5f2d9',
                                pressure_mm: 756,
                                condition: 'clear',
                                icon: 'skc_n',
                                wind_speed: 1.2,
                                temp_color: 'f5f2d9',
                                wind_gust: 4.8,
                                hour: '4',
                                humidity: 93,
                                temp: 14,
                                feels_like: 14,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 16,
                                _fallback_temp: false,
                                pressure_pa: 1010,
                                uv_index: 0,
                                hour_ts: 1502244000,
                                soil_moisture: 0.24,
                                prec_mm: 0,
                                wind_dir: 'ne',
                                feels_like_color: 'f5f2d9',
                                pressure_mm: 757,
                                condition: 'clear',
                                icon: 'skc_d',
                                wind_speed: 1.2,
                                temp_color: 'f5f2d9',
                                wind_gust: 4.6,
                                hour: '5',
                                humidity: 92,
                                temp: 14,
                                feels_like: 14,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 16,
                                _fallback_temp: false,
                                pressure_pa: 1010,
                                uv_index: 0,
                                hour_ts: 1502247600,
                                soil_moisture: 0.24,
                                prec_mm: 0,
                                wind_dir: 'ne',
                                feels_like_color: 'f5f2d9',
                                pressure_mm: 757,
                                condition: 'clear',
                                icon: 'skc_d',
                                wind_speed: 1,
                                temp_color: 'f5f2d9',
                                wind_gust: 3.5,
                                hour: '6',
                                humidity: 87,
                                temp: 14,
                                feels_like: 14,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 16,
                                _fallback_temp: false,
                                pressure_pa: 1010,
                                uv_index: 0,
                                hour_ts: 1502251200,
                                soil_moisture: 0.24,
                                prec_mm: 0,
                                wind_dir: 'ne',
                                feels_like_color: 'f6f3d6',
                                pressure_mm: 757,
                                condition: 'clear',
                                icon: 'skc_d',
                                wind_speed: 1.3,
                                temp_color: 'f6f3d6',
                                wind_gust: 4,
                                hour: '7',
                                humidity: 80,
                                temp: 16,
                                feels_like: 15,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 19,
                                _fallback_temp: false,
                                pressure_pa: 1011,
                                uv_index: 3,
                                hour_ts: 1502254800,
                                soil_moisture: 0.24,
                                prec_mm: 0,
                                wind_dir: 'ne',
                                feels_like_color: 'f7f3d3',
                                pressure_mm: 758,
                                condition: 'clear',
                                icon: 'skc_d',
                                wind_speed: 1.5,
                                temp_color: 'f7f3d3',
                                wind_gust: 2.9,
                                hour: '8',
                                humidity: 70,
                                temp: 18,
                                feels_like: 18,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 19,
                                _fallback_temp: false,
                                pressure_pa: 1011,
                                uv_index: 3,
                                hour_ts: 1502258400,
                                soil_moisture: 0.24,
                                prec_mm: 0,
                                wind_dir: 'ne',
                                feels_like_color: 'f7f3d3',
                                pressure_mm: 758,
                                condition: 'clear',
                                icon: 'skc_d',
                                wind_speed: 2,
                                temp_color: 'f8f4d0',
                                wind_gust: 4.2,
                                hour: '9',
                                humidity: 61,
                                temp: 19,
                                feels_like: 18,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 19,
                                _fallback_temp: false,
                                pressure_pa: 1011,
                                uv_index: 3,
                                hour_ts: 1502262000,
                                soil_moisture: 0.24,
                                prec_mm: 0,
                                wind_dir: 'ne',
                                feels_like_color: 'f8f4d0',
                                pressure_mm: 758,
                                condition: 'clear',
                                icon: 'skc_d',
                                wind_speed: 1.9,
                                temp_color: 'f8f4d0',
                                wind_gust: 3,
                                hour: '10',
                                humidity: 57,
                                temp: 20,
                                feels_like: 19,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 21,
                                _fallback_temp: false,
                                pressure_pa: 1010,
                                uv_index: 6,
                                hour_ts: 1502265600,
                                soil_moisture: 0.24,
                                prec_mm: 0,
                                wind_dir: 'ne',
                                feels_like_color: 'f8f4d0',
                                pressure_mm: 757,
                                condition: 'clear',
                                icon: 'skc_d',
                                wind_speed: 2,
                                temp_color: 'f8f1c8',
                                wind_gust: 2.7,
                                hour: '11',
                                humidity: 50,
                                temp: 22,
                                feels_like: 20,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 21,
                                _fallback_temp: false,
                                pressure_pa: 1010,
                                uv_index: 6,
                                hour_ts: 1502269200,
                                soil_moisture: 0.24,
                                prec_mm: 0,
                                wind_dir: 'n',
                                feels_like_color: 'f8f4d0',
                                pressure_mm: 757,
                                condition: 'clear',
                                icon: 'skc_d',
                                wind_speed: 2.3,
                                temp_color: 'f8f1c8',
                                wind_gust: 2.8,
                                hour: '12',
                                humidity: 46,
                                temp: 22,
                                feels_like: 20,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 21,
                                _fallback_temp: false,
                                pressure_pa: 1010,
                                uv_index: 6,
                                hour_ts: 1502272800,
                                soil_moisture: 0.24,
                                prec_mm: 0,
                                wind_dir: 'n',
                                feels_like_color: 'f8f1c8',
                                pressure_mm: 757,
                                condition: 'clear',
                                icon: 'skc_d',
                                wind_speed: 2.3,
                                temp_color: 'f9eec0',
                                wind_gust: 2.3,
                                hour: '13',
                                humidity: 43,
                                temp: 23,
                                feels_like: 21,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 23,
                                _fallback_temp: false,
                                pressure_pa: 1010,
                                uv_index: 4,
                                hour_ts: 1502276400,
                                soil_moisture: 0.23,
                                prec_mm: 0,
                                wind_dir: 'ne',
                                feels_like_color: 'f8f1c8',
                                pressure_mm: 757,
                                condition: 'clear',
                                icon: 'skc_d',
                                wind_speed: 2.2,
                                temp_color: 'f9eec0',
                                wind_gust: 2.2,
                                hour: '14',
                                humidity: 40,
                                temp: 23,
                                feels_like: 21,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 23,
                                _fallback_temp: false,
                                pressure_pa: 1010,
                                uv_index: 4,
                                hour_ts: 1502280000,
                                soil_moisture: 0.23,
                                prec_mm: 0,
                                wind_dir: 'ne',
                                feels_like_color: 'f8f1c8',
                                pressure_mm: 757,
                                condition: 'clear',
                                icon: 'skc_d',
                                wind_speed: 2,
                                temp_color: 'f9eec0',
                                wind_gust: 2.6,
                                hour: '15',
                                humidity: 38,
                                temp: 24,
                                feels_like: 22,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 23,
                                _fallback_temp: false,
                                pressure_pa: 1010,
                                uv_index: 4,
                                hour_ts: 1502283600,
                                soil_moisture: 0.23,
                                prec_mm: 0,
                                wind_dir: 'ne',
                                feels_like_color: 'f9eec0',
                                pressure_mm: 757,
                                condition: 'clear',
                                icon: 'skc_d',
                                wind_speed: 2,
                                temp_color: 'f9eec0',
                                wind_gust: 2.5,
                                hour: '16',
                                humidity: 39,
                                temp: 24,
                                feels_like: 23,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 22,
                                _fallback_temp: false,
                                pressure_pa: 1010,
                                uv_index: 1,
                                hour_ts: 1502287200,
                                soil_moisture: 0.23,
                                prec_mm: 0,
                                wind_dir: 'ne',
                                feels_like_color: 'f8f1c8',
                                pressure_mm: 757,
                                condition: 'clear',
                                icon: 'skc_d',
                                wind_speed: 2,
                                temp_color: 'f9eec0',
                                wind_gust: 2.5,
                                hour: '17',
                                humidity: 41,
                                temp: 23,
                                feels_like: 22,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 22,
                                _fallback_temp: false,
                                pressure_pa: 1010,
                                uv_index: 1,
                                hour_ts: 1502290800,
                                soil_moisture: 0.23,
                                prec_mm: 0,
                                wind_dir: 'ne',
                                feels_like_color: 'f8f1c8',
                                pressure_mm: 757,
                                condition: 'clear',
                                icon: 'skc_d',
                                wind_speed: 2,
                                temp_color: 'f9eec0',
                                wind_gust: 2.2,
                                hour: '18',
                                humidity: 41,
                                temp: 23,
                                feels_like: 22,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 22,
                                _fallback_temp: false,
                                pressure_pa: 1010,
                                uv_index: 1,
                                hour_ts: 1502294400,
                                soil_moisture: 0.23,
                                prec_mm: 0,
                                wind_dir: 'ne',
                                feels_like_color: 'f8f1c8',
                                pressure_mm: 757,
                                condition: 'clear',
                                icon: 'skc_d',
                                wind_speed: 1.9,
                                temp_color: 'f8f1c8',
                                wind_gust: 2.3,
                                hour: '19',
                                humidity: 47,
                                temp: 22,
                                feels_like: 21,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 19,
                                _fallback_temp: false,
                                pressure_pa: 1010,
                                uv_index: 0,
                                hour_ts: 1502298000,
                                soil_moisture: 0.23,
                                prec_mm: 0,
                                wind_dir: 'ne',
                                feels_like_color: 'f8f4d0',
                                pressure_mm: 757,
                                condition: 'clear',
                                icon: 'skc_d',
                                wind_speed: 1.4,
                                temp_color: 'f8f4d0',
                                wind_gust: 3,
                                hour: '20',
                                humidity: 58,
                                temp: 20,
                                feels_like: 19,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 19,
                                _fallback_temp: false,
                                pressure_pa: 1010,
                                uv_index: 0,
                                hour_ts: 1502301600,
                                soil_moisture: 0.23,
                                prec_mm: 0,
                                wind_dir: 'ne',
                                feels_like_color: 'f8f4d0',
                                pressure_mm: 757,
                                condition: 'clear',
                                icon: 'skc_n',
                                wind_speed: 0.9,
                                temp_color: 'f8f4d0',
                                wind_gust: 4.3,
                                hour: '21',
                                humidity: 60,
                                temp: 19,
                                feels_like: 19,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 19,
                                _fallback_temp: false,
                                pressure_pa: 1010,
                                uv_index: 0,
                                hour_ts: 1502305200,
                                soil_moisture: 0.23,
                                prec_mm: 0,
                                wind_dir: 'ne',
                                feels_like_color: 'f7f3d3',
                                pressure_mm: 757,
                                condition: 'clear',
                                icon: 'skc_n',
                                wind_speed: 0.6,
                                temp_color: 'f7f3d3',
                                wind_gust: 2.5,
                                hour: '22',
                                humidity: 62,
                                temp: 17,
                                feels_like: 17,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 16,
                                _fallback_temp: false,
                                pressure_pa: 1010,
                                uv_index: 0,
                                hour_ts: 1502308800,
                                soil_moisture: 0.23,
                                prec_mm: 0,
                                wind_dir: 'ne',
                                feels_like_color: 'f6f3d6',
                                pressure_mm: 757,
                                condition: 'clear',
                                icon: 'skc_n',
                                wind_speed: 0.6,
                                temp_color: 'f6f3d6',
                                wind_gust: 2.6,
                                hour: '23',
                                humidity: 71,
                                temp: 16,
                                feels_like: 16,
                                prec_period: 60
                            }
                        ],
                        date: '2017-08-08T21:00:00.000Z',
                        parts: [
                            {
                                temperature_to: 17,
                                wind_speed: 1.2,
                                temperature_from: 14,
                                temperature_min: 14,
                                temp_avg: 16,
                                weather_type: 'Ясно',
                                humidity: 88,
                                pressure: {
                                    content: 756,
                                    units: 'mm'
                                },
                                temperature_max: 17,
                                'temperature-data': {
                                    to: 17,
                                    from: 14
                                },
                                'image-v3': {
                                    content: 'skc_n'
                                },
                                type: 'night',
                                weather_condition: {
                                    code: 'clear'
                                }
                            },
                            {
                                temperature_to: 22,
                                wind_speed: 2,
                                temperature_from: 14,
                                temperature_min: 14,
                                temp_avg: 18,
                                weather_type: 'Ясно',
                                humidity: 67,
                                pressure: {
                                    content: 758,
                                    units: 'mm'
                                },
                                temperature_max: 22,
                                'temperature-data': {
                                    to: 22,
                                    from: 14
                                },
                                'image-v3': {
                                    content: 'skc_d'
                                },
                                type: 'morning',
                                weather_condition: {
                                    code: 'clear'
                                }
                            },
                            {
                                temperature_to: 24,
                                wind_speed: 2.3,
                                temperature_from: 22,
                                temperature_min: 22,
                                temp_avg: 23,
                                weather_type: 'Ясно',
                                humidity: 41,
                                pressure: {
                                    content: 757,
                                    units: 'mm'
                                },
                                temperature_max: 24,
                                'temperature-data': {
                                    to: 24,
                                    from: 22
                                },
                                'image-v3': {
                                    content: 'skc_d'
                                },
                                type: 'day',
                                weather_condition: {
                                    code: 'clear'
                                }
                            },
                            {
                                temperature_to: 23,
                                wind_speed: 2,
                                temperature_from: 16,
                                temperature_min: 16,
                                temp_avg: 20,
                                weather_type: 'Ясно',
                                humidity: 57,
                                pressure: {
                                    content: 757,
                                    units: 'mm'
                                },
                                temperature_max: 23,
                                'temperature-data': {
                                    to: 23,
                                    from: 16
                                },
                                'image-v3': {
                                    content: 'skc_d'
                                },
                                type: 'evening',
                                weather_condition: {
                                    code: 'clear'
                                }
                            },
                            {
                                wind_speed: 0.5,
                                temperature: 13,
                                weather_type: 'Ясно',
                                image: {
                                    content: 'skc_n_+14'
                                },
                                humidity: 88,
                                pressure: {
                                    content: 758,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 14,
                                        bgcolor: 'f5f2d9'
                                    }
                                },
                                'image-v2': {
                                    content: 'skc_n_+14'
                                },
                                'image-v3': {
                                    content: 'skc_n'
                                },
                                type: 'night_short',
                                weather_condition: {
                                    code: 'clear'
                                }
                            },
                            {
                                wind_speed: 2.3,
                                temperature: 24,
                                temperature_from: 18,
                                temperature_min: 18,
                                image: {
                                    content: 'skc_d_+24'
                                },
                                weather_type: 'Ясно',
                                humidity: 48,
                                pressure: {
                                    content: 757,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 24,
                                        bgcolor: 'f9eec0'
                                    },
                                    from: 18
                                },
                                'image-v2': {
                                    content: 'skc_d_+24'
                                },
                                'image-v3': {
                                    content: 'skc_d'
                                },
                                type: 'day_short',
                                weather_condition: {
                                    code: 'clear'
                                }
                            }
                        ]
                    },
                    {
                        hours: [
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 16,
                                _fallback_temp: false,
                                pressure_pa: 1011,
                                uv_index: 0,
                                hour_ts: 1502312400,
                                soil_moisture: 0.23,
                                prec_mm: 0,
                                wind_dir: 'ne',
                                feels_like_color: 'f5f2d9',
                                pressure_mm: 758,
                                condition: 'clear',
                                icon: 'skc_n',
                                wind_speed: 0.5,
                                temp_color: 'f5f2d9',
                                wind_gust: 0.5,
                                hour: '0',
                                humidity: 81,
                                temp: 14,
                                feels_like: 14,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 16,
                                _fallback_temp: false,
                                pressure_pa: 1011,
                                uv_index: 0,
                                hour_ts: 1502316000,
                                soil_moisture: 0.23,
                                prec_mm: 0,
                                wind_dir: 'ne',
                                feels_like_color: 'f5f2d9',
                                pressure_mm: 758,
                                condition: 'clear',
                                icon: 'skc_n',
                                wind_speed: 0.3,
                                temp_color: 'f5f2d9',
                                wind_gust: 0.4,
                                hour: '1',
                                humidity: 85,
                                temp: 14,
                                feels_like: 14,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 15,
                                _fallback_temp: false,
                                pressure_pa: 1011,
                                uv_index: 0,
                                hour_ts: 1502319600,
                                soil_moisture: 0.23,
                                prec_mm: 0,
                                wind_dir: 'ne',
                                feels_like_color: 'f5f2d9',
                                pressure_mm: 758,
                                condition: 'clear',
                                icon: 'skc_n',
                                wind_speed: 0.4,
                                temp_color: 'f5f2d9',
                                wind_gust: 2.1,
                                hour: '2',
                                humidity: 88,
                                temp: 13,
                                feels_like: 13,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 15,
                                _fallback_temp: false,
                                pressure_pa: 1011,
                                uv_index: 0,
                                hour_ts: 1502323200,
                                soil_moisture: 0.23,
                                prec_mm: 0,
                                wind_dir: 'e',
                                feels_like_color: 'f5f2d9',
                                pressure_mm: 758,
                                condition: 'clear',
                                icon: 'skc_n',
                                wind_speed: 0.4,
                                temp_color: 'f5f2d9',
                                wind_gust: 0.4,
                                hour: '3',
                                humidity: 90,
                                temp: 13,
                                feels_like: 13,
                                prec_period: 60
                            },
                            {
                                _fallback_prec: false,
                                prec_prob: 0,
                                soil_temp: 15,
                                _fallback_temp: false,
                                pressure_pa: 1011,
                                uv_index: 0,
                                hour_ts: 1502326800,
                                soil_moisture: 0.23,
                                prec_mm: 0,
                                wind_dir: 'se',
                                feels_like_color: 'f5f2d9',
                                pressure_mm: 758,
                                condition: 'clear',
                                icon: 'skc_n',
                                wind_speed: 0.5,
                                temp_color: 'f5f2d9',
                                wind_gust: 2.5,
                                hour: '4',
                                humidity: 94,
                                temp: 13,
                                feels_like: 13,
                                prec_period: 60
                            }
                        ],
                        date: '2017-08-09T21:00:00.000Z',
                        parts: [
                            {
                                temperature_to: 14,
                                wind_speed: 0.5,
                                temperature_from: 13,
                                temperature_min: 13,
                                temp_avg: 14,
                                weather_type: 'Ясно',
                                humidity: 88,
                                pressure: {
                                    content: 758,
                                    units: 'mm'
                                },
                                temperature_max: 14,
                                'temperature-data': {
                                    to: 14,
                                    from: 13
                                },
                                'image-v3': {
                                    content: 'skc_n'
                                },
                                type: 'night',
                                weather_condition: {
                                    code: 'clear'
                                }
                            },
                            {
                                temperature_to: 19,
                                wind_speed: 1.4,
                                temperature_from: 14,
                                temperature_min: 14,
                                temp_avg: 17,
                                weather_type: 'Облачно с прояснениями',
                                humidity: 58,
                                pressure: {
                                    content: 758,
                                    units: 'mm'
                                },
                                temperature_max: 19,
                                'temperature-data': {
                                    to: 19,
                                    from: 14
                                },
                                'image-v3': {
                                    content: 'bkn_d'
                                },
                                type: 'morning',
                                weather_condition: {
                                    code: 'cloudy'
                                }
                            },
                            {
                                temperature_to: 25,
                                wind_speed: 2,
                                temperature_from: 23,
                                temperature_min: 23,
                                temp_avg: 24,
                                weather_type: 'Ясно',
                                humidity: 39,
                                pressure: {
                                    content: 755,
                                    units: 'mm'
                                },
                                temperature_max: 25,
                                'temperature-data': {
                                    to: 25,
                                    from: 23
                                },
                                'image-v3': {
                                    content: 'skc_d'
                                },
                                type: 'day',
                                weather_condition: {
                                    code: 'clear'
                                }
                            },
                            {
                                temperature_to: 24,
                                wind_speed: 1.2,
                                temperature_from: 20,
                                temperature_min: 20,
                                temp_avg: 22,
                                weather_type: 'Ясно',
                                humidity: 65,
                                pressure: {
                                    content: 754,
                                    units: 'mm'
                                },
                                temperature_max: 24,
                                'temperature-data': {
                                    to: 24,
                                    from: 20
                                },
                                'image-v3': {
                                    content: 'skc_n'
                                },
                                type: 'evening',
                                weather_condition: {
                                    code: 'clear'
                                }
                            },
                            {
                                wind_speed: 1.5,
                                temperature: 16,
                                weather_type: 'Ясно',
                                image: {
                                    content: 'skc_n_+16'
                                },
                                humidity: 78,
                                pressure: {
                                    content: 752,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 16,
                                        bgcolor: 'f6f3d6'
                                    }
                                },
                                'image-v2': {
                                    content: 'skc_n_+16'
                                },
                                'image-v3': {
                                    content: 'skc_n'
                                },
                                type: 'night_short',
                                weather_condition: {
                                    code: 'clear'
                                }
                            },
                            {
                                wind_speed: 2,
                                temperature: 25,
                                temperature_from: 14,
                                temperature_min: 14,
                                image: {
                                    content: 'bkn_d_+26'
                                },
                                weather_type: 'Переменная облачность',
                                humidity: 48,
                                pressure: {
                                    content: 757,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 26,
                                        bgcolor: 'f9ebb9'
                                    },
                                    from: 14
                                },
                                'image-v2': {
                                    content: 'bkn_d_+26'
                                },
                                'image-v3': {
                                    content: 'bkn_d'
                                },
                                type: 'day_short',
                                weather_condition: {
                                    code: 'partly-cloudy'
                                }
                            }
                        ]
                    },
                    {
                        hours: [],
                        date: '2017-08-10T21:00:00.000Z',
                        parts: [
                            {
                                temperature_to: 18,
                                wind_speed: 1.5,
                                temperature_from: 16,
                                temperature_min: 16,
                                temp_avg: 17,
                                weather_type: 'Ясно',
                                humidity: 78,
                                pressure: {
                                    content: 752,
                                    units: 'mm'
                                },
                                temperature_max: 18,
                                'temperature-data': {
                                    to: 18,
                                    from: 16
                                },
                                'image-v3': {
                                    content: 'skc_n'
                                },
                                type: 'night',
                                weather_condition: {
                                    code: 'clear'
                                }
                            },
                            {
                                temperature_to: 21,
                                wind_speed: 2.8,
                                temperature_from: 16,
                                temperature_min: 16,
                                temp_avg: 19,
                                weather_type: 'Облачно с прояснениями',
                                humidity: 62,
                                pressure: {
                                    content: 750,
                                    units: 'mm'
                                },
                                temperature_max: 21,
                                'temperature-data': {
                                    to: 21,
                                    from: 16
                                },
                                'image-v3': {
                                    content: 'bkn_d'
                                },
                                type: 'morning',
                                weather_condition: {
                                    code: 'cloudy'
                                }
                            },
                            {
                                temperature_to: 26,
                                wind_speed: 3.5,
                                temperature_from: 23,
                                temperature_min: 23,
                                temp_avg: 25,
                                weather_type: 'Переменная облачность',
                                humidity: 60,
                                pressure: {
                                    content: 748,
                                    units: 'mm'
                                },
                                temperature_max: 26,
                                'temperature-data': {
                                    to: 26,
                                    from: 23
                                },
                                'image-v3': {
                                    content: 'bkn_d'
                                },
                                type: 'day',
                                weather_condition: {
                                    code: 'partly-cloudy'
                                }
                            },
                            {
                                temperature_to: 26,
                                wind_speed: 1.3,
                                temperature_from: 22,
                                temperature_min: 22,
                                temp_avg: 24,
                                weather_type: 'Ясно',
                                humidity: 81,
                                pressure: {
                                    content: 747,
                                    units: 'mm'
                                },
                                temperature_max: 26,
                                'temperature-data': {
                                    to: 26,
                                    from: 22
                                },
                                'image-v3': {
                                    content: 'skc_n'
                                },
                                type: 'evening',
                                weather_condition: {
                                    code: 'clear'
                                }
                            },
                            {
                                wind_speed: 1.1,
                                temperature: 19,
                                weather_type: 'Ясно',
                                image: {
                                    content: 'skc_n_+20'
                                },
                                humidity: 67,
                                pressure: {
                                    content: 747,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 20,
                                        bgcolor: 'f8f4d0'
                                    }
                                },
                                'image-v2': {
                                    content: 'skc_n_+20'
                                },
                                'image-v3': {
                                    content: 'skc_n'
                                },
                                type: 'night_short',
                                weather_condition: {
                                    code: 'clear'
                                }
                            },
                            {
                                wind_speed: 3.5,
                                temperature: 26,
                                temperature_from: 16,
                                temperature_min: 16,
                                image: {
                                    content: 'bkn_d_+26'
                                },
                                weather_type: 'Облачно с прояснениями',
                                humidity: 61,
                                pressure: {
                                    content: 749,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 26,
                                        bgcolor: 'f9ebb9'
                                    },
                                    from: 16
                                },
                                'image-v2': {
                                    content: 'bkn_d_+26'
                                },
                                'image-v3': {
                                    content: 'bkn_d'
                                },
                                type: 'day_short',
                                weather_condition: {
                                    code: 'cloudy'
                                }
                            }
                        ]
                    },
                    {
                        hours: [],
                        date: '2017-08-11T21:00:00.000Z',
                        parts: [
                            {
                                temperature_to: 21,
                                wind_speed: 1.1,
                                temperature_from: 19,
                                temperature_min: 19,
                                temp_avg: 20,
                                weather_type: 'Ясно',
                                humidity: 67,
                                pressure: {
                                    content: 747,
                                    units: 'mm'
                                },
                                temperature_max: 21,
                                'temperature-data': {
                                    to: 21,
                                    from: 19
                                },
                                'image-v3': {
                                    content: 'skc_n'
                                },
                                type: 'night',
                                weather_condition: {
                                    code: 'clear'
                                }
                            },
                            {
                                temperature_to: 21,
                                wind_speed: 1.4,
                                temperature_from: 17,
                                temperature_min: 17,
                                temp_avg: 19,
                                weather_type: 'Ясно',
                                humidity: 52,
                                pressure: {
                                    content: 748,
                                    units: 'mm'
                                },
                                temperature_max: 21,
                                'temperature-data': {
                                    to: 21,
                                    from: 17
                                },
                                'image-v3': {
                                    content: 'skc_d'
                                },
                                type: 'morning',
                                weather_condition: {
                                    code: 'clear'
                                }
                            },
                            {
                                temperature_to: 26,
                                wind_speed: 2.6,
                                temperature_from: 24,
                                temperature_min: 24,
                                temp_avg: 25,
                                weather_type: 'Ясно',
                                humidity: 32,
                                pressure: {
                                    content: 748,
                                    units: 'mm'
                                },
                                temperature_max: 26,
                                'temperature-data': {
                                    to: 26,
                                    from: 24
                                },
                                'image-v3': {
                                    content: 'skc_d'
                                },
                                type: 'day',
                                weather_condition: {
                                    code: 'clear'
                                }
                            },
                            {
                                temperature_to: 25,
                                wind_speed: 0.8,
                                temperature_from: 21,
                                temperature_min: 21,
                                temp_avg: 23,
                                weather_type: 'Ясно',
                                humidity: 52,
                                pressure: {
                                    content: 748,
                                    units: 'mm'
                                },
                                temperature_max: 25,
                                'temperature-data': {
                                    to: 25,
                                    from: 21
                                },
                                'image-v3': {
                                    content: 'skc_n'
                                },
                                type: 'evening',
                                weather_condition: {
                                    code: 'clear'
                                }
                            },
                            {
                                wind_speed: 0.6,
                                temperature: 15,
                                weather_type: 'Ясно',
                                image: {
                                    content: 'skc_n_+16'
                                },
                                humidity: 77,
                                pressure: {
                                    content: 748,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 16,
                                        bgcolor: 'f6f3d6'
                                    }
                                },
                                'image-v2': {
                                    content: 'skc_n_+16'
                                },
                                'image-v3': {
                                    content: 'skc_n'
                                },
                                type: 'night_short',
                                weather_condition: {
                                    code: 'clear'
                                }
                            },
                            {
                                wind_speed: 2.6,
                                temperature: 26,
                                temperature_from: 17,
                                temperature_min: 17,
                                image: {
                                    content: 'skc_d_+26'
                                },
                                weather_type: 'Ясно',
                                humidity: 42,
                                pressure: {
                                    content: 748,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 26,
                                        bgcolor: 'f9ebb9'
                                    },
                                    from: 17
                                },
                                'image-v2': {
                                    content: 'skc_d_+26'
                                },
                                'image-v3': {
                                    content: 'skc_d'
                                },
                                type: 'day_short',
                                weather_condition: {
                                    code: 'clear'
                                }
                            }
                        ]
                    },
                    {
                        hours: [],
                        date: '2017-08-12T21:00:00.000Z',
                        parts: [
                            {
                                temperature_to: 17,
                                wind_speed: 0.6,
                                temperature_from: 15,
                                temperature_min: 15,
                                temp_avg: 16,
                                weather_type: 'Ясно',
                                humidity: 77,
                                pressure: {
                                    content: 748,
                                    units: 'mm'
                                },
                                temperature_max: 17,
                                'temperature-data': {
                                    to: 17,
                                    from: 15
                                },
                                'image-v3': {
                                    content: 'skc_n'
                                },
                                type: 'night',
                                weather_condition: {
                                    code: 'clear'
                                }
                            },
                            {
                                temperature_to: 21,
                                wind_speed: 1.6,
                                temperature_from: 17,
                                temperature_min: 17,
                                temp_avg: 19,
                                weather_type: 'Ясно',
                                humidity: 49,
                                pressure: {
                                    content: 749,
                                    units: 'mm'
                                },
                                temperature_max: 21,
                                'temperature-data': {
                                    to: 21,
                                    from: 17
                                },
                                'image-v3': {
                                    content: 'skc_d'
                                },
                                type: 'morning',
                                weather_condition: {
                                    code: 'clear'
                                }
                            },
                            {
                                temperature_to: 28,
                                wind_speed: 2.3,
                                temperature_from: 26,
                                temperature_min: 26,
                                temp_avg: 27,
                                weather_type: 'Облачно',
                                humidity: 27,
                                pressure: {
                                    content: 748,
                                    units: 'mm'
                                },
                                temperature_max: 28,
                                'temperature-data': {
                                    to: 28,
                                    from: 26
                                },
                                'image-v3': {
                                    content: 'ovc'
                                },
                                type: 'day',
                                weather_condition: {
                                    code: 'overcast'
                                }
                            },
                            {
                                temperature_to: 28,
                                wind_speed: 1.3,
                                temperature_from: 24,
                                temperature_min: 24,
                                temp_avg: 26,
                                weather_type: 'Облачно с прояснениями',
                                humidity: 52,
                                pressure: {
                                    content: 747,
                                    units: 'mm'
                                },
                                temperature_max: 28,
                                'temperature-data': {
                                    to: 28,
                                    from: 24
                                },
                                'image-v3': {
                                    content: 'bkn_n'
                                },
                                type: 'evening',
                                weather_condition: {
                                    code: 'cloudy'
                                }
                            },
                            {
                                wind_speed: 1.1,
                                temperature: 18,
                                weather_type: 'Облачно',
                                image: {
                                    content: 'ovc_+18'
                                },
                                humidity: 69,
                                pressure: {
                                    content: 747,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 18,
                                        bgcolor: 'f7f3d3'
                                    }
                                },
                                'image-v2': {
                                    content: 'ovc_+18'
                                },
                                'image-v3': {
                                    content: 'ovc'
                                },
                                type: 'night_short',
                                weather_condition: {
                                    code: 'overcast'
                                }
                            },
                            {
                                wind_speed: 2.3,
                                temperature: 28,
                                temperature_from: 17,
                                temperature_min: 17,
                                image: {
                                    content: 'bkn_d_+28'
                                },
                                weather_type: 'Облачно с прояснениями',
                                humidity: 38,
                                pressure: {
                                    content: 749,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 28,
                                        bgcolor: 'f9e8b1'
                                    },
                                    from: 17
                                },
                                'image-v2': {
                                    content: 'bkn_d_+28'
                                },
                                'image-v3': {
                                    content: 'bkn_d'
                                },
                                type: 'day_short',
                                weather_condition: {
                                    code: 'cloudy'
                                }
                            }
                        ]
                    },
                    {
                        hours: [],
                        date: '2017-08-13T21:00:00.000Z',
                        parts: [
                            {
                                temperature_to: 20,
                                wind_speed: 1.1,
                                temperature_from: 18,
                                temperature_min: 18,
                                temp_avg: 19,
                                weather_type: 'Облачно',
                                humidity: 69,
                                pressure: {
                                    content: 747,
                                    units: 'mm'
                                },
                                temperature_max: 20,
                                'temperature-data': {
                                    to: 20,
                                    from: 18
                                },
                                'image-v3': {
                                    content: 'ovc'
                                },
                                type: 'night',
                                weather_condition: {
                                    code: 'overcast'
                                }
                            },
                            {
                                temperature_to: 22,
                                wind_speed: 2.2,
                                temperature_from: 17,
                                temperature_min: 17,
                                temp_avg: 20,
                                weather_type: 'Переменная облачность, небольшой дождь',
                                humidity: 55,
                                pressure: {
                                    content: 747,
                                    units: 'mm'
                                },
                                temperature_max: 22,
                                'temperature-data': {
                                    to: 22,
                                    from: 17
                                },
                                'image-v3': {
                                    content: 'bkn_-ra_d'
                                },
                                type: 'morning',
                                weather_condition: {
                                    code: 'partly-cloudy-and-light-rain'
                                }
                            },
                            {
                                temperature_to: 25,
                                wind_speed: 2.7,
                                temperature_from: 23,
                                temperature_min: 23,
                                temp_avg: 24,
                                weather_type: 'Облачно с прояснениями',
                                humidity: 50,
                                pressure: {
                                    content: 742,
                                    units: 'mm'
                                },
                                temperature_max: 25,
                                'temperature-data': {
                                    to: 25,
                                    from: 23
                                },
                                'image-v3': {
                                    content: 'bkn_d'
                                },
                                type: 'day',
                                weather_condition: {
                                    code: 'cloudy'
                                }
                            },
                            {
                                temperature_to: 24,
                                wind_speed: 2.5,
                                temperature_from: 20,
                                temperature_min: 20,
                                temp_avg: 22,
                                weather_type: 'Переменная облачность, небольшой дождь',
                                humidity: 72,
                                pressure: {
                                    content: 743,
                                    units: 'mm'
                                },
                                temperature_max: 24,
                                'temperature-data': {
                                    to: 24,
                                    from: 20
                                },
                                'image-v3': {
                                    content: 'bkn_-ra_n'
                                },
                                type: 'evening',
                                weather_condition: {
                                    code: 'partly-cloudy-and-light-rain'
                                }
                            },
                            {
                                wind_speed: 2.9,
                                temperature: 16,
                                weather_type: 'Ясно',
                                image: {
                                    content: 'skc_n_+16'
                                },
                                humidity: 62,
                                pressure: {
                                    content: 746,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 16,
                                        bgcolor: 'f6f3d6'
                                    }
                                },
                                'image-v2': {
                                    content: 'skc_n_+16'
                                },
                                'image-v3': {
                                    content: 'skc_n'
                                },
                                type: 'night_short',
                                weather_condition: {
                                    code: 'clear'
                                }
                            },
                            {
                                wind_speed: 2.7,
                                temperature: 25,
                                temperature_from: 17,
                                temperature_min: 17,
                                image: {
                                    content: 'bkn_-ra_d_+26'
                                },
                                weather_type: 'Переменная облачность, небольшой дождь',
                                humidity: 53,
                                pressure: {
                                    content: 745,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 26,
                                        bgcolor: 'f9ebb9'
                                    },
                                    from: 17
                                },
                                'image-v2': {
                                    content: 'bkn_-ra_d_+26'
                                },
                                'image-v3': {
                                    content: 'bkn_-ra_d'
                                },
                                type: 'day_short',
                                weather_condition: {
                                    code: 'partly-cloudy-and-light-rain'
                                }
                            }
                        ]
                    },
                    {
                        hours: [],
                        date: '2017-08-14T21:00:00.000Z',
                        parts: [
                            {
                                temperature_to: 18,
                                wind_speed: 2.9,
                                temperature_from: 16,
                                temperature_min: 16,
                                temp_avg: 17,
                                weather_type: 'Ясно',
                                humidity: 62,
                                pressure: {
                                    content: 746,
                                    units: 'mm'
                                },
                                temperature_max: 18,
                                'temperature-data': {
                                    to: 18,
                                    from: 16
                                },
                                'image-v3': {
                                    content: 'skc_n'
                                },
                                type: 'night',
                                weather_condition: {
                                    code: 'clear'
                                }
                            },
                            {
                                temperature_to: 18,
                                wind_speed: 2.7,
                                temperature_from: 14,
                                temperature_min: 14,
                                temp_avg: 16,
                                weather_type: 'Облачно',
                                humidity: 51,
                                pressure: {
                                    content: 749,
                                    units: 'mm'
                                },
                                temperature_max: 18,
                                'temperature-data': {
                                    to: 18,
                                    from: 14
                                },
                                'image-v3': {
                                    content: 'ovc'
                                },
                                type: 'morning',
                                weather_condition: {
                                    code: 'overcast'
                                }
                            },
                            {
                                temperature_to: 21,
                                wind_speed: 3.8,
                                temperature_from: 19,
                                temperature_min: 19,
                                temp_avg: 20,
                                weather_type: 'Облачно, небольшой дождь',
                                humidity: 42,
                                pressure: {
                                    content: 751,
                                    units: 'mm'
                                },
                                temperature_max: 21,
                                'temperature-data': {
                                    to: 21,
                                    from: 19
                                },
                                'image-v3': {
                                    content: 'bkn_-ra_d'
                                },
                                type: 'day',
                                weather_condition: {
                                    code: 'overcast-and-light-rain'
                                }
                            },
                            {
                                temperature_to: 22,
                                wind_speed: 1.7,
                                temperature_from: 17,
                                temperature_min: 17,
                                temp_avg: 20,
                                weather_type: 'Ясно',
                                humidity: 56,
                                pressure: {
                                    content: 752,
                                    units: 'mm'
                                },
                                temperature_max: 22,
                                'temperature-data': {
                                    to: 22,
                                    from: 17
                                },
                                'image-v3': {
                                    content: 'skc_n'
                                },
                                type: 'evening',
                                weather_condition: {
                                    code: 'clear'
                                }
                            },
                            {
                                wind_speed: 1.4,
                                temperature: 13,
                                weather_type: 'Облачно с прояснениями',
                                image: {
                                    content: 'bkn_n_+14'
                                },
                                humidity: 66,
                                pressure: {
                                    content: 753,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 14,
                                        bgcolor: 'f5f2d9'
                                    }
                                },
                                'image-v2': {
                                    content: 'bkn_n_+14'
                                },
                                'image-v3': {
                                    content: 'bkn_n'
                                },
                                type: 'night_short',
                                weather_condition: {
                                    code: 'cloudy'
                                }
                            },
                            {
                                wind_speed: 3.8,
                                temperature: 21,
                                temperature_from: 14,
                                temperature_min: 14,
                                image: {
                                    content: 'bkn_-ra_d_+22'
                                },
                                weather_type: 'Облачно, небольшой дождь',
                                humidity: 47,
                                pressure: {
                                    content: 750,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 22,
                                        bgcolor: 'f8f1c8'
                                    },
                                    from: 14
                                },
                                'image-v2': {
                                    content: 'bkn_-ra_d_+22'
                                },
                                'image-v3': {
                                    content: 'bkn_-ra_d'
                                },
                                type: 'day_short',
                                weather_condition: {
                                    code: 'overcast-and-light-rain'
                                }
                            }
                        ]
                    },
                    {
                        hours: [],
                        date: '2017-08-15T21:00:00.000Z',
                        parts: [
                            {
                                temperature_to: 16,
                                wind_speed: 1.4,
                                temperature_from: 13,
                                temperature_min: 13,
                                temp_avg: 15,
                                weather_type: 'Облачно с прояснениями',
                                humidity: 66,
                                pressure: {
                                    content: 753,
                                    units: 'mm'
                                },
                                temperature_max: 16,
                                'temperature-data': {
                                    to: 16,
                                    from: 13
                                },
                                'image-v3': {
                                    content: 'bkn_n'
                                },
                                type: 'night',
                                weather_condition: {
                                    code: 'cloudy'
                                }
                            },
                            {
                                temperature_to: 18,
                                wind_speed: 1.6,
                                temperature_from: 13,
                                temperature_min: 13,
                                temp_avg: 16,
                                weather_type: 'Ясно',
                                humidity: 48,
                                pressure: {
                                    content: 754,
                                    units: 'mm'
                                },
                                temperature_max: 18,
                                'temperature-data': {
                                    to: 18,
                                    from: 13
                                },
                                'image-v3': {
                                    content: 'skc_d'
                                },
                                type: 'morning',
                                weather_condition: {
                                    code: 'clear'
                                }
                            },
                            {
                                temperature_to: 23,
                                wind_speed: 3.4,
                                temperature_from: 21,
                                temperature_min: 21,
                                temp_avg: 22,
                                weather_type: 'Ясно',
                                humidity: 39,
                                pressure: {
                                    content: 753,
                                    units: 'mm'
                                },
                                temperature_max: 23,
                                'temperature-data': {
                                    to: 23,
                                    from: 21
                                },
                                'image-v3': {
                                    content: 'skc_d'
                                },
                                type: 'day',
                                weather_condition: {
                                    code: 'clear'
                                }
                            },
                            {
                                temperature_to: 23,
                                wind_speed: 1.7,
                                temperature_from: 18,
                                temperature_min: 18,
                                temp_avg: 21,
                                weather_type: 'Переменная облачность',
                                humidity: 61,
                                pressure: {
                                    content: 751,
                                    units: 'mm'
                                },
                                temperature_max: 23,
                                'temperature-data': {
                                    to: 23,
                                    from: 18
                                },
                                'image-v3': {
                                    content: 'bkn_n'
                                },
                                type: 'evening',
                                weather_condition: {
                                    code: 'partly-cloudy'
                                }
                            },
                            {
                                wind_speed: 2.5,
                                temperature: 14,
                                weather_type: 'Облачно с прояснениями',
                                image: {
                                    content: 'bkn_n_+14'
                                },
                                humidity: 59,
                                pressure: {
                                    content: 749,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 14,
                                        bgcolor: 'f5f2d9'
                                    }
                                },
                                'image-v2': {
                                    content: 'bkn_n_+14'
                                },
                                'image-v3': {
                                    content: 'bkn_n'
                                },
                                type: 'night_short',
                                weather_condition: {
                                    code: 'cloudy'
                                }
                            },
                            {
                                wind_speed: 3.4,
                                temperature: 23,
                                temperature_from: 13,
                                temperature_min: 13,
                                image: {
                                    content: 'skc_d_+24'
                                },
                                weather_type: 'Ясно',
                                humidity: 43,
                                pressure: {
                                    content: 754,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 24,
                                        bgcolor: 'f9eec0'
                                    },
                                    from: 13
                                },
                                'image-v2': {
                                    content: 'skc_d_+24'
                                },
                                'image-v3': {
                                    content: 'skc_d'
                                },
                                type: 'day_short',
                                weather_condition: {
                                    code: 'clear'
                                }
                            }
                        ]
                    }
                ],
                current: {
                    wind_speed: 3,
                    uptime: '2017-08-08T14:00:00',
                    temperature: 23,
                    weather_type: 'Переменная облачность',
                    wind_direction: 'n',
                    humidity: 45,
                    pressure: {
                        content: 753,
                        units: 'mm'
                    },
                    'temperature-data': {
                        avg: {
                            content: 24,
                            bgcolor: 'f9eec0'
                        }
                    },
                    'image-v2': {
                        content: 'bkn_d_+24'
                    },
                    'image-v3': {
                        content: 'bkn_d'
                    },
                    image: {
                        content: 'bkn_d_+24'
                    },
                    weather_condition: {
                        code: 'partly-cloudy'
                    }
                },
                city_id: 213,
                weather_link: 'https://yandex.ru/pogoda/moscow',
                link: 'https://yandex.ru/pogoda/moscow',
                template: 'weather',
                get_wind_type: null,
                current_hour: 17,
                counter_prefix: '/snippet/weather/',
                voiceInfo: {
                    ru: [
                        {
                            lang: 'ru-RU',
                            text: 'Сегодня в Москве 23°, Ясно. '
                        }
                    ]
                },
                serp_info: {
                    slot: 'full',
                    flat: true,
                    type: 'weather',
                    format: 'json'
                },
                types: {
                    kind: 'wizard',
                    main: 'weather',
                    all: ['snippets', 'weather']
                }
            }
        },
        size: 0
    }
};
