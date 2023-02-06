from glob import glob
import cv2
from screenqual.core.screenshot import Screenshot
from tests.regression.precision_recall_calculator import PrecisionRecallCalculator
import os
import warnings
import unittest
import inspect


def _append_result(results, expected, found, path):
    if expected and found:
        results.append("TP:: " + path + "..." + str(found))
    elif not expected and found:
        results.append("FP:: " + path + "..." + str(found))
    elif expected and not found:
        results.append("FN:: " + path + "..." + str(found))
    else:
        results.append("TN:: " + path + "..." + str(found))


class TestRegression(unittest.TestCase):
    def setUp(self):
        self.results = []

    def tearDown(self):
        log_filename = os.path.splitext(inspect.getfile(self.__class__))[0] + "_" + self._testMethodName + ".log"
        with open(log_filename, "w") as log_file:
            for result in sorted(self.results):
                log_file.write(result + os.linesep)

    def process_path(self, path, has_anomaly, detector, pr_calculator, extension, img_callback=None):
        filenames = glob(path + "*." + extension)

        for filename in filenames:
            img = cv2.imread(filename)
            if img_callback:
                img = img_callback(filename, img)
            screenshot = Screenshot(img)
            got_value = detector.execute(screenshot)
            _append_result(self.results, has_anomaly, got_value, filename)
            pr_calculator.expected(has_anomaly).found(got_value)

        return len(filenames)

    def fscore(self, paths_with_anomaly, paths_without_anomaly,
               filter, f_score, pr_calculator_name,
               extensions=["png"], img_callback=None):
        DATA_ROOT = os.path.join(os.path.dirname(__file__), "../..", "data")
        paths_with_anomaly = [DATA_ROOT + el for el in paths_with_anomaly]
        paths_without_anomaly = [DATA_ROOT + el for el in paths_without_anomaly]
        pr_calculator = PrecisionRecallCalculator(pr_calculator_name)
        for path in paths_with_anomaly:
            processed = 0
            for ext in extensions:
                processed += self.process_path(path, True, filter, pr_calculator, ext, img_callback)
            if processed == 0:
                warnings.warn("No files found at " + path, UserWarning)
        for path in paths_without_anomaly:
            processed = 0
            for ext in extensions:
                processed += self.process_path(path, False, filter, pr_calculator, ext, img_callback)
            if processed == 0:
                warnings.warn("No files found at " + path, UserWarning)
        print(pr_calculator)
        self.assertTrue(pr_calculator.fscore() > f_score)
