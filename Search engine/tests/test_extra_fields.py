from nile.api.v1 import (
    Record,
    clusters,
)

from search.gta.ltv.hc1_reducer.lib.hc1_reducer import (
    GetDataset,
    get_dataset_streams,
)
from search.gta.ltv.dataset_dates import dataset_dates as dd

TABLE_DATE = '2018-01-20'


def get(hypercube_sample, categ_fields_from_recs, categ_fields_from_recs_priority='activation'):
    cluster = clusters.MockCluster()
    pools_and_dates = dd.get_pools_and_dates(
        TABLE_DATE,
        train_range=35, test_range=7, min_delay=7,
        target_ranges=((0, 14), (0, 60)),
        pool_names=('train1', 'test1', 'train2', 'test2'),
    )
    dataset_getter = GetDataset(
        TABLE_DATE,
        pools_and_dates,
        1, 1,
        accounting_mode='productonly',
        device_type='mobile',
        value_types=['requests'],
        data_type='primal',
        stl=None,
        version='',
        categ_fields_from_recs=categ_fields_from_recs,
        categ_fields_from_recs_priority=categ_fields_from_recs_priority,
    )

    job = cluster.job()
    hypercube = job.table('').debug_input(hypercube_sample)

    dataset_streams = get_dataset_streams(
        hypercube,
        dataset_getter
    )
    test2_output = []
    dataset_streams['test2'].debug_output(test2_output)
    job.debug_run()
    return test2_output


def test_1():
    out = get([
        Record(user_id='1', date='2018-01-14', log='activations', geo_id='1', activation_id=0, service=None, direct_cost=None),
        Record(user_id='1', date='2018-01-14', log='requests', browser='browser', service=None, direct_cost=None),
    ], ['browser'])
    assert out
    for r in out:
        assert r['activation_browser'] == 'browser'


def test_2():
    out = get([
        Record(user_id='1', date='2018-01-14', log='activations', geo_id='1', activation_id=0, service=None, direct_cost=None),
        Record(user_id='1', date='2018-01-14', log='requests', browser='browser', service=None, direct_cost=None),
    ], [])
    assert out
    for r in out:
        assert r.get('activation_browser') == 'Undefined'


def test_3():
    out = get([
        Record(user_id='1', date='2018-01-14', log='activations', geo_id='1', browser='browser_1', activation_id=0, service=None, direct_cost=None),
        Record(user_id='1', date='2018-01-14', log='requests', browser='browser_2', service=None, direct_cost=None),
    ], ['browser'])
    assert out
    for r in out:
        assert r['activation_browser'] == 'browser_1'


def test_4():
    out = get([
        Record(user_id='1', date='2018-01-14', log='activations', geo_id='1', browser='browser_1', activation_id=0, service=None, direct_cost=None),
        Record(user_id='1', date='2018-01-14', log='requests', browser='browser_2', service=None, direct_cost=None),
    ], ['browser'], 'recs')
    assert out
    for r in out:
        assert r['activation_browser'] == 'browser_2'

