#!/usr/bin/env python
# coding: utf-8
import pytest

from market.idx.pylibrary.offer_flags.flags import OfferFlags
from market.idx.offers.yatf.utils.fixtures import default_genlog, default_blue_genlog, default_shops_dat
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.pylibrary.offer_flags.flags import DisabledFlags

from hamcrest import assert_that, is_not
from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecord
from market.idx.yatf.resources.shops_dat import ShopsDat
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


NO_CIS_FEED = 100
SBX_CIS_FEED = 200
REAL_CIS_FEED = 300


def make_shop_id(feed_id):
    return feed_id * 10


@pytest.fixture(scope="module")
def genlog_rows():
    blue_flags = OfferFlags.DEPOT
    blue_flags|= OfferFlags.AVAILABLE
    blue_flags |= OfferFlags.STORE
    blue_flags |= OfferFlags.IS_FULFILLMENT

    flags = OfferFlags.DEPOT
    flags|= OfferFlags.AVAILABLE
    flags |= OfferFlags.STORE

    offer = default_genlog()
    offer['classifier_magic_id'] = '73ba6bb98d2aec3f32056a63fb1b9a04'
    offer['flags'] = flags

    rejected_offer = default_genlog()
    rejected_offer['classifier_magic_id'] = '73ba6bb98d2aec3f32056a63fb1b9a05'
    rejected_offer['rejected'] = True
    rejected_offer['flags'] = flags

    rejected_by_ware_md5_dup_offer = default_genlog()
    rejected_by_ware_md5_dup_offer['classifier_magic_id'] = '73ba6bb98d2aec3f32056a63fb1b9a06'
    rejected_by_ware_md5_dup_offer['rejected_by_duplicated_ware_md5'] = True
    rejected_by_ware_md5_dup_offer['flags'] = flags

    disabled_by_market_idx_offer = default_blue_genlog(
        offer_id='disabled_by_market_idx_offer',
        disabled_flags=DisabledFlags.MARKET_IDX,
        weight=1.0,
        length=1.0,
        width=1.0,
        height=1.0,
        flags=blue_flags
    )

    disabled_by_market_stock_offer = default_blue_genlog(
        offer_id='disabled_by_market_stock_offer',
        disabled_flags=DisabledFlags.MARKET_STOCK,
        weight=1.0,
        length=1.0,
        width=1.0,
        height=1.0,
        flags=blue_flags
    )

    rejected_by_no_cis_cargo_980_offer = default_blue_genlog(
        offer_id='rejected_by_no_cis_cargo_980_offer',
        weight=1.0,
        length=1.0,
        width=1.0,
        height=1.0,
        feed_id=NO_CIS_FEED,
        shop_id=make_shop_id(NO_CIS_FEED),
        cargo_types=[980],
        flags=blue_flags
    )

    rejected_by_no_cis_cargo_985_offer = default_blue_genlog(
        offer_id='rejected_by_no_cis_cargo_985_offer',
        weight=1.0,
        length=1.0,
        width=1.0,
        height=1.0,
        feed_id=NO_CIS_FEED,
        shop_id=make_shop_id(NO_CIS_FEED),
        cargo_types=[985],
        flags=blue_flags
    )

    not_rejected_by_no_cis_offer = default_blue_genlog(
        offer_id='not_rejected_by_no_cis_offer',
        weight=1.0,
        length=1.0,
        width=1.0,
        height=1.0,
        feed_id=NO_CIS_FEED,
        shop_id=make_shop_id(NO_CIS_FEED),
        cargo_types=[990],
        flags=blue_flags
    )

    not_rejected_by_sbx_cis_offer = default_blue_genlog(
        offer_id='not_rejected_by_sbx_cis_offer',
        weight=1.0,
        length=1.0,
        width=1.0,
        height=1.0,
        feed_id=SBX_CIS_FEED,
        shop_id=make_shop_id(SBX_CIS_FEED),
        cargo_types=[985],
        flags=blue_flags
    )

    not_rejected_by_real_cis_offer = default_blue_genlog(
        offer_id='not_rejected_by_real_cis_offer',
        weight=1.0,
        length=1.0,
        width=1.0,
        height=1.0,
        feed_id=REAL_CIS_FEED,
        shop_id=make_shop_id(REAL_CIS_FEED),
        cargo_types=[985],
        flags=blue_flags
    )

    not_rejected_by_unknown_cis_offer = default_blue_genlog(
        offer_id='not_rejected_by_unknown_cis_offer',
        weight=1.0,
        length=1.0,
        width=1.0,
        height=1.0,
        cargo_types=[985],
        flags=blue_flags
    )

    return [
        offer,
        rejected_offer,
        disabled_by_market_idx_offer,
        disabled_by_market_stock_offer,
        rejected_by_no_cis_cargo_980_offer,
        rejected_by_no_cis_cargo_985_offer,
        not_rejected_by_unknown_cis_offer,
        not_rejected_by_no_cis_offer,
        not_rejected_by_sbx_cis_offer,
        not_rejected_by_real_cis_offer,
        rejected_by_ware_md5_dup_offer,
    ]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.fixture(scope="module")
