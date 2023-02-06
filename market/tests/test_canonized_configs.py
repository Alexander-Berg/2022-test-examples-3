import copy
import os
import pytest
import shutil
import runtime_cloud.install_lib.generate_config as gen_conf
import yatest
import yatest.common


BSCONFIG_ITAGS_TEMPLATE = "a_ctype_{env} a_dc_vla a_prj_report-{report_role} a_shard_0 itag_replica_1"

ENV_TESTING = 'testing'
ENV_PRIEMKA = 'priemka'
ENV_EXP = 'productionexp1'
ENV_PRESTABLE = 'prestable'
ENV_PRODUCTION = 'production'

ROLE_MARKET = 'general-market'
ROLE_GOODS = 'general-goods-warehouse'
ROLE_GOODS_PARALLEL = 'general-goods-parallel'
ROLE_FRESH_BASE = 'general-fresh-base'
ROLE_META_MARKET = 'meta-market'
ROLE_API = 'general-api'
ROLE_INT = 'general-int'
ROLE_MARKET_KRAKEN = 'general-market-kraken'
ROLE_META_INT = 'meta-int'
ROLE_META_MARKET_KRAKEN = 'meta-market-kraken'
ROLE_META_PARALLEL = 'meta-parallel'
ROLE_PARALLEL = 'general-parallel'
ROLE_SHADOW = 'general-shadow'
ROLE_PLANESHIFT = 'general-planeshift'
ROLE_SNIPPET_PLANESHIFT = 'snippet-planeshift'


def create_context(env, role):
    return {
        'BSCONFIG_IHOST': 'sas2-5281',
        'BSCONFIG_IPORT': '17050',
        'HOME': '/home/container',
        'BSCONFIG_ITAGS': BSCONFIG_ITAGS_TEMPLATE.format(
            env=env,
            report_role=role,
        ),
        'PYTEST_INFO': {
            'env': env,
            'role': role,
        },
    }


def all_contexts_for_role(role, env_list):
    return [create_context(env, role) for env in env_list]


def update_environ(ctx):
    ctx = copy.deepcopy(ctx)
    del ctx['PYTEST_INFO']
    os.environ.update(ctx)


def make_test_name(ctx):
    ctx = ctx['PYTEST_INFO']
    name = '{}_{}'.format(ctx['role'], ctx['env'])
    return name.replace('-', '_')


def generate_config(filename):
    output_dir = yatest.common.output_path()
    root_dir = yatest.common.source_path('market/report/runtime_cloud/push_client/zeus_tmpl/conf/push-client')
    dst_tmpl_dir = os.path.join(output_dir, 'zeus_tmpl')
    src_file_path = os.path.join(root_dir, filename)
    dst_file_path = os.path.join(dst_tmpl_dir, filename)
    generated_file_path = os.path.join(output_dir, filename)

    if not os.path.isdir(dst_tmpl_dir):
        os.makedirs(dst_tmpl_dir)
    shutil.copy2(src_file_path, dst_file_path)

    gen_conf.generate(output_dir, is_good_file=lambda f: f == filename)
    with open(generated_file_path) as f:
        return f.read()


ALL_CONTEXTS = \
    all_contexts_for_role(ROLE_MARKET, [ENV_TESTING, ENV_EXP, ENV_PRESTABLE, ENV_PRODUCTION]) \
    + all_contexts_for_role(ROLE_GOODS, [ENV_TESTING, ENV_PRESTABLE, ENV_PRODUCTION]) \
    + all_contexts_for_role(ROLE_GOODS_PARALLEL, [ENV_PRESTABLE, ENV_PRODUCTION]) \
    + all_contexts_for_role(ROLE_FRESH_BASE, [ENV_TESTING, ENV_PRESTABLE, ENV_PRODUCTION]) \
    + all_contexts_for_role(ROLE_META_MARKET, [ENV_TESTING, ENV_PRODUCTION]) \
    + all_contexts_for_role(ROLE_API, [ENV_EXP, ENV_PRESTABLE, ENV_PRODUCTION]) \
    + all_contexts_for_role(ROLE_INT, [ENV_PRESTABLE, ENV_PRODUCTION]) \
    + all_contexts_for_role(ROLE_MARKET_KRAKEN, [ENV_PRODUCTION]) \
    + all_contexts_for_role(ROLE_META_INT, [ENV_PRESTABLE, ENV_PRODUCTION]) \
    + all_contexts_for_role(ROLE_META_MARKET_KRAKEN, [ENV_PRODUCTION]) \
    + all_contexts_for_role(ROLE_META_PARALLEL, [ENV_PRESTABLE, ENV_PRODUCTION]) \
    + all_contexts_for_role(ROLE_PARALLEL, [ENV_EXP, ENV_PRESTABLE, ENV_PRODUCTION]) \
    + all_contexts_for_role(ROLE_SHADOW, [ENV_PRIEMKA]) \
    + all_contexts_for_role(ROLE_PLANESHIFT, [ENV_TESTING, ENV_PRODUCTION]) \
    + all_contexts_for_role(ROLE_SNIPPET_PLANESHIFT, [ENV_TESTING, ENV_PRODUCTION])


@pytest.mark.parametrize(argnames='ctx', argvalues=ALL_CONTEXTS, ids=make_test_name)
def test_market_health(ctx):
    update_environ(ctx)
    return generate_config('market_health.yaml')


@pytest.mark.parametrize(argnames='ctx', argvalues=ALL_CONTEXTS, ids=make_test_name)
def test_market_offers_trace(ctx):
    update_environ(ctx)
    return generate_config('market_offers_trace.yaml')


@pytest.mark.parametrize(argnames='ctx', argvalues=ALL_CONTEXTS, ids=make_test_name)
def test_market_search(ctx):
    update_environ(ctx)
    return generate_config('market_search.yaml')
