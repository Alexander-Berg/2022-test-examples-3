# -*- coding: utf-8 -*-
from test.base import DiskTestCase
from test.fixtures import users


class CommonJsonApiTestCase(DiskTestCase):
    uid_1 = users.user_1.uid
    uid_3 = users.user_3.uid
    login_1 = users.user_1.login
    email = users.default_user.email
    email_1 = users.user_1.email
    email_3 = users.user_3.email
    email_cyrillic = users.email_cyrillic
    email_cyrillic_dots = users.email_cyrillic_dots
    email_dots = users.email_dots

    def _clean_datetimes(self, json):
        for item in json:
            for field in ['ctime', 'mtime', 'utime']:
                try:
                    del (item[field])
                except KeyError:
                    pass

    def share_dir(self, owner_uid, invitee_uid, invitee_email, path, rights=660):
        args = {
            'uid': owner_uid,
            'path': path,
            'rights': rights,
            'universe_login': invitee_email,
            'universe_service': 'email',
            'avatar': 'http://localhost/echo',
            'name': 'mpfs',
            'connection_id': '12345',
        }
        result = self.json_ok('share_invite_user', args)
        hsh = result.get('hash')
        args = {
            'hash': hsh,
            'uid': invitee_uid,
        }
        self.json_ok('share_activate_invite', args)
