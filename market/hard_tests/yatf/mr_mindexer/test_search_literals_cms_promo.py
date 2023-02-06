# coding: utf-8

"""Проверяет прокидывание published_on_blue_market до флага синего оффера.
"""

from hamcrest import assert_that, all_of
import pytest

from market.idx.yatf.resources.mbo.cms_promo import CmsPromoPbsn, create_cms_msku_promo, create_cms_model_promo
from market.idx.generation.yatf.test_envs.mr_mindexer import MrMindexerBuildTestEnv, MrMindexerMergeTestEnv
from market.idx.generation.yatf.resources.mr_mindexer.mr_mindexer_helpers import MrMindexerMergeOptions, MrMindexerMergeIndexType

from market.idx.offers.yatf.matchers.offers_indexer.env_matchers import HasLiterals, HasNoLiterals
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


SKU_WITH_PROMO_1_ID = 1
SKU_WITH_PROMO_1_2_ID = 2
SKU_WITHOUT_PROMO_ID = 3
MISSED_SKU_ID = 4       # Этого СКУ нет в индексе

MODEL_WITH_PROMO_3_ID = 10
MODEL_WITH_PROMO_4_ID = 11
GROUP_MODEL_WITH_PROMO_4_ID = 12

OFFER_1_WITH_PROMO_1_ID = 'Offer1WithPromo1zggggg'
OFFER_2_WITH_PROMO_1_ID = 'Offer2WithPromo1zggggg'
SKU_WITH_PROMO_1_OFFER_ID = 'SkuWithPromo1zgggggggg'

OFFER_1_WITH_PROMO_1_2_ID = 'Offer1WithPromo1z2zggg'
SKU_WITH_PROMO_1_2_OFFER_ID = 'SkuWithPromo1z2zgggggg'

OFFER_1_WITHOUT_PROMO_ID = 'Offer1WithoutPromozggg'
SKU_WITHOUT_PROMO_OFFER_ID = 'SkuWithoutPromozgggggg'

WHITE_OFFER_ID = 'WhiteOfferzggggggggggg'
WHITE_OFFER_WITH_PROMO_ID = 'WhiteOfferzffffffffffg'
WHITE_OFFER_GROUP_WITH_PROMO_ID = 'WhiteOfferzddddddddddg'

PROMO_1_NAME = 'Promo1'
# Т.к. поисковый литерал имеет только строчные буквы, изменяем строку, добавляя символ ^ перед символом со сменой регистра
PROMO_1_SEARCH_LITERAL = '^p^romo1'

PROMO_2_NAME = 'РусскоеПромо%#$'
# Сперва обрабатываем смену регистра, затем экранируем символы
PROMO_2_SEARCH_LITERAL = '^\xf0^\xf3\xf1\xf1\xea\xee\xe5^\xef^\xf0\xee\xec\xee%#$'

PROMO_3_NAME = 'promo3'
PROMO_4_NAME = 'promo4'


def create_msku(msku, sku_offer_id, offers):
    sku = default_genlog()
    sku['model_id'] = 1
    sku['is_fake_msku_offer'] = True
    sku['market_sku'] = msku
    sku['offer_id'] = sku_offer_id
    result = [sku]

    for offer_id in offers:
        offer = default_genlog()
        offer['model_id'] = 1
        offer['is_blue_offer'] = True
        offer['market_sku'] = msku
        offer['offer_id'] = offer_id
        result += [offer]
    return result


