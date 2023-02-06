# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from common.models.transport import TransportType
from common.tester.factories import create_thread, create_supplier
from mapping.models import RouteMapBlacklist


@pytest.mark.dbuser
@pytest.mark.parametrize('t_type, result', [
    (TransportType.TRAIN_ID, True),
    (TransportType.SUBURBAN_ID, True),
    (TransportType.BUS_ID, True),
    (TransportType.WATER_ID, True),
    (TransportType.PLANE_ID, False)
])
def test_is_thread_mapped_by_t_type(t_type, result):
    thread = create_thread(t_type=t_type)
    assert RouteMapBlacklist.is_thread_mapped(thread) == result


@pytest.mark.dbuser
def test_is_thread_mapped_by_number():
    good_number = 'number_not_in_blacklist'
    bad_number = 'number_in_blacklist'
    RouteMapBlacklist.clear_numbers_cache()
    RouteMapBlacklist.objects.create(number=bad_number)

    bad_thread = create_thread(number=bad_number)
    assert not RouteMapBlacklist.is_thread_mapped(bad_thread)

    good_thread = create_thread(number=good_number)
    assert RouteMapBlacklist.is_thread_mapped(good_thread)


@pytest.mark.dbuser
def test_is_thread_mapped_by_supplier():
    bad_thread = create_thread(t_type=TransportType.TRAIN_ID, supplier=create_supplier(code='oag'))
    assert not RouteMapBlacklist.is_thread_mapped(bad_thread)

    good_thread = create_thread(t_type=TransportType.TRAIN_ID, supplier=create_supplier(code='tis'))
    assert RouteMapBlacklist.is_thread_mapped(good_thread)
