# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock

from travel.rasp.library.python.common23.models.core.geo.settlement import Settlement
from travel.rasp.library.python.common23.tester.factories import create_settlement
from travel.rasp.library.python.common23.tester.testcase import TestCase
from travel.rasp.library.python.common23.tester.utils.language_activator import LanguageActivator
from travel.rasp.library.python.common23.utils.text import NBSP


class TestSettlementTitlePhraseFromBase(TestCase, LanguageActivator):
    def setUp(self):
        self.settlement = create_settlement()

    def tearDown(self):
        self.rollback_language()


@mock.patch.object(Settlement, 'L_title')
class TestSettlementTitlePhraseFrom_RuDefault(TestSettlementTitlePhraseFromBase):
    """
    Явно язык не передается. Используется текущий язык - русский.
    """
    def setUp(self):
        super(TestSettlementTitlePhraseFrom_RuDefault, self).setUp()
        self.set_language('ru')

    def test_l_title_exists(self, m_l_title):
        m_l_title.return_value = u'Киева'
        assert self.settlement.L_title_phrase_from() == u'из{}Киева'.format(NBSP)
        m_l_title.assert_called_once_with(case='genitive', fallback=False, lang='ru')

    def test_no_l_title(self, m_l_title):
        m_l_title.return_value = None
        assert self.settlement.L_title_phrase_from() is None
        m_l_title.assert_called_once_with(case='genitive', fallback=False, lang='ru')


@mock.patch.object(Settlement, 'L_title')
class TestSettlementTitlePhraseFrom_Ru(TestSettlementTitlePhraseFromBase):
    """
    Явно передается русский язык. Текущий язык - английский.
    """
    def setUp(self):
        super(TestSettlementTitlePhraseFrom_Ru, self).setUp()
        self.set_language('en')

    def test_l_title_exists(self, m_l_title):
        m_l_title.return_value = u'Киева'
        assert self.settlement.L_title_phrase_from(lang='ru') == u'из{}Киева'.format(NBSP)
        m_l_title.assert_called_once_with(case='genitive', fallback=False, lang='ru')

    def test_no_l_title(self, m_l_title):
        m_l_title.return_value = None
        assert self.settlement.L_title_phrase_from(lang='ru') is None
        m_l_title.assert_called_once_with(case='genitive', fallback=False, lang='ru')


@mock.patch.object(Settlement, 'L_title')
class TestSettlementTitlePhraseFrom_UkDefault(TestSettlementTitlePhraseFromBase):
    """
    Явно язык не передается. Используется текущий язык - украинский.
    """
    def setUp(self):
        super(TestSettlementTitlePhraseFrom_UkDefault, self).setUp()
        self.set_language('uk')

    def test_l_title_exists(self, m_l_title):
        m_l_title.return_value = u'Києва'
        assert self.settlement.L_title_phrase_from() == u'з{}Києва'.format(NBSP)
        m_l_title.assert_called_once_with(case='genitive', fallback=False, lang='uk')

    def test_no_l_title(self, m_l_title):
        m_l_title.return_value = None
        assert self.settlement.L_title_phrase_from() is None
        m_l_title.assert_called_once_with(case='genitive', fallback=False, lang='uk')


@mock.patch.object(Settlement, 'L_title')
class TestSettlementTitlePhraseFrom_Uk(TestSettlementTitlePhraseFromBase):
    """
    Явно передается украинский язык. Текущий язык - английский.
    """
    def setUp(self):
        super(TestSettlementTitlePhraseFrom_Uk, self).setUp()
        self.set_language('en')

    def test_l_title_exists(self, m_l_title):
        m_l_title.return_value = u'Києва'
        assert self.settlement.L_title_phrase_from(lang='uk') == u'з{}Києва'.format(NBSP)
        m_l_title.assert_called_once_with(case='genitive', fallback=False, lang='uk')

    def test_no_l_title(self, m_l_title):
        m_l_title.return_value = None
        assert self.settlement.L_title_phrase_from(lang='uk') is None
        m_l_title.assert_called_once_with(case='genitive', fallback=False, lang='uk')
