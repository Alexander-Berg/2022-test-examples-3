# coding: utf-8

"""
Проверяется корректность работы утилиты market/idx/generation/dssm_mapper,
цель которой -- запускать DSSM на параметрах офферов (а именно, на
параметрах url, title и description). Эта утилита запускается после
этапа offers-processor и принимает на вход genlog. На выходе получается
таблица, схема которой повторяет схему таблиц genlog'а, но заполнены
лишь поля 'sequence_number' и 'value' (сериализованный протобуф
GenerationLog::Record), у которого заполнены поля title, url,
repeated-поле dssm_embedding и repeated-поле hard_dssm_ebmedding.

Также она умеет работать с маппингами модельных id (такие маппинги
содержат информацию о переносах модельных id в случае удаления/разделения
моделей), что необходимо учитывать при запуске DSSM для MSKU.

На данный момент у DSSM есть одна особенность: они обучены на URL'ах
вида 'http://market.yandex.ru/product/<model-id>' (см. MARKETINDEXER-30606),
поэтому для MSKU вычисления производятся для URL'ов соответствующих моделей.
"""

import pytest

from hamcrest import assert_that, equal_to, all_of, has_entries, has_item, has_length, only_contains, is_not
from market.idx.generation.yatf.test_envs.dssm_mapper import DssmMapperTestEnv
from market.idx.models.yatf.resources.model_transitions.model_transitions_pb import ModelTransitionsPbInput
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog, genererate_default_pictures
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.resources.yt_table_resource import YtFileResource
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from yt.wrapper.ypath import ypath_join


_OFFER_DATA = {
    'msku_1': {
        'title': 'msku_1',
        'feed_id': 1,
        'model_id': 1,
        'is_fake_msku_offer': True,
        'url': 'https://beru.ru/product/1'
    },
    'msku_2': {
        'title': 'msku_2',
        'feed_id': 2,
        'model_id': 2,
        'is_fake_msku_offer': True,
        'url': 'https://beru.ru/product/2',
    },
    'offer_1': {
        'title': 'offer_1',
        'feed_id': 3,
        'url': 'http://rogaikopyta.ru/tovar/100500',
        'model_title_ext': 'model title will be ignored because no model_id/cluster_id',
    },
    'offer_2': {
        'title': 'offer_2',
        'feed_id': 3,
        'cluster_id': 3,
        'url': 'http://rogaikopyta.ru/tovar/100501',
        'pictures': genererate_default_pictures(),
        'model_title_ext': 'model title ok',
    },
    'offer_3': {
        'title': 'offer_3',  # no model title
        'feed_id': 3,
        'model_id': 4,
        'url': 'http://rogaikopyta.ru/tovar/100502',
    },
    'blue_offer_1': {
        'title': 'blue_offer_1',
        'feed_id': 5,
        'model_id': 5,
        'url': 'https://pokupki.market.yandex.ru/product/5',
        'market_sku': 5,
    },
    'blue_offer_2': {
        'title': 'blue_offer_2',
        'feed_id': 6,
        'model_id': 6,
        'url': 'https://market.yandex.ru/product/6?sku=6',
        'market_sku': 6,
    },
}


_OFFER_DATA_NORMAL_TITLES = {
    'msku_1': {
        'title': 'Бра VELE LUCE Aspen VL5163W01.',
        'feed_id': 1,
        'model_id': 1,
        'is_fake_msku_offer': True,
        'url': 'https://beru.ru/product/1'
    },
    'msku_2': {
        'title': 'Анемостат VENTS с фланцем А 80 ВРФ',
        'feed_id': 2,
        'model_id': 2,
        'is_fake_msku_offer': True,
        'url': 'https://beru.ru/product/2',
    },
    'offer_1': {
        'title': 'Настольная игра ZHORYA Ф93880 Забрось мяч',
        'feed_id': 3,
        'url': 'http://rogaikopyta.ru/tovar/100500',
    },
}


def _list_dssm_data(output_dssm_table):
    for data in output_dssm_table:
        yield data


@pytest.fixture(scope="module")
def genlog_rows():
    return [
        default_genlog(**offer_data)
        for offer_data in _OFFER_DATA.values()
    ]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), 'genlog_rows', '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.fixture(scope="module")
