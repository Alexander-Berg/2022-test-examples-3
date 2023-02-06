import yaml
import os
from unittest.mock import patch
from collections import OrderedDict


from settings import BASE_DIR
from common.models import Server
from common.tests import CommonTestCase
from firestarter.models import JobTank
from firestarter.views.tank_finder import TankFinder
from api.util.config import transform_config, extract_data_from_config, extract_address, extract_tanks, extract_target_and_port, \
    BadRequest
from api.files.result_config import result_config_ini, result_config_yaml


class TransformConfigTest(CommonTestCase):

    maxDiff = None

    @patch.object(TankFinder, 'approve_target')
    def test_dict_convertion(self, mock):
        dict_config = {'phantom': {'ssl': 1, 'ignore_lock': 1, 'address': 'ya.ru'}, 'uploader': {'enabled': False}}
        output, tanks = transform_config(dict_config)
        self.assertEqual(output['configinitial'], dict_config)
        self.assertIsInstance(output, dict)
        self.assertEqual(tanks, '')

    @patch.object(TankFinder, 'approve_target')
    def test_ini_convertion(self, mock):
        with open(os.path.join(BASE_DIR, 'www/api/files/config.ini'), 'r') as input_config:
            str_config = input_config.read()
        new_config, tanks = transform_config(str_config)
        self.assertDictContainsSubset(result_config_ini, new_config)
        self.assertEqual(tanks, '')

    @patch.object(TankFinder, 'approve_target')
    def test_yaml_conversion(self, mock):
        with open(os.path.join(BASE_DIR, 'www/api/files/config.yaml'), 'r') as input_config:
            str_config = input_config.read()
        new_config, tanks = transform_config(str_config)
        self.assertEqual(tanks, 'vla1-3afb98127029.qloud-c.yandex.net')
        self.assertDictContainsSubset( result_config_yaml, new_config)

    def test_invalid_input(self):
        self.assertRaises(BadRequest, transform_config, False)


class ExpandConfigTest(CommonTestCase):

    @patch.object(TankFinder, 'approve_target')
    def test_fields(self, mock_function):
        input_dict = {
            'phantom': {'address': 'checker:80', 'instances': 435, 'ammofile': '/tmp/ok'},
            'meta': {'job_name': 'CHECK', 'operator': 'Kitty', 'ver': 12},

        }
        output, tanks = extract_data_from_config(input_dict)
        self.assertTrue(isinstance(output['srv'], Server))
        self.assertTrue(isinstance(output['component'], int))
        self.assertEqual(output['person'], 'Kitty')
        self.assertEqual(output['name'], 'CHECK')
        self.assertEqual(output['dsc'], '')
        self.assertEqual(output['status'], 'queued')
        self.assertEqual(output['ver'], 12)
        self.assertEqual(output['instances'], 435)
        self.assertEqual(output['ammo_path'], '/tmp/ok')
        self.assertEqual(output['finalized'], False)
        self.assertEqual(output['configinitial'], None)


class ExtractAddressTest(CommonTestCase):

    @patch.object(TankFinder, 'approve_target')
    def test_phantom_address(self, mock_function):
        config = {'phantom': {'address': 'checker'}}
        target, port = extract_address(config)
        self.assertEqual(port, 80)
        self.assertEqual(target.host, 'checker')

    @patch.object(TankFinder, 'approve_target')
    def test_bfg_address(self, mock_function):
        config = {'bfg': {'address': 'bfg_address:145'}}
        target, port = extract_address(config)
        self.assertEqual(target.host, 'bfg_address')
        self.assertEqual(port, 145)

    def empty_target(self):
        self.assertEqual(extract_address({}), ())


class ExtractTargetTest(CommonTestCase):
    def test_ipv6(self):
        self.assertEqual(
            extract_target_and_port('[2a02:6b8::2:242]:295'),
            ('2a02:6b8::2:242', 295)
        )

    def test_non_default_port(self):
        self.assertEqual(
            extract_target_and_port('[192.168.1.12]:90'),
            ('192.168.1.12', 90)
        )

    def test_default_port(self):
        self.assertEqual(
            extract_target_and_port('ya.ru'),
            ('ya.ru', 80)
        )


class ExtractTanksTest(CommonTestCase):

    def test_server_created(self):
        extract_tanks("tank1, tank2", 1)
        self.assertEqual(len(Server.objects.filter(host__startswith='tank')), 2)

    def test_jobtank_created(self):
        extract_tanks("tank1, tank2", 1)
        self.assertEqual(len(JobTank.objects.filter(tank__host__startswith='tank', job_id=1)), 2)
