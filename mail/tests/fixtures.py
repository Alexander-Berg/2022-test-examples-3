from logctl._config import Config
import pytest
from pathlib import PurePath
from unittest.mock import MagicMock

PATH = "mail/test-app"
APP = "test-app"
ENV = "test-env"
TVM_ID = "test-tvm-id"
TVM_SECRET_VALUE = "test-tvm-secret"
LOG = "test-log"
LOG_FIELDS = ["field1", "field2"]
TOPIC_PATTERN = "{app}/{env}/{log}"
TOPIC = TOPIC_PATTERN.format(app=APP, env=ENV, log=LOG)
TIME_FORMAT = "%Y-%m-%d %H:%M:%S"

PUSH_CLIENT_CONFIG_LOCAL_PATH = "etc/statbox-push-client/push-client.yaml"
PUSH_CLIENT_CONFIG_PATH = "{}/{}".format(PATH, PUSH_CLIENT_CONFIG_LOCAL_PATH)
LOG_PATH = "/var/log/{}/{}.log".format(APP, LOG)

LOGFELLER_CONFIGS_DIR = "logfeller/configs"
LOGFELLER_LOG_CONFIGS_DIR = "{}/logs".format(LOGFELLER_CONFIGS_DIR)
LOGFELLER_STREAM_CONFIGS_DIR = LOGFELLER_LOG_CONFIGS_DIR
LOGFELLER_PARSER_CONFIGS_DIR = "{}/parsers".format(LOGFELLER_CONFIGS_DIR)
LOGFELLER_LOGS_CONFIG_NAME = "test_logs"
LOGFELLER_STREAMS_CONFIG_NAME = "test_streams"
LOGFELLER_LOGS_CONFIG_PATH = "{}/{}.json".format(
    LOGFELLER_LOG_CONFIGS_DIR, LOGFELLER_LOGS_CONFIG_NAME
)
LOGFELLER_STREAMS_CONFIG_PATH = "{}/{}.json".format(
    LOGFELLER_STREAM_CONFIGS_DIR, LOGFELLER_STREAMS_CONFIG_NAME
)
LOGFELLER_PARSER_CONFIG_PATH = "{}/{}.json".format(LOGFELLER_PARSER_CONFIGS_DIR, LOG)


@pytest.fixture
def fake_config():
    config = Config(
        {
            "app": APP,
            "envs": [ENV],
            "tvm_id": {ENV: TVM_ID},
            "log_settings": {
                "topic": TOPIC_PATTERN,
                "push_client_config_path": PUSH_CLIENT_CONFIG_LOCAL_PATH,
                "fs_log_path": LOG_PATH,
                "tvm_secret_value": TVM_SECRET_VALUE,
                "time_format": TIME_FORMAT,
                "logfeller_parser_name": LOG,
                "logfeller_log_name": LOG,
                "logfeller_logs_config_name": LOGFELLER_LOGS_CONFIG_NAME,
                "logfeller_streams_config_name": LOGFELLER_STREAMS_CONFIG_NAME,
            },
            "logs": [{"name": LOG, "fields": LOG_FIELDS}],
        }
    )
    return config


@pytest.fixture
def mock_logbroker():
    logbroker = MagicMock()
    logbroker.topics = []

    def fake_list(path):
        path = PurePath(path)
        ret = []
        for topic in logbroker.topics:
            topic_path = PurePath("/" + topic)
            if path not in topic_path.parents:
                continue
            while topic_path != PurePath("/"):
                if topic_path.parent == path:
                    ret.append(topic_path.name)
                    break
                topic_path = topic_path.parent
        return ret

    def fake_create_topic(path):
        logbroker.topics.append(path)

    logbroker.list.side_effect = fake_list
    logbroker.create_topic.side_effect = fake_create_topic

    return logbroker


@pytest.fixture
def mock_logfeller():
    logfeller = MagicMock()
    logfeller.configs = {
        LOGFELLER_LOGS_CONFIG_PATH: {"logs": []},
        LOGFELLER_STREAMS_CONFIG_PATH: {"topics": []},
    }

    def logs_config_path(config_name):
        return "{}/{}.json".format(LOGFELLER_LOG_CONFIGS_DIR, config_name)

    def streams_config_path(config_name):
        return "{}/{}.json".format(LOGFELLER_STREAM_CONFIGS_DIR, config_name)

    def parser_config_path(parser_name):
        return "{}/{}.json".format(LOGFELLER_PARSER_CONFIGS_DIR, parser_name)

    def config_exists(path):
        return path in logfeller.configs

    def get_logs_from_logs_config(path):
        return logfeller.configs[path]["logs"]

    def get_topics_from_streams_config(path):
        return logfeller.configs[path]["topics"]

    def get_fields_from_parser_config(path):
        return logfeller.configs[path]["fields"]

    logfeller.logs_config_path.side_effect = logs_config_path
    logfeller.streams_config_path.side_effect = streams_config_path
    logfeller.parser_config_path.side_effect = parser_config_path
    logfeller.config_exists.side_effect = config_exists
    logfeller.get_logs_from_logs_config.side_effect = get_logs_from_logs_config
    logfeller.get_topics_from_streams_config.side_effect = get_topics_from_streams_config
    logfeller.get_fields_from_parser_config.side_effect = get_fields_from_parser_config
    logfeller.parser_configs_path.return_value = LOGFELLER_PARSER_CONFIGS_DIR
    logfeller.log_configs_dir.return_value = LOGFELLER_LOG_CONFIGS_DIR
    logfeller.stream_configs_dir.return_value = LOGFELLER_STREAM_CONFIGS_DIR

    return logfeller


@pytest.fixture
def mock_push_client():
    push_client = MagicMock()
    push_client.configs = {}

    def config_exists(path):
        return path in push_client.configs

    def get_topics_from_config(path):
        return push_client.configs[path]["topics"]

    push_client.config_exists.side_effect = config_exists
    push_client.get_topics_from_config.side_effect = get_topics_from_config

    return push_client


@pytest.fixture
def mock_repo():
    return MagicMock()