@pytest.yield_fixture(scope='module')
def genlog_rows():
    result = create_msku(SKU_WITH_PROMO_1_ID, SKU_WITH_PROMO_1_OFFER_ID, [OFFER_1_WITH_PROMO_1_ID, OFFER_2_WITH_PROMO_1_ID])
    result += create_msku(SKU_WITH_PROMO_1_2_ID, SKU_WITH_PROMO_1_2_OFFER_ID, [OFFER_1_WITH_PROMO_1_2_ID])
    result += create_msku(SKU_WITHOUT_PROMO_ID, SKU_WITHOUT_PROMO_OFFER_ID, [OFFER_1_WITHOUT_PROMO_ID])

    white_offer = default_genlog()
    white_offer['model_id'] = 1
    white_offer['offer_id'] = WHITE_OFFER_ID
    white_offer['market_sku'] = SKU_WITH_PROMO_1_ID

    result += [white_offer]

    white_offer_with_promo_simple = default_genlog()
    white_offer_with_promo_simple['model_id'] = MODEL_WITH_PROMO_3_ID
    white_offer_with_promo_simple['offer_id'] = WHITE_OFFER_WITH_PROMO_ID

    white_offer_with_promo_group = default_genlog()
    white_offer_with_promo_group['model_id'] = MODEL_WITH_PROMO_4_ID
    white_offer_with_promo_group['model_id'] = GROUP_MODEL_WITH_PROMO_4_ID
    white_offer_with_promo_group['offer_id'] = WHITE_OFFER_GROUP_WITH_PROMO_ID

    result += [white_offer_with_promo_simple, white_offer_with_promo_group]

    return result


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope='module')
def offers_processor_workflow(yt_server, genlog_table):
    input_table_paths = [genlog_table.get_path()]
    resources = {
        'cms_report_promo_pbsn': CmsPromoPbsn([
            create_cms_msku_promo(PROMO_1_NAME, available_mskus=[SKU_WITH_PROMO_1_ID, SKU_WITH_PROMO_1_2_ID]),
            create_cms_msku_promo(PROMO_2_NAME, available_mskus=[SKU_WITH_PROMO_1_2_ID]),
            create_cms_msku_promo("unused_promo", available_mskus=[MISSED_SKU_ID]),
            create_cms_model_promo(PROMO_3_NAME, available_models=[MODEL_WITH_PROMO_3_ID]),
            create_cms_model_promo(PROMO_4_NAME, available_models=[GROUP_MODEL_WITH_PROMO_4_ID])
        ])
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


@pytest.fixture(scope="module")
def doc_id_by_offer_id(mr_mindexer_direct_arc):
    mapping = {}
    arc = mr_mindexer_direct_arc.outputs['indexarc']
    for i in arc.doc_ids:
        offer_id = arc.load_doc_description(i)['offer_id']
        mapping[offer_id] = i
    return mapping


def test_cms_promo_search_literal(mr_mindexer_direct, mr_mindexer_direct_arc, doc_id_by_offer_id):
    sku1_doc_ids = [
        doc_id_by_offer_id[SKU_WITH_PROMO_1_OFFER_ID],
        doc_id_by_offer_id[OFFER_1_WITH_PROMO_1_ID],
        doc_id_by_offer_id[OFFER_2_WITH_PROMO_1_ID],
    ]

    sku2_doc_ids = [
        doc_id_by_offer_id[SKU_WITH_PROMO_1_2_OFFER_ID],
        doc_id_by_offer_id[OFFER_1_WITH_PROMO_1_2_ID],
    ]

    all_doc_ids = mr_mindexer_direct_arc.outputs['indexarc'].doc_ids

    assert_that(mr_mindexer_direct, all_of(
        # СКУ и его оферы с одной промо акцией
        HasLiterals('#cms_promo="' + PROMO_1_SEARCH_LITERAL, sku1_doc_ids),

        # СКУ и его офер с двумя промо акциями
        HasLiterals('#cms_promo="' + PROMO_1_SEARCH_LITERAL, sku2_doc_ids),
        HasLiterals('#cms_promo="' + PROMO_2_SEARCH_LITERAL, sku2_doc_ids),

        # Для белого офера без промо нет поискового литерала
        HasNoLiterals('#cms_promo', [doc_id_by_offer_id[WHITE_OFFER_ID]]),

        # Белый оффер с промо
        HasLiterals('#cms_promo="' + PROMO_3_NAME, [doc_id_by_offer_id[WHITE_OFFER_WITH_PROMO_ID]]),

        # Нет документов с неиспользуемым промо
        HasNoLiterals('#cms_promo="unused_promo', all_doc_ids)
    ))
