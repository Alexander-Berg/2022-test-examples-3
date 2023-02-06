# -*- coding: utf-8 -*-
import mock
import math
import time
import datetime
from collections import OrderedDict

import pytest
from nose_parameterized import parameterized
from click.testing import CliRunner

from test.base import DiskTestCase, time_machine
from test.conftest import INIT_USER_IN_POSTGRES
from test.parallelly.json_api.base import CommonJsonApiTestCase
from test.fixtures.users import user_1
from test.helpers.stubs.services import MulcaServiceStub, QuellerStub

import mpfs.core.versioning.cli as versioning_cli
from mpfs.common.util import pairwise
from mpfs.core.filesystem.cleaner.models import DeletedStid
from mpfs.core.filesystem.helpers.lock import LockHelper
from mpfs.core.versioning.logic.cleaner import VersionsCleanerManager
from mpfs.core.versioning.logic.version_manager import ResourceVersionManager, VERSIONING_SAVE_VERSION_DT_FORMAT
from mpfs.core.versioning.logic.version_chain import VersionChain
from mpfs.core.versioning.logic.version import Version, VersionManager
from mpfs.core.versioning.iteration_keys import VersioningIterationKey
from mpfs.core.versioning.dao.version_data import VersionType, VersionDataDAO
from mpfs.core.versioning.errors import VersionLinkNotFound, VersionWrongTimes
from mpfs.core.factory import get_resource
from mpfs.core.address import Address
from mpfs.dao.base import get_all_shard_endpoints


class RelativeTimeDelta(object):
    default_timedelta = datetime.timedelta(seconds=1)

    def __init__(self, base_dt=None):
        self._dt = base_dt if base_dt else datetime.datetime.now()

    def step(self, delta=None):
        self._dt += delta if delta else self.default_timedelta
        return self._dt

    @property
    def cur_dt(self):
        return self._dt


def get_all_versions(uid, path):
    resource = get_resource(uid, path)
    vl = VersionChain.get_by_resource_id(resource.resource_id)
    _, versions = vl.get_all_versions(VersioningIterationKey.first_page(limit=100))
    return versions


class VersioningCLITestCase(DiskTestCase):
    def setup_method(self, method):
        super(VersioningCLITestCase, self).setup_method(method)
        path = '/disk/1.txt'
        self.upload_file(self.uid, path)
        self.upload_file(self.uid, path)
        self.resource = get_resource(self.uid, path)

    def test_get_versions(self):
        runner = CliRunner()
        result = runner.invoke(versioning_cli.get_versions, [self.resource.address.id])
        assert result.exit_code == 0
        result = runner.invoke(versioning_cli.get_versions, [self.resource.resource_id.serialize()])
        assert result.exit_code == 0

    def test_get_versioned_resources(self):
        runner = CliRunner()
        result = runner.invoke(versioning_cli.get_versioned_resources, ['--versions', self.uid])
        assert result.exit_code == 0
        result = runner.invoke(versioning_cli.get_versioned_resources, [self.uid])
        assert result.exit_code == 0


class VersionManagerTestCase(CommonJsonApiTestCase):
    one_day = datetime.timedelta(days=1)

    def setup_method(self, method):
        super(VersionManagerTestCase, self).setup_method(method)
        self.json_ok('user_init', {'uid': self.uid_1})

        file_path = "/disk/%s" % '1.txt'
        self.upload_file(self.uid, file_path)
        self.resource = get_resource(self.uid, file_path)
        self.upload_file(self.uid_1, file_path)
        self.resource_1 = get_resource(self.uid_1, file_path)

        now_dt = datetime.datetime.now()
        yesterday = now_dt - self.one_day
        tomorrow = now_dt + self.one_day

        for resource, dt in ((self.resource, yesterday),
                             (self.resource_1, yesterday),
                             (self.resource, tomorrow),
                             (self.resource_1, tomorrow)):
            vc = VersionChain.ensure(resource.resource_id)
            version = Version.create_binary(resource, dt)
            vc.append_version(version)

    def test_fetch_expired_versions(self):
        versions = []
        for shard_endpoint in get_all_shard_endpoints():
            versions.extend(list(VersionManager.fetch_expired_versions_on_shard(shard_endpoint, 1000)))
        assert len(versions) == 2
        assert versions[0].dao_item.uid in {self.uid, self.uid_1}
        assert versions[1].dao_item.uid in {self.uid, self.uid_1}

    def test_remove_expired_versions(self):
        assert len(list(VersionChain.ensure(self.resource.resource_id).iterate_over_all_versions())) == 2
        assert len(list(VersionChain.ensure(self.resource_1.resource_id).iterate_over_all_versions())) == 2
        with MulcaServiceStub(), \
             mock.patch('mpfs.core.filesystem.cleaner.controllers.DeletedStidsController.bulk_create') as stids_stub:
            for shard_endpoint in get_all_shard_endpoints():
                versions = list(VersionManager.fetch_expired_versions_on_shard(shard_endpoint, 1000))
                VersionManager.bulk_remove_versions_on_shard(shard_endpoint, versions)
        for shard_endpoint in get_all_shard_endpoints():
            assert not list(VersionManager.fetch_expired_versions_on_shard(shard_endpoint, 1000))
        assert len(list(VersionChain.ensure(self.resource.resource_id).iterate_over_all_versions())) == 1
        assert len(list(VersionChain.ensure(self.resource_1.resource_id).iterate_over_all_versions())) == 1

        deleted_stids = {s.stid for calls in stids_stub.call_args_list for s in calls[0][0]}
        assert len(deleted_stids) == 6

    def test_bulk_remove_versions(self):
        versions = list(VersionChain.ensure(self.resource.resource_id).iterate_over_all_versions())
        versions += list(VersionChain.ensure(self.resource_1.resource_id).iterate_over_all_versions())
        assert len(versions) == 4
        with MulcaServiceStub():
            VersionManager.bulk_remove_versions(versions)
        versions = list(VersionChain.ensure(self.resource.resource_id).iterate_over_all_versions())
        versions += list(VersionChain.ensure(self.resource_1.resource_id).iterate_over_all_versions())
        assert len(versions) == 0

    def test_save_locked_version(self):
        resource = self.resource
        _, versions_before = ResourceVersionManager.get_all_versions(resource)
        restore_uid = self.uid
        save_version = versions_before[-1]
        restore_platform = 'andr'

        LockHelper.lock(resource, time_offset=60)
        resp = self.json_error(
            'versioning_save',
            {'uid': restore_uid,
             'resource_id': resource.resource_id.serialize(),
             'version_id': save_version.id,
             'meta': 'hid,file_mid,pmid,digest_mid,md5,size,sha256'},
            headers={'Yandex-Cloud-Request-ID': '%s-123' % restore_platform},
            code=105
        )
        LockHelper.unlock(resource)

    def test_restore_locked_version(self):
        resource = self.resource
        _, versions_before = ResourceVersionManager.get_all_versions(resource)
        restore_uid = self.uid
        save_version = versions_before[-1]
        restore_platform = 'andr'

        LockHelper.lock(resource, time_offset=60)
        resp = self.json_error(
            'versioning_restore',
            {'uid': restore_uid,
             'resource_id': resource.resource_id.serialize(),
             'version_id': versions_before[-1].dao_item.id,
             'meta': 'hid,file_mid,pmid,digest_mid,md5,size,sha256'},
            headers={'Yandex-Cloud-Request-ID': '%s-123' % restore_platform},
            code=105
        )
        LockHelper.unlock(resource)


