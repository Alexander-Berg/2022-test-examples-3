# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from unittest import TestCase

from travel.rasp.touch.touch.core.lib.train_number import TrainNumberParser, TrainNumber, TrainNumberReverser


class TestTrainNumber(TestCase):
    def test_serialize_zero(self):
        assert TrainNumber(digits=0, letters='X').serialize() == '000X'

    def test_serialize_one(self):
        assert TrainNumber(digits=1, letters='X').serialize() == '001X'

    def test_serialize_twelve(self):
        assert TrainNumber(digits=12, letters='X').serialize() == '012X'

    def test_serialize_one_hundred_and_sixteen(self):
        assert TrainNumber(digits=116, letters='X').serialize() == '116X'


class TestTrainNumberParser(TestCase):
    def setUp(self):
        self._parser = TrainNumberParser()

    def test_correct_number_with_leader_zero(self):
        assert self._parser.parse('082X') == TrainNumber(
            digits=82, letters='X'
        )

    def test_correct_number_without_leader_zero(self):
        assert self._parser.parse('82X') == TrainNumber(
            digits=82, letters='X'
        )

    def test_correct_number_with_russian_letters(self):
        assert self._parser.parse('82Я') == TrainNumber(
            digits=82, letters='Я'
        )

    def test_correct_number_with_many_letters(self):
        assert self._parser.parse('82ABC') == TrainNumber(
            digits=82, letters='A'
        )
        assert self._parser.parse('82C') == TrainNumber(
            digits=82, letters='C'
        )

    def test_incorrect_format_with_leader_letters(self):
        assert self._parser.parse('X82Я') is None

    def test_incorrect_format_without_numbers(self):
        assert self._parser.parse('XЯ') is None

    def test_incorrect_format_without_letters(self):
        assert self._parser.parse('82') is None

    def test_incorrect_format_with_letters_in_the_end(self):
        assert self._parser.parse('82X82') is None


class TestTrainNumberReverser(TestCase):
    def setUp(self):
        self._reverser = TrainNumberReverser()

    def test_odd_number(self):
        assert self._reverser.reverse(
            TrainNumber(digits=79, letters='X')
        ) == TrainNumber(digits=80, letters='X')

    def test_even_number(self):
        assert self._reverser.reverse(
            TrainNumber(digits=80, letters='X')
        ) == TrainNumber(digits=79, letters='X')

    def test_circle(self):
        assert TrainNumber(digits=80, letters='X') == self._reverser.reverse(
            self._reverser.reverse(
                TrainNumber(digits=80, letters='X')
            )
        )
