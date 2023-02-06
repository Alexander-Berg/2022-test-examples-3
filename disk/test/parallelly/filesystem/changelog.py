# -*- coding: utf-8 -*-
from test.base import DiskTestCase

import pytest
import datetime

from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase
from mpfs.metastorage.postgres.ttl import delete_expired_entries
from test.base import time_machine
from test.conftest import INIT_USER_IN_POSTGRES


class ChangelogFilesystemTestCase(DiskTestCase):
    TTL_PERIOD_SECS = 2592000

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Postgres is here!!!')
    def test_ttl_on_postgres(self):
        """
        Сравниваем работу ttl индекса на постгресе, чтобы оно работало также, как на монге
        """

        folder_path_1 = '/disk/folder_1'
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path_1})

        one_minute_after_expiration = datetime.datetime.now() - datetime.timedelta(seconds=self.TTL_PERIOD_SECS) - \
                                      datetime.timedelta(hours=3) - datetime.timedelta(minutes=1)

        one_minute_before_expiration = datetime.datetime.now() - datetime.timedelta(seconds=self.TTL_PERIOD_SECS) - \
                                       datetime.timedelta(hours=3) + datetime.timedelta(minutes=1)

        with time_machine(one_minute_after_expiration):
            folder_path_2 = '/disk/folder_2'
            self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path_2})

        with time_machine(one_minute_before_expiration):
            folder_path_3 = '/disk/folder_3'
            self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path_3})

        db = CollectionRoutedDatabase()
        full_changelog_list_before = list(db.changelog.find({'uid': self.uid}))
        for path in (folder_path_1, folder_path_2, folder_path_3):
            assert any(item['key'] == path for item in full_changelog_list_before)

        delete_expired_entries()

        full_changelog_list_after = list(db.changelog.find({'uid': self.uid}))
        for path in (folder_path_1, folder_path_3):
            assert any(item['key'] == path for item in full_changelog_list_after)
        assert not any(item['key'] == folder_path_2 for item in full_changelog_list_after)

        assert len(full_changelog_list_after) == len(full_changelog_list_before) - 1
