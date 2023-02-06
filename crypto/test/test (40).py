#!/usr/bin/env python
# -*- coding: utf-8 -*-
import unittest
from yabs.proto import user_profile_pb2

from crypta.profile.lib import bb_helpers


class TestParseBbRecord(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.maxDiff = None

    def test_parse_bb_record(self):
        profile = user_profile_pb2.Profile()
        profile.is_full = True
        time = 1567425600

        for keyword_id, uint_values in (
            (887, [2]),
            (220, [286458]),
            (198, [99136256, 2596874, 264646912, 103822592, 527804673, 20582926, 593340687]),
            (547, [1024, 1025, 1036, 1038, 1039, 1042, 1044, 1049, 1050, 1051]),
            (888, [4]),
            (601, [34, 78, 141, 210, 84, 207, 92, 61, 230, 232, 233, 317, 115]),
            (549, [1152, 1199, 1177, 1149, 1150, 1215]),
            (886, [1]),
            (885, [1]),
        ):
            item = profile.items.add()
            item.keyword_id = keyword_id
            item.uint_values[:] = uint_values
            item.update_time = time

        for keyword_id, weighted_uint_values in (
            (878, (
                (0, 1),
                (1, 864000),
                (2, 125205),
                (3, 10379),
                (4, 403),
                (5, 12),
            )),
            (175, (
                (0, 1),
                (1, 864000),
                (2, 125205),
                (3, 10379),
                (4, 415),
            )),
            (879, (
                (0, 3695),
                (1, 112666),
                (2, 883638),
            )),
            (281, (
                (188, 1000000),
                (254, 1000000),
            )),
            (877, (
                (0, 78000),
                (1, 922000),
            )),
            (880, (
                (0, 3695),
                (1, 29763),
                (2, 82903),
                (3, 553004),
                (4, 330633),
            )),
            (174, (
                (0, 78000),
                (1, 922000),
            )),
            (543, (
                (0, 1),
                (1, 864000),
                (2, 125205),
                (3, 10379),
                (4, 403),
                (5, 12),
            )),
            (176, (
                (0, 3695),
                (1, 112666),
                (2, 883638),
            )),
            (614, (
                (0, 3695),
                (1, 29763),
                (2, 82903),
                (3, 553004),
                (4, 330633),
            )),
            (1084, (
                (1301, 581175),
                (2286, 509921),
                (2299, 83594),
                (2306, 536286),
            )),
            (546, (
                (2466, 406661),
                (2627, 448477),
                (2874, 20158),
            )),
        ):
            item = profile.items.add()
            item.keyword_id = keyword_id
            item.update_time = time
            for value, weight in weighted_uint_values:
                weighted_uint_value = item.weighted_uint_values.add()
                weighted_uint_value.first = value
                weighted_uint_value.weight = weight

        for keyword_id, pair_values in (
            (595, (
                (1018646792, 617080),
                (45311488, 601783),
                (142201088, 563997),
                (81146637, 483766),
                (123212288, 705472),
            )),
            (216, (
                (743, 1),
                (744, 1),
                (600, 1),
                (668, 1),
                (555, 1),
                (627, 1),
                (558, 1),
                (636, 1),
                (657, 1),
                (732, 1),
                (520, 1),
                (523, 1),
                (548, 1),
                (549, 1),
                (587, 1),
                (595, 1),
                (719, 1),
            )),
            (569, (
                (543, 1),
                (176, 2),
                (174, 1),
                (614, 4),
            )),
        ):
            item = profile.items.add()
            item.keyword_id = keyword_id
            item.update_time = time
            for first, second in pair_values:
                pair_value = item.pair_values.add()
                pair_value.first = first
                pair_value.second = second

        for keyword_id, weighted_pair_values in (
            (217, (
                (15, 1, 1000000),
                (15, 0, 0),
                (578, 0, 859712),
                (223, 0, 840656),
            )),
        ):
            item = profile.items.add()
            item.keyword_id = keyword_id
            item.update_time = time
            for first, second, weight in weighted_pair_values:
                weighted_pair_value = item.weighted_pair_values.add()
                weighted_pair_value.first = first
                weighted_pair_value.second = second
                weighted_pair_value.weight = weight

        correct_parsed_profile = {
            'gender': {'f': 0.922, 'm': 0.078},
            'age_segments': {
                '0_17': 1e-06,
                '18_24': 0.864,
                '25_34': 0.125205,
                '35_44': 0.010379,
                '45_99': 0.000415,
            },
            'income_segments': {
                'A': 0.003695,
                'B': 0.112666,
                'C': 0.883638,
            },
            'income_5_segments': {
                'A': 0.003695,
                'B1': 0.029763,
                'B2': 0.082903,
                'C1': 0.553004,
                'C2': 0.330633,
            },
            'user_age_6s': {
                '0_17': 1e-06,
                '18_24': 0.864,
                '25_34': 0.125205,
                '35_44': 0.010379,
                '45_54': 0.000403,
                '55_99': 1.2e-05,
            },
            'offline_gender': {'f': 0.922, 'm': 0.078},
            'offline_user_age_6s': {
                '0_17': 1e-06,
                '18_24': 0.864,
                '25_34': 0.125205,
                '35_44': 0.010379,
                '45_54': 0.000403,
                '55_99': 1.2e-05,
            },
            'offline_income_segments': {
                'A': 0.003695,
                'B': 0.112666,
                'C': 0.883638,
            },
            'offline_income_5_segments': {
                'A': 0.003695,
                'B1': 0.029763,
                'B2': 0.082903,
                'C1': 0.553004,
                'C2': 0.330633,
            },
            'exact_socdem': {
                'age_segment': '18_24',
                'gender': 'f',
                'income_5_segment': 'C2',
                'income_segment': 'C',
            },
            'offline_exact_socdem': {
                'age_segment': '18_24',
                'gender': 'f',
                'income_5_segment': 'C2',
                'income_segment': 'C',
            },
            'yandex_loyalty': 0.286458,
            'top_common_site_ids': [
                99136256,
                2596874,
                264646912,
                103822592,
                527804673,
                20582926,
                593340687,
            ],
            'affinitive_site_ids': {
                '1018646792': 0.61708,
                '123212288': 0.705472,
                '142201088': 0.563997,
                '45311488': 0.601783,
                '81146637': 0.483766,
            },
            'heuristic_common': [
                1024,
                1025,
                1036,
                1038,
                1039,
                1042,
                1044,
                1049,
                1050,
                1051,
            ],
            'heuristic_internal': [
                1152,
                1199,
                1177,
                1149,
                1150,
                1215,
            ],
            'heuristic_segments': {
                '520': 1,
                '523': 1,
                '548': 1,
                '549': 1,
                '555': 1,
                '558': 1,
                '587': 1,
                '595': 1,
                '600': 1,
                '627': 1,
                '636': 1,
                '657': 1,
                '668': 1,
                '719': 1,
                '732': 1,
                '743': 1,
                '744': 1,
            },
            'interests_composite': {
                '15': {
                    '0': 0.0,
                    '1': 1.0,
                },
            },
            'longterm_interests': [
                34,
                78,
                141,
                210,
                84,
                207,
                92,
                61,
                230,
                232,
                233,
                317,
                115,
            ],
            'marketing_segments': {
                '188': 1.0,
                '254': 1.0,
            },
            'probabilistic_segments': {
                '223': {'0': 0.840656},
                '578': {'0': 0.859712},
            },
            'lal_internal': {
                '2466': 0.406661,
                '2627': 0.448477,
                '2874': 0.020158,
            },
            'trainable_segments': {
                '1301': 0.581175,
                '2286': 0.509921,
                '2299': 0.083594,
                '2306': 0.536286,
            },

        }

        parsed_profile, update_times = bb_helpers.parse_bb_record(profile)

        self.assertEquals(
            parsed_profile,
            correct_parsed_profile,
        )
