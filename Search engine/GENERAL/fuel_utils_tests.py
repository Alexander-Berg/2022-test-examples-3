import lxml.etree as ET
import unittest

from search.geo.tools.fuel_snippet.lib.fuel_utils import (
    NAMESPACES,
    MultigoFuelSnippet,
    YandexFuelSnippet,
    construct_attribution,
    construct_fuel_item,
    construct_fuel_json,
    construct_fuel_xml,
    load_from_xml,
    load_from_yson,
    select_best_snippet,
)


FUELS = [
    {'Name': 'АИ 95', 'Cost': 43.55},
    {'Name': 'АИ 92', 'Cost': 44.35}
]


class TestConstructAttribution(unittest.TestCase):
    def test_given_data_returns_attribution(self):
        attribution = {
            'author': {
                'name': 'MultiGo',
                'uri': 'multigo.ru'
            }
        }
        self.assertEqual(
            construct_attribution('MultiGo', 'multigo.ru'),
            attribution
        )


class TestConstructFuelItem(unittest.TestCase):
    def test_given_name_cost_returns_fuel_item(self):
        fuel_item = {
            'name': "Пропан",
            'price': {
                'value': 26.5,
                'text': '26.5 ₽',
                'currency': 'RUB'
            }
        }
        self.assertEqual(
            construct_fuel_item("Пропан", 26.5),
            fuel_item
        )


class TestConstructFuelJson(unittest.TestCase):
    def test_given_data_returns_fuel_json(self):
        attribution = construct_attribution('MultiGo', 'multigo.ru')
        timestamp = 1558359347
        fuels = construct_fuel_item('Пропан', 26.5)

        expected_json = {
            'timestamp': timestamp,
            'fuel': fuels,
            'attribution': attribution
        }

        self.assertEqual(
            construct_fuel_json(timestamp, fuels, attribution),
            expected_json
        )


class TestLoadFromYson(unittest.TestCase):
    def test_given_none_returns_none(self):
        snippet = load_from_yson("1", None, timestamp=0)
        self.assertIsNone(snippet)

    def test_given_empty_list_returns_none(self):
        snippet = load_from_yson("1", [], timestamp=0)
        self.assertIsNone(snippet)

    def test_given_fuel_without_cost_returns_none(self):
        yson = [{
            "Name": "ПРОПАН"
        }]
        snippet = load_from_yson("1", yson, timestamp=0)
        self.assertIsNone(snippet)

    def test_given_fuel_without_name_returns_none(self):
        yson = [{
            "Cost": 18.5,
        }]
        snippet = load_from_yson("1", yson, timestamp=0)
        self.assertIsNone(snippet)

    def test_given_fuel_returns_snippet(self):
        yson = [{
            "Cost": 18.5,
            "Name": "ПРОПАН"
        }]
        snippet = load_from_yson("1", yson, timestamp=1558359347)
        self.assertIs(type(snippet), YandexFuelSnippet)


class TestLoadFromXml(unittest.TestCase):
    def test_given_none_returns_none(self):
        snippet = load_from_xml(None)
        self.assertIsNone(snippet)

    def test_given_xml_returns_snippet(self):
        timestamp = 1575320400
        xml = construct_fuel_xml(timestamp, FUELS)
        snippet = load_from_xml(xml)
        self.assertIs(type(snippet), MultigoFuelSnippet)

    def test_given_zero_prices_removes_them(self):
        fuels = [
            {'Name': 'АИ 76', 'Cost': 0.0},
            {'Name': 'ПРОПАН', 'Cost': 42.0}
        ]
        timestamp = 1575320400

        snippet = load_from_xml(construct_fuel_xml(timestamp, fuels))

        self.assertFalse(snippet.xml_root.xpath(
            "*/fuel:name[text() = 'АИ 76']",
            namespaces=NAMESPACES
        ))

    def test_given_only_zero_prices_returns_none(self):
        fuels = [
            {'Name': 'АИ 76', 'Cost': 0.0},
            {'Name': 'ПРОПАН', 'Cost': 0.0}
        ]
        timestamp = 1575320400

        snippet = load_from_xml(construct_fuel_xml(timestamp, fuels))
        self.assertIsNone(snippet)


