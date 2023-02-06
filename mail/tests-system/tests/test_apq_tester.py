from hamcrest import assert_that, equal_to

from components.apq_tester import ApqTester
from mail.devpack.lib.components.sharpei import Sharpei


def test_apq_tester_ping(coordinator):
    response = coordinator.components[ApqTester].api().ping(request_id='request_id')
    assert_that((response.status_code, response.text), equal_to((200, '{"result":"pong"}')))


def test_apq_tester_pingdb(coordinator):
    stat = coordinator.components[Sharpei].api().stat()
    shard = stat.json()["1"][0]["address"]
    conninfo = "host={host} port={port} dbname={dbname}".format(
        host=shard["host"],
        port=shard["port"],
        dbname=shard["dbname"],
    )
    response = coordinator.components[ApqTester].api().pingdb(
        conninfo=conninfo,
        request_id='request_id',
    )
    assert_that((response.status_code, response.text), equal_to((200, '{"result":"pongdb"}')))
