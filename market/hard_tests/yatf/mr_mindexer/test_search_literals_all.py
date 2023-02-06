# coding: utf-8

from hamcrest import assert_that
import pytest

from market.idx.datacamp.proto.offer.OfferMapping_pb2 import Mapping as MappingPb

from market.idx.generation.yatf.test_envs.mr_mindexer import MrMindexerBuildTestEnv, MrMindexerMergeTestEnv
from market.idx.generation.yatf.resources.mr_mindexer.mr_mindexer_helpers import MrMindexerMergeOptions, MrMindexerMergeIndexType
from market.idx.yatf.resources.mbo.global_vendors_xml import GlobalVendorsXml

from market.idx.offers.yatf.matchers.offers_indexer.env_matchers import HasLiterals, HasNoLiterals
from market.idx.yatf.resources.model_ids import ModelIds
from market.idx.offers.yatf.resources.offers_indexer.model_medical_flags import ModelMedicalFlags
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog, default_blue_genlog, default_shops_dat, genererate_default_pictures

from market.idx.pylibrary.offer_flags.flags import OfferFlags, MedicalFlags
from market.idx.yatf.resources.shops_dat import ShopsDat

from market.proto.content.mbo.ExportReportModel_pb2 import (
    ExportReportModel,
    ParameterValue
)
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join
import yt.wrapper as yt


MARKET_SKU_TYPE_FAST = MappingPb.MarketSkuType.MARKET_SKU_TYPE_FAST
MEDICAL_BOOKING_SHOP_ID = 4243
MEDICAL_BOOKING_FEED_ID = 101963
MEDICAL_MODEL_ID = 1


def add_pictures(offer):
    offer['picture_urls'] = ["beremvsetut.ru/upload/iblock/610/7fb8ccb244dd.png"]
    offer['picture_crcs'] = [u'asdasdasddasdsdd']


