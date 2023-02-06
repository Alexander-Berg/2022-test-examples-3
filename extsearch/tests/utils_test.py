from extsearch.audio.deepdive.common import utils

import yatest.common as yc
from mapreduce.yt.python.yt_stuff import YtConfig
import pytest


def test_check_kp_id():
    assert utils.check_kp_id('1234-1-1', '1234-1-1')
    assert utils.check_kp_id('1234-1-1', '1234-1')
    assert utils.check_kp_id('1234-1-1', '1234')
    assert not utils.check_kp_id('1234-1-1', '1234-1-2')
    assert not utils.check_kp_id('1234-1-1', '5555')
    assert not utils.check_kp_id('123456', '1234')


CYPRESS_DIR = 'extsearch/audio/deepdive/common/tests/cypress_dir'


@pytest.fixture(scope='module')
def yt_config(request):
    return YtConfig(
        local_cypress_dir=yc.source_path(CYPRESS_DIR)
    )


def test_read_control(yt_stuff):
    yt_client = yt_stuff.get_yt_client()
    control, finished, operations = utils.read_control(
        yt_client,
        '//data/control_table',
        collect_operations=['op1', 'op2'], store_operations_name=True
    )
    return [control, finished, sorted(list(operations))]
