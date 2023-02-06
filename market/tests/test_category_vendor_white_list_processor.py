# coding: utf-8
import os
import tempfile

import pytest
from hamcrest import assert_that

from market.idx.datacamp.miner.yatf.resources.config import MinerConfig
from market.idx.datacamp.miner.yatf.test_env import MinerTestEnv
from market.idx.datacamp.proto.api.DatacampMessage_pb2 import DatacampMessage
from market.idx.datacamp.proto.offer.DataCampOffer_pb2 import (
    BLUE,
    WHITE,
    Explanation,
    Flag,
    MARKET_IDX,
    MarketContent,
    Offer as DatacampOffer,
    OfferContent,
    OfferIdentifiers,
    OfferMeta,
    OfferStatus,
    PartnerInfo,
    Resolution,
    UpdateMeta,
    ValidationResult,
    Verdict,
    Verdicts,
)
from market.idx.datacamp.proto.offer.UnitedOffer_pb2 import (
    UnitedOffer,
    UnitedOffersBatch,
)
from market.proto.abo.CategoryVendorFilter_pb2 import CategoryVendorRule
import market.proto.ir.UltraController_pb2 as UC

from market.idx.datacamp.yatf.matchers.matchers import HasSerializedDatacampMessages

from market.idx.datacamp.miner.yatf.resources.category_vendor_white_list_pb import CategoryVendorWhiteList
from market.idx.yatf.matchers.protobuf_matchers import IsProtobufMap

from google.protobuf.json_format import MessageToDict

RESTRICTIONS_BASE_DIR = tempfile.mkdtemp()
FIRST_PARTY_TESTING=10447296
DISABLED=True
IN_WHITE_LIST=not DISABLED
SKIPPED=not DISABLED
# Businesses
B1=1
B2=2
B3=3
B4=4
B6=6
B7=7
B_NOT_IN_RULES=9000
# Categories
TSHIRTS=1
PANTS=2
JACKETS=3
# Vendors
EMKA=1
GG=2
NOT_GG=-GG
Levis=3
Nike=4
Adidas=5


@pytest.fixture(scope='module')
def category_vendor_white_list():
    return CategoryVendorWhiteList(
        [
            CategoryVendorRule(
                BusinessId=B1,
                CategoryIds=[TSHIRTS],
            ),
            CategoryVendorRule(
                BusinessId=B2,
                CategoryIds=[TSHIRTS],
            ),
            CategoryVendorRule(
                BusinessId=B3,
                CategoryIds=[TSHIRTS],
            ),
            CategoryVendorRule(
                BusinessId=B1,
                CategoryIds=[TSHIRTS],
                VendorId=Levis
            ),
            CategoryVendorRule(
                BusinessId=B4,
                CategoryIds=[TSHIRTS],
                VendorId=Levis
            ),
            CategoryVendorRule(
                BusinessId=B2,
                VendorId=Levis
            ),
            CategoryVendorRule(
                BusinessId=B3,
                VendorId=GG
            ),
            CategoryVendorRule(
                BusinessId=B6,
                VendorId=EMKA
            ),
            CategoryVendorRule(
                BusinessId=B7,
                CategoryIds=[PANTS],
                VendorId=Nike
            ),
        ],
        preset_file_path=RESTRICTIONS_BASE_DIR
    )


@pytest.fixture(scope='module')
def miner_config(log_broker_stuff, input_topic, output_topic, offers_blog_topic, category_vendor_white_list):
    cfg = MinerConfig()
    cfg.create_datacamp_logger_initializer(log_broker_stuff, offers_blog_topic, enable_trace_log=False)

    fashion_category_restriction_filepath = os.path.join(RESTRICTIONS_BASE_DIR, category_vendor_white_list.filename)

    reader = cfg.create_lbk_reader_batch_processor(log_broker_stuff, input_topic, united=True)
    unpacker = cfg.create_proto_unpacker_processor()
    adapter_converter = cfg.create_offer_adapter_converter()
    cfg.create_miner_initializer()
    category_vendor_white_list_processor = cfg.create_category_vendor_white_list_processor(
        fashion_category_restriction_filepath=fashion_category_restriction_filepath
    )
    writer = cfg.create_lbk_writer_processor(log_broker_stuff, output_topic)

    cfg.create_link(reader, unpacker)
    cfg.create_link(unpacker, adapter_converter)
    cfg.create_link(adapter_converter, category_vendor_white_list_processor)
    cfg.create_link(category_vendor_white_list_processor, writer)

    return cfg


