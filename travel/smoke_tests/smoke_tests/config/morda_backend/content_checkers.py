# coding: utf8

from travel.rasp.smoke_tests.smoke_tests.common_content_checkers import MinItemsCount
from travel.rasp.smoke_tests.smoke_tests.config.morda_backend.env import env


class SearchPlatformsPercent(object):
    def __init__(self, percent):
        self.percent = percent

    def __call__(self, checker, response):
        platforms = []
        for seg in response.json()['result']['segments']:
            platforms.append(seg['stationFrom']['platform'])
            platforms.append(seg['stationTo']['platform'])

        real_platforms = [p for p in platforms if p]

        if platforms and len(real_platforms) / len(platforms) * 100 < self.percent:
            raise Exception(
                f'Count of known platforms is less than {self.percent}%, '
                f'({len(platforms)} known, {len(real_platforms)} total)'
            )


class SearchEventsPercent(object):
    def __init__(self, percent):
        self.percent = percent

    def __call__(self, checker, response):
        segments = response.json()['result']['segments']

        num_arrival_events = num_departure_events = 0
        for segment in segments:
            if 'arrivalEvent' in segment:
                num_arrival_events += 1
            if 'departureEvent' in segment:
                num_departure_events += 1

        if num_arrival_events / len(segments) * 100 < self.percent:
            raise Exception(
                f'Count of known arrival events is less than {self.percent}%, '
                f'({num_arrival_events} known, {len(segments)} total)'
            )

        if num_departure_events / len(segments) * 100 < self.percent:
            raise Exception(
                f'Count of known departure events is less than {self.percent}%, '
                f'({num_departure_events} known, {len(segments)} total)'
            )


class PlaneTariffsQid(object):
    """Идентификатор запроса к тикет-демону"""
    def set_params(self, qid):
        self.qid = qid


class PlaneTariffsQuid(object):
    def __init__(self, tariffs_qid):
        self.tariffs_qid = tariffs_qid

    def __call__(self, checker, response):
        qid = response.json()['qids'][0]
        self.tariffs_qid.set_params(qid)


def check_tariffs(init_url, min_items_count):
    tariffs_qid = PlaneTariffsQid()
    url_params = {'processes': [PlaneTariffsQuid(tariffs_qid)]}
    tariffs_poll_url = [[
        lambda: f'ru/tariffs/plane/poll/?qid={tariffs_qid.qid}',
        {
            'processes': [MinItemsCount(min_items_count, ['segments'])] if env.check_avia_tariffs else [],
            'retries': 10,
            'retries_delay': 2
        }
    ]]

    return [init_url, url_params, tariffs_poll_url]
