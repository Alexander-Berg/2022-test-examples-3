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


def prepare_test_environment(test_data_dir, settings_file_name, shards_config_name):
    root_dir = os.path.join(yatest.common.test_output_path('data'))

    shutil.copytree(test_data_dir, root_dir)
    shutil.copy(os.path.join(test_data_dir, 'shards-data', shards_config_name + '.conf'), os.path.join(root_dir, 'shards.conf'))

    with open(os.path.join(test_data_dir, settings_file_name)) as f:
        settings_template = Template(f.read())
        generated_settings = settings_template.substitute(report_conf_dir=root_dir, search_dir=root_dir, shards_config='shards')

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


@mock.patch('socket.gethostname', return_value='somehost.vla.yp-c.yandex.net')
def test_main_base_testing(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'shards_main_base_testing.conf', 'base_market_vla')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.gethostname', return_value='somehost.vla.yp-c.yandex.net')
def test_main_base_hosts_testing(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'shards_main_base_testing.conf', 'base_market_hosts_vla')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.gethostname', return_value='somehost.vla.yp-c.yandex.net')
def test_main_base_snippet_testing(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'shards_main_base_testing.conf', 'base_market_snippet_vla')
    return generate_config(SnippetReportControl, root_dir)


@mock.patch('socket.gethostname', return_value='somehost.vla.yp-c.yandex.net')
def test_main_fresh_testing(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'shards_main_base_testing.conf', 'fresh_market_vla')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.gethostname', return_value='somehost.vla.yp-c.yandex.net')
def test_main_meta_hosts_testing(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'shards_main_meta_testing.conf', 'meta_market_hosts_vla')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.gethostname', return_value='somehost.vla.yp-c.yandex.net')
def test_main_meta_shards_testing(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'shards_main_meta_testing.conf', 'meta_market_shards_vla')
    return generate_config(SearchReportControl, root_dir)


@mock.patch('socket.gethostname', return_value='somehost.vla.yp-c.yandex.net')
def test_main_meta_hosts_ext_snippet_testing(mock_socket):
    root_dir = prepare_test_environment(TEST_DATA, 'shards_main_meta_testing.conf', 'meta_market_hosts_ext_snippet_vla')
    return generate_config(SearchReportControl, root_dir)
