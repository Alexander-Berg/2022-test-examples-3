# encoding: UTF-8

import unittest

import mock

from ws_properties.conversion.converter import Converter, ConversionError, \
    ToEnumConverter, ToDateTimeConverter, ToDateConverter, ToTimeConverter, \
    ToTimeDeltaConverter, ToBoolConverter, ToIntegerConverter, ToFloatConverter
from ws_properties.conversion.service import ConversionService, \
    StandardConversionService


class ConversionServiceTestCase(unittest.TestCase):
    class NopConverter(Converter):
        def convert(self, source, target_type, **hints):
            """Will be mocked"""

    def test_converts_none_to_none(self):
        for type in (int, str, float, unicode):
            service = ConversionService()
            converter = self.NopConverter()
            converter.convert = mock.MagicMock()
            service.converters.append(converter)
            self.assertIs(
                service.convert(None, type),
                None,
            )
            converter.convert.assert_not_called()

    def test_converts_value_to_itself(self):
        samples = [
            (1, int),
            (1., float),
            (True, bool),
            ('test', str),
            (u'test', unicode),
        ]
        for source, type in samples:
            service = ConversionService()
            converter = self.NopConverter()
            converter.convert = mock.MagicMock()
            service.converters.append(converter)
            self.assertIs(
                service.convert(source, type),
                source,
            )
            converter.convert.assert_not_called()

    def test_call_converters(self):
        failed_converter = self.NopConverter()
        failed_converter.convert = mock.Mock(side_effect=ConversionError())

        success_converter = self.NopConverter()
        success_converter.convert = mock.Mock(return_value='success')

        service = ConversionService()
        service.converters.extend([failed_converter, success_converter])
        self.assertEqual(
            service.convert(1, float),
            'success',
        )
        failed_converter.convert.assert_called_once_with(1, float)
        success_converter.convert.assert_called_once_with(1, float)

    def test_raises(self):
        failed_converter1 = self.NopConverter()
        failed_converter1.convert = mock.Mock(side_effect=ConversionError())

        failed_converter2 = self.NopConverter()
        failed_converter2.convert = mock.Mock(side_effect=ConversionError())

        service = ConversionService()
        service.converters.extend([failed_converter1, failed_converter2])
        with self.assertRaises(ConversionError):
            service.convert(1, float),
        failed_converter1.convert.assert_called_once_with(1, float)
        failed_converter2.convert.assert_called_once_with(1, float)


class StandardConversionServiceTestCase(unittest.TestCase):
    def test_has_default_converters(self):
        service = StandardConversionService()

        expected_converter_types = {
            ToEnumConverter,
            ToDateTimeConverter,
            ToDateConverter,
            ToTimeConverter,
            ToTimeDeltaConverter,
            ToBoolConverter,
            ToIntegerConverter,
            ToFloatConverter,
        }

        found_converter_types = {
            type(converter) for converter in service.converters
        }

        self.assertSetEqual(
            expected_converter_types,
            found_converter_types,
        )
