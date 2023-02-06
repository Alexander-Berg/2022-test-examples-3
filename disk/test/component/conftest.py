# -*- coding: utf-8 -*-
import os


def pytest_configure(config):
    os.environ['MPFS_REAL_MONGO'] = 'TRUE'
