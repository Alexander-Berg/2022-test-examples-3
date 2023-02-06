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
                                temp: 0,
                                _fallback_prec: false,
                                temp_color: 'f0eff0',
                                wind_gust: 2.2,
                                hour: '4',
                                _fallback_temp: false,
                                hour_ts: 1488243600,
                                condition: 'cloudy',
                                pressure_pa: 996,
                                humidity: 76,
                                pressure_mm: 747,
                                wind_speed: 0.3,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -3,
                                wind_dir: 'e',
                                icon: 'bkn_n'
                            },
                            {
                                temp: 0,
                                _fallback_prec: false,
                                temp_color: 'f0eff0',
                                wind_gust: 1.7,
                                hour: '5',
                                _fallback_temp: false,
                                hour_ts: 1488247200,
                                condition: 'cloudy',
                                pressure_pa: 996,
                                humidity: 75,
                                pressure_mm: 747,
                                wind_speed: 1.1,
                                prec_period: 60,
                                feels_like_color: 'eaedef',
                                prec_mm: 0,
                                feels_like: -4,
                                wind_dir: 'se',
                                icon: 'bkn_n'
                            },
                            {
                                temp: -1,
                                _fallback_prec: false,
                                temp_color: 'f0eff0',
                                wind_gust: 2.2,
                                hour: '6',
                                _fallback_temp: false,
                                hour_ts: 1488250800,
                                condition: 'cloudy',
                                pressure_pa: 996,
                                humidity: 70,
                                pressure_mm: 747,
                                wind_speed: 0.9,
                                prec_period: 60,
                                feels_like_color: 'eaedef',
                                prec_mm: 0,
                                feels_like: -4,
                                wind_dir: 's',
                                icon: 'bkn_n'
                            },
                            {
                                temp: -1,
                                _fallback_prec: false,
                                temp_color: 'f0eff0',
                                wind_gust: 3.9,
                                hour: '7',
                                _fallback_temp: false,
                                hour_ts: 1488254400,
                                condition: 'cloudy',
                                pressure_pa: 998,
                                humidity: 92,
                                pressure_mm: 748,
                                wind_speed: 1.7,
                                prec_period: 60,
                                feels_like_color: 'eaedef',
                                prec_mm: 0,
                                feels_like: -4,
                                wind_dir: 'sw',
                                icon: 'bkn_n'
                            },
                            {
                                temp: -1,
                                _fallback_prec: false,
                                temp_color: 'f0eff0',
                                wind_gust: 3,
                                hour: '8',
                                _fallback_temp: false,
                                hour_ts: 1488258000,
                                condition: 'overcast',
                                pressure_pa: 998,
                                humidity: 92,
                                pressure_mm: 748,
                                wind_speed: 1.7,
                                prec_period: 60,
                                feels_like_color: 'eaedef',
                                prec_mm: 0,
                                feels_like: -5,
                                wind_dir: 'sw',
                                icon: 'ovc'
                            },
                            {
                                temp: -1,
                                _fallback_prec: false,
                                temp_color: 'f0eff0',
                                wind_gust: 1.4,
                                hour: '9',
                                _fallback_temp: false,
                                hour_ts: 1488261600,
                                condition: 'overcast',
                                pressure_pa: 998,
                                humidity: 87,
                                pressure_mm: 748,
                                wind_speed: 1.3,
                                prec_period: 60,
                                feels_like_color: 'eaedef',
                                prec_mm: 0,
                                feels_like: -4,
                                wind_dir: 'sw',
                                icon: 'ovc'
                            },
                            {
                                temp: 0,
                                _fallback_prec: false,
                                temp_color: 'f0eff0',
                                wind_gust: 2.9,
                                hour: '10',
                                _fallback_temp: false,
                                hour_ts: 1488265200,
                                condition: 'overcast',
                                pressure_pa: 998,
                                humidity: 81,
                                pressure_mm: 748,
                                _nowcast: true,
                                wind_speed: 1,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -3,
                                wind_dir: 's',
                                icon: 'ovc'
                            },
                            {
                                temp: 1,
                                _fallback_prec: false,
                                temp_color: 'f0f0ec',
                                wind_gust: 3.4,
                                hour: '11',
                                _fallback_temp: false,
                                hour_ts: 1488268800,
                                condition: 'overcast',
                                pressure_pa: 999,
                                humidity: 76,
                                pressure_mm: 749,
                                _nowcast: true,
                                wind_speed: 1.6,
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
                                wind_gust: 3.8,
                                hour: '12',
                                _fallback_temp: false,
                                hour_ts: 1488272400,
                                condition: 'overcast',
                                pressure_pa: 999,
                                humidity: 72,
                                pressure_mm: 749,
                                _nowcast: true,
                                wind_speed: 2.2,
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
                                wind_gust: 6.2,
                                hour: '13',
                                _fallback_temp: false,
                                hour_ts: 1488276000,
                                condition: 'overcast',
                                pressure_pa: 999,
                                humidity: 72,
                                pressure_mm: 749,
                                wind_speed: 2.4,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -2,
                                wind_dir: 's',
                                icon: 'ovc'
                            },
                            {
                                temp: 3,
                                _fallback_prec: false,
                                temp_color: 'f1f0e9',
                                wind_gust: 6.3,
                                hour: '14',
                                _fallback_temp: false,
                                hour_ts: 1488279600,
                                condition: 'overcast',
                                pressure_pa: 999,
                                humidity: 65,
                                pressure_mm: 749,
                                wind_speed: 2.6,
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
                                wind_gust: 5.6,
                                hour: '15',
                                _fallback_temp: false,
                                hour_ts: 1488283200,
                                condition: 'overcast',
                                pressure_pa: 998,
                                humidity: 64,
                                pressure_mm: 748,
                                wind_speed: 2.4,
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
                                wind_gust: 6.3,
                                hour: '16',
                                _fallback_temp: false,
                                hour_ts: 1488286800,
                                condition: 'overcast',
                                pressure_pa: 998,
                                humidity: 69,
                                pressure_mm: 748,
                                wind_speed: 2.2,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: 0,
                                wind_dir: 's',
                                icon: 'ovc'
                            },
                            {
                                temp: 2,
                                _fallback_prec: false,
                                temp_color: 'f0f0ec',
                                wind_gust: 4.6,
                                hour: '17',
                                _fallback_temp: false,
                                hour_ts: 1488290400,
                                condition: 'overcast',
                                pressure_pa: 998,
                                humidity: 77,
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
                                temp: 2,
                                _fallback_prec: false,
                                temp_color: 'f0f0ec',
                                wind_gust: 5.4,
                                hour: '18',
                                _fallback_temp: false,
                                hour_ts: 1488294000,
                                condition: 'overcast',
                                pressure_pa: 998,
                                humidity: 81,
                                pressure_mm: 748,
                                wind_speed: 2.2,
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
                                wind_gust: 5.9,
                                hour: '19',
                                _fallback_temp: false,
                                hour_ts: 1488297600,
                                condition: 'overcast',
                                pressure_pa: 998,
                                humidity: 83,
                                pressure_mm: 748,
                                wind_speed: 2.4,
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
                                wind_gust: 6.5,
                                hour: '20',
                                _fallback_temp: false,
                                hour_ts: 1488301200,
                                condition: 'overcast',
                                pressure_pa: 998,
                                humidity: 84,
                                pressure_mm: 748,
                                wind_speed: 2.4,
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
                                wind_gust: 6.8,
                                hour: '21',
                                _fallback_temp: false,
                                hour_ts: 1488304800,
                                condition: 'overcast',
                                pressure_pa: 998,
                                humidity: 82,
                                pressure_mm: 748,
                                wind_speed: 2.5,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -1,
                                wind_dir: 'se',
                                icon: 'ovc'
                            },
                            {
                                temp: 3,
                                _fallback_prec: false,
                                temp_color: 'f1f0e9',
                                wind_gust: 7.7,
                                hour: '22',
                                _fallback_temp: false,
                                hour_ts: 1488308400,
                                condition: 'overcast',
                                pressure_pa: 996,
                                humidity: 84,
                                pressure_mm: 747,
                                wind_speed: 2.5,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -1,
                                wind_dir: 'se',
                                icon: 'ovc'
                            },
                            {
                                temp: 2,
                                _fallback_prec: false,
                                temp_color: 'f0f0ec',
                                wind_gust: 8.4,
                                hour: '23',
                                _fallback_temp: false,
                                hour_ts: 1488312000,
                                condition: 'overcast',
                                pressure_pa: 996,
                                humidity: 87,
                                pressure_mm: 747,
                                wind_speed: 2.6,
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
                                temperature_to: 0,
                                wind_speed: 1.2,
                                temperature_from: -2,
                                temperature_min: -2,
                                temp_avg: -1,
                                weather_type: 'Облачно с прояснениями',
                                humidity: 84,
                                pressure: {
                                    content: 746,
                                    units: 'mm'
                                },
                                temperature_max: 0,
                                'temperature-data': {
                                    to: 0,
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
                                temperature_to: 1,
                                wind_speed: 1.7,
                                temperature_from: -1,
                                temperature_min: -1,
                                temp_avg: 0,
                                weather_type: 'Облачно',
                                humidity: 83,
                                pressure: {
                                    content: 748,
                                    units: 'mm'
                                },
                                temperature_max: 1,
                                'temperature-data': {
                                    to: 1,
                                    from: -1
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
                                temperature_to: 4,
                                wind_speed: 2.6,
                                temperature_from: 2,
                                temperature_min: 2,
                                temp_avg: 3,
                                weather_type: 'Облачно',
                                humidity: 70,
                                pressure: {
                                    content: 749,
                                    units: 'mm'
                                },
                                temperature_max: 4,
                                'temperature-data': {
                                    to: 4,
                                    from: 2
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
                                wind_speed: 2.6,
                                temperature_from: 2,
                                temperature_min: 2,
                                temp_avg: 3,
                                weather_type: 'Облачно',
                                humidity: 84,
                                pressure: {
                                    content: 748,
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
                                wind_speed: 2.6,
                                temperature: 2,
                                weather_type: 'Облачно',
                                humidity: 85,
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
                                wind_speed: 2.6,
                                temperature: 4,
                                weather_type: 'Облачно',
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
                                    content: 'ovc_+4'
                                },
                                'image-v3': {
                                    content: 'ovc'
                                },
                                type: 'day_short',
                                weather_condition: {
                                    code: 'overcast'
                                }
                            }
                        ],
                        type: 'today',
                        current_part: 1
                    },
                    {
                        hours: [
                            {
                                temp: 2,
                                _fallback_prec: false,
                                temp_color: 'f0f0ec',
                                wind_gust: 8.5,
                                hour: '0',
                                _fallback_temp: false,
                                hour_ts: 1488315600,
                                condition: 'overcast',
                                pressure_pa: 996,
                                humidity: 87,
                                pressure_mm: 747,
                                wind_speed: 2.6,
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
                                wind_gust: 8.3,
                                hour: '1',
                                _fallback_temp: false,
                                hour_ts: 1488319200,
                                condition: 'overcast',
                                pressure_pa: 996,
                                humidity: 87,
                                pressure_mm: 747,
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
                                wind_gust: 7.6,
                                hour: '2',
                                _fallback_temp: false,
                                hour_ts: 1488322800,
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
                                wind_gust: 8,
                                hour: '3',
                                _fallback_temp: false,
                                hour_ts: 1488326400,
                                condition: 'overcast',
                                pressure_pa: 995,
                                humidity: 83,
                                pressure_mm: 746,
                                wind_speed: 2.5,
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
                                wind_gust: 7.1,
                                hour: '4',
                                _fallback_temp: false,
                                hour_ts: 1488330000,
                                condition: 'overcast',
                                pressure_pa: 995,
                                humidity: 83,
                                pressure_mm: 746,
                                wind_speed: 2.4,
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
                                hour: '5',
                                _fallback_temp: false,
                                hour_ts: 1488333600,
                                condition: 'overcast',
                                pressure_pa: 995,
                                humidity: 83,
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
                                wind_gust: 8.5,
                                hour: '6',
                                _fallback_temp: false,
                                hour_ts: 1488337200,
                                condition: 'overcast',
                                pressure_pa: 994,
                                humidity: 82,
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
                                wind_gust: 8.9,
                                hour: '7',
                                _fallback_temp: false,
                                hour_ts: 1488340800,
                                condition: 'overcast',
                                pressure_pa: 994,
                                humidity: 83,
                                pressure_mm: 745,
                                wind_speed: 2.7,
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
                                wind_gust: 9.3,
                                hour: '8',
                                _fallback_temp: false,
                                hour_ts: 1488344400,
                                condition: 'overcast',
                                pressure_pa: 994,
                                humidity: 84,
                                pressure_mm: 745,
                                wind_speed: 2.8,
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
                                hour: '9',
                                _fallback_temp: false,
                                hour_ts: 1488348000,
                                condition: 'overcast',
                                pressure_pa: 994,
                                humidity: 85,
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
                                wind_gust: 9.1,
                                hour: '10',
                                _fallback_temp: false,
                                hour_ts: 1488351600,
                                condition: 'overcast',
                                pressure_pa: 994,
                                humidity: 86,
                                pressure_mm: 745,
                                wind_speed: 3.2,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -2,
                                wind_dir: 's',
                                icon: 'ovc'
                            },
                            {
                                temp: 3,
                                _fallback_prec: false,
                                temp_color: 'f1f0e9',
                                wind_gust: 12.3,
                                hour: '11',
                                _fallback_temp: false,
                                hour_ts: 1488355200,
                                condition: 'overcast',
                                pressure_pa: 994,
                                humidity: 81,
                                pressure_mm: 745,
                                wind_speed: 3.3,
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
                                wind_gust: 11.8,
                                hour: '12',
                                _fallback_temp: false,
                                hour_ts: 1488358800,
                                condition: 'overcast',
                                pressure_pa: 994,
                                humidity: 81,
                                pressure_mm: 745,
                                wind_speed: 3.3,
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
                                wind_gust: 11.1,
                                hour: '13',
                                _fallback_temp: false,
                                hour_ts: 1488362400,
                                condition: 'overcast',
                                pressure_pa: 994,
                                humidity: 81,
                                pressure_mm: 745,
                                wind_speed: 3.3,
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
                                wind_gust: 10.9,
                                hour: '14',
                                _fallback_temp: false,
                                hour_ts: 1488366000,
                                condition: 'overcast',
                                pressure_pa: 994,
                                humidity: 82,
                                pressure_mm: 745,
                                wind_speed: 3.2,
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
                                wind_gust: 11.1,
                                hour: '15',
                                _fallback_temp: false,
                                hour_ts: 1488369600,
                                condition: 'overcast',
                                pressure_pa: 994,
                                humidity: 83,
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
                                wind_gust: 8.1,
                                hour: '16',
                                _fallback_temp: false,
                                hour_ts: 1488373200,
                                condition: 'overcast',
                                pressure_pa: 994,
                                humidity: 83,
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
                                wind_gust: 9.1,
                                hour: '17',
                                _fallback_temp: false,
                                hour_ts: 1488376800,
                                condition: 'overcast',
                                pressure_pa: 994,
                                humidity: 85,
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
                                wind_gust: 9.5,
                                hour: '18',
                                _fallback_temp: false,
                                hour_ts: 1488380400,
                                condition: 'overcast',
                                pressure_pa: 994,
                                humidity: 84,
                                pressure_mm: 745,
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
                                wind_gust: 9.8,
                                hour: '19',
                                _fallback_temp: false,
                                hour_ts: 1488384000,
                                condition: 'overcast',
                                pressure_pa: 994,
                                humidity: 83,
                                pressure_mm: 745,
                                wind_speed: 2.8,
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
                                wind_gust: 9,
                                hour: '20',
                                _fallback_temp: false,
                                hour_ts: 1488387600,
                                condition: 'overcast',
                                pressure_pa: 994,
                                humidity: 81,
                                pressure_mm: 745,
                                wind_speed: 2.8,
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
                                wind_gust: 9,
                                hour: '21',
                                _fallback_temp: false,
                                hour_ts: 1488391200,
                                condition: 'overcast',
                                pressure_pa: 994,
                                humidity: 79,
                                pressure_mm: 745,
                                wind_speed: 2.5,
                                prec_period: 60,
                                feels_like_color: 'f0eff0',
                                prec_mm: 0,
                                feels_like: -1,
                                wind_dir: 's',
                                icon: 'ovc'
                            }
                        ],
                        date: '2017-02-28T21:00:00.000Z',
                        parts: [
                            {
                                temperature_to: 2,
                                wind_speed: 2.6,
                                temperature_from: 2,
                                temperature_min: 2,
                                temp_avg: 2,
                                weather_type: 'Облачно',
                                humidity: 85,
                                pressure: {
                                    content: 746,
                                    units: 'mm'
                                },
                                temperature_max: 2,
                                'temperature-data': {
                                    to: 2,
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
                                temperature_to: 3,
                                wind_speed: 3.3,
                                temperature_from: 2,
                                temperature_min: 2,
                                temp_avg: 3,
                                weather_type: 'Облачно',
                                humidity: 84,
                                pressure: {
                                    content: 745,
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
                                type: 'morning',
                                weather_condition: {
                                    code: 'overcast'
                                }
                            },
                            {
                                temperature_to: 3,
                                wind_speed: 3.3,
                                temperature_from: 3,
                                temperature_min: 3,
                                temp_avg: 3,
                                weather_type: 'Облачно',
                                humidity: 83,
                                pressure: {
                                    content: 745,
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
                                type: 'day',
                                weather_condition: {
                                    code: 'overcast'
                                }
                            },
                            {
                                temperature_to: 3,
                                wind_speed: 3,
                                temperature_from: 3,
                                temperature_min: 3,
                                temp_avg: 3,
                                weather_type: 'Облачно',
                                humidity: 82,
                                pressure: {
                                    content: 745,
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
                                wind_speed: 2.3,
                                temperature: 2,
                                weather_type: 'Облачно',
                                humidity: 82,
                                pressure: {
                                    content: 745,
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
                                wind_speed: 3.3,
                                temperature: 3,
                                weather_type: 'Облачно',
                                humidity: 83,
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
                                    content: 'ovc_+4'
                                },
                                'image-v3': {
                                    content: 'ovc'
                                },
                                type: 'day_short',
                                weather_condition: {
                                    code: 'overcast'
                                }
                            }
                        ]
                    },
                    {
                        hours: [],
                        date: '2017-03-01T21:00:00.000Z',
                        parts: [
                            {
                                temperature_to: 3,
                                wind_speed: 2.3,
                                temperature_from: 2,
                                temperature_min: 2,
                                temp_avg: 3,
                                weather_type: 'Облачно',
                                humidity: 82,
                                pressure: {
                                    content: 745,
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
                                temperature_to: 2,
                                wind_speed: 2.9,
                                temperature_from: 1,
                                temperature_min: 1,
                                temp_avg: 2,
                                weather_type: 'Облачно, небольшой дождь',
                                humidity: 75,
                                pressure: {
                                    content: 744,
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
                                temperature_to: 2,
                                wind_speed: 2.6,
                                temperature_from: 1,
                                temperature_min: 1,
                                temp_avg: 2,
                                weather_type: 'Облачно, небольшой дождь',
                                humidity: 89,
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
                                type: 'day',
                                weather_condition: {
                                    code: 'overcast-and-light-rain'
                                }
                            },
                            {
                                temperature_to: 1,
                                wind_speed: 1.9,
                                temperature_from: 0,
                                temperature_min: 0,
                                temp_avg: 1,
                                weather_type: 'Облачно, дождь со снегом',
                                humidity: 100,
                                pressure: {
                                    content: 743,
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
                                type: 'evening',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                }
                            },
                            {
                                wind_speed: 1.5,
                                temperature: 0,
                                weather_type: 'Облачно',
                                humidity: 100,
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
                                wind_speed: 2.9,
                                temperature: 2,
                                weather_type: 'Облачно, небольшой дождь',
                                humidity: 82,
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
                                    content: 'ovc_-ra_+2'
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
                                wind_speed: 1.5,
                                temperature_from: 0,
                                temperature_min: 0,
                                temp_avg: 1,
                                weather_type: 'Облачно',
                                humidity: 100,
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
                                temperature_to: 0,
                                wind_speed: 1.8,
                                temperature_from: -1,
                                temperature_min: -1,
                                temp_avg: 0,
                                weather_type: 'Облачно, дождь со снегом',
                                humidity: 98,
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
                                temperature_to: 2,
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
                                weather_type: 'Облачно, дождь со снегом',
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
                                temperature: 2,
                                weather_type: 'Облачно, дождь со снегом',
                                humidity: 92,
                                pressure: {
                                    content: 741,
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
                        date: '2017-03-03T21:00:00.000Z',
                        parts: [
                            {
                                temperature_to: 1,
                                wind_speed: 3.6,
                                temperature_from: 0,
                                temperature_min: 0,
                                temp_avg: 1,
                                weather_type: 'Облачно, дождь со снегом',
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
                                    content: 'ovc_ra'
                                },
                                type: 'night',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                }
                            },
                            {
                                temperature_to: 0,
                                wind_speed: 3.5,
                                temperature_from: -1,
                                temperature_min: -1,
                                temp_avg: 0,
                                weather_type: 'Облачно, дождь со снегом',
                                humidity: 80,
                                pressure: {
                                    content: 746,
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
                                wind_speed: 2.8,
                                temperature_from: 1,
                                temperature_min: 1,
                                temp_avg: 2,
                                weather_type: 'Облачно, небольшой дождь',
                                humidity: 64,
                                pressure: {
                                    content: 751,
                                    units: 'mm'
                                },
                                temperature_max: 3,
                                'temperature-data': {
                                    to: 3,
                                    from: 1
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
                                temperature_to: 2,
                                wind_speed: 1.8,
                                temperature_from: 1,
                                temperature_min: 1,
                                temp_avg: 2,
                                weather_type: 'Облачно, дождь со снегом',
                                humidity: 77,
                                pressure: {
                                    content: 752,
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
                                type: 'evening',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                }
                            },
                            {
                                wind_speed: 2.3,
                                temperature: 0,
                                weather_type: 'Облачно, дождь со снегом',
                                humidity: 82,
                                pressure: {
                                    content: 749,
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
                                wind_speed: 3.5,
                                temperature: 3,
                                weather_type: 'Облачно, дождь со снегом',
                                humidity: 72,
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
                        hours: [],
                        date: '2017-03-04T21:00:00.000Z',
                        parts: [
                            {
                                temperature_to: 1,
                                wind_speed: 2.3,
                                temperature_from: 0,
                                temperature_min: 0,
                                temp_avg: 1,
                                weather_type: 'Облачно, дождь со снегом',
                                humidity: 82,
                                pressure: {
                                    content: 749,
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
                                temperature_to: 1,
                                wind_speed: 3.4,
                                temperature_from: 0,
                                temperature_min: 0,
                                temp_avg: 1,
                                weather_type: 'Облачно, дождь со снегом',
                                humidity: 94,
                                pressure: {
                                    content: 747,
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
                                humidity: 91,
                                pressure: {
                                    content: 747,
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
                                wind_speed: 2.2,
                                temperature_from: 2,
                                temperature_min: 2,
                                temp_avg: 3,
                                weather_type: 'Облачно, небольшой дождь',
                                humidity: 90,
                                pressure: {
                                    content: 748,
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
                                wind_speed: 1.9,
                                temperature: 0,
                                weather_type: 'Облачно',
                                humidity: 86,
                                pressure: {
                                    content: 749,
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
                                wind_speed: 3.4,
                                temperature: 3,
                                weather_type: 'Облачно, дождь со снегом',
                                humidity: 93,
                                pressure: {
                                    content: 747,
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
                        date: '2017-03-05T21:00:00.000Z',
                        parts: [
                            {
                                temperature_to: 2,
                                wind_speed: 1.9,
                                temperature_from: 0,
                                temperature_min: 0,
                                temp_avg: 1,
                                weather_type: 'Облачно',
                                humidity: 86,
                                pressure: {
                                    content: 749,
                                    units: 'mm'
                                },
                                temperature_max: 2,
                                'temperature-data': {
                                    to: 2,
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
                                wind_speed: 1.6,
                                temperature_from: 0,
                                temperature_min: 0,
                                temp_avg: 1,
                                weather_type: 'Облачно, дождь со снегом',
                                humidity: 88,
                                pressure: {
                                    content: 749,
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
                                wind_speed: 3,
                                temperature_from: 1,
                                temperature_min: 1,
                                temp_avg: 2,
                                weather_type: 'Облачно, небольшой дождь',
                                humidity: 57,
                                pressure: {
                                    content: 751,
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
                                wind_speed: 2.9,
                                temperature_from: 1,
                                temperature_min: 1,
                                temp_avg: 2,
                                weather_type: 'Облачно',
                                humidity: 42,
                                pressure: {
                                    content: 753,
                                    units: 'mm'
                                },
                                temperature_max: 2,
                                'temperature-data': {
                                    to: 2,
                                    from: 1
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
                                temperature: -1,
                                weather_type: 'Облачно',
                                humidity: 39,
                                pressure: {
                                    content: 754,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: -2,
                                        bgcolor: 'f0eff0'
                                    }
                                },
                                'image-v2': {
                                    content: 'ovc_-2'
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
                                temperature: 3,
                                weather_type: 'Облачно, дождь со снегом',
                                humidity: 72,
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
                        date: '2017-03-06T21:00:00.000Z',
                        parts: [
                            {
                                temperature_to: 0,
                                wind_speed: 2.5,
                                temperature_from: -1,
                                temperature_min: -1,
                                temp_avg: 0,
                                weather_type: 'Облачно',
                                humidity: 39,
                                pressure: {
                                    content: 754,
                                    units: 'mm'
                                },
                                temperature_max: 0,
                                'temperature-data': {
                                    to: 0,
                                    from: -1
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
                                humidity: 33,
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
                                temperature_to: -1,
                                wind_speed: 3.2,
                                temperature_from: -3,
                                temperature_min: -3,
                                temp_avg: -2,
                                weather_type: 'Переменная облачность',
                                humidity: 35,
                                pressure: {
                                    content: 756,
                                    units: 'mm'
                                },
                                temperature_max: -1,
                                'temperature-data': {
                                    to: -1,
                                    from: -3
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
                                temperature_to: -2,
                                wind_speed: 1.5,
                                temperature_from: -3,
                                temperature_min: -3,
                                temp_avg: -2,
                                weather_type: 'Облачно',
                                humidity: 100,
                                pressure: {
                                    content: 757,
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
                                type: 'evening',
                                weather_condition: {
                                    code: 'overcast'
                                }
                            },
                            {
                                wind_speed: 1.4,
                                temperature: -5,
                                weather_type: 'Облачно',
                                humidity: 100,
                                pressure: {
                                    content: 758,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: -6,
                                        bgcolor: 'eaedef'
                                    }
                                },
                                'image-v2': {
                                    content: 'ovc_-6'
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
                                temperature: -1,
                                weather_type: 'Переменная облачность',
                                humidity: 34,
                                pressure: {
                                    content: 756,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: -2,
                                        bgcolor: 'f0eff0'
                                    }
                                },
                                'image-v2': {
                                    content: 'bkn_d_-2'
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
                                temperature_to: -4,
                                wind_speed: 1.4,
                                temperature_from: -5,
                                temperature_min: -5,
                                temp_avg: -4,
                                weather_type: 'Облачно',
                                humidity: 100,
                                pressure: {
                                    content: 758,
                                    units: 'mm'
                                },
                                temperature_max: -4,
                                'temperature-data': {
                                    to: -4,
                                    from: -5
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
                                temperature_to: -4,
                                wind_speed: 1.3,
                                temperature_from: -5,
                                temperature_min: -5,
                                temp_avg: -4,
                                weather_type: 'Облачно, дождь со снегом',
                                humidity: 87,
                                pressure: {
                                    content: 758,
                                    units: 'mm'
                                },
                                temperature_max: -4,
                                'temperature-data': {
                                    to: -4,
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
                                temperature_to: -1,
                                wind_speed: 2.5,
                                temperature_from: -4,
                                temperature_min: -4,
                                temp_avg: -2,
                                weather_type: 'Облачно',
                                humidity: 74,
                                pressure: {
                                    content: 757,
                                    units: 'mm'
                                },
                                temperature_max: -1,
                                'temperature-data': {
                                    to: -1,
                                    from: -4
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
                                temperature_to: -3,
                                wind_speed: 1.6,
                                temperature_from: -4,
                                temperature_min: -4,
                                temp_avg: -3,
                                weather_type: 'Облачно, дождь со снегом',
                                humidity: 79,
                                pressure: {
                                    content: 751,
                                    units: 'mm'
                                },
                                temperature_max: -3,
                                'temperature-data': {
                                    to: -3,
                                    from: -4
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                type: 'evening',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                }
                            },
                            {
                                wind_speed: 1.5,
                                temperature: -6,
                                weather_type: 'Облачно, небольшой снег',
                                humidity: 83,
                                pressure: {
                                    content: 752,
                                    units: 'mm'
                                },
                                'temperature-data': {
                                    avg: {
                                        content: -6,
                                        bgcolor: 'eaedef'
                                    }
                                },
                                'image-v2': {
                                    content: 'ovc_-sn_-6'
                                },
                                'image-v3': {
                                    content: 'ovc_-sn'
                                },
                                type: 'night_short',
                                weather_condition: {
                                    code: 'overcast-and-light-snow'
                                }
                            },
                            {
                                wind_speed: 2.5,
                                temperature: -1,
                                weather_type: 'Облачно, дождь со снегом',
                                humidity: 80,
                                pressure: {
                                    content: 758,
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
                    uptime: '2017-02-28T07:00:00',
                    temperature: 1,
                    weather_type: 'Облачно',
                    wind_direction: 'se',
                    humidity: 67,
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
                        content: 'ovc_+2'
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
                current_hour: 11,
                counter_prefix: '/snippet/weather/',
                voiceInfo: {
                    ru: [
                        {
                            lang: 'ru-RU',
                            text: 'Сегодня в Москве 4°, Облачно. '
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
