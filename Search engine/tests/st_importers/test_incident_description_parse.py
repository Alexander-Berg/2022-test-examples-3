from search.mon.warden.src.services.reducers.st_importers.incident_importer import SpiIncidentImporter
from search.mon.warden.tests.utils.base_test_case import BaseTestCase


class TestIncidentDescriptionParse(BaseTestCase):

    def test_incident_description_parsing(self):
        text = '''
        ==== Что во время инцидента видели пользователи
            Конечные юзеры не страдали.
        === Описание
            Релиз, который выкатился 2-го февраля в 19:30, содержал ошибку в LPM.
        === Действия дежурного
            ++//!!(grey)Здесь нужно указать действия дежурного!!//++
        === Хронология событий
            Хронлогия\nHere
        === Анализ
            ++//!!(grey)Что было хорошо, что пошло не так!!//++
        '''
        importer = SpiIncidentImporter()
        res = importer.parse_incident_description(text)
        self.assertEqual(res.description, 'Релиз, который выкатился 2-го февраля в 19:30, содержал ошибку в LPM.')
        self.assertEqual(res.external_impact, 'Конечные юзеры не страдали.')
        self.assertEqual(res.chronology, 'Хронлогия\nHere')

    def test_analyzed_incident_check(self):
        text = '''
        ==== Что во время инцидента видели пользователи
        Something
        === Описание
        Description
        === Хронология событий
        Chronology
        '''
        importer = SpiIncidentImporter()
        analysis = importer.parse_incident_description(text)
        self.assertTrue(importer.is_incident_analyzed(analysis))

    def test_not_analyzed_incident_check(self):
        text = '''
        ==== Что во время инцидента видели пользователи
        ++//!!(grey)Как проявилось у пользователей и сколько их пострадало!!//++
        === Описание
        Description
        === Хронология событий
        Chronology
        '''
        importer = SpiIncidentImporter()
        analysis = importer.parse_incident_description(text)
        self.assertFalse(importer.is_incident_analyzed(analysis))
