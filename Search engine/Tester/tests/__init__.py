# coding: utf-8

from __future__ import absolute_import
from __future__ import unicode_literals
from __future__ import division

from search.resonance.tester.tests.script import ScriptTest
from search.resonance.tester.tests.release import ReleaseTest
from search.resonance.tester.tests.weight_check import WeightCheckTest


TESTS_TABLE = {
    'Script': ScriptTest,
    'WeightCheck': WeightCheckTest,
    'Release': ReleaseTest,
}
