# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from django.utils.translation import activate

from common.models.transport import TransportType
from common.tester.factories import create_settlement

from travel.rasp.morda_backend.morda_backend.search.parse_context.serialization import InputSearchContextSchema


@pytest.mark.dbuser
def test_can_deserialize():
    client_settlement = create_settlement(id=42)
    activate('uk')

    params = {
        'from_key': 'from key',
        'from_title': 'from title',
        'from_slug': 'from slug',
        'to_key': 'to key',
        'to_title': 'to title',
        'to_slug': 'to slug',
        't_type': 'train',
        'client_settlement_id': '42',
        'national_version': 'ua',
        'nearest': '1'
    }

    input_context, errors = InputSearchContextSchema().load(params)

    assert input_context.from_key == params['from_key']
    assert input_context.from_title == params['from_title']
    assert input_context.from_slug == params['from_slug']
    assert input_context.to_key == params['to_key']
    assert input_context.to_title == params['to_title']
    assert input_context.to_slug == params['to_slug']
    assert input_context.t_type == TransportType.objects.get(id=TransportType.TRAIN_ID)
    assert input_context.client_settlement == client_settlement
    assert input_context.national_version == params['national_version']
    assert input_context.nearest
    assert input_context.language == 'uk'
