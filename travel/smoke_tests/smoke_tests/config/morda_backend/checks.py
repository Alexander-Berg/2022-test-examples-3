# coding: utf8
from travel.rasp.smoke_tests.smoke_tests.stableness import StablenessVariants
from travel.rasp.smoke_tests.smoke_tests.config.morda_backend.threads import check_threads
from travel.rasp.smoke_tests.smoke_tests.config.morda_backend.content_checkers import (
    SearchPlatformsPercent, SearchEventsPercent, check_tariffs
)
from travel.rasp.smoke_tests.smoke_tests.common_content_checkers import MinItemsCount, HasItem, ResponseConditionCheck
from travel.rasp.smoke_tests.smoke_tests.config.morda_backend.env import env


def with_plane_station_defaults(params):
    return dict({
        'retries': 5,
        'retries_delay': 20,
    }, **params)


urls = [
    'ping',
    'version',

    # ----- Город и город-транспорт -----

    # Москва
    [
        'ru/settlement/?slug=moscow',
        {'processes': [HasItem('title', 'Москва')]}
    ],
    # Санкт-Петербург
    'ru/settlement/?id=2',
    # Сысерть
    [
        'ru/settlement/?id=20595',
        {'processes': [HasItem('title', 'Сысерть')]}
    ],
    # Екатеринбург (по ближайшему geo_id)
    'ru/settlement/?geo_ids=54'
    # Ташкент (через указание домена)
    'ru/settlement/?root_domain=uz&national_version=uz',
    # Киев
    'uk/settlement/?root_domain=ua&national_version=ua',

    # ----- Станции города -----

    # Неизвестный город
    ['ru/settlement/11111111/stations/', {'code': 404}],
    # Москва
    [
        'ru/settlement/moscow/stations/',
        {'processes': [MinItemsCount(20, 'connected')]}
    ],
    # Москва, только самолеты
    [
        'ru/settlement/moscow/stations/?t_type=plane',
        {'processes': [MinItemsCount(3, 'connected')]}
    ],
    # Самара (есть все виды транспорта)
    [
        'ru/settlement/samara/stations/',
        {'processes': [MinItemsCount(5, 'connected')]}
    ],
    # Самара, только поезда
    [
        'ru/settlement/samara/stations/?t_type=train',
        {'processes': [MinItemsCount(1, 'connected')]}
    ],
    # Псков
    [
        'ru/settlement/25/stations/',
        {'processes': [MinItemsCount(4, 'connected')]}
    ],
    # Сысерть, есть связанная станция
    [
        'ru/settlement/20595/stations/',
        {'processes': [MinItemsCount(1, 'related')]}
    ],

    # ----- Направления электричек города -----

    # Неизвестный город
    ['ru/settlement/unknown_city/directions/', {'code': 404}],
    # Екатеринбург
    [
        'ru/settlement/54/directions/',
        {'processes': [MinItemsCount(7)]}
    ],
    # Домодедово
    [
        'ru/settlement/10725/directions/',
        {'processes': [MinItemsCount(12)]}
    ],
    # Сысерть, электричек нет
    'ru/settlement/20595/directions/',

    # ----- Популярные направления города -----

    # Неизвестный город
    ['ru/settlement/111111111/directions/', {'code': 404}],
    # Санкт-Петербург
    [
        'ru/settlement/2/popular-directions/',
        {'processes': [MinItemsCount(5, ['to', 'points']), MinItemsCount(5, ['from', 'points'])]}
    ],
    # Первоуральск
    [
        'ru/settlement/11171/popular-directions/',
        {'processes': [MinItemsCount(5, ['to', 'points']), MinItemsCount(5, ['from', 'points'])]}
    ],
    # Москва, автобусы
    [
        'ru/settlement/moscow/transport-popular-directions/?t_type=bus&limit=10',
        {'processes': [MinItemsCount(10, 'fromSettlement'), MinItemsCount(10, 'toSettlement')]}
    ],
    # Екатеринбург, самолеты
    [
        'ru/settlement/yekaterinburg/transport-popular-directions/?t_type=plane&limit=3',
        {'processes': [MinItemsCount(3, 'fromSettlement'), MinItemsCount(3, 'toSettlement')]}
    ],
    # Москва, поезда
    'ru/settlement/213/train-popular-directions/',

    # ----- Город, другие ручки -----

    # Екатеринбург, тизеры
    'ru/settlement/54/teasers/',

    # Примеры поискового контекста
    # Россия (указана Москва)
    'ru/settlement/213/search-sample-points/',
    # Украина (указан Киев)
    'ru/settlement/143/search-sample-points/',

    # ----- Транспорт -----

    # Россия
    [
        'ru/transport/train/?country=RU',
        {'processes': [
            HasItem(['mainCity', 'stations']),
            HasItem(['secondaryCity', 'stations']),
            HasItem(['cities']),
            HasItem(['countries'])
        ]}
    ],
    'ru/transport/suburban/?country=RU',
    'ru/transport/plane/?country=RU',
    'ru/transport/bus/?country=RU',
    # Украина
    'ru/transport/bus/?country=UA',
    # Казахстан
    'ru/transport/train/?country=KZ',

    # ----- Поиск parse-context -----

    # Пустой запрос со всеми параметрами
    'ru/search/parse-context/?t_type=train&national_version=ru&from_key=&from_title=&to_key=&to_title=',

    # Москва - Нахабино по именам и слагам
    'ru/search/parse-context/?national_version=ru_RU&from_title=Москва&to_title=Нахабино',
    'ru/search/parse-context/?national_version=ru_RU&from_slug=moscow&to_title=Нахабино',
    'ru/search/parse-context/?national_version=ru_RU&from_slug=moscow&to_slug=nakhabino',

    # Москва - Санкт-Петербург
    [
        'ru/search/parse-context/?national_version=ru_RU&from_title=Moscow&to_title=Saint-Petersburg',
        {
            'processes': [
                HasItem(['to', 'title']), HasItem(['from', 'title']),
                HasItem(['originalTo', 'title']), HasItem(['originalFrom', 'title']),
            ],
        },
    ],
    # Москва - Санкт-Петербург, поезд
    [
        'ru/search/parse-context/?national_version=ru_RU&t_type=plane'
        '&from_title=Moscow&to_title=Saint-Petersburg',
        {
            'processes': [
                HasItem(['to', 'title']), HasItem(['from', 'title']),
                HasItem(['originalTo', 'title']), HasItem(['originalFrom', 'title']),
            ]
        }
    ],
    # Москва - Санкт-Петербург, по id городов
    [
        'ru/search/parse-context/?national_version=ru_RU&from_key=c213&to_key=c2',
        {
            'processes': [
                HasItem(['to', 'title']), HasItem(['from', 'title']),
                HasItem(['originalTo', 'title']), HasItem(['originalFrom', 'title']),
            ]
        }
    ],

    # Москва - пункт с ошибкой
    [
        'ru/search/parse-context/?national_version=ru_RU&from_title=Moscow&to_title=Saint',
        {'processes': [ResponseConditionCheck(
            'Parse context has variants',
            lambda res: res['errors'][0]['type'] == 'ambiguous' and len(res['errors'][0]['variants']) > 0
        )]}
    ],
    [
        'ru/search/parse-context/?national_version=ru_RU&from_title=Moscow&to_title=SSSS',
        {'processes': [ResponseConditionCheck(
            'Parse context has no variants',
            lambda res: res['errors'][0]['type'] == 'point_not_found'
        )]}
    ],

    # Кольцово - Шереметьево
    [
        'ru/search/parse-context/?national_version=ru_RU&from_title=SVX&to_title=SVO',
        {
            'processes': [
                HasItem(['to', 'title']), HasItem(['from', 'title']),
                HasItem(['originalTo', 'title']), HasItem(['originalFrom', 'title']),
            ],
        },
    ],

    # ----- Поиск поездами -----

    # Москва - Екатеринбург на сегодня
    [
        'ru/search/search/?pointFrom=c213&pointTo=c54&transportType=train&when=today',
        {'processes': [MinItemsCount(5, ['result', 'segments'])]}
    ],
    # Москва - Екатеринбург на сегодня с разными параметрами
    [
        'ru/search/search/?pointFrom=c213&pointTo=c54&transportType=train&when=today&nationalVersion=ru'
        '&nearest=false&isMobile=false&allowChangeContext=true'
        '&timezones=Europe%2FMoscow&timezones=Asia%2FYekaterinburg',
        {'processes': [MinItemsCount(5, ['result', 'segments'])]}
    ],

    # Москва - Екатеринбург на вчера
    [
        'ru/search/search/?pointFrom=c213&pointTo=c54&transportType=train&when={msk_yesterday}',
        {'processes': [MinItemsCount(5, ['result', 'segments'])]}
    ],
    # Москва - Екатеринбург на завтра
    [
        'ru/search/search/?pointFrom=c213&pointTo=c54&transportType=train&when={msk_tomorrow}',
        {'processes': [MinItemsCount(5, ['result', 'segments'])]}
    ],

    # Ярославский вокзал - Екатеринбург-Пасс на сегодня
    [
        'ru/search/search/?pointFrom=s2000002&pointTo=s9607404&transportType=train&when=today',
        {'processes': [MinItemsCount(3, ['result', 'segments'])]}
    ],
    [
        'ru/search/search/?pointFrom=s2000002&pointTo=s9607404&when=today',
        {'processes': [MinItemsCount(3, ['result', 'segments'])]}
    ],

    # Архангельск - Екатеринбург, рейсов нет
    'ru/search/search/?pointFrom=c20&pointTo=c54&transportType=train&when=today',
    # Домодедово - Кольцово, на сегодня, поезд, обобщение
    [
        'ru/search/search/?pointFrom=s9600216&pointTo=s9600370&transportType=train&when=today',
        {'processes': [MinItemsCount(5, ['result', 'segments'])]}
    ],

    # Москва - Екатеринбург на все дни
    [
        'ru/search/search/?pointFrom=c213&pointTo=c54&transportType=train',
        {'processes': [MinItemsCount(10, ['result', 'segments'])], 'timeout': env.timeout_slow},
    ],
    # Москва - Екатеринбург на все дни с разными параметрами
    [
        'ru/search/search/?pointFrom=c213&pointTo=c54&transportType=train&nationalVersion=ru'
        '&nearest=false&isMobile=true&allowChangeContext=true&timezones=Europe%2FMoscow&timezones=Asia%2FYekaterinburg',
        {'processes': [MinItemsCount(10, ['result', 'segments'])], 'timeout': env.timeout_slow}
    ],
    # Ярославский вокзал - Екатеринбург-Пасс на все дни
    [
        'ru/search/search/?pointFrom=s2000002&pointTo=s9607404&transportType=train',
        {'processes': [MinItemsCount(5, ['result', 'segments'])]}
    ],
    # Домодедово - Кольцово, на все дни, поезд, обобщение
    [
        'ru/search/search/?pointFrom=s9600216&pointTo=s9600370&transportType=train',
        {
            'processes': [MinItemsCount(5, ['result', 'segments'])],
            'timeout': env.timeout_slow
        }
    ],

    # ----- Поиск электрички -----

    # Екатеринбург - Первоуральск, на сегодня
    [
        'ru/search/search/?pointFrom=c54&pointTo=c11171&transportType=suburban&when=today',
        {'processes': [
            MinItemsCount(10, ['result', 'segments']),
            SearchEventsPercent(30)
        ]}
    ],
    # Екатеринбург - Первоуральск, на завтра
    [
        'ru/search/search/?pointFrom=c54&pointTo=c11171&transportType=suburban&when={msk_tomorrow}',
        {'processes': [MinItemsCount(5, ['result', 'segments'])]}
    ],
    # Екатеринбург-Пасс - вокзал Первоуральска на сегодня
    [
        'ru/search/search/?pointFrom=s9607404&pointTo=s9607449&transportType=suburban&when=today',
        {'processes': [MinItemsCount(5, ['result', 'segments'])]}
    ],
    # Екатеринбург - Первоуральск, ближайшие
    [
        'ru/search/search/?pointFrom=c54&pointTo=c11171&transportType=suburban&nearest=true',
        {'processes': [MinItemsCount(5, ['result', 'segments'])]}
    ],
    # Екатеринбург-Пасс - вокзал Первоуральска на все дни
    [
        'ru/search/search/?pointFrom=s9607404&pointTo=s9607449&transportType=suburban',
        {'processes': [MinItemsCount(5, ['result', 'segments'])]}
    ],

    # Курский вокзал - Подольск, на сегодня
    [
        'ru/search/search/?pointFrom=s2000001&pointTo=c10747&transportType=suburban&when=today',
        {'processes': [
            MinItemsCount(30, ['result', 'segments']),
            SearchEventsPercent(30)
        ]}
    ],
    # Курский вокзал - Подольск, на сегодня с разными параметрами
    [
        'ru/search/search/?pointFrom=s2000001&pointTo=c10747&transportType=suburban&when=today'
        '&nationalVersion=ru&nearest=false&isMobile=true&allowChangeContext=true'
        '&timezones=Europe%2FMoscow&timezones=Asia%2FYekaterinburg',
        {'processes': [MinItemsCount(30, ['result', 'segments'])]}
    ],
    # Курский вокзал - Подольск, на все дни
    [
        'ru/search/search/?pointFrom=s2000001&pointTo=c10747&transportType=suburban',
        {
            'processes': [MinItemsCount(30, ['result', 'segments'])],
            'timeout': env.timeout_slow
        }
    ],

    # Белорусский вокзал - Шереметьево (Аэропорт Шереметьево - Северный терминал (B, C)), динамические платформы
    [
        'ru/search/search/?pointFrom=s2000006&pointTo=s9881704&transportType=suburban&when={msk_today}',
        {
            'name': 'Suburban search with dynamic platforms',
            'processes': [SearchPlatformsPercent(1)]
        }
    ],

    # Москва - Екатеринбург, электричек нет
    'ru/search/search/?pointFrom=c213&pointTo=c54&transportType=suburban',

    # ----- Поиск самолеты -----

    # Москва - Санкт-Петербург на сегодня
    [
        'ru/search/search/?pointFrom=c213&pointTo=c2&transportType=plane&when=today',
        {'processes': [MinItemsCount(7, ['result', 'segments'])]}
    ],
    # Москва - Санкт-Петербург на завтра
    [
        'ru/search/search/?pointFrom=c213&pointTo=c2&transportType=plane&when={msk_tomorrow}',
        {'processes': [MinItemsCount(7, ['result', 'segments'])]}
    ],
    # Москва - Санкт-Петербург, ближайшие
    [
        'ru/search/search/?pointFrom=c213&pointTo=c2&transportType=plane&nearest=true',
        {'processes': [MinItemsCount(4, ['result', 'segments'])]}
    ],
    # Москва - Санкт-Петербург с разными параметрами
    [
        'ru/search/search/?pointFrom=c213&pointTo=c2&transportType=plane&when=today'
        '&nearest=false&isMobile=true&allowChangeContext=true&timezones=Asia%2FYekaterinburg',
        {'processes': [MinItemsCount(7, ['result', 'segments'])]}
    ],
    # Москва - Санкт-Петербург на все дни
    [
        'ru/search/search/?pointFrom=c213&pointTo=c2&transportType=plane',
        {'processes': [MinItemsCount(7, ['result', 'segments'])]}
    ],

    # Шереметьево - Кольцово на сегодня
    [
        'ru/search/search/?pointFrom=s9600213&pointTo=s9600370&when={msk_today}',
        {'processes': [MinItemsCount(2, ['result', 'segments'])]}
    ],
    # Шереметьево - Кольцово на все дни
    [
        'ru/search/search/?pointFrom=s9600213&pointTo=s9600370'
        '&timezones=Europe%2FMoscow&timezones=Asia%2FYekaterinburg',
        {'processes': [MinItemsCount(4, ['result', 'segments'])]}
    ],

    # Екатеринбург - Петропавловск-Камчатский, рейсов нет
    'ru/search/search/?pointFrom=c54&pointTo=c78&transportType=plane&when=today',
    'ru/search/search/?pointFrom=c54&pointTo=c78&transportType=plane',

    # Лондон - Париж, на завтра
    [
        'ru/search/search/?pointFrom=c10393&pointTo=c10502&transportType=plane&when={msk_tomorrow}',
        {'processes': [MinItemsCount(2, ['result', 'segments'])]}
    ],
    # Лондон - Париж, на все дни
    [
        'ru/search/search/?pointFrom=c10393&pointTo=c10502',
        {'processes': [MinItemsCount(2, ['result', 'segments'])]}
    ],

    # ----- Поиск автобусы -----

    # Екатеринбург - Челябинск на сегодня
    [
        'ru/search/search/?pointFrom=c54&pointTo=c56&transportType=bus&when=today',
        {'processes': [MinItemsCount(5, ['result', 'segments'])]}
    ],
    # Екатеринбург - Челябинск на завтра
    [
        'ru/search/search/?pointFrom=c54&pointTo=c56&transportType=bus&when={msk_tomorrow}',
        {'processes': [MinItemsCount(5, ['result', 'segments'])]}
    ],
    # Екатеринбург - Челябинск ближайшие
    [
        'ru/search/search/?pointFrom=c54&pointTo=c56&transportType=bus&is_nearest=true',
        {'processes': [MinItemsCount(3, ['result', 'segments'])]}
    ],
    # Екатеринбург - Челябинск на все дни
    [
        'ru/search/search/?pointFrom=c54&pointTo=c56&transportType=bus',
        {'processes': [MinItemsCount(5, ['result', 'segments'])]}
    ],
    # Екатеринбург (Северный автовокзал) - Челябинск (Юность) на сегодня
    [
        'ru/search/search/?pointFrom=s9635953&pointTo=s9635958&transportType=bus&when=today',
        {'processes': [MinItemsCount(1, ['result', 'segments'])]}
    ],
    # Екатеринбург (Северный автовокзал) - Челябинск (Юность) на все дни
    [
        'ru/search/search/?pointFrom=s9635953&pointTo=s9635958&transportType=bus',
        {'processes': [MinItemsCount(1, ['result', 'segments'])]}
    ],
    # Москва - Химки на сегодня
    [
        'ru/search/search/?pointFrom=c213&pointTo=c10758&transportType=bus&when=today',
        {
            'processes': [MinItemsCount(30, ['result', 'segments'])],
            'timeout': env.timeout_very_slow
        }
    ],

    # ----- Поиск всеми типами транспорта -----

    # Москва - Санкт-Петербург на сегодня
    [
        'ru/search/search/?pointFrom=c213&pointTo=c2&when=today',
        {'processes': [MinItemsCount(20, ['result', 'segments'])]}
    ],
    # Москва - Санкт-Петербург на сегодня с разными параметрами
    [
        'ru/search/search/?pointFrom=c213&pointTo=c2&when=today&nationalVersion=ru&nearest=false'
        '&isMobile=true&allowChangeContext=true&timezones=Asia%2FYekaterinburg',
        {'processes': [MinItemsCount(10, ['result', 'segments'])]}
    ],
    # Москва - Санкт-Петербург на все дни
    [
        'ru/search/search/?pointFrom=c213&pointTo=c2',
        {
            'processes': [MinItemsCount(30, ['result', 'segments'])],
            'timeout': env.timeout_slow
        }
    ],
    # Екатеринбург - Петропавловск-Камчатский, рейсов нет
    'ru/search/search/?pointFrom=c54&pointTo=c78&when=today',

    # Москва - Химки на сегодня
    [
        'ru/search/search/?pointFrom=c213&pointTo=c10758&when=today',
        {
            'processes': [MinItemsCount(30, ['result', 'segments'])],
            'timeout': env.timeout_very_slow

        }
    ],

    # ----- Пересадки -----

    # Кольцово - Архангельск (Талаги), самолетами
    [
        'ru/search/transfers/?pointFrom=s9600370&pointTo=s9600185&transportType=plane&when={msk_today}',
        {
            'processes': [MinItemsCount(2, ['transferVariants'])],
            'stableness': StablenessVariants.TESTING_UNSTABLE
        }
    ],
    # Екатеринбург - Петропавловск-Камчатский, на завтра, есть только самолеты
    [
        'ru/search/transfers/?pointFrom=c54&pointTo=c78&when={msk_tomorrow}',
        {
            'processes': [MinItemsCount(1, ['transferVariants'])],
            'stableness': StablenessVariants.TESTING_UNSTABLE
        }
    ],


    # Екатеринбург - Вологда, поезда
    [
        'ru/search/transfers/?pointFrom=c54&pointTo=c21&transportType=train&when={msk_today}',
        {
            'processes': [MinItemsCount(1, ['transferVariants'])],
            'stableness': StablenessVariants.TESTING_UNSTABLE
        }
    ],

    # Екатеринбург - Красноуфимск, электрички
    [
        'ru/search/transfers/?pointFrom=c54&pointTo=c20691&transportType=suburban&when={msk_tomorrow}',
        {
            'processes': [MinItemsCount(1, ['transferVariants'])],
            'stableness': StablenessVariants.TESTING_UNSTABLE
        }
    ],

    # Кунгур - Соликамск всеми видами транспорта
    [
        'ru/search/transfers/?pointFrom=c20250&pointTo=c11110&when={msk_today}',
        {
            'processes': [MinItemsCount(3, ['transferVariants'])],
            'stableness': StablenessVariants.TESTING_UNSTABLE
        }
    ],
    # Екатеринбург - Рязань всеми видами транспорта
    [
        'ru/search/transfers/?pointFrom=c54&pointTo=c11&when={msk_today}',
        {
            'processes': [MinItemsCount(10, ['transferVariants'])],
            'stableness': StablenessVariants.TESTING_UNSTABLE
        }
    ],

    # ----- Пересадки для pathfinder_maps ------

    # Кольцово - Архангельск (Талаги), самолетами
    [
        'ru/search/pathfinder-maps-variants/?pointFrom=s9600370&pointTo=s9600185&transportType=plane&when={msk_today}',
        {
            'processes': [MinItemsCount(2, ['variants'])],
            'stableness': StablenessVariants.TESTING_UNSTABLE
        }
    ],

    # Екатеринбург - Вологда, поезда
    [
        'ru/search/pathfinder-maps-variants/?pointFrom=c54&pointTo=c21&transportType=train&when={msk_today}',
        {
            'processes': [MinItemsCount(1, ['variants'])],
            'stableness': StablenessVariants.TESTING_UNSTABLE
        }
    ],

    # Екатеринбург - Красноуфимск, электрички
    [
        'ru/search/pathfinder-maps-variants/?pointFrom=c54&pointTo=c20691&transportType=suburban&when={msk_tomorrow}',
        {
            'processes': [MinItemsCount(1, ['variants'])],
            'stableness': StablenessVariants.TESTING_UNSTABLE
        }
    ],

    # Екатеринбург - Рязань всеми видами транспорта
    [
        'ru/search/pathfinder-maps-variants/?pointFrom=c54&pointTo=c11&when={msk_today}',
        {
            'processes': [MinItemsCount(10, ['variants'])],
            'stableness': StablenessVariants.TESTING_UNSTABLE
        }
    ],

    # Екатеринбург - Нижний Тагил, есть прямые рейсы
    [
        'ru/search/pathfinder-maps-variants/?pointFrom=c54&pointTo=c11168&when={msk_tomorrow}',
        {
            'processes': [MinItemsCount(10, ['variants'])],
            'stableness': StablenessVariants.TESTING_UNSTABLE
        }
    ],
    # Екатеринбург - Москва самолетами, есть прямые рейсы
    [
        'ru/search/pathfinder-maps-variants/?pointFrom=c54&pointTo=c213&transportType=plane&when={msk_today}',
        {
            'processes': [MinItemsCount(5, ['variants'])],
            'stableness': StablenessVariants.TESTING_UNSTABLE
        }
    ],

    # ----- Canonicals ------

    # Екатеринбург - Москва
    [
        'ru/search/canonicals/?pointFrom=c54&pointTo=c213',
        {'processes': [MinItemsCount(1, ['result', 'canonicals'])]}
    ],
    # Екатеринбург - Москва, кроме самолетов
    [
        'ru/search/canonicals/?pointFrom=c54&pointTo=c213&transportType=plane',
        {'processes': [MinItemsCount(1, ['result', 'canonicals'])]}
    ],
    # Екатеринбург - Москва, кроме поездов
    [
        'ru/search/canonicals/?pointFrom=c54&pointTo=c213&transportType=train',
        {'processes': [MinItemsCount(1, ['result', 'canonicals'])]}
    ],

    # Екатеринбург-Пасс - Первоуральск вокзал
    [
        'ru/search/canonicals/?pointFrom=s9607404&pointTo=s9607449',
        {'processes': [MinItemsCount(2, ['result', 'canonicals'])]}
    ],
    # Екатеринбург-Пасс - Первоуральск вокзал, кроме поездов
    [
        'ru/search/canonicals/?pointFrom=s9607404&pointTo=s9607449&transportType=train',
        {'processes': [MinItemsCount(1, ['result', 'canonicals'])]}
    ],
    # Екатеринбург-Пасс - Первоуральск вокзал, кроме электричек
    [
        'ru/search/canonicals/?pointFrom=s9607404&pointTo=s9607449&transportType=suburban',
        {'processes': [MinItemsCount(1, ['result', 'canonicals'])]}
    ],

    # ----- Авиа-тарифы -----

    # Санкт-Петербург - Москва
    [
        'ru/segments/tariffs/?pointFrom=c2&pointTo=c213&date={msk_tomorrow}&national_version=ru',
        {'processes': [MinItemsCount(7, ['segments'])] if env.check_avia_tariffs else []}
    ],
    # Санкт-Петербург - Москва, другие значения параметров
    [
        'ru/segments/tariffs/?pointFrom=c2&pointTo=c213&date={msk_tomorrow}'
        '&national_version=ru&clientSettlementId=213&transportType=plane',
        {'processes': [MinItemsCount(7, ['segments'])] if env.check_avia_tariffs else []}
    ],
    # Санкт-Петербург - Москва, поллинг
    [
        'ru/segments/tariffs/poll/?pointFrom=c2&pointTo=c213'
        '&date={msk_tomorrow}&national_version=ru&clientSettlementId=213',
        {'processes': [MinItemsCount(7, ['segments'])] if env.check_avia_tariffs else []}
    ],
    # Кольцово - Шереметьево
    [
        'ru/segments/tariffs/?pointFrom=s9600370&pointTo=s9600213&date={msk_tomorrow}&national_version=ru',
        {'processes': [MinItemsCount(2, ['segments'])] if env.check_avia_tariffs else []}
    ],
    # Кольцово - Шереметьево, поллинг
    [
        'ru/segments/tariffs/poll/?pointFrom=s9600370&pointTo=s9600213&date={msk_tomorrow}&national_version=ru',
        {'processes': [MinItemsCount(2, ['segments'])] if env.check_avia_tariffs else []}
    ],
    # Париж - Лондон
    [
        'ru/segments/tariffs/?pointFrom=c10502&pointTo=c10393&date={msk_tomorrow}&national_version=ru',
        {'processes': [MinItemsCount(2, ['segments'])] if env.check_avia_tariffs else []}
    ],
    # Петропавловск-Камчатский - Тиличики
    'ru/segments/tariffs/?pointFrom=c78&pointTo=c26358&date={msk_tomorrow}&national_version=ru&clientSettlementId=78',

    # Екатеринбург - Москва
    check_tariffs(
        'ru/tariffs/plane/?pointFrom=c54&pointTo=c213&date={msk_tomorrow}&national_version=ru&clientSettlementId=54',
        min_items_count=4
    ),
    # Кольцово - Шереметьево
    check_tariffs(
        'ru/tariffs/plane/?pointFrom=s9600370&pointTo=s9600213&date={msk_tomorrow}&national_version=ru',
        min_items_count=2
    ),
    # Париж - Лондон
    check_tariffs(
        'ru/tariffs/plane/?pointFrom=c10502&pointTo=c10393&date={msk_tomorrow}&national_version=ru',
        min_items_count=2
    ),
    # Петропавловск-Камчатский - Тиличики
    check_tariffs(
        'ru/tariffs/plane/?pointFrom=c78&pointTo=c26358&date={msk_tomorrow}&national_version=ru',
        min_items_count=0
    ),

    # ----- Минимальные тарифы, поезда и самолеты -----

    # Екатеринбург - Нижний Тагил
    [
        'ru/segments/min-tariffs/?pointFrom=c54&pointTo=c11168&national_version=ru&clientSettlementId=54',
        {'processes': [MinItemsCount(2, ['tariffs'])]}
    ],
    # Екатеринбург-Пасс - Казанский вокзал
    [
        'ru/segments/min-tariffs/?pointFrom=s9607404&pointTo=s2000003&national_version=ru&transportType=train',
        {'processes': [MinItemsCount(2, ['tariffs'])]}
    ],
    # Екатеринбург - Москва, самолеты
    [
        'ru/segments/min-tariffs/?pointFrom=c54&pointTo=c213&national_version=ru&transportType=plane',
        {'processes': [MinItemsCount(4, ['tariffs'])]}
    ],

    # ----- Тарифы, автобусы -----

    # Екатеринбург - Серов
    [
        'ru/tariffs/min-static-tariffs/?pointFrom=c54&pointTo=c11172&national_version=ru',
        {'processes': [MinItemsCount(10, ['tariffs'])]}
    ],
    # Екатеринбург - Нижний Тагил с разными параметрами
    [
        'ru/tariffs/min-static-tariffs/?pointFrom=c54&pointTo=c11168'
        '&national_version=ru&transportType=bus&date={msk_tomorrow}',
        {'processes': [MinItemsCount(20, ['tariffs'])]}
    ],

    # Екатеринбург - Челябинск
    'ru/segments/bus-tariffs/?pointFrom=c54&pointTo=c56&date={msk_tomorrow}&national_version=ru',
    # Пермь, автовокзал - Ижевск, автовокзал
    'ru/segments/bus-tariffs/?pointFrom=s9635690&pointTo=s9635683&date={msk_tomorrow}&national_version=ru',
    # Екатеринбург - Нижний Тагил, без даты
    'ru/segments/bus-tariffs/?pointFrom=c54&pointTo=c11168&national_version=ru',

    # ----- Тарифы, электрички -----

    # Можайск - Белорусский вокзал
    [
        'ru/tariffs/suburban/?pointFrom=s9601006&pointTo=s2000006&national_version=ru&clientSettlementId=33816',
        {'processes': [MinItemsCount(2, ['tariffs'])]}
    ],
    # Екатеринбург - Нижний Тагил
    [
        'ru/tariffs/suburban/?pointFrom=c54&pointTo=c11168&national_version=ru&clientSettlementId=54',
        {'processes': [MinItemsCount(2, ['tariffs'])]}
    ],

    # ----- Быстрая ручка станции -----

    # Екатеринбург-Пасс, поезда
    'ru/station/quick/?station_id=9607404&subtype=train',
    'ru/station/quick/?station_id=9607404',
    # Белорусский вокзал, электрички
    'ru/station/quick/?station_id=2000006&subtype=suburban',
    # Домодедово, самолеты
    'ru/station/quick/?station_id=9600216&subtype=plane',
    # Домодедово, электрички'
    'ru/station/quick/?station_id=9600216&subtype=suburban',
    # Челябинск, Южный автовокзал
    'ru/station/quick/?station_id=9851724',

    # ----- ЖД-станция, поезда -----

    # Екатеринбург-Пасс, поезда на сегодня
    [
        'ru/station/?station_id=9607404&date=today',
        {'processes': [
            HasItem(['result', 'station']),
            HasItem(['result', 'context']),
            HasItem(['result', 'pageType', 'currentSubtype'], 'train'),
            MinItemsCount(3, ['result', 'pageType', 'subtypes']),
            MinItemsCount(10, ['result', 'threads']),
        ]}
    ],
    # То же самое, полученное другими комбинациями параметров
    'ru/station/?station_id=9607404&date=today&subtype=train',
    'ru/station/?station_id=9607404&date={msk_today}&subtype=train',
    'ru/station/?station_id=9607404&date=today&subtype=train&event=departure',
    'ru/station/?station_id=9607404&date=today&is_mobile=true',
    # Пермь-2, поезда на завтра
    [
        'ru/station/?station_id=9607774&date=tomorrow&subtype=train',
        {'processes': [MinItemsCount(10, ['result', 'threads'])]}
    ],
    # Пермь-2, поезда на завтра, прибытие
    [
        'ru/station/?station_id=9607774&date=tomorrow&subtype=train&event=departure',
        {'processes': [MinItemsCount(10, ['result', 'threads'])]}
    ],
    # Пермь-2, поезда на вчера
    [
        'ru/station/?station_id=9607774&date={msk_yesterday}&subtype=train',
        {'processes': [MinItemsCount(10, ['result', 'threads'])]}
    ],

    # Екатеринбург-Пасс, поезда на все дни
    [
        'ru/station/?station_id=9607404&date=all-days',
        {
            'timeout': env.timeout_slow,
            'processes': [MinItemsCount(20, ['result', 'threads'])]
        }
    ],
    # Курский вокзал, поезда на все дни, прибытие
    [
        'ru/station/?station_id=2000001&date=all-days&event=arrival',
        {
            'timeout': env.timeout_slow,
            'processes': [MinItemsCount(10, ['result', 'threads'])]
        }
    ],

    # ----- ЖД-станция, электрички -----

    # Екатеринбург-Пасс, электрички на сегодня, все направления
    [
        'ru/station/?station_id=9607404&date=today&subtype=suburban&direction=all',
        {'processes': [
            HasItem(['result', 'context']),
            HasItem(['result', 'pageType', 'currentSubtype'], 'suburban'),
            MinItemsCount(3, ['result', 'pageType', 'subtypes']),
            MinItemsCount(6, ['result', 'station', 'directions']),
            MinItemsCount(10, ['result', 'threads']),
        ]}
    ],
    # Екатеринбург-Пасс, электрички на сегодня, направление по умолчанию
    [
        'ru/station/?station_id=9607404&date=today&subtype=suburban',
        {'processes': [
            MinItemsCount(6, ['result', 'station', 'directions']),
            MinItemsCount(5, ['result', 'threads']),
        ]}
    ],
    # Екатеринбург-Пасс, электрички на сегодня, направление на Шалю
    [
        'ru/station/?station_id=9607404&date=today&subtype=suburban&direction=на+Шалю',
        {'processes': [
            MinItemsCount(6, ['result', 'station', 'directions']),
            MinItemsCount(5, ['result', 'threads']),
        ]}
    ],
    # Гать, электрички на завтра
    [
        'ru/station/?station_id=9607983&date=tomorrow',
        {'processes': [
            HasItem(['result', 'pageType', 'currentSubtype'], 'suburban'),
            MinItemsCount(3, ['result', 'station', 'directions']),
            MinItemsCount(5, ['result', 'threads']),
        ]}
    ],
    [
        'ru/station/?station_id=9607983&date={msk_tomorrow}',
        {'processes': [MinItemsCount(5, ['result', 'threads'])]}
    ],
    # Гать, электрички на все дни
    [
        'ru/station/?station_id=9607983&date=all-days',
        {'processes': [
            HasItem(['result', 'pageType', 'currentSubtype'], 'suburban'),
            MinItemsCount(3, ['result', 'station', 'directions']),
            MinItemsCount(5, ['result', 'threads']),
        ]}
    ],
    # Белорусский вокзал, электрички на все дни
    [
        'ru/station/?station_id=2000006&date=all-days&subtype=suburban',
        {'processes': [MinItemsCount(30, ['result', 'threads'])]}
    ],
    [
        'ru/station/?station_id=2000006&date=all-days&subtype=suburban&is_mobile=true',
        {'processes': [MinItemsCount(30, ['result', 'threads'])]}
    ],
    # Белорусский вокзал, электрички на все дни, Белорусское направление
    [
        'ru/station/?station_id=2000006&date=all-days&subtype=suburban&direction=Белорусское+направление',
        {'processes': [MinItemsCount(30, ['result', 'threads'])]}
    ],

    # ----- ЖД-станция, табло -----

    # Белорусский вокзал, табло на сегодня
    [
        'ru/station/?station_id=2000006&date=today&subtype=tablo',
        {'processes': [MinItemsCount(30, ['result', 'threads'])]}
    ],
    [
        'ru/station/?station_id=2000006&date=today&subtype=tablo&is_mobile=true',
        {'processes': [MinItemsCount(30, ['result', 'threads'])]}
    ],
    # Пермь-2, табло на завтра, прибытие
    [
        'ru/station/?station_id=9607774&date=tomorrow&subtype=tablo',
        {'processes': [MinItemsCount(20, ['result', 'threads'])]}
    ],
    # Пермь-2, табло на завтра, прибытие
    [
        'ru/station/?station_id=9607774&date=tomorrow&subtype=tablo&event=departure',
        {'processes': [MinItemsCount(20, ['result', 'threads'])]}
    ],
    # Пермь-2, табло на все дни, прибытие
    [
        'ru/station/?station_id=9607774&date=all-days&subtype=tablo',
        {'processes': [MinItemsCount(20, ['result', 'threads'])]}
    ],

    # ----- Аэропорт -----

    # Шереметьево, сегодня на весь день
    [
        'ru/station/?station_id=9600213&date=today',
        with_plane_station_defaults({'processes': [
            HasItem(['result', 'context']),
            HasItem(['result', 'companies']),
            HasItem(['result', 'pageType', 'currentSubtype'], 'plane'),
            MinItemsCount(5, ['result', 'pageType', 'terminals']),
            MinItemsCount(30, ['result', 'threads']),
        ]})
    ],
    # Шереметьево, разные дополнительные параметры
    [
        'ru/station/?station_id=9600213&date=today&country=ru',
        with_plane_station_defaults({'processes': [MinItemsCount(30, ['result', 'threads'])]})
    ],
    [
        'ru/station/?station_id=9600213&date=today&country=ru&subtype=plane',
        with_plane_station_defaults({'processes': [
            HasItem(['result', 'pageType', 'currentSubtype'], 'plane'),
            MinItemsCount(30, ['result', 'threads'])
        ]})
    ],
    # Домодедово, сегодня с 8 до 12
    [
        'ru/station/?station_id=9600216&date=today&time_after=08:00&time_before=12:00',
        with_plane_station_defaults({'processes': [MinItemsCount(15, ['result', 'threads'])]})
    ],
    # Домодедово, сегодня с 8 до 12, прибытие
    [
        'ru/station/?station_id=9600216&date=today&time_after=08:00&time_before=12:00&event=arrival',
        with_plane_station_defaults({'processes': [MinItemsCount(15, ['result', 'threads'])]})
    ],
    # Домодедово, завтра с 12 до 16
    [
        'ru/station/?station_id=9600216&date={msk_tomorrow}&time_after=12:00&time_before=16:00',
        with_plane_station_defaults({'processes': [MinItemsCount(15, ['result', 'threads'])]})
    ],
    # Хитроу, сегодня с 8 до 12
    [
        'ru/station/?station_id=9600378&date=today&time_after=08:00&time_before=12:00',
        with_plane_station_defaults({'processes': [MinItemsCount(20, ['result', 'threads'])]})
    ],
    # Шереметьево на все дни
    [
        'ru/station/?station_id=9600213&date=all-days',
        with_plane_station_defaults({
            'processes': [MinItemsCount(30, ['result', 'threads'])],
            'timeout': env.timeout_slow
        })
    ],
    # Кольцово на все дни
    [
        'ru/station/?station_id=9600370&date=all-days',
        with_plane_station_defaults({'processes': [MinItemsCount(20, ['result', 'threads'])]})
    ],
    # Кольцово на все дни, прибытие
    [
        'ru/station/?station_id=9600370&date=all-days&event=arrival',
        with_plane_station_defaults({'processes': [MinItemsCount(20, ['result', 'threads'])]})
    ],
    # Кольцово на все дни, с 12 до 14
    [
        'ru/station/?station_id=9600370&date=all-days&time_after=12:00&time_before=14:00',
        with_plane_station_defaults({'processes': [MinItemsCount(5, ['result', 'threads'])]})
    ],

    # ----- Автобусная остановка и порт -----

    # Челябинск, Южный автовокзал на сегодня
    [
        'ru/station/?station_id=9851724&date=today',
        {'processes': [
            HasItem(['result', 'context']),
            HasItem(['result', 'station']),
            HasItem(['result', 'companies']),
            HasItem(['result', 'pageType', 'currentSubtype'], 'schedule'),
            MinItemsCount(12, ['result', 'scheduleBlocks']),
        ]}
    ],
    # Челябинск, Южный автовокзал, вызов с разными параметрами
    [
        'ru/station/?station_id=9851724&date=today&is_mobile=true',
        {'processes': [MinItemsCount(12, ['result', 'scheduleBlocks'])]}
    ],
    [
        'ru/station/?station_id=9851724&date=today&subtype=schedule',
        {'processes': [MinItemsCount(12, ['result', 'scheduleBlocks'])]}
    ],

    # Бутаково (автобусная остановка в Химках) на завтра, есть интервальные рейсы
    [
        'ru/station/?station_id=9743011&date=tomorrow',
        {'processes': [MinItemsCount(20, ['result', 'scheduleBlocks'])]}
    ],
    [
        'ru/station/?station_id=9743011&date={msk_tomorrow}',
        {'processes': [MinItemsCount(20, ['result', 'scheduleBlocks'])]}
    ],

    # Бутаково, на все дни
    [
        'ru/station/?station_id=9743011&date=all-days',
        {
            'processes': [MinItemsCount(20, ['result', 'scheduleBlocks'])],
            'timeout': env.timeout_slow
        }
    ],

    # Самара, Речной вокзал, на сегодня
    'ru/station/?station_id=9801127&date=today',
    # Самара, Речной вокзал, на все дни
    'ru/station/?station_id=9801127&date=all-days',

    # Список остановок от Южного автовокзала Челябинска, на сегодня
    [
        'ru/station/stops/?station_id=9851724&date=today&return_for_types=bus',
        {'processes': [MinItemsCount(30, ['result', 'stops'])]}
    ],
    # Список остановок от Южного автовокзала Челябинска, на все дни
    [
        'ru/station/stops/?station_id=9851724&date=all-days&return_for_types=bus',
        {
            'processes': [MinItemsCount(30, ['result', 'stops'])],
            'timeout': env.timeout_slow
        }
    ],

    # ----- Станция, другие ручки -----

    # Несуществующая станция
    ['ru/station/?station_id=111111111&date=today', {'code': 404}],
    ['ru/station/city_stations/?station_id=111111111', {'code': 404}],
    ['ru/station/popular_directions/?station_id=111111111&limit=5', {'code': 404}],

    # Самара, вокзал, другие станции
    [
        'ru/station/city_stations/?station_id=9606096',
        {'processes': [
            MinItemsCount(1, ['result', 'cityStations', 'plane']),
            MinItemsCount(1, ['result', 'cityStations', 'train']),
            MinItemsCount(1, ['result', 'cityStations', 'bus']),
            HasItem(['result', 'settlement'])
        ]}

    ],

    # Екатеринбург-Пасс, популярные направления
    [
        'ru/station/popular_directions/?station_id=9607404&limit=5',
        {'processes': [
            MinItemsCount(5, ['result', 'from']),
            MinItemsCount(5, ['result', 'to']),
            HasItem(['result', 'station'])
        ]}
    ],
    # Кольцово, популярные направления
    [
        'ru/station/popular_directions/?station_id=9600370&limit=5',
        {'processes': [
            MinItemsCount(5, ['result', 'from']),
            MinItemsCount(5, ['result', 'to']),
            HasItem(['result', 'station'])
        ]}
    ],

    # ----- Нитка -----

    # Нитка поезда, Екатеринбург - Москва
    check_threads(
        'ru/search/search/?pointFrom=c54&pointTo=c213&transportType=train&when={msk_today}',
        {'name': 'Train thread', 'timeout': env.timeout_slow},
        is_railway=True
    ),
    # Нитка электрички, Белорусский вокзал - Шереметьево (Аэропорт Шереметьево - Северный терминал (B, C))
    check_threads(
        'ru/search/search/?pointFrom=s2000006&pointTo=s9881704&transportType=suburban&when={msk_today}',
        {'name': 'Suburban thread'},
        is_railway=True
    ),
    # Нитка автобуса, Екатеринбург - Челябинск
    check_threads(
        'ru/search/search/?pointFrom=c54&pointTo=c56&transportType=bus&when={msk_today}',
        {'name': 'Bus thread'},
        is_railway=False
    ),

    # ----- Прочие ручки -----

    # Статьи
    'ru/articles/?country=ru&settlement_slug=minsk&t_type=train&limit=2',
    'uk/articles/?country=ua&settlement_slug=aktoguy&t_type=train&limit=2',

    # Статический текст
    'ru/statictext/disclaimers/?codes=morda_rasp_description',

    # Валюты, перевод в рубли
    'ru/currencies/?national_version=ru&base=RUB',
    'ru/currencies/',
    # Валюты, перевод в гривны
    'uk/currencies/?national_version=ua&base=UAH',

    # Страны
    'ru/countries/?national_version=ru'
]

if env.allow_large_search:
    urls.extend([
        # Москва - Химки на все дни, автобусы
        [
            'ru/search/search/?pointFrom=c213&pointTo=c10758&transportType=bus',
            {
                'processes': [MinItemsCount(30, ['result', 'segments'])],
                'timeout': env.timeout_very_slow
            }
        ],
        # Москва - Химки на все дни
        [
            'ru/search/search/?pointFrom=c213&pointTo=c10758',
            {
                'processes': [MinItemsCount(30, ['result', 'segments'])],
                'timeout': env.timeout_very_slow
            }
        ]
    ])

checks = [
    {
        'host': env.host,
        'params': {'timeout': env.timeout, 'allow_redirects': False},
        'urls': urls
    }
]
