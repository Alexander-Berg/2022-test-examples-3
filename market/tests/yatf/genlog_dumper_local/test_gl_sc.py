"""
Проверяем, что данные о параметрах офера сохраняются в файле gl_sc.mmap
"""

import pytest

from hamcrest import assert_that

from market.idx.generation.yatf.resources.genlog_dumper.input_records_proto import (
    InputRecordsProto,
    make_gl_record,
    make_params,
    make_param_entry
)
from market.idx.generation.yatf.resources.genlog_dumper.input_run_options import RunOptions
from market.idx.generation.yatf.test_envs.genlog_dumper import (
    GenlogDumperTestEnv,
    RUN_RESOURCE_NAME,
    OFFERS_RESOURCE_NAME
)


WARE_ID_1 = 'FirstOffer0V7gLLUBANyg'
WARE_ID_2 = 'SecondOffer07gLLUBANyg'


def get_binary_ware_md5(id):
    return (id + '==').decode('base64')


@pytest.fixture(scope="module")
def offers():
    return InputRecordsProto([
        make_gl_record(
            feed_id=1,
            offer_id='abc',
            ware_md5=WARE_ID_1,
            binary_ware_md5=get_binary_ware_md5(WARE_ID_1),
            params_entry=make_params(
                model=2,
                category=3,
                values=[
                    make_param_entry(key=100, num=123.45),
                    make_param_entry(key=101, id=17),
                ]
            )
        ),
        make_gl_record(
            feed_id=1,
            offer_id='def',
            ware_md5=WARE_ID_2,
            binary_ware_md5=get_binary_ware_md5(WARE_ID_2),
            params_entry=make_params(
                model=4,
                category=5,
                values=[
                    make_param_entry(key=101, id=18),
                ]
            )
        ),
    ])


@pytest.fixture(
    scope="module",
    params=[2]
)
def mmap_version(request):
    return request.param


@pytest.yield_fixture(scope="module")
def genlog_dumper(offers, mmap_version):
    gd_resources = {
        RUN_RESOURCE_NAME: RunOptions([
            '--dumper', 'GL_SC',
            '--dumper', 'WARE_MD5',
        ]),
        OFFERS_RESOURCE_NAME: offers
    }
    with GenlogDumperTestEnv(**gd_resources) as genlog_dumper:
        genlog_dumper.execute(gl_sc_mmap_version=mmap_version)
        genlog_dumper.verify()
        yield genlog_dumper


def test_output(genlog_dumper, offers, mmap_version):
    gl_sc = genlog_dumper.gl_sc

    def check_params(params, sample):
        for index, param in enumerate(sample):
            for key, value in param.iteritems():
                assert_that(params[index][key], value)

    params_1 = [{
        'PARAM_ID': 100,
        'VALUE_NUM': '123.45',
    }, {
        'PARAM_ID': 101,
        'VALUE_ID': '17',
    }]
    params_2 = [{
        'PARAM_ID': 101,
        'VALUE_ID': '18',
    }]

    # Идентификатор во второй версии - это номер документа.
    # Порядок добавления документов не гарантирован.
    # Поэтому связь делаем по wareMd5 документа

    ware_md5 = genlog_dumper.ware_md5

    assert_that(len(gl_sc), 2)

    samples = {
        WARE_ID_1: params_1,
        WARE_ID_2: params_2,
    }

    for index in range(2):
        current_sample = samples[ware_md5.get_ware_md5(index)]

        assert_that(len(gl_sc[index]['PARAMS']), len(current_sample))
        check_params(gl_sc[index]['PARAMS'], current_sample)
