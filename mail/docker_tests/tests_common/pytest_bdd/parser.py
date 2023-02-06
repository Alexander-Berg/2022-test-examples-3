import logging
import re

import pytest_bdd
from pytest_bdd.exceptions import StepError, InvalidStepParserError
from .context import context

log = logging.getLogger(__name__)


class BehaveParser(object):
    extra_types = {}

    def __init__(self, name, ctx, *args, **kwargs):
        self.name = name
        self.context = ctx
        parse_builder = kwargs.pop('parse_builder', pytest_bdd.parsers.cfparse)
        if parse_builder in (pytest_bdd.parsers.cfparse, pytest_bdd.parsers.parse):
            kwargs.setdefault('extra_types', {}).update(BehaveParser.extra_types)
            self.parser_extra = parse_builder(name + '\n{extra}', *args, **kwargs)
            self.parser = parse_builder(name, *args, **kwargs)
        elif parse_builder == pytest_bdd.parsers.re:
            self.parser_extra = parse_builder(name + '\n(?P<extra>.+)$', re.DOTALL, *args, **kwargs)
            self.parser = parse_builder(name + '$', *args, **kwargs)

    def parse_arguments(self, name):
        name = self._parse_outlined(name)
        parser = self._get_parser(name)
        if not parser:
            raise InvalidStepParserError('parser not found, should not be here')
        log.debug('Step text [%s] applied to parser [%s]', name, parser.name)

        args = parser.parse_arguments(name)
        log.debug('Arguments: %s', args)

        self.context.set_extra(self._parse_outlined(args.pop('extra', None)))
        self.context.set_args(args)
        for argname, val in args.items():
            if argname in self.context.outline_param_names and str(val) != str(self._get_fixture(argname)):
                raise StepError(
                    'Step parameter "%s" with value [%s] overrides outline parameter with value [%s]. '
                    'Please rename one of them' % (argname, val, self._get_fixture(argname))
                )
        return args

    def is_matching(self, name):
        name = self._parse_outlined(name)
        return bool(self._get_parser(name))

    def _get_parser(self, name):
        if self.parser_extra.is_matching(name):
            return self.parser_extra
        if self.parser.is_matching(name):
            return self.parser
        return None

    def _parse_outlined(self, text):
        if not text:
            return text
        for param_name in self.context.outline_param_names:
            fixture = self._get_fixture(param_name)
            if fixture is not None:
                text = text.replace('<{}>'.format(param_name), str(fixture))
            else:
                log.debug('outlined param %s has no value', param_name)
        return text

    def _get_fixture(self, name):
        try:
            return self.context.request.getfixturevalue(name)
        except:
            return None


def parse(name, *args, **kwargs):
    return BehaveParser(name, context, *args, **kwargs)


def matcher_parsed(matcher):
    def wrapper(step_text, *args, **kwargs):
        return matcher(parse(step_text, *args, **kwargs))
    return wrapper


given = matcher_parsed(pytest_bdd.given)
when = matcher_parsed(pytest_bdd.when)
then = matcher_parsed(pytest_bdd.then)
