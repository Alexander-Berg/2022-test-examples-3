# coding=utf-8

"""
Test of checking datacamp statistics.

Тест используется для проверки сборки статистик для datacamp.
"""

import os
import pytest
import time
import yatest.common
from datetime import datetime, timedelta
from hamcrest import assert_that

import market.idx.datacamp.proto.common.Consumer_pb2 as DTC_CONSUMER
import market.idx.datacamp.proto.external.Offer_pb2 as EDTC
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.datacamp.proto.api.ExportMessage_pb2 import ExportMessage
from market.idx.datacamp.routines.lib.dumper import TABLE_NAME_FORMAT
from market.idx.datacamp.routines.yatf.matchers.env_matchers import HasStatsRecord
from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.resources.stats_info import StatsInfo
from market.idx.datacamp.routines.yatf.test_env import StatsCalcEnv
from market.idx.datacamp.yatf.utils import create_update_meta, dict2tskv, create_meta
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampOutOffersTable, DataCampPartnersTable, DataCampBasicOffersTable
from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.yatf.utils.utils import create_pb_timestamp
from market.idx.pylibrary.datacamp.conversion import offer_to_basic_row
from market.proto.common.common_pb2 import PriceExpression

from yt import wrapper as yt


assert yt

BUSINESS_ID = 1
SHOP_ID_1P = 465852
WAREHOUSE_ID = 145

MARKET_BUSINESS_ID = 2
NOT_MARKET_BUSINESS_ID = 3

NOW_TS = create_pb_timestamp()  # JSON serializer should always use UTC


def generate_past_ts(hours):
    return int(NOW_TS.seconds - hours * 60 * 60)


def convert_ts_to_counter(ts):
    return int(ts) << 30 + 1


ZERO_VERSION_COUNTER = convert_ts_to_counter(0)
ACTUAL_VERSION_COUNTER = convert_ts_to_counter(generate_past_ts(1))
OUTDATED_VERSION_COUNTER = convert_ts_to_counter(generate_past_ts(2))


class DumperLog(YtTableResource):
    def __init__(self, yt_server, path, data=None):
        super(DumperLog, self).__init__(
            yt_stuff=yt_server,
            path=path,
            attributes={'schema': [
                dict(name='business_id', type='uint64'),
                dict(name='offer_id', type='string'),
                dict(name='shop_id', type='uint64'),
                dict(name='warehouse_id', type='uint64'),
                dict(name='type', type='any'),
                dict(name='description', type='string'),
                dict(name='rgb', type='any'),
            ]},
            data=data
        )


def extra_ids(market_sku_id=111):
    return DTC.OfferExtraIdentifiers(market_sku_id=market_sku_id)


def empty_extra_ids():
    return DTC.OfferExtraIdentifiers()


def version_status(master_data_version, actual_content_version):
    return DTC.VersionStatus(
        master_data_version=DTC.VersionCounter(
            counter=master_data_version
        ) if master_data_version is not None else None,
        actual_content_version=DTC.VersionCounter(
            counter=actual_content_version
        ) if actual_content_version is not None else None
    )


def hidden_by_status(source_id, master_data_version, actual_content_version):
    return DTC.OfferStatus(
        publish=DTC.SummaryPublicationStatus.HIDDEN,
        disabled=[
            DTC.Flag(
                flag=True,
                meta=DTC.UpdateMeta(source=source_id)
            )
        ],
        version=version_status(
            master_data_version=master_data_version,
            actual_content_version=actual_content_version
        )
    )


def ok_status(master_data_version, actual_content_version):
    return DTC.OfferStatus(
        publish=DTC.SummaryPublicationStatus.AVAILABLE,
        version=version_status(
            master_data_version=master_data_version,
            actual_content_version=actual_content_version
        )
    )


def price(price_value, with_out_discout=False):
    return DTC.OfferPrice(
        basic=DTC.PriceBundle(
            binary_price=PriceExpression(
                id='RUR',
                price=price_value,
            )
        ),
        enable_auto_discounts=DTC.Flag(
            flag=with_out_discout,
        )
    )


def empty_price():
    return DTC.OfferPrice(
        basic=DTC.PriceBundle(
            binary_price=PriceExpression(
                id='RUR',
            )
        )
    )


def empty_delivery():
    return DTC.OfferDelivery(
        specific=DTC.SpecificDeliveryOptions(),
        calculator=DTC.DeliveryCalculatorOptions(),
        delivery_info=DTC.DeliveryInfo(),
    )


def delivery_with_pickup_buckets():
    return DTC.OfferDelivery(
        specific=DTC.SpecificDeliveryOptions(),
        calculator=DTC.DeliveryCalculatorOptions(pickup_bucket_ids=[1]),
        delivery_info=DTC.DeliveryInfo(),
    )


def delivery_full_house():
    return DTC.OfferDelivery(
        specific=DTC.SpecificDeliveryOptions(),
        calculator=DTC.DeliveryCalculatorOptions(pickup_bucket_ids=[7], delivery_bucket_ids=[1, 2], post_bucket_ids=[33]),
        delivery_info=DTC.DeliveryInfo(),
    )


