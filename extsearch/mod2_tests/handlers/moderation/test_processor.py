from unittest import TestCase

from extsearch.video.ugc.sqs_moderation.clients.db_client import (
    ModerationStatus, ModerationInfo, ModerationInfoField
)
from extsearch.video.ugc.sqs_moderation.clients.moderation.processor import (
    ModerationProcessor
)

FIELD1 = "F1"
FIELD2 = "F2"
FIELD3 = "F3"
FIELD4 = "F4"
REQUIRED_MODERATIONS_123 = {FIELD1, FIELD2, FIELD3}


class TestMultipleFieldsModeration(TestCase):
    def setUp(self):
        self.processor = ModerationProcessor(None, REQUIRED_MODERATIONS_123)

    def test_empty(self):
        self.assertEqual(self.processor.moderation_status({}), None)

    def test_no_required(self):
        processor = ModerationProcessor(None, set())
        self.assertEqual(processor.moderation_status({}), ModerationStatus.success)

    def test_partial(self):
        info: ModerationInfo = {
            FIELD1: ModerationInfoField(ModerationStatus.success)
        }
        self.assertEqual(self.processor.moderation_status(info), ModerationStatus.partial_success)

        info: ModerationInfo = {
            FIELD1: ModerationInfoField(ModerationStatus.success),
            FIELD2: ModerationInfoField(ModerationStatus.success),
            FIELD3: ModerationInfoField(ModerationStatus.partial_success),
        }
        self.assertEqual(self.processor.moderation_status(info), ModerationStatus.partial_success)

        info: ModerationInfo = {
            FIELD1: ModerationInfoField(ModerationStatus.success),
            FIELD2: ModerationInfoField(ModerationStatus.success),
            FIELD3: ModerationInfoField(None),
        }
        self.assertEqual(self.processor.moderation_status(info), ModerationStatus.partial_success)

        info: ModerationInfo = {
            FIELD1: ModerationInfoField(ModerationStatus.success),
            FIELD2: ModerationInfoField(ModerationStatus.success),
            FIELD4: ModerationInfoField(ModerationStatus.success),
        }
        self.assertEqual(self.processor.moderation_status(info), ModerationStatus.partial_success)

    def test_fail(self):
        info: ModerationInfo = {
            FIELD1: ModerationInfoField(ModerationStatus.success),
            FIELD2: ModerationInfoField(ModerationStatus.fail)
        }
        self.assertEqual(self.processor.moderation_status(info), ModerationStatus.fail)

    def test_manual_req_done(self):
        info: ModerationInfo = {
            FIELD1: ModerationInfoField(ModerationStatus.success),
            FIELD2: ModerationInfoField(ModerationStatus.success),
            FIELD3: ModerationInfoField(ModerationStatus.success),
            FIELD4: ModerationInfoField(ModerationStatus.manual_check)
        }
        self.assertEqual(self.processor.moderation_status(info), ModerationStatus.manual_check)

    def test_manual(self):
        info: ModerationInfo = {
            FIELD1: ModerationInfoField(ModerationStatus.manual_check)
        }
        self.assertEqual(self.processor.moderation_status(info), ModerationStatus.manual_check)

    def test_success(self):
        info: ModerationInfo = {
            FIELD1: ModerationInfoField(ModerationStatus.success),
            FIELD2: ModerationInfoField(ModerationStatus.success),
            FIELD3: ModerationInfoField(ModerationStatus.success),
        }
        self.assertEqual(self.processor.moderation_status(info), ModerationStatus.success)

    def test_old_success(self):
        info: ModerationInfo = {
            FIELD1: ModerationInfoField(ModerationStatus.old_success),
            FIELD2: ModerationInfoField(ModerationStatus.success),
            FIELD3: ModerationInfoField(ModerationStatus.success),
        }
        self.assertEqual(self.processor.moderation_status(info), ModerationStatus.success)

    def test_old_fail(self):
        info: ModerationInfo = {
            FIELD1: ModerationInfoField(ModerationStatus.old_fail),
            FIELD2: ModerationInfoField(ModerationStatus.success),
            FIELD3: ModerationInfoField(ModerationStatus.success),
        }
        self.assertEqual(self.processor.moderation_status(info), ModerationStatus.old_fail)

    def test_old_success_verdicts(self):
        info: ModerationInfo = {
            FIELD1: ModerationInfoField(ModerationStatus.old_success, verdicts={}),
            FIELD2: ModerationInfoField(ModerationStatus.success),
            FIELD3: ModerationInfoField(ModerationStatus.success),
        }
        self.assertEqual(self.processor.moderation_status(info), ModerationStatus.success)

    def test_old_fail_verdicts(self):
        info: ModerationInfo = {
            FIELD1: ModerationInfoField(ModerationStatus.old_fail, verdicts={}),
            FIELD2: ModerationInfoField(ModerationStatus.success),
            FIELD3: ModerationInfoField(ModerationStatus.success),
        }
        self.assertEqual(self.processor.moderation_status(info), ModerationStatus.old_fail)


