import json

from crm.space.test.components.base import TestBaseIssue
from crm.space.test.schemas.issue import IssueSchema


class TestBaseSuiteIssueCreate(TestBaseIssue):
    path = '/issues/create'
    method = 'POST'

    def test_create_issue_check_attributes(self, credential_zomb_crmtest):
        res = self.get_response(**credential_zomb_crmtest,
                                data_path='request.issue.create_issue.create_issue_all_params')
        assert res.status_code == 200
        issue = IssueSchema().loads(res.text, partial=True)
        issue_id = self.get_issue_id(issue)
        issue_data = self.get_issue_data(issue, issue_id)
        assert issue["messages"][0]["level"] == "Success"
        assert issue["messages"][0]["text"] == "Тикетов создано [1]"
        assert issue_data["account"]["id"] == 68623831
        assert issue_data["state"]["id"] == 2
        assert issue_data["state"]["name"] == "Открыт"
        assert issue_data["author"]["login"] == "zomb-crmtest"
        assert issue_data["owner"]["login"] == "zomb-crmtest"
        assert issue_data["workflow"]["id"] == 14
        assert issue_data["category"]["id"] == 11552734
        assert issue_data["queue"]["id"] == 20001
        assert issue_data["name"] == "TestBaseSuiteIssueCreate"
        assert issue_data["typeId"] == 3
        assert issue_data["type"] == "Тикет"
        assert issue_data["id"] == int(issue_id)

    def test_create_issue_without_account(self, credential_zomb_crmtest):
        body = json.loads(TestBaseIssue.get_json_data('request.issue.create_issue.create_issue_all_params'))
        body["data"]["accountId"] = None
        body = json.dumps(body)
        res = self.get_response(**credential_zomb_crmtest, body=body)
        assert res.status_code == 200
        issue = IssueSchema().loads(res.text, partial=True)
        issue_id = self.get_issue_id(issue)
        issue_data = self.get_issue_data(issue, issue_id)
        assert ("account" in issue_data.keys()) is False

    def test_create_issue_zero_account(self, credential_zomb_crmtest):
        body = json.loads(TestBaseIssue.get_json_data('request.issue.create_issue.create_issue_all_params'))
        body["data"]["accountId"] = 1
        body = json.dumps(body)
        res = self.get_response(**credential_zomb_crmtest, body=body)
        assert res.status_code == 200
        issue = IssueSchema().loads(res.text, partial=True)
        issue_id = self.get_issue_id(issue)
        issue_data = self.get_issue_data(issue, issue_id)
        assert issue_data["account"]["id"] == 1
        assert issue_data["account"]["info"]["name"] == "НУЛЕВОЙ КЛИЕНТ"
        assert issue_data["account"]["info"]["type"] == "Client"
        assert issue_data["account"]["info"]["state"] == "None"

    def test_create_issue_without_category(self, credential_zomb_crmtest):
        body = json.loads(TestBaseIssue.get_json_data('request.issue.create_issue.create_issue_all_params'))
        body["data"]["categoryId"] = None
        body = json.dumps(body)
        res = self.get_response(**credential_zomb_crmtest, body=body)
        assert res.status_code == 200
        issue = IssueSchema().loads(res.text, partial=True)
        issue_id = self.get_issue_id(issue)
        issue_data = self.get_issue_data(issue, issue_id)
        assert ("category" in issue_data.keys()) is False

    def test_create_issue_without_state(self, credential_zomb_crmtest):
        body = json.loads(TestBaseIssue.get_json_data('request.issue.create_issue.create_issue_all_params'))
        body["data"]["stateId"] = None
        body = json.dumps(body)
        res = self.get_response(**credential_zomb_crmtest, body=body)
        assert res.status_code == 200
        issue = IssueSchema().loads(res.text, partial=True)
        issue_id = self.get_issue_id(issue)
        issue_data = self.get_issue_data(issue, issue_id)
        assert issue_data["state"]["id"] == 2
        assert issue_data["state"]["name"] == "Открыт"

    def test_create_issue_error_if_no_queue(self, credential_zomb_crmtest):
        body = json.loads(TestBaseIssue.get_json_data('request.issue.create_issue.create_issue_all_params'))
        body["data"]["queueId"] = None
        body = json.dumps(body)
        res = self.get_response(**credential_zomb_crmtest, body=body)
        assert res.status_code == 400
        assert res.text == '{"message":"queueId"}'

    def test_create_issue_error_if_no_workflow(self, credential_zomb_crmtest):
        body = json.loads(TestBaseIssue.get_json_data('request.issue.create_issue.create_issue_all_params'))
        body["data"]["workflowId"] = None
        body = json.dumps(body)
        res = self.get_response(**credential_zomb_crmtest, body=body)
        assert res.status_code == 202
        assert res.text == '{"message":"Тикет должен иметь Workflow"}'

    def test_create_issue_error_if_no_module(self, credential_zomb_crmtest):
        body = json.loads(TestBaseIssue.get_json_data('request.issue.create_issue.create_issue_all_params'))
        del body["context"]["module"]
        body = json.dumps(body)
        res = self.get_response(**credential_zomb_crmtest, body=body)
        assert res.status_code == 202
        assert res.text == '{"message":"Задачи не должны иметь очередь"}'


class TestBaseSuiteIssueGet(TestBaseIssue):
    path = 'issue/get'
    method = 'POST'

    def test_get_issue_max_params(self, credential_zomb_crmtest):
        issue_id = '12831669'
        body = f'{{"data":{{"id":{issue_id}}}}}'
        res = self.get_response(**credential_zomb_crmtest, body=body)
        assert res.status_code == 200
        IssueSchema().loads(res.text)
        assert res.text == self.get_json_data('request.issue.get_issue.get_issue_max_params')

    def test_get_issue_zero_account(self, credential_zomb_crmtest):
        pass

    def test_get_issue_max_params2(self, credential_space_odyssey):
        body = '{"data":{"id":12832009}}'
        res = self.get_response(**credential_space_odyssey, body=body)
        assert res.status_code == 200
        result = IssueSchema().loads(res.text, partial=True)
        print(result)
