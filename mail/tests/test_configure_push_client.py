from .fixtures import *
from logctl._configure_push_client import configure_push_client

_TVM_OPTIONS = {"id": TVM_ID, "secret_value": TVM_SECRET_VALUE}
_TVM_SECRET_PATH = "/etc/app/tvm_secret"
_TVM_OPTIONS_WITH_SECRET_PATH = {"id": TVM_ID, "secret_path": _TVM_SECRET_PATH}


@pytest.fixture
def fake_config_with_tvm_secret_path(fake_config):
    fake_config["log_settings"]["tvm_secret_path"] = _TVM_SECRET_PATH
    del fake_config["log_settings"]["tvm_secret_value"]
    return fake_config


def test_create_config(fake_config, mock_push_client, mock_repo):
    mock_push_client.configs = {}
    configure_push_client(mock_push_client, mock_repo, fake_config, PATH, auto_confirm=True)
    mock_push_client.create_config.assert_called_with(
        _TVM_OPTIONS, TOPIC, LOG_PATH, PUSH_CLIENT_CONFIG_PATH, None
    )


def test_append_topic_to_existing_config(fake_config, mock_push_client, mock_repo):
    mock_push_client.configs[PUSH_CLIENT_CONFIG_PATH] = {"topics": []}
    configure_push_client(mock_push_client, mock_repo, fake_config, PATH, auto_confirm=True)
    mock_push_client.append_topic_to_config.assert_called_with(
        TOPIC, LOG_PATH, PUSH_CLIENT_CONFIG_PATH, None
    )


def test_dont_touch_config_if_topic_already_configured(fake_config, mock_push_client, mock_repo):
    mock_push_client.configs[PUSH_CLIENT_CONFIG_PATH] = {"topics": [TOPIC]}
    configure_push_client(mock_push_client, mock_repo, fake_config, PATH, auto_confirm=True)
    mock_push_client.create_config.assert_not_called()
    mock_push_client.append_topic_to_config.assert_not_called()


def test_commit_changes(fake_config, mock_push_client, mock_repo):
    mock_push_client.configs = {}
    configure_push_client(mock_push_client, mock_repo, fake_config, PATH, auto_confirm=True)
    mock_repo.commit.assert_called_once()


def test_pass_additional_options(fake_config, mock_push_client, mock_repo):
    OPTIONS = {"test_option": "test_value"}
    fake_config["log_settings"]["push_client_log_options"] = OPTIONS
    mock_push_client.configs = {}
    configure_push_client(mock_push_client, mock_repo, fake_config, PATH, auto_confirm=True)
    mock_push_client.create_config.assert_called_with(
        _TVM_OPTIONS, TOPIC, LOG_PATH, PUSH_CLIENT_CONFIG_PATH, OPTIONS
    )


def test_pass_tvm_secret_path(fake_config_with_tvm_secret_path, mock_push_client, mock_repo):
    mock_push_client.configs = {}
    configure_push_client(
        mock_push_client, mock_repo, fake_config_with_tvm_secret_path, PATH, auto_confirm=True
    )
    mock_push_client.create_config.assert_called_with(
        _TVM_OPTIONS_WITH_SECRET_PATH, TOPIC, LOG_PATH, PUSH_CLIENT_CONFIG_PATH, None
    )
