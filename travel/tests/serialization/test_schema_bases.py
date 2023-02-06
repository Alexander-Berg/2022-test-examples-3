# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from marshmallow import ValidationError

from common.tester.factories import create_country, create_settlement
from common.tester.testcase import TestCase
from travel.rasp.morda_backend.morda_backend.serialization.schema_bases import (
    PointsQuerySchema, PointsShowHiddenQuerySchema
)


class TestPointsQuerySchema(TestCase):
    def setUp(self):
        self.schema = PointsQuerySchema()

    def test_validate_point_is_country(self):
        data, errors = self.schema.load({'pointFrom': create_country().point_key,
                                         'pointTo': create_country().point_key})
        assert errors['_schema'] == [{
            'point_from': 'country_point',
            'point_to': 'country_point'
        }]

    def test_validate_point_not_found(self):
        data, errors = self.schema.load({'pointFrom': '', 'pointTo': 'c1111111'})
        assert errors['_schema'] == [{
            'point_from': 'no_such_point',
            'point_to': 'no_such_point'
        }]

    def test_hidden_point(self):
        create_settlement(id=101, hidden=True)
        create_settlement(id=102, hidden=True)
        data, errors = self.schema.load({'pointFrom': 'c101', 'pointTo': 'c102'})
        assert errors['_schema'] == [{
            'point_from': 'no_such_point',
            'point_to': 'no_such_point'
        }]

    def test_validate_ambiguous_points(self):
        settlement = create_settlement()
        with pytest.raises(ValidationError) as exc:
            self.schema.validate_points({
                'point_from': settlement,
                'point_to': settlement
            })
            assert exc.messages == {
                'ambiguous': 'ambiguous_points',
            }


class TestPointsShowHiddenQuerySchema(TestCase):
    def setUp(self):
        self.schema = PointsShowHiddenQuerySchema()

    def test_hidden_point(self):
        create_settlement(id=101, hidden=True)
        create_settlement(id=102, hidden=True)
        data, errors = self.schema.load({'pointFrom': 'c101', 'pointTo': 'c102'})
        assert errors == {}