@pytest.fixture(scope="module")
def genlog_rows():
    modification_offer = default_genlog(offer_id='1')
    modification_offer['model_id'] = 2
    modification_offer['ware_md5'] = '09lEaAKkQll1XTaaaaaaaQ'

    model_offer = default_genlog(offer_id='2')
    model_offer['model_id'] = 3
    model_offer['ware_md5'] = 'kP3oC5KjARGI5f9EEkNGtA'
    model_offer['market_sku'] = 123456789

    # https://a.yandex-team.ru/arc/trunk/arcadia/market/idx/offers/lib/iworkers/FillIndexDocumentFields.cpp?rev=r9253847#L539
    visual_model_offer = default_genlog(offer_id='3')
    visual_model_offer['cluster_id'] = 666
    visual_model_offer['ware_md5'] = 'eZjINkpeLXpTXaRI/9NKWA'
    visual_model_offer['pictures'] = genererate_default_pictures()
    add_pictures(visual_model_offer)

    blue_model_visual_offer = default_genlog(offer_id='4')
    blue_model_visual_offer['model_id'] = 5
    blue_model_visual_offer['cluster_id'] = 666
    blue_model_visual_offer['ware_md5'] = 'a2arNpb7M1bYvuTQzJ4rYg'
    blue_model_visual_offer['pictures'] = genererate_default_pictures()
    add_pictures(blue_model_visual_offer)

    blue_model_offer = default_genlog(offer_id='5')
    blue_model_offer['model_id'] = 5
    blue_model_offer['ware_md5'] = 'bmsD+9/S6qcBoJx09K/A9A'
    blue_model_offer['pictures'] = genererate_default_pictures()
    add_pictures(blue_model_offer)

    blue_model_blue_offer = default_genlog(offer_id='6')
    blue_model_blue_offer['model_id'] = 5
    blue_model_blue_offer['is_blue_offer'] = True
    blue_model_blue_offer['ware_md5'] = 'nomkVuHL2Q0wYGPdnvvfKg'
    blue_model_blue_offer['pictures'] = genererate_default_pictures()
    add_pictures(blue_model_blue_offer)

    simple_offer = default_genlog(offer_id='7')
    simple_offer['ware_md5'] = 'fDbQKU6BwzM0vDugM73auA'
    simple_offer['shop_id'] = 1234
    simple_offer['yx_money'] = 5678
    simple_offer['market_sku'] = 1234567891

    url_hash_offer1 = default_genlog(offer_id='8')
    url_hash_offer1['ware_md5'] = '2b0+iAnHLZST2Ekoq4xElg'
    url_hash_offer1['url'] = 'http://notcheater.com/index.html?param1&param2=value'

    url_hash_offer2 = default_genlog(offer_id='9')
    url_hash_offer2['ware_md5'] = 'Li/2sW0BMcWWmEkd3+7MHg'
    url_hash_offer2['url'] = 'http://notcheater.com/index.html?param1&param2=value&utm_city=nsk'

    url_hash_offer3 = default_genlog(offer_id='10')
    url_hash_offer3['ware_md5'] = 'yNWXdRty80uhcFXrHX8ohA'
    url_hash_offer3['url'] = 'http://notcheater.com/index.html?param1&param2=value#anchor'

    real_vendor_offer = default_genlog(offer_id='11')
    real_vendor_offer['ware_md5'] = 'U456gPGGzNMUNkkQMQAkgQ'
    real_vendor_offer['vendor_id'] = 1
    real_vendor_offer['fake_vendor_id'] = 1  # https://a.yandex-team.ru/arc/trunk/arcadia/market/idx/offers/lib/loaders/load_biz_logic.cpp?rev=r9258321#L540

    fake_vendor_offer = default_genlog(offer_id='12')
    fake_vendor_offer['ware_md5'] = 'ZxsaX66q4bw4J4KxaxPSUg'
    fake_vendor_offer['vendor_id'] = 2

    zero_vendor_offer = default_genlog(offer_id='13')
    zero_vendor_offer['ware_md5'] = 'NzA5NDU0MWI2ODgzNDY4Mg'
    zero_vendor_offer['vendor_id'] = 0

    fake_msku_offer = default_genlog(offer_id='14')
    fake_msku_offer['ware_md5'] = 'FakeMSKUMWI2ODgzNDY4Mg'
    fake_msku_offer['vendor_id'] = 123456
    fake_msku_offer['fake_vendor_id'] = 123456
    fake_msku_offer['is_blue_offer'] = True
    fake_msku_offer['is_fake_msku_offer'] = True

    fake_p_offer = default_genlog(offer_id='15')
    fake_p_offer['ware_md5'] = 'FAKE1POFFEROOOOOOOOOOA'
    fake_p_offer['is_blue_offer'] = True
    fake_p_offer['supplier_type'] = 1
    fake_p_offer['supplier_id'] = 42

    fake_p_offer_nb = default_genlog(offer_id='16')
    fake_p_offer_nb['ware_md5'] = 'FAKE1POFFERTTTTTTTTTTQ'
    fake_p_offer_nb['supplier_type'] = 1

    blue_3p_offer = default_genlog(offer_id='17')
    blue_3p_offer['ware_md5'] = 'blue3PofferTTTTTTTTTTQ'
    blue_3p_offer['is_blue_offer'] = True
    blue_3p_offer['supplier_type'] = 3
    blue_3p_offer['supplier_id'] = 42

    virtual_offer = default_genlog(offer_id='18')
    virtual_offer['supplier_id'] = 322
    virtual_offer['ware_md5'] = 'eZXtcGaz7OQdVIGMgwxKZw'

    smb_offer = default_genlog(offer_id='19')
    smb_offer['flags'] = OfferFlags.IS_SMB
    smb_offer['ware_md5'] = 'JMnlC1mXj1yXQVOO2eZQng'

    white_cpa_offer = default_genlog(offer_id='20')
    white_cpa_offer['shop_id'] = 4242  # white shop with cpa=real
    white_cpa_offer['feed_id'] = 101962
    white_cpa_offer['cpa'] = 4
    white_cpa_offer['ware_md5'] = 'offerXdsbsXXXXXXXXXXXg'
    white_cpa_offer['market_sku'] = 1234567892

    white_cpa_no_msku_offer = default_genlog(offer_id='21')
    white_cpa_no_msku_offer['shop_id'] = 4242  # white shop with cpa=real
    white_cpa_no_msku_offer['feed_id'] = 101962
    white_cpa_no_msku_offer['cpa'] = 4
    white_cpa_no_msku_offer['ware_md5'] = 'offerXdsbsXnoXmskuXXXg'

    white_cpa_offer_no_cpa_shop = default_genlog(offer_id='22')
    white_cpa_offer_no_cpa_shop['cpa'] = 2
    white_cpa_offer_no_cpa_shop['ware_md5'] = 'offerXdsbsXnoXcpaXshXg'

    offer_without_msku = default_genlog(offer_id='23')
    offer_without_msku['ware_md5'] = 'withoutmskuWmEkd3+7MHg'

    offer_with_msku = default_genlog(offer_id='24')
    offer_with_msku['ware_md5'] = 'withmskuWmEkd3+7MHgqwe'
    offer_with_msku['market_sku'] = 1

    offer_with_fast_sku = default_blue_genlog(
        offer_id='26',
        market_sku=600,
        market_sku_type=MARKET_SKU_TYPE_FAST,
        ware_md5='7BdJMr5yGQjkaWoplalTmQ',
        is_fast_sku=True  # https://a.yandex-team.ru/arc/trunk/arcadia/market/idx/offers/lib/loaders/load_biz_logic.cpp?rev=r9258321#L240
    )

    contex_msku_cloned_offer = default_blue_genlog(
        offer_id='27',
        market_sku=1000,
        model_id=11,
        contex_info={
            'experiment_id': 'some_exp',
            'original_msku_id': yt.yson.YsonUint64(2000),
            'is_experimental': True,
            'original_model_id': yt.yson.YsonUint64(11),
            'experimental_model_id': yt.yson.YsonUint64(12)
        },
        ware_md5='9AkYdt9qXTnmaQtzlalTmQ',
    )

    contex_msku_original_offer = default_blue_genlog(
        offer_id='28',
        model_id=11,
        market_sku=2000,
        contex_info={
            'experiment_id': 'some_exp',
            'experimental_msku_id': yt.yson.YsonUint64(1000),
            'original_model_id': yt.yson.YsonUint64(11),
            'experimental_model_id': yt.yson.YsonUint64(12)
        },
        ware_md5='AomkVuHL2Q0wYGPdnvvfKg',
    )

    b2b_offer = default_genlog(offer_id='29')
    b2b_offer['flags'] = OfferFlags.AVAILABLE_FOR_BUSINESSES
    b2b_offer['ware_md5'] = 'B2BOFFERbbbbbbbbbbbbbb'

    sample_offer = default_genlog(offer_id='30')
    sample_offer['flags'] = OfferFlags.IS_SAMPLE
    sample_offer['ware_md5'] = 'IsSamplessssssssssssss'

    medical_booking_offer = default_genlog(offer_id='31')
    medical_booking_offer['model_id'] = MEDICAL_MODEL_ID
    medical_booking_offer['shop_id'] = MEDICAL_BOOKING_SHOP_ID
    medical_booking_offer['feed_id'] = MEDICAL_BOOKING_FEED_ID
    medical_booking_offer['ware_md5'] = 'MedicalBookingOfferrrr'

    eats_retail_offer = default_genlog(offer_id='32')
    eats_retail_offer['flags'] = OfferFlags.IS_EDA_RETAIL
    eats_retail_offer['ware_md5'] = 'IsEdaRetail__________g'

    only_b2b_offer = default_genlog(offer_id='33')
    only_b2b_offer['flags'] = OfferFlags.AVAILABLE_FOR_BUSINESSES | OfferFlags.PROHIBITED_FOR_PERSONS
    only_b2b_offer['ware_md5'] = 'ONLYB2BOFFERbbbbbbbbbb'

    not_b2c_offer = default_genlog(offer_id='34')
    not_b2c_offer['flags'] = OfferFlags.PROHIBITED_FOR_PERSONS
    not_b2c_offer['ware_md5'] = 'NOTB2COFFERbbbbbbbbbbb'

    resale_offer = default_genlog(offer_id='35')
    resale_offer['flags'] = OfferFlags.IS_RESALE
    resale_offer['ware_md5'] = 'IsResaleOffer________g'

    # Вендорская ставка выставляется с помощью вендорской автостратегии amore_beru_vendor_data
    # POSITIONAL автостратегия с ненулевой ставкой (основная в репорте) соответствует 12 байтам: 0000'2050'0000
    ABVD_POSITIONAL_NONZERO = b'\x00\x00\x00\x00\x02\x00\x05\x00\x00\x00\x00\x00'
    # Мерчовая ставка (shop_fee) выставляется с помощью автостратегии amore_data
    # CPA автостратегия с ненулевой ставкой (основная в репорте) соответствует 12 байтам: 0000'5100'0000
    AD_CPA_NONZERO = b'\x00\x00\x00\x00\x05\x01\x00\x00\x00\x00\x00\x00'
    offer_with_fast_sku_and_vendor_fee = default_blue_genlog(
        offer_id='36',
        market_sku=601,
        market_sku_type=MARKET_SKU_TYPE_FAST,
        ware_md5='fastwithvendorfee____g',
        amore_beru_vendor_data=ABVD_POSITIONAL_NONZERO,
        is_fast_sku=True,
        supplier_type=3,
    )

    offer_with_fast_sku_and_fee = default_blue_genlog(
        offer_id='37',
        market_sku=602,
        market_sku_type=MARKET_SKU_TYPE_FAST,
        ware_md5='fastwithfee__________g',
        amore_data=AD_CPA_NONZERO,
        is_fast_sku=True,
        cpa=4,
        supplier_type=3,
    )

    # 1p оффер с fast-sku, но без ставок - он получит рекомендованную ставку в репорте
    offer_1p_with_fast_sku_and_no_fee = default_blue_genlog(
        offer_id='38',
        market_sku=603,
        market_sku_type=MARKET_SKU_TYPE_FAST,
        ware_md5='fastnofeefirstparty__g',
        is_fast_sku=True,
        cpa=4,
        supplier_type=1,
    )

    url_hash_offers = [url_hash_offer1, url_hash_offer2, url_hash_offer3]

    return url_hash_offers + [
        modification_offer,  # doc_id = 3
        model_offer,
        visual_model_offer,
        blue_model_visual_offer,
        blue_model_offer,
        blue_model_blue_offer,
        simple_offer,
        real_vendor_offer,  # doc_id = 10
        zero_vendor_offer,
        fake_vendor_offer,
        blue_3p_offer,
        fake_msku_offer,
        fake_p_offer,
        fake_p_offer_nb,
        virtual_offer,
        smb_offer,
        contex_msku_cloned_offer,
        contex_msku_original_offer,  # doc_id = 20
        white_cpa_offer,
        white_cpa_no_msku_offer,
        white_cpa_offer_no_cpa_shop,
        offer_without_msku,
        offer_with_msku,
        offer_with_fast_sku,
        b2b_offer,
        only_b2b_offer,
        not_b2c_offer,
        sample_offer,  # doc_id = 30
        resale_offer,
        medical_booking_offer,
        eats_retail_offer,
        offer_with_fast_sku_and_vendor_fee,  # doc_id = 34
        offer_with_fast_sku_and_fee,
        offer_1p_with_fast_sku_and_no_fee,
    ]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.fixture(scope="module")
