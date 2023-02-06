import functools

import numpy as np
import pytest
import pytest_bdd
from pytest_bdd import (
    given,
    then,
    when,
    parsers
)
import yatest.common

from crypta.audience.lib.affinity.affinity import (
    _compute_affinities,
)
from crypta.lab.lib.tables import (
    UserDataStats,
)
from crypta.lib.proto.user_data.user_data_pb2 import (
    TSegment,
)


def get_path(resource):
    path = yatest.common.source_path(resource).split("/")
    path.pop()
    result = "/".join(path)
    return result


@pytest.fixture(scope="session", autouse=True)
def context():
    class Context(dict):
        pass

    return Context()


scenario = functools.partial(
    pytest_bdd.scenario,
    features_base_dir=get_path("crypta/audience/test/feature/stats.feature")
)


@scenario('stats.feature', 'Compute affinities')
def test_compute_affinities():
    pass


@scenario('stats.feature', 'Compute affinities with empty stats')
def test_compute_affinities_with_empty():
    pass


@given(parsers.parse('segments {segments}'))
def targets(segments):
    return [
        TSegment(ID=int(id_), Keyword=int(keyword))
        for (id_, keyword) in (
            each.split('/') for each in segments.split(',')
        )
    ]


def _stats(targets, count, total):
    stats = UserDataStats.Proto()
    strata = stats.Stratum.Strata.add()
    strata.Count = total
    for target in targets:
        segment = strata.Segment.add()
        segment.Count = count
        segment.Segment.CopyFrom(target)
    return stats


def _finite(value):
    if np.isfinite(value):
        return value
    else:
        return None


@when(parsers.parse('stats {name} has {count:d} out of {total:d} for each segment'))
def regular_stats(context, targets, name, count, total):
    context[name] = _stats(targets, count, total)


@when(parsers.parse('stats {name} is empty'))
def empty_stats(context, name):
    context[name] = UserDataStats.Proto()


@when(parsers.parse('we compute affinities of {a} over {b}'))
def compute_affinities(context, targets, name, a, b):
    context['affinities'] = _compute_affinities(targets, context[a], context[b])


@then(parsers.parse('all affinities are approximately {value:f}'))
def check_affinities(context, name, value):
    assert len(context['affinities']), "There should be some affinities"
    for key, affinity in context['affinities'].items():
        assert affinity == pytest.approx(value)
