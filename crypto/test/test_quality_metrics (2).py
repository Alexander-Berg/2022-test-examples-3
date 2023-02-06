import functools
import os

import yatest.common

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.prism.lib.config import config
from crypta.prism.lib.test_utils import schemas
from crypta.prism.services.priors.lib import quality_metrics
from crypta.profile.lib import date_helpers


def test_quality_metrics(yt_client, yql_client, date):
    priors = [tables.get_yson_table_with_schema(
        file_path='priors.yson',
        cypress_path=os.path.join(config.PRISM_PRIORS_DIR, table_name),
        schema=schemas.priors_schema,
    ) for table_name in [date_helpers.get_date_from_past(current_date=date, days=1), date]]

    return tests.yt_test_func(
        yt_client=yt_client,
        func=functools.partial(
            quality_metrics.send_correlation_metrics,
            yt_client=yt_client,
            yql_client=yql_client,
            date=date,
        ),
        data_path=yatest.common.test_source_path('data'),
        return_result=False,
        input_tables=[
            (table, tests.TableIsNotChanged()) for table in priors
        ] + [
            (
                tables.get_yson_table_with_schema(
                    file_path='features_and_cluster_by_user.yson',
                    cypress_path=os.path.join(config.FEATURES_AND_CLUSTER_BY_USER_DIR, date),
                    schema=schemas.prior_features_and_cluster_by_user_schema,
                ),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[
            (
                tables.YsonTable(
                    'priors_quality_metrics.yson',
                    config.PRIORS_QUALITY_METRICS,
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
        ],
    )
