# coding: utf-8

import os
import yatest.common


from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.test_envs.base_env import BaseEnv


class PrepareMarketUserDataMode(object):
    PREPARE_LAST_DAY = "prepare-last-day"
    MERGE_DAYS = "merge-days"
    PREPARE_OLD_DAYS = "prepare-old-days"


class PrepareLastDayTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, **resources):
        super(PrepareLastDayTestEnv, self).__init__(**resources)

    @property
    def description(self):
        return 'PrepareLastDay'

    def execute(self, yt_stuff, bin_path, market_squeeze_path, daily_state_path, date=None, expiration_time=None, set_state_attr=False):
        if bin_path is None:
            relative_path = os.path.join('market', 'idx', 'streams',
                                         'src', 'prepare_market_user_data',
                                         'src', 'prepare_market_user_data')
            absolute_path = yatest.common.binary_path(relative_path)
            bin_path = absolute_path

        proxy = yt_stuff.get_server()

        cmd = [bin_path, PrepareMarketUserDataMode.PREPARE_LAST_DAY]

        cmd.extend([
            '--proxy', proxy,
            '--market-squeeze-path', market_squeeze_path,
            '--daily-state-path', daily_state_path,
        ])

        if date:
            cmd += ['--date', date]

        if expiration_time:
            cmd += ['--expiration-time', expiration_time]

        if set_state_attr:
            cmd += ['--set-state-attr']

        self.exec_result = self.try_execute_under_gdb(cmd)

        self.outputs.update(
            {
                "daily_state_path": daily_state_path,
            }
        )


class MergeDaysTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, **resources):
        super(MergeDaysTestEnv, self).__init__(**resources)

    @property
    def description(self):
        return 'MergeDays'

    def execute(self, yt_stuff, bin_path, daily_state_path, prod_state_path, days_to_merge=None):
        if bin_path is None:
            relative_path = os.path.join('market', 'idx', 'streams',
                                         'src', 'prepare_market_user_data',
                                         'src', 'prepare_market_user_data')
            absolute_path = yatest.common.binary_path(relative_path)
            bin_path = absolute_path

        proxy = yt_stuff.get_server()

        cmd = [bin_path, PrepareMarketUserDataMode.MERGE_DAYS]

        cmd.extend([
            '--proxy', proxy,
            '--daily-state-path', daily_state_path,
            '--prod-state-path', prod_state_path
        ])

        if days_to_merge:
            cmd += ['--days-to-merge', days_to_merge]

        self.exec_result = self.try_execute_under_gdb(cmd)

        self.outputs.update(
            {
                "prod_state_path": prod_state_path,
            }
        )


class PrepareOldDaysTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(self, **resources):
        super(PrepareOldDaysTestEnv, self).__init__(**resources)

    @property
    def description(self):
        return 'PrepareOldDays'

    def execute(self, yt_stuff, bin_path, market_squeeze_path, daily_state_path,
                start_date, end_date, days_per_run, expiration_time=None):
        if bin_path is None:
            relative_path = os.path.join('market', 'idx', 'streams',
                                         'src', 'prepare_market_user_data',
                                         'src', 'prepare_market_user_data')
            absolute_path = yatest.common.binary_path(relative_path)
            bin_path = absolute_path

        proxy = yt_stuff.get_server()

        cmd = [bin_path, PrepareMarketUserDataMode.PREPARE_OLD_DAYS]

        cmd.extend([
            '--proxy', proxy,
            '--market-squeeze-path', market_squeeze_path,
            '--daily-state-path', daily_state_path,
            '--start-date', start_date,
            '--end-date', end_date,
            '--days-per-run', days_per_run,
        ])

        if expiration_time:
            cmd += ['--expiration-time', expiration_time]

        self.exec_result = self.try_execute_under_gdb(cmd)

        self.outputs.update(
            {
                "daily_state_path": daily_state_path,
            }
        )