def genlog_rows_with_normal_titles():
    return [
        default_genlog(**offer_data)
        for offer_data in _OFFER_DATA_NORMAL_TITLES.values()
    ]


@pytest.fixture(scope="module")
def genlog_table_with_normal_titles(yt_server, genlog_rows_with_normal_titles):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), 'genlog_rows_with_normal_titles', '0001'), genlog_rows_with_normal_titles)
    genlog_table.dump()
    return genlog_table


@pytest.fixture(scope="module")
def offers_processor_workflow(yt_server, genlog_table):
    input_table_paths = [genlog_table.get_path()]

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths
    ) as env:
        env.execute()
        env.verify()
        yield env


@pytest.fixture(scope="module")
def offers_with_normal_titles_processor_workflow(yt_server, genlog_table_with_normal_titles):
    input_table_paths = [genlog_table_with_normal_titles.get_path()]

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths
    ) as env:
        env.execute()
        env.verify()
        yield env


@pytest.yield_fixture(scope="module")
def dssm_mapper_workflow(yt_server, offers_processor_workflow):
    output_table_path = ypath_join(get_yt_prefix(), 'dssm_0')

    resources = {
        'genlog': YtFileResource(yt_server, offers_processor_workflow.output_table),
        'output': YtFileResource(yt_server, output_table_path),
    }

    with DssmMapperTestEnv(yt_server, **resources) as env:
        env.execute()
        env.verify()
        yield env


@pytest.yield_fixture(scope="module")
def dssm_mapper_workflow_with_transitions(yt_server, offers_processor_workflow):
    output_table_path = ypath_join(get_yt_prefix(), 'dssm_1')
    new_id_to_old_id = {
        2: 20
    }

    resources = {
        'genlog': YtFileResource(yt_server, offers_processor_workflow.output_table),
        'model_transitions': ModelTransitionsPbInput(new_id_to_old_id=new_id_to_old_id),
        'output': YtFileResource(yt_server, output_table_path),
    }

    with DssmMapperTestEnv(yt_server, **resources) as env:
        env.execute()
        env.verify()
        yield env


@pytest.yield_fixture(scope="module")
def dssm_mapper_workflow_hard2_dssm(yt_server, offers_processor_workflow):
    output_table_path = ypath_join(get_yt_prefix(), 'dssm_2')

    resources = {
        'genlog': YtFileResource(yt_server, offers_processor_workflow.output_table),
        'output': YtFileResource(yt_server, output_table_path),
    }

    with DssmMapperTestEnv(yt_server, **resources) as env:
        env.execute(enable_hard2_dssm=True, enable_reformulation_dssm=True, enable_bert=True,
                    enable_super_embed=True, enable_assessment_binary=True, enable_assessment=True,
                    enable_click=True, enable_has_cpa_click=True, enable_cpa=True,
                    enable_billed_cpa=True)
        env.verify()
        yield env


@pytest.yield_fixture(scope="module")
def dssm_mapper_workflow_click_sim(yt_server, offers_processor_workflow):
    output_table_path = ypath_join(get_yt_prefix(), 'dssm_3')

    resources = {
        'genlog': YtFileResource(yt_server, offers_processor_workflow.output_table),
        'output': YtFileResource(yt_server, output_table_path),
    }

    with DssmMapperTestEnv(yt_server, **resources) as env:
        env.execute(enable_click_sim=True)
        env.verify()
        yield env


@pytest.yield_fixture(scope="module")
def dssm_mapper_workflow_bm_categories(yt_server, offers_with_normal_titles_processor_workflow):
    output_table_path = ypath_join(get_yt_prefix(), 'bm_categories')

    resources = {
        'genlog': YtFileResource(yt_server, offers_with_normal_titles_processor_workflow.output_table),
        'output': YtFileResource(yt_server, output_table_path),
    }

    with DssmMapperTestEnv(yt_server, **resources) as env:
        env.execute(enable_catalogia=True)
        env.verify()
        yield env


def test_mapper_has_output(dssm_mapper_workflow):
    """
    Проверяем, что вычисления производятся для всех офферов.
    """
    assert_that(len(dssm_mapper_workflow.output_dssm_table), equal_to(7))


