import pytest
import yatest.common

from crypta.lib.python import yaml_config


CONFIG_WITH_ZERO_RETURN_CODE = {
    "task_name": "zero_return_code",
    "run_period_seconds": 10,
    "cmd": "date +%s",
}

CONFIG_WITH_NONZERO_RETURN_CODE = {
    "task_name": "nonzero_return_code",
    "run_period_seconds": 10,
    "cmd": "date --unknown-argument",
}


@pytest.mark.parametrize("enable_task_monitoring", [
    pytest.param(True, id="task_monitoring_enabled"),
    pytest.param(False, id="task_monitoring_disabled"),
])
@pytest.mark.parametrize("environment", [
    "testing",
    "production",
])
@pytest.mark.parametrize("config", [
    pytest.param(CONFIG_WITH_ZERO_RETURN_CODE, id="config_with_zero_return_code"),
    pytest.param(CONFIG_WITH_NONZERO_RETURN_CODE, id="config_with_nonzero_return_code"),
])
def test_run_periodic_task(mock_juggler_server, enable_task_monitoring, environment, config):
    binary = yatest.common.binary_path("crypta/utils/run_periodic_task/bin/crypta-run-periodic-task")
    config = dict(
        enable_task_monitoring=enable_task_monitoring,
        juggler_host=mock_juggler_server.host,
        juggler_port=mock_juggler_server.port,
        **config)
    config_path = yatest.common.test_output_path("config.yaml")

    yaml_config.dump(config, config_path)

    try:
        yatest.common.execute(
            [binary, "--config", config_path],
            check_exit_code=False,
            timeout=15,
            env=dict(CRYPTA_ENVIRONMENT=environment))
    except Exception:
        pass

    return mock_juggler_server.dump_events_requests()
