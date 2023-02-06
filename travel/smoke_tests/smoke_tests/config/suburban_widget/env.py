class Production(object):
    host = 'https://suburban-widget.rasp.yandex.net'
    timeout = 1
    timeout_slow = 2


class Testing(Production):
    host = 'https://testing.suburban-widget.rasp.common.yandex.net'
    timeout = 2
    timeout_slow = 4


env = Production()
