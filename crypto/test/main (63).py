import logging
import os

import mock
import yatest.common
from yt.wrapper import ypath

from crypta.lib.python import time_utils
from crypta.lib.python.smtp.test_helpers import mail_canonizers
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.services.disable_unused_segments import lib
from crypta.profile.services.disable_unused_segments.bin.test.proto import segment_pb2

logger = logging.getLogger(__name__)


EXPORT_ACTIVE_DISABLE_REQUESTED = segment_pb2.TExport(
    id="export-active-disable-requested",
    keywordId=549,
    segmentId=1,
    state="ACTIVE",
    nextActivityCheckTimestamp=1647892800,
)

EXPORT_ACTIVE_ACTIVE_ZEN = segment_pb2.TExport(
    id="export-active-active",
    keywordId=557,
    segmentId=2,
    state="ACTIVE",
    nextActivityCheckTimestamp=1647892800,
)

EXPORT_DISABLE_REQUESTED_ACTIVE = segment_pb2.TExport(
    id="export-disable-requested-active",
    keywordId=549,
    segmentId=3,
    state="DISABLE_REQUESTED",
    nextActivityCheckTimestamp=1647892800,
)

EXPORT_NEW = segment_pb2.TExport(
    id="export-new",
    keywordId=546,
    segmentId=4,
    state="ACTIVE",
    nextActivityCheckTimestamp=0,
)

EXPORT_ACTIVE_ACTIVE_DIRECT = segment_pb2.TExport(
    id="export-active-active-direct",
    keywordId=557,
    segmentId=5,
    state="ACTIVE",
    nextActivityCheckTimestamp=1647892800,
)

EXPORT_DISABLE_REQUESTED_DISABLE_REQUESTED = segment_pb2.TExport(
    id="export-disable-requested-disable-requested",
    keywordId=557,
    segmentId=6,
    state="DISABLE_REQUESTED",
    nextActivityCheckTimestamp=1648598401,
)

EXPORT_DISABLE_REQUESTED_DELETED = segment_pb2.TExport(
    id="export-disable-requested-deleted",
    keywordId=557,
    segmentId=7,
    state="DISABLE_REQUESTED",
    nextActivityCheckTimestamp=1647332549,
)

EXPORT_CRYPTA = segment_pb2.TExport(
    id="export-crypta",
    keywordId=557,
    segmentId=8,
    state="ACTIVE",
    nextActivityCheckTimestamp=1647892800,
)

EXPORT_PROLONGED = segment_pb2.TExport(
    id="export-prolonged",
    keywordId=549,
    segmentId=9,
    state="ACTIVE",
    nextActivityCheckTimestamp=1650560084,
)

EXPORT_DELETED = segment_pb2.TExport(
    id="export-deleted",
    keywordId=549,
    segmentId=10,
    state="DELETED",
    nextActivityCheckTimestamp=1647892800,
)

SEGMENT_ACTIVE_DISABLE_REQUESTED = segment_pb2.TSegment(
    Exports=[EXPORT_ACTIVE_DISABLE_REQUESTED],
    id="segment-1",
    author="author-1",
    responsibles=["author-1", "responsible-1"],
    stakeholders=["stakeholder-1", "responsible-1"],
)

SEGMENT_ACTIVE_ACTIVE_ZEN = segment_pb2.TSegment(
    Exports=[EXPORT_ACTIVE_ACTIVE_ZEN],
    id="segment-2",
    author="author-2",
    responsibles=["author-2", "responsible-2"],
    stakeholders=["stakeholder-2"],
)

SEGMENT_MULTI_EXPORT = segment_pb2.TSegment(
    Exports=[EXPORT_DISABLE_REQUESTED_ACTIVE, EXPORT_NEW],
    id="segment-3",
    author="author-3",
    responsibles=["author-3", "responsible-3"],
    stakeholders=["stakeholder-3"],
)

SEGMENT_ACTIVE_ACTIVE_DIRECT = segment_pb2.TSegment(
    Exports=[EXPORT_ACTIVE_ACTIVE_DIRECT],
    id="segment-5",
    author="author-5",
    responsibles=["author-5", "responsible-5"],
    stakeholders=["stakeholder-5"],
)

SEGMENT_EXPORT_DISABLE_REQUESTED_DISABLE_REQUESTED = segment_pb2.TSegment(
    Exports=[EXPORT_DISABLE_REQUESTED_DISABLE_REQUESTED],
    id="segment-6",
    author="author-6",
    responsibles=["author-6", "responsible-6"],
    stakeholders=["stakeholder-6"],
)

SEGMENT_DISABLE_REQUESTED_DELETED = segment_pb2.TSegment(
    Exports=[EXPORT_DISABLE_REQUESTED_DELETED],
    id="segment-7",
    author="author-7",
    responsibles=["author-7", "responsible-7"],
    stakeholders=["stakeholder-7"],
)

