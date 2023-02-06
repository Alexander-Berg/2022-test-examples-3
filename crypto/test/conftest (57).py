import os

import pytest
import yatest.common

from crypta.ext_fp.delay_line.bin.test.utils.delay_line import DelayLine
from crypta.lib.python.logbroker.test_helpers.logbroker_config import LogbrokerConfig
from crypta.lib.python.logbroker.test_helpers.simple_logbroker_client import SimpleLogbrokerClient

pytest_plugins = [
    "crypta.lib.python.logbroker.test_helpers.fixtures",
]


@pytest.fixture(scope="session")
def ext_fp_event_delayed_log_lb_config(logbroker_port):
    return LogbrokerConfig("localhost", logbroker_port, "ext-fp-event-delayed-log")


@pytest.fixture(scope="session")
def ext_fp_event_log_lb_config(logbroker_port):
    return LogbrokerConfig("localhost", logbroker_port, "ext-fp-event-log")


@pytest.fixture(scope="session")
def ext_fp_event_delayed_log_lb_client(ext_fp_event_delayed_log_lb_config):
    with SimpleLogbrokerClient(ext_fp_event_delayed_log_lb_config) as client:
        yield client


@pytest.fixture(scope="session")
def ext_fp_event_log_lb_client(ext_fp_event_log_lb_config):
    with SimpleLogbrokerClient(ext_fp_event_log_lb_config) as client:
        yield client


@pytest.fixture(scope="module")
def frozen_time(request):
    return getattr(request.module, "FROZEN_TIME", None)


@pytest.fixture(scope="function")
def delay_line_zero_delay(request, ext_fp_event_delayed_log_lb_config, ext_fp_event_log_lb_config, frozen_time):
    app_working_dir = os.path.join(yatest.common.test_output_path(), "delay_line")

    with DelayLine(working_dir=app_working_dir,
                   artificial_delay_sec=0,
                   ext_fp_event_delayed_log_lb_config=ext_fp_event_delayed_log_lb_config,
                   ext_fp_event_log_lb_config=ext_fp_event_log_lb_config,
                   frozen_time=frozen_time) as delay_line:
        yield delay_line


@pytest.fixture(scope="function")
def delay_line_30sec_delay(request, ext_fp_event_delayed_log_lb_config, ext_fp_event_log_lb_config, frozen_time):
    app_working_dir = os.path.join(yatest.common.test_output_path(), "delay_line")

    with DelayLine(working_dir=app_working_dir,
                   artificial_delay_sec=30,
                   ext_fp_event_delayed_log_lb_config=ext_fp_event_delayed_log_lb_config,
                   ext_fp_event_log_lb_config=ext_fp_event_log_lb_config,
                   frozen_time=frozen_time) as delay_line:
        yield delay_line
