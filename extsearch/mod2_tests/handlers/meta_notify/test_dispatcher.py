import pytest

from vh.lib.sqs_watcher import ParseError


def test_dispatcher(dispatcher, dispatcher_test_message):
    dispatcher.route_message(dispatcher_test_message)


def test_dispatcher_wrong_services(dispatcher, dispatcher_test_message_full_services):
    with pytest.raises(ParseError):
        dispatcher.route_message(dispatcher_test_message_full_services)
