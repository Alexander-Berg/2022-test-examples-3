# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock

from travel.rasp.library.python.common23.models.texts.geobase_ltitle import Geobase_L_title
from travel.rasp.library.python.common23.tester.factories import create_settlement
from travel.rasp.library.python.common23.tester.testcase import TestCase
from travel.rasp.library.python.common23.tester.utils.language_activator import LanguageActivator


class LingusticsWithGenitive(object):
    def __init__(self, genitive):
        self.genitive = genitive


class LingusticsWithoutGenitive(object):
    pass


class TestLTitleGenitiveUkBase(TestCase, LanguageActivator):
    """
    В БД Я.Расписаний (таблица www_settlement) нет поля для родительного падежа на украинском языке.
    При выбранном украинском языке используется только Геобаза.

    В тестах на родительный падеж на украинском языке в модель города
    записываем русское название в именительном и родительном падежах
    и украинское название в именительном падеже.
    Это нужно для того, чтобы проверить, что не происходит fallback от родительного падежа к именительному
    и от украинского языка к русскому.
    """
    def setUp(self):
        super(TestLTitleGenitiveUkBase, self).setUp()
        self.rasp_title_ru_nominative = u'Горловка (именительный падеж из БД Я.Расписаний)'
        self.rasp_title_ru_genitive = u'Горловки (родительный падеж из БД Я.Расписаний)'
        self.rasp_title_uk_nominative = u'Горлiвка (именительный падеж из БД Я.Расписаний)'

        self.geobase_title_uk_genitive_ascii = 'Горлiвки (родительный падеж из Геобазы)'  # не юникодная строка!
        self.geobase_title_uk_genitive = u'Горлiвки (родительный падеж из Геобазы)'

        self.geo_id = 21774

    def tearDown(self):
        super(TestLTitleGenitiveUkBase, self).tearDown()
        self.rollback_language()


create_settlement = create_settlement.mutate(slug='Dummy-Slug')


@mock.patch.object(Geobase_L_title, 'get_geobase_linguistics')
class TestLTitleGenitiveUkDefault(TestLTitleGenitiveUkBase):
    """
    Явно язык не передается. Используется текущий язык - украинский.
    """
    def setUp(self):
        super(TestLTitleGenitiveUkDefault, self).setUp()
        self.set_language('uk')

    def test_geobase(self, m_get_geobase_linguistics):
        """
        В Геобазе есть город, у города есть винительный падеж на украинском языке.
        """
        m_get_geobase_linguistics.return_value = LingusticsWithGenitive(self.geobase_title_uk_genitive_ascii)
        settlement = create_settlement(title=self.rasp_title_ru_nominative,
                                       title_ru=self.rasp_title_ru_nominative,
                                       title_ru_genitive=self.rasp_title_ru_genitive,
                                       title_uk=self.rasp_title_uk_nominative,
                                       _geo_id=self.geo_id)

        assert settlement.L_title(case='genitive', fallback=False) == self.geobase_title_uk_genitive
        m_get_geobase_linguistics.assert_called_once_with(self.geo_id, 'uk')

    def test_geobase_not_found(self, m_get_geobase_linguistics):
        """
        В Геобазе города нет.
        """
        m_get_geobase_linguistics.return_value = None
        settlement = create_settlement(title=self.rasp_title_ru_nominative,
                                       title_ru=self.rasp_title_ru_nominative,
                                       title_ru_genitive=self.rasp_title_ru_genitive,
                                       title_uk=self.rasp_title_uk_nominative,
                                       _geo_id=self.geo_id)

        assert settlement.L_title(case='genitive', fallback=False) is None
        m_get_geobase_linguistics.assert_called_once_with(self.geo_id, 'uk')

    def test_linguistics_case_not_found(self, m_get_geobase_linguistics):
        """
        В Геобазе город есть, но нет родительного падежа на украинском языке.
        """
        m_get_geobase_linguistics.return_value = LingusticsWithoutGenitive()
        settlement = create_settlement(title=self.rasp_title_ru_nominative,
                                       title_ru=self.rasp_title_ru_nominative,
                                       title_ru_genitive=self.rasp_title_ru_genitive,
                                       title_uk=self.rasp_title_uk_nominative,
                                       _geo_id=self.geo_id)

        assert settlement.L_title(case='genitive', fallback=False) is None
        m_get_geobase_linguistics.assert_called_once_with(self.geo_id, 'uk')

    def test_no_geo_id(self, m_get_geobase_linguistics):
        """
        Geo-ID города не задан. К Геобазе не обращаемся.
        """
        settlement = create_settlement(title=self.rasp_title_ru_nominative,
                                       title_ru=self.rasp_title_ru_nominative,
                                       title_ru_genitive=self.rasp_title_ru_genitive,
                                       title_uk=self.rasp_title_uk_nominative)

        assert settlement.L_title(case='genitive', fallback=False) is None
        m_get_geobase_linguistics.assert_not_called()


