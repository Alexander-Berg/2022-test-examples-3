import pytest
import yatest.common

from crypta.s2s.services.conversions_processor.lib.processor import cdp_api_client


@pytest.mark.parametrize("lines_per_file", [
    1,
    4,
    5,
    6,
])
def test_prepare_csv_parts(lines_per_file):
    filepath = yatest.common.test_source_path("data/to_upload.csv")
    return {
        filename: body
        for filename, body in cdp_api_client.prepare_csv_parts(filepath, lines_per_file)
    }
