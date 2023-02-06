# -*- coding: utf-8 -*-
import pytest

from rtcc.core.session import Session
from rtcc.dataprovider.dnsprovider import DnsDataProvider
from rtcc.dataprovider.hwdata import HWData
from rtcc.dataprovider.topology import TopologyProvider
from rtcc.model.raw import EndpointRaw
from rtcc.model.raw import Service
from rtcc.view.endpoint import EndpointTemplateView
from rtcc.view.endpoint import EndpointView

SESSION = Session()
TOPOLOGY = SESSION.register(TopologyProvider())
DNSDATAPROVIDER = SESSION.register(DnsDataProvider())
HWDATA = SESSION.register(HWData())


@pytest.mark.skip(reason="too fragile test")
def test_endpoint_none():
    srv = Service(type="resolver", expression="C@ONLINE . I@SAS_ENTITYSEARCH", args=[])
    ep = EndpointRaw(service=srv, path="search", schema="http", grouping="none")
    assert EndpointView(TOPOLOGY, HWDATA, DNSDATAPROVIDER).view(ep)


@pytest.mark.long
def test_endpoint_location_simple():
    srv = Service(type="resolver", expression="C@ONLINE . I@SAS_ENTITYSEARCH", args=[])
    ep = EndpointRaw(service=srv, path="yandsearch", schema="http", grouping="locations")
    assert EndpointView(TOPOLOGY, HWDATA, DNSDATAPROVIDER).view(ep)


@pytest.mark.long
def test_endpoint_localhost_first():
    srv = Service(type="resolver", expression="C@ONLINE . I@SAS_ENTITYSEARCH", args=[])
    ep = EndpointRaw(service=srv, path="yandsearch", schema="http", grouping="localfirst")
    assert EndpointView(TOPOLOGY, HWDATA, DNSDATAPROVIDER).view(ep)


@pytest.mark.long
def test_endpoint_template():
    srv = Service(type="resolver", expression="C@ONLINE . I@SAS_ENTITYSEARCH", args=[])
    ep = EndpointRaw(service=srv, path="yandsearch", schema="http", grouping="localfirst")
    assert EndpointTemplateView().view(ep)


@pytest.mark.long
def test_endpoint_second_attempt():
    srv = Service(type="resolver", expression="C@ONLINE . I@SAS_ENTITYSEARCH", args=[])
    ep = EndpointRaw(service=srv, path="yandsearch", schema="http", grouping="locations_se")
    assert EndpointView(TOPOLOGY, HWDATA, DNSDATAPROVIDER).view(ep)
