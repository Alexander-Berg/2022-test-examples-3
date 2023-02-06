# -*- coding: utf-8 -*-
from collections import namedtuple
import json
import logging

from yt.common import YtResponseError

from crypta.profile.runners.segments.lib.constructor_segments.build_constructor_segments import ConstructorSegmentsConfig
from crypta.profile.runners.segments.lib.constructor_segments.daily_rule_processors.browser import (
    GetStandardSegmentsByBrowserUrlsAndHostsDayProcessor,
    GetStandardSegmentsByBrowserTitlesDayProcessor,
)
from crypta.profile.runners.segments.lib.constructor_segments.daily_rule_processors.catalogia.catalogia import GetStandardSegmentsByCatalogiaDailyProcessor
from crypta.profile.runners.segments.lib.constructor_segments.daily_rule_processors.metrica import (
    GetStandardSegmentsByMetricaUrlsAndHostsDayProcessor,
    GetStandardSegmentsByMetricaTitlesDayProcessor,
    GetStandardSegmentsByMetricaCountersAndGoalsDailyProcessor)
from crypta.profile.runners.segments.lib.constructor_segments.daily_rule_processors.mobile_apps.apps import GetStandardSegmentsByMobileApp
from crypta.profile.runners.segments.lib.constructor_segments.daily_rule_processors.precalculated_tables.precalculated_tables import GetStandardSegmentsByPrecalculatedTables
from crypta.profile.runners.segments.lib.constructor_segments.daily_rule_processors.search_requests import (
    GetStandardSegmentsBySearchRequestsDayProcessor,
    GetStandardSegmentsBySearchResultsDayProcessor,
)
from crypta.profile.runners.segments.lib.constructor_segments.daily_rule_processors.yandex_referrer import GetStandardSegmentsByYandexReferrerUrlsAndHostsDayProcessor
from crypta.profile.utils.segment_utils.url_filter import UrlFilter


Export = namedtuple('Export', ['keywordId', 'segmentId'])
ExportWithRuleId = namedtuple('ExportWithRuleId', ['keywordId', 'segmentId', 'state', 'type', 'ruleId'])
Rule = namedtuple('Rule', ['id', 'minDays', 'days', 'conditions'])
RuleCondition = namedtuple('RuleCondition', ['ruleId', 'revision', 'values', 'source', 'state'])


class NestedResult(object):
    def __init__(self, value):
        self.value = value

    def result(self):
        return self.value


class ApiLabMock(object):
    def __init__(self, *args, **kwargs):
        self.exports = kwargs.get('exports', [])

    def getExportsWithRuleId(self):
        return NestedResult(self.exports)

    def getRuleCondition(*args, **kwargs):
        return NestedResult(RuleCondition('', '', '', '', ''))

    def putRuleCondition(*args, **kwargs):
        return NestedResult(None)


class ApiMock(object):
    def __init__(self, *args, **kwargs):
        self.lab = ApiLabMock(*args, **kwargs)


class YtMock(object):
    def __init__(self, paths):
        self.paths = set(paths)

    def exists(self, path):
        return path in self.paths

    def get_attribute(self, path, name):
        if path in self.paths and name == 'schema':
            return []

        error = YtResponseError(None)
        error.is_access_denied = lambda: True
        raise error


def test_good_exports():
    exports = [
        ExportWithRuleId(217, 1, 'ACTIVE', 'PROBABILISTIC', 'rule-1'),
        ExportWithRuleId(544, 5, 'CREATED', 'HEURISTIC', 'rule-2'),
    ]

    api = ApiMock(exports=exports)

    segments_config = ConstructorSegmentsConfig(logger=logging, yt=None, api=api)
    rule_id_to_segment_export = segments_config.rule_id_to_segment_export
    assert rule_id_to_segment_export['rule-1'] == (217, 1)
    assert rule_id_to_segment_export['rule-2'] == (544, 5)


def test_filter_inactive_exports():
    exports = [
        ExportWithRuleId(217, 1, 'DISABLED', 'PROBABILISTIC', 'rule-1'),
        ExportWithRuleId(544, 5, 'CREATED', 'HEURISTIC', 'rule-2'),
    ]

    api = ApiMock(exports=exports)

    segments_config = ConstructorSegmentsConfig(logger=logging, yt=None, api=api)
    rule_id_to_segment_export = segments_config.rule_id_to_segment_export
    assert 'rule-1' not in rule_id_to_segment_export
    assert rule_id_to_segment_export['rule-2'] == (544, 5)


