# coding: utf-8

import time

from lib.viewed_models import MultiCluster


def test():
    def master(x):
        if x % 2 == 0:
            raise RuntimeError('bla')
        return x, 'master'

    def slave(x):
        return x * 2, 'slave'

    def check_master(result, expected):
        assert result[1] == 'master'
        assert result[0] == expected

    def check_puppet(result, expected):
        assert result[1] == 'slave'
        assert result[0] == expected

    mc = MultiCluster(master, slave)
    check_master(mc(1), 1)
    check_puppet(mc(2), 4)
    check_puppet(mc(3), 6)
    check_master(mc(3, now=time.time()+mc._slave_max_seconds), 3)

    mc = MultiCluster(master, slave, slave_max_requests=2)
    check_puppet(mc(2), 4)
    check_puppet(mc(3), 6)
    check_master(mc(3), 3)
