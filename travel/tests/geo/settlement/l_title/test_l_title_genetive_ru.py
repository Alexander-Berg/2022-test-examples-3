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


class TestLTitleGenitiveRuBase(TestCase, LanguageActivator):
    def setUp(self):
        super(TestLTitleGenitiveRuBase, self).setUp()
        self.rasp_title_nominative = u'Горловка (именительный падеж из БД Я.Расписаний)'
        self.rasp_title_genitive = u'Горловку (родительный падеж из БД Я.Расписаний)'
        self.geobase_title_genitive_ascii = 'Горловку (родительный падеж из Геобазы)'  # не юникодная строка!
        self.geobase_title_genitive = u'Горловку (родительный падеж из Геобазы)'
        self.geo_id = 21774

    def tearDown(self):
        super(TestLTitleGenitiveRuBase, self).tearDown()
        self.rollback_language()


create_settlement = create_settlement.mutate(slug='Dummy-Slug')


@mock.patch.object(Geobase_L_title, 'get_geobase_linguistics')
class TestLTitleGenitiveRuDefault(TestLTitleGenitiveRuBase):
    """
    Явно язык не передается. Используется текущий язык - русский.
    """
    def setUp(self):
        super(TestLTitleGenitiveRuDefault, self).setUp()
        self.set_language('ru')

    def test_rasp_db(self, m_get_geobase_linguistics):
        """
        Название города в винительном падеже есть в БД Я.Расписаний. К геобазе не обращаемся.
        """
        settlement = create_settlement(title=self.rasp_title_nominative, title_ru=self.rasp_title_nominative,
                                       title_ru_genitive=self.rasp_title_genitive)
        assert settlement.L_title(case='genitive', fallback=False) == self.rasp_title_genitive
        m_get_geobase_linguistics.assert_not_called()

    def test_geobase(self, m_get_geobase_linguistics):
        """
        Названия города в винительном падеже нет в БД Я.Расписаний. Город в Геобазе есть. Берется значение из геобазы.
        """
        m_get_geobase_linguistics.return_value = LingusticsWithGenitive(self.geobase_title_genitive_ascii)
        settlement = create_settlement(title=self.rasp_title_nominative, title_ru=self.rasp_title_nominative,
                                       _geo_id=self.geo_id)
        assert settlement.L_title(case='genitive', fallback=False) == self.geobase_title_genitive
        m_get_geobase_linguistics.assert_called_once_with(self.geo_id, 'ru')

    def test_rasp_db_and_geobase(self, m_get_geobase_linguistics):
        """
        Название города в винительном падеже есть и в БД Я.Расписаний, и в Геобзае. Берется значение из БД Я.Расписаний.
        """
        settlement = create_settlement(title=self.rasp_title_nominative, title_ru=self.rasp_title_nominative,
                                       title_ru_genitive=self.rasp_title_genitive, _geo_id=self.geo_id)
        assert settlement.L_title(case='genitive', fallback=False) == self.rasp_title_genitive
        m_get_geobase_linguistics.assert_not_called()

    def test_geobase_not_found(self, m_get_geobase_linguistics):
        """
        Названия города в винительном падеже нет в БД Я.Расписаний, в Геобазе города нет.
        """
        m_get_geobase_linguistics.return_value = None
        settlement = create_settlement(title=self.rasp_title_nominative, title_ru=self.rasp_title_nominative,
                                       _geo_id=self.geo_id)
        assert settlement.L_title(case='genitive', fallback=False) is None
        m_get_geobase_linguistics.assert_called_once_with(self.geo_id, 'ru')

    def test_linguistics_case_not_found(self, m_get_geobase_linguistics):
        """
        Названия города в винительном падеже нет в БД Я.Расписаний.
        В Геобазе город есть, но нет родительного падежа на русском языке.
        """
        m_get_geobase_linguistics.return_value = LingusticsWithoutGenitive()
        settlement = create_settlement(title=self.rasp_title_nominative, title_ru=self.rasp_title_nominative,
                                       _geo_id=self.geo_id)
        assert settlement.L_title(case='genitive', fallback=False) is None
        m_get_geobase_linguistics.assert_called_once_with(self.geo_id, 'ru')

    def test_no_geo_id(self, m_get_geobase_linguistics):
        """
        Названия города в винительном падеже нет в БД Я.Расписаний. Geo-ID города не задан. К Геобазе не обращаемся.
        """
        settlement = create_settlement(title=self.rasp_title_nominative, title_ru=self.rasp_title_nominative)
        assert settlement.L_title(case='genitive', fallback=False) is None
        m_get_geobase_linguistics.assert_not_called()


