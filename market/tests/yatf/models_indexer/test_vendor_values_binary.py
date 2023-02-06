# coding: utf-8
import pytest
import time
import itertools
from market.idx.offers.yatf.resources.offers_indexer.snapshot_pbuf_sn import Snapshot
from market.idx.yatf.resources.mbo.models_pb import ModelsPb
from market.idx.yatf.resources.mbo.parameters_pb import ParametersPb
from market.mbi.mbi.proto.bidding.MbiBids_pb2 import Bid, Parcel
from market.proto.content.mbo.MboParameters_pb2 import Category
from market.idx.yatf.test_envs.create_meta_env import CreateMetaTestEnv
from market.proto.content.mbo.ExportReportModel_pb2 import ExportReportModel
from market.idx.models.yatf.test_envs.models_indexer import ModelsIndexerTestEnv
from market.idx.offers.yatf.resources.offers_indexer.snapshot_meta import SnapshotMeta
from market.idx.offers.yatf.resources.offers_indexer.vendor_values_binary import VendorValuesBinary

MODEL_VENDOR_CATEGORIES_APPLICATION_RESULT = [
    (10, 0, 10615014, (100, True, 1)),
    (20, 0, 10615014, (0, True, 2)),
    (777, 0, 10615014, (0, False, 0)),
    (30, 0, 10615014, (0, False, 0)),
    (777, 10, 90401, (102, True, 1)),
    (30, 40, 90402, (401, True, 7)),
    (30, 40, 90829, (402, True, 7)),
    (777, 50, 90886, (201, True, 6)),
    (10, 40, 90401, (100, True, 1)),
    (0, 50, 91046, (201, True, 6)),
    (0, 30, 91046, (0, False, 0)),
    (0, 10, 90401, (102, True, 1)),
    (10, 10, 90401, (100, True, 1)),
    (0, 11, 90401, (102, True, 2)),
    (0, 11, 90402, (102, True, 2)),
    (0, 11, 90477, (103, True, 2)),
    (0, 11, 90839, (105, True, 2)),
    (0, 11, 90886, (102, True, 2)),
    (0, 50, 90839, (201, True, 6)),
    (0, 777, 90839, (0, False, 0))
]

ALL_MODELS = [
    ExportReportModel(
        id=model_id,
        category_id=category_id,
        vendor_id=vendor_id,
        current_type='GURU',
        published_on_market=True,
    )
    for model_id, vendor_id, category_id, _ in MODEL_VENDOR_CATEGORIES_APPLICATION_RESULT
]

BIDS = [
    # vendor_ds_id, domain_ids, model_search_bid_value, domain_type
    (5, [50], 500, 'MODEL_ID'),
    (3, [30], 300, 'MODEL_ID'),
    (4, [30], 400, 'MODEL_ID'),
    (1, [10], 100, 'MODEL_ID'),
    (2, [20], 0, 'MODEL_ID'),
    (1, [10], 101, 'VENDOR_ID'),
    (6, [50], 201, 'VENDOR_ID'),
    (2, [30], 301, 'VENDOR_ID'),
    (2, [30], 302, 'VENDOR_ID'),  # Дубликат
    (1, [10, 90401], 102, 'VENDOR_CATEGORY_ID'),
    (7, [40, 90402], 401, 'VENDOR_CATEGORY_ID'),
    (7, [40, 90829], 402, 'VENDOR_CATEGORY_ID'),
    (7, [50, 90886], 501, 'VENDOR_CATEGORY_ID'),
    (7, [50, 90886], 502, 'VENDOR_CATEGORY_ID'),  # Дубликат
    (2, [11, 90401], 102, 'VENDOR_CATEGORY_ID'),  # -- Для тестирования DepthCategoryApply
    (2, [11, 90477], 103, 'VENDOR_CATEGORY_ID'),
    (2, [11, 90830], 104, 'VENDOR_CATEGORY_ID'),
    (2, [11, 90839], 105, 'VENDOR_CATEGORY_ID'),
    (2, [11, 90829], 106, 'VENDOR_CATEGORY_ID'),  # эти две ставки уйдут как дубликаты, т.е. при подъеме по дереву категорий в поисках
    (2, [11, 90829], 107, 'VENDOR_CATEGORY_ID'),  # ставок не остановимся в узле hyper_categ_id = 90829
    (2, [11, 90829], 108, 'VENDOR_CATEGORY_ID'),
]


@pytest.fixture(scope="module")
def mb_snapshot_pbuf_sn():
    return [
        Parcel(
            bids=[
                Bid(
                    partner_id=t[0],
                    domain_type=t[3],
                    value_for_model_search=Bid.Value(
                        value=t[2],
                        modification_time=int(time.time())
                    ),
                    domain_ids=[str(q) for q in t[1]],
                    target='MODEL',  # Обязательно для модельного индекстора
                    partner_type='VENDOR'  # Lo mismo
                )
                for t in BIDS
            ]
        )
    ]


@pytest.yield_fixture(scope="module")
def create_meta(mb_snapshot_pbuf_sn):
    cm_resources = {
        'snapshot_pbuf_sn': Snapshot(mb_snapshot_pbuf_sn)
    }
    with CreateMetaTestEnv(modelbids=True, **cm_resources) as create_meta:
        create_meta.execute()
        create_meta.verify()
        yield create_meta


@pytest.yield_fixture(scope="module")
def models():
    category_to_models = {k: list(v) for k, v in itertools.groupby(sorted(ALL_MODELS, key=lambda t: t.category_id), key=lambda t: t.category_id)}
    return [ModelsPb(models, category_id) for category_id, models in category_to_models.items()]


@pytest.yield_fixture(scope="module")
def parameters():
    return [ParametersPb(Category(hid=m.category_id)) for m in ALL_MODELS]


@pytest.yield_fixture(scope="module")
def model_indexer(create_meta, mb_snapshot_pbuf_sn, models, parameters):
    resources = {
        'models': models,
        'parameters': parameters,
        'snapshot_meta': SnapshotMeta.load_from_file(
            create_meta.outputs['snapshot_meta'].path,
            'mb_snapshot.meta'
        ),
        'snapshot_pbuf_sn': Snapshot(mb_snapshot_pbuf_sn, filename='mb_snapshot.pbuf.sn'),
    }
    with ModelsIndexerTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


@pytest.yield_fixture(scope="module")
def vendor_values_binary(model_indexer):
    b = VendorValuesBinary(model_indexer.outputs['vendor_values_binary'].path)
    b.load()
    return [b.get_props(i) for i in range(len(MODEL_VENDOR_CATEGORIES_APPLICATION_RESULT))]


def test_vendor_values_binary(vendor_values_binary, model_indexer):
    for o in MODEL_VENDOR_CATEGORIES_APPLICATION_RESULT:
        found_pos = None
        for pos, values in model_indexer.offers.items():
            model_id = int(values['model_id'])
            vendor_id = int(values['vendor_id'])
            category_id = int(values['hidd'])
            if o[0] == model_id and o[1] == vendor_id and o[2] == category_id:
                found_pos = pos
                break
        assert found_pos is not None, 'Failed to find entry for model_id = {}, vendor_id = {}, category_id = {} in vendor_values_binary'.format(o[0], o[1], o[2])
        v = vendor_values_binary[found_pos]
        assert v['vendor_bid'] == o[3][0] and v['is_recommended_by_vendor'] == o[3][1] and v['vendor_ds_id'] == o[3][2], 'Wrong entry for item {}'.format(o)