def market_master_data(version):
    return DTC.MarketMasterData(
        manufacturer_countries=DTC.Countries(
            countries=[
                DTC.Countries.Country(geo_id=1),
                DTC.Countries.Country(geo_id=2)
            ]
        ),
        dimensions=DTC.PreciseDimensions(
            length_mkm=10000,
            width_mkm=20000,
            height_mkm=30000,
        ),
        weight_gross=DTC.PreciseWeight(
            value_mg=1000000,
        ),
        version=DTC.VersionCounterValue(
            value=DTC.VersionCounter(
                counter=version
            )
        )
    )


def empty_market_master_data(version):
    return DTC.MarketMasterData(
        version=DTC.VersionCounterValue(
            value=DTC.VersionCounter(
                counter=version
            )
        )
    )


def offer_content(mdm_version, status_content_version, has_master_data=False, has_ir_data=True):
    return DTC.OfferContent(
        market=DTC.MarketContent(
            ir_data=DTC.EnrichedOfferSubset(
                classifier_category_id=1
            ) if has_ir_data else None,
        ),
        master_data=market_master_data(mdm_version) if has_master_data else empty_market_master_data(mdm_version),
        status=DTC.ContentStatus(
            content_system_status=DTC.ContentSystemStatus(
                status_content_version=DTC.VersionCounter(
                    counter=status_content_version
                ),
            ),
        ),
    )


def stock_info_market_partner_stocks():
    return DTC.OfferStockInfo(
        market_stocks=DTC.OfferStocks(count=4),
        partner_stocks=DTC.OfferStocks(count=5),
    )


def stock_info_market_stocks():
    return DTC.OfferStockInfo(
        market_stocks=DTC.OfferStocks(count=8),
        partner_stocks=DTC.OfferStocks(),
    )


def empty_stock_info():
    return DTC.OfferStockInfo(
        market_stocks=DTC.OfferStocks(),
        partner_stocks=DTC.OfferStocks(),
    )


def promos():
    meta = create_update_meta(10)
    return DTC.OfferPromos(
        anaplan_promos=DTC.MarketPromos(
            active_promos=DTC.Promos(
                promos=[
                    DTC.Promo(id="2", meta=meta, active=True),
                    DTC.Promo(id="3", meta=meta, active=True, discount_price=PriceExpression(
                        id='RUR',
                        price=100,
                    )),
                ],
                meta=create_update_meta(1)
            ),
            all_promos=DTC.Promos(
                promos=[
                    DTC.Promo(id="1", meta=meta),
                    DTC.Promo(id="2", meta=meta, active=True),
                    DTC.Promo(id="3", meta=meta, active=True, discount_price=PriceExpression(
                        id='RUR',
                        price=100,
                    )),
                ],
                meta=create_update_meta(1)
            ),
        )
    )


def empty_pictures():
    return DTC.PartnerPictures()


def source_pictures():
    return DTC.PartnerPictures(
        original=DTC.SourcePictures(
            meta=create_update_meta(0),
            source=[
                DTC.SourcePicture(url='2000url1/'),
                DTC.SourcePicture(url='https://2000url2/'),
                DTC.SourcePicture(url='2000url3/'),
                DTC.SourcePicture(url='https://2000url4/'),
            ],
        )
    )


def with_all_actual_pictures():
    return DTC.PartnerPictures(
        original=DTC.SourcePictures(
            meta=create_update_meta(0),
            source=[
                DTC.SourcePicture(url='2000url1/'),
                DTC.SourcePicture(url='https://2000url2/'),
            ],
        ),
        actual={
            '2000url1/': DTC.MarketPicture(
                id='asdfasdf',
                status=1,
            ),
            'https://2000url2/': DTC.MarketPicture(
                id='asdfasdf',
                status=1,
            )
        }
    )


def with_part_actual_pictures():
    return DTC.PartnerPictures(
        original=DTC.SourcePictures(
            meta=create_update_meta(0),
            source=[
                DTC.SourcePicture(url='2000url1/'),
                DTC.SourcePicture(url='https://2000url2/'),
            ],
        ),
        actual={
            '2000url1/': DTC.MarketPicture(
                id='asdfasdf',
                status=1,
            )
        }
    )


