# -*- coding: utf-8 -*-
from __future__ import absolute_import

from travel.avia.library.python.tester.factories import create_partner_user, create_partner
from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.backend.main.api_types.partnersLogins import PartnersLoginsHandler


class TestPartnersLogins(TestCase):
    def test_partners_logins(self):
        partner = create_partner(
            code='partner',
            id=666,
        )
        partner_user1 = create_partner_user(
            login='user1',
            role='role1',
            partner=partner,
            name='name1',
        )
        partner_user2 = create_partner_user(
            login='user2',
            role='role2',
            name='name2',
        )

        actual_response_content = PartnersLoginsHandler()(None, None)
        expected = [{
            u'login': partner_user1.login,
            u'role': partner_user1.role,
            u'partnerCode': partner.code,
        }, {
            u'login': partner_user2.login,
            u'role': partner_user2.role,
            u'partnerCode': None
        }]
        for partner_login in actual_response_content:
            assert partner_login in expected
