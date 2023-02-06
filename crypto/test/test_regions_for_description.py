import functools

import yatest.common

from crypta.affinitive_geo.services.org_embeddings.lib import regions_for_description
from crypta.affinitive_geo.services.org_embeddings.lib.utils import (
    config,
    test_utils,
)
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


def test_regions_for_description(yt_client, yql_client, user_data_table):
    return tests.yt_test_func(
        yt_client=yt_client,
        func=functools.partial(
            regions_for_description.get,
            yt_client=yt_client,
            yql_client=yql_client,
        ),
        data_path=yatest.common.test_source_path('data/test_regions_for_description'),
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    file_path='orgs_info.yson',
                    cypress_path=config.ORGS_INFO_TABLE,
                    schema=test_utils.get_orgs_info_schema(),
                ),
                tests.TableIsNotChanged(),
            ),
            (
                user_data_table('cryptaid_userdata.yson', config.CRYPTAID_USERDATA_TABLE),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[
            (
                tables.YsonTable(
                    file_path='regions_for_description.yson',
                    cypress_path=config.REGIONS_FOR_DESCRIPTION_TABLE,
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
        ],
    )
