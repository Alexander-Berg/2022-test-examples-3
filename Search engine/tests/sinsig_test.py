import json
from pytest import approx

from sinsig import SinSigMetric
from judgements import JUDGEMENTS_SINSIG_SLIDER_VALUES, JUDGEMENTS_SINSIG_JUDGEMENT_VALUES
from test_utils import create_serp, create_failed_serp


def create_sinsig_component(slider_values, judgement_values):
    component = {
        JUDGEMENTS_SINSIG_SLIDER_VALUES: {'name': json.dumps(slider_values)},
        JUDGEMENTS_SINSIG_JUDGEMENT_VALUES: {'name': json.dumps(judgement_values)}
    }
    return component


def create_component_0():
    return create_sinsig_component([38.3, 29.1, 22.7], ["SLIDER_GRADE", "SLIDER_GRADE", "SLIDER_GRADE"])


def create_component_1():
    return create_sinsig_component([-1.0, -1.0, -1.0, -1.0, -1.0, -1.0], ["IRREL", "IRREL", "IRREL", "IRREL", "IRREL", "IRREL"])


def create_component_2():
    return create_sinsig_component([45.7661290323, 64.371257485, -1.0], ["SLIDER_GRADE", "SLIDER_GRADE", "RELEVANCE_MINUS_BAD"])


def test_empty():
    metric = SinSigMetric()
    assert metric(create_serp()) == 0


def test_failed():
    metric = SinSigMetric()
    assert metric(create_failed_serp()) == 0


def test_one_component():
    component_0 = create_sinsig_component([38.3, 29.1, 22.7], ["SLIDER_GRADE", "SLIDER_GRADE", "SLIDER_GRADE"])
    metric = SinSigMetric()(create_serp([component_0]))
    assert metric == approx(0.6103999999999999)


def test_three_components():
    component_0 = create_component_0()
    component_1 = create_component_1()
    component_2 = create_component_2()
    metric = SinSigMetric()(create_serp([component_0, component_1, component_2]))
    assert metric == approx(0.826138737579)


def test_depth():
    component_0 = create_component_0()
    component_1 = create_component_1()
    component_2 = create_component_2()
    metric = SinSigMetric(depth=2)(create_serp([component_0, component_1, component_2]))
    assert metric == approx(0.6103999999999999)
