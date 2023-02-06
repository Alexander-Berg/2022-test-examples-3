#! /usr/bin/python

import thread
import requests
import json
from socket import gethostname
from urlparse import urlparse
from nose.tools import *
from nose.plugins.attrib import attr
from utils import *
from users import *
from urls import *
import queries


@attr(type="proxy")
@retry(tries=5, delay=1)  # FIXME retries is workaround for HTTP Error 413: RequestEntityTooLarge
def test_proxy_to_master():
    user = TEST_USERS["common_zoho_user"]
    check_auth(user)


@attr(type="acqured_auth_lock")
def test_acquired_auth_lock():
    user = TEST_USERS["common_zoho_user"]
    check_auth(user)
