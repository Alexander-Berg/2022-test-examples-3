from nile.api.v1 import clusters
from search.gta.ltv.report.accumulative_report import Task
from search.gta.ltv.report.test.data import (
    mobile_predictions,
    desktop_predictions,
)
from search.gta.ltv.dataset_dates import dataset_dates as dd
from qb2.api.v1 import (
    typing as qt,
)
import yatest.common
from shutil import copy


def test_mobile():
    copy(
        yatest.common.source_path('search/gta/ltv/report/test/yasoft_key_groups.yaml'),
        'yasoft_key_groups.yaml',
    )
    copy(
        yatest.common.source_path('search/gta/ltv/report/test/mobile_ltv_acl.json'),
        'mobile_ltv_acl.json',
    )
    pools_and_dates = dd.get_pools_and_dates(
        TABLE_DATE,
        train_range=35, test_range=7, min_delay=7,
        target_ranges=((0, 60)),
        pool_names=('train1', 'test1', 'train2', 'test2'),
    )

    cluster = clusters.MockCluster()
    job = cluster.job()

    new_stream = job.table('').debug_input(mobile_predictions)
    prev_stream = job.table('').debug_input([])
    task = Task(
        ['requests'],
        'mobile',
        'test',
        0,
        '2018-01-01',
        'productonly',
        'vtest',
        'activation_date',
        output_prediction_days=(7, 14, 21, 28, 37, 56, 'max'),
        target_ranges=[(0, 60)],
        pools_and_dates=pools_and_dates,
        pool_name='test1',
    )
    output_streams = task.get_output_streams(
        new_stream,
        prev_stream,
    )
    out = []

    output_streams['publish_result'].debug_output(out)
    job.debug_run()
    assert out
    for o in out:
        assert o.project


def test_desktop():
    copy(
        yatest.common.source_path('search/gta/ltv/report/test/yasoft_key_groups.yaml'),
        'yasoft_key_groups.yaml',
    )
    copy(
        yatest.common.source_path('search/gta/ltv/report/test/mobile_ltv_acl.json'),
        'mobile_ltv_acl.json',
    )
    pools_and_dates = dd.get_pools_and_dates(
        TABLE_DATE,
        train_range=35, test_range=7, min_delay=7,
        target_ranges=((0, 60)),
        pool_names=('train1', 'test1', 'train2', 'test2'),
    )

    cluster = clusters.MockCluster()
    job = cluster.job()

    new_stream = job.table('').debug_input(desktop_predictions)
    prev_stream = job.table('').debug_input([])
    task = Task(
        ['requests'],
        'desktop',
        'test',
        0,
        '2018-01-01',
        'productonly',
        'vtest',
        'activation_date',
        output_prediction_days=(7, 14, 21, 28, 37, 56, 'max'),
        target_ranges=[(0, 60)],
        optional_fields={
            'activation_browser': qt.String,
        },
        yasoft_path_instead_of_project=True,
        pools_and_dates=pools_and_dates,
        pool_name='test1',
    )
    output_streams = task.get_output_streams(
        new_stream,
        prev_stream,
    )
    out = []

    output_streams['publish_result'].debug_output(out)
    job.debug_run()
    assert out
    for o in out:
        # assert o.raw_yasoft_path
        assert o.yasoft_path
