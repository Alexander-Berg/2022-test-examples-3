from django.test import TestCase

from timeline.models import Tag


class TestTag(TestCase):
    def setUp(self):
        for t in ('foo', 'bar'):
            Tag.get(t)
            for i in range(0, 5):
                Tag.get('{}:{}'.format(t, i))
        Tag.get('baz')

    def test_tokenize(self):
        tags = Tag.expand(r'^(foo|bar|baz)')
        tokenized = Tag.tokenize(tags)

        self.assertEqual(set(tokenized.keys()), {'foo', 'bar', 'baz'})
        self.assertEqual(tokenized['foo'], {True, '0', '1', '2', '3', '4'})
        self.assertEqual(tokenized['bar'], {True, '0', '1', '2', '3', '4'})
        self.assertEqual(tokenized['baz'], {True})

    def test_detokenize(self):
        tokenized = {
            'foo': {True, 'bar'},
            'bar': {'baz'},
            'baz': {True, '1', '2', '3'}
        }

        self.assertEqual(sorted(Tag.detokenize(tokenized)), [
            'bar:baz', 'baz', 'baz:1', 'baz:2', 'baz:3', 'foo', 'foo:bar'
        ])

    def test_tokenize_revert(self):
        tags = Tag.expand(r'^(foo|bar|baz)')
        self.assertEqual(
            sorted(Tag.detokenize(Tag.tokenize(tags))),
            sorted([t.text for t in tags]),
        )
