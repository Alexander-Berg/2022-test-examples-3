# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import mock
import pytest
from django.utils.http import urlunquote
from hamcrest import assert_that, contains_inanyorder, has_entries
from six.moves.urllib_parse import urlparse, parse_qs

from common.apps.train_order.enums import CoachType
from common.models.geo import Country
from common.models.schedule import RThread
from common.models.transport import TransportType
from common.tester.factories import create_station, create_settlement, create_thread
from common.tester.utils.replace_setting import replace_setting, replace_dynamic_setting
from common.utils.date import UTC_TZ, KIEV_TZ
from travel.rasp.train_api.tariffs.train.base.country_availability_manager import country_availability_manager
from travel.rasp.train_api.tariffs.train.base.utils import (
    make_segment_train_keys, TRAIN_KEY_FORMAT, make_tariff_segment_key, _build_train_order_url,
    make_segment_suburban_express_keys, can_sale_on_rasp
)
from travel.rasp.train_api.train_partners.ufs.reserve_tickets import ADVERT_DOMAIN
from travel.rasp.train_api.train_purchase.core.enums import TrainOrderUrlOwner

pytestmark = pytest.mark.dbuser('module')


class SegmentStub(object):
    def __init__(self, number, departure, number_to_get_route=None, old_ufs_order=False,
                 t_type_id=TransportType.TRAIN_ID, train_purchase_numbers=None, provider=None, coach_owners=[]):
        self.number = number
        self.train_number_to_get_route = number_to_get_route or number
        self.original_number = (train_purchase_numbers and train_purchase_numbers[0]) or number
        self.departure = departure
        self.thread = RThread(t_type_id=t_type_id)
        self.old_ufs_order = old_ufs_order
        self.title = self.thread.title
        self.first_country_code = None
        self.last_country_code = None
        self.provider = provider
        self.coach_owners = coach_owners

        if train_purchase_numbers:
            self.train_purchase_numbers = train_purchase_numbers


class TariffSegmentStub(object):
    def __init__(self, original_number, departure, coach_type=CoachType.COMPARTMENT.value):
        self.original_number = original_number
        self.departure = departure
        self.thread = RThread(t_type_id=TransportType.TRAIN_ID)
        self.coach_type = coach_type


@replace_setting('ALIGN_SEARCH_SEGMENT_KEYS', True)
def test_make_segment_train_keys_aligned():
    assert_that(
        make_segment_train_keys(SegmentStub('001Ы', datetime(2016, 1, 1, 10))),
        contains_inanyorder(
            TRAIN_KEY_FORMAT.format('001Ы', '20160101_10'),
            TRAIN_KEY_FORMAT.format('001Ы', '20160101_08'),
            TRAIN_KEY_FORMAT.format('002Ы', '20160101_08'),
            TRAIN_KEY_FORMAT.format('002Ы', '20160101_10')
        )
    )


@replace_setting('ALIGN_SEARCH_SEGMENT_KEYS', False)
def test_make_segment_train_keys_not_aligned():
    assert_that(
        make_segment_train_keys(SegmentStub('001Ы', datetime(2016, 1, 1, 10, 33))),
        contains_inanyorder(
            TRAIN_KEY_FORMAT.format('001Ы', '20160101_1033'),
            TRAIN_KEY_FORMAT.format('002Ы', '20160101_1033')
        )
    )


@replace_setting('ALIGN_SEARCH_SEGMENT_KEYS', True)
def test_make_segment_suburban_express_keys_aligned():
    assert_that(
        make_segment_suburban_express_keys(
            SegmentStub('001', datetime(2016, 1, 1, 10, 33), t_type_id=TransportType.SUBURBAN_ID,
                        train_purchase_numbers=['001Ы', '014Б'])
        ),
        contains_inanyorder(
            TRAIN_KEY_FORMAT.format('001Ы', '20160101_10'),
            TRAIN_KEY_FORMAT.format('001Ы', '20160101_08'),
            TRAIN_KEY_FORMAT.format('002Ы', '20160101_10'),
            TRAIN_KEY_FORMAT.format('002Ы', '20160101_08'),
            TRAIN_KEY_FORMAT.format('013Б', '20160101_10'),
            TRAIN_KEY_FORMAT.format('013Б', '20160101_08'),
            TRAIN_KEY_FORMAT.format('014Б', '20160101_10'),
            TRAIN_KEY_FORMAT.format('014Б', '20160101_08')
        )
    )


@replace_setting('ALIGN_SEARCH_SEGMENT_KEYS', False)
def test_make_segment_suburban_express_keys_not_aligned():
    assert_that(
        make_segment_suburban_express_keys(
            SegmentStub('001', datetime(2016, 1, 1, 10, 33), t_type_id=TransportType.SUBURBAN_ID,
                        train_purchase_numbers=['001Ы', '014Б'])
        ),
        contains_inanyorder(
            TRAIN_KEY_FORMAT.format('001Ы', '20160101_1033'),
            TRAIN_KEY_FORMAT.format('002Ы', '20160101_1033'),
            TRAIN_KEY_FORMAT.format('013Б', '20160101_1033'),
            TRAIN_KEY_FORMAT.format('014Б', '20160101_1033')
        )
    )


@replace_setting('ALIGN_SEARCH_SEGMENT_KEYS', True)
def test_make_tariff_segment_key_aligned():
    key = make_tariff_segment_key(TariffSegmentStub('001Ы', datetime(2016, 1, 1, 10, 33)))
    assert key == 'train 001Ы 20160101_10'


@replace_setting('ALIGN_SEARCH_SEGMENT_KEYS', False)
def test_make_tariff_segment_key_not_aligned():
    key = make_tariff_segment_key(TariffSegmentStub('001Ы', datetime(2016, 1, 1, 10, 33)))
    assert key == 'train 001Ы 20160101_1033'


