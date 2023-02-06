# coding: utf-8

from pyparsing import Word, Keyword, alphanums, Optional, OneOrMore, Literal, CharsNotIn

from pymdb.types import FilterAction, FilterCondition


class FiltersParser(object):
    def __init__(self):
        self.conditions = []
        self.actions = []
        self.prev_link = None

    def _make_gramma(self):  # pylint: disable=R0914
        def condition_cb(s, loc, toks):
            link = toks.link or self.prev_link
            link = (link or 'or').lower()
            self.prev_link = link
            field_type = toks.type
            field = toks.field
            if field_type in ['to', 'from']:
                field = field_type
                field_type = 'header'
            self.conditions.append(
                FilterCondition(
                    field_type=field_type,
                    field=field,
                    pattern=toks.pattern,
                    oper=toks.oper,
                    link=link,
                    negative=bool(toks.not_cond),
                ))

        def action_cb(s, loc, toks):
            verified = False
            if not toks.not_verified and toks.verified:
                verified = True
            self.actions.append(
                FilterAction(
                    action_id=None,
                    oper=toks.type,
                    param=toks.param,
                    verified=verified
                )
            )

        AND = Keyword('AND')
        OR = Keyword('OR')
        IF = Keyword('IF')
        THEN = Keyword('THEN')
        NOT = Keyword('NOT')

        field_type_alone = (
            Literal('from') | Literal('to') | Literal('body') | Literal('type')
        ).setResultsName('type')
        field_type_full = (
            (Literal('flag') | Literal('header')).setResultsName('type')
            + Literal('.')
            + Word(alphanums + ':_-').setResultsName('field')
        )
        field_type = field_type_alone | field_type_full

        cond_oper_exists = Keyword('exists').setResultsName('oper')

        cond_oper = (
            Keyword('contains') | Keyword('matches')
        ).setResultsName('oper')

        cond_param = (
            Word(alphanums + ':.@_-').setResultsName('pattern') | (
                Literal('"') + CharsNotIn('"').setResultsName('pattern') + Literal('"')
            )
        )

        cond_link = (AND | OR).setResultsName('link')

        not_condition = NOT.setResultsName('not_cond')
        condition = (
            Optional(not_condition) + field_type + (
                cond_oper + cond_param | cond_oper_exists
            ) + Optional(cond_link)
        )
        condition.addParseAction(condition_cb)

        action_type = (
            Keyword('move') | Keyword('movel') |
            Keyword('forward') | Keyword('forwardwithstore') |
            Keyword('reply') | Keyword('notify')
        ).setResultsName('type')
        action_param = (
            Word(alphanums + ':.@_-').setResultsName('param') | (
                Literal('"') + CharsNotIn('"').setResultsName('param') + Literal('"')
            )
        )

        action_verified = (
            Optional(Keyword('NOT')).setResultsName('not_verified')
            + Keyword('VERIFIED').setResultsName('verified')
        )
        action = (
            action_type + (Keyword("as") | Keyword("to"))
            + Optional(action_verified) + action_param
        )
        action.addParseAction(action_cb)

        return IF + OneOrMore(condition) + THEN + OneOrMore(action + Optional(AND))

    def parse(self, filter_str):
        self._make_gramma().parseString(filter_str, parseAll=True)
        return self


def parse_filter(filter_str):
    assert filter_str is not None
    parser = FiltersParser().parse(filter_str)
    return parser
