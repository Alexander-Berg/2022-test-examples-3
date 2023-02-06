# coding: utf-8

from __future__ import absolute_import
from __future__ import unicode_literals
from __future__ import division

from collections import OrderedDict
from random import Random


RANDOM = Random()


def generate_proxy(data):
    d = OrderedDict()
    d['proxy'] = data
    d['conn-timeout'] = '1s'
    return d


def generate_serval_config(
    localhost,
    serval_port,
    serval_admin_port,
    rpslimiter_ports,
    serval_workers,
    weight_range=None
):
    if weight_range is None:
        weight_range = (1, 1)
    RANDOM.uniform(*weight_range)
    return {
        'bind': ['http://{}:{}'.format(localhost, serval_port)],
        'admin': ['http://{}:{}'.format(localhost, serval_admin_port)],
        'workers': int(serval_workers),
        'actions': [
            {
                'main': [
                    {
                        'proxy': [
                            {
                                RANDOM.uniform(*weight_range):
                                    generate_proxy('http://{}:{}'.format(localhost, rpslimiter_port))
                            }
                            for rpslimiter_port in rpslimiter_ports
                        ]
                    }
                ]
            }
        ]
    }
