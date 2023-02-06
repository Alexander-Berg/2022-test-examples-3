# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import mock
import pytest


@pytest.fixture
def m_kikimr_client():
    from common.db.kikimr import client
    with mock.patch.object(client, 'KiKiMRClient', autospec=True) as m_kikimr_client:
        yield m_kikimr_client
