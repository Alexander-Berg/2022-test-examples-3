import mock
from io import StringIO
import pytest

from ora2pg.tools.find_master_helpers import get_sharddb_master_host, MasterNotFoundError

SHARPEI = 'test://sharpei.net'
DSN_SUFFIX = 'user=sharpei  connect_timeout=5'


class FakeContext(object):
    def __init__(self, value):
        self.__value = value

    def __enter__(self):
        return self.__value

    def __exit__(self, *_):
        pass


def get_response(resp):
    return FakeContext(StringIO(resp))


@mock.patch('ora2pg.tools.http.request')
def test_when_sharpei_responce_contains_master(mocked_request):
    mocked_request.return_value = get_response(
        '''
        [
            {"address":{"host":"repl01.mail.sharddb.net","port":7777,"dbname":"sharddb_name","dataCenter":"iva"},"role":"replica","status":"alive","state":{"lag":0}},
            {"address":{"host":"repl02.mail.sharddb.net","port":7777,"dbname":"sharddb_name","dataCenter":"iva"},"role":"replica","status":"alive","state":{"lag":0}},
            {"address":{"host":"mast01.mail.sharddb.net","port":7777,"dbname":"sharddb_name","dataCenter":"iva"},"role":"master","status":"alive","state":{"lag":0}}
        ]
        '''
    )

    master = get_sharddb_master_host(SHARPEI, DSN_SUFFIX)
    assert master == f'host=mast01.mail.sharddb.net port=7777 dbname=sharddb_name {DSN_SUFFIX}'

    mocked_request.assert_called_once_with(
        url=f'{SHARPEI}/sharddb_stat',
        do_retries=True,
    )


@mock.patch('ora2pg.tools.http.request')
def test_when_sharpei_responce_contains_no_master(mocked_request):
    mocked_request.return_value = get_response(
        '''
        [
            {"address":{"host":"repl01.mail.sharddb.net","port":7777,"dbname":"sharddb_name","dataCenter":"iva"},"role":"replica","status":"alive","state":{"lag":0}},
            {"address":{"host":"repl02.mail.sharddb.net","port":7777,"dbname":"sharddb_name","dataCenter":"iva"},"role":"replica","status":"alive","state":{"lag":0}}
        ]
        '''
    )

    with pytest.raises(MasterNotFoundError):
        get_sharddb_master_host(SHARPEI, DSN_SUFFIX)

    mocked_request.assert_called_once_with(
        url=f'{SHARPEI}/sharddb_stat',
        do_retries=True,
    )
