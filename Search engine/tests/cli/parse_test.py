from cli_test_utils import check_cli_line


def test_monitoring_command():
    args = check_cli_line("parse local --module yandex_baobab_html_parser --class YandexBaobabHTMLParser")
    assert args.module == "yandex_baobab_html_parser"
    assert args.classname == "YandexBaobabHTMLParser"