class VersionCleanerTestCase(VersionManagerTestCase):
    def test_cleaner(self):
        versions = []
        for shard_endpoint in get_all_shard_endpoints():
            versions.extend(list(VersionManager.fetch_expired_versions_on_shard(shard_endpoint, 1000)))
        assert len(versions) == 2
        with QuellerStub():
            VersionsCleanerManager.run()
        versions = []
        for shard_endpoint in get_all_shard_endpoints():
            versions.extend(list(VersionManager.fetch_expired_versions_on_shard(shard_endpoint, 1000)))
        assert len(versions) == 0

    def test_version_links_cleaner(self):
        vlinks_without_versions = []
        vlinks_with_versions = []
        dt = datetime.datetime.now() + self.one_day

        # Files without version data
        for file_num in range(4):
            file_path = "/disk/no_version_%i.txt" % file_num
            self.upload_file(self.uid, file_path)
            resource = get_resource(self.uid, file_path)
            with time_machine(datetime.datetime.now() - datetime.timedelta(hours=2)):
                vl = VersionChain.ensure(resource.resource_id)
            vlinks_without_versions.append((resource.resource_id, vl.dao_item.id))

        # Files with version data
        for file_num in range(4):
            file_path = "/disk/with_version_%i.txt" % file_num
            self.upload_file(self.uid, file_path)
            resource = get_resource(self.uid, file_path)
            with time_machine(datetime.datetime.now() - datetime.timedelta(hours=2)):
                vl = VersionChain.ensure(resource.resource_id)
                version = Version.create_binary(resource, dt)
            vl.append_version(version)
            vlinks_with_versions.append((resource.resource_id, vl.dao_item.id))

        # File with versions from parent class
        vlinks_with_versions.append((self.resource.resource_id,
                                        VersionChain.get_by_resource_id(self.resource.resource_id).dao_item.id))

        with QuellerStub():
            VersionsCleanerManager.run()

        # Check, that initial version link id match
        for resource_id, vl_id in vlinks_with_versions:
            assert vl_id == VersionChain.get_by_resource_id(resource_id).dao_item.id

        # Verify that empty version links were cleaned
        for resource_id, _ in vlinks_without_versions:
            with self.assertRaises(VersionLinkNotFound):
                VersionChain.get_by_resource_id(resource_id)

    def test_version_links_cleaner_timestamp_check(self):
        vlinks_without_time_lag = []
        vlinks_with_time_lag = []

        # Version links with current timestamp
        for file_num in range(4):
            file_path = "/disk/no_lag_%i.txt" % file_num
            self.upload_file(self.uid, file_path)
            resource = get_resource(self.uid, file_path)
            vl = VersionChain.ensure(resource.resource_id)
            vlinks_without_time_lag.append((resource.resource_id, vl.dao_item.id))

        # Version links with time lag
        for file_num in range(4):
            file_path = "/disk/with_lag_%i.txt" % file_num
            self.upload_file(self.uid, file_path)
            resource = get_resource(self.uid, file_path)
            with time_machine(datetime.datetime.now() - datetime.timedelta(hours=2)):
                vl = VersionChain.ensure(resource.resource_id)
                vlinks_with_time_lag.append((resource.resource_id, vl.dao_item.id))

        with QuellerStub():
            VersionsCleanerManager.run()

        for resource_id, vl_id in vlinks_without_time_lag:
            assert vl_id == VersionChain.get_by_resource_id(resource_id).dao_item.id

        for resource_id, _ in vlinks_with_time_lag:
            with self.assertRaises(VersionLinkNotFound):
                VersionChain.get_by_resource_id(resource_id)


class VersionTestCase(DiskTestCase):
    def setup_method(self, method):
        super(VersionTestCase, self).setup_method(method)
        path = '/disk/1.txt'
        self.upload_file(self.uid, path)
        self.resource = get_resource(self.uid, path)

    def test_save_with_same_time(self):
        now_dt = datetime.datetime.now()
        vc = VersionChain.ensure(self.resource.resource_id)

        version = Version.create_fake(self.uid, VersionType.trashed, now_dt, now_dt)
        vc.append_version(version)
        version = Version.create_fake(self.uid, VersionType.trashed, now_dt, now_dt)
        vc.append_version(version)

        versions = list(vc.iterate_over_all_versions())
        assert len(versions) == 2
        assert versions[0].date_created != versions[1].date_created

    def test_save_not_binded_version(self):
        now_dt = datetime.datetime.now()
        version = Version.create_fake(self.uid, VersionType.trashed, now_dt, now_dt)
        with self.assertRaises(ValueError):
            version.save()

    def test_save(self):
        now_dt = datetime.datetime.now()
        vc = VersionChain.ensure(self.resource.resource_id)
        version = Version.create_fake(self.uid, VersionType.trashed, now_dt, now_dt)
        vc._bind_version(version)
        version.save()
        versions = list(vc.iterate_over_all_versions())
        assert len(versions) == 1
        for field in ('id', 'platform_created', 'size', 'type', 'uid_created'):
            assert getattr(version, field) == getattr(versions[0], field)
        for field in ('date_created',):
            expected = getattr(version, field).replace(microsecond=0)
            in_db = getattr(versions[0], field).replace(microsecond=0)
            assert expected == in_db

    def test_upsert(self):
        now_dt = datetime.datetime.now()
        vc = VersionChain.ensure(self.resource.resource_id)
        version = Version.create_fake(self.uid, VersionType.trashed, now_dt, now_dt)
        vc._bind_version(version)
        version.save()
        saved_version = list(vc.iterate_over_all_versions())[0]
        assert saved_version.uid_created == self.uid

        version.dao_item.uid_created = '111'
        version.save()
        saved_version = list(vc.iterate_over_all_versions())[0]
        assert saved_version.uid_created == '111'
        for field in ('id', 'platform_created', 'size', 'type', 'uid_created'):
            assert getattr(version, field) == getattr(saved_version, field)
        for field in ('date_created',):
            expected = getattr(version, field).replace(microsecond=0)
            in_db = getattr(saved_version, field).replace(microsecond=0)
            assert expected == in_db