OFFERS = [
    # offers_count 1
    {
        'shop_id': 11111,
        'offer_id': 'T11111',
        # offers_with_uc_matching 1
        'extra_ids': extra_ids(111),
        #  offers_hidden_by_push_partner_feed 1, offers_hidden 1
        'status': hidden_by_status(DTC.DataSource.PUSH_PARTNER_FEED, ACTUAL_VERSION_COUNTER, ACTUAL_VERSION_COUNTER),
        # offers_zero_price 1
        'price': price(0),
        'delivery': empty_delivery(),
        # offers_with_market_stocks 1, offer_with_partner_stocks 1
        'stock_info': stock_info_market_partner_stocks(),
        # версия КИ и версия хранилища совпадают - не попадает в статистику
        # версия МДМ и версия хранилища совпадают - не попадает в статистику
        'content': offer_content(ACTUAL_VERSION_COUNTER, ACTUAL_VERSION_COUNTER),
        'pictures': source_pictures(),
        'last_mining_ts': time.time() - 10 * 60
    },
    # offers_count 2
    {
        'shop_id': SHOP_ID_1P,
        'offer_id': 'T22222',
        # offers_with_uc_matching 2
        'extra_ids': extra_ids(222),
        'status': ok_status(ACTUAL_VERSION_COUNTER, ACTUAL_VERSION_COUNTER),
        # offers_with_discout 1
        'price': price(2222, with_out_discout=True),
        # offers_with_delivery_buckets 1, offers_with_pickup_buckets 1, offers_with_post_buckets 1
        'delivery': delivery_full_house(),
        'stock_info': empty_stock_info(),
        # версия КИ меньше, чем версия хранилища совпадают - попадает в статистику
        # версия МДМ меньше, чем версия хранилища совпадают - попадает в статистику
        'content': offer_content(OUTDATED_VERSION_COUNTER, OUTDATED_VERSION_COUNTER),
        'pictures': with_part_actual_pictures(),
        'last_mining_ts': time.time() - 5 * 60
    },
    # offers_count 3
    {
        'shop_id': 33333,
        'offer_id': 'T33333',
        'extra_ids': empty_extra_ids(),
        # offers_hidden_by_stock 1, offers_hidden 2
        'status': hidden_by_status(DTC.DataSource.MARKET_STOCK, OUTDATED_VERSION_COUNTER, OUTDATED_VERSION_COUNTER),
        # offers_zero_price 1
        'price': empty_price(),
        # offers_with_pickup_buckets 2
        'delivery': delivery_with_pickup_buckets(),
        # offers_with_market_stocks 2, offer_with_partner_stocks 2
        'stock_info': stock_info_market_stocks(),
        # версия КИ больше, чем версия хранилища совпадают - не попадает в статистику
        # версия МДМ больше, чем версия хранилища совпадают - не попадает в статистику
        'content': offer_content(ACTUAL_VERSION_COUNTER, ACTUAL_VERSION_COUNTER),
        'pictures': with_all_actual_pictures(),
        # отключенный магазин не учитывается в свежести майнинга
        'last_mining_ts': time.time() - 300 * 24 * 60 * 60
    },
    # offers_count 4
    {
        'shop_id': SHOP_ID_1P,
        'offer_id': 'T44444',
        # offers_with_uc_matching 3
        'extra_ids': extra_ids(444),
        #  offers_hidden_by_push_partner_feed 2, offers_hidden 3
        'status': hidden_by_status(DTC.DataSource.PUSH_PARTNER_FEED, ZERO_VERSION_COUNTER, ZERO_VERSION_COUNTER),
        # offers_zero_price 2
        'price': price(0),
        'delivery': empty_delivery(),
        # offers_with_market_stocks 1, offer_with_partner_stocks 1
        'stock_info': stock_info_market_partner_stocks(),
        # offers_with_promos 1
        'promos': promos(),
        # the status versions are zero, should not be counted in the histogram
        'content': offer_content(ACTUAL_VERSION_COUNTER, ACTUAL_VERSION_COUNTER),
        'pictures': empty_pictures(),
        'last_mining_ts': time.time() - 30 * 60
    },
    # offers_count 5
    {
        'shop_id': 55555,
        'offer_id': 'T55555',
        # offers_with_uc_matching 4
        'extra_ids': extra_ids(555),
        'status': DTC.OfferStatus(
            publish=DTC.SummaryPublicationStatus.AVAILABLE,
            # offers_count_wild_web 1
            disabled=[
                DTC.Flag(flag=False, meta=DTC.UpdateMeta(source=DTC.PUSH_PARTNER_SITE)),
            ],
            version=version_status(ACTUAL_VERSION_COUNTER, ACTUAL_VERSION_COUNTER)
        ),
        # offers_with_discout 2
        'price': price(5555, with_out_discout=True),
        # offers_with_delivery_buckets 2, offers_with_pickup_buckets 3, offers_with_post_buckets 2
        'delivery': delivery_full_house(),
        'stock_info': empty_stock_info(),
        # offers_with_promos 2
        'promos': promos(),
        # не заполнено поле ir_data, не учитываем в статистике отставания версии КИ, даже если верися отстает
        # the master data version is zero, should not be counted in the histogram
        'content': offer_content(ZERO_VERSION_COUNTER, OUTDATED_VERSION_COUNTER, has_ir_data=False, has_master_data=True),
        'pictures': empty_pictures(),
        # offers_not_mined_in_time 1
        'last_mining_ts': time.time() - (48 + 1) * 60 * 60
    },
]

