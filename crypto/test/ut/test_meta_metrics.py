import collections

from crypta.dmp.common.data.python import segment_status
from crypta.dmp.common.metrics import meta_metrics


def test_calc_meta_metrics():
    sizes_dict = collections.defaultdict(meta_metrics.SegmentsSizesAccum)
    sizes_dict[segment_status.ENABLED] = meta_metrics.SegmentsSizesAccum(count=8, yuid_size=70, ext_id_size=100)
    sizes_dict[segment_status.DISABLED] = meta_metrics.SegmentsSizesAccum(count=2, yuid_size=100, ext_id_size=200)
    sizes_dict[segment_status.DELETED] = meta_metrics.SegmentsSizesAccum(count=1, yuid_size=1, ext_id_size=300)

    ref_metrics = {
        'segments_count.enabled': 8,
        'segments_count.disabled': 2,
        'segments_count.deleted': 1,

        'segments_size.enabled.ext_id': 100,
        'segments_size.enabled.yuid': 70,

        'segments_size.disabled.ext_id': 200,
        'segments_size.disabled.yuid': 100,

        'segments_size.deleted.ext_id': 300,
        'segments_size.deleted.yuid': 1,

        'matching_rate.total.bindings.enabled': 70.0 / 100,
        'enabled_segments_rate': 8.0 / 10,

        'enabled_segments_size_rate.ext_id': 100.0 / 300,
        'enabled_segments_size_rate.yuid': 70.0 / 170
    }

    assert meta_metrics.calc(sizes_dict) == ref_metrics


def test_calc_meta_metrics_no_deleted():
    sizes_dict = collections.defaultdict(meta_metrics.SegmentsSizesAccum)
    sizes_dict[segment_status.ENABLED] = meta_metrics.SegmentsSizesAccum(count=8, yuid_size=70, ext_id_size=100)
    sizes_dict[segment_status.DISABLED] = meta_metrics.SegmentsSizesAccum(count=2, yuid_size=100, ext_id_size=200)

    ref_metrics = {
        'segments_count.enabled': 8,
        'segments_count.disabled': 2,
        'segments_count.deleted': 0,

        'segments_size.enabled.ext_id': 100,
        'segments_size.enabled.yuid': 70,

        'segments_size.disabled.ext_id': 200,
        'segments_size.disabled.yuid': 100,

        'segments_size.deleted.ext_id': 0,
        'segments_size.deleted.yuid': 0,

        'matching_rate.total.bindings.enabled': 70.0 / 100,
        'enabled_segments_rate': 8.0 / 10,

        'enabled_segments_size_rate.ext_id': 100.0 / 300,
        'enabled_segments_size_rate.yuid': 70.0 / 170
    }

    assert meta_metrics.calc(sizes_dict) == ref_metrics


def test_calc_meta_metrics_zero_sizes():
    sizes_dict = collections.defaultdict(meta_metrics.SegmentsSizesAccum)
    sizes_dict[segment_status.ENABLED] = meta_metrics.SegmentsSizesAccum(count=8, yuid_size=0, ext_id_size=0)
    sizes_dict[segment_status.DISABLED] = meta_metrics.SegmentsSizesAccum(count=2, yuid_size=0, ext_id_size=0)
    sizes_dict[segment_status.DELETED] = meta_metrics.SegmentsSizesAccum(count=1, yuid_size=0, ext_id_size=0)

    ref_metrics = {
        'segments_count.enabled': 8,
        'segments_count.disabled': 2,
        'segments_count.deleted': 1,

        'segments_size.enabled.ext_id': 0,
        'segments_size.enabled.yuid': 0,

        'segments_size.disabled.ext_id': 0,
        'segments_size.disabled.yuid': 0,

        'segments_size.deleted.ext_id': 0,
        'segments_size.deleted.yuid': 0,

        'matching_rate.total.bindings.enabled': 0,
        'enabled_segments_rate': 8.0 / 10,

        'enabled_segments_size_rate.ext_id': 0,
        'enabled_segments_size_rate.yuid': 0
    }

    assert meta_metrics.calc(sizes_dict) == ref_metrics
