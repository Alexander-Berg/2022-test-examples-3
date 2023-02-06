# coding: utf-8

import pytest

from hamcrest import assert_that, is_not

from market.idx.generation.yatf.resources.genlog_dumper.input_records_proto import InputRecordsProto, make_gl_record
from market.idx.generation.yatf.resources.genlog_dumper.input_run_options import RunOptions
from market.idx.generation.yatf.test_envs.genlog_dumper import (
    GenlogDumperTestEnv,
    RUN_RESOURCE_NAME,
    OFFERS_RESOURCE_NAME
)

from market.idx.yatf.resources.model_ids import ModelIds
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable

from market.idx.offers.yatf.matchers.offers_indexer.env_matchers import HasBlueOfferModel
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


@pytest.fixture(scope='module')
def genlog_rows():
    return [
        default_genlog(
            # white offer, white model
            is_blue_offer=False,
            model_id=13924,
        ),
        default_genlog(
            # blue offer, white model
            is_blue_offer=True,
            model_id=13925,
        ),
        default_genlog(
            # white offer, blue model
            is_blue_offer=False,
            model_id=13926,
        ),
        default_genlog(
            # blue offer, blue model
            is_blue_offer=True,
            model_id=13927,
        ),
        default_genlog(
            # fake blue offer, blue model
            is_blue_offer=True,
            is_fake_msku_offer=True,
            model_id=13928,
        ),
        ]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.fixture(scope="module")
def model_ids():
    return ModelIds([13924, 13925], blue_ids=[13926, 13927, 13928])


@pytest.yield_fixture(scope="module")
def offers_processor_workflow(model_ids, yt_server, genlog_table):
    input_table_paths = [genlog_table.get_path()]

    resources = {
        'model_ids': model_ids,
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
def genlog_dumper(offers_processor_workflow):
    records = []
    for offer in offers_processor_workflow.genlog_dicts:
        records.append(make_gl_record(
            flags=offer.get('flags'),
            model_published_on_blue_market=offer.get('model_published_on_blue_market'),
            model_id=offer.get('model_id'),
        ))

    gd_resources = {
        RUN_RESOURCE_NAME: RunOptions([
            '--dumper', 'BLUE_OFFER_MODELS',
        ]),
        OFFERS_RESOURCE_NAME: InputRecordsProto(records)
    }
    with GenlogDumperTestEnv(**gd_resources) as env:
        env.execute()
        env.verify()
        yield env


def test_blue_offer_models_txt(genlog_dumper):
    '''Проверяем, что в blue_offer_models.txt попадают опубликованные на синем
    модели синих офферов
    '''

    expected = [13927]
    not_expected = [13924, 13925, 13926, 13928]

    for model_id in expected:
        assert_that(
            genlog_dumper,
            HasBlueOfferModel(model_id),
            u'blue_offer_models.txt contains expected model'
            )
    for model_id in not_expected:
        assert_that(
            genlog_dumper,
            is_not(HasBlueOfferModel(model_id)),
            u'blue_offer_models.txt doesn\'t contain unexpected model'
            )