ECOM_EXPORT_OFFERS = [
    # just export offer
    {
        'Data': ExportMessage(
            offer=EDTC.Offer()
        ).SerializeToString()
    },
    # offer from the topic of the wild web
    {
        'Data': ExportMessage(
            offer=EDTC.Offer(
                disable_status={
                    # offers_count_wild_web 1
                    DTC.PUSH_PARTNER_SITE: DTC.Flag(flag=True)
                }
            )
        ).SerializeToString()
    },
    # offer from the topic of the wild web
    {
        'Data': ExportMessage(
            offer=EDTC.Offer(
                disable_status={
                    # offers_count_wild_web 2
                    DTC.PUSH_PARTNER_SITE: DTC.Flag(flag=False)
                }
            )
        ).SerializeToString()
    },
    # vertical offer from the topic of the wild web
    {
        'Data': ExportMessage(
            offer=EDTC.Offer(
                disable_status={
                    # offers_count_wild_web 3
                    DTC.PUSH_PARTNER_SITE: DTC.Flag(flag=True)
                },
                service=EDTC.Service(
                    # ecom_export_vertical offers_count 1
                    vertical_approved=True
                )
            )
        ).SerializeToString()
    },
    # just an vertical offer
    {
        'Data': ExportMessage(
            offer=EDTC.Offer(
                # without DTC.PUSH_PARTNER_SITE is not a wild web
                service=EDTC.Service(
                    # ecom_export_vertical offers_count 2
                    vertical_approved=True
                )
            )
        ).SerializeToString()
    },
    # direct offer from the topic of the wild web
    {
        'Data': ExportMessage(
            offer=EDTC.Offer(
                disable_status={
                    # offers_count_wild_web 4
                    DTC.PUSH_PARTNER_SITE: DTC.Flag(flag=True)
                },
                service=EDTC.Service(
                    # ecom_export_direct offers_count 3
                    platform=DTC_CONSUMER.Platform.DIRECT
                )
            )
        ).SerializeToString()
    },
    # direct offer with pictures
    {
        'Data': ExportMessage(
            offer=EDTC.Offer(
                original_pictures=[
                    DTC.SourcePicture(
                        url='https://directpic.url/',
                        timestamp=generate_past_ts(4)
                    )
                ],
                service=EDTC.Service(
                    # ecom_export_direct offers_count 3
                    platform=DTC_CONSUMER.Platform.DIRECT
                )
            )
        ).SerializeToString()
    },
    # direct offer with picture w/o ts
    {
        'Data': ExportMessage(
            offer=EDTC.Offer(
                service=EDTC.Service(
                    # ecom_export_direct offers_count 3
                    platform=DTC_CONSUMER.Platform.DIRECT
                ),
                original_pictures=[
                    DTC.SourcePicture(
                        url='https://directpic.url/'
                    )
                ],
            )
        ).SerializeToString()
    },
]

PARTNERS = [
    {
        'shop_id': 33333,
        'status': 'disable',
        'mbi': dict2tskv({
            'shop_id': 33333,
            'business_id': BUSINESS_ID,
            'datafeed_id': 4321,
            'warehouse_id': 0,
            'is_push_partner': True,
        })
    },
    {
        'shop_id': 4444,
        'mbi': dict2tskv({
            'shop_id': 4444,
            'business_id': MARKET_BUSINESS_ID,
            'datafeed_id': 1234,
            'warehouse_id': 0,
            'is_push_partner': True,
            'is_site_market': True
        })
    },
    {
        'shop_id': 5555,
        'mbi': dict2tskv({
            'shop_id': 4444,
            'business_id': NOT_MARKET_BUSINESS_ID,
            'datafeed_id': 6789,
            'warehouse_id': 0,
            'is_push_partner': True,
            'is_site_market': False
        })
    },
]

ECOM_EXPORT_TURBO_OFFERS = [
]


