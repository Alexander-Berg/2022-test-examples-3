# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from django.test import Client
from hamcrest import anything, assert_that, has_entries, contains_string
from six.moves.urllib_parse import quote

from common.tester.matchers import has_json
from travel.rasp.train_api.train_purchase.core.factories import UserInfoFactory
from travel.rasp.train_api.train_purchase.views.test_utils import create_order

pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser]


class TestOrdersCountByEmail(object):
    url = '/ru/train-purchase-backoffice/support/orders-count-by-email/'

    def test_no_email(self):
        response = Client().get(self.url)

        assert response.status_code == 400
        assert_that(response.content,
                    has_json(has_entries('errors', has_entries({'email': anything()}))))

    @pytest.mark.parametrize('emails_in_orders, email, expected_count', (
        ([], 'user@example.org', 0),
        (['a@example.org'], 'user@example.org', 0),
        (['user@example.org'], 'user@example.org', 1),
        (['a@example.org', 'user@example.org'], 'user@example.org', 1),
        (['a@example.org', 'user@example.org', 'user@example.org'], 'user@example.org', 2),
        (['USER@example.org', 'user@EXAMPLE.org', 'user@example.ORG'], 'user@example.org', 3),
    ))
    def test_orders_count_by_email(self, emails_in_orders, email, expected_count):
        for e in emails_in_orders:
            create_order(user_info=UserInfoFactory(email=e))

        response = Client().get(self.url, {'email': email})

        assert response.status_code == 200
        assert_that(response.content, has_json(has_entries(
            count=expected_count,
            url=contains_string(quote(email)),
        )))
