from .env import env


desktop_urls = [
    # Главная
    '',
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
    # Поиск на сегодня электричками в старом формате - без слагов
    'search/suburban/?fromName=Москва+(Ленинградский+вокзал)&toName=Тверь&from,004&toId=s9603093&when=сегодня',
    # Тяжелый поиск всеми видами транспорта: Бровары → Киев на все дни
    'search/?fromName=Бровары&fromId=c22615&toName=Киев&toId=c143&when=на+все+дни',
    # Тяжелый поиск всеми видами транспорта: Бровары → Киев на завтра
    'search/?fromName=Бровары&fromId=c22615&toName=Киев&toId=c143&when=завтра',
    # Тяжелый поиск всеми видами транспорта: Москва → Химки на все дни
    'search/?fromName=Москва&toName=Химки&toId=s9603401&when=на+все+дни',
    # Тяжелый поиск всеми видами транспорта: Москва → Химки на завтра
    'search/?fromName=Москва&toName=Химки&toId=s9603401&when=завтра',
    # Поиск до страны: Екатеринбург → Азербайджан на сегодня
    ['search/?fromName=Екатеринбург&fromId=c54&toName=Азербайджан&toId=&when=сегодня', {'code': 404}],
    # Ранее пятисотящая выдача: Москва (Павелецкий вокзал) → Санкт-Петербург(Московский вокзал)
    'search/?fromName=Москва+(Павелецкий+вокзал)&fromId=s2000005&toName=Санкт-Петербург+(Московский+вокзал)&toId=s9602494&when=на+все+дни',
    # Поиск до аэропорта: Екатеринбург → Кольцово на завтра
    'search/?fromName=Екатеринбург&fromId=c54&toName=Кольцово%2C+Свердловская+область&toId=s9600370&when=завтра',
    # Предзаполнение формы Куда: Санкт-Петербург
    'city/prefill/?active_tab=train&city_id=2&autofocus=toName&when=tomorrow&from_city_id=2',
    # Хороший поиск всеми видами транспорта: Москва → Санкт-Петербург на завтра
    'search/?fromName=Москва&toName=Санкт-Петербург&toId=c2&when=завтра',
    # Хороший поиск всеми видами транспорта: Нижний Новгород → Тверь на все дни
    'search/?fromName=Нижний+Новгород&fromId=c23243&toName=Тверь&toId=c14&when=на+все+дни',
    # Хороший поиск самолетами: Волгоград → Москва на завтра
    'search/plane/?fromName=Волгоград&fromId=c38&toName=Москва+&when=завтра',
    # Пустой поиск самолетами: Иркутск → Абакан на все дни
    'search/plane/?fromName=Иркутск&fromId=c63&toName=Абакан&toId=c1095&when=на+все+дни',
    # Хороший поиск поездами: Улан-Удэ → Чита на завтра
    'search/train/?fromId=c198&fromName=Улан-Удэ&toId=c68&toName=Чита&when=завтра',
    #  Хороший поиск поездами: Красноярск → Ачинск на все дни
    ['search/train/?fromId=c62&fromName=Красноярск&toId=c11302&toName=Ачинск&when=на+все+дни', {'timeout': env.timeout_slow}],
    # Хороший поиск электричками: Лосиноостровская → Чкаловская на завтра
    'search/suburban/?fromId=s9601716&fromName=Лосиноостровская&toId=s9601911&toName=Чкаловская&when=завтра',
    # Хороший поиск электричками: Реутово → Москва на все дни
    'search/suburban/?fromName=Реутово&fromId=s9600796&toName=Москва&when=на+все+дни',
    # Хороший поиск автобусами: Москва → Зеленоград на завтра
    ['search/bus/?fromName=Москва&toName=Зеленоград&toId=c216&when=завтра', {'timeout': env.timeout_slow}],
    # Хороший поиск автобусами: Подольск → Троицк на все дни
    'search/bus/?fromName=Подольск&fromId=c10747&toName=Троицк&toId=c20674&when=на+все+дни',
    # Поиск с сужением: Минск → Марьина Горка электричками на завтра
    'search/suburban/?fromId=c157&fromName=Минск&toId=c23258&toName=Марьина+Горка&when=завтра',
    # Поиск с расширением: Санкт-Петербург (Московский вокзал) → Москва (Киевский вокзал) на завтра
    'search/?fromName=Санкт-Петербург+(Московский+вокзал)&fromId=s9602494&toName=Москва+(Киевский+вокзал)&to,007&when=завтра',
    # Ранее пятисотящий поиск: Харьков - Лозовая
    'search/?fromId=c147&fromName=Харьков&toId=c23122&toName=Лозовая&when=на+все+дни',
    # Ранее пятисотящий поиск: Харьков - Запорожье
    'search/?fromName=Харьков&fromId=c147&toName=Запорожье-1&toId=s9617051&when=на+все+дни',
    # Ранее пятисотящий поиск: Харьков-Пасс - Змиёв
    'search/?fromName=Харьков-Пасс.&fromId=s9615638&toName=Змиёв&toId=c23118&when=на+все+дни',
    # Ранее пятистоящий поиск: Запорожье - Харьков
    'search/?fromName=Запорожье&toName=Орехов&fromId=c960&toId=c23651&when=на+все+дни',
    # Ранее пятисотящий поиск: Москва - Курск на дальние даты
    'search/train/?fromName=Москва&toName=Курск&when=через+месяц',
    # Ранее пятисотящий поиск: Поиск до скрытого города
    'search/?fromName=Челябинск&toName=&fromId=c56&toId=c11134&when=завтра',

    # Поиски со слагами
    ['all-transport/moscow--mytischi', {'timeout': env.timeout_very_slow + 5}],
    'suburban/moscow--tver',
    'suburban/moscow-oktyabrskaya--tver-train-station',
    ['train/moscow--saint-petersburg', {'timeout': env.timeout_very_slow}],
    'plane/moscow--yekaterinburg',
    'bus/moscow--voronezh',
    'suburban/moscow--tver/today',
    'suburban/moscow-oktyabrskaya--tver-train-station/today',

    # Ссылка на выдачу аэроэкспрессами с главной
    'suburban/moscow--domodedovo-airport?aeroex=y',
    # Ссылка на выдачу аэроэкспрессами со старых страниц (например, информации о станции)
    'search/suburban/?toName=Домодедово&fromName=Москва&toId=s9600216&aeroex=y&fromId=c213',

    # Страница Табло Домодедово
    'station/9600216/',
    # Табло Домодедово, прибытие
    'station/9600216/?event=arrival',
    'station/9600216/?event=departure',
    'station/9600216/?type=suburban&event=departure',
    'station/9600216/?direction=arrival&type=suburban&event=departure',
    'station/9600216/?direction=all&type=suburban&event=departure',
    # Вокзал
    'station/2000003',
    # Автовокзал
    'station/9860432',
    'station/9860432?span=tomorrow',
    # Страница станции: Подволошная
    'station/9607448?type=suburban&span=g16',
    # Страница с информацией о станции: Екатеринбург-Пасс
    'info/station/9607404',
    # Информация о станции Домодедово
    'info/station/9600216',
    # Страница направления электричек: Егоршенское направление
    'city/54/direction?direction=ekt_ego',
    # Страница направления электричек: Киевское направление
    'city/213/direction?direction=msk_kiv',
    # Страница электричечной нитки: Поезд 6457, Москва (Киевский вокзал) — Нара
    ['thread/6457_0_2000007_g18_4', {'timeout': env.timeout_slow}],
    # Страница автобусной нитки: Маршрут автобуса Воронеж — Курск — Киев
    'thread/empty_3_f9850359t9692694_352?tt=bus',
    # Страница нитки поезда: Фирменный поезд «Россия» 001М, Владивосток — Москва
    'thread/001M_0_2?from_search=1&tt=train&number=001М&lang=ru',
    # Страница нитки аэроэкспресса: Поезд 7230, Москва (Белорусский вокзал) — аэропорт Шереметьево
    ['thread/7230_0_2000006_g18_4?tt=suburban', {'timeout': env.timeout_very_slow}],

    # Страница с партнерами
    'info/partners',
    ['info/region/1', {'timeout': env.timeout_slow}],
    'info/geo',

    # Привязка цен к электричкам на Украине: Киев → Фастов на завтра
    'search/?fromName=Киев&fromId=c143&toName=Фастов&toId=c22619&when=завтра',
    # Выдача только автобусами
    'search/bus/?fromName=Чебоксары&toName=Нижний+Новгород&fromId=c45&toId=c23243&when=завтра',
    # Выдача с автобусами, где попадают нулевые сегменты
    'search/?fromName=Челябинск&toName=Миасс&fromId=c56&toId=c11212&when=завтра',
    # Автобуснаыя нитка, которая 500 при опозданиях
    'thread/1095_28_f9816952t9816958_44?point_to=s9744842&tt=bus&point_from=s9744800',
    # Пятисотящая выдача, где в номере электричек есть буква
    'search/?fromName=Новая+Боровая&toName=Винница&fromId=c25068&toId=c963&when=завтра',
    # Нитка аэропорта без города
    'thread/YC-262_3_c69_547?tt=plane&departure=2017-11-01',
]

