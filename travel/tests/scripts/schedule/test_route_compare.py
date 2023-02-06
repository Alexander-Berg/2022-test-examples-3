# -*- coding: utf-8 -*-

from travel.rasp.admin.lib.unittests.testcase import TestCase
from travel.rasp.admin.scripts.schedule.utils.route_compare import get_path_relation, PATH_RELATIONS


module_fixtures = [
    'travel.rasp.admin.tester.fixtures.www:countries.yaml',
    'travel.rasp.admin.tester.fixtures.www:regions.yaml',
    'travel.rasp.admin.tester.fixtures.www:settlements.yaml',
    'travel.rasp.admin.tester.fixtures.www:stations.yaml'
]


class PathRelationTest(TestCase):
    class_fixtures = module_fixtures

    def testNoIntersection(self):
        path1 = [1, 2, 3]
        path2 = [4, 5, 6]

        self.assertEqual(get_path_relation(path1, path2).code, PATH_RELATIONS.NO_INTERSECTION)

    def testSubPath(self):
        variants = [
            (
                [1, 2, 3],
                [[1, 2, 3, 4], [0, 1, 2, 3], [0, 1, 2, 3, 4], [0, 1, 2, 3, 4, 5, 6], [-2, -1, 0, 1, 2, 3]]
            ),
            (
                [3, 4],
                [[1, 2, 3, 4], [0, 1, 2, 3, 4], [0, 1, 2, 3, 4, 5, 6]]
            )
        ]

        for path1, path2_variants in variants:
            for path2 in path2_variants:
                self.assertEqual(get_path_relation(path1, path2).code, PATH_RELATIONS.SUB_PATH,
                                 "%s vs %s" % (path1, path2))

    def testReverseSubPath(self):
        path1_variants = [[3, 2, 1]]
        path2_variants = [[1, 2, 3, 4], [0, 1, 2, 3], [0, 1, 2, 3, 4], [0, 1, 2, 3, 4, 5, 6], [-2, -1, 0, 1, 2, 3]]

        for path1 in path1_variants:
            for path2 in path2_variants:
                self.assertEqual(get_path_relation(path1, path2).code, PATH_RELATIONS.REVERSED_SUB_PATH,
                                 "%s vs %s" % (path1, path2))

    def testSuperPath(self):
        path1_variants = [[1, 2, 3, 4], [0, 1, 2, 3], [0, 1, 2, 3, 4], [0, 1, 2, 3, 4, 5, 6], [-2, -1, 0, 1, 2, 3]]
        path2 = [1, 2, 3]

        for path1 in path1_variants:
            self.assertEqual(get_path_relation(path1, path2).code, PATH_RELATIONS.SUPER_PATH)

    def testReverseSuperPath(self):
        path1 = [3, 2, 1]
        path2_variants = [[1, 2, 3, 4], [0, 1, 2, 3], [0, 1, 2, 3, 4], [0, 1, 2, 3, 4, 5, 6], [-2, -1, 0, 1, 2, 3]]

        for path2 in path2_variants:
            self.assertEqual(get_path_relation(path2, path1).code, PATH_RELATIONS.REVERSED_SUPER_PATH)
