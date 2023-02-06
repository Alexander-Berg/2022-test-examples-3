import copy
import os
import pytest
import runtime_cloud.environment as rc_env
import runtime_cloud.install_lib.generate_config as gen_conf
import market.report.runtime_cloud.unified_agent.lib as config_generator
import yatest


def get_all_config_files():
    root_dir = yatest.common.source_path('market/report/runtime_cloud/unified_agent/zeus_tmpl')

    result = [yatest.common.source_path('market/report/runtime_cloud/unified_agent/conf/unified_agent/unified_agent.yml')]

    for dir_, _, files in os.walk(root_dir):
        for file_name in files:
            rel_dir = os.path.relpath(dir_, root_dir)
            rel_file = os.path.join(rel_dir, file_name)
            result.append(rel_file)

    return result

UNIFIED_AGENT_CONFIG_FILES = get_all_config_files()

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


def create_context(env, role, filepath):
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
            'filepath': filepath
        },
    }


def all_contexts_for_role(role, env_list):
    return [
        create_context(env, role, file)
        for env in env_list
        for file in UNIFIED_AGENT_CONFIG_FILES
    ]


def update_environ(ctx):
    ctx = copy.deepcopy(ctx)
    del ctx['PYTEST_INFO']
    os.environ.update(ctx)


def make_test_name(ctx):
    ctx = ctx['PYTEST_INFO']
    name = '{}_{}_{}'.format(ctx['role'], ctx['env'], os.path.basename(ctx['filepath']))
    return name.replace('-', '_').replace('.', '_')


def generate_config(ctx):
    filename = ctx['PYTEST_INFO']['filepath']
    root_dir = yatest.common.source_path('market/report/runtime_cloud/unified_agent')
    generated_file_path = os.path.join(root_dir, filename)

    gen_conf.generate(root_dir, is_good_file=lambda f: f == filename)
    test_secrets = os.path.join(root_dir, "secrets")
    with open(test_secrets, "w") as f:
        f.write("""{"clients": {"market-report": {"self_tvm_id": 1234567}}}""")
    os.makedirs(os.path.join(root_dir, "conf/unified_agent"), exist_ok=True)
    cfg = {
        "unified_agent_grpc_port": rc_env.ports.unified_agent_grpc,
        "unified_agent_status_port": rc_env.ports.unified_agent_status,
        "unified_agent_metrics_read_port": rc_env.ports.unified_agent_metrics_read,
        "unified_agent_metrics_write_port": rc_env.ports.unified_agent_metrics_write,
        "host_metric_logger_port": rc_env.ports.host_metric_logger,
        "nginx_port": rc_env.ports.nginx,
        "report_port": rc_env.ports.report,
        "additional_metric_labels": {
            "role": rc_env.report.role,
            "subrole": rc_env.report.subrole,
            "cluster_index": rc_env.report.cluster_index
        },
        "conf_path": rc_env.paths.conf,
        "logs_path": rc_env.paths.logs,
        "report_log_dir": rc_env.report.log_dir,
        "environment": rc_env.host.environment,
        "data": rc_env.paths.data
    }
    config_generator.generate_config(os.path.join(root_dir, "conf/unified_agent/unified_agent.yml"), cfg, test_secrets)
    with open(generated_file_path) as f:
        return f.read()


ALL_CONTEXTS = (
    all_contexts_for_role(ROLE_MARKET, [ENV_TESTING, ENV_EXP, ENV_PRESTABLE, ENV_PRODUCTION])
    + all_contexts_for_role(ROLE_GOODS, [ENV_TESTING, ENV_PRESTABLE, ENV_PRODUCTION])
    + all_contexts_for_role(ROLE_GOODS_PARALLEL, [ENV_PRESTABLE, ENV_PRODUCTION])
    + all_contexts_for_role(ROLE_FRESH_BASE, [ENV_TESTING, ENV_PRESTABLE, ENV_PRODUCTION])
    + all_contexts_for_role(ROLE_META_MARKET, [ENV_TESTING, ENV_PRODUCTION])
    + all_contexts_for_role(ROLE_API, [ENV_EXP, ENV_PRESTABLE, ENV_PRODUCTION])
    + all_contexts_for_role(ROLE_INT, [ENV_PRESTABLE, ENV_PRODUCTION])
    + all_contexts_for_role(ROLE_MARKET_KRAKEN, [ENV_PRODUCTION])
    + all_contexts_for_role(ROLE_META_INT, [ENV_PRESTABLE, ENV_PRODUCTION])
    + all_contexts_for_role(ROLE_META_MARKET_KRAKEN, [ENV_PRODUCTION])
    + all_contexts_for_role(ROLE_META_PARALLEL, [ENV_PRESTABLE, ENV_PRODUCTION])
    + all_contexts_for_role(ROLE_PARALLEL, [ENV_EXP, ENV_PRESTABLE, ENV_PRODUCTION])
    + all_contexts_for_role(ROLE_SHADOW, [ENV_PRIEMKA])
    + all_contexts_for_role(ROLE_PLANESHIFT, [ENV_TESTING, ENV_PRODUCTION])
    + all_contexts_for_role(ROLE_SNIPPET_PLANESHIFT, [ENV_TESTING, ENV_PRODUCTION])
)


@pytest.mark.parametrize(argnames='ctx', argvalues=ALL_CONTEXTS, ids=make_test_name)
def test_confgs(ctx):
    update_environ(ctx)
    return generate_config(ctx)
