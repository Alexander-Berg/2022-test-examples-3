# -*- coding: utf-8 -*-
import datetime
import time

import pytest

from mpfs.common.static.tags.billing import PRIMARY_2018_DISCOUNT_10
from mpfs.core.office.dao.migrator import OfficeAllowedPddDomainsMigration
from mpfs.core.office.dao.office_allowed_pdd_domains import OfficeAllowedPddDomainsDAOItem, OfficeAllowedPddDomainsDAO
from mpfs.core.promo_codes.dao.migrator import DiscountTemplateMigration, DiscountArchiveMigration
from mpfs.core.promo_codes.logic.discount import DiscountTemplate, DiscountArchive, Discount
from test.conftest import COMMON_DB_IN_POSTGRES
from test.parallelly.dao.base import BaseMigrationTestCase

pytestmark = pytest.mark.skipif(COMMON_DB_IN_POSTGRES, reason='Tests use migration from mongo to postgres explicitly')


class OfficeAllowedPddDomainsMigrationTestCase(BaseMigrationTestCase):
    migrator = OfficeAllowedPddDomainsMigration()

    def test_forward_migration(self):
        item = self.migrator.dao.dao_item_cls.create_from_mongo_dict({"_id": "example.com"})
        self.migrator.dao.get_mongo_impl().insert(item.get_mongo_representation())
        assert self.migrator.run()
        assert self.migrator.dao.get_pg_impl().count() == 1