touch_urls = [
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
    # Тяжелый поиск автобусами: Бровары - Киев
    'search/bus/?toName=Киев&fromName=Бровары&when=завтра&toId=c143&delta=1&fromId=c22615',
    # Тяжелый поиск электричками: Москва - Химки
    ['search/?fromName=Москва&toName=Химки&when=завтра&fromId=c213&toId=s9603401', {'timeout': env.timeout_slow}],
    # Выдача аэроэкспрессами: Белорусский вокзал - Шереметьево (Аэропорт Шереметьево - Северный терминал (B, C))
    'search/suburban/?toName=Шереметьево&fromName=Москва&toId=s9881704&aeroex=y&fromId=c213',
    # Хороший поиск самолетами: Улан-Удэ - Москва
    'search/plane/?toName=Москва&fromName=Улан-Удэ&when=завтра&toId=c213&delta=1&fromId=c198',
    # Хороший поиск поездами: Абакан - Красноярск
    'search/?fromName=Абакан&toName=Красноярск&when=завтра&fromId=c1095&toId=c62',

    # Табло
    # Табло с электричками: Москва (Курский вокзал)
    ['station/2000001/suburban/?filter=all', {'timeout': env.timeout_slow}],
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
    # Табло с автобусами: Москва, автостанция Новоясеневская
    'station/9746351/?filter=all',

    # Страница направления: Ярославское направление
    'direction/?direction=msk_yar',

    # Страница нитки: Поезд 020У «Мегаполис»
    'thread/020U_1_2/?station_to=9602494&t_type=train&thread=020U_1_2&station_from=2006004&date=2016-10-28&point_to=c2&point_from=c213&departure=2016-10-28&number=020У',
    # Страница автобусной нитки
    'thread/empty_1_f9856685t9833299_271/?station_to=9833299&t_type=bus&thread=empty_1_f9856685t9833299_271&station_from=9856685&date=2016-10-28&point_to=c2&point_from=c213',
]
