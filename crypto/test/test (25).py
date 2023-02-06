#!/usr/bin/env python
# -*- coding: utf-8 -*-
import unittest

from crypta.lib.python import secret_manager

secret_name_to_secret_info = {
    'YT_TOKEN': secret_manager.Secret('sec-01csvzgg3mdasmdygkr5s8n6mz', 'token'),
    'ROBOT_UNICORN_STAFF_OAUTH': secret_manager.Secret('sec-01csvzjgsc81rdbvzhwz461n3s', 'oauth'),
    'CRYPTAIDOR_AUDIENCE_TVM_SECRET': secret_manager.Secret('sec-01ea01np15xqtkf4z0s59j3ajw', 'client_secret'),
}

secrets_by_name = {
    'YT_TOKEN': 'production_yt_token',
    'ROBOT_UNICORN_STAFF_OAUTH': 'unicorn_token',
    'CRYPTAIDOR_AUDIENCE_TVM_SECRET': 'yet_another_token',
}

secrets = secret_manager.SecretManager(secret_name_to_secret_info, secrets_by_name=secrets_by_name)


class Test(unittest.TestCase):
    def test_simple(self):
        self.assertEquals(secrets.get_secret('YT_TOKEN'), 'production_yt_token')
        self.assertEquals(secrets.get_secret('CRYPTAIDOR_AUDIENCE_TVM_SECRET'), 'yet_another_token')

    def test_no_token(self):
        with self.assertRaises(KeyError):
            secrets.get_secret('SOME_TOKEN')
