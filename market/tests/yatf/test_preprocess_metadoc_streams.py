# coding=utf-8
import pytest

from hamcrest import assert_that, equal_to, has_entries, has_items, all_of, has_item

from market.idx.streams.src.streams_converter.yatf.test_env import StreamsConverterTestEnv, StreamsConverterMode
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogRow
from market.idx.streams.src.streams_converter.yatf.resources.streams_converter_input import PreprocessMetadocStreamsInput
from market.idx.streams.src.streams_converter.yatf.resources.streams_converter_output import PreprocessMetadocStreamsOutput
from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.utils.genlog import genlog_table_schema
from market.idx.yatf.resources.mbo.cataloger_navigation_xml import CatalogerNavigationXml, NavigationTree, NavigationNode

from mapreduce.yt.python.table_schema import extract_column_attributes

from yt import wrapper as yt


GENLOG_PATH = '//out/genlogs/recent'
MAX_TEXT_SIZE = 30


FEED_ID=1
MSKU1=1
MSKU2=2
MSKU3_EMPTY_TEXTS=3
MSKU4_FAST_SKU_OFFER=4

OFFER_ID1 = '1'
SHOP_ID1 = 10
WAREHOUSE_ID1 = 20
BUSINESS_ID1 = 40

OFFER_ID2 = '2'
SHOP_ID2 = 11
WAREHOUSE_ID2 = 21
BUSINESS_ID2 = 41

OFFER_ID3_FAST_SKU = '3'
SHOP_ID3 = 12
WAREHOUSE_ID3 = 22
BUSINESS_ID3 = 42
SUPPLIER_NAME_OFFER3 = 'supplier_name_fast_sku_offer3_very_long_text'

SHOP_NAME_MSKU1_1 = 'shop_name_msku1_1'
SHOP_NAME_MSKU1_2 = 'shop_name_msku1_2'

SHOP_NAME_MSKU2_1 = 'shop_name_msku2'
SUPPLIER_NAME_MSKU2_1 = 'supplier_name_msku2_1'

SHOP_NAME_OFFER1 = 'shop_name_offer1'
SUPPLIER_NAME_OFFER2 = 'supplier_name_offer2'

SHOP_CATEGORY1 = 'cat1'
SHOP_CATEGORY2 = 'cat2'
SHOP_CATEGORY3 = 'cat3'

NID2_NAME = 'nid2'
NID22_NAME = 'nid22'
NID4_NAME = 'nid4'
NID41_NAME = 'nid41'
NID411_NAME = 'nid411'
NID42_NAME = 'nid42'
NID421_NAME = 'nid421'
NOT_EXISTING_NID = 111


@pytest.fixture(scope="module")
def cataloger_navigation_old_tree():
    return NavigationTree(
        1111,
        NavigationNode(
            nid=2, hid=1, is_blue=1, unique_name=NID2_NAME,
            children=[
                # simple nid
                NavigationNode(
                    nid=21, hid=11, is_blue=1, unique_name='nid21',
                    children=[
                        NavigationNode(
                            nid=211, hid=111, is_blue=1, unique_name='nid211',
                        ),
                    ]
                ),

                # virtual nid
                NavigationNode(
                    nid=22, is_blue=1, unique_name=NID22_NAME,
                    children=[
                        NavigationNode(
                            nid=221, hid=121, is_blue=1, unique_name='nid221',
                        ),
                    ]
                ),
            ]
        )
    )


@pytest.fixture(scope="module")
def cataloger_navigation_blue_tree():
    # Отдельное синее дерево
    # Для него не важно наличие маркера is_blue, как для старого дерева
    return NavigationTree(
        1113,
        NavigationNode(
            nid=4, hid=1, unique_name=NID4_NAME,
            children=[
                # simple nid
                NavigationNode(
                    nid=41, hid=11, unique_name=NID41_NAME,
                    children=[
                        NavigationNode(
                            nid=411, hid=111, unique_name=NID411_NAME,
                        ),
                    ]
                ),

                # virtual nid
                NavigationNode(
                    nid=42, unique_name=NID42_NAME,
                    children=[
                        NavigationNode(
                            nid=421, hid=121, unique_name=NID421_NAME,
                        ),
                    ]
                ),
            ]
        ),
        code='blue'
    )


