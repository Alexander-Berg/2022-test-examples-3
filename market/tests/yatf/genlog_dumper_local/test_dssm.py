# coding: utf-8

import pytest

from hamcrest import assert_that, all_of

from market.idx.yatf.matchers.env_matchers import HasOutputFiles, HasNotOutputFiles, HasOutputFileSize, HasNonZeroBytes

from market.idx.generation.yatf.resources.genlog_dumper.input_records_proto import InputRecordsProto, make_gl_record
from market.idx.generation.yatf.resources.genlog_dumper.input_run_options import RunOptions
from market.idx.generation.yatf.test_envs.genlog_dumper import (
    GenlogDumperTestEnv,
    RUN_RESOURCE_NAME,
    OFFERS_RESOURCE_NAME
)


DSSM_VECTOR_SIZE_30_ONE_OFFER = 30
DSSM_VECTOR_SIZE_40_ONE_OFFER = 40
DSSM_VECTOR_SIZE_50_ONE_OFFER = 50

DSSM_VECTOR_30_1 = [1 for i in xrange(0, DSSM_VECTOR_SIZE_30_ONE_OFFER)]
DSSM_VECTOR_30_2 = [2 for i in xrange(0, DSSM_VECTOR_SIZE_30_ONE_OFFER)]
DSSM_VECTOR_30_3 = [3 for i in xrange(0, DSSM_VECTOR_SIZE_30_ONE_OFFER)]

DSSM_VECTOR_40_1 = [1 for i in xrange(0, DSSM_VECTOR_SIZE_40_ONE_OFFER)]
DSSM_VECTOR_40_2 = [2 for i in xrange(0, DSSM_VECTOR_SIZE_40_ONE_OFFER)]
DSSM_VECTOR_40_3 = [3 for i in xrange(0, DSSM_VECTOR_SIZE_40_ONE_OFFER)]

DSSM_VECTOR_50_1 = [1 for i in xrange(0, DSSM_VECTOR_SIZE_50_ONE_OFFER)]
DSSM_VECTOR_50_2 = [2 for i in xrange(0, DSSM_VECTOR_SIZE_50_ONE_OFFER)]
DSSM_VECTOR_50_3 = [3 for i in xrange(0, DSSM_VECTOR_SIZE_50_ONE_OFFER)]


def MakeString(array):
    res = ''
    for c in array:
        res += chr(c)
    return res


@pytest.fixture(scope="module")
def offers(request):
    return [
        make_gl_record(
            hard2_dssm_embedding_str=MakeString(DSSM_VECTOR_50_1),
            reformulation_dssm_embedding_str=MakeString(DSSM_VECTOR_50_1),
            bert_dssm_embedding_str=MakeString(DSSM_VECTOR_50_1),
            super_embed_dssm_embedding_str=MakeString(DSSM_VECTOR_50_1),
            assessment_binary_dssm_embedding_str=MakeString(DSSM_VECTOR_30_1),
            assessment_dssm_embedding_str=MakeString(DSSM_VECTOR_30_1),
            click_dssm_embedding_str=MakeString(DSSM_VECTOR_40_1),
            has_cpa_click_dssm_embedding_str=MakeString(DSSM_VECTOR_40_1),
            cpa_dssm_embedding_str=MakeString(DSSM_VECTOR_40_1),
            billed_cpa_dssm_embedding_str=MakeString(DSSM_VECTOR_40_1),
        ),
        make_gl_record(
            hard2_dssm_embedding_str=MakeString(DSSM_VECTOR_50_2),
            reformulation_dssm_embedding_str=MakeString(DSSM_VECTOR_50_2),
            bert_dssm_embedding_str=MakeString(DSSM_VECTOR_50_2),
            super_embed_dssm_embedding_str=MakeString(DSSM_VECTOR_50_2),
            assessment_binary_dssm_embedding_str=MakeString(DSSM_VECTOR_30_2),
            assessment_dssm_embedding_str=MakeString(DSSM_VECTOR_30_2),
            click_dssm_embedding_str=MakeString(DSSM_VECTOR_40_2),
            has_cpa_click_dssm_embedding_str=MakeString(DSSM_VECTOR_40_2),
            cpa_dssm_embedding_str=MakeString(DSSM_VECTOR_40_2),
            billed_cpa_dssm_embedding_str=MakeString(DSSM_VECTOR_40_2),
        ),
        make_gl_record(
            hard2_dssm_embedding_str=MakeString(DSSM_VECTOR_50_3),
            reformulation_dssm_embedding_str=MakeString(DSSM_VECTOR_50_3),
            bert_dssm_embedding_str=MakeString(DSSM_VECTOR_50_3),
            super_embed_dssm_embedding_str=MakeString(DSSM_VECTOR_50_3),
            assessment_binary_dssm_embedding_str=MakeString(DSSM_VECTOR_30_3),
            assessment_dssm_embedding_str=MakeString(DSSM_VECTOR_30_3),
            click_dssm_embedding_str=MakeString(DSSM_VECTOR_40_3),
            has_cpa_click_dssm_embedding_str=MakeString(DSSM_VECTOR_40_3),
            cpa_dssm_embedding_str=MakeString(DSSM_VECTOR_40_3),
            billed_cpa_dssm_embedding_str=MakeString(DSSM_VECTOR_40_3),
        ),
    ]


