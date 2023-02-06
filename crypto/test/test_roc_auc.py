import functools

import yatest.common

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.prism.lib.config import config
from crypta.prism.lib.test_utils import schemas
from crypta.prism.services.training.lib import roc_auc


def test_get(yt_client, date):
    return tests.yt_test_func(
        yt_client=yt_client,
        func=functools.partial(
            roc_auc.get,
            yt_client=yt_client,
            json_input=yatest.common.test_source_path('data/roc_auc.json'),
            date=date,
        ),
        output_tables=[
            (
                tables.get_yson_table_with_schema(
                    file_path='roc_auc.yson',
                    cypress_path=config.DATALENS_REALTIME_PRISM_ROC_AUC_TABLE,
                    schema=schemas.roc_auc_schema,
                ),
                tests.Diff(),
            ),
        ],
    )