# sequence_number здесь называется id
@pytest.fixture(scope="module")
def genlog0(yt_stuff):
    rows = [
        GenlogRow(shard_id=0, id=0, feed_id=FEED_ID, offer_id='offerid', market_sku=MSKU1, shop_name=SHOP_NAME_MSKU1_1,
                  shop_category_path=SHOP_CATEGORY1 + '\\' + SHOP_CATEGORY2, supplier_name='', nids_literals=[yt.yson.YsonUint64(2)]),
        GenlogRow(shard_id=0, id=1, feed_id=FEED_ID, offer_id='offerid', market_sku=MSKU1, shop_name=SHOP_NAME_MSKU1_2, is_fake_msku_offer=True,
                  shop_category_path=SHOP_CATEGORY1, supplier_name='', nids_literals=[yt.yson.YsonUint64(22), yt.yson.YsonUint64(421)]),
        GenlogRow(shard_id=0, id=2, feed_id=FEED_ID, offer_id=OFFER_ID1, market_sku=0, shop_id=SHOP_ID1, warehouse_id=WAREHOUSE_ID1,
                  business_id=BUSINESS_ID1, shop_name=SHOP_NAME_OFFER1, shop_category_path=SHOP_CATEGORY2,
                  nids_literals=[yt.yson.YsonUint64(4), yt.yson.YsonUint64(41), yt.yson.YsonUint64(411), yt.yson.YsonUint64(42)]),  # nid'ов больше, чем NIDS_LIMIT
        GenlogRow(shard_id=0, id=3, feed_id=FEED_ID, offer_id='offerid', market_sku=MSKU3_EMPTY_TEXTS,
                  shop_name='', shop_category_path=''),
        GenlogRow(shard_id=0, id=2, feed_id=FEED_ID, offer_id='offer_empty_texts', market_sku=0, shop_id=SHOP_ID1, warehouse_id=WAREHOUSE_ID1,
                  business_id=BUSINESS_ID1, shop_name='', shop_category_path=''),
    ]

    return YtTableResource(
        yt_stuff=yt_stuff,
        path=yt.ypath_join(get_yt_prefix(), GENLOG_PATH, '0000'),
        data=rows,
        attributes={
            'schema': genlog_table_schema()
        }
    )


@pytest.fixture(scope="module")
def genlog1(yt_stuff):
    rows = [
        GenlogRow(shard_id=1, id=0, feed_id=FEED_ID, offer_id='offerid', market_sku=MSKU2, shop_name=SHOP_NAME_MSKU2_1,
                  supplier_name=SUPPLIER_NAME_MSKU2_1, shop_category_path=SHOP_CATEGORY1 + '\\' + SHOP_CATEGORY2),
        GenlogRow(shard_id=1, id=1, feed_id=FEED_ID, offer_id='offerid', market_sku=MSKU2, shop_category_path=SHOP_CATEGORY3, shop_name=''),
        GenlogRow(shard_id=1, id=2, feed_id=FEED_ID, offer_id=OFFER_ID2, market_sku=0, shop_id=SHOP_ID2, warehouse_id=WAREHOUSE_ID2,
                  business_id=BUSINESS_ID2, supplier_name=SUPPLIER_NAME_OFFER2, shop_category_path='', nids_literals=[yt.yson.YsonUint64(NOT_EXISTING_NID)]),
        GenlogRow(shard_id=1, id=3, feed_id=FEED_ID, offer_id=OFFER_ID3_FAST_SKU, market_sku=MSKU4_FAST_SKU_OFFER, shop_id=SHOP_ID3, warehouse_id=WAREHOUSE_ID3,
                  business_id=BUSINESS_ID3, supplier_name=SUPPLIER_NAME_OFFER3, is_fast_sku=True, shop_category_path=''),
    ]
    return YtTableResource(
        yt_stuff=yt_stuff,
        path=yt.ypath_join(get_yt_prefix(), GENLOG_PATH, '0001'),
        data=rows,
        attributes={
            'schema': genlog_table_schema()
        }
    )


@pytest.fixture(scope="module")
def genlog_tables(genlog0, genlog1):
    return [genlog0, genlog1]


@pytest.fixture(scope='module')
def input_data(genlog_tables):
    return PreprocessMetadocStreamsInput(genlog_path=GENLOG_PATH, genlog_tables=genlog_tables,
                                         msku_max_offers_count=3, nids_limit=3, max_text_size=MAX_TEXT_SIZE)


# output info and YT tables
@pytest.fixture(scope='module')
def output_data():
    msku_table = '//indexer/streams/preprocess_metadoc_streams/msku'
    no_msku_offers_table = '//indexer/streams/preprocess_metadoc_streams/no_msku_offers_table'
    return PreprocessMetadocStreamsOutput(msku_table_path=msku_table, no_msku_offers_table_path=no_msku_offers_table)


