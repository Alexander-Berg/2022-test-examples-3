# -*- coding: utf-8 -*-
from rtcc.core.common import ConfigurationID
from rtcc.dataprovider.patchfile import CONTOUR_LIST_LOCATOR
from rtcc.dataprovider.patchfile import PatchFileProvider

PATCH = [
    {
        "stands": [
            {
                "type": "balancer",
                "config": {
                    "parent": {
                        "contour": "production",
                        "location": "msk",
                        "stand_name": "balancer_rkub"
                    },
                    "patch": {
                        "search_backends": {
                            "sas": [
                                "SAS_WEB_NMETA"
                            ],
                            "msk": [
                                "MSK_WEB_NMETA(domain=.search.yandex.net,resolve=6)"
                            ],
                            "man": [
                                "MAN_WEB_NMETA"
                            ]
                        }
                    }
                },
                "stand_name": "balancer_rkub",
                "location": "msk"
            },
            {
                "type": "balancer",
                "config": {
                    "parent": {
                        "contour": "production",
                        "location": "sas",
                        "stand_name": "balancer_com"
                    },
                    "patch": {
                        "search_backends": {
                            "sas": [
                                "SAS_WEB_NMETA(domain=.search.yandex.net,resolve=6)"
                            ],
                            "man": [
                                "MAN_WEB_NMETA"
                            ],
                            "msk": [
                                "MSK_WEB_NMETA(domain=.search.yandex.net,resolve=6)"
                            ],
                            "pumpkin": [
                                "MSK_WEB_RUS_YALITE(domain=.search.yandex.net,resolve=6)",
                                "SAS_WEB_RUS_YALITE(domain=.search.yandex.net,resolve=6)",
                                "MAN_WEB_RUS_YALITE"
                            ]
                        }
                    }
                },
                "stand_name": "balancer_com",
                "location": "sas"
            }
        ],
        "name": "noapache"
    },
    {
        "stands": [
            {
                "type": "balancer",
                "config": {
                    "parent": {
                        "contour": "hamster",
                        "location": "msk",
                        "stand_name": "balancer_rkub"
                    },
                    "patch": {
                        "search_backends": {
                            "sas": [
                                "SAS_WEB_NMETA"
                            ],
                            "msk": [
                                "MSK_WEB_NMETA(domain=.search.yandex.net,resolve=6)"
                            ],
                            "man": [
                                "MAN_WEB_NMETA"
                            ]
                        }
                    }
                },
                "stand_name": "balancer_rkub",
                "location": "msk"
            },
            {
                "type": "balancer",
                "config": {
                    "parent": {
                        "contour": "hamster",
                        "location": "sas",
                        "stand_name": "balancer_com"
                    },
                    "patch": {
                        "search_backends": {
                            "sas": [
                                "SAS_WEB_NMETA(domain=.search.yandex.net,resolve=6)"
                            ],
                            "man": [
                                "MAN_WEB_NMETA"
                            ],
                            "msk": [
                                "MSK_WEB_NMETA(domain=.search.yandex.net,resolve=6)"
                            ],
                            "pumpkin": [
                                "MSK_WEB_RUS_YALITE(domain=.search.yandex.net,resolve=6)",
                                "SAS_WEB_RUS_YALITE(domain=.search.yandex.net,resolve=6)",
                                "MAN_WEB_RUS_YALITE"
                            ]
                        }
                    }
                },
                "stand_name": "balancer_com",
                "location": "sas"
            }
        ],
        "name": "kitty"
    }
]

CONFIG_ID = ConfigurationID.parse("MSK_RKUB_KITTY", "balancer", "balancer")


def test_patchfile():
    provider = PatchFileProvider(PATCH)
    assert len(provider.get(js_path=CONTOUR_LIST_LOCATOR)) == 2


def test_patchfile_get_configurations():
    provider = PatchFileProvider(PATCH)
    assert len(provider.get_configurations_ids("balancer")) == 4


def test_patchfile_get_parent():
    provider = PatchFileProvider(PATCH)
    assert provider.get_parent_id(CONFIG_ID).to_json() == {"contour": "hamster",
                                                 "stand": "balancer_rkub",
                                                 "location": "msk"}


def test_patchfile_get_patch():
    provider = PatchFileProvider(PATCH)
    assert provider.get_patch(CONFIG_ID) is not None
