# coding=utf-8
from __future__ import unicode_literals

import six

from travel.avia.library.python.sirena_client import SirenaAnswerError


def test_sirena_answer_error_stringification():
    err = SirenaAnswerError(
        u'Текст ошибки на русском',
        error_code=12345,
        error_xml=u'<xml>Юникодный xml</xml>',
    )

    r = repr(err)
    assert type(r) == str

    s = str(err)
    assert type(s) == str

    if six.PY2:
        u = unicode(err)  # noqa: F821
        assert type(u) == six.text_type

    err = SirenaAnswerError(
        u'Текст ошибки на русском'.encode('utf-8'),
        error_code=12345,
        error_xml=u'<xml>Юникодный xml</xml>'.encode('utf-8'),
    )

    r = repr(err)
    assert type(r) == str

    s = str(err)
    assert type(s) == str

    if six.PY2:
        u = unicode(err)  # noqa: F821
        assert type(u) == six.text_type
