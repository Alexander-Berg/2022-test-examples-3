import unittest
from parse_deps import parse_line, parse_deps


class ParseDepsTest(unittest.TestCase):
    def test_parse_line(self):
        name = 'contrib/java/commons-logging/commons-logging/1.1.1'
        line = ('|   |   |-->' + name
                + ' (omitted because of confict with 1.2)')
        res = parse_line(line)
        self.assertEqual(4, res['depth'])
        self.assertEquals(name, res['name'])

    def test_parse_oneline_deps(self):
        res = parse_deps(['a', 'b'])
        self.assertEquals(set(['a', 'b']), res['nodes'])
        self.assertEquals(set(), res['links'])

    def test_parse_one_link_deps(self):
        deps = [
            'a',
            '|-->b'
        ]
        res = parse_deps(deps)
        self.assertEquals(set(['a', 'b']), res['nodes'])
        self.assertEquals(set([('a', 'b')]), res['links'])

    def test_parse_deps(self):
        deps = [
            'a',
            '|-->b',
            '|   |-->c',
            '|-->d'
        ]
        res = parse_deps(deps)
        self.assertEquals(set(['a', 'b', 'c', 'd']), res['nodes'])
        self.assertEquals(set([('a', 'b'), ('b', 'c'), ('a', 'd')]),
                          res['links'])

    def test_parse_deps2(self):
        deps = [
            'a',
            '|-->b0',
            '|-->b',
            '|   |-->c0',
            '|   |   |-->d0',
            '|   |-->c',
            '|   |   |-->d',
            'e',
            'f',
            '|-->g',
        ]
        res = parse_deps(deps)
        self.assertEquals(
            set(['a', 'b0', 'b', 'c0', 'c', 'd0', 'd', 'e', 'f', 'g']),
            res['nodes'])
        self.assertEquals(
            set([('a', 'b0'), ('a', 'b'), ('b', 'c0'), ('c0', 'd0'),
                 ('b', 'c'), ('c', 'd'), ('f', 'g')]),
            res['links'])


if __name__ == '__main__':
    unittest.main()
