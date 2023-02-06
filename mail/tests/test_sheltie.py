from hamcrest import assert_that, equal_to

from mail.devpack.lib.components.sheltie import Sheltie


def test_sheltie_ping(coordinator):
    response = coordinator.components[Sheltie].api().ping(request_id='request_id')
    assert_that((response.status_code, response.text), equal_to((200, 'pong')))
