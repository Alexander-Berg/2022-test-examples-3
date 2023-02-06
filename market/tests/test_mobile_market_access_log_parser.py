import itertools
import pytest

from yamarec_log_parsers import log_parser
from yamarec_log_parsers import mobile_market_access_log_parser


@pytest.fixture
def parser():
    return mobile_market_access_log_parser.MobileMarketAccessLogParser()


@pytest.mark.parametrize("timestamp, action, user, obj, context", [
    (1467019886, "view", {"uuid": "0f1ed"}, {"id": "model_card", "type": "page"},
     {"block": "analog", "model_id": "11599017", "page": "model_card"}),
    (1466524225, "view", {"uuid": "c44f1"}, {"id": "model_card", "type": "page"},
     {"model_id": "11053790", "page": "deep_link"}),
    (1467019886, "view", {"shmuid": "", "puid": "1000"}, {"id": "model_card", "type": "page"},
     {"model_id": "11053790", "page": "deep_link"}),
    (1466524225, "view", {"uuid": "c44f1"}, {"id": None, "type": "page"},
     {"model_id": "11053790", "page": "deep_link"}),
    (1466524225, "view", {"uuid": "c44f1"}, {"id": "main", "type": "page"}, {}),
    (1466524225, "view", {"uuid": "c44f1", "puid": ""}, {"id": None, "type": "page"},
     {"model_id": "", "page": "deep_link"}),
    pytest.param
        (1466524225, "view", {"uuid": "c44f1"}, {"id": "model_card", "type": "page"},
         {"model_id": "", "page": "deep_link"}, marks=pytest.mark.xfail),
    pytest.param
        (1466524225, "view", {"uuid": "c44f1"}, {"id": "model_card", "type": "page"},
         {"page": "deep_link"}, marks=pytest.mark.xfail),
    pytest.param
        (0, "view", {"uuid": "c44f1"}, {"id": "model_card", "type": "page"},
         {"model_id": "11053790", "page": "deep_link"}, marks=pytest.mark.xfail),
    pytest.param
        (None, "view", {"uuid": "c44f1"}, {"id": "model_card", "type": "page"},
         {"model_id": "11053790", "page": "deep_link"}, marks=pytest.mark.xfail),
    pytest.param
        (1467019886, "", {"uuid": "c44f1"}, {"id": "model_card", "type": "page"},
         {"model_id": "11053790", "page": "deep_link"}, marks=pytest.mark.xfail),
    pytest.param
        (1467019886, None, {"uuid": "c44f1"}, {"id": "model_card", "type": "page"},
         {"model_id": "11053790", "page": "deep_link"}, marks=pytest.mark.xfail),
    pytest.param
        (1467019886, "view", {"uuid": ""}, {"id": "model_card", "type": "page"},
         {"model_id": "11053790", "page": "deep_link"}, marks=pytest.mark.xfail),
    pytest.param
        (1467019886, "view", {"shmuid": "c44f1"}, {"id": "model_card", "type": "page"},
         {"model_id": "11053790", "page": "deep_link"}, marks=pytest.mark.xfail),
    pytest.param
        (1467019886, "view", {}, {"id": "model_card", "type": "page"},
         {"model_id": "11053790", "page": "deep_link"}, marks=pytest.mark.xfail),
    pytest.param
        (1467019886, "view", None, {"id": "model_card", "type": "page"},
         {"model_id": "11053790", "page": "deep_link"}, marks=pytest.mark.xfail),
    pytest.param
        (1467019886, "view", {"uuid": "c44f1"}, None,
         {"model_id": "11053790", "page": "deep_link"}, marks=pytest.mark.xfail),
    pytest.param
        (1467019886, "view", {"uuid": "c44f1"}, {"id": "", "type": "page"},
         {"model_id": "11053790", "page": "deep_link"}, marks=pytest.mark.xfail),
    pytest.param
        (1467019886, "view", {"uuid": "c44f1"}, {"id": "model_card", "type": ""},
         {"model_id": "11053790", "page": "deep_link"}, marks=pytest.mark.xfail),
    pytest.param
        (1467019886, "view", {"uuid": "c44f1"}, {"id": "model_card", "type": None},
         {"model_id": "11053790", "page": "deep_link"}, marks=pytest.mark.xfail),
    pytest.param
        (1467019886, "view", {"uuid": "c44f1"}, {"id": "model_card", "type": ""}, None, marks=pytest.mark.xfail),
    pytest.param
        (1467019886, "view", {"uuid": "0f1ed"}, {"id": "model_card", "type": "page"},
         {"model_id": "01159901", "page": "model_card"}, marks=pytest.mark.xfail),
    pytest.param
        (1467019886, "view", {"uuid": "0f1ed"}, {"id": "model_card", "type": "page"},
         {"model_id": "1159901d", "page": "model_card"}, marks=pytest.mark.xfail),
])
def test_parsing(parser, timestamp, action, user, obj, context):
    log_record = _create_log_record(timestamp, action, user, obj, context)
    event = parser.parse(log_record).next()
    assert event.timestamp == timestamp
    assert event.user == _remove_empty_values(user)
    assert event.action == action
    assert event.object_type == obj["type"]
    assert event.object_id == obj["id"]
    assert event.context == _remove_empty_values(context)


@pytest.mark.parametrize("timestamp, action, user, obj, context, expected_events", [
    (
        1467019886, "view", {"uuid": "0f1ed"}, {"id": "main", "type": "page"}, {},
        [
            ("view", "page", "main", {}),
        ]
    ),
    (
        1467019886, "view", {"uuid": "0f1ed"}, {"id": "model_card", "type": "page"},
        {"block": "analog", "model_id": "11599017"},
        [
            ("view", "page", "model_card", {"block": "analog", "model_id": "11599017"}),
            ("view", "model", "11599017", {})
        ]
    ),
])
def test_additional_events(parser, timestamp, action, user, obj, context, expected_events):
    log_record = _create_log_record(timestamp, action, user, obj, context)
    expected_events = map(lambda args: _market_event(*args), expected_events)
    for expected_event, event in itertools.izip_longest(expected_events, parser.parse(log_record)):
        _check_event(expected_event, event)


def _create_log_record(timestamp=None, action=None, user=None, obj=None, context=None):
    return {
        "timestamp": timestamp,
        "action": action,
        "user": user,
        "object": obj,
        "context": context
    }


def _remove_empty_values(field):
    return {key: value for key, value in field.iteritems() if value}


def _market_event(action, object_type, object_id, context=None):
    return log_parser.MarketEvent(
        action=action, object_type=object_type, object_id=object_id, context=context)


def _check_event(expected_event, event):
    assert expected_event.action == event.action
    assert expected_event.object_type == event.object_type
    assert expected_event.object_id == event.object_id
    for key, value in expected_event.context.iteritems():
        assert value == event.context.get(key)
