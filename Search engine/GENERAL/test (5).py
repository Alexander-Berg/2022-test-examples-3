# coding: utf-8

from components_app.configs.base import *

zk_storage = dict(
    node='/components_app/tests/zk_storage',
    identifier='test_script',
    zk=dict(
        hosts=[
            'ws34-526.search.yandex.net:1039',
            'ws39-272.search.yandex.net:1027',
            'ws25-500.search.yandex.net:1034',
            'sas1-0375.search.yandex.net:1025',
            'sas1-5285.search.yandex.net:1039',
            'man1-3020.search.yandex.net:1039',
            'man1-7171.search.yandex.net:1030',
        ]
    )
)


zk_state_machine = dict(
    node='/components_app/tests/zk_state_machine',
    identifier='test_script',
    zk=dict(
        hosts=[
            'ws34-526.search.yandex.net:1039',
            'ws39-272.search.yandex.net:1027',
            'ws25-500.search.yandex.net:1034',
            'sas1-0375.search.yandex.net:1025',
            'sas1-5285.search.yandex.net:1039',
            'man1-3020.search.yandex.net:1039',
            'man1-7171.search.yandex.net:1030',
        ]
    )
)
