from ora2pg.tools.recipients import serialize_recipients
from pytest import mark


@mark.parametrize(('recipients', 'string'), [
    ([], u''),
    ([(u'', u'')], u''),
    ([(u'Somebody', u'')], u'Somebody'),
    ([(u'', u'somebody@somewhere')], u'<somebody@somewhere>'),
    ([(u'Somebody', u'somebody@somewhere')], u'Somebody <somebody@somewhere>'),
    ([(u'A', u'a@a'), (u'B', u'b@b')], u'A <a@a>, B <b@b>'),
    ([(u'no_address', u'')], u'no_address'),
])
def test_for_given_name_and_email_pairs_list_returns_string(recipients, string):
    result = serialize_recipients(recipients)
    assert result == string
