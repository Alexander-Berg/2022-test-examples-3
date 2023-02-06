from __future__ import unicode_literals, absolute_import

from market.sre.tools.rtc.nanny.models.dashboard import Dashboard


class TestDashboard(object):
    def test_find_group(self, dashboard):
        """
        :type dashboard: market.sre.tools.rtc.nanny.models.Dashboard
        """
        group = dashboard.find_group("testing")
        assert group
        assert group["id"] == "testing"

    def test_add_service_new(self, dashboard):
        """
        :type dashboard: market.sre.tools.rtc.nanny.models.Dashboard
        """
        service = "testing_market_test_service_iva"
        group = "testing"
        dashboard.add_service(service_id=service, group=group)
        group_testing = dashboard.find_group(group)
        assert any(s for s in group_testing["services"] if s["service_id"] == service)

    def test_add_service_exists(self, dashboard):
        """
        :type dashboard: market.sre.tools.rtc.nanny.models.Dashboard
        """
        service = "testing_market_test_service_vla"
        group = "testing"
        dashboard.add_service(service_id=service, group=group)
        group_testing = dashboard.find_group(group)
        assert (
            len([s for s in group_testing["services"] if s["service_id"] == service])
            == 1
        )

    def test_add_service_new_group(self, dashboard):
        """
        :type dashboard: market.sre.tools.rtc.nanny.models.Dashboard
        """
        service = "testing_market_test_service_vla"
        group = "testing2"
        dashboard.add_service(service_id=service, group=group)
        group_testing2 = dashboard.find_group(group)
        assert group_testing2
        assert any(s for s in group_testing2["services"] if s["service_id"] == service)

    def test_create_dashboard(self):
        dashboard = Dashboard(id="test_dashboard", content=dict())
        dashboard.allow_updating_auto_scheduled_services = True
        dashboard.owners.append(logins="test_login", groups="123")
        dashboard.add_service(
            service_id="testing_market_test_service_sas", group="testing"
        )
        dashboard.add_service(
            service_id="testing_market_test_service_vla", group="testing"
        )
        dashboard.add_service(
            service_id="production_market_test_service_vla", group="production"
        )
        assert dashboard.find_group("testing")
