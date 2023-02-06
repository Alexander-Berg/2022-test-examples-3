# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest

from travel.buses.connectors.tests.yabus.busfor.data import RAW_TICKET
from yabus.busfor.entities.ticket import Ticket


@pytest.fixture(autouse=True)
def app_context(app):
    with app.app_context():
        yield


class TestTicket(object):
    def test_format(self):
        assert Ticket.format(RAW_TICKET) == {
            "status": {"id": 1, "name": "booked"},
            "passenger": {
                "docNumber": "9417645155",
                "citizenship": "RU",
                "firstName": "Иван",
                "middleName": "Савельевич",
                "lastName": "Кожедуб",
                "docType": {"id": 1, "name": "id"},
                "birthDate": "1980-02-01T00:00:00",
                "seat": "1",
                "genderType": {"id": 1, "name": "male"},
                "ticketType": {"id": 1, "name": "full"},
            },
            "supplierId": "15093932",
            "url": None,
            "@id": "eyJvcmRlcl9zaWQiOiAxMTQ1MTc3NiwgInRpY2tldF9zaWQiOiAxNTA5MzkzMn0=",
            "price": 1182.38,
            "revenue": 60.30,
            "priceVat": "nds_none",
            "feeVat": "nds_none",
            "data": None,
            "cpaPayload": {
                "commissions": [
                    {
                        "description": "agent fee №2",
                        "disposition": 1,
                        "return_value": None,
                        "return_vat": None,
                        "type": 3,
                        "value": 6000,
                        "vat": 1339,
                        "vat_value": 2000,
                    },
                    {
                        "description": "service charge",
                        "disposition": 2,
                        "return_value": None,
                        "return_vat": None,
                        "type": 6,
                        "value": 3000,
                        "vat": 1339,
                        "vat_value": 2000,
                    },
                    {
                        "description": "agent fee №1",
                        "disposition": 2,
                        "return_value": None,
                        "return_vat": None,
                        "type": 5,
                        "value": 2000,
                        "vat": 1339,
                        "vat_value": 2000,
                    },
                ]
            },
        }

    @pytest.mark.parametrize(
        "commissions, expected",
        (
            (
                [
                    {
                        "description": "agent fee №2",
                        "disposition": 1,
                        "return_value": None,
                        "return_vat": None,
                        "type": 3,
                        "value": 6000,
                        "vat": 1339,
                        "vat_value": 2000,
                    },
                    {
                        "description": "service charge",
                        "disposition": 2,
                        "return_value": None,
                        "return_vat": None,
                        "type": 6,
                        "value": 3000,
                        "vat": 1339,
                        "vat_value": 2000,
                    },
                    {
                        "description": "agent fee №1",
                        "disposition": 2,
                        "return_value": None,
                        "return_vat": None,
                        "type": 5,
                        "value": 2000,
                        "vat": 1339,
                        "vat_value": 2000,
                    },
                ],
                60.30,
            ),
            (
                [
                    {
                        "description": "Сумма других сборов",
                        "disposition": 0,
                        "return_value": None,
                        "return_vat": None,
                        "type": -2,
                        "value": 1026,
                        "vat": 0,
                        "vat_value": 0,
                    },
                    {
                        "description": "Сервисный сбор",
                        "disposition": 0,
                        "return_value": None,
                        "return_vat": None,
                        "type": 6,
                        "value": 7,
                        "vat": 0,
                        "vat_value": 0,
                    },
                    {
                        "description": "Сбор перевозчика",
                        "disposition": 0,
                        "return_value": None,
                        "return_vat": None,
                        "type": 18,
                        "value": 1026,
                        "vat": 0,
                        "vat_value": 0,
                    },
                ],
                None,
            ),
            (None, None),
        ),
    )
    def test_format_revenue(self, commissions, expected):
        assert (
            Ticket.format(dict(RAW_TICKET, **{"commissions": {"commissions": commissions}}))["revenue"]
            == expected
        )
