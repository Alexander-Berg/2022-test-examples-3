import yatest.common as common
from yatest.common import process


def test_remote():
    binary_path = common.binary_path(
        "cloud/disk_manager/test/remote/cmd/cmd")
    dm_config = common.get_param("disk-manager-client-config")
    assert dm_config is not None
    nbs_config = common.get_param("nbs-client-config")
    assert nbs_config is not None

    cmd = [
        binary_path,
        "--disk-manager-client-config", common.source_path(dm_config),
        "--nbs-client-config", common.source_path(nbs_config),
    ]
    process.execute(cmd)
