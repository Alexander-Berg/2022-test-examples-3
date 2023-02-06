class Production(object):
    host = 'https://production.pathfinder-core.rasp.yandex.net'
    timeout = 0.8


class Testing(Production):
    host = 'https://testing.pathfinder-core.rasp.yandex.net'


env = Production()
