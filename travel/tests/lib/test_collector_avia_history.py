# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import pytest

from checkers import TaskChecker


@pytest.fixture(scope='function', autouse=True)
def clear_avia_partner_booking_log(yt_stuff):
    yt_stuff.yt_client.remove('//home/avia/logs/avia-partner-booking-log', recursive=True, force=True)


def test_aeroflot_history_common_flow(yt_stuff):
    TaskChecker(
        test_fn='travel/cpa/tests/lib/data/collectors/avia_history/aeroflot_common_flow.toml',
        yt_client=yt_stuff.yt_client,
        yt_proxy=yt_stuff.get_server(),
    )


def test_agent_history_common_flow(yt_stuff):
    TaskChecker(
        test_fn='travel/cpa/tests/lib/data/collectors/avia_history/agent_common_flow.toml',
        yt_client=yt_stuff.yt_client,
        yt_proxy=yt_stuff.get_server(),
    )


def test_aviakassa_history_common_flow(yt_stuff):
    TaskChecker(
        test_fn='travel/cpa/tests/lib/data/collectors/avia_history/aviakassa_common_flow.toml',
        yt_client=yt_stuff.yt_client,
        yt_proxy=yt_stuff.get_server(),
    )


def test_aviaoperator_history_common_flow(yt_stuff):
    TaskChecker(
        test_fn='travel/cpa/tests/lib/data/collectors/avia_history/aviaoperator_common_flow.toml',
        yt_client=yt_stuff.yt_client,
        yt_proxy=yt_stuff.get_server(),
    )


def test_azimuth_history_common_flow(yt_stuff):
    TaskChecker(
        test_fn='travel/cpa/tests/lib/data/collectors/avia_history/azimuth_common_flow.toml',
        yt_client=yt_stuff.yt_client,
        yt_proxy=yt_stuff.get_server(),
    )


def test_biletdv_history_common_flow(yt_stuff):
    TaskChecker(
        test_fn='travel/cpa/tests/lib/data/collectors/avia_history/biletdv_common_flow.toml',
        yt_client=yt_stuff.yt_client,
        yt_proxy=yt_stuff.get_server(),
    )


def test_biletikaeroag_history_common_flow(yt_stuff):
    TaskChecker(
        test_fn='travel/cpa/tests/lib/data/collectors/avia_history/biletikaeroag_common_flow.toml',
        yt_client=yt_stuff.yt_client,
        yt_proxy=yt_stuff.get_server(),
    )


def test_biletinet_history_common_flow(yt_stuff):
    TaskChecker(
        test_fn='travel/cpa/tests/lib/data/collectors/avia_history/biletinet_common_flow.toml',
        yt_client=yt_stuff.yt_client,
        yt_proxy=yt_stuff.get_server(),
    )


def test_biletix_history_common_flow(yt_stuff):
    TaskChecker(
        test_fn='travel/cpa/tests/lib/data/collectors/avia_history/biletix_common_flow.toml',
        yt_client=yt_stuff.yt_client,
        yt_proxy=yt_stuff.get_server(),
    )


def test_booktripruag_history_common_flow(yt_stuff):
    TaskChecker(
        test_fn='travel/cpa/tests/lib/data/collectors/avia_history/booktripruag_common_flow.toml',
        yt_client=yt_stuff.yt_client,
        yt_proxy=yt_stuff.get_server(),
    )


def test_citytravel_history_common_flow(yt_stuff):
    TaskChecker(
        test_fn='travel/cpa/tests/lib/data/collectors/avia_history/citytravel_common_flow.toml',
        yt_client=yt_stuff.yt_client,
        yt_proxy=yt_stuff.get_server(),
    )


def test_clickavia_history_common_flow(yt_stuff):
    TaskChecker(
        test_fn='travel/cpa/tests/lib/data/collectors/avia_history/clickavia_common_flow.toml',
        yt_client=yt_stuff.yt_client,
        yt_proxy=yt_stuff.get_server(),
    )