def test_mapper_has_correct_urls(dssm_mapper_workflow):
    """
    Проверяем, что URL'ы обрабатываются корректно: для MSKU они
    меняются на market.yandex.*, а для всех остальных остаются
    без изменений.
    """
    expected = {
        'msku_1': 'http://market.yandex.ru/product/1',
        'msku_2': 'http://market.yandex.ru/product/2',
        'offer_1': 'http://rogaikopyta.ru/tovar/100500',
        'offer_2': 'http://rogaikopyta.ru/tovar/100501',
        'offer_3': 'http://rogaikopyta.ru/tovar/100502',
        'blue_offer_1': 'https://beru.ru/product/blue-offer-1/5',
        'blue_offer_2': 'https://beru.ru/product/blue-offer-2/6',
    }

    result = {
        data["title"]: data["url"] for data in _list_dssm_data(dssm_mapper_workflow.output_dssm_table)
    }

    assert_that(result, equal_to(expected))


def _get_hard2_dssm_data(list_dssm_data):
    result = []
    for data in list_dssm_data:
        result.append({
            'title': data["title"],
            'dssm_host': data["dssm_host"],
            'dssm_path': data["dssm_path"],
            'hard2_dssm_embedding': data["hard2_dssm_embedding"],
            'model_title_ext': data["model_title_ext"],
        })
    return result


def test_mapper_has_not_hard2_dssm_data(dssm_mapper_workflow):
    """
    Проверяем, что по умолчанию генерация hard2 dssm отключена
    """
    expected = [
        {'title': 'msku_1', 'hard2_dssm_embedding': []},
        {'title': 'msku_2', 'hard2_dssm_embedding': []},
        {'title': 'offer_1', 'hard2_dssm_embedding': []},
        {'title': 'offer_2', 'hard2_dssm_embedding': []},
        {'title': 'offer_3', 'hard2_dssm_embedding': []},
        {'title': 'blue_offer_1', 'hard2_dssm_embedding': []},
        {'title': 'blue_offer_2', 'hard2_dssm_embedding': []},
    ]

    assert_that(
        _get_hard2_dssm_data(_list_dssm_data(dssm_mapper_workflow.output_dssm_table)),
        all_of(
            *[has_item(has_entries(doc)) for doc in expected]
        ),
        "no expected (empty) hard2 dssm data"
    )


def test_mapper_has_not_reformulation_dssm_data(dssm_mapper_workflow):
    """
    Проверяем, что по умолчанию генерация reformulation dssm отключена
    """
    expected = {
        'http://market.yandex.ru/product/1': [],
        'http://market.yandex.ru/product/2': [],
        'http://rogaikopyta.ru/tovar/100500': [],
        'http://rogaikopyta.ru/tovar/100501': [],
        'http://rogaikopyta.ru/tovar/100502': [],
        'https://beru.ru/product/blue-offer-1/5': [],
        'https://beru.ru/product/blue-offer-2/6': [],
    }

    result = {
        data["url"]: data["reformulation_dssm_embedding"] for data in _list_dssm_data(dssm_mapper_workflow.output_dssm_table)
    }
    assert_that(result, equal_to(expected))


def test_mapper_has_output_with_transitions(dssm_mapper_workflow_with_transitions):
    """
    Проверяем, что вычисления производятся для всех офферов, даже если есть маппинги.
    """
    assert_that(len(dssm_mapper_workflow_with_transitions.output_dssm_table), equal_to(7))


def test_mapper_has_correct_urls_with_transitions(dssm_mapper_workflow_with_transitions):
    """
    Проверяем, что URL'ы обрабатываются корректно: для MSKU они
    меняются на market.yandex.*, а для всех остальных остаются
    без изменений, но также проверяем, что теперь учитываюся
    маппинги моделей: "старая" модель с id=20 теперь стала
    моделью с id=2, поэтому при вычислениях надо использовать
    старый URL, соответствующий модели с id=20.
    """
    expected = {
        'msku_1': 'http://market.yandex.ru/product/1',
        'msku_2': 'http://market.yandex.ru/product/20',  # Now we have a mapping: 2 -> 20
        'offer_1': 'http://rogaikopyta.ru/tovar/100500',
        'offer_2': 'http://rogaikopyta.ru/tovar/100501',
        'offer_3': 'http://rogaikopyta.ru/tovar/100502',
        'blue_offer_1': 'https://beru.ru/product/blue-offer-1/5',
        'blue_offer_2': 'https://beru.ru/product/blue-offer-2/6',
    }

    result = {
        data["title"]: data["url"] for data in _list_dssm_data(dssm_mapper_workflow_with_transitions.output_dssm_table)
    }

    assert_that(result, equal_to(expected))


