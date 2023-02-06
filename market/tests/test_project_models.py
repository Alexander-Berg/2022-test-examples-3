import unittest
from modules.metric_models import Metric
from modules.project_models import Project
from modules import project_constants
import json


class TestProjects(unittest.TestCase):
    @staticmethod
    def GetDataDir():
        return "tests/tests_resources/data_storage_projects_data"

    def test_simple_project_load_correct(self):
        filepath = "{}/correct_simple_project.json".format(self.GetDataDir())
        with open(filepath) as f:
            jsonRepr = json.load(f)
        project = Project(jsonRepr=jsonRepr)
        self.assertEqual(project.Type, project_constants.AVAILABLE_TYPES[0])
        self.assertEqual(project.Id, "1")
        self.assertEqual(project.Title, "title1")
        self.assertEqual(project.Description, "descr1")
        self.assertEqual(project.Status, project_constants.AVAILABLE_STATUSES[1])
        self.assertEqual(project.Tags, ["t1", "t2"])
        self.assertEqual(project.Metrics, [Metric(jsonRepr=jsonRepr["Metrics"][0])])
        self.assertEqual(project.Children, [])
        self.assertEqual(len(project.Errors), 2)
        self.assertEqual(project.Errors[0].Source, "111")
        self.assertEqual(project.Errors[0].Error, "222")
        self.assertEqual(project.Errors[1].Source, "333")
        self.assertEqual(project.Errors[1].Error, "444")

    def test_children_project_load_correct(self):
        filepath = "{}/correct_children_project.json".format(self.GetDataDir())
        with open(filepath) as f:
            jsonRepr = json.load(f)
        project = Project(jsonRepr=jsonRepr)
        child = project.Children[0]
        project.Children = []
        self.assertEqual(project, child)



if __name__ == '__main__':
    unittest.main()