def model_ids():
    return ModelIds([1, 2, 3, 11, 12, 666], blue_ids=[4, 5])


@pytest.fixture(scope="module")
def global_vendors():
    return '''
        <global-vendors>
          <vendor id="1" name="name1"/>
          <vendor id="2" name="yandex">
            <is-fake-vendor>true</is-fake-vendor>
          </vendor>
        </global-vendors>
    '''


@pytest.yield_fixture(scope='module')
def custom_shops_dat():
    shops = [
        default_shops_dat(
            name="Shop_cpa_real", fesh=4242, priority_region=213,
                regions=[225], home_region=225, datafeed_id=101962, cpa='REAL'
        ),
        default_shops_dat(
            name="Shop_medical_booking", fesh=MEDICAL_BOOKING_SHOP_ID, priority_region=213,
                regions=[225], home_region=225, datafeed_id=MEDICAL_BOOKING_FEED_ID, medical_booking='true'
        )
    ]

    return ShopsDat(shops)


@pytest.fixture(scope="module")
def medical_models():
    with_medicine_flag=ExportReportModel(
        id=MEDICAL_MODEL_ID,
        parameter_values=[
            ParameterValue(
                param_id=MedicalFlags.DRUG_TYPE_PARAM,
                option_id=MedicalFlags.MEDICINE_TYPE_VALUE,
            ),
        ]
    )

    return ModelMedicalFlags([with_medicine_flag])


