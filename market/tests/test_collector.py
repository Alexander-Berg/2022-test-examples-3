# -*- coding: utf-8 -*-

import datetime
import pytest
import yt.wrapper as yt

# from protobuf_to_dict import protobuf_to_dict
from market.idx.promos.promo_collector.yatf.collector_env import PromoCollectorTestEnv
from market.proto.common.promo_pb2 import ESourceType
from market.proto.feedparser.Promo_pb2 import PromoDetails
from market.pylibrary.const.offer_promo import PromoType
from market.pylibrary.promo.utils import PromoDetailsHelper


PROMO_SRC = {}


def __make_pd(promo_src, promo, part_id=None):
    promo_src[promo.shop_promo_id] = promo
    res = (promo.promo_key, promo.protobuf.SerializeToString())
    if part_id is not None:
        res += (part_id,)
    return res


# задача коллектора - собрать данные из N входных таблиц
# для теста делаем 3 входные таблицы, с суффиксами {0,1,2}
# для теста их порядок и наполнение не важны
# таблица с суффиксом 2 также проверяет разделение промо на части (деление по part_id)
@pytest.fixture(scope="module")
def promo_details_info0():

    promo1 = PromoDetailsHelper(ESourceType.ROBOT, PromoType.BLUE_SET, 'promo0_id_1')
    promo1.generation_ts = int((datetime.datetime.now() - datetime.datetime(1970, 1, 1)).total_seconds())

    promo2 = PromoDetailsHelper(ESourceType.ROBOT, PromoType.BLUE_FLASH, 'promo0_id_2')
    promo2.generation_ts = int((datetime.datetime.now() - datetime.datetime(1970, 1, 1)).total_seconds())

    return (
        __make_pd(PROMO_SRC, promo1),
        __make_pd(PROMO_SRC, promo2),
    )


@pytest.fixture(scope="module")
def promo_details_info1():

    promo1 = PromoDetailsHelper(ESourceType.ROBOT, PromoType.GENERIC_BUNDLE, 'promo1_id_1')
    promo1.generation_ts = int((datetime.datetime.now() - datetime.datetime(1970, 1, 1)).total_seconds())

    promo2 = PromoDetailsHelper(ESourceType.ROBOT, PromoType.BLUE_SET, 'promo1_id_2')

    return (
        __make_pd(PROMO_SRC, promo1),
        __make_pd(PROMO_SRC, promo2),
    )


@pytest.fixture(scope="module")
def promo_details_info2():

    promo1 = PromoDetailsHelper(ESourceType.ROBOT, PromoType.BLUE_SET, 'promo2_id_1')
    promo1.generation_ts = int((datetime.datetime.now() - datetime.datetime(1970, 1, 1)).total_seconds())

    return (
        __make_pd(PROMO_SRC, promo1, 0),
        __make_pd(PROMO_SRC, promo1, 1),
        __make_pd(PROMO_SRC, promo1, 2),
        __make_pd(PROMO_SRC, promo1, 3),
    )


@pytest.fixture(scope="module")
def rejected_promos():
    return [
        dict(zip(PromoCollectorTestEnv.REJECTED_TABLE_COLUMNS, data))
        for data in (
        ('Missing generation_ts', 21, PROMO_SRC['promo1_id_2'].promo_key, 'ROBOT', 'promo1_id_2'),
    )]


@pytest.yield_fixture(scope="module")
def workflow(yt_server, promo_details_info0, promo_details_info1, promo_details_info2):
    with PromoCollectorTestEnv(
        yt_server,
        promodetails_datas_list=[promo_details_info0, promo_details_info1],
        promodetails_parted_datas_list=[promo_details_info2],
    ) as env:
        env.execute(keep_temporaries=True)
        env.verify()
        yield env


def test_collector(workflow, promo_details_info0, promo_details_info1, promo_details_info2, rejected_promos):

    def __check_source_table(all_promos, table_num, ethalon_data):
        promodetails_table_data = workflow.promodetails_table_data(table_num)
        data_len = len(ethalon_data)
        assert len(promodetails_table_data) == data_len
        for i in range(data_len):
            actual_pb = PromoDetails.FromString(yt.yson.get_bytes(promodetails_table_data[i]['promo']))
            expect_pb = PromoDetails.FromString(ethalon_data[i][1])
            assert actual_pb == expect_pb
            shop_promo_id = actual_pb.shop_promo_id
            actual_pb.url = PROMO_SRC[shop_promo_id].gen_url(False)
            actual_pb.landing_url = PROMO_SRC[shop_promo_id].gen_url(True)
            all_promos[shop_promo_id] = actual_pb
        return data_len

    all_promos = {}

    total_count = 0
    total_count += __check_source_table(all_promos, 0, promo_details_info0)
    total_count += __check_source_table(all_promos, 1, promo_details_info1)
    total_count += __check_source_table(all_promos, 2, promo_details_info2)
    total_count -= len(rejected_promos)

    result_promos_table_data = workflow.result_promos_table_data
    result_promos_table_data_len = len(result_promos_table_data)
    assert result_promos_table_data_len == total_count

    result_count = 0
    for row in result_promos_table_data:
        actual_pb = PromoDetails.FromString(yt.yson.get_bytes(row['promo']))
        assert all_promos[actual_pb.shop_promo_id] == actual_pb
        result_count += 1
    assert result_count == total_count

    result_reject_table_data = workflow.result_reject_table_data
    assert result_reject_table_data == rejected_promos
