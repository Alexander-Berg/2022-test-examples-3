# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
import pytz
from django.http import QueryDict
from marshmallow import ValidationError

from common.tester.factories import create_settlement
from common.tester.testcase import TestCase

from travel.rasp.morda_backend.morda_backend.search.search.serialization.request_serialization import ContextQuerySchema
from travel.rasp.morda_backend.morda_backend.serialization.segment import INVALID_VALUE


pytestmark = pytest.mark.dbuser


def test_search_request():
    create_settlement(id=54)
    create_settlement(id=55)

    request_dict = {
        'pointFrom': 'c54',
        'pointTo': 'c55',
    }
    query_dict = QueryDict(mutable=True)
    query_dict.update(request_dict)
    context, errors = ContextQuerySchema().load(query_dict)

    assert errors == {}
    assert context.point_from.point_key == 'c54'
    assert context.point_to.point_key == 'c55'
    assert context.when is None
    assert context.nearest is False
    assert context.transport_type is None
    assert context.timezones == []
    assert context.national_version == 'ru'
    assert context.is_mobile is False
    assert context.allow_change_context is True

    request_dict = {
        'pointFrom': 'c54',
        'pointTo': 'c55',
        'when': '2020-01-01',
        'nearest': True,
        'transportType': 'plane',
        'timezones': 'Europe/Moscow',
        'nationalVersion': 'ua',
        'isMobile': True,
        'allowChangeContext': False,
    }
    query_dict = QueryDict(mutable=True)
    query_dict.update(request_dict)
    context, errors = ContextQuerySchema().load(query_dict)

    assert errors == {}
    assert context.point_from.point_key == 'c54'
    assert context.point_to.point_key == 'c55'
    assert context.when == '2020-01-01'
    assert context.nearest is True
    assert context.transport_type == 'plane'
    assert len(context.timezones) == 1
    assert context.timezones[0].zone == 'Europe/Moscow'
    assert context.national_version == 'ua'
    assert context.is_mobile is True
    assert context.allow_change_context is False


class TestContextQuerySchema(TestCase):
    def setUp(self):
        self.schema = ContextQuerySchema(strict=True)

    def test_prepare(self):
        """
        data может быть QueryDict, в этом случае мы должны достать из
        него timezones в виде списка.
        """
        settlement1 = create_settlement()
        settlement2 = create_settlement()
        query_dict = QueryDict('point_from=c{}&point_to=c{}&timezones=1&timezones=2'.format(
            settlement1.pk, settlement2.pk
        ))
        data = self.schema.prepare(query_dict)
        assert data['timezones'] == ['1', '2']

    def test_timezones_deserialization(self):
        """
        Временные зоны должны быть превращены в объекты pytz.timezone.
        """
        settlement1 = create_settlement()
        settlement2 = create_settlement()
        context = self.schema.load({
            'point_from': 'c{}'.format(settlement1.pk),
            'point_to': 'c{}'.format(settlement2.pk),
            'timezones': ['Europe/Moscow', 'America/Phoenix']
        })
        assert context.data.timezones == [
            pytz.timezone('Europe/Moscow'),
            pytz.timezone('America/Phoenix')
        ]

    def test_timezones_deserialization_error(self):
        """
        Если переданы некорректные временные зоны - должна быть возвращена соответствущюая ошибка
        """
        settlement1 = create_settlement()
        settlement2 = create_settlement()
        with pytest.raises(ValidationError) as exc:
            self.schema.load({
                'point_from': 'c{}'.format(settlement1.pk),
                'point_to': 'c{}'.format(settlement2.pk),
                'timezones': ['1']
            })

        assert exc.value.normalized_messages() == {
            'timezones': INVALID_VALUE
        }

    def test_when_validation(self):
        settlement1 = create_settlement()
        settlement2 = create_settlement()

        with pytest.raises(ValidationError) as ex_info:
            self.schema.load({
                'point_from': 'c{}'.format(settlement1.pk),
                'point_to': 'c{}'.format(settlement2.pk),
                'when': '2031-31-31',
            })
        assert ex_info.value.messages == {'when': ["Invalid value '2031-31-31' for parameter 'when'"]}

    def test_transport_type_validation(self):
        settlement1 = create_settlement()
        settlement2 = create_settlement()

        with pytest.raises(ValidationError) as ex_info:
            self.schema.load({
                'point_from': 'c{}'.format(settlement1.pk),
                'point_to': 'c{}'.format(settlement2.pk),
                'transportType': 'wrong_type',
            })
        assert ex_info.value.messages == {'transportType': ["Invalid transport type 'wrong_type'"]}
