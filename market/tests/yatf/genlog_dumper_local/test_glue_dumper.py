# coding: utf-8
import pytest
import os

from hamcrest import assert_that, all_of, equal_to, has_items, has_entries

from market.idx.yatf.matchers.env_matchers import HasOutputFiles, HasNonZeroBytes
from market.idx.yatf.resources.glue_config import GlueConfig
from market.idx.generation.yatf.resources.genlog_dumper.input_records_proto import InputRecordsProto, make_gl_record
from market.idx.generation.yatf.resources.genlog_dumper.input_run_options import RunOptions
from market.idx.generation.yatf.resources.genlog_dumper.glue_output_file import GlueFileResource

from market.idx.generation.yatf.test_envs.genlog_dumper import (
    GenlogDumperTestEnv,
    RUN_RESOURCE_NAME,
    OFFERS_RESOURCE_NAME
)
from market.proto.indexer.GenerationLog_pb2 import Record
from market.proto.common.declarative_index_extension_pb2 import idx_named_doc_field


@pytest.fixture(scope='module')
def offers():
    return [
        make_gl_record(1, '1', glue_fields=[
            {'glue_id': 0, 'uint32_value': 1609},
            {'glue_id': 15, 'uint32_value': 114},
            {'glue_id': 20, 'double_value': 21.1},
            {'glue_id': 128, 'bool_value': True}
        ]),
        make_gl_record(2, '2'),
        make_gl_record(3, '3'),
        make_gl_record(4, '4', glue_fields=[
            {'glue_id': 15, 'uint32_value': 115},
            {'glue_id': 20, 'double_value': 22.1},
            {'glue_id': 128, 'bool_value': False}
        ]),
        make_gl_record(5, '5')
    ]


@pytest.fixture(scope='module')
def glue_config():
    # source_path is not used in genlog_dumper, so i'm allowed to write anything
    return GlueConfig(
    {'Fields': [
        {
            'glue_id': 0,
            'declared_cpp_type': 'UINT32',
            'target_name': 'business_id',
            'is_from_datacamp': True,
            'source_field_path': 'just any bullshit for dumper',
            'use_as_named_field': True
        },
        {
            'glue_id': 15,
            'declared_cpp_type': 'UINT32',
            'target_name': 'mbid',
            'is_from_datacamp': True,
            'source_field_path': 'abc',
            'use_as_named_field': False
        },
        {
            'glue_id': 20,
            'declared_cpp_type': 'DOUBLE',
            'target_name': 'some_value_1',
            'is_from_datacamp': True,
            'source_field_path': 'just any bullshit for dumper',
            'use_as_named_field': True
        },
        {
            'glue_id': 128,
            'declared_cpp_type': 'BOOL',
            'target_name': 'some_value_2',
            'is_from_datacamp': True,
            'source_field_path': 'just any bullshit for dumper',
            'use_as_named_field': True
        }

    ]}, 'glue_config.json')


@pytest.yield_fixture()
def genlog_dumper(offers, glue_config):
    gd_resources = {
        RUN_RESOURCE_NAME: RunOptions([
            '--dumper', 'GLUE_DUMPER',
            '--glue-config-path', glue_config.path
        ]),
        OFFERS_RESOURCE_NAME: InputRecordsProto(offers)
    }
    with GenlogDumperTestEnv(**gd_resources) as env:
        env.execute()
        env.verify()
        yield env


@pytest.yield_fixture()
def expected_fields():
    result = ['business_id', 'feed_id']

    for field in Record.DESCRIPTOR.fields:
        if field.GetOptions().Extensions[idx_named_doc_field]:
            result.append(field.name)

    return result


def test_output_files(genlog_dumper, expected_fields):
    expected_constraints = []
    expected_files = []
    for field in expected_fields:
        filename = field + '.values.fb'
        expected_constraints.append(HasNonZeroBytes(filename))
        expected_files.append(filename)

    expected_constraints.append(HasOutputFiles(set(expected_files)))

    assert_that(
        genlog_dumper,
        all_of(
            *expected_constraints
        )
    )


def test_glue_file_format(genlog_dumper, expected_fields):
    expected = [
        ('business_id', 'TUInt32ValuesViewer'),
        ('feed_id', 'TUInt32ValuesViewer'),
        ('some_value_1', 'TDoubleValuesViewer'),
        ('some_value_2', 'TBoolValuesViewer'),
        ('is_not_tsar', 'TBoolValuesViewer'),
    ]
    for field, file_type in expected:
        filename = field + '.values.fb'
        res = GlueFileResource(file_type=file_type, path=os.path.join(genlog_dumper.output_dir, filename))
        assert_that(res.schema, equal_to(res.file_type))


def test_glue_file_content(genlog_dumper, expected_fields):
    expected = [
        ('business_id', [(0, 1609)]),
        ('feed_id', [(0, 1), (1, 2), (2, 3), (3, 4), (4, 5)]),
        ('some_value_1', [(0, 21.1), (1, 0), (2, 0), (3, 22.1)]),
        ('some_value_2', [(0, True), (1, False), (2, False), (3, False)]),
        ('is_not_tsar', [(0, False), (1, False), (2, False), (3, False)])
    ]
    for field, values in expected:
        filename = field + '.values.fb'
        res = GlueFileResource(path=os.path.join(genlog_dumper.output_dir, filename))

        # к сожалению у нас нет гарантии на порядок в localmode, поэтому просто проверяем наличие значения
        assert_that(res.values, has_items(*[has_entries({'value': v[1]}) for v in values]))