SEGMENT_CRYPTA = segment_pb2.TSegment(
    Exports=[EXPORT_CRYPTA],
    id="segment-8",
    author="author-8",
    responsibles=["author-8", "responsible-8"],
    stakeholders=["stakeholder-8"],
)

SEGMENT_PROLONGED = segment_pb2.TSegment(
    Exports=[EXPORT_PROLONGED],
    id="segment-9",
    author="author-9",
    responsibles=["author-9", "responsible-9"],
    stakeholders=["stakeholder-9"],
)

SEGMENT_DELETED = segment_pb2.TSegment(
    Exports=[EXPORT_DELETED],
    id="segment-10",
    author="author-10",
    responsibles=["author-10", "responsible-10"],
    stakeholders=["stakeholder-10"],
)

EXPORTS = {
    "export-active-disable-requested": EXPORT_ACTIVE_DISABLE_REQUESTED,
    "export-active-active": EXPORT_ACTIVE_ACTIVE_ZEN,
    "export-disable-requested-active": EXPORT_DISABLE_REQUESTED_ACTIVE,
    "export-new": EXPORT_NEW,
    "export-active-active-direct": EXPORT_ACTIVE_ACTIVE_DIRECT,
    "export-disable-requested-disable-requested": EXPORT_DISABLE_REQUESTED_DISABLE_REQUESTED,
    "export-disable-requested-deleted": EXPORT_DISABLE_REQUESTED_DELETED,
    "export-crypta": EXPORT_CRYPTA,
    "export-prolonged": EXPORT_PROLONGED,
    "export-deleted": EXPORT_DELETED,
}

SEGMENTS = {
    "segment-1": SEGMENT_ACTIVE_DISABLE_REQUESTED,
    "segment-2": SEGMENT_ACTIVE_ACTIVE_ZEN,
    "segment-3": SEGMENT_MULTI_EXPORT,
    "segment-5": SEGMENT_ACTIVE_ACTIVE_DIRECT,
    "segment-6": SEGMENT_EXPORT_DISABLE_REQUESTED_DISABLE_REQUESTED,
    "segment-7": SEGMENT_DISABLE_REQUESTED_DELETED,
    "segment-8": SEGMENT_CRYPTA,
    "segment-9": SEGMENT_PROLONGED,
    "segment-10": SEGMENT_DELETED,
}

EXPORT_TO_SEGMENT = {
    "export-active-disable-requested": SEGMENT_ACTIVE_DISABLE_REQUESTED,
    "export-active-active": SEGMENT_ACTIVE_ACTIVE_ZEN,
    "export-disable-requested-active": SEGMENT_MULTI_EXPORT,
    "export-new": SEGMENT_MULTI_EXPORT,
    "export-active-active-direct": SEGMENT_ACTIVE_ACTIVE_DIRECT,
    "export-disable-requested-disable-requested": SEGMENT_EXPORT_DISABLE_REQUESTED_DISABLE_REQUESTED,
    "export-disable-requested-deleted": SEGMENT_DISABLE_REQUESTED_DELETED,
    "export-crypta": SEGMENT_CRYPTA,
    "export-prolonged": SEGMENT_PROLONGED,
    "export-deleted": SEGMENT_DELETED,
}

DATE_1 = "2021-10-01"
DATE_2 = "2021-10-02"
DATE_3 = "2021-10-03"


def update_export_state(id, state):
    class MockApi:
        def __init__(self, export_id, state):
            self.export_id = export_id
            self.state = state

        def result(self):
            EXPORTS[self.export_id].state = self.state

    return MockApi(id, state)


def delete_segment_export(id):
    return update_export_state(id, "DELETED")


def update_export_next_activity_check_timestamp(id, timestamp):
    class MockApi:
        def __init__(self, export_id, timestamp):
            self.export_id = export_id
            self.timestamp = timestamp

        def result(self):
            EXPORTS[self.export_id].nextActivityCheckTimestamp = int(self.timestamp)

    return MockApi(id, timestamp)


def get_segment_by_export_id(id):
    class MockApi:
        def __init__(self, export_id):
            self.export_id = export_id

        def result(self):
            return EXPORT_TO_SEGMENT[self.export_id]

    return MockApi(id)


def get_segment(id):
    class MockApi:
        def __init__(self, segment_id):
            self.segment_id = segment_id

        def result(self):
            return SEGMENTS[self.segment_id]

    return MockApi(id)


def get_exports_with_rules():
    class MockApi:
        def __init__(self):
            pass

        def result(self):
            return [EXPORTS[key] for key in EXPORTS]

    return MockApi()


def get_parents_per_segment():
    class MockApi:
        def __init__(self):
            pass

        def result(self):
            return {
                "segment-1": ["root-users"],
                "segment-2": ["root-users"],
                "segment-3": ["root-users"],
                "segment-5": ["root-users"],
                "segment-6": ["root-users"],
                "segment-7": ["root-users"],
                "segment-8": ["group-1", "root-crypta"],
                "segment-9": ["root-users"],
                "segment-10": ["root-users"],
            }

    return MockApi()


