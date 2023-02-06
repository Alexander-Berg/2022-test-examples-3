class Production(object):
    host = 'https://morda-backend.rasp.yandex.net'
    timeout = 4
    timeout_slow = 8
    timeout_very_slow = 20
    allow_large_search = True
    check_avia_tariffs = True


class Prestable(Production):
    host = 'https://prestable.morda-backend.rasp.yandex.net'


class Testing(Production):
    host = 'https://testing.morda-backend.rasp.yandex.net'
    timeout = 6
    timeout_slow = 10
    timeout_very_slow = 30
    allow_large_search = False
    check_avia_tariffs = False

env = Production()
