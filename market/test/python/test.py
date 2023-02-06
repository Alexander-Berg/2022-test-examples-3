# coding: utf-8

import json
import os
import re
import shutil
import subprocess
from google.protobuf.json_format import Parse
import market.report.proto.BoosterConfig_pb2 as booster_config
import market.report.proto.FmcgParametersConfig_pb2 as fmcg_parameters_config

from lxml import etree

import yatest


def validate_csv(source, delimiter, types, escapechar=None):
    nfields = len(types)
    with open(source) as fobj:
        lineno = 0

        for line in fobj:
            lineno += 1
            if line.startswith('#'):
                continue

            row = line.rstrip().split(delimiter)
            assert len(row) == nfields, 'wrong data in line {}:{} [{}]'.format(source, lineno, line)
            for field_type, field in zip(types, row):
                try:
                    field_type(field)
                except ValueError:
                    assert False, 'wrong data in line {}:{} [{}]'.format(source, lineno, line)


def test_csv():
    fname_types = [
        ('market/svn-data/package-data', [
            ('parallel-query-cpa-models.db', [str, str]),
            ('parallel-query-cpc-models.db', [str, str]),
            ('redirect-black-list.db', [str]),
            ('redirect-stop-categories.db', [str, str]),
            ('redirect-stop-vendors.db', [str, int]),
            ('redirect-white-list-blue.db', [str, str]),
            ('redirect-white-list-low.db', [str, str]),
            ('redirect-white-list-blue-low.db', [str, str]),
            ('redirect-white-list-low-hitman.db', [str, str]),
            ('suggest-white-list.txt', [str, str, str]),
            ('suggest-white-list-blue.txt', [str, str, str]),
            ('beru_min_order_price_by_region.tsv', [int, int]),
            ('beru-region-service-delay.tsv', [int, int, int]),
            ('nailed-docs-white-list-catalog.db', [int, str]),
            ('incut-black-list-hid.db', [int, str]),
            ('boost_fee_groups.tsv', [int, int]),
            ('boost_fee_reserve_prices.tsv', [int, int]),
        ]),
        ('market/svn-data/package-data/fast', [
            ('redirect-white-list.db', [str, str]),
            ('redirect-white-list-app.db', [str, str]),
            ('nailed-docs-white-list.db', [str, str]),
            ('offers-min-price-for-region-delivery.db', [int, int, int, int])
        ]),
    ]
    for dir, fname_types_list in fname_types:
        for filename, types in fname_types_list:
            path = yatest.common.source_path(os.path.join(dir, filename))
            validate_csv(path, '\t', types)


def test_xml():
    filenames = [
        'forbidden_category_regions.xml',
        'vendor-recommended-shops.xml',
        'warnings.xml',
    ]
    for filename in filenames:
        path = yatest.common.source_path(os.path.join('market/svn-data/package-data', filename))
        with open(path) as fobj:
            try:
                etree.parse(fobj)
            except etree.LxmlError as e:
                assert False, '{} {}'.format(path, e)


def test_json():
    filenames = [
        'credit_plans.testing.json',
        'credit_plans.production.json',
        'express_partners.json',
        '../warehouse_priorities/warehouse_priorities.production.json',
        '../warehouse_priorities/warehouse_priorities.testing.json',
        '../hide_cash_only_conditions/hide_cash_only_conditions.json',
        'bnpl_conditions.json',
        'preorder_dates.testing.json',
        'preorder_dates.production.json',
        'hidden-warehouses.json',
        'fashion_categories.json',
        '1p_fashion_premium.json',
        '3p_fashion_premium.json',
        'fmcg_parameters.json',
        'fast/booster.json',
        'fast/credit_plans.production.json',
        'fast/credit_plans.testing.json',
        'parent_promos.json',
        'parallel_import_warranty.json',
    ]
    for filename in filenames:
        path = yatest.common.source_path(os.path.join('market/svn-data/package-data', filename))
        with open(path) as fobj:
            try:
                json.load(fobj)
            except Exception as e:
                assert False, '{} {}'.format(path, e)


def test_fmcg_parameters():
    path = yatest.common.source_path('market/svn-data/package-data/fmcg_parameters.json')
    with open(path) as conf_file:
        try:
            config = json.load(conf_file)
            assert config["pessimize_categories"], "No pessimize_categories in config"
            assert config["pessimize_categories"]["fbs"], "No fbs in config"
            assert config["pessimize_categories"]["dbs"], "No dbs in config"
            Parse(json.dumps(config), fmcg_parameters_config.TFmcgParametersConfig())
        except Exception as e:
            assert False, 'market/svn-data/package-data/fmcg_parameters.json {}'.format(e)


def test_booster():
    path = yatest.common.source_path('market/svn-data/package-data/fast/booster.json')
    with open(path) as conf_file:
        try:
            config = json.load(conf_file)
            assert config["boosts"], "No booster config"
            names = set()
            for boost in config["boosts"]:
                assert boost["name"], "No boost name"
                assert boost["type"], "No boost type"
                assert not boost["name"] in names, "Name ambiguity"
                names.add(boost["name"])

            Parse(json.dumps(config), booster_config.TBoosterConfig())
        except Exception as e:
            assert False, 'market/svn-data/package-data/fast/booster.json {}'.format(e)


