import yatest.common


def test_all():

    port = yatest.common.network.get_port
    cmd = [yatest.common.binary_path("market/bootcamp/deep_dive_2022/goltsov_m/http_server/http_server")]
    cmd.extend(['-c', "cfg.pb.txt"])
    cmd.extend(['-p', port])

    yatest.common.binary_path
    yatest.common.execute
