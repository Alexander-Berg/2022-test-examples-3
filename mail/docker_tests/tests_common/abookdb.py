import bson
import copy
import lz4.block
import msgpack
import logging

log = logging.getLogger(__file__)


def add_user(conn, user):
    user_data = make_user_data(uid=user.uid, suid=user.suid)
    collection = conn['developer_moko4']['addressbooks']
    collection.insert_one(user_data)


def make_user_data(uid, suid):
    data = copy.deepcopy(USER_DATA_TEMPLATE)
    data[u'_id'] = unicode(uid)
    data[u'suid'] = unicode(suid)
    data[u'_compressed'][u'data'] = pack(data[u'_compressed'][u'data'])
    data[u'_compressed'][u'indx'] = pack(data[u'_compressed'][u'indx'])
    return data


def pack(value):
    return bson.Binary(lz4.block.compress(msgpack.dumps(value)))


USER_DATA_TEMPLATE = {
    u'_id': None,
    u'suid': None,
    u'srcs_maxid': 2,
    u'_ts': 1517319037,
    u'rev': 14,
    u'_ct': 1391165045,
    u'__bck_t': 1511190670,
    u's_collect': u'',
    u'maxcid': 4,
    u'_ver': u'4',
    u'_c': 4,
    u'_compressed': {
        u'fmt': u'msgpack.lz4',
        u'data': {
            u'srcs': [
                {
                    u't': u'ya_sent',
                    u'id': 1,
                    u'_ct': 1391165045
                }, {
                    u'id': 2,
                    u't': u'ya_byhand',
                    u'_ct': 1511190665
                }
            ],
            u'roster': {
                1: {
                    u'raws': [{
                        u'src_id': 1,
                        u'_ct': 1391165045,
                        u'id': 1
                    }],
                    u'raws_maxid': 1,
                    u'h_ph': False,
                    u'tags': [1],
                    u'_ct': 1391165045,
                    u'_ut': 1510444800,
                    u'_r': 10,
                    u'h_e': True,
                    u'data_maxid': 1,
                    u'_utto': 1510444800,
                    u'_tc': 7,
                    u'data': [
                        {
                            u'e': u'dlafjekfw@yandex.ru',
                            u'_ct': 1391165045,
                            u'_ut': 1510444800,
                            u'dt': u'e',
                            u'rc': 1,
                            u'tg': [1],
                            u'_utto': 1510444800,
                            u'_tc': 7,
                            u'id': 1,
                            u'_tcto': 7
                        }
                    ],
                    u'id': 1,
                    u'_tcto': 7
                },
                2: {
                    u'raws': [{
                        u'src_id': 1,
                        u'_ct': 1391165280,
                        u'id': 1
                    }],
                    u'h_ph': False,
                    u'_tc': 1,
                    u'tags': [1],
                    u'_ct': 1391165280,
                    u'_ut': 1391165280,
                    u'h_e': True,
                    u'_tcto': 1,
                    u'id': 2,
                    u'_utto': 1391165280,
                    u'raws_maxid': 1,
                    u'data': [
                        {
                            u'e': u'mailproto-bugs@yandex-team.ru',
                            u'_ct': 1391165280,
                            u'_ut': 1391165280,
                            u'rc': 1,
                            u'id': 1,
                            u'tg': [1],
                            u'_utto': 1391165280,
                            u'_tc': 1,
                            u'dt': u'e',
                            u'_tcto': 1
                        }
                    ],
                    u'_r': 8,
                    u'data_maxid': 1
                },
                3: {
                    u'raws': [{
                        u'src_id': 2,
                        u'_ct': 1511190665,
                        u'id': 1
                    }],
                    u'_ut': 1511190665,
                    u'dnid': 3,
                    u'_r': 11,
                    u'data_maxid': 3,
                    u'raws_maxid': 1,
                    u'data': [
                        {
                            u'dt': u'e',
                            u'rc': 1,
                            u'e': u'foo@bar.baz',
                            u'id': 1,
                            u'_ct': 1511190665
                        }, {
                            u'neo': True,
                            u'_ct': 1511190665,
                            u'p': u'1342',
                            u'rc': 1,
                            u'dt': u'p',
                            u'id': 2
                        }, {
                            u'f': u'foo',
                            u'm': u'bar',
                            u'l': u'baz',
                            u'_ct': 1511190665,
                            u'rc': 1,
                            u'dt': u'sn',
                            u'id': 3
                        }
                    ],
                    u'id': 3,
                    u'_ct': 1511190665
                },
                4: {
                    u'raws': [{
                        u'src_id': 2,
                        u'_ct': 1517318977,
                        u'id': 1
                    }],
                    u'_ut': 1517318977,
                    u'dnid': 3,
                    u'_r': 13,
                    u'data_maxid': 6,
                    u'raws_maxid': 1,
                    u'data': [
                        {
                            u'dt': u'e',
                            u'e': u'soqkweno@yandex.ru',
                            u'id': 1,
                            u'_ct': 1517318977,
                            u'rc': 1
                        }, {
                            u'p': u'+99999999999',
                            u'dt': u'p',
                            u'rc': 1,
                            u'id': 2,
                            u'_ct': 1517319037
                        }, {
                            u'f': u'xgphrtpe',
                            u'l': u'rjpqgikh',
                            u'_ct': 1517319037,
                            u'rc': 1,
                            u'dt': u'sn',
                            u'id': 3
                        }, {
                            u'id': 6,
                            u'dt': u'n',
                            u'rc': 1,
                            u'_ct': 1517319037,
                            u'n': u'lvlikvwf'
                        }
                    ],
                    u'id': 4,
                    u'_ct': 1517318977
                }
            },
            u'tags': {
                1: {
                    u'color': u'',
                    u'tid': 1,
                    u'count': 2,
                    u'parent': None,
                    u'title': u'asd'
                }
            },
        },
        u'indx': {
            u'phone': {
                u'1': {
                    u'3': {
                        u'4': {
                            u'2': {
                                u'__': 1,
                                u'_d': [3]
                            }
                        }
                    }
                },
                u'+': {
                    u'9': {
                        u'9': {
                            u'9': {
                                u'9': {
                                    u'9': {
                                        u'9': {
                                            u'99999': {
                                                u'__': 1,
                                                u'_d': [4]
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            u'email': {
                u's': {
                    u'o': {
                        u'q': {
                            u'k': {
                                u'w': {
                                    u'e': {
                                        u'n': {
                                            u'o@yandex.ru': {
                                                u'__': 1,
                                                u'_d': [4]
                                            },
                                            u'__': 1,
                                            u'_d': [4]
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                u'm': {
                    u'a': {
                        u'i': {
                            u'l': {
                                u'p': {
                                    u'r': {
                                        u'o': {
                                            u'to-bugs@yandex-team.ru': {
                                                u'__': 1,
                                                u'_d': [2]
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                u'd': {
                    u'l': {
                        u'a': {
                            u'f': {
                                u'j': {
                                    u'e': {
                                        u'k': {
                                            u'fw@yandex.ru': {
                                                u'__': 1,
                                                u'_d': [1]
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                u'f': {
                    u'o': {
                        u'o': {
                            u'@': {
                                u'b': {
                                    u'a': {
                                        u'r': {
                                            u'.baz': {
                                                u'__': 1,
                                                u'_d': [3]
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            u'ver': u'3',
            u'uri': {},
            u'name': {
                u'x': {
                    u'g': {
                        u'p': {
                            u'h': {
                                u'r': {
                                    u't': {
                                        u'p': {
                                            u'e': {
                                                u'__': 1,
                                                u'_d': [4]
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                u'r': {
                    u'j': {
                        u'p': {
                            u'q': {
                                u'g': {
                                    u'i': {
                                        u'k': {
                                            u'h': {
                                                u'__': 1,
                                                u'_d': [4]
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                u'b': {
                    u'a': {
                        u'r': {
                            u'__': 1,
                            u'_d': [3]
                        },
                        u'z': {
                            u'__': 1,
                            u'_d': [3]
                        }
                    }
                },
                u'f': {
                    u'o': {
                        u'o': {
                            u'__': 1,
                            u'_d': [3]
                        }
                    }
                }
            }
        },
    },
}
