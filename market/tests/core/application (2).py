import os, subprocess, logging
from datetime import datetime


class Application:
    def __init__(self, env, date=None):
        self._cluster = os.environ["YT_PROXY"]
        self._date = date if date else datetime.now().strftime("%Y-%m-%d")
        self._cur_ts = None
        self._env = env

    def set_time(self, ts):
        assert ts[:10] == self._date
        self._cur_ts = ts

    def run_checks(self, yt_context, prices_path):
        args = [
            "--config-table", yt_context.config.path,
            "--input-erp-table", yt_context.erp_input.path,
            "--input-prices-path", prices_path,
            "--output-check-path", yt_context.root + "/check",
            "--date", self._date
        ]
        if self._cur_ts:
            args.extend(["--timestamp", self._cur_ts])
        args = [self._env.CHECKS_BIN, "--cluster", self._cluster] + args
        logging.info("Run checks calculation: {}".format(" ".join(args)))
        subprocess.check_call(args, shell=False)

    def run_raw(self, args):
        args = [self._env.DYNAMIC_PRICING_BIN, "--cluster", self._cluster] + args
        logging.info("Run dynamic_pricing: {}".format(" ".join(args)))
        subprocess.check_call(args, shell=False, stderr=subprocess.STDOUT)

    def run(self, yt_context):
        args = [
            "--config-table", yt_context.config.path,
            "--input-erp-table", yt_context.erp_input.path,
            "--input-demand-table", yt_context.demand_input.path,
            "--output-prices-path", yt_context.root + "/prices",
            "--output-margins-path", yt_context.root + "/margins",
            "--date", self._date
        ]
        if self._cur_ts:
            args.extend(["--timestamp", self._cur_ts])

        self.run_raw(args)

    def get_tables(self):
        table_name = self._cur_ts if self._cur_ts else self._date
        return (
            "prices/" + table_name,
            "margins/" + table_name,
            "check/" + table_name,
            "stock_check/" + table_name
        )

