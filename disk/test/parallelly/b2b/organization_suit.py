# -*- coding: utf-8 -*-

import mock

from mpfs.common.static import codes
from mpfs.core.billing import Client, Product, ServiceList
from mpfs.core.billing.processing.common import simple_create_service
from mpfs.core.organizations.dao.organizations import OrganizationDAO
from mpfs.core.organizations.logic import resync_organizations, B2B_FAKE_ORGANIZATION_SPACE_SERVICE_NAME, \
    update_organization
from mpfs.core.services.directory_service import DirectoryService, MigrationInProcessException
from mpfs.core.user.base import User
from test.helpers.stubs.services import DirectoryServiceSmartMockHelper
from test.parallelly.billing.base import BillingTestCaseMixin
from test.parallelly.json_api.base import CommonJsonApiTestCase


class OrganizationTestCaseBase(CommonJsonApiTestCase):
    dao = OrganizationDAO()

    def setup_method(self, method):
        super(OrganizationTestCaseBase, self).setup_method(method)
        DirectoryServiceSmartMockHelper.clear_cache()


class OrganizationEventTestCase(OrganizationTestCaseBase):
    def test_organization_added(self):
        DirectoryServiceSmartMockHelper.add_organization('125', 1000, 500, False)
        with DirectoryServiceSmartMockHelper.mock(), \
             mock.patch('mpfs.core.organizations.logic.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True):
            self.json_ok('organization_event', json=DirectoryServiceSmartMockHelper.get_event_organization_added('125'))

        organization = self.dao.find_by_id('125')
        assert not organization.is_paid
        assert organization.quota_limit == 1000
        assert organization.quota_free == 500

    def test_organization_subscription_plan_changed(self):
        self.dao.set_quota_limits_and_paid('125', 0, 0, False)
        DirectoryServiceSmartMockHelper.add_organization('125', 1000, 500, True)
        with DirectoryServiceSmartMockHelper.mock(), \
             mock.patch('mpfs.core.organizations.logic.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True):
            self.json_ok('organization_event', json=DirectoryServiceSmartMockHelper.get_event_organization_subscription_plan_changed('125', True))

        organization = self.dao.find_by_id('125')
        assert organization.is_paid
        assert organization.quota_limit == 1000
        assert organization.quota_free == 500

    def test_organization_in_migration(self):
        DirectoryServiceSmartMockHelper.add_organization('1', 0, 0, True, in_migration=True)
        with DirectoryServiceSmartMockHelper.mock(), \
             mock.patch('mpfs.core.organizations.logic.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True), \
             self.assertRaises(MigrationInProcessException):
            DirectoryService().get_organization_info(1)

    def test_organization_in_migration_update(self):
        DirectoryServiceSmartMockHelper.add_organization('1', 0, 0, True, in_migration=True)
        with DirectoryServiceSmartMockHelper.mock(), \
             mock.patch('mpfs.core.organizations.logic.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True):
            update_organization(1)


class OrganizationUserFileUploadTestCase(OrganizationTestCaseBase):
    def test__free_organization__file_size_bigger_than_quota__copy_succeeds(self):
        self.upload_file(self.uid, '/disk/file.jpg', file_data={'size': 10000})
        User(self.uid).make_b2b('125')
        self.dao.set_quota_limits_and_paid('125', 100000, 1000, False)
        with mock.patch('mpfs.core.services.disk_service.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True):
            self.json_ok('copy', {'uid': self.uid, 'src': '/disk/file.jpg', 'dst': '/disk/copy.jpg'})

    def test__free_organization__file_size_bigger_than_quota__store_succeeds(self):
        User(self.uid).make_b2b('125')
        self.dao.set_quota_limits_and_paid('125', 100000, 1000, False)
        with mock.patch('mpfs.core.services.disk_service.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True):
            self.json_ok('store', {'uid': self.uid, 'path': '/disk/file.jpg', 'size': 10000})

    def test__nonexistent_organization__copy_succeeds(self):
        self.upload_file(self.uid, '/disk/file.jpg', file_data={'size': 10000})
        User(self.uid).make_b2b('abc')
        with mock.patch('mpfs.core.services.disk_service.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True):
            self.json_ok('copy', {'uid': self.uid, 'src': '/disk/file.jpg', 'dst': '/disk/copy.jpg'})

    def test__nonexistent_organization__store_succeeds(self):
        User(self.uid).make_b2b('abc')
        with mock.patch('mpfs.core.services.disk_service.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True):
            self.json_ok('store', {'uid': self.uid, 'path': '/disk/file.jpg', 'size': 10000})

    def test__paid_organization__file_size_bigger_than_quota__feature_disabled__copy_succeeds(self):
        self.upload_file(self.uid, '/disk/file.jpg', file_data={'size': 10000})
        User(self.uid).make_b2b('125')
        self.dao.set_quota_limits_and_paid('125', 100000, 1000, True)
        with mock.patch('mpfs.core.services.disk_service.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', False):
            self.json_ok('copy', {'uid': self.uid, 'src': '/disk/file.jpg', 'dst': '/disk/copy.jpg'})

    def test__paid_organization__file_size_bigger_than_quota__feature_disabled__store_succeeds(self):
        User(self.uid).make_b2b('125')
        self.dao.set_quota_limits_and_paid('125', 100000, 1000, True)
        with mock.patch('mpfs.core.services.disk_service.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', False):
            self.json_ok('store', {'uid': self.uid, 'path': '/disk/file.jpg', 'size': 10000})

    def test__paid_organization__file_size_bigger_than_quota__feature_enabled__copy_fails(self):
        self.upload_file(self.uid, '/disk/file.jpg', file_data={'size': 10000})
        User(self.uid).make_b2b('125')
        self.dao.set_quota_limits_and_paid('125', 100000, 1000, True)
        with mock.patch('mpfs.core.services.disk_service.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True):
            self.json_error('copy', {'uid': self.uid, 'src': '/disk/file.jpg', 'dst': '/disk/copy.jpg'}, code=codes.NO_FREE_SPACE_COPY_TO_DISK)

    def test__paid_organization__file_size_bigger_than_quota__feature_enabled__store_fails(self):
        User(self.uid).make_b2b('125')
        self.dao.set_quota_limits_and_paid('125', 100000, 1000, True)
        with mock.patch('mpfs.core.services.disk_service.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True), \
             mock.patch('mpfs.core.filesystem.base.FEATURE_TOGGLES_CORRECT_SPACE_CHECKS_FOR_SHARED_FOLDERS', False):
            self.json_error('store', {'uid': self.uid, 'path': '/disk/file.jpg', 'size': 10000}, code=codes.NO_FREE_SPACE)

    def test__paid_organization__file_size_smaller_than_quota__copy_succeeds(self):
        self.upload_file(self.uid, '/disk/file.jpg', file_data={'size': 100})
        User(self.uid).make_b2b('125')
        self.dao.set_quota_limits_and_paid('125', 100000, 1000, True)
        with mock.patch('mpfs.core.services.disk_service.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True):
            self.json_ok('copy', {'uid': self.uid, 'src': '/disk/file.jpg', 'dst': '/disk/copy.jpg'})

    def test__paid_organization__file_size_smaller_than_quota__store_succeeds(self):
        User(self.uid).make_b2b('125')
        self.dao.set_quota_limits_and_paid('125', 100000, 1000, True)
        with mock.patch('mpfs.core.services.disk_service.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True):
            self.json_ok('store', {'uid': self.uid, 'path': '/disk/file.jpg', 'size': 100})


class OrganizationUserFreeSpaceTestCase(OrganizationTestCaseBase):
    def test_b2b_user__free_organization__feature_enabled__uses_personal_space(self):
        self.upload_file(self.uid, '/disk/file.jpg', file_data={'size': 100})
        User(self.uid).make_b2b('125')
        self.dao.set_quota_limits_and_paid('125', 100000, 1000, False)
        with mock.patch('mpfs.core.services.disk_service.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True):
            user_info = self.json_ok('user_info', {'uid': self.uid})
        assert user_info['space']['used'] == 100
        assert user_info['space']['free'] != 1000
        assert user_info['space']['limit'] != 1100

    def test_b2b_user__paid_organization__feature_disabled__uses_personal_space(self):
        self.upload_file(self.uid, '/disk/file.jpg', file_data={'size': 100})
        User(self.uid).make_b2b('125')
        self.dao.set_quota_limits_and_paid('125', 100000, 1000, True)
        with mock.patch('mpfs.core.services.disk_service.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', False):
            user_info = self.json_ok('user_info', {'uid': self.uid})
        assert user_info['space']['used'] == 100
        assert user_info['space']['free'] != 1000
        assert user_info['space']['limit'] != 1100

    def test_b2b_user__paid_organization__feature_enabled__uses_organization_space(self):
        self.upload_file(self.uid, '/disk/file.jpg', file_data={'size': 100})
        User(self.uid).make_b2b('125')
        self.dao.set_quota_limits_and_paid('125', 100000, 1000, True)
        with mock.patch('mpfs.core.services.disk_service.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True):
            user_info = self.json_ok('user_info', {'uid': self.uid})
        assert user_info['space']['used'] == 100
        assert user_info['space']['free'] == 1000
        assert user_info['space']['limit'] == 1100


class OrganizationSpaceRecalculationTestCase(OrganizationTestCaseBase):
    DELAY_ASYNC_TASKS = True

    def setup_method(self, method):
        super(OrganizationSpaceRecalculationTestCase, self).setup_method(method)
        self.json_ok('user_init', {'uid': self.uid_3})

    def test_b2b_users_with_same_organization_id_have_single_limit(self):
        User(self.uid).make_b2b('125')
        User(self.uid_3).make_b2b('125')
        DirectoryServiceSmartMockHelper.add_organization('125', 1000, 500, True)
        with DirectoryServiceSmartMockHelper.mock(), \
             mock.patch('mpfs.core.services.disk_service.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True), \
             mock.patch('mpfs.core.organizations.logic.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True):
            self.json_ok('organization_event', json=DirectoryServiceSmartMockHelper.get_event_organization_added('125'))
            self.upload_file(self.uid, '/disk/file_100.jpg', file_data={'size': 100})
            self.upload_file(self.uid_3, '/disk/file_50.jpg', file_data={'size': 50})
        organization = self.dao.find_by_id('125')
        assert organization.quota_limit == 1000
        assert organization.quota_free == 350

    def test_b2b_users_with_different_organization_id_have_different_limits(self):
        User(self.uid).make_b2b('125')
        User(self.uid_3).make_b2b('25')
        DirectoryServiceSmartMockHelper.add_organization('125', 1000, 500, True)
        DirectoryServiceSmartMockHelper.add_organization('25', 2000, 1500, True)
        with DirectoryServiceSmartMockHelper.mock(), \
             mock.patch('mpfs.core.services.disk_service.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True), \
             mock.patch('mpfs.core.organizations.logic.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True):
            self.json_ok('organization_event', json=DirectoryServiceSmartMockHelper.get_event_organization_added('125'))
            self.json_ok('organization_event', json=DirectoryServiceSmartMockHelper.get_event_organization_added('25'))
            self.upload_file(self.uid, '/disk/file_100.jpg', file_data={'size': 100})
            self.upload_file(self.uid_3, '/disk/file_50.jpg', file_data={'size': 50})
        organization = self.dao.find_by_id('125')
        assert organization.quota_limit == 1000
        assert organization.quota_free == 400
        organization_3 = self.dao.find_by_id('25')
        assert organization_3.quota_limit == 2000
        assert organization_3.quota_free == 1450


class ResyncOrganizationsTestCase(OrganizationTestCaseBase):
    def test_resync_organizations(self):
        """
            DirectoryServiceSmartMockHelper возвращает все организации по одной с использованием pagination.
        """
        DirectoryServiceSmartMockHelper.add_organization('125', 1000, 500, False)
        DirectoryServiceSmartMockHelper.add_organization('225', 2000, 1500, True)
        DirectoryServiceSmartMockHelper.add_organization('325', 3000, 2500, True)
        with DirectoryServiceSmartMockHelper.mock(), \
             mock.patch('mpfs.core.organizations.logic.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True):
            resync_organizations()
        organization1 = self.dao.find_by_id('125')
        assert organization1.quota_limit == 1000
        assert organization1.quota_free == 500
        assert not organization1.is_paid
        organization2 = self.dao.find_by_id('225')
        assert organization2.quota_limit == 2000
        assert organization2.quota_free == 1500
        assert organization2.is_paid
        organization3 = self.dao.find_by_id('325')
        assert organization3.quota_limit == 3000
        assert organization3.quota_free == 2500
        assert organization3.is_paid


class UserInitTestCase(OrganizationTestCaseBase):
    def test_init_with_b2b_key_updates_organization(self):
        DirectoryServiceSmartMockHelper.add_organization('125', 1000, 500, True)
        with DirectoryServiceSmartMockHelper.mock(), \
             mock.patch('mpfs.core.organizations.logic.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True):
            self.json_ok('user_init', {'uid': self.uid_3, 'b2b_key': '125'})
        organization = self.dao.find_by_id('125')
        assert organization.quota_limit == 1000
        assert organization.quota_free == 500
        assert organization.is_paid

    def test_init_with_b2b_key_with_disabled_feature_does_not_update_organization(self):
        DirectoryServiceSmartMockHelper.add_organization('125', 1000, 500, True)
        with DirectoryServiceSmartMockHelper.mock(), \
             mock.patch('mpfs.core.organizations.logic.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', False):
            self.json_ok('user_init', {'uid': self.uid_3, 'b2b_key': '125'})
            assert self.dao.find_by_id('125') is None

    def test_init_with_nonexistent_b2b_key_does_not_fail(self):
        with DirectoryServiceSmartMockHelper.mock(), \
             mock.patch('mpfs.core.organizations.logic.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True):
            self.json_ok('user_init', {'uid': self.uid_3, 'b2b_key': 'abc'})
        assert self.dao.find_by_id('abc') is None


class OrganizationUserWithPaidSpaceTestCase(OrganizationTestCaseBase, BillingTestCaseMixin):
    def test_organization_used_space_does_not_change_when_user_uses_less_than_bought(self):
        User(self.uid).make_b2b('125')
        DirectoryServiceSmartMockHelper.add_organization('125', 1000, 500, True)
        simple_create_service(Client(self.uid), Product('test_1kb_eternal'))
        with DirectoryServiceSmartMockHelper.mock(), \
                mock.patch('mpfs.core.services.disk_service.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True), \
                mock.patch('mpfs.core.organizations.logic.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True):
            self.json_ok('organization_event', json=DirectoryServiceSmartMockHelper.get_event_organization_added('125'))
            self.upload_file(self.uid, '/disk/file_50.jpg', file_data={'size': 50})
        organization = self.dao.find_by_id('125')
        assert organization.quota_limit == 1000
        assert organization.quota_free == 500

    def test_organization_used_space_decreases_by_amount_above_bought(self):
        User(self.uid).make_b2b('125')
        DirectoryServiceSmartMockHelper.add_organization('125', 1000, 500, True)
        simple_create_service(Client(self.uid), Product('test_1kb_eternal'))
        with DirectoryServiceSmartMockHelper.mock(), \
                mock.patch('mpfs.core.services.disk_service.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True), \
                mock.patch('mpfs.core.organizations.logic.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True):
            self.json_ok('organization_event', json=DirectoryServiceSmartMockHelper.get_event_organization_added('125'))
            self.upload_file(self.uid, '/disk/file_50.jpg', file_data={'size': 1024 + 50})
        organization = self.dao.find_by_id('125')
        assert organization.quota_limit == 1000
        assert organization.quota_free == 500 - 50

    def test_user_free_space(self):
        User(self.uid).make_b2b('125')
        DirectoryServiceSmartMockHelper.add_organization('125', 1000, 500, True)
        simple_create_service(Client(self.uid), Product('test_1kb_eternal'))
        with DirectoryServiceSmartMockHelper.mock(), \
                mock.patch('mpfs.core.services.disk_service.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True), \
                mock.patch('mpfs.core.organizations.logic.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True):
            self.json_ok('organization_event', json=DirectoryServiceSmartMockHelper.get_event_organization_added('125'))
            self.upload_file(self.uid, '/disk/file_50.jpg', file_data={'size': 50})
            user_info = self.json_ok('user_info', {'uid': self.uid})
        assert user_info['space']['used'] == 50
        assert user_info['space']['free'] == 1024 + 500 - 50
        assert user_info['space']['limit'] == 1024 + 500

    def test_user_free_space_with_negative_organization_free(self):
        User(self.uid).make_b2b('125')
        DirectoryServiceSmartMockHelper.add_organization('125', 1000, -500, True)
        simple_create_service(Client(self.uid), Product('test_1kb_eternal'))
        with DirectoryServiceSmartMockHelper.mock(), \
                mock.patch('mpfs.core.services.disk_service.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True), \
                mock.patch('mpfs.core.organizations.logic.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True):
            self.json_ok('organization_event', json=DirectoryServiceSmartMockHelper.get_event_organization_added('125'))
            self.upload_file(self.uid, '/disk/file_50.jpg', file_data={'size': 50})
            user_info = self.json_ok('user_info', {'uid': self.uid})
        assert user_info['space']['used'] == 50
        assert user_info['space']['free'] == 1024 - 50
        assert user_info['space']['limit'] == 1024

    def test_user_subscriptions_cancelled_on_join(self):
        with DirectoryServiceSmartMockHelper.mock(), \
                mock.patch('mpfs.core.services.disk_service.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True), \
                mock.patch('mpfs.core.organizations.logic.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True):
            self.bind_user_to_market_for_uid(self.uid)
            self.create_subscription(self.uid, 'test_1kb_for_five_seconds')

            services = ServiceList(client=Client(self.uid))
            assert {x['pid'] for x in services if x['auto']} == {'test_1kb_for_five_seconds'}

            User(self.uid).make_b2b('125')
            DirectoryServiceSmartMockHelper.add_organization('125', 1000, 500, True)
            self.json_ok('organization_event', json=DirectoryServiceSmartMockHelper.get_event_organization_added('125'))

            services = ServiceList(client=Client(self.uid))
            assert not {x['pid'] for x in services if x['auto']}

    def test_user_subscriptions_cancelled_when_organization_becomes_paid(self):
        with DirectoryServiceSmartMockHelper.mock(), \
                mock.patch('mpfs.core.services.disk_service.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True), \
                mock.patch('mpfs.core.organizations.logic.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True):
            self.bind_user_to_market_for_uid(self.uid)
            self.create_subscription(self.uid, 'test_1kb_for_five_seconds')

            services = ServiceList(client=Client(self.uid))
            assert {x['pid'] for x in services if x['auto']} == {'test_1kb_for_five_seconds'}

            User(self.uid).make_b2b('125')
            DirectoryServiceSmartMockHelper.add_organization('125', 1000, 500, False)
            self.json_ok('organization_event', json=DirectoryServiceSmartMockHelper.get_event_organization_added('125'))

            services = ServiceList(client=Client(self.uid))
            assert {x['pid'] for x in services if x['auto']} == {'test_1kb_for_five_seconds'}

            DirectoryServiceSmartMockHelper.add_organization('125', 1000, 500, True)
            self.json_ok('organization_event', json=DirectoryServiceSmartMockHelper.get_event_organization_added('125'))

            services = ServiceList(client=Client(self.uid))
            assert not {x['pid'] for x in services if x['auto']}


class UserInfoTestCase(OrganizationTestCaseBase):
    def test_b2b_user_paid_organization(self):
        User(self.uid).make_b2b('1111')
        DirectoryServiceSmartMockHelper.add_organization('1111', 1000, 500, True)
        with DirectoryServiceSmartMockHelper.mock(), \
                mock.patch('mpfs.core.organizations.logic.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True):
            resync_organizations()
        user_info = self.json_ok('user_info', {'uid': self.uid})
        assert user_info['b2b_paid'] is True

    def test_b2b_user_free_organization(self):
        User(self.uid).make_b2b('1112')
        DirectoryServiceSmartMockHelper.add_organization('1112', 1000, 500, False)
        with DirectoryServiceSmartMockHelper.mock(), \
                mock.patch('mpfs.core.organizations.logic.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True):
            resync_organizations()
        user_info = self.json_ok('user_info', {'uid': self.uid})
        assert user_info['b2b_paid'] is False

    def test_b2b_user_no_organization(self):
        User(self.uid).make_b2b('abc')
        user_info = self.json_ok('user_info', {'uid': self.uid})
        assert user_info['b2b_paid'] is False

    def test_normal_user(self):
        user_info = self.json_ok('user_info', {'uid': self.uid})
        assert 'b2b_paid' not in user_info


class ServiceListTestCase(OrganizationTestCaseBase, BillingTestCaseMixin):
    def test__free_organization__user_has_fake_service(self):
        User(self.uid).make_b2b('1111')
        DirectoryServiceSmartMockHelper.add_organization('1111', 1000, 500, False)
        with DirectoryServiceSmartMockHelper.mock(), \
                mock.patch('mpfs.core.organizations.logic.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True):
            resync_organizations()
            result = self.billing_ok('service_list', {'uid': self.uid, 'ip': '127.0.0.1'})
        assert len(result) == 2
        assert {x['name'] for x in result} == {'initial_10gb', 'b2b_10gb'}

    def test__paid_organization__user_has_fake_service(self):
        User(self.uid).make_b2b('1111')
        DirectoryServiceSmartMockHelper.add_organization('1111', 1000, 500, True)
        with DirectoryServiceSmartMockHelper.mock(), \
                mock.patch('mpfs.core.organizations.logic.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True):
            resync_organizations()
            result = self.billing_ok('service_list', {'uid': self.uid, 'ip': '127.0.0.1'})
        assert len(result) == 1
        assert {x['name'] for x in result} == {B2B_FAKE_ORGANIZATION_SPACE_SERVICE_NAME}
        assert next((x['size'] for x in result if x['name'] == B2B_FAKE_ORGANIZATION_SPACE_SERVICE_NAME), None) == 500

    def test__paid_organization_and_paid_service__user_has_fake_service(self):
        User(self.uid).make_b2b('1111')
        self.bind_user_to_market_for_uid(self.uid)
        self.create_subscription(self.uid, 'test_1kb_for_five_seconds')
        DirectoryServiceSmartMockHelper.add_organization('1111', 1000, 500, True)
        with DirectoryServiceSmartMockHelper.mock(), \
                mock.patch('mpfs.core.organizations.logic.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True):
            resync_organizations()
            result = self.billing_ok('service_list', {'uid': self.uid, 'ip': '127.0.0.1'})
        assert len(result) == 2
        assert {x['name'] for x in result} == {B2B_FAKE_ORGANIZATION_SPACE_SERVICE_NAME, 'test_1kb_for_five_seconds'}
        assert next((x['size'] for x in result if x['name'] == B2B_FAKE_ORGANIZATION_SPACE_SERVICE_NAME), None) == 500

    def test__paid_organization_and_from_db__user_does_not_have_fake_service(self):
        User(self.uid).make_b2b('1111')
        DirectoryServiceSmartMockHelper.add_organization('1111', 1000, 500, True)
        with DirectoryServiceSmartMockHelper.mock(), \
                mock.patch('mpfs.core.organizations.logic.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True):
            resync_organizations()
            result = self.billing_ok('service_list', {'uid': self.uid, 'ip': '127.0.0.1', 'from_db': 1})
        assert len(result) == 2
        assert {x['name'] for x in result} == {'initial_10gb', 'b2b_10gb'}

    def test__paid_organization_and_paid_service_and_from_db__user_does_not_have_fake_service(self):
        User(self.uid).make_b2b('1111')
        self.bind_user_to_market_for_uid(self.uid)
        self.create_subscription(self.uid, 'test_1kb_for_five_seconds')
        DirectoryServiceSmartMockHelper.add_organization('1111', 1000, 500, True)
        with DirectoryServiceSmartMockHelper.mock(), \
                mock.patch('mpfs.core.organizations.logic.B2B_SHARED_ORGANIZATION_SPACE_ENABLED', True):
            resync_organizations()
            result = self.billing_ok('service_list', {'uid': self.uid, 'ip': '127.0.0.1', 'from_db': 1})
        assert len(result) == 3
        assert {x['name'] for x in result} == {'initial_10gb', 'b2b_10gb', 'test_1kb_for_five_seconds'}
