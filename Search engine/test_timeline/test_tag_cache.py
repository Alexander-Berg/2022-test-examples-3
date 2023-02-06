# coding: utf-8

from django.test import TestCase
from django.contrib.auth import get_user_model

from timeline.models import Story, Event, Tag


class TestTagCache(TestCase):
    def setUp(self):
        self.foo = Tag.get('foo')
        self.bar = Tag.get('bar')
        self.author, _ = get_user_model().objects.get_or_create(username='test')

    def test_story_cache_update_after_creation(self):
        s = Story()
        s.save()
        s.tags.add(self.foo)

        self.assertEqual(s.tag_cache, {'foo': {True}}, msg='Story tag cache was not updated after Story creation')

    def test_event_cache_update(self):
        s = Story()
        s.save()

        # noinspection PyArgumentList
        e = Event(story=s, author=self.author)
        e.save()
        e.tags.add(self.foo)

        self.assertEqual(e.tag_cache, {'foo': {True}}, msg='Event tag cache was not updated after Event creation')
        self.assertEqual(s.tag_cache, {'foo': {True}}, msg='Story tag cache was not updated after Event addition')

    def test_story_cache_update_after_event_cache_update(self):
        s = Story()
        s.save()

        # noinspection PyArgumentList
        e = Event(story=s, author=self.author)
        e.save()

        self.assertEqual(s.tag_cache, {}, msg='Story got unexpected non-empty tag cache')

        e.tags.add(self.foo)

        self.assertEqual(
            s.tag_cache, {'foo': {True}},
            msg='Story tag cache was not updated after Event tag cache update'
        )

    def test_story_cache_update_after_multiple_event_addition(self):
        s = Story()
        s.save()

        # noinspection PyArgumentList
        e = Event(story=s, author=self.author)
        e.save()
        e.tags.add(self.foo)

        self.assertEqual(
            s.tag_cache, {'foo': {True}},
            msg='Story tag cache was not updated after Event tag cache update'
        )

        # noinspection PyArgumentList
        e = Event(story=s, author=self.author)
        e.save()
        e.tags.add(self.bar)

        self.assertEqual(
            s.tag_cache, {'foo': {True}, 'bar': {True}},
            msg='Story tag cache was not updated after Event tag cache update'
        )
