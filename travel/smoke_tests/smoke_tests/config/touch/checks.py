# coding: utf8
from travel.rasp.smoke_tests.smoke_tests.config.touch.env import env


checks = [
    {
        'host': env.host,
        'params': {'timeout': env.timeout, 'retries': env.retries},
        'urls': [
            # Города
            # Морда города Екатеринбург
            'city/54',
            # Морда города Москва
            'city/213',
            # Морда города Санкт-Петербург
            'city/2',
            # Морда города Владивосток
            'city/75',
            # Морда города Казань
            'city/43',
            # Морда города Киев
            'city/143',
            # Морда города Львов
            'city/144',

            # Поиски
            # Тяжелый поиск электричками: Москва - Химки
            ['search/?fromName=Москва&toName=Химки&when=завтра&fromId=c213&toId=s9603401', {'timeout': env.timeout_very_slow}],
            # Хороший поиск самолетами: Улан-Удэ - Москва
            'search/plane/?toName=Москва&fromName=Улан-Удэ&when=завтра&toId=c213&delta=1&fromId=c198',
            # Хороший поиск поездами: Абакан - Красноярск
            'search/?fromName=Абакан&toName=Красноярск&when=завтра&fromId=c1095&toId=c62',
            # Поиск автобус Екатеринбург - Нижний Тагил с фильтрами
            'search/bus/?fromName=Екатеринбург&toName=Нижний+Тагил&fromId=c54&toId=c11168&when=сегодня&sortBy=duration&seats=y&gone=y&stationTo=9624011&stationFrom=9635953&departure=night',

            # Направления электричек выборг-фин вокзал
            'direction-search/?fromId=s9603175&toId=s9602497&direction=spb_vyb&city=2&all_days=1&lang=ru',

            # Табло
            # Табло с электричками: Москва (Курский вокзал)
            ['station/2000001/suburban/?filter=all', {'timeout': env.timeout_very_slow}],
            # Табло с поездами на все дни: Москва (Ярославский вокзал)
            'station/2000002/train/?span=schedule',
            # Табло с поездами завтра: Москва (Белорусский вокзал)
            'station/2000006/train/?span=tomorrow',
            # Табло с поездами сегодня: Санкт - Петербург (Ладожский вокзал)
            'station/9602499/train/',
            # Табло отправление: Москва (Киевский вокзал)
            'station/2000007/',
            # Табло прибытие: Киев-Пасс.
            'station/9614928/?event=arrival',
            # Табло аэропорта: Домодедово
            'station/9600216/',
            # Табло аэропорта Звартноц
            'station/9600369/?filter=all&event=arrival',
            'station/9600369/?event=arrival',
            'station/9600369/?filter=all',
            # Табло с автобусами: Москва, автостанция Новоясеневская
            'station/9746351/?filter=all',
            # Табло с водным
            'stations/water/?city_geo_id=2&utm_source=yamain&utm_medium=block_tickets_mob&utm_campaign=water',
            # Страница направления: Ярославское направление, Белорусское направление
            'direction/?direction=msk_yar',
            'direction/?direction=msk_bel',
            # Страница направления, электрички с курского
            'direction-search/?fromName=&fromId=s2000001&toName=&toId=s9601675&direction=msk_gor&city=213',
            # Направления Москвы
            'stations/train/?city=213',
            'stations/bus/?city=213',
            # Другие направления
            'suburban-directions?appsearch_header=1',

            # Страница нитки: Поезд 020У «Мегаполис»
            'thread/020U_1_2/?station_to=9602494&t_type=train&thread=020U_1_2&station_from=2006004&date={msk_today}&point_to=c2&point_from=c213&departure=2016-10-28&number=020У',
            # Страница нитки: SU 1409
            'thread/SU-1409_0_c26_5/?station_to=9600213&t_type=plane&thread=SU-1409_0_c26_5&station_from=9600370&date={msk_today}&point_to=c213&point_from=c54&departure=2017-02-10&number=SU+1409',
            # Страница автобусной нитки
            'thread/empty_1_f9856685t9833299_271/?station_to=9833299&t_type=bus&thread=empty_1_f9856685t9833299_271&station_from=9856685&date={msk_today}&point_to=c2&point_from=c213',

            # Страница компании перевозчика: "Победа"
            'info/company/9144',
            # Страница компании перевозчика: "РЖД"
            'info/company/112',
            # Страница компании перевозчика: "Авто Турист"
            'info/company/9916',
            # Инфо о вокзале спб
            'info/station/9602497'

        ],
    },
]
