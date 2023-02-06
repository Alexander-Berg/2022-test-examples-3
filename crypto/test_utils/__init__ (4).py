from crypta.lib.python.test_utils.mock_sandbox_server_with_resource import mock_sandbox_server_with_resource
from crypta.lib.python.yt.test_helpers import tables
from crypta.lib.python.yt.test_helpers.tests import FloatToStr


resources_path = 'crypta/siberia/bin/custom_audience/lib/python/clustering/test_utils/sandbox_data'


def mock_sandbox_dssm_lookalike_model():
    return mock_sandbox_server_with_resource(resources_path, 'crypta_look_alike_model', 'stable')


def get_dssm_lookalike_url(mocked_sandbox_resource):
    return mocked_sandbox_resource.get_resource_url('dssm_lal_model.applier')


def yson_table_with_float_values(file_path, cypress_path, attributes=None, yson_format='pretty'):
    return tables.YsonTable(
        file_path,
        cypress_path,
        yson_format=yson_format,
        on_read=tables.OnRead(row_transformer=FloatToStr(precision=1)),
        on_write=tables.OnWrite(
            attributes=attributes,
            row_transformer=FloatToStr(precision=1)
        ),
    )
