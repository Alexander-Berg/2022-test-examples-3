# -*- coding: utf-8 -*-
import pytest

from mock import MagicMock, Mock, patch
from logging import Logger
from itertools import izip

from travel.avia.library.python.tester.factories import create_partner, create_instance_by_abstract_class

from travel.avia.ticket_daemon.ticket_daemon.lib.partner_status_fetcher import IPartnerStatusFetcher
from travel.avia.ticket_daemon.ticket_daemon.lib.yt_loggers.query_cache_hit_logger import IQueryCacheHitLogger
from travel.avia.ticket_daemon.ticket_daemon.lib.query_partner_provider import QueryPartnerProvider
from travel.avia.ticket_daemon.ticket_daemon.daemon_tester import create_query
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches


def setup_params():
    reset_all_caches()

    fake_query_cache_hit_logger = create_instance_by_abstract_class(IQueryCacheHitLogger)
    fake_status_fetcher = create_instance_by_abstract_class(IPartnerStatusFetcher)
    fake_meta_fetcher = create_instance_by_abstract_class(IPartnerStatusFetcher)
    fake_logger = Mock(Logger)

    provider = QueryPartnerProvider(
        query_cache_hit_logger=fake_query_cache_hit_logger,
        logger=fake_logger,
        partner_status_fetcher=fake_status_fetcher,
        partner_result_meta_fetcher=fake_meta_fetcher,
    )

    query = create_query()
    partner = create_partner(code='partner')
    other_partner = create_partner(code='other_partner')

    return provider, query, partner, other_partner, fake_query_cache_hit_logger, fake_status_fetcher, fake_meta_fetcher, fake_logger


@pytest.mark.dbuser
@patch('travel.avia.ticket_daemon.ticket_daemon.lib.query_partner_provider.get_related_partners', return_value=[])
def test_empty(_get_related_partners_mock):
    """
    Все партнеры выключены для данного направления
    """
    ignore_cache = False
    provider, query, partner, other_partner, fake_query_cache_hit_logger, fake_fetcher, fake_meta_fetcher, fake_logger = setup_params()

    fake_fetcher.fetch_all = Mock(return_value={})
    fake_meta_fetcher.fetch_all = Mock(return_value={})
    result = provider.get_partners_for(
        query=query,
        only_partners=[],
        ignore_cache=ignore_cache,
        test_id=''
    )

    assert _get_related_partners_mock.call_count == 1
    fake_fetcher.fetch_all.assert_called_once_with(query, [])
    fake_meta_fetcher.fetch_all.assert_called_once_with(query, [])
    fake_query_cache_hit_logger.log.assert_called_once_with(
        query=query,
        partners=[],
        partner_code_to_status={},
        partner_result_metas={},
        ignore_cache=ignore_cache,
        test_id=''
    )

    assert result.partners == []


@pytest.mark.dbuser
@patch('travel.avia.ticket_daemon.ticket_daemon.lib.query_partner_provider.get_related_partners')
def test_normal(_get_related_partners_mock):
    """
    Один в кэше, а другой не в кэше
    """
    ignore_cache = False
    provider, query, partner, other_partner, fake_query_cache_hit_logger, fake_fetcher, fake_meta_fetcher, fake_logger = setup_params()

    partners = [partner, other_partner]
    statuses = [MagicMock(), None]
    result_metas = [{'qid': 'qid1'}, {'qid': 'qid2'}]
    statuses_by_p_code = {p.code: s for p, s in izip(partners, statuses)}
    meta_by_p_code = {p.code: meta for p, meta in izip(partners, result_metas)}
    _get_related_partners_mock.return_value=partners[:]
    fake_fetcher.fetch_all = Mock(return_value=statuses_by_p_code)
    fake_meta_fetcher.fetch_all = Mock(return_value=meta_by_p_code)

    result = provider.get_partners_for(
        query=query,
        only_partners=[],
        ignore_cache=ignore_cache,
        test_id=''
    )

    assert _get_related_partners_mock.call_count == 1
    fake_fetcher.fetch_all.assert_called_once_with(query, [p.code for p in partners])
    fake_meta_fetcher.fetch_all.assert_called_once_with(query, [p.code for p in partners])
    fake_query_cache_hit_logger.log.assert_called_once_with(
        query=query,
        partners=partners,
        partner_code_to_status=statuses_by_p_code,
        partner_result_metas=meta_by_p_code,
        ignore_cache=ignore_cache,
        test_id=''
    )

    assert result.partners == [other_partner]


@pytest.mark.dbuser
@patch('travel.avia.ticket_daemon.ticket_daemon.lib.query_partner_provider.get_related_partners')
def test_ignore_cache(_get_related_partners_mock):
    """
    Один в кэше, а другой не в кэше. Но запрашивают с пробитием кэша
    """
    ignore_cache = True
    provider, query, partner, other_partner, fake_query_cache_hit_logger, fake_fetcher, fake_meta_fetcher, fake_logger = setup_params()

    partners = [partner, other_partner]
    statuses = [MagicMock(), None]
    result_metas = [{'qid': 'qid1'}, None]
    statuses_by_p_code = {p.code: s for p, s in izip(partners, statuses)}
    meta_by_p_code = {p.code: meta for p, meta in izip(partners, result_metas)}
    _get_related_partners_mock.return_value=partners[:]
    fake_fetcher.fetch_all = Mock(return_value=statuses_by_p_code)
    fake_meta_fetcher.fetch_all = Mock(return_value=meta_by_p_code)

    result = provider.get_partners_for(
        query=query,
        only_partners=[],
        ignore_cache=ignore_cache,
        test_id=''
    )

    assert _get_related_partners_mock.call_count == 1
    fake_fetcher.fetch_all.assert_called_once_with(query, [p.code for p in partners])
    fake_query_cache_hit_logger.log.assert_called_once_with(
        query=query,
        partners=partners,
        partner_code_to_status=statuses_by_p_code,
        partner_result_metas=meta_by_p_code,
        ignore_cache=ignore_cache,
        test_id=''
    )

    assert result.partners == partners


@pytest.mark.dbuser
@patch('travel.avia.ticket_daemon.ticket_daemon.lib.query_partner_provider.get_related_partners')
def test_with_filter_by_parametrs(_get_related_partners_mock):
    """
    оба партнера не в кэше, но хотят получить только одного
    """
    ignore_cache = False
    provider, query, partner, other_partner, fake_query_cache_hit_logger, fake_fetcher, fake_meta_fetcher, fake_logger = setup_params()

    partners = [partner, other_partner]
    _get_related_partners_mock.return_value=partners[:]
    fake_fetcher.fetch_all = Mock(return_value={other_partner.code: None})
    fake_meta_fetcher.fetch_all = Mock(return_value={other_partner.code: None})

    result = provider.get_partners_for(
        query=query,
        only_partners=[other_partner.code],
        ignore_cache=ignore_cache,
        test_id=''
    )

    assert _get_related_partners_mock.call_count == 1
    fake_fetcher.fetch_all.assert_called_once_with(query, [other_partner.code])
    fake_query_cache_hit_logger.log.assert_called_once_with(
        query=query,
        partners=[other_partner],
        partner_code_to_status={other_partner.code: None},
        partner_result_metas={other_partner.code: None},
        ignore_cache=ignore_cache,
        test_id=''
    )

    assert result.partners == [other_partner]