def test_mapper_has_click_sim(dssm_mapper_workflow_click_sim):
    """
    Проверяем, что по title вытаскивается корректный click similarity- данные.
    В тестом trie лежат строки, которые совпадают с title документа.
    """
    expected = {
        'msku_1': 'msku_1',
        'msku_2': 'msku_2',
        'offer_1': 'offer_1',
        'offer_2': 'offer_2',
        'offer_3': 'offer_3',
        'blue_offer_1': 'blue_offer_1',
        'blue_offer_2': None,
    }

    result = {
        data["title"]: data["click_sim_vector"] for data in _list_dssm_data(dssm_mapper_workflow_click_sim.output_dssm_table)
    }

    assert_that(result, equal_to(expected))


def test_mapper_has_output_hard2_dssm(dssm_mapper_workflow_hard2_dssm):
    """
    Проверяем, что вычисления производятся для всех офферов, даже если есть маппинги.
    """
    assert_that(len(dssm_mapper_workflow_hard2_dssm.output_dssm_table), equal_to(7))


def test_mapper_has_hard2_dssm_data(dssm_mapper_workflow_hard2_dssm):
    """
    Проверяем, что при включенной настройке рассчитываются корректные данные для hard2 dssm
    """
    for data in _list_dssm_data(dssm_mapper_workflow_hard2_dssm.output_dssm_table):
        assert_that(data["hard2_dssm_embedding_str"], has_length(50))
        assert_that(data["hard2_dssm_embedding_str"], is_not(only_contains(0)))


def test_mapper_has_reformulation_dssm_data(dssm_mapper_workflow_hard2_dssm):
    """
    Проверяем, что при включенной настройке рассчитываются корректные данные для reformulation dssm
    """
    for data in _list_dssm_data(dssm_mapper_workflow_hard2_dssm.output_dssm_table):
        assert_that(data["reformulation_dssm_embedding_str"], has_length(50))
        assert_that(data["reformulation_dssm_embedding_str"], is_not(only_contains(0)))


def test_mapper_has_bert_dssm_data(dssm_mapper_workflow_hard2_dssm):
    """
    Проверяем, что при включенной настройке рассчитываются корректные данные для reformulation dssm
    """
    for data in _list_dssm_data(dssm_mapper_workflow_hard2_dssm.output_dssm_table):
        assert_that(data["bert_dssm_embedding_str"], has_length(50))
        assert_that(data["bert_dssm_embedding_str"], is_not(only_contains(0)))


def test_mapper_has_super_embed_dssm_data(dssm_mapper_workflow_hard2_dssm):
    """
    Проверяем, что при включенной настройке рассчитываются корректные данные для super_embed dssm
    """
    for data in _list_dssm_data(dssm_mapper_workflow_hard2_dssm.output_dssm_table):
        assert_that(data["super_embed_dssm_embedding_str"], has_length(50))
        assert_that(data["super_embed_dssm_embedding_str"], is_not(only_contains(0)))


def test_mapper_has_assessment_binary_dssm_data(dssm_mapper_workflow_hard2_dssm):
    """
    Проверяем, что при включенной настройке рассчитываются корректные данные для assessment_binary dssm
    """
    for data in _list_dssm_data(dssm_mapper_workflow_hard2_dssm.output_dssm_table):
        assert_that(data["assessment_binary_dssm_embedding_str"], has_length(30))
        assert_that(data["assessment_binary_dssm_embedding_str"], is_not(only_contains(0)))


def test_mapper_has_assessment_dssm_data(dssm_mapper_workflow_hard2_dssm):
    """
    Проверяем, что при включенной настройке рассчитываются корректные данные для assessment dssm
    """
    for data in _list_dssm_data(dssm_mapper_workflow_hard2_dssm.output_dssm_table):
        assert_that(data["assessment_dssm_embedding_str"], has_length(30))
        assert_that(data["assessment_dssm_embedding_str"], is_not(only_contains(0)))