def test_filter_shortterm_exports():
    exports = [
        ExportWithRuleId(602, 1, 'ACTIVE', 'PROBABILISTIC', 'rule-1'),
        ExportWithRuleId(544, 5, 'CREATED', 'SHORTTERM', 'rule-2'),
        ExportWithRuleId(602, 5, 'CREATED', 'SHORTTERM', 'rule-3'),
    ]

    api = ApiMock(exports=exports)

    segments_config = ConstructorSegmentsConfig(logger=logging, yt=None, api=api)
    rule_id_to_segment_export = segments_config.rule_id_to_segment_export
    assert len(rule_id_to_segment_export) == 0


def test_good_config():
    segments_config = ConstructorSegmentsConfig(logger=logging, yt=YtMock(['//path']), api=ApiMock())
    segments_config.rule_id_to_segment_export = {
        'rule-1': Export(216, 42),
        'rule-2': Export(217, 666),
    }
    segments_config.existing_apps = {
        'youdrive.today',
        'com.matreshcar',
    }
    segments_config.existing_hosts = {
        'catalog-svadba.ru': (1, 2),
        'gorko.by': (2, 3),
        'nalog.ru': (10, 1),
        'example.com': (10, 0),
        'ura.org': (0, 3),
    }

    segments_config.rules = [
        Rule(
            'rule-1',
            2,
            35,
            [
                RuleCondition(
                    'rule-1',
                    1,
                    [u'(анализ AND финансирование) OR (анализ AND NOT инвестиция)', u'риск'],
                    'WORDS',
                    'APPROVED',
                ),
                RuleCondition('rule-1', 2, ['youdrive.today', 'com.matreshcar', 'ru.car5.app'], 'APPS', 'APPROVED'),
                RuleCondition('rule-1', 11, ['nalog.ru', 'example.com', 'gorko.by/someurl'], 'PUBLIC_SITES', 'APPROVED'),
                RuleCondition('rule-1', 12, [u'анализ AND финансирование'], 'PUBLIC_WORDS', 'APPROVED'),
                RuleCondition('rule-1', 13, [json.dumps({
                    'path': '//path',
                    'idKey': 'yuid',
                    'idType': 'yandexuid',
                    'updateInterval': 0
                })], 'PRECALCULATED_TABLES', 'APPROVED'),
            ]
        ),
        Rule(
            'rule-2',
            1,
            35,
            [
                RuleCondition('rule-2', 3, ['catalog-svadba.ru', 'gorko.by/someurl', 'krasnayagorka.ru', r'regexp:nalog.ru/rn\d+/ip', 'ura.org/salut'], 'SITES', 'APPROVED'),
                RuleCondition('rule-2', 4, ['one', 'some AND someelse'], 'PUBLIC_WORDS', 'APPROVED'),
                RuleCondition('rule-2', 6, ['1028356:8596450', '1028357'], 'METRICA_COUNTERS_AND_GOALS', 'APPROVED'),
                RuleCondition('rule-2', 7, ['three', 'some4 AND someelse4'], 'WORDS', 'APPROVED'),
                RuleCondition('rule-2', 8, ['aws.amazon.com', 'selectel.ru'], 'SEARCH_RESULTS_HOSTS', 'APPROVED'),
                RuleCondition('rule-2', 9, ['200001450 # comment', '200005871', '200006993'], 'CATALOGIA', 'APPROVED'),
                RuleCondition('rule-2', 10, ['approve AND me'], 'PUBLIC_WORDS', 'NEED_APPROVE'),
            ]
        ),
    ]
    segments_config.read()
    daily_tasks, aggregate_tasks = segments_config.prepare_rules()

    assert aggregate_tasks[GetStandardSegmentsByMobileApp]['app_to_rule_lab_id'] == {'com.matreshcar': {'rule-1'}, 'youdrive.today': {'rule-1'}}
    assert aggregate_tasks[GetStandardSegmentsByMobileApp]['rule_revision_ids'] == {2L}

    assert daily_tasks[GetStandardSegmentsBySearchResultsDayProcessor]['host_to_rule_revision_id'] == {'selectel.ru': {8L}, 'aws.amazon.com': {8L}}
    assert daily_tasks[GetStandardSegmentsBySearchResultsDayProcessor]['rule_revision_ids'] == {8L}

    assert segments_config.rule_revision_id_to_rule_id == {
        1L: 'rule-1',
        2L: 'rule-1',
        3L: 'rule-2',
        4L: 'rule-2',
        6L: 'rule-2',
        7L: 'rule-2',
        8L: 'rule-2',
        9L: 'rule-2',
        11L: 'rule-1',
        12L: 'rule-1',
        13L: 'rule-1',
    }
    assert (daily_tasks[GetStandardSegmentsByCatalogiaDailyProcessor]['catalogia_id_to_rule_revision_id'] == {200006993: {9L}, 200001450: {9L}, 200005871: {9L}})
    assert (daily_tasks[GetStandardSegmentsByCatalogiaDailyProcessor]['rule_revision_ids'] == {9L})

    assert (daily_tasks[GetStandardSegmentsByMetricaTitlesDayProcessor]['rule_revision_ids'] == {1L, 7L})
    assert (daily_tasks[GetStandardSegmentsBySearchRequestsDayProcessor]['rule_revision_ids'] == {1L, 4L, 7L, 12L})
    assert (daily_tasks[GetStandardSegmentsByBrowserTitlesDayProcessor]['rule_revision_ids'] == {1L, 4L, 7L, 12L})

    assert (daily_tasks[GetStandardSegmentsByMetricaCountersAndGoalsDailyProcessor]['metrica_counter_ids_conditions'] == {6L: {'1028356', '1028357'}})
    assert (daily_tasks[GetStandardSegmentsByMetricaCountersAndGoalsDailyProcessor]['metrica_goal_ids_conditions'] == {6L: {'8596450'}})
    assert (daily_tasks[GetStandardSegmentsByMetricaCountersAndGoalsDailyProcessor]['rule_revision_ids'] == {6L})

    browser = UrlFilter()
    browser.add_url(11L, 'nalog.ru', r'nalog\\.ru/.*')
    browser.add_url(3L, 'catalog-svadba.ru', r'catalog-svadba\\.ru/.*')
    browser.add_url(3L, 'gorko.by', r'gorko\\.by/someurl.*')
    browser.add_url(11L, 'gorko.by', r'gorko\\.by/someurl.*')
    browser.add_url(3L, 'nalog.ru', r'nalog\\.ru/rn\\d+/ip')
    browser.add_url(3L, 'ura.org', r'ura\\.org/salut.*')
    assert daily_tasks[GetStandardSegmentsByBrowserUrlsAndHostsDayProcessor]['url_filter'].rule_revisions == browser.rule_revisions
    assert daily_tasks[GetStandardSegmentsByBrowserUrlsAndHostsDayProcessor]['rule_revision_ids'] == {3L, 11L}

    yandex = UrlFilter()
    yandex.add_url(11L, 'nalog.ru', r'nalog\\.ru/.*')
    yandex.add_url(11L, 'example.com', r'example\\.com/.*')
    yandex.add_url(11L, 'gorko.by', r'gorko\\.by/someurl.*')
    assert daily_tasks[GetStandardSegmentsByYandexReferrerUrlsAndHostsDayProcessor]['url_filter'].rule_revisions == yandex.rule_revisions
    assert daily_tasks[GetStandardSegmentsByYandexReferrerUrlsAndHostsDayProcessor]['rule_revision_ids'] == {11L}

    metrica = UrlFilter()
    metrica.add_url(3L, 'catalog-svadba.ru', r'catalog-svadba\\.ru/.*')
    metrica.add_url(3L, 'gorko.by', r'gorko\\.by/someurl.*')
    metrica.add_url(3L, 'nalog.ru', r'nalog\\.ru/rn\\d+/ip')
    assert daily_tasks[GetStandardSegmentsByMetricaUrlsAndHostsDayProcessor]['url_filter'].rule_revisions == metrica.rule_revisions
    assert daily_tasks[GetStandardSegmentsByMetricaUrlsAndHostsDayProcessor]['rule_revision_ids'] == {3L}

    assert (segments_config.min_days_thresholds_by_rule_id == {'rule-2': 1, 'rule-1': 2})
    assert (segments_config.max_days_thresholds_by_rule_id == {'rule-2': 35, 'rule-1': 35})

    assert aggregate_tasks[GetStandardSegmentsByPrecalculatedTables]['table_rules'] == {'//path': {'rule-1': {
        'id_column': 'yuid',
        'id_type': 'yandexuid',
        'update_interval': 0,
    }}}
    assert aggregate_tasks[GetStandardSegmentsByPrecalculatedTables]['rule_revision_ids'] == {13L}


