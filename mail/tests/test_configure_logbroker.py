from .fixtures import *
from logctl._configure_logbroker import configure_logbroker
from pathlib import PurePath


def test_create_new_topic(fake_config, mock_logbroker):
    mock_logbroker.topics = []
    configure_logbroker(mock_logbroker, fake_config, auto_confirm=True)
    mock_logbroker.create_topic.assert_called_with(TOPIC)


def test_create_missing_directories(fake_config, mock_logbroker, mocker):
    mock_logbroker.topics = []
    configure_logbroker(mock_logbroker, fake_config, auto_confirm=True)
    mock_logbroker.create_directory.assert_has_calls(
        [mocker.call(str(parent)) for parent in parents(TOPIC)]
    )


def test_grant_write_access_for_new_topic(fake_config, mock_logbroker):
    mock_logbroker.topics = []
    configure_logbroker(mock_logbroker, fake_config, auto_confirm=True)
    mock_logbroker.grant_write_access.assert_called_with(TOPIC, TVM_ID)


def test_enable_yt_delivery_for_new_topic(fake_config, mock_logbroker):
    mock_logbroker.topics = []
    configure_logbroker(mock_logbroker, fake_config, auto_confirm=True)
    mock_logbroker.enable_yt_delivery.assert_called_with(TOPIC)


def test_dont_create_if_topic_already_exists(fake_config, mock_logbroker):
    mock_logbroker.topics = [TOPIC]
    configure_logbroker(mock_logbroker, fake_config, auto_confirm=True)
    mock_logbroker.create_topic.assert_not_called()
    mock_logbroker.create_directory.assert_not_called()


def parents(path):
    parents = reversed(PurePath("/" + path).parents)
    parents = filter(lambda path: path != PurePath("/"), parents)
    return parents
