import os
from unittest import TestCase

import yatest.common

from lib import instance


def _get_test_path(test_path):
    try:
        path = yatest.common.test_source_path(test_path)
    except NotImplementedError:
        path = os.path.join(os.path.abspath(os.path.dirname(__file__)), test_path)

    return path


class TestInstance(TestCase):
    def test_to_instance_list(self):
        with open(_get_test_path("resources/instances.json")) as f:
            instance_info = instance._extract_instances(f.read())

        instance_info.sort(key=lambda i: i.path)

        expected = [
            instance.Instance(
                '/place/db/iss3/instances/13425_testing_market_formalizer_fol_7TDpOH0VdxV',
                '',
                'testing_market_formalizer_fol#testing_market_formalizer_fol-1493723297009',
                ' '.join(['FOL_MARKET_TEST_FORMALIZER', 'a_ctype_testing', 'a_dc_fol', 'a_geo_msk',
                          'a_itype_marketformalizer', 'a_line_fol-2', 'a_metaprj_market', 'a_prj_marketir',
                          'a_tier_none',
                          'a_topology_group-FOL_MARKET_TEST_FORMALIZER', 'a_topology_stable-97-r167',
                          'a_topology_version-stable-97-r167', 'a_topology_stable-97-r167', 'enable_hq_report'])

            ),
            instance.Instance(
                '/place/db/iss3/instances/21980_testing_market_light_matcher_fol_F9gRkX4TrOK',
                '',
                'testing_market_light_matcher_fol#testing_market_light_matcher_fol-1493723214109',
                ' '.join(['FOL_MARKET_TEST_LMATCHER', 'a_ctype_testing', 'a_dc_fol', 'a_geo_msk',
                          'a_itype_marketlightmatcher', 'a_line_fol-2', 'a_metaprj_market', 'a_prj_marketir',
                          'a_tier_none',
                          'a_topology_group-FOL_MARKET_TEST_LMATCHER', 'a_topology_stable-97-r92',
                          'a_topology_version-stable-97-r92', 'a_topology_stable-97-r92', 'enable_hq_report'])
            ),
            instance.Instance(
                '/place/db/iss3/instances/9624_testing_market_clutcher_fol_WA88FWZFTYC',
                '123',
                'testing_market_clutcher_fol#testing_market_clutcher_fol-1493724001899',
                ' '.join(['a_itype_marketclutcher', 'a_topology_version-stable-98-r35',
                          'a_topology_group-FOL_MARKET_TEST_CLUTCHER', 'a_prj_marketir', 'a_line_fol-2',
                          'FOL_MARKET_TEST_CLUTCHER', 'a_geo_msk', 'a_tier_none', 'a_metaprj_market',
                          'a_topology_stable-98-r35', 'a_ctype_testing', 'a_dc_fol', 'a_topology_stable-98-r35',
                          'enable_hq_report'])
            ),
            instance.Instance(
                '/place/db/iss3/instances/9885_testing_market_matcher_fol_H9AbJDFc32N',
                '234',
                'testing_market_matcher_fol#testing_market_matcher_fol-1493724170098',
                ' '.join(
                    ['FOL_MARKET_TEST_MATCHER', 'a_ctype_testing', 'a_dc_fol', 'a_geo_msk', 'a_itype_marketmatcher',
                     'a_line_fol-2', 'a_metaprj_market', 'a_prj_marketir', 'a_tier_none',
                     'a_topology_group-FOL_MARKET_TEST_MATCHER', 'a_topology_stable-97-r143',
                     'a_topology_version-stable-97-r143', 'a_topology_stable-97-r143', 'enable_hq_report'])
            )
        ]

        def check_inst_eq(first, second):
            self.assertEqual(first.path, second.path)
            self.assertEqual(first.conf_id, second.conf_id)
            self.assertEqual(first.service_name, second.service_name)
            self.assertNotEqual(first.service_name, "")
            self.assertNotEqual(first.service_id, "")
            self.assertEqual(first.service_id, second.service_id)
            self.assertListEqual(first.tags, second.tags)

        for i in xrange(len(expected)):
            check_inst_eq(instance_info[i], expected[i])

    def test_empty_instance(self):
        self.assertEqual(0, len(instance._extract_instances("\n")))

    def test_file_check_sums(self):
        path = _get_test_path('test_paths')

        inst = instance.Instance(path, "", "", "")
        inst.check_paths = ["checked"]

        files_to_check = [
            'checked/checked_subpath/file_in_subpath.txt',
            'checked/first_checked.txt',
            'checked/second_checked.txt'
        ]

        check_sums = inst.get_file_check_sums(files_to_check)
        paths = [d["path"] for d in check_sums]
        paths.sort()

        self.assertListEqual(paths, files_to_check)
        self.assertEqual(len(files_to_check), len(set([d["md5"] for d in check_sums])))

    def test_extra_resources(self):
        instance_with_report = _get_test_path('instances/instance_with_report')
        inst = instance.Instance(instance_with_report, "", "", "")

        self.assertEqual([{'resourceType': 'MARKET_REPORT', 'resourceTaskId': '202281379'}], inst.get_extra_resources())

    def test_service_in_deploy(self):
        def _check_inst_deploy(inst_path, expected):
            instance_with_report = _get_test_path(inst_path)
            inst = instance.Instance(instance_with_report, "", "", "")
            self.assertEqual(expected, inst.deploy_in_progress())

        _check_inst_deploy('instances/instance_with_report', False)
        _check_inst_deploy('instances/instance_with_report_in_deploy', True)
