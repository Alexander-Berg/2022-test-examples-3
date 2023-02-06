# -*- coding: utf-8 -*-
import functools
from itertools import izip
from logging import Logger

import flask
import pytest
from mock import Mock

from travel.avia.library.python.tester.factories import create_partner

from travel.avia.ticket_daemon.ticket_daemon.daemon_tester import create_query
from travel.avia.ticket_daemon.ticket_daemon.api.cache import LoggingMemcachedCache
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon.ticket_daemon.lib.partner_status_fetcher import PartnerStatusFetcher


def setup_params():
    reset_all_caches()

    fake_logger = Mock(spec=Logger)
    fake_mcr = Mock(spec=LoggingMemcachedCache)
    fake_mcr.unpack = Mock(side_effect=lambda x: x)
    fetcher = PartnerStatusFetcher(
        logger=fake_logger,
        cache=fake_mcr
    )

    flask.g = {}  # костыль на время эксперимента с SaaS

    query = create_query()

    return (query, fake_logger, fake_mcr, fetcher)


def fake_get_many(mock_statuses, keys):
    return {key: status for key, status in izip(keys, mock_statuses)
            if status}


@pytest.mark.dbuser
def test_some_partners_not_in_cache():
    """
    Сценарий:
    1) поиск был не так давно, но по части партнеров статусы исчезли
    2) статусы исчезли из кэша
    """
    query, fake_logger, fake_mcr, fetcher = setup_params()
    mock_statuses = [Mock(), None]

    fake_mcr.get_many.side_effect = functools.partial(fake_get_many, mock_statuses)

    create_partner(code='p1')
    create_partner(code='p2')
    actual_statuses = fetcher.fetch_all(
        query=query,
        partner_codes=['p1', 'p2']
    )

    assert actual_statuses == {
        'p1': mock_statuses[0],
        'p2': mock_statuses[1],
    }

    assert not fake_logger.exception.called


@pytest.mark.dbuser
def test_all_partners_not_in_cache():
    """
    Сценарий:
    1) поиск был давно
    2) статусы исчезли из кэша
    """
    query, fake_logger, fake_mcr, fetcher = setup_params()
    mock_statuses = [None, None]

    fake_mcr.get_many.side_effect = functools.partial(fake_get_many, mock_statuses)

    create_partner(code='p1')
    create_partner(code='p2')
    actual_statuses = fetcher.fetch_all(
        query=query,
        partner_codes=['p1', 'p2']
    )

    assert actual_statuses == {
        'p1': mock_statuses[0],
        'p2': mock_statuses[1]
    }

    assert not fake_logger.exception.called


@pytest.mark.dbuser
def test_all_partners_in_cache():
    """
    Сценарий:
    1) предыдущий поиск был недавно
    2) статусы по каждому партнеру еще в кэша
    """
    query, fake_logger, fake_mcr, fetcher = setup_params()
    mock_statuses = [Mock()]

    fake_mcr.get_many.side_effect = functools.partial(fake_get_many, mock_statuses)

    create_partner(code='p1')
    actual_statuses = fetcher.fetch_all(
        query=query,
        partner_codes=['p1']
    )

    assert actual_statuses == {
        'p1': mock_statuses[0]
    }

    assert not fake_logger.exception.called


@pytest.mark.dbuser
def test_cant_deserialize_partners_status():
    """
    Сценарий:
    1) формат статусов изменился/криво записался
    2) статус по этому партнеру становится None
    """
    query, fake_logger, fake_mcr, fetcher = setup_params()
    mock_statuses = [Mock(), Mock(), Mock()]

    fake_mcr.get_many.side_effect = functools.partial(fake_get_many, mock_statuses)

    def fake_unpack(packed_value):
        if mock_statuses[1] == packed_value:
            raise Exception('can\'t deserialize status')
        return packed_value

    fake_mcr.unpack = Mock(side_effect=fake_unpack)

    create_partner(code='p1')
    create_partner(code='p2')
    create_partner(code='p3')

    actual_statuses = fetcher.fetch_all(
        query=query,
        partner_codes=['p1', 'p2', 'p3']
    )

    assert actual_statuses == {
        'p1': mock_statuses[0],
        'p2': None,
        'p3': mock_statuses[2]
    }

    assert fake_logger.exception.called