class TestSelectBestSnippet(unittest.TestCase):
    def test_given_xml_and_none_returns_multigo_snippet(self):
        timestamp = 1575320400
        row = [1, construct_fuel_xml(timestamp, FUELS), None]
        permalink, snippet = select_best_snippet(row, yson_timestamp=0)
        self.assertIs(type(snippet), MultigoFuelSnippet)
        self.assertEqual(permalink, 1)

    def test_given_none_and_yson_returns_yandex_snippet(self):
        yson = [{
            "Cost": 18.5,
            "Date": 1558359347,
            "Name": "ПРОПАН"
        }]
        row = [1, None, yson]
        permalink, snippet = select_best_snippet(row, yson_timestamp=0)
        self.assertIs(type(snippet), YandexFuelSnippet)
        self.assertEqual(permalink, 1)

    def test_given_xml_and_yson_returns_snippet_with_max_timestamp(self):
        timestamp = 1575320400
        yson_timestamp = timestamp + 1
        yson = [{
            "Cost": 18.5,
            "Name": "ПРОПАН"
        }]
        row = [1, construct_fuel_xml(timestamp, FUELS), yson]
        permalink, snippet = select_best_snippet(row, yson_timestamp)
        self.assertIs(type(snippet), YandexFuelSnippet)
        self.assertEqual(permalink, 1)


class TestMultigoFuelSnippet(unittest.TestCase):
    def test_given_xml_returns_json(self):
        timestamp = 1575320400
        fuel_items = [
            construct_fuel_item(v['Name'], v['Cost']) for v in FUELS
        ]
        xml = construct_fuel_xml(timestamp, FUELS)
        snippet = load_from_xml(xml)
        self.assertEqual(
            snippet.as_json(),
            construct_fuel_json(
                timestamp,
                fuel_items,
                MultigoFuelSnippet.attribution
            )
        )


class TestYandexFuelSnippetAsJson(unittest.TestCase):
    def test_given_yson_returns_json(self):
        timestamp = 1575320400
        yson = [
            {
                "Cost": 26.45,
                "Name": "ПРОПАН"
            }
        ]
        fuels = [construct_fuel_item("ПРОПАН", 26.45)]
        snippet = load_from_yson(1, yson, timestamp)
        self.assertEqual(
            snippet.as_json(),
            construct_fuel_json(
                timestamp,
                fuels,
                YandexFuelSnippet.attribution
            )
        )


def reformat_xml(xml):
    parser = ET.XMLParser(remove_blank_text=True)
    root = ET.XML(xml, parser=parser)
    return ET.tostring(root, encoding='UTF-8')


class TestYandexFuelSnippetAsXml(unittest.TestCase):
    def test_given_yson_returns_xml(self):
        timestamp = 1575320400
        ai95 = {'Name': 'АИ 95', 'Cost': 43.55}
        ai92 = {'Name': 'АИ 92', 'Cost': 44.35}
        expected_xml = reformat_xml(
            f'''<FuelInfo xmlns=\"http://maps.yandex.ru/snippets/fuel/1.x\">
                <timestamp>{timestamp}</timestamp>
                <Fuel>
                    <name>{ai95['Name']}</name>
                    <Price>
                        <value>{ai95['Cost']}</value>
                        <text>{ai95['Cost']} ₽</text>
                        <currency>RUB</currency>
                    </Price>
                </Fuel>
                <Fuel>
                    <name>{ai92['Name']}</name>
                    <Price>
                        <value>{ai92['Cost']}</value>
                        <text>{ai92['Cost']} ₽</text>
                        <currency>RUB</currency>
                    </Price>
                </Fuel>
            </FuelInfo>''')
        snippet = load_from_yson(1, [ai95, ai92], timestamp)
        self.assertEqual(snippet.as_xml(), expected_xml)
