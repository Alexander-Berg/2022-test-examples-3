# coding: utf-8

import pytest

from travel.rasp.admin.scripts.direction_bind import clean_binds
from tester.factories import create_thread, create_direction


@pytest.mark.dbuser
def test_clean_binds():
    thread = create_thread(schedule_v1=(
        (None, 0, {}, dict(departure_subdir='start', departure_direction=create_direction(code='start'))),
        (9, None, {}, dict(arrival_subdir='finish', arrival_direction=create_direction(code='finish'))),
    ))

    clean_binds([thread])

    path = list(thread.path)
    first_rts = path[0]
    last_rts = path[-1]
    assert first_rts.departure_subdir is None
    assert first_rts.departure_direction_id is None
    assert last_rts.arrival_subdir is None
    assert last_rts.arrival_direction_id is None
