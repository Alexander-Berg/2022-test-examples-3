#!/usr/bin/env python
# -*- coding: utf-8 -*-

import glob
import json
import os
import shutil
import sys
import distutils.dir_util

from os.path import join

import yatest.common

from extsearch.geo.base.geobasesearch.tests.business.test_cases.test_cases import BusinessTestcase

from mapreduce.yt.python.yt_stuff import YtStuff, YtConfig
import yt.wrapper
from yt.wrapper.ypath import ypath_join

import devtools.ya.yalibrary.upload.lib as uploader
import yalibrary.upload.consts

from extsearch.geo.indexer.business_indexer_yt.tests.common import upload_data


def upload_duplicates_urls(yt_client, table):
    data = [
        {'permalink': 111, 'DuplicatesUrls': 'http://www.ya.ru@@http://www.yandex.ru'},
        {'permalink': 140, 'DuplicatesUrls': 'http://www.vk.com'},
    ]

    yt_client.write_table(table, (x for x in data), format="yson")
    yt_client.run_sort(table, sort_by="permalink")


def upload_quarantine_rubric_fixlists(yt_client, table):
    data = [
        # "184105514"
        {
            'rubric_id': 184105514,
            'geo_id': 225,
            'closed_for_visitors': True,
            'closed_for_quarantine': True,
            'temporarily_closed': True,
        },
        # "Гостиницы"
        {
            'rubric_id': 184106414,
            'geo_id': 213,
            'closed_for_visitors': True,
            'closed_for_quarantine': False,
            'temporarily_closed': None,
        },
        # "Медцентр"
        {
            'rubric_id': 184106108,
            'geo_id': 213,
            'closed_for_visitors': False,
            'closed_for_quarantine': True,
            'temporarily_closed': None,
        },
        # "Кафе"
        {
            'rubric_id': 184106390,
            'geo_id': 213,
            'closed_for_visitors': True,
            'closed_for_quarantine': None,
            'temporarily_closed': None,
        },
        # "184106386"
        {
            'rubric_id': 184106386,
            'geo_id': 225,
            'closed_for_visitors': None,
            'closed_for_quarantine': True,
            'temporarily_closed': None,
        },
        # "Исправительные заведения"
        {
            'rubric_id': 184105698,
            'geo_id': 11111,
            'closed_for_visitors': True,
            'closed_for_quarantine': None,
            'temporarily_closed': None,
        },
    ]
    yt_client.write_table(table, (x for x in data), format="yson")


def upload_quarantine_chain_fixlists(yt_client, table):
    data = [
        # "McDonald’s"
        {
            'chain_id': 6003441,
            'geo_id': 193,
            'closed_for_visitors': True,
            'closed_for_quarantine': False,
            'temporarily_closed': None,
        },
        # "McDonald’s"
        {
            'chain_id': 6003441,
            'geo_id': 213,
            'closed_for_visitors': True,
            'closed_for_quarantine': True,
            'temporarily_closed': None,
        },
        # "Крошка Картошка"
        {
            'chain_id': 6002347,
            'geo_id': 225,
            'closed_for_visitors': False,
            'closed_for_quarantine': True,
            'temporarily_closed': None,
        },
    ]
    yt_client.write_table(table, (x for x in data), format="yson")


def upload_review_count(yt_client, table):
    data = [
        {'permalink': 1009550112, 'count': 12},
        {'permalink': 1045755033, 'count': 13},
        {'permalink': 1054821695, 'count': 5},
        {'permalink': 1120046349, 'count': 1},
    ]
    yt_client.write_table(table, (x for x in data), format="yson")


def upload_top_lists_data(yt_client, table):
    data = [
        {'permalink': 182, 'TopListsData': "{\"default\":{\"Position\":15,\"Score\":21.0186458}}"},
        {'permalink': 1054821695, 'TopListsData': "{\"default\":{\"Position\":0,\"Score\":61.97920587}}"},
    ]
    yt_client.write_table(table, (x for x in data), format="json")
    yt_client.run_sort(table, sort_by="permalink")


