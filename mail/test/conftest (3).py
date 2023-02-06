import pytest
from os import environ
from xconf import XConf

@pytest.fixture(scope='session')
def xconf():
    conninfo = environ['TEST_XCONF_CONNINFO']
    return XConf(conninfo)
