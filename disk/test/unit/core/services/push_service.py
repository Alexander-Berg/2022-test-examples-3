# -*- coding: utf-8 -*-
import json

import mpfs.common.errors as errors
from hamcrest import assert_that, calling, raises
from mpfs.core.services.push_service import XivaSubscribeService


def test_parse_app_token():
    parsed_token = {'app_name': 'ru.yandex.disk', 'platform': 'ios', 'push_token': 'test', 'uuid': '1'}
    assert XivaSubscribeService.parse_app_token('{"c":"ru.yandex.disk","d":"1","t":"test","p":"i"}') == parsed_token


def test_parse_app_token_urlencoded():
    parsed_token = {'app_name': 'ru.yandex.disk', 'platform': 'ios', 'push_token': 'test', 'uuid': '1'}
    encoded_token =  '%7B%22c%22%3A%22ru.yandex.disk%22,%22d%22%3A%221%22,%22t%22%3A%22test%22,%22p%22%3A%22i%22%7D'
    assert XivaSubscribeService.parse_app_token(encoded_token) == parsed_token


def test_parse_app_token_with_empty_tag():
    token_template = {"c": "ru.yandex.disk", "d": "1", "t":"test", "p": "i"}
    for empty_arg in ("c", "d", "t", "p"):
        token = token_template.copy()
        token[empty_arg] = ""
        token = json.dumps(token)
        assert_that(calling(XivaSubscribeService.parse_app_token).with_args(token), raises(errors.XivaBadToken))


def test_parse_app_token_missed_tags():
    assert_that(calling(XivaSubscribeService.parse_app_token).with_args('{"d": "1"}'), raises(errors.XivaBadToken))
    assert_that(calling(XivaSubscribeService.parse_app_token).with_args(''), raises(errors.XivaBadToken))


def test_parse_app_token_with_bad_push_token():
    token_template = {"c": "ru.yandex.disk", "d": "1", "t": "BLACKLISTED", "p": "i"}
    token = json.dumps(token_template)
    assert_that(calling(XivaSubscribeService.parse_app_token).with_args(token), raises(errors.XivaBadToken))
