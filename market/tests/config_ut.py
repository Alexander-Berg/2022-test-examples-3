from unittest import TestCase
from base_config.config import BaseConfig
from base_config.section import Section, EPatchResult


class TestBaseConfig(TestCase):
    def test(self):
        section_1 = Section('First', [('aaa', 'bbb')])
        section_2 = Section('Second', [('ccc', 'ddd')])
        section_3 = Section('Third')

        config = BaseConfig()
        config.add_section(section_1)
        config.add_sections([section_2, section_3])

        expected = '\n'.join([
            '<First>',
            '    aaa bbb',
            '</First>',
            '',
            '<Second>',
            '    ccc ddd',
            '</Second>',
            '',
            '<Third>',
            '</Third>',
        ])
        self.assertEqual(config.render(), expected)
        config.save('test.conf')
        with open('test.conf') as result_file:
            self.assertEqual(result_file.read(), expected)

    def _get_config_for_patch(self):
        config = BaseConfig()
        config.add_sections([
            Section('Child', [('common', 'fff'), ('eee', 'hhh')]),
            Section('Child', [('common', 'ttt'), ('sss', 'hhh')]),
            Section('UniqChild', [('common', 'qqq'), ('atr', 'vvv')])
        ])
        return config

    def _get_patched_config(self, patch):
        config = self._get_config_for_patch()
        patch_ = {}
        expected = {}
        for k, v in patch.items():
            patch_[k] = v[0]
            expected[k] = v[1]
        res = config.patch(patch_)
        self.assertEqual(res, expected)
        return config

    def test_patch_add_and_mofify(self):
        patch = {
            'Child.common': ('abc', EPatchResult.CHANGED),
            'Child.new': ('hhh', EPatchResult.ADDED),
            'aaa': ('bbb', EPatchResult.ERROR),
            'NewChild.attr': ('val', EPatchResult.ADDED)
        }

        config = self._get_patched_config(patch)

        self.assertEqual(config.render().split('\n'), [
            '<Child>',
            '    common abc',
            '    eee hhh',
            '    new hhh',
            '</Child>',
            '',
            '<Child>',
            '    common abc',
            '    sss hhh',
            '    new hhh',
            '</Child>',
            '',
            '<UniqChild>',
            '    common qqq',
            '    atr vvv',
            '</UniqChild>',
            '',
            '<NewChild>',
            '    attr val',
            '</NewChild>',
        ])

    def test_patch_remove(self):
        patch = {
            'Child.common': ('__remove__', EPatchResult.REMOVED),
            'Child.new': ('__remove__', EPatchResult.NOTMODIFIED),
            'aaa': ('__remove__', EPatchResult.NOTMODIFIED),
            'NewChild.attr': ('__remove__', EPatchResult.NOTMODIFIED),
            'UniqChild': ('__remove__', EPatchResult.REMOVED)
        }

        config = self._get_patched_config(patch)

        self.assertEqual(config.render().split('\n'), [
            '<Child>',
            '    eee hhh',
            '</Child>',
            '',
            '<Child>',
            '    sss hhh',
            '</Child>',
        ])
