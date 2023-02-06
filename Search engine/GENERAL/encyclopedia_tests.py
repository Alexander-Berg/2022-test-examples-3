import unittest
import base64
import urllib
import xml.parsers.expat as expat

from google.protobuf import json_format

import yandex.maps.proto.common2.metadata_pb2 as pb_metadata
import yandex.maps.proto.search.encyclopedia_pb2 as pb_encyclopedia
from search.geo.tools.task_manager.generators.encyclopedia.lib.make_encyclopedia_snippet import make_encyclopedia_snippet


INVALID_JSON_INSIDE_CDATA = '''
    <EntitySearch>
        <![CDATA[
            {
                \"cards\": [
                    {
                        \"base_info\": {
                            \"title\":\"test title
                            \"description\":\"test description\",
                        }
                    }
                ]
            }
        ]]>
    </EntitySearch>
'''


TEST_XML_SNIPPET = '''
    <EntitySearch>
        <![CDATA[
            {
                \"cards\": [
                    {
                        \"base_info\": {
                            \"title\":\"test title\",
                            \"description\":\"test description\",
                            \"description_source\": {
                                \"name\":\"test name\",
                                \"url\":\"http://test-url.com\"
                            }
                        }
                    }
                ]
            }
        ]]>
    </EntitySearch>
'''

SNIPPET_WITH_WIKI_FACTS = '''
    <EntitySearch>
        <![CDATA[
            {
                \"cards\": [
                    {
                        \"base_info\": {
                            \"title\": \"Сиг\",
                            \"description\": \"Озеро в Осташковском районе Тверской области\",
                            \"description_source\": {
                                \"name\": \"Википедия\",
                                \"url\": \"http://ru.wikipedia.org/wiki/Сиг (озеро)\"
                            }
                        },
                        \"wiki_snippet\": {
                            \"item\": [
                                {
                                    \"name\": \"Площадь\",
                                    \"value\": [
                                        {
                                            \"text\": \"12,1 км²\",
                                            \"_source_code\": 0
                                        }
                                    ],
                                    \"key\": \"Area@on\"
                                },
                                {
                                    \"name\": \"Максимальная глубина\",
                                    \"value\": [
                                        {
                                            \"text\": \"5 м\",
                                            \"_source_code\" :0
                                        }
                                    ],
                                    \"key\": \"MaxDepth@on\"
                                },
                                {
                                    \"name\": \"Смотри также\",
                                    \"value\": [
                                        {
                                            \"text\": \"Озеро Волго\",
                                            \"link_text\": \"Волго\",
                                            \"search_request\": \"Волго (озеро)\",
                                            \"_source_code\" :0
                                        },
                                        {
                                            \"text\": \"Селигер\",
                                            \"link_text\": \"Селигер\",
                                            \"search_request\": \"Озеро Селигер\",
                                            \"url\": \"https://www.tourister.ru/world/europe/russia/city/ostashkov/lakes/28998\",
                                            \"_source_code\" :0
                                        },
                                        {
                                            \"text\": \"Обыкновенный сиг (рыба)\",
                                            \"link_text\": \"Обыкновенный сиг\",
                                            \"url\": \"https://ribxoz.ru/sig-kak-i-gde-lovit-osobennosti/\",
                                            \"_source_code\" :0
                                        }
                                    ],
                                    \"key\": \"SeeAlso@on\"
                                }
                            ]
                        }
                    }
                ]
            }
        ]]>
    </EntitySearch>
'''


def make_url_from_request(request):
    return 'https://yandex.ru/search/?text=' + urllib.quote(request)


def make_snippet_pb_object(xml_snippet):
    serialized_snippet = make_encyclopedia_snippet(xml_snippet)
    metadata = pb_metadata.Metadata()
    metadata.ParseFromString(base64.b64decode(serialized_snippet))
    return metadata.Extensions[pb_encyclopedia.ENCYCLOPEDIA_METADATA]


def add_fact(snippet, fact_dict):
    fact = snippet.fact.add()
    json_format.ParseDict(fact_dict, fact)


class EncyclopediaTester(unittest.TestCase):
    def test_exceptions(self):
        not_an_xml_string = 'dummy string'
        self.assertRaises(expat.ExpatError, make_encyclopedia_snippet, not_an_xml_string)

        xml_without_entitysearch = '<DummySection>dummy section content</DummySection>'
        self.assertRaises(IndexError, make_encyclopedia_snippet, xml_without_entitysearch)

        entitysearch_without_cdata = '<EntitySearch>no CDATA section here</EntitySearch>'
        self.assertRaises(ValueError, make_encyclopedia_snippet, entitysearch_without_cdata)

        self.assertRaises(ValueError, make_encyclopedia_snippet, INVALID_JSON_INSIDE_CDATA)

    def test_empty_snippet(self):
        empty_snippet_str = '<EntitySearch><![CDATA[{}]]></EntitySearch>'
        self.assertEqual(make_snippet_pb_object(empty_snippet_str), pb_encyclopedia.EncyclopediaMetadata())

    def test_valid_snippet(self):
        expected_snippet = pb_encyclopedia.EncyclopediaMetadata()
        expected_snippet.title = 'test title'
        expected_snippet.description = 'test description'
        expected_snippet.attribution.author.name = 'test name'
        expected_snippet.attribution.link.href = 'http://test-url.com'

        result_snippet = make_snippet_pb_object(TEST_XML_SNIPPET)

        self.assertEqual(expected_snippet, result_snippet)

    def test_snippet_with_facts(self):
        expected_snippet = pb_encyclopedia.EncyclopediaMetadata()
        expected_snippet.title = 'Сиг'
        expected_snippet.description = 'Озеро в Осташковском районе Тверской области'
        expected_snippet.attribution.author.name = 'Википедия'
        expected_snippet.attribution.link.href = 'http://ru.wikipedia.org/wiki/Сиг (озеро)'

        area_value = [
            {
                'title': {
                    'text': '12,1 км²',
                    'span': []
                }
            }
        ]
        add_fact(expected_snippet, {'name': 'Площадь', 'value': area_value})

        max_depth_value = [
            {
                'title': {
                    'text': '5 м',
                    'span': []
                }
            }
        ]
        add_fact(expected_snippet, {'name': 'Максимальная глубина', 'value': max_depth_value})

        see_also_values = [
            {
                'title': {
                    'text': 'Озеро Волго',
                    'span': [
                        {
                            'begin': 6,
                            'end': 11
                        }
                    ]
                },
                'url': make_url_from_request('Волго (озеро)')
            },
            {
                'title': {
                    'text': 'Селигер',
                    'span': [
                        {
                            'begin': 0,
                            'end': 7
                        }
                    ]
                },
                'url': make_url_from_request('Озеро Селигер')
            },
            {
                'title': {
                    'text': 'Обыкновенный сиг (рыба)',
                    'span': [
                        {
                            'begin': 0,
                            'end': 16
                        }
                    ]
                },
                'url': 'https://ribxoz.ru/sig-kak-i-gde-lovit-osobennosti/'
            }
        ]
        add_fact(expected_snippet, {'name': 'Смотри также', 'value': see_also_values})

        result_snippet = make_snippet_pb_object(SNIPPET_WITH_WIKI_FACTS)

        self.assertEqual(expected_snippet, result_snippet)
