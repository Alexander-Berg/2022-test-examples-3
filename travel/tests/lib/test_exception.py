# -*- coding: utf-8 -*-

import unittest

from django.utils.functional import lazy
from django.utils.translation import get_language
from django.utils import translation

from travel.avia.admin.www.utils.data import rus2translit
from travel.avia.admin.lib.exceptions import TranslatableException


def ugettext(text):
    lang = get_language()

    if lang == 'en':
        return rus2translit(text)

    elif lang == 'ru':
        return text

    raise NotImplementedError('Language %s is not supported', lang)

_ = ugettext_lazy = lazy(ugettext, unicode)


class CacheMethodResultTest(unittest.TestCase):
    def testTranslation(self):
        msg = u'Тестовая строка'
        msg_en = u'Testovaya stroka'

        with translation.override('en'):
            self.assertEqual(ugettext(msg), msg_en)

        with translation.override('ru'):
            self.assertEqual(ugettext(msg), msg)

        e = TranslatableException(_(msg))

        with translation.override('en'):
            self.assertEqual(e.msg, msg_en)

        with translation.override('ru'):
            self.assertEqual(e.msg, msg)

    def testExceptionWithArg(self):
        msg = u'Тестовая строка %s'
        msg_en = u'Testovaya stroka %s'

        arg = u'123'

        e = TranslatableException(_(msg), arg)

        with translation.override('en'):
            self.assertEqual(e.msg, msg_en % arg)

        with translation.override('ru'):
            self.assertEqual(e.msg, msg % arg)

    def testExceptionWithArgs(self):
        msg = u'Тестовая строка %s %s'
        msg_en = u'Testovaya stroka %s %s'

        arg = u'123'
        arg2 = u'Второй аргумент'

        e = TranslatableException(_(msg), arg, arg2)

        with translation.override('en'):
            self.assertEqual(e.msg, msg_en % (arg, arg2))

        with translation.override('ru'):
            self.assertEqual(e.msg, msg % (arg, arg2))

    def testExceptionWithKwargs(self):
        msg = u'Тестовая строка %(line)s %(test)s'
        msg_en = u'Testovaya stroka %(line)s %(test)s'

        test = u'123'
        line = u'Второй аргумент'

        e = TranslatableException(_(msg), test=test, line=line)

        with translation.override('en'):
            self.assertEqual(e.msg, msg_en % dict(test=test, line=line))

        with translation.override('ru'):
            self.assertEqual(e.msg, msg % dict(test=test, line=line))
