# encoding: utf-8
'''
Тест на проверку отправки фидлога в MBI.
В тесте запускается команда mindexer_clt upload_feedlog_to_mbi, которая внутри дёргает моку mbi api.
После того, как команда отработала мы смотрим, что в mbi пришли 2 файла (мета и feedlog).
Они могу не прийти в том случае, если упадёт код этой команды или дёрнулись не две ручки mbi api, а только одна.
'''

import os
import pytest
import yatest.common
from datetime import datetime
from hamcrest import assert_that, only_contains

from market.idx.marketindexer.yatf.test_env import MarketIndexer
from market.idx.marketindexer.yatf.resources.common_ini import CommonIni
from market.idx.marketindexer.yatf.resources.mbi_feedlog_api_server import MbiApiMock
from market.idx.marketindexer.yatf.resources.mi_state import MiState
from market.idx.marketindexer.yatf.resources.mbi_feedlog import MbiFeedLog
import market.idx.pylibrary.mindexer_core.feedlog.feedlog as feedlog
from market.pylibrary.mindexerlib import util


RELEASE_DATE = 0
MBI_FILES_DIR_PATH = 'mbi'
PIPELINE_TYPE = 'full'
LOCAL_CONFIG_FILE_NAME = 'common.ini'


@pytest.fixture(scope='module')
def root_dir():
    return yatest.common.test_output_path()


@pytest.fixture(scope='module')
def generation_name():
    dt = datetime.now()
    return util.datetime2generation(dt=dt)


@pytest.fixture(scope='module')
def mbi_dump_path(root_dir):
    path = os.path.join(root_dir, MBI_FILES_DIR_PATH)
    util.makedirs(path)
    return path


@pytest.fixture(scope='module')
def feedlog_mbi_result():
    feedlog = MbiFeedLog([])
    return feedlog


@pytest.yield_fixture(scope='function')
def workflow(
        root_dir,
        mbi_dump_path,
        generation_name,
        feedlog_mbi_result
):
    resources = {
        'common_ini': CommonIni(
            path=os.path.join(root_dir, LOCAL_CONFIG_FILE_NAME),
            test_work_dir=root_dir,
        ),
        'mbi_api': MbiApiMock(
            mbi_dump_dir_path=mbi_dump_path,
            pipeline_type=PIPELINE_TYPE,
            release_date=RELEASE_DATE
        ),
        'mi_state': MiState(
            start_date=0,
            end_date=0,
            release_date=RELEASE_DATE,
            generation_name=generation_name,
        ),
        'feed_log': feedlog_mbi_result
    }

    with MarketIndexer(None, **resources) as env:
        env.execute(clt_command_args_list=[
            'upload_feedlog_to_mbi',
            '--mbi_feedlog_file_path', env.resources['feed_log'].path,
            '--mbi_report_metafile_path', os.path.join(env.working_dir, feedlog.MBI_METAFILE),
            '--generation', generation_name
        ])
        yield env


def test_mbi_get_files(workflow, mbi_dump_path):
    actual_output_files = sorted([
        f for f in os.listdir(mbi_dump_path)
        if os.path.isfile(os.path.join(mbi_dump_path, f))
    ])

    assert_that(
        actual_output_files,
        only_contains(feedlog.MBI_METAFILE, feedlog.MBI_FEEDLOG_RESULT_FILE)
    )
