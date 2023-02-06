import json
import os
import unittest

import cv2

from screenqual.core.screenshot import Screenshot


# Base class for all unit tests run on different filters

class UnitTest(unittest.TestCase):
    def _get_analyser(self):
        return self.analyser

    def assert_has_anomaly(self, relative_path):
        self.assertTrue(self._analyze(self._get_analyser(), relative_path))

    def assert_no_anomaly(self, relative_path):
        self.assertFalse(self._analyze(self._get_analyser(), relative_path))

    def _analyze(self, analyser, relative_path):
        base_path = os.path.join(os.path.dirname(__file__))
        img = cv2.imread(base_path + "/" + relative_path)
        result = analyser.execute(Screenshot(img))
        self._assert_json_serializable(result.info)
        return result

    def _assert_json_serializable(self, obj):
        json.dumps(obj)
