"""Cucumber json output formatter."""

import json
import os
import re


def _cut_traceback(tb):
    lines = tb.splitlines(True)
    for i, line in enumerate(lines):
        if line.startswith('E '):
            return ''.join(lines[max(i - 2, 0) : i + 1])
    return tb


def _get_example_kv(scenario):
    '''
    returns two lists: keys and values
    '''
    examples = scenario.get('examples', [])
    if examples:
        keys, values = examples[0]["rows"]
        row_index = examples[0]["row_index"]
        cur_values = values[row_index]
        return keys, cur_values
    return [], []


def _format_step_name(report, step):
    name = step['name']
    keys, values = _get_example_kv(report.scenario)
    for key, value in zip(keys, values):
        name = name.replace('<{}>'.format(key), value)
    return name


def _format_test_name(report, scenario):
    if scenario is None:
        return report.nodeid

    res = '{filename} :: {scenario_name}'.format(
        filename=os.path.basename(scenario['feature']['filename']),
        scenario_name=scenario['name'],
    )
    _, values = _get_example_kv(scenario)
    if values:
        # to make name shorter, take only the first param
        res += ' [{}]'.format(values[0])
    return res


def _create_structured_doc(report, scenario):
    doc = {}
    doc['nodeid'] = report.nodeid

    doc['steps'] = []
    if scenario is not None:
        for step in scenario['steps']:
            doc['steps'].append(
                {
                    'keyword': step['keyword'],
                    'name': _format_step_name(report, step).replace('\n', ' '),
                    'failed': step['failed'],
                }
            )

    if report.longrepr is not None:
        doc['traceback'] = _cut_traceback(report.longreprtext)

    doc['links'] = []
    for _, v in report.sections:
        for line in v.splitlines():
            m = re.search(r'(?P<reqtype>Request.*): (?P<url>http:.*)$', line)
            if m is None:
                continue
            doc['links'].append(
                {
                    'url': m.group('url'),
                    'name': m.group('reqtype'),
                }
            )

    return doc


class JsonReporter:
    """Logging plugin for json output."""

    def __init__(self, logfile):
        self._logfile = logfile

        self._details = {}
        self._failed_tests = []

        self._scenarios_passed = 0
        self._scenarios_failed = 0
        self._scenarios_skipped = 0
        self._steps_passed = 0
        self._steps_failed = 0
        self._features = set()

    def _process_scenario(self, report, scenario):
        if scenario is not None:
            self._features.add(scenario['feature']['filename'])

            for step in scenario['steps']:
                if step['failed']:
                    self._steps_failed += 1
                else:
                    self._steps_passed += 1
        else:
            self._features.add(report.location[0])

        if report.outcome == 'passed':
            self._scenarios_passed += 1
            return

        if report.outcome == 'skipped':
            self._scenarios_skipped += 1
            return

        self._scenarios_failed += 1

        self._failed_tests.append(report.nodeid)
        self._details[_format_test_name(report, scenario)] = _create_structured_doc(report, scenario)

    def pytest_deselected(self, items):
        self._scenarios_skipped += len(items)

    def pytest_runtest_logreport(self, report):
        if report.when != 'call':
            # skip if there isn't a result
            return

        scenario = getattr(report, 'scenario', None)
        self._process_scenario(report, scenario)

    def pytest_sessionfinish(self):
        with open(self._logfile, 'w', encoding='utf-8') as logfile:
            data = {
                'details': self._details,
                'stats': {
                    'features': ['{} features in total'.format(len(self._features))],
                    'scenarios': [
                        '{} scenarios passed, {} failed, {} skipped'.format(
                            self._scenarios_passed, self._scenarios_failed, self._scenarios_skipped
                        )
                    ],
                    'steps': ['{} steps passed, {} failed'.format(self._steps_passed, self._steps_failed)],
                },
                'failedTests': self._failed_tests,
            }
            json.dump(data, logfile, ensure_ascii=False)

    def pytest_terminal_summary(self, terminalreporter):
        terminalreporter.write_sep('-', 'generated json file: {}'.format(self._logfile))
