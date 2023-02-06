class Production(object):
    host = 'https://production.touch.rasp.common.yandex.ru'
    timeout = 5
    timeout_slow = 10
    timeout_very_slow = 100
    retries = 3


class Testing(Production):
    host = 'https://testing.touch.rasp.common.yandex.ru'


env = Production()
