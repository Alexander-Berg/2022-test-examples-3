# -*- coding: utf-8 -*-
from mock import patch
from test.base import DiskTestCase
from test.fixtures.users import pdd_user

from mpfs.common.static.codes import USER_IS_NOT_PDD
from mpfs.core.services.passport_service import Passport
from mpfs.core.user.base import User


class PDDTestCase(DiskTestCase):
    PDD_UID = pdd_user.uid

    def test_create_pdd(self):
        user_info = Passport().userinfo(self.PDD_UID)
        assert Passport().is_from_pdd(user_info['login'])

        self.create_user(self.PDD_UID, noemail=1)
        user = User(self.PDD_UID)
        assert user.is_pdd() is True
        assert user.is_domain_allowed_for_office() is False

    def test_update_pdd_domain(self):
        assert not User(self.uid).is_pdd()

        new_pdd_domain = 'foo.com'
        with patch.object(Passport, 'is_from_pdd', return_value=True):
            with patch.object(Passport, 'userinfo', return_value={'pdd_domain': new_pdd_domain}):
                self.service_ok('user_set_pdd_domain', {'uid': self.uid})
                user = User(self.uid)
                assert user.is_pdd()
                assert user.get_pdd_domain() == new_pdd_domain

    @patch.object(Passport, 'is_from_pdd', return_value=False)
    def test_user_set_pdd_domain_is_not_pdd_error(self, _):
        result = self.service_error('user_set_pdd_domain', {'uid': self.uid})
        assert result['code'] == USER_IS_NOT_PDD
