from search.geo.tools.geocube.lib.common import CalculateTable
from search.geo.tools.geocube.lib.commands import TCommand, TDoCalcCommand, TGetAvailableDatesCommand
from search.geo.tools.geocube.lib.runner import TRunner


class TTestCommandWrapper(TCommand):

    def __init__(self, command):
        self.command = command

    @property
    def command_name(self):
        return self.command.command_name

    def setup_args_parser(self, parser):
        self.command.setup_args_parser(parser)

    @staticmethod
    def run(args):
        return args


def test_do_calc_command():
    all_commands = [
        TTestCommandWrapper(TDoCalcCommand())
    ]

    args = TRunner(
        all_commands, [
            "do_calc",
            "--start-date", "1951-03-09",
            "--final-date", "1951-03-10",
            "--yt-pool", "yt-pool",
            "--cluster", "hahn",
            "--gc-table-prefix", "//home/test/result",
            "--us-table-prefix", "//home/test/user_sessions",
            "--mr-table-prefix", "//home/test/mapreqans",
            "--mm-table-prefix", "//home/test/mmetrika",
            "--mm-cache-prefix", "//home/test/mmetrics_cache",
            "--br-table-prefix", "//home/test/cooked-bebr-log",
            "--af-table-prefix", "//home/test/antifraud/export/uid_types/daily",
            "--sort-results-by", "serpid",
            "--sort-results-by", "reqid",
            "--calculate-table", "serp",
            "--recalc-mmetrika",
        ]
    ).run()

    assert args.start_date == "1951-03-09"
    assert args.final_date == "1951-03-10"
    assert args.yt_pool == "yt-pool"
    assert args.cluster == "hahn"
    assert args.gc_table_prefix == "//home/test/result"
    assert args.us_table_prefix == "//home/test/user_sessions"
    assert args.mr_table_prefix == "//home/test/mapreqans"
    assert args.mm_table_prefix == "//home/test/mmetrika"
    assert args.mm_cache_prefix == "//home/test/mmetrics_cache"
    assert args.br_table_prefix == "//home/test/cooked-bebr-log"
    assert args.af_table_prefix == "//home/test/antifraud/export/uid_types/daily"
    assert args.sort_results_by == ["serpid", "reqid"]
    assert args.calculate_table == CalculateTable.Serp
    assert args.recalc_mmetrika is True
    assert args.recalculate_all is False
    assert args.compression_max is False


def test_get_available_dates_command():
    all_commands = [
        TTestCommandWrapper(TGetAvailableDatesCommand())
    ]

    args = TRunner(
        all_commands, [
            "get_available_dates",
            "--start-date", "1951-03-09",
            "--cluster", "hahn",
            "--gc-table-prefix", "//home/test/result",
            "--us-table-prefix", "//home/test/user_sessions",
            "--mr-table-prefix", "//home/test/mapreqans",
            "--mm-table-prefix", "//home/test/mmetrika",
            "--br-table-prefix", "//home/test/cooked-bebr-log",
            "--af-table-prefix", "//home/test/antifraud/export/uid_types/daily",
            "--calculate-table", "maps",
        ]
    ).run()

    assert args.start_date == "1951-03-09"
    assert args.cluster == "hahn"
    assert args.gc_table_prefix == "//home/test/result"
    assert args.us_table_prefix == "//home/test/user_sessions"
    assert args.mr_table_prefix == "//home/test/mapreqans"
    assert args.mm_table_prefix == "//home/test/mmetrika"
    assert args.br_table_prefix == "//home/test/cooked-bebr-log"
    assert args.af_table_prefix == "//home/test/antifraud/export/uid_types/daily"
    assert args.calculate_table == CalculateTable.Maps
