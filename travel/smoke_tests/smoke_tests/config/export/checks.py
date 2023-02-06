# coding: utf8
from travel.rasp.smoke_tests.smoke_tests.stableness import StablenessVariants
from travel.rasp.smoke_tests.smoke_tests.common_content_checkers import MinItemsCount, HasItem
from travel.rasp.smoke_tests.smoke_tests.config.export.content_checkers import (
    SearchPlatformsPercent, check_threads, SuburbanSellingCheck, check_train_selling_check
)
from travel.rasp.smoke_tests.smoke_tests.config.export.env import env

selling_params = '&selling_version=3&selling_flows=simple&selling_flows=validator&selling_flows=aeroexpress' \
                 '&selling_barcode_presets=PDF417_cppk&selling_barcode_presets=PDF417_szppk' \
                 '&selling_barcode_presets=Aztec_mtppk'


urls = [    # ----- Разные ручки -----
    'ping',
    'version',
    'robots.txt',
    'static/payment_completed/',
    'v3/suburban/settings/',

    ['dev/current_events', {'timeout': env.timeout_very_slow}],

    # ----- Пригородные зоны -----

    [
        'v3/suburban/zones/?lang=ru_RU&use_directions=true',
        {'processes': [MinItemsCount(100, 'suburban_zones')]}
    ],
    [
        'v3/suburban/zone/1',
        {'processes': [MinItemsCount(100, 'zone_stations')]}
    ],

    # ----- Поиск -----

    # Белорусский вокзал - Шереметьево на сегодня, динамические платформы
    [
        'v3/suburban/search_on_date?station_from=198230&station_to=321&date=today',
        {
            'name': 'Search with dynamical platforms',
            'processes': [SearchPlatformsPercent(1)]
        }
    ],

    # Екатеринбург - Нижний Тагил на завтра
    [
        'v3/suburban/search_on_date?city_from=54&city_to=11168&date={msk_tomorrow}&days_ahead=3&lang=ru_RU'
        + selling_params,
        {'processes': [
            MinItemsCount(3, 'days'), MinItemsCount(1, 'tariffs'),
            HasItem('settings'), HasItem('date_time'), HasItem('sup_tags')
        ]}
    ],

    # Москва - Нахабино на все дни
    [
        'v3/suburban/search?city_from=213&station_to=196606',
        {'processes': [
            MinItemsCount(50, 'segments'), MinItemsCount(20, 'tariffs'),
            HasItem('settings'), HasItem('date_time')
        ]}
    ],

    # Подольск - Москва на все дни
    [
        'v3/suburban/search?station_from=190900&city_to=213&lang=ru_RU&add_subtypes=true',
        {'processes': [MinItemsCount(50, 'segments'), MinItemsCount(20, 'tariffs')]}
    ],

    # ----- Поиск с тарифами продаж -----

    # АЭ. Белорусский вокзал - Шереметьево
    [
        'v3/suburban/search_on_date?station_from=198230&station_to=321&date=today&days_ahead=2&lang=ru_RU'
        + selling_params,
        {
            'processes': [SuburbanSellingCheck(days=2, segments=15, selling_tariffs=2, selling_partners=1)],
            'stableness': StablenessVariants.TESTING_UNSTABLE
        }
    ],

    # АЭ и Мовиста. а/п Домодедово - Павелецкий вокзал
    [
        'v3/suburban/search_on_date?station_from=193114&station_to=193519&date=today&days_ahead=2&lang=ru_RU'
        + selling_params,
        {
            'processes': [SuburbanSellingCheck(days=2, segments=30, selling_tariffs=4, selling_partners=2)],
            'stableness': StablenessVariants.TESTING_UNSTABLE
        }
    ],

    # Мовиста. Москва - Одинцово
    [
        'v3/suburban/search_on_date?city_from=213&station_to=182209&date={msk_today}&days_ahead=2'
        + selling_params,
        {
            'processes': [SuburbanSellingCheck(days=2, segments=50, selling_tariffs=4, selling_partners=1)],
            'stableness': StablenessVariants.TESTING_UNSTABLE
        }
    ],

    # Мовиста. Курский вокзал - Подольск
    [
        'v3/suburban/search_on_date?station_from=191602&station_to=190900&date=today&days_ahead=1'
        '&lang=ru_RU&add_subtypes=true&transfers=auto' + selling_params,
        {
            'processes': [SuburbanSellingCheck(days=1, segments=50, selling_tariffs=2, selling_partners=1)],
            'stableness': StablenessVariants.TESTING_UNSTABLE
        }
    ],

    # Мовиста. Антоновка - Загорново
    [
        'v3/suburban/search_on_date?station_from=193928&station_to=194935&date=today&days_ahead=2'
        '&lang=ru_RU&add_subtypes=true&transfers=all' + selling_params,
        {
            'processes': [SuburbanSellingCheck(days=2, segments=15, selling_tariffs=2, selling_partners=1)],
            'stableness': StablenessVariants.TESTING_UNSTABLE
        }
    ],

    # ИМ. СЗППК. Санкт-Петербург - Выборг
    [
        'v3/suburban/search_on_date?city_from=2&city_to=969&date={msk_tomorrow}&days_ahead=2&lang=ru_RU'
        + selling_params,
        {
            'processes': [SuburbanSellingCheck(days=2, segments=5, selling_tariffs=4, selling_partners=1)],
            'stableness': StablenessVariants.TESTING_UNSTABLE
        }
    ],

    # ИМ. СЗППК. Витебский вокзал - Павловск
    [
        'v3/suburban/search_on_date?station_from=033061&station_to=032508'
        '&date={msk_tomorrow}&days_ahead=2&lang=ru_RU'
        + selling_params,
        {
            'processes': [SuburbanSellingCheck(days=2, segments=20, selling_tariffs=2, selling_partners=1)],
            'stableness': StablenessVariants.TESTING_UNSTABLE
        }
    ],

    # ИМ. СЗППК. Мга - Санкт-Петербург
    [
        'v3/suburban/search_on_date?station_from=030203&city_to=2&date={msk_tomorrow}&days_ahead=2&lang=ru_RU'
        + selling_params,
        {
            'processes': [SuburbanSellingCheck(days=2, segments=15, selling_tariffs=8, selling_partners=1)],
            'stableness': StablenessVariants.TESTING_UNSTABLE
        }
    ],

    # ИМ. МТППК. Тверь - Ленинградский вокзал
    [
        'v3/suburban/search_on_date?station_from=061502&station_to=060073&date={msk_tomorrow}&days_ahead=2'
        + selling_params,
        {
            'processes': [SuburbanSellingCheck(days=2, segments=10, selling_tariffs=4, selling_partners=1)],
            'stableness': StablenessVariants.TESTING_UNSTABLE
        }
    ],

    # ИМ. МТППК. Москва - Химки
    [
        'v3/suburban/search_on_date?city_from=213&city_to=10758&date={msk_tomorrow}&days_ahead=2'
        + selling_params,
        {
            'processes': [SuburbanSellingCheck(days=2, segments=40, selling_tariffs=2, selling_partners=1)],
            'stableness': StablenessVariants.TESTING_UNSTABLE,
            'timeout': env.timeout_slow,
        }
    ],

    # Мовиста. Не указан selling_flow=validator
    [
        'v3/suburban/search_on_date?city_from=213&station_to=182209&date=today&days_ahead=2&selling_version=3',
        {'processes': [SuburbanSellingCheck(days=2, segments=50, selling_tariffs=0, selling_partners=0)]}
    ],

    # Мовиста. Не указан selling_barcode_presets=PDF417_cppk
    [
        'v3/suburban/search_on_date?city_from=213&station_to=182209&date=today&days_ahead=2&selling_version=3'
        '&selling_version=3&selling_flows=simple&selling_flows=validator&selling_flows=aeroexpress'
        '&selling_barcode_presets=PDF417_szppk&selling_barcode_presets=Aztec_mtppk',
        {
            'processes': [SuburbanSellingCheck(days=2, segments=50, selling_tariffs=0, selling_partners=0)],
            'stableness': StablenessVariants.TESTING_UNSTABLE
        }
    ],

    # ИМ. Не указан selling_flow=simple
    [
        'v3/suburban/search_on_date?station_from=031812&station_to=030006&date=today&days_ahead=2'
        '&selling_version=3&selling_flows=validator&selling_flows=aeroexpress',
        {'processes': [SuburbanSellingCheck(days=2, segments=20, selling_tariffs=0, selling_partners=0)]}
    ],

    # ИМ. СЗППК. Не указан selling_barcode_presets=PDF417_szppk
    [
        'v3/suburban/search_on_date?station_from=031812&station_to=030006&date=today&days_ahead=2'
        '&selling_version=3&selling_flows=validator&selling_flows=aeroexpress&selling_flows=simple'
        '&selling_barcode_presets=PDF417_cppk&selling_barcode_presets=Aztec_mtppk',
        {
            'processes': [SuburbanSellingCheck(days=2, segments=20, selling_tariffs=0, selling_partners=0)],
            'stableness': StablenessVariants.TESTING_UNSTABLE
        }
    ],

    # ----- Нитка -----

    # Курский вокзал - Подольск
    check_threads(
        'v3/suburban/search_on_date?station_from=191602&station_to=190900&date=today',
        {'name': 'Thread'}, date='today'
    ),
    # Белорусский вокзал - Шереметьево
    check_threads(
        'v3/suburban/search_on_date?station_from=198230&station_to=321&date={msk_tomorrow}',
        {'name': 'Thread'}, date='{msk_tomorrow}'
    ),

    # ----- Станция -----

    # Курский вокзал, реквизиты
    'v3/suburban/station/191602',
    'v3/suburban/station/191602/info/',

    # Курский вокзал на сегодня
    [
        'v3/suburban/station_schedule_on_date/191602?date=today&lang=ru_RU',
        {
            'timeout': env.timeout_slow,
            'processes': [MinItemsCount(50, ['threads']), HasItem('date_time')]
        }
    ],
    # Екатеринбург-Пасс на завтра
    [
        'v3/suburban/station_schedule_on_date/780506?date={msk_tomorrow}'
        '&timezone=Asia%2FYekaterinburg&lang=ru_RU',
        {
            'timeout': env.timeout_slow,
            'processes': [MinItemsCount(30, 'threads')]
        }
    ],
    # Подольск на все дни
    [
        'v3/suburban/station_schedule/190900/',
        {
            'timeout': env.timeout_slow,
            'processes': [MinItemsCount(50, 'threads')]
        }
    ],

    # ----- Выгрузка станций для ПП ----

    [
        'v3/partners/nearest_suburban_main_stations',
        {'processes': [MinItemsCount(30)]}
    ],
    [
        'v3/partners/nearest_suburban_main_stations_departures',
        {
            'timeout': env.timeout_slow,
            'processes': [MinItemsCount(30)]
        }
    ],
    [
        'v3/partners/nearest_suburban_stations',
        {'processes': [MinItemsCount(100)]}
    ],
    [
        'v3/partners/nearest_suburban_to_from_center',
        {
            'timeout': env.timeout_slow,
            'processes': [MinItemsCount(100)]
        }
    ],
    [
        'v3/partners/nearest_suburban_to_from_center_all',
        {
            'timeout': env.timeout_slow,
            'processes': [MinItemsCount(100)]
        }
    ],

    # ------------------------------------------- Экспорт v1 -------------------------------------------

    'export/suburban/trip/191602/190900/?date=today',
    'export/suburban/trip/191602/190900',

    'export/suburban/city/213/stations',
    'export/suburban/country/255/directions',

    'export/suburban/zones/1/?lang=ru&national_version=ru',
    'export/suburban/zones?lang=ru',

    ['export/suburban/station/191602/?date=today', {'timeout': env.timeout_slow}],

    'export/suburban/search/?station_from=190900&city_to=213&date=today',

    # ------------------------------------------- Экспорт v2 -------------------------------------------

    'export/v2/suburban/zones?lang=ru',
    ['export/v2/suburban/zones/1',  {'timeout': env.timeout_slow}],

    'export/v2/suburban/trip/774000/770103',

    'export/v2/suburban/search/?station_from=190900&city_to=213'
    '&date=today&tomorrow_upto=3&lang=ru&add_subtypes=true',

    ['export/v2/suburban/station/191602/?date=today&lang=ru&add_subtypes=true', {'timeout': env.timeout_slow}],
]