BASIC_OFFERS = [
    offer_to_basic_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=MARKET_BUSINESS_ID,
                offer_id='MarketOfferWithMissedPictures',
            ),
            meta=create_meta(10),
            pictures=DTC.OfferPictures(
                partner=DTC.PartnerPictures(
                    original=DTC.SourcePictures(
                        source=[
                            # картинка скачана в маркетный неймспес через 5 часов
                            DTC.SourcePicture(
                                url='https://downloaded.marketpic.url/',
                                timestamp=generate_past_ts(12)
                            ),
                            # скачанная картинка в немаркетный неймспейс - считаем её продолбанной
                            DTC.SourcePicture(
                                url='https://downloaded.not.marketpic.url/',
                                timestamp=generate_past_ts(12)
                            ),
                            # картинка вообще не скачалась
                            DTC.SourcePicture(
                                url='https://missed.url/',
                                timestamp=generate_past_ts(12)
                            ),
                            # картинка скачалась через 2 часа но статус FAILED
                            DTC.SourcePicture(
                                url='https://failed.url/',
                                timestamp=generate_past_ts(12)
                            ),
                            # некорректный url картинки
                            DTC.SourcePicture(
                                url='заменить путь картинки',
                                timestamp=generate_past_ts(12)
                            )
                        ],
                        meta=create_update_meta(generate_past_ts(12))
                    ),
                    multi_actual={
                        'https://downloaded.marketpic.url/': DTC.NamespacePictures(
                            by_namespace={
                                'marketpic': DTC.MarketPicture(
                                    namespace='marketpic',
                                    meta=create_update_meta(generate_past_ts(7)),  # скачалась через 5 часов
                                    status=DTC.MarketPicture.AVAILABLE
                                ),
                                'turbo': DTC.MarketPicture(
                                    namespace='turbo',
                                    meta=create_update_meta(generate_past_ts(1)),  # другой неймспейс
                                    status=DTC.MarketPicture.AVAILABLE
                                )
                            }
                        ),
                        'https://downloaded.not.marketpic.url/': DTC.NamespacePictures(
                            by_namespace={
                                'turbo': DTC.MarketPicture(
                                    namespace='turbo',
                                    meta=create_update_meta(generate_past_ts(11)),  # другой неймспейс
                                    status=DTC.MarketPicture.AVAILABLE
                                )
                            }
                        ),
                        'https://failed.url/': DTC.NamespacePictures(
                            by_namespace={
                                'marketpic': DTC.MarketPicture(
                                    namespace='marketpic',
                                    meta=create_update_meta(generate_past_ts(10)),  # скачалась через 2 часа
                                    status=DTC.MarketPicture.FAILED
                                )
                            }
                        )
                    }
                )
            )
        )),
    offer_to_basic_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=NOT_MARKET_BUSINESS_ID,
                offer_id='NotMarketOffer',
            ),
            meta=create_meta(10),
            pictures=DTC.OfferPictures(
                partner=DTC.PartnerPictures(
                    original=DTC.SourcePictures(
                        source=[
                            # оффер не маркетный - игнорируем его при подсчете статистик по партнерским картинкам
                            DTC.SourcePicture(
                                url='https://missed.url.2/',
                                timestamp=20
                            ),
                            DTC.SourcePicture(
                                url='https://missed.url.1/',
                                timestamp=18
                            )
                        ],
                        meta=create_update_meta(20)
                    )
                )
            )
        )),
    offer_to_basic_row(
        DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                business_id=MARKET_BUSINESS_ID,
                offer_id='AnotherMarketOfferWithMissedPictures',
            ),
            meta=create_meta(10),
            pictures=DTC.OfferPictures(
                partner=DTC.PartnerPictures(
                    original=DTC.SourcePictures(
                        source=[
                            # картинка скачана в маркетный неймспес через 1 час
                            DTC.SourcePicture(
                                url='https://downloaded.marketpic.url/',
                                timestamp=generate_past_ts(8)
                            ),
                            # картинка не скачивается уже 2 дня
                            DTC.SourcePicture(
                                url='https://old.missed.url/',
                                timestamp=generate_past_ts(48)
                            ),
                            # картинка не скачивается 1 час
                            DTC.SourcePicture(
                                url='https://fresh.missed.url/',
                                timestamp=generate_past_ts(1)
                            ),
                        ],
                        meta=create_update_meta(generate_past_ts(1))
                    ),
                    multi_actual={
                        'https://downloaded.marketpic.url/': DTC.NamespacePictures(
                            by_namespace={
                                'marketpic': DTC.MarketPicture(
                                    namespace='marketpic',
                                    meta=create_update_meta(generate_past_ts(7)),  # скачалась через 1 час
                                    status=DTC.MarketPicture.AVAILABLE
                                )
                            }
                        )
                    }
                )
            )
        )),
]


@pytest.fixture(scope="module")
def offers_table_name():
    return (datetime.now() + timedelta(minutes=5)).strftime(TABLE_NAME_FORMAT)


@pytest.fixture(scope="module")
def yt_routines_dir():
    return "//home/market/production/indexer/datacamp/united"


@pytest.fixture(scope="module")
def yt_blue_output_dir(yt_routines_dir):
    return yt.ypath_join(yt_routines_dir, "blue_out")


@pytest.fixture(scope="module")
def yt_blue_output_offers(yt_blue_output_dir, offers_table_name):
    return yt.ypath_join(yt_blue_output_dir, offers_table_name)


@pytest.fixture(scope='module')
def blue_datacamp_offers_recent(yt_blue_output_dir):
    return yt.ypath_join(yt_blue_output_dir, 'recent')


@pytest.fixture(scope="module")
def yt_partners_tablepath(yt_routines_dir):
    return yt.ypath_join(yt_routines_dir, "partners")


@pytest.fixture(scope="module")
def yt_basic_offers_tablepath(yt_routines_dir):
    return yt.ypath_join(yt_routines_dir, "basic_offers")


@pytest.fixture(scope="module")
def yt_direct_output_dir(yt_routines_dir):
    return yt.ypath_join(yt_routines_dir, "direct_out")


