var stubs = require('@yandex-int/gemini-serp-stubs');

module.exports = {
    type: 'snippet',
    request_text: 'погода',
    data_stub: {
        num: 0,
        snippets: {
            full: {
                slot: 'full',
                city: stubs.moscowStub(),
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
                                temp: -1,
                                _fallback_prec: false,
                                temp_color: 'f0eff0',
                                wind_gust: 2.1,
                                hour: '0',
                                _fallback_temp: false,
                                hour_ts: 1488229200,
                                condition: 'cloudy',
                                pressure_pa: 994,
                                humidity: 89,
                                pressure_mm: 745,
                                wind_speed: 1.2,
                                prec_period: 60,
                                feels_like_color: 'eaedef',
                                prec_mm: 0,
                                feels_like: -4,
                                wind_dir: 'n',
                                icon: 'bkn_n'
                            },
                            {
                                temp: -1,
                                _fallback_prec: false,
                                temp_color: 'f0eff0',
                                wind_gust: 2.1,
                                hour: '1',
                                _fallback_temp: false,
                                hour_ts: 1488232800,
                                condition: 'overcast',
                                pressure_pa: 995,
                                humidity: 90,
                                pressure_mm: 746,
                                wind_speed: 1.2,
                                prec_period: 60,
                                feels_like_color: 'eaedef',
                                prec_mm: 0,
                                feels_like: -4,
                                wind_dir: 'ne',
                                icon: 'ovc'
                            },
                            {
                                temp: -1,
                                _fallback_prec: false,
                                temp_color: 'f0eff0',
                                wind_gust: 1.3,
                                hour: '2',
                                _fallback_temp: false,
                                hour_ts: 1488236400,
                                condition: 'overcast',
                                pressure_pa: 995,
                                humidity: 86,
                                pressure_mm: 746,
                                wind_speed: 1.2,
                                prec_period: 60,
                                feels_like_color: 'eaedef',
                                prec_mm: 0,
                                feels_like: -5,
                                wind_dir: 'ne',
                                icon: 'ovc'
                            },
                            {
                                temp: -2,
                                _fallback_prec: false,
                                temp_color: 'f0eff0',
                                wind_gust: 0.1,
                                hour: '3',
                                _fallback_temp: false,
                                hour_ts: 1488240000,
                                condition: 'clear',
                                pressure_pa: 995,
                                humidity: 91,
                                pressure_mm: 746,
                                wind_speed: 0,
                                prec_period: 60,
                                feels_like_color: 'eaedef',
                                prec_mm: 0,
                                feels_like: -5,
                                wind_dir: 's',
                                icon: 'skc_n'
                            },
                            {
                                temp: -2,
                                _fallback_prec: false,
                                temp_color: 'f0eff0',
                                wind_gust: 2.2,
                                hour: '4',
                                _fallback_temp: false,
                                hour_ts: 1488243600,
                                condition: 'cloudy',
                                pressure_pa: 996,
                                humidity: 87,
                                pressure_mm: 747,
                                wind_speed: 0.3,
                                prec_period: 60,
                                feels_like_color: 'eaedef',
                                prec_mm: 0,
                                feels_like: -5,
                                wind_dir: 'e',
                                icon: 'bkn_n'
                            },
                            {
                                temp: -2,
                                _fallback_prec: false,
                                temp_color: 'f0eff0',
                                wind_gust: 1.7,
                                hour: '5',
                                _fallback_temp: false,
                                hour_ts: 1488247200,
                                condition: 'cloudy',
                                pressure_pa: 996,
                                humidity: 87,
                                pressure_mm: 747,
                                wind_speed: 1.1,
                                prec_period: 60,
                                feels_like_color: 'eaedef',
                                prec_mm: 0,
                                feels_like: -5,
                                wind_dir: 'se',
                                icon: 'bkn_n'
                            },
                            {
                                temp: -3,
                                _fallback_prec: false,
                                temp_color: 'f0eff0',
                                wind_gust: 2.2,
                                hour: '6',
                                _fallback_temp: false,
                                hour_ts: 1488250800,
                                condition: 'cloudy',
                                pressure_pa: 996,
                                humidity: 81,
                                pressure_mm: 747,
                                wind_speed: 0.9,
                                prec_period: 60,
                                feels_like_color: 'eaedef',
                                prec_mm: 0,
                                feels_like: -6,
                                wind_dir: 's',
                                icon: 'bkn_n'
                            },
                            {
                                temp: -3,
                                _fallback_prec: false,
                                temp_color: 'f0eff0',
                                wind_gust: 3.9,
                                hour: '7',
                                _fallback_temp: false,
                                hour_ts: 1488254400,
                                condition: 'cloudy',
                                pressure_pa: 998,
                                humidity: 100,
                                pressure_mm: 748,
                                wind_speed: 1.7,
                                prec_period: 60,
                                feels_like_color: 'eaedef',
                                prec_mm: 0,
                                feels_like: -6,
                                wind_dir: 'sw',
                                icon: 'bkn_n'
                            },
                            {
                                temp: -3,
                                _fallback_prec: false,
                                temp_color: 'f0eff0',
                                wind_gust: 3,
                                hour: '8',
                                _fallback_temp: false,
                                hour_ts: 1488258000,
                                condition: 'overcast',
                                pressure_pa: 998,
                                humidity: 100,
                                pressure_mm: 748,
                                wind_speed: 1.7,
                                prec_period: 60,
                                feels_like_color: 'eaedef',
                                prec_mm: 0,
                                feels_like: -7,
                                wind_dir: 'sw',
                                icon: 'ovc'
                            },
                            {
                                temp: -3,
                                _fallback_prec: false,
                                temp_color: 'f0eff0',
                                wind_gust: 0.1,
                                hour: '9',
                                _fallback_temp: false,
                                hour_ts: 1488261600,
                                condition: 'clear',
                                pressure_pa: 999,
                                humidity: 94,
                                pressure_mm: 749,
                                wind_speed: 0,
                                prec_period: 60,
                                feels_like_color: 'eaedef',
                                prec_mm: 0,
                                feels_like: -5,
                                wind_dir: 's',
                                icon: 'skc_d'
                            },
                            {
                                temp: 1,
                                _fallback_prec: false,
                                temp_color: 'f0f0ec',
                                wind_gust: 3.5,
                                hour: '10',
                                _fallback_temp: false,
                                hour_ts: 1488265200,
                                condition: 'overcast',
                                pressure_pa: 998,
                                humidity: 73,
                                pressure_mm: 748,
                                wind_speed: 1.3,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -2,
                                wind_dir: 's',
                                icon: 'ovc'
                            },
                            {
                                temp: 2,
                                _fallback_prec: false,
                                temp_color: 'f0f0ec',
                                wind_gust: 4.2,
                                hour: '11',
                                _fallback_temp: false,
                                hour_ts: 1488268800,
                                condition: 'overcast',
                                pressure_pa: 999,
                                humidity: 67,
                                pressure_mm: 749,
                                wind_speed: 2.1,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -2,
                                wind_dir: 'sw',
                                icon: 'ovc'
                            },
                            {
                                temp: 3,
                                _fallback_prec: false,
                                temp_color: 'f1f0e9',
                                wind_gust: 5.4,
                                hour: '12',
                                _fallback_temp: false,
                                hour_ts: 1488272400,
                                condition: 'overcast',
                                pressure_pa: 999,
                                humidity: 63,
                                pressure_mm: 749,
                                wind_speed: 2.8,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -1,
                                wind_dir: 'sw',
                                icon: 'ovc'
                            },
                            {
                                temp: 3,
                                _fallback_prec: false,
                                temp_color: 'f1f0e9',
                                wind_gust: 6.2,
                                hour: '13',
                                _fallback_temp: false,
                                hour_ts: 1488276000,
                                condition: 'overcast',
                                pressure_pa: 999,
                                humidity: 63,
                                pressure_mm: 749,
                                wind_speed: 3,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -1,
                                wind_dir: 'sw',
                                icon: 'ovc'
                            },
                            {
                                temp: 4,
                                _fallback_prec: false,
                                temp_color: 'f1f0e9',
                                wind_gust: 6.2,
                                hour: '14',
                                _fallback_temp: false,
                                hour_ts: 1488279600,
                                condition: 'overcast',
                                pressure_pa: 999,
                                humidity: 64,
                                pressure_mm: 749,
                                wind_speed: 3.1,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -1,
                                wind_dir: 'sw',
                                icon: 'ovc'
                            },
                            {
                                temp: 4,
                                _fallback_prec: false,
                                temp_color: 'f1f0e9',
                                wind_gust: 6.8,
                                hour: '15',
                                _fallback_temp: false,
                                hour_ts: 1488283200,
                                condition: 'overcast',
                                pressure_pa: 999,
                                humidity: 66,
                                pressure_mm: 749,
                                wind_speed: 3.1,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -1,
                                wind_dir: 's',
                                icon: 'ovc'
                            },
                            {
                                temp: 4,
                                _fallback_prec: false,
                                temp_color: 'f1f0e9',
                                wind_gust: 7.4,
                                hour: '16',
                                _fallback_temp: false,
                                hour_ts: 1488286800,
                                condition: 'overcast',
                                pressure_pa: 998,
                                humidity: 69,
                                pressure_mm: 748,
                                _nowcast: true,
                                wind_speed: 3,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -1,
                                wind_dir: 's',
                                icon: 'ovc'
                            },
                            {
                                temp: 3,
                                _fallback_prec: false,
                                temp_color: 'f1f0e9',
                                wind_gust: 7.3,
                                hour: '17',
                                _fallback_temp: false,
                                hour_ts: 1488290400,
                                condition: 'overcast-and-wet-snow',
                                pressure_pa: 998,
                                humidity: 75,
                                pressure_mm: 748,
                                _nowcast: true,
                                wind_speed: 2.7,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -1,
                                wind_dir: 's',
                                icon: 'ovc_ra_sn'
                            },
                            {
                                temp: 3,
                                _fallback_prec: false,
                                temp_color: 'f1f0e9',
                                wind_gust: 7.9,
                                hour: '18',
                                _fallback_temp: false,
                                hour_ts: 1488294000,
                                condition: 'overcast',
                                pressure_pa: 998,
                                humidity: 81,
                                pressure_mm: 748,
                                _nowcast: true,
                                wind_speed: 2.4,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -1,
                                wind_dir: 's',
                                icon: 'ovc'
                            },
                            {
                                temp: 3,
                                _fallback_prec: false,
                                temp_color: 'f1f0e9',
                                wind_gust: 8.7,
                                hour: '19',
                                _fallback_temp: false,
                                hour_ts: 1488297600,
                                condition: 'overcast',
                                pressure_pa: 998,
                                humidity: 83,
                                pressure_mm: 748,
                                wind_speed: 2.2,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -1,
                                wind_dir: 's',
                                icon: 'ovc'
                            },
                            {
                                temp: 3,
                                _fallback_prec: false,
                                temp_color: 'f1f0e9',
                                wind_gust: 6.4,
                                hour: '20',
                                _fallback_temp: false,
                                hour_ts: 1488301200,
                                condition: 'overcast',
                                pressure_pa: 996,
                                humidity: 84,
                                pressure_mm: 747,
                                wind_speed: 2.3,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -1,
                                wind_dir: 's',
                                icon: 'ovc'
                            },
                            {
                                temp: 3,
                                _fallback_prec: false,
                                temp_color: 'f1f0e9',
                                wind_gust: 6.8,
                                hour: '21',
                                _fallback_temp: false,
                                hour_ts: 1488304800,
                                condition: 'overcast',
                                pressure_pa: 996,
                                humidity: 85,
                                pressure_mm: 747,
                                wind_speed: 2.3,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -1,
                                wind_dir: 's',
                                icon: 'ovc'
                            },
                            {
                                temp: 3,
                                _fallback_prec: false,
                                temp_color: 'f1f0e9',
                                wind_gust: 6.4,
                                hour: '22',
                                _fallback_temp: false,
                                hour_ts: 1488308400,
                                condition: 'overcast',
                                pressure_pa: 996,
                                humidity: 85,
                                pressure_mm: 747,
                                wind_speed: 2.3,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -1,
                                wind_dir: 's',
                                icon: 'ovc'
                            },
                            {
                                temp: 3,
                                _fallback_prec: false,
                                temp_color: 'f1f0e9',
                                wind_gust: 6.3,
                                hour: '23',
                                _fallback_temp: false,
                                hour_ts: 1488312000,
                                condition: 'overcast',
                                pressure_pa: 996,
                                humidity: 88,
                                pressure_mm: 747,
                                wind_speed: 2.2,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -1,
                                wind_dir: 's',
                                icon: 'ovc'
                            }
                        ],
                        date: '2017-02-27T21:00:00.000Z',
                        parts: [
                            {
                                temperature_to: -1,
                                wind_speed: 1.2,
                                temperature_from: -2,
                                temperature_min: -2,
                                temp_avg: -1,
                                weather_type: 'Облачно с прояснениями',
                                humidity: 88,
                                pressure: {
                                    content: 746,
                                    units: 'mm'
                                },
                                temperature_max: -1,
                                'temperature-data': {
                                    to: -1,
                                    from: -2
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
                                temperature_to: 2,
                                wind_speed: 2.1,
                                temperature_from: -3,
                                temperature_min: -3,
                                temp_avg: 0,
                                weather_type: 'Облачно с прояснениями',
                                humidity: 86,
                                pressure: {
                                    content: 748,
                                    units: 'mm'
                                },
                                temperature_max: 2,
                                'temperature-data': {
                                    to: 2,
                                    from: -3
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
                                temperature_to: 4,
                                wind_speed: 3.1,
                                temperature_from: 3,
                                temperature_min: 3,
                                temp_avg: 4,
                                weather_type: 'Облачно, дождь со снегом',
                                humidity: 67,
                                pressure: {
                                    content: 749,
                                    units: 'mm'
                                },
                                temperature_max: 4,
                                'temperature-data': {
                                    to: 4,
                                    from: 3
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                type: 'day',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                }
                            },
                            {
                                temperature_to: 3,
                                wind_speed: 2.4,
                                temperature_from: 3,
                                temperature_min: 3,
                                temp_avg: 3,
                                weather_type: 'Облачно',
                                humidity: 84,
                                pressure: {
                                    content: 747,
                                    units: 'mm'
                                },
                                temperature_max: 3,
                                'temperature-data': {
                                    to: 3,
                                    from: 3
                                },
                                'image-v3': {
                                    content: 'ovc'
                                },
                                type: 'evening',
                                weather_condition: {
                                    code: 'overcast'
                                }
                            },
                            {
                                wind_speed: 2.6,
                                temperature: 2,
                                weather_type: 'Облачно',
                                humidity: 88,
                                pressure: {
                                    content: 746,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 2,
                                        bgcolor: 'f0f0ec'
                                    }
                                },
                                'image-v2': {
                                    content: 'ovc_+2'
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
                                wind_speed: 3.1,
                                temperature: 4,
                                weather_type: 'Облачно, дождь со снегом',
                                humidity: 75,
                                pressure: {
                                    content: 749,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 4,
                                        bgcolor: 'f1f0e9'
                                    }
                                },
                                'image-v2': {
                                    content: 'ovc_ra_+4'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                type: 'day_short',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                }
                            }
                        ]
                    },
                    {
                        hours: [
                            {
                                temp: 3,
                                _fallback_prec: false,
                                temp_color: 'f1f0e9',
                                wind_gust: 7.2,
                                hour: '0',
                                _fallback_temp: false,
                                hour_ts: 1488315600,
                                condition: 'overcast',
                                pressure_pa: 996,
                                humidity: 88,
                                pressure_mm: 747,
                                wind_speed: 2.2,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -1,
                                wind_dir: 's',
                                icon: 'ovc'
                            },
                            {
                                temp: 2,
                                _fallback_prec: false,
                                temp_color: 'f0f0ec',
                                wind_gust: 7.3,
                                hour: '1',
                                _fallback_temp: false,
                                hour_ts: 1488319200,
                                condition: 'overcast',
                                pressure_pa: 995,
                                humidity: 90,
                                pressure_mm: 746,
                                wind_speed: 2.3,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -2,
                                wind_dir: 's',
                                icon: 'ovc'
                            },
                            {
                                temp: 2,
                                _fallback_prec: false,
                                temp_color: 'f0f0ec',
                                wind_gust: 6.7,
                                hour: '2',
                                _fallback_temp: false,
                                hour_ts: 1488322800,
                                condition: 'overcast',
                                pressure_pa: 995,
                                humidity: 90,
                                pressure_mm: 746,
                                wind_speed: 2.3,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -2,
                                wind_dir: 's',
                                icon: 'ovc'
                            },
                            {
                                temp: 2,
                                _fallback_prec: false,
                                temp_color: 'f0f0ec',
                                wind_gust: 7.5,
                                hour: '3',
                                _fallback_temp: false,
                                hour_ts: 1488326400,
                                condition: 'overcast',
                                pressure_pa: 995,
                                humidity: 87,
                                pressure_mm: 746,
                                wind_speed: 2.3,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -2,
                                wind_dir: 's',
                                icon: 'ovc'
                            },
                            {
                                temp: 2,
                                _fallback_prec: false,
                                temp_color: 'f0f0ec',
                                wind_gust: 8,
                                hour: '4',
                                _fallback_temp: false,
                                hour_ts: 1488330000,
                                condition: 'overcast',
                                pressure_pa: 995,
                                humidity: 85,
                                pressure_mm: 746,
                                wind_speed: 2.5,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -2,
                                wind_dir: 's',
                                icon: 'ovc'
                            },
                            {
                                temp: 2,
                                _fallback_prec: false,
                                temp_color: 'f0f0ec',
                                wind_gust: 8.3,
                                hour: '5',
                                _fallback_temp: false,
                                hour_ts: 1488333600,
                                condition: 'overcast',
                                pressure_pa: 994,
                                humidity: 85,
                                pressure_mm: 745,
                                wind_speed: 2.6,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -2,
                                wind_dir: 's',
                                icon: 'ovc'
                            },
                            {
                                temp: 2,
                                _fallback_prec: false,
                                temp_color: 'f0f0ec',
                                wind_gust: 9.8,
                                hour: '6',
                                _fallback_temp: false,
                                hour_ts: 1488337200,
                                condition: 'overcast',
                                pressure_pa: 994,
                                humidity: 82,
                                pressure_mm: 745,
                                wind_speed: 2.9,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -2,
                                wind_dir: 's',
                                icon: 'ovc'
                            },
                            {
                                temp: 2,
                                _fallback_prec: false,
                                temp_color: 'f0f0ec',
                                wind_gust: 9.4,
                                hour: '7',
                                _fallback_temp: false,
                                hour_ts: 1488340800,
                                condition: 'overcast',
                                pressure_pa: 994,
                                humidity: 83,
                                pressure_mm: 745,
                                wind_speed: 3,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -2,
                                wind_dir: 's',
                                icon: 'ovc'
                            },
                            {
                                temp: 2,
                                _fallback_prec: false,
                                temp_color: 'f0f0ec',
                                wind_gust: 9.7,
                                hour: '8',
                                _fallback_temp: false,
                                hour_ts: 1488344400,
                                condition: 'overcast-and-light-rain',
                                pressure_pa: 994,
                                humidity: 85,
                                pressure_mm: 745,
                                wind_speed: 3,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0.05,
                                feels_like: -2,
                                wind_dir: 's',
                                icon: 'ovc_-ra'
                            },
                            {
                                temp: 2,
                                _fallback_prec: false,
                                temp_color: 'f0f0ec',
                                wind_gust: 9.4,
                                hour: '9',
                                _fallback_temp: false,
                                hour_ts: 1488348000,
                                condition: 'overcast',
                                pressure_pa: 994,
                                humidity: 86,
                                pressure_mm: 745,
                                wind_speed: 3,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -2,
                                wind_dir: 's',
                                icon: 'ovc'
                            },
                            {
                                temp: 2,
                                _fallback_prec: false,
                                temp_color: 'f0f0ec',
                                wind_gust: 8.6,
                                hour: '10',
                                _fallback_temp: false,
                                hour_ts: 1488351600,
                                condition: 'overcast-and-light-rain',
                                pressure_pa: 994,
                                humidity: 87,
                                pressure_mm: 745,
                                wind_speed: 3,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0.05,
                                feels_like: -2,
                                wind_dir: 's',
                                icon: 'ovc_-ra'
                            },
                            {
                                temp: 4,
                                _fallback_prec: false,
                                temp_color: 'f1f0e9',
                                wind_gust: 11.8,
                                hour: '11',
                                _fallback_temp: false,
                                hour_ts: 1488355200,
                                condition: 'overcast',
                                pressure_pa: 994,
                                humidity: 79,
                                pressure_mm: 745,
                                wind_speed: 3,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: 0,
                                wind_dir: 's',
                                icon: 'ovc'
                            },
                            {
                                temp: 4,
                                _fallback_prec: false,
                                temp_color: 'f1f0e9',
                                wind_gust: 11.2,
                                hour: '12',
                                _fallback_temp: false,
                                hour_ts: 1488358800,
                                condition: 'overcast',
                                pressure_pa: 994,
                                humidity: 80,
                                pressure_mm: 745,
                                wind_speed: 3,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: 0,
                                wind_dir: 's',
                                icon: 'ovc'
                            },
                            {
                                temp: 4,
                                _fallback_prec: false,
                                temp_color: 'f1f0e9',
                                wind_gust: 10.9,
                                hour: '13',
                                _fallback_temp: false,
                                hour_ts: 1488362400,
                                condition: 'overcast',
                                pressure_pa: 992,
                                humidity: 80,
                                pressure_mm: 744,
                                wind_speed: 3,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: 0,
                                wind_dir: 's',
                                icon: 'ovc'
                            },
                            {
                                temp: 4,
                                _fallback_prec: false,
                                temp_color: 'f1f0e9',
                                wind_gust: 11.2,
                                hour: '14',
                                _fallback_temp: false,
                                hour_ts: 1488366000,
                                condition: 'overcast',
                                pressure_pa: 992,
                                humidity: 81,
                                pressure_mm: 744,
                                wind_speed: 3.1,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: 0,
                                wind_dir: 's',
                                icon: 'ovc'
                            },
                            {
                                temp: 4,
                                _fallback_prec: false,
                                temp_color: 'f1f0e9',
                                wind_gust: 7.5,
                                hour: '15',
                                _fallback_temp: false,
                                hour_ts: 1488369600,
                                condition: 'overcast',
                                pressure_pa: 992,
                                humidity: 82,
                                pressure_mm: 744,
                                wind_speed: 3.1,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: 0,
                                wind_dir: 's',
                                icon: 'ovc'
                            },
                            {
                                temp: 4,
                                _fallback_prec: false,
                                temp_color: 'f1f0e9',
                                wind_gust: 8.2,
                                hour: '16',
                                _fallback_temp: false,
                                hour_ts: 1488373200,
                                condition: 'overcast',
                                pressure_pa: 994,
                                humidity: 81,
                                pressure_mm: 745,
                                wind_speed: 3.1,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: 0,
                                wind_dir: 's',
                                icon: 'ovc'
                            },
                            {
                                temp: 3,
                                _fallback_prec: false,
                                temp_color: 'f1f0e9',
                                wind_gust: 9.4,
                                hour: '17',
                                _fallback_temp: false,
                                hour_ts: 1488376800,
                                condition: 'overcast',
                                pressure_pa: 994,
                                humidity: 82,
                                pressure_mm: 745,
                                wind_speed: 3.1,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -1,
                                wind_dir: 's',
                                icon: 'ovc'
                            },
                            {
                                temp: 3,
                                _fallback_prec: false,
                                temp_color: 'f1f0e9',
                                wind_gust: 10.2,
                                hour: '18',
                                _fallback_temp: false,
                                hour_ts: 1488380400,
                                condition: 'overcast',
                                pressure_pa: 992,
                                humidity: 85,
                                pressure_mm: 744,
                                wind_speed: 3.1,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -1,
                                wind_dir: 's',
                                icon: 'ovc'
                            },
                            {
                                temp: 3,
                                _fallback_prec: false,
                                temp_color: 'f1f0e9',
                                wind_gust: 9.9,
                                hour: '19',
                                _fallback_temp: false,
                                hour_ts: 1488384000,
                                condition: 'overcast',
                                pressure_pa: 992,
                                humidity: 83,
                                pressure_mm: 744,
                                wind_speed: 2.9,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -1,
                                wind_dir: 's',
                                icon: 'ovc'
                            },
                            {
                                temp: 3,
                                _fallback_prec: false,
                                temp_color: 'f1f0e9',
                                wind_gust: 9.4,
                                hour: '20',
                                _fallback_temp: false,
                                hour_ts: 1488387600,
                                condition: 'overcast',
                                pressure_pa: 992,
                                humidity: 82,
                                pressure_mm: 744,
                                wind_speed: 2.6,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -1,
                                wind_dir: 's',
                                icon: 'ovc'
                            },
                            {
                                temp: 3,
                                _fallback_prec: false,
                                temp_color: 'f1f0e9',
                                wind_gust: 9.8,
                                hour: '21',
                                _fallback_temp: false,
                                hour_ts: 1488391200,
                                condition: 'cloudy',
                                pressure_pa: 992,
                                humidity: 80,
                                pressure_mm: 744,
                                wind_speed: 2.5,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -1,
                                wind_dir: 's',
                                icon: 'bkn_n'
                            },
                            {
                                temp: 3,
                                _fallback_prec: false,
                                temp_color: 'f1f0e9',
                                wind_gust: 10.3,
                                hour: '22',
                                _fallback_temp: false,
                                hour_ts: 1488394800,
                                condition: 'cloudy',
                                pressure_pa: 992,
                                humidity: 80,
                                pressure_mm: 744,
                                wind_speed: 2.4,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -1,
                                wind_dir: 's',
                                icon: 'bkn_n'
                            },
                            {
                                temp: 2,
                                _fallback_prec: false,
                                temp_color: 'f0f0ec',
                                wind_gust: 11,
                                hour: '23',
                                _fallback_temp: false,
                                hour_ts: 1488398400,
                                condition: 'cloudy',
                                pressure_pa: 992,
                                humidity: 79,
                                pressure_mm: 744,
                                wind_speed: 2.4,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -1,
                                wind_dir: 's',
                                icon: 'bkn_n'
                            }
                        ],
                        date: '2017-02-28T21:00:00.000Z',
                        parts: [
                            {
                                temperature_to: 3,
                                wind_speed: 2.6,
                                temperature_from: 2,
                                temperature_min: 2,
                                temp_avg: 3,
                                weather_type: 'Облачно',
                                humidity: 88,
                                pressure: {
                                    content: 746,
                                    units: 'mm'
                                },
                                temperature_max: 3,
                                'temperature-data': {
                                    to: 3,
                                    from: 2
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
                                temperature_to: 4,
                                wind_speed: 3,
                                temperature_from: 2,
                                temperature_min: 2,
                                temp_avg: 3,
                                weather_type: 'Облачно, небольшой дождь',
                                humidity: 84,
                                pressure: {
                                    content: 745,
                                    units: 'mm'
                                },
                                temperature_max: 4,
                                'temperature-data': {
                                    to: 4,
                                    from: 2
                                },
                                'image-v3': {
                                    content: 'ovc_-ra'
                                },
                                type: 'morning',
                                weather_condition: {
                                    code: 'overcast-and-light-rain'
                                }
                            },
                            {
                                temperature_to: 4,
                                wind_speed: 3.1,
                                temperature_from: 3,
                                temperature_min: 3,
                                temp_avg: 4,
                                weather_type: 'Облачно',
                                humidity: 81,
                                pressure: {
                                    content: 745,
                                    units: 'mm'
                                },
                                temperature_max: 4,
                                'temperature-data': {
                                    to: 4,
                                    from: 3
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
                                temperature_to: 3,
                                wind_speed: 3.1,
                                temperature_from: 2,
                                temperature_min: 2,
                                temp_avg: 3,
                                weather_type: 'Облачно с прояснениями',
                                humidity: 81,
                                pressure: {
                                    content: 744,
                                    units: 'mm'
                                },
                                temperature_max: 3,
                                'temperature-data': {
                                    to: 3,
                                    from: 2
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
                                wind_speed: 2.6,
                                temperature: 2,
                                weather_type: 'Переменная облачность',
                                humidity: 76,
                                pressure: {
                                    content: 744,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 2,
                                        bgcolor: 'f0f0ec'
                                    }
                                },
                                'image-v2': {
                                    content: 'bkn_n_+2'
                                },
                                'image-v3': {
                                    content: 'bkn_n'
                                },
                                type: 'night_short',
                                weather_condition: {
                                    code: 'partly-cloudy'
                                }
                            },
                            {
                                wind_speed: 3.1,
                                temperature: 4,
                                weather_type: 'Облачно, небольшой дождь',
                                humidity: 82,
                                pressure: {
                                    content: 745,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 4,
                                        bgcolor: 'f1f0e9'
                                    }
                                },
                                'image-v2': {
                                    content: 'ovc_-ra_+4'
                                },
                                'image-v3': {
                                    content: 'ovc_-ra'
                                },
                                type: 'day_short',
                                weather_condition: {
                                    code: 'overcast-and-light-rain'
                                }
                            }
                        ],
                        type: 'by_day'
                    },
                    {
                        hours: [
                            {
                                temp: 2,
                                _fallback_prec: false,
                                temp_color: 'f0f0ec',
                                wind_gust: 11,
                                hour: '0',
                                _fallback_temp: false,
                                hour_ts: 1488402000,
                                condition: 'partly-cloudy',
                                pressure_pa: 992,
                                humidity: 77,
                                pressure_mm: 744,
                                wind_speed: 2.5,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -1,
                                wind_dir: 's',
                                icon: 'bkn_n'
                            },
                            {
                                temp: 2,
                                _fallback_prec: false,
                                temp_color: 'f0f0ec',
                                wind_gust: 10.9,
                                hour: '1',
                                _fallback_temp: false,
                                hour_ts: 1488405600,
                                condition: 'partly-cloudy',
                                pressure_pa: 992,
                                humidity: 77,
                                pressure_mm: 744,
                                wind_speed: 2.5,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -2,
                                wind_dir: 's',
                                icon: 'bkn_n'
                            },
                            {
                                temp: 2,
                                _fallback_prec: false,
                                temp_color: 'f0f0ec',
                                wind_gust: 10.5,
                                hour: '2',
                                _fallback_temp: false,
                                hour_ts: 1488409200,
                                condition: 'partly-cloudy',
                                pressure_pa: 992,
                                humidity: 76,
                                pressure_mm: 744,
                                wind_speed: 2.5,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -2,
                                wind_dir: 's',
                                icon: 'bkn_n'
                            },
                            {
                                temp: 2,
                                _fallback_prec: false,
                                temp_color: 'f0f0ec',
                                wind_gust: 10.1,
                                hour: '3',
                                _fallback_temp: false,
                                hour_ts: 1488412800,
                                condition: 'partly-cloudy',
                                pressure_pa: 992,
                                humidity: 75,
                                pressure_mm: 744,
                                wind_speed: 2.6,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -2,
                                wind_dir: 's',
                                icon: 'bkn_n'
                            }
                        ],
                        date: '2017-03-01T21:00:00.000Z',
                        parts: [
                            {
                                temperature_to: 2,
                                wind_speed: 2.6,
                                temperature_from: 2,
                                temperature_min: 2,
                                temp_avg: 2,
                                weather_type: 'Переменная облачность',
                                humidity: 76,
                                pressure: {
                                    content: 744,
                                    units: 'mm'
                                },
                                temperature_max: 2,
                                'temperature-data': {
                                    to: 2,
                                    from: 2
                                },
                                'image-v3': {
                                    content: 'bkn_n'
                                },
                                type: 'night',
                                weather_condition: {
                                    code: 'partly-cloudy'
                                }
                            },
                            {
                                temperature_to: 2,
                                wind_speed: 2.7,
                                temperature_from: 1,
                                temperature_min: 1,
                                temp_avg: 2,
                                weather_type: 'Облачно, небольшой дождь',
                                humidity: 75,
                                pressure: {
                                    content: 743,
                                    units: 'mm'
                                },
                                temperature_max: 2,
                                'temperature-data': {
                                    to: 2,
                                    from: 1
                                },
                                'image-v3': {
                                    content: 'ovc_-ra'
                                },
                                type: 'morning',
                                weather_condition: {
                                    code: 'overcast-and-light-rain'
                                }
                            },
                            {
                                temperature_to: 3,
                                wind_speed: 2.6,
                                temperature_from: 1,
                                temperature_min: 1,
                                temp_avg: 2,
                                weather_type: 'Облачно, небольшой дождь',
                                humidity: 92,
                                pressure: {
                                    content: 742,
                                    units: 'mm'
                                },
                                temperature_max: 3,
                                'temperature-data': {
                                    to: 3,
                                    from: 1
                                },
                                'image-v3': {
                                    content: 'ovc_-ra'
                                },
                                type: 'day',
                                weather_condition: {
                                    code: 'overcast-and-light-rain'
                                }
                            },
                            {
                                temperature_to: 2,
                                wind_speed: 2.2,
                                temperature_from: 0,
                                temperature_min: 0,
                                temp_avg: 1,
                                weather_type: 'Облачно, небольшой дождь',
                                humidity: 92,
                                pressure: {
                                    content: 742,
                                    units: 'mm'
                                },
                                temperature_max: 2,
                                'temperature-data': {
                                    to: 2,
                                    from: 0
                                },
                                'image-v3': {
                                    content: 'ovc_-ra'
                                },
                                type: 'evening',
                                weather_condition: {
                                    code: 'overcast-and-light-rain'
                                }
                            },
                            {
                                wind_speed: 1.9,
                                temperature: 0,
                                weather_type: 'Облачно, дождь со снегом',
                                humidity: 99,
                                pressure: {
                                    content: 742,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 0,
                                        bgcolor: 'f0eff0'
                                    }
                                },
                                'image-v2': {
                                    content: 'ovc_ra_+0'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                type: 'night_short',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                }
                            },
                            {
                                wind_speed: 2.7,
                                temperature: 3,
                                weather_type: 'Облачно, небольшой дождь',
                                humidity: 83,
                                pressure: {
                                    content: 743,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 4,
                                        bgcolor: 'f1f0e9'
                                    }
                                },
                                'image-v2': {
                                    content: 'ovc_-ra_+4'
                                },
                                'image-v3': {
                                    content: 'ovc_-ra'
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
                        date: '2017-03-02T21:00:00.000Z',
                        parts: [
                            {
                                temperature_to: 1,
                                wind_speed: 1.9,
                                temperature_from: 0,
                                temperature_min: 0,
                                temp_avg: 1,
                                weather_type: 'Облачно, дождь со снегом',
                                humidity: 99,
                                pressure: {
                                    content: 742,
                                    units: 'mm'
                                },
                                temperature_max: 1,
                                'temperature-data': {
                                    to: 1,
                                    from: 0
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                type: 'night',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                }
                            },
                            {
                                temperature_to: 0,
                                wind_speed: 1.8,
                                temperature_from: -1,
                                temperature_min: -1,
                                temp_avg: 0,
                                weather_type: 'Облачно, дождь со снегом',
                                humidity: 91,
                                pressure: {
                                    content: 741,
                                    units: 'mm'
                                },
                                temperature_max: 0,
                                'temperature-data': {
                                    to: 0,
                                    from: -1
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                type: 'morning',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                }
                            },
                            {
                                temperature_to: 3,
                                wind_speed: 2.7,
                                temperature_from: 1,
                                temperature_min: 1,
                                temp_avg: 2,
                                weather_type: 'Облачно, небольшой дождь',
                                humidity: 85,
                                pressure: {
                                    content: 740,
                                    units: 'mm'
                                },
                                temperature_max: 3,
                                'temperature-data': {
                                    to: 3,
                                    from: 1
                                },
                                'image-v3': {
                                    content: 'ovc_-ra'
                                },
                                type: 'day',
                                weather_condition: {
                                    code: 'overcast-and-light-rain'
                                }
                            },
                            {
                                temperature_to: 3,
                                wind_speed: 2.8,
                                temperature_from: 1,
                                temperature_min: 1,
                                temp_avg: 2,
                                weather_type: 'Облачно, небольшой дождь',
                                humidity: 92,
                                pressure: {
                                    content: 739,
                                    units: 'mm'
                                },
                                temperature_max: 3,
                                'temperature-data': {
                                    to: 3,
                                    from: 1
                                },
                                'image-v3': {
                                    content: 'ovc_-ra'
                                },
                                type: 'evening',
                                weather_condition: {
                                    code: 'overcast-and-light-rain'
                                }
                            },
                            {
                                wind_speed: 3.6,
                                temperature: 0,
                                weather_type: 'Облачно',
                                humidity: 93,
                                pressure: {
                                    content: 742,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 0,
                                        bgcolor: 'f0eff0'
                                    }
                                },
                                'image-v2': {
                                    content: 'ovc_+0'
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
                                wind_speed: 2.7,
                                temperature: 3,
                                weather_type: 'Облачно, дождь со снегом',
                                humidity: 88,
                                pressure: {
                                    content: 741,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 4,
                                        bgcolor: 'f1f0e9'
                                    }
                                },
                                'image-v2': {
                                    content: 'ovc_ra_+4'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                type: 'day_short',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                }
                            }
                        ]
                    },
                    {
                        hours: [],
                        date: '2017-03-03T21:00:00.000Z',
                        parts: [
                            {
                                temperature_to: 1,
                                wind_speed: 3.6,
                                temperature_from: 0,
                                temperature_min: 0,
                                temp_avg: 1,
                                weather_type: 'Облачно',
                                humidity: 93,
                                pressure: {
                                    content: 742,
                                    units: 'mm'
                                },
                                temperature_max: 1,
                                'temperature-data': {
                                    to: 1,
                                    from: 0
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
                                temperature_to: 1,
                                wind_speed: 3.5,
                                temperature_from: 0,
                                temperature_min: 0,
                                temp_avg: 1,
                                weather_type: 'Облачно, дождь со снегом',
                                humidity: 80,
                                pressure: {
                                    content: 746,
                                    units: 'mm'
                                },
                                temperature_max: 1,
                                'temperature-data': {
                                    to: 1,
                                    from: 0
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                type: 'morning',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                }
                            },
                            {
                                temperature_to: 3,
                                wind_speed: 3.1,
                                temperature_from: 1,
                                temperature_min: 1,
                                temp_avg: 2,
                                weather_type: 'Облачно, небольшой дождь',
                                humidity: 74,
                                pressure: {
                                    content: 749,
                                    units: 'mm'
                                },
                                temperature_max: 3,
                                'temperature-data': {
                                    to: 3,
                                    from: 1
                                },
                                'image-v3': {
                                    content: 'ovc_-ra'
                                },
                                type: 'day',
                                weather_condition: {
                                    code: 'overcast-and-light-rain'
                                }
                            },
                            {
                                temperature_to: 3,
                                wind_speed: 1.6,
                                temperature_from: 2,
                                temperature_min: 2,
                                temp_avg: 3,
                                weather_type: 'Облачно, небольшой дождь',
                                humidity: 73,
                                pressure: {
                                    content: 751,
                                    units: 'mm'
                                },
                                temperature_max: 3,
                                'temperature-data': {
                                    to: 3,
                                    from: 2
                                },
                                'image-v3': {
                                    content: 'ovc_-ra'
                                },
                                type: 'evening',
                                weather_condition: {
                                    code: 'overcast-and-light-rain'
                                }
                            },
                            {
                                wind_speed: 2.1,
                                temperature: 1,
                                weather_type: 'Облачно, дождь со снегом',
                                humidity: 91,
                                pressure: {
                                    content: 749,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 2,
                                        bgcolor: 'f0f0ec'
                                    }
                                },
                                'image-v2': {
                                    content: 'ovc_ra_+2'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                type: 'night_short',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                }
                            },
                            {
                                wind_speed: 3.5,
                                temperature: 3,
                                weather_type: 'Облачно, дождь со снегом',
                                humidity: 77,
                                pressure: {
                                    content: 748,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 4,
                                        bgcolor: 'f1f0e9'
                                    }
                                },
                                'image-v2': {
                                    content: 'ovc_ra_+4'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                type: 'day_short',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                }
                            }
                        ]
                    },
                    {
                        hours: [],
                        date: '2017-03-04T21:00:00.000Z',
                        parts: [
                            {
                                temperature_to: 2,
                                wind_speed: 2.1,
                                temperature_from: 1,
                                temperature_min: 1,
                                temp_avg: 2,
                                weather_type: 'Облачно, дождь со снегом',
                                humidity: 91,
                                pressure: {
                                    content: 749,
                                    units: 'mm'
                                },
                                temperature_max: 2,
                                'temperature-data': {
                                    to: 2,
                                    from: 1
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                type: 'night',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                }
                            },
                            {
                                temperature_to: 2,
                                wind_speed: 2.1,
                                temperature_from: 1,
                                temperature_min: 1,
                                temp_avg: 2,
                                weather_type: 'Облачно, дождь со снегом',
                                humidity: 98,
                                pressure: {
                                    content: 748,
                                    units: 'mm'
                                },
                                temperature_max: 2,
                                'temperature-data': {
                                    to: 2,
                                    from: 1
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                type: 'morning',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                }
                            },
                            {
                                temperature_to: 2,
                                wind_speed: 3.1,
                                temperature_from: 1,
                                temperature_min: 1,
                                temp_avg: 2,
                                weather_type: 'Облачно, небольшой дождь',
                                humidity: 95,
                                pressure: {
                                    content: 746,
                                    units: 'mm'
                                },
                                temperature_max: 2,
                                'temperature-data': {
                                    to: 2,
                                    from: 1
                                },
                                'image-v3': {
                                    content: 'ovc_-ra'
                                },
                                type: 'day',
                                weather_condition: {
                                    code: 'overcast-and-light-rain'
                                }
                            },
                            {
                                temperature_to: 2,
                                wind_speed: 2.8,
                                temperature_from: 1,
                                temperature_min: 1,
                                temp_avg: 2,
                                weather_type: 'Облачно, небольшой дождь',
                                humidity: 100,
                                pressure: {
                                    content: 746,
                                    units: 'mm'
                                },
                                temperature_max: 2,
                                'temperature-data': {
                                    to: 2,
                                    from: 1
                                },
                                'image-v3': {
                                    content: 'ovc_-ra'
                                },
                                type: 'evening',
                                weather_condition: {
                                    code: 'overcast-and-light-rain'
                                }
                            },
                            {
                                wind_speed: 3,
                                temperature: 1,
                                weather_type: 'Облачно, небольшой дождь',
                                humidity: 99,
                                pressure: {
                                    content: 748,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 2,
                                        bgcolor: 'f0f0ec'
                                    }
                                },
                                'image-v2': {
                                    content: 'ovc_-ra_+2'
                                },
                                'image-v3': {
                                    content: 'ovc_-ra'
                                },
                                type: 'night_short',
                                weather_condition: {
                                    code: 'overcast-and-light-rain'
                                }
                            },
                            {
                                wind_speed: 3.1,
                                temperature: 2,
                                weather_type: 'Облачно, дождь со снегом',
                                humidity: 96,
                                pressure: {
                                    content: 747,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 2,
                                        bgcolor: 'f0f0ec'
                                    }
                                },
                                'image-v2': {
                                    content: 'ovc_ra_+2'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                type: 'day_short',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                }
                            }
                        ]
                    },
                    {
                        hours: [],
                        date: '2017-03-05T21:00:00.000Z',
                        parts: [
                            {
                                temperature_to: 2,
                                wind_speed: 3,
                                temperature_from: 1,
                                temperature_min: 1,
                                temp_avg: 2,
                                weather_type: 'Облачно, небольшой дождь',
                                humidity: 99,
                                pressure: {
                                    content: 748,
                                    units: 'mm'
                                },
                                temperature_max: 2,
                                'temperature-data': {
                                    to: 2,
                                    from: 1
                                },
                                'image-v3': {
                                    content: 'ovc_-ra'
                                },
                                type: 'night',
                                weather_condition: {
                                    code: 'overcast-and-light-rain'
                                }
                            },
                            {
                                temperature_to: 3,
                                wind_speed: 1.6,
                                temperature_from: 2,
                                temperature_min: 2,
                                temp_avg: 3,
                                weather_type: 'Облачно, небольшой дождь',
                                humidity: 76,
                                pressure: {
                                    content: 749,
                                    units: 'mm'
                                },
                                temperature_max: 3,
                                'temperature-data': {
                                    to: 3,
                                    from: 2
                                },
                                'image-v3': {
                                    content: 'ovc_-ra'
                                },
                                type: 'morning',
                                weather_condition: {
                                    code: 'overcast-and-light-rain'
                                }
                            },
                            {
                                temperature_to: 4,
                                wind_speed: 3,
                                temperature_from: 3,
                                temperature_min: 3,
                                temp_avg: 4,
                                weather_type: 'Облачно, небольшой дождь',
                                humidity: 57,
                                pressure: {
                                    content: 751,
                                    units: 'mm'
                                },
                                temperature_max: 4,
                                'temperature-data': {
                                    to: 4,
                                    from: 3
                                },
                                'image-v3': {
                                    content: 'ovc_-ra'
                                },
                                type: 'day',
                                weather_condition: {
                                    code: 'overcast-and-light-rain'
                                }
                            },
                            {
                                temperature_to: 3,
                                wind_speed: 2.9,
                                temperature_from: 2,
                                temperature_min: 2,
                                temp_avg: 3,
                                weather_type: 'Облачно',
                                humidity: 39,
                                pressure: {
                                    content: 753,
                                    units: 'mm'
                                },
                                temperature_max: 3,
                                'temperature-data': {
                                    to: 3,
                                    from: 2
                                },
                                'image-v3': {
                                    content: 'ovc'
                                },
                                type: 'evening',
                                weather_condition: {
                                    code: 'overcast'
                                }
                            },
                            {
                                wind_speed: 2.5,
                                temperature: 0,
                                weather_type: 'Облачно',
                                humidity: 39,
                                pressure: {
                                    content: 754,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 0,
                                        bgcolor: 'f0eff0'
                                    }
                                },
                                'image-v2': {
                                    content: 'ovc_+0'
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
                                wind_speed: 3,
                                temperature: 4,
                                weather_type: 'Облачно, небольшой дождь',
                                humidity: 66,
                                pressure: {
                                    content: 750,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 4,
                                        bgcolor: 'f1f0e9'
                                    }
                                },
                                'image-v2': {
                                    content: 'ovc_-ra_+4'
                                },
                                'image-v3': {
                                    content: 'ovc_-ra'
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
                        date: '2017-03-06T21:00:00.000Z',
                        parts: [
                            {
                                temperature_to: 1,
                                wind_speed: 2.5,
                                temperature_from: 0,
                                temperature_min: 0,
                                temp_avg: 1,
                                weather_type: 'Облачно',
                                humidity: 39,
                                pressure: {
                                    content: 754,
                                    units: 'mm'
                                },
                                temperature_max: 1,
                                'temperature-data': {
                                    to: 1,
                                    from: 0
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
                                temperature_to: -1,
                                wind_speed: 2.7,
                                temperature_from: -2,
                                temperature_min: -2,
                                temp_avg: -1,
                                weather_type: 'Переменная облачность',
                                humidity: 31,
                                pressure: {
                                    content: 756,
                                    units: 'mm'
                                },
                                temperature_max: -1,
                                'temperature-data': {
                                    to: -1,
                                    from: -2
                                },
                                'image-v3': {
                                    content: 'bkn_d'
                                },
                                type: 'morning',
                                weather_condition: {
                                    code: 'partly-cloudy'
                                }
                            },
                            {
                                temperature_to: 0,
                                wind_speed: 3.2,
                                temperature_from: -2,
                                temperature_min: -2,
                                temp_avg: -1,
                                weather_type: 'Переменная облачность',
                                humidity: 35,
                                pressure: {
                                    content: 756,
                                    units: 'mm'
                                },
                                temperature_max: 0,
                                'temperature-data': {
                                    to: 0,
                                    from: -2
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
                                temperature_to: -1,
                                wind_speed: 2.4,
                                temperature_from: -3,
                                temperature_min: -3,
                                temp_avg: -2,
                                weather_type: 'Облачно',
                                humidity: 37,
                                pressure: {
                                    content: 757,
                                    units: 'mm'
                                },
                                temperature_max: -1,
                                'temperature-data': {
                                    to: -1,
                                    from: -3
                                },
                                'image-v3': {
                                    content: 'ovc'
                                },
                                type: 'evening',
                                weather_condition: {
                                    code: 'overcast'
                                }
                            },
                            {
                                wind_speed: 2.2,
                                temperature: -3,
                                weather_type: 'Облачно',
                                humidity: 41,
                                pressure: {
                                    content: 756,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: -4,
                                        bgcolor: 'f0eff0'
                                    }
                                },
                                'image-v2': {
                                    content: 'ovc_-4'
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
                                wind_speed: 3.2,
                                temperature: 0,
                                weather_type: 'Переменная облачность',
                                humidity: 33,
                                pressure: {
                                    content: 756,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: 0,
                                        bgcolor: 'f0eff0'
                                    }
                                },
                                'image-v2': {
                                    content: 'bkn_d_+0'
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
                        date: '2017-03-07T21:00:00.000Z',
                        parts: [
                            {
                                temperature_to: -2,
                                wind_speed: 2.2,
                                temperature_from: -3,
                                temperature_min: -3,
                                temp_avg: -2,
                                weather_type: 'Облачно',
                                humidity: 41,
                                pressure: {
                                    content: 756,
                                    units: 'mm'
                                },
                                temperature_max: -2,
                                'temperature-data': {
                                    to: -2,
                                    from: -3
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
                                temperature_to: -3,
                                wind_speed: 2.4,
                                temperature_from: -5,
                                temperature_min: -5,
                                temp_avg: -4,
                                weather_type: 'Облачно, дождь со снегом',
                                humidity: 53,
                                pressure: {
                                    content: 754,
                                    units: 'mm'
                                },
                                temperature_max: -3,
                                'temperature-data': {
                                    to: -3,
                                    from: -5
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                type: 'morning',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                }
                            },
                            {
                                temperature_to: -2,
                                wind_speed: 2.9,
                                temperature_from: -4,
                                temperature_min: -4,
                                temp_avg: -3,
                                weather_type: 'Облачно, дождь со снегом',
                                humidity: 76,
                                pressure: {
                                    content: 752,
                                    units: 'mm'
                                },
                                temperature_max: -2,
                                'temperature-data': {
                                    to: -2,
                                    from: -4
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                type: 'day',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                }
                            },
                            {
                                temperature_to: -2,
                                wind_speed: 1.8,
                                temperature_from: -4,
                                temperature_min: -4,
                                temp_avg: -3,
                                weather_type: 'Облачно',
                                humidity: 91,
                                pressure: {
                                    content: 751,
                                    units: 'mm'
                                },
                                temperature_max: -2,
                                'temperature-data': {
                                    to: -2,
                                    from: -4
                                },
                                'image-v3': {
                                    content: 'ovc'
                                },
                                type: 'evening',
                                weather_condition: {
                                    code: 'overcast'
                                }
                            },
                            {
                                wind_speed: 1.8,
                                temperature: -5,
                                weather_type: 'Облачно, дождь со снегом',
                                humidity: 99,
                                pressure: {
                                    content: 751,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: -6,
                                        bgcolor: 'eaedef'
                                    }
                                },
                                'image-v2': {
                                    content: 'ovc_ra_-6'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                type: 'night_short',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                }
                            },
                            {
                                wind_speed: 2.9,
                                temperature: -2,
                                weather_type: 'Облачно, дождь со снегом',
                                humidity: 65,
                                pressure: {
                                    content: 753,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: -2,
                                        bgcolor: 'f0eff0'
                                    }
                                },
                                'image-v2': {
                                    content: 'ovc_ra_-2'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                type: 'day_short',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                }
                            }
                        ]
                    }
                ],
                current: {
                    wind_speed: 1,
                    uptime: '2017-02-28T13:00:00',
                    temperature: 3,
                    weather_type: 'Облачно',
                    wind_direction: 's',
                    humidity: 61,
                    pressure: {
                        content: 748,
                        units: 'mm'
                    },
                    'temperature-data': {
                        avg: {
                            content: 4,
                            bgcolor: 'f1f0e9'
                        }
                    },
                    'image-v2': {
                        content: 'ovc_+4'
                    },
                    'image-v3': {
                        content: 'ovc'
                    },
                    weather_condition: {
                        code: 'overcast'
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
                            text: 'Завтра в Москве 4°, Облачно, небольшой дождь. '
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
                    all: [
                        'snippets',
                        'weather'
                    ]
                }
            }
        },
        size: 0
    }
};
