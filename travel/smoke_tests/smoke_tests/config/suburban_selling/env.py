class Production(object):
    host = 'https://suburban-selling.rasp.yandex.net'
    timeout = 6
    export_host = 'https://export.rasp.yandex.net'
    export_timeout = 6

    allow_test_context = False
    order_info_retries_number = 10
    order_info_retries_delay = 3


class Prestable(Production):
    host = 'https://prestable.suburban-selling.rasp.yandex.net'
    export_host = 'https://prestable.export.rasp.yandex.net'


class Testing(Production):
    host = 'https://testing.suburban-selling.rasp.yandex.net'
    export_host = 'https://testing.export.rasp.yandex.net'
    export_timeout = 10
    timeout = 15

    allow_test_context = True
    travel_api_tvm_service_id = 2002548
    travel_api_host = 'https://api.travel-balancer-test.yandex.net/api/'
    travel_api_timeout = 2
    order_info_retries_number = 20
    order_info_retries_delay = 5


env = Production()
