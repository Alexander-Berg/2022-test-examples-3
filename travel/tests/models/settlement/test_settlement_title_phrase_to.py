# -*- coding: utf-8 -*-
import mock

from travel.avia.library.python.common.models.geo import Settlement
from travel.avia.library.python.common.utils.text import NBSP
from travel.avia.library.python.tester.factories import create_settlement
from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.library.python.tester.utils.language_activator import LanguageActivator


class TestSettlementTitlePhraseToBase(TestCase, LanguageActivator):
    def setUp(self):
        self.settlement = create_settlement()

    def tearDown(self):
        self.rollback_language()


@mock.patch.object(Settlement, 'L_title')
class TestSettlementTitlePhraseTo_RuDefault(TestSettlementTitlePhraseToBase):
    """
    Явно язык не передается. Используется текущий язык - русский.
    """

    def setUp(self):
        super(TestSettlementTitlePhraseTo_RuDefault, self).setUp()
        self.set_language('ru')

    def test_l_title_exists(self, m_l_title):
        m_l_title.return_value = u'Киев'
        assert self.settlement.L_title_phrase_to() == u'в{}Киев'.format(NBSP)
        m_l_title.assert_called_once_with(case='accusative', fallback=False, lang='ru')

    def test_no_l_title(self, m_l_title):
        m_l_title.return_value = None
        assert self.settlement.L_title_phrase_to() is None
        m_l_title.assert_called_once_with(case='accusative', fallback=False, lang='ru')


@mock.patch.object(Settlement, 'L_title')
class TestSettlementTitlePhraseTo_Ru(TestSettlementTitlePhraseToBase):
    """
    Явно передается русский язык. Текущий язык - английский.
    """

    def setUp(self):
        super(TestSettlementTitlePhraseTo_Ru, self).setUp()
        self.set_language('en')

    def test_l_title_exists(self, m_l_title):
        m_l_title.return_value = u'Киев'
        assert self.settlement.L_title_phrase_to(lang='ru') == u'в{}Киев'.format(NBSP)
        m_l_title.assert_called_once_with(case='accusative', fallback=False, lang='ru')

    def test_no_l_title(self, m_l_title):
        m_l_title.return_value = None
        assert self.settlement.L_title_phrase_to(lang='ru') is None
        m_l_title.assert_called_once_with(case='accusative', fallback=False, lang='ru')


@mock.patch.object(Settlement, 'L_title')
class TestSettlementTitlePhraseTo_UkDefault(TestSettlementTitlePhraseToBase):
    """
    Явно язык не передается. Используется текущий язык - украинский.
    """

    def setUp(self):
        super(TestSettlementTitlePhraseTo_UkDefault, self).setUp()
        self.set_language('uk')

    def test_l_title_exists(self, m_l_title):
        m_l_title.return_value = u'Києва'
        assert self.settlement.L_title_phrase_to() == u'до{}Києва'.format(NBSP)
        m_l_title.assert_called_once_with(case='genitive', fallback=False, lang='uk')

    def test_no_l_title(self, m_l_title):
        m_l_title.return_value = None
        assert self.settlement.L_title_phrase_to() is None
        m_l_title.assert_called_once_with(case='genitive', fallback=False, lang='uk')


@mock.patch.object(Settlement, 'L_title')
class TestSettlementTitlePhraseTo_Uk(TestSettlementTitlePhraseToBase):
    """
    Явно передается украинский язык. Текущий язык - английский.
    """

    def setUp(self):
        super(TestSettlementTitlePhraseTo_Uk, self).setUp()
        self.set_language('en')

    def test_l_title_exists(self, m_l_title):
        m_l_title.return_value = u'Києва'
        assert self.settlement.L_title_phrase_to(lang='uk') == u'до{}Києва'.format(NBSP)
        m_l_title.assert_called_once_with(case='genitive', fallback=False, lang='uk')

    def test_no_l_title(self, m_l_title):
        m_l_title.return_value = None
        assert self.settlement.L_title_phrase_to(lang='uk') is None
        m_l_title.assert_called_once_with(case='genitive', fallback=False, lang='uk')
