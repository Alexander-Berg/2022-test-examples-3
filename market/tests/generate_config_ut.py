import json
import os
import shutil
from string import Template

import mock
from settings.settings import Settings, ReportSearchType
from market.pylibrary.experiment_flags import JsonKeys
import yatest.common

from util.report_control import SearchReportControl, SnippetReportControl

DEFAULT_EXPERIMENT_FLAGS = {
    JsonKeys.REPORT_SEARCH_TYPE: {
        JsonKeys.CONDITIONS: [
            {
                JsonKeys.CONDITION: 'IS_META',
                JsonKeys.VALUE: ReportSearchType.META_ONLY
            },
            {
                JsonKeys.CONDITION: '(not IS_META) and (IS_MARKET_KRAKEN or IS_INT)',
                JsonKeys.VALUE: ReportSearchType.BASE_ONLY
            },
            {
                JsonKeys.CONDITION: "SUBROLE == 'fresh-base'",
                JsonKeys.VALUE: ReportSearchType.FRESH_BASE
            }
        ],
        JsonKeys.DEFAULT_VALUE: ReportSearchType.META_AND_BASE
    },
}
DEFAULT_EMERGENCY_FLAGS = {
    JsonKeys.REPORT_FRESH_BASE_CONFIG_ENABLED: {
        JsonKeys.DEFAULT_VALUE: '1'
    }
}
TEST_DATA = yatest.common.source_path('market/report/runtime_cloud/report_ctl/tests/test_data')


def write_flags(root_dir, filename, flags):
    controls_path = os.path.join(root_dir, 'controls')
    if not os.path.exists(controls_path):
        os.makedirs(controls_path)

    path = os.path.join(controls_path, filename)
    with open(path, 'w') as f:
        json.dump(flags, f)


def write_experiment_flags(root_dir, new_flags):
    flags = DEFAULT_EXPERIMENT_FLAGS.copy()
    flags.update(new_flags)
    write_flags(root_dir, 'experiment_flags.json', flags)


def write_emergency_flags(root_dir, new_flags):
    flags = DEFAULT_EMERGENCY_FLAGS.copy()
    flags.update(new_flags)
    write_flags(root_dir, 'emergency_flags.json', flags)


def prepare_test_environment(test_data_dir, settings_file_name):
    root_dir = os.path.join(yatest.common.test_output_path('data'))

    shutil.copytree(test_data_dir, root_dir)
    shutil.copytree(
        os.path.join(test_data_dir, 'report-data'), os.path.join(root_dir, 'snippet_index', 'index', 'snippet-data')
    )

    with open(os.path.join(test_data_dir, settings_file_name)) as f:
        settings_template = Template(f.read())
        generated_settings = settings_template.substitute(report_conf_dir=root_dir, search_dir=root_dir)

    with open(os.path.join(root_dir, 'report_ctl.conf'), 'w') as f:
        f.write(generated_settings)
        f.write('\nroot_directory = {}\n'.format(root_dir))

    write_experiment_flags(root_dir, DEFAULT_EXPERIMENT_FLAGS)
    write_emergency_flags(root_dir, DEFAULT_EMERGENCY_FLAGS)

    # for runtime_cloud.environment
    for item in generated_settings.split("\n"):
        if 'nginx_port' in item:
            os.environ['BSCONFIG_IPORT'] = item.split("=")[1].strip()
            break
    os.environ['NODE_NAME'] = ''

    return root_dir


def generate_config(generator_cls, root_dir, is_rty_test=False, conf_file=None):
    settings = Settings(os.path.join(root_dir, 'report_ctl.conf'))
    report_ctl = generator_cls(settings)
    report_ctl.generate_config()
    generated_config_path = (
        os.path.join(root_dir, 'rty', 'server.cfg') if is_rty_test else os.path.join(root_dir, 'report.conf')
    )
    if conf_file is not None:
        generated_config_path = os.path.join(root_dir, conf_file)
    with open(generated_config_path) as fn:
        generated_config = fn.read()
    generated_config = generated_config.replace(root_dir, '')
    return generated_config


