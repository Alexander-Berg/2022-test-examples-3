# -+- coding: utf-8 -+-

from getter.service import lms
from getter.validator import VerificationError

from market.pylibrary import yenv

import os
import json
import unittest
import subprocess
import yatest.common
from market.pylibrary.yatestwrap.yatestwrap import source_path


class Test(unittest.TestCase):

    def prepare_pbsn_file(self, pbsn_path):
        DATA_DIR = source_path('market/getter/tests/data/')
        PBSNCAT_BIN = yatest.common.binary_path('market/idx/tools/pbsncat/bin/pbsncat')

        json_path = os.path.join(DATA_DIR, pbsn_path.split('.')[0] + '.json')
        json_string_path = pbsn_path.split('.')[0] + '-s.json'

        with open(json_path) as json_file:
            json_value = json.loads(json_file.read())

        with open(json_string_path, 'w+') as json_string_file:
            json_string_file.write(json.dumps(json_value))

        with open(json_string_path) as json_input:
            with open(pbsn_path, 'w+') as pbsn_output:
                subprocess.check_call(
                    args=[
                        PBSNCAT_BIN,
                        '--input-format', 'json',
                        '--format', 'pbsn',
                        '--magic', 'MLMS',
                    ],
                    stdin=json_input,
                    stdout=pbsn_output
                )

    def test_lms_validator(self):
        VALID_PATH = 'lms-valid.pbuf.sn'
        INVALID1_PATH = 'lms-invalid1.pbuf.sn'
        INVALID2_PATH = 'lms-invalid2.pbuf.sn'

        self.prepare_pbsn_file(VALID_PATH)
        self.prepare_pbsn_file(INVALID1_PATH)
        self.prepare_pbsn_file(INVALID2_PATH)

        lms.validate_lms(open(VALID_PATH, 'rb'))

        with self.assertRaises(VerificationError):
            lms.validate_lms(open(INVALID1_PATH, 'rb'))

        validation_errors_ids = range(10, 14) + range(15, 28)
        exception_regex = "".join(
            r"(?=[\S\s]*#{0:03d})".format(error_id) for error_id in validation_errors_ids) + r"[\S\s]*"
        with self.assertRaisesRegexp(VerificationError, exception_regex):
            lms.validate_lms(open(INVALID2_PATH, 'rb'))

    def test_lms_resources(self):
        LMS_FILE_NAMES = {
            (yenv.TESTING, yenv.STRATOCASTER): "lms.pbuf.sn",
            (yenv.PRODUCTION, yenv.STRATOCASTER): "lms.pbuf.sn",
            (yenv.TESTING, yenv.STRATOCASTER): "business_warehouse_info.json",
            (yenv.PRODUCTION, yenv.STRATOCASTER): "business_warehouse_info.json",
        }

        for (envtype, mitype) in LMS_FILE_NAMES:
            yenv.set_environment_type(envtype)
            yenv.set_marketindexer_type(mitype)

            resources = lms.create_resources()
            self.assertEqual(len(resources), 2)

            for resource_name in resources.keys():
                resource = resources[resource_name]
                self.assertEqual(resource.name, resource_name)
                self.assertEqual(len(resource.urls), 1)

                url = resource.urls[0]
                file_name = url.split("/")[-1]
                self.assertEqual(file_name, resource_name)


if __name__ == '__main__':
    unittest.main()
