import yatest.common

import os
import os.path

import pytest


class NamedStruct:
    def __init__(self, **kwargs):
        self.__dict__.update(kwargs)


@pytest.fixture(scope='module')
def jsons():
    return NamedStruct(
        rubric=os.path.join(os.getcwd(), 'data/rubric.json'),
        feature=os.path.join(os.getcwd(), 'data/feature.json'),
        feature_enum_value=os.path.join(os.getcwd(), 'data/feature_enum_value.json'),
        features2_fast=os.path.join(os.getcwd(), 'data/features2_fast.json'),
        chain=os.path.join(os.getcwd(), 'data/chain.json'),
        feature_rubric_triggers=os.path.join(os.getcwd(), 'data/feature_rubric_triggers.json'),
        tag_tree=yatest.common.source_path('search/geo/tools/wizard/ppo/gzt_builder/tests/test_tag_tree.json'))


def do_test(arg_list, dir_name):
    try:
        os.mkdir(dir_name)
    except OSError:
        pass

    path_to_binary = yatest.common.binary_path('search/geo/tools/wizard/ppo/gzt_builder/gzt_builder')
    path_to_chains = yatest.common.source_path('search/geo/tools/wizard/ppo/gzt_builder/good_chain_ids.txt')

    os.chdir(dir_name)
    yatest.common.execute([path_to_binary, '--chain-ids', path_to_chains] + arg_list)
    os.chdir('..')

    return [
        yatest.common.canonical_file(os.path.join(dir_name, 'rubrics.gzt')),
        yatest.common.canonical_file(os.path.join(dir_name, 'features.gzt')),
        yatest.common.canonical_file(os.path.join(dir_name, 'rubric_features.txt')),
        yatest.common.canonical_file(os.path.join(dir_name, 'rubric_filters.txt')),
        yatest.common.canonical_file(os.path.join(dir_name, 'chains.gzt'))]


def test_normal(jsons):
    return do_test(
        [
            '--rubric', jsons.rubric,
            '--feature', jsons.feature,
            '--feature-enum-value', jsons.feature_enum_value,
            '--chain', jsons.chain],
        'data_normal')


def test_fast_features(jsons):
    return do_test(
        [
            '--rubric', jsons.rubric,
            '--feature', jsons.feature,
            '--feature-enum-value', jsons.feature_enum_value,
            '--chain', jsons.chain,
            '--features2-fast', jsons.features2_fast],
        'data_fast_features')


def test_lang_info(jsons):
    return do_test(
        [
            '-l',
            '--rubric', jsons.rubric,
            '--feature', jsons.feature,
            '--feature-enum-value', jsons.feature_enum_value,
            '--chain', jsons.chain],
        'data_lang_info')


def test_fast_features_lang_info(jsons):
    return do_test(
        [
            '-l',
            '--rubric', jsons.rubric,
            '--feature', jsons.feature,
            '--feature-enum-value', jsons.feature_enum_value,
            '--chain', jsons.chain,
            '--features2-fast', jsons.features2_fast],
        'data_fast_features_lang_info')


def test_triggers(jsons):
    return do_test(
        [
            '-l',
            '--rubric', jsons.rubric,
            '--feature', jsons.feature,
            '--feature-enum-value', jsons.feature_enum_value,
            '--chain', jsons.chain,
            '--feature-rubric-triggers', jsons.feature_rubric_triggers],
        'data_triggers')


def test_root_article(jsons):
    return do_test(
        [
            '-r',
            '--rubric', jsons.rubric,
            '--feature', jsons.feature,
            '--feature-enum-value', jsons.feature_enum_value,
            '--chain', jsons.chain],
        'data_root_article')


def test_tag_tree(jsons):
    return do_test(
        [
            '--rubric', jsons.rubric,
            '--feature', jsons.feature,
            '--feature-enum-value', jsons.feature_enum_value,
            '--chain', jsons.chain,
            '--tag-tree', jsons.tag_tree],
        'tag_tree')
