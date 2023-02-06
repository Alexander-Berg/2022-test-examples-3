from unittest import TestCase

from extsearch.video.ugc.sqs_moderation.clients.db_client import (
    ModerationStatus, ModerationInfoField
)
from extsearch.video.ugc.sqs_moderation.config_utils.models import (
    ModerationConfig, FieldConfig, SendMode, CheckMode
)
from extsearch.video.ugc.sqs_moderation.clients.moderation.configurable_provider import get_translator
from extsearch.video.ugc.sqs_moderation.clients.moderation.const import COMPLEX_FIELDNAME
from extsearch.video.ugc.sqs_moderation.clients.moderation.processor import (
    ModerationProcessor,
    ModerationModifier
)

FIELD1 = "F1"
FIELD2 = "F2"
FIELD3 = "F3"
FIELD4 = "F4"
REQUIRED_MODERATIONS_123 = {FIELD1, FIELD2, FIELD3}


class TestWhitelist(TestCase):
    def setUp(self) -> None:
        self.translator = get_translator(ModerationConfig(
            require_published=False,
            ignore_short_videos=False,
            fields={
                COMPLEX_FIELDNAME: FieldConfig(
                    dest=[],
                    send=SendMode.ON_CHANGE,
                    check=CheckMode.REQUIRE,
                    only_verdicts=None,
                    ignore_verdicts=None
                )
            }
        ))

    def test_manual(self):
        verdicts = {
            "zen_unknown": ModerationStatus.manual_check
        }
        processor = ModerationProcessor(None, set(),
                                        verdicts_translator=self.translator)
        self.assertEqual(
            processor.field_status(COMPLEX_FIELDNAME,
                                   ModerationInfoField(ModerationStatus.partial_success, verdicts=verdicts),
                                   finished=True, modifiers=set()),
            ModerationStatus.manual_check,
            "Manual check must be keeped"
        )

    def test_whitelist(self):
        # for zen verdicts statuses are inverted :`(
        verdicts_1 = {
            "zen_other_ban": ModerationStatus.fail,  # actually it means success
            "zen_hard_copypaste": ModerationStatus.success,  # actually it means fail
        }
        processor = ModerationProcessor(None, REQUIRED_MODERATIONS_123,
                                        verdicts_translator=self.translator)
        self.assertEqual(
            processor.field_status(COMPLEX_FIELDNAME,
                                   ModerationInfoField(ModerationStatus.partial_success, verdicts=verdicts_1),
                                   finished=True, modifiers=set()),
            ModerationStatus.fail,
            "No modifiers, must be failed"
        )
        self.assertEqual(
            processor.field_status(COMPLEX_FIELDNAME,
                                   ModerationInfoField(ModerationStatus.partial_success, verdicts=verdicts_1),
                                   finished=True, modifiers={ModerationModifier.COPYRIGHT_HOLDER}),
            ModerationStatus.success,
            "Whitelist modifier, must be success"
        )

        verdicts_2 = {
            "zen_other_ban": ModerationStatus.success,  # actually it means fail
            "zen_hard_copypaste": ModerationStatus.success,  # actually it means fail
        }
        self.assertEqual(
            processor.field_status(COMPLEX_FIELDNAME,
                                   ModerationInfoField(ModerationStatus.partial_success, verdicts=verdicts_2),
                                   finished=True, modifiers={ModerationModifier.COPYRIGHT_HOLDER}),
            ModerationStatus.fail,
            "Non-whitelist verdict fails, result must be failed even with modifier"
        )