@mock.patch.object(Geobase_L_title, 'get_geobase_linguistics')
class TestLTitleGenitiveUk(TestLTitleGenitiveUkBase):
    """
    Явно передан украинский язык. Текущий язык - английский.
    """
    def setUp(self):
        super(TestLTitleGenitiveUk, self).setUp()
        self.set_language('en')

    def test_geobase(self, m_get_geobase_linguistics):
        """
        В Геобазе есть город, у города есть винительный падеж на украинском языке.
        """
        m_get_geobase_linguistics.return_value = LingusticsWithGenitive(self.geobase_title_uk_genitive_ascii)
        settlement = create_settlement(title=self.rasp_title_ru_nominative,
                                       title_ru=self.rasp_title_ru_nominative,
                                       title_ru_genitive=self.rasp_title_ru_genitive,
                                       title_uk=self.rasp_title_uk_nominative,
                                       _geo_id=self.geo_id)

        assert settlement.L_title(lang='uk', case='genitive', fallback=False) == self.geobase_title_uk_genitive
        m_get_geobase_linguistics.assert_called_once_with(self.geo_id, 'uk')

    def test_geobase_not_found(self, m_get_geobase_linguistics):
        """
        В Геобазе города нет.
        """
        m_get_geobase_linguistics.return_value = None
        settlement = create_settlement(title=self.rasp_title_ru_nominative,
                                       title_ru=self.rasp_title_ru_nominative,
                                       title_ru_genitive=self.rasp_title_ru_genitive,
                                       title_uk=self.rasp_title_uk_nominative,
                                       _geo_id=self.geo_id)

        assert settlement.L_title(lang='uk', case='genitive', fallback=False) is None
        m_get_geobase_linguistics.assert_called_once_with(self.geo_id, 'uk')

    def test_linguistics_case_not_found(self, m_get_geobase_linguistics):
        """
        В Геобазе город есть, но нет родительного падежа на украинском языке.
        """
        m_get_geobase_linguistics.return_value = LingusticsWithoutGenitive()
        settlement = create_settlement(title=self.rasp_title_ru_nominative,
                                       title_ru=self.rasp_title_ru_nominative,
                                       title_ru_genitive=self.rasp_title_ru_genitive,
                                       title_uk=self.rasp_title_uk_nominative,
                                       _geo_id=self.geo_id)

        assert settlement.L_title(lang='uk', case='genitive', fallback=False) is None
        m_get_geobase_linguistics.assert_called_once_with(self.geo_id, 'uk')

    def test_no_geo_id(self, m_get_geobase_linguistics):
        """
        Geo-ID города не задан. К Геобазе не обращаемся.
        """
        settlement = create_settlement(title=self.rasp_title_ru_nominative,
                                       title_ru=self.rasp_title_ru_nominative,
                                       title_ru_genitive=self.rasp_title_ru_genitive,
                                       title_uk=self.rasp_title_uk_nominative)

        assert settlement.L_title(lang='uk', case='genitive', fallback=False) is None
        m_get_geobase_linguistics.assert_not_called()
