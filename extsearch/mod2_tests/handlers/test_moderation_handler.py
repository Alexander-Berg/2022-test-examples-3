from contextvars import ContextVar
from datetime import datetime
from unittest import TestCase
from unittest.mock import Mock

from ..mock.client_manager import ClientManagerMock

from extsearch.video.ugc.sqs_moderation.clients.db_client import (
    ModerationStatus, ModerationInfoField,
    VideoInfo, VideoMeta, Privacy, SERVICE_UGC
)
from extsearch.video.ugc.sqs_moderation.clients.moderation.const import (
    TITLE_FIELDNAME, DESC_FIELDNAME, CLASSIFIERS_FIELDNAME
)
from extsearch.video.ugc.sqs_moderation.mod2.handlers.moderation_handler import (
    ModerationHandler
)

id_var = ContextVar("id_var")
OTHER_VALUE = "***"
META_FIELD_VALUE = "meta field"
FILE_FIELD_VALUE = "file field"


class TestHandlerField(TestCase):
    def setUp(self) -> None:

        moderation_provider = Mock()
        moderation_provider.meta_value_getter = Mock(return_value=META_FIELD_VALUE)
        moderation_provider.file_value_getter = Mock(return_value=0)

        client_manager = ClientManagerMock()
        client_manager.make_moderation_provider = Mock(return_value=moderation_provider)

        self.handler = ModerationHandler(
            client_manager,
            "",
            {},
            id_var,
            Mock(),
        )
        self.send_field = Mock()
        self.handler.send_field_moderation = self.send_field
        self.file = VideoInfo(
            id=1,
            service=SERVICE_UGC,
            service_id="service_id",
            source_url="",
            moderation_info={},
            moderation_status=ModerationStatus.fail,
            transcoder_status="",
            transcoder_info={"SignaturesStatusStr": "ESSFail"},
            experiment_transcoder_info={"test_exp": {"SignaturesStatusStr": "ESSFail"}},
            transcoder_task_info=None,
            transcoder_quota="default",
            transcoder_params={"Graph": "regular"},
            deleted=False,
            create_time=datetime.now(),
            update_time=datetime.now().isoformat(),
        )
        self.meta = VideoMeta(
            id=2,
            channel_id=0,
            video_file_id=1,
            video_stream_id=None,
            title="title1",
            thumbnail="thumb1",
            description="desc1",
            moderation_info={},
            moderation_status=ModerationStatus.fail,
            deleted=False,
            update_time="datetime.now()",
            privacy=Privacy.PUBLIC,
            release_date="release",
        )

    def test_new_field(self):
        res = self.handler.process_field(self.file, self.meta, TITLE_FIELDNAME, {}, False)
        self.assertEqual(res, {TITLE_FIELDNAME})
        expected_field = ModerationInfoField(None, META_FIELD_VALUE, META_FIELD_VALUE, {}, {})
        self.assertEqual(self.meta.moderation_info, {TITLE_FIELDNAME: expected_field})
        self.send_field.assert_called()

    def test_existing_other_success(self):
        old_field = ModerationInfoField(ModerationStatus.success, OTHER_VALUE, OTHER_VALUE, {}, {})
        self.meta.moderation_info = {TITLE_FIELDNAME: old_field}
        res = self.handler.process_field(self.file, self.meta, TITLE_FIELDNAME, {}, False)
        self.assertEqual(res, {TITLE_FIELDNAME})
        expected_field = ModerationInfoField(ModerationStatus.old_success, META_FIELD_VALUE, META_FIELD_VALUE, {}, {})
        self.assertEqual(self.meta.moderation_info, {TITLE_FIELDNAME: expected_field})
        self.send_field.assert_called()

    def test_existing_other_fail(self):
        old_field = ModerationInfoField(ModerationStatus.fail, OTHER_VALUE, OTHER_VALUE, {}, {})
        self.meta.moderation_info = {TITLE_FIELDNAME: old_field}
        res = self.handler.process_field(self.file, self.meta, TITLE_FIELDNAME, {}, False)
        self.assertEqual(res, {TITLE_FIELDNAME})
        expected_field = ModerationInfoField(ModerationStatus.old_fail, META_FIELD_VALUE, META_FIELD_VALUE, {}, {})
        self.assertEqual(self.meta.moderation_info, {TITLE_FIELDNAME: expected_field})
        self.send_field.assert_called()

    def test_two_fields(self):
        old_field = ModerationInfoField(None, OTHER_VALUE, OTHER_VALUE, {}, {})
        other_field = ModerationInfoField(None, OTHER_VALUE, OTHER_VALUE, {}, {})
        self.meta.moderation_info = {TITLE_FIELDNAME: old_field, DESC_FIELDNAME: other_field}
        res = self.handler.process_field(self.file, self.meta, TITLE_FIELDNAME, {}, False)
        self.assertEqual(res, {TITLE_FIELDNAME})
        expected_field = ModerationInfoField(None, META_FIELD_VALUE, META_FIELD_VALUE, {}, {})
        self.assertEqual(self.meta.moderation_info, {TITLE_FIELDNAME: expected_field, DESC_FIELDNAME: other_field})
        self.send_field.assert_called()

    def test_exising_same_value(self):
        old_field = ModerationInfoField(None, META_FIELD_VALUE, META_FIELD_VALUE, {}, {})
        self.meta.moderation_info = {TITLE_FIELDNAME: old_field}
        res = self.handler.process_field(self.file, self.meta, TITLE_FIELDNAME, {}, False)
        self.assertEqual(res, set())
        expected_field = ModerationInfoField(None, META_FIELD_VALUE, META_FIELD_VALUE, {}, {})
        self.assertEqual(self.meta.moderation_info, {TITLE_FIELDNAME: expected_field})
        self.send_field.assert_not_called()

    def test_reset_same_value(self):
        old_field = ModerationInfoField(None, META_FIELD_VALUE, META_FIELD_VALUE, {}, {})
        self.meta.moderation_info = {TITLE_FIELDNAME: old_field}
        res = self.handler.process_field(self.file, self.meta, TITLE_FIELDNAME, {}, True)
        self.assertEqual(res, set())
        expected_field = ModerationInfoField(None, META_FIELD_VALUE, META_FIELD_VALUE, {}, {})
        self.assertEqual(self.meta.moderation_info, {TITLE_FIELDNAME: expected_field})
        self.send_field.assert_not_called()

    def test_reset_other_value(self):
        old_field = ModerationInfoField(None, OTHER_VALUE, OTHER_VALUE, {}, {})
        self.meta.moderation_info = {TITLE_FIELDNAME: old_field}
        res = self.handler.process_field(self.file, self.meta, TITLE_FIELDNAME, {}, True)
        self.assertEqual(res, set())
        self.assertEqual(self.meta.moderation_info, {})
        self.send_field.assert_not_called()

    def test_reset_absent(self):
        self.meta.moderation_info = {}
        res = self.handler.process_field(self.file, self.meta, TITLE_FIELDNAME, {}, True)
        self.assertEqual(res, set())
        self.assertEqual(self.meta.moderation_info, {})
        self.send_field.assert_not_called()

    def test_ignore_signature_errors(self):
        res = self.handler.process_field(self.file, self.meta, CLASSIFIERS_FIELDNAME, {}, False, True)
        self.assertEqual(res, {CLASSIFIERS_FIELDNAME})
        self.assertEqual(self.file.moderation_info[CLASSIFIERS_FIELDNAME].status, ModerationStatus.success)
        self.send_field.assert_not_called()
