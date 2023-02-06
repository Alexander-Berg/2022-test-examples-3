class Production(object):
    timeout = 3
    host = 'https://api.rasp.yandex.net'
    api_key = '8eb91582-941a-4ac8-a618-040c314495ab'
    timeout_slow = 8
    timeout_very_slow = 15


class Prestable(Production):
    host = 'https://prestable.api-public.rasp.yandex.net'


class Testing(Production):
    host = 'https://testing.api-public.rasp.yandex.net'
    api_key = '559cdea5-2b77-4152-adf5-f6c600f2ebe2'
    timeout = 5
    timeout_slow = 10
    timeout_very_slow = 20


env = Production()