def test_expressavia_history_common_flow(yt_stuff):
    TaskChecker(
        test_fn='travel/cpa/tests/lib/data/collectors/avia_history/expressavia_common_flow.toml',
        yt_client=yt_stuff.yt_client,
        yt_proxy=yt_stuff.get_server(),
    )


def test_kupibilet_history_common_flow(yt_stuff):
    TaskChecker(
        test_fn='travel/cpa/tests/lib/data/collectors/avia_history/kupibilet_common_flow.toml',
        yt_client=yt_stuff.yt_client,
        yt_proxy=yt_stuff.get_server(),
    )


def test_megotravel_history_common_flow(yt_stuff):
    TaskChecker(
        test_fn='travel/cpa/tests/lib/data/collectors/avia_history/megotravel_common_flow.toml',
        yt_client=yt_stuff.yt_client,
        yt_proxy=yt_stuff.get_server(),
    )


def test_nebotravel_history_common_flow(yt_stuff):
    TaskChecker(
        test_fn='travel/cpa/tests/lib/data/collectors/avia_history/nebotravel_common_flow.toml',
        yt_client=yt_stuff.yt_client,
        yt_proxy=yt_stuff.get_server(),
    )


def test_ozon_history_common_flow(yt_stuff):
    TaskChecker(
        test_fn='travel/cpa/tests/lib/data/collectors/avia_history/ozon_common_flow.toml',
        yt_client=yt_stuff.yt_client,
        yt_proxy=yt_stuff.get_server(),
    )


def test_pobeda_history_common_flow(yt_stuff):
    TaskChecker(
        test_fn='travel/cpa/tests/lib/data/collectors/avia_history/pobeda_common_flow.toml',
        yt_client=yt_stuff.yt_client,
        yt_proxy=yt_stuff.get_server(),
    )


def test_rusline_history_common_flow(yt_stuff):
    TaskChecker(
        test_fn='travel/cpa/tests/lib/data/collectors/avia_history/rusline_common_flow.toml',
        yt_client=yt_stuff.yt_client,
        yt_proxy=yt_stuff.get_server(),
    )


def test_superkassa_history_common_flow(yt_stuff):
    TaskChecker(
        test_fn='travel/cpa/tests/lib/data/collectors/avia_history/superkassa_common_flow.toml',
        yt_client=yt_stuff.yt_client,
        yt_proxy=yt_stuff.get_server(),
    )


def test_svyaznoy_history_common_flow(yt_stuff):
    TaskChecker(
        test_fn='travel/cpa/tests/lib/data/collectors/avia_history/svyaznoy_common_flow.toml',
        yt_client=yt_stuff.yt_client,
        yt_proxy=yt_stuff.get_server(),
    )


def test_ticketsru_history_common_flow(yt_stuff):
    TaskChecker(
        test_fn='travel/cpa/tests/lib/data/collectors/avia_history/ticketsru_common_flow.toml',
        yt_client=yt_stuff.yt_client,
        yt_proxy=yt_stuff.get_server(),
    )


def test_tinkoff_history_common_flow(yt_stuff):
    TaskChecker(
        test_fn='travel/cpa/tests/lib/data/collectors/avia_history/tinkoff_common_flow.toml',
        yt_client=yt_stuff.yt_client,
        yt_proxy=yt_stuff.get_server(),
    )


def test_uzairways_history_common_flow(yt_stuff):
    TaskChecker(
        test_fn='travel/cpa/tests/lib/data/collectors/avia_history/uzairways_common_flow.toml',
        yt_client=yt_stuff.yt_client,
        yt_proxy=yt_stuff.get_server(),
    )


def test_tripcom_history_common_flow(yt_stuff):
    TaskChecker(
        test_fn='travel/cpa/tests/lib/data/collectors/avia_history/tripcom_common_flow.toml',
        yt_client=yt_stuff.yt_client,
        yt_proxy=yt_stuff.get_server(),
    )
