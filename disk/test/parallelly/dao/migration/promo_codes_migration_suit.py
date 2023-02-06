# -*- coding: utf-8 -*-
import pytest

from mpfs.common.static.tags.promo_codes import ACTIVATED
from mpfs.core.promo_codes.dao.migrator import PromoCodeMigration, PromoCodeArchiveMigration
from mpfs.core.promo_codes.logic.promo_code import PromoCode
from test.conftest import COMMON_DB_IN_POSTGRES
from test.parallelly.dao.base import BaseMigrationTestCase

from test.parallelly.promo_codes import PromoCodeMethodsMixin

pytestmark = pytest.mark.skipif(COMMON_DB_IN_POSTGRES, reason='Tests use migration from mongo to postgres explicitly')


class PromoCodeMigrationTestCase(PromoCodeMethodsMixin, BaseMigrationTestCase):
    migrator = PromoCodeMigration()

    def test_forward_migration(self):
        self._generate_promo_code(pid='FORWARD_MIGRATION')
        assert self.migrator.run()
        assert self.migrator.dao.get_pg_impl().count() > 0


class PromoCodeArchiveMigrationTestCase(PromoCodeMethodsMixin, BaseMigrationTestCase):
    migrator = PromoCodeArchiveMigration()

    def test_forward_migration(self):
        promo_code = PromoCode.get_promo_code(self._generate_promo_code(pid='FORWARD_MIGRATION'))
        promo_code.add_to_archive(uid=self.uid, status_to_set=ACTIVATED, linked_sid='test')
        assert self.migrator.run()
        assert self.migrator.dao.get_pg_impl().count() > 0
