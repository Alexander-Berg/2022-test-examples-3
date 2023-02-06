import logging

import unittest

from crypta.profile.runners.export_profiles.lib.profiles_generation.postprocess_profiles import (
    Export,
    PostprocessProfilesMapper,
)
from crypta.profile.utils.segment_utils.boolparser import ExpressionParser


expressions_dict = {
    'export-3e5274c5': Export(
        expressions='export-3e5274c5',
        field_name='heuristic_internal',
        segment_id=1276,
    ),
    'export-f3ecc373': Export(
        expressions='export-f3ecc373',
        field_name='heuristic_segments',
        segment_id=609,
    ),
    'export-d679b45b': Export(
        expressions='export-d679b45b',
        field_name='heuristic_private',
        segment_id=613,
    ),

    'export-63df6f34': Export(
        expressions='export-f3ecc373 OR export-d679b45b',
        field_name='heuristic_segments',
        segment_id=614,
    ),

    'export-9a44f4bf': Export(
        expressions='(export-9a44f4bf OR export-1e2f9547 OR export-b5dc76d6 OR export-e55d34bf OR\
         export-a23315a6 OR export-aa27e042) AND NOT export-1eba878d',
        field_name='probabilistic_segments',
        segment_id=566,
    ),

    'export-1e2f9547': Export(
        expressions='export-1e2f9547 AND NOT export-1eba878d',
        field_name='heuristic_common',
        segment_id=1026,
    ),

    'export-b5dc76d6': Export(
        expressions='export-b5dc76d6 AND NOT export-1eba878d',
        field_name='heuristic_common',
        segment_id=1027,
    ),

    'export-e55d34bf': Export(
        expressions='export-e55d34bf AND NOT export-1eba878d',
        field_name='heuristic_common',
        segment_id=1028,
    ),

    'export-a23315a6': Export(
        expressions='export-a23315a6 AND NOT export-1eba878d',
        field_name='heuristic_common',
        segment_id=1029,
    ),

    'export-aa27e042': Export(
        expressions='export-aa27e042 AND NOT export-1eba878d',
        field_name='heuristic_common',
        segment_id=1030,
    ),

    'export-1eba878d': Export(
        expressions='export-1eba878d',
        field_name='user_age_6s',
        segment_id=0,
    ),
    'export-65ca457e': Export(
        expressions='export-65ca457e',
        field_name='user_age_6s',
        segment_id=1,
    ),
    'export-768ab44a': Export(
        expressions='export-768ab44a',
        field_name='user_age_6s',
        segment_id=2,
    ),
    'export-7bebb023': Export(
        expressions='export-7bebb023',
        field_name='user_age_6s',
        segment_id=3,
    ),
    'export-7d4e94c4': Export(
        expressions='export-7d4e94c4',
        field_name='user_age_6s',
        segment_id=4,
    ),
    'export-41cbbd26': Export(
        expressions='export-41cbbd26',
        field_name='user_age_6s',
        segment_id=5,
    ),
    'export-870f7015': Export(
        expressions='export-870f7015 AND NOT export-80d3a68a',
        field_name='probabilistic_segments',
        segment_id=171,
    ),
    'export-785da493': Export(
        expressions='export-ea1aba45 AND export-a4011800',
        field_name='heuristic_segments',
        segment_id=520,
    ),
    'export-3e540434': Export(
        expressions='export-785da493',
        field_name='audience_segments',
        segment_id=1785390,
    ),
    'export-ea1aba45': Export(
        expressions='export-ea1aba45',
        field_name='heuristic_common',
        segment_id=1629,
    ),
    'export-df1abe35': Export(
        expressions='export-ea1aba45',
        field_name='heuristic_common',
        segment_id=1678,
    ),
    'export-a4011800': Export(
        expressions='export-a4011800',
        field_name='heuristic_internal',
        segment_id=1631,
    ),
    'export-21cc7623': Export(
        expressions='export-21cc7623 AND NOT export-a6218139',
        field_name='heuristic_segments',
        segment_id=596,
    ),
    'export-a6218139': Export(
        expressions='export-a6218139',
        field_name='heuristic_segments',
        segment_id=595,
    ),
    'export-803adb50': Export(
        expressions='export-803adb50',
        field_name='probabilistic_segments',
        segment_id=564,
    ),
    'export-48ca2c3f': Export(
        expressions='export-48ca2c3f AND NOT export-803adb50',
        field_name='probabilistic_segments',
        segment_id=688,
    ),
    'export-e3a005eb': Export(
        expressions='export-e3a005eb AND NOT export-532db077',
        field_name='marketing_segments',
        segment_id=169,
    ),
    'export-532db077': Export(
        expressions='export-532db077',
        field_name='heuristic_internal',
        segment_id=1120,
    ),
    'export-29ade1a4': Export(
        expressions='export-1e2f9547 OR export-8851e475',
        field_name='probabilistic_segments',
        segment_id=612,
    ),
    'export-8851e475': Export(
        expressions='export-8851e475',
        field_name='probabilistic_segments',
        segment_id=565,
    ),
    'export-68510964': Export(
        expressions='export-41cbbd26',
        field_name='heuristic_segments',
        segment_id=546,
    ),
    'export-cb306314': Export(
        expressions='export-7bebb023 OR export-7d4e94c4',
        field_name='heuristic_segments',
        segment_id=547,
    ),
    'export-f2a833c3': Export(
        expressions='export-65ca457e OR export-768ab44a',
        field_name='heuristic_segments',
        segment_id=548,
    ),
    'export-36a1a6be': Export(
        expressions='export-36a1a6be AND export-bbdbe627',
        field_name='marketing_segments',
        segment_id=334,
    ),
    'export-0dae712e': Export(
        expressions='export-0dae712e AND NOT export-2ba1cea6',
        field_name='marketing_segments',
        segment_id=335,
    ),
    'export-bbdbe627': Export(
        expressions='export-bbdbe627',
        field_name='gender',
        segment_id=1,
    ),
    'export-2ba1cea6': Export(
        expressions='export-2ba1cea6',
        field_name='gender',
        segment_id=0,
    ),
    'export-80d3a68a': Export(
        expressions='export-80d3a68a',
        field_name='income_5_segments',
        segment_id=0,
    ),
    'export-b5b825d6': Export(
        expressions='export-b5b825d6 AND NOT export-80d3a68a',
        field_name='probabilistic_segments',
        segment_id=199,
    ),
    'export-364124e5': Export(
        expressions='export-bbdbe627 AND (export-65ca457e OR export-768ab44a OR export-7bebb023) AND \
        (export-364124e5 OR export-b5dc76d6)',
        field_name='audience_segments',
        segment_id=9606130,
    ),
    'export-b5b82525': Export(
        expressions='export-36412443 OR export-b5b82525',
        field_name='audience_segments',
        segment_id=9606121,
    ),
    'export-36412443': Export(
        expressions='export-36412443',
        field_name='audience_segments',
        segment_id=9606153,
    ),
    'export-b5b82554': Export(
        expressions='export-b5b82525',
        field_name='audience_segments',
        segment_id=9606199,
    ),
    'export-ecd8536c': Export(
        expressions='export-bbdbe627 AND (export-65ca457e OR export-768ab44a OR export-7bebb023) AND \
        (export-1e2f9547 OR export-b5dc76d6)',
        field_name='audience_segments',
        segment_id=12469873,
    ),
    'export-598b2464': Export(
        expressions='export-bbdbe627 AND export-65ca457e AND (export-1e2f9547 OR export-b5dc76d6)',
        field_name='audience_segments',
        segment_id=12469822,
    ),
    'export-b8472c0d': Export(
        field_name='shortterm_interests',
        segment_id=11,
        expressions='export-b8472c0d',
    ),
    'export-b8472c0e': Export(
        field_name='shortterm_interests',
        segment_id=12,
        expressions='export-b8472c0e',
    ),
    'export-ba1e415d': Export(
        field_name='heuristic_segments',
        segment_id=555,
        expressions='export-ba1e415d',
    ),
    'export-b8472c0c': Export(
        field_name='heuristic_segments',
        segment_id=561,
        expressions='export-b8472c0c',
    ),
    'export-a48c0d1b': Export(
        field_name='heuristic_segments',
        segment_id=549,
        expressions='export-7199fdcf OR export-ba1e415d',
    ),
    'export-0927f3fa': Export(
        field_name='heuristic_segments',
        segment_id=560,
        expressions='export-fb22f2ff OR export-951d22ac',
    ),
    'export-6b8b6406': Export(
        field_name='probabilistic_segments',
        segment_id=88,
        expressions='export-6b8b6406 AND NOT (export-1eba878d OR export-7d4e94c4 OR export-41cbbd26)',
    ),
    'export-35eae157': Export(
        field_name='probabilistic_segments',
        segment_id=89,
        expressions='export-35eae157',
    ),
    'export-7199fdcf': Export(
        field_name='heuristic_segments',
        segment_id=552,
        expressions='export-7199fdcf',
    ),
    'export-fb22f2ff': Export(
        field_name='heuristic_segments',
        segment_id=561,
        expressions='export-fb22f2ff',
    ),
    'export-951d22ac': Export(
        field_name='heuristic_segments',
        segment_id=562,
        expressions='export-951d22ac',
    ),
    'export-e5973655': Export(
        field_name='audience_segments',
        segment_id=3296839,
        expressions='export-6b8b6406',
    ),
    'export-de307b40': Export(
        field_name='audience_segments',
        segment_id=16385473,
        expressions='export-bbdbe627 AND NOT (export-1e2f9547 OR export-b5dc76d6 OR export-e55d34bf OR export-a23315a6 OR export-aa27e042)'
    ),
}

