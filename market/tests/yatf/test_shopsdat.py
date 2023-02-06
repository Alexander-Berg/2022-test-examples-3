# coding=utf-8
import pytest

from hamcrest import assert_that, empty

from market.idx.tools.market_yt_data_upload.yatf.test_env import YtDataUploadTestEnv


@pytest.yield_fixture(scope='module')
def workflow(yt_server):
    resources = {
    }

    with YtDataUploadTestEnv(**resources) as env:
        env.execute(yt_server, type='shopsdat', output_table="//home/test/shopsdat")
        env.verify()
        yield env


@pytest.fixture(scope='module')
def result_yt_table(workflow):
    return workflow.outputs.get('result_table')


def test_result_table_exist(result_yt_table, yt_server):
    assert_that(yt_server.get_yt_client().exists(result_yt_table.get_path()), 'Table exist')


def test_result_table_row_count(result_yt_table):
    """
        Тест проверяет что отгружаемая нами схема совпадает с тем, что написано в TFullRecord
        Если тест падает, надо синхронизировать схему с текущей версий в library/libshopsdat
    """
    non_keys = [key for key, value in result_yt_table.data[0].items() if value is None]

    assert_that(non_keys, empty(), "Not none")
