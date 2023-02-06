import functools
import os

import yatest.common

from crypta.affinitive_geo.services.org_embeddings.lib import dssm_vectors
from crypta.affinitive_geo.services.org_embeddings.lib.utils import config
from crypta.lib.python.test_utils.mock_sandbox_server_with_resource import mock_sandbox_server_with_resource
from crypta.lib.python.yt.test_helpers import (
    files,
    tables,
    tests,
)
from crypta.lookalike.proto import yt_node_names_pb2


def get_dssm_features_schema():
    return [
        {'name': 'GroupID', 'type': 'string'},
        {'name': 'segment_float_features', 'type': 'string'},
        {'name': 'segment_affinitive_sites_ids', 'type': 'string'},
        {'name': 'segment_affinitive_apps', 'type': 'string'},
    ]


def test_dssm_vectors(yt_client, yql_client):
    input_table = config.ORGS_DSSM_FEATURES_TABLE
    output_table = config.ORGS_DSSM_VECTORS_TABLE

    resource_path = 'crypta/affinitive_geo/services/org_embeddings/lib/test/sandbox_data'
    resource_type = 'crypta_look_alike_model'
    dssm_file_name = 'dssm_lal_model.applier'

    with mock_sandbox_server_with_resource(
        resources_path=resource_path,
        resource_type=resource_type,
        released='stable',
    ) as mocked_lookalike_model:
        return tests.yt_test_func(
            yt_client=yt_client,
            func=functools.partial(
                dssm_vectors.get,
                yt_client=yt_client,
                yql_client=yql_client,
                input_table=input_table,
                output_table=output_table,
            ),
            data_path=yatest.common.test_source_path('data/test_dssm_vectors'),
            input_tables=[
                (
                    files.YtFile(
                        file_path=yatest.common.build_path(os.path.join(resource_path, resource_type, dssm_file_name)),
                        cypress_path=os.path.join(config.LOOKALIKE_VERSION_DIR, 'dssm_model.applier'),
                        on_write=tables.OnWrite(
                            attributes={
                                yt_node_names_pb2.TYtNodeNames().DssmSandboxLinkAttr: mocked_lookalike_model.get_resource_url(dssm_file_name),
                            },
                        ),
                    ),
                    tests.YtTest(),
                ),
                (
                    tables.get_yson_table_with_schema(
                        file_path='dssm_features.yson',
                        cypress_path=input_table,
                        schema=get_dssm_features_schema(),
                    ),
                    tests.TableIsNotChanged(),
                ),
            ],
            output_tables=[
                (
                    tables.YsonTable(
                        file_path='dssm_vectors.yson',
                        cypress_path=output_table,
                        yson_format='pretty',
                        on_read=tables.OnRead(row_transformer=tests.FloatToStr(precision=1)),
                        on_write=tables.OnWrite(row_transformer=tests.FloatToStr(precision=1)),
                    ),
                    tests.Diff(),
                ),
            ],
        )
