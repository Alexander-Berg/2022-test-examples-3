import unittest
from parse_deps import parse_deps
from restrict_deps import trim_sep, restrict_deps, \
    get_common_part, remove_from_start


class RestrictDepsTest(unittest.TestCase):
    def test_trim_sep(self):
        self.assertEquals('a', trim_sep('a'))
        self.assertEquals('a/b', trim_sep('/a/b'))
        self.assertEquals('a/b/c', trim_sep('/a/b/c/'))

    def test_restrict_deps(self):
        deps = parse_deps([
            'a',
            '|-->o/b0',
            '|-->a/b',
            '|   |-->o/c0',
            '|   |   |-->o/d0',
            '|   |-->a/c',
            '|   |   |-->a/d',
            'e',
            'a/f',
            '|-->a/g'
        ])
        self.assertEquals(parse_deps([
            'a',
            '|-->a/b',
            '|   |-->a/c',
            '|   |   |-->a/d',
            'a/f',
            '|-->a/g'
        ]), restrict_deps(deps, 'a'))

    def test_get_common_part(self):
        self.assertEquals('', get_common_part([]))
        self.assertEquals('a', get_common_part(['a']))
        self.assertEquals('', get_common_part(['a', 'b']))
        self.assertEquals('a', get_common_part(['a/b', 'a/c']))
        self.assertEquals('a', get_common_part(['a/b', 'a/c', 'a/c/d']))
        self.assertEquals(
            'a/b', get_common_part(['a/b', 'a/b/c', 'a/b/c/d']))
        self.assertEquals(
            'a', get_common_part(['a/b', 'a/b/c', 'a/b/c/d', 'a']))
        self.assertEquals(
            '', get_common_part(['a/b', 'a/b/c', 'a/b/c/d', 'e']))

    def test_remove_from_start(self):
        self.assertEquals('c/d', remove_from_start('a/b/c/d', 'a/b'))
        self.assertEquals('/', remove_from_start('a/b', 'a/b'))