class VersionChainTestCase(DiskTestCase):
    checkpoint_delta = datetime.timedelta(minutes=10, seconds=1)
    page_size = 40

    def setup_method(self, method):
        super(VersionChainTestCase, self).setup_method(method)
        path = '/disk/1.txt'
        self.upload_file(self.uid, path)
        self.resource = get_resource(self.uid, path)

    def test_ensure(self):
        vl = VersionChain.ensure(self.resource.resource_id)
        assert vl.dao_item.uid == self.resource.uid
        assert vl.dao_item.file_id == self.resource.resource_id.file_id

    def test_version_wrong_times(self):
        vl = VersionChain.ensure(self.resource.resource_id)
        versions = [
            Version.create_fake(self.uid, VersionType.trashed, datetime.datetime.now(), datetime.datetime.now()),
            Version.create_fake(self.uid, VersionType.trashed, datetime.datetime.now() - datetime.timedelta(days=1), datetime.datetime.now()),
        ]
        with self.assertRaises(VersionWrongTimes):
            vl.append_versions(versions)

    def test_remove(self):
        vl = VersionChain.ensure(self.resource.resource_id)
        versions = [
            Version.create_binary(self.resource, datetime.datetime.now()),
            Version.create_fake(self.uid, VersionType.trashed, datetime.datetime.now(), datetime.datetime.now()),
        ]
        for version in versions:
            vl.append_version(version)

        _, versions = vl.get_all_versions(VersioningIterationKey.first_page())
        assert len(versions) == 2

        with mock.patch('mpfs.core.filesystem.cleaner.controllers.DeletedStidsController.bulk_create') as stids_stub:
            vl.remove()

        with self.assertRaises(VersionLinkNotFound):
            VersionChain.get_by_resource_id(self.resource.resource_id)

        with self.assertRaises(VersionLinkNotFound):
            VersionChain.get_by_resource_id(self.resource.resource_id)

        version_dao_items = VersionDataDAO().get_all(vl.dao_item.uid, vl.dao_item.id, None, None)
        assert len(version_dao_items) == 0

        resources_stids = {
            self.resource.file_mid(),
            self.resource.digest_mid(),
            self.resource.preview_mid(),
        }
        deleted_stids = {s.stid for calls in stids_stub.call_args_list for s in calls[0][0]}
        assert deleted_stids == resources_stids

    def test_get_resource(self):
        with self.assertRaises(VersionLinkNotFound):
            VersionChain.get_by_resource_id(self.resource.resource_id)
        VersionChain.ensure(self.resource.resource_id)
        vl = VersionChain.get_by_resource_id(self.resource.resource_id)
        assert vl.dao_item.uid == self.resource.uid
        assert vl.dao_item.file_id == self.resource.resource_id.file_id

    def test_appendleft_versions(self):
        rd = RelativeTimeDelta()
        day_delta = datetime.timedelta(days=1)
        versions = []
        for _ in range(5):
            versions.append(
                Version.create_fake(self.uid, VersionType.trashed, rd.step(day_delta), rd.cur_dt)
            )
        vl = VersionChain.ensure(self.resource.resource_id)
        vl.append_versions(versions[3:])
        vl.appendleft_versions(versions[1:3])
        vl.appendleft_versions(versions[:1])
        with self.assertRaises(VersionWrongTimes):
            vl.appendleft_versions(versions[3:])

    def test_move_versions(self):
        rd = RelativeTimeDelta()
        day_delta = datetime.timedelta(days=1)
        versions = []
        for _ in range(5000):
            versions.append(
                Version.create_fake(self.uid, VersionType.trashed, rd.step(day_delta), rd.cur_dt)
            )
        vl = VersionChain.ensure(self.resource.resource_id)
        vl.append_versions(versions)

        path = '/disk/2.txt'
        self.upload_file(self.uid, path)
        another_resource = get_resource(self.uid, path)
        another_vl = VersionChain.ensure(another_resource.resource_id)
        ResourceVersionManager.move_versions(self.resource.resource_id, another_resource.resource_id)

        with self.assertRaises(VersionLinkNotFound):
            vl.get_by_resource_id(self.resource.resource_id)
        another_vl = VersionChain.get_by_resource_id(another_resource.resource_id)
        assert len(list(another_vl.iterate_over_all_versions())) == 5000

    def test_phantom_path(self):
        vl = VersionChain.ensure(self.resource.resource_id)
        assert vl.dao_item.disk_path is None
        assert vl.dao_item.disk_path_hash is None

        phantom_path = '/disk/test'
        vl.set_phantom_path(phantom_path)
        assert vl.dao_item.disk_path is phantom_path
        assert vl.dao_item.disk_path_hash is not None

        get_by_path_vl = VersionChain.get_by_phantom_address(self.uid, Address.Make(self.uid, phantom_path))
        assert vl.dao_item.id == get_by_path_vl.dao_item.id

        vl.reset_phantom_path()
        with self.assertRaises(VersionLinkNotFound):
            VersionChain.get_by_phantom_address(self.uid, Address.Make(self.uid, phantom_path))
        assert vl.dao_item.disk_path is None
        assert vl.dao_item.disk_path_hash is None

        vl = VersionChain.ensure(self.resource.resource_id)
        assert vl.dao_item.disk_path is None
        assert vl.dao_item.disk_path_hash is None

    def test_get_all_versions_empty(self):
        vl = VersionChain.ensure(self.resource.resource_id)
        iter_key, versions = vl.get_all_versions(VersioningIterationKey.first_page())
        assert iter_key is None
        assert versions == []

    def test_get_all_versions_one_page(self):
        vl = VersionChain.ensure(self.resource.resource_id)
        relative_delta = RelativeTimeDelta()
        for i in range(self.page_size - 1):
            version = Version.create_fake(self.uid, VersionType.trashed, relative_delta.step(), relative_delta.cur_dt)
            vl.append_version(version)
        iter_key, versions = vl.get_all_versions(VersioningIterationKey.first_page())
        assert iter_key is None
        assert len(versions) == self.page_size - 1

    def test_get_all_versions_two_pages(self):
        vl = VersionChain.ensure(self.resource.resource_id)
        relative_delta = RelativeTimeDelta()
        for i in range(self.page_size + 1):
            version = Version.create_fake(self.uid, VersionType.trashed, relative_delta.step(), relative_delta.cur_dt)
            vl.append_version(version)

        iter_key, versions = vl.get_all_versions(VersioningIterationKey.first_page())
        assert iter_key is not None
        assert len(versions) == self.page_size
        iter_key, versions = vl.get_all_versions(iter_key)
        assert iter_key is None
        assert len(versions) == 1

    def test_get_checkpoint_versions_empty(self):
        vl = VersionChain.ensure(self.resource.resource_id)
        iter_key, versions = vl.get_checkpoint_versions(VersioningIterationKey.first_page())
        assert iter_key is None
        assert versions == []

    @parameterized.expand([
        ('folded', datetime.timedelta(seconds=1)),
        ('checkpoints', datetime.timedelta(hours=1)),
    ])
    def test_get_earliest_and_latest_version(self, _, dt_delta):
        relative_delta = RelativeTimeDelta()
        versions = []
        for i in range(10):
            version = Version.create_fake(self.uid, VersionType.trashed, relative_delta.step(dt_delta), relative_delta.cur_dt)
            versions.append(version)
        vl = VersionChain.ensure(self.resource.resource_id)
        vl.append_versions(versions)

        earliest_version = vl.get_earliest_version()
        latest_version = vl.get_latest_version()
        assert earliest_version.id == versions[0].id
        assert latest_version.id == versions[-1].id

    def test_one_version(self):
        vl = VersionChain.ensure(self.resource.resource_id)
        version = Version.create_fake(self.uid, VersionType.trashed, datetime.datetime.now(), datetime.datetime.now())
        vl.append_version(version)
        iter_key, versions = vl.get_checkpoint_versions(VersioningIterationKey.first_page())
        assert iter_key is None
        assert len(versions) == 1
        assert versions[0].dao_item.folded_counter == 0
        assert versions[0].get_iteration_key_for_folded_items() is None

    def test_get_checkpoint_versions_one(self):
        vl = VersionChain.ensure(self.resource.resource_id)

        relative_delta = RelativeTimeDelta()
        for i in range(10):
            version = Version.create_fake(self.uid, VersionType.trashed, relative_delta.step(), relative_delta.cur_dt)
            vl.append_version(version)

        iter_key, versions = vl.get_checkpoint_versions(VersioningIterationKey.first_page())
        assert iter_key is None
        assert len(versions) == 1
        assert versions[0].dao_item.id == version.dao_item.id
        assert versions[0].dao_item.folded_counter == 10 - 1
        assert versions[0].get_iteration_key_for_folded_items() is not None

    def test_get_checkpoint_versions_one_page(self):
        vl = VersionChain.ensure(self.resource.resource_id)
        relative_delta = RelativeTimeDelta()
        checkpoint_version_ids = []
        for _ in range(5):
            for _ in range(3):
                version = Version.create_fake(self.uid, VersionType.trashed, relative_delta.cur_dt, relative_delta.cur_dt)
                vl.append_version(version)
                relative_delta.step(datetime.timedelta(seconds=1))
            checkpoint_version_ids.append(version.dao_item.id)
            relative_delta.step(self.checkpoint_delta)
        checkpoint_version_ids.reverse()

        iter_key, versions = vl.get_checkpoint_versions(VersioningIterationKey.first_page())
        assert iter_key is None
        assert len(versions) == 5
        assert [v.dao_item.id for v in versions] == checkpoint_version_ids
        assert all([v.dao_item.is_checkpoint for v in versions])
        for v in versions:
            assert v.dao_item.folded_counter == 3 - 1
            assert v.get_iteration_key_for_folded_items() is not None

    def test_get_folded_versions_two_pages(self):
        vl = VersionChain.ensure(self.resource.resource_id)
        relative_delta = RelativeTimeDelta()
        folded_versions_ids = []
        for i in range(50):
            version = Version.create_fake(self.uid, VersionType.trashed, relative_delta.step(), relative_delta.cur_dt)
            vl.append_version(version)
            folded_versions_ids.append(version.dao_item.id)
        folded_versions_ids.pop()
        folded_versions_ids.reverse()

        iter_key, versions = vl.get_checkpoint_versions(VersioningIterationKey.first_page())
        assert len(versions) == 1

        folded_iter_key = versions[0].get_iteration_key_for_folded_items()
        folded_iter_key, folded_versions = vl.get_folded_versions(folded_iter_key)
        assert folded_iter_key is not None
        assert len(folded_versions) == self.page_size
        assert all([v.dao_item.is_checkpoint is False for v in folded_versions])
        assert [v.dao_item.id for v in folded_versions] == folded_versions_ids[:self.page_size]

        folded_iter_key, folded_versions = vl.get_folded_versions(folded_iter_key)
        assert folded_iter_key is None
        assert len(folded_versions) == 9  # создали 50 версий, 1 - чекпойнт, 40 отдали на для первой стринцы, осталось 9
        assert all([v.dao_item.is_checkpoint is False for v in folded_versions])
        assert [v.dao_item.id for v in folded_versions] == folded_versions_ids[self.page_size:50]

    def test_get_checkpoint_versions_two_pages(self):
        vl = VersionChain.ensure(self.resource.resource_id)
        relative_delta = RelativeTimeDelta()
        checkpoint_version_ids = []
        for _ in range(self.page_size + 1):
            for _ in range(3):
                version = Version.create_fake(self.uid, VersionType.trashed, relative_delta.cur_dt, relative_delta.cur_dt)
                vl.append_version(version)
                relative_delta.step(datetime.timedelta(seconds=1))
            checkpoint_version_ids.append(version.dao_item.id)
            relative_delta.step(self.checkpoint_delta)
        checkpoint_version_ids.reverse()

        iter_key, versions = vl.get_checkpoint_versions(VersioningIterationKey.first_page())
        assert iter_key is not None
        assert len(versions) == self.page_size
        assert all([v.dao_item.is_checkpoint for v in versions])
        assert [v.dao_item.id for v in versions] == checkpoint_version_ids[:self.page_size]
        iter_key, versions = vl.get_checkpoint_versions(iter_key)
        assert iter_key is None
        assert len(versions) == 1
        assert all([v.dao_item.is_checkpoint for v in versions])
        assert [v.dao_item.id for v in versions] == checkpoint_version_ids[self.page_size:self.page_size + 1]

    def test_get_like_verstka(self):
        # верстка работает так:
        # 1. Получает сначала чекпойнты
        # 2. По требованию пользователя разворачивает свернутые версии
        vl = VersionChain.ensure(self.resource.resource_id)
        relative_delta = RelativeTimeDelta()
        version_ids_map = OrderedDict()
        # добавляем 2 страницы чекпойнтов
        # у каждого чекпойнта по 22свернутых версий
        for _ in range(self.page_size + 1):
            version_ids = []
            for _ in range(3):
                version = Version.create_fake(self.uid, VersionType.trashed, relative_delta.cur_dt, relative_delta.cur_dt)
                vl.append_version(version)
                relative_delta.step(datetime.timedelta(seconds=1))
                version_ids.append(version.dao_item.id)
            checkpoint_version_id = version_ids.pop()
            version_ids_map[checkpoint_version_id] = version_ids
            relative_delta.step(self.checkpoint_delta)

        checkpoint_version_ids = version_ids_map.keys()
        checkpoint_version_ids.reverse()

        # проверям получение чекпойнтов
        get_checkpoint_version_ids = []
        iter_key = VersioningIterationKey.first_page()
        while True:
            iter_key, checkpoint_versions = vl.get_checkpoint_versions(iter_key)

            for checkpoint_version in checkpoint_versions:
                folded_iter_key = checkpoint_version.get_iteration_key_for_folded_items()
                next_folded_iter_key, folded_versions = vl.get_folded_versions(folded_iter_key)
                assert next_folded_iter_key is None
                assert len(folded_versions) == 2
                assert version_ids_map[checkpoint_version.dao_item.id] == [v.dao_item.id for v in folded_versions][::-1]

            get_checkpoint_version_ids += [v.dao_item.id for v in checkpoint_versions]
            assert all([v.dao_item.folded_counter == 2 for v in checkpoint_versions])
            assert all([v.dao_item.is_checkpoint for v in checkpoint_versions])
            if iter_key is None:
                break
        assert checkpoint_version_ids == get_checkpoint_version_ids

    def test_linked_versions(self):
        vl = VersionChain.ensure(self.resource.resource_id)
        relative_delta = RelativeTimeDelta()
        for i in range(10):
            version = Version.create_fake(self.uid, VersionType.trashed, relative_delta.step(), relative_delta.cur_dt)
            vl.append_version(version)

        _, versions = vl.get_all_versions(VersioningIterationKey.first_page())
        for cur, nex in pairwise(versions):
            assert cur.dao_item.parent_version_id == nex.dao_item.id
        nex.dao_item.parent_version_id is None

    def test_truncate_common_versions(self):
        # создаем один чекпойнт и много обычных версий
        vl = VersionChain.ensure(self.resource.resource_id)
        relative_delta = RelativeTimeDelta()
        version_ids = []
        for _ in range(50):
            version = Version.create_fake(self.uid, VersionType.trashed, relative_delta.cur_dt, relative_delta.cur_dt)
            vl.append_version(version)
            relative_delta.step(datetime.timedelta(seconds=1))
            version_ids.append(version.dao_item.id)
        version_ids.reverse()

        all_versions = list(vl.iterate_over_all_versions())
        assert len(all_versions) == 50
        assert [i.id for i in all_versions] == version_ids

        vl.truncate(40)
        all_versions = list(vl.iterate_over_all_versions())
        assert len(all_versions) == 40
        assert [i.id for i in all_versions] == version_ids[:40]

        vl.truncate(10)
        all_versions = list(vl.iterate_over_all_versions())
        assert len(all_versions) == 10
        assert [i.id for i in all_versions] == version_ids[:10]

        vl.truncate(0)
        all_versions = list(vl.iterate_over_all_versions())
        assert len(all_versions) == 0

    def test_truncate_checkpoints(self):
        # создаем одни чекпойнты
        vl = VersionChain.ensure(self.resource.resource_id)
        relative_delta = RelativeTimeDelta()
        version_ids = []
        for _ in range(50):
            version = Version.create_fake(self.uid, VersionType.trashed, relative_delta.cur_dt, relative_delta.cur_dt)
            vl.append_version(version)
            relative_delta.step(datetime.timedelta(minutes=20))
            version_ids.append(version.dao_item.id)
        version_ids.reverse()

        all_versions = list(vl.iterate_over_all_versions())
        assert all([i.is_checkpoint for i in all_versions])
        assert len(all_versions) == 50
        assert [i.id for i in all_versions] == version_ids

        vl.truncate(40)
        all_versions = list(vl.iterate_over_all_versions())
        assert len(all_versions) == 40
        assert [i.id for i in all_versions] == version_ids[:40]

        vl.truncate(10)
        all_versions = list(vl.iterate_over_all_versions())
        assert len(all_versions) == 10
        assert [i.id for i in all_versions] == version_ids[:10]

        vl.truncate(0)
        all_versions = list(vl.iterate_over_all_versions())
        assert len(all_versions) == 0

    def test_truncate_common_and_checkpoint_versions(self):
        # создаем 10 чекпойнтов с 4 обычными версиями каждый
        vl = VersionChain.ensure(self.resource.resource_id)
        relative_delta = RelativeTimeDelta()
        version_ids = []
        for _ in range(10):
            for _ in range(5):
                version = Version.create_fake(self.uid, VersionType.trashed, relative_delta.cur_dt, relative_delta.cur_dt)
                vl.append_version(version)
                relative_delta.step(datetime.timedelta(seconds=1))
                version_ids.append(version.dao_item.id)
            relative_delta.step(datetime.timedelta(minutes=20))
        version_ids.reverse()

        all_versions = list(vl.iterate_over_all_versions())
        assert len(all_versions) == 50
        assert [i.id for i in all_versions] == version_ids

        vl.truncate(40)
        all_versions = list(vl.iterate_over_all_versions())
        assert len(all_versions) == 40
        # оставляем чекпойнты, они находятся на каждой 5-ой позиции
        assert [i.id for i in all_versions] == version_ids[:38] + [version_ids[40]] + [version_ids[45]]
        assert all_versions[-1].dao_item.folded_counter == 0

        vl.truncate(11)
        all_versions = list(vl.iterate_over_all_versions())
        assert len(all_versions) == 11
        assert [i.id for i in all_versions] == version_ids[:2] + version_ids[5::5]

        vl.truncate(6)
        all_versions = list(vl.iterate_over_all_versions())
        # для дебага посмотреть как отрезались версии
        #for version in all_versions:
        #    print version.id, version.is_checkpoint, version_ids.index(version.id)
        assert len(all_versions) == 6
        assert [i.id for i in all_versions] == version_ids[0:30:5]

        vl.truncate(0)
        all_versions = list(vl.iterate_over_all_versions())
        assert len(all_versions) == 0


