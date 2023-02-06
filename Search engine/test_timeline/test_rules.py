from django.test import TestCase

from timeline.rules import Rules, InvalidRule


class TestRules(TestCase):
    def test_invalid_rules(self):
        strings = (
            'merge',
            'type:whatever',
            'group:,nanny-service, merge:,nanny-snapshot,',
        )
        for s in strings:
            with self.assertRaises(InvalidRule):
                Rules.from_string(s)

    def test_rules_from_to_string(self):
        strings = (
            'group:^nanny-service:,merge:^nanny-snapshot',
            'group:^nanny-service:',
            'merge:^nanny-snapshot',
            'content:{nanny-service} {default},merge:^nanny-snapshot-comment:',
            '',
        )

        for s in strings:
            self.assertEqual(Rules.from_string(s).to_string(), s)