@pytest.yield_fixture(scope='module')
def offers_processor_workflow(yt_server, genlog_table, model_ids, global_vendors, custom_shops_dat, medical_models):
    input_table_paths = [genlog_table.get_path()]
    resources = {
        'shops_dat': custom_shops_dat,
        'model_ids': model_ids,
        'model_medical_flags': medical_models,
        'global_vendors_xml': GlobalVendorsXml.from_str(global_vendors),
    }

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
            **resources
    ) as env:
        env.execute()
        env.verify()
        yield env


@pytest.yield_fixture(scope="module")
def mr_mindexer_build(yt_server, offers_processor_workflow):
    with MrMindexerBuildTestEnv() as build_env:
        build_env.execute_from_offers_list(yt_server, offers_processor_workflow.genlog_dicts)
        build_env.verify()
        yield build_env


@pytest.yield_fixture(scope="module")
def mr_mindexer_direct(yt_server, mr_mindexer_build):
    resourses = {
        'merge_options': MrMindexerMergeOptions(
            input_portions_path=mr_mindexer_build.yt_index_portions_path,
            part=0,
            index_type=MrMindexerMergeIndexType.DIRECT,
        ),
    }

    with MrMindexerMergeTestEnv(**resourses) as env:
        env.execute(yt_server)
        env.verify()
        env.literal_lemmas.load()
        yield env


@pytest.yield_fixture(scope="module")
def mr_mindexer_build_cpc(yt_server, offers_processor_workflow):
    resources = {
        'is_cpc': True
    }

    with MrMindexerBuildTestEnv(**resources) as build_env:
        build_env.execute_from_offers_list(yt_server, offers_processor_workflow.genlog_dicts)
        build_env.verify()
        yield build_env


@pytest.yield_fixture(scope="module")
def mr_mindexer_direct_cpc(yt_server, mr_mindexer_build_cpc):
    resourses = {
        'merge_options': MrMindexerMergeOptions(
            input_portions_path=mr_mindexer_build_cpc.yt_index_portions_path,
            part=0,
            index_type=MrMindexerMergeIndexType.DIRECT,
        ),
    }

    with MrMindexerMergeTestEnv(**resourses) as env:
        env.execute(yt_server)
        env.verify()
        env.literal_lemmas.load()
        yield env


@pytest.yield_fixture(scope="module")
def mr_mindexer_direct_arc(yt_server, mr_mindexer_build):
    resourses = {
        'merge_options': MrMindexerMergeOptions(
            input_portions_path=mr_mindexer_build.yt_index_portions_path,
            part=0,
            index_type=MrMindexerMergeIndexType.DIRECT_ARCH,
        ),
    }

    with MrMindexerMergeTestEnv(**resourses) as env:
        env.execute(yt_server)
        env.verify()
        env.outputs['indexarc'].load()
        yield env