@pytest.yield_fixture(scope="module")
def genlog_dumper(offers):
    gd_resources = {
        RUN_RESOURCE_NAME: RunOptions([
            '--dumper', 'DSSM',
        ]),
        OFFERS_RESOURCE_NAME: InputRecordsProto(offers)
    }
    with GenlogDumperTestEnv(**gd_resources) as env:
        env.execute()
        env.verify()
        yield env


def test_new_dssm_disabled(genlog_dumper, offers):
    """
    Проверяем, что по умолчанию генерация hard2 dssm отключена
    """
    assert_that(
        genlog_dumper,
        HasNotOutputFiles({'hard2_dssm.values.binary', 'reformulation_dssm.values.binary', 'bert_dssm.values.binary',
                           'omni.wad', 'assessment_binary.values.binary', 'assessment.values.binary',
                           'click.values.binary', 'has_cpa_click.values.binary', 'cpa.values.binary',
                           'billed_cpa.values.binary'}),
        'new dssm binary files disabled and not generated'
    )


@pytest.yield_fixture(scope="module")
def genlog_dumper_new_dssm(offers):
    gd_resources = {
        RUN_RESOURCE_NAME: RunOptions([
            '--dumper', 'DSSM',
        ]),
        OFFERS_RESOURCE_NAME: InputRecordsProto(offers)
    }
    with GenlogDumperTestEnv(**gd_resources) as env:
        env.execute(
            enable_hard2_dssm=True,
            enable_reformulation_dssm=True,
            enable_bert=True,
            enable_super_embed=True,
            enable_assessment_binary=True,
            enable_assessment=True,
            enable_click=True,
            enable_has_cpa_click=True,
            enable_cpa=True,
            enable_billed_cpa=True
        )
        env.verify()
        yield env


def test_new_dssm_enabled(genlog_dumper_new_dssm, offers):
    """
    Проверяем, что при включенной настройке genlog_dumper генерит не пустой dssm binary файл нужного размера
    """
    assert_that(
        genlog_dumper_new_dssm,
        all_of(
            HasOutputFiles({'hard2_dssm.values.binary', 'reformulation_dssm.values.binary'}),
            HasOutputFileSize('hard2_dssm.values.binary', DSSM_VECTOR_SIZE_50_ONE_OFFER * len(offers)),
            HasNonZeroBytes('hard2_dssm.values.binary'),
            HasOutputFileSize('reformulation_dssm.values.binary', DSSM_VECTOR_SIZE_50_ONE_OFFER * len(offers)),
            HasNonZeroBytes('reformulation_dssm.values.binary'),
            HasOutputFileSize('bert_dssm.values.binary', DSSM_VECTOR_SIZE_50_ONE_OFFER * len(offers)),
            HasNonZeroBytes('bert_dssm.values.binary'),
            HasNonZeroBytes('omni.wad'),
            HasOutputFileSize('assessment_binary.values.binary', DSSM_VECTOR_SIZE_30_ONE_OFFER * len(offers)),
            HasNonZeroBytes('assessment_binary.values.binary'),
            HasOutputFileSize('assessment.values.binary', DSSM_VECTOR_SIZE_30_ONE_OFFER * len(offers)),
            HasNonZeroBytes('assessment.values.binary'),
            HasOutputFileSize('click.values.binary', DSSM_VECTOR_SIZE_40_ONE_OFFER * len(offers)),
            HasNonZeroBytes('click.values.binary'),
            HasOutputFileSize('has_cpa_click.values.binary', DSSM_VECTOR_SIZE_40_ONE_OFFER * len(offers)),
            HasNonZeroBytes('has_cpa_click.values.binary'),
            HasOutputFileSize('cpa.values.binary', DSSM_VECTOR_SIZE_40_ONE_OFFER * len(offers)),
            HasNonZeroBytes('cpa.values.binary'),
            HasOutputFileSize('billed_cpa.values.binary', DSSM_VECTOR_SIZE_40_ONE_OFFER * len(offers)),
            HasNonZeroBytes('billed_cpa.values.binary'),
        ),
        u'новые dssm-ки сгенерены, нужного размера и внутри что-то есть'
    )
