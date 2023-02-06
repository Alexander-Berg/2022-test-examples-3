from uuid import uuid4

from django.contrib.auth.models import User
from django.utils.text import slugify

import six
from exam import fixture
from mock import Mock

from fan.models import (
    Account,
    AccountUnsubscribeList,
    Project,
)


class Fixtures:
    @fixture
    def user(self):
        return self.create_user("admin@localhost", is_superuser=True)

    @fixture
    def account(self):
        return self.create_account(
            name="baz",
            owner=self.user,
        )

    @fixture
    def project(self):
        project = self.create_project(
            account=self.account,
            title="foo",
            slug="foo",
        )
        return project

    @fixture
    def campaign(self):
        return self.create_campaign(project=self.project)

    @fixture
    def letter(self):
        return self.create_letter(
            campaign=self.campaign,
        )

    @fixture
    def unsubscribe_list(self):
        return self.create_unsubscribe_list(
            account=self.account, name={"ru": "Test List"}, visible=True
        )

    def create_account(self, **kwargs):
        owner = kwargs.pop("owner", None)
        if not owner:
            owner = self.user

        kwargs.setdefault("name", uuid4().hex)

        account = Account.objects.create(**kwargs)
        return account

    def create_project(self, **kwargs):
        kwargs.setdefault("title", uuid4().hex)
        if not kwargs.get("slug"):
            kwargs["slug"] = slugify(six.text_type(kwargs["title"]))
        if not kwargs.get("account"):
            kwargs["account"] = self.account

        return Project.objects.create(**kwargs)

    def create_user(self, email=None, **kwargs):
        if not email:
            from django_yauth.user import YandexTestUserDescriptor

            request = Mock()
            yauser = YandexTestUserDescriptor()._get_yandex_user(request=request)
            # email = uuid4().hex + '@example.com'
            email = yauser.login + "@example.com"
            username = yauser.login
        else:
            username = email

        kwargs.setdefault("username", username)
        kwargs.setdefault("is_staff", True)
        kwargs.setdefault("is_active", True)
        kwargs.setdefault("is_superuser", False)

        user = User(email=email, **kwargs)
        user.save()

        return user

    def create_campaign(self, project=None, **kwargs):
        project = project or self.project
        account = project.account
        import fan.campaigns.create

        campaign = fan.campaigns.create.create_campaign(account=account, project=project, **kwargs)
        letter = campaign.get_letter(code="A")
        letter.html_body = "html code"
        letter.save()
        return campaign

    def create_letter(self, campaign=None, **kwargs):
        campaign = campaign or self.campaign
        kwargs.setdefault("html_body", "... html")
        kwargs.setdefault("subject", "... subject")
        kwargs.setdefault("from_name", "Me Robot")
        kwargs.setdefault("from_email", "from@me.tld")
        return campaign.create_letter(**kwargs)

    def create_unsubscribe_list(self, account=None, **kwargs):
        kwargs["slug"] = slugify(six.text_type(kwargs["name"]))

        return AccountUnsubscribeList.objects.create(account=account or self.account, **kwargs)