@pytest.yield_fixture(scope='module')
def miner(miner_config, input_topic, output_topic, category_vendor_white_list, offers_blog_topic):
    resources = {
        'miner_cfg': miner_config,
        'input_topic': input_topic,
        'output_topic': output_topic,
        'category_vendor_white_list': category_vendor_white_list,
        'offers_blog_topic': offers_blog_topic
    }
    with MinerTestEnv(**resources) as miner:
        miner.verify()
        yield miner


def make_datacamp_offer(business_id, category_id=None, vendor_id=None, rgb=BLUE, partner_info=None):
    return UnitedOffer(
        basic=DatacampOffer(
            identifiers=OfferIdentifiers(
                business_id=business_id,
                offer_id=str(business_id) + str(category_id) + str(vendor_id),
            ),
            content=OfferContent(
                market=MarketContent(
                    enriched_offer=UC.EnrichedOffer(
                        vendor_id=vendor_id,
                        category_id=category_id,
                    ),
                ),
            ),
        ),
        service={
            123: DatacampOffer(
                identifiers=OfferIdentifiers(
                    business_id=business_id,
                    offer_id=str(business_id) + str(category_id) + str(vendor_id),
                    shop_id=123,
                ),
                meta=OfferMeta(rgb=rgb),
                partner_info=partner_info,
            )
        }
    )


def add_verdict_and_convert_to_json(offer, flag):
    service = DatacampOffer(
        identifiers=offer.service[123].identifiers,
        status=OfferStatus(
            disabled=[
                Flag(
                    flag=flag,
                    meta=UpdateMeta(source=MARKET_IDX)
                )
            ]
        )
    )
    if flag:
        service.resolution.CopyFrom(Resolution(
            by_source=[
                Verdicts(
                    verdict=[Verdict(results=[ValidationResult(
                        is_banned=flag,
                        messages=[Explanation(code='49h')]
                    )])]
                )
            ]
        ))
    return {'united_offers': [{
        'offer': [
            {
                'basic': MessageToDict(
                    DatacampOffer(identifiers=offer.basic.identifiers),
                    preserving_proto_field_name=True,
                    use_integers_for_enums=True,
                ),
                'service': IsProtobufMap({
                    123: MessageToDict(service, preserving_proto_field_name=True, use_integers_for_enums=True),
                })
            }
        ]
    }]}


