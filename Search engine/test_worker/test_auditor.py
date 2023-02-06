from __future__ import annotations

import mock
import pytest

from concurrent.futures import Future
from typing import Generator, Iterable

from search.beholder.proto.structures.graph_pb2 import GraphNameList
from search.beholder.src.worker.workers_auditor import WorkersAuditor, ConsistencyState

WORKER = WorkersAuditor()
DEFAULT_GRAPHS = ('graph-1', 'graph-2', 'graph-3')


class DummyException(Exception):
    """ Dummy exception for test purposes """


def fake_unicast(graphs: Iterable[str] | None = DEFAULT_GRAPHS, exception : Exception | None = None) -> callable:
    def _fake_unicast() -> GraphNameList:
        if exception:
            raise exception
        return GraphNameList(graphs=graphs)
    return _fake_unicast


def fake_multicast(*unicasts: callable) -> callable:
    def _fake_multicast(pool) -> Generator[Future]:
        for unicast in unicasts:
            yield pool.submit(unicast)
    return _fake_multicast


@pytest.fixture(scope='function')
def mock_workers():
    with mock.patch.object(WORKER.__class__, 'workers', new_callable=mock.PropertyMock) as workers:
        yield workers


def test_run_once_worker_states(mock_workers):
    query = 'multicast_list_running_graphs.side_effect'
    mock_workers.return_value = {
        0: mock.Mock(**{query: fake_multicast(fake_unicast(), fake_unicast())}),
        1: mock.Mock(**{query: fake_multicast(fake_unicast(exception=DummyException()), fake_unicast())}),
        2: mock.Mock(**{query: fake_multicast()}),
        3: mock.Mock(
            **{
                query: fake_multicast(fake_unicast(), fake_unicast(), fake_unicast(DEFAULT_GRAPHS + ('extra_graph',)))
            }
        ),
    }

    expected = {
        0: ConsistencyState.CONSISTENT,
        1: ConsistencyState.HAVE_MISSED,
        2: ConsistencyState.UNKNOWN,
        3: ConsistencyState.INCONSISTENT,
    }

    with mock.patch.object(WORKER, 'update_metrics') as update_metrics:
        WORKER.run_once()
        update_metrics.assert_called_with(expected)


def test_update_metrics():
    WORKER.metrics.clear()
    worker_states = {
        0: ConsistencyState.CONSISTENT,
        1: ConsistencyState.HAVE_MISSED,
        2: ConsistencyState.UNKNOWN,
        3: ConsistencyState.INCONSISTENT,
    }
    worst_state = max(worker_states.values())
    WORKER.update_metrics(worker_states)

    metrics = {
        idx: [
            WORKER.metrics[WORKER.metrics.get_metric_name(f'worker-{idx}-state-{state.name.lower()}_dttt')[1]]
            for state in ConsistencyState
        ]
        for idx in worker_states
    }
    metrics.update({
        'overall': [
            WORKER.metrics[WORKER.metrics.get_metric_name(f'workers-state-{state.name.lower()}_dttt')[1]]
            for state in ConsistencyState
        ]
    })

    expected = {
        idx: [int(worker_state is state) for state in ConsistencyState]
        for idx, worker_state in worker_states.items()
    }
    expected.update({
        'overall': [int(state is worst_state) for state in ConsistencyState]
    })

    assert metrics == expected


def test_update_metrics_overall_consistent():
    WORKER.metrics.clear()
    worker_states = {
        0: ConsistencyState.CONSISTENT,
        1: ConsistencyState.CONSISTENT,
    }
    WORKER.update_metrics(worker_states)

    metrics = [
        WORKER.metrics[WORKER.metrics.get_metric_name(f'workers-state-{state.name.lower()}_dttt')[1]]
        for state in ConsistencyState
    ]

    expected = [int(state is ConsistencyState.CONSISTENT) for state in ConsistencyState]

    assert metrics == expected


def test_update_metrics_all_metrics_defined():
    WORKER.metrics.clear()
    worker_states = {0: ConsistencyState.CONSISTENT}
    WORKER.update_metrics(worker_states)
    expected_metrics = [
        WORKER.metrics.get_metric_name(f'workers-state-{state.name.lower()}_dttt')[1]
        for state in ConsistencyState
    ] + [
        WORKER.metrics.get_metric_name(f'worker-0-state-{state.name.lower()}_dttt')[1]
        for state in ConsistencyState
    ]

    undefined_metrics = set(expected_metrics) - set(WORKER.metrics.keys())
    assert not undefined_metrics
