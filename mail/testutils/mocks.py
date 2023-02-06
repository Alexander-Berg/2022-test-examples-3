import json
import pytest
import re
import requests
import responses
from django.conf import settings
from rest_framework import status
from urllib.parse import urlparse, parse_qs
from fan.utils.tvm import TVM_CFG


@pytest.fixture
def mock_tvm():
    TVM_CFG.force_tvm2_ticket("", "TEST_TVM_TICKET")
    yield
    TVM_CFG.force_tvm2_ticket("", None)


# Allows usage of several responses.RequestsMock
class SharedRequestsMock:
    requests_mock = None
    counter = 0

    def __init__(self):
        self.mocked = []

    def __enter__(self):
        SharedRequestsMock.counter += 1
        if SharedRequestsMock.requests_mock is None:
            SharedRequestsMock.requests_mock = responses.RequestsMock()
            SharedRequestsMock.requests_mock.start()
        return self

    def __exit__(self, *args, **kwargs):
        for method, url in self.mocked:
            SharedRequestsMock.requests_mock.remove(method, url)
        SharedRequestsMock.counter -= 1
        if SharedRequestsMock.counter == 0:
            SharedRequestsMock.requests_mock.stop()
            SharedRequestsMock.requests_mock = None

    def add_callback(self, method, url, callback):
        self.mocked += [(method, url)]
        SharedRequestsMock.requests_mock.add_callback(method, url, callback)


@pytest.fixture
def mock_directory_user(settings):
    url_pattern = re.compile(
        re.escape(settings.DIRECTORY_HOST) + r"/v11/users/(\w+)/\?fields=is_admin"
    )
    with SharedRequestsMock() as mock:
        mock.req_user_id = None
        mock.req_org_id = None
        mock.req_tvm_ticket = None
        mock.users = {}
        mock.resp_json = None
        mock.resp_code = None

        def callback(request):
            mock.req_user_id = url_pattern.search(request.url).group(1)
            mock.req_org_id = request.headers["X-ORG-ID"]
            mock.req_tvm_ticket = request.headers["X-Ya-Service-Ticket"]
            if mock.resp_json is not None and mock.resp_code is not None:
                return (mock.resp_code, {"content-type": "application/json"}, mock.resp_json)
            if mock.req_user_id not in mock.users:
                return (status.HTTP_404_NOT_FOUND, {}, "")
            return (
                status.HTTP_200_OK,
                {"content-type": "application/json"},
                json.dumps({"is_admin": mock.users[mock.req_user_id] == "admin"}),
            )

        mock.add_callback(responses.GET, url_pattern, callback=callback)
        yield mock


@pytest.fixture
def mock_directory_users(settings):
    url_pattern = re.compile(re.escape(settings.DIRECTORY_HOST) + r"/v11/users/\?")
    with SharedRequestsMock() as mock:
        mock.req_org_id = None
        mock.req_tvm_ticket = None
        mock.users = {}
        mock.resp_json = None
        mock.resp_code = None

        def callback(request):
            mock.req_org_id = request.headers["X-ORG-ID"]
            mock.req_tvm_ticket = request.headers["X-Ya-Service-Ticket"]
            if mock.resp_json is not None and mock.resp_code is not None:
                return (mock.resp_code, {"content-type": "application/json"}, mock.resp_json)
            resp = {
                "result": [
                    {"id": user_id, "is_admin": role == "admin"}
                    for user_id, role in mock.users.items()
                ]
            }
            return (status.HTTP_200_OK, {"content-type": "application/json"}, json.dumps(resp))

        mock.add_callback(responses.GET, url_pattern, callback=callback)
        yield mock


@pytest.fixture
def user_admin_in_directory(mock_directory_user, user_id):
    mock_directory_user.users[user_id] = "admin"


