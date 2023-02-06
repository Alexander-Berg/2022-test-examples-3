import functools

import mock
import yatest.common

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.prism.lib.test_utils import schemas
from crypta.prism.services.offline_weighting.lib import (
    table_paths,
    user_weights_formatting,
)


@mock.patch("time.time", mock.MagicMock(return_value=10000000))
def test_user_weights_formatting_to_bigb(custom_output_dir, yt_client, date):
    resolved_table_paths = table_paths.resolve(custom_output_dir, date)

    return tests.yt_test_func(
        yt_client=yt_client,
        func=functools.partial(
            user_weights_formatting.to_bigb_by_date,
            yt_client=yt_client,
            date=date,
            custom_output_dir=custom_output_dir,
        ),
        data_path=yatest.common.test_source_path('data'),
        return_result=False,
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    file_path='user_weights.yson',
                    cypress_path=resolved_table_paths['user_weights'],
                    schema=schemas.user_weights_schema,
                ),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[
            (
                tables.YsonTable(
                    file_path='user_weights_bigb_output.yson',
                    cypress_path=resolved_table_paths['user_weights_bigb'],
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
        ],
    )