class TestFieldModeration(TestCase):
    def setUp(self):
        self.processor = ModerationProcessor(None, REQUIRED_MODERATIONS_123)

    def test_no_verdicts_finished(self):
        """Compat mode: Finished field ignored if None verdicts"""
        self.assertEqual(
            self.processor.field_status("", ModerationInfoField(ModerationStatus.success), finished=True),
            ModerationStatus.success
        )
        self.assertEqual(
            self.processor.field_status("", ModerationInfoField(ModerationStatus.fail), finished=True),
            ModerationStatus.fail
        )
        self.assertEqual(
            self.processor.field_status("", ModerationInfoField(ModerationStatus.partial_success), finished=True),
            ModerationStatus.partial_success
        )

    def test_no_verdicts(self):
        """Compat mode: Finished field ignored if None verdicts"""
        self.assertEqual(
            self.processor.field_status("", ModerationInfoField(ModerationStatus.success), finished=False),
            ModerationStatus.success
        )
        self.assertEqual(
            self.processor.field_status("", ModerationInfoField(ModerationStatus.fail), finished=False),
            ModerationStatus.fail
        )
        self.assertEqual(
            self.processor.field_status("", ModerationInfoField(ModerationStatus.partial_success), finished=False),
            ModerationStatus.partial_success
        )

    def test_has_failes(self):
        verdicts = {
            "xxx": ModerationStatus.fail,
            "yyy": ModerationStatus.success,
        }
        self.assertEqual(
            self.processor.field_status("",
                                        ModerationInfoField(ModerationStatus.success, verdicts=verdicts),
                                        finished=False),
            ModerationStatus.fail
        )
        self.assertEqual(
            self.processor.field_status("",
                                        ModerationInfoField(ModerationStatus.success, verdicts=verdicts),
                                        finished=True),
            ModerationStatus.fail
        )

    def test_success(self):
        verdicts = {
            "yyy": ModerationStatus.success,
            "zzz": ModerationStatus.partial_success,
        }
        self.assertEqual(
            self.processor.field_status("",
                                        ModerationInfoField(ModerationStatus.success, verdicts=verdicts),
                                        finished=False),
            ModerationStatus.success,
            "Already finished, though not finished again"
        )
        self.assertEqual(
            self.processor.field_status("",
                                        ModerationInfoField(ModerationStatus.partial_success, verdicts=verdicts),
                                        finished=False),
            ModerationStatus.partial_success,
            "Not finished before, not finished now"
        )
        self.assertEqual(
            self.processor.field_status("",
                                        ModerationInfoField(ModerationStatus.success, verdicts=verdicts),
                                        finished=True),
            ModerationStatus.success,
            "Finished before, finished again"
        )
        self.assertEqual(
            self.processor.field_status("",
                                        ModerationInfoField(ModerationStatus.partial_success, verdicts=verdicts),
                                        finished=True),
            ModerationStatus.success,
            "Not finished before, but finished now"
        )


def simple_value_getter(obj, field):
    return field


