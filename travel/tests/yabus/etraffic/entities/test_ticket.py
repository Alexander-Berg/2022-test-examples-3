# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest

from yabus.etraffic.entities.ticket import Ticket


class TestTicket(object):
    @pytest.mark.parametrize('fields, expected', (
        ({'docSeries': None, 'docNum': '12345'}, '12345'),
        ({'docSeries': 'АА', 'docNum': '12345'}, 'АА 12345'),
    ))
    def test_format_docNumber(self, fields, expected):
        result = Ticket().format(dict({'__countries__': ()}, **fields))
        assert result['passenger']['docNumber'] == expected
