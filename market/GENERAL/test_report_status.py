#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main

import datetime
import os
import time
from six.moves import http_client


class T(TestCase):
    @classmethod
    def prepare(cls):
        pass

    def _wait_for_report_status(self, expected_report_status):
        deadline = datetime.datetime.now() + datetime.timedelta(minutes=1)
        while datetime.datetime.now() < deadline:
            response = self.report.request_plain('place=report_status')
            if expected_report_status in str(response):
                return
            time.sleep(0.01)
        self.assertIn(expected_report_status, str(response))

    def test(self):
        # репорт стартует в состоянии CLOSED_INCONSISTENT_MANUAL_OPENING
        response = self.report.request_plain('admin_action=versions')
        self.assertIn('<report-status>CLOSED_INCONSISTENT_MANUAL_OPENING</report-status>', str(response))

        # place=report_status обновляет состояние консистентности
        response = self.report.request_plain('place=report_status')
        self.assertIn('2;CLOSED_CONSISTENT_MANUAL_OPENING', str(response))

        # что будет, если закрыть репорт в "закрытом" состоянии? Ничего
        response = self.report.request_plain('place=report_status&report-status-action=close')
        self.assertIn('2;CLOSED_CONSISTENT_MANUAL_OPENING', str(response))

        # что будет, если открыть репорт в "закрытом" состоянии? Он откроется
        response = self.report.request_plain('place=report_status&report-status-action=open')
        self.assertIn('0;OK', str(response))

        # Статус репорт обновился и при запросе admin_action=versions
        response = self.report.request_plain('admin_action=versions')
        self.assertIn('<report-status>OPENED_CONSISTENT</report-status>', str(response))

        response = self.report.request_plain('place=report_status')
        self.assertIn('0;OK', str(response))

        # что будет, если открыть репорт в "открытом" состоянии? Ничего
        response = self.report.request_plain('place=report_status&report-status-action=open')
        self.assertIn('0;OK', str(response))

        # что будет, если закрыть репорт в "открытом" состоянии? Он закроется
        response = self.report.request_plain('place=report_status&report-status-action=close')
        self.assertIn('2;CLOSED_CONSISTENT_MANUAL_OPENING', str(response))

        # новое состояние сохраняется и при запросе без параметров
        response = self.report.request_plain('place=report_status')
        self.assertIn('2;CLOSED_CONSISTENT_MANUAL_OPENING', str(response))

        # Статус репорт обновился и при запросе admin_action=versions
        response = self.report.request_plain('admin_action=versions')
        self.assertIn('<report-status>CLOSED_CONSISTENT_MANUAL_OPENING</report-status>', str(response))

        # Переводим Репорт в локдаун, проверяем что возвращает статус 2 на любую команду.
        response = self.report.request_plain('place=report_status&report-lockdown=user')
        self.assertIn('2;CLOSED_CONSISTENT_MANUAL_OPENING;LOCKDOWN:user', str(response))
        response = self.report.request_plain('place=report_status&report-status-action=open')
        self.assertIn('2;OK;LOCKDOWN:user', str(response))
        response = self.report.request_plain('admin_action=versions')
        self.assertIn('<report-lockdown>user</report-lockdown>', str(response))
        # Записано ли состояние в файл?
        report_lockdown_file_path = os.path.join(self.meta_paths.lock_path, 'market_report_lockdown')
        self.assertTrue(os.path.isfile(report_lockdown_file_path))

        # Отключаем локдаун и смотрим что все ok.
        response = self.report.request_plain('place=report_status&report-lockdown=0')
        self.assertIn('0;OK', str(response))
        response = self.report.request_plain('admin_action=versions')
        self.assertIn('<report-lockdown></report-lockdown>', str(response))
        self.assertFalse(os.path.isfile(report_lockdown_file_path))

        # Проверяем что локдаун включается и выключается через файл.
        with open(report_lockdown_file_path, 'w') as f:
            f.write('user')
        self._wait_for_report_status('2;OK;LOCKDOWN:user')
        os.remove(report_lockdown_file_path)
        self._wait_for_report_status('0;OK')

        # Проверяем закрытие репорта без ожидания завершения production запросов
        response = self.report.request_plain(
            'place=report_status&report-status-action=close&wait-for-production-requests-to-stop=0'
        )
        self.assertIn('2;CLOSED_CONSISTENT_MANUAL_OPENING', str(response))

    def test_auto_closing_on_degradation_level(self):
        self.experiment_flags.reset()
        self.experiment_flags.add_flags(
            graceful_degradation_force_level=1,
            disable_production_on_graceful_degradation_level=2,
        )
        self.experiment_flags.save()
        # Just open report
        response = self.report.request_plain('place=report_status&report-status-action=open')
        self.assertIn('0;OK', str(response))

        response = self.report.request_plain('place=report_status&report-status-action=check-prod')
        self.assertIn('0;OK', str(response))

        response = self.report.request_plain('place=report_status')
        self.assertIn('0;OK', str(response))

        response = self.report.request_plain(
            'place=report_status&report-status-action=check-prod&rearr-factors=graceful_degradation_force_level=2;'
        )
        self.assertIn('0;OK', str(response))

        response = self.report.request_plain(
            'place=report_status&report-status-action=check-prod&rearr-factors=graceful_degradation_force_level=2;'
        )
        self.assertIn('0;OK', str(response))

        response = self.report.request_plain(
            'place=report_status&report-status-action=check-prod&rearr-factors=graceful_degradation_force_level=2'
            ';disable_production_on_graceful_degradation_level=3'
        )
        self.assertIn('0;OK', str(response))

        self.experiment_flags.reset()
        self.experiment_flags.add_flags(
            graceful_degradation_force_level=3,
            disable_production_on_graceful_degradation_level=2,
        )
        self.experiment_flags.save()
        self.common_log.wait_line('Flag graceful_degradation_force_level changed by emergency break')

        response = self.report.request_plain('place=report_status&report-status-action=check-prod')
        self.assertIn('2;CLOSED_INCONSISTENT_AUTO_OPENING', str(response))

        # Проверяем закрытие репорта без ожидания завершения production запросов
        response = self.report.request_plain(
            'place=report_status&report-status-action=close&wait-for-production-requests-to-stop=0'
        )
        self.assertIn('2;CLOSED_CONSISTENT_MANUAL_OPENING', str(response))

    def test_report_last_document_freshness(self):
        response = self.base_search_client.request_plain('admin_action=versions&aquirestats=1')
        self.assertIn('<last-rty-document-freshness>0</last-rty-document-freshness>', str(response))

    def test_strict_check_of_report_status(self):
        request_template = 'place=report_status&report-status-action=strict-check'

        def check_service_unavailable():
            response = self.report.request_plain(request_template, strict=False)
            self.assertEquals(response.code, http_client.LOCKED)

        def check_ok():
            response = self.report.request_plain(request_template)
            self.assertEquals(response.code, http_client.OK)

        # 423 on start
        check_service_unavailable()

        # 200 after open
        self.report.request_plain('place=report_status&report-status-action=open')
        check_ok()

        # 423 after close
        begin = time.time()
        self.report.request_plain('place=report_status&report-status-action=close')
        self.assertTrue(time.time() - begin > 5.0)
        check_service_unavailable()

        # don't wait on closing twice
        begin = time.time()
        self.report.request_plain('place=report_status&report-status-action=close')
        self.assertTrue(time.time() - begin < 5.0)

        # 423 under lockdown too
        self.report.request_plain('place=report_status&report-lockdown=user')
        check_service_unavailable()

        # still under lockdown so 423
        _ = self.report.request_plain('place=report_status&report-status-action=open')
        check_service_unavailable()

        # turn off lockdown so 200
        _ = self.report.request_plain('place=report_status&report-lockdown=0')
        check_ok()

    def test_mbo_stuff(self):
        # check mbo_stuff
        response = self.report.request_plain('admin_action=versions')
        self.assertIn('<mbo-stuff>20210830_0457</mbo-stuff>', str(response))


if __name__ == '__main__':
    main()
