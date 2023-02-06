# -*- coding: utf-8 -*-

from unittest import TestCase

from mpfs.platform.rate_limiters import PerSomethingUniqueLimiter


class UidWrapper(object):
    def __init__(self, uid):
        self.uid = uid


class RequestImitator(object):
    def __init__(self, uid=None, user_agent=None, yandexuid=None):
        self.user = UidWrapper(uid) if uid else None

        self.raw_headers = {}
        if user_agent:
            self.raw_headers['user-agent'] = user_agent
        if yandexuid:
            self.raw_headers['cookie'] = 'yandexuid=%s' % yandexuid


def user_agent_for_id(_id):
    return 'Yandex.Disk {"os":"android", "id":"%s"}' % _id


class PerSomethingUniqueLimiterTestCase(TestCase):
    sut = PerSomethingUniqueLimiter('not_relevant')

    def test_empty(self):
        assert self.sut.get_counter_key(RequestImitator()) == 'anonymous'

    def test_uid(self):
        assert self.sut.get_counter_key(RequestImitator(uid='111')) == '111'

    def test_user_agent(self):
        assert self.sut.get_counter_key(RequestImitator(user_agent=user_agent_for_id('222'))) == '222'

    def test_wrong_user_agent(self):
        assert self.sut.get_counter_key(RequestImitator(user_agent='Google Chrome')) == 'anonymous'

    def test_yandexuid(self):
        assert self.sut.get_counter_key(RequestImitator(yandexuid='333')) == '333'

    def test_uid_is_preferable(self):
        assert self.sut.get_counter_key(RequestImitator(uid='111', user_agent=user_agent_for_id('222'), yandexuid='333')) == '111'
