import os
import pytest

from crypta.lib.python.yt import yt_helpers
from crypta.profile.runners.segments.lib.constructor_segments.common import utils
from crypta.profile.utils.config import config


@pytest.fixture
def prepared_rule_revision_ids():
    return [1, 2, 3]


@pytest.fixture
def rule_revision_ids():
    return [1, 2, 3, 4, 5]


@pytest.fixture
def class_name():
    return 'GetStandardSegmentsByYandexReferrerUrlsAndHostsDayProcessor'


@pytest.fixture
def target_table(class_name, date):
    return os.path.join(
        config.DAILY_STANDARD_HEURISTIC_DIRECTORY,
        class_name,
        date,
    )


@pytest.fixture
def addition_table(class_name, date):
    return os.path.join(
        config.DAILY_ADDITION_STANDARD_HEURISTIC_DIRECTORY,
        class_name,
        date,
    )


def test_daily_rules_target(clean_local_yt, date, prepared_rule_revision_ids, rule_revision_ids, target_table, addition_table, class_name):
    yt_client = clean_local_yt.get_yt_client()

    yt_helpers.create_empty_table(
        yt_client=yt_client,
        path=target_table,
        additional_attributes={'rule_ids': prepared_rule_revision_ids},
    )
    drt = utils.DailyRulesTarget(date, class_name, rule_revision_ids, yt_client=yt_client)

    assert drt.get_rule_ids() == frozenset(prepared_rule_revision_ids)
    assert drt.table == target_table
    assert not drt.exists()


def test_daily_rules_processor(clean_local_yt, date, prepared_rule_revision_ids, rule_revision_ids, target_table, class_name, patched_config):
    yt_client = clean_local_yt.get_yt_client()

    yt_helpers.create_empty_table(
        yt_client=yt_client,
        path=target_table,
        additional_attributes={'rule_ids': prepared_rule_revision_ids},
    )
    drp = utils.DailyRulesProcessor(date, rule_revision_ids)
    drp.output_target = utils.DailyRulesTarget(date, class_name, rule_revision_ids, yt_client=yt_client)

    assert drp.rule_revision_ids_to_be_prepared == frozenset({4, 5})