@pytest.yield_fixture(scope="module")
def mr_mindexer_direct_arc_cpc(yt_server, mr_mindexer_build_cpc):
    resourses = {
        'merge_options': MrMindexerMergeOptions(
            input_portions_path=mr_mindexer_build_cpc.yt_index_portions_path,
            part=0,
            index_type=MrMindexerMergeIndexType.DIRECT_ARCH,
        ),
    }

    with MrMindexerMergeTestEnv(**resourses) as env:
        env.execute(yt_server)
        env.verify()
        env.outputs['indexarc'].load()
        yield env


@pytest.fixture(scope="module")
def doc_id_by_offer_id(mr_mindexer_direct_arc):
    mapping = {}
    arc = mr_mindexer_direct_arc.outputs['indexarc']
    for i in arc.doc_ids:
        offer_id = arc.load_doc_description(i)['offer_id']
        mapping[offer_id] = i
    return mapping


@pytest.fixture(scope="module")
def doc_id_by_offer_id_cpc(mr_mindexer_direct_arc_cpc):
    mapping = {}
    arc = mr_mindexer_direct_arc_cpc.outputs['indexarc']
    for i in arc.doc_ids:
        offer_id = arc.load_doc_description(i)['offer_id']
        mapping[offer_id] = i
    return mapping


def test_model_simple_model(mr_mindexer_direct, doc_id_by_offer_id):
    # Оффер обычной или групповой модели не имеет parent_model_id
    doc_id = doc_id_by_offer_id['2']
    assert_that(mr_mindexer_direct, HasLiterals('#market_sku="123456789', [doc_id]))


def test_visual_cluster(mr_mindexer_direct, doc_id_by_offer_id):
    # Оффер, приматченный к кластеру, имеет соответствющий hyper_id. Не приматченный к sku имеет market_sku=0.
    doc_id = doc_id_by_offer_id['3']
    assert_that(mr_mindexer_direct, HasLiterals('#hyper_id="666', [doc_id]))
    assert_that(mr_mindexer_direct, HasLiterals('#market_sku="0', [doc_id]))


def test_blue_model_visual_offer(mr_mindexer_direct, doc_id_by_offer_id):
    # Оффер, приматченный к синей модели и к кластеру, выглядит как будто приматчен только к кластеру
    doc_id = doc_id_by_offer_id['4']
    assert_that(mr_mindexer_direct, HasLiterals('#hyper_id="666', [doc_id]))


def test_blue_model(mr_mindexer_direct, doc_id_by_offer_id):
    # Оффер, приматченный к синей модели, не имеет hyper_id
    doc_id = doc_id_by_offer_id['5']
    assert_that(mr_mindexer_direct, HasLiterals('#hyper_id="0', [doc_id]))


def test_blue_model_blue_offer(mr_mindexer_direct, doc_id_by_offer_id):
    # Синий оффер, приматченный к синей модели, имеет hyper_id
    doc_id = doc_id_by_offer_id['6']
    assert_that(mr_mindexer_direct, HasLiterals('#hyper_id="5', [doc_id]))


def test_blue_doctype_blue_offer(mr_mindexer_direct, doc_id_by_offer_id):
    # Синий офер имеет поисковой литерал blue_doctype:b
    doc_id = doc_id_by_offer_id['6']
    assert_that(mr_mindexer_direct, HasLiterals('#blue_doctype="b', [doc_id]))


def test_no_model(mr_mindexer_direct, doc_id_by_offer_id):
    # Оффер, не приматченый к модели не имеет hyper_id
    doc_id = doc_id_by_offer_id['7']
    assert_that(mr_mindexer_direct, HasLiterals('#yx_ds_id="1234', [doc_id]))
    assert_that(mr_mindexer_direct, HasLiterals('#yx_money="5678', [doc_id]))
    assert_that(mr_mindexer_direct, HasLiterals('#market_sku="1234567891', [doc_id]))


def test_blue_doctype_simple_offer(mr_mindexer_direct, doc_id_by_offer_id):
    # Обычный (не синий) офер имеет поисковой литерал blue_doctype:w
    doc_id = doc_id_by_offer_id['8']
    assert_that(mr_mindexer_direct, HasLiterals('#blue_doctype="w', [doc_id]))


def test_blue_doctype_msku(mr_mindexer_direct, doc_id_by_offer_id):
    # Псевдоофер msku имеет поисковой литерал blue_doctype:m
    doc_id = doc_id_by_offer_id['14']
    assert_that(mr_mindexer_direct, HasLiterals('#blue_doctype="m', [doc_id]))
    assert_that(mr_mindexer_direct, HasLiterals('#vendor_id="123456', [doc_id]))


