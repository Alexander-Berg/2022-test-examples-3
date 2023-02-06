class Production(object):
    timeout = 8
    timeout_slow = 12
    timeout_very_slow = 20
    touch_timeout = 8
    retries = 5
    retries_delay = 30
    old_stack_retries = 6
    old_stack_retries_delay = 40

    host_ru = 'https://rasp.yandex.ru'
    host_by = 'https://rasp.yandex.by'

    host_ru_t = 'https://t.rasp.yandex.ru'
    host_by_t = 'https://t.rasp.yandex.by'

    @property
    def hosts(self):
        return [h for h in (self.host_ru, self.host_by) if h]

    @property
    def touch_hosts(self):
        return [h for h in (self.host_ru_t, self.host_by_t) if h]


class Prestable(Production):
    host_ru = 'https://prestable.morda-front.rasp.common.yandex.ru'
    host_by = None

    host_ru_t = 'https://t.prestable.morda-front.rasp.common.yandex.ru'
    host_by_t = None


class Testing(Production):
    timeout = 12
    timeout_slow = 24
    timeout_very_slow = 36
    touch_timeout = 12

    host_ru = 'https://testing.morda-front.rasp.common.yandex.ru'
    host_by = 'https://testing.morda-front.rasp.common.yandex.by'

    host_ru_t = 'https://testing.morda-front.t.rasp.common.yandex.ru'
    host_by_t = 'https://testing.morda-front.t.rasp.common.yandex.by'


env = Production()