# Тестовые примеры взяты из пасты @thesonsa
# https://paste.yandex-team.ru/6027680/text. Хранись эта паста вечно!
def test_category_vendor_white_list_processor(miner, input_topic, output_topic):
    offers = [
        # если никто из бизнеса, картегории, вендора не упомянут в правиах, значит оффер валиден
        make_datacamp_offer(B_NOT_IN_RULES, None, NOT_GG),

        # куртки Levis может продавать только B2 [1-3]
        make_datacamp_offer(B1, JACKETS, Levis),
        make_datacamp_offer(B2, JACKETS, Levis),
        make_datacamp_offer(B6, JACKETS, Levis),

        # вендор GG - только b3 [4-7]
        make_datacamp_offer(B3, JACKETS, GG),
        make_datacamp_offer(B4, JACKETS, GG),
        make_datacamp_offer(B4, PANTS, GG),
        make_datacamp_offer(B4, JACKETS, NOT_GG),

        # куртки всех брендов кроме EMKA & GG & levis могут продавать все. [8]
        make_datacamp_offer(B_NOT_IN_RULES, JACKETS, NOT_GG),

        # куртки EMKA - только b6 [9-10]
        make_datacamp_offer(B6, JACKETS, EMKA),
        make_datacamp_offer(B7, JACKETS, EMKA),

        # штаны адидас могут продавать все [11-13]
        make_datacamp_offer(B1, PANTS, Adidas),
        make_datacamp_offer(B3, PANTS, Adidas),
        make_datacamp_offer(B7, PANTS, Adidas),

        # штаны GG может продавать только b3 [14-16]
        make_datacamp_offer(B1, PANTS, GG),
        make_datacamp_offer(B3, PANTS, GG),
        make_datacamp_offer(B7, PANTS, GG),

        # футболку EMKA не может продавать никто (b6 может торговать EMKA,  но не футболками.
        # А бизнесы b1, b2, b3 есть в футболках, но не в EMKA) [17-20]
        make_datacamp_offer(B1, TSHIRTS, EMKA),
        make_datacamp_offer(B4, TSHIRTS, EMKA),
        make_datacamp_offer(B6, TSHIRTS, EMKA),
        make_datacamp_offer(B_NOT_IN_RULES, TSHIRTS, EMKA),

        # футболку GG может продавать только b3 [21-24]
        make_datacamp_offer(B3, TSHIRTS, GG),
        make_datacamp_offer(B1, TSHIRTS, GG),
        make_datacamp_offer(B4, TSHIRTS, GG),
        make_datacamp_offer(B_NOT_IN_RULES, TSHIRTS, GG),

        # футболку Адидас могут продавать b1, b2, b3 [25-30]
        make_datacamp_offer(B1, TSHIRTS, Adidas),
        make_datacamp_offer(B2, TSHIRTS, Adidas),
        make_datacamp_offer(B3, TSHIRTS, Adidas),
        make_datacamp_offer(B4, TSHIRTS, Adidas),
        make_datacamp_offer(B6, TSHIRTS, Adidas),
        make_datacamp_offer(B_NOT_IN_RULES, TSHIRTS, Adidas),

        # b1 - любые футболки (в тч levis), кроме футболок GG & EMKA [31-33]
        make_datacamp_offer(B1, TSHIRTS, Levis),
        make_datacamp_offer(B1, TSHIRTS, GG),
        make_datacamp_offer(B1, TSHIRTS, EMKA),

        # b4 из футболок может продавать только футболки levis [34-38]
        make_datacamp_offer(B4, TSHIRTS, Levis),
        make_datacamp_offer(B4, TSHIRTS, GG),
        make_datacamp_offer(B4, TSHIRTS, EMKA),
        make_datacamp_offer(B4, TSHIRTS, GG),
        make_datacamp_offer(B4, TSHIRTS, NOT_GG),

        # При этом штанами nike может торговать только b7 [39-42]
        make_datacamp_offer(B7, PANTS, Nike),
        make_datacamp_offer(B1, PANTS, Nike),
        make_datacamp_offer(B3, PANTS, Nike),
        make_datacamp_offer(B_NOT_IN_RULES, PANTS, Nike),

        # b7 может торговать всеми товарами nike(кроме футболок) и любыми штанами (за исключением штанов levis, gg, emka):
        # тк b7 отсутствует в правиле "футболки" и для levis, gg, emka есть повендорные правила без b7 [43-48]
        make_datacamp_offer(B7, JACKETS, Nike),
        make_datacamp_offer(B7, TSHIRTS, Nike),
        make_datacamp_offer(B7, PANTS, Nike),
        make_datacamp_offer(B7, PANTS, Levis),
        make_datacamp_offer(B7, PANTS, GG),
        make_datacamp_offer(B7, PANTS, EMKA),
    ]

    request = UnitedOffersBatch()
    request.offer.extend(offers)

    input_topic.write(DatacampMessage(united_offers=[request]).SerializeToString())

    data = output_topic.read(count=1)

    # если никто из бизнеса, картегории, вендора не упомянут в правиах, значит оффер валиден
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[0], IN_WHITE_LIST)]))

    # куртки Levis може#  т продавать только B2 [1-3]
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[1], DISABLED)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[2], IN_WHITE_LIST)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[3], DISABLED)]))
    #
    #  # вендор GG - только b3 [4-7]
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[4], IN_WHITE_LIST)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[5], DISABLED)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[6], DISABLED)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[7], IN_WHITE_LIST)]))

    # куртки всех брендов кроме EMKA & GG & levis могут продавать все. [8]
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[8], IN_WHITE_LIST)]))

    # куртки EMKA - только b6 [9-10]
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[9], IN_WHITE_LIST)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[10], DISABLED)]))

    # штаны адидас могут продавать все [11-13]
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[11], IN_WHITE_LIST)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[12], IN_WHITE_LIST)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[13], IN_WHITE_LIST)]))

    # штаны GG может продавать только b3 [14-16]
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[14], DISABLED)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[15], IN_WHITE_LIST)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[16], DISABLED)]))

    # футболку EMKA не может продавать никто (b6 может торговать EMKA,  но не футболками.
    # А бизнесы b1, b2, b3 есть в футболках, но не в EMKA) [17-20]
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[17], DISABLED)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[18], DISABLED)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[19], DISABLED)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[20], DISABLED)]))

    # футболку GG может продавать только b3 [21-24]
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[21], IN_WHITE_LIST)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[22], DISABLED)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[23], DISABLED)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[24], DISABLED)]))

    # футболку Адидас могут продавать b1, b2, b3 [25-30]
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[25], IN_WHITE_LIST)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[26], IN_WHITE_LIST)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[27], IN_WHITE_LIST)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[28], DISABLED)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[29], DISABLED)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[30], DISABLED)]))

    # b1 - любые футболки (в тч levis), кроме футболок GG & EMKA [31-33]
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[31], IN_WHITE_LIST)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[32], DISABLED)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[33], DISABLED)]))

    # b4 из футболок может продавать только футболки levis [34-38]
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[34], IN_WHITE_LIST)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[35], DISABLED)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[36], DISABLED)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[37], DISABLED)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[38], DISABLED)]))

    # При этом штанами nike может торговать только b7 [39-42]
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[39], IN_WHITE_LIST)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[40], DISABLED)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[41], DISABLED)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[42], DISABLED)]))

    # b7 может торговать всеми товарами nike(кроме футболок) и любыми штанами (за исключением штанов levis, gg, emka):
    # тк b7 отсутствует в правиле "футболки" и для levis, gg, emka есть повендорные правила без b7 [43-48]
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[43], IN_WHITE_LIST)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[44], DISABLED)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[45], IN_WHITE_LIST)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[46], DISABLED)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[47], DISABLED)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offers[48], DISABLED)]))


def test_category_vendor_white_list_processor_1p(miner, input_topic, output_topic):
    offer_white = make_datacamp_offer(B_NOT_IN_RULES, PANTS, Nike, WHITE)

    offer_1p = make_datacamp_offer(FIRST_PARTY_TESTING, PANTS, Nike)

    offer_not_dsbs = make_datacamp_offer(B_NOT_IN_RULES, PANTS, Nike, BLUE, PartnerInfo(is_dsbs=False))

    request = UnitedOffersBatch()
    request.offer.extend([offer_white, offer_1p, offer_not_dsbs])

    input_topic.write(DatacampMessage(united_offers=[request]).SerializeToString())

    data = output_topic.read(count=1)

    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offer_white, SKIPPED)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offer_1p, SKIPPED)]))
    assert_that(data, HasSerializedDatacampMessages([add_verdict_and_convert_to_json(offer_not_dsbs, SKIPPED)]))