def test_blue_doctype_white_cpa(mr_mindexer_direct, doc_id_by_offer_id):
    # Белый cpa оффер c msku имеет поисковой литерал blue_doctype:w_cpa  blue_doctype:w_cpa_m и blue_doctype:w
    doc_id = doc_id_by_offer_id['20']
    assert_that(mr_mindexer_direct, HasLiterals('#blue_doctype="w', [doc_id]))
    assert_that(mr_mindexer_direct, HasLiterals('#blue_doctype="w_cpa', [doc_id]))
    assert_that(mr_mindexer_direct, HasLiterals('#blue_doctype="w_cpa_m', [doc_id]))


def test_blue_doctype_white_cpa_no_msku(mr_mindexer_direct, doc_id_by_offer_id):
    # Белый cpa оффер без msku имеет поисковые литералы blue_doctype:w_cpa, blue_doctype:w
    doc_id = doc_id_by_offer_id['21']
    assert_that(mr_mindexer_direct, HasLiterals('#blue_doctype="w', [doc_id]))
    assert_that(mr_mindexer_direct, HasLiterals('#blue_doctype="w_cpa', [doc_id]))

    # Белый cpa оффер без msku не имеет поисковой литерал blue_doctype:w_cpa_m
    assert_that(mr_mindexer_direct, HasNoLiterals('#blue_doctype="w_cpa_m', [doc_id]))


def test_blue_doctype_white_msku(mr_mindexer_direct, doc_id_by_offer_id):
    # Белый cpa оффер c msku в не cpa магазине имеет поисковой литерал blue_doctype:w
    doc_id = doc_id_by_offer_id['22']
    assert_that(mr_mindexer_direct, HasLiterals('#blue_doctype="w', [doc_id]))

    # Белый cpa оффер c msku в не cpa магазине не имеет поисковой литерал blue_doctype:w_cpa
    assert_that(mr_mindexer_direct, HasNoLiterals('#blue_doctype="w_cpa', [doc_id]))


def test_offer_url_hash(mr_mindexer_direct, doc_id_by_offer_id):
    url_hash_tests = [
        # http://notcheater.com/index.html?param1&param2=value
        # Offer has url hash as a search literal and a property
        ('8', '5666692493974024187'),

        # http://notcheater.com/index.html?param1&param2=value&utm_city=nsk
        # Url is the same due to canonization so hash must be the same
        ('9', '5666692493974024187'),

        # http://notcheater.com/index.html?param1&param2=value#anchor
        # Url differs due to anchor so hash differs
        ('10', '11547559326314431395')
    ]

    for test in url_hash_tests:
        doc_id = doc_id_by_offer_id[test[0]]
        assert_that(mr_mindexer_direct, HasLiterals('#offer_url_hash="' + test[1], [doc_id]))


def test_real_vendor_id(mr_mindexer_direct, doc_id_by_offer_id):
    doc_id = doc_id_by_offer_id['11']
    assert_that(mr_mindexer_direct, HasLiterals('#vendor_id="1', [doc_id]))

    doc_id = doc_id_by_offer_id['12']
    assert_that(mr_mindexer_direct, HasNoLiterals('#vendor_id', [doc_id]))


def test_zero_vendor_id(mr_mindexer_direct, doc_id_by_offer_id):
    # У оффера отсутствует нулевой vendor_id
    doc_id = doc_id_by_offer_id['13']
    assert_that(mr_mindexer_direct, HasNoLiterals('#vendor_id', [doc_id]))


def test_supplier_type(mr_mindexer_direct, doc_id_by_offer_id):
    doc_id = doc_id_by_offer_id['15']
    assert_that(mr_mindexer_direct, HasLiterals('#supplier_type="1', [doc_id]))
    # Оффер не из синего маркета
    doc_id = doc_id_by_offer_id['16']
    assert_that(mr_mindexer_direct, HasNoLiterals('#supplier_type', [doc_id]))
    # У 3p-оффера должен проставляться поисковый литерал supplier_type: 3
    doc_id = doc_id_by_offer_id['17']
    assert_that(mr_mindexer_direct, HasLiterals('#supplier_type="3', [doc_id]))


def test_smb_offer(mr_mindexer_direct, doc_id_by_offer_id):
    # У smb офферов проставился литерал "is_smb_offer". Оффер не модельный и не кластерный, поэтому market_sku=0 не ставится
    doc_id = doc_id_by_offer_id['19']
    assert_that(mr_mindexer_direct, HasLiterals('#is_smb_offer="1', [doc_id]))
    assert_that(mr_mindexer_direct, HasNoLiterals('#market_sku', [doc_id]))


def test_contex_green(mr_mindexer_direct, doc_id_by_offer_id):
    # литерал contex=green для белого оффера, синего оффера, синего оффера с флагом "is_fake_msku_offer"
    doc_ids_contex_green = [doc_id_by_offer_id['8'], doc_id_by_offer_id['15'], doc_id_by_offer_id['14']]
    assert_that(mr_mindexer_direct, HasLiterals('#contex="green', doc_ids_contex_green))


