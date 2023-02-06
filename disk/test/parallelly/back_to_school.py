# -*- coding: utf-8 -*-
from test.base import DiskTestCase
from test.fixtures.users import user_1

from mpfs.core.user.back_to_school import Back2SchoolApplication, process_application, ApplicationStatus


class BackToSchoolTestCase(DiskTestCase):
    def setup_method(self, method):
        super(BackToSchoolTestCase, self).setup_method(method)
        self.upload_file(self.uid, '/disk/1.txt')
        resp = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/1.txt'})
        self._application = Back2SchoolApplication(
            public_url=resp['short_url'],
            email=user_1.email,
            is_unique=True,
            is_assessor_accepted=True,
            not_valid_url=False,
        )

    def test_ok(self):
        status = process_application(self._application.as_dict())
        assert status == ApplicationStatus.ok
        result = self.billing_ok('service_list', {'uid': self.uid, 'ip': '127.0.0.1'})
        assert 'back_to_school' in [s['name'] for s in result]

    def test_not_unique(self):
        self._application.is_unique = False
        status = process_application(self._application.as_dict())
        assert status == ApplicationStatus.already_used

    def test_not_valid_url(self):
        self._application.not_valid_url = True
        status = process_application(self._application.as_dict())
        assert status == ApplicationStatus.not_valid_url

    def test_not_accepted(self):
        self._application.is_assessor_accepted = False
        status = process_application(self._application.as_dict())
        assert status == ApplicationStatus.not_accepted

    def test_already_provided(self):
        process_application(self._application.as_dict())
        status = process_application(self._application.as_dict())
        assert status == ApplicationStatus.service_already_provided
