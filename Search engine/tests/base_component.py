# coding: utf-8

import unittest

from components_app.component import Component


class TestComponent(unittest.TestCase):
    def test_init(self):
        c = Component()
        self.assertEqual(c.links, {})
        self.assertEqual(c.started, False)

    def test_add_child(self):
        c = Component()
        c2 = Component()
        c.child_1 = c2
        self.assertEqual(c.links['child_1'], c2)

        c.non_child = object()
        self.assertTrue(hasattr(c, 'non_child'))
        self.assertFalse(hasattr(c.links, 'non_child'))

    def test_start(self):
        c = Component()
        c.child = Component()
        self.assertFalse(c.started)
        self.assertFalse(c.child.started)
        c.load_config()
        c.start()

        self.assertTrue(c.started)
        self.assertTrue(c.child.started)

    def test_stop(self):
        c = Component()
        c.child = Component()
        c.load_config()
        c.start()
        c.stop()

        self.assertFalse(c.started)
        self.assertFalse(c.child.started)

    def test_getattribute(self):
        c = Component()
        c2 = Component()
        c.links['child'] = c2
        self.assertEqual(c.child, c2)

    def test_dict_interface(self):
        c = Component()
        c2 = Component()
        c['child'] = c2
        self.assertEqual(c.child, c2)
        self.assertEqual(c.links['child'], c2)
        self.assertEqual(c['child'], c2)
        del c['child']
        with self.assertRaises(KeyError):
            t = c['child']

    def test_type_change(self):
        c = Component()
        c.c1 = None
        c2 = Component()
        c.c1 = c2
        self.assertEqual(c.c1, c2)
        c.c1 = None
        self.assertEqual(c.c1, None)
        with self.assertRaises(KeyError):
            t = c.links['c1']

    def test_from_config(self):
        c = Component.from_config(config={})
        self.assertTrue(c.configured)
        self.assertTrue(c.started)
        c.stop()
