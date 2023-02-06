# -*- coding: utf-8 -*-
from mock import Mock

from travel.avia.ticket_daemon.ticket_daemon.partners.dohop import gen_variants_chunks


def test_corrupted_data():

    pooler = [{
        "continuation": "1",
        "search": {"flights": [], "outbound": [], "homebound": []},
        "airports": {}, "aircraft": {}, "airlines": {},
        "fares": {
            "1": {
                "h": [],
                "o": [
                    "DPS", "SIN", "KL836", "2019-05-15",
                    "SIN", "AMS", "KL836", "2019-05-16",
                    "AMS", "SVO", "KL3180", "2019-05-16"
                ],
                "f": {
                    "151": {"a": 0, "c": "IDR", "d": "2019-04-25 10:14:57", "f": 10262300}
                }
            },
        },
        "vendors": {"151": {"n": "KLM"}},
        "is_done": True
    }]
    chunks = list(gen_variants_chunks(None, Mock(), 'key', pooler))
    assert [] == chunks
