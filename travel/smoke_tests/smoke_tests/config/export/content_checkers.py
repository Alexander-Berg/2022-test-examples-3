# coding: utf8

class SuburbanSellingCheck(object):
    content_dependent = True

    def __init__(self, days, segments, selling_tariffs, selling_partners):
        self.days = days
        self.segments = segments
        self.selling_tariffs = selling_tariffs
        self.selling_partners = selling_partners

    def __call__(self, checker, response):
        res = response.json()

        if len(res['days']) < self.days:
            raise Exception(f'Not enough days. Found {len(res["days"])}, should be: {self.days}')
        if len(res['days'][0]['segments']) < self.segments:
            raise Exception(
                f'Not enough segments in response. Found {len(res["days"][0]["segments"])}, should be: {self.segments}'
            )
        if len(res['selling_tariffs']) < self.selling_tariffs:
            raise Exception(
                f'Not enough selling_tariffs. Found {len(res["selling_tariffs"])}, should be: {self.selling_tariffs}'
            )
        if len(res['selling_partners']) != self.selling_partners:
            raise Exception(
                f'Not enough selling_partners. Found {len(res["selling_partners"])}, should be: {self.selling_partners}'
            )


class SearchPlatformsPercent(object):
    content_dependent = True

    def __init__(self, percent):
        self.percent = percent

    def __call__(self, checker, response):
        platforms = []
        for day_data in response.json()['days']:
            for seg in day_data['segments']:
                platforms.append(seg['departure'].get('platform'))
                platforms.append(seg['arrival'].get('platform'))

        real_platforms = [p for p in platforms if p]

        if platforms and len(real_platforms) / len(platforms) * 100 < self.percent:
            raise Exception('Count of known platforms is less than {}% ({} known, {} total)'.format(
                self.percent, len(platforms), len(real_platforms)))


def _get_thread_urls(thread, date):
    """Вызов нитки с разными параметрами"""
    return [
        lambda: f'v3/suburban/thread_on_date/{thread.uid}/?date={thread.start_date}',
        lambda: f'v3/suburban/thread_on_date/{thread.uid}/?date={thread.start_date}'
                f'&station_from={thread.station_from}&station_to={thread.station_to}',
        lambda: f'v3/suburban/thread_on_date/{thread.uid}/?date={date}',
        lambda: f'v3/suburban/thread_on_date/{thread.uid}/?date={date}'
                f'&station_from={thread.station_from}&station_to={thread.station_to}',

        lambda: f'v3/suburban/thread/{thread.uid}/?lang=ru_RU',
        lambda: f'v3/suburban/thread/{thread.uid}/?lang=ru_RU'
                f'&station_from={thread.station_from}&station_to={thread.station_to}',

        lambda: f'export/suburban/thread/{thread.uid}',
        lambda: f'export/v2/suburban/thread/{thread.uid}/?lang=ru&national_version=ru'
    ]


class ThreadParams(object):
    """Нитка, полученная в поиске, и используемая потом для вызова ручки нитки"""
    def set_params(self, uid, canonical_uid, start_date, station_from, station_to):
        self.uid = uid
        self.canonical_uid = canonical_uid
        self.start_date = start_date
        self.station_from = station_from
        self.station_to = station_to


class SetThread(object):
    def __init__(self, thread):
        self.thread = thread

    def __call__(self, checker, response):
        day = response.json()['days'][0]
        segment = day['segments'][-1]

        self.thread.set_params(
            uid=segment['thread']['uid'],
            canonical_uid=segment['thread']['canonical_uid'],
            start_date=day['date'],
            station_from=segment['departure']['station'],
            station_to=segment['arrival']['station']
        )


def check_threads(search_url, search_url_params, date):
    """
    Запуск тестов для нитки
    :param search_url: поиск, из результатов которого выбирается нитка
    :param params: параметры для вызова поиска
    """
    thread = ThreadParams()
    url_params = search_url_params.copy()
    url_params['processes'] = [SetThread(thread)]
    thread_urls = _get_thread_urls(thread, date)

    return [
        search_url,
        url_params,
        thread_urls
    ]


class TrainSellingParams(object):
    def set_params(self, url):
        self.url = url


class SetTrainSellingPath(object):
    def __init__(self, train_selling):
        self.train_selling = train_selling

    def __call__(self, checker, response):
        selling_url = None
        for day_data in response.json()['days']:
            for seg in day_data['segments']:
                selling_info = seg.get('selling_info')
                if selling_info and selling_info['type'] == 'train':
                    selling_url = selling_url or selling_info['tariffs'][0]['order_url']

        if not selling_url:
            raise Exception('No selling links found')

        self.train_selling.set_params(url=selling_url)


def check_train_selling_check(search_url, search_url_params):
    """
    Запуск тестов для нитки
    :param search_url: поиск, из результатов которого выбирается нитка
    :param params: параметры для вызова поиска
    """
    train_selling = TrainSellingParams()
    url_params = search_url_params.copy()
    url_params['processes'] = [SetTrainSellingPath(train_selling)]
    train_selling_urls = [[
        lambda: train_selling.url,
        {'use_host': False}
    ]]

    return [
        search_url,
        url_params,
        train_selling_urls
    ]
