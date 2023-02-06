# -*- coding: utf-8 -*-


class MiddlewareRequest(object):
    def __init__(
            self,
            yauser=None,
            is_robot=False,
            is_secure=False,
            blackbox_session=None,
            cookies=None,
            is_browser=None,
            is_ajax=False,
            method='GET',
            nocookiesupport=False,
            yandexuid=None,
            tld='ru',
            has_new_yandexuid=False,
            client_ip='127.0.0.1'
    ):
        self.yauser = yauser
        self.uatraits_result = {'isRobot': True} if is_robot else {}
        self.uatraits_result['isBrowser'] = is_browser
        self._is_secure = is_secure
        self.blackbox_session = blackbox_session
        self.COOKIES = cookies or {}
        self.META = {}
        self._is_ajax = is_ajax
        self.method = method
        self.GET = {'nocookiesupport': 'yes'} if nocookiesupport else {}
        self.yandexuid = yandexuid
        self.tld = tld
        self.has_new_yandexuid = has_new_yandexuid
        self.client_ip = client_ip

    def is_secure(self):
        return self._is_secure

    def is_ajax(self):
        return self._is_ajax

    def get_host(self):
        return ''

    def build_absolute_uri(self, *args, **kwargs):
        return ''
