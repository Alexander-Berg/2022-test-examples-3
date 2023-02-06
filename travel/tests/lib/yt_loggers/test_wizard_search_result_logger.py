# -*- coding: utf-8 -*-
import pytest
from mock import Mock

from travel.avia.library.python.tester.factories import create_instance_by_abstract_class

from travel.avia.ticket_daemon.ticket_daemon.lib.yt_loggers.wizard_search_result_logger import WizardSearchResultLogger
from travel.avia.ticket_daemon.ticket_daemon.lib.yt_loggers.abstract_yt_logger import IObjectLogger
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon.ticket_daemon.daemon_tester import create_query


def setup_params():
    reset_all_caches()

    query = create_query()

    fake_logger = create_instance_by_abstract_class(IObjectLogger)
    fake_logger.log = Mock()

    wizard_search_result_logger = WizardSearchResultLogger(
        logger=fake_logger
    )

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
    }

    return wizard_search_result_logger, fake_logger, query, common_logged_data


@pytest.mark.dbuser
def test_common():
    wizard_search_result_logger, fake_logger, query, common_logged_data = setup_params()

    min_price = 104201.
    test_id = 'zzz'
    is_experimental = False
    variants_count = 42
    wizard_search_result_logger.log(
        query=query,
        variants_count=variants_count,
        min_price=min_price,
        test_id=test_id,
        is_experimental=is_experimental,
    )

    expected_call = dict(common_logged_data, **{
        'service': 'ticket',
        'experiment_test_ids': test_id,
        'variants_count': variants_count,
        'min_price': min_price,
        'is_experimental': is_experimental,
        'base_qid': None,
    })
    fake_logger.log.assert_called_once_with(expected_call)
