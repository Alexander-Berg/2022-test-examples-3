import factory
import pytest
from multidict import MultiDict

from travel.library.python.aioapp.utils import localize_dt
from travel.rasp.pathfinder_proxy.cache import AbstractCache
from travel.rasp.pathfinder_proxy.const import UTC_TZ, TTransport
from travel.rasp.pathfinder_proxy.settings import Settings


@pytest.fixture
def cache():
    class Cache(AbstractCache):
        def __init__(self):
            self.db = {}

        async def set_cache(self, cache_type, point_from, point_to, when, tld, language, result, transport_types=None):
            key = self._build_cache_key(cache_type, point_from, point_to, when, tld, language)
            self.db[key] = result

        async def get_from_cache(self, cache_type, point_from, point_to, when, tld, language, transport_types=None):
            key = self._build_cache_key(cache_type, point_from, point_to, when, tld, language)
            return self.db.get(key)

    return Cache()


@pytest.fixture
def settings():
    return Settings()


@pytest.fixture
def request_not_bot():
    class RequestFactory(factory.Factory):
        query = MultiDict([
            ('pointFrom', 'c39'),
            ('pointTo', 'c54'),
            ('tld', 'ru'),
            ('language', 'ru'),
            ('when', '2019-08-20'),
            ('prices_on', False),
            ('transportType', 'train'),
            ('transportType', 'plane'),
            ('isBot', 'false'),
            ('includePriceFee', 'true'),
        ])
        headers = {}

    return RequestFactory


@pytest.fixture
def request_is_bot():
    class RequestFactory(factory.Factory):
        query = MultiDict([
            ('pointFrom', 'c39'),
            ('pointTo', 'c54'),
            ('tld', 'ru'),
            ('language', 'ru'),
            ('when', '2019-08-20'),
            ('prices_on', False),
            ('transportType', 'train'),
            ('transportType', 'plane'),
            ('isBot', 'true'),
            ('includePriceFee', 'true'),
        ])
        headers = {}

    return RequestFactory


@pytest.fixture
def transfer_variants():
    return [
        {
            'segments': [
                {
                    'transport': {'code': 'train'},
                    'departure': '2019-08-06T15:10:00+00:00',
                    'arrival': '2019-08-08T07:20:00+00:00',
                    'stationFrom': {'id': 9609235},
                    'stationTo': {'id': 9605179},
                    'thread': {'number': 'train_1'}
                },
                {
                    'transport': {'code': 'train'},
                    'departure': '2019-08-08T07:50:00+00:00',
                    'arrival': '2019-08-08T15:19:00+00:00',
                    'stationFrom': {'id': 9605179},
                    'stationTo': {'id': 9612913},
                    'thread': {'number': 'train_2'}
                }
            ]
        },
        {
            'segments': [
                {
                    'transport': {'code': 'plane'},
                    'departure': '2019-08-20T18:30:00+00:00',
                    'arrival': '2019-08-20T20:15:00+00:00',
                    'stationFrom': {'id': 9866615},
                    'stationTo': {'id': 9600216},
                    'thread': {'number': 'plane_1'}
                },
                {
                    'transport': {'code': 'plane'},
                    'departure': '2019-08-21T07:00:00+00:00',
                    'arrival': '2019-08-21T09:20:00+00:00',
                    'stationFrom': {'id': 9600216},
                    'stationTo': {'id': 9600370},
                    'thread': {'number': 'plane_2'}
                }
            ]
        }
    ]


@pytest.fixture
def transfer_variants_mixed():
    return [
        {
            'segments': [
                {
                    'transport': {'code': 'train'},
                    'departure': '2019-08-06T15:10:00+00:00',
                    'arrival': '2019-08-08T07:20:00+00:00',
                    'stationFrom': {'id': 9609235},
                    'stationTo': {'id': 9605179},
                    'thread': {'number': 'train_1'}
                },
                {
                    'transport': {'code': 'bus'},
                    'departure': '2019-08-08T07:50:00+00:00',
                    'arrival': '2019-08-08T15:19:00+00:00',
                    'stationFrom': {'id': 9605179},
                    'stationTo': {'id': 9612913},
                    'thread': {'number': 'bus_1'}
                }
            ]
        }
    ]


