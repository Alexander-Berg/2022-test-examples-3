import os

import pytest
import yatest.common

from crypta.ext_fp.matcher.bin.test.utils.matcher import Matcher
from crypta.ext_fp.matcher.bin.test.utils.mock_beeline_api import MockBeelineApi
from crypta.ext_fp.matcher.bin.test.utils.mock_ertelecom_api import MockErtelecomApi
from crypta.ext_fp.matcher.bin.test.utils.mock_intentai_api import MockIntentaiApi
from crypta.ext_fp.matcher.bin.test.utils.mock_mts_api import MockMtsApi
from crypta.ext_fp.matcher.bin.test.utils.mock_rostelecom_api import MockRostelecomApi
from crypta.lib.python.logbroker.test_helpers.logbroker_config import LogbrokerConfig
from crypta.lib.python.logbroker.test_helpers.simple_logbroker_client import SimpleLogbrokerClient

pytest_plugins = [
    "crypta.lib.python.logbroker.test_helpers.fixtures",
]

ROSTELECOM_IP_RANGES = [
    "15.3.62.0-15.3.62.255",
    "15.3.100.0-15.3.100.255",
]


@pytest.fixture(scope="session")
def fp_event_log_lb_config(logbroker_port):
    return LogbrokerConfig("localhost", logbroker_port, "fp-event-log")


@pytest.fixture(scope="session")
def ext_fp_match_log_lb_config(logbroker_port):
    return LogbrokerConfig("localhost", logbroker_port, "ext-fp-match-log")


@pytest.fixture(scope="session")
def fp_event_log_lb_client(fp_event_log_lb_config):
    with SimpleLogbrokerClient(fp_event_log_lb_config) as client:
        yield client


@pytest.fixture(scope="session")
def ext_fp_match_log_lb_client(ext_fp_match_log_lb_config):
    with SimpleLogbrokerClient(ext_fp_match_log_lb_config) as client:
        yield client


@pytest.fixture(scope="module")
def frozen_time(request):
    return getattr(request.module, "FROZEN_TIME", None)


@pytest.fixture(scope="module")
def mock_beeline_api(frozen_time):
    with MockBeelineApi(frozen_time) as api:
        yield api


@pytest.fixture(scope="module")
def mock_ertelecom_api(frozen_time):
    with MockErtelecomApi(frozen_time) as api:
        yield api


@pytest.fixture(scope="module")
def mock_intentai_api(frozen_time):
    with MockIntentaiApi(frozen_time) as api:
        yield api


@pytest.fixture(scope="module")
def mock_mts_api(frozen_time):
    with MockMtsApi(frozen_time) as api:
        yield api


@pytest.fixture(scope="module")
def mock_rostelecom_api(frozen_time):
    with MockRostelecomApi(ROSTELECOM_IP_RANGES, frozen_time) as api:
        yield api


@pytest.fixture(scope="module")
def matcher(
        request,
        fp_event_log_lb_config,
        ext_fp_match_log_lb_config,
        mock_beeline_api,
        mock_ertelecom_api,
        mock_intentai_api,
        mock_mts_api,
        mock_rostelecom_api,
):
    app_working_dir = os.path.join(yatest.common.test_output_path(), "matcher")

    with Matcher(working_dir=app_working_dir,
                 fp_event_log_lb_config=fp_event_log_lb_config,
                 ext_fp_match_log_lb_config=ext_fp_match_log_lb_config,
                 beeline_api_url=mock_beeline_api.address,
                 ertelecom_api_url=mock_ertelecom_api.address,
                 mts_api_url=mock_mts_api.address,
                 intentai_api_url=mock_intentai_api.address,
                 rostelecom_api_url=mock_rostelecom_api.address) as matcher:
        yield matcher
