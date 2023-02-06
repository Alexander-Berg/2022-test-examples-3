# -*- coding: utf-8 -*-
from collections import defaultdict

import urllib3
import os
from retrying import retry
from startrek_client import Startrek
from set_secret import set_secret

set_secret.set_secrets()

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

client = Startrek(useragent="curl/7.53.1", token=os.environ['STARTRECK_TOKEN'])

QUEUES = [
    'DARIA',
    'QUINN',
    'MOBDISK',
    'DISKSUP',
    'CHEMODAN',
    'DISCSW',
    'DOCVIEWER',
    'XIVA',
    'MAILPG',
    'MAILDEV',
    'MPROTO',
    'MOBILEMAIL',
    'PASSP',
    'MAYA',
    'MOBDEVAUTH',
    'XENO',
    'MAILDLV',
    'PSBTBSUP',
    'TELEMOSTSUP',
    'MOBTEL',
    'MAILSUP',
]

SUPPORT_QUEUES = [
    'DISKSUP',
    'TELEMOSTSUP',
    'SPMAIL',
    'MAILSUP',
    'PSBTBSUP',
]

EXTRA_COMPLAIN_QUEUES = [
    'CORPMAIL',
]


@retry(stop_max_attempt_number=3, wait_fixed=50000)
def get_all_updated():
    print(u'Ищем проапдейченные тикеты во всех очередях: %s' % QUEUES)
    all_updated_tickets = []
    for i in QUEUES:
        issues = client.issues.find(
            'Queue: ' + i + ' Updated: >=now() - 1days'
        )
        for issue in issues:
            all_updated_tickets.append(issue.key)
    return all_updated_tickets


@retry(stop_max_attempt_number=3, wait_fixed=50000)
def get_support_updated():
    print(u'Ищем проапдейченные тикеты в саппортных очередях: %s' % SUPPORT_QUEUES)
    issues = client.issues.find(
        'Queue: ' + ','.join(SUPPORT_QUEUES) + ' Updated: >=now() - 1days'
    )
    support_updated_tickets = []
    for issue in issues:
        linked_issues_keys = [link.object.key for link in issue.links]
        if linked_issues_keys:
            for linked_issue_key in linked_issues_keys:
                linked_issue_queue = linked_issue_key.split('-')[0]
                if linked_issue_queue in QUEUES:
                    support_updated_tickets.append(linked_issue_key)
    return support_updated_tickets


@retry(stop_max_attempt_number=3, wait_fixed=50000)
def get_tickets_need_to_update():
    all_updated = get_all_updated()
    support_updated = get_support_updated()
    need_to_update_complaints = set(support_updated) - set(all_updated)
    return list(need_to_update_complaints)


@retry(stop_max_attempt_number=3, wait_fixed=50000)
def count_complaints_with_filter(filter):
    print(u'Считаем жалобы по фильтру: %s' % filter)
    issues = client.issues.find(filter)
    complaints = defaultdict(list)
    issues_count = len(issues)
    for i, issue in enumerate(issues):
        try:
            otrs_tickets_ids = get_otrs_tickets(issue)
            otrs_tickets_ids_in_support_queues = []
            otrs_tickets_total = set(otrs_tickets_ids)

            if issue.queue.key not in SUPPORT_QUEUES:
                linked_issues_keys = [link.object.key for link in issue.links]
                for linked_issue_key in linked_issues_keys:
                    linked_issue_queue = linked_issue_key.split('-')[0]
                    if linked_issue_queue in SUPPORT_QUEUES:
                        otrs_tickets_ids_in_support_queues += get_otrs_tickets(client.issues[linked_issue_key])
                    if linked_issue_queue in EXTRA_COMPLAIN_QUEUES:
                        otrs_tickets_ids_in_support_queues.append(linked_issue_key)

            otrs_tickets_total.update(otrs_tickets_ids_in_support_queues)
            total_count = len(otrs_tickets_total)
            if total_count > 0:
                complaints[total_count].append(issue)
            print(
                '%s / %s\t-\tТикет: %s\tЖалоб в тикете: %s\tЖалоб в связанных тикетах: %s\tЖалоб всего: %s' %
                (
                    i + 1,
                    issues_count,
                    issue.key.encode('utf-8'),
                    len(otrs_tickets_ids),
                    len(otrs_tickets_ids_in_support_queues),
                    total_count
                )
            )
        except Exception as e:
            print(u'Проблемы с получением данных для тикета ' + issue.key.encode("utf-8"))
            print(e)
    update_issues(client, complaints)


@retry(stop_max_attempt_number=3, wait_fixed=10000)
def get_otrs_tickets(issue):
    return [remotelink.object.key for remotelink in issue.remotelinks if get_link_app_name(remotelink) == 'OTRS']


@retry(stop_max_attempt_number=3, wait_fixed=3000)
def get_link_app_name(link):
    try:
        return link.object.application.name
    except Exception as e:
        print(u"Не смогли получить инфомацию по одной из связей")
        return 'NO_INFO'


@retry(stop_max_attempt_number=5, wait_fixed=20000)
def update_issues(client, complaints):
    for issues_with_same_count in complaints:
        bulk_change = client.bulkchange.update(
            complaints[issues_with_same_count],
            duplicatesCount=issues_with_same_count
        )
        print(issues_with_same_count)
        bulk_change = bulk_change.wait()
        print(bulk_change.status)


if __name__ == '__main__':
    for queue in QUEUES:
        count_complaints_with_filter('Queue: ' + queue + ' Updated: >=now() - 1days')
    for key in get_tickets_need_to_update():
        count_complaints_with_filter('Key: ' + key)
