from __future__ import unicode_literals, absolute_import

import pytest
from market.sre.tools.rtc.nanny.models.attributes.auth_attrs import AuthAttrs
from market.sre.tools.rtc.nanny.models.attributes.info_attrs import InfoAttrs
from market.sre.tools.rtc.nanny.models.attributes.runtime_attrs import RuntimeAttrs
from market.sre.tools.rtc.nanny.models.attributes.current_state import CurrentState
from market.sre.tools.rtc.nanny.models.service import Service
from market.sre.tools.rtc.nanny.utils import diff


class TestDiff:
    def test_compare_the_same_services(self):
        s1 = Service(
            id="test_service1",
            info_attrs=InfoAttrs(snapshot_id="123", content=dict(k1="k1")),
            runtime_attrs=RuntimeAttrs(snapshot_id="234", content=dict(k2="k2")),
            auth_attrs=AuthAttrs(snapshot_id="567", content=dict(k3="k3")),
            current_state=CurrentState(snapshot_id='890', content=dict(k4="k4")),
        )
        assert diff(s1, s1) == {}

    def test_compare_different_services(self):
        s1 = Service(
            id="test_service1",
            info_attrs=InfoAttrs(snapshot_id="123", content=dict(k1="k1")),
            runtime_attrs=RuntimeAttrs(snapshot_id="234", content=dict(k2="k2")),
            auth_attrs=AuthAttrs(snapshot_id="890", content=dict(k3="k3")),
            current_state=CurrentState(snapshot_id='890', content=dict(k4="k4")),
        )
        s2 = Service(
            id="test_service2",
            info_attrs=InfoAttrs(snapshot_id="123", content=dict(k1="k2")),
            runtime_attrs=RuntimeAttrs(
                snapshot_id="234", content=dict(k1="k1", k2="k2")
            ),
            auth_attrs=AuthAttrs(snapshot_id="890", content=dict(k3="k3")),
            current_state=CurrentState(snapshot_id='890', content=dict(k4="k4"))
        )
        assert diff(s1, s2) != {}

    def test_compare_objects_wo_method(self):
        class A:
            pass

        with pytest.raises(AttributeError):
            diff(A(), A())
