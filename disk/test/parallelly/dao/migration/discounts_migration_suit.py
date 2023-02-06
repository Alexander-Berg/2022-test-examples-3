# -*- coding: utf-8 -*-
import datetime
import time

import pytest

from mpfs.common.static.tags.billing import PRIMARY_2018_DISCOUNT_10
from mpfs.core.promo_codes.dao.migrator import DiscountTemplateMigration, DiscountArchiveMigration
from mpfs.core.promo_codes.logic.discount import DiscountTemplate, DiscountArchive, Discount
from test.conftest import COMMON_DB_IN_POSTGRES
from test.parallelly.dao.base import BaseMigrationTestCase

pytestmark = pytest.mark.skipif(COMMON_DB_IN_POSTGRES, reason='Tests use migration from mongo to postgres explicitly')


class DiscountTemplateMigrationTestCase(BaseMigrationTestCase):
    migrator = DiscountTemplateMigration()

    def test_forward_migration(self):
        discount_template = DiscountTemplate.create(PRIMARY_2018_DISCOUNT_10, "test", False,
                                                    period_timedelta=datetime.timedelta(days=1),
                                                    end_datetime=None)
        discount_template.save()
        assert self.migrator.run()
        assert self.migrator.dao.get_pg_impl().count() > 0


class DiscountArchiveMigrationTestCase(BaseMigrationTestCase):
    migrator = DiscountArchiveMigration()

    def test_forward_migration(self):
        discount_template = DiscountTemplate.create(PRIMARY_2018_DISCOUNT_10, "test", False,
                                                    period_timedelta=datetime.timedelta(days=1),
                                                    end_datetime=None)
        discount_template.save()
        discount = Discount.create(self.uid, time.time(), discount_template)
        discount_archive = DiscountArchive.create(self.uid, discount)
        discount_archive.save()
        assert self.migrator.run()
        assert self.migrator.dao.get_pg_impl().count() > 0
