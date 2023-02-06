# coding=utf-8
import os

import yatest.common

from search.wizard.entitysearch.tools.es_hook_notifier.lib import fs_manipulate

# test name: (input, extra cgi, print-path)
subtestsProps = {
    'top1000.names.ru': ('top1000ru', 'wizextra=ent_today=2020-01-17%3Bentlang=ru', 'base_info/title,base_info/name'),
    'top1000.names.ua': ('top1000ua', 'wizextra=ent_today=2020-01-17%3Bentlang=uk', 'base_info/title,base_info/name'),
    'top1000.names.tr': ('top1000tr', 'wizextra=ent_today=2020-01-17%3Bentlang=tr', 'base_info/title,base_info/name'),
    'top1000.facts.ru': ('top1000ru', 'wizextra=ent_today=2020-01-17%3Bentlang=ru', 'facts'),
    'top1000.facts.ua': ('top1000ua', 'wizextra=ent_today=2020-01-17%3Bentlang=uk', 'facts'),
    'top1000.facts.tr': ('top1000tr', 'wizextra=ent_today=2020-01-17%3Bentlang=tr', 'facts'),
    'top1000.wikisnippet.ru': ('top1000ru', 'wizextra=ent_today=2020-01-17%3Bentlang=ru', 'wiki_snippet/item'),
    'top1000.wikisnippet.ua': ('top1000ua', 'wizextra=ent_today=2020-01-17%3Bentlang=uk', 'wiki_snippet/item'),
    'top1000.wikisnippet.tr': ('top1000tr', 'wizextra=ent_today=2020-01-17%3Bentlang=tr', 'wiki_snippet/item'),
    'top1000.descr.ru': (
        'top1000ru',
        'wizextra=ent_today=2020-01-17%3Bentlang=ru',
        'base_info/description,base_info/description_source,base_info/sources',
    ),
    'top1000.descr.ua': (
        'top1000ua',
        'wizextra=ent_today=2020-01-17%3Bentlang=uk',
        'base_info/description,base_info/description_source,base_info/sources',
    ),
    'top1000.descr.tr': (
        'top1000tr',
        'wizextra=ent_today=2020-01-17%3Bentlang=tr',
        'base_info/description,base_info/description_source,base_info/sources',
    ),
    'top1000.search_req.ru': ('top1000ru', 'wizextra=ent_today=2020-01-17%3Bentlang=ru', 'base_info/search_request'),
    'top1000.search_req.ua': ('top1000ua', 'wizextra=ent_today=2020-01-17%3Bentlang=uk', 'base_info/search_request'),
    'top1000.search_req.tr': ('top1000tr', 'wizextra=ent_today=2020-01-17%3Bentlang=tr', 'base_info/search_request'),
    'top1000.image.ru': ('top1000ru', 'wizextra=ent_today=2020-01-17%3Bentlang=ru', 'base_info/image'),
    'top1000.image.ua': ('top1000ua', 'wizextra=ent_today=2020-01-17%3Bentlang=uk', 'base_info/image'),
    'top1000.image.tr': ('top1000tr', 'wizextra=ent_today=2020-01-17%3Bentlang=tr', 'base_info/image'),
    'meta': ('meta.txt', 'wizextra=ent_today=2020-01-17', None),
    'turkish': ('turkish.txt', 'wizextra=ent_today=2020-01-17', None),
    'meta.names': ('meta.txt', 'wizextra=ent_today=2020-01-17', 'base_info/title,base_info/name'),
    'turkish.names': ('turkish.txt', 'wizextra=ent_today=2020-01-17', 'base_info/title,base_info/name'),
    'meta.facts': ('meta.txt', 'wizextra=ent_today=2020-01-17', 'facts'),
    'turkish.facts': ('turkish.txt', 'wizextra=ent_today=2020-01-17', 'facts'),
    'meta.wikisnippet': ('meta.txt', 'wizextra=ent_today=2020-01-17', 'wiki_snippet/item'),
    'turkish.wikisnippet': ('turkish.txt', 'wizextra=ent_today=2020-01-17', 'wiki_snippet/item'),
    'meta.image': ('meta.txt', 'wizextra=ent_today=2020-01-17', 'base_info/image'),
    'turkish.image': ('turkish.txt', 'wizextra=ent_today=2020-01-17', 'base_info/image'),
    'meta.descr': (
        'meta.txt',
        'wizextra=ent_today=2020-01-17',
        'base_info/description,base_info/description_source,base_info/sources',
    ),
    'turkish.descr': (
        'turkish.txt',
        'wizextra=ent_today=2020-01-17',
        'base_info/description,base_info/description_source,base_info/sources',
    ),
    'meta.search_req': ('meta.txt', 'wizextra=ent_today=2020-01-17', 'base_info/search_request'),
    'turkish.search_req': ('turkish.txt', 'wizextra=ent_today=2020-01-17', 'base_info/search_request'),
}


def get_extra_cgi(subtest_name):
    _, wizextra, _ = subtestsProps[subtest_name]
    return wizextra


def get_paths_to_print(subtest_name):
    _, _, paths = subtestsProps[subtest_name]
    return paths


def get_input_file(subtest_name):
    input, _, _ = subtestsProps[subtest_name]
    return yatest.common.source_path("search/wizard/entitysearch/tools/outentity/tests/" + input)


def get_output_file(subtest_name):
    return yatest.common.output_path(subtest_name + ".out")


def get_shard_path():
    return yatest.common.binary_path("search/wizard/entitysearch/data/shard/search/wizard/entitysearch/data/shard_data")


def get_main_db_path():
    return yatest.common.binary_path("search/wizard/entitysearch/data/test_main_db/main_db")


def get_fresh_path():
    return yatest.common.binary_path("search/wizard/entitysearch/data/test_data")


def get_outentity_path():
    return yatest.common.binary_path("search/wizard/entitysearch/tools/outentity/outentity")


def get_arguments(subtest_name):
    dst_data_dir = os.path.join(yatest.common.work_path(), 'prepared_es_data')

    fs_manipulate.prepare_entitysearch_data_dir(
        dest_dir=dst_data_dir,
        entitysearch_data_resource_dir=get_shard_path(),
        main_db_resource_dir=get_main_db_path(),
    )

    fresh_dir = get_fresh_path()
    args = [
        get_outentity_path(),
        '-d',
        dst_data_dir,
        '-f',
        fresh_dir,
        '-s',
        'test.cfg',
        '--add-cgi',
        get_extra_cgi(subtest_name),
        '-j',
        '8',
        '--input',
        get_input_file(subtest_name),
        '--output',
        get_output_file(subtest_name),
    ]

    paths = get_paths_to_print(subtest_name)
    if paths is not None:
        args.append('--print-path')
        args.append(paths)

    return args


def run_subtest(subtest_name):
    yatest.common.execute(get_arguments(subtest_name))
    return yatest.common.canonical_file(get_output_file(subtest_name))
