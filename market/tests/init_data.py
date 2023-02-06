
# -*- coding: utf-8 -*-

from context import daas
from daas.button_report.cluster_store import ClusterStore
from daas.button_report.index_inspector import IndexQueryManager
import tempfile
import os
from daas.button_report.robot import create_options

CLUSTER_S = '{'\
            '"id": 1,'\
            '"name": "hd",'\
            '"hosts": ["msh01hd.market.yandex.net",'\
            '          "msh02hd.market.yandex.net",'\
            '          "msh03hd.market.yandex.net",'\
            '          "msh04hd.market.yandex.net",'\
            '          "msh05hd.market.yandex.net",'\
            '          "msh06hd.market.yandex.net",'\
            '          "msh07hd.market.yandex.net",'\
            '          "msh08hd.market.yandex.net",'\
            '          "msh-off01hd.market.yandex.net"]'\
            '}'\



def gen_cluster_data(data_path):
    cluster_path = os.path.join(data_path,
                                'cluster_store',
                                'hd.json')

    os.mkdir(os.path.join(data_path, 'cluster_store'))
    with open(cluster_path, 'w+') as f:
        f.write(CLUSTER_S)


def gen_index_data(data_path):
    os.mkdir(os.path.join(data_path, 'db'))


def gen_init_data():
    data_path = tempfile.mkdtemp(dir='/tmp')
    log_path = os.path.join(data_path, 'button_robot.log')
    gen_cluster_data(data_path)
    gen_index_data(data_path)
    return create_options(dummy=True, data_path=data_path, log_filename=log_path)
