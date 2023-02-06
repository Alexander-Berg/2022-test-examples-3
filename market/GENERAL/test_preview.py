#!/usr/bin/python
# -*- coding: utf-8 -*-

import os
import unittest
import subprocess


DATADIR = os.path.abspath('test_data')
SERVANT_CONFIG_DEST = os.path.abspath('servant_config.cfg')
GETTER_CONFIG_DEST = os.path.abspath('getter_config.conf')

PREVIEW_HOST = 'kgb-preview01e.market.yandex.net'


def run_retarget(hostname, data_getter_script, corba_getter_script):
    if data_getter_script:
        getter_param = '--data-getter-config=%s/getter.conf\n--data-getter-script=%s' % (DATADIR, data_getter_script)
    elif corba_getter_script:
        getter_param = '--corba-getter-config=%s/getter.conf\n--corba-getter-script=%s' % (DATADIR, corba_getter_script)

    args = '''python ./preview_servant_version.py retarget_config \
                                                  --servant-name=test_servant \
                                                  {GETTER_PARAM} \
                                                  --servant-preview-config={CONFDIR}/preview.cfg \
                                                  --servant-market-config={CONFDIR}/market.cfg \
                                                  --servant-config-dest={S_CFG_DEST} \
                                                  --getter-config-dest={G_CFG_DEST} \
                                                  --hostname={HOSTNAME}
            '''.format(CONFDIR=DATADIR, S_CFG_DEST=SERVANT_CONFIG_DEST,
                       G_CFG_DEST=GETTER_CONFIG_DEST, HOSTNAME=hostname,
                       GETTER_PARAM=getter_param)

    p = subprocess.Popen(args=args.split(), env={})
    p.wait()
    return p.poll()


def run_delete():
    args = '''python ./preview_servant_version.py delete_config \
                                                  --servant-name=test_servant \
                                                  --servant-config-dest={S_CFG_DEST} \
                                                  --getter-config-dest={G_CFG_DEST}
            '''.format(CONFDIR=DATADIR, S_CFG_DEST=SERVANT_CONFIG_DEST, G_CFG_DEST=GETTER_CONFIG_DEST)
    return subprocess.call(args=args.split())


class TestRetargetConfig(unittest.TestCase):
    def _test(self, hostname, expected_servant_config, expected_getter_config, data_getter_script='', corba_getter_script=''):
        # '' - брать скрипт по-умолчанию, None - не использовать скрипт
        dgs = 'fake-get-market-data' if data_getter_script == '' else data_getter_script
        cgs = 'fake-get-market-corba' if corba_getter_script == '' else corba_getter_script
        self.assertEqual(run_retarget(hostname, dgs, cgs), False)
        servant_config_link = os.path.basename(os.readlink(SERVANT_CONFIG_DEST))
        self.assertEqual(servant_config_link, expected_servant_config)
        if expected_getter_config:
            getter_config_link = os.path.basename(os.readlink(GETTER_CONFIG_DEST))
            self.assertEqual(getter_config_link, expected_getter_config)
        self.assertEqual(run_delete(), 0)

    def _print_test_index(self, index):
        print '\n===== TEST %d =====' % index

    def test_localhost(self):
        self._print_test_index(1)
        self._test('localhost', 'market.cfg', '')

    def test_preview_with_broken_getters(self):
        self._print_test_index(2)
        # если используем несуществующий корба-геттер, то выбор превью валится
        self._test(PREVIEW_HOST, 'market.cfg', '', data_getter_script=None, corba_getter_script='foo')
        # аналгично c несуществующим дата-геттером
        self._test(PREVIEW_HOST, 'market.cfg', '', data_getter_script='bar', corba_getter_script=None)

    def test_preview_with_fake_corba_getter(self):
        self._print_test_index(3)
        self._test(PREVIEW_HOST, 'preview.cfg', 'getter.conf', data_getter_script=None)

    def test_preview_with_fake_data_getter(self):
        self._print_test_index(4)
        self._test(PREVIEW_HOST, 'preview.cfg', 'getter.conf', corba_getter_script=None)


if __name__ == '__main__':
    result = unittest.main()