def test_bad_source():
    segments_config = ConstructorSegmentsConfig(logger=logging, yt=None, api=None)
    segments_config.rule_id_to_segment_export = {'rule-1': Export(216, 42)}
    segments_config.existing_apps = {'youdrive.today', 'com.matreshcar'}
    segments_config.existing_hosts = {'catalog-svadba.ru': (1, 2), 'gorko.by': (2, 3), 'nalog.ru': (10, 0)}

    segments_config.rules = [
        Rule(
            'rule-1',
            2,
            35,
            [
                RuleCondition(
                    'rule-1',
                    1,
                    ['asa', 'asda'],
                    'SOME_CRAZY_SOURCE',
                    'APPROVED',
                ),
            ]
        ),
    ]

    segments_config.read()
    daily_tasks, aggregate_tasks = segments_config.prepare_rules()
    assert not daily_tasks
    assert not aggregate_tasks


def test_bad_condition():
    segments_config = ConstructorSegmentsConfig(logger=logging, yt=YtMock(['//missing_parameter']), api=ApiMock())
    segments_config.rule_id_to_segment_export = {'rule-1': Export(216, 42)}
    segments_config.existing_apps = {'youdrive.today', 'com.matreshcar'}
    segments_config.existing_hosts = {'catalog-svadba.ru': (1, 2), 'gorko.by': (2, 3), 'nalog.ru': (10, 0)}

    segments_config.rules = [
        Rule(
            'rule-1',
            2,
            35,
            [
                RuleCondition(
                    'rule-1',
                    1,
                    ['asa asda', 'asda'],
                    'WORDS',
                    'APPROVED',
                ),
                RuleCondition(
                    'rule-1',
                    2,
                    [],
                    'WORDS',
                    'APPROVED',
                ),
                RuleCondition('rule-1', 3, [json.dumps({
                    'path': '//missing_parameter',
                    'idKey': 'yuid',
                    'idType': 'yandexuid',
                })], 'PRECALCULATED_TABLES', 'APPROVED'),
                RuleCondition('rule-1', 4, [json.dumps({
                    'path': '//unknown_path',
                    'idKey': 'yuid',
                    'idType': 'yandexuid',
                    'updateInterval': 0
                })], 'PRECALCULATED_TABLES', 'APPROVED'),
                RuleCondition('rule-1', 5, [], 'PRECALCULATED_TABLES', 'APPROVED'),
            ]
        ),
    ]

    segments_config.read()
    daily_tasks, aggregate_tasks = segments_config.prepare_rules()

    assert set(daily_tasks.keys()) == {GetStandardSegmentsBySearchRequestsDayProcessor, GetStandardSegmentsByMetricaTitlesDayProcessor, GetStandardSegmentsByBrowserTitlesDayProcessor}
    assert (daily_tasks[GetStandardSegmentsBySearchRequestsDayProcessor]['yql_filter'].conditions == {1: 'asda'})
    assert (daily_tasks[GetStandardSegmentsByMetricaTitlesDayProcessor]['yql_filter'].conditions == {1: 'asda'})
    assert (daily_tasks[GetStandardSegmentsByBrowserTitlesDayProcessor]['yql_filter'].conditions == {1: 'asda'})