class TestFieldUpdate(TestCase):
    def setUp(self):
        self.processor = ModerationProcessor(simple_value_getter, REQUIRED_MODERATIONS_123)

    def test_nothing_no_verdicts_fail(self):
        """Compat mode: Nothing before, None verdicts. Used status """
        prev_info: ModerationInfo = {}
        info: ModerationInfo = self.processor.add_verdicts(
            None, prev_info,
            FIELD1, FIELD1, ModerationStatus.fail, None, None,
        )
        expected = ModerationInfoField(ModerationStatus.fail, FIELD1, FIELD1, None)
        self.assertEqual(info, {FIELD1: expected})

    def test_nothing_verdicts(self):
        """Nothing before, some verdicts. Status ignored """
        prev_info: ModerationInfo = {}
        info: ModerationInfo = self.processor.add_verdicts(
            None, prev_info,
            FIELD1, FIELD1, None, {"xxx": ModerationStatus.fail}, None,
        )
        expected = ModerationInfoField(
            ModerationStatus.fail,
            FIELD1,
            FIELD1,
            {"xxx": ModerationStatus.fail},
            {"xxx": ModerationStatus.fail},
        )
        self.assertEqual(info, {FIELD1: expected})

    def test_status_translator(self):
        """ Changing effective status """
        processor = ModerationProcessor(simple_value_getter, set(), lambda a, b, c, d: ModerationStatus.success)

        prev_info: ModerationInfo = {}
        info: ModerationInfo = processor.add_verdicts(
            None, prev_info,
            FIELD1, FIELD1, None, {"xxx": ModerationStatus.fail}, None,
        )
        expected = ModerationInfoField(
            ModerationStatus.success,
            FIELD1,
            FIELD1,
            {"xxx": ModerationStatus.fail},
            {"xxx": ModerationStatus.success},
        )
        self.assertEqual(info, {FIELD1: expected})

    def test_nothing_empty_verdicts(self):
        """Nothing before, empty verdicts. Status ignored """
        prev_info: ModerationInfo = {}
        info: ModerationInfo = self.processor.add_verdicts(
            None, prev_info,
            FIELD1, FIELD1, ModerationStatus.fail, {}, True,
        )
        expected = ModerationInfoField(ModerationStatus.partial_success, FIELD1, FIELD1, {}, {})
        self.assertEqual(info, {FIELD1: expected})

    def test_add_verdicts(self):
        """Field was before, add new verdicts. Status ignored"""
        prev_info: ModerationInfo = {
            FIELD1: ModerationInfoField(ModerationStatus.partial_success, FIELD1, FIELD1, {})
        }
        info: ModerationInfo = self.processor.add_verdicts(
            None, prev_info,
            FIELD1, FIELD1, ModerationStatus.fail, {"xxx": ModerationStatus.success}, True,
        )
        expected = ModerationInfoField(
            ModerationStatus.partial_success,
            FIELD1,
            FIELD1,
            {"xxx": ModerationStatus.success},
            {"xxx": ModerationStatus.success},
        )
        self.assertEqual(info, {FIELD1: expected})

    def test_replace_verdicts(self):
        """Field was before, add new verdicts. Status ignored"""
        prev_info: ModerationInfo = {
            FIELD1: ModerationInfoField(ModerationStatus.partial_fail, FIELD1, FIELD1, {"xxx": ModerationStatus.error})
        }
        info: ModerationInfo = self.processor.add_verdicts(
            None, prev_info,
            FIELD1, FIELD1, ModerationStatus.fail, {"xxx": ModerationStatus.success}, True,
        )
        expected = ModerationInfoField(
            ModerationStatus.partial_success,
            FIELD1,
            FIELD1,
            {"xxx": ModerationStatus.success},
            {"xxx": ModerationStatus.success},
        )
        self.assertEqual(info, {FIELD1: expected})

    def test_finish(self):
        """Field was before, no new verdicts, just finish. Status ignored"""
        prev_info: ModerationInfo = {
            FIELD1: ModerationInfoField(ModerationStatus.partial_success, FIELD1, FIELD1,
                                        {"xxx": ModerationStatus.success})
        }
        info: ModerationInfo = self.processor.add_verdicts(
            None, prev_info,
            FIELD1, FIELD1, ModerationStatus.fail, {}, False,
        )
        expected = ModerationInfoField(
            ModerationStatus.success,
            FIELD1,
            FIELD1,
            {"xxx": ModerationStatus.success},
            {"xxx": ModerationStatus.success},
        )
        self.assertEqual(info, {FIELD1: expected})

    def test_finished_add_verdicts(self):
        """Field was before, add new verdicts. Status ignored"""
        prev_info: ModerationInfo = {
            FIELD1: ModerationInfoField(ModerationStatus.success, FIELD1, FIELD1, {"xxx": ModerationStatus.success})
        }
        info: ModerationInfo = self.processor.add_verdicts(
            None, prev_info,
            FIELD1, FIELD1, ModerationStatus.success, {"yyy": ModerationStatus.fail}, False,
        )
        expected = ModerationInfoField(
            ModerationStatus.fail,
            FIELD1,
            FIELD1,
            {"xxx": ModerationStatus.success, "yyy": ModerationStatus.fail},
            {"xxx": ModerationStatus.success, "yyy": ModerationStatus.fail},
        )
        self.assertEqual(info, {FIELD1: expected})

    def test_value_changed(self):
        """Field was before, value changed. Old verdicts ignored"""
        prev_info: ModerationInfo = {
            FIELD1: ModerationInfoField(ModerationStatus.fail, "old value", "old value", {"xxx": ModerationStatus.fail})
        }
        info: ModerationInfo = self.processor.add_verdicts(
            None, prev_info,
            FIELD1, FIELD1, ModerationStatus.success, {"yyy": ModerationStatus.success}, False,
        )
        expected = ModerationInfoField(
            ModerationStatus.success,
            FIELD1,
            FIELD1,
            {"yyy": ModerationStatus.success},
            {"yyy": ModerationStatus.success},
        )
        self.assertEqual(info, {FIELD1: expected})

    def test_finished_empty_verdicts(self):
        """Nothing before, empty verdicts, finished"""
        prev_info: ModerationInfo = {}
        info: ModerationInfo = self.processor.add_verdicts(
            None, prev_info,
            FIELD1, FIELD1, None, {}, False,
        )
        expected = ModerationInfoField(ModerationStatus.success, FIELD1, FIELD1, {}, {})
        self.assertEqual(info, {FIELD1: expected})
