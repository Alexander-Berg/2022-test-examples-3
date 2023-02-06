# encoding: UTF-8

import datetime
import unittest

import enum
import isodate
import mock

from ws_properties.conversion.converter import ToDateTimeConverter, \
    ConversionError, ToDateConverter, ToTimeConverter, ToBoolConverter, \
    ToIntegerConverter, ToFloatConverter, ToEnumConverter, ToTimeDeltaConverter, \
    AbstractConverter, Converter, CompositeConverter


class AbstractConverterTestCase(unittest.TestCase):
    class SampleConverter(AbstractConverter):
        def _can_convert(self, source_type, target_type):
            """Will be mocked"""

        def _do_convert(self, source, target_type, **hints):
            """Will be mocked"""

    def test_converts_none_to_none(self):
        converter = self.SampleConverter()
        converter._can_convert = mock.Mock()
        converter._do_convert = mock.Mock()
        for type in (int, str, float, unicode):
            self.assertIs(
                converter.convert(None, type),
                None,
            )
        converter._can_convert.assert_not_called()
        converter._do_convert.assert_not_called()

    def test_failed_check_prevents_conversion(self):
        converter = self.SampleConverter()
        converter._can_convert = mock.Mock(return_value=False)
        converter._do_convert = mock.Mock()
        with self.assertRaises(ConversionError):
            converter.convert('test', int),
        converter._can_convert.assert_called_once_with(str, int)
        converter._do_convert.assert_not_called()

    def test_success_check_fires_conversion(self):
        converter = self.SampleConverter()
        converter._can_convert = mock.Mock(return_value=True)
        converter._do_convert = mock.Mock(return_value=1.0)
        self.assertEqual(
            converter.convert('test', int),
            1.0,
        )
        converter._can_convert.assert_called_once_with(str, int)
        converter._do_convert.assert_called_once_with('test', int)

class ToDateTimeConverterTestCase(unittest.TestCase):
    def setUp(self):
        self.converter = ToDateTimeConverter()
        self.source = datetime.datetime.utcnow().replace(microsecond=0)
        self.str_source = isodate.datetime_isoformat(self.source)

    def test_converts_none_to_none(self):
        self.assertIs(
            self.converter.convert(None, datetime.datetime),
            None,
        )

    def test_converts_date(self):
        target = self.converter.convert(self.str_source, datetime.datetime)
        self.assertEqual(self.source, target)

    def test_raises_on_date(self):
        with self.assertRaises(ConversionError):
            self.converter.convert(self.str_source, datetime.date)

    def test_raises_on_time(self):
        with self.assertRaises(ConversionError):
            self.converter.convert(self.str_source, datetime.time)

    def test_raises_on_invalid_target_type(self):
        with self.assertRaises(ConversionError):
            self.converter.convert(self.str_source, int)

    def test_raises_on_invalid_source(self):
        with self.assertRaises(ConversionError):
            self.converter.convert('invalid source', datetime.datetime)


class ToDateConverterTestCase(unittest.TestCase):
    def setUp(self):
        self.converter = ToDateConverter()
        self.source = datetime.datetime.utcnow().date()
        self.str_source = isodate.date_isoformat(self.source)

    def test_converts_none_to_none(self):
        self.assertIs(
            self.converter.convert(None, datetime.date),
            None,
        )

    def test_converts_date(self):
        target = self.converter.convert(self.str_source, datetime.date)
        self.assertEqual(self.source, target)

    def test_raises_on_datetime(self):
        with self.assertRaises(ConversionError):
            self.converter.convert(self.str_source, datetime.datetime)

    def test_raises_on_time(self):
        with self.assertRaises(ConversionError):
            self.converter.convert(self.str_source, datetime.time)

    def test_raises_on_invalid_target_type(self):
        with self.assertRaises(ConversionError):
            self.converter.convert(self.str_source, int)

    def test_raises_on_invalid_source(self):
        with self.assertRaises(ConversionError):
            self.converter.convert('invalid source', datetime.date)