def test_missing_site_condition():
    segments_config = ConstructorSegmentsConfig(logger=logging, yt=None, api=None)
    segments_config.rule_id_to_segment_export = {'rule-1': Export(216, 42)}
    segments_config.existing_hosts = {'catalog-svadba.ru': (1, 2), 'gorko.by': (2, 3), 'nalog.ru': (10, 0)}

    segments_config.rules = [
        Rule(
            'rule-1',
            2,
            35,
            [
                RuleCondition(
                    'rule-1',
                    1,
                    ['example.com', 'nalog.ru'],
                    'PUBLIC_SITES',
                    'APPROVED',
                ),
            ]
        ),
    ]

    segments_config.read()
    daily_tasks, aggregate_tasks = segments_config.prepare_rules()
    sites = UrlFilter()
    sites.add_url(1, 'nalog.ru', r'nalog\\.ru/.*')
    assert set(daily_tasks.keys()) == {GetStandardSegmentsByYandexReferrerUrlsAndHostsDayProcessor, GetStandardSegmentsByBrowserUrlsAndHostsDayProcessor}
    assert daily_tasks[GetStandardSegmentsByYandexReferrerUrlsAndHostsDayProcessor]['url_filter'].rule_revisions == sites.rule_revisions
    assert not daily_tasks[GetStandardSegmentsByBrowserUrlsAndHostsDayProcessor]['url_filter'].rule_revisions
