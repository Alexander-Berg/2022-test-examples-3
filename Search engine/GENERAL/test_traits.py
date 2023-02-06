import unittest

import search.geo.tools.task_manager.generators.building_info.lib.traits as traits
import data


class DomMingkhRu(unittest.TestCase):
    def test_get_address(self):
        self.assertEqual('Dummy st.', traits.DomMingkhRuTraits.get_address(data.DOM_MINGKH_RU_ROW))
        self.assertRaises(Exception, traits.DomMingkhRuTraits.get_address, {})
        self.assertIsNone(traits.DomMingkhRuTraits.get_address({'Общая информация': {}}))

    def test_get_info(self):
        self.assertDictEqual(
            data.DOM_MINGKH_RU_BUILDING_INFO,
            traits.DomMingkhRuTraits.get_info(data.DOM_MINGKH_RU_ROW)
        )


class ReformagkhRuAll(unittest.TestCase):
    def test_get_address(self):
        self.assertEqual(traits.ReformagkhRuAllTraits.get_address(data.REFORMAGKH_RU_ROW), 'Dummy st.')
        self.assertRaises(Exception, traits.ReformagkhRuAllTraits.get_address, {})
        self.assertIsNone(traits.ReformagkhRuAllTraits.get_address({'value': '{}'}))

    def test_get_info(self):
        self.assertDictEqual(
            data.REFORMAGKH_RU_BUILDING_INFO,
            traits.ReformagkhRuAllTraits.get_info(data.REFORMAGKH_RU_ROW)
        )
