from dateutil import parser


def _get_thread_urls(thread):
    """Вызов нитки с разными параметрами"""
    return [
        [
            lambda: f'ru/thread/?mixed_uid=wrong&station_from={thread.station_from}&station_to={thread.station_to}',
            {'code': 404}
        ],

        lambda: f'ru/thread/?mixed_uid={thread.canonical_uid}',

        lambda: f'ru/thread/?mixed_uid={thread.canonical_uid}&departure={thread.departure}',

        lambda: f'ru/thread/?mixed_uid={thread.canonical_uid}&station_from={thread.station_from}'
                f'&station_to={thread.station_to}',

        lambda: f'ru/thread/?mixed_uid={thread.canonical_uid}&station_from={thread.station_from}'
                f'&station_to={thread.station_to}&departure_from={thread.departure_from}',

        lambda: f'ru/thread/?mixed_uid={thread.canonical_uid}&station_from={thread.station_from}'
                f'&station_to={thread.station_to}&departure_from_date={thread.departure_from_date}',

        lambda: f'ru/thread/?mixed_uid={thread.canonical_uid}&station_from={thread.station_from}'
                f'&station_to={thread.station_to}&departure_from_date={thread.departure_from_date}'
                f'&time_zone=Europe%2FMoscow',

        lambda: f'ru/thread/?uid={thread.uid}',

        lambda: f'uk/thread/?uid={thread.uid}&country=UA',

        lambda: f'ru/thread/?uid={thread.uid}&station_from={thread.station_from}'
                f'&station_to={thread.station_to}&departure_from={thread.departure_from}',
    ]


def _get_railway_thread_urls(thread):
    """Вызов железнодорожной нитки"""
    urls = _get_thread_urls(thread)

    urls.extend([
        lambda: f'ru/thread/map/?mixed_uid={thread.canonical_uid}',

        lambda: f'ru/thread/map/?mixed_uid={thread.canonical_uid}&station_from={thread.station_from}'
                f'&station_to={thread.station_to}&departure_from={thread.departure_from}',
    ])
    return urls


class ThreadParams(object):
    """Нитка, полученная в поиске, и используемая потом для вызова ручки нитки"""
    def set_params(self, uid, canonical_uid, station_from, station_to, start_date, departure):
        self.uid = uid
        self.canonical_uid = canonical_uid
        self.station_from = station_from
        self.station_to = station_to
        self.departure = start_date
        self.departure_from = parser.parse(departure).strftime('%Y-%m-%dT%H:%M')
        self.departure_from_date = parser.parse(departure).strftime('%Y-%m-%d')


class SetThread(object):
    def __init__(self, thread):
        self.thread = thread

    def __call__(self, checker, response):
        segment = response.json()['result']['segments'][0]

        self.thread.set_params(
            uid=segment['thread']['uid'],
            canonical_uid=segment['thread']['canonicalUid'],
            station_from=segment['stationFrom']['id'],
            station_to=segment['stationTo']['id'],
            start_date=segment['startDate'],
            departure=segment['departureLocalDt']
        )


def check_threads(search_url, search_url_params, is_railway):
    """
    Запуск тестов для нитки
    :param search_url: поиск, из результатов которого выбирается нитка
    :param params: параметры для вызова поиска
    :param is_railway: нитка является ниткой ЖД
    """
    thread = ThreadParams()
    url_params = search_url_params.copy()
    url_params['processes'] = [SetThread(thread)]
    thread_urls = _get_railway_thread_urls(thread) if is_railway else _get_thread_urls(thread)

    return [
        search_url,
        url_params,
        thread_urls
    ]
