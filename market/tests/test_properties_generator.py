from properties_generator import create_properties_files
import yatest.common
import os
import glob
from unittest import TestCase
import logging


class TestPropertiesGenerator(TestCase):

    def test_generate(self):
        test_dir = 'market/infra/java-application/properties-generator/tests'
        test_dada_dir = yatest.common.source_path(os.path.join(test_dir, 'data'))
        expected_data_dir = yatest.common.source_path(os.path.join(test_dir, 'expected'))

        result_dir = os.path.join(yatest.common.test_output_path(), 'result')
        logging.info('Test result dir: %s' % result_dir)

        create_properties_files(test_dada_dir, result_dir)

        expected_files = get_all_files(expected_data_dir)
        results_files = get_all_files(result_dir)

        assert len(expected_files) > 0  # Check test test works
        assert set(expected_files) == set(results_files)
        for file in expected_files:
            expected_file = os.path.join(expected_data_dir, file)
            actual_file = os.path.join(result_dir, file)
            logging.info('Comparing %s with %s' % (expected_file, actual_file))
            assert open(expected_file, 'r').read() == open(actual_file, 'r').read()


def get_all_files(dir):
    files = glob.glob(dir + '/**/*.properties', recursive=True)
    return [os.path.relpath(file, dir) for file in files]
