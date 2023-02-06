# -*- coding: utf-8 -*-

import json

import pytest

from tester.factories import create_station, create_station_majority


@pytest.mark.dbuser
def test_change_majority(superuser_client):
    station = create_station()

    resp = superuser_client.post('/admin/blue/station_set_majority/',
                                 {'station_id': station.id, 'majority_id': ''})

    assert resp.status_code == 200
    assert json.loads(resp.content)['status'] == 'bad'

    majority = create_station_majority()
    resp = superuser_client.post('/admin/blue/station_set_majority/',
                                 {'station_id': station.id, 'majority_id': majority.id})
    station.refresh_from_db()

    assert resp.status_code == 200
    assert json.loads(resp.content)['status'] == 'ok'
    assert station.majority == majority
