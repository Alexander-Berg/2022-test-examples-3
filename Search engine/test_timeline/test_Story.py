from django.contrib.auth.models import User
from django.test import TestCase
from django.utils import timezone
from martylib.core.date_utils import get_datetime

from timeline.models import Story, Tag


class TestStory(TestCase):
    # February 10th, 6:00 AM.
    time_from = get_datetime('2017-02-10 06:00:00+00:00')
    # February 20th, 6:00 PM.
    time_to = get_datetime('2017-02-20 18:00:00+00:00')

    @classmethod
    def setUpClass(cls):
        super().setUpClass()

        # Refer to `timeline.models.StoryManager.search` comments for illustration.

        cls.foo = Tag.get('foo')
        cls.bar = Tag.get('bar')
        cls.baz = Tag.get('baz')
        cls.one = Tag.get('one')
        cls.two = Tag.get('two')

        cls.alice = User.objects.create(username='alice', id=1)
        cls.bob = User.objects.create(username='bob', id=2)
        cls.charlie = User.objects.create(username='charlie', id=3)
        cls.dave = User.objects.create(username='dave', id=4)
        cls.emily = User.objects.create(username='emily', id=5)

        Story.objects.create(
            id=1, name='Started and ended before `time_from`.',
            start=cls.time_from - timezone.timedelta(days=3), end=cls.time_from - timezone.timedelta(days=2),
        )

        Story.objects.create(
            id=2, name='Started before `time_from`, ended exactly at `time_from`.',
            start=cls.time_from - timezone.timedelta(days=1), end=cls.time_from,
        )

        Story.objects.create(
            id=3, name='Started before `time_from`, ended ended after `time_from`.',
            start=cls.time_from - timezone.timedelta(days=1), end=cls.time_from + timezone.timedelta(days=1),
        )

        Story.objects.create(
            id=4, name='Started exactly at `time_from`, ended before `time_to`.',
            start=cls.time_from, end=cls.time_to - timezone.timedelta(days=1),
        )

        Story.objects.create(
            id=5, name='Started after `time_from`, ended before `time_to`.',
            start=cls.time_from + timezone.timedelta(days=1), end=cls.time_to - timezone.timedelta(days=1),
        )

        Story.objects.create(
            id=6, name='Started before `time_to`, ended exactly at `time_to`.',
            start=cls.time_to - timezone.timedelta(days=1), end=cls.time_to,
        )

        Story.objects.create(
            id=7, name='Started before `time_to`, ended after `time_to`.',
            start=cls.time_to - timezone.timedelta(days=1), end=cls.time_to + timezone.timedelta(days=1),
        )

        Story.objects.create(
            id=8, name='Started exactly at `time_to`, ended after `time_to`.',
            start=cls.time_to, end=cls.time_to + timezone.timedelta(days=1),
        )

        Story.objects.create(
            id=9, name='Started and ended after `time_to`.',
            start=cls.time_to + timezone.timedelta(days=1), end=cls.time_to + timezone.timedelta(days=2),
        )

        Story.objects.create(
            id=10, name='Started before `time_from`, ended after `time_to`.',
            start=cls.time_from - timezone.timedelta(days=1), end=cls.time_to + timezone.timedelta(days=1),
        )

        Story.objects.create(
            id=11, name='Started before `time_from`, did not end.',
            start=cls.time_from - timezone.timedelta(days=1),
        )

        Story.objects.create(
            id=12, name='Started exactly at `time_from`, did not end.',
            start=cls.time_from,
        )

        Story.objects.create(
            id=13, name='Started after `time_from`, did not end.',
            start=cls.time_from + timezone.timedelta(days=1),
        )

        Story.objects.create(
            id=14, name='Started exactly at `time_to`, did not end.',
            start=cls.time_to,
        )

        Story.objects.create(
            id=15, name='Started after `time_to`, did not end.',
            start=cls.time_to + timezone.timedelta(days=1),
        )

        Story.objects.get(id=1).tags.add(cls.foo, cls.bar, cls.baz)
        Story.objects.get(id=1).authors.add(cls.alice, cls.bob, cls.charlie)

        Story.objects.get(id=2).tags.add(cls.foo, cls.bar)
        Story.objects.get(id=2).authors.add(cls.alice, cls.bob)

        Story.objects.get(id=3).tags.add(cls.foo)
        Story.objects.get(id=3).authors.add(cls.alice)

        Story.objects.get(id=4).tags.add(cls.one)
        Story.objects.get(id=4).authors.add(cls.dave)

        Story.objects.get(id=5).tags.add(cls.one, cls.two)
        Story.objects.get(id=5).authors.add(cls.dave, cls.emily)

        cls.limited_queryset = Story.objects.filter(id__in=[1, 2, 3, 4, 5])

    def test_search_time_window(self):
        self.assertEqual(
            [s.id for s in Story.objects.search(time_from=self.time_from, time_to=self.time_to, order_by='id')],
            [2, 3, 4, 5, 6, 7, 8, 10, 11, 12, 13, 14]
        )

    def test_search_time_from_only(self):
        self.assertEqual(
            [s.id for s in Story.objects.search(time_from=self.time_from, order_by='id')],
            [2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15]
        )

    def test_search_time_to_only(self):
        self.assertEqual(
            [s.id for s in Story.objects.search(time_to=self.time_to, order_by='id')],
            [1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 12, 13, 14]
        )

    def test_search_no_time_window(self):
        self.assertEqual(
            [s.id for s in Story.objects.search(order_by='id')],
            [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15]
        )

    # Tests below should use limited queryset of first five stories.

    def test_search_filter_tags(self):
        self.assertEqual(
            [
                s.id for s in Story.objects.search(
                    queryset=self.limited_queryset,
                    filter_tags=[self.foo], order_by='id'
                )
            ],
            [1, 2, 3]
        )
        self.assertEqual(
            [
                s.id for s in Story.objects.search(
                    queryset=self.limited_queryset,
                    filter_tags=[self.foo, self.bar], order_by='id'
                )
            ],
            [1, 2, 3]
        )
        self.assertEqual(
            [
                s.id for s in Story.objects.search(
                    queryset=self.limited_queryset,
                    filter_tags=[self.one], order_by='id'
                )
            ],
            [4, 5]
        )
        self.assertEqual(
            [
                s.id for s in Story.objects.search(
                    queryset=self.limited_queryset,
                    filter_tags=[self.foo, self.one], order_by='id'
                )
            ],
            [1, 2, 3, 4, 5]
        )
        self.assertEqual(
            [
                s.id for s in Story.objects.search(
                    queryset=self.limited_queryset,
                    filter_tags=[self.bar, self.two], order_by='id'
                )
            ],
            [1, 2, 5]
        )

    def test_search_exclude_tags(self):
        self.assertEqual(
            [
                s.id for s in Story.objects.search(
                    queryset=self.limited_queryset,
                    exclude_tags=[self.foo], order_by='id'
                )
            ],
            [4, 5]
        )
        self.assertEqual(
            [
                s.id for s in Story.objects.search(
                    queryset=self.limited_queryset,
                    exclude_tags=[self.foo, self.bar], order_by='id'
                )
            ],
            [4, 5]
        )
        self.assertEqual(
            [
                s.id for s in Story.objects.search(
                    queryset=self.limited_queryset,
                    exclude_tags=[self.one], order_by='id'
                )
            ],
            [1, 2, 3]
        )
        self.assertEqual(
            [
                s.id for s in Story.objects.search(
                    queryset=self.limited_queryset,
                    exclude_tags=[self.foo, self.one], order_by='id'
                )
            ],
            []
        )
        self.assertEqual(
            [
                s.id for s in Story.objects.search(
                    queryset=self.limited_queryset,
                    exclude_tags=[self.bar, self.two], order_by='id'
                )
            ],
            [3, 4]
        )

    def test_search_intersect_tags(self):
        self.assertEqual(
            [
                s.id for s in Story.objects.search(
                    queryset=self.limited_queryset,
                    intersect_tags=[self.foo], order_by='id'
                )
            ],
            [1, 2, 3]
        )
        self.assertEqual(
            [
                s.id for s in Story.objects.search(
                    queryset=self.limited_queryset,
                    intersect_tags=[self.foo, self.bar], order_by='id'
                )
            ],
            [1, 2]
        )
        self.assertEqual(
            [
                s.id for s in Story.objects.search(
                    queryset=self.limited_queryset,
                    intersect_tags=[self.one], order_by='id'
                )
            ],
            [4, 5]
        )
        self.assertEqual(
            [
                s.id for s in Story.objects.search(
                    queryset=self.limited_queryset,
                    intersect_tags=[self.foo, self.one], order_by='id'
                )
            ],
            []
        )
        self.assertEqual(
            [
                s.id for s in Story.objects.search(
                    queryset=self.limited_queryset,
                    intersect_tags=[self.bar, self.two], order_by='id'
                )
            ],
            []
        )

    def test_search_filter_authors(self):
        self.assertEqual(
            [
                s.id for s in Story.objects.search(
                    queryset=self.limited_queryset,
                    filter_authors=[self.alice], order_by='id'
                )
            ],
            [1, 2, 3]
        )
        self.assertEqual(
            [
                s.id for s in Story.objects.search(
                    queryset=self.limited_queryset,
                    filter_authors=[self.alice, self.bob], order_by='id'
                )
            ],
            [1, 2, 3]
        )
        self.assertEqual(
            [
                s.id for s in Story.objects.search(
                    queryset=self.limited_queryset,
                    filter_authors=[self.dave], order_by='id'
                )
            ],
            [4, 5]
        )
        self.assertEqual(
            [
                s.id for s in Story.objects.search(
                    queryset=self.limited_queryset,
                    filter_authors=[self.alice, self.dave], order_by='id'
                )
            ],
            [1, 2, 3, 4, 5]
        )
        self.assertEqual(
            [
                s.id for s in Story.objects.search(
                    queryset=self.limited_queryset,
                    filter_authors=[self.bob, self.emily], order_by='id'
                )
            ],
            [1, 2, 5]
        )

    def test_search_exclude_authors(self):
        self.assertEqual(
            [
                s.id for s in Story.objects.search(
                    queryset=self.limited_queryset,
                    exclude_authors=[self.alice], order_by='id'
                )
            ],
            [4, 5]
        )
        self.assertEqual(
            [
                s.id for s in Story.objects.search(
                    queryset=self.limited_queryset,
                    exclude_authors=[self.alice, self.bob], order_by='id'
                )
            ],
            [4, 5]
        )
        self.assertEqual(
            [
                s.id for s in Story.objects.search(
                    queryset=self.limited_queryset,
                    exclude_authors=[self.dave], order_by='id'
                )
            ],
            [1, 2, 3]
        )
        self.assertEqual(
            [
                s.id for s in Story.objects.search(
                    queryset=self.limited_queryset,
                    exclude_authors=[self.alice, self.dave], order_by='id'
                )
            ],
            []
        )
        self.assertEqual(
            [
                s.id for s in Story.objects.search(
                    queryset=self.limited_queryset,
                    exclude_authors=[self.bob, self.emily], order_by='id'
                )
            ],
            [3, 4]
        )

    def test_search_intersect_authors(self):
        self.assertEqual(
            [
                s.id for s in Story.objects.search(
                    queryset=self.limited_queryset,
                    intersect_authors=[self.alice], order_by='id'
                )
            ],
            [1, 2, 3]
        )
        self.assertEqual(
            [
                s.id for s in Story.objects.search(
                    queryset=self.limited_queryset,
                    intersect_authors=[self.alice, self.bob], order_by='id'
                )
            ],
            [1, 2]
        )
        self.assertEqual(
            [
                s.id for s in Story.objects.search(
                    queryset=self.limited_queryset,
                    intersect_authors=[self.dave], order_by='id'
                )
            ],
            [4, 5]
        )
        self.assertEqual(
            [
                s.id for s in Story.objects.search(
                    queryset=self.limited_queryset,
                    intersect_authors=[self.alice, self.dave], order_by='id'
                )
            ],
            []
        )
        self.assertEqual(
            [
                s.id for s in Story.objects.search(
                    queryset=self.limited_queryset,
                    intersect_authors=[self.bob, self.emily], order_by='id'
                )
            ],
            []
        )

    def test_custom_lookup(self):
        self.assertEqual(
            [s.id for s in Story.objects.search(id__in=[1, 2, 3])],
            [1, 2, 3]
        )
        self.assertEqual(
            [s.id for s in Story.objects.search(name__istartswith='started before `time_from`')],
            [2, 3, 10, 11]
        )
