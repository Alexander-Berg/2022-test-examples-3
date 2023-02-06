class Production(object):
    host = 'https://suggests.rasp.yandex.net'
    timeout = 0.3


class Testing(Production):
    host = 'https://testing.suggests.rasp.common.yandex.net'


env = Production()
