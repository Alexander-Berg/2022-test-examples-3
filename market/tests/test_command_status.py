import os
import shutil
import unittest
import threading

from six.moves.configparser import ConfigParser
from datetime import datetime

import context
from market.pylibrary.mindexerlib import command_status
from market.pylibrary.mindexerlib import util


rootdir = 'tmp'


class TestCommandStatus(unittest.TestCase):
    def setUp(self):
        self.tearDown()
        util.makedirs(rootdir)

    def tearDown(self):
        shutil.rmtree(rootdir, ignore_errors=True, onerror=None)

    def test_cronstatus(self):
        cronstatus = command_status.CommandStatus(rootdir)
        cronstatus.clear()

        self.assertEqual('2;cannot find cron status file: %s' %
                         os.path.join(rootdir, 'command_status.ini'), cronstatus.check())

        cronstatus.update('offers-robot', True)
        cronstatus.update('feedchecker', True)
        cronstatus.update('feedparser', False)
        cronstatus.update('feedparser', False)
        cronstatus.update('feedparser', False)
        self.assertEqual(
            '2;feedparser failed 3 times (last_succeeded: None)',
            cronstatus.check()
        )

        cronstatus.update('feedparser', True)
        self.assertEqual('0;OK', cronstatus.check())

        date_str = datetime.utcnow().strftime('%Y-%m-%d')
        cronstatus.update('offers-robot', False)
        self.assertTrue(
            cronstatus.check().startswith(
                '1;offers-robot failed 1 times (last_succeeded: {}'.format(date_str)
            )
        )
        cronstatus.update('feedchecker', False)
        self.assertTrue(
            '1;offers-robot failed 1 times (last_succeeded: {}'.format(date_str) in cronstatus.check()
        )
        self.assertTrue(
            'feedchecker failed 1 times (last_succeeded: {}'.format(date_str) in cronstatus.check()
        )

    def test_cronstatus_for_exact_command(self):
        cronstatus = command_status.CommandStatus(rootdir)
        cronstatus.clear()

        self.assertEqual('2;cannot find cron status file: %s' %
                         os.path.join(rootdir, 'command_status.ini'), cronstatus.check_cmd('offers-robot'))

        cronstatus.update('offers-robot', True)
        cronstatus.update('feedchecker', True)
        cronstatus.update('feedparser', False)
        cronstatus.update('feedparser', False)
        cronstatus.update('feedparser', False)
        self.assertEqual(
            '2;feedparser failed 3 times (last_succeeded: None)',
            cronstatus.check_cmd('feedparser')
        )
        self.assertEqual(
            '0;OK',
            cronstatus.check_cmd('offers-robot')
        )

        cronstatus.update('feedparser', True)
        self.assertEqual('0;OK', cronstatus.check_cmd('feedparser'))

        date_str = datetime.utcnow().strftime('%Y-%m-%d')
        cronstatus.update('offers-robot', False)
        self.assertTrue(
            '1;offers-robot failed 1 times (last_succeeded: {}'.format(date_str) in cronstatus.check_cmd('offers-robot')
        )

        cronstatus.update('feedchecker', False)
        self.assertTrue(
            '1;offers-robot failed 1 times (last_succeeded: {}'.format(date_str) in cronstatus.check_cmd('offers-robot')
        )
        self.assertTrue(
            'feedchecker failed 1 times (last_succeeded: {}'.format(date_str) in cronstatus.check_cmd('feedchecker')
        )

    def test_cronstatus_polite_monitoring(self):
        cronstatus = command_status.CommandStatus(rootdir)
        cronstatus.clear()

        cronstatus.update('qidx', True)

        cronstatus.update('qidx', False)
        self.assertTrue(cronstatus.check().startswith('1;qidx failed 1 times'))

        cronstatus.update('qidx', False)
        self.assertTrue(cronstatus.check().startswith('1;qidx failed 2 times'))

        cronstatus.update('qidx', False)
        self.assertTrue(cronstatus.check().startswith('2;qidx failed 3 times'))

        cronstatus.update('qidx', False)
        self.assertTrue(cronstatus.check().startswith('2;qidx failed 4 times'))

        cronstatus.update('qidx', True)
        self.assertTrue(cronstatus.check().startswith('0;OK'))

    def test_cronstatus_multithreaded(self):
        NUM_THREADS = 7
        cronstatus = command_status.CommandStatus(rootdir)
        cronstatus.clear()
        cronstatus.update('ZERO', True)

        def separate_thread(cmd):
            cronstatus.update(cmd, True)

        def check_thread():
            self.assertEqual('0;OK', cronstatus.check())

        def update_thread_pool():
            thread_pool = []
            for idx in range(1, NUM_THREADS + 1):
                cmd = 'cmd_%d' % idx
                cmd_thread = threading.Thread(target=separate_thread, args=(cmd,))
                cmd_thread.start()
                thread_pool.append(cmd_thread)

            for cmd_thread in thread_pool:
                cmd_thread.join()

        def check_thread_pool():
            thread_pool = []
            for idx in range(1, NUM_THREADS + 1):
                cmd_thread = threading.Thread(target=check_thread)
                cmd_thread.start()
                thread_pool.append(cmd_thread)

            for cmd_thread in thread_pool:
                cmd_thread.join()

        update_pool = threading.Thread(target=update_thread_pool)
        check_pool = threading.Thread(target=check_thread_pool)

        update_pool.start()
        check_pool.start()
        update_pool.join()
        check_pool.join()

        cp = ConfigParser()
        cp.read([os.path.join(rootdir, 'command_status.ini')])
        self.assertEqual(NUM_THREADS + 1, len(cp.sections()))


if '__main__' == __name__:
    context.main()
