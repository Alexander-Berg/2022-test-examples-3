# coding=utf-8
"""
Проверяем что офферы c id картинок от нового пикробота правильно проставляют picUrls
"""

import pytest
from datetime import datetime

from hamcrest import assert_that, has_entries, has_items

from yt.wrapper import ypath_join

from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampOutOffersTable,
    DataCampOutStatsTable,
    DataCampPartnersTable,
)

from market.idx.datacamp.proto.offer import DataCampOffer_pb2 as DTC
from market.idx.datacamp.yatf.utils import create_meta, dict2tskv
from market.idx.yatf.resources.shops_dat import ShopsDat

from market.idx.offers.yatf.test_envs.full_maker import Or3FullMakerTestEnv
from market.idx.offers.yatf.resources.idx_prepare_offers.config import Or3Config
from market.idx.generation.yatf.resources.prepare.feeds_yt_table import FeedsTable
from market.idx.generation.yatf.resources.prepare.sessions_yt_table import SessionsTable


BUSINESS_ID = 1000
SHOP_ID = 2000
FEED_ID = 3000

GENERATION = datetime.now().strftime('%Y%m%d_%H%M')
MI3_TYPE = 'main'


@pytest.fixture(scope='module')
def or3_config_data(yt_server):
    home_dir = yt_server.get_yt_client().config['prefix']
    config = {
        'yt': {
            'home_dir': home_dir
        },
        'datacamp': {
            'indexation_enabled': 'true',
            'partners_path': ypath_join(home_dir, 'datacamp/direct/partners'),
            'united_offers_tablepath': ypath_join(home_dir, 'datacamp/united/white_out/recent'),
            'united_stats_tablepath': ypath_join(home_dir, 'datacamp/united/white_out/stats/recent'),
        },
        'feeds': {
            'status_set': "'mock', 'publish'",
        },
        'fullmaker': {
            'fill_genlog_in_fullmaker': 'true'
        },
    }
    return config


@pytest.fixture(scope='module')
def datacamp_partners_table(yt_server, or3_config_data):
    data = [
        {
            'shop_id': SHOP_ID,
            'status': 'publish',
            'mbi': dict2tskv({
                'business_id': BUSINESS_ID,
                'datafeed_id': FEED_ID,
                'shop_id': SHOP_ID,
                'cpa': 'REAL',
                'cpc': 'NO',
                'ff_program': 'NO',
                'direct_shipping': 'true',
                'is_dsbs': 'true',
                'is_push_partner': 'true',
                'is_site_market': 'true',
                'tariff': 'CLICKS',
                'regions': '213;',
                'is_enabled': 'true',
                'warehouse_id': '124',
            }),
        },
    ]
    return DataCampPartnersTable(
        yt_server,
        or3_config_data['datacamp']['partners_path'],
        data
    )


