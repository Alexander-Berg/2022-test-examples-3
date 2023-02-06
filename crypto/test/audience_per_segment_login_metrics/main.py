import yatest.common
from yt.wrapper import ypath

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


def test_basic(yt_stuff, mock_solomon_server, config_file, config):
    dates = ("2020-01-01", "2020-01-02", "2020-01-03")

    output_files = tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/buchhalter/services/main/bin/crypta-buchhalter"),
        args=[
            "--config", config_file,
            "audience_per_segment_login_metrics",
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.YsonTable("dmp_{}.yson".format(date), ypath.ypath_join(config.DmpPerSegmentLoginStats.SourceDir, date)), [tests.TableIsNotChanged()])
            for date in dates
        ] + [
            (tables.YsonTable("sdmp_{}.yson".format(date), ypath.ypath_join(config.SdmpPerSegmentLoginStats.SourceDir, date)), [tests.TableIsNotChanged()])
            for date in dates
        ] + [
            (tables.YsonTable("dmp_track.yson", config.DmpPerSegmentLoginStats.TrackTable), []),
            (tables.YsonTable("sdmp_track.yson", config.SdmpPerSegmentLoginStats.TrackTable), []),
            (tables.YsonTable("dmp_index.yson", config.DmpIndexTable), [tests.TableIsNotChanged()]),
            (tables.YsonTable("sdmp_index.yson", config.SdmpIndexTable), [tests.TableIsNotChanged()]),
        ],
        output_tables=[
            (tables.YsonTable("output_dmp_track.yson", config.DmpPerSegmentLoginStats.TrackTable), [tests.Diff()]),
            (tables.YsonTable("output_sdmp_track.yson", config.SdmpPerSegmentLoginStats.TrackTable), [tests.Diff()]),
        ],
    )

    solomon_requests = [_serialize_solomon_request(request) for request in mock_solomon_server.dump_push_requests()]
    solomon_requests.sort()

    return output_files, solomon_requests


def _serialize_solomon_request(request):
    parts = request["cluster"] + request["project"] + request["service"] + [request["sensors"][0]["labels"]["segment_owner"], request["sensors"][0]["labels"]["sensor"]]
    return ".".join(parts), request["sensors"][0]["value"]
