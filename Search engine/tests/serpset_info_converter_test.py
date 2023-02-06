from serpset_info_converter import convert_from_monitoring_to_download_format
from testing_utils import read_json_test_data


def test_monitoring_serp_info_conversion():
    download_request = read_json_test_data("converter_request.json")
    serp_info_monitoring = read_json_test_data("converter_serp_info_monitoring_format.json")[0]

    actual = convert_from_monitoring_to_download_format(download_request, serp_info_monitoring, "2019-12-09T08:00:00.000+03:00")

    expected = read_json_test_data("converter_serp_info_serp_download_format.json")
    assert actual == expected
