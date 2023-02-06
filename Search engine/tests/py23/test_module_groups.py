import argparse

from search.martylib.modules import ModuleGroups
from search.martylib.test_utils import TestCase


class TestModuleGroups(TestCase):
    def test_resolve(self):
        args = argparse.Namespace()
        groups = ModuleGroups()

        groups['alpha'] = {'delta', 'echo'}
        groups['bravo'] = {'foxtrot', 'golf', '-echo'}
        groups['charlie'] = {'hotel', 'india', '-delta'}

        args.modules = ['alpha', 'bravo', 'juliett']

        self.assertEqual(
            sorted(groups.resolve(args).modules),
            [
                'delta',
                'foxtrot',
                'golf',
                'juliett',
            ],
        )
