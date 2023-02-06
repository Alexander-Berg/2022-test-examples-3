# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from travel.avia.ticket_daemon.ticket_daemon.lib.sirena import SirenaVariantBuilder, SirenaFlight


def test_variant_hashcode_equals():
    builder1 = SirenaVariantBuilder(627, 'РУБ', 'A')
    builder1.add_flight('ЕКБ-СОЧ', SirenaFlight(
        'WZ', '425', 'ЕКБ', 'СОЧ', '17.06.21', '15:30', '17.06.21', '16:10',
        'Э', 'Т', '1PC', 'LTLTOW',
    ))

    builder2 = SirenaVariantBuilder(627, 'РУБ', 'B')
    builder2.add_flight('ЕКБ-СОЧ', SirenaFlight(
        'WZ', '425', 'ЕКБ', 'СОЧ', '17.06.21', '15:30', '17.06.21', '16:10',
        'Э', 'Т', '1КМ', 'LTLTOW',
    ))

    variant1 = builder1.build()
    variant2 = builder2.build()

    assert hash(variant1) == hash(variant2)
    assert variant1 == variant2

    s = set()
    assert variant1 not in s
    assert variant2 not in s

    s.add(variant1)
    assert variant1 in s
    assert variant2 in s