@mock.patch('socket.gethostname', return_value='main-report-with-diff-backend')
def test_main_with_diff(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_main_with_diff.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.gethostname', return_value='main-report-without-diff-backend')
def test_main_without_diff(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_main_without_diff.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.gethostname', return_value='snippet-report-with-diff-backend-substituted')
def test_snippet_with_diff_main(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_snippet_with_diff_main.conf')
    return generate_config(SnippetReportControl, root_dir)


@mock.patch('socket.gethostname', return_value='snippet-report-without-diff-backend')
def test_snippet_without_diff_main(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_snippet_without_diff_main.conf')
    return generate_config(SnippetReportControl, root_dir)


@mock.patch('socket.gethostname', return_value='main-report-without-diff-backend')
def test_main_api(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_main_api.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.gethostname', return_value='main-report-without-diff-backend')
def test_main_int(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_main_int.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.gethostname', return_value='main-report-without-diff-backend')
def test_main_prod_sas(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_main_prod_sas.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.gethostname', return_value='main-report-without-diff-backend')
def test_main_prod_vla(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_main_prod_vla.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.gethostname', return_value='main-report-without-diff-backend')
def test_main_prod_man(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_main_prod_man.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.gethostname', return_value='main-report-without-diff-backend')
def test_main_prep(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_main_prep.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.gethostname', return_value='main-report-without-diff-backend')
def test_main_prep_vla(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_main_prep_vla.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.gethostname', return_value='main-report-without-diff-backend')
def test_planeshift(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_planeshift.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.gethostname', return_value='main-report-without-diff-backend')
def test_parallel(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_parallel.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.getfqdn', return_value='main-report-without-diff-backend')
def test_main_perf_testing(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_main_perf_testing.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.getfqdn', return_value='snippet-report-without-diff-backend')
def test_snippet_perf_testing(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_snippet_perf_testing.conf')
    return generate_config(SnippetReportControl, root_dir)


@mock.patch('socket.gethostname', return_value='main-report-without-diff-backend')
def test_main_unstable(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_main_unstable.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.gethostname', return_value='main-report-without-diff-backend')
def test_main_testing(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_main_testing.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.gethostname', return_value='main-report-without-diff-backend')
def test_rty_testing(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_main_testing.conf')
    return generate_config(SearchReportControl, root_dir, is_rty_test=True)


@mock.patch('socket.gethostname', return_value='mbo-preview-report')
def test_mbo_preview(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_mbo_preview.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.gethostname', return_value='main-report-without-diff-backend')
def test_bk(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_bk.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.gethostname', return_value='blue-report')
def test_blue(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_blue.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.gethostname', return_value='blue-report')
def test_blue_shards(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_blue_shards.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.gethostname', return_value='blue-report')
def test_blue_testing(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_blue_testing.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.gethostname', return_value='goods-report')
def test_goods_testing(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_goods_testing.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.gethostname', return_value='goods-report')
def test_goods(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_goods.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.gethostname', return_value='main-report-without-diff-backend')
def test_shadow(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_shadow.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.getfqdn', return_value='main-report-without-diff-backend')
def test_incorrect_hostname(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_main_prod_sas.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.getfqdn', return_value='main-report1-1')
def test_multiple_shards(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_multiple_shards.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.getfqdn', return_value='main-report-without-diff-backend')
def test_kombat(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_kombat.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.getfqdn', return_value='main-report-without-diff-backend')
def test_kombat_saaskv_snippets(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_kombat_saaskv_snippets.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.getfqdn', return_value='msh17hp.market.yandex.net')
def test_model_collection_service(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_model_collection_service.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.getfqdn', return_value='msh17hp.market.yandex.net')
def test_model_collection_service_blue_shards(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_model_collection_service_blue_shards.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.getfqdn', return_value='sas6-1716-057-sas-market-prod--ec7-17050.gencfg-c.yandex.net')
def test_base_search(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_base_search.conf')
    flags = {
        JsonKeys.REPORT_SEARCH_TYPE: {
            JsonKeys.CONDITIONS: [{
                JsonKeys.VALUE: ReportSearchType.BASE_ONLY,
                JsonKeys.CONDITION: 'IS_SAS'
            }]
        }
    }
    write_experiment_flags(root_dir, flags)
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.getfqdn', return_value='sas4-2890-b77-sas-market-prod--6ec-17058.gencfg-c.yandex.net')
def test_meta_search(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_meta_search.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.getfqdn', return_value='sas4-2890-b77-sas-market-prod--6ec-17058.gencfg-c.yandex.net')
def test_meta_search_with_yp(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_meta_search_with_yp.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.getfqdn', return_value='sas4-2890-b77-sas-market-prod--6ec-17058.gencfg-c.yandex.net')
def test_meta_search_int(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_meta_search_int.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.getfqdn', return_value='sas4-2890-b77-sas-market-prod--6ec-17058.gencfg-c.yandex.net')
def test_meta_search_int_with_exp_flag(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_meta_search_int.conf')
    experiment_flags = {
        'enable_access_log_on_meta': {
            JsonKeys.CONDITIONS: [{
                JsonKeys.VALUE: '1',
                JsonKeys.CONDITION: 'IS_INT'
            }]
        },
        JsonKeys.META_ENABLE_CROSS_DC_BASE_SEARCH: {
            JsonKeys.CONDITIONS: [{
                JsonKeys.VALUE: '1',
                JsonKeys.CONDITION: 'IS_INT'
            }]
        }
    }

    emergency_flags = {
        JsonKeys.NEH_HTTP_OUT_CONN_SOFT_LIMIT: {
            JsonKeys.CONDITIONS: [{
                JsonKeys.VALUE: '5000',
                JsonKeys.CONDITION: 'IS_INT'
            }]
        },
        JsonKeys.NEH_HTTP_OUT_CONN_HARD_LIMIT: {
            JsonKeys.CONDITIONS: [{
                JsonKeys.VALUE: '40000',
                JsonKeys.CONDITION: 'IS_INT'
            }]
        }
    }

    write_experiment_flags(root_dir, experiment_flags)
    write_emergency_flags(root_dir, emergency_flags)
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.getfqdn', return_value='sas4-2890-b77-sas-market-prod--6ec-17058.gencfg-c.yandex.net')
def test_meta_search_kraken(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_meta_search_kraken.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.getfqdn', return_value='sas4-2890-b77-sas-market-prod--6ec-17058.gencfg-c.yandex.net')
def test_meta_search_with_increased_clusters_count(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_meta_search_increased_clusters_count.conf')
    flags = {
        JsonKeys.META_MAX_HOSTS_PER_COLLECTION: {
            JsonKeys.DEFAULT_VALUE: 47
        },
        JsonKeys.META_SEARCH_CLUSTERS_COUNT_LIMIT: {
            JsonKeys.DEFAULT_VALUE: 20
        }
    }
    write_experiment_flags(root_dir, flags)
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.getfqdn', return_value='sas2-5281-9f5-sas-market-test--900-17050.gencfg-c.yandex.net')
def test_fresh_base_testing(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_fresh_base_testing.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.getfqdn', return_value='fresh-base-report')
def test_fresh_base_production(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_fresh_base_production.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.getfqdn', return_value='main-report-backend')
def test_report_with_fresh_base_testing(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_report_with_fresh_base_testing.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.getfqdn', return_value='sas0-0496-8ec-sas-market-prod--280-17050.gencfg-c.yandex.net')
def test_report_with_fresh_base_production(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_report_with_fresh_base_production.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.getfqdn', return_value='vla3-5085-vla-market-prod-repo-76c-17050.gencfg-c.yandex.net')
def test_meta_with_fresh_base_production(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_meta_with_fresh_base_production.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.getfqdn', return_value='man1-5447-79e-man-market-prod--ef4-17066.gencfg-c.yandex.net')
def test_meta_api_with_fresh_base_production(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_meta_api_with_fresh_base_production.conf')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.getfqdn', return_value='man1-5447-79e-man-market-prod--ef4-17066.gencfg-c.yandex.net')
def test_meta_api_with_fresh_base_with_emerg_flag_production(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_meta_api_with_fresh_base_production.conf')
    flags = {
        JsonKeys.REPORT_FRESH_BASE_CONFIG_ENABLED: {
            JsonKeys.CONDITIONS: [{
                JsonKeys.VALUE: '0',
                JsonKeys.CONDITION: 'IS_API'
            }]
        }
    }
    write_emergency_flags(root_dir, flags)
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.gethostname', return_value='main-report-without-diff-backend')
def test_rty_profile_testing(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'test_main_testing.conf')
    flags = {
        JsonKeys.DISABLE_PORTAL_PROFILER: {
            JsonKeys.DEFAULT_VALUE: '1'
        }
    }
    write_experiment_flags(root_dir, flags)
    return generate_config(SearchReportControl, root_dir, is_rty_test=True)
