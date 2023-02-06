# -*- coding: utf-8 -*-

import unittest
from operations_department import OperationsDepartmentProcessor
from common import CommentSigns
from mock import Mock
from helpers import load_ticket, set_heads_ok, add_summon_comment, set_heads_no

TICKET_ID = u'MARKETJOBTEST-18'
DEPARTMENT = u'yandex_monetize_market_3141_3226_dep30872'
TYPE = u'hire_new'
STAGE = u'offer'


# Департамент операционного управления - новая вакансия без превышения бюджета
class MainTest(unittest.TestCase):

    # новая вакансия - призвать руководителей
    def test_call_1(self):
        ticket, root_deps = load_ticket(TICKET_ID, DEPARTMENT, TYPE, STAGE)
        ticket[u'salary'] = 40000
        ticket[u'maxSalary'] = None
        ticket[u'option'] = 0
        ticket['offer_comment'] = ticket['comments'][1]

        processor = OperationsDepartmentProcessor()

        res = processor.get_summonees(ticket['dep'], None, ticket, ticket['stage'], [])
        self.assertListEqual(res, [u'damir-askarov', u'm-krasavin', u'yaroshevsk'])

    # Руководителей призвали, кто-то поставил не ok - призвать автора
    def test_call_2(self):
        ticket, root_deps = load_ticket(TICKET_ID, DEPARTMENT, TYPE, STAGE)
        ticket[u'salary'] = 40000
        ticket[u'maxSalary'] = 40000
        ticket[u'option'] = 0
        ticket['offer_comment'] = ticket['comments'][1]

        add_summon_comment(
            ticket,
            [u'damir-askarov', u'm-krasavin', u'yaroshevsk'],
            CommentSigns.heads_summon_comment +
            u"+ +!!(yellow)(?_код_автоматизации_markethire/ok_requested_stage=offer?)!!++",
        )
        set_heads_no(ticket, [u'damir-askarov'])

        processor = OperationsDepartmentProcessor()
        processor.create_comment = Mock()

        processor.process_ticket(ticket, ticket['dep'], root_deps)

        processor.create_comment.assert_called_once()
        processor.create_comment.assert_called_with(
            TICKET_ID,
            CommentSigns.author_summon_comment,
            requested=[u'ndanisimov'],
            code=u"код_автоматизации_markethire/not_ok_author_requested_stage=offer"
        )

    # Руководителей призвали, все поставили ok - призвать hr
    def test_call_3(self):
        ticket, root_deps = load_ticket(TICKET_ID, DEPARTMENT, TYPE, STAGE)
        ticket[u'salary'] = 40000
        ticket[u'maxSalary'] = 40000
        ticket[u'option'] = 0
        ticket['offer_comment'] = ticket['comments'][1]

        add_summon_comment(
            ticket,
            [u'damir-askarov', u'm-krasavin', u'yaroshevsk'],
            CommentSigns.heads_summon_comment +
            u"+ +!!(yellow)(?_код_автоматизации_markethire/ok_requested_stage=offer?)!!++",
        )
        set_heads_ok(ticket, [u'damir-askarov', u'm-krasavin', u'yaroshevsk'])

        processor = OperationsDepartmentProcessor()
        processor.create_comment = Mock()

        processor.process_ticket(ticket, ticket['dep'], root_deps)

        processor.create_comment.assert_called_once()
        processor.create_comment.assert_called_with(
            TICKET_ID,
            CommentSigns.hr_summon_comment,
            requested=[u'kiko'],
            code=u"код_автоматизации_markethire/ok_hr_requested_stage=offer"
        )

    # Руководители и hr одобрили, ceo2 не призывать - нет реакции больше трех дней - призвать автора тикета
    def test_call_4(self):
        ticket, root_deps = load_ticket(TICKET_ID, DEPARTMENT, TYPE, STAGE)
        ticket[u'salary'] = 40000
        ticket[u'maxSalary'] = 40000
        ticket[u'option'] = 0
        ticket['offer_comment'] = ticket['comments'][1]

        add_summon_comment(
            ticket,
            [u'damir-askarov', u'm-krasavin', u'yaroshevsk'],
            CommentSigns.heads_summon_comment +
            u"+ +!!(yellow)(?_код_автоматизации_markethire/ok_requested_stage=offer?)!!++",
        )
        set_heads_ok(ticket, [u'damir-askarov', u'm-krasavin', u'yaroshevsk'])

        add_summon_comment(
            ticket,
            [u'kiko'],
            CommentSigns.hr_summon_comment +
            u"+ +!!(yellow)(?_код_автоматизации_markethire/ok_hr_requested_stage=vacancy?)!!++",
        )

        set_heads_ok(ticket, [u'kiko'])

        processor = OperationsDepartmentProcessor()
        processor.create_comment = Mock()

        processor.process_ticket(ticket, ticket['dep'], root_deps)

        processor.create_comment.assert_called_once()
        processor.create_comment.assert_called_with(
            TICKET_ID,
            CommentSigns.no_reaction_comment,
            requested=[u'ndanisimov'],
            code=u"код_автоматизации_markethire/three_days_passed_requested_stage=offer"
        )


if __name__ == "main":
    unittest.main()