@pytest.fixture(scope='module')
def datacamp_offers_table(yt_server, or3_config_data):
    data = [
        dict(
            business_id=BUSINESS_ID,
            shop_id=SHOP_ID,
            offer_id="direct_offer_1",
            offer=DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=BUSINESS_ID,
                    extra=DTC.OfferExtraIdentifiers(
                        classifier_good_id="81a25999cc4f1d161a6ff97582a693bd",
                        classifier_magic_id2="d90928147985beb16facf6f00a617a9c",
                        recent_business_id=BUSINESS_ID,
                        recent_feed_id=FEED_ID,
                        recent_warehouse_id=0,
                        ware_md5="x1LWMo-JhRjsqcDqMEB-aw"
                    ),
                    feed_id=FEED_ID,
                    offer_id="direct_offer_1",
                    outlet_id=0,
                    shop_id=SHOP_ID,
                    warehouse_id=0
                ),
                content=DTC.OfferContent(
                    market=DTC.MarketContent(
                        category_id=12704139,
                        market_category="Туалеты и аксессуары для кошек",
                        market_sku_published_on_blue_market=False,
                        market_sku_published_on_market=False,
                        product_name="",
                        vendor_id=12694696
                    ),
                    partner=DTC.PartnerContent(
                        actual=DTC.ProcessedSpecification(
                            description=DTC.StringValue(
                                value="Малый сплошной совок для уборки наполнителя из кошачьего туалета, пластик."
                            ),
                            name=DTC.StringValue(
                                value="Совок для кошачьего туалета Trixie сплошной малый пластик в ассортименте"
                            ),
                            title=DTC.StringValue(
                                value="Совок для кошачьего туалета Trixie сплошной малый пластик в ассортименте"
                            ),
                            url=DTC.StringValue(
                                value="https://sbermarket.ru/products/604815-sovok-dlya-koshachiego-tualeta-trixie-sploshnoy-malyy-plastik"
                            ),
                            category=DTC.PartnerCategory(
                                id=12704139,
                                path_category_ids="2\\12704139",
                                path_category_names="Все товары\\Туалеты и аксессуары для кошек",
                                parent_id=90401,
                                name="Туалеты и аксессуары для кошек",
                            ),
                        ),
                        original=DTC.OriginalSpecification(
                            description=DTC.StringValue(
                                value="Малый сплошной совок для уборки наполнителя из кошачьего туалета, пластик."
                            ),
                            name=DTC.StringValue(
                                value="Совок для кошачьего туалета Trixie сплошной малый пластик в ассортименте"
                            ),
                            url=DTC.StringValue(
                                value="https://sbermarket.ru/products/604815-sovok-dlya-koshachiego-tualeta-trixie-sploshnoy-malyy-plastik"
                            ),
                            category=DTC.PartnerCategory(
                                id=12704139,
                                path_category_ids="2\\12704139",
                                path_category_names="Все товары\\Туалеты и аксессуары для кошек",
                                parent_id=90401,
                                name="Туалеты и аксессуары для кошек",
                            ),
                        )
                    )
                ),
                partner_info=DTC.PartnerInfo(
                    supplier_id=SHOP_ID,
                    autobroker_enabled=False,
                    cpa=1,
                    cpc=4,
                    datasource_name="",
                    direct_product_mapping=False,
                    is_dsbs=False,
                    priority_regions="0",
                    shop_name=""
                ),
                pictures=DTC.OfferPictures(
                    partner=DTC.PartnerPictures(
                        original=DTC.SourcePictures(
                            source=[
                                DTC.SourcePicture(
                                    url="https://sbermarket.ru/products/604815-sovok-dlya-koshachiego-tualeta-trixie-sploshnoy-malyy-plastik/1.jpg"
                                ),
                                DTC.SourcePicture(
                                    url="pic1"
                                ),
                                DTC.SourcePicture(
                                    url="pic2"
                                ),
                                DTC.SourcePicture(
                                    url="pic3"
                                ),
                            ]
                        ),
                        actual=dict(
                            pic1=DTC.MarketPicture(
                                id='picc34df8aff68b6c16d8e059243b78fef5',
                                status=1,
                                meta=DTC.UpdateMeta(
                                    applier=10
                                ),
                            ),
                            pic2=DTC.MarketPicture(
                                id='picc34df8aff68b6c16d8e059243b78fef6',
                                status=1,
                                meta=DTC.UpdateMeta(
                                    applier=10
                                ),
                            ),
                            # не должен попадать из-за статуса
                            pic3=DTC.MarketPicture(
                                id='picc34df8aff68b6c16d8e059243b78fef7',
                                status=0,
                                meta=DTC.UpdateMeta(
                                    applier=10
                                ),
                            ),
                        )
                    )
                ),
                meta=create_meta(0, color=DTC.DIRECT, scope=2),
            ).SerializeToString(),
        ),
    ]
    return DataCampOutOffersTable(
        yt_server,
        or3_config_data['datacamp']['united_offers_tablepath'],
        data
    )


@pytest.fixture(scope='module')
def datacamp_stats_table(yt_server, or3_config_data):
    return DataCampOutStatsTable(
        yt_server,
        or3_config_data['datacamp']['united_stats_tablepath'],
        []
    )


@pytest.fixture(scope='module')
def full_maker(
        yt_server,
        or3_config_data,
        datacamp_partners_table,
        datacamp_offers_table,
        datacamp_stats_table,
):
    resources = {
        'config': Or3Config(**or3_config_data),
        'shops_dat': ShopsDat([]),
        'sessions': SessionsTable(
            yt_stuff=yt_server,
            path=ypath_join(or3_config_data['yt']['home_dir'], 'headquarters', 'sessions'),
            data=[]
        ),
        'feeds': FeedsTable(
            yt_stuff=yt_server,
            path=ypath_join(or3_config_data['yt']['home_dir'], 'headquarters', 'feeds'),
            data=[]
        ),
        'datacamp_partners_table': datacamp_partners_table,
        'united_datacamp_offers_table': datacamp_offers_table,
        'united_datacamp_stats_table': datacamp_stats_table,
    }
    with Or3FullMakerTestEnv(yt_server, GENERATION, MI3_TYPE, **resources) as fm:
        fm.verify()
        fm.execute()
        yield fm


def test_run(full_maker):
    assert_that(len(full_maker.offers_raw_corrected) == 1)
    # проверяем что поля для картинок проставились правильно
    assert_that(
        full_maker.offers_raw_corrected,
        has_items(
            has_entries({
                'offer': has_entries({
                    'URL': 'https://sbermarket.ru/products/604815-sovok-dlya-koshachiego-tualeta-trixie-sploshnoy-malyy-plastik',
                    'picURLS': 'https://sbermarket.ru/products/604815-sovok-dlya-koshachiego-tualeta-trixie-sploshnoy-malyy-plastik/1.jpg\tpic1\tpic2\tpic3',
                }),
            }),
        )
    )
    ids = set()
    for id in full_maker.offers_raw_corrected[0]['offer']['picUrlIds']:
        ids.add(id)
    assert ids == set(['picc34df8aff68b6c16d8e059243b78fef5', 'picc34df8aff68b6c16d8e059243b78fef6', ''])
