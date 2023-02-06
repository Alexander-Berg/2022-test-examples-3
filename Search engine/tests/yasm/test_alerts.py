# -*- coding: utf-8 -*-

import itertools
import random


from search.mon.wabbajack.libs.modlib.api_wrappers.yasm.fresh_history.external import get_fresh_history_alert
from search.mon.wabbajack.libs.modlib.api_wrappers.yasm.fresh_history.common import VALUE_MODIFY_OBJECT


class StateHelper(object):

    def __init__(self, alert):
        self._state = alert.get_alert_state()

    def assert_state(self, state):
        assert self._state.state == state
        return self

    def assert_value(self, value):
        assert round(self._state.value, 2) == value
        return self


class AlertHelper(object):

    def __init__(self, alert):
        self._alert = alert
        self._counter = itertools.count()

    def push(self, value):
        self._alert.push(next(self._counter), value)
        return StateHelper(self._alert)


class TestTrend(object):
    """Тесты трендовых алертов"""

    def _init_alert_conf(self, warn=50, crit=80, interval=500, trend="down"):
        """Синтетический конфиг алерта"""
        self.warn = warn
        self.crit = crit
        self.interval = interval
        self.trend_type = trend

        return {
            "crit": [crit, None],
            "warn": [warn, None],
            "interval": interval,
            "trend": trend,
            "mgroups": ["ABALANCER"],
            "signal": "balancer-balancer_success",
            "tag": "balancer_custom_self"
        }

    def _init_test_param(self, base=(1000, 1001), start=100, stop=None):
        self.base = base
        self.start = start
        self.stop = stop

    def _minmax(self, n):
        if self.stop and n >= self.stop:
            return self.base
        elif self.start and n >= self.start:
            min_perc = int((self.base[0] / 100) * (self.warn + 1))
            max_perc = int((self.base[1] / 100) * (self.warn + 1))

            if self.trend_type == "down":
                return self.base[0] - min_perc, self.base[1] - max_perc
            elif self.trend_type == "up":
                if min_perc == 0:
                    min_perc = 1
                if max_perc == 0:
                    max_perc = 1

                return self.base[0] + min_perc, self.base[1] + max_perc
        else:
            return self.base

    def _generate(self):
        for n in range(0, 1000):
            mi, ma = self._minmax(n)

            x = random.randint(mi, ma)
            yield n, x

    def test_trend_norm(self):
        state = ""
        pos = 0
        self._init_test_param(start=None)

        conf = self._init_alert_conf()
        trend = get_fresh_history_alert(conf)

        for n, x in self._generate():
            trend.push(n, x)
            cur_state = trend.get_alert_state().state

            if state != cur_state:
                state = cur_state

                if cur_state == "warn":
                    pos = n

        assert pos == 0

    def test_trend_down(self):
        state = ""
        pos = 0
        self._init_test_param()

        conf = self._init_alert_conf()
        trend = get_fresh_history_alert(conf)

        for n, x in self._generate():
            trend.push(n, x)
            cur_state = trend.get_alert_state().state

            if state != cur_state:
                state = cur_state

                if cur_state == "warn":
                    pos = n

        assert pos == self.start

    def test_trend_down_up(self):
        state = ""
        pos_start = 0
        pos_stop = 0
        self._init_test_param(stop=199)

        conf = self._init_alert_conf()
        trend = get_fresh_history_alert(conf)

        for n, x in self._generate():
            trend.push(n, x)
            cur_state = trend.get_alert_state().state

            if state != cur_state:
                state = cur_state

                if cur_state == "warn":
                    pos_start = n

                if cur_state == "ok":
                    pos_stop = n

        assert pos_start == self.start
        assert pos_stop == self.stop

    def test_trend_up(self):
        state = ""
        pos_start = 0
        self._init_test_param()

        conf = self._init_alert_conf(trend="up")
        trend = get_fresh_history_alert(conf)

        for n, x in self._generate():
            trend.push(n, x)
            cur_state = trend.get_alert_state().state

            if state != cur_state:
                state = cur_state

                if cur_state == "warn":
                    pos_start = n

        assert pos_start == self.start

    def test_trend_up_down(self):
        state = ""
        pos_start = 0
        pos_stop = 0
        self._init_test_param(stop=199)

        conf = self._init_alert_conf(trend="up")
        trend = get_fresh_history_alert(conf)

        for n, x in self._generate():
            trend.push(n, x)
            cur_state = trend.get_alert_state().state

            if state != cur_state:
                state = cur_state

                if cur_state == "warn":
                    pos_start = n

                if cur_state == "ok":
                    pos_stop = n

        assert pos_start == self.start
        assert pos_stop == self.stop

    def test_trend_aver_current_value_window(self):
        # 'warn' and 'crit' are 'warn_perc' and 'crit_perc' actually
        trend = get_fresh_history_alert({
            "trend": "up",
            "warn": [8, None],
            "crit": [15, None],
            "interval": 50,
            "value_modify": {
                "type": "aver",
                "window": 15
            }
        })
        assert trend.get_window_size() == 65

        trend = AlertHelper(trend)

        for _ in range(12):
            # ignore first 4 iteration
            trend.push(100).assert_state(None)  # nodata

        # goto normal state
        trend.push(100).assert_state("ok")
        trend.push(120).assert_state("ok").assert_value(6.67)  # 106.67%
        trend.push(120).assert_state("warn").assert_value(13.33)  # 113.33%
        trend.push(120).assert_state("crit").assert_value(20.0)  # 120%

    def test_current_minute_is_half_of_previous(self):
        # see https://st.yandex-team.ru/GOLOVAN-4434#1489574960000 for picture

        # 'warn' and 'crit' are 'warn_perc' and 'crit_perc' actually
        trend = get_fresh_history_alert({
            "trend": "down",
            "warn": [50, None],
            "crit": [60, None],
            "interval": 120,  # two minutes
            "interval_modify": {
                "type": "aver",
            },
            "value_modify": {
                "type": "aver",
                "window": 60  # one minutes
            }
        })
        assert trend.get_window_size() == 180

        trend = AlertHelper(trend)

        # nodata while interval not filled
        for _ in range(12 * 3 - 1):
            trend.push(200).assert_state(None)

        # still has fewer than 50% down on current minute
        for _ in range(12 - 1):
            trend.push(100).assert_state("ok")

        # here we have average 100 on current minute, and 200 on previous
        # so it is 50% down trand, so warn
        trend.push(100).assert_state("warn").assert_value(50.0)

    def test_absolute_down(self):
        trend = get_fresh_history_alert({
            "trend": "down",
            "warn": [1, 1],
            "crit": [2, 2],
            "absolute": True,
            "interval": 60
        })
        assert trend.get_window_size() == 65

        trend = AlertHelper(trend)

        for _ in range(12):
            trend.push(10).assert_state(None)

        trend.push(10).assert_state("ok")
        trend.push(11).assert_state("ok")
        trend.push(10).assert_state("warn")
        trend.push(9).assert_state("crit")
        trend.push(10).assert_state("warn")

        for _ in range(9):
            trend.push(10).assert_state("warn")

        trend.push(10).assert_state("ok")

    def test_is_ready(self):
        trend = get_fresh_history_alert({
            "trend": "down",
            "warn": [1, 1],
            "crit": [2, 2],
            "absolute": True,
            "interval": 60
        })
        for step in range(int(trend.get_window_size() / 5)):
            assert not trend.is_ready()
            trend.push(step, 10)
        assert trend.is_ready()