class VersioningIntegrationTestCase(DiskTestCase):
    def setup_method(self, method):
        super(VersioningIntegrationTestCase, self).setup_method(method)
        self.file_path = '/disk/1.txt'
        self.upload_file(self.uid, self.file_path)

    def test_meta_versioning_status(self):
        get_versioning_status = lambda p: self.json_ok('info', {'uid': self.uid, 'path': p, 'meta': ''})['meta'].get('versioning_status')
        get_is_versionable = lambda p: ResourceVersionManager.is_versionable(get_resource(self.uid, p))

        assert get_versioning_status(self.file_path) == 'versionable'

        trash_path = self.json_ok('trash_append', {'uid': self.uid, 'path': self.file_path})['this']['id']
        assert get_versioning_status(trash_path) is None

        self.upload_file(self.uid, '/attach/1.txt')
        attach_path = self.json_ok('list', {'uid': self.uid, 'path': '/attach'})[-1]['path']
        assert get_versioning_status(attach_path) is None

    def test_get_versions(self):
        # у нас 5 версий (из них 4 свернуты) + сам файл
        for _ in range(5):
            self.upload_file(self.uid, self.file_path, force=1)
        resource = get_resource(self.uid, self.file_path)

        resp = self.json_ok('versioning_get_checkpoints', {'uid': self.uid, 'resource_id': resource.resource_id.serialize()})
        assert len(resp['versions']) == 2
        assert resp['iteration_key'] is None
        binary_version = resp['versions'][-1]
        assert 'file_url' in binary_version
        assert 'md5' in binary_version
        assert len(binary_version['md5']) == 32
        assert 'sha256' in binary_version
        assert len(binary_version['sha256']) == 64
        assert len(resp['versions'][-1]['file_url'])

        resp = self.json_ok('versioning_get_folded', {'uid': self.uid,
                                                      'resource_id': resource.resource_id.serialize(),
                                                      'iteration_key': resp['versions'][-1]['folded_items_iteration_key']})
        assert len(resp['versions']) == 4
        assert resp['iteration_key'] is None
        assert 'file_url' in resp['versions'][-1]
        assert len(resp['versions'][-1]['file_url'])

        self.json_error('versioning_get_folded',
                        {'uid': self.uid,
                         'resource_id': resource.resource_id.serialize(),
                         'iteration_key': ''},
                        status=400)

    def test_store_force(self):
        self.upload_file(self.uid, self.file_path, force=1)

        versions = get_all_versions(self.uid, self.file_path)
        assert len(versions) == 1
        assert versions[0].dao_item.type == VersionType.binary

    def test_store_force_hardlink(self):
        self.upload_file(self.uid, '/disk/another_file')
        another_resource = get_resource(self.uid, '/disk/another_file')
        checksums = another_resource.get_checksums()

        self.upload_file(self.uid, self.file_path, force=1,
                         file_data={'md5': checksums.md5, 'sha256': checksums.sha256, 'size': checksums.size})
        resource = get_resource(self.uid, self.file_path)
        # проверка, что был хардлинк
        assert resource.file_mid() == another_resource.file_mid()

        versions = get_all_versions(self.uid, self.file_path)
        assert len(versions) == 1
        assert versions[0].dao_item.type == VersionType.binary

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Postgres test')
    def test_bind_from_trash_hardlink_store_with_checksums(self):
        self.upload_file(self.uid, '/disk/another_file')
        another_resource = get_resource(self.uid, '/disk/another_file')
        checksums = another_resource.get_checksums()

        self.json_ok('trash_append', {'uid': self.uid, 'path': self.file_path})
        with MulcaServiceStub():
            resp = self.json_ok(
                'store',
                {
                    'uid': self.uid,
                    'path': self.file_path,
                    'md5': checksums.md5,
                    'sha256': checksums.sha256,
                    'size': checksums.size
                }
            )
        assert resp['status'] == 'hardlinked'
        resource = get_resource(self.uid, self.file_path)
        # проверка, что был хардлинк
        assert resource.file_mid() == another_resource.file_mid()

        versions = get_all_versions(self.uid, self.file_path)
        assert len(versions) == 2
        assert versions[0].dao_item.type == VersionType.trashed
        assert versions[1].dao_item.type == VersionType.binary

    def test_trash_append(self):
        trash_path = self.json_ok('trash_append', {'uid': self.uid, 'path': self.file_path})['this']['id']

        versions = get_all_versions(self.uid, trash_path)
        assert len(versions) == 2
        assert versions[0].dao_item.type == VersionType.trashed
        assert versions[1].dao_item.type == VersionType.binary
        resource = get_resource(self.uid, trash_path)
        vl = VersionChain.ensure(resource.resource_id)
        assert vl.dao_item.disk_path == self.file_path
        assert vl.dao_item.disk_path_hash is not None

    def test_trash_restore(self):
        trash_path = self.json_ok('trash_append', {'uid': self.uid, 'path': self.file_path})['this']['id']
        self.json_ok('trash_restore', {'uid': self.uid, 'path': trash_path})

        versions = get_all_versions(self.uid, self.file_path)
        assert len(versions) == 3
        assert versions[0].dao_item.type == VersionType.restored
        assert versions[1].dao_item.type == VersionType.trashed
        assert versions[2].dao_item.type == VersionType.binary

        resource = get_resource(self.uid, self.file_path)
        vl = VersionChain.ensure(resource.resource_id)
        assert vl.dao_item.disk_path is None
        assert vl.dao_item.disk_path_hash is None

    def test_trash_append_and_store(self):
        self.json_ok('trash_append', {'uid': self.uid, 'path': self.file_path})
        self.upload_file(self.uid, self.file_path)

        versions = get_all_versions(self.uid, self.file_path)
        assert len(versions) == 2
        assert versions[0].dao_item.type == VersionType.trashed
        assert versions[1].dao_item.type == VersionType.binary

    def test_rate_limiter(self):
        start_dt = datetime.datetime.now()
        for i in range(10):
            trash_path = self.json_ok('trash_append', {'uid': self.uid, 'path': self.file_path})['this']['id']
            self.json_ok('trash_restore', {'uid': self.uid, 'path': trash_path})
        end_dt = datetime.datetime.now()
        test_duration = (end_dt - start_dt).total_seconds()
        full_minutes = math.ceil(test_duration / 60.0)
        versions = get_all_versions(self.uid, self.file_path)
        # в минуту лимит 10 версий. Меряем вермя создания версий и проверяем лимит сверху
        assert len(versions) <= 10 * full_minutes + 1

    def test_rate_limiter_per_user(self):
        with mock.patch.object(ResourceVersionManager.limiter_per_user, '_burst', 6), \
                mock.patch.object(ResourceVersionManager.limiter_per_user, '_rph', 1):
            for i in range(3):
                trash_path = self.json_ok('trash_append', {'uid': self.uid, 'path': self.file_path})['this']['id']
                self.json_ok('trash_restore', {'uid': self.uid, 'path': trash_path})
            assert len(get_all_versions(self.uid, self.file_path)) == 6

    def test_rate_limiter_per_user_not_limited_file(self):
        file_path = '/disk/1.docx'
        self.upload_file(self.uid, file_path)
        with mock.patch.object(ResourceVersionManager.limiter_per_user, '_burst', 6), \
             mock.patch.object(ResourceVersionManager.limiter_per_user, '_rph', 1):
            for i in range(3):
                trash_path = self.json_ok('trash_append', {'uid': self.uid, 'path': file_path})['this']['id']
                self.json_ok('trash_restore', {'uid': self.uid, 'path': trash_path})
            # trash_append генерит 2 версии
            assert len(get_all_versions(self.uid, file_path)) == 3 * (2 + 1)

    def test_truncate(self):
        with mock.patch.object(ResourceVersionManager, 'truncate_activation_limit', 12), \
                mock.patch.object(ResourceVersionManager, 'truncate_limit', 5), \
                mock.patch.object(ResourceVersionManager, 'limit_value', 500):
            for i in range(10):
                trash_path = self.json_ok('trash_append', {'uid': self.uid, 'path': self.file_path})['this']['id']
                self.json_ok('trash_restore', {'uid': self.uid, 'path': trash_path})
                assert len(get_all_versions(self.uid, self.file_path)) <= 12

    def append_version_to_resource(self, uid, path):
        resource = get_resource(uid, path)
        version = Version.create_binary(resource, datetime.datetime.now())
        vl = VersionChain.ensure(resource.resource_id)
        vl.append_version(version)
        return version

    def test_move_file(self):
        version = self.append_version_to_resource(self.uid, self.file_path)
        self.json_ok('move', {'uid': self.uid, 'src': self.file_path, 'dst': '/disk/dst.txt'})
        versions = get_all_versions(self.uid, '/disk/dst.txt')
        assert len(versions) == 1
        assert versions[0].dao_item.id == version.dao_item.id

    def test_copy_file(self):
        version = self.append_version_to_resource(self.uid, self.file_path)
        self.json_ok('copy', {'uid': self.uid, 'src': self.file_path, 'dst': '/disk/dst.txt'})

        with self.assertRaises(VersionLinkNotFound):
            get_all_versions(self.uid, '/disk/dst.txt')
        versions = get_all_versions(self.uid, self.file_path)
        assert len(versions) == 1
        assert versions[0].dao_item.id == version.dao_item.id

    def _prepare_folder_test(self):
        folder_path = '/disk/folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path})
        file_path = '%s/1.txt' % folder_path
        self.json_ok('move', {'uid': self.uid, 'src': self.file_path, 'dst': file_path})
        version = self.append_version_to_resource(self.uid, file_path)
        return folder_path, file_path, version

    def test_move_folder(self):
        folder_path, file_path, version = self._prepare_folder_test()
        new_folder_path = '%s_new' % folder_path
        new_file_path = '%s/1.txt' % new_folder_path
        self.json_ok('move', {'uid': self.uid, 'src': folder_path, 'dst': new_folder_path})
        versions = get_all_versions(self.uid, new_file_path)
        assert len(versions) == 1
        assert versions[0].dao_item.id == version.dao_item.id

    def test_copy_folder(self):
        folder_path, file_path, version = self._prepare_folder_test()
        new_folder_path = '%s_new' % folder_path
        new_file_path = '%s/1.txt' % new_folder_path
        self.json_ok('copy', {'uid': self.uid, 'src': folder_path, 'dst': new_folder_path})
        with self.assertRaises(VersionLinkNotFound):
            get_all_versions(self.uid, new_file_path)
        versions = get_all_versions(self.uid, file_path)
        assert len(versions) == 1
        assert versions[0].dao_item.id == version.dao_item.id

    def test_trash_append_folder(self):
        folder_path, file_path, version = self._prepare_folder_test()
        trash_path = self.json_ok('trash_append', {'uid': self.uid, 'path': folder_path})['this']['id']
        new_file_path = file_path.replace(folder_path, trash_path, 1)
        versions = get_all_versions(self.uid, new_file_path)
        assert len(versions) == 1
        assert versions[0].dao_item.id == version.dao_item.id

    def test_trash_append_and_restore_folder(self):
        folder_path, file_path, version = self._prepare_folder_test()
        trash_path = self.json_ok('trash_append', {'uid': self.uid, 'path': folder_path})['this']['id']
        self.json_ok('trash_restore', {'uid': self.uid, 'path': trash_path})

        versions = get_all_versions(self.uid, file_path)
        assert len(versions) == 1
        assert versions[0].dao_item.id == version.dao_item.id

    def test_get_version(self):
        self.upload_file(self.uid, self.file_path)
        resource = get_resource(self.uid, self.file_path)
        _, versions = ResourceVersionManager.get_all_versions(resource)
        version_id = versions[-1].id

        resp_by_path = self.json_ok('versioning_get_version', {'uid': self.uid, 'path': self.file_path, 'version_id': version_id})
        resp_by_resource_id = self.json_ok('versioning_get_version', {'uid': self.uid, 'resource_id': resource.resource_id.serialize(), 'version_id': version_id})
        assert resp_by_path == resp_by_resource_id
        assert resp_by_path['id'] == version_id

    def test_versions_common_visibility(self):
        now_dt = datetime.datetime.now()
        resource = get_resource(self.uid, self.file_path)
        vc = VersionChain.ensure(resource.resource_id)
        # создаём 100 версий по одной в день
        rd = RelativeTimeDelta(now_dt - datetime.timedelta(days=100))
        for _ in range(100):
            cur_dt = rd.step(datetime.timedelta(days=1))
            with time_machine(cur_dt):
                version = Version.create_fake(self.uid, VersionType.trashed, cur_dt, now_dt)
                vc.append_version(version)

        all_versions = list(vc.iterate_over_all_versions())
        assert len(all_versions) == 100

        # обычный пользователь
        resp = self.json_ok('versioning_get_checkpoints', {'uid': self.uid, 'resource_id': resource.resource_id.serialize()})
        assert len(resp['versions']) == 14 + 1  # +1 - текущая
        # делаем платным
        uid, sid = self._make_paid(self.uid)
        resp = self.json_ok('versioning_get_checkpoints', {'uid': self.uid, 'resource_id': resource.resource_id.serialize()})
        assert len(resp['versions']) == 40 + 1  # срабатывает пагинация - 40 версий на страницу
        resp = self.json_ok('versioning_get_checkpoints', {'uid': self.uid, 'resource_id': resource.resource_id.serialize(), 'iteration_key': resp['iteration_key']})
        assert len(resp['versions']) == 40
        resp = self.json_ok('versioning_get_checkpoints', {'uid': self.uid, 'resource_id': resource.resource_id.serialize(), 'iteration_key': resp['iteration_key']})
        assert len(resp['versions']) == 10
        # удаляем услугу - опять бесплатный
        resp = self.billing_ok('service_delete', {'uid': uid, 'ip': '1', 'sid': sid})
        resp = self.json_ok('versioning_get_checkpoints', {'uid': self.uid, 'resource_id': resource.resource_id.serialize()})
        assert len(resp['versions']) == 14 + 1  # +1 - текущая

    def _make_paid(self, uid):
        resp = self.billing_ok('service_create', {'uid': uid, 'ip': '1', 'line': 'primary_2015', 'pid': '10gb_1m_2015'})
        return uid, resp['sid']


