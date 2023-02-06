import datetime
import os

import flask
import pytest
import yatest.common
from yt import yson
from yt.wrapper import ypath

from crypta.buchhalter.services.main.lib.audience_shadow_dmp_report_calc.config_pb2 import TConfig
from crypta.lib.python import (
    templater,
    test_utils,
    yaml_config,
)
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
    utils,
)


pytest_plugins = [
    "crypta.lib.python.test_utils.fixtures",
    "crypta.lib.python.tvm.test_utils.fixtures",
    "crypta.lib.python.yql.test_helpers.fixtures",
    "crypta.lib.python.yt.test_helpers.fixtures",
]


@pytest.fixture(scope="function")
def mock_audience_server():
    class MockAudienceServer(test_utils.FlaskMockServer):
        def __init__(self):
            super(MockAudienceServer, self).__init__("MockAudienceServer")
            self.grants = {
                1: [{"user_login": "representative1.1"}, {"user_login": "xxx"}],
                2: [{"user_login": "chief1"}, {"user_login": "representative1.1"}],
                3: [{"user_login": "chief2"}, {"user_login": "representative2.1"}],
                4: [{"user_login": "chief1"}],
                5: [{"user_login": "chief1"}],
            }
            self.segments = {
                "shadow-dmp-login-1": [{
                    "id": 1,
                    "name": "title 1",
                    "owner": "shadow-dmp-login-1",
                }, {
                    "id": 2,
                    "name": "title 2",
                    "owner": "shadow-dmp-login-1",
                }, {
                    "id": 4,
                    "name": "title 4",
                    "owner": "shadow-dmp-login-444@local.local",
                }, {
                    "id": 5,
                    "name": "title 5",
                    "owner": "shadow-dmp-login-777",
                }],
                "shadow-dmp-login-2": [{
                    "id": 3,
                    "name": "тайтл 3",
                    "owner": "shadow-dmp-login-2",
                }],
                "shadow-dmp-login-444@local.local": [{
                    "id": 4,
                    "name": "title 4",
                    "owner": "shadow-dmp-login-444@local.local",
                }],
                "shadow-dmp-login-777": [{
                    "id": 5,
                    "name": "title 5",
                    "owner": "shadow-dmp-login-777",
                }],
            }

            self.app.config["JSON_AS_ASKII"] = False

            @self.app.route("/v1/management/segments")
            def list_segments():
                login = flask.request.args["ulogin"]
                return flask.jsonify(segments=self.segments[login])

            @self.app.route("/v1/management/segment/<int:segment_id>/grants")
            def public_list_grants(segment_id):
                return flask.jsonify(grants=self.grants[segment_id])

    with MockAudienceServer() as mock:
        yield mock


@pytest.fixture(scope="function")
def config_file(local_yt, mock_audience_server, mock_sandbox_server_with_identifiers_udf, input_stats_dates, tvm_api):
    config_file_path = yatest.common.test_output_path("config.yaml")

    ttl_days = utils.get_unexpired_ttl_days_for_daily(min(input_stats_dates))

    templater.render_file(
        yatest.common.source_path("crypta/buchhalter/services/main/config/audience_shadow_dmp_report_calc/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": local_yt.get_server(),
            "audience_url": "http://{}".format(mock_audience_server.host),
            "audience_port": mock_audience_server.port,
            "crypta_identifier_udf_url": mock_sandbox_server_with_identifiers_udf.get_udf_url(),
            "stats_ttl_days": ttl_days,
            "daily_report_ttl_days": ttl_days + 1,
            "monthly_report_ttl_days": ttl_days + 2,
            "audience_src_tvm_id": tvm_api.issue_id(),
            "audience_dst_tvm_id": tvm_api.issue_id(),
        },
    )
    return config_file_path


@pytest.fixture(scope="function")
def config(config_file):
    return yaml_config.parse_config(TConfig, config_file)


@pytest.fixture(scope="function")
def input_stats_dates():
    return [datetime.datetime(year=2020, month=1, day=day).strftime("%Y-%m-%d") for day in range(1, 31)]


@pytest.fixture(scope="function")
def input_stats_tables(config, input_stats_dates):
    result = []
    for date in input_stats_dates:
        local_path = os.path.abspath("input_table_{}.yson".format(date))
        yt_path = ypath.ypath_join(config.ShadowDmpPerSegmentLoginStatsDir, date)

        with open(local_path, "w") as f:
            yson.dump([{
                "date": date,
                "placeid": 542,
                "logins_to_charge": ["xxx"],
                "segment_owner": "shadow-dmp-login-1",
                "clicks": 2,
                "shows": 4,
                "segmentid": 777,
                "segment_name": "Name 777",
                "usage": "Retargeting & Audiences",
            }], f, yson_type="list_fragment")

        result.append((tables.YsonTable(local_path, yt_path), tests.Exists()))

    return result
