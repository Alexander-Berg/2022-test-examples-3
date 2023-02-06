import pytest
from .api import Api
from .passport import userinfo_by_login, get_social_task_id


@pytest.fixture(scope="session")
def api_url():
    return "http://localhost:3048"


@pytest.fixture(scope="session")
def db_connstring():
    return "host=localhost port=5432 user=rpop dbname=postgres"


@pytest.fixture(scope="session")
def mailru_login():
    return "collext-src@mail.ru"


@pytest.fixture(scope="session")
def mailru_password():
    return "pkE2DkXM9V3SR5RsGbpg"


# How to generate in case of expire:
# In browser: https://social-test.yandex.ru/broker2/start?application=mailru-o2-mail&consumer=mail&return_brief_profile=1&retpath=https%3A%2F%2Fmail-test.yandex.ru&scope=userinfo+mail.imap
# Get task id from redirected url after auth
# curl -s "https://api.social-test.yandex.ru/api/task/$TASK_ID" | jq -rc ".token.refresh"
@pytest.fixture(scope="session")
def mailru_refresh_token():
    return "66b429711b267b5aa49da0e97f05146d1db273a037363830"


@pytest.fixture(scope="session")
def mailru_oauth_application():
    return "mailru-o2-mail"


@pytest.fixture(scope="session")
def mailru_social_task_id(mailru_login, mailru_refresh_token, mailru_oauth_application):
    return get_social_task_id(mailru_login, mailru_oauth_application, mailru_refresh_token)


@pytest.fixture(scope="module")
def dst_user(request):
    name_parts = request.node.name.split(".")[1:-1]
    name = "-".join(name_parts)
    login = "collext-test-" + name + "@yandex.ru"
    return userinfo_by_login(login)


@pytest.fixture(scope="module")
def api(api_url, dst_user):
    return Api(api_url, dst_user)
