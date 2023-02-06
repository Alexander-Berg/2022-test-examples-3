import unittest

from context import daas
from daas.external.dolbilo.operations import CompareDocs


class Test(unittest.TestCase):

    def test_generate_json_diffs(self):
        a = {"int": 1, "Object": {"float": 10.00, "deep": {"object": {"deep_key": {"bool": False}}}},
             "ObjArr": [{"Caption": "A", "Value": 65}, {"Caption": "B", "Value": 66}], "Arr": [0, 1, 2], "non_diff": 1,
             "dict": {"key": "val"}}
        b = {"int": 9, "Object": {"float": 12.00, "deep": {"object": {"deep_key": {"bool": True}}}},
             "ObjArr": [{"Caption": "A", "Value": 10}, {"Caption": "C", "Value": 66}], "Arr": [7, 1, 2], "non_diff": 1,
             "dict": ["key", "val"], "list": ["str1", "str2"]}
        expected = {"int": [1, 9], "Object": {"float": [10.00, 12.00],
                    "deep": {"object": {"deep_key": {"bool": [False, True]}}}},
                    "ObjArr": {"0": {"Value": [65, 10]}, "1": {"Caption": ["B", "C"]}}, "Arr": {"0": [0, 7]},
                    "dict": ["<type 'dict'>", "<type 'list'>"], "list": [None, ["str1", "str2"]]}

        for diff in CompareDocs.generate_json_diffs(a, b):
            self.assertEqual(diff, expected)


if __name__ == '__main__':
    unittest.main()
