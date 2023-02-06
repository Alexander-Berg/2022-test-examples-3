#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import shutil
import time
from core.types import Offer, RtyOffer
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.rty_qpipe = True
        cls.settings.rty_backup_period = '3s'
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

        # проверяем, что оффер берется из большого поколения, т.к. в rty его еще нет
        self._check_prices(400)

        # добавляем его в rty, но с бОльшим таймстемпом, нежели у поколения
        self.rty.offers += [RtyOffer(feedid=26, offerid='aaa', price=500, modification_time=11000000)]
        self.rty_controller.reopen_indexes()

        self.rty.offers += [RtyOffer(feedid=26, offerid='bbb', price=500, modification_time=11000000)]
        self.rty_controller.reopen_indexes()

        # проверяем, что теперь оффер берется из rty
        self._check_prices(500)
        while True:
            resp = self.base_search_client.request_xml('admin_action=versions&aquirestats=1')

            last_backup = resp.root.find("report-stats/rty-backup/last_backup") if resp.code == 200 else None
            last_backup_ts = int(last_backup.attrib.get("ts")) if last_backup is not None else 0
            index = resp.root.find("report-stats/rty-backup/index") if resp.code == 200 else None
            index_ts = int(index.attrib.get("ts")) if index is not None else 0
            if last_backup_ts > 0 and index_ts > 0 and last_backup_ts >= index_ts:
                break
            time.sleep(1)

        # рестарт репорта
        self.stop_report()
        # очищаем индекс
        shutil.rmtree(self.meta_paths.rty_index)
        self.restart_report()

        # проверяем, что оффер из rty никуда не делся.
        self._check_prices(500)


if __name__ == '__main__':
    main()
