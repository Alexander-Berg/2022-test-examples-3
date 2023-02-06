# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import mock
import pytest

from travel.buses.connectors.tests.yabus.unitiki.data import RAW_TICKET
from yabus.unitiki.entities.ticket import Ticket
from yabus.unitiki.entities.ride_details import RideDetails


class TestTicket(object):
    @pytest.mark.parametrize('ticket_patch, expected', (
        (
            {
                'status': 3  # booked
            },
            None
        ),
        (
            {
                'status': 1,  # sold
                'gen_url': True,
            },
            'api/unitiki-new/tickets/eyJvcmRlcl9zaWQiOiAyMjYyLCAidGlja2V0X3NpZCI6IDIzNzJ9/blank'
        )
    ))
    def test_init_url(self, app, ticket_patch, expected):
        ticket_patch = dict(
            ticket_patch, __partner__='unitiki-new', __countries__=[], __gen_url__=(ticket_patch['status'] == 1),
        )

        with mock.patch.dict(app.config, {'YANDEX_BUS_API': 'api'}), app.app_context():
            assert Ticket.init(dict(RAW_TICKET, **ticket_patch))['url'] == expected

    @pytest.mark.parametrize('ticket_patch, expected', (
        (
            {
                'citizenship_id': 170,  # Россия
                'card_identity_id': 1,  # Паспорт РФ
            },
            RideDetails.DOC_TYPE_PASSPORT
        ),
        (
            {
                'citizenship_id': 150,  # не Россия
                'card_identity_id': 1,  # Паспорт
            },
            RideDetails.DOC_TYPE_FOREIGN_PASSPORT
        ),
        (
            {
                'citizenship_id': 170,  # Россия
                'card_identity_id': 3,  # Свидетельство о рождении
            },
            RideDetails.DOC_TYPE_BIRTH_CERT
        )
    ))
    def test_doc_type(self, ticket_patch, expected):
        ticket_patch = dict(ticket_patch,
                            __countries__=[{"code": "170", "name": "RU"}, {"code": "150", "name": "not RU"}],
                            __partner__='unitiki-new',
                            status=3)

        assert Ticket.init(dict(RAW_TICKET, **ticket_patch))['passenger']['docType']['id'] == expected

    @pytest.mark.parametrize('ticket_patch, expected', (
        (
            {'position': '1'},
            '1'
        ),
        (
            {'position': None},
            '0'
        )
    ))
    def test_seat(self, ticket_patch, expected):
        ticket_patch = dict(ticket_patch, __countries__=[], __partner__='unitiki-new', status=3)

        assert Ticket.init(dict(RAW_TICKET, **ticket_patch))['passenger']['seat'] == expected
