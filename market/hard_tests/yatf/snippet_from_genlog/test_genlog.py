# coding=utf-8

from hamcrest import (
    assert_that,
    equal_to,
)
import hashlib
import pytest

from market.idx.pylibrary.offer_flags.flags import (
    DisabledFlags,
)
from market.idx.offers.yatf.utils.fixtures import (
    default_genlog,
    generate_binary_price_dict,
    genererate_default_pictures,
)
from market.idx.yatf.resources.mbo.global_vendors_xml import GlobalVendorsXml
from market.idx.offers.yatf.resources.offers_indexer.vendor_bids import VendorBids
from market.idx.offers.yatf.test_envs.offers_processor import (
    OffersProcessorTestEnv,
)
from market.idx.generation.yatf.test_envs.snippet_diff_builder import (
    SnippetDiffBuilderTestEnv,
)
from market.proto.common.common_pb2 import (
    PriceExpression,
    ApiData,
    EConversionType,
)
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join
import yt.wrapper as yt


# subscriptions do not go into genlog per MARKETINDEXER-12055
MARKET_SUBSCRIPTIONS_CATEG_ID = 15723259


@pytest.fixture(scope="module")
def genlog_rows():
    offers = [
        default_genlog(
            offer_id='0',
            category_id=MARKET_SUBSCRIPTIONS_CATEG_ID
        ),
        default_genlog(
            offer_id='1',
            market_sku=123456789012
        ),

        # test_skip_link_to_fake_vendor
        # offer without vendor
        default_genlog(
            offer_id='10'
        ),
        # offer with some vendor
        default_genlog(
            offer_id='11',
            vendor_id=1,
        ),
        # offer with fake vendor
        default_genlog(
            offer_id='12',
            vendor_id=2,
        ),
        default_genlog(
            offer_id='13',
            api_data=ApiData(
                binary_price=PriceExpression(price=10),
                binary_oldprice=PriceExpression(price=100),
                price_deleted=False,
                offer_deleted=False,
                vat=1
            ).SerializeToString()
        ),
        # is_recommended_by_vendor = true
        default_genlog(
            offer_id='20',
            vendor_id=3,
            category_id=14369216,
            shop_id=100,
        ),
        # is_recommended_by_vendor = false
        default_genlog(
            offer_id='21',
            vendor_id=4,
            category_id=14369216,
            shop_id=200,
        ),
        # is_recommended_by_vendor = undefined = false
        default_genlog(
            offer_id='22',
            vendor_id=5,
            category_id=14369216,
            shop_id=300,
        ),
        # offer with pickup options
        default_genlog(
            offer_id='30',
            pickup_options=[
                {
                    'Cost': 100.0,
                    'DaysMin': yt.yson.YsonUint64(1),
                    'DaysMax': yt.yson.YsonUint64(4),
                    'OrderBeforeHour': yt.yson.YsonUint64(17)
                },
            ],
        ),
        # offer with vendor_code
        default_genlog(
            offer_id='31',
            vendor_code='Vendor-Code-1',
        ),
        # red offer with vendor string
        default_genlog(
            offer_id='32',
            feed_group_id_hash='1234',
            pictures=genererate_default_pictures(),
            vendor='test_vendor_32',
        ),
        # non-red offer with vendor string
        default_genlog(
            offer_id='33',
            vendor='test_vendor_33',
        ),
        # offer with stock store count
        default_genlog(
            offer_id='34',
            stock_store_count=yt.yson.YsonUint64(42),
        ),
        # offer which is disabled by stock
        default_genlog(
            offer_id='35',
            disabled_flags=DisabledFlags.MARKET_STOCK,
        ),
        # offer with conversion
        default_genlog(
            offer_id='36',
            offer_conversion=[
                {
                    'conversion_value': 0.2,
                    'conversion_type': EConversionType.Value('SPB')
                },
                {
                    'conversion_value': 0.3,
                    'conversion_type': EConversionType.Value('MSK')
                },
            ],
        ),
        # offer with enabled auto discounts
        default_genlog(
            offer_id='37',
            enable_auto_discounts=True,
        ),
        # offer with from webmaster flag
        default_genlog(
            offer_id='38',
            from_webmaster=True,
        ),
        # check picture
        default_genlog(
            offer_id='39',
            picture_flags='3|4',
            picture_urls=[
                'beremvsetut.ru/upload/iblock/610/6103040979a8909055f87fb8ccb244dd.png',
                'market.yandex.ru?backdoor=true&exec=transfer_money'
            ],
            pic=[{
                'md5': hashlib.md5('the picture').digest(),
                'dups_id': 3,
                'width': 500,
                'height': 500,
                'thumb_mask': 292591,
                'group_id': 5,
                'signatures': [
                    {
                        'version': 0,
                        'similar': "aa",
                        'clothes': "bb",
                        'clothes_bin': 0.0
                    },
                    {
                        'version': 3,
                        'similar': "cc",
                        'clothes': "cc",
                        'clothes_bin': 3.0
                    },
                ],
            }]
        ),
        # offer with dynamic pricing options
        default_genlog(
            offer_id='40',
            dynamic_pricing_type=1,
            dynamic_pricing_threshold_is_percent=True,
            dynamic_pricing_threshold_value=443,
        ),
    ]
    return offers