@mock.patch.object(Geobase_L_title, 'get_geobase_linguistics')
class TestLTitleGenitiveRu(TestLTitleGenitiveRuBase):
    """
    Явно передается русский язык. Текущий язык - английский.
    """
    def setUp(self):
        super(TestLTitleGenitiveRu, self).setUp()
        self.set_language('en')

    def test_rasp_db(self, m_get_geobase_linguistics):
        """
        Название города в винительном падеже есть в БД Я.Расписаний. К геобазе не обращаемся.
        """
        settlement = create_settlement(title=self.rasp_title_nominative, title_ru=self.rasp_title_nominative,
                                       title_ru_genitive=self.rasp_title_genitive)
        assert settlement.L_title(lang='ru', case='genitive', fallback=False) == self.rasp_title_genitive
        m_get_geobase_linguistics.assert_not_called()

    def test_geobase(self, m_get_geobase_linguistics):
        """
        Названия города в винительном падеже нет в БД Я.Расписаний. Город есть в Геобазе. Берется значение из геобазы.
        """
        m_get_geobase_linguistics.return_value = LingusticsWithGenitive(self.geobase_title_genitive_ascii)
        settlement = create_settlement(title=self.rasp_title_nominative, title_ru=self.rasp_title_nominative,
                                       _geo_id=self.geo_id)
        assert settlement.L_title(lang='ru', case='genitive', fallback=False) == self.geobase_title_genitive
        m_get_geobase_linguistics.assert_called_once_with(self.geo_id, 'ru')

    def test_rasp_db_and_geobase(self, m_get_geobase_linguistics):
        """
        Название города в винительном падеже есть и в БД Я.Расписаний, и в Геобзае. Берется значение из БД Я.Расписаний.
        """
        settlement = create_settlement(title=self.rasp_title_nominative, title_ru=self.rasp_title_nominative,
                                       title_ru_genitive=self.rasp_title_genitive, _geo_id=self.geo_id)
        assert settlement.L_title(lang='ru', case='genitive', fallback=False) == self.rasp_title_genitive
        m_get_geobase_linguistics.assert_not_called()

    def test_geobase_not_found(self, m_get_geobase_linguistics):
        """
        Названия города в винительном падеже нет в БД Я.Расписаний, в Геобазе города нет.
        """
        m_get_geobase_linguistics.return_value = None
        settlement = create_settlement(title=self.rasp_title_nominative, title_ru=self.rasp_title_nominative,
                                       _geo_id=self.geo_id)
        assert settlement.L_title(lang='ru', case='genitive', fallback=False) is None
        m_get_geobase_linguistics.assert_called_once_with(self.geo_id, 'ru')

    def test_linguistics_case_not_found(self, m_get_geobase_linguistics):
        """
        Названия города в винительном падеже нет в БД Я.Расписаний.
        В Геобазе город есть, но нет родительного падежа на русском языке.
        """
        m_get_geobase_linguistics.return_value = LingusticsWithoutGenitive()
        settlement = create_settlement(title=self.rasp_title_nominative, title_ru=self.rasp_title_nominative,
                                       _geo_id=self.geo_id)
        assert settlement.L_title(lang='ru', case='genitive', fallback=False) is None
        m_get_geobase_linguistics.assert_called_once_with(self.geo_id, 'ru')

    def test_no_geo_id(self, m_get_geobase_linguistics):
        """
        Названия города в винительном падеже нет в БД Я.Расписаний. Geo-ID города не задан. К Геобазе не обращаемся.
        """
        settlement = create_settlement(title=self.rasp_title_nominative, title_ru=self.rasp_title_nominative)
        assert settlement.L_title(lang='ru', case='genitive', fallback=False) is None
        m_get_geobase_linguistics.assert_not_called()
