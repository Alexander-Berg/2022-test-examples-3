# coding: utf8
from travel.rasp.smoke_tests.smoke_tests.config.api_public.env import env
from travel.rasp.smoke_tests.smoke_tests.config.api_public.content_checkers import check_threads


def url_processor(url):
    ch = '&' if '?' in url else '?'
    key = f'{ch}apikey={env.api_key}'
    return url + key


checks = [
    {
        'host': env.host,
        'params': {'timeout': env.timeout, 'url_processor': url_processor},
        'urls': [
            'ping',
            'version',

            # Разное написание номера версии
            'v3/copyright',
            'v3.0/copyright',
            'v3.0.0/copyright',

            # Поиск

            # Челябинск - Екатеринбург  (автобусы, поезда)
            ['v3/search/?from=c56&to=c54', {'url_processor': None, 'code': 400}],  # идем без ключа -> 400
            'v3/search/?from=c56&to=c54',
            'v3/search/?from=c56&to=c54&transport_types=bus',
            'v3/search/?from=c56&to=c54&transport_types=train',
            'v3/search/?from=c56&to=c54&transport_types=suburban',
            'v3/search/?from=c56&to=c54&transport_types=plane',
            'v3/search/?from=c56&to=c54&lang=uk_UA',
            'v3/search/?from=c56&to=c54&result_timezone=Europe%2FMoscow',
            'v3/search/?from=c56&to=c54&add_days_mask=true',
            'v3/search/?from=c56&to=c54&limit=10&offset=10',
            'v3/search/?from=c56&to=c54&add_days_mask=true&format=xml',
            'v3/search/?system=esr&from=800008&to=780506&show_systems=esr',

            'v3/search/?from=c56&to=c54&date={msk_today}',
            'v3/search/?from=c56&to=c54&date={msk_today}&transport_types=bus',
            'v3/search/?from=c56&to=c54&date={msk_today}&transport_types=train%2Cbus',
            'v3/search/?from=c56&to=c54&date={msk_today}&result_timezone=Europe%2FMoscow',
            'v3/search/?from=c56&to=c54&date={msk_today}&add_days_mask=true',

            # Екатеринбург-Пасс - Северка (электрички)
            'v3/search/?from=s9607404&to=s9607451',
            'v3/search/?from=s9607404&to=s9607451&transport_types=suburban',
            'v3/search/?from=s9607404&to=s9607451&date={msk_today}',

            # Москва - Химки (много ниток, есть интервальные)
            ['v3/search/?from=c213&to=c10758&date={msk_today}', {'timeout': env.timeout_very_slow}],
            ['v3/search/?from=c213&to=c10758', {'timeout': env.timeout_very_slow}],

            # Екатеринбург - Москва (самолеты, поезда)
            ['v3/search/?from=c54&to=c213', {'timeout': env.timeout_slow}],
            ['v3/search/?from=c54&to=c213&result_timezone=Europe%2FMoscow', {'timeout': env.timeout_slow}],
            ['v3/search/?from=c54&to=c213&add_days_mask=true', {'timeout': env.timeout_slow}],
            'v3/search/?from=c54&to=c213&transport_types=plane',

            'v3/search/?from=c54&to=c213&date={msk_today}',
            'v3/search/?from=c54&to=c213&date={msk_today}&result_timezone=Europe%2FMoscow',
            'v3/search/?from=c54&to=c213&date={msk_today}&add_days_mask=true',
            'v3/search/?from=c54&to=c213&date={msk_today}&transport_types=plane',

            # Екатеринбург - Рязань (только пересадки)
            'v3/search/?from=c54&to=c11&date={msk_today}',
            'v3/search/?from=c54&to=c11&date={msk_today}&transfers=true',
            'v3/search/?from=c54&to=c11&date={msk_today}&transfers=true&transport_types=train%2Cbus',

            # Екатеринбург - Благовещенск (прямые рейсы и пересадки)
            'v3/search/?from=c54&to=c77&date={msk_today}',
            'v3/search/?from=c54&to=c77&date={msk_today}&transfers=true',
            'v3/search/?from=c54&to=c77&date={msk_today}&transfers=true&transport_types=plane',

            # Станция

            # Екатеринбург-Пасс
            ['v3/schedule/?station=s9607404', {'url_processor': None, 'code': 400}],  # идем без ключа -> 400
            'v3/schedule/?station=s9607404',
            'v3/schedule/?station=s9607404&transport_types=train',
            'v3/schedule/?station=s9607404&transport_types=plane',
            'v3/schedule/?station=s9607404&transport_types=suburban',
            'v3/schedule/?station=s9607404&transport_types=suburban&direction=на+Шалю',
            'v3/schedule/?station=s9607404&result_timezone=Europe%2FMoscow',
            'v3/schedule/?station=s9607404&event=departure',
            'v3/schedule/?station=s9607404&event=arrival',
            'v3/schedule/?station=s9607404&event=arrival&result_timezone=Europe%2FMoscow',

            'v3/schedule/?station=s9607404&date={msk_today}',
            'v3/schedule/?station=s9607404&transport_types=train&date={msk_today}',
            'v3/schedule/?station=s9607404&transport_types=suburban&date={msk_today}',
            'v3/schedule/?station=s9607404&transport_types=suburban&direction=на+Шалю&date={msk_today}',
            'v3/schedule/?station=s9607404&result_timezone=Europe%2FMoscow&date={msk_today}',
            'v3/schedule/?station=s9607404&event=arrival&date={msk_today}',

            'v3/schedule/?station=s9607404&lang=uk_UA',
            'v3/schedule/?station=s9607404&limit=10&offset=10',
            'v3/schedule/?station=s9607404&format=xml',
            'v3/schedule/?system=esr&station=780506&show_systems=esr',

            # Белорусский вокзал
            'v3/schedule/?station=s2000006',
            'v3/schedule/?station=s2000006&transport_types=suburban',
            'v3/schedule/?station=s2000006&event=arrival',

            'v3/schedule/?station=s2000006&date={msk_today}',
            'v3/schedule/?station=s2000006&transport_types=suburban&date={msk_today}',
            'v3/schedule/?station=s2000006&event=arrival&date={msk_today}',

            # Кольцово
            'v3/schedule/?station=s9600370',
            'v3/schedule/?station=s9600370&transport_types=plane',
            'v3/schedule/?station=s9600370&transport_types=suburban',
            'v3/schedule/?station=s9600370&transport_types=train',
            'v3/schedule/?station=s9600370&result_timezone=Europe%2FMoscow',
            'v3/schedule/?station=s9600370&event=arrival',
            'v3/schedule/?station=s9600370&lang=uk_UA',

            'v3/schedule/?station=s9600370&date={msk_today}',
            'v3/schedule/?station=s9600370&transport_types=plane&date={msk_today}',
            'v3/schedule/?station=s9600370&transport_types=suburban&date={msk_today}',
            'v3/schedule/?station=s9600370&transport_types=train&date={msk_today}',
            'v3/schedule/?station=s9600370&result_timezone=Europe%2FMoscow&date={msk_today}',
            'v3/schedule/?station=s9600370&event=arrival&date={msk_today}',
            'v3/schedule/?station=s9600370&tablo=true&date={msk_today}',
            'v3/schedule/?station=s9600370&tablo=true&event=arrival&date={msk_today}',

            # Шереметьево
            ['v3/schedule/?station=s9600213', {'timeout': env.timeout_slow}],
            ['v3/schedule/?station=s9600213&event=arrival', {'timeout': env.timeout_slow}],

            'v3/schedule/?station=s9600213&date={msk_today}',
            'v3/schedule/?station=s9600213&tablo=true&date={msk_today}',
            'v3/schedule/?station=s9600213&tablo=true&event=arrival&date={msk_today}',

            # Бутаково (автобусная остановка в Химках), есть интервальные рейсы
            ['v3/schedule/?station=s9743011', {'timeout': env.timeout_slow}],
            ['v3/schedule/?station=s9743011&transport_types=bus', {'timeout': env.timeout_slow}],
            ['v3/schedule/?station=s9743011&transport_types=suburban', {'timeout': env.timeout_slow}],
            ['v3/schedule/?station=s9743011&event=arrival', {'timeout': env.timeout_slow}],
            ['v3/schedule/?station=s9743011&result_timezone=Europe%2FMoscow', {'timeout': env.timeout_slow}],

            'v3/schedule/?station=s9743011&date={msk_today}',
            'v3/schedule/?station=s9743011&transport_types=bus&date={msk_today}',
            'v3/schedule/?station=s9743011&transport_types=plane&date={msk_today}',
            'v3/schedule/?station=s9743011&event=arrival&date={msk_today}',

            # Нитка поезда, Екатеринбург - Москва
            check_threads(
                'v3/search/?from=c54&to=c213&date={msk_today}&transport_types=train',
                {'name': 'Train thread'}
            ),

            # Нитка электрички, Белорусский вокзал - Шереметьево (Аэропорт Шереметьево - Северный терминал (B, C))
            check_threads(
                'v3/search/?from=s2000006&to=s9881704&date={msk_today}&transport_types=suburban',
                {'name': 'Suburban thread'}
            ),

            # Нитка автобуса, Екатеринбург - Челябинск
            check_threads(
                'v3/search/?from=c54&to=c56&date={msk_today}&transport_types=bus',
                {'name': 'Bus thread'}
            ),

            # Нитка самолета, Москва - Санкт-Петербург
            check_threads(
                'v3/search/?from=c213&to=c2&date={msk_today}&transport_types=plane',
                {'name': 'Plane thread'}
            ),

            # Другие ручки

            'v3/nearest_stations/?distance=30&lng=60&lat=56',
            'v3/nearest_settlement/?distance=30&lng=60&lat=56',

            ['v3/stations_list/', {'timeout': env.timeout_very_slow}],

            'v3/carrier/?code=320',

            # Старые версии API

            'v1.0/carrier/?format=json&lang=ru&code=55874&system=yandex',
            'v1.0/nearest_stations/?format=json&lat=56.838011&lng=60.597465&distance=5&lang=ru&transport_type=plane,train,suburban',
            'v1.0/nearest_points/?lat=48.004185&lng=33.459352&distance=30&apikey=4d659d73-da4e-4a05-9e59-8a30393bc4b5&format=json&lang=ru',

            'v2/route/?to=c6&from=c213&format=json',
        ],
    },
]
