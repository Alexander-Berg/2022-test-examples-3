# -*- coding: utf-8 -*-
from extensions.configurations.sfront import view as viewer
from extensions.configurations.sfront.model import SimpleSources, ExtendedSources
from rtcc.controllers import requests
from rtcc.model.raw import EndpointRaw
from rtcc.model.raw import Service


def test_get_extended_host_group():
    hg = requests._create_extended_host_group(
            "Cms(\"C@UPPER . I@MSK_WIZARD_WIZARD_HAMSTER\", groupping=\"localfirst\")")
    assert hg.expression == "C@UPPER . I@MSK_WIZARD_WIZARD_HAMSTER"
    assert hg.path == ""
    assert hg.groupping == "localfirst"
    assert hg.type == "cms"


def test_get_simple_host_group():
    hg = requests._create_simple_host_group(
            "hamster.yandex.ru", 80)
    assert hg.expression == "hamster.yandex.ru"
    assert hg.port == 80
    assert hg.path == ""
    assert hg.groupping == "none"
    assert hg.type == "user"


def test_get_source_simple():
    s = requests._create_source("TEST_SOURCE", ["hamster.yandex.test.ru", 8080, 15000])
    assert s.name == "TEST_SOURCE"
    assert isinstance(s, SimpleSources)
    assert s.timeout == 15000
    assert s.host_groups.expression == "hamster.yandex.test.ru"


def test_get_source_extended():
    s = requests._create_source("TEST_SOURCE",
                                {
                                    'HostSelectionPolicy': 'hhh',
                                    'Delays': '0 70ms',
                                    'TimeoutMilliseconds': 1000,
                                    'Hosts': 'Resolver("C@UPPER . I@MSK_WIZARD_WIZARD_HAMSTER", groupping="localfirst")',
                                    'RequestsCount': 3,
                                    'ReconnectTimeoutMilliseconds': 70})
    assert s.name == "TEST_SOURCE"
    assert isinstance(s, ExtendedSources)
    assert s.timeout == 1000
    assert s.host_selection_policy == "hhh"
    assert s.delays == "0 70ms"
    assert s.requests_count == 3
    assert s.host_groups.expression == "C@UPPER . I@MSK_WIZARD_WIZARD_HAMSTER"
    assert s.host_groups.type == "resolver"
    assert s.host_groups.path == ""
    assert s.host_groups.groupping == "localfirst"


def test_get_source_extended_localfirst():
    s = requests._create_source("TEST_SOURCE",
                                {
                                    'HostSelectionPolicy': 'hhh',
                                    'Delays': '0 70ms',
                                    'TimeoutMilliseconds': 1000,
                                    'Hosts': 'User("hamster.yandex.ru", 8080, groupping="localfirst")',
                                    'RequestsCount': 3,
                                    'ReconnectTimeoutMilliseconds': 70})
    assert s.name == "TEST_SOURCE"
    assert isinstance(s, ExtendedSources)
    assert s.timeout == 1000
    assert s.host_selection_policy == "hhh"
    assert s.delays == "0 70ms"
    assert s.requests_count == 3
    assert s.host_groups.expression == "hamster.yandex.ru"


def test_read_parse_host_group():
    EXAMPLE = 'User("hamster.yandex.ru", 8081, groupping="localfirst")'
    endpoint_raw = EndpointRaw(service=Service(type="user", expression="hamster.yandex.ru", args=("8081",)),
                               path="",
                               grouping="localfirst",
                               schema="",
                               )
    assert EXAMPLE == viewer.SourceRequestView("TEST_SOURCE")._view_extended_endpoint(endpoint_raw)

    hg_parsed = requests._create_extended_host_group(EXAMPLE)
    assert "hamster.yandex.ru" == hg_parsed.expression
    assert "" == hg_parsed.path
    assert 8081 == hg_parsed.port
    assert "user" == hg_parsed.type
    assert "localfirst" == hg_parsed.groupping