@pytest.fixture
def transfer_variants_with_prices():
    return [
        {
            'segments': [
                {
                    'transport': {'code': 'train'},
                    'departure': '2019-08-06T15:10:00+00:00',
                    'arrival': '2019-08-08T07:20:00+00:00',
                    'stationFrom': {'id': 9609235},
                    'stationTo': {'id': 9605179},
                    'thread': {'number': 'train_1'},
                    'hasDynamicPricing': True,
                    'rawTrainName': None,
                    'provider': 'P1',
                    'tariffs': {
                        "electronicTicket": True,
                        "classes": {
                            "compartment": {
                                "price": {
                                    "currency": "RUB",
                                    "value": 1000.10
                                },
                                "servicePrice": {
                                    "currency": "RUB",
                                    "value": 100.0
                                }
                            }
                        }
                    }
                },
                {
                    'transport': {'code': 'train'},
                    'departure': '2019-08-08T07:50:00+00:00',
                    'arrival': '2019-08-08T15:19:00+00:00',
                    'stationFrom': {'id': 9605179},
                    'stationTo': {'id': 9612913},
                    'thread': {'number': 'train_2'},
                    'hasDynamicPricing': True,
                    'rawTrainName': None,
                    'provider': 'P1',
                    'tariffs': {
                        "brokenClasses": {
                            "compartment": [
                                5
                            ]
                        }
                    }
                }
            ]
        },
        {
            'segments': [
                {
                    'transport': {'code': 'plane'},
                    'departure': '2019-08-20T18:30:00+00:00',
                    'arrival': '2019-08-20T20:15:00+00:00',
                    'stationFrom': {'id': 9866615},
                    'stationTo': {'id': 9600216},
                    'thread': {'number': 'plane_1'}
                },
                {
                    'transport': {'code': 'plane'},
                    'departure': '2019-08-21T07:00:00+00:00',
                    'arrival': '2019-08-21T09:20:00+00:00',
                    'stationFrom': {'id': 9600216},
                    'stationTo': {'id': 9600370},
                    'thread': {'number': 'plane_2'}
                }
            ]
        }
    ]


@pytest.fixture
def suburban_transfer_variants():
    return [
        {
            'segments': [
                {
                    'transport': {'code': 'suburban'},
                    'departure': '2019-08-06T15:10:00+00:00',
                    'arrival': '2019-08-08T07:20:00+00:00',
                    'stationFrom': {'id': 9609235},
                    'stationTo': {'id': 9605179},
                    'thread': {'number': 'suburban_1'},
                    'trainNumbers': ['train_1']
                },
                {
                    'transport': {'code': 'suburban'},
                    'departure': '2019-08-08T07:50:00+00:00',
                    'arrival': '2019-08-08T15:19:00+00:00',
                    'stationFrom': {'id': 9605179},
                    'stationTo': {'id': 9612913},
                    'thread': {'number': 'suburban_2'},
                    'trainNumbers': None
                }
            ]
        }
    ]


def create_train_api_collector_entry(from_id, to_id, departure, price, number=None):
    return {
        'key': (from_id, to_id, localize_dt(departure, UTC_TZ)),
        'result': {
            'tariff': {
                'classes': {
                    'platzkarte': {
                        'price': {'currency': 'RUB', 'value': price},
                        'trainOrderUrl': '/'
                    }
                }
            },
            'number': number,
            'provider': 'P1',
            'hasDynamicPricing': True,
            'rawTrainName': None
        }
    }


def create_train_api_service_entry(price):
    return {
        'classes': {
            'platzkarte': {
                'price': {'currency': 'RUB', 'value': price},
                'trainOrderUrl': '/'
            }
        },
    }


def create_ticket_daemon_collector_entry(from_id, to_id, departure, price):
    return {
        'key': (from_id, to_id, localize_dt(departure, UTC_TZ)),
        'result': {
            'electronicTicket': False,
            'classes': {
                'economy': {
                    'partner': 'test_partner',
                    'price': {'currency': 'RUB', 'value': price},
                    'orderUrl': 'https://travel.yandex.ru/avia/'
                }
            }
        }
    }


def create_ticket_daemon_service_entry(price):
    return {
        'electronicTicket': False,
        'classes': {
            'economy': {
                'partner': 'test_partner',
                'price': {'currency': 'RUB', 'value': price},
                'orderUrl': 'https://travel.yandex.ru/avia/'
            }
        }
    }


def create_interline_collector_entry(key, price):
    return {
        'key': tuple(
            (from_id, to_id, localize_dt(departure, UTC_TZ), 'plane_{}'.format(number))
            for from_id, to_id, departure, number in key
        ),
        'result': {
            'electronicTicket': False,
            'classes': {
                'economy': {
                    'partner': 'test_partner',
                    'price': {'currency': 'RUB', 'value': price},
                    'orderUrl': 'https://travel.yandex.ru/avia/'
                }
            }
        }
    }


class CollectorStub:
    def __init__(self, tariff_infos, querying):
        self._tariff_infos = tariff_infos
        self._querying = querying

    async def iter_tariffs_for_variants(self, transfer_variants, tld, language):
        yield self._tariff_infos, self._querying


class CollectorStubCreator:
    def __init__(self, tariff_infos, querying):
        self._tariff_infos = tariff_infos
        self._querying = querying

    def __call__(self):
        return CollectorStub(self._tariff_infos, self._querying)


class TrainFeeServiceStub:
    def __init__(self, fee=0.0):
        self.fee = fee

    async def apply_fee(self, transfer_variants, icookie, bandit_type, yandex_uid, user_device, req_id):
        transfer_variants = list(transfer_variants)
        for transfer_index, transfer_variant in enumerate(transfer_variants):
            if not transfer_variant.get('segments'):
                continue
            segment_datas = list(transfer_variant['segments'])
            for segment_index, segment_data in enumerate(segment_datas):
                transport = segment_data['transport']['code']
                if transport != TTransport.get_name(TTransport.TRAIN):
                    continue
                if not segment_data.get('tariffs') or not segment_data['tariffs'].get('classes'):
                    continue
                classes = segment_data['tariffs']['classes']
                for car_type, class_data in classes.items():
                    if class_data['price']:
                        class_data['price']['value'] += self.fee
        return transfer_variants
