# -*- coding: utf-8 -*-
from collections import OrderedDict

import pytest

from rtcc.core.dataobjects import ConfigurationInfo
from rtcc.core.discoverer import Discoverer
from rtcc.core.session import Session

KITTY_BALANCER_PATCH = {
    "internal_iplist": [],
    "images_backend": OrderedDict({
        "sas": [
            "ALL_IMGS_NOAPACHE_PRIEMKA_STABLE(domain=.search.yandex.net,resolve=6)"
        ],
        "vla": [
            "ALL_IMGS_NOAPACHE_PRIEMKA_STABLE(domain=.search.yandex.net,resolve=6)"
        ],
        "man": [
            "ALL_IMGS_NOAPACHE_PRIEMKA_STABLE(domain=.search.yandex.net,resolve=6)"
        ]
    }),
    "molly_enabled": True,
    "rpcrewrite_enabled": True,
    "main_iplist": [
        "2a02:6b8:0:3400::eeee:2"
    ],
    "familysearch_iplist": [],
    "search_backends": OrderedDict({
        "sas": [
            "ALL_WEB_NMETA_PRIEMKA_KITTY(domain=.search.yandex.net,resolve=6)"
        ],
        "vla": [
            "ALL_WEB_NMETA_PRIEMKA_KITTY(domain=.search.yandex.net,resolve=6)"
        ],
        "man": [
            "ALL_WEB_NMETA_PRIEMKA_KITTY(domain=.search.yandex.net,resolve=6)"
        ]
    }),
    "touch_searchapp": OrderedDict({
        "sas": [
            "ALL_WEB_MOBILE_PRIEMKA_KITTY(domain=.search.yandex.net,resolve=6)"
        ],
        "vla": [
            "ALL_WEB_MOBILE_PRIEMKA_KITTY(domain=.search.yandex.net,resolve=6)"
        ],
        "man": [
            "ALL_WEB_MOBILE_PRIEMKA_KITTY(domain=.search.yandex.net,resolve=6)"
        ]
    }),
    "https_instance_port": 22011,
    "http_instance_port": 22010,
    "instance_iplist": []
}

SESSION = Session()


def test_discoverer():
    assert Discoverer().config_types


@pytest.mark.long
def test_balancer_generate():
    generator = Discoverer().get("balancer")
    view = generator.view_config(ConfigurationInfo(None, None, KITTY_BALANCER_PATCH), "complete")
    assert view


@pytest.mark.long
def test_sfront_generate():
    from rtcc.core.common import ConfigurationID
    conf_id = ConfigurationID.parse_stand_name("upper_web_rkub", "vla", "production", "sfront")
    generator = Discoverer().get("sfront")
    view = generator.view_config(ConfigurationInfo(conf_id, None, None), "complete")
    assert view


def test_sfront_generate_template():
    from rtcc.core.common import ConfigurationID
    conf_id = ConfigurationID.parse_stand_name("upper_web_rkub", "vla", "production", "sfront")
    generator = Discoverer().get("sfront")
    view = generator.view_config(ConfigurationInfo(conf_id, None, None), "template")
    assert view


@pytest.mark.long
def test_noapache_generate():
    from rtcc.core.common import ConfigurationID
    conf_id = ConfigurationID.parse_stand_name("upper_web_rkub", "vla", "production", "noapache")
    generator = Discoverer().get("noapache")
    view = generator.view_config(ConfigurationInfo(conf_id, None, None), "complete")
    assert view


def test_noapache_generate_template():
    from rtcc.core.common import ConfigurationID
    conf_id = ConfigurationID.parse_stand_name("upper_web_rkub", "vla", "production", "noapache")
    generator = Discoverer().get("noapache")
    view = generator.view_config(ConfigurationInfo(conf_id, None, None), "template")
    assert view
