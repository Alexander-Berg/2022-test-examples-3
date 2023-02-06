# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from django.utils.translation import get_language
from mongoengine import StringField
from pymongo.collation import CollationStrength

from common.utils import gen_hex_uuid
from travel.rasp.train_api.train_purchase.core.models import (
    TrainPurchaseBaseDocument, _make_case_insensitive_meta_index, QuerySetWithCollation
)

pytestmark = [pytest.mark.mongouser]


class TestQuerySetWithCollation(object):
    class SomeDocument(TrainPurchaseBaseDocument):
        uuid = StringField(max_length=64, min_length=32, required=True, unique=True, default=gen_hex_uuid)
        first_name = StringField(required=True)
        last_name = StringField(required=True)

        meta = {
            'indexes': [
                _make_case_insensitive_meta_index('first_name', name='first_name_with_collation'),
            ],
            'index_background': True,
            'queryset_class': QuerySetWithCollation
        }

    @pytest.mark.parametrize('query_first_name, expected_count', [
        ('Ольга', 2),
        ('ольга', 2),
        ('олЬГА', 2),
        ('о', 1),
        ('Х', 0),
    ])
    def test_search_with_collation(self, query_first_name, expected_count):
        TestQuerySetWithCollation.SomeDocument.objects.create(first_name='Ольга', last_name='1')
        TestQuerySetWithCollation.SomeDocument.objects.create(first_name='Ольга', last_name='2')
        TestQuerySetWithCollation.SomeDocument.objects.create(first_name='Олег', last_name='3')
        TestQuerySetWithCollation.SomeDocument.objects.create(first_name='Игорь', last_name='4')
        TestQuerySetWithCollation.SomeDocument.objects.create(first_name='О', last_name='5')
        qs = TestQuerySetWithCollation.SomeDocument.objects.filter(first_name=query_first_name)\
            .collation(locale=get_language(), strength=CollationStrength.PRIMARY)
        assert isinstance(qs, QuerySetWithCollation)
        assert qs.count() == expected_count
        assert len(list(qs)) == expected_count
        qs2 = qs.clone()
        assert qs2.count() == expected_count
        assert len(list(qs2)) == expected_count
