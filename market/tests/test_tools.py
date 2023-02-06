import unittest
from modules.project_models import ProjectStorage
from modules.tools import DoMetricsAggregation


class TestDataReceiver(unittest.TestCase):

    @staticmethod
    def GetDataDir():
        return "tests/tests_resources/tools_data"

    def test_aggregation(self):
        storageBeforeAggregation = ProjectStorage("{}/{}".format(self.GetDataDir(), "aggregated_config_before_aggregate.js"))
        storageBeforeAggregation.Load()

        DoMetricsAggregation(storageBeforeAggregation.ProjectsTreeRoot)

        storageAfterAggregation = ProjectStorage(
            "{}/{}".format(self.GetDataDir(), "aggregated_config_after_aggregate.js"))
        storageAfterAggregation.Load()

        self.assertEqual(storageBeforeAggregation.ProjectsTreeRoot, storageAfterAggregation.ProjectsTreeRoot)

    def test_aggregation(self):
        storageBeforeAggregation = ProjectStorage("{}/{}".format(self.GetDataDir(), "aggregated_config_before_aggregate.js"))
        storageBeforeAggregation.Load()

        DoMetricsAggregation(storageBeforeAggregation.ProjectsTreeRoot)

        storageAfterAggregation = ProjectStorage(
            "{}/{}".format(self.GetDataDir(), "aggregated_config_after_aggregate.js"))
        storageAfterAggregation.Load()

        self.assertEqual(storageBeforeAggregation.ProjectsTreeRoot, storageAfterAggregation.ProjectsTreeRoot)


if __name__ == '__main__':
    unittest.main()
