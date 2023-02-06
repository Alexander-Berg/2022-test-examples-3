# -*- coding: utf-8 -*-

import unittest

from travel.avia.admin.tests.lib.unittests import UnicodeAssertionError
from travel.avia.admin.scripts.utils.action_list import PythonScriptAction


unittest.TestCase.failureException = UnicodeAssertionError


class PythonScriptActionTest(unittest.TestCase):
    def testDescription(self):
        action = PythonScriptAction(['echo.py', 'off', '-n'], params={})

        self.assertEqual(u"python script: echo.py off -n", action.get_description())
