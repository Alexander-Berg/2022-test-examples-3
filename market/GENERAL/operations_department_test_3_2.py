# -*- coding: utf-8 -*-

import unittest
from operations_department import OperationsDepartmentProcessor
from common import CommentSigns
from mock import Mock
from helpers import load_ticket, set_heads_ok, add_summon_comment, set_heads_no

TICKET_ID = u'MARKETJOBTEST-20'
DEPARTMENT = u'yandex_monetize_market_3141_3226_dep30872'
TYPE = u'hire_new'
STAGE = u'offer'


# Департамент операционного управления - новая вакансия с превышением бюджета
class MainTest(unittest.TestCase):

    # новая вакансия - призвать руководителей
    def test_call_1(self):
        ticket, root_deps = load_ticket(TICKET_ID, DEPARTMENT, TYPE, STAGE)
        ticket[u'salary'] = 60000
        ticket[u'maxSalary'] = 50000
        ticket[u'option'] = 2000
        ticket['offer_comment'] = ticket['comments'][1]

        processor = OperationsDepartmentProcessor()

        res = processor.get_summonees(ticket['dep'], None, ticket, ticket['stage'], [])
        self.assertListEqual(res, [u'damir-askarov', u'm-krasavin', u'yaroshevsk'])

    # Руководителей призвали, кто-то поставил не ok - призвать автора
    def test_call_2(self):
        ticket, root_deps = load_ticket(TICKET_ID, DEPARTMENT, TYPE, STAGE)
        ticket[u'salary'] = 60000
        ticket[u'maxSalary'] = 50000
        ticket[u'option'] = 2000
        ticket['offer_comment'] = ticket['comments'][1]

        add_summon_comment(
            ticket,
            [u'damir-askarov', u'm-krasavin'],
            CommentSigns.heads_summon_comment +
            u"+ +!!(yellow)(?_код_автоматизации_markethire/ok_requested_stage=offer?)!!++",
        )
        set_heads_no(ticket, [u'm-krasavin'])

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

    # Руководители и hr одобрили, Указан опцион - надо призвать CEO1
    def test_call_3(self):
        ticket, root_deps = load_ticket(TICKET_ID, DEPARTMENT, TYPE, STAGE)
        ticket[u'salary'] = 60000
        ticket[u'maxSalary'] = 50000
        ticket[u'option'] = 2000
        ticket['offer_comment'] = ticket['comments'][1]

        add_summon_comment(
            ticket,
            [u'damir-askarov', u'm-krasavin', u'yaroshevsk'],
            CommentSigns.heads_summon_comment +
            u"+ +!!(yellow)(?_код_автоматизации_markethire/ok_requested_stage=offer?)!!++",
        )
        set_heads_ok(ticket, [u'damir-askarov', u'm-krasavin'])

        processor = OperationsDepartmentProcessor()
        processor.create_comment = Mock()

        processor.process_ticket(ticket, ticket['dep'], root_deps)

        processor.create_comment.assert_called_once()
        processor.create_comment.assert_called_with(
            TICKET_ID,
            CommentSigns.ceo_summon_comment,
            requested=[u'avalkov'],
            code=u"код_автоматизации_markethire/ok_ceo1_requested_stage=offer"
        )

    # Руководителей призвали, все поставили ok. Вакансия с превышением бюджета - призвать HR
    def test_call_4(self):
        ticket, root_deps = load_ticket(TICKET_ID, DEPARTMENT, TYPE, STAGE)
        ticket[u'salary'] = 60000
        ticket[u'maxSalary'] = 50000
        ticket[u'option'] = 2000
        ticket['offer_comment'] = ticket['comments'][1]

        add_summon_comment(
            ticket,
            [u'apogorelets'],
            CommentSigns.heads_summon_comment +
            u"+ +!!(yellow)(?_код_автоматизации_markethire/ok_requested_stage=offer?)!!++",
        )
        set_heads_ok(ticket, [u'apogorelets'])

        add_summon_comment(
            ticket,
            [u'avalkov'],
            CommentSigns.ceo_summon_comment +
            u"+ +!!(yellow)(?_код_автоматизации_markethire/ok_ceo1_requested_stage=offer?)!!++",
        )
        set_heads_ok(ticket, [u'avalkov'])

        processor = OperationsDepartmentProcessor()
        processor.create_comment = Mock()

        processor.process_ticket(ticket, ticket['dep'], root_deps)

        processor.create_comment.assert_called_once()
        processor.create_comment.assert_called_with(
            TICKET_ID,
            CommentSigns.hr_summon_comment,
            requested=[u'kiko', u'korshevam'],
            code=u"код_автоматизации_markethire/ok_hr_requested_stage=offer"
        )


if __name__ == "main":
    unittest.main()
