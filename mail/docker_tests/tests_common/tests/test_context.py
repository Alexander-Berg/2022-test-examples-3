import pytest
import json
from tests_common.pytest_bdd import (
    Context,
    StepError,
)


@pytest.fixture(scope='function')
def context():
    return Context()


def test_none(context):
    context.set_extra(None)
    assert context.text is None
    assert context.table is None


def test_text(context):
    context.set_extra('''
        """
          Some text
        """
    ''')
    assert context.text.strip() == 'Some text'
    assert context.table is None


def test_text_json(context):
    context.set_extra('''
        """
          {
            "par1": "val1",
            "par2": "val2"
          }
        """
    ''')
    assert context.table is None
    assert json.loads(context.text) == {'par1': 'val1', 'par2': 'val2'}


def test_table(context):
    context.set_extra('''
        | col1 | col2   |
        | 42   | asdf   |
        | 13   | qwerty |
    ''')
    assert context.text is None
    assert context.table.to_dicts() == [
        {'col1': '42', 'col2': 'asdf'},
        {'col1': '13', 'col2': 'qwerty'},
    ]


@pytest.mark.parametrize('extra, exception', [
    ('Invalid', 'Step parse failed, extra block should be between ["""] for text or [|] for table, but got: Invalid'),
    (' | col1 | col2 |\n  42   | asdf | ', 'invalid table format, row does not start with |'),
    (' | col1 | col2  \n| 42   | asdf | ', 'invalid table format, row does not end with |'),
    (' | col1 | col2 |\n| 42     asdf | ', 'invalid table format, unmatched number of headers and values'),
])
def test_invalid_data(context, extra, exception):
    with pytest.raises(StepError) as exc:
        context.set_extra(extra)
    assert str(exc.value) == exception
