#!/usr/bin/python
# -*- coding: utf-8 -*-
import re
import os
import yaml

from startrek_client import Startrek

# import module snippets
from ansible.module_utils.basic import *


DOCUMENTATION = '''
---
module: startrek
short_description: Обеспечивает работу с трекером Startrek.
description:
     - Можно производить поиск по тикетам.
     - Позволяет добавлять комментарии.
version_added: "2.2"
options:
  find:
    description:
      - запрос на Stratrek Query Language.
    required: false
  issue:
    description:
      - ключ тикета
    required: false
  comment:
    description:
      - текст комментария
    required: false
requirements: []
author:
    - "Ivan Kuznetsov (@kis8ya)"
'''

EXAMPLES = '''
- name: Find issue
  startrek:
    find: >
      Queue: CHEMODAN and
      Resolution: empty() and
      Summary: "Протестировать mpfs для Платформы"
  register: issues

- name: Comment an issue
  startrek:
    issue: TASK-1234
    comment: |
      Тесты пройдены.
      Отчет: {{ report_url }}
'''


class TestReport(object):
    def __init__(self, name=None, total=0, passed=0, failed=0, skipped=0, report_url=None):
        self.name = name
        self.report_url = report_url
        self.total = int(total)
        self.failed = int(failed)
        self.passed = int(passed)
        self.skipped = int(skipped)


class ReleaseComment(object):
    def __init__(self, version, additions, original_comment, *reports):
        self.original_comment = original_comment
        self.version = version
        self.additions = additions
        self.reports = list(reports)

    @classmethod
    def parse(cls, comment):
        lines = comment.text.splitlines()

        version = None
        additions = ''
        parsed_header = re.search(".*: \*\*([^*]*)\*\*(.*)", lines[0])
        if parsed_header:
            version = parsed_header.groups()[0]
            additions = parsed_header.groups()[1]

        reports = []
        if len(lines) > 1 and lines[1] == '#|' and lines[-1] == '|#':
            lines = lines[2:-1]
            for line in lines:
                search_failed = re.search(
                    u'\|\| ([^|]*) \| \!\!\(red\)failed\!\!: ([^,]*), passed: ([^|]*) \| \(\(([^)]*) ',
                    line
                )
                if search_failed:
                    name, failed, passed, url = search_failed.groups()
                    reports.append(TestReport(name, failed=int(failed), passed=int(passed), report_url=url))
                    continue
                search_passed = re.search(
                    u'\|\| ([^|]*) \| \!\!\(green\)passed\!\!: ([^,]*) \| \(\(([^)]*) ',
                    line
                )
                if search_passed:
                    name, passed, url = search_passed.groups()
                    reports.append(TestReport(name, passed=int(passed), report_url=url))

        return cls(version, additions, comment, *reports)

    def update_on_startrek(self, new_report):
        for i, report in enumerate(self.reports):
            if report.name == new_report.name:
                self.reports[i] = new_report
                break
        else:
            self.reports.append(new_report)
        return self.original_comment.update(text=unicode(self))

    def reports_to_wiki_table(self):
        inner_text = u'#|\n'
        for report in self.reports:
            if not report:
                continue
            if report.failed or report.passed == 0:
                verdict = u'!!(red)failed!!: %s, passed: %s' % (report.failed,
                                                                report.passed)
            else:
                verdict = u'!!(green)passed!!: %s' % report.passed
            inner_text += u'|| %s | %s | ((%s Отчет)) ||\n' % (report.name,
                                                               verdict,
                                                               report.report_url)
        inner_text += u'|#'
        return inner_text

    def get_wiki_header(self):
        return u'Версия: **%s**%s' % (self.version, self.additions)

    def __unicode__(self):
        return u'%s\n%s' % (self.get_wiki_header(),
                            self.reports_to_wiki_table())


def issue_to_dict(issue):
    fields = ["description", "key", "summary"]
    dict_issue = {field: issue[field]
                  for field in fields}
    return dict_issue


def get_client():
    full_config_path = os.path.expanduser("~/.startrek")
    with open(full_config_path) as config_file:
        config = yaml.load(config_file, Loader=yaml.BaseLoader)

    base_url = config['base_url']
    token = config['token']

    return Startrek(useragent="Disk Automation Robot",
                    base_url=base_url,
                    token=token)


def find(query):
    client = get_client()
    issues = client.issues.find(query)
    return [issue_to_dict(issue)
            for issue in issues]


def comment_issue(key, text):
    client = get_client()
    result = client.issues[key].comments.create(text=text)
    return result.self


def find_comment_with_version(ticket, version):
    for comment in ticket.comments.get_all():
        if comment.createdBy.login != 'robot-disk-mpfs':
            continue

        parsed_comment = ReleaseComment.parse(comment)
        if parsed_comment.version == version:
            return parsed_comment


def update_comment(issue_key, update_tests_status):
    client = get_client()
    ticket = client.issues[issue_key]

    report = TestReport(name=update_tests_status['name'],
                        passed=update_tests_status['passed'],
                        failed=update_tests_status['failed'],
                        report_url=update_tests_status['report_url'])

    comment_with_version = find_comment_with_version(ticket, update_tests_status['version'])
    if comment_with_version:
        result = comment_with_version.update_on_startrek(report)
    else:
        result = ticket.comments.create(text=unicode(ReleaseComment(update_tests_status['version'], '', None, report)))
    return result.self


def main():
    module = AnsibleModule(
        argument_spec=dict(
            find=dict(type="str", default=None),
            issue=dict(type="str", default=None),
            comment=dict(type="str", default=None),
            update_tests_status=dict(type="dict", default=None)
        ),
        supports_check_mode=True
    )

    find_query = module.params.get("find")
    issue = module.params.get("issue")
    comment_text = module.params.get("comment")
    update_tests_status = module.params.get("update_tests_status")

    if find_query:
        data = find(find_query)
    elif issue and comment_text:
        data = comment_issue(issue, comment_text)
    elif issue and update_tests_status:
        data = update_comment(issue, update_tests_status)
    else:
        module.fail_json(msg="No valid options were provided.",
                         result=False)

    result_args = dict(
        result=True,
        data=data
    )
    module.exit_json(**result_args)


if __name__ == '__main__':
    main()
