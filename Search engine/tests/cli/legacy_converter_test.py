from cli_test_utils import check_cli_line


def test_monitoring_command():
    line = "tool lc local --ssr config.json --prepare --preparer YandexBaobabHTMLParser"
    args = check_cli_line(line)
    assert args.ssr == "config.json"
    assert args.prepare
    assert args.preparer == "YandexBaobabHTMLParser"
