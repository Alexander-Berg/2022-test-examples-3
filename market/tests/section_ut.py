from unittest import TestCase
from base_config.section import Section, EPatchResult


class TestSection(TestCase):
    def test_empty(self):
        section = Section('Test')
        self.assertEqual(section.get_lines(), [
            '<Test>',
            '</Test>',
        ])

    def test_render(self):
        section = Section('TestRender', [('aaa', 'bbb')])
        self.assertEqual(
            section.render(),
            '<TestRender>\n'
            '    aaa bbb\n'
            '</TestRender>'
        )

    def test_options(self):
        section = Section('TestOptions', [
            ('init_key_1', 'init_val_1'),
            ('init_key_2', 'init_val_2')
        ])
        section.set_option('single_key_1', 'single_val_1')
        section.set_option('single_key_2', 'single_val_2')
        section.set_options([
            ('mult_key_1', 'mult_val_1'),
            ('mult_key_2', 'mult_val_2')
        ])
        section.set_option('space_key', 'text with space')

        self.assertEqual(section.get_lines(), [
            '<TestOptions>',
            '    init_key_1 init_val_1',
            '    init_key_2 init_val_2',
            '    single_key_1 single_val_1',
            '    single_key_2 single_val_2',
            '    mult_key_1 mult_val_1',
            '    mult_key_2 mult_val_2',
            '    space_key text with space',
            '</TestOptions>',
        ])

    def test_attributes(self):
        section = Section('TestAttributes')
        section.set_attribute('single_key_1', 'single_val_1')
        section.set_attribute('single_key_2', 'single_val_2')
        section.set_attributes([
            ('mult_key_1', 'mult_val_1'),
            ('mult_key_2', 'mult_val_2')
        ])
        self.assertEqual(section.get_lines(), [
            '<TestAttributes '
            'single_key_1="single_val_1" '
            'single_key_2="single_val_2" '
            'mult_key_1="mult_val_1" '
            'mult_key_2="mult_val_2">',
            '</TestAttributes>',
        ])

    def test_children(self):
        section = Section('Parent', [('aaa', 'bbb'), ('ccc', 'ddd')])
        child_1 = Section('FirstChild', [('ccc', 'ddd')])
        child_2 = Section('SecondChild', [('ccc', 'ddd')])
        child_3 = Section('ThirdChild', [('eee', 'fff')])
        grand_child_1 = Section('FirstGrandChild', [('aaa', 'bbb')])
        grand_child_3 = Section('ThirdGrandChild', [('ccc', 'ddd')])
        section.add_children([child_1, child_2])
        section.add_child(child_3)
        child_1.add_child(grand_child_1)
        child_3.add_children([grand_child_3])
        self.assertEqual(section.get_lines(), [
            '<Parent>',
            '    aaa bbb',
            '    ccc ddd',
            '    <FirstChild>',
            '        ccc ddd',
            '        <FirstGrandChild>',
            '            aaa bbb',
            '        </FirstGrandChild>',
            '    </FirstChild>',
            '    <SecondChild>',
            '        ccc ddd',
            '    </SecondChild>',
            '    <ThirdChild>',
            '        eee fff',
            '        <ThirdGrandChild>',
            '            ccc ddd',
            '        </ThirdGrandChild>',
            '    </ThirdChild>',
            '</Parent>',
        ])

    def test_not_str(self):
        section = Section('TestNotStr', [
            ('int', 123),
            ('none1', 'value'),
            ('none2', None),
        ])
        section.set_option('float', 0.05)
        section.set_option('none1', None)
        section.set_attribute('attr', 100)
        section.set_attribute('none_attr', None)
        self.assertEqual(section.get_lines(), [
            '<TestNotStr attr="100">',
            '    int 123',
            '    float 0.05',
            '</TestNotStr>',
        ])

    def test_delimiter(self):
        section = Section('TestDelimiter', [('aaa', 'bbb'), ('ccc', 'ddd')],
                          delimiter=' = ')
        self.assertEqual(section.get_lines(), [
            '<TestDelimiter>',
            '    aaa = bbb',
            '    ccc = ddd',
            '</TestDelimiter>',
        ])
        section = Section('TestDelimiter', [('aaa', 'bbb'), ('ccc', 'ddd')],
                          delimiter=':-)')
        self.assertEqual(section.get_lines(), [
            '<TestDelimiter>',
            '    aaa:-)bbb',
            '    ccc:-)ddd',
            '</TestDelimiter>',
        ])

    def _get_section_for_patch(self):
        section = Section('Patch', [('aaa', 'bbb'), ('ccc', 'ddd')])
        child1 = Section('Child', [('common', 'fff'), ('eee', 'hhh')])
        child2 = Section('Child', [('common', 'ttt'), ('sss', 'hhh')])
        uniq_child = Section('UniqChild', [('common', 'qqq'), ('atr', 'vvv')])
        section.add_child(child1)
        section.add_child(child2)
        section.add_child(uniq_child)
        return section

    def _get_patched_section(self, patch):
        section = self._get_section_for_patch()
        for k, v in patch.items():
            path = k.split('.')
            res = section.patch(path, v[0])
            self.assertEqual(res, v[1], k)
        return section

    def test_patch_add_and_mofify(self):
        patch = {
            'Child.common': ('abc', EPatchResult.CHANGED),
            'Child.new': ('hhh', EPatchResult.ADDED),
            'aaa': ('bbb', EPatchResult.NOTMODIFIED),
            'ccc': ('123', EPatchResult.CHANGED),
            'new': ('val', EPatchResult.ADDED),
            'NewChild.attr': ('val', EPatchResult.ADDED)
        }

        section = self._get_patched_section(patch)

        self.assertEqual(section.get_lines(), [
            '<Patch>',
            '    aaa bbb',
            '    ccc 123',
            '    new val',
            '    <Child>',
            '        common abc',
            '        eee hhh',
            '        new hhh',
            '    </Child>',
            '    <Child>',
            '        common abc',
            '        sss hhh',
            '        new hhh',
            '    </Child>',
            '    <UniqChild>',
            '        common qqq',
            '        atr vvv',
            '    </UniqChild>',
            '    <NewChild>',
            '        attr val',
            '    </NewChild>',
            '</Patch>',
        ])

    def test_patch_remove(self):
        patch = {
            'Child.common': ('__remove__', EPatchResult.REMOVED),
            'Child.new': ('__remove__', EPatchResult.NOTMODIFIED),
            'aaa': ('__remove__', EPatchResult.REMOVED),
            'new': ('__remove__', EPatchResult.NOTMODIFIED),
            'NewChild.attr': ('__remove__', EPatchResult.NOTMODIFIED),
            'UniqChild': ('__remove__', EPatchResult.REMOVED)
        }

        section = self._get_patched_section(patch)

        self.assertEqual(section.get_lines(), [
            '<Patch>',
            '    ccc ddd',
            '    <Child>',
            '        eee hhh',
            '    </Child>',
            '    <Child>',
            '        sss hhh',
            '    </Child>',
            '</Patch>',
        ])
