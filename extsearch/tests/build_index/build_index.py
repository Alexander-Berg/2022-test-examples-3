#!/usr/bin/env python
# -*- coding: utf-8 -*-

import glob
import json
import os
import shutil

from os.path import join

import yatest.common

import yt.wrapper
from yt.wrapper.ypath import ypath_join

from mapreduce.yt.python.yt_stuff import YtStuff

from extsearch.geo.indexer.business_indexer_yt.tests.common import upload_data

index = "indexer-business/index"


def upload_similar_orgs(yt_client, similars_path, destination_path):
    result = []
    files = os.listdir(similars_path)
    files.sort()
    for fname in files:
        with open(os.path.join(similars_path, fname), 'r') as f:
            similars = json.load(f)
            tbl_name = '{0}/similar_orgs_{1}'.format(destination_path, fname)
            yt_client.write_table(
                yt.wrapper.TablePath(tbl_name, sorted_by=['permalink']), (x for x in similars), format="yson"
            )
            result.append('{0}:{1}'.format(fname, tbl_name))
    return result


def buildBusinessIndex():
    yt_stuff = YtStuff(None)
    try:
        yt_stuff.start_local_yt()
        yt_indexer_base_path = "extsearch/geo/indexer/business_indexer_yt"
        indexer = yatest.common.binary_path(yt_indexer_base_path + "/standalone_indexer/standalone_indexer")
        downloader = yatest.common.binary_path(yt_indexer_base_path + "/download_index/download_index")
        data_builder = yatest.common.binary_path(yt_indexer_base_path + "/prepare_data/prepare_data")

        shutil.copytree(
            yatest.common.source_path("extsearch/geo/base/geobasesearch/tests/business/indexer-business/source"),
            "source",
        )

        config = yatest.common.source_path(
            "extsearch/geo/base/geobasesearch/tests/business/indexer-business/config.xml"
        )
        oxygen_config = yatest.common.source_path(
            "extsearch/geo/base/geobasesearch/tests/business/indexer-business/OxygenOptions.cfg"
        )
        daemon_config = yatest.common.source_path(
            "extsearch/geo/base/geobasesearch/tests/business/indexer-business/daemon_config.cfg"
        )

        shards_count = '4'

        server = yt_stuff.get_server()
        yt_env = {
            'YT_STORAGE': 'yes',
            'YT_SERVER': server,
            'YT_PROXY': server,
            'YT_PATH': '//',
        }

        yt_client = yt_stuff.get_yt_client()
        input_path = "//snapshot"
        result_path = "//output"

        exported_path = "//export/exported"
        yt_client.create("map_node", exported_path, recursive=True)

        test_data_path = "./source"
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
        yt_client.run_sort(duplicates_table, sort_by="DupId")

        queryrec_path = 'unpacked_queryrec'
        os.mkdir(queryrec_path)
        for file in glob.iglob(join(yatest.common.binary_path("search/wizard/data/wizard/language"), "queryrec.*")):
            if os.path.isfile(file):
                shutil.copy2(file, queryrec_path)
        annotations_table = input_path + '/annotations'
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
        )
        yt_client.run_sort(annotations_table, sort_by="permalink")
        yt_client.run_sort(company_table, sort_by="permalink")

        os.symlink(yatest.common.binary_path('geobase/data/v6/geodata6.bin'), 'geodata6.bin')

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
        yatest.common.execute(similars_cmd, env=yt_env)

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
                'Rubrics:geosearch_dssm_rubrics_doc.dssm',
                'SerpFps:geosearch_dssm_serp_fps_doc.dssm',
                'WebFast:l2_bigrams_doc.dssm',
            ],
            env=yt_env,
        )

        source_proto_table = ypath_join(input_path, 'source_proto')
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
        )

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
        )

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
                result_path,
                '--shards-count',
                shards_count,
                '-a',
                similars_table,
                '--ann-table',
                annotations_table,
                '--mapper-tmpfs-size',
                '0',
                '--merger-tmpfs-size',
                '0',
                '--oxygen-config',
                oxygen_config,
                '--malls-table',
                '//tmp/malls',
                '-a',
                geosearch_dssm_table,
                '-a',
                source_proto_table,
            ],
            env=yt_env,
        )

        assert yt_client.exists(result_path + "/0")

        indexdir = 'indexer-business'
        yatest.common.execute(
            [downloader, '-s', server, '-i', result_path, '-n', '0', '-d', indexdir],
            env=yt_env,
        )

        os.rename(os.path.join(indexdir, "index_0000000000_0000000000"), os.path.join(indexdir, "index"))

        yatest.common.execute(
            [
                data_builder,
                'export_svd',
                '-s',
                server,
                '-i',
                result_path,
                '--shards-count',
                shards_count,
                '-d',
                'svd.txt',
            ],
            env=yt_env,
        )
        os.rename('svd.txt', 'svd_export.txt')

        yatest.common.execute(
            [
                data_builder,
                'export_canonizer',
                '-s',
                server,
                '-i',
                result_path,
                '--shards-count',
                shards_count,
                '-d',
                'canonizer.zz',
            ],
            env=yt_env,
        )

    finally:
        try:
            yt_stuff.stop_local_yt()
        except:
            pass


REQUIRED_FILES = [
    'address_storage.mms',
    'chains.pbs',
    'companies.pbs',
    'docembeddings.mms',
    'factors.pbs',
    'features.pbs',
    'geosearch_dssm_doc_embedding.wad',
    'indexarc',
    'indexdir',
    'indexinv',
    'indexkey',
    'providers.pbs',
    'rubrics.pbs',
]


def _get_stdout(command, txtname, is_local=True):
    with open(txtname, 'w') as fd:
        yatest.common.execute(command, stdout=fd)
    return yatest.common.canonical_file(txtname, local=is_local)


def _print_pbs(name):
    printpbs = yatest.common.binary_path("extsearch/geo/tools/printpbs/printpbs")
    return _get_stdout([printpbs, join(index, name)], '{0}.txt'.format(name))


class TestYTBusinessIndexer:
    @classmethod
    def setup_class(cls):
        buildBusinessIndex()

    def testSvdExport(self):
        return yatest.common.canonical_file('svd_export.txt', local=True)

    def testCanonizer(self):
        return yatest.common.canonical_file('canonizer.zz', local=False)

    def testIndexFilesExistence(self):
        ls = os.listdir(index)
        for filename in REQUIRED_FILES:
            assert filename in ls

    # PBS tests
    def test_companies_pbs(self):
        return _print_pbs('companies.pbs')

    def test_factors_pbs(self):
        return _print_pbs('factors.pbs')

    def test_rubrics_pbs(self):
        return _print_pbs('rubrics.pbs')

    def test_chains_pbs(self):
        return _print_pbs('chains.pbs')

    def test_features_pbs(self):
        return _print_pbs('features.pbs')

    def test_providers_pbs(self):
        return _print_pbs('providers.pbs')

    def test_sprav_proto_wad(self):
        viewer = yatest.common.binary_path('extsearch/geo/kernel/sprav_proto/storage/viewer/viewer')
        return _get_stdout([viewer, join(index, 'sprav_proto.wad')], 'sprav_proto.wad.txt', is_local=False)