@pytest.fixture(scope="module")
def blue_genlog_rows():
    offers = [
        # blue 1P offer
        default_genlog(
            offer_id='20',
            ware_md5='000000000000000000020w',
            is_blue_offer=True,
            supplier_type=1,
            warehouse_id=222
        ),
        # blue 3P offer
        default_genlog(
            offer_id='21',
            ware_md5='000000000000000000021w',
            is_blue_offer=True,
            supplier_type=3,
            warehouse_id=111
        ),
        # blue MSKU=100500 first offer
        default_genlog(
            offer_id='22',
            ware_md5='000000000000000000022w',
            is_blue_offer=True,
            supplier_type=1,
            market_sku=100500,
            binary_price=generate_binary_price_dict(200),
            ),
        # blue MSKU=100500 second offer minimal price for buybox
        default_genlog(
            offer_id='23',
            ware_md5='000000000000000000023w',
            is_blue_offer=True,
            supplier_type=1,
            market_sku=100500,
            binary_price=generate_binary_price_dict(100),
        ),
        # blue MSKU=100500 third offer
        default_genlog(
            offer_id='24',
            ware_md5='000000000000000000024w',
            is_blue_offer=True,
            supplier_type=1,
            market_sku=100500,
            binary_price=generate_binary_price_dict(150),
        ),
        # blue MSKU=100000 single offer
        default_genlog(
            offer_id='25',
            ware_md5='000000000000000000025w',
            is_blue_offer=True,
            supplier_type=1,
            market_sku=100000,
            binary_price=generate_binary_price_dict(50),
            cargo_types=[1, 2, 3]
        ),
        # blue 3P offer which is disabled by stock
        default_genlog(
            offer_id='26',
            ware_md5='000000000000000000026w',
            is_blue_offer=True,
            disabled_flags=DisabledFlags.MARKET_STOCK
        ),
    ]
    return offers


@pytest.fixture(scope="module")
def global_vendors():
    return '''
        <global-vendors>
          <vendor id="1" name="name1">
            <site>site</site>
            <picture>picture</picture>
          </vendor>
          <vendor id="2" name="yandex">
            <is-fake-vendor>true</is-fake-vendor>
          </vendor>
        </global-vendors>
    '''


