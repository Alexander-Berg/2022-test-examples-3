# -*- coding: utf-8 -*-

import unittest
import mock

from reductor.reductor import Reductor, Backend, BACKCTLD_PORT
from market.pylibrary.yatestwrap.yatestwrap import source_path

DATA_DIR = source_path('market/reductor/reductor/test/data')


class ReductorForTests(Reductor):
    def __init__(self, *args, **kwargs):
        self.backend_mocks = {}
        kwargs['backend_factory'] = self.mock_backend_factory
        Reductor.__init__(self, *args, **kwargs)

    def mock_backend_factory(self, hostname, datacenter, port, timeout):
        MockBackend = mock.create_autospec(Backend)
        mock_backend = MockBackend(hostname, datacenter, port, timeout)
        mock_backend.hostname = hostname
        mock_backend.datacenter = datacenter
        mock_backend.short = hostname.split('.')[0]
        mock_backend.port = port or BACKCTLD_PORT
        backend_key = '{}@{}:{}'.format(mock_backend.hostname, mock_backend.datacenter, mock_backend.port)
        self.backend_mocks[backend_key] = mock_backend
        return mock_backend


def main():
    unittest.main()
