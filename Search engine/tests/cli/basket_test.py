from cli_test_utils import check_cli_line


def test_monitoring_command():
    line = "qg local --url https://metrics-qgaas.metrics.yandex-team.ru/api/query/{} 296146 " \
           "--configuration preparer-configuration.json"
    args = check_cli_line(line)
    assert args.url == "https://metrics-qgaas.metrics.yandex-team.ru/api/query/{}"
