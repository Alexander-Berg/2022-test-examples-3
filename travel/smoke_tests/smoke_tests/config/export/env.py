class Production(object):
    timeout = 3
    host = 'https://export.rasp.yandex.net'
    timeout_slow = 8
    timeout_very_slow = 15
    is_production = True


class Prestable(Production):
    host = 'https://prestable.export.rasp.yandex.net'


class Testing(Production):
    host = 'https://testing.export.rasp.yandex.net'
    timeout = 8
    timeout_slow = 30
    timeout_very_slow = 40
    is_production = False


env = Production()
