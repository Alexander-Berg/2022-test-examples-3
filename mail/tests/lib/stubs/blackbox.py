import logging

from urllib.parse import urlparse

from jinja2 import StrictUndefined
from jinja2.environment import Template

from aiohttp import web

from mail.nwsmtp.tests.lib.stubs.base_stub import BaseHTTPStub
from mail.nwsmtp.tests.lib.users import from_mail_yandex_team, Assessor, UserWithAppPasswordEnabled
from mail.nwsmtp.tests.lib.util import read_data
import re

log = logging.getLogger(__name__)


def get_template(name):
    data = read_data(name)
    return Template(data, undefined=StrictUndefined)


def render_response(name, *args, **kwargs):
    template = get_template(name)
    return template.render(*args, **kwargs)


def get_user(users, email):
    email = from_mail_yandex_team(email)
    return users.get(email)


def glue_zones(email):
    regexp = re.compile(r"@(?:yandex|ya)\.(?:ru|ua|com|by|kz|tr)")
    return regexp.sub("@yandex.ru", email)


def get_country(email):
    match = re.match(r".*@(?:yandex|ya)\.([a-z]{2,3})", email)
    return match.group(1) if match else "ru"


def handle_user_info(users, request, glue_different_zones_emails):
    email = request.query["login"]

    if glue_different_zones_emails:
        email = glue_zones(email)

    user = get_user(users, email)
    if user and user.is_registered_in_blackbox:
        return web.Response(
            text=render_response("user_info.json", user=user)
        )
    return web.Response(
        text=render_response("user_info_not_found.json")
    )


async def handle_login(users, request, allow_only_login, glue_different_zones_emails,
                       check_password_on_login, country_from_email):
    email = request.query["login"]
    if allow_only_login and "@" not in email:
        email += "@yandex.ru"
    country = get_country(email)

    if glue_different_zones_emails:
        email = glue_zones(email)

    user = get_user(users, email)
    if country_from_email:
        user.set_country(country)

    post = await request.post()
    if user and (not check_password_on_login or post["password"] == user.passwd):
        return web.Response(text=render_response(
            "login.json",
            user=user,
            is_assessor=isinstance(user, Assessor),
            is_user_with_app_password_enabled=isinstance(user, UserWithAppPasswordEnabled),
        ))
    return web.Response(
        text=render_response("login_not_found.json")
    )


async def handle_oauth(users, request):
    post = await request.post()
    user = users.get_by_token(post["oauth_token"])
    if user:
        return web.Response(text=render_response(
            "oauth.json",
            user=user,
            is_assessor=isinstance(user, Assessor),
        ))
    return web.Response(
        text=render_response("oauth_not_found.json")
    )


class Blackbox(BaseHTTPStub):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.requests = []
        self.glue_different_zones_emails = False
        self.allow_only_login = False
        self.check_password_on_login = False
        self.country_from_email = False

    def get_routes(self):
        return [
            web.get("/blackbox/", self.handle_get),
            web.post("/blackbox/", self.handle_post)
        ]

    def set_glue_different_zones_emails(self, value):
        self.glue_different_zones_emails = value

    def set_allow_only_login(self, value):
        self.allow_only_login = value

    def set_check_password_on_login(self, value):
        self.check_password_on_login = value

    def set_country_from_email(self, value):
        self.country_from_email = value

    def get_base_path(self):
        return self.conf.hosts[0]["__text"]

    def get_port(self):
        return urlparse(self.get_base_path()).port

    def get_alt_base_path(self):
        return self.conf.hosts[1]["__text"]

    def get_alt_port(self):
        return urlparse(self.get_alt_base_path()).port

    def handle_get(self, request):
        self.requests.append(request)
        if request.query["method"] == "userinfo":
            return handle_user_info(self.users, request, self.glue_different_zones_emails)
        return web.Response(status=404)

    async def handle_post(self, request):
        self.requests.append(request)
        if request.query["method"] == "login":
            return await handle_login(self.users, request, self.allow_only_login, self.glue_different_zones_emails,
                                      self.check_password_on_login, self.country_from_email)
        if request.query["method"] == "oauth":
            return await handle_oauth(self.users, request)
        return web.Response(status=404)