def shops_dat():
    default = default_shops_dat()

    no_cis_shop = default_shops_dat()
    no_cis_shop['datafeed_id'] = NO_CIS_FEED
    no_cis_shop['shop_id'] = make_shop_id(NO_CIS_FEED)
    no_cis_shop['cis'] = 'NO'

    sbx_cis_shop = default_shops_dat()
    sbx_cis_shop['datafeed_id'] = SBX_CIS_FEED
    sbx_cis_shop['shop_id'] = make_shop_id(SBX_CIS_FEED)
    sbx_cis_shop['cis'] = 'SBX'

    real_cis_shop = default_shops_dat()
    real_cis_shop['datafeed_id'] = REAL_CIS_FEED
    real_cis_shop['shop_id'] = make_shop_id(REAL_CIS_FEED)
    real_cis_shop['cis'] = 'REAL'

    return ShopsDat(shops=[default, no_cis_shop, sbx_cis_shop, real_cis_shop])


@pytest.yield_fixture(scope="module")
def workflow(yt_server, genlog_table, shops_dat):
    input_table_paths = [genlog_table.get_path()]

    resources = {
        'shops_utf8_dat': shops_dat,
    }

    with OffersProcessorTestEnv(
            yt_server,
            drop_offers_with_no_sizes=True,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
            **resources
    ) as env:
        env.execute()
        env.verify()
        yield env


def test_not_rejected(genlog_rows, workflow):
    assert_that(
        workflow,
        HasGenlogRecord(
            {
                'classifier_magic_id': genlog_rows[0]['classifier_magic_id']
            }
        ),
        u'Офферы без фалага Rejected должны обрабатываться'
    )


def test_rejected(genlog_rows, workflow):
    assert_that(
        workflow,
        is_not(HasGenlogRecord(
            {
                'classifier_magic_id': genlog_rows[1]['classifier_magic_id']
            }
        )),
        u'Офферы с флагом Rejected должны отбрасываться'
    )


def test_declined_by_market_idx(genlog_rows, workflow):
    assert_that(
        workflow,
        is_not(HasGenlogRecord(
            {
                'offer_id': genlog_rows[2]['offer_id']
            }
        )),
        u'Офферы с флагом скрытия MARKET_IDX должны отбрасываться'
    )


def test_not_declined_by_market_stock(genlog_rows, workflow):
    assert_that(
        workflow,
        HasGenlogRecord(
            {
                'offer_id': genlog_rows[3]['offer_id']
            }
        ),
        u'Офферы с остальными флагами скрытия не должны отбрасываться'
    )


@pytest.mark.parametrize(
    'offer_id',
    [
        'rejected_by_no_cis_cargo_985_offer',
        'rejected_by_no_cis_cargo_980_offer',
    ]
)
def test_rejected_by_cis_offer(workflow, offer_id):
    """
    В индекс не попадают офферы магазина с cis == NO, у которых есть cargo_type 985 или 980
    """
    assert_that(
        workflow,
        is_not(HasGenlogRecord(
            {
                'offer_id': offer_id
            }
        )),
    )


@pytest.mark.parametrize(
    'offer_id, no_cis',
    [
        ('not_rejected_by_no_cis_offer', False),
        ('not_rejected_by_sbx_cis_offer', True),
        ('not_rejected_by_real_cis_offer', False),
        ('not_rejected_by_unknown_cis_offer', False),
    ]
)
def test_not_rejected_by_cis_offer(workflow, offer_id, no_cis):
    """
    В индекс попадают офферы магазина:
      - с cis == NO, если нет cargo_type 985 или 980
      - с cis == REAL, даже если есть cargo_type 985 или 980
      - с cis == SBX, для которых, если есть cargo_type 985 или 980, то проставляется флаг NO_CIS.
    """
    expected_flags = OfferFlags.DEPOT
    expected_flags |= OfferFlags.AVAILABLE
    expected_flags |= OfferFlags.STORE
    expected_flags |= OfferFlags.MODEL_COLOR_WHITE
    expected_flags |= OfferFlags.IS_FULFILLMENT
    expected_flags |= OfferFlags.CPC
    expected_flags |= OfferFlags.BLUE_OFFER
    if no_cis:
        expected_flags |= OfferFlags.NO_CIS

    assert_that(
        workflow,
        HasGenlogRecord(
            {
                'offer_id': offer_id,
                'flags': expected_flags,
            }
        ),
    )


def test_rejected_by_duplicated_ware_md5(genlog_rows, workflow):
    assert_that(
        workflow,
        is_not(HasGenlogRecord(
            {
                'classifier_magic_id': genlog_rows[10]['classifier_magic_id']
            }
        )),
        u'Офферы с флагом rejected_by_duplicated_ware_md5 должны отбрасываться'
    )