@pytest.fixture(scope="module")
def yt_direct_output_offers(yt_direct_output_dir, offers_table_name):
    return yt.ypath_join(yt_direct_output_dir, offers_table_name)


@pytest.fixture(scope='module')
def direct_datacamp_offers_recent(yt_direct_output_dir):
    return yt.ypath_join(yt_direct_output_dir, 'recent')


@pytest.fixture(scope="module")
def yt_white_output_dir(yt_routines_dir):
    return yt.ypath_join(yt_routines_dir, "white_out")


@pytest.fixture(scope="module")
def yt_white_output_offers(yt_white_output_dir, offers_table_name):
    return yt.ypath_join(yt_white_output_dir, offers_table_name)


@pytest.fixture(scope='module')
def white_datacamp_offers_recent(yt_white_output_dir):
    return yt.ypath_join(yt_white_output_dir, 'recent')


@pytest.fixture(scope="module")
def yt_turbo_output_dir(yt_routines_dir):
    return yt.ypath_join(yt_routines_dir, "turbo_out")


@pytest.fixture(scope="module")
def yt_turbo_output_offers(yt_turbo_output_dir, offers_table_name):
    return yt.ypath_join(yt_turbo_output_dir, offers_table_name)


@pytest.fixture(scope='module')
def turbo_datacamp_offers_recent(yt_turbo_output_dir):
    return yt.ypath_join(yt_turbo_output_dir, 'recent')


@pytest.fixture(scope="module")
def yt_export_dir():
    return "//home/market/production/ecom/export/offers"


@pytest.fixture(scope="module")
def yt_ecom_export_table(yt_export_dir, offers_table_name):
    return yt.ypath_join(yt_export_dir, 'merged', offers_table_name)


@pytest.fixture(scope='module')
def ecom_export_offers_recent(yt_export_dir):
    return yt.ypath_join(yt_export_dir, 'merged', 'recent')


@pytest.fixture(scope="module")
def yt_export_genlog_turbo_dir():
    return '//home/market/production/indexer/turbo.gibson/offers'


@pytest.fixture(scope="module")
def yt_export_genlog_turbo_table(yt_export_genlog_turbo_dir):
    return yt.ypath_join(yt_export_genlog_turbo_dir, "recent")


def dump_table_rows(color):
    rows = [{
        'business_id': BUSINESS_ID,
        'shop_id': offer['shop_id'],
        'warehouse_id': WAREHOUSE_ID,
        'offer_id': offer['offer_id'],
        'offer': DTC.Offer(
            identifiers=DTC.OfferIdentifiers(
                shop_id=offer['shop_id'],
                offer_id=offer['offer_id'],
                warehouse_id=WAREHOUSE_ID,
                extra=offer['extra_ids'],
            ),
            status=offer['status'],
            price=offer['price'],
            delivery=offer['delivery'],
            stock_info=offer['stock_info'],
            promos=offer.get('promos'),
            content=offer.get('content'),
            pictures=DTC.OfferPictures(
                partner=offer.get('pictures'),
            ),
            tech_info=DTC.OfferTechInfo(
                last_mining=DTC.MiningTrace(meta=DTC.UpdateMeta(timestamp=create_pb_timestamp(int(offer.get('last_mining_ts', time.time())))))
            ),
            meta=create_meta(10, color)
        ).SerializeToString()
    } for offer in OFFERS]
    return rows


@pytest.fixture(scope='module')
def partners_table(yt_server, yt_partners_tablepath):
    return DataCampPartnersTable(yt_server, yt_partners_tablepath, data=PARTNERS)


@pytest.fixture(scope='module')
def basic_offers_table(yt_server, yt_basic_offers_tablepath):
    return DataCampBasicOffersTable(yt_server, yt_basic_offers_tablepath, data=BASIC_OFFERS)


@pytest.fixture(scope='module')
def blue_datacamp_offers(yt_server, yt_blue_output_offers, blue_datacamp_offers_recent):
    return DataCampOutOffersTable(yt_server, yt_blue_output_offers, data=dump_table_rows(DTC.BLUE), link_paths=[blue_datacamp_offers_recent])


@pytest.fixture(scope='module')
def direct_datacamp_offers(yt_server, yt_direct_output_offers, direct_datacamp_offers_recent):
    return DataCampOutOffersTable(yt_server, yt_direct_output_offers, data=dump_table_rows(DTC.DIRECT_GOODS_ADS), link_paths=[direct_datacamp_offers_recent])


@pytest.fixture(scope='module')
def white_datacamp_offers(yt_server, yt_white_output_offers, white_datacamp_offers_recent):
    return DataCampOutOffersTable(yt_server, yt_white_output_offers, data=dump_table_rows(DTC.WHITE), link_paths=[white_datacamp_offers_recent])


@pytest.fixture(scope='module')
def turbo_datacamp_offers(yt_server, yt_turbo_output_offers, turbo_datacamp_offers_recent):
    return DataCampOutOffersTable(yt_server, yt_turbo_output_offers, data=dump_table_rows(DTC.TURBO), link_paths=[turbo_datacamp_offers_recent])


