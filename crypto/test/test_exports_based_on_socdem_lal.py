import yatest.common

from crypta.lib.python import templater
from crypta.lib.python.yt import yt_helpers
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


def run_testing(yt_stuff, output_file, lab_mock):
    output_table_path = '//home/crypta/qa/output_table'
    config_vars = {
        'environment': 'qa',
        'output_table_path': output_table_path,
        'yt_proxy': yt_stuff.get_server(),
        'api_url': lab_mock.url_prefix + '/swagger.json',
    }

    config_template = yatest.common.source_path(
        'crypta/profile/services/exports_based_on_socdem_or_lal/bundle/config.yaml',
    )
    ready_config = yatest.common.work_path('config.yaml')
    templater.render_file(config_template, ready_config, config_vars, strict=True)

    binary_path = yatest.common.binary_path(
        'crypta/profile/services/exports_based_on_socdem_or_lal/bin/crypta-profile-exports-based-on-socdem-or-lal',
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
    return run_testing(yt_stuff, 'exports_on_socdem_lal_common.yson', common_example_lab_mock)


def test_cycle_example(yt_stuff, cycle_example_lab_mock):
    return run_testing(yt_stuff, 'exports_on_socdem_lal_cycle.yson', cycle_example_lab_mock)