def test_contex_blue_offers(mr_mindexer_direct, doc_id_by_offer_id):
    # https://wiki.yandex-team.ru/market/report/infra/abtcontent/?from=%252Fusers%252Fzhnick%252Fabtblue%252F#poiskovyeliteratycontex
    doc_id = doc_id_by_offer_id['27']

    # Клонированный оффер
    assert_that(mr_mindexer_direct, HasLiterals('#contex="some_exp', [doc_id]))
    assert_that(mr_mindexer_direct, HasLiterals('#hyper_id="11', [doc_id]))
    assert_that(mr_mindexer_direct, HasLiterals('#market_sku="2000', [doc_id]))
    assert_that(mr_mindexer_direct, HasNoLiterals('#anti_contex', [doc_id]))

    # Оригинальный оффер
    doc_id = doc_id_by_offer_id['28']
    assert_that(mr_mindexer_direct, HasLiterals('#contex="classic', [doc_id]))
    assert_that(mr_mindexer_direct, HasLiterals('#hyper_id="11', [doc_id]))
    assert_that(mr_mindexer_direct, HasLiterals('#market_sku="2000', [doc_id]))
    assert_that(mr_mindexer_direct, HasLiterals('#anti_contex="some_exp', [doc_id]))


def test_fake_msku_regions(mr_mindexer_direct, doc_id_by_offer_id):
    """Проверяем, что не ставим региональный литерал "Земля" для fake-msku
    при включенной настройке --reduce-regions-for-fake-msku
    """
    # fake-msku оффер: ставим metadoc, не ставим not_empty_metadoc
    doc_id1 = doc_id_by_offer_id['14']
    assert_that(mr_mindexer_direct, HasNoLiterals('#offer_region="10000', [doc_id1]))


def test_is_metadoc_search_literal(mr_mindexer_direct, doc_id_by_offer_id):
    # fake-msku оффер: ставим metadoc, не ставим not_empty_metadoc
    doc_id1 = doc_id_by_offer_id['14']
    assert_that(mr_mindexer_direct, HasLiterals('#is_metadoc="1', [doc_id1]))
    assert_that(mr_mindexer_direct, HasNoLiterals('#is_not_empty_metadoc="1', [doc_id1]))

    # оффер без market_sku: ставим metadoc и not_empty_metadoc
    doc_id2 = doc_id_by_offer_id['23']
    assert_that(mr_mindexer_direct, HasLiterals('#is_metadoc="1', [doc_id2]))
    assert_that(mr_mindexer_direct, HasLiterals('#is_not_empty_metadoc="1', [doc_id2]))

    # Ставим оба литерала офферу с быстрой sku
    doc_id3 = doc_id_by_offer_id['26']
    assert_that(mr_mindexer_direct, HasLiterals('#is_metadoc="1', [doc_id3]))
    assert_that(mr_mindexer_direct, HasLiterals('#is_not_empty_metadoc="1', [doc_id3]))

    # У оффера с мску не ставится литерал is_metadoc и not_empty_metadoc
    doc_id4 = doc_id_by_offer_id['24']
    assert_that(mr_mindexer_direct, HasNoLiterals('#is_metadoc="1', [doc_id4]))
    assert_that(mr_mindexer_direct, HasNoLiterals('#is_not_empty_metadoc="1', [doc_id4]))


def test_is_metadoc_search_literal_cpc(mr_mindexer_direct_cpc, doc_id_by_offer_id_cpc):
    for doc in doc_id_by_offer_id_cpc.keys():
        doc_id = doc_id_by_offer_id_cpc[doc]
        if doc in ['2', '7', '14', '27', '28', '20', '24', '26', '36', '37', '38']:
            assert_that(mr_mindexer_direct_cpc, HasLiterals('#is_metadoc="1', [doc_id]))
            if doc != '14':   # fake-msku
                assert_that(mr_mindexer_direct_cpc, HasLiterals('#is_not_empty_metadoc="1', [doc_id]))
            else:
                assert_that(mr_mindexer_direct_cpc, HasNoLiterals('#is_not_empty_metadoc="1', [doc_id]))
        else:
            assert_that(mr_mindexer_direct_cpc, HasNoLiterals('#is_metadoc="1', [doc_id]))
            assert_that(mr_mindexer_direct_cpc, HasNoLiterals('#is_not_empty_metadoc="1', [doc_id]))