@pytest.fixture(scope='module')
def ecom_export_offers(yt_server, yt_ecom_export_table, ecom_export_offers_recent):
    attrs = {
        'schema': [
            dict(name='BusinessId', type='uint32', sort_order='ascending'),
            dict(name='ShopId', type='uint32', sort_order='ascending'),
            dict(name='OfferYabsId', type='uint64', sort_order='ascending'),
            dict(name='WarehouseId', type='uint32', sort_order='ascending'),
            dict(name='Service', type='string'),
            dict(name='OfferId', type='string'),
            dict(name='MarketFeedId', type='uint32'),
            dict(name='Data', type='string'),
            dict(name='DirectFeedId', type='uint64'),
            dict(name='DirectFilterId', type='any'),
        ],
        'dynamic': False,
    }
    return YtTableResource(yt_server, yt_ecom_export_table, data=ECOM_EXPORT_OFFERS, attributes=attrs, link_paths=[ecom_export_offers_recent])


@pytest.fixture(scope="module")
def export_genlog_turbo_offers(yt_server, yt_export_genlog_turbo_table):
    attrs = {
        'schema': [
            dict(name='is_blue_offer', type='boolean'),
            dict(name='is_direct', type='boolean'),
            dict(name='is_eda', type='boolean'),
            dict(name='is_lavka', type='boolean')
        ],
        'dynamic': False
    }
    return YtTableResource(yt_server, yt_export_genlog_turbo_table, data=ECOM_EXPORT_TURBO_OFFERS, attributes=attrs)


@pytest.fixture(scope='module')
def stats_accumuluted_result_data():
    return os.path.join(yatest.common.output_path(), "camp_stats.log")


@pytest.fixture(scope='module')
def stats_accumuluted_result_data_1p():
    return os.path.join(yatest.common.output_path(), "camp_stats_1p.log")


@pytest.fixture(scope='module')
def config(
    yt_server,
    yt_export_dir,
    yt_routines_dir,
    yt_blue_output_dir,
    yt_white_output_dir,
    yt_turbo_output_dir,
    yt_direct_output_dir,
    yt_export_genlog_turbo_dir,
    stats_accumuluted_result_data,
    stats_accumuluted_result_data_1p,
):
    config = RoutinesConfigMock(
        yt_server,
        config={
            "general": {
                "yt_home": "//home/market/production/indexer/datacamp/united"
            },
            "yt": {
                "blue_out": yt_blue_output_dir,
                "white_out": yt_white_output_dir,
                "direct_out": yt_direct_output_dir,
                "turbo_out": yt_turbo_output_dir,
                "routines_dir": yt_routines_dir,
                "export_dir": yt_export_dir,
                "turbo_export_genlog": yt_export_genlog_turbo_dir,
                "partners_path": "partners",
                "basic_offers_tablepath": "basic_offers",
                "dumper_log": "dumper_log",
            },
            "calc_stats": {
                "enable": True,
                "enable_1p": True,
                "yt_proxies": [yt_server.get_yt_client().config["proxy"]["url"]],
                "output": stats_accumuluted_result_data,
                "output_1p": stats_accumuluted_result_data_1p,
                "consts_resource_name": "/consts_testing.json",
            },
            "routines": {
                "shop_id_1p": str(SHOP_ID_1P),
                "mining_time": 48 * 60 * 60,
                "blue_mining_time": 48 * 60 * 60
            }
        })
    return config


@pytest.fixture(scope='module')
def dumper_log(yt_server, yt_routines_dir):
    return DumperLog(yt_server, yt.ypath_join(yt_routines_dir, "dumper_log"), data=[{
        'business_id': 1,
        'offer_id': "offer_id",
        'shop_id': 1,
        'warehouse_id': 0,
        'type': "NO_ACTUAL_SERVICE_PART",
        'description': "",
        'rgb': "BLUE",
    }])


@pytest.yield_fixture(scope='module')
def calc_stats(
    yt_server,
    blue_datacamp_offers,
    white_datacamp_offers,
    direct_datacamp_offers,
    turbo_datacamp_offers,
    ecom_export_offers,
    export_genlog_turbo_offers,
    config,
    partners_table,
    basic_offers_table
):
    resources = {
        "partners_table": partners_table,
        "basic_offers_table": basic_offers_table,
        "input_blue": blue_datacamp_offers,
        "input_white": white_datacamp_offers,
        "input_direct": direct_datacamp_offers,
        "input_turbo": turbo_datacamp_offers,
        "input_ecom_export": ecom_export_offers,
        "input_export_genlog_turbo": export_genlog_turbo_offers,
        "config": config
    }

    with StatsCalcEnv(yt_server, **resources) as env:
        yield env


