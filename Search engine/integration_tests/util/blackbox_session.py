# -*- coding: utf-8 -*-

import requests
from util.const import TLD
from util.params import get_env_bool


TEST_USER_BLACKBOX_RESPONSE = {'users': [{
                               'id': '886656875',
                               'regname': 'robot-srch-int-tests',
                               'display_name': {
                                   'name': 'robot-srch-int-tests',
                                   'avatar': {
                                       'default': '63032/1k5LOaTrBgkfquog93QbMuo5k-1',
                                       'empty': False
                                   }
                               }}]}


class BlackboxSession(object):

    def GetProfile(self):
        return self.profile

    def GetSession(self):
        return self.session

    def Auth(self, login, password):
        self.profile = {'login': login, 'password': password}
        self.session = requests.Session()
        for tld in TLD.AUTH_TLD:
            c = {}
            resp = None
            for i in range(1):
                resp = self.session.post(
                    'https://passport.yandex.{tld}/auth'.format(tld=tld),
                    params={'retpath': 'https://passport.yandex.{tld}/profile'.format(tld=tld), 'twoweeks': '1'},
                    data={'login': self.profile['login'], 'passwd': self.profile['password']}, allow_redirects=False
                )
                c = self.session.cookies.get_dict()
                if 'yandex_login' in c:
                    break

            assert 'yandex_login' in c, "{} headers={} content={}".format(resp.status_code, resp.headers, resp.content)

            self.profile.update({'yandexuid': c['yandexuid']})

            if not get_env_bool('ONLY_YANDEXUID'):
                self.profile.update({
                    'yandex_login': c['yandex_login'],
                    'lah': c['lah'],
                    'sessionid2': c['sessionid2'],
                    'Session_id': c['Session_id'],
                    'L': c['L'],
                    'mda2_beacon': c['mda2_beacon'],
                })

        # addrinfo = socket.getaddrinfo(socket.gethostname(), None)
        # ipaddrs = list(map(lambda x: x[4][0], addrinfo))
        # bb_params = {'method': 'userinfo',
        #              'userip': ipaddrs[0],
        #              'login': self.profile['login'],
        #              'regname': 'yes',
        #              'format': 'json'}
        # bb_resp = self.session.get('http://blackbox.yandex.net/blackbox',
        #                            params=bb_params,
        #                            allow_redirects=False)
        # ui = json.loads(bb_resp.content)

        ui = TEST_USER_BLACKBOX_RESPONSE
        self.profile.update({'display_name': ui['users'][0]['display_name'],
                             'id': ui['users'][0]['id'],
                             'regname': ui['users'][0]['regname']})

    def __init__(self, login, password):
        self.profile = {}
        self.Auth(login, password)
