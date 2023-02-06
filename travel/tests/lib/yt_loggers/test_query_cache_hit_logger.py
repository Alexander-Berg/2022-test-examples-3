# -*- coding: utf-8 -*-
import pytest
from mock import Mock

from travel.avia.library.python.tester.factories import (
    create_partner,
    create_dohop_vendor,
    create_instance_by_abstract_class
)

from travel.avia.ticket_daemon.ticket_daemon.lib.yt_loggers.query_cache_hit_logger import (
    QueryCacheHitLogger
)
from travel.avia.ticket_daemon.ticket_daemon.lib.yt_loggers.abstract_yt_logger import IObjectLogger
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon.ticket_daemon.daemon_tester import create_query


def setup_params():
    reset_all_caches()

    partner = create_partner(
        code='dohop'
    )

    vendor = create_dohop_vendor(
        dohop_id=1
    )

    query = create_query()

    fake_logger = create_instance_by_abstract_class(IObjectLogger)
    actual_logger_calls = []
    fake_logger.log_many = Mock(side_effect=actual_logger_calls.append)

    query_cache_hit_logger = QueryCacheHitLogger(
        logger=fake_logger
    )

    common_logged_data = {
        'lang': 'ru',
        'from_key': query.point_from.point_key,
        'adults': 1,
        'national_version': 'ru',
        'qid': query.id,
        'when': '2017-09-01',
        'class': 'economy',
        'infants': 0,
        'to_key': query.point_to.point_key,
        'children': 0,
        'return_date': '',
        'experimentsTestIds': ''
    }

    return (
        query_cache_hit_logger, fake_logger, query,
        partner, vendor, common_logged_data,
        actual_logger_calls
    )


@pytest.mark.dbuser
def test_empty():
    (
        query_cache_hit_logger, fake_logger, query,
        partner, vendor, common_logged_data,
        actual_logger_calls
    ) = setup_params()

    query_cache_hit_logger.log(
        query=query,
        partners=[],
        partner_code_to_status={},
        partner_result_metas={},
        ignore_cache=False,
        test_id=''
    )

    assert len(actual_logger_calls) == 1
    assert list(actual_logger_calls[0]) == []


@pytest.mark.dbuser
def test_common():
    (
        query_cache_hit_logger, fake_logger, query,
        partner, vendor, common_logged_data,
        actual_logger_calls
    ) = setup_params()

    query_cache_hit_logger.log(
        query=query,
        partners=[partner, vendor],
        partner_code_to_status={
            partner.code: {'service': 'avia'},
            vendor.code: {'service': 'avia'},
        },
        partner_result_metas={
            partner.code: {'qid': 'partner_qid'},
            vendor.code: {'qid': 'vendor_qid'},
        },
        ignore_cache=False,
        test_id='zzz'
    )

    assert len(actual_logger_calls) == 1
    assert list(actual_logger_calls[0]) == [
        dict(common_logged_data, **{
            'service': 'ticket',
            'ignore_cache': False,
            'cache_hit': True,
            'query_source': 'avia',
            'partner': 'dohop',
            'init_id': 'partner_qid',
            'experimentsTestIds': 'zzz'
        }),
        dict(common_logged_data, **{
            'service': 'ticket',
            'ignore_cache': False,
            'cache_hit': True,
            'query_source': 'avia',
            'partner': 'dohop_1',
            'init_id': 'vendor_qid',
            'experimentsTestIds': 'zzz'
        })
    ]


@pytest.mark.dbuser
def test_partner_not_in_cache():
    (
        query_cache_hit_logger, fake_logger, query,
        partner, vendor, common_logged_data,
        actual_logger_calls
    ) = setup_params()

    query_cache_hit_logger.log(
        query=query,
        partners=[partner],
        partner_code_to_status={
            partner.code: None
        },
        partner_result_metas={
            partner.code: None,
        },
        ignore_cache=False,
        test_id=''
    )

    assert len(actual_logger_calls) == 1
    assert list(actual_logger_calls[0]) == [
        dict(common_logged_data, **{
            'service': 'ticket',
            'ignore_cache': False,
            'cache_hit': False,
            'query_source': 'ticket',
            'partner': 'dohop',
            'init_id': None,
        })
    ]


@pytest.mark.dbuser
def test_partner_not_in_cache_actual_instant_search_result():
    """Протух статус 20-минутка, но вариант для инстантсерча был найден и показан"""
    (
        query_cache_hit_logger, fake_logger, query,
        partner, vendor, common_logged_data,
        actual_logger_calls
    ) = setup_params()

    query_cache_hit_logger.log(
        query=query,
        partners=[partner],
        partner_code_to_status={
            partner.code: None
        },
        partner_result_metas={
            partner.code: {'qid': 'qid1'},
        },
        ignore_cache=False,
        test_id=''
    )

    assert len(actual_logger_calls) == 1
    assert list(actual_logger_calls[0]) == [
        dict(common_logged_data, **{
            'service': 'ticket',
            'ignore_cache': False,
            'cache_hit': False,
            'query_source': 'ticket',
            'partner': 'dohop',
            'init_id': 'qid1',
        })
    ]


@pytest.mark.dbuser
def test_partner_in_cache_without_query_source():
    (
        query_cache_hit_logger, fake_logger, query,
        partner, vendor, common_logged_data,
        actual_logger_calls
    ) = setup_params()

    query_cache_hit_logger.log(
        query=query,
        partners=[partner],
        partner_code_to_status={
            partner.code: {'some': 123}
        },
        partner_result_metas={
            partner.code: {'qid': 'qid1'},
        },
        ignore_cache=False,
        test_id=''
    )

    assert len(actual_logger_calls) == 1
    assert list(actual_logger_calls[0]) == [
        dict(common_logged_data, **{
            'service': 'ticket',
            'ignore_cache': False,
            'cache_hit': True,
            'query_source': 'ticket',
            'partner': 'dohop',
            'init_id': 'qid1',
        })
    ]
