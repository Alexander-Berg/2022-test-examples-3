import functools

import yatest.common

from crypta.affinitive_geo.services.org_embeddings.lib import user_data_stats
from crypta.affinitive_geo.services.org_embeddings.lib.utils import config
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.siberia.bin.make_id_to_crypta_id.lib.maker.id_to_crypta_id_pb2 import TIdToCryptaId


def get_orgvisits_for_description_schema():
    return [
        {'name': 'GroupID', 'type': 'string'},
        {'name': 'IdType', 'type': 'string'},
        {'name': 'IdValue', 'type': 'string'},
    ]


def test_user_data_stats(yt_client, crypta_id_user_data_table):
    input_table = config.ORGVISITS_FOR_DESCRIPTION_TABLE
    output_table = config.ORGS_USER_DATA_STATS_TABLE

    return tests.yt_test_func(
        yt_client=yt_client,
        func=functools.partial(
            user_data_stats.get,
            yt_client=yt_client,
            input_table=input_table,
            output_table=output_table,
        ),
        data_path=yatest.common.test_source_path('data/test_user_data_stats'),
        input_tables=[
            (
                tables.YsonTable(
                    'id_to_crypta_id.yson',
                    '//home/crypta/production/siberia/id_to_crypta_id',
                    on_write=tables.OnWrite(
                        sort_by=['id', 'id_type'],
                        attributes={'schema': schema_utils.get_schema_from_proto(TIdToCryptaId)},
                    ),
                ),
                [tests.TableIsNotChanged()],
            ),
            (
                crypta_id_user_data_table(
                    'crypta_id_user_data.yson',
                    '//home/crypta/production/siberia/custom/crypta_id_user_data/by_crypta_id',
                ),
                tests.TableIsNotChanged(),
            ),
            (
                tables.get_yson_table_with_schema(
                    file_path='orgvisits_for_description.yson',
                    cypress_path=input_table,
                    schema=get_orgvisits_for_description_schema(),
                ),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[
            (
                tables.YsonTable(
                    file_path='user_data_stats.yson',
                    cypress_path=output_table,
                    yson_format='pretty',
                ),
                tests.Diff(),
            ),
        ],
    )