if env.is_production:
    urls.extend(
        [
            # ИМ. СЗППК. Московский вокзал - Сортировочная
            [
                'v3/suburban/search_on_date?station_from=031812&station_to=030006'
                '&date={msk_tomorrow}&days_ahead=1&lang=ru_RU'
                + selling_params,
                {
                    'processes': [SuburbanSellingCheck(days=1, segments=20, selling_tariffs=2, selling_partners=1)]
                }
            ],
            # Курский вокзал - Владимир, продажа поездов
            check_train_selling_check(
                'v3/suburban/search_on_date?add_subtypes=true&station_from=191602&city_to=192&date={msk_tomorrow}'
                '&selling_version=3&selling_flows=validator&selling_flows=aeroexpress&selling_flows=simple'
                '&selling_barcode_presets=PDF417_cppk&selling_barcode_presets=Aztec_mtppk',
                {
                    'name': 'Train selling check',
                    'stableness': StablenessVariants.UNSTABLE
                }
            ),
            # Санкт-Петербург - Выборг, продажа поездов
            check_train_selling_check(
                'v3/suburban/search_on_date?add_subtypes=true&city_from=2&city_to=969&date={msk_tomorrow}'
                '&selling_version=3&selling_flows=validator&selling_flows=aeroexpress&selling_flows=simple'
                '&selling_barcode_presets=PDF417_cppk&selling_barcode_presets=Aztec_mtppk',
                {
                    'name': 'Train selling check',
                    'stableness': StablenessVariants.UNSTABLE
                }
            )
        ]
    )


checks = [
    {
        'host': env.host,
        'params': {'timeout': env.timeout, 'allow_redirects': False},

        'urls': urls
    }
]