class ToTimeConverterTestCase(unittest.TestCase):
    def setUp(self):
        self.converter = ToTimeConverter()
        self.source = datetime.datetime.utcnow().time().replace(microsecond=0)
        self.str_source = isodate.time_isoformat(self.source)

    def test_converts_none_to_none(self):
        self.assertIs(
            self.converter.convert(None, datetime.time),
            None,
        )

    def test_converts_time(self):
        target = self.converter.convert(self.str_source, datetime.time)
        self.assertEqual(self.source, target)

    def test_raises_on_datetime(self):
        with self.assertRaises(ConversionError):
            self.converter.convert(self.str_source, datetime.datetime)

    def test_raises_on_date(self):
        with self.assertRaises(ConversionError):
            self.converter.convert(self.str_source, datetime.date)

    def test_raises_on_invalid_target_type(self):
        with self.assertRaises(ConversionError):
            self.converter.convert(self.str_source, int)

    def test_raises_on_invalid_source(self):
        with self.assertRaises(ConversionError):
            self.converter.convert('invalid source', datetime.time)


class ToTimeDeltaConverterTestCase(unittest.TestCase):
    def setUp(self):
        self.converter = ToTimeDeltaConverter()
        self.source = datetime.timedelta(seconds=100)
        self.str_source = isodate.duration_isoformat(self.source)

    def test_converts_none_to_none(self):
        self.assertIs(
            self.converter.convert(None, datetime.timedelta),
            None,
        )

    def test_converts_timedelta(self):
        target = self.converter.convert(self.str_source, datetime.timedelta)
        self.assertEqual(self.source, target)

    def test_raises_on_invalid_target_type(self):
        with self.assertRaises(ConversionError):
            self.converter.convert(self.str_source, int)

    def test_raises_on_invalid_source(self):
        with self.assertRaises(ConversionError):
            self.converter.convert('invalid source', datetime.timedelta)


class ToBoolConverterTestCase(unittest.TestCase):
    def setUp(self):
        self.converter = ToBoolConverter()

    def test_converts_none_to_none(self):
        self.assertIs(
            self.converter.convert(None, bool),
            None,
        )

    def test_converts_to_bool(self):
        samples = [
            (True, True),
            (False, False),
            ('true', True),
            ('false', False),
            ('TRUE', True),
            ('FALSE', False),
            ('1', True),
            ('100500', True),
            ('0', False),
            (100500, True),
            (0, False),
        ]
        for source, target in samples:
            self.assertIs(
                self.converter.convert(source, bool),
                target,
            )

    def test_raises_on_invalid_target_type(self):
        with self.assertRaises(ConversionError):
            self.converter.convert(True, str)

    def test_raise_on_invalid_string_source(self):
        with self.assertRaises(ConversionError):
            self.converter.convert('invalid source', bool)

    def test_raise_on_invalid_source(self):
        with self.assertRaises(ConversionError):
            self.converter.convert(1.0, bool)


class ToIntegerConverterTestCase(unittest.TestCase):
    def setUp(self):
        self.converter = ToIntegerConverter()

    def test_converts_none_to_none(self):
        self.assertIs(
            self.converter.convert(None, int),
            None,
        )

    def test_converts_to_int(self):
        samples = [
            (True, 1),
            (False, 0),
            (0.1, 0),
            ('100500', 100500),
        ]
        for source, target in samples:
            self.assertEqual(
                self.converter.convert(source, int),
                target,
            )

    def test_converts_from_custom_bases(self):
        samples = [
            (2, '10', 2),
            (8, '70', 56),
            (16, 'f0', 240),
            (16, 'Ff', 255),
            (16, 15, 15),
        ]
        for base, source, target in samples:
            self.assertEqual(
                self.converter.convert(source, int, base=base),
                target,
            )

    def test_raises_on_invalid_base(self):
        with self.assertRaises(ConversionError):
            self.converter.convert('1', int, base=-1)

    def test_raises_on_invalid_target_type(self):
        with self.assertRaises(ConversionError):
            self.converter.convert(1, str)

    def test_raise_on_invalid_source(self):
        with self.assertRaises(ConversionError):
            self.converter.convert('invalid source', int)


