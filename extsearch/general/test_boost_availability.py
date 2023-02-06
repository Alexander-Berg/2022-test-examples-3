import os
import yatest
import yt.wrapper as yt


class Resource:
    def __init__(self, yson_filepath):
        self.yson_filepath = yson_filepath


def test():
    availability_stats = Resource("availability_stats")
    proxy = os.getenv("YT_PROXY")
    yt_client = yt.YtClient(proxy=proxy)
    yt_client.create("table", path="//tmp/availability_stats", recursive=True)
    with open(yatest.common.work_path(availability_stats.yson_filepath), 'rb') as src:
        yt_client.write_table("//tmp/availability_stats", src.read(), format=yt.YsonFormat(), raw=True)
    out_path = yatest.common.output_path("availability_output.txt")
    yatest.common.execute(
        [
            yatest.common.binary_path('extsearch/geo/tools/similar_orgs/calc_similar_orgs/calc_similar_orgs'),
            "-t",
            "20",
            "--geobase",
            yatest.common.binary_path('geobase/data/v6/geodata6.bin'),
            "--model",
            yatest.common.binary_path('extsearch/geo/tools/similar_orgs/calc_similar_orgs/test/model/matrixnet.info'),
            "--server",
            proxy,
            "--wizard-boost-size",
            "10",
            "--hotel-availabilities-table",
            "//tmp/availability_stats",
            yatest.common.build_path("extsearch/geo/tests_data/business_index/"),
        ],
        stdin=open(
            yatest.common.source_path(
                "extsearch/geo/tools/similar_orgs/calc_similar_orgs/test/similar_orgs_hypotheses.txt"
            )
        ),
        stdout=open(out_path, "w"),
    )
    return yatest.common.canonical_file(out_path, local=True)
