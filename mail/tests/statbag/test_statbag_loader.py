import pytest
from datetime import date
from fan_feedback.stats.management.commands._statbag import StatbagLoader


@pytest.fixture
def mock_clickhouse(mocker):
    class ClickhouseMock:
        def __init__(self):
            self.query_arg = None
            self.query_result = None

        def query(self, query):
            self.query_arg = query
            return self.query_result

    return mocker.patch(
        "fan_feedback.stats.management.commands._statbag.connect_clickhouse",
        return_value=ClickhouseMock(),
    )


@pytest.fixture
def mock_settings(mocker):
    class SettingsMock:
        def __init__(self):
            self.CLICKHOUSE_DATABASE = ""

    return mocker.patch(
        "fan_feedback.stats.management.commands._statbag.settings",
        return_value=SettingsMock(),
    )


@pytest.fixture
def statbag_loader(mock_clickhouse, mock_settings):
    return StatbagLoader()


def test_load_updated_campaigns_by_date(statbag_loader):
    statbag_loader.clickhouse.query_result = b"1234\n5678\n9101\n"
    campaigns = statbag_loader._load_updated_campaigns_by_date(date.today())
    assert campaigns == [1234, 5678, 9101]


def test_load_aggregated_stats_for_campaigns(statbag_loader):
    statbag_loader.clickhouse.query_result = b"unsubscribe\t1234\t1\npixel\t5678\t2\n"
    stats = statbag_loader._load_aggregated_stats_for_campaigns([1234, 5678])
    assert stats == {
        1234: {"pixel": 0, "unsubscribe": 1},
        5678: {"pixel": 2, "unsubscribe": 0},
    }
