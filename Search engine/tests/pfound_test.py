from pytest import approx

from pfound import PFoundMetric
from test_utils import create_serp, create_failed_serp, create_component, SCALE


def create_relevant_plus_component():
    return create_component('RELEVANT_PLUS')


def create_vital_component():
    return create_component('VITAL')


def test_empty():
    metric = PFoundMetric(SCALE)
    assert metric(create_serp()) == 0


def test_empty_judged():
    metric = PFoundMetric(SCALE, judged=True)
    assert metric(create_serp()) == 0


def test_error():
    metric = PFoundMetric(SCALE)
    assert metric(create_failed_serp()) == 0


def test_error_judged():
    metric = PFoundMetric(SCALE, judged=True)
    assert metric(create_failed_serp()) == 0


def test_10_rel_plus():
    metric = PFoundMetric(SCALE)
    serp = create_serp([create_relevant_plus_component()] * 10)
    assert metric(serp) == approx(0.497771)


def test_10_rel_plus_judged():
    metric = PFoundMetric(SCALE, judged=True)
    serp = create_serp([create_relevant_plus_component()] * 5 + [{}] + [create_relevant_plus_component()] * 5)
    assert metric(serp) == approx(0.497771)


def test_vital():
    metric = PFoundMetric(SCALE)
    serp = create_serp([create_vital_component()] * 2)
    assert metric(serp) == approx(0.812215)


def test_depth():
    metric = PFoundMetric(SCALE, depth=1)
    serp = create_serp([create_vital_component()] * 2)
    assert metric(serp) == 0.61


def test_invalid_judgement_no_assessment_result():
    metric = PFoundMetric(SCALE)
    serp = create_serp([{SCALE: {}}])
    assert metric(serp) == 0


def test_invalid_judgement_no_scale():
    metric = PFoundMetric(SCALE)
    serp = create_serp([{SCALE: {"assessment_result": {}}}])
    assert metric(serp) == 0