def test_accumulated_stats(calc_stats, stats_accumuluted_result_data):
    assert_that(
        StatsInfo(stats_accumuluted_result_data).load(),
        HasStatsRecord(
            {
                "white": {
                    "lagging_mbo_histo": {
                        "[1209600, inf)": 1
                    },
                },
                "blue_in_united": {
                    'offers_count': 5,
                    # price
                    'offers_zero_price': 2,
                    'offers_no_price': 1,
                    'offers_with_discout': 2,
                    # dimensions
                    'offers_with_dimensions': 1,
                    # stocks
                    'offers_with_market_stocks': 3,
                    'offers_with_partner_stocks': 2,
                    # delivery
                    'offers_with_pickup_buckets': 3,
                    'offers_with_delivery_buckets': 2,
                    'offers_with_post_buckets': 2,
                    # uc
                    'offers_with_uc_matching': 4,
                    # status
                    'offers_hidden': 3,
                    'offers_hidden_by_push_partner_feed': 2,
                    'offers_hidden_by_stock': 1,
                    # promos
                    'offers_with_promos': 2,
                    'offers_with_promos_active': 2,
                    'offers_with_promos_discount': 2,
                    'offers_with_promos_potencial': 2,
                    # master_data
                    'offers_with_master_data': 1,
                    'lagging_mdm_histo': {
                        '[1209600, inf)': 1
                    },
                    'lagging_mbo_histo': {
                        '[1209600, inf)': 1
                    },
                    # pictures
                    'offers_count_with_pics': 3,
                    'offers_count_with_all_ava_pics': 1,
                    # miner freshness
                    'offers_not_mined_in_time': 1,
                    'offers_to_mine': 4,
                    'miner_freshness_histo': {
                        '[300, 1800)': 2,
                        '[1800, 3600)': 1,
                        '[172800, 345600)': 1
                    }
                },
                "direct": {
                    'offers_count': 5,
                    'offers_count_with_pics': 3,
                    'offers_count_with_all_ava_pics': 1,
                },
                "ecom_export": {
                    'offers_count': 8,
                },
                "ecom_export_direct": {
                    'offers_count': 3,
                    'picture_processing': {
                        'all-rolling_1d-not_ready': {
                            '[10800, 21600)': 1
                        },
                        'all-rolling_1w-not_ready': {
                            '[10800, 21600)': 1
                        },
                        'all-start_date-not_ready': {
                            '[10800, 21600)': 1
                        },
                        'first-rolling_1d-not_ready': {
                            '[10800, 21600)': 1
                        },
                        'first-rolling_1w-not_ready': {
                            '[10800, 21600)': 1
                        },
                        'first-start_date-not_ready': {
                            '[10800, 21600)': 1
                        }
                    },
                    "offers_picture_processing_no_timestamp": {
                        'all-rolling_1d-not_ready': 1,
                        'all-rolling_1w-not_ready': 1,
                        'all-start_date-not_ready': 1,
                        'first-rolling_1d-not_ready': 1,
                        'first-rolling_1w-not_ready': 1,
                        'first-start_date-not_ready': 1,
                    }
                },
                "ecom_export_vertical_approved": {
                    'offers_count': 2,
                },
                "ecom_export_direct_wild_web": {
                    'offers_count': 1,
                },
                "ecom_export_vertical_approved_wild_web": {
                    'offers_count': 1,
                },
                "inner_state_basic": {
                    'all_partner_pictures': 7,
                    'downloaded_fail_partner_pictures': 1,
                    'invalid_url_partner_pictures': 1,
                }
            }
        )
    )


def test_accumulated_stats_1p(calc_stats, stats_accumuluted_result_data_1p):
    assert_that(
        StatsInfo(stats_accumuluted_result_data_1p).load(),
        HasStatsRecord(
            {
                "1P_in_united": {
                    'offers_count': 2,
                    # price
                    'offers_zero_price': 1,
                    'offers_with_discout': 1,
                    # dimensions
                    'offers_with_dimensions': 0,
                    # stocks
                    'offers_with_market_stocks': 1,
                    'offers_with_partner_stocks': 1,
                    # delivery
                    'offers_with_pickup_buckets': 1,
                    'offers_with_delivery_buckets': 1,
                    'offers_with_post_buckets': 1,
                    # uc
                    'offers_with_uc_matching': 2,
                    # status
                    'offers_hidden': 1,
                    'offers_hidden_by_push_partner_feed': 1,
                    # promos
                    'offers_with_promos': 1,
                    'offers_with_promos_active': 1,
                    'offers_with_promos_discount': 1,
                    'offers_with_promos_potencial': 1,
                    # pictures
                    'offers_count_with_pics': 1,
                    # miner freshness
                    'miner_freshness_histo': {
                        '[300, 1800)': 1,
                        '[1800, 3600)': 1
                    }
                }
            }
        )
    )


def test_accumulated_stats_1p_types(calc_stats, stats_accumuluted_result_data_1p):
    assert_that(set(StatsInfo(stats_accumuluted_result_data_1p).load().keys()) == {"1P_in_united"})
