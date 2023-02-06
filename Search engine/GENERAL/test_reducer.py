import unittest

import search.geo.tools.task_manager.generators.building_info.lib.reducer as reducer
import search.geo.tools.task_manager.generators.building_info.lib.traits as traits
import data


class Reducer(unittest.TestCase):
    def test_fill_metadate(self):
        building_info_dict = {
            traits.DomMingkhRuTraits.get_table_index(): data.DOM_MINGKH_RU_BUILDING_INFO,
            traits.ReformagkhRuAllTraits.get_table_index(): data.REFORMAGKH_RU_BUILDING_INFO,
            traits.GasificationTraits.get_table_index(): data.GASIFICATION_BUILDING_INFO,
        }
        metadata = reducer.fill_metadata(building_info_dict)
        self.assertEqual(metadata.built_year, 2020)
        self.assertEqual(metadata.floors, 42)
        self.assertEqual(metadata.apartments, 242)
        self.assertEqual(metadata.emergency_condition, True)
        self.assertEqual(metadata.cadastral_number, '00:00:0000000:0')
        self.assertEqual(metadata.capital_repair_year, 2077)
        self.assertEqual(metadata.managing_company, 'Dummy company')
        self.assertEqual(metadata.gasification_plan, '02.2022-10.2022')
        self.assertEqual(
            metadata.attribution[0].author.name,
            'Реформа ЖКХ'
        )
        self.assertEqual(
            metadata.attribution[1].author.name,
            'Дом.МинЖКХ.РУ'
        )
