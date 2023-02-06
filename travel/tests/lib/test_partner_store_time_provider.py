from travel.avia.ticket_daemon.ticket_daemon.lib.partner_store_time_provider import PartnerStoreTimeProvider

from travel.avia.library.python.tester.factories import create_partner, create_dohop_vendor
from travel.avia.library.python.tester.testcase import TestCase


class TestPartnerStoreTimeProvider(TestCase):
    def setUp(self):
        self._provider = PartnerStoreTimeProvider()

    def test_partner_store_time(self):
        partner = create_partner(variant_cache_ttl=30)

        assert self._provider.get_status_time(partner, 0) == 30 * 60
        assert self._provider.get_result_time(partner, 0) == 35 * 60

    def test_partner_store_custom_time(self):
        partner = create_partner(variant_cache_ttl=30)

        assert self._provider.get_status_time(partner, 100 * 60) == 100 * 60
        assert self._provider.get_result_time(partner, 100 * 60) == 105 * 60

    def test_partner_so_low_store_time(self):
        partner = create_partner(variant_cache_ttl=1)

        assert self._provider.get_status_time(partner, 1 * 60) == 5 * 60
        assert self._provider.get_result_time(partner, 1 * 60) == 10 * 60

    def test_dohop_vendor_store_time(self):
        dohop = create_partner(code='dohop')
        dohop_vendor = create_dohop_vendor(dohop_id=dohop.id, dohop_cache_ttl=30)

        assert self._provider.get_status_time(dohop_vendor, 0) == 30 * 60
        assert self._provider.get_result_time(dohop_vendor, 0) == 35 * 60

    def test_dohop_store_custom_time(self):
        dohop = create_partner(code='dohop')
        dohop_vendor = create_dohop_vendor(dohop_id=dohop.id, dohop_cache_ttl=30)

        assert self._provider.get_status_time(dohop_vendor, 100 * 60) == 100 * 60
        assert self._provider.get_result_time(dohop_vendor, 100 * 60) == 105 * 60

    def test_dohop_vendor_so_low_store_time(self):
        dohop = create_partner(code='dohop')
        dohop_vendor = create_dohop_vendor(dohop_id=dohop.id, dohop_cache_ttl=1)

        assert self._provider.get_status_time(dohop_vendor, 1 * 60) == 5 * 60
        assert self._provider.get_result_time(dohop_vendor, 1 * 60) == 10 * 60
