# -*- coding: utf-8 -*-

import os
import sys
import unittest

from travel.avia.admin.lib.processes import get_ps_aux_lines, get_ps_ax_dicts


class ProcessesManipulationTest(unittest.TestCase):

    def testGetPsAuxLines(self):
        lines = get_ps_aux_lines()

        self.assertTrue(lines[0].startswith('USER'))
        self.assertTrue(lines[1].strip().split()[1].isdigit())

    def testGetPsAxDicts(self):
        dicts = get_ps_ax_dicts()
        rowdict = dicts[0]

        self.assertItemsEqual(['RUSER', 'PID', 'COMMAND'], rowdict.keys())
        self.assertIn(str(os.getpid()), [r['PID'] for r in dicts])

        myrowdict = [r for r in dicts if r['PID'] == str(os.getpid())][0]
        self.assertTrue(myrowdict['COMMAND'].count(sys.argv[0]))
        self.assertTrue(myrowdict['COMMAND'].count('python'))