def test_make_segment_keys_not_train():
    assert make_segment_train_keys(mock.Mock(thread=RThread(t_type_id=TransportType.PLANE_ID))) == []
    assert make_segment_train_keys(mock.Mock(thread=None)) == []


@pytest.mark.parametrize(
    'from_country_code, to_country_code, old_ufs_order, rasp_url', [
        ('RU', 'KZ', False, True),
        ('RU', 'UA', False, False),
        ('RU', 'RU', True, False),
        ('XX', 'RU', False, False),
    ])
def test_build_train_order_url_ufs(from_country_code, to_country_code, old_ufs_order, rasp_url):
    country_from, _ = Country.objects.get_or_create(code=from_country_code, defaults={'title': 'country_from'})
    country_to, _ = Country.objects.get_or_create(code=to_country_code, defaults={'title': 'country_to'})
    point_from = create_station(country=country_from, time_zone=KIEV_TZ, __={'codes': {'express': '1111111111'}})
    point_to = create_settlement(country=country_to)
    create_station(settlement=point_to, __={'codes': {'express': '2222222222'}}, t_type=TransportType.TRAIN_ID)
    segment = SegmentStub('001У', UTC_TZ.localize(datetime(2018, 4, 7, 10)), '001', old_ufs_order,
                          train_purchase_numbers=['001Я'])
    tariff = TariffSegmentStub('001У', UTC_TZ.localize(datetime(2018, 4, 7, 10)))
    segment.thread = create_thread()
    segment.thread.first_country_code = country_from.code
    segment.thread.last_country_code = country_to.code

    url, owner = _build_train_order_url(segment, tariff, point_from, point_to)
    order_url = urlparse(url)
    order_url_params = parse_qs(urlunquote(order_url.query))
    if rasp_url:
        assert owner == TrainOrderUrlOwner.TRAINS
        assert order_url.path == '/order/'
        assert_that(order_url_params, has_entries({
            'fromId': [point_from.point_key],
            'toId': [point_to.point_key],
            'number': ['001Я'],
            'when': ['2018-04-07'],
            'time': ['13:00'],
            'coachType': [tariff.coach_type]
        }))
    else:
        assert owner == TrainOrderUrlOwner.UFS
        assert order_url.hostname == 'ufs-online.ru'
        assert order_url.path == '/kupit-zhd-bilety/1111111111/2222222222'
        assert_that(order_url_params, has_entries({
            'trainNumber': ['001У'],
            'domain': [ADVERT_DOMAIN],
            'date': ['07.04.2018'],
        }))


@pytest.mark.parametrize('old_ufs_order, first_country_code, last_country_code, allow_international_routes, expected', (
    (True, 'RU', 'RU', False, False),

    (False, 'RU', 'RU', False, True),
    (False, 'RU', 'GB', False, False),
    (False, 'GB', 'RU', False, False),
    (False, 'GB', 'GB', False, False),

    (False, 'RU', None, False, False),
    (False, None, 'RU', False, False),
    (False, None, None, False, False),

    (False, 'DE', 'RU', False, False),
    (False, 'DE', 'RU', True, True),
    (False, 'RU', 'DE', True, True),
    (False, 'RU', 'GB', True, False),
))
def test_can_sale_on_rasp(old_ufs_order, first_country_code, last_country_code, allow_international_routes, expected):
    russia = Country.objects.get(id=Country.RUSSIA_ID)
    point_from = create_station(country=russia)
    point_to = create_station(country=russia)

    assert expected == can_sale_on_rasp(old_ufs_order, first_country_code, last_country_code, point_from, point_to, '',
                                        allow_international_routes)


@pytest.mark.parametrize('title, allow_international_routes, check_in_ussr_by_train_title, expected', (
    (None, False, True, False),
    ('invalid title', False, True, False),
    ('Шадринск — Казань', False, True, True),
    ('Шадринск — Москва', False, True, True),
    ('Шадринск — Петербург', False, True, False),
    ('Шадринск — Петербург', False, False, False),
    ('Шадринск — Лас-Вегас', False, True, False),
    ('Шадринск — Берлин', False, True, False),
    ('Шадринск — Нет такого города', False, True, False),
    ('Шадринск — Лас-Вегас', True, True, False),
    ('Шадринск — Берлин', True, True, True),
    ('Берлин — Шадринск', True, True, True),

    ('Шадринск — Казань', False, False, False),
    ('Шадринск — Москва', False, False, False),
    ('Шадринск — Берлин', True, False, False),
))
def test_can_sale_on_rasp_by_title(title, allow_international_routes, check_in_ussr_by_train_title, expected):
    russia = Country.objects.get(id=Country.RUSSIA_ID)
    usa = Country.objects.create(title='США', id=84, code='US')
    germany = Country.objects.create(title='Германия', id=96, code='DE')

    create_settlement(country=russia, title_ru='Шадринск')
    create_settlement(country=russia, title_ru='Казань')
    create_settlement(country=russia, title_ru='Петербург')
    create_settlement(country=usa, title_ru='Петербург')
    create_settlement(country=usa, title_ru='Лас-Вегас')
    create_settlement(country=usa, title_ru='Берлин')
    create_settlement(country=germany, title_ru='Берлин')

    point_from = create_station(country=russia)
    point_to = create_station(country=russia)

    with country_availability_manager.using_precache():
        with replace_dynamic_setting('TRAIN_PURCHASE_CHECK_IN_USSR_BY_TRAIN_TITLE', check_in_ussr_by_train_title):
            result = can_sale_on_rasp(False, None, None, point_from, point_to, title, allow_international_routes)
    assert result == expected
