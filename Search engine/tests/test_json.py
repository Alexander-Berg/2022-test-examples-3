# -*- coding: utf-8 -*-
from rtcc.utils.json import convert_unicode_to_str

JSON = [
    {
        "stands": [
            {
                u"type": u"balancer",
                u"config": {
                    u"parent": {
                        u"contour": u"production",
                        u"location": u"msk",
                        u"name": u"balancer_rkub"
                    },
                    u"patch": {
                        u"search_backends": {
                            u"sas": [
                                u"SAS_WEB_NMETA"
                            ],
                            u"msk": [
                                u"MSK_WEB_NMETA(domain=.search.yandex.net,resolve=6)"
                            ],
                            u"man": [
                                u"MAN_WEB_NMETA"
                            ]
                        }
                    }
                },
                u"name": u"balancer_rkub",
                u"location": u"msk"
            },
            {
                u"type": u"balancer",
                u"config": {
                    u"parent": {
                        u"contour": u"production",
                        u"location": u"sas",
                        u"name": u"balancer_com"
                    },
                    u"patch": {
                        u"search_backends": {
                            u"sas": [
                                u"SAS_WEB_NMETA(domain=.search.yandex.net,resolve=6)"
                            ],
                            u"man": [
                                u"MAN_WEB_NMETA"
                            ],
                            u"msk": [
                                u"MSK_WEB_NMETA(domain=.search.yandex.net,resolve=6)"
                            ],
                            u"pumpkin": [
                                u"MSK_WEB_RUS_YALITE(domain=.search.yandex.net,resolve=6)",
                                u"SAS_WEB_RUS_YALITE(domain=.search.yandex.net,resolve=6)",
                                u"MAN_WEB_RUS_YALITE"
                            ]
                        }
                    }
                },
                u"name": u"balancer_com",
                u"location": u"sas"
            }
        ],
        u"name": u"noapache"
    },
    {
        "stands": [
            {
                "type": "balancer",
                "config": {
                    "parent": {
                        "contour": "hamster",
                        "location": "msk",
                        "name": "balancer_rkub"
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
                "name": "balancer_rkub",
                "location": "msk"
            },
            {
                "type": "balancer",
                "config": {
                    "parent": {
                        "contour": "hamster",
                        "location": "sas",
                        "name": "balancer_com"
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
                "name": "balancer_com",
                "location": "sas"
            }
        ],
        "name": "kitty"
    }
]


def test_json():
    json = convert_unicode_to_str(JSON)
    assert isinstance(json[0]["stands"][0]["type"], str)
