class Production(object):
    host = 'https://infocenter.rasp.yandex.net'
    host_admin = 'https://infocenter.rasp.yandex-team.ru'
    timeout = 2


class Testing(Production):
    host = 'https://testing.infocenter.rasp.yandex.net'
    host_admin = 'https://testing.infocenter.rasp.yandex-team.ru'
    timeout = 4


env = Production()
