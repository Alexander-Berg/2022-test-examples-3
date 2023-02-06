# -*- coding: utf-8 -*-

import json

import pytest

from travel.rasp.admin.admin.red.convertors import metaroute_to_json
from travel.rasp.admin.admin.red.models import Package, MetaRoute
from common.models.transport import TransportType, TransportSubtype
from tester.factories import create_supplier


@pytest.mark.dbuser
@pytest.mark.parametrize('update_fields, expected_update', (
    ({'number': 'xxx'}, {'number': 'xxx'}),
    ({'t_subtype_id': 0}, {'t_subtype_id': None}),
    ({'supplier_id': None}, None),
    ({'t_type_id': TransportType.BUS_ID, 't_subtype_id': TransportSubtype.TRAIN_ID}, None),
))
def test_thread_update(superuser_client, update_fields, expected_update):
    metaroute = MetaRoute.objects.create(
        package=Package.objects.create(title=u'Красный тестовый пакет'),
        supplier=create_supplier(),
        title=u'Красный тестовый рейс',
        t_type_id=TransportType.PLANE_ID,
    )
    metaroute_fields = metaroute_to_json(metaroute)
    response = superuser_client.put(
        '/admin/redmap/api/threads/{}/'.format(metaroute.id),
        json.dumps(dict(metaroute_fields, **update_fields))
    )

    assert response.status_code == 200
    validation_status = json.loads(response.content).get('validationStatus')
    updated_fields = metaroute_to_json(MetaRoute.objects.get(id=metaroute.id))
    if expected_update:
        assert validation_status is None
        assert updated_fields == dict(metaroute_fields, **expected_update)
    else:
        assert validation_status is not None
        assert updated_fields == metaroute_fields
