# -*- coding: utf-8 -*-

import unittest

from travel.rasp.admin.lib.unittests import UnicodeAssertionError
from travel.rasp.admin.scripts.utils.action_list import PythonScriptAction


unittest.TestCase.failureException = UnicodeAssertionError


class PythonScriptActionTest(unittest.TestCase):
    def testDescription(self):
        action = PythonScriptAction(['echo.py', 'off', '-n'], None)

        self.assertEqual(u"python script: echo.py off -n", action.get_description())
