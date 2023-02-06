# -*- coding: utf-8 -*-

import unittest
from common import CommonProcessor, CommentSigns
from mock import Mock
from helpers import load_ticket, set_heads_ok, add_summon_comment, set_heads_no


TICKET_ID = u'MARKETJOBTEST-2'
DEPARTMENT = u'yandex_monetize_market_marketdev_business_stat'
TYPE = u'hire_to_replace'
STAGE = u'vacancy'


class MainTest(unittest.TestCase):

    # призвать первого руководителя
    def test_call_1(self):
        ticket, root_deps = load_ticket(TICKET_ID, DEPARTMENT, TYPE, STAGE)

        processor = CommonProcessor()
        processor.create_comment = Mock()

        processor.process_ticket(ticket, ticket['dep'], root_deps)

        processor.create_comment.assert_called_with(
            TICKET_ID,
            CommentSigns.heads_summon_comment,
            requested=[u'geradmi'],
            code=u"код_автоматизации_markethire/ok_requested_stage=vacancy"
        )

    # первый руководитель сказал нет - призвать автора тикета
    def test_call_2(self):
        ticket, root_deps = load_ticket(TICKET_ID, DEPARTMENT, TYPE, STAGE)

        processor = CommonProcessor()
        processor.create_comment = Mock()

        add_summon_comment(
            ticket,
            [u'geradmi'],
            CommentSigns.heads_summon_comment +
            u"+ +!!(yellow)(?_код_автоматизации_markethire/ok_requested_stage=vacancy?)!!++",
        )

        set_heads_no(ticket, [u'geradmi'])

        processor.process_ticket(ticket, ticket['dep'], root_deps)

        processor.create_comment.assert_called_with(
            TICKET_ID,
            CommentSigns.author_summon_comment,
            requested=[u'oroboros'],
            code=u"код_автоматизации_markethire/not_ok_author_requested_stage=vacancy"
        )

    # руководитель ok-нул, призвать hr-ов
    def test_call_3(self):
        ticket, root_deps = load_ticket(TICKET_ID, DEPARTMENT, TYPE, STAGE)

        processor = CommonProcessor()
        processor.create_comment = Mock()

        add_summon_comment(
            ticket,
            [u'geradmi'],
            CommentSigns.heads_summon_comment +
            u"+ +!!(yellow)(?_код_автоматизации_markethire/ok_requested_stage=vacancy?)!!++",
        )

        set_heads_ok(ticket, [u'geradmi'])

        processor.process_ticket(ticket, ticket['dep'], root_deps)

        processor.create_comment.assert_called_with(
            TICKET_ID,
            CommentSigns.hr_summon_comment,
            requested=[u'vikalobanova', u'korshevam'],
            code=u"код_автоматизации_markethire/ok_hr_requested_stage=vacancy"
        )

    # руководитель ok-нул, hr-ы одобрили
    def test_call_4(self):
        ticket, root_deps = load_ticket(TICKET_ID, DEPARTMENT, TYPE, STAGE)

        processor = CommonProcessor()
        processor.create_comment = Mock()

        add_summon_comment(
            ticket,
            [u'geradmi'],
            CommentSigns.heads_summon_comment +
            u"+ +!!(yellow)(?_код_автоматизации_markethire/ok_requested_stage=vacancy?)!!++",
        )

        set_heads_ok(ticket, [u'geradmi'])

        add_summon_comment(
            ticket,
            [u'vikalobanova', u'korshevam'],
            CommentSigns.hr_summon_comment +
            u"+ +!!(yellow)(?_код_автоматизации_markethire/ok_hr_requested_stage=vacancy?)!!++",
        )

        set_heads_ok(ticket, [u'vikalobanova', u'korshevam'])

        processor.process_ticket(ticket, ticket['dep'], root_deps)

        processor.create_comment.assert_called_with(
            TICKET_ID,
            CommentSigns.no_reaction_comment,
            requested=[u'oroboros'],
            code=u"код_автоматизации_markethire/three_days_passed_requested_stage=vacancy"
        )


if __name__ == "main":
    unittest.main()
