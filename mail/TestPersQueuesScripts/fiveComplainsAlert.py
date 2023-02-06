# -*- coding: utf-8 -*-

import datetime

import requests
import yt.wrapper as yt
from retrying import retry
from startrek_client import Startrek

import parameters

today = datetime.date.today().strftime('%d-%m-%y')
print(today)

table_alerts = "//home/mailfront/qa/st_tickets_alerts"
yt.config["proxy"]["url"] = "hahn"
yt.config["token"] = parameters.AUTH_YT

QUEUES_SUMMONEES = [
    ['DARIA', 'cosmopanda'],
    ['MOBDISK', 'yaroslavna'],
    ['PASSP', 'gladnik'],
    ['MOBILEMAIL', 'kateogar'],
    ['DISCW', 'xiona'],
    ['QUINN', 'oleshko'],
    ['MAYA', 'olga-ganchikova'],
    ['CHEMODAN', 'yaroslavna'],
    ['DOCVIEWER', 'yaroslavna']
]


# @retry(stop_max_attempt_number=3, wait_fixed=10000)
def five_complains_alert(queue, summonee):
    issues = []
    issuesKeys = []

    print()
    print(queue, summonee)

    client = Startrek(useragent="curl/7.53.1", token=parameters.myToken)

    issues = client.issues.find(
        'Queue: "' + queue + '" AND Type: Bug AND "OTRS Ticket": notEmpty() AND Priority: Normal,Minor,Trivial AND '
                             'Resolution: empty() AND Tags: !do_not_complain'
    )
    for issue in issues:
        find_five_or_more_complains_in_ticket(issue, issuesKeys)

    for ticket in issuesKeys:
        if make_exist_check_week_passed(ticket):
            issue = client.issues[ticket]
            issue.comments.create(
                text=u'В этом тикете набралось больше пяти жалоб за последние полгода, не пора ли перевести его в '
                     u'Critical? Я приду сюда через неделю, если вы больше не хотите получать напоминания в этом '
                     u'тикете, поставьте тег do_not_complain',
                summonees=summonee
            )
            update_alert_date(ticket)

    print(issuesKeys)


@retry(stop_max_attempt_number=3, wait_fixed=10000)
def find_five_or_more_complains_in_ticket(issue, issuesKeys):
    today = datetime.datetime.today()
    headers = {"Authorization": "OAuth " + parameters.myToken}
    count = 0
    r = requests.get("https://st-api.yandex-team.ru/v2/issues/" + issue.key + "/remotelinks", headers=headers)
    r = r.json()
    for link in r:
        created = datetime.datetime.strptime(link["createdAt"], '%Y-%m-%dT%H:%M:%S.%f+0000')
        created_delta = today - created
        need_delta = datetime.timedelta(183)
        if (link["object"]["application"]["name"] == "OTRS") & (created_delta < need_delta):
            count += 1
    if count > 4:
        print(issue.key)
        issuesKeys.append(issue.key)


def add_to_table(ticket_id):
    print("Adding to db: %s" % ticket_id)
    values_list = get_table_data()
    values_list.append({
        "ticket_id": ticket_id,
        "date_today": today})
    yt.write_table(table_alerts, values_list, format=yt.JsonFormat(attributes={"encode_utf8": False}))


def make_exist_check_week_passed(ticket_id):
    values_list = get_table_data()
    flag = 1
    for item in values_list:
        if item.get("ticket_id") == ticket_id:
            flag = 0
            print("Last alert date for ticket %s is %s " % (ticket_id, item.get("date_today")))
            last_alert_time = datetime.datetime.strptime(item.get("date_today"), '%d-%m-%y')
            today_date = datetime.datetime.strptime(today, '%d-%m-%y')
            if today_date - last_alert_time > datetime.timedelta(days=7):
                print (today_date - last_alert_time)
                return True
    if flag == 1:
        add_to_table(ticket_id)
    return False


def update_alert_date(ticket_id):
    values_list = get_table_data()
    for item in values_list:
        if item.get("ticket_id") == ticket_id:
            item["date_today"] = today
    yt.write_table(table_alerts, values_list, format=yt.JsonFormat(attributes={"encode_utf8": False}))


def get_table_data():
    values_list = []
    for i in yt.read_table(table_alerts, format=yt.JsonFormat(attributes={"encode_utf8": False})):
        values_list.append(i)
    return values_list


for q_s in QUEUES_SUMMONEES:
    five_complains_alert(q_s[0], q_s[1])
