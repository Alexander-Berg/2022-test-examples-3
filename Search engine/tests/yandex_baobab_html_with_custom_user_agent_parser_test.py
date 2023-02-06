import pytest

from base_parsers import JSONSerpParser
from test_utils import TestParser

WizardTypes = JSONSerpParser.MetricsMagicNumbers.WizardTypes
ComponentTypes = JSONSerpParser.MetricsMagicNumbers.ComponentTypes

from yandex_baobab.yandex_baobab_html_with_custom_user_agent_parser import YandexBaobabHTMLParserWithCustomUserAgent


class TestYandexBaobabHTMLParserWithCustomUserAgent(TestParser):
    _parser_class = YandexBaobabHTMLParserWithCustomUserAgent

    @pytest.mark.parametrize("user_agent, expected", [
        ("Windows 121", "User-Agent: Windows 121"),
        (None, "User-Agent: Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) "
               "Chrome/91.0.4472.77 YaBrowser/21.5.0 Yowser/2.5 Safari/537.36")
    ])
    def test_prepare(self, user_agent, expected):
        query = {
            "text": "text",
            "region": {"id": 0}
        }
        if user_agent:
            query["per-query-parameters"] = {"additional-parameters": {"scraper-custom-user-agent": [user_agent]}}
        assert self.prepare(query=query)["headers"][2] == expected