class VersioningShareIntegrationTestCase(CommonJsonApiTestCase):
    def setup_method(self, method):
        super(VersioningShareIntegrationTestCase, self).setup_method(method)
        self.json_ok('user_init', {'uid': self.uid})
        self.json_ok('user_init', {'uid': self.uid_1})

        self.shared_dir_path = '/disk/shared_dir'
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.shared_dir_path})
        self.subdir_path = '/disk/shared_dir/subdir'
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.subdir_path})
        self.share_dir(self.uid, self.uid_1, self.email_1, self.shared_dir_path, rights=660)

    @staticmethod
    def get_versioned_fields(resource):
        checksums = resource.get_checksums()
        return {
            'source_uid': resource.meta.get('source_uid') or resource.owner_uid,
            'source_platform': resource.meta.get('source_platform'),
            'hid': checksums.hid,
            'size': checksums.size,
            'md5': checksums.md5,
            'sha256': checksums.sha256,
            'file_mid': resource.file_mid(),
            'preview_mid': resource.preview_mid(),
            'pmid': resource.preview_mid(),
            'digest_mid': resource.digest_mid(),
            'video_info': resource.get_video_info(),
            'etime': resource.meta.get('etime'),
            'drweb': resource.drweb(),
        }

    def upload_file_get_fields(self, uid, path, platform='mpfs', file_data=None):
        self.upload_file(uid, path,
                         file_data=file_data,
                         headers={'Yandex-Cloud-Request-ID': '%s-123' % platform})
        resource = get_resource(uid, path)
        return self.get_versioned_fields(resource)

    @staticmethod
    def check_version(version, resource_fields):
        assert version.dao_item.type in (VersionType.binary, VersionType.current)
        assert resource_fields['source_platform'] == version.dao_item.platform_created
        assert resource_fields['source_uid'] == version.dao_item.uid_created
        assert resource_fields['hid'] == version.dao_item.hid
        assert resource_fields['sha256'] == version.dao_item.sha256
        assert resource_fields['md5'] == version.dao_item.md5
        assert resource_fields['size'] == version.dao_item.size
        assert resource_fields['file_mid'] == version.dao_item.file_stid
        assert resource_fields['preview_mid'] == version.dao_item.preview_stid
        assert resource_fields['digest_mid'] == version.dao_item.digest_stid
        assert resource_fields['video_info'] == version.dao_item.video_info
        etime = time.mktime(version.dao_item.exif_time.timetuple()) if version.dao_item.exif_time else None
        assert resource_fields['etime'] == etime

    def test_binary_version_fields(self):
        file_path = "%s/%s" % (self.shared_dir_path, '1.txt')

        platforms = ['web', 'ios', 'rest']
        actor_uids = [self.uid, self.uid_1, self.uid]
        resources_fields = list()
        for uid, platform in zip(actor_uids, platforms):
            resources_fields.append(self.upload_file_get_fields(
                uid, file_path, platform=platform,
                file_data={'etime': str(int(time.time()))},
            ))

        resource = get_resource(self.uid, file_path)
        _, versions = ResourceVersionManager.get_all_versions(resource)
        versions.reverse()
        for version, resource_fields in zip(versions, resources_fields):
            self.check_version(version, resource_fields)

    @parameterized.expand([
        ('owner', True),
        ('invited', False),
    ])
    def test_save_version_as_file(self, case_name, is_owner_actor):
        file_path = "%s/%s" % (self.shared_dir_path, '1.txt')
        platforms = ['web', 'ios', 'rest']
        actor_uids = [self.uid, self.uid_1, self.uid]
        resources_fields = list()
        for uid, platform in zip(actor_uids, platforms):
            resources_fields.append(self.upload_file_get_fields(
                uid, file_path, platform=platform,
                file_data={'etime': str(int(time.time()))},
            ))

        resource = get_resource(self.uid, file_path)
        resource_fields_before = self.get_versioned_fields(resource)
        _, versions_before = ResourceVersionManager.get_all_versions(resource)
        restore_platform = 'andr'
        restore_uid = self.uid if is_owner_actor else self.uid_1

        save_version = versions_before[-1]

        resp_1 = self.json_ok(
            'versioning_save',
            {'uid': restore_uid,
             'resource_id': resource.resource_id.serialize(),
             'version_id': save_version.id,
             'meta': 'hid,file_mid,pmid,digest_mid,md5,size,sha256'},
            headers={'Yandex-Cloud-Request-ID': '%s-123' % restore_platform}
        )
        formated_dt = save_version.date_created.strftime(VERSIONING_SAVE_VERSION_DT_FORMAT)
        expected_path = u'%s/1 (версия от %s).txt' % (self.shared_dir_path, formated_dt)
        assert resp_1['path'] == expected_path
        resource_fields_1 = self.get_versioned_fields(get_resource(self.uid, expected_path))
        resp_2 = self.json_ok(
            'versioning_save',
            {'uid': restore_uid,
             'resource_id': resource.resource_id.serialize(),
             'version_id': save_version.id,
             'meta': 'hid,file_mid,pmid,digest_mid,md5,size,sha256'},
            headers={'Yandex-Cloud-Request-ID': '%s-123' % restore_platform}
        )
        expected_path = u'%s/1 (версия от %s) (1).txt' % (self.shared_dir_path, formated_dt)
        assert resp_2['path'] == expected_path
        resource_fields_2 = self.get_versioned_fields(get_resource(self.uid, expected_path))

        for field_name in ('hid', 'file_mid', 'pmid', 'digest_mid', 'md5', 'size', 'sha256'):
            assert resp_1['meta'][field_name] == resource_fields_1[field_name]
            assert resp_2['meta'][field_name] == resource_fields_2[field_name]
            assert resp_1['meta'][field_name] == resp_2['meta'][field_name]

    @parameterized.expand([
        ('owner', True),
        ('invited', False),
    ])
    def test_restore_version(self, case_name, is_owner_actor):
        file_path = "%s/%s" % (self.shared_dir_path, '1.txt')
        platforms = ['web', 'ios', 'rest']
        actor_uids = [self.uid, self.uid_1, self.uid]
        resources_fields = list()
        for uid, platform in zip(actor_uids, platforms):
            resources_fields.append(self.upload_file_get_fields(
                uid, file_path, platform=platform,
                file_data={'etime': str(int(time.time()))},
            ))

        resource = get_resource(self.uid, file_path)
        resource_fields_before = self.get_versioned_fields(resource)
        _, versions_before = ResourceVersionManager.get_all_versions(resource)
        restore_platform = 'andr'
        restore_uid = self.uid if is_owner_actor else self.uid_1

        resp = self.json_ok(
            'versioning_restore',
            {'uid': restore_uid,
             'resource_id': resource.resource_id.serialize(),
             'version_id': versions_before[-1].dao_item.id,
             'meta': 'hid,file_mid,pmid,digest_mid,md5,size,sha256'},
            headers={'Yandex-Cloud-Request-ID': '%s-123' % restore_platform}
        )

        resource = get_resource(self.uid, file_path)
        resource_fields_after = self.get_versioned_fields(resource)
        for field_name in ('hid', 'file_mid', 'pmid', 'digest_mid', 'md5', 'size', 'sha256'):
            assert resp['meta'][field_name] == resource_fields_after[field_name]
        _, versions_after = ResourceVersionManager.get_all_versions(resource)
        assert len(versions_after) == len(versions_before) + 1
        assert [v.dao_item.id for v in versions_after[2:]] == [v.dao_item.id for v in versions_before[1:]]

        # поля той версии, которую восстановили, сейчас у ресурса
        restored_version = versions_before[-1]
        restored_version.dao_item.uid_created = restore_uid
        restored_version.dao_item.platform_created = restore_platform
        self.check_version(restored_version, resource_fields_after)
        # добавили версию с предыдущими значениями
        self.check_version(versions_after[1], resource_fields_before)

    @parameterized.expand([
        ('owner', True),
        ('invited', False),
    ])
    def test_move_file_to_non_shared_folder(self, case_name, is_owner_actor):
        file_path = "%s/%s" % (self.subdir_path, '1.txt')
        self.upload_file(self.uid, file_path)
        self.upload_file(self.uid_1, file_path)

        resource = get_resource(self.uid, file_path)
        uid = self.uid if is_owner_actor else user_1.uid
        self.json_ok('move', {'uid': uid, 'src': file_path, 'dst': '/disk/1.txt'})

        resource = get_resource(uid, '/disk/1.txt')
        _, versions = ResourceVersionManager.get_all_versions(resource)
        assert len(versions) == 2

    @parameterized.expand([
        ('owner', True),
        ('invited', False),
    ])
    def test_move_folder_to_non_shared_folder(self, case_name, is_owner_actor):
        file_path = "%s/%s" % (self.subdir_path, '1.txt')
        self.upload_file(self.uid, file_path)
        self.upload_file(self.uid_1, file_path)

        resource = get_resource(self.uid, file_path)
        uid = self.uid if is_owner_actor else user_1.uid
        self.json_ok('move', {'uid': uid, 'src': self.subdir_path, 'dst': '/disk/dst'})

        resource = get_resource(uid, '/disk/dst/1.txt')
        _, versions = ResourceVersionManager.get_all_versions(resource)
        assert len(versions) == 2

    @parameterized.expand([
        ('owner', True),
        ('invited', False),
    ])
    def test_rm_folder(self, case_name, is_owner_actor):
        file_path = "%s/%s" % (self.subdir_path, '1.txt')
        self.upload_file(self.uid, file_path)
        self.upload_file(self.uid_1, file_path)

        resource = get_resource(self.uid, file_path)
        uid = self.uid if is_owner_actor else user_1.uid
        self.json_ok('rm', {'uid': uid, 'path': self.subdir_path})

        with self.assertRaises(VersionLinkNotFound):
            VersionChain.get_by_resource_id(resource.resource_id)

    @parameterized.expand([
        ('owner', True),
        ('invited', False),
    ])
    def test_rm_file(self, case_name, is_owner_actor):
        file_path = "%s/%s" % (self.subdir_path, '1.txt')
        self.upload_file(self.uid, file_path)
        self.upload_file(self.uid_1, file_path)

        resource = get_resource(self.uid, file_path)
        uid = self.uid if is_owner_actor else user_1.uid
        self.json_ok('rm', {'uid': uid, 'path': file_path})

        with self.assertRaises(VersionLinkNotFound):
            VersionChain.get_by_resource_id(resource.resource_id)

    @parameterized.expand([
        ('owner', True),
        ('invited', False),
    ])
    def test_trash_append_file(self, case_name, is_owner_actor):
        file_path = "%s/%s" % (self.subdir_path, '1.txt')
        self.upload_file(self.uid, file_path)
        self.upload_file(self.uid_1, file_path)

        resource = get_resource(self.uid, file_path)
        uid = self.uid if is_owner_actor else user_1.uid
        self.json_ok('trash_append', {'uid': uid, 'path': file_path})

        version_chain = VersionChain.get_by_resource_id(resource.resource_id)
        versions = list(version_chain.iterate_over_all_versions())
        assert len(versions) == 3
        assert [v.dao_item.type for v in versions] == [VersionType.trashed, VersionType.binary, VersionType.binary]

    @parameterized.expand([
        ('owner', True),
        ('invited', False),
    ])
    def test_trash_append_folder(self, case_name, is_owner_actor):
        file_path = "%s/%s" % (self.subdir_path, '1.txt')
        self.upload_file(self.uid, file_path)
        self.upload_file(self.uid_1, file_path)

        resource = get_resource(self.uid, file_path)
        uid = self.uid if is_owner_actor else user_1.uid
        self.json_ok('trash_append', {'uid': uid, 'path': self.subdir_path})
        version_chain = VersionChain.get_by_resource_id(resource.resource_id)
        versions = list(version_chain.iterate_over_all_versions())
        assert len(versions) == 1
        assert [v.dao_item.type for v in versions] == [VersionType.binary]

    def test_trash_drop_all(self):
        file_path = "/disk/%s" % '1.txt'
        self.upload_file(self.uid, file_path)
        self.upload_file(self.uid, file_path)
        file_resource_id = get_resource(self.uid, file_path).resource_id
        self.json_ok('trash_append', {'uid': self.uid, 'path': file_path})

        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/f'})
        file_path = "/disk/f/%s" % '1.txt'
        self.upload_file(self.uid, file_path)
        self.upload_file(self.uid, file_path)
        in_folder_file_resource_id = get_resource(self.uid, file_path).resource_id
        self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/f'})

        self.json_ok('trash_drop_all', {'uid': self.uid})

        with self.assertRaises(VersionLinkNotFound):
            VersionChain.get_by_resource_id(file_resource_id)
        with self.assertRaises(VersionLinkNotFound):
            VersionChain.get_by_resource_id(in_folder_file_resource_id)

    def test_trash_drop_all_check_remove_version_tasks_num(self):
        for i in range(2):
            self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/%i' % i})
            self.upload_file(self.uid, '/disk/%i/1.txt' % i)
            self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/%i' % i})

        for i in range(5):
            self.upload_file(self.uid, '/disk/%i.txt' % i)
            self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/%i.txt' % i})

        with mock.patch('mpfs.core.versioning.logic.version_manager.ResourceVersionManager.async_bulk_remove_versions', return_value=None) as task_put_mock:
            self.json_ok('trash_drop_all', {'uid': self.uid})
            # 2 таска на папки, один на остальные файлы
            assert len(task_put_mock.call_args_list) == 2 + 1

    def test_trash_drop_element(self):
        file_path = "/disk/%s" % '1.txt'
        self.upload_file(self.uid, file_path)
        self.upload_file(self.uid, file_path)
        file_resource_id = get_resource(self.uid, file_path).resource_id
        trash_path_1 = self.json_ok('trash_append', {'uid': self.uid, 'path': file_path})['this']['id']

        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/f'})
        file_path = "/disk/f/%s" % '1.txt'
        self.upload_file(self.uid, file_path)
        self.upload_file(self.uid, file_path)
        in_folder_file_resource_id = get_resource(self.uid, file_path).resource_id
        trash_path_f = self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/f'})['this']['id']

        self.json_ok('trash_drop', {'uid': self.uid, 'path': trash_path_f})
        with self.assertRaises(VersionLinkNotFound):
            VersionChain.get_by_resource_id(in_folder_file_resource_id)

        self.json_ok('trash_drop', {'uid': self.uid, 'path': trash_path_1})
        with self.assertRaises(VersionLinkNotFound):
            VersionChain.get_by_resource_id(file_resource_id)

    def test_common_workflow(self):
        # залили файл, перезаписали, удалили в корзину, по этому же пути залили
        # новый файл
        file_path = '/disk/1.txt'
        resource_fields = []
        self.upload_file(self.uid, file_path)
        resource_fields.append(self.get_versioned_fields(get_resource(self.uid, file_path)))
        self.upload_file(self.uid, file_path)
        resource_fields.append(self.get_versioned_fields(get_resource(self.uid, file_path)))
        trash_path = self.json_ok('trash_append', {'uid': self.uid, 'path': file_path})['this']['id']
        self.upload_file(self.uid, file_path)
        resource_fields.append(self.get_versioned_fields(get_resource(self.uid, file_path)))

        _, versions = ResourceVersionManager.get_all_versions(get_resource(self.uid, '/disk/1.txt'))
        assert len(versions) == 4
        assert [v.type for v in versions] == [VersionType.current, VersionType.trashed, VersionType.binary, VersionType.binary]
        self.check_version(versions[0], resource_fields[2])
        self.check_version(versions[2], resource_fields[1])
        self.check_version(versions[3], resource_fields[0])

        # восстановили файл из корзины, смотрим на его версии
        self.json_ok('trash_restore', {'uid': self.uid, 'path': trash_path})
        _, versions = ResourceVersionManager.get_all_versions(get_resource(self.uid, '/disk/1 (1).txt'))
        assert len(versions) == 5
        assert [v.type for v in versions] == [VersionType.current, VersionType.restored, VersionType.trashed, VersionType.binary, VersionType.binary]
        self.check_version(versions[0], resource_fields[1])
        self.check_version(versions[3], resource_fields[1])
        self.check_version(versions[4], resource_fields[0])
