# coding: utf8

def _get_thread_urls(thread):
    """Вызов нитки с разными параметрами"""
    return [
        lambda: f'v3/thread/?uid={thread.uid}',
        lambda: f'v3/thread/?uid={thread.uid}&date={thread.start_date}',
        lambda: f'v3/thread/?uid={thread.uid}&from={thread.station_from}&to={thread.station_to}',
        lambda: f'v3/thread/?uid={thread.uid}&from={thread.station_from}&to={thread.station_to}'
                f'&date={thread.start_date}',
        lambda: f'v3/thread/?uid={thread.uid}&from={thread.station_from}&to={thread.station_to}'
                f'&date={thread.start_date}&result_timezone=Europe%2FLondon',
    ]


class ThreadParams(object):
    """Нитка, полученная в поиске, и используемая потом для вызова ручки нитки"""
    def set_params(self, uid, station_from, station_to, start_date):
        self.uid = uid
        self.station_from = station_from
        self.station_to = station_to
        self.start_date = start_date


class SetThread(object):
    def __init__(self, thread):
        self.thread = thread

    def __call__(self, checker, response):
        segment = response.json()['segments'][0]

        self.thread.set_params(
            uid=segment['thread']['uid'],
            station_from=segment['from']['code'],
            station_to=segment['to']['code'],
            start_date=segment['start_date']
        )


def check_threads(search_url, search_url_params):
    """
    Запуск тестов для нитки
    :param search_url: поиск, из результатов которого выбирается нитка
    :param params: параметры для вызова поиска
    """
    thread = ThreadParams()
    url_params = search_url_params.copy()
    url_params['processes'] = [SetThread(thread)]
    thread_urls = _get_thread_urls(thread)

    return [
        search_url,
        url_params,
        thread_urls
    ]