def test_available_for_businesses_and_persons_search_literals(mr_mindexer_direct, doc_id_by_offer_id):
    # Если у оффера есть флаг AVAILABLE_FOR_BUSINESSES и нет PROHIBITED_FOR_PERSONS, то ставим оба литерала
    doc_id1 = doc_id_by_offer_id['29']
    assert_that(mr_mindexer_direct, HasLiterals('#is_b2b="1', [doc_id1]))
    assert_that(mr_mindexer_direct, HasLiterals('#is_b2c="1', [doc_id1]))

    # Если нет ни одного флага, то ставим только литерал is_b2c
    doc_id2 = doc_id_by_offer_id['28']
    assert_that(mr_mindexer_direct, HasLiterals('#is_b2c="1', [doc_id2]))
    assert_that(mr_mindexer_direct, HasNoLiterals('#is_b2b="1', [doc_id2]))

    # Если у оффера есть оба флага, то ставим только литерал is_b2b
    doc_id3 = doc_id_by_offer_id['33']
    assert_that(mr_mindexer_direct, HasNoLiterals('#is_b2c="1', [doc_id3]))
    assert_that(mr_mindexer_direct, HasLiterals('#is_b2b="1', [doc_id3]))

    # Если у оффера есть флаг PROHIBITED_FOR_PERSONS и нет AVAILABLE_FOR_BUSINESSES, то не ставим никаких литералов
    doc_id4 = doc_id_by_offer_id['34']
    assert_that(mr_mindexer_direct, HasNoLiterals('#is_b2c="1', [doc_id4]))
    assert_that(mr_mindexer_direct, HasNoLiterals('#is_b2b="1', [doc_id4]))


def test_sample_offer(mr_mindexer_direct, doc_id_by_offer_id):
    doc_id = doc_id_by_offer_id['30']
    assert_that(mr_mindexer_direct, HasLiterals('#is_sample_offer="1', [doc_id]))


def test_resale_offer(mr_mindexer_direct, doc_id_by_offer_id):
    doc_id = doc_id_by_offer_id['35']
    assert_that(mr_mindexer_direct, HasLiterals('#is_resale_offer="1', [doc_id]))


def test_medical_booking_search_literal(mr_mindexer_direct, doc_id_by_offer_id):
    doc_id = doc_id_by_offer_id['31']
    assert_that(mr_mindexer_direct, HasLiterals('#medical_booking="1', [doc_id]))


def test_eats_offer_region(mr_mindexer_direct, doc_id_by_offer_id):
    """
    Проверяем, что ставим региональный литерал "Земля" для оферов Еды
    """
    doc_id1 = doc_id_by_offer_id['32']
    assert_that(mr_mindexer_direct, HasLiterals('#offer_region="10000', [doc_id1]))


def test_has_adv_bid_search_literal(mr_mindexer_direct, doc_id_by_offer_id):
    """
    Проверяем проставление литерала has_adv_bid
    """
    # оффер без market_sku и без ставки: есть metadoc, но нет has_adv_bid
    doc_id1 = doc_id_by_offer_id['23']
    assert_that(mr_mindexer_direct, HasLiterals('#is_metadoc="1', [doc_id1]))
    assert_that(mr_mindexer_direct, HasNoLiterals('#has_adv_bid="1', [doc_id1]))

    # fast-sku со ставкой вендора: ставим has_adv_bid
    doc_id2 = doc_id_by_offer_id['36']
    assert_that(mr_mindexer_direct, HasLiterals('#is_metadoc="1', [doc_id2]))
    assert_that(mr_mindexer_direct, HasLiterals('#has_adv_bid="1', [doc_id2]))

    # fast-sku со ставкой мерча: ставим has_adv_bid
    doc_id3 = doc_id_by_offer_id['37']
    assert_that(mr_mindexer_direct, HasLiterals('#is_metadoc="1', [doc_id3]))
    assert_that(mr_mindexer_direct, HasLiterals('#has_adv_bid="1', [doc_id3]))

    # fast-sku, но без ставки: не ставим has_adv_bid
    doc_id4 = doc_id_by_offer_id['26']
    assert_that(mr_mindexer_direct, HasLiterals('#is_metadoc="1', [doc_id4]))
    assert_that(mr_mindexer_direct, HasNoLiterals('#has_adv_bid="1', [doc_id4]))

    # fake-msku оффер, на sku нет оффера со ставкой: не ставим has_adv_bid
    doc_id5 = doc_id_by_offer_id['14']
    assert_that(mr_mindexer_direct, HasLiterals('#is_metadoc="1', [doc_id5]))
    assert_that(mr_mindexer_direct, HasNoLiterals('#has_adv_bid="1', [doc_id5]))

    # fast-sku оффер без ставок, но 1p: ставим has_adv_bid, т.к. на 1p офферы
    # ставка проставляется в репорте (рекомендованная)
    doc_id6 = doc_id_by_offer_id['38']
    assert_that(mr_mindexer_direct, HasLiterals('#is_metadoc="1', [doc_id6]))
    assert_that(mr_mindexer_direct, HasLiterals('#has_adv_bid="1', [doc_id6]))


def test_blue_offer_has_supplier_id(mr_mindexer_direct, doc_id_by_offer_id):
    # Синий офер имеет поисковой литерал supplier_id
    blue_offers = [doc_id_by_offer_id['15'], doc_id_by_offer_id['17']]
    assert_that(mr_mindexer_direct, HasLiterals('#supplier_id="42', blue_offers))

    non_blue_offers = [doc_id_by_offer_id['11'], doc_id_by_offer_id['16']]
    assert_that(mr_mindexer_direct, HasNoLiterals('#supplier_id="42', non_blue_offers))
