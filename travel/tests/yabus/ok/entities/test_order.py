# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import freezegun

from yabus.ok.entities import Order

DUMMY_ORDER = {
    "operation_id": "958257",
    "status": 3,
    "ticket_list": [
        {
            "ticket_id": 907596,
            "operation_id": 958257,
            "status": 3,
            "price_unitiki": 724.5,
            "name": "Иванов Иван Иванович",
            "birthday": "1980-01-01",
            "gender": 1,
            "citizenship_id": 170,
            "card_identity_id": 1,
            "series_number": "1234123456",
            "tariff": {"tariff_id": 1},
            "position": 1,
        }
    ],
    "time_left_to_cancel": 60,
}


class TestOrder(object):
    @freezegun.freeze_time("2000-01-01")
    def test_init(self):
        assert Order.init(DUMMY_ORDER, tickets_fwd={"__countries__": [{"code": "170", "name": "RU"}]}) == {
            "partner": "ok",
            "partnerName": "ОднаКасса",
            "@id": "eyJvcmRlcl9zaWQiOiAiOTU4MjU3In0=",
            "supplierId": "958257",
            "status": {"id": 1, "name": "booked"},
            "tickets": [
                {
                    "@id": "eyJvcmRlcl9zaWQiOiA5NTgyNTcsICJ0aWNrZXRfc2lkIjogOTA3NTk2fQ==",
                    "cpaPayload": None,
                    "data": None,
                    "feeVat": "nds_none",
                    "passenger": {
                        "birthDate": "1980-01-01T00:00:00",
                        "citizenship": "RU",
                        "docNumber": "1234123456",
                        "docType": {"id": 1, "name": "id"},
                        "firstName": "Иван",
                        "genderType": {"id": 1, "name": "male"},
                        "lastName": "Иванов",
                        "middleName": "Иванович",
                        "seat": "1",
                        "ticketType": {"id": 1, "name": "full"},
                    },
                    "price": 724.5,
                    "revenue": None,
                    "priceVat": "nds_18_118",
                    "status": {"id": 1, "name": "booked"},
                    "supplierId": "907596",
                    "url": None,
                }
            ],
            "expirationDateTime": "2000-01-01T00:01:00+00:00",
            "price": 724.5,
        }