def test_redirects_white_list_db():
    """В редиректах для белого и синего маркета должны быть ЧПУ"""
    parametric_url_re = re.compile(r'/(?:product|shop|brands|catalog|franchise|licensor)/\d+/.*')

    fname_types = [
        ('market/svn-data/package-data', [
            'redirect-white-list-low.db',
            'redirect-white-list-blue-low.db',
            'redirect-white-list-low-hitman.db',
            'redirect-white-list-blue.db',
        ]),
        ('market/svn-data/package-data/fast', [
            'redirect-white-list.db',
            'redirect-white-list-app.db',
        ]),
    ]
    for dir, fname_list in fname_types:
        for filename in fname_list:
            path = yatest.common.source_path(os.path.join(dir, filename))

            with open(path, 'rt') as fd:
                lineno = 0

                for line in fd:
                    lineno += 1
                    if line.startswith('#'):
                        continue

                    _, url = line.strip('\n').split('\t')
                    assert url.startswith('/'), \
                        u'{0}:{1} Url "{2}" must start with a "/"'.format(filename, lineno, url)
                    assert parametric_url_re.match(url) is None, \
                        u'{0}:{1} Url "{2}" must be friendly'.format(filename, lineno, url)
                    # MARKETOUT-37766 redirects' url must not contain suggest_text, it will be generated automatically
                    assert u'suggest_text' not in url, \
                        u'{0}:{1} Url "{2}" must not contain suggest_text parameter'.format(filename, lineno, url)


def test_suggest_white_lists_has_unique_aliases():
    """Проверяем, что среди алиасов, записанных в вайтлист нет дублей"""
    fname_list = [
        'suggest-white-list.txt',
        'suggest-white-list-blue.txt'
    ]

    for filename in fname_list:
        path = yatest.common.source_path(os.path.join('market/svn-data/package-data', filename))

        with open(path, 'rt') as f:
            used_aliases = set()
            for lineno, line in enumerate(f):
                alias = line.split('\t')[0].strip()
                assert alias not in used_aliases, \
                    u'{}:{} duplicated alias "{}"'.format(filename.decode('ascii'), lineno, alias.decode('utf-8'))
                used_aliases.add(alias)


def test_flatbuffers_match_schemas():
    fname_fbs = [
        ('market/svn-data/package-data', [
            ('incut-black-list.json', 'market/report/library/global/incut_black_list_fb/schema/incut_black_list_fb.fbs', []),
        ])
    ]
    tmp_dir = '/tmp/market_svn_data_fbs_test_{}_{}'.format(os.getpid(), os.getuid())
    flatc_name32 = yatest.common.binary_path('contrib/tools/flatc/flatc')
    flatc_name64 = yatest.common.binary_path('contrib/tools/flatc64/flatc64')
    try:
        for dir, fname_fbs_list in fname_fbs:
            for file_name, schema_name, extra_opts in fname_fbs_list:
                file_name_base = file_name[:-5] if file_name.endswith('.json') else file_name
                file_path_rel = os.path.join(dir, file_name)
                file_path = yatest.common.source_path(file_path_rel)
                schema_path = yatest.common.source_path(schema_name)
                flatc_name = flatc_name64 if schema_path.endswith('64') else flatc_name32
                bin_path = os.path.join(tmp_dir, file_name_base + '.bin')
                json_path = os.path.join(tmp_dir, file_name_base + '.json')
                cmd = [flatc_name, '--binary', '-o', tmp_dir] + extra_opts + [schema_path, file_path]
                p = subprocess.Popen(cmd)
                rc = p.wait()
                if rc != 0:
                    raise RuntimeError('Comparing flatbuffer "{fb}" with "{schema}" error: couldn\'t convert to binary, flatc error code: {rc}, cmd: {cmd}'.format(
                        fb=file_path_rel, schema=schema_name, rc=rc, cmd=' '.join(cmd)))
                cmd = [flatc_name, '--json', '--strict-json', '--natural-utf8', '--raw-binary', '-o', tmp_dir] + extra_opts + [schema_path, '--', bin_path]
                p = subprocess.Popen(cmd)
                rc = p.wait()
                if rc != 0:
                    raise RuntimeError('Comparing flatbuffer "{fb}" with "{schema}" error: couldn\'t convert to json, flatc error code: {rc}, cmd: {cmd}'.format(
                        fb=file_path_rel, schema=schema_name, rc=rc, cmd=' '.join(cmd)))
                with open(file_path) as fobj:
                    src_json = json.load(fobj)
                with open(json_path) as fobj:
                    dst_json = json.load(fobj)
                if json.dumps(src_json, sort_keys=True) != json.dumps(dst_json, sort_keys=True):
                    raise RuntimeError('Comparing flatbuffer "{fb}" with "{schema}" error: source json differs form generated'.format(
                        fb=file_path_rel, schema=schema_name))
    finally:
        try:
            shutil.rmtree(tmp_dir + "qwe")
        except:
            pass
