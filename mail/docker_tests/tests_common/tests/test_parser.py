import pytest
import mock
from tests_common.pytest_bdd import (
    BehaveParser,
    InvalidStepParserError,
)


class ContextMock(object):
    outline_param_names = []

    def set_args(self, args):
        pass

    def set_extra(self, text):
        pass


@pytest.fixture(scope='function')
def context():
    return ContextMock()


@pytest.fixture(scope='function')
def parser(context):
    return BehaveParser('step with "{parameter}"', context)


PARSER = __name__ + '.BehaveParser.'
CONTEXT = __name__ + '.ContextMock.'


@pytest.mark.parametrize('name, expected', [
    ('other', False),
    ('step with value', False),
    ('step with "value"', True),
    ('step with "value"\nextra', True),
])
def test_is_matching(parser, name, expected):
    assert parser.is_matching(name) == expected


@pytest.mark.parametrize('name, expected', [
    ('step with "value"', None),
    ('step with "value"\nextra', 'extra'),
])
@mock.patch(CONTEXT + 'set_extra')
def test_parse_arguments(mock_set_extra, parser, name, expected):
    res = parser.parse_arguments(name)

    mock_set_extra.assert_called_once_with(expected)

    assert res == {'parameter': 'value'}


def test_parse_arguments_error(parser):
    with pytest.raises(InvalidStepParserError) as exc:
        parser.parse_arguments('other')
    assert str(exc.value) == 'parser not found, should not be here'