@pytest.yield_fixture(scope='module')
def workflow(yt_stuff, input_data, output_data, cataloger_navigation_old_tree, cataloger_navigation_blue_tree):
    resources = {
        "input": input_data,
        "output": output_data,
        'cataloger_navigation_xml': CatalogerNavigationXml(
            filename="cataloger.navigation.xml",
            nav_trees=[cataloger_navigation_old_tree],
        ),
        'cataloger_navigation_all_xml': CatalogerNavigationXml(
            filename="cataloger.navigation.all.xml",
            nav_trees=[cataloger_navigation_blue_tree],
        ),
    }

    with StreamsConverterTestEnv(**resources) as env:
        env.execute(StreamsConverterMode.PREPROCESS_METADOC_STREAMS, yt_stuff)
        env.verify()
        yield env


def _res_max_size(text):
    return text[0:MAX_TEXT_SIZE]


def test_msku_count(workflow):
    assert_that(len(workflow.outputs.get("msku_table").data), equal_to(2))


def test_msku(workflow):
    assert_that(workflow.outputs.get("msku_table").data, has_items(
        has_entries({"market_sku": MSKU1, "available_for_fake_msku": True, "supplier_or_shop_name": _res_max_size(SHOP_NAME_MSKU1_1 + ' ' + SHOP_NAME_MSKU1_2),
                     "shop_categories": SHOP_CATEGORY1 + ' ' + SHOP_CATEGORY2, "nid_names": NID22_NAME + ' ' + NID421_NAME}),
        has_entries({"market_sku": MSKU2, "available_for_fake_msku": True, "supplier_or_shop_name": SUPPLIER_NAME_MSKU2_1,
                     "shop_categories": SHOP_CATEGORY1 + ' ' + SHOP_CATEGORY2 + ' ' + SHOP_CATEGORY3, "nid_names": ""}),
    ))


def test_no_msku_offers_count(workflow):
    assert_that(len(workflow.outputs.get("no_msku_offers_table").data), equal_to(3))


def test_no_msku_offers(workflow):
    assert_that(workflow.outputs.get("no_msku_offers_table").data, has_items(
        has_entries({"offer_id": OFFER_ID1, "shop_id": SHOP_ID1, "business_id": BUSINESS_ID1, "warehouse_id": WAREHOUSE_ID1,
                     "supplier_or_shop_name": SHOP_NAME_OFFER1, "shop_categories": SHOP_CATEGORY2, "nid_names": ' '.join([NID4_NAME, NID41_NAME, NID411_NAME])}),
        has_entries({"offer_id": OFFER_ID2, "shop_id": SHOP_ID2, "business_id": BUSINESS_ID2, "warehouse_id": WAREHOUSE_ID2,
                     "supplier_or_shop_name": SUPPLIER_NAME_OFFER2, "shop_categories": "", 'nid_names': ""}),
        has_entries({"offer_id": OFFER_ID3_FAST_SKU, "shop_id": SHOP_ID3, "business_id": BUSINESS_ID3, "warehouse_id": WAREHOUSE_ID3,
                     "supplier_or_shop_name": _res_max_size(SUPPLIER_NAME_OFFER3), "shop_categories": "", 'nid_names': ""}),
    ))


def test_table_schema_msku(workflow):
    """Проверяем наличие в схеме полей, необходимых для клея по мску"""
    result_schema = extract_column_attributes(list(workflow.outputs.get("msku_table").schema))
    expected_schema = [
        {'required': True, "name": "market_sku", "type": "uint64", "sort_order": "ascending"},
        {'required': False, "name": "available_for_fake_msku", "type": "boolean"},
    ]
    assert_that(result_schema, all_of(*[has_item(col) for col in expected_schema]), "Msku schema is incorrect")


def test_table_schema_offers(workflow):
    """Проверяем наличие в схеме полей, необходимых для клея по full_offer_id"""
    result_schema = extract_column_attributes(list(workflow.outputs.get("no_msku_offers_table").schema))
    expected_schema = [
        {'required': True, "name": "business_id", "type": "uint32", "sort_order": "ascending"},
        {'required': True, "name": "offer_id", "type": "string", "sort_order": "ascending"},
        {'required': True, "name": "shop_id", "type": "uint32", "sort_order": "ascending"},
        {'required': True, "name": "warehouse_id", "type": "uint32", "sort_order": "ascending"},
    ]
    assert_that(result_schema, all_of(*[has_item(col) for col in expected_schema]), "Offers schema is incorrect")