OUTDATED_SHORTTERM_INTERESTS_THRESHOLD = 1000000

parser = ExpressionParser(expressions_dict, logging)
trees = parser.build_trees()
rules_processor = PostprocessProfilesMapper(
    trees=trees,
    exports_expressions=expressions_dict,
    outdated_shortterm_interests_threshold=OUTDATED_SHORTTERM_INTERESTS_THRESHOLD,
)


class Test(unittest.TestCase):

    def test_outdated_shortterm_interests(self):
        record_with_outdated_shortterm_interests = {
            'shortterm_interests': {
                '12': OUTDATED_SHORTTERM_INTERESTS_THRESHOLD + 1000,
                '11': OUTDATED_SHORTTERM_INTERESTS_THRESHOLD - 1000,  # outdated
            },
        }

        modified_record_with_outdated_shortterm_interests = rules_processor.apply_all_rules(
            record_with_outdated_shortterm_interests)
        self.assertTrue(modified_record_with_outdated_shortterm_interests['shortterm_interests'].get('11') is None)
        self.assertTrue(modified_record_with_outdated_shortterm_interests['shortterm_interests'].get('12') == OUTDATED_SHORTTERM_INTERESTS_THRESHOLD + 1000)

    def test_mirror(self):
        record = {
            'heuristic_common': [1629]
        }

        mirror_modified_record = rules_processor.apply_all_rules(record)
        self.assertTrue(1678 in mirror_modified_record['heuristic_common'])
        self.assertTrue(1629 in mirror_modified_record['heuristic_common'])

    def test_cycle(self):
        expressions_dict = {
            'export-811311c5': Export(
                expressions='export-d16fe471',
                field_name='marketing_segments',
                segment_id=277,
            ),
            'export-d16fe471': Export(
                expressions='export-811311c5',
                field_name='marketing_segments',
                segment_id=276,
            ),
        }

        self.assertSetEqual(set(), set(ExpressionParser(expressions_dict, logging).build_trees().keys()))

    def test_wrong_expression(self):
        expressions_dict = {
            'export-811311c5': Export(
                expressions='export-811311c5 AND NOT',
                field_name='marketing_segments',
                segment_id=277,
            ),
        }

        self.assertSetEqual(set(), set(ExpressionParser(expressions_dict, logging).build_trees().keys()))

    def test_bad_expression_in_dependencies(self):
        expressions_dict = {
            'export-00000000': Export(
                field_name='heuristic_segments',
                segment_id=560,
                expressions='export-88888888 AND export-eeeeeeee',
            ),
            'export-88888888': Export(
                expressions='export-88888888 AND NOT',
                field_name='marketing_segments',
                segment_id=277,
            ),
            'export-eeeeeeee': Export(
                field_name='audience_segments',
                segment_id=3296839,
                expressions='export-eeeeeeee',
            ),
        }

        self.assertSetEqual({"export-eeeeeeee"}, set(ExpressionParser(expressions_dict, logging).build_trees().keys()))

    def test_negative_expressions(self):
        expressions_dict = {
            'export-00000000': Export(
                field_name='audience_segments',
                segment_id=2000000560,
                expressions='NOT export-88888888',
            ),
            'export-88888888': Export(
                expressions='export-88888888',
                field_name='audience_segments',
                segment_id=2000000277,
            ),
            'export-eeeeeeee': Export(
                field_name='audience_segments',
                segment_id=2003296839,
                expressions='NOT export-00000000',
            ),
        }

        parser = ExpressionParser(expressions_dict, logging)
        trees = parser.build_trees()
        rules_processor = PostprocessProfilesMapper(
            trees=trees,
            exports_expressions=expressions_dict,
            outdated_shortterm_interests_threshold=OUTDATED_SHORTTERM_INTERESTS_THRESHOLD,
        )

        empty_record = {"audience_segments": []}
        empty_modified_record = rules_processor.apply_all_rules(empty_record)
        self.assertSetEqual({2000000560}, set(empty_modified_record["audience_segments"]))

        record_with_segment = {
            'audience_segments': [
                2000000277,
            ]
        }
        modified_record_with_segment = rules_processor.apply_all_rules(record_with_segment)
        self.assertSetEqual({2000000277, 2003296839}, set(modified_record_with_segment["audience_segments"]))

    def test_students(self):
        old_student_record = {
            'exact_socdem': {
                'gender': 'f',
                'income_segment': 'B',
                'age_segment': '45_54',
            },
            'probabilistic_segments': {
                '88': {'0': 1.0},
                '89': {'0': 0.6},
            },
        }

        old_student_modified_record = rules_processor.apply_all_rules(old_student_record)
        self.assertIsNone(old_student_modified_record['probabilistic_segments'].get('88'))

        student_record = {
            'exact_socdem': {
                'gender': 'f',
                'income_segment': 'B',
                'age_segment': '18_24',
            },
            'probabilistic_segments': {
                '88': {'0': 1.0},
            },
        }

        student_modified_record = rules_processor.apply_all_rules(student_record)
        self.assertIsNotNone(student_modified_record['probabilistic_segments'].get('88'))
        self.assertIn(3296839, student_modified_record['audience_segments'])

    def test_gamers(self):
        record = {
            'heuristic_segments': {
                '609': 1,
            },
        }
        modified_record = rules_processor.apply_all_rules(record)
        self.assertTrue('609' in modified_record['heuristic_segments'])
        self.assertTrue('614' in modified_record['heuristic_segments'])

    def test_children(self):
        have_children_record = {
            'exact_socdem': {
                'gender': 'f',
                'income_segment': 'B',
                'age_segment': '35_44',
            },
            'heuristic_common': [1026, 1029],
        }

        planing_children_record = {
            'exact_socdem': {
                'gender': 'f',
                'income_segment': 'B',
                'age_segment': '35_44',
            },
            'probabilistic_segments': {
                '565': {'0': 0.6},
            },
        }

        have_children_modified_record = rules_processor.apply_all_rules(have_children_record)
        self.assertTrue('566' in have_children_modified_record['probabilistic_segments'])
        self.assertTrue('171' not in have_children_modified_record['probabilistic_segments'])

        planing_children_modified_record = rules_processor.apply_all_rules(planing_children_record)
        self.assertTrue('612' in planing_children_modified_record['probabilistic_segments'])
        self.assertTrue('565' in planing_children_modified_record['probabilistic_segments'])

    def test_too_young(self):
        too_young_to_have_children_record = {
            'exact_socdem': {
                'gender': 'f',
                'income_segment': 'B',
                'age_segment': '0_17',
            },
            'probabilistic_segments': {
                '171': {'0': 0.6},
                '566': {'0': 0.8},
            },
            'heuristic_common': [1026, 1029],
        }

        too_young_to_have_children_modified_record = rules_processor.apply_all_rules(too_young_to_have_children_record)
        self.assertIsNone(too_young_to_have_children_modified_record['heuristic_common'])
        self.assertNotIn('566', too_young_to_have_children_modified_record['probabilistic_segments'])
        self.assertIn('171', too_young_to_have_children_modified_record['probabilistic_segments'])
        self.assertSetEqual({16385473}, set(too_young_to_have_children_modified_record['audience_segments']))

    def test_small_and_medium_business(self):
        record = {
            'heuristic_internal': [1631],
            'heuristic_common': [1629]
        }
        modified_record = rules_processor.apply_all_rules(record)
        self.assertTrue('520' in modified_record['heuristic_segments'])
        self.assertTrue(1785390 in modified_record['audience_segments'])

    def test_remove_conflicting(self):
        record = {
            'heuristic_segments': {
                '596': 1,
                '595': 1,
            },
            'probabilistic_segments': {
                '564': {'0': 0.6},
                '688': {'0': 0.8},
            },
            'marketing_segments': {
                '169': 1.0,
            },
            'heuristic_internal': [1120]
        }

        modified_record = rules_processor.apply_all_rules(record)
        self.assertNotIn('596', modified_record['heuristic_segments'])
        self.assertIn('595', modified_record['heuristic_segments'])
        self.assertNotIn('688', modified_record['probabilistic_segments'])
        self.assertIn('564', modified_record['probabilistic_segments'])
        self.assertIsNone(modified_record['marketing_segments'])
        self.assertIn(1120, modified_record['heuristic_internal'])

    def test_generations(self):
        record_baby = {
            'exact_socdem': {
                'gender': 'f',
                'income_segment': 'B',
                'age_segment': '0_17',
            },
        }
        record_boomer = {
            'exact_socdem': {
                'gender': 'f',
                'income_segment': 'B',
                'age_segment': '55_99',
            },
        }
        record_millenium = {
            'exact_socdem': {
                'gender': 'f',
                'income_segment': 'B',
                'age_segment': '18_24',
            },
        }
        record_X = {
            'exact_socdem': {
                'gender': 'f',
                'income_segment': 'B',
                'age_segment': '35_44',
            },
        }

        modified_record_baby = rules_processor.apply_all_rules(record_baby)
        modified_record_boomer = rules_processor.apply_all_rules(record_boomer)
        modified_record_millenium = rules_processor.apply_all_rules(record_millenium)
        modified_record_X = rules_processor.apply_all_rules(record_X)

        self.assertTrue('547' in modified_record_X['heuristic_segments'])
        self.assertTrue('548' in modified_record_millenium['heuristic_segments'])
        self.assertTrue('546' in modified_record_boomer['heuristic_segments'])
        self.assertTrue(modified_record_baby['heuristic_segments'] is None)

    def test_mobile_owners(self):
        record = {
            'heuristic_segments': {
                '555': 1,
                '561': 1,
            },
        }
        modified_record = rules_processor.apply_all_rules(record)
        self.assertTrue('549' in modified_record['heuristic_segments'])
        self.assertTrue('560' in modified_record['heuristic_segments'])

    def test_loreal_segments(self):
        record = {
            'marketing_segments': {
                '334': 1.0,
            },
            'exact_socdem': {
                'gender': 'f',
                'income_segment': 'B',
                'age_segment': '35_44',
            },
        }
        modified_record = rules_processor.apply_all_rules(record)
        self.assertTrue('334' in modified_record['marketing_segments'])

        record = {
            'marketing_segments': {
                '335': 1.0,
            },
            'exact_socdem': {
                'gender': 'm',
                'income_segment': 'B',
                'age_segment': '35_44',
            },
        }
        modified_record = rules_processor.apply_all_rules(record)
        self.assertIsNone(modified_record['marketing_segments'])

    def test_low_budget(self):
        record = {
            'exact_socdem': {
                'gender': 'f',
                'income_5_segment': 'A',
                'age_segment': '35_44',
            },
            'probabilistic_segments': {
                '171': {'0': 0.6},
                '199': {'0': 0.8},
                '564': {'0': 0.5}
            }
        }

        modified_record = rules_processor.apply_all_rules(record)
        self.assertListEqual(modified_record['probabilistic_segments'].keys(), ['564'])

    def test_self_mention(self):
        record = {
            'exact_socdem': {
                'gender': 'f',
                'income_segment': 'B',
                'age_segment': '18_24',
            },
            'heuristic_common': [1027],
            'audience_segments': [9606130],
        }

        modified_record = rules_processor.apply_all_rules(record)
        self.assertTrue(modified_record['audience_segments'] == [12469873, 9606130, 12469822])
        self.assertTrue(modified_record['heuristic_segments'] == {'548': 1})
        self.assertTrue(modified_record['probabilistic_segments'] == {'566': {'0': 1.0}})

    def test_mirror_self_mentioned(self):
        record = {
            'audience_segments': [9606121],
        }

        modified_record = rules_processor.apply_all_rules(record)
        self.assertTrue(modified_record['audience_segments'] == [9606121, 9606199])

    def test_women_without_children(self):
        record = {
            "exact_socdem": {
                "gender": "f"
            }
        }
        modified_record = rules_processor.apply_all_rules(record)
        self.assertIn(16385473, modified_record['audience_segments'])

    def test_socdem_or_segment(self):
        expressions_dict = {
            'export-00000000': Export(
                field_name='audience_segments',
                segment_id=2000000560,
                expressions='export-88888888 OR export-bbdbe627',
            ),
            'export-88888888': Export(
                expressions='export-88888888',
                field_name='audience_segments',
                segment_id=2000000277,
            ),
            'export-bbdbe627': Export(
                expressions='export-bbdbe627',
                field_name='gender',
                segment_id=1,
            ),
            'export-2ba1cea6': Export(
                expressions='export-2ba1cea6',
                field_name='gender',
                segment_id=0,
            ),
            'export-11111111': Export(
                field_name='audience_segments',
                segment_id=2000000561,
                expressions='export-88888888 AND export-2ba1cea6 OR export-bbdbe627',
            ),
        }

        parser = ExpressionParser(expressions_dict, logging)
        trees = parser.build_trees()
        rules_processor = PostprocessProfilesMapper(
            trees=trees,
            exports_expressions=expressions_dict,
            outdated_shortterm_interests_threshold=OUTDATED_SHORTTERM_INTERESTS_THRESHOLD,
        )

        record = {
            'exact_socdem': {
                'gender': 'f',
            },
        }
        modified_record = rules_processor.apply_all_rules(record)
        self.assertSetEqual({2000000560, 2000000561}, set(modified_record["audience_segments"]))


if __name__ == '__main__':
    unittest.main()
