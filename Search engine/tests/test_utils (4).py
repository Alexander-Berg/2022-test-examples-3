import codecs
import importlib
import inspect
import json
import os
from urllib.parse import urlparse, parse_qs
from typing import TypeVar, Generic

from base_parsers import YandexJSONSerpParser

ComponentTypes = YandexJSONSerpParser.MetricsMagicNumbers.ComponentTypes
Alignments = YandexJSONSerpParser.MetricsMagicNumbers.Alignments
WizardTypes = YandexJSONSerpParser.MetricsMagicNumbers.WizardTypes

T = TypeVar("T")


class TestParser(Generic[T]):
    """
    This is a base class for parser tests, not a dummy parser to be tested.
    """
    _parser_class = None
    _parser_instance = None

    @classmethod
    def _get_parser_class_name(cls):
        """E.g. 'YandexIznankaGeoJSONParser'"""
        assert cls.__name__.startswith("Test")
        return cls.__name__[4:]

    @classmethod
    def _get_parser_test_module_name(cls):
        """E.g. 'yandex_iznanka_geo_json_parser_test'"""
        class_full_path = inspect.getfile(cls)
        class_filename = os.path.basename(class_full_path)
        return os.path.splitext(class_filename)[0]

    @classmethod
    def _get_parser_module_name(cls):
        """E.g. 'yandex_iznanka_geo_json_parser'"""
        parser_test_module_name = cls._get_parser_test_module_name()
        assert parser_test_module_name.endswith("_test")
        return parser_test_module_name[:-5]

    @classmethod
    def _get_parser_class(cls):
        """E.g. YandexIznankaGeoJSONParser"""
        if cls._parser_class:
            return cls._parser_class
        module_name = cls._get_parser_module_name()
        parser_module = importlib.import_module(module_name)
        parser_class_name = cls._get_parser_class_name()
        parser_class = getattr(parser_module, parser_class_name)
        assert callable(getattr(parser_class, "parse")), "A parser class should provide a .parse method."
        cls._parser_class = parser_class
        return parser_class

    @classmethod
    def _get_parser_test_data_directory_name(cls):
        """E.g. 'yandex_iznanka_geo_json_parser_data'"""
        return "{}_data".format(cls._get_parser_module_name())

    @classmethod
    def _read_file(cls, *path_components, strip=False):
        content = load_fixture_file(cls._get_parser_test_data_directory_name(), *path_components)
        if strip:
            return content.strip()
        return content

    @classmethod
    def _read_json_file(cls, *path_components):
        return json.loads(cls._read_file(*path_components))

    def get_parser(self, module_name: str = None, class_name: str = None) -> T:
        """Returns new instance of parser under test."""
        if module_name and class_name:
            self.set_parser_instance(module_name=module_name, class_name=class_name)

        if not self._parser_instance:
            parser_class = self._get_parser_class()
            self._parser_instance = parser_class()
        return self._parser_instance

    def parse(self, string, additional_parameters={}, module_name: str = None, class_name: str = None):
        """Parse the given string."""
        return self.get_parser(module_name=module_name, class_name=class_name).parse(string, additional_parameters)

    def parse_file(self, *path_components, additional_parameters={}):
        """Parse the given file from the corresponding parser's test directory."""
        return self.parse(self._read_file(*path_components), additional_parameters)

    def parse_file_with_base_url(self, base_url, *path_components):
        return self.parse(self._read_file(*path_components), {"url": base_url})

    def read_components(self, filename):
        return self.parse_file(filename)['components']

    def prepare(self, num=0, query={}, host='yandex.ru', additional_parameters={}):
        return self._get_parser_class()().prepare(num, query, host, additional_parameters)

    def get_basket_query(self, input_filename):
        return self._read_json_file("preparer", input_filename)

    def set_parser_instance(self, module_name: str, class_name: str) -> None:
        parser_module = importlib.import_module(module_name)
        self._parser_instance = getattr(parser_module, class_name)()

    def compare_preparer_output(self, input_filename, expected_output_filename, host="yandex.ru"):
        basket_query = self.get_basket_query(input_filename)
        prepared = self._get_parser_class()().prepare(0, basket_query, host)
        expected = self._read_json_file("preparer", expected_output_filename)
        self.compare_preparer_content(expected, prepared)

    def compare_preparer_content(self, expected, prepared):
        for key in prepared.keys():
            if key in {"cookies", "headers"}:
                assert sorted(prepared[key]) == sorted(expected[key])
            elif key == 'userdata':
                assert json.loads(prepared[key]) == json.loads(expected[key])
            else:
                assert prepared[key] == expected[key], "preparer result for key '{}' is wrong. expected: '{}', actual: '{}'".format(key, expected[key], prepared[key])

    @classmethod
    def _get_search_results(cls, components):
        return [c for c in components if c.get("componentInfo", {}).get("type") == ComponentTypes.SEARCH_RESULT]

    def _check_wizard_type(self, component, wizard_type):
        return (component["componentInfo"]["type"] == ComponentTypes.WIZARD and
                component["componentInfo"]["wizardType"] == wizard_type)

    def _wizard_is_present(self, components, wizard_type):
        return any(self._check_wizard_type(c, wizard_type) for c in components)

    def _get_wizard_components(self, components, wizard_type):
        return [component for component in components if self._check_wizard_type(component, wizard_type)]

    @staticmethod
    def _get_page_url(component):
        return component["componentUrl"]["pageUrl"]


def compare_urls(url_1, url_2):
    augmented = urlparse(url_1)
    expected = urlparse(url_2)
    assert augmented.scheme == expected.scheme
    assert augmented.netloc == expected.netloc
    assert augmented.params == expected.params
    assert augmented.path == expected.path
    assert parse_qs(augmented.query) == parse_qs(expected.query)
    assert augmented.fragment == expected.fragment


def load_fixture_file(*path_components):
    resource_path = os.path.join(
        os.path.dirname(os.path.realpath(__file__)),
        *path_components
    )

    with codecs.open(resource_path, encoding='utf-8') as data_file:
        return data_file.read()


def read_json_from_fixture_file(*path_components):
    return json.loads(load_fixture_file(*path_components))


def cleanup_falsy(input):
    if isinstance(input, dict):
        empty = []
        for k, v in input.items():
            if v:
                cleanup_falsy(v)
            if not v:
                empty.append(k)
        for k in empty:
            del input[k]
    elif isinstance(input, list):
        for x in input:
            cleanup_falsy(x)
    return input


def convert_json_to_single_line(json_string):
    return json.dumps(json.loads(json_string))


def load_as_single_line_JSON(filename):
    """Load JSON from file and serialize as a single line JSON string

    It's handy to store JSON test assets as formatted files, but yt_mapper_wrapper.py
    expects each input on a separate single line."""

    return convert_json_to_single_line(load_fixture_file("yt_mapper_wrapper_test_data", filename))


class ThisIsAnErrorFromParser(Exception):
    pass


class ExceptionThrowingParser:
    def parse(self, input_string, additional_parameters={}):
        raise ThisIsAnErrorFromParser("ThisIsAnErrorFromParser")
