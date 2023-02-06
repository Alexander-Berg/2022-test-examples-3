# coding: utf-8

from hamcrest import assert_that, raises, calling
import multiprocessing
import pytest
from market.idx.datacamp.parser.lib.service import RepositoryParserService
from market.idx.datacamp.parser.yatf.resources.config_mock import PushParserConfigMock
from market.idx.pylibrary.datacamp.utils import wait_until


@pytest.fixture(scope='module')
def config(tmpdir_factory, log_broker_stuff, yt_server):
    cfg = {}

    return PushParserConfigMock(
        workdir=tmpdir_factory.mktemp('workdir'),
        yt_server=yt_server,
        log_broker_stuff=log_broker_stuff,
        config=cfg
    )


def test_repository_parser_service_runnable(config):
    service = RepositoryParserService(
        config=config,
        config_path='',
        api_host='0.0.0.0',
        api_port=0,
    )
    service_proc = multiprocessing.Process(target=service.run, args=())
    service_proc.start()
    def notAlive():
        return not service_proc.is_alive()
    assert_that(calling(wait_until).with_args(condition=notAlive, timeout=20), raises(RuntimeError))
    service_proc.terminate()
