# -*- coding: utf-8 -*-

from async_publishing.action_state_logger import ActionStateLogger
from async_publishing.generation_meta import GenerationMeta
from async_publishing import actions

FULL_GENERATION = GenerationMeta('20180101_0600')


def test_dump_and_load_empty():
    action_state_logger = ActionStateLogger()
    action_state_logger.dump_state("empty.json")

    action_state_logger2 = ActionStateLogger()
    action_state_logger2.load_state("empty.json")

    assert action_state_logger == action_state_logger2


def test_dump():
    action_state_logger = ActionStateLogger()

    action_state_logger.log_action('cluster1', actions.ClusterReloadFull({}, {}, FULL_GENERATION, 600, {}),)
    action_state_logger.log_action('cluster2', actions.NoNeedToReload({}))
    action_state_logger.log_action('cluster3', actions.ClusterOverLimit({}))
    action_state_logger.log_action('cluster4', actions.ClusterWaitForSecondReloadPhase({}))

    data = action_state_logger.as_dict()

    assert data['clusters']['cluster1']['action'] == "ClusterReloadFull"
    assert data['clusters']['cluster1']['full_generation'] == FULL_GENERATION.name
    assert data['clusters']['cluster2']['action'] == "NoNeedToReload"
    assert data['clusters']['cluster3']['action'] == "ClusterOverLimit"
    assert data['clusters']['cluster4']['action'] == "ClusterWaitForSecondReloadPhase"


def test_dump_and_load():
    action_state_logger = ActionStateLogger()

    action_state_logger.log_action('cluster1', actions.ClusterReloadFull({}, {}, FULL_GENERATION, 600, {}),)
    action_state_logger.log_action('cluster2', actions.NoNeedToReload({}))
    action_state_logger.log_action('cluster3', actions.ClusterOverLimit({}))
    action_state_logger.log_action('cluster3', actions.ClusterWaitForSecondReloadPhase({}))

    action_state_logger.dump_state("dump1.json")

    action_state_logger2 = ActionStateLogger()
    assert action_state_logger2.load_state("dump1.json") is True

    assert action_state_logger == action_state_logger2


def test_load_unknown_file():
    action_state_logger = ActionStateLogger()
    assert action_state_logger.load_state('no_such_file') is False


def test_dump_bad_file():
    action_state_logger = ActionStateLogger()
    assert action_state_logger.dump_state("/no_such_dir/123/state.json") is False
