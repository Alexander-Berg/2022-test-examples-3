from calculate import run_local
import yatest.common


class Args():
    def __init__(self):
        self.aspect = "sinsig"
        self.nojudgements = True
        self.aggregate = True
        self.serps = False
        self.yt_proxy = "localhost"
        self.raw_serps_table_path = None
        self.batch_size = 10
        self.convert_to_metrics_format = False


def test_over_serpset_30549985():
    dst = yatest.common.test_output_path("result.json_lines")
    run_local(
        Args(),
        input_file=open("serpset_30549985_100rows.json_lines"),
        output_file=open(dst, "w"),
    )
    return yatest.common.canonical_file(dst, local=True)
