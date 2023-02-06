from mock import Mock, call
from typing import cast

from travel.avia.stat_admin.tester.testcase import TestCase

from travel.avia.stat_admin.data.models import PartnerReview
from travel.avia.stat_admin.lib.review_filter import ReviewFilter
from travel.avia.stat_admin.lib.title_index import TitleIndex
from travel.avia.stat_admin.stat_tester import partner_review_fabric


class ReviewFilterTest(TestCase):
    def setUp(self):
        self._fake_index = Mock()
        self._filter = ReviewFilter(
            title_index=cast(TitleIndex, self._fake_index),
            ModelClass=PartnerReview
        )

    def test_from(self):
        partner_review = partner_review_fabric.create(
            'from_id', 'to_id'
        )
        other_partner_review = partner_review_fabric.create(
            'to_id', 'from_id'
        )
        self._fake_index.find = Mock(return_value='from_id')
        query_set = self._filter.search(
            ' f= from_title  ', PartnerReview.objects.all()
        )
        self._fake_index.find.assert_called_once_with('from_title')

        answer = list(query_set)
        assert len(answer) == 1
        assert answer[0].id == partner_review.id

        self._fake_index.find = Mock(return_value='to_id')
        query_set = self._filter.search(
            '  f=to_title   ',
            PartnerReview.objects.all())
        self._fake_index.find.assert_called_once_with('to_title')

        answer = list(query_set)
        assert len(answer) == 1
        assert answer[0].id == other_partner_review.id

    def test_unknown(self):
        partner_review_fabric.create(
            'from_id', 'to_id'
        )
        self._fake_index.find = Mock(return_value=None)
        query_set = self._filter.search(' f=unknown ', PartnerReview.objects.all())
        self._fake_index.find.assert_called_once_with(
            'unknown'
        )

        answer = list(query_set)
        assert len(answer) == 0

        partner_review_fabric.create(
            'from_id', 'to_id'
        )
        self._fake_index.find = Mock(return_value=None)
        query_set = self._filter.search(
            '   f=unknown1  ,  t=unknown2  ',
            PartnerReview.objects.all()
        )
        self._fake_index.find.assert_has_calls([
            call('unknown1'), call('unknown2')
        ])

        answer = list(query_set)
        assert len(answer) == 0

    def test_to_filter_only(self):
        partner_review = partner_review_fabric.create(
            'from_id', 'to_id'
        )
        self._fake_index.find = Mock(return_value='to_id')
        query_set = self._filter.search(' t = to_title  ', PartnerReview.objects.all())
        self._fake_index.find.assert_called_once_with('to_title')

        answer = list(query_set)
        assert len(answer) == 1
        assert answer[0].id == partner_review.id

    def test_redir_key_filter_only(self):
        partner_review = partner_review_fabric.create(
            'from_id', 'to_id', wizard_redir_key='some_key'
        )
        other_partner_review = partner_review_fabric.create(
            'from_id', 'to_id', wizard_redir_key='other_key'
        )
        self._fake_index.find = Mock(side_effect={
            'from_title': 'from_id',
            'to_title': 'to_id'
        }.get)
        query_set = self._filter.search(' f = from_title , t=to_title ,k=other_key  ', PartnerReview.objects.all())

        answer = list(query_set)
        assert len(answer) == 1
        assert answer[0].id == other_partner_review.id
