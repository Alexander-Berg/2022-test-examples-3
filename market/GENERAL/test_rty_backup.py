#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import shutil
import os
from core.types import Offer, RtyOffer
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.rty_qpipe = True
        cls.index.creation_time = 10000000
        cls.index.offers += [
            Offer(title='brick', feedid=26, offerid='aaa', price=400, fesh=21),
            Offer(title='brick', feedid=26, offerid='bbb', price=400, fesh=21),
        ]

    def _check_prices(self, price):
        response = self.report.request_json('place=prime&text=brick&rearr-factors=rty_qpipe=1')
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'shop': {'feed': {'id': '26', 'offerId': 'aaa'}},
                    'prices': {'currency': 'RUR', 'value': str(price)},
                },
                {
                    'entity': 'offer',
                    'shop': {'feed': {'id': '26', 'offerId': 'bbb'}},
                    'prices': {'currency': 'RUR', 'value': str(price)},
                },
            ],
        )

    def test_backup(self):
        """
        Проверяем работу фолбека в двух случаях:
        1) когда в rty-индексе документа нет
        2) когда в rty-индексе документ старше, чем большое поколение
        """
        paths = self.base_paths

        # проверяем, что оффер берется из большого поколения, т.к. в rty его еще нет
        self._check_prices(400)

        # добавляем его в rty, но с бОльшим таймстемпом, нежели у поколения
        self.rty.offers += [RtyOffer(feedid=26, offerid='aaa', price=500, modification_time=11000000)]
        self.rty_controller.reopen_indexes()

        self.rty.offers += [RtyOffer(feedid=26, offerid='bbb', price=500, modification_time=11000000)]
        self.rty_controller.reopen_indexes()

        # проверяем, что теперь оффер берется из rty
        self._check_prices(500)

        response = self.base_search_client.request_xml('admin_action=rty_backup&action=make')
        self.assertFragmentIn(response, "<admin-action>ok</admin-action>")

        # рестарт репорта
        self.stop_report()
        # очищаем индекс
        shutil.rmtree(paths.rty_index)
        self.restart_report()

        # проверяем, что оффер из rty никуда не делся.
        self._check_prices(500)

        # рестарт репорта
        self.stop_report()
        # очищаем индекс
        shutil.rmtree(paths.rty_index)
        # делаем бэкап невалидным
        backup_dir = os.path.join(paths.persistent, 'rty_backup')
        for dir in os.listdir(backup_dir):
            header = os.path.join(backup_dir, dir, 'backup_header')
            if os.path.exists(header):
                os.remove(header)
        self.restart_report()

        # проверяем, что оффер не востановился
        self._check_prices(400)


if __name__ == '__main__':
    main()
