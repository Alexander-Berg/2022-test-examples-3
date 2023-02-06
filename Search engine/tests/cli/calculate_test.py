from cli_test_utils import check_cli_line


def test_monitoring_command():
    line = "calculate local --serps --aggregate --yt_proxy arnold.yt.yandex.net --aspect sinsig"
    args = check_cli_line(line)
    assert args.serps
    assert args.aggregate
    assert args.yt_proxy == "arnold.yt.yandex.net"
    assert args.aspect == "sinsig"
