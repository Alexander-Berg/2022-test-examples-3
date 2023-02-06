import functools
import os

import mock
import yatest.common

from crypta.lib.python.test_utils.mock_sandbox_server_with_resource import mock_sandbox_server_with_resource
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.lookalike.services.custom_lookalike.lib import sample


def test_get_group_features(yt_client, yql_client, describe_input, model_version, date):
    sample_table = tables.get_yson_table_with_schema(
        file_path='sample.yson',
        cypress_path='//home/inputs/sample',
        schema=[
            {'name': 'GroupID', 'type': 'string'},
            {'name': 'IdType', 'type': 'string'},
            {'name': 'IdValue', 'type': 'string'},
        ],
    )

    group_features_table = tables.YsonTable(
        file_path='lookalike_features.yson',
        cypress_path='//home/outputs/lookalike_features',
        yson_format='pretty',
    )

    resource_type = 'crypta_look_alike_model'
    resource_file = 'dssm_lal_model.applier'

    os.mkdir(yatest.common.work_path(resource_type))
    os.symlink(yatest.common.work_path(resource_file), yatest.common.work_path(os.path.join(resource_type, resource_file)))

    with mock_sandbox_server_with_resource(yatest.common.work_path(), resource_type, 'stable') as mocked_lookalike_model, \
            mock.patch(
                'crypta.lookalike.lib.python.utils.utils.get_lal_model_source_link',
                return_value=mocked_lookalike_model.get_resource_url(resource_file),
            ):
        return tests.yt_test_func(
            yt_client=yt_client,
            func=functools.partial(
                sample.get_group_features,
                yt_client=yt_client,
                yql_client=yql_client,
                sample_table=sample_table.cypress_path,
                group_features_table=group_features_table.cypress_path,
            ),
            data_path=yatest.common.test_source_path('data'),
            return_result=False,
            input_tables=describe_input + model_version(date) + [
                (sample_table, tests.TableIsNotChanged()),
            ],
            output_tables=[
                (group_features_table, tests.Diff()),
            ],
        )
