# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import pytest
from travel.avia.contrib.python.mongoengine.mongoengine.errors import NotUniqueError

from travel.avia.avia_api.avia.v1.model.user import User


class TestUserCreation(object):
    @pytest.fixture()
    def clear_users(self, request):
        User.objects.delete()

    @pytest.mark.usefixtures('clear_users')
    def test_should_keep_uid_and_yauid_unique(self):
        uuids = (None, 'uuid1', 'uuid2')
        yauids = (None, 'yauid1', 'yauid2')

        for uuid in uuids:
            for yauid in yauids:
                User(uuid=uuid, yandex_uid=yauid).save()

        for uuid in uuids:
            for yauid in yauids:
                with pytest.raises(NotUniqueError):
                    User(uuid=uuid, yandex_uid=yauid).save()