@pytest.fixture(scope="module")
def vendor_bids():
    vendor_bids = VendorBids()
    # vendor_id, vendor_ds_id, category_id, shop_id, vbid, is_recommended
    vendor_bids.add(3, 30, 14369216, 100, 1234, True)
    vendor_bids.add(4, 40, 14369216, 200, 5678, False)
    return vendor_bids


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.fixture(scope="module")
def blue_genlog_table(yt_server, blue_genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0001'), blue_genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope="module")
def workflow(yt_server, genlog_table, global_vendors, vendor_bids):
    input_table_paths = [genlog_table.get_path()]
    resources = {
        'global_vendors_xml': GlobalVendorsXml.from_str(global_vendors),
        'vendor_bids_csv': vendor_bids,
    }

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
            **resources
    ) as env:
        env.execute()
        yield env


@pytest.yield_fixture(scope="module")
def blue_workflow(yt_server, blue_genlog_table, global_vendors):
    input_table_paths = [genlog_table.get_path()]
    resources = {
        'global_vendors_xml': GlobalVendorsXml.from_str(global_vendors),
    }

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
            **resources
    ) as env:
        env.execute()
        yield env


@pytest.yield_fixture(scope="module")
def offers_snippet_workflow(workflow, yt_server):
    yt = yt_server.get_yt_client()
    offers = []
    for input_table_path in workflow.input_table_paths:
        offers.extend(yt.read_table(input_table_path))
    with SnippetDiffBuilderTestEnv(
        'offers_snippet_workflow',
        yt_server,
        offers=offers,
        genlogs=[],
        models=[],
        state=[]
    ) as env:
        env.execute()
        env.verify()
        yield env


@pytest.yield_fixture(scope="module")
def genlog_snippet_workflow(workflow, yt_server):
    genlogs = []
    for id, glProto in enumerate(workflow.genlog_dicts):
        if "offer_conversion" in glProto:
            for conv in glProto["offer_conversion"]:
                conv["conversion_type"] = EConversionType.Value(conv["conversion_type"])
        genlogs.append(glProto)

    with SnippetDiffBuilderTestEnv(
        'genlog_snippet_workflow',
        yt_server,
        offers=[],
        genlogs=genlogs,
        models=[],
        state=[]
    ) as env:
        env.execute()
        env.verify()
        yield env


@pytest.yield_fixture(scope="module")
def blue_offers_snippet_workflow(blue_workflow, yt_server):
    yt = yt_server.get_yt_client()
    offers = []
    for input_table_path in blue_workflow.input_table_paths:
        offers.extend(yt.read_table(input_table_path))
    with SnippetDiffBuilderTestEnv(
        'blue_offers_snippet_workflow',
        yt_server,
        offers=offers,
        genlogs=[],
        models=[],
        state=[]
    ) as env:
        env.execute()
        env.verify()
        yield env


@pytest.yield_fixture(scope="module")
def blue_genlog_snippet_workflow(blue_workflow, yt_server):
    genlogs = []
    for id, glProto in enumerate(blue_workflow.genlog_dicts):
        genlogs.append(glProto)

    with SnippetDiffBuilderTestEnv(
        'blue_genlog_snippet_workflow',
        yt_server,
        offers=[],
        genlogs=genlogs,
        models=[],
        state=[]
    ) as env:
        env.execute()
        env.verify()
        yield env


def genlog_comparator(all_snippet_from_offers, all_snippet_from_genlog):
    # by offer_id as int from key
    sorter = lambda record: int(record['value']['key'].split('-')[1])

    for snippet_from_offers, snippet_from_genlog in zip(
        sorted(all_snippet_from_offers, key=sorter),
        sorted(all_snippet_from_genlog, key=sorter)
    ):
        value_snippet_from_offers = snippet_from_offers['value']
        value_snippet_from_genlog = snippet_from_genlog['value']

        # this is time-dependant fields
        for key in ['deadline']:
            del value_snippet_from_offers[key]
            del value_snippet_from_genlog[key]

        assert_that(
            value_snippet_from_offers,
            equal_to(value_snippet_from_genlog),
            'snippet documents are same'
        )
