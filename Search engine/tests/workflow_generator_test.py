from datetime import datetime

from testing_utils import read_json_test_data
from workflow_generator import process
from workflow_constants import TEST

import vh


def test_workflow_generator_multiple_request():
    multi_download_request = read_json_test_data("multi_download_request.json")
    basket = read_json_test_data("basket.json")
    serpset_info_file, cross_link_files = process(multi_download_request, basket, {}, TEST, datetime.now())
    assert isinstance(serpset_info_file, vh.File)
    assert len(cross_link_files) == 19

    workflow_instance = vh.dump_workflow_instance(offline=True)
    assert workflow_instance
