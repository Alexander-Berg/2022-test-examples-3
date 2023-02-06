# -*- coding: utf-8 -*-
import os
import smtplib
import sys
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from pathlib import Path

import urllib3
from retrying import retry
from startrek_client import Startrek
from datetime import datetime

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

subject = "Время написать кейсы"
fromaddr = "testoviy-test111@yandex.ru"
mypass = "Testoviy1"
toaddr = ['mail-qa-duty@yandex-team.ru']
toaddrdisk = ['360-qa@yandex-team.ru']

sys.path.append(str(Path(__file__).parent.parent.parent))
from set_secret import set_secret

set_secret.set_secrets()

PROJECTS = {
    'CHEMODAN': ['#B2B', '#Tuning']
}


def get_tickets_needed_cases(queue):
    client = Startrek(useragent='curl/7.53.1', token=os.environ['STARTRECK_TOKEN'])
    if queue == 'CHEMODAN':
        for project in PROJECTS[queue]:
            issues_need_case = client.issues.find(
                f'Queue: {queue} AND  testcase: "Нужен" AND "Resolved": < today() AND '
                f'(Components: "{project}")'
            )
            if issues_need_case:
                tickets = sort_tickets(issues_need_case, True)
                send_message(queue + ' ' + project, tickets, toaddrdisk)
    else:
        issues_need_case = client.issues.find(
            f'Queue: {queue} AND  testcase: "Нужен" AND "Resolved": < today()'
        )
        if issues_need_case:
            tickets = sort_tickets(issues_need_case)
            send_message(queue, tickets, toaddr)


def sort_tickets(issues_need_case, queue=''):
    if len(issues_need_case) > 0:
        tickets_dict = {}
        for issue in issues_need_case:
            resolvedTime = datetime.strptime(issue.resolvedAt[:-5], "%Y-%m-%dT%H:%M:%S.%f").timestamp()
            if float(datetime.now().timestamp()) - float(resolvedTime) < 1209600:
                deadline = '<font color="green">Этим тикетам меньше 2 недель'
            elif float(datetime.now().timestamp()) - float(resolvedTime) < 2592000:
                deadline = '<font color="orange">Этим тикетам меньше месяца'
            elif float(datetime.now().timestamp()) - float(resolvedTime) > 2592000:
                deadline = '<font color="red">Этим тикетам уже больше месяца'
            if queue:
                try:
                    qa = issue.qa[0].login
                except (AttributeError, IndexError):
                    qa = "Ничейное"
            else:
                try:
                    qa = issue.qaEngineer.login
                except AttributeError:
                    qa = "Ничейное"
            if qa not in tickets_dict:
                tickets_dict[qa] = {}
            if deadline not in tickets_dict[qa]:
                tickets_dict[qa][deadline] = []
            tickets_dict[qa][deadline].append(issue)
        return tickets_dict


@retry(stop_max_attempt_number=3, wait_fixed=10000)
def send_message(queue, body, toaddr):
    text = convert_dict_to_text(body)
    msg = MIMEMultipart()
    msg['From'] = fromaddr
    msg['To'] = ', '.join(toaddr)
    msg['Subject'] = f'{queue} {subject}'

    msg.attach(MIMEText(text, 'html', 'utf-8'))

    server = smtplib.SMTP_SSL('smtp.yandex.ru', 465, timeout=10)
    server.set_debuglevel(1)
    server.login(fromaddr, mypass)
    text = msg.as_string()
    server.sendmail(fromaddr, toaddr, text)
    server.quit()


def convert_dict_to_text(dict):
    text = ''
    for qa in dict:
        text += '</ul><br><strong>' + qa + '</strong><br>'
        for deadline in dict[qa]:
            text += f'</ul><ul>{deadline}'
            for ticket in dict[qa][deadline]:
                text += f'<li><a href=https://st.yandex-team.ru/{ticket.key}>{ticket.summary}</a><br></li>'
    return text


if __name__ == '__main__':
    for queue in ['DARIA', 'QUINN', 'MAYA', 'CHEMODAN']:
        get_tickets_needed_cases(queue)
