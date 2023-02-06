class Production(object):
    host = 'https://production.old-morda-python.rasp.common.yandex.ru'
    timeout = 5
    timeout_slow = 10
    timeout_very_slow = 15
    retries = 3


class ProductionService(Production):
    host = 'https://production-service-db.old-morda-python.rasp.common.yandex.ru'


class Testing(Production):
    host = 'https://testing.old-morda-python.rasp.common.yandex.ru'


env = Production()
