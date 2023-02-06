#!/usr/bin/python
# -*- coding: utf-8 -*-

import os
import re
import xml.dom.minidom
import subprocess
import ConfigParser

from glob import glob
from argparse import ArgumentParser
import yaml
from startrek_client import Startrek


ROBOT_LOGIN = 'robot-disk-mpfs'


class TestReport(object):
    def __init__(self, name=None, total=0, passed=0, failed=0, skipped=0, report_url=None):
        self.name = name
        self.report_url = report_url
        self.total = int(total)
        self.failed = int(failed)
        self.passed = int(passed)
        self.skipped = int(skipped)

    @classmethod
    def from_file(cls, file_name):
        with open(file_name) as f:
            doc = xml.dom.minidom.parse(f)
        test_suite = doc.getElementsByTagName("testsuite")[0]

        failed = int(test_suite.attributes.get('errors').value) + int(test_suite.attributes.get('failures').value)
        total = int(test_suite.attributes.get('tests').value)
        skipped = int(test_suite.attributes.get('skips').value)
        passed = total - skipped - failed

        return cls(total=total, passed=passed, failed=failed, skipped=skipped)

    @classmethod
    def combine(cls, *reports):
        total = passed = failed = skipped = 0
        for report in reports:
            total += report.total
            passed += report.passed
            skipped += report.skipped
            failed += report.failed
        return cls(total=total, passed=passed, failed=failed, skipped=skipped)


class MPFSVersion(object):
    def __init__(self, major, minor):
        self.major = major
        self.minor = minor

    @classmethod
    def from_str(cls, version):
        major, minor = version.split("-")
        return cls(major, minor)

    def __str__(self):
        return '%s-%s' % (self.major,
                          self.minor)

    def __eq__(self, other):
        return str(other) == str(self)


class ReleaseComment(object):
    ADDITIONS_TMPLS = {
        "sandbox": " ((%s %s))"
    }

    def __init__(self, version, additions, original_comment, *reports):
        self.original_comment = original_comment
        self.version = version
        if isinstance(additions, list):
            self.additions = ReleaseComment.additions_dict_to_str(additions)
        else:
            self.additions = additions
        if reports:
            self.reports = list(reports)
        else:
            self.reports = []

    @classmethod
    def additions_dict_to_str(cls, additions):
        result = ""
        for item_type, link, name in additions:
            result += cls.ADDITIONS_TMPLS[item_type] % (link, name)
        return result

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

    def update_on_startrek(self, new_report, additions):
        if new_report:
            for i, report in enumerate(self.reports):
                if report.name == new_report.name:
                    self.reports[i] = new_report
                    break
            else:
                self.reports.append(new_report)
        elif additions:
            self.additions += self.additions_dict_to_str(additions)
        self.original_comment.update(text=unicode(self))

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


def parse_args():
    parser = ArgumentParser()
    parser.add_argument("--name", help="Testsuit name")
    parser.add_argument("--version", help="Package version")
    parser.add_argument("--report-url", dest='report_url', help="Report URL")
    parser.add_argument("--api-sandbox-url", dest='api_sandbox_url', help="Sandbox URL")
    return parser.parse_args()


def get_mpfs_version(raw_version=None):
    if raw_version is not None:
        return MPFSVersion.from_str(raw_version)
    try:
        branch = subprocess.check_output("git rev-parse --abbrev-ref HEAD".split())
    except subprocess.CalledProcessError:
        return None

    config = ConfigParser.ConfigParser()
    config.read('tools/builder.conf')
    minor = config.get('global', 'release')

    if not branch.startswith("release-"):
        return None
    return MPFSVersion(branch[8:].strip(), minor)


def get_startrek_client():
    full_config_path = os.path.expanduser("~/.startrek")
    with open(full_config_path) as config_file:
        config = yaml.load(config_file)

    base_url = config['base_url']
    token = config['token']

    return Startrek(useragent="Disk Automation Robot",
                    base_url=base_url,
                    token=token)


def get_ticket(version):
    client = get_startrek_client()
    issues = client.issues.find("""
      Queue: DISKBACK AND
      Resolution: empty() AND
      (Summary: "Протестировать mpfs для Диска" OR
       Summary: "Протестировать mpfs для Платформы") AND
      Summary: "версия {0}-"
    """.format(version.major))
    if issues:
        return issues[0]


def find_comment_with_version(ticket, version):
    for comment in ticket.comments.get_all():
        if comment.createdBy.login != ROBOT_LOGIN:
            continue

        parsed_comment = ReleaseComment.parse(comment)
        if parsed_comment.version == version:
            return parsed_comment


def report_to_issue(version, additions, key, report):
    client = get_startrek_client()
    ticket = client.issues[key]
    comment_with_version = find_comment_with_version(ticket, version)
    if comment_with_version:
        comment_with_version.update_on_startrek(report, additions)
    else:
        ticket.comments.create(text=unicode(ReleaseComment(version, additions, None, report)))


def report_build(raw_version, package, link):
    # Определяем релизную версию
    version = get_mpfs_version(raw_version)
    if not version:
        return
    ticket = get_ticket(version)
    if not ticket:
        print "There is no release ticket for %s" % version
        return
    report_to_issue(version, [('sandbox', link, package)], ticket.key, None)


def main():
    args = parse_args()

    # Определяем релизную версию
    version = get_mpfs_version(args.version)
    if not version:
        print "Not a release branch"
        return

    # Ищем релизный тикет
    ticket = get_ticket(version)
    if not ticket:
        print "There is no release ticket for %s" % version
        return

    report = None
    additions = []
    if args.report_url:
        # Собираем общий отчет о пройденных тестах
        report = TestReport.combine(*[TestReport.from_file(report_path)
                                      for report_path in glob("test/*.xml")])
        report.name = args.name
        report.report_url = args.report_url
    elif args.api_sandbox_url:
        additions.append(('sandbox', args.api_sandbox_url, 'python-mpfs-api'))

    # Отписываем в тикет
    report_to_issue(version, additions, ticket.key, report)


if __name__ == "__main__":
    main()
