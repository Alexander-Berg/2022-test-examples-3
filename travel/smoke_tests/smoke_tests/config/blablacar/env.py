class Production(object):
    host = 'https://production.blablacar.rasp.yandex.net'
    timeout = 2
    retries = 6


class Testing(Production):
    host = 'http://testing.blablacar.rasp.yandex.net'
    timeout = 3


env = Production()
