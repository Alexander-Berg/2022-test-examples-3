from .fixtures import *
from logctl._configure_logfeller import configure_logfeller


def test_create_parser_config(fake_config, mock_logfeller, mock_repo):
    configure_logfeller(mock_logfeller, mock_repo, fake_config, auto_confirm=True)
    mock_logfeller.create_parser_config.assert_called_with(LOG_FIELDS, LOGFELLER_PARSER_CONFIG_PATH)


def test_append_to_parsers_config(fake_config, mock_logfeller, mock_repo):
    configure_logfeller(mock_logfeller, mock_repo, fake_config, auto_confirm=True)
    mock_logfeller.append_to_parsers_config.assert_called_with(LOG, TIME_FORMAT)


def test_update_parsers_ya_make(fake_config, mock_logfeller, mock_repo):
    configure_logfeller(mock_logfeller, mock_repo, fake_config, auto_confirm=True)
    mock_logfeller.update_parsers_ya_make.assert_called()


def test_rewrite_parser_config(fake_config, mock_logfeller, mock_repo):
    fields_without_last_element = LOG_FIELDS[:-1]
    mock_logfeller.configs[LOGFELLER_PARSER_CONFIG_PATH] = {"fields": fields_without_last_element}
    configure_logfeller(mock_logfeller, mock_repo, fake_config, auto_confirm=True)
    mock_logfeller.rewrite_parser_config.assert_called_with(
        LOG_FIELDS, LOGFELLER_PARSER_CONFIG_PATH
    )


def test_dont_touch_parser_configs_if_it_already_configured(fake_config, mock_logfeller, mock_repo):
    mock_logfeller.configs[LOGFELLER_PARSER_CONFIG_PATH] = {"fields": LOG_FIELDS}
    configure_logfeller(mock_logfeller, mock_repo, fake_config, auto_confirm=True)
    mock_logfeller.create_parser_config.assert_not_called()
    mock_logfeller.rewrite_parser_config.assert_not_called()
    mock_logfeller.append_to_parsers_config.assert_not_called()
    mock_logfeller.update_parsers_ya_make.assert_not_called()


def test_raises_on_not_existing_logs_config(fake_config, mock_logfeller, mock_repo):
    del mock_logfeller.configs[LOGFELLER_LOGS_CONFIG_PATH]
    with pytest.raises(Exception):
        configure_logfeller(mock_logfeller, mock_repo, fake_config, auto_confirm=True)


def test_append_to_logs_config(fake_config, mock_logfeller, mock_repo):
    configure_logfeller(mock_logfeller, mock_repo, fake_config, auto_confirm=True)
    mock_logfeller.append_to_logs_config.assert_called_with(
        TOPIC, LOG, LOGFELLER_LOGS_CONFIG_PATH, None
    )


def test_dont_touch_logs_config_if_it_already_configured(fake_config, mock_logfeller, mock_repo):
    mock_logfeller.configs[LOGFELLER_LOGS_CONFIG_PATH] = {"logs": [LOG]}
    configure_logfeller(mock_logfeller, mock_repo, fake_config, auto_confirm=True)
    mock_logfeller.append_to_logs_config.assert_not_called()


def test_pass_custom_periods(fake_config, mock_logfeller, mock_repo):
    TEST_PERIODS = [{"name": "1d", "lifetime": "10d"}]
    fake_config["log_settings"]["periods"] = TEST_PERIODS
    configure_logfeller(mock_logfeller, mock_repo, fake_config, auto_confirm=True)
    mock_logfeller.append_to_logs_config.assert_called_with(
        TOPIC, LOG, LOGFELLER_LOGS_CONFIG_PATH, TEST_PERIODS
    )


def test_raises_on_not_existing_streams_config(fake_config, mock_logfeller, mock_repo):
    del mock_logfeller.configs[LOGFELLER_STREAMS_CONFIG_PATH]
    with pytest.raises(Exception):
        configure_logfeller(mock_logfeller, mock_repo, fake_config, auto_confirm=True)


def test_append_to_streams_config(fake_config, mock_logfeller, mock_repo):
    configure_logfeller(mock_logfeller, mock_repo, fake_config, auto_confirm=True)
    mock_logfeller.append_to_streams_config.assert_called_with(
        TOPIC, LOG, LOGFELLER_STREAMS_CONFIG_PATH
    )


def test_dont_touch_streams_config_if_it_already_configured(fake_config, mock_logfeller, mock_repo):
    mock_logfeller.configs[LOGFELLER_STREAMS_CONFIG_PATH] = {"topics": [TOPIC]}
    configure_logfeller(mock_logfeller, mock_repo, fake_config, auto_confirm=True)
    mock_logfeller.append_to_streams_config.assert_not_called()


def test_commit_changes(fake_config, mock_logfeller, mock_repo):
    configure_logfeller(mock_logfeller, mock_repo, fake_config, auto_confirm=True)
    mock_repo.commit.assert_called_once()
