# -*- coding: utf-8 -*-
import pytest
from mock import Mock

from travel.avia.ticket_daemon.ticket_daemon.lib.baggage import BaggageParser


@pytest.mark.parametrize('baggage,expected_baggage', [
    ('1PC', '1pc'),
    ('0PC', '0pc'),
    ('2PC', '2pc'),
    ('1PC23', '1pc 23kg'),
    ('1PC20K', '1pc 20kg'),
    ('1PC15KG', '1pc 15kg'),
    ('15K', '1pc 15kg'),
    ('23KG', '1pc 23kg'),
    ('1pc', '1pc'),
    ('1pc20kg', '1pc 20kg'),
    ('23kg', '1pc 23kg'),
    ('1N', '1pc'),
    ('N/A', 'None'),
])
def test_get_baggage(baggage, expected_baggage):
    parser = BaggageParser(logger=Mock())
    parsed_baggage = parser.parse_from_string(baggage)
    assert str(parsed_baggage) == expected_baggage


@pytest.mark.parametrize('baggage', ['1', '1P', '1C', '1G', '25', ''])
def test_parse_bad_baggage(baggage):
    parser = BaggageParser(logger=Mock())
    assert parser.parse_from_string(baggage).pieces is None
    assert parser.parse_from_string(baggage).weight is None
    assert parser.parse_from_string(baggage).included is None
