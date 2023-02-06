#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import     # noqa
import market.quoter.manager.mt.env as env
import time
import os


class T(env.QuoterManagerSuite):
    def test_manager(self):
        checker_filepath = self._Suite__server.config.Checks[0].Checker.Filepath
        action_1_filepath = self._Suite__server.config.Checks[0].Actions[0].File.Path
        action_2_filepath = self._Suite__server.config.Checks[0].Actions[1].File.Path

        # мониторинг не горел, не должны реагировать
        self.quoter_manager.sync('check1')
        self.assertFalse(os.path.exists(action_1_filepath))
        self.assertFalse(os.path.exists(action_2_filepath))

        # мониторинг только что загорелся, должны реагировать
        with open(checker_filepath, 'w') as f:
            current_timestamp = int(time.time())
            f.write(str(current_timestamp))
        self.quoter_manager.sync('check1')
        self.assertTrue(os.path.exists(action_1_filepath))
        self.assertTrue(os.path.exists(action_2_filepath))

        # мониторинг горел, но давно, не должны реагировать
        with open(checker_filepath, 'w') as f:
            old_timestamp = int(time.time()) - 600
            f.write(str(old_timestamp))
        self.quoter_manager.sync('check1')
        self.assertFalse(os.path.exists(action_1_filepath))
        self.assertFalse(os.path.exists(action_2_filepath))


if __name__ == '__main__':
    env.main()
