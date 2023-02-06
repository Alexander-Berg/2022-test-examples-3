import pytest
import os.path
from yatest import common as yc

from rtmapreduce.tests.yatest import rtmr_test


manifests = {
    "org_rates": {
        "input_tables": ["splitted_ugc_db_update_log"],
        "output_tables": ["org_rates", "ugc_rejects"],
        "expect_empty_tables": ["ugc_rejects"],
    },
}


@pytest.mark.parametrize("task", manifests.keys())
def test_rtmr_actions_exporter(task, tmpdir):
    rtmr_test.init(tmpdir)
    manifest = manifests[task]

    manifest["dynlibs"] = [
        yc.binary_path("extsearch/geo/recommender/tools/ugc_rtmr_processing/dynlib/libugc_rtmr_processing-dynlib.so")
    ]
    manifest["config"] = yc.source_path("extsearch/geo/recommender/tools/ugc_rtmr_processing/tests/" + task + ".cfg")

    return rtmr_test.run(task, manifest, split_files=True, output_format="simple")
