import json
import multiprocessing
import time

from library.python.monlib import (
    encoder,
    metric_registry,
)

from crypta.lib.python.worker_utils.multiprocessing_metrics_registry import MultiprocessingMetricRegistry


def test_registry():
    mp_metric_registry = MultiprocessingMetricRegistry()
    mp_metric_registry.start()

    rate = mp_metric_registry.rate({"sensor": "rate"})
    histogram_rate = mp_metric_registry.histogram_rate(
        {"sensor": "histogram_rate"},
        metric_registry.HistogramType.Explicit,
        buckets=[5, 15, 25],
    )

    def add_metrics():
        rate.inc()
        rate.add(5)
        histogram_rate.collect(10)
        histogram_rate.collect(20)

    process = multiprocessing.Process(target=add_metrics)
    process.start()
    process.join()

    time.sleep(3)

    return json.loads(encoder.dumps(mp_metric_registry, format='json'))


def test_aggregate_registry():
    mp_metric_registry = MultiprocessingMetricRegistry()
    mp_metric_registry.add_aggregation_label("id")
    mp_metric_registry.start()

    rate1 = mp_metric_registry.rate({"sensor": "rate", "id": "1"})
    rate2 = mp_metric_registry.rate({"sensor": "rate", "id": "2"})
    mp_metric_registry.rate({"sensor": "rate"})

    def add_metrics():
        rate1.inc()
        rate2.add(5)

    process = multiprocessing.Process(target=add_metrics)
    process.start()
    process.join()

    time.sleep(3)

    return json.loads(encoder.dumps(mp_metric_registry, format='json'))
