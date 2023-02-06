from cli_test_utils import check_cli_line


def test_monitoring_command():
    line = "prepare local --configuration preparer-configuration.json hamster.yandex.ru"
    args = check_cli_line(line)
    assert args.host == "hamster.yandex.ru"
    assert args.configuration == "preparer-configuration.json"
    assert args.preparer == "YandexBaobabHTMLParser"
