# -*- coding: utf8 -*-
from datetime import datetime

from travel.avia.library.python.common.models.scripts import ScriptResult
from travel.avia.admin.lib.script_helpers.script_result_cleaner import ScriptResultCleaner
from travel.avia.library.python.tester.factories import create_script, create_script_result

from travel.avia.library.python.tester.testcase import TestCase


class TestScriptResultCleaner(TestCase):
    def setUp(self):
        self._cleaner = ScriptResultCleaner(max_count=2)

    def test(self):
        times = [datetime(2017, 9, i+1) for i in xrange(5)]
        will_not_cleaned_script = create_script(code='will_not_cleaned')
        for i in xrange(2):
            create_script_result(script=will_not_cleaned_script, started_at=times[i])

        will_cleaned_script = create_script(code='will_cleaned')
        for i in xrange(5):
            create_script_result(script=will_cleaned_script, started_at=times[i])

        self._cleaner.clean()

        script_results = list(ScriptResult.objects.filter(script=will_cleaned_script).order_by('started_at'))
        assert len(script_results) == 2
        assert script_results[0].started_at == times[3]
        assert script_results[1].started_at == times[4]

        assert ScriptResult.objects.filter(script=will_cleaned_script).count() == 2
        assert ScriptResult.objects.filter(script=will_not_cleaned_script).count() == 2
