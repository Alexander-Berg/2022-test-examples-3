# coding: utf-8

import allure
import requests
import logging
from allure.constants import AttachmentType
from library.python.svn_version import svn_revision
from constants import (
    DEBUG_NAME_FIELD,
    BRIEF_NAME_FIELD,
    DEBUG_YES,
    INDEXER_VERSION_NAME_FIELD
)

from market.pylibrary.putil.protector import retry, geometric_progression

import yatest
import pytest


def _canonization_version(version_tuple):
    version_tuple = version_tuple + (0, 0, 0, 0)
    return version_tuple[:4]


def get_response_brief_field(response, field_name):
    def get_dict_value(container, key):
        if not isinstance(container, dict):
            return None

        return container.get(key, None)

    debug = get_dict_value(response, DEBUG_NAME_FIELD)
    brief = get_dict_value(debug, BRIEF_NAME_FIELD)
    field_value = get_dict_value(brief, field_name)
    if field_value:
        return str(field_value)
    else:
        return None


@retry(retries_count=5, exceptions=(requests.exceptions.HTTPError, ValueError), timeout=geometric_progression(10, 2))
def get_report_response(url, headers, params):
    '''
    Репорт умеет 500-ть, поэтому обкладываем ретраями
    и проверяем, что ответы это json
    '''
    response = requests.get(url, headers=headers, params=params)
    response.raise_for_status()
    response.json()
    return response


def report_response(test_request_params_dict):
    url = 'http://{host}:{port}/yandsearch'.format(
        host=yatest.common.get_param("report_host"),
        port=yatest.common.get_param("report_port")
    )

    headers = {}
    generation = yatest.common.get_param('generation')
    if generation is not None:
        headers['X-Market-Req-Ctx-Report-Config-Hint'] = generation

    request_params = {}
    request_params.update(test_request_params_dict)
    rearr_factors = ['graceful_degradation_force_level=0']
    if 'rearr-factors' in test_request_params_dict:
        rearr_factors.append(test_request_params_dict['rearr-factors'])
    request_params.update({
        'debug': DEBUG_YES,
        'rearr-factors': ';'.join(rearr_factors),
    })
    base_cluster = yatest.common.get_param('base_cluster')
    if base_cluster is not None:
        request_params['debug-base-cluster'] = base_cluster

    with allure.step('Отправка запроса в репорт'):
        logging.info('Revision {}'.format(str(svn_revision())))
        response = get_report_response(url, headers, request_params)
        allure.attach('Запрос', str(response.url))
        allure.attach('Ответ', str(response.text.encode('utf-8')), type=AttachmentType.TEXT)

        idx_version = yatest.common.get_param("release_version")
        if idx_version:
            indexer_version = get_response_brief_field(
                response.json(), INDEXER_VERSION_NAME_FIELD
            )
            allure.attach('Версия индесатора в ответе репорта', str(indexer_version))

            response_indexer_version = _canonization_version(
                tuple(map(int, indexer_version.split('.')))
            )
            release_indexer_version = _canonization_version(
                tuple(map(int, idx_version.split('.')))
            )

            assert response_indexer_version == release_indexer_version

    return response


white_only = pytest.mark.skipif(yatest.common.get_param('color') == 'blue', reason="Only white test. Skip it!")

blue_only = pytest.mark.skipif(yatest.common.get_param('color') != 'blue',  reason="Only blue test. Skip it!")

skip_testing = pytest.mark.skipif(yatest.common.get_param('env') == 'testing', reason="Skip this test for testing env")
