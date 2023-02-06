from __future__ import unicode_literals, absolute_import

from market.sre.tools.rtc.nanny.models.attributes.auth_attrs import AuthAttrs
from market.sre.tools.rtc.nanny.models.attributes.info_attrs import InfoAttrs
from market.sre.tools.rtc.nanny.models.attributes.runtime_attrs import RuntimeAttrs
from market.sre.tools.rtc.nanny.models.service import Service
from market.sre.tools.rtc.nanny.scenarios.copy_service_properties import (
    copy_service_properties,
)


class TestCopyServiceProperties:
    def test_copying(self):
        s1 = Service(
            id="testing_market_app1_vla",
            info_attrs=InfoAttrs(snapshot_id="123", content=dict(k1="k1")),
            runtime_attrs=RuntimeAttrs(snapshot_id="234", content=dict(k2="k2")),
            auth_attrs=AuthAttrs(snapshot_id="890", content=dict(k3="k3")),
        )
        s2 = Service(
            id="production_market_app1_vla",
            info_attrs=InfoAttrs(snapshot_id="123", content=dict(k1="k2")),
            runtime_attrs=RuntimeAttrs(
                snapshot_id="234", content=dict(k1="k1", k2="k2")
            ),
            auth_attrs=AuthAttrs(snapshot_id="890", content=dict(k3="k3")),
        )

        class FakeManager:
            def get_service(self, name):
                return {
                    "testing_market_app1_vla": s1,
                    "production_market_app1_sas": s2,
                }[name]

            def update_service(self, service):
                pass

        copy_service_properties(
            FakeManager(),
            source_service_name="testing_market_app1_vla",
            target_service_names=["production_market_app1_sas"],
            properties=["info_attrs.content", "runtime_attrs.content"],
        )

        assert s1.info_attrs.content == s2.info_attrs.content
        assert s1.runtime_attrs.content == s2.runtime_attrs.content
