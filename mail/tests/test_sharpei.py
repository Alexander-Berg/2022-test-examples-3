import json
from mail.devpack.lib.components.sharpei import Sharpei
from mail.devpack.lib.components.mdb import Mdb
from mail.devpack.lib.components.fakebb import FakeBlackbox


def test_sharpei_ping(coordinator):
    sharpei = coordinator.components[Sharpei]
    response = sharpei.ping()
    assert response.status_code == 200
    assert response.text == "pong"


def test_sharpei_stat(coordinator):
    sharpei = coordinator.components[Sharpei]
    mdb = coordinator.components[Mdb]
    response = sharpei.api().stat()
    assert response.text != '{}'
    assert json.loads(response.text) == {
        "1": [{
            "address": {"host": "localhost", "port": str(mdb.shard_by_id(1).master.port),
                        "dbname": "maildb", "dataCenter": "local"},
            "role": "master",
            "status": "alive",
            "state": {"lag": 0}
        }],
        "2": [{
            "address": {"host": "localhost", "port": str(mdb.shard_by_id(2).master.port),
                        "dbname": "maildb", "dataCenter": "local"},
            "role": "master",
            "status": "alive",
            "state": {"lag": 0}
        }],
    }


def test_sharpei_conninfo(coordinator):
    sharpei = coordinator.components[Sharpei]
    mdb = coordinator.components[Mdb]
    fbb = coordinator.components[FakeBlackbox]

    bb_response = fbb.register('test-sharpei@yandex.ru')
    bb_response_dict = json.loads(bb_response.text)
    assert bb_response_dict['status'] == 'ok'
    uid = bb_response_dict['uid']

    response = sharpei.api().conninfo(uid=uid, mode='master')
    assert response.status_code == 200
    assert json.loads(response.text) == {
        "id": 1,
        "name": "shard1",
        "addrs": [{"host": "localhost", "port": mdb.port(), "dbname": "maildb", "dataCenter": "local"}]
    }