class ToFloatConverterTestCase(unittest.TestCase):
    def setUp(self):
        self.converter = ToFloatConverter()

    def test_converts_none_to_none(self):
        self.assertIs(
            self.converter.convert(None, float),
            None,
        )

    def test_converts_to_float(self):
        samples = [
            (True, 1),
            (False, 0),
            (0.1, 0.1),
            ('100500', 100500),
            ('100500.10', 100500.10),
            ('15e-1', 1.5),
        ]
        for source, target in samples:
            self.assertEqual(
                self.converter.convert(source, float),
                target,
            )

    def test_raises_on_invalid_target_type(self):
        with self.assertRaises(ConversionError):
            self.converter.convert(1, str)

    def test_raise_on_invalid_source(self):
        with self.assertRaises(ConversionError):
            self.converter.convert('invalid source', float)


class ToEnumConverterTestCase(unittest.TestCase):
    class StrEnum(str, enum.Enum):
        A = 'a'
        B = 'b'

    class IntEnum(int, enum.Enum):
        ONE = 1
        TWO = 2

    def setUp(self):
        self.converter = ToEnumConverter()

    def test_converts_none_to_none(self):
        self.assertIs(
            self.converter.convert(None, self.StrEnum),
            None,
        )

    def test_converts_to_enum(self):
        samples = [
            (self.StrEnum, False, 'a', self.StrEnum.A),
            (self.StrEnum, False, 'b', self.StrEnum.B),
            (self.StrEnum, True, 'A', self.StrEnum.A),
            (self.StrEnum, True, 'B', self.StrEnum.B),
            (self.IntEnum, False, 1, self.IntEnum.ONE),
            (self.IntEnum, False, 2, self.IntEnum.TWO),
            (self.IntEnum, True, 'ONE', self.IntEnum.ONE),
            (self.IntEnum, True, 'TWO', self.IntEnum.TWO),
        ]
        for type, by_name, source, target in samples:
            self.assertEqual(
                self.converter.convert(source, type, by_name=by_name),
                target,
            )

    def test_raises_on_invalid_target_type(self):
        with self.assertRaises(ConversionError):
            self.converter.convert('a', int)

    def test_raises_on_invalid_source(self):
        with self.assertRaises(ConversionError):
            self.converter.convert('invalid source', self.StrEnum)

        with self.assertRaises(ConversionError):
            self.converter.convert('invalid source', self.IntEnum)

        with self.assertRaises(ConversionError):
            self.converter.convert('invalid source', self.StrEnum, by_name=True)



class CompositeConverterTestCase(unittest.TestCase):
    class NopConverter(Converter):
        def convert(self, source, target_type, **hints):
            """Will be mocked"""

    def test_converts_none_to_none(self):
        for type in (int, str, float, unicode):
            service = CompositeConverter()
            service.converters = mock.MagicMock()
            self.assertIs(
                service.convert(None, type),
                None,
            )
            service.converters.__iter__.assert_not_called()

    def test_converts_value_to_itself(self):
        samples = [
            (1, int),
            (1., float),
            (True, bool),
            ('test', str),
            (u'test', unicode),
        ]
        for source, type in samples:
            service = CompositeConverter()
            service.converters = mock.MagicMock()
            self.assertIs(
                service.convert(source, type),
                source,
            )
            service.converters.__iter__.assert_not_called()

    def test_call_converters(self):
        failed_converter = self.NopConverter()
        failed_converter.convert = mock.Mock(side_effect=ConversionError())

        success_converter = self.NopConverter()
        success_converter.convert = mock.Mock(return_value='success')

        service = CompositeConverter()
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

        service = CompositeConverter()
        service.converters.extend([failed_converter1, failed_converter2])
        with self.assertRaises(ConversionError):
            service.convert(1, float),
        failed_converter1.convert.assert_called_once_with(1, float)
        failed_converter2.convert.assert_called_once_with(1, float)