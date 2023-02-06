from dataclasses import dataclass

from test_utils import TestParser


@dataclass()
class ExpectedComponent:

    url: str
    title: str


class TestKinopoiskApphostParser(TestParser):

    def serp(self):
        return self.parse_file('data.json')

    def test_serp(self):
        self._assert_kinopoisk_components(
            self.serp(),
            components=[
                ExpectedComponent(url='https://kinopoisk.ru/name/513', title='Уилл Смит'),
                ExpectedComponent(url='https://kinopoisk.ru/name/21324', title='Уилл Феррелл'),
                ExpectedComponent(url='https://kinopoisk.ru/film/539', title='Умница Уилл Хантинг'),
                ExpectedComponent(url='https://kinopoisk.ru/name/9274', title='Уиллем Дефо'),
                ExpectedComponent(url='https://kinopoisk.ru/film/518184', title='Уилл'),
            ],
        )

    def _assert_kinopoisk_components(self, serp, components: list[ExpectedComponent]):
        assert len(serp['components']) == len(components)
        for component, expected_component in zip(serp['components'], components):
            self._assert_kinopoisk_component(component, expected_component)

    @staticmethod
    def _assert_kinopoisk_component(component: dict, expected_component: ExpectedComponent):
        assert component['type'] == 'COMPONENT'
        assert component['componentUrl']['pageUrl'] == expected_component.url
        assert component['text.title'] == expected_component.title
        assert isinstance(component['text.snippet'], str)

    def test_prepare(self):
        parser = self.get_parser()
        prepared_request = parser.prepare(
            num=0,
            basket_query=self._read_json_file('preparer', '0_input.json'),
            host='apphost.tst.kp.yandex.net',
            additional_parameters=self._read_json_file('preparer', 'config.json'))
        assert prepared_request == self._read_json_file('preparer', '0_output.json')
