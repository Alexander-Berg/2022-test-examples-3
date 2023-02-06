import pytest
import yatest.common

from crypta.lib.python import templater
from crypta.lib.python.yt import yt_helpers
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


def run_testing(yt_stuff, output_file, lab_mock, max_vars=None):
    output_table_path = '//home/crypta/qa/output_table'
    config_vars = {
        'environment': 'qa',
        'output_table_path': output_table_path,
        'yt_proxy': yt_stuff.get_server(),
        'api_url': lab_mock.url_prefix + '/swagger.json',
    }
    if max_vars is not None:
        config_vars['max_vars_to_evaluate'] = max_vars

    config_template = yatest.common.source_path(
        'crypta/profile/services/socdem_expressions_for_direct/bundle/config.yaml',
    )
    ready_config = yatest.common.work_path('config.yaml')
    templater.render_file(config_template, ready_config, config_vars, strict=True)

    binary_path = yatest.common.binary_path(
        'crypta/profile/services/socdem_expressions_for_direct/bin/crypta-profile-socdem-expressions-for-direct',
    )

    return tests.yt_test(
        yt_client=yt_helpers.get_yt_client(yt_stuff.get_server(), yt_token='YT_TOKEN'),
        binary=binary_path,
        args=['--config', ready_config],
        output_tables=[(
            tables.YsonTable(
                output_file,
                output_table_path,
                yson_format='pretty',
            ),
            tests.Diff(),
        )],
        env={
            'API_TOKEN': 'API_TOKEN',
            'YT_TOKEN': 'YT_TOKEN',
        },
    )


def test_common_example(yt_stuff, common_example_lab_mock):
    return run_testing(yt_stuff, 'direct_socdem_expressions_common.yson', common_example_lab_mock)


def test_errors_example(yt_stuff, errors_example_lab_mock):
    return run_testing(yt_stuff, 'direct_socdem_expressions_errors.yson', errors_example_lab_mock)


@pytest.mark.parametrize('max_vars', [
    pytest.param(2, id='full_evaluation'),
    pytest.param(1, id='shorten_evaluation'),
])
def test_max_vars_diff_example(yt_stuff, max_vars_diff_example_lab_mock, max_vars):
    return run_testing(
        yt_stuff,
        'direct_socdem_expressions_max_vars_diff.yson',
        max_vars_diff_example_lab_mock,
        max_vars,
    )
