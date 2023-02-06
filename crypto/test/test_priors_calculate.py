import functools
import os

import mock
import yatest.common

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.prism.lib.config import config
from crypta.prism.lib.test_utils import schemas
from crypta.prism.services.priors.lib import priors
from crypta.profile.lib import date_helpers


def replace_date_transformer(date):
    def row_transformer(row):
        row['date'] = date
        return row

    return row_transformer


@mock.patch.object(config, 'MIN_PRIOR_SIZE', 1)
def test_priors_calculate(yt_client, yql_client, date):
    features_and_cluster_by_user_dir = '//inputs/features_and_cluster_by_user'
    features_and_cluster_by_user = [tables.YsonTable(
        file_path='features_and_cluster_by_user.yson',
        cypress_path=os.path.join(
            features_and_cluster_by_user_dir,
            date_helpers.get_date_from_past(date, days=days),
        ),
        on_write=tables.OnWrite(
            attributes={
                'schema': schemas.prior_features_and_cluster_by_user_schema,
            },
            row_transformer=replace_date_transformer(date_helpers.get_date_from_past(date, days=days)),
        ),
    ) for days in range(config.PRIOR_SAMPLE_PERIOD_DAYS)]

    priors_output = tables.YsonTable(
        file_path='priors.yson',
        cypress_path='//outputs/priors',
        yson_format='pretty',
    )

    return tests.yt_test_func(
        yt_client=yt_client,
        func=functools.partial(
            priors.calculate,
            yt_client=yt_client,
            yql_client=yql_client,
            date=date,
            output_table=priors_output.cypress_path,
            features_and_cluster_by_user_dir=features_and_cluster_by_user_dir,
        ),
        data_path=yatest.common.test_source_path('data'),
        return_result=False,
        input_tables=[
            (table, tests.TableIsNotChanged()) for table in features_and_cluster_by_user
        ] + [
            (
                tables.get_yson_table_with_schema(
                    file_path='prism_segment_weights.yson',
                    cypress_path=config.PRISM_CLUSTER_MAPPING_TABLE,
                    schema=schemas.prism_segment_weights_schema,
                ),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[(priors_output, tests.Diff())],
    )
