# encoding: UTF-8

import unittest
import subprocess
import os

from hamcrest import *
from ws_properties.conversion.service import StandardConversionService
from ws_properties.environ.environment import StandardEnvironment
from ws_properties.environ.properties import DictPropertySource

from appcore.injection import inject
from dns_hosting.app_factory import create_app
from dns_hosting.services.auth.providers.debug import DebugAuthProvider


pg_started = []

def ensure_postgres_is_running():
    if pg_started:
        return

    if os.environ.get('JETBRAINS_REMOTE_RUN'):
        lines = [
            "/etc/init.d/postgresql start",
            "su -c 'yes \"internal_pwd\" | createuser internal_user -P' postgres",
            "su -c 'createdb workspace_dns -O internal_user -T template0 --lc-collate C --lc-ctype C' postgres",
            "su -c 'psql -d workspace_dns -c \"CREATE EXTENSION IF NOT EXISTS btree_gist WITH SCHEMA public;\"' postgres",
            "/opt/dns-hosting/bin/dnscli pgmigrate migrate -t latest",
        ]
        for line in lines:
            print "Executing {}".format(line)
            subprocess.check_call(line, shell = True)
        print "POSTGRES STARTED"

    pg_started.append(True)


class BaseAppTestCase(unittest.TestCase):
    app_profiles = ['unittest']
    test_properties = {}

    def setUp(self):
        environment = StandardEnvironment()
        environment.conversion_service = StandardConversionService()
        environment.profiles = self.app_profiles
        environment.activate_profiles()

        test_property_source = DictPropertySource(self.test_properties)
        environment.property_sources.insert(0, test_property_source)

        app = create_app(environment)

        self.environment = environment
        self.app = app

        ensure_postgres_is_running()

    def auth_without_scopes(self, scopes):
        inject('security_manager', self.app).auth_providers = [DebugAuthProvider(scopes)]


class AppTestCase(BaseAppTestCase):
    app_profiles = [
                       'db-master',
                       'db-slave',
                       'db-pdd',
                   ] + BaseAppTestCase.app_profiles

    def test_environment_injected(self):
        assert_that(
            calling(inject).with_args('environment', self.app),
            not_(raises(Exception))
        )

    def test_metadata_injected(self):
        assert_that(
            calling(inject).with_args('metadata', self.app),
            not_(raises(Exception))
        )

    def test_models_injected(self):
        assert_that(
            calling(inject).with_args('models', self.app),
            not_(raises(Exception))
        )

    def test_master_engine_injected(self):
        assert_that(
            calling(inject).with_args('master_engine', self.app),
            not_(raises(Exception))
        )

    def test_slave_engine_injected(self):
        assert_that(
            calling(inject).with_args('slave_engine', self.app),
            not_(raises(Exception))
        )

    def test_pdd_engine_injected(self):
        assert_that(
            calling(inject).with_args('pdd_engine', self.app),
            not_(raises(Exception))
        )

    def test_master_session_factory_injected(self):
        assert_that(
            calling(inject).with_args('master_session_factory', self.app),
            not_(raises(Exception))
        )

    def test_slave_session_factory_injected(self):
        assert_that(
            calling(inject).with_args('slave_session_factory', self.app),
            not_(raises(Exception))
        )

    def test_session_factory_injected(self):
        assert_that(
            calling(inject).with_args('session_factory', self.app),
            not_(raises(Exception))
        )

    def test_domain_repository_injected(self):
        assert_that(
            calling(inject).with_args('domain_repository', self.app),
            not_(raises(Exception))
        )

    def test_record_repository_injected(self):
        assert_that(
            calling(inject).with_args('record_repository', self.app),
            not_(raises(Exception))
        )

    def test_changelog_repository_injected(self):
        assert_that(
            calling(inject).with_args('changelog_repository', self.app),
            not_(raises(Exception))
        )
