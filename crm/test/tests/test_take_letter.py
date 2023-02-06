import uuid
from datetime import date, timedelta

from crm.space.test.components.base import TestBaseIssue, TestBaseTimeline
from crm.space.test.schemas.issue import IssueSchema
from crm.space.test.schemas.timeline import TimelineSchema
from crm.space.test.schemas.common.mail import MailSchema
from crm.space.test.ora_client import Oracle
from crm.space.test.components.mailer import send_mail
from crm.space.test.helpers import wait_for_execution


class TestCreateIssueByMail(TestBaseIssue, TestBaseTimeline):
    path = 'issue/get'
    method = 'POST'

    @staticmethod
    def get_recently_mail_id(subject, from_dt=None):
        ora = Oracle()
        sql = """SELECT * FROM CRM.MAIL m JOIN CRM.MAIL_RAW mr ON m.id = mr.MAIL_ID WHERE m.MAIL_DT > :1 AND mr.H_SUBJECT = :2"""
        from_dt = from_dt if from_dt is not None else date.today() - timedelta(days=1)
        res = ora.query(sql, [from_dt, subject])
        if len(res) > 0:
            return res[0][0]
        else:
            return None

    @staticmethod
    def get_issue_by_mail_id(mail_id):
        ora = Oracle()
        sql = """SELECT * FROM CRM.I_TIMELINE WHERE EID = :1 and ETYPE = :2"""
        res = ora.query(sql, [mail_id, 1])
        if len(res) > 0:
            return res[0][1]
        else:
            return None

    def test_issue_by_mail_without_account(self, credential_zomb_crmtest):
        unique_value = uuid.uuid4()
        subject = {'Subject': 'Default mail by autotests ' + str(unique_value)}
        send_mail(**subject)
        mail_id = wait_for_execution(self.get_recently_mail_id, 30, 1, subject['Subject'])
        assert mail_id is not None

        issue_id = wait_for_execution(self.get_issue_by_mail_id, 30, 1, mail_id)
        assert issue_id is not None

        body = f'{{"data":{{"id":{issue_id}}}}}'
        res = self.get_response(**credential_zomb_crmtest, body=body)
        assert res.status_code == 200
        issue = IssueSchema().loads(res.text, partial=True)
        issue_id = self.get_issue_id(issue)
        issue_data = self.get_issue_data(issue, issue_id)
        assert ("account" in issue_data.keys()) is False
        assert issue_data['number'] == int(issue_id)
        assert issue_data['author']['id'] == 1
        assert issue_data['ticketLine'] == 1
        assert issue_data['typeId'] == 3
        assert issue_data['type'] == 'Тикет'
        assert issue_data['priority']['name'] == 'Нормальный'
        assert issue_data['communicationTypeId']['name'] == 'Mail'
        assert issue_data['name'] == subject['Subject']
        assert issue_data['state']['name'] == 'Открыт'
        assert issue_data['workflow']['name'] == 'Стандартный тикет'
        assert len(issue_data['skills']) == 2
        assert issue_data['skills'][0]['id'] == 1
        assert issue_data['skills'][1]['id'] == 2
        assert issue_data['isDone'] is False
        assert len(issue_data['tags']) == 0
        assert len(issue_data['timers']) == 0
        assert issue_data['queue']['id'] == 20000

        self.path = '/issue/timeline/get'
        body = f'{{"data":{{"issueId":{issue_id},"length":100000}}}}'
        res = self.get_response(**credential_zomb_crmtest, body=body)
        assert res.status_code == 200
        timeline = TimelineSchema().loads(res.text, partial=True)
        timeline_mail = self.get_timeline_mail_data(timeline, mail_id)

        assert timeline_mail['hasAttach'] is False
        assert timeline_mail['eType'] == 'Mail'
        assert timeline_mail['isExternal'] is False
        assert timeline_mail['bodyPreview'] == ' Test test test '
        assert timeline_mail['author']['id'] == 1
        assert timeline_mail['id'] == mail_id
        assert timeline_mail['type'] == 'Inbox'

        self.path = '/view/mail/preview'
        self.method = 'GET'
        params = {"mailId": mail_id}

        res = self.get_response(**credential_zomb_crmtest, params=params)
        assert res.status_code == 200
        mail_data = MailSchema().loads(res, partial=True)

        assert mail_data['id'] == mail_id
        assert mail_data['from'] == '\"default-test-address@yandex-team.ru\" <default-test-address@yandex-team.ru>'
        assert mail_data['to'] == '\"robot-tcrm-test-sp@yandex-team.ru\" <robot-tcrm-test-sp@yandex-team.ru>'
        assert mail_data['cc'] == ''
        assert mail_data['subject'] == subject['Subject']
        assert mail_data['body'] == '<p>Test test test<br /></p>'
        assert mail_data['type'] == 'Inbox'
        assert mail_data['isHtml'] is True
        assert mail_data['isSpam'] is False
        assert mail_data['files'] == []

    def test_issue_by_mail_with_account_x_otrs_login(self, credential_zomb_crmtest):
        unique_value = uuid.uuid4()
        headers = {'Subject': 'Mail with account by autotests ' + str(unique_value), 'x-otrs-login': 'yapju'}
        send_mail(**headers)
        mail_id = wait_for_execution(self.get_recently_mail_id, 30, 1, headers['Subject'])
        assert mail_id is not None

        issue_id = wait_for_execution(self.get_issue_by_mail_id, 30, 1, mail_id)
        assert issue_id is not None

        body = f'{{"data":{{"id":{issue_id}}}}}'
        res = self.get_response(**credential_zomb_crmtest, body=body)
        assert res.status_code == 200
        issue = IssueSchema().loads(res.text, partial=True)
        issue_id = self.get_issue_id(issue)
        issue_data = self.get_issue_data(issue, issue_id)
        assert issue_data['account']['id'] == 68623831
