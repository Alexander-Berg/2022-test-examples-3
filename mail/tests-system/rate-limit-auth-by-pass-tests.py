#! /usr/bin/python

import thread
import requests
import json
from socket import gethostname
from urlparse import urlparse
from nose.tools import *
from utils import *
from users import *
from urls import *
import queries
import time
from nose.tools import *

# It's important to have non-oauth accounts for this tests


def test_setup_limited_auth_from_same_ip():
    user = TEST_USERS["common_zoho_user"]
    check_auth(user)


def test_limited_auth_from_same_ip():
    user = TEST_USERS["common_zoho_user"]
    check_auth_failed(user, expected_status=3, expected_phrase="auth error: rate limit exceeded")
