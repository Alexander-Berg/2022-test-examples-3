from pycommon.asserts import *
from pycommon.xiva_api import *
from pycommon.fake_server import *
from json import loads
from msgpack import packb

# Until RTEC-5277 is resolved,
# this needs to be updated each time
# the list of services changes in test_tokens.json.
LATEST_XCONF_DUMP_REVISION = '67'

def setUp(self):
    global xiva_idm_api, xconf_server, tvm_info_server
    xiva_idm_api = XivaIdmApi(host="localhost", port=18083)
    xconf_server = fake_server(host="localhost", port=17083, raw_response=packb([0, []]))
    tvm_info_server = fake_server(
        host="localhost",
        port=17088,
        raw_response="""{
            "id": 2001075,
            "name": "mssngr-fanout-production",
            "create_time": 1527157934,
            "modification_time": 1527157934,
            "creator_uid": 1120000000079405,
            "abc_service_id": 1927,
            "vault_link": "https://yav.yandex-team.ru/secret/12345",
            "status": "ok"
        }""",
    )


def tearDown(self):
    global xconf_server, tvm_info_server
    xconf_server.fini()
    tvm_info_server.fini()


class TestIdmApi:
    def setup(self):
        global xconf_server
        self.xconf_server = xconf_server
        self.xconf_server.response_chain_clear()
        self.xconf_server.response_chain_append(Response(body=packb([0, []])))
        self.xconf_server.response_chain_append(Response(body="0"))
        self.xconf_req_checked = False

    def teardown(self):
        error_in_hook = self.xconf_server.set_request_hook(None)
        if error_in_hook:
            raise error_in_hook

    def check_xconf_req(self, req, expected_path, expected_body):
        if "/list?" in req.path:
            return
        eq_(req.path, expected_path)
        print(">>>", msgpack.unpackb(req.body))
        eq_(req.body, expected_body)
        self.xconf_req_checked = True

    def check_info_response_body(self, body):
        eq_(len(body), 2)
        eq_(body['code'], 0)
        self.check_info_roles(body['roles'])

    def check_info_roles(self, roles):
        eq_(len(roles), 3)
        eq_(roles['slug'], 'project')
        eq_(roles['name'], 'project')
        self.check_info_values(roles['values'])

    def check_info_values(self, values):
        assert(len(values) > 0)
        for service in values:
            self.check_info_value(service, values[service])

    def check_info_value(self, service, value):
        eq_(len(value), 3)
        eq_(value['name'], service)
        self.check_service_roles(service, value['roles'])
        self.check_service_aliases(service, value['aliases'])

    def check_service_roles(self, service, roles):
        eq_(len(roles), 3)
        eq_(roles['slug'], 'environment')
        eq_(roles['name'], 'environment')
        self.check_service_role_values(roles['values'])

    def check_service_role_values(self, role_values):
        eq_(len(role_values), 3)
        self.check_service_role_value('sandbox', role_values['sandbox'])
        self.check_service_role_value('corp', role_values['corp'])
        self.check_service_role_value('production', role_values['production'])

    def check_service_role_value(self, env, role_value):
        eq_(role_value, {
            'name': env,
            'roles': {
                'slug': 'role',
                'name': 'role',
                'values': {
                    'subscriber': 'subscriber',
                    'publisher': 'publisher'
                }
            }
        })

    def check_service_aliases(self, service, aliases):
        eq_(len(aliases), 1)
        eq_(aliases[0]['type'], 'default')
        assert(aliases[0]['name'].startswith(service + '%%'))

    def check_get_all_roles_response_body(self, body):
        eq_(len(body), 2)
        eq_(body['code'], 0)
        self.check_get_all_roles_users(body['users'])

    def check_get_all_roles_users(self, users):
        assert(len(users) > 0)
        for user_info in users:
            self.check_get_all_roles_user_info(user_info)

    def check_get_all_roles_user_info(self, user_info):
        eq_(len(user_info), 3)
        assert(isinstance(user_info['login'], unicode))
        eq_(user_info['subject_type'], 'tvm_app')
        for role in user_info['roles']:
            self.check_get_all_roles_user_role(role)

    def check_get_all_roles_user_role(self, user_role):
        eq_(len(user_role), 3)
        assert(isinstance(user_role['project'], unicode))
        assert(user_role['environment'] in ['sandbox', 'corp', 'production'])
        assert(user_role['role'] in ['publisher', 'subscriber'])

    def add_role_xconf_request_hook(self):
        return lambda (req): self.check_xconf_req(
            req,
            "/put?name=test-abc&type=service&owner=abc%3aabc-service&token=&environment=&revision=" + LATEST_XCONF_DUMP_REVISION,
            packb(
                [
                    "test-abc",
                    "abc:",
                    "abc-service",
                    "",
                    False,
                    [],
                    False,
                    0,
                    False,
                    False,
                    True,
                    {
                        "sandbox": [
                            (
                                12345,
                                "unknown",
                                False,
                            )
                        ]
                    },
                    {
                        "corp": [
                            (
                                777,
                                "mssngr-fanout-production",
                                False,
                            )
                        ]
                    },
                ]
            ),
        )


    def test_info(self):
        resp = xiva_idm_api.info()
        assert_ok(resp)
        self.check_info_response_body(loads(resp.body))

    def test_get_all_roles(self):
        resp = xiva_idm_api.get_all_roles()
        assert_ok(resp)
        self.check_get_all_roles_response_body(loads(resp.body))

    def test_add_role(self):
        self.xconf_server.set_request_hook(self.add_role_xconf_request_hook())
        resp = xiva_idm_api.add_role(
            subject_type="tvm_app",
            role='{"project": "test-abc", "environment": "corp", "role": "subscriber"}',
            login='777',
        )
        assert_ok(resp)
        assert self.xconf_req_checked

    def test_add_role_outdated_revision(self):
        self.xconf_server.set_request_hook(self.add_role_xconf_request_hook())
        self.xconf_server.response_chain[-1] = Response(code=400, body="stale revision")
        resp = xiva_idm_api.add_role(
            subject_type="tvm_app",
            role='{"project": "test-abc", "environment": "corp", "role": "subscriber"}',
            login='777',
        )
        # We expect IDM to retry this.
        assert_internal_error(resp)
        assert self.xconf_req_checked

    def test_remove_role(self):
        self.xconf_server.set_request_hook(
            lambda (req): self.check_xconf_req(
                req,
                "/put?name=test-abc&type=service&owner=abc%3aabc-service&token=&environment=&revision=" + LATEST_XCONF_DUMP_REVISION,
                packb(
                    [
                        "test-abc",
                        "abc:",
                        "abc-service",
                        "",
                        False,
                        [],
                        False,
                        0,
                        False,
                        False,
                        True,
                        {"sandbox": []},
                        {},
                    ]
                ),
            )
        )
        resp = xiva_idm_api.remove_role(
            subject_type="tvm_app",
            role='{"project": "test-abc", "environment": "sandbox", "role": "publisher"}',
            login='12345',
        )
        assert_ok(resp)
        assert self.xconf_req_checked
