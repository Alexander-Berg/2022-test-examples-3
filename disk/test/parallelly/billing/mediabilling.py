# -*- coding: utf-8 -*-
import mock

from mpfs.core.billing import THIRD_TARIFF_PLUS_PIDS, YANDEX_PLUS_UPSALE, ctimestamp
from mpfs.core.billing.processing.billing import push_billing_commands
from test.helpers.stubs.services import MediaBillingStub
from test.parallelly.billing.base import BaseBillingTestCase


class MediabillingProcessingTestCase(BaseBillingTestCase):
    def test_provide_service(self):
        services = self.get_services_list(uid=self.uid)

        assert not set(THIRD_TARIFF_PLUS_PIDS) & set(service['name']
                                                     for service in services)

        with MediaBillingStub():
            self.billing_ok('process_mediabilling_callback', opts={'uid': self.uid})

        services = self.get_services_list(uid=self.uid)
        assert len(set(THIRD_TARIFF_PLUS_PIDS) & set(service['name']
                                                     for service in services)) == 1

    def test_delete_service(self):
        pid = THIRD_TARIFF_PLUS_PIDS[0]
        sid = self.service_create(uid=self.uid, pid=pid, line=YANDEX_PLUS_UPSALE)
        # делаем услугу протухшей
        self.service_manual_set_params(uid=self.uid, sid=sid, btime=(ctimestamp() - 1))

        # услуга пока есть
        services = self.get_services_list(uid=self.uid)
        assert pid in set(service['name']
                          for service in services)

        # запускаем кроновую обработку (раз в 30 мин, которая запускается)
        push_billing_commands()

        # проверяем что услугу отобрали
        services = self.get_services_list(uid=self.uid)
        assert pid not in set(service['name']
                              for service in services)
