# -*- coding: utf-8 -*-
import pytest
from mock import Mock

from travel.avia.library.python.tester.factories import create_instance_by_abstract_class

from travel.avia.ticket_daemon.ticket_daemon.lib.yt_loggers.search_result_logger import SearchResultLogger
from travel.avia.ticket_daemon.ticket_daemon.lib.yt_loggers.abstract_yt_logger import IObjectLogger
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon.ticket_daemon.daemon_tester import create_query


def setup_params():
    reset_all_caches()
    query = create_query()
    fake_logger = create_instance_by_abstract_class(IObjectLogger)
    fake_logger.log = Mock()
    search_result_logger = SearchResultLogger(fake_logger)
    common_logged_data = {
        'language': 'ru',
        'point_from': query.point_from.point_key,
        'adults': 1,
        'national_version': 'ru',
        'qid': query.id,
        'date_forward': '2017-09-01',
        'class': 'economy',
        'infants': 0,
        'point_to': query.point_to.point_key,
        'children': 0,
        'date_backward': None,
        'service': 'ticket',
    }
    return search_result_logger, fake_logger, query, common_logged_data


@pytest.mark.dbuser
def test_common():
    search_result_logger, fake_logger, query, common_logged_data = setup_params()
    test_id = 'zzz'
    partner_code = 'test-partner-code'
    importer = 'test-importer'
    result = 'got_reply'
    variants_count = 42
    query_time = 999
    search_result_logger.log(
        query, partner_code, importer, result, test_id,
        variants_count=variants_count,
        query_time=query_time,
        errors=None,
    )
    expected_call = dict(common_logged_data, **{
        'partner_code': partner_code,
        'importer': importer,
        'result': result,
        'variants_count': variants_count,
        'query_time': query_time,
        'experiment_test_ids': test_id,
        'errors': None,
        'base_qid': None,
    })
    fake_logger.log.assert_called_once_with(expected_call)