def test_basic(local_yt, local_yt_and_yql_env, local_smtp_server, config):
    os.environ.update(local_yt_and_yql_env)
    os.environ[time_utils.CRYPTA_FROZEN_TIME_ENV] = "1648598400"

    mock_api = mock.MagicMock(name="api")
    mock_api.lab.deleteSegmentExport.side_effect = delete_segment_export
    mock_api.lab.updateExportState.side_effect = update_export_state
    mock_api.lab.updateExportNextActivityCheckTs.side_effect = update_export_next_activity_check_timestamp
    mock_api.lab.getSegmentByExportId.side_effect = get_segment_by_export_id
    mock_api.lab.getSegment.side_effect = get_segment
    mock_api.lab.getExportsWithRuleId.side_effect = get_exports_with_rules
    mock_api.lab.getParentsPerSegment.side_effect = get_parents_per_segment

    output_files = [
        "mail_0.txt",
        "mail_1.txt",
        "mail_2.txt",
    ]

    with mock.patch("crypta.profile.services.disable_unused_segments.lib.swagger.swagger") as mock_get_api, local_smtp_server:
        mock_get_api.return_value = mock_api

        tests.yt_test_func(
            yt_client=local_yt.get_yt_client(),
            func=lambda: lib.run(config, logger),
            data_path=yatest.common.test_source_path("data"),
            input_tables=[
                (tables.get_yson_table_with_schema("segments_simple.yson",
                                                   config.SegmentsSimpleTable,
                                                   get_segments_simple_schema()),
                 [tests.TableIsNotChanged()]),

                (tables.get_yson_table_with_schema("2021-10-01.yson",
                                                   ypath.ypath_join(config.SegmentsStatsDir, DATE_1),
                                                   get_segment_stats_schema()),
                 [tests.TableIsNotChanged()]),
            ],
        )

        assert EXPORT_ACTIVE_DISABLE_REQUESTED.state == "DISABLE_REQUESTED"
        assert EXPORT_ACTIVE_DISABLE_REQUESTED.nextActivityCheckTimestamp == 1649808000
        assert EXPORT_ACTIVE_ACTIVE_ZEN.state == "ACTIVE"
        assert EXPORT_ACTIVE_ACTIVE_ZEN.nextActivityCheckTimestamp == 1648684800
        assert EXPORT_DISABLE_REQUESTED_ACTIVE.state == "ACTIVE"
        assert EXPORT_DISABLE_REQUESTED_ACTIVE.nextActivityCheckTimestamp == 1648684800
        assert EXPORT_NEW.state == "ACTIVE"
        assert EXPORT_NEW.nextActivityCheckTimestamp == 1648684800
        assert EXPORT_ACTIVE_ACTIVE_DIRECT.state == "ACTIVE"
        assert EXPORT_ACTIVE_ACTIVE_DIRECT.nextActivityCheckTimestamp == 1648684800
        assert EXPORT_DISABLE_REQUESTED_DISABLE_REQUESTED.state == "DISABLE_REQUESTED"
        assert EXPORT_DISABLE_REQUESTED_DISABLE_REQUESTED.nextActivityCheckTimestamp == 1648598401
        assert EXPORT_DISABLE_REQUESTED_DELETED.state == "DELETED"
        assert EXPORT_DISABLE_REQUESTED_DELETED.nextActivityCheckTimestamp == 1647332549
        assert EXPORT_CRYPTA.state == "ACTIVE"
        assert EXPORT_CRYPTA.nextActivityCheckTimestamp == 1647892800
        assert EXPORT_PROLONGED.state == "ACTIVE"
        assert EXPORT_PROLONGED.nextActivityCheckTimestamp == 1650560084
        assert EXPORT_DELETED.nextActivityCheckTimestamp == 1647892800
        assert EXPORT_DELETED.state == "DELETED"
        assert not os.path.exists("mail_3.txt")

        return mail_canonizers.canonize_mails_by_addressee(output_files)


def get_segment_stats_schema():
    return schema_utils.get_strict_schema([
        {"name": "SegmentID", "type": "uint64", "required": False},
        {"name": "KeywordID", "type": "uint64", "required": False},
        {"name": "DirectMultipliersCommercialCampaignsCount", "type": "uint64", "required": True},
        {"name": "DirectMultipliersNoncommercialCampaignsCount", "type": "uint64", "required": True},
        {"name": "DirectRetargetingCommercialCampaignsCount", "type": "uint64", "required": True},
        {"name": "DirectRetargetingNoncommercialCampaignsCount", "type": "uint64", "required": True},
        {"name": "AdfoxShows", "type": "uint64", "required": True},
        {"name": "DisplayShows", "type": "uint64", "required": True},
        {"name": "CreateDate", "type": "string", "required": False},
    ])


def get_segments_simple_schema():
    return schema_utils.get_strict_schema([
        {"name": "id", "type": "uint64", "required": False},
        {"name": "zen_retargeting", "type": "uint64", "required": False},
    ])
