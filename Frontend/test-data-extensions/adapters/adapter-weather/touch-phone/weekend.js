var stubs = require('@yandex-int/gemini-serp-stubs');

module.exports = {
    type: 'snippet',
    request_text: 'погода',
    data_stub: {
        num: 0,
        snippets: {
            full: {
                types: {
                    kind: 'wizard',
                    all: [
                        'snippets',
                        'weather'
                    ],
                    main: 'weather'
                },
                city_disambiguation: [],
                counter_prefix: '/snippet/weather/',
                forecast: [
                    {
                        hours: [
                            {
                                icon: 'ovc_ra_sn',
                                hour: '0',
                                feels_like_color: 'f0eff0',
                                temp_color: 'f0f0ec',
                                pressure_mm: 745,
                                feels_like: -2,
                                wind_gust: 3,
                                humidity: 100,
                                hour_ts: 1488747600,
                                pressure_pa: 994,
                                temp: 1,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.09,
                                wind_dir: 'n',
                                wind_speed: 1.1,
                                condition: 'overcast-and-wet-snow'
                            },
                            {
                                icon: 'ovc_ra_sn',
                                hour: '1',
                                feels_like_color: 'f0eff0',
                                temp_color: 'f0f0ec',
                                pressure_mm: 745,
                                feels_like: -3,
                                wind_gust: 4.2,
                                humidity: 100,
                                hour_ts: 1488751200,
                                pressure_pa: 994,
                                temp: 1,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.15,
                                wind_dir: 'ne',
                                wind_speed: 2,
                                condition: 'overcast-and-wet-snow'
                            },
                            {
                                icon: 'ovc_ra_sn',
                                hour: '2',
                                feels_like_color: 'f0eff0',
                                temp_color: 'f0f0ec',
                                pressure_mm: 745,
                                feels_like: -3,
                                wind_gust: 4,
                                humidity: 100,
                                hour_ts: 1488754800,
                                pressure_pa: 994,
                                temp: 1,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.47,
                                wind_dir: 'ne',
                                wind_speed: 2.1,
                                condition: 'overcast-and-wet-snow'
                            },
                            {
                                icon: 'skc_n',
                                hour: '3',
                                feels_like_color: 'f0eff0',
                                temp_color: 'f0eff0',
                                pressure_mm: 745,
                                feels_like: -2,
                                wind_gust: 1.5,
                                humidity: 100,
                                hour_ts: 1488758400,
                                pressure_pa: 994,
                                temp: 0,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0,
                                wind_dir: 's',
                                wind_speed: 0,
                                condition: 'clear'
                            },
                            {
                                icon: 'ovc',
                                hour: '4',
                                feels_like_color: 'f0eff0',
                                temp_color: 'f0eff0',
                                pressure_mm: 745,
                                feels_like: -3,
                                wind_gust: 3.4,
                                humidity: 100,
                                hour_ts: 1488762000,
                                pressure_pa: 994,
                                temp: 0,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0,
                                wind_dir: 'ne',
                                wind_speed: 1.8,
                                condition: 'overcast'
                            },
                            {
                                icon: 'ovc_sn',
                                hour: '5',
                                feels_like_color: 'eaedef',
                                temp_color: 'f0eff0',
                                pressure_mm: 745,
                                feels_like: -4,
                                wind_gust: 6.3,
                                humidity: 100,
                                hour_ts: 1488765600,
                                pressure_pa: 994,
                                temp: -1,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.57,
                                wind_dir: 'ne',
                                wind_speed: 2.5,
                                condition: 'cloudy-and-snow'
                            },
                            {
                                icon: 'ovc_-sn',
                                hour: '6',
                                feels_like_color: 'eaedef',
                                temp_color: 'f0eff0',
                                pressure_mm: 746,
                                feels_like: -5,
                                wind_gust: 6.9,
                                humidity: 89,
                                hour_ts: 1488769200,
                                pressure_pa: 995,
                                temp: -1,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.35,
                                wind_dir: 'ne',
                                wind_speed: 2.9,
                                condition: 'overcast-and-light-snow'
                            },
                            {
                                icon: 'ovc_sn',
                                hour: '7',
                                feels_like_color: 'eaedef',
                                temp_color: 'f0eff0',
                                pressure_mm: 745,
                                feels_like: -5,
                                wind_gust: 6.1,
                                humidity: 85,
                                hour_ts: 1488772800,
                                pressure_pa: 994,
                                temp: -1,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.68,
                                wind_dir: 'ne',
                                wind_speed: 2.7,
                                condition: 'cloudy-and-snow'
                            },
                            {
                                icon: 'ovc_-sn',
                                hour: '8',
                                feels_like_color: 'eaedef',
                                temp_color: 'f0eff0',
                                pressure_mm: 745,
                                feels_like: -5,
                                wind_gust: 6.2,
                                humidity: 82,
                                hour_ts: 1488776400,
                                pressure_pa: 994,
                                temp: -1,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.3,
                                wind_dir: 'ne',
                                wind_speed: 2.6,
                                condition: 'overcast-and-light-snow'
                            },
                            {
                                icon: 'ovc_ra_sn',
                                hour: '9',
                                feels_like_color: 'f0eff0',
                                temp_color: 'f0eff0',
                                pressure_mm: 746,
                                feels_like: -2,
                                wind_gust: 2.6,
                                humidity: 96,
                                hour_ts: 1488780000,
                                pressure_pa: 995,
                                temp: 0,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.05,
                                wind_dir: 's',
                                wind_speed: 0,
                                condition: 'overcast-and-wet-snow'
                            },
                            {
                                icon: 'ovc_ra_sn',
                                hour: '10',
                                feels_like_color: 'f0eff0',
                                temp_color: 'f0eff0',
                                pressure_mm: 746,
                                feels_like: -3,
                                wind_gust: 3.6,
                                humidity: 92,
                                hour_ts: 1488783600,
                                pressure_pa: 995,
                                temp: 0,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.4,
                                wind_dir: 'ne',
                                wind_speed: 2.1,
                                condition: 'overcast-and-wet-snow'
                            },
                            {
                                icon: 'ovc_ra_sn',
                                hour: '11',
                                feels_like_color: 'eaedef',
                                temp_color: 'f0eff0',
                                pressure_mm: 747,
                                feels_like: -4,
                                wind_gust: 5,
                                humidity: 91,
                                hour_ts: 1488787200,
                                pressure_pa: 996,
                                temp: 0,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.81,
                                wind_dir: 'ne',
                                wind_speed: 2.4,
                                condition: 'overcast-and-wet-snow'
                            },
                            {
                                icon: 'ovc_sn',
                                hour: '12',
                                feels_like_color: 'eaedef',
                                temp_color: 'f0eff0',
                                pressure_mm: 747,
                                feels_like: -4,
                                wind_gust: 5.4,
                                humidity: 89,
                                hour_ts: 1488790800,
                                pressure_pa: 996,
                                temp: 0,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 1.28,
                                wind_dir: 'ne',
                                wind_speed: 2.6,
                                condition: 'cloudy-and-snow'
                            },
                            {
                                icon: 'ovc_sn',
                                hour: '13',
                                feels_like_color: 'eaedef',
                                temp_color: 'f0eff0',
                                pressure_mm: 747,
                                feels_like: -5,
                                wind_gust: 6.1,
                                humidity: 87,
                                hour_ts: 1488794400,
                                pressure_pa: 996,
                                temp: -1,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.94,
                                wind_dir: 'ne',
                                wind_speed: 2.8,
                                condition: 'cloudy-and-snow'
                            },
                            {
                                icon: 'ovc_sn',
                                hour: '14',
                                feels_like_color: 'eaedef',
                                temp_color: 'f0eff0',
                                pressure_mm: 747,
                                feels_like: -5,
                                wind_gust: 5.4,
                                humidity: 87,
                                hour_ts: 1488798000,
                                pressure_pa: 996,
                                temp: 0,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.69,
                                wind_dir: 'ne',
                                wind_speed: 2.8,
                                condition: 'cloudy-and-snow'
                            },
                            {
                                icon: 'ovc_-ra',
                                hour: '15',
                                feels_like_color: 'eaedef',
                                temp_color: 'f0eff0',
                                pressure_mm: 747,
                                feels_like: -5,
                                _nowcast: true,
                                wind_gust: 5.8,
                                humidity: 87,
                                hour_ts: 1488801600,
                                pressure_pa: 996,
                                temp: 0,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.28,
                                wind_dir: 'ne',
                                wind_speed: 2.8,
                                condition: 'overcast-and-light-rain'
                            },
                            {
                                icon: 'ovc_ra_sn',
                                hour: '16',
                                feels_like_color: 'eaedef',
                                temp_color: 'f0eff0',
                                pressure_mm: 747,
                                feels_like: -5,
                                _nowcast: true,
                                wind_gust: 6,
                                humidity: 87,
                                hour_ts: 1488805200,
                                pressure_pa: 996,
                                temp: -1,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.08,
                                wind_dir: 'ne',
                                wind_speed: 2.7,
                                condition: 'overcast-and-wet-snow'
                            },
                            {
                                icon: 'ovc_ra_sn',
                                hour: '17',
                                feels_like_color: 'eaedef',
                                temp_color: 'f0eff0',
                                pressure_mm: 748,
                                feels_like: -5,
                                _nowcast: true,
                                wind_gust: 6.4,
                                humidity: 88,
                                hour_ts: 1488808800,
                                pressure_pa: 998,
                                temp: -1,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.05,
                                wind_dir: 'ne',
                                wind_speed: 2.6,
                                condition: 'overcast-and-wet-snow'
                            },
                            {
                                icon: 'ovc_-sn',
                                hour: '18',
                                feels_like_color: 'eaedef',
                                temp_color: 'f0eff0',
                                pressure_mm: 748,
                                feels_like: -5,
                                wind_gust: 7.4,
                                humidity: 91,
                                hour_ts: 1488812400,
                                pressure_pa: 998,
                                temp: -1,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.05,
                                wind_dir: 'ne',
                                wind_speed: 2.7,
                                condition: 'overcast-and-light-snow'
                            },
                            {
                                icon: 'ovc_sn',
                                hour: '19',
                                feels_like_color: 'eaedef',
                                temp_color: 'f0eff0',
                                pressure_mm: 748,
                                feels_like: -6,
                                wind_gust: 8.1,
                                humidity: 93,
                                hour_ts: 1488816000,
                                pressure_pa: 998,
                                temp: -1,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.79,
                                wind_dir: 'ne',
                                wind_speed: 2.9,
                                condition: 'cloudy-and-snow'
                            },
                            {
                                icon: 'ovc_sn',
                                hour: '20',
                                feels_like_color: 'eaedef',
                                temp_color: 'f0eff0',
                                pressure_mm: 748,
                                feels_like: -5,
                                wind_gust: 8.1,
                                humidity: 92,
                                hour_ts: 1488819600,
                                pressure_pa: 998,
                                temp: -1,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 1.8,
                                wind_dir: 'ne',
                                wind_speed: 3,
                                condition: 'cloudy-and-snow'
                            },
                            {
                                icon: 'ovc_sn',
                                hour: '21',
                                feels_like_color: 'eaedef',
                                temp_color: 'f0eff0',
                                pressure_mm: 748,
                                feels_like: -6,
                                wind_gust: 8.2,
                                humidity: 89,
                                hour_ts: 1488823200,
                                pressure_pa: 998,
                                temp: -1,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 1.05,
                                wind_dir: 'ne',
                                wind_speed: 3,
                                condition: 'cloudy-and-snow'
                            },
                            {
                                icon: 'ovc_sn',
                                hour: '22',
                                feels_like_color: 'eaedef',
                                temp_color: 'f0eff0',
                                pressure_mm: 748,
                                feels_like: -5,
                                wind_gust: 8,
                                humidity: 84,
                                hour_ts: 1488826800,
                                pressure_pa: 998,
                                temp: 0,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.71,
                                wind_dir: 'ne',
                                wind_speed: 3,
                                condition: 'cloudy-and-snow'
                            },
                            {
                                icon: 'ovc_-sn',
                                hour: '23',
                                feels_like_color: 'eaedef',
                                temp_color: 'f0eff0',
                                pressure_mm: 748,
                                feels_like: -5,
                                wind_gust: 7.2,
                                humidity: 84,
                                hour_ts: 1488830400,
                                pressure_pa: 998,
                                temp: -1,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.36,
                                wind_dir: 'e',
                                wind_speed: 2.8,
                                condition: 'overcast-and-light-snow'
                            }
                        ],
                        date: '2017-03-06T21:00:00.000Z',
                        parts: [
                            {
                                pressure: {
                                    content: 745,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'bkn_sn_n'
                                },
                                temp_avg: 0,
                                'temperature-data': {
                                    to: 1,
                                    from: -1
                                },
                                temperature_from: -1,
                                temperature_max: 1,
                                weather_type: 'Облачно с прояснениями, снег',
                                weather_condition: {
                                    code: 'cloudy-and-snow'
                                },
                                type: 'night',
                                temperature_min: -1,
                                temperature_to: 1,
                                wind_speed: 2.5,
                                humidity: 100
                            },
                            {
                                pressure: {
                                    content: 746,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'bkn_sn_d'
                                },
                                temp_avg: 0,
                                'temperature-data': {
                                    to: 0,
                                    from: -1
                                },
                                temperature_from: -1,
                                temperature_max: 0,
                                weather_type: 'Облачно с прояснениями, снег',
                                weather_condition: {
                                    code: 'cloudy-and-snow'
                                },
                                type: 'morning',
                                temperature_min: -1,
                                temperature_to: 0,
                                wind_speed: 2.9,
                                humidity: 89
                            },
                            {
                                pressure: {
                                    content: 747,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_sn'
                                },
                                temp_avg: 0,
                                'temperature-data': {
                                    to: 0,
                                    from: -1
                                },
                                temperature_from: -1,
                                temperature_max: 0,
                                weather_type: 'Облачно с прояснениями, снег',
                                weather_condition: {
                                    code: 'cloudy-and-snow'
                                },
                                type: 'day',
                                temperature_min: -1,
                                temperature_to: 0,
                                wind_speed: 2.8,
                                humidity: 88
                            },
                            {
                                pressure: {
                                    content: 748,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_sn'
                                },
                                temp_avg: 0,
                                'temperature-data': {
                                    to: 0,
                                    from: -1
                                },
                                temperature_from: -1,
                                temperature_max: 0,
                                weather_type: 'Облачно с прояснениями, снег',
                                weather_condition: {
                                    code: 'cloudy-and-snow'
                                },
                                type: 'evening',
                                temperature_min: -1,
                                temperature_to: 0,
                                wind_speed: 3,
                                humidity: 89
                            },
                            {
                                pressure: {
                                    content: 749,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_-sn'
                                },
                                'image-v2': {
                                    content: 'ovc_-sn_+0'
                                },
                                'temperature-data': {
                                    avg: {
                                        bgcolor: 'f0eff0',
                                        content: 0
                                    }
                                },
                                weather_type: 'Облачно, небольшой снег',
                                weather_condition: {
                                    code: 'overcast-and-light-snow'
                                },
                                type: 'night_short',
                                wind_speed: 2.6,
                                humidity: 81,
                                temperature: 0
                            },
                            {
                                pressure: {
                                    content: 747,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_sn'
                                },
                                'image-v2': {
                                    content: 'ovc_sn_+0'
                                },
                                'temperature-data': {
                                    avg: {
                                        bgcolor: 'f0eff0',
                                        content: 0
                                    }
                                },
                                weather_type: 'Облачно с прояснениями, снег',
                                weather_condition: {
                                    code: 'cloudy-and-snow'
                                },
                                type: 'day_short',
                                wind_speed: 2.9,
                                humidity: 89,
                                temperature: 0
                            }
                        ]
                    },
                    {
                        hours: [
                            {
                                icon: 'ovc_-sn',
                                hour: '0',
                                feels_like_color: 'eaedef',
                                temp_color: 'f0eff0',
                                pressure_mm: 748,
                                feels_like: -5,
                                wind_gust: 6.6,
                                humidity: 82,
                                hour_ts: 1488834000,
                                pressure_pa: 998,
                                temp: 0,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.05,
                                wind_dir: 'e',
                                wind_speed: 2.6,
                                condition: 'overcast-and-light-snow'
                            },
                            {
                                icon: 'ovc_-sn',
                                hour: '1',
                                feels_like_color: 'eaedef',
                                temp_color: 'f0eff0',
                                pressure_mm: 749,
                                feels_like: -5,
                                wind_gust: 6.8,
                                humidity: 82,
                                hour_ts: 1488837600,
                                pressure_pa: 999,
                                temp: 0,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.05,
                                wind_dir: 'e',
                                wind_speed: 2.6,
                                condition: 'overcast-and-light-snow'
                            },
                            {
                                icon: 'ovc_ra_sn',
                                hour: '2',
                                feels_like_color: 'eaedef',
                                temp_color: 'f0eff0',
                                pressure_mm: 749,
                                feels_like: -4,
                                wind_gust: 6.8,
                                humidity: 80,
                                hour_ts: 1488841200,
                                pressure_pa: 999,
                                temp: 0,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.05,
                                wind_dir: 'e',
                                wind_speed: 2.6,
                                condition: 'overcast-and-wet-snow'
                            },
                            {
                                icon: 'ovc_ra_sn',
                                hour: '3',
                                feels_like_color: 'eaedef',
                                temp_color: 'f0eff0',
                                pressure_mm: 749,
                                feels_like: -4,
                                wind_gust: 7.1,
                                humidity: 82,
                                hour_ts: 1488844800,
                                pressure_pa: 999,
                                temp: 0,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.05,
                                wind_dir: 'e',
                                wind_speed: 2.6,
                                condition: 'overcast-and-wet-snow'
                            },
                            {
                                icon: 'ovc_ra_sn',
                                hour: '4',
                                feels_like_color: 'eaedef',
                                temp_color: 'f0eff0',
                                pressure_mm: 749,
                                feels_like: -4,
                                wind_gust: 7,
                                humidity: 82,
                                hour_ts: 1488848400,
                                pressure_pa: 999,
                                temp: 0,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.05,
                                wind_dir: 'e',
                                wind_speed: 2.6,
                                condition: 'overcast-and-wet-snow'
                            },
                            {
                                icon: 'ovc_ra_sn',
                                hour: '5',
                                feels_like_color: 'f0eff0',
                                temp_color: 'f0f0ec',
                                pressure_mm: 750,
                                feels_like: -3,
                                wind_gust: 6.6,
                                humidity: 80,
                                hour_ts: 1488852000,
                                pressure_pa: 1000,
                                temp: 1,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.05,
                                wind_dir: 'e',
                                wind_speed: 2.5,
                                condition: 'overcast-and-wet-snow'
                            },
                            {
                                icon: 'ovc',
                                hour: '6',
                                feels_like_color: 'f0eff0',
                                temp_color: 'f0f0ec',
                                pressure_mm: 750,
                                feels_like: -3,
                                wind_gust: 6,
                                humidity: 82,
                                hour_ts: 1488855600,
                                pressure_pa: 1000,
                                temp: 1,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0,
                                wind_dir: 'e',
                                wind_speed: 2.4,
                                condition: 'overcast'
                            },
                            {
                                icon: 'ovc_ra_sn',
                                hour: '7',
                                feels_like_color: 'f0eff0',
                                temp_color: 'f0f0ec',
                                pressure_mm: 750,
                                feels_like: -3,
                                wind_gust: 6.5,
                                humidity: 84,
                                hour_ts: 1488859200,
                                pressure_pa: 1000,
                                temp: 1,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.05,
                                wind_dir: 'e',
                                wind_speed: 2.3,
                                condition: 'overcast-and-wet-snow'
                            },
                            {
                                icon: 'ovc_ra_sn',
                                hour: '8',
                                feels_like_color: 'f0eff0',
                                temp_color: 'f0f0ec',
                                pressure_mm: 750,
                                feels_like: -3,
                                wind_gust: 6.8,
                                humidity: 85,
                                hour_ts: 1488862800,
                                pressure_pa: 1000,
                                temp: 1,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.05,
                                wind_dir: 'e',
                                wind_speed: 2.4,
                                condition: 'overcast-and-wet-snow'
                            },
                            {
                                icon: 'ovc_ra_sn',
                                hour: '9',
                                feels_like_color: 'f0eff0',
                                temp_color: 'f0f0ec',
                                pressure_mm: 751,
                                feels_like: -3,
                                wind_gust: 6.9,
                                humidity: 87,
                                hour_ts: 1488866400,
                                pressure_pa: 1002,
                                temp: 1,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.05,
                                wind_dir: 'e',
                                wind_speed: 2.5,
                                condition: 'overcast-and-wet-snow'
                            },
                            {
                                icon: 'ovc_ra_sn',
                                hour: '10',
                                feels_like_color: 'f0eff0',
                                temp_color: 'f0f0ec',
                                pressure_mm: 751,
                                feels_like: -3,
                                wind_gust: 6.9,
                                humidity: 82,
                                hour_ts: 1488870000,
                                pressure_pa: 1002,
                                temp: 2,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.05,
                                wind_dir: 'e',
                                wind_speed: 2.7,
                                condition: 'overcast-and-wet-snow'
                            },
                            {
                                icon: 'ovc_-ra',
                                hour: '11',
                                feels_like_color: 'f0eff0',
                                temp_color: 'f0f0ec',
                                pressure_mm: 751,
                                feels_like: -2,
                                wind_gust: 7.7,
                                humidity: 80,
                                hour_ts: 1488873600,
                                pressure_pa: 1002,
                                temp: 2,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.05,
                                wind_dir: 'e',
                                wind_speed: 2.8,
                                condition: 'overcast-and-light-rain'
                            },
                            {
                                icon: 'ovc',
                                hour: '12',
                                feels_like_color: 'f0eff0',
                                temp_color: 'f0f0ec',
                                pressure_mm: 751,
                                feels_like: -2,
                                wind_gust: 7.5,
                                humidity: 82,
                                hour_ts: 1488877200,
                                pressure_pa: 1002,
                                temp: 2,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0,
                                wind_dir: 'se',
                                wind_speed: 3.1,
                                condition: 'overcast'
                            },
                            {
                                icon: 'ovc_-ra',
                                hour: '13',
                                feels_like_color: 'f0eff0',
                                temp_color: 'f1f0e9',
                                pressure_mm: 752,
                                feels_like: -1,
                                wind_gust: 6.5,
                                humidity: 81,
                                hour_ts: 1488880800,
                                pressure_pa: 1003,
                                temp: 3,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.05,
                                wind_dir: 'se',
                                wind_speed: 3,
                                condition: 'overcast-and-light-rain'
                            },
                            {
                                icon: 'ovc_-ra',
                                hour: '14',
                                feels_like_color: 'f0eff0',
                                temp_color: 'f1f0e9',
                                pressure_mm: 752,
                                feels_like: -1,
                                wind_gust: 6.5,
                                humidity: 85,
                                hour_ts: 1488884400,
                                pressure_pa: 1003,
                                temp: 3,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0.05,
                                wind_dir: 'se',
                                wind_speed: 2.9,
                                condition: 'overcast-and-light-rain'
                            },
                            {
                                icon: 'ovc',
                                hour: '15',
                                feels_like_color: 'f0eff0',
                                temp_color: 'f1f0e9',
                                pressure_mm: 752,
                                feels_like: 0,
                                wind_gust: 6.6,
                                humidity: 82,
                                hour_ts: 1488888000,
                                pressure_pa: 1003,
                                temp: 4,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0,
                                wind_dir: 'se',
                                wind_speed: 2.8,
                                condition: 'overcast'
                            },
                            {
                                icon: 'ovc',
                                hour: '16',
                                feels_like_color: 'f0eff0',
                                temp_color: 'f1f0e9',
                                pressure_mm: 752,
                                feels_like: 0,
                                wind_gust: 6.5,
                                humidity: 82,
                                hour_ts: 1488891600,
                                pressure_pa: 1003,
                                temp: 4,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0,
                                wind_dir: 'se',
                                wind_speed: 2.8,
                                condition: 'overcast'
                            },
                            {
                                icon: 'ovc',
                                hour: '17',
                                feels_like_color: 'f0eff0',
                                temp_color: 'f1f0e9',
                                pressure_mm: 753,
                                feels_like: 0,
                                wind_gust: 6.2,
                                humidity: 84,
                                hour_ts: 1488895200,
                                pressure_pa: 1004,
                                temp: 4,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0,
                                wind_dir: 'se',
                                wind_speed: 2.6,
                                condition: 'overcast'
                            },
                            {
                                icon: 'ovc',
                                hour: '18',
                                feels_like_color: 'f0eff0',
                                temp_color: 'f1f0e9',
                                pressure_mm: 753,
                                feels_like: 0,
                                wind_gust: 6,
                                humidity: 84,
                                hour_ts: 1488898800,
                                pressure_pa: 1004,
                                temp: 4,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0,
                                wind_dir: 'se',
                                wind_speed: 2.6,
                                condition: 'overcast'
                            },
                            {
                                icon: 'ovc',
                                hour: '19',
                                feels_like_color: 'f0eff0',
                                temp_color: 'f1f0e9',
                                pressure_mm: 753,
                                feels_like: -1,
                                wind_gust: 7.2,
                                humidity: 85,
                                hour_ts: 1488902400,
                                pressure_pa: 1004,
                                temp: 3,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0,
                                wind_dir: 'se',
                                wind_speed: 2.6,
                                condition: 'overcast'
                            },
                            {
                                icon: 'ovc',
                                hour: '20',
                                feels_like_color: 'f0eff0',
                                temp_color: 'f0f0ec',
                                pressure_mm: 754,
                                feels_like: -2,
                                wind_gust: 6.9,
                                humidity: 86,
                                hour_ts: 1488906000,
                                pressure_pa: 1006,
                                temp: 2,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0,
                                wind_dir: 'se',
                                wind_speed: 2.6,
                                condition: 'overcast'
                            },
                            {
                                icon: 'ovc',
                                hour: '21',
                                feels_like_color: 'f0eff0',
                                temp_color: 'f0f0ec',
                                pressure_mm: 754,
                                feels_like: -2,
                                wind_gust: 7.1,
                                humidity: 82,
                                hour_ts: 1488909600,
                                pressure_pa: 1006,
                                temp: 2,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0,
                                wind_dir: 'se',
                                wind_speed: 2.5,
                                condition: 'overcast'
                            },
                            {
                                icon: 'ovc',
                                hour: '22',
                                feels_like_color: 'f0eff0',
                                temp_color: 'f0f0ec',
                                pressure_mm: 755,
                                feels_like: -2,
                                wind_gust: 7.8,
                                humidity: 83,
                                hour_ts: 1488913200,
                                pressure_pa: 1007,
                                temp: 2,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0,
                                wind_dir: 'se',
                                wind_speed: 2.6,
                                condition: 'overcast'
                            },
                            {
                                icon: 'ovc',
                                hour: '23',
                                feels_like_color: 'f0eff0',
                                temp_color: 'f0f0ec',
                                pressure_mm: 755,
                                feels_like: -3,
                                wind_gust: 7.5,
                                humidity: 84,
                                hour_ts: 1488916800,
                                pressure_pa: 1007,
                                temp: 1,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0,
                                wind_dir: 'se',
                                wind_speed: 2.6,
                                condition: 'overcast'
                            }
                        ],
                        date: '2017-03-07T21:00:00.000Z',
                        parts: [
                            {
                                pressure: {
                                    content: 749,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_-sn'
                                },
                                temp_avg: 1,
                                'temperature-data': {
                                    to: 1,
                                    from: 0
                                },
                                temperature_from: 0,
                                temperature_max: 1,
                                weather_type: 'Облачно, небольшой снег',
                                weather_condition: {
                                    code: 'overcast-and-light-snow'
                                },
                                type: 'night',
                                temperature_min: 0,
                                temperature_to: 1,
                                wind_speed: 2.6,
                                humidity: 81
                            },
                            {
                                pressure: {
                                    content: 751,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                temp_avg: 2,
                                'temperature-data': {
                                    to: 2,
                                    from: 1
                                },
                                temperature_from: 1,
                                temperature_max: 2,
                                weather_type: 'Облачно, дождь со снегом',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                },
                                type: 'morning',
                                temperature_min: 1,
                                temperature_to: 2,
                                wind_speed: 2.8,
                                humidity: 83
                            },
                            {
                                pressure: {
                                    content: 752,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_-ra'
                                },
                                temp_avg: 3,
                                'temperature-data': {
                                    to: 4,
                                    from: 2
                                },
                                temperature_from: 2,
                                temperature_max: 4,
                                weather_type: 'Облачно, небольшой дождь',
                                weather_condition: {
                                    code: 'overcast-and-light-rain'
                                },
                                type: 'day',
                                temperature_min: 2,
                                temperature_to: 4,
                                wind_speed: 3.1,
                                humidity: 83
                            },
                            {
                                pressure: {
                                    content: 754,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc'
                                },
                                temp_avg: 3,
                                'temperature-data': {
                                    to: 4,
                                    from: 1
                                },
                                temperature_from: 1,
                                temperature_max: 4,
                                weather_type: 'Облачно',
                                weather_condition: {
                                    code: 'overcast'
                                },
                                type: 'evening',
                                temperature_min: 1,
                                temperature_to: 4,
                                wind_speed: 2.6,
                                humidity: 84
                            },
                            {
                                pressure: {
                                    content: 756,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc'
                                },
                                'image-v2': {
                                    content: 'ovc_-2'
                                },
                                'temperature-data': {
                                    avg: {
                                        bgcolor: 'f0eff0',
                                        content: -2
                                    }
                                },
                                weather_type: 'Облачно',
                                weather_condition: {
                                    code: 'overcast'
                                },
                                type: 'night_short',
                                wind_speed: 2.5,
                                humidity: 83,
                                temperature: -1
                            },
                            {
                                pressure: {
                                    content: 752,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                'image-v2': {
                                    content: 'ovc_ra_+4'
                                },
                                'temperature-data': {
                                    avg: {
                                        bgcolor: 'f1f0e9',
                                        content: 4
                                    }
                                },
                                weather_type: 'Облачно, дождь со снегом',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                },
                                type: 'day_short',
                                wind_speed: 3.1,
                                humidity: 83,
                                temperature: 4
                            }
                        ]
                    },
                    {
                        hours: [
                            {
                                icon: 'ovc',
                                hour: '0',
                                feels_like_color: 'f0eff0',
                                temp_color: 'f0f0ec',
                                pressure_mm: 755,
                                feels_like: -3,
                                wind_gust: 6.6,
                                humidity: 83,
                                hour_ts: 1488920400,
                                pressure_pa: 1007,
                                temp: 1,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0,
                                wind_dir: 'se',
                                wind_speed: 2.5,
                                condition: 'overcast'
                            },
                            {
                                icon: 'ovc',
                                hour: '1',
                                feels_like_color: 'eaedef',
                                temp_color: 'f0eff0',
                                pressure_mm: 756,
                                feels_like: -4,
                                wind_gust: 6.6,
                                humidity: 84,
                                hour_ts: 1488924000,
                                pressure_pa: 1008,
                                temp: 0,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0,
                                wind_dir: 'se',
                                wind_speed: 2.5,
                                condition: 'overcast'
                            },
                            {
                                icon: 'ovc',
                                hour: '2',
                                feels_like_color: 'eaedef',
                                temp_color: 'f0eff0',
                                pressure_mm: 756,
                                feels_like: -4,
                                wind_gust: 5.8,
                                humidity: 84,
                                hour_ts: 1488927600,
                                pressure_pa: 1008,
                                temp: 0,
                                _fallback_temp: false,
                                prec_period: 60,
                                _fallback_prec: false,
                                prec_mm: 0,
                                wind_dir: 'se',
                                wind_speed: 2.4,
                                condition: 'overcast'
                            }
                        ],
                        date: '2017-03-08T21:00:00.000Z',
                        parts: [
                            {
                                pressure: {
                                    content: 756,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc'
                                },
                                temp_avg: 0,
                                'temperature-data': {
                                    to: 1,
                                    from: -1
                                },
                                temperature_from: -1,
                                temperature_max: 1,
                                weather_type: 'Облачно',
                                weather_condition: {
                                    code: 'overcast'
                                },
                                type: 'night',
                                temperature_min: -1,
                                temperature_to: 1,
                                wind_speed: 2.5,
                                humidity: 83
                            },
                            {
                                pressure: {
                                    content: 758,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc'
                                },
                                temp_avg: -1,
                                'temperature-data': {
                                    to: -1,
                                    from: -2
                                },
                                temperature_from: -2,
                                temperature_max: -1,
                                weather_type: 'Облачно',
                                weather_condition: {
                                    code: 'overcast'
                                },
                                type: 'morning',
                                temperature_min: -2,
                                temperature_to: -1,
                                wind_speed: 2.2,
                                humidity: 95
                            },
                            {
                                pressure: {
                                    content: 759,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc'
                                },
                                temp_avg: 2,
                                'temperature-data': {
                                    to: 3,
                                    from: 1
                                },
                                temperature_from: 1,
                                temperature_max: 3,
                                weather_type: 'Облачно',
                                weather_condition: {
                                    code: 'overcast'
                                },
                                type: 'day',
                                temperature_min: 1,
                                temperature_to: 3,
                                wind_speed: 2.6,
                                humidity: 69
                            },
                            {
                                pressure: {
                                    content: 758,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc'
                                },
                                temp_avg: 3,
                                'temperature-data': {
                                    to: 4,
                                    from: 2
                                },
                                temperature_from: 2,
                                temperature_max: 4,
                                weather_type: 'Облачно',
                                weather_condition: {
                                    code: 'overcast'
                                },
                                type: 'evening',
                                temperature_min: 2,
                                temperature_to: 4,
                                wind_speed: 2,
                                humidity: 100
                            },
                            {
                                pressure: {
                                    content: 758,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc'
                                },
                                'image-v2': {
                                    content: 'ovc_+0'
                                },
                                'temperature-data': {
                                    avg: {
                                        bgcolor: 'f0eff0',
                                        content: 0
                                    }
                                },
                                weather_type: 'Облачно',
                                weather_condition: {
                                    code: 'overcast'
                                },
                                type: 'night_short',
                                wind_speed: 1.9,
                                humidity: 100,
                                temperature: 0
                            },
                            {
                                pressure: {
                                    content: 759,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc'
                                },
                                'image-v2': {
                                    content: 'ovc_+4'
                                },
                                'temperature-data': {
                                    avg: {
                                        bgcolor: 'f1f0e9',
                                        content: 4
                                    }
                                },
                                weather_type: 'Облачно',
                                weather_condition: {
                                    code: 'overcast'
                                },
                                type: 'day_short',
                                wind_speed: 2.6,
                                humidity: 82,
                                temperature: 3
                            }
                        ]
                    },
                    {
                        hours: [],
                        date: '2017-03-09T21:00:00.000Z',
                        parts: [
                            {
                                pressure: {
                                    content: 758,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc'
                                },
                                temp_avg: 1,
                                'temperature-data': {
                                    to: 1,
                                    from: 0
                                },
                                temperature_from: 0,
                                temperature_max: 1,
                                weather_type: 'Облачно',
                                weather_condition: {
                                    code: 'overcast'
                                },
                                type: 'night',
                                temperature_min: 0,
                                temperature_to: 1,
                                wind_speed: 1.9,
                                humidity: 100
                            },
                            {
                                pressure: {
                                    content: 758,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                temp_avg: 0,
                                'temperature-data': {
                                    to: 1,
                                    from: -1
                                },
                                temperature_from: -1,
                                temperature_max: 1,
                                weather_type: 'Облачно, дождь со снегом',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                },
                                type: 'morning',
                                temperature_min: -1,
                                temperature_to: 1,
                                wind_speed: 1.8,
                                humidity: 100
                            },
                            {
                                pressure: {
                                    content: 756,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc'
                                },
                                temp_avg: 5,
                                'temperature-data': {
                                    to: 6,
                                    from: 4
                                },
                                temperature_from: 4,
                                temperature_max: 6,
                                weather_type: 'Облачно',
                                weather_condition: {
                                    code: 'overcast'
                                },
                                type: 'day',
                                temperature_min: 4,
                                temperature_to: 6,
                                wind_speed: 2.2,
                                humidity: 71
                            },
                            {
                                pressure: {
                                    content: 754,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc'
                                },
                                temp_avg: 5,
                                'temperature-data': {
                                    to: 6,
                                    from: 4
                                },
                                temperature_from: 4,
                                temperature_max: 6,
                                weather_type: 'Облачно',
                                weather_condition: {
                                    code: 'overcast'
                                },
                                type: 'evening',
                                temperature_min: 4,
                                temperature_to: 6,
                                wind_speed: 2.4,
                                humidity: 99
                            },
                            {
                                pressure: {
                                    content: 752,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc'
                                },
                                'image-v2': {
                                    content: 'ovc_+2'
                                },
                                'temperature-data': {
                                    avg: {
                                        bgcolor: 'f0f0ec',
                                        content: 2
                                    }
                                },
                                weather_type: 'Облачно',
                                weather_condition: {
                                    code: 'overcast'
                                },
                                type: 'night_short',
                                wind_speed: 2.6,
                                humidity: 100,
                                temperature: 2
                            },
                            {
                                pressure: {
                                    content: 757,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                'image-v2': {
                                    content: 'ovc_ra_+6'
                                },
                                'temperature-data': {
                                    avg: {
                                        bgcolor: 'f2f0e6',
                                        content: 6
                                    }
                                },
                                weather_type: 'Облачно, дождь со снегом',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                },
                                type: 'day_short',
                                wind_speed: 2.2,
                                humidity: 85,
                                temperature: 6
                            }
                        ]
                    },
                    {
                        hours: [],
                        date: '2017-03-10T21:00:00.000Z',
                        parts: [
                            {
                                pressure: {
                                    content: 752,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc'
                                },
                                temp_avg: 3,
                                'temperature-data': {
                                    to: 3,
                                    from: 2
                                },
                                temperature_from: 2,
                                temperature_max: 3,
                                weather_type: 'Облачно',
                                weather_condition: {
                                    code: 'overcast'
                                },
                                type: 'night',
                                temperature_min: 2,
                                temperature_to: 3,
                                wind_speed: 2.6,
                                humidity: 100
                            },
                            {
                                pressure: {
                                    content: 750,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                temp_avg: 2,
                                'temperature-data': {
                                    to: 2,
                                    from: 1
                                },
                                temperature_from: 1,
                                temperature_max: 2,
                                weather_type: 'Облачно, дождь со снегом',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                },
                                type: 'morning',
                                temperature_min: 1,
                                temperature_to: 2,
                                wind_speed: 1.9,
                                humidity: 89
                            },
                            {
                                pressure: {
                                    content: 747,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_-ra'
                                },
                                temp_avg: 5,
                                'temperature-data': {
                                    to: 6,
                                    from: 4
                                },
                                temperature_from: 4,
                                temperature_max: 6,
                                weather_type: 'Облачно, небольшой дождь',
                                weather_condition: {
                                    code: 'overcast-and-light-rain'
                                },
                                type: 'day',
                                temperature_min: 4,
                                temperature_to: 6,
                                wind_speed: 3,
                                humidity: 67
                            },
                            {
                                pressure: {
                                    content: 743,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_-ra'
                                },
                                temp_avg: 5,
                                'temperature-data': {
                                    to: 6,
                                    from: 4
                                },
                                temperature_from: 4,
                                temperature_max: 6,
                                weather_type: 'Облачно, небольшой дождь',
                                weather_condition: {
                                    code: 'overcast-and-light-rain'
                                },
                                type: 'evening',
                                temperature_min: 4,
                                temperature_to: 6,
                                wind_speed: 3.4,
                                humidity: 98
                            },
                            {
                                pressure: {
                                    content: 739,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                'image-v2': {
                                    content: 'ovc_ra_+2'
                                },
                                'temperature-data': {
                                    avg: {
                                        bgcolor: 'f0f0ec',
                                        content: 2
                                    }
                                },
                                weather_type: 'Облачно, дождь со снегом',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                },
                                type: 'night_short',
                                wind_speed: 2.7,
                                humidity: 100,
                                temperature: 2
                            },
                            {
                                pressure: {
                                    content: 749,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                'image-v2': {
                                    content: 'ovc_ra_+6'
                                },
                                'temperature-data': {
                                    avg: {
                                        bgcolor: 'f2f0e6',
                                        content: 6
                                    }
                                },
                                weather_type: 'Облачно, дождь со снегом',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                },
                                type: 'day_short',
                                wind_speed: 3,
                                humidity: 78,
                                temperature: 6
                            }
                        ]
                    },
                    {
                        hours: [],
                        date: '2017-03-11T21:00:00.000Z',
                        parts: [
                            {
                                pressure: {
                                    content: 739,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                temp_avg: 3,
                                'temperature-data': {
                                    to: 4,
                                    from: 2
                                },
                                temperature_from: 2,
                                temperature_max: 4,
                                weather_type: 'Облачно, дождь со снегом',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                },
                                type: 'night',
                                temperature_min: 2,
                                temperature_to: 4,
                                wind_speed: 2.7,
                                humidity: 100
                            },
                            {
                                pressure: {
                                    content: 735,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                temp_avg: 2,
                                'temperature-data': {
                                    to: 3,
                                    from: 1
                                },
                                temperature_from: 1,
                                temperature_max: 3,
                                weather_type: 'Облачно, дождь со снегом',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                },
                                type: 'morning',
                                temperature_min: 1,
                                temperature_to: 3,
                                wind_speed: 2.3,
                                humidity: 97
                            },
                            {
                                pressure: {
                                    content: 732,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_-ra'
                                },
                                temp_avg: 4,
                                'temperature-data': {
                                    to: 5,
                                    from: 2
                                },
                                temperature_from: 2,
                                temperature_max: 5,
                                weather_type: 'Облачно, небольшой дождь',
                                weather_condition: {
                                    code: 'overcast-and-light-rain'
                                },
                                type: 'day',
                                temperature_min: 2,
                                temperature_to: 5,
                                wind_speed: 2.3,
                                humidity: 70
                            },
                            {
                                pressure: {
                                    content: 731,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                temp_avg: 4,
                                'temperature-data': {
                                    to: 4,
                                    from: 3
                                },
                                temperature_from: 3,
                                temperature_max: 4,
                                weather_type: 'Облачно, дождь со снегом',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                },
                                type: 'evening',
                                temperature_min: 3,
                                temperature_to: 4,
                                wind_speed: 2.5,
                                humidity: 77
                            },
                            {
                                pressure: {
                                    content: 731,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                'image-v2': {
                                    content: 'ovc_ra_+2'
                                },
                                'temperature-data': {
                                    avg: {
                                        bgcolor: 'f0f0ec',
                                        content: 2
                                    }
                                },
                                weather_type: 'Облачно, дождь со снегом',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                },
                                type: 'night_short',
                                wind_speed: 2.3,
                                humidity: 80,
                                temperature: 2
                            },
                            {
                                pressure: {
                                    content: 734,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                'image-v2': {
                                    content: 'ovc_ra_+6'
                                },
                                'temperature-data': {
                                    avg: {
                                        bgcolor: 'f2f0e6',
                                        content: 6
                                    }
                                },
                                weather_type: 'Облачно, дождь со снегом',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                },
                                type: 'day_short',
                                wind_speed: 2.3,
                                humidity: 83,
                                temperature: 5
                            }
                        ],
                        type: 'by_day'
                    },
                    {
                        hours: [],
                        date: '2017-03-12T21:00:00.000Z',
                        parts: [
                            {
                                pressure: {
                                    content: 731,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                temp_avg: 3,
                                'temperature-data': {
                                    to: 3,
                                    from: 2
                                },
                                temperature_from: 2,
                                temperature_max: 3,
                                weather_type: 'Облачно, дождь со снегом',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                },
                                type: 'night',
                                temperature_min: 2,
                                temperature_to: 3,
                                wind_speed: 2.3,
                                humidity: 80
                            },
                            {
                                pressure: {
                                    content: 743,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_-ra'
                                },
                                temp_avg: 3,
                                'temperature-data': {
                                    to: 4,
                                    from: 2
                                },
                                temperature_from: 2,
                                temperature_max: 4,
                                weather_type: 'Облачно, небольшой дождь',
                                weather_condition: {
                                    code: 'overcast-and-light-rain'
                                },
                                type: 'morning',
                                temperature_min: 2,
                                temperature_to: 4,
                                wind_speed: 2.8,
                                humidity: 97
                            },
                            {
                                pressure: {
                                    content: 744,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_-ra'
                                },
                                temp_avg: 5,
                                'temperature-data': {
                                    to: 6,
                                    from: 4
                                },
                                temperature_from: 4,
                                temperature_max: 6,
                                weather_type: 'Облачно, небольшой дождь',
                                weather_condition: {
                                    code: 'overcast-and-light-rain'
                                },
                                type: 'day',
                                temperature_min: 4,
                                temperature_to: 6,
                                wind_speed: 3.2,
                                humidity: 88
                            },
                            {
                                pressure: {
                                    content: 745,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                temp_avg: 5,
                                'temperature-data': {
                                    to: 6,
                                    from: 4
                                },
                                temperature_from: 4,
                                temperature_max: 6,
                                weather_type: 'Облачно, дождь со снегом',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                },
                                type: 'evening',
                                temperature_min: 4,
                                temperature_to: 6,
                                wind_speed: 2.1,
                                humidity: 100
                            },
                            {
                                pressure: {
                                    content: 746,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                'image-v2': {
                                    content: 'ovc_ra_+2'
                                },
                                'temperature-data': {
                                    avg: {
                                        bgcolor: 'f0f0ec',
                                        content: 2
                                    }
                                },
                                weather_type: 'Облачно, дождь со снегом',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                },
                                type: 'night_short',
                                wind_speed: 1.5,
                                humidity: 100,
                                temperature: 2
                            },
                            {
                                pressure: {
                                    content: 744,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_-ra'
                                },
                                'image-v2': {
                                    content: 'ovc_-ra_+6'
                                },
                                'temperature-data': {
                                    avg: {
                                        bgcolor: 'f2f0e6',
                                        content: 6
                                    }
                                },
                                weather_type: 'Облачно, небольшой дождь',
                                weather_condition: {
                                    code: 'overcast-and-light-rain'
                                },
                                type: 'day_short',
                                wind_speed: 3.2,
                                humidity: 93,
                                temperature: 6
                            }
                        ],
                        type: 'by_day'
                    },
                    {
                        hours: [],
                        date: '2017-03-13T21:00:00.000Z',
                        parts: [
                            {
                                pressure: {
                                    content: 746,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                temp_avg: 3,
                                'temperature-data': {
                                    to: 4,
                                    from: 2
                                },
                                temperature_from: 2,
                                temperature_max: 4,
                                weather_type: 'Облачно, дождь со снегом',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                },
                                type: 'night',
                                temperature_min: 2,
                                temperature_to: 4,
                                wind_speed: 1.5,
                                humidity: 100
                            },
                            {
                                pressure: {
                                    content: 748,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                temp_avg: 3,
                                'temperature-data': {
                                    to: 3,
                                    from: 2
                                },
                                temperature_from: 2,
                                temperature_max: 3,
                                weather_type: 'Облачно, дождь со снегом',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                },
                                type: 'morning',
                                temperature_min: 2,
                                temperature_to: 3,
                                wind_speed: 1.4,
                                humidity: 100
                            },
                            {
                                pressure: {
                                    content: 749,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_-ra'
                                },
                                temp_avg: 4,
                                'temperature-data': {
                                    to: 5,
                                    from: 3
                                },
                                temperature_from: 3,
                                temperature_max: 5,
                                weather_type: 'Облачно, небольшой дождь',
                                weather_condition: {
                                    code: 'overcast-and-light-rain'
                                },
                                type: 'day',
                                temperature_min: 3,
                                temperature_to: 5,
                                wind_speed: 1.1,
                                humidity: 85
                            },
                            {
                                pressure: {
                                    content: 751,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                temp_avg: 4,
                                'temperature-data': {
                                    to: 4,
                                    from: 3
                                },
                                temperature_from: 3,
                                temperature_max: 4,
                                weather_type: 'Облачно, дождь со снегом',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                },
                                type: 'evening',
                                temperature_min: 3,
                                temperature_to: 4,
                                wind_speed: 2.5,
                                humidity: 100
                            },
                            {
                                pressure: {
                                    content: 753,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                'image-v2': {
                                    content: 'ovc_ra_+2'
                                },
                                'temperature-data': {
                                    avg: {
                                        bgcolor: 'f0f0ec',
                                        content: 2
                                    }
                                },
                                weather_type: 'Облачно, дождь со снегом',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                },
                                type: 'night_short',
                                wind_speed: 2.9,
                                humidity: 80,
                                temperature: 1
                            },
                            {
                                pressure: {
                                    content: 749,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                'image-v2': {
                                    content: 'ovc_ra_+6'
                                },
                                'temperature-data': {
                                    avg: {
                                        bgcolor: 'f2f0e6',
                                        content: 6
                                    }
                                },
                                weather_type: 'Облачно, дождь со снегом',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                },
                                type: 'day_short',
                                wind_speed: 1.4,
                                humidity: 92,
                                temperature: 5
                            }
                        ]
                    },
                    {
                        hours: [],
                        date: '2017-03-14T21:00:00.000Z',
                        parts: [
                            {
                                pressure: {
                                    content: 753,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                temp_avg: 2,
                                'temperature-data': {
                                    to: 2,
                                    from: 1
                                },
                                temperature_from: 1,
                                temperature_max: 2,
                                weather_type: 'Облачно, дождь со снегом',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                },
                                type: 'night',
                                temperature_min: 1,
                                temperature_to: 2,
                                wind_speed: 2.9,
                                humidity: 80
                            },
                            {
                                pressure: {
                                    content: 755,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                temp_avg: 2,
                                'temperature-data': {
                                    to: 2,
                                    from: 1
                                },
                                temperature_from: 1,
                                temperature_max: 2,
                                weather_type: 'Облачно, дождь со снегом',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                },
                                type: 'morning',
                                temperature_min: 1,
                                temperature_to: 2,
                                wind_speed: 3.6,
                                humidity: 48
                            },
                            {
                                pressure: {
                                    content: 756,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_-ra'
                                },
                                temp_avg: 3,
                                'temperature-data': {
                                    to: 4,
                                    from: 2
                                },
                                temperature_from: 2,
                                temperature_max: 4,
                                weather_type: 'Облачно, небольшой дождь',
                                weather_condition: {
                                    code: 'overcast-and-light-rain'
                                },
                                type: 'day',
                                temperature_min: 2,
                                temperature_to: 4,
                                wind_speed: 4,
                                humidity: 37
                            },
                            {
                                pressure: {
                                    content: 758,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                temp_avg: 3,
                                'temperature-data': {
                                    to: 4,
                                    from: 2
                                },
                                temperature_from: 2,
                                temperature_max: 4,
                                weather_type: 'Облачно, дождь со снегом',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                },
                                type: 'evening',
                                temperature_min: 2,
                                temperature_to: 4,
                                wind_speed: 3.4,
                                humidity: 72
                            },
                            {
                                pressure: {
                                    content: 759,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_-sn'
                                },
                                'image-v2': {
                                    content: 'ovc_-sn_+0'
                                },
                                'temperature-data': {
                                    avg: {
                                        bgcolor: 'f0eff0',
                                        content: 0
                                    }
                                },
                                weather_type: 'Облачно, небольшой снег',
                                weather_condition: {
                                    code: 'overcast-and-light-snow'
                                },
                                type: 'night_short',
                                wind_speed: 3.4,
                                humidity: 51,
                                temperature: 0
                            },
                            {
                                pressure: {
                                    content: 756,
                                    units: 'mm'
                                },
                                'image-v3': {
                                    content: 'ovc_ra'
                                },
                                'image-v2': {
                                    content: 'ovc_ra_+4'
                                },
                                'temperature-data': {
                                    avg: {
                                        bgcolor: 'f1f0e9',
                                        content: 4
                                    }
                                },
                                weather_type: 'Облачно, дождь со снегом',
                                weather_condition: {
                                    code: 'overcast-and-wet-snow'
                                },
                                type: 'day_short',
                                wind_speed: 4,
                                humidity: 42,
                                temperature: 4
                            }
                        ]
                    }
                ],
                current_hour: 15,
                city: stubs.moscowStub(),
                applicable: 1,
                slot_rank: 0,
                current: {
                    wind_direction: 'ne',
                    pressure: {
                        content: 747,
                        units: 'mm'
                    },
                    'image-v3': {
                        content: 'ovc'
                    },
                    'image-v2': {
                        content: 'ovc_-2'
                    },
                    'temperature-data': {
                        avg: {
                            bgcolor: 'f0eff0',
                            content: -2
                        }
                    },
                    weather_type: 'Облачно',
                    uptime: '2017-03-06T12:00:00',
                    weather_condition: {
                        code: 'overcast'
                    },
                    wind_speed: 2,
                    humidity: 92,
                    temperature: -1
                },
                serp_info: {
                    format: 'json',
                    type: 'weather',
                    slot: 'full',
                    flat: true
                },
                slot: 'full',
                link: 'https://yandex.ru/pogoda/moscow',
                city_id: 213,
                template: 'weather',
                cityid: 'moscow',
                data: {},
                weather_link: 'https://yandex.ru/pogoda/moscow',
                get_wind_type: null,
                type: 'weather'
            }
        },
        size: 0
    }
};