class TestThresholdsFreshHistory(object):
    # see https://st.yandex-team.ru/GOLOVAN-4460#1502702455000

    def test_upper_crit(self):
        alert = get_fresh_history_alert({
            "warn": [10.0, 20.0],
            "crit": [20.0, None],
            VALUE_MODIFY_OBJECT: {
                "type": "aver",
                "window": 60  # one minute
            }
        })
        assert alert.get_window_size() == 60

        alert = AlertHelper(alert)

        for _ in range(12):
            alert.push(None).assert_state(None)

        alert.push(9).assert_state("ok")
        alert.push(20).assert_state("warn")
        alert.push(40).assert_state("crit")
        alert.push(19).assert_state("crit")  # aver still > 20

        # goto stable crit state
        for _ in range(12):
            alert.push(21).assert_state("crit")

        # still in crit state
        for _ in range(6):
            alert.push(19).assert_state("crit")

        # goto warn state
        for _ in range(6):
            alert.push(19).assert_state("warn")

    def test_thresholds_fresh_history(self):
        alert = get_fresh_history_alert({
            "crit": [None, 10.0],
            "warn": [10.0, 20.0],
            VALUE_MODIFY_OBJECT: {
                "type": "aver",
                "window": 60  # one minute
            }
        })
        assert alert.get_window_size() == 60

        alert = AlertHelper(alert)

        for _ in range(11):
            alert.push(None).assert_state(None)

        alert.push(0).assert_state("crit")
        alert.push(11).assert_state("crit")  # aver still < 10
        alert.push(100).assert_state("ok")

        # goto stable ok state
        for _ in range(12):
            alert.push(21).assert_state("ok")

        for _ in range(6):
            alert.push(1).assert_state("warn")

        for step in range(6):
            alert.push(1).assert_state("crit")

    def test_thresholds_summ_aggregator(self):
        alert = get_fresh_history_alert({
            "crit": [10, None],
            "warn": [5, 10],
            VALUE_MODIFY_OBJECT: {
                "type": "summ",
                "window": 60  # one minute
            }
        })
        assert alert.get_window_size() == 60

        alert = AlertHelper(alert)

        for _ in range(11):
            alert.push(None).assert_state(None)

        for _ in range(4):
            alert.push(1).assert_state("ok")

        for _ in range(5):
            alert.push(1).assert_state("warn")

        alert.push(1).assert_state("crit")

    def test_is_ready(self):
        alert = get_fresh_history_alert({
            "crit": [10, None],
            "warn": [5, 10],
            VALUE_MODIFY_OBJECT: {
                "type": "summ",
                "window": 60  # one minute
            }
        })
        for step in range(int(alert.get_window_size() / 5)):
            assert not alert.is_ready()
            alert.push(step, 1)
        assert alert.is_ready()