def test_mapper_has_click_dssm_data(dssm_mapper_workflow_hard2_dssm):
    """
    Проверяем, что при включенной настройке рассчитываются корректные данные для click dssm
    """
    for data in _list_dssm_data(dssm_mapper_workflow_hard2_dssm.output_dssm_table):
        assert_that(data["click_dssm_embedding_str"], has_length(40))
        assert_that(data["click_dssm_embedding_str"], is_not(only_contains(0)))


def test_mapper_has_has_cpa_click_dssm_data(dssm_mapper_workflow_hard2_dssm):
    """
    Проверяем, что при включенной настройке рассчитываются корректные данные для has_cpa_click dssm
    """
    for data in _list_dssm_data(dssm_mapper_workflow_hard2_dssm.output_dssm_table):
        assert_that(data["has_cpa_click_dssm_embedding_str"], has_length(40))
        assert_that(data["has_cpa_click_dssm_embedding_str"], is_not(only_contains(0)))


def test_mapper_has_cpa_dssm_data(dssm_mapper_workflow_hard2_dssm):
    """
    Проверяем, что при включенной настройке рассчитываются корректные данные для cpa dssm
    """
    for data in _list_dssm_data(dssm_mapper_workflow_hard2_dssm.output_dssm_table):
        assert_that(data["cpa_dssm_embedding_str"], has_length(40))
        assert_that(data["cpa_dssm_embedding_str"], is_not(only_contains(0)))


def test_mapper_has_billed_cpa_dssm_data(dssm_mapper_workflow_hard2_dssm):
    """
    Проверяем, что при включенной настройке рассчитываются корректные данные для billed_cpa dssm
    """
    for data in _list_dssm_data(dssm_mapper_workflow_hard2_dssm.output_dssm_table):
        assert_that(data["billed_cpa_dssm_embedding_str"], has_length(40))
        assert_that(data["billed_cpa_dssm_embedding_str"], is_not(only_contains(0)))


def test_mapper_dssm_query(dssm_mapper_workflow_hard2_dssm):
    """
    Проверяем урлы и тайтлы запросов в hard2 и reformulation дссм
    """
    expected = [
        {'title': 'msku_1',  'dssm_host': 'https://market.yandex.ru', 'dssm_path': '/product/1/', 'model_title_ext': 'msku_1'},
        {'title': 'msku_2',  'dssm_host': 'https://market.yandex.ru', 'dssm_path': '/product/2/', 'model_title_ext': 'msku_2'},
        {'title': 'offer_1', 'dssm_host': 'https://rogaikopyta.ru', 'dssm_path': '/tovar/100500/', 'model_title_ext': 'offer_1'},
        {'title': 'offer_2', 'dssm_host': 'https://market.yandex.ru', 'dssm_path': '/product/3/', 'model_title_ext': 'model title ok'},
        {'title': 'offer_3', 'dssm_host': 'https://market.yandex.ru', 'dssm_path': '/product/4/', 'model_title_ext': 'offer_3'},
        {'title': 'blue_offer_1', 'dssm_host': 'https://market.yandex.ru', 'dssm_path': '/product/5/', 'model_title_ext': 'blue_offer_1'},
        {'title': 'blue_offer_2', 'dssm_host': 'https://market.yandex.ru', 'dssm_path': '/product/6/', 'model_title_ext': 'blue_offer_2'},
    ]

    actual = _get_hard2_dssm_data(_list_dssm_data(dssm_mapper_workflow_hard2_dssm.output_dssm_table))
    assert_that(
        actual,
        all_of(
            *[has_item(has_entries(doc)) for doc in expected]
        ),
        "no expected hard2 dssm data"
    )


def test_mapper_has_bm_categories(dssm_mapper_workflow_bm_categories):
    """
    Проверяем, что по title вытаскивается корректный набор категорий.
    """
    expected = {
        'Бра VELE LUCE Aspen VL5163W01.': [200007479],  # Светильники
        'Анемостат VENTS с фланцем А 80 ВРФ': [200006182],  # Арматура промышленная трубопроводная
        'Настольная игра ZHORYA Ф93880 Забрось мяч': [200003752],  # Игры
    }

    result = {
        data["title"]: data["bm_categories"] for data in _list_dssm_data(dssm_mapper_workflow_bm_categories.output_dssm_table)
    }

    assert_that(result, equal_to(expected))