def upload_similar_orgs(yt_client, similars_path, destination_path):
    result = []
    for fname in os.listdir(similars_path):
        with open(os.path.join(similars_path, fname), 'r') as f:
            similars = json.load(f)
            tbl_name = '{0}/similar_orgs_{1}'.format(destination_path, fname)
            yt_client.write_table(
                yt.wrapper.TablePath(tbl_name, sorted_by=['permalink']), (x for x in similars), format="yson"
            )
            result.append('{0}:{1}'.format(fname, tbl_name))
    return result


def upload_table_yt(yt_client, input_json, destination_path, table_name):
    tbl_name = os.path.join(destination_path, table_name)
    data = []
    with open(input_json, 'r') as f:
        for line in f:
            data.append(json.loads(line))
    yt_client.write_table(tbl_name, (x for x in data), format="yson")
    return tbl_name


def buildBusinessIndex():
    indexer_web_ann = yatest.common.binary_path('extsearch/geo/indexer/web_annotation/indexer-web-annotation')

    SOURCE_PREFIX = 'extsearch/geo/base/geobasesearch/tests/business/indexer-business/source'
    # put small Backa export into ./source directory
    shutil.copytree(yatest.common.source_path(SOURCE_PREFIX), 'source')
    # now we have ./source/companies2.xml, ./source/rubrics2.xml, ...

    # make empty directory ./indexer-business/index
    indexdir = os.path.join('indexer-business', 'index')
    os.makedirs(indexdir)

    yt_cfg = YtConfig(node_count=3)
    yt_stuff = YtStuff(yt_cfg)
    try:
        static_factors_json = yatest.common.source_path('{}/static_factors_map.json'.format(SOURCE_PREFIX))

        yt_stuff.start_local_yt()
        yt_indexer_base_path = "extsearch/geo/indexer/business_indexer_yt"
        indexer = yatest.common.binary_path(yt_indexer_base_path + "/standalone_indexer/standalone_indexer")
        downloader = yatest.common.binary_path(yt_indexer_base_path + "/download_index/download_index")
        data_builder = yatest.common.binary_path(yt_indexer_base_path + "/prepare_data/prepare_data")

        base_path = "extsearch/geo/base/geobasesearch/tests/business"
        config = yatest.common.source_path(base_path + "/indexer-business/config.xml")
        daemon_config = yatest.common.source_path(base_path + "/indexer-business/daemon_config.cfg")

        os.mkdir("config")
        shutil.copy2(yatest.common.source_path(base_path + "/indexer-business/OxygenOptions.cfg"), "config")
        shutil.copy2(yatest.common.source_path(base_path + "/indexer-business/OxygenOptions_panther.cfg"), "config")

        server = yt_stuff.get_server()
        yt_env = {
            'YT_STORAGE': 'yes',
            'YT_SERVER': server,
            'YT_PROXY': server,
            'YT_PATH': '//',
        }

        yt_client = yt_stuff.get_yt_client()
        input_path = "//snapshot"
        result_path = "//result"

        exported_path = "//export/exported"
        yt_client.create("map_node", exported_path, recursive=True)

        test_data_path = 'source'
        yt_client.create('map_node', input_path)
        yt_client.create('map_node', result_path)
        assert yt_client.exists(input_path)
        assert yt_client.exists(result_path)

        yt_client.create('group', attributes={'name': 'geosearch'})

        company_table = input_path + '/company'
        upload_data.upload_companies_data(yt_client, company_table, test_data_path)
        upload_data.upload_exported_dir(yt_client, exported_path, test_data_path)

        result_path = "//result"
        duplicates_table = input_path + '/duplicates'

        yatest.common.execute(
            [data_builder, 'companies_duplicates', '-s', server, '-o', duplicates_table, '-c', company_table],
            env=yt_env,
        )

        annotations_data = test_data_path + '/ann.txt'
        annotations_data_tbl = input_path + '/annotations_data'
        upload_data.upload_annotations(yt_client, annotations_data, annotations_data_tbl)

        parallel_procs = []

        queryrec_path = 'unpacked_queryrec'
        os.mkdir(queryrec_path)
        for file in glob.iglob(join(yatest.common.binary_path("search/wizard/data/wizard/language"), "queryrec.*")):
            if os.path.isfile(file):
                shutil.copy2(file, queryrec_path)
        annotations_table = input_path + '/annotations'
        parallel_procs.append(
            yatest.common.execute(
                [
                    data_builder,
                    'annotations',
                    '-s',
                    server,
                    '-o',
                    annotations_table,
                    '-d',
                    duplicates_table,
                    '-i',
                    annotations_data_tbl,
                    '-q',
                    queryrec_path,
                ],
                env=yt_env,
                wait=False,
            )
        )

        os.symlink(yatest.common.binary_path('geobase/data/v6/geodata6.bin'), 'geodata6.bin')

        parallel_procs.append(
            yatest.common.execute(
                [
                    data_builder,
                    'malls',
                    '-o',
                    '//tmp/malls',
                    '-s',
                    server,
                    '-c',
                    company_table,
                    '-e',
                    '//export/',
                    '--memory',
                    '1',
                ],
                env=yt_env,
                wait=False,
            )
        )

        factors_tbl = upload_table_yt(yt_client, static_factors_json, input_path, 'static_factors_data')
        static_factors_table = input_path + '/static_factors_yt'
        parallel_procs.append(
            yatest.common.execute(
                [
                    data_builder,
                    'static_factors_yt',
                    '-o',
                    static_factors_table,
                    '-s',
                    server,
                    '-d',
                    duplicates_table,
                    factors_tbl,
                ],
                env=yt_env,
                wait=False,
            )
        )

        org_data_annotation_json = yatest.common.source_path('{}/org_data_annotation.json'.format(SOURCE_PREFIX))
        org_data_annotation_tbl = upload_table_yt(
            yt_client, org_data_annotation_json, input_path, 'org_data_annotation'
        )
        yt_client.run_sort(org_data_annotation_tbl, sort_by=("key", "subkey"))
        factor_annotations_tbl = input_path + '/factor_annotations'
        parallel_procs.append(
            yatest.common.execute(
                [
                    data_builder,
                    'factor_annotations',
                    '-o',
                    factor_annotations_tbl,
                    '-s',
                    server,
                    '-d',
                    duplicates_table,
                    'org_data:{}'.format(org_data_annotation_tbl),
                ],
                env=yt_env,
                wait=False,
            )
        )

        similars = upload_similar_orgs(yt_client, os.path.join(test_data_path, 'similar_org_tables'), input_path)
        similars_table = input_path + '/similars'
        similars_cmd = [
            data_builder,
            'similar_orgs',
            '-o',
            similars_table,
            '-s',
            server,
            '-d',
            duplicates_table,
        ]
        for item in similars:
            similars_cmd.extend(['-i', item])
        parallel_procs.append(
            yatest.common.execute(
                similars_cmd,
                env=yt_env,
                wait=False,
            )
        )

        reviews_table = input_path + '/reviews'
        reviews = exported_path + '/reviews'
        upload_review_count(yt_client, reviews)

        top_lists_data_table = input_path + '/top_lists_data'
        upload_top_lists_data(yt_client, top_lists_data_table)

        parallel_procs.append(
            yatest.common.execute(
                [
                    data_builder,
                    'reviews',
                    '-o',
                    reviews_table,
                    '-s',
                    server,
                    '--reviews',
                    reviews,
                ],
                env=yt_env,
                wait=False,
            )
        )

        duplicates_urls_table = input_path + '/duplicates_urls'
        upload_duplicates_urls(yt_client, duplicates_urls_table)

        os.symlink(
            yatest.common.binary_path('extsearch/geo/tests_data/dssm/geosearch_dssm_serp_fps_doc.dssm'),
            'geosearch_dssm_serp_fps_doc.dssm',
        )
        os.symlink(
            yatest.common.binary_path('extsearch/geo/tests_data/dssm/geosearch_dssm_rubrics_doc.dssm'),
            'geosearch_dssm_rubrics_doc.dssm',
        )
        os.symlink(
            yatest.common.binary_path('extsearch/geo/tests_data/dssm/l2_bigrams_doc.dssm'), 'l2_bigrams_doc.dssm'
        )
        geosearch_dssm_table = input_path + '/geosearch_dssm'
        parallel_procs.append(
            yatest.common.execute(
                [
                    data_builder,
                    'geosearch_dssm',
                    '-o',
                    geosearch_dssm_table,
                    '-s',
                    server,
                    '-i',
                    company_table,
                    '-d',
                    duplicates_table,
                    '-r',
                    ypath_join(exported_path, 'rubric'),
                    'SerpFps:geosearch_dssm_serp_fps_doc.dssm',
                    'Rubrics:geosearch_dssm_rubrics_doc.dssm',
                    'WebFast:l2_bigrams_doc.dssm',
                ],
                env=yt_env,
                wait=False,
            )
        )

        parallel_procs.append(
            yatest.common.execute(
                [
                    data_builder,
                    'precompute_filters',
                    '-o',
                    'precomputed_filters.pb',
                    '-s',
                    server,
                    '-c',
                    company_table,
                    '-e',
                    exported_path,
                    '--proto',
                    '--geobase-path',
                    yatest.common.binary_path('geobase/data/v6/geodata6.bin'),
                ],
                env=yt_env,
                wait=False,
            )
        )

        chain_fixlist = input_path + '/chain_fixlist'
        upload_quarantine_chain_fixlists(yt_client, chain_fixlist)
        rubric_fixlist = input_path + '/rubric_fixlist'
        upload_quarantine_rubric_fixlists(yt_client, rubric_fixlist)
        fast_features_source = yatest.common.source_path(
            'extsearch/geo/base/geobasesearch/tests/business/indexer-fast-features/source/fast.json'
        )
        fast_references_source = yatest.common.source_path(
            'extsearch/geo/base/geobasesearch/tests/business/indexer-fast-features/source/references.json'
        )
        hotel_price_source = yatest.common.source_path(
            'extsearch/geo/base/geobasesearch/tests/business/indexer-fast-features/source/hotel-prices.json'
        )
        maps_goods_export_source = yatest.common.source_path(
            'extsearch/geo/base/geobasesearch/tests/business/indexer-fast-features/source/company-prices.json'
        )
        fast_feature_indexer = yatest.common.binary_path(
            'extsearch/geo/indexer/fast_features/yandex-geosearch-indexer-fast-features'
        )
        fast_features_mms = 'fast_features.mms'
        parallel_procs.append(
            yatest.common.execute(
                [
                    fast_feature_indexer,
                    '-j',
                    '-i',
                    fast_features_source,
                    '--fresh-input',
                    fast_references_source,
                    '--price-input',
                    hotel_price_source,
                    '-o',
                    fast_features_mms,
                    '-r',
                    '-p',
                    server,
                    '--rubric-fixlist',
                    rubric_fixlist,
                    '--chain-fixlist',
                    chain_fixlist,
                    '--maps-goods-export',
                    maps_goods_export_source,
                ],
                env=yt_env,
                wait=False,
            )
        )

        search_text_table = input_path + '/search_text'
        parallel_procs.append(
            yatest.common.execute(
                [
                    data_builder,
                    'search_text',
                    '-o',
                    search_text_table,
                    '-s',
                    server,
                    '-c',
                    company_table,
                ],
                env=yt_env,
                wait=False,
            )
        )

        source_proto_table = ypath_join(input_path, 'source_proto')
        parallel_procs.append(
            yatest.common.execute(
                [
                    data_builder,
                    'source_proto',
                    '--server',
                    server,
                    '--snapshot-company',
                    company_table,
                    '--output',
                    source_proto_table,
                ],
                env=yt_env,
                wait=False,
            )
        )

        parallel_procs.append(
            yatest.common.execute(
                [
                    data_builder,
                    'moved',
                    '--cluster',
                    server,
                    '--companies',
                    company_table,
                    '--duplicates',
                    duplicates_table,
                    '-o',
                    'moved.bin',
                ],
                env=yt_env,
                wait=False,
            )
        )

        for proc in parallel_procs:
            proc.wait()

        yatest.common.execute(
            [
                indexer,
                '-c',
                server,
                '-s',
                company_table,
                '--export',
                '//export/',
                '--tmp-dir',
                '//tmp',
                '--error-log',
                '//tmp/error_log',
                '--indexer-config',
                config,
                '--merger-config',
                daemon_config,
                '--output-dir',
                '//output',
                '--shards-count',
                '1',
                '-a',
                static_factors_table,
                '-a',
                similars_table,
                '-a',
                duplicates_urls_table,
                '-a',
                annotations_table,
                '-a',
                reviews_table,
                '-a',
                geosearch_dssm_table,
                '-a',
                factor_annotations_tbl,
                '-a',
                search_text_table,
                '-a',
                source_proto_table,
                '-a',
                top_lists_data_table,
                '--mapper-tmpfs-size',
                '0',
                '--merger-tmpfs-size',
                '0',
                '--oxygen-config',
                'config/OxygenOptions.cfg',
                '--oxygen-config',
                'config/OxygenOptions_panther.cfg',
                '--malls-table',
                '//tmp/malls',
            ],
            env=yt_env,
        )

        assert yt_client.exists("//output/0")

        indexdir = 'indexer-business'
        yatest.common.execute(
            [downloader, '-s', server, '-i', '//output', '-n', '0', '-d', indexdir],
            env=yt_env,
        )

        os.rename(os.path.join(indexdir, "index_0000000000_0000000000"), os.path.join(indexdir, "index"))

        indexdir = os.path.join(indexdir, "index")

        open(os.path.join(indexdir, "freqs.txt"), 'w').close()

        shutil.copy('precomputed_filters.pb', indexdir)

        # create web ann index
        web_ann_raw_input = yatest.common.source_path(
            'extsearch/geo/base/geobasesearch/tests/business/indexer-webann/raw_data.txt'
        )
        yatest.common.execute(
            [indexer_web_ann, '-i', web_ann_raw_input, '-o', os.path.join(indexdir, 'indexann'), '-f', 'text']
        )

        # copy geo.stat to index
        shutil.copy('geo.stat', indexdir)

        additional_index_files_dir = yatest.common.source_path(
            'extsearch/geo/base/geobasesearch/tests/business/indexer-business/additional_files'
        )
        distutils.dir_util.copy_tree(additional_index_files_dir, indexdir)

        static_factors_chain = yatest.common.source_path('{}/static_factors_chain.mms'.format(SOURCE_PREFIX))
        shutil.copy(static_factors_chain, indexdir)

        static_factors_rubric = yatest.common.source_path('{}/static_factors_rubric.mms'.format(SOURCE_PREFIX))
        shutil.copy(static_factors_rubric, indexdir)

        shutil.copy(fast_features_mms, indexdir)

        # create shard_info.json file
        with open(os.path.join(indexdir, 'shard_info.json'), 'w') as shard_info:
            shard_info.write('{"shard_id": 2}')

        # copy index to output (may be used later for investigating some problems, index will be available in TestEnv)
        shutil.copytree(indexdir, yatest.common.output_path('indexer-business/index'))

    finally:
        try:
            yt_stuff.stop_local_yt()
        except:
            pass


##
# TEST CASE
#
class YTIndexBusinessTestcase(BusinessTestcase):
    @classmethod
    def BuildBusinessIndex(cls):
        buildBusinessIndex()

    def testUploadIndex(self):
        sb_upload = yatest.common.get_param('refresh_index')
        if sb_upload:
            sys.stderr.write('uploading to Sandbox...\n')
            uploader.fix_logging()
            target = 'indexer-business/index'
            rid = uploader.do(
                paths=[target],
                paths_root='',
                resource_description='Geobasesearch index for local run',
                ttl='inf',
                should_tar=False,
                transport=yalibrary.upload.consts.UploadTransport.Skynet,
            )
            sys.stderr.write('resource id is %s\n' % rid)
            sys.stderr.write('now you have to manually replace it in ya.make\n')


del BusinessTestcase
