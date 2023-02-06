import datetime
import time

import pytest

from yamarec_metarouter.exceptions import DispatchError


def test_agent_resolves_hosts_correctly(agent):
    assert agent.resolve("market.yandex.ru").pattern.match("market.yandex.by")
    assert agent.resolve("market.yandex.ru").pattern.match("market.yandex.kz")
    assert agent.resolve("market.yandex.ru").pattern.match("market.yandex.ru")
    assert not agent.resolve("market.yandex.ru").pattern.match("m.market.yandex.ru")


def test_agent_dispatches_correctly(agent):
    assert agent.dispatch("/market/desktop", "/product/123") == "model"
    assert agent.dispatch("/market/touch", "/model/123") == "model"
    with pytest.raises(DispatchError):
        agent.dispatch("/market/desktop", "/model/123")
    with pytest.raises(DispatchError):
        agent.dispatch("/market/touch", "/product/123")


def test_agent_synchonizes_correctly(agent):
    initial_metadispatcher_id = id(agent._metadispatcher)
    agent.synchronize(delay=datetime.timedelta(seconds=0.1))
    assert id(agent._metadispatcher) == initial_metadispatcher_id
    time.sleep(0.2)
    assert id(agent._metadispatcher) != initial_metadispatcher_id
    agent.desynchronize()
    initial_metadispatcher_id = id(agent._metadispatcher)
    time.sleep(0.2)
    assert id(agent._metadispatcher) == initial_metadispatcher_id
    agent.synchronize(delay=datetime.timedelta(seconds=0.1))
    assert id(agent._metadispatcher) == initial_metadispatcher_id
    time.sleep(0.2)
    assert id(agent._metadispatcher) != initial_metadispatcher_id
    agent.desynchronize()
