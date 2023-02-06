# coding: utf-8
import itertools
import string
from collections import namedtuple
from datetime import datetime, timedelta

import pytest
from mock import patch

from travel.avia.library.python.tester.factories import (
    create_partner, create_settlement, create_station, create_company
)
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon.ticket_daemon.api.flights import IATAFlight, Variant
from travel.avia.ticket_daemon.ticket_daemon.api.query import Query
from travel.avia.ticket_daemon.ticket_daemon.daemon.utils import fill
from travel.avia.ticket_daemon.ticket_daemon.api.result.filtering import good_flight_numbers
from travel.avia.ticket_daemon.ticket_daemon.lib.synthetic_variants import synthetic_query_fn

fgood = 'SU 132'
fbad = 'TRA SHcompa- -ny1 233number'


@pytest.mark.dbuser
@pytest.mark.parametrize('good_numbers', [
    ([fgood], []),
    ([fgood, fgood], []),
    ([fgood], [fgood]),
], ids=repr)
@pytest.mark.parametrize('bad_numbers', [
    # Bad in forward
    ([fbad], []),
    ([fbad], [fgood]),
    ([fbad, fgood], [fgood]),
    ([fgood, fbad], [fgood]),
    ([fgood, fbad, fgood], [fgood]),
    # Bad in backward
    ([fgood], [fbad]),
    ([fgood], [fbad, fgood]),
    ([fgood], [fgood, fbad]),
    ([fgood], [fgood, fbad, fgood]),
], ids=repr)
def test_filter_variants_bad_flight_number(good_numbers, bad_numbers):
    reset_all_caches()
    p = create_partner(code=u'one')
    create_settlement(id=213)
    create_station(settlement_id=213)
    create_settlement(id=2)
    create_station(settlement_id=2)

    q = Query.from_key('c213_c2_2016-12-03_2016-12-10_economy_2_0_0_ru',
                       service='ticket', lang='ru', t_code='plane')

    good_variant = Variant()
    good_variant.forward.segments, good_variant.backward.segments = [
        [fill(IATAFlight(), number=n) for n in path] for path in good_numbers]

    bad_variant = Variant()
    bad_variant.forward.segments, bad_variant.backward.segments = [
        [fill(IATAFlight(), number=n) for n in path] for path in bad_numbers]

    with patch.object(Variant, '__repr__', side_effect=repr_variant, autospec=True):
        assert good_flight_numbers(good_variant, p.code, q.id)
        assert not good_flight_numbers(bad_variant, p.code, q.id)


def repr_variant(self):
    return ' @ '.join(' '.join(repr(s.number) for s in path)
                      for path in [self.forward.segments, self.backward.segments])


ImporterTuple = namedtuple('ImporterTuple', 'partners')


def create_airport(iata, **kwargs):
    return create_station(t_type='plane', __={'codes': {'iata': iata}}, **kwargs)


def _make_variants(variants_count):
    partner = create_partner(code='one_partner')

    point_from = create_airport(iata='DEP')
    point_to = create_airport(iata='ARR')
    when = datetime.now().date() + timedelta(3)
    back = when + timedelta(7)

    q = Query.from_key(
        '{}_{}_{}_{}_economy_1_0_0_ru'.format(
            point_from.point_key,
            point_to.point_key,
            when.strftime('%Y-%m-%d'),
            back.strftime('%Y-%m-%d'),
        ),
        service='ticket',
        lang='ru',
        t_code='plane'
    )
    q.prepare_attrs_for_import()
    q.importer = ImporterTuple(partners=[partner])

    variants = synthetic_query_fn(tracker=None, q=q, variants_count=variants_count)

    for v in variants:
        v.partner = partner
        v.partner_code = partner.code

    assert len(variants) == variants_count

    return q, partner, variants


@pytest.mark.dbuser
def test_good_flight_number_and_completed():
    reset_all_caches()

    letters = string.ascii_uppercase
    iata2_iter = (''.join(letter) for letter in itertools.product(letters, repeat=2))
    [create_company(iata=next(iata2_iter), t_type='plane') for _ in range(40)]

    variants_count = 300
    q, partner, variants = _make_variants(variants_count)

    for flight in (f for v in variants for f in v.all_segments if not f.completed):
        flight.complete()

    variants = [v for v in variants if v.completed_ok]
    assert len(variants) == variants_count

    vs = [v for v in variants if good_flight_numbers(v, partner.code, q.id)]
    assert len(vs) == len(variants)
