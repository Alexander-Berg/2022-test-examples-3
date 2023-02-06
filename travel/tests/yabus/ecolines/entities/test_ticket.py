# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import mock
import pytest

from yabus.common import cipher
from yabus.ecolines.entities.ticket import Ticket


class TestTicket(object):
    @mock.patch.object(cipher, 'decrypt', side_effect=lambda v: v)
    @pytest.mark.parametrize('fields, expected', (
        ({'docSeries': '', 'docNumber': '12345'}, '12345'),
        ({'docSeries': 'АА', 'docNumber': '12345'}, 'АА 12345'),
    ))
    def test_format_docNumber(self, m_decrypt, fields, expected):
        result = Ticket().format(dict({'note': {}}, **fields))
        assert result['passenger']['docNumber'] == expected