@pytest.fixture
def mock_directory_domains(settings, domain):
    url_pattern = re.compile(re.escape(settings.DIRECTORY_HOST) + r"/v11/domains/\?")
    with SharedRequestsMock() as mock:
        mock.req_org_id = None
        mock.req_tvm_ticket = None
        mock.resp_code = 200
        mock.resp_domain = domain
        mock.resp_owned = True
        mock.resp_master = True

        def callback(request):
            mock.req_org_id = request.headers["X-ORG-ID"]
            mock.req_tvm_ticket = request.headers["X-Ya-Service-Ticket"]
            return (
                mock.resp_code,
                {"content-type": "application/json"},
                json.dumps(
                    [
                        {
                            "name": mock.resp_domain,
                            "owned": mock.resp_owned,
                            "master": mock.resp_master,
                        }
                    ]
                ),
            )

        mock.add_callback(responses.GET, url_pattern, callback=callback)
        yield mock


@pytest.fixture
def mock_gendarme(settings):
    url_pattern = re.compile(re.escape(settings.GENDARME_HOST) + r"/domain/status\?name=([\w\.]+)")
    with SharedRequestsMock() as mock:
        mock.req_domain = None
        mock.req_tvm_ticket = None
        mock.resp_code = 200
        mock.resp_mx = True
        mock.resp_dkim = True
        mock.resp_spf = True

        def callback(request):
            mock.req_domain = url_pattern.search(request.url).group(1)
            mock.req_tvm_ticket = request.headers["X-Ya-Service-Ticket"]
            return (
                mock.resp_code,
                {"content-type": "application/json"},
                json.dumps(
                    {
                        "status": "ok",
                        "response": {
                            "mx": {"match": mock.resp_mx},
                            "dkim": [{"match": mock.resp_dkim}],
                            "spf": {"match": mock.resp_spf},
                        },
                    }
                ),
            )

        mock.add_callback(responses.GET, url_pattern, callback=callback)
        yield mock


@pytest.fixture
def mock_avatars_publish():
    url_pattern = re.compile(
        "http://{host}/put-{namespace}/".format(
            host=settings.AVATARS_HOSTS["write"],
            namespace=settings.AVATARS_NAMESPACE,
        )
    )
    with SharedRequestsMock() as mock:
        mock.publish_path = None
        mock.resp_code = status.HTTP_200_OK
        mock.resp_json = {}
        mock.raise_timeout_on_call_number = -1
        mock.resp_code_on_call_number = -1
        mock.published_images = []

        def callback(request):
            parsed_url = urlparse(request.url)
            image_name = parsed_url.path.rpartition("/")[-1]
            source_url = parse_qs(parsed_url.query).get("url", [None])[0]
            default_path = "/get-{namespace}/603/{imagename}/orig".format(
                namespace=settings.AVATARS_NAMESPACE, imagename=image_name
            )
            default_resp_json = {"sizes": {"orig": {"path": mock.publish_path or default_path}}}
            if mock.raise_timeout_on_call_number == len(mock.published_images) + 1:
                raise requests.exceptions.Timeout
            if mock.resp_code_on_call_number == len(mock.published_images) + 1:
                return (
                    mock.resp_code,
                    {"content-type": "application/json"},
                    json.dumps(mock.resp_json or default_resp_json),
                )
            published_image = (image_name,)
            if source_url is not None:
                published_image += (source_url,)
            mock.published_images.append(published_image)
            return (
                status.HTTP_200_OK,
                {"content-type": "application/json"},
                json.dumps(mock.resp_json or default_resp_json),
            )

        mock.add_callback(responses.GET, url_pattern, callback=callback)
        mock.add_callback(responses.POST, url_pattern, callback=callback)
        yield mock


@pytest.fixture
def mock_avatars_unpublish():
    url_pattern = re.compile(
        "http://{host}/delete-{namespace}/[0-9]+/".format(
            host=settings.AVATARS_HOSTS["write"],
            namespace=settings.AVATARS_NAMESPACE,
        )
    )
    with SharedRequestsMock() as mock:
        mock.raise_timeout = False
        mock.resp_code = status.HTTP_200_OK
        mock.unpublished_images = []

        def callback(request):
            if mock.raise_timeout:
                raise requests.exceptions.Timeout
            image_name = url_pattern.sub("", request.url).replace("/orig", "")
            mock.unpublished_images.append((image_name,))
            return (
                mock.resp_code,
                {"content-type": "application/json"},
                json.dumps({}),
            )

        mock.add_callback(responses.GET, url_pattern, callback=callback)
        yield mock
