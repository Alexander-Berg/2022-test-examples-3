from tempfile import NamedTemporaryFile
from datetime import timedelta
from uuid import uuid4
from django.conf import settings
from django.contrib.auth.models import User
from django.utils import timezone
from django.utils.text import slugify

import pytest
import six
from fan.lists.singleuse import store_csv_maillist_for_campaign
from fan.message.message import SendrMessage
from fan.campaigns.create import create_campaign
from fan.models import (
    Account,
    AccountUnsubscribeList,
    Campaign,
    OrganizationSettings,
    Project,
    UserRole,
)
from fan.models import TestSendTask as TSendTask
from fan.testutils.campaign import (
    add_test_letter_to_campaign,
    set_campaign_stats,
    set_delivery_stats,
)
from fan.testutils.maillist import store_maillist, store_n_maillists
from fan.testutils.maillist_fixtures import *  # NOQA
from fan.testutils.utils import rndstr
from fan.utils.cached_getters import cache_clear
from fan.utils.date import now_for_tests


@pytest.fixture
def user(user_id):
    username = "admin@localhost"

    user = User(
        email=username,
        username=username,
        is_staff=True,
        is_active=True,
        is_superuser=True,
    )
    user.save()

    user.user_id = user_id

    return user


@pytest.fixture
def user_id():
    return "test_uid"


@pytest.fixture
def user_ids():
    return [
        "test_uid1",
        "test_uid2",
        "test_uid3",
    ]


@pytest.fixture
def foreign_user_id():
    return "foreign_user_id"


@pytest.fixture
def user_id_to_recheck(user_id, account):
    checked_at = timezone.now() - settings.RECHECK_USER_ROLE_MIN_PERIOD - timedelta(minutes=1)
    UserRole.objects.filter(user_id=user_id).update(checked_at=checked_at)
    return user_id


@pytest.fixture
def user_id_too_soon_to_recheck(user_id, account):
    checked_at = timezone.now() - settings.RECHECK_USER_ROLE_MIN_PERIOD + timedelta(minutes=1)
    UserRole.objects.filter(user_id=user_id).update(checked_at=checked_at)
    return user_id


@pytest.fixture
def user_id_never_checked(user_id, account):
    UserRole.objects.filter(user_id=user_id).update(checked_at=None)
    return user_id


@pytest.fixture
def org_id():
    return "test_org_id"


@pytest.fixture
def small_send_emails_limit():
    return 1500


@pytest.fixture
def small_org(org_id, small_send_emails_limit):
    OrganizationSettings.objects.update_or_create(
        org_id=org_id, defaults={"send_emails_limit": small_send_emails_limit}
    )
    return org_id


@pytest.fixture
def large_send_emails_limit():
    return 50000


@pytest.fixture
def trusty():
    return True


@pytest.fixture
def org_with_custom_settings(org_id, large_send_emails_limit, trusty):
    OrganizationSettings.objects.update_or_create(
        org_id=org_id,
        defaults={
            "send_emails_limit": large_send_emails_limit,
            "trusty": trusty,
        },
    )
    return org_id


@pytest.fixture
def org_with_overridden_settings(small_org):
    org = {
        "SEND_EMAILS_LIMIT_FOR_ORG": 10,
        "TRUSTY_FOR_ORG": True,
        "DRAFT_CAMPAIGNS_LIMIT_FOR_ORG": 20,
        "MAILLISTS_LIMIT_FOR_ORG": 15,
    }
    store = {}
    for key, val in org.items():
        store[key] = getattr(settings, key).copy()
        setattr(settings, key, {small_org: val})
    org.update({"org_id": small_org}),
    yield org
    for key, val in store.items():
        setattr(settings, key, val)


@pytest.fixture
def account_name():
    return "baz"


@pytest.fixture
def account(account_name, user, org_id, settings):
    account = Account.objects.create(
        name=account_name,
        status_store_period=10,
        org_id=org_id,
        from_logins=settings.ACCOUNT_FROM_LOGINS_DEFAULT,
    )

    UserRole.objects.create(
        account=account,
        user_id=user.user_id,
        role=UserRole.ROLES.USER,
        checked_at=timezone.now(),
    ).save()

    return account


@pytest.fixture
def new_unknown_account(account):
    assert account.created_at > settings.IS_NEW_ACCOUNT_AFTER
    assert len(OrganizationSettings.objects.filter(org_id=account.org_id)) == 0
    return account


@pytest.fixture
def new_untrusty_account(account):
    OrganizationSettings.objects.update_or_create(org_id=account.org_id, defaults={"trusty": False})
    assert account.created_at > settings.IS_NEW_ACCOUNT_AFTER
    assert OrganizationSettings.objects.get(org_id=account.org_id).trusty == False
    return account


@pytest.fixture
def new_trusty_account(account):
    OrganizationSettings.objects.update_or_create(org_id=account.org_id, defaults={"trusty": True})
    assert account.created_at > settings.IS_NEW_ACCOUNT_AFTER
    assert OrganizationSettings.objects.get(org_id=account.org_id).trusty == True
    return account


@pytest.fixture
def old_unknown_account(account):
    account.created_at = settings.IS_NEW_ACCOUNT_AFTER - timedelta(days=1)
    account.save()
    assert account.created_at < settings.IS_NEW_ACCOUNT_AFTER
    assert len(OrganizationSettings.objects.filter(org_id=account.org_id)) == 0
    return account


@pytest.fixture
def old_untrusty_account(account):
    OrganizationSettings.objects.update_or_create(org_id=account.org_id, defaults={"trusty": False})
    account.created_at = settings.IS_NEW_ACCOUNT_AFTER - timedelta(days=1)
    account.save()
    assert account.created_at < settings.IS_NEW_ACCOUNT_AFTER
    assert OrganizationSettings.objects.get(org_id=account.org_id).trusty == False
    return account


@pytest.fixture
def old_trusty_account(account):
    OrganizationSettings.objects.update_or_create(org_id=account.org_id, defaults={"trusty": True})
    account.created_at = settings.IS_NEW_ACCOUNT_AFTER - timedelta(days=1)
    account.save()
    assert account.created_at < settings.IS_NEW_ACCOUNT_AFTER
    assert OrganizationSettings.objects.get(org_id=account.org_id).trusty == True
    return account


@pytest.fixture
def another_account(org_id):
    return Account.objects.create(name="another_account", org_id=org_id)


@pytest.fixture
def maillist(account):
    maillist = store_maillist(account, "Список рассылки 0", "maillist.csv")
    return maillist


@pytest.fixture
def another_maillist(account):
    maillist = store_maillist(account, "Другой список рассылки", "another_maillist.csv")
    return maillist


@pytest.fixture
def account_without_users(org_id):
    return Account.objects.create(name="acc_without_users", org_id=org_id)


@pytest.fixture
def account_with_users(org_id, user_ids):
    account = Account.objects.create(name="acc_with_users", org_id=org_id)
    for user_id in user_ids:
        UserRole.objects.create(user_id=user_id, account=account, role=UserRole.ROLES.USER)
    return account


@pytest.fixture
def account_with_non_user_roles(org_id, user_ids):
    account = Account.objects.create(name="acc_with_non_user_roles", org_id=org_id)
    for user_id in user_ids:
        UserRole.objects.create(user_id=user_id, account=account, role=UserRole.ROLES.MODERATOR)
    return account


@pytest.fixture
def account_with_almost_max_maillists_count(account, overriden_maillists_limit):
    store_n_maillists(account, overriden_maillists_limit - 1)
    return account


@pytest.fixture
def account_with_max_maillists_count(account, overriden_maillists_limit):
    store_n_maillists(account, overriden_maillists_limit)
    return account


@pytest.fixture
def project(account):
    title = uuid4().hex
    project = Project.objects.create(
        account=account,
        title=title,
        slug=slugify(title),
    )
    return project


@pytest.fixture
def domain():
    return "test.ru"


@pytest.fixture
def from_login():
    return "no-reply"


@pytest.fixture
def from_email():
    return "no-reply@test.ru"


@pytest.fixture
def email_with_wrong_from_login():
    return "wrong_from_login@test.ru"


@pytest.fixture
def unsubscribers():
    return ["unsubscriber1@domain.ru", "unsubscriber2@domain.ru", "unsubscriber3@domain.ru"]


@pytest.fixture
def campaign(project, account):
    campaign = create_campaign(account=account, project=project)
    return campaign


@pytest.fixture
def another_campaign(project, account):
    campaign = create_campaign(account=account, project=project)
    return campaign


@pytest.fixture
def campaign_from_another_account(another_account):
    campaign = create_campaign(account=another_account)
    return campaign


@pytest.fixture
def campaign_with_maillist(campaign, maillist):
    campaign.maillist = maillist
    campaign.save()
    return campaign


@pytest.fixture
def campaign_with_letter(campaign, from_email):
    add_test_letter_to_campaign(campaign, from_email)
    return campaign


@pytest.fixture
def campaign_with_wrong_from_login(campaign_with_singleusemaillist, email_with_wrong_from_login):
    add_test_letter_to_campaign(campaign_with_singleusemaillist, email_with_wrong_from_login)
    return campaign_with_singleusemaillist


@pytest.fixture
def campaign_with_singleusemaillist(campaign, maillist_csv_content):
    store_csv_maillist_for_campaign(campaign, maillist_csv_content, "singleusemaillist.csv")
    return campaign


@pytest.fixture
def another_campaign_with_singleusemaillist(another_campaign, maillist_content_with_unicode):
    store_csv_maillist_for_campaign(
        another_campaign, maillist_content_with_unicode, "singleusemaillist.csv"
    )
    return another_campaign


@pytest.fixture
def ready_campaign(campaign_with_letter, maillist_csv_content):
    store_csv_maillist_for_campaign(
        campaign_with_letter, maillist_csv_content, "singleusemaillist.csv"
    )
    return campaign_with_letter


@pytest.fixture
def ready_campaign_from_new_untrusty_account(new_untrusty_account, ready_campaign):
    return ready_campaign


@pytest.fixture
def ready_campaign_from_old_unknown_account(old_unknown_account, ready_campaign):
    return ready_campaign


@pytest.fixture
def sending_campaign(ready_campaign):
    ready_campaign.state = Campaign.STATUS_SENDING
    ready_campaign.save()
    return ready_campaign


@pytest.fixture
def sent_campaign(sending_campaign, campaign_stats):
    sending_campaign.state = Campaign.STATUS_SENT
    sending_campaign.save()
    set_campaign_stats(sending_campaign, campaign_stats)
    set_delivery_stats(sending_campaign, campaign_stats)
    return sending_campaign


@pytest.fixture
def failed_campaign(sending_campaign):
    sending_campaign.state = Campaign.STATUS_FAILED
    sending_campaign.save()
    return sending_campaign


@pytest.fixture
def campaign_with_unsubscribers(campaign, unsubscribers):
    unsubscribe_list = campaign.account.unsubscribe_lists.get(general=True)
    unsubscribe_list.bulk_upsert_elements(unsubscribers)
    return campaign


@pytest.fixture
def campaigns_to_send(account, from_email, maillist_csv_content):
    campaigns = []
    for i in range(5):
        campaign = create_campaign(
            account=account,
            state=Campaign.STATUS_DRAFT,
            created_at=now_for_tests() + i * timedelta(seconds=1),
        )
        add_test_letter_to_campaign(campaign, from_email)
        store_csv_maillist_for_campaign(campaign, maillist_csv_content, "singleusemaillist.csv")
        campaign.state = Campaign.STATUS_SENDING
        campaign.save()
        campaigns.append(campaign)
    return campaigns


@pytest.fixture
def campaign_stats():
    return {
        "uploaded": 10,
        "unsubscribed_before": 4,
        "duplicated": 3,
        "invalid": 2,
        "unsubscribed_after": 1,
        "views": 1,
    }


@pytest.fixture
def letter(campaign_with_letter):
    return campaign_with_letter.default_letter


@pytest.fixture
def unsub_list(account):
    name = {"ru": rndstr()}
    return AccountUnsubscribeList.objects.create(
        account=account,
        slug=slugify(six.text_type(name)),
        name=name,
        visible=True,
    )


@pytest.fixture
def unsub_list_general(account):
    return AccountUnsubscribeList.objects.get_general(account)


@pytest.fixture
def message(letter):
    return SendrMessage(letter)


@pytest.fixture
def clean_cache():
    cache_clear()


@pytest.fixture
def test_send_tasks(campaign_with_letter):
    tasks = []
    for i in range(5):
        task = TSendTask(
            created_at=now_for_tests() + i * timedelta(seconds=1),
            campaign=campaign_with_letter,
            recipients=["luckyone@yandex.ru"],
        )
        task.save()
        tasks.append(task)
    return tasks


@pytest.fixture
def sent_state():
    return Campaign.STATUS_SENT


@pytest.fixture
def sent_stats():
    return {
        "email_uploaded": 10,
        "email_unsubscribed": 4,
        "email_duplicated": 3,
        "email_invalid": 2,
        "undefined_error": 1,
    }


@pytest.fixture
def prohibit_setting_check_campaign_from_login():
    saved_value = settings.CHECK_CAMPAIGN_FROM_LOGIN
    settings.CHECK_CAMPAIGN_FROM_LOGIN = False
    yield
    settings.CHECK_CAMPAIGN_FROM_LOGIN = saved_value


@pytest.fixture
def permit_setting_check_campaign_from_login():
    saved_value = settings.CHECK_CAMPAIGN_FROM_LOGIN
    settings.CHECK_CAMPAIGN_FROM_LOGIN = True
    yield
    settings.CHECK_CAMPAIGN_FROM_LOGIN = saved_value


@pytest.fixture
def enable_silently_drop_sents_for_new_untrusty_accounts():
    stored = settings.SILENTLY_DROP_SENTS_FOR_NEW_UNTRUSTY_ACCOUNTS
    settings.SILENTLY_DROP_SENTS_FOR_NEW_UNTRUSTY_ACCOUNTS = True
    yield
    settings.SILENTLY_DROP_SENTS_FOR_NEW_UNTRUSTY_ACCOUNTS = stored


@pytest.fixture
def fan_feedback_loglines():
    return [
        "tskv	tskv_format=fan-feedback-typed-log	timestamp=2022-07-19 00:00:00.000001	timezone=+0300	campaign=123	email=a@b.ru	error_code=	error_text=	event=pixel	letter=0	test=False	user_agent=	user_ip=127.0.0.1\n",
        "tskv	tskv_format=fan-feedback-typed-log	timestamp=2022-07-19 00:00:00.000002	timezone=+0300	campaign=123	email=a@b.ru	error_code=	error_text=	event=pixel	letter=1	test=False	user_agent=	user_ip=127.0.0.1\n",
        "tskv	tskv_format=fan-feedback-typed-log	timestamp=2022-07-19 00:00:00.000003	timezone=+0300	campaign=123	email=a@b.ru	error_code=	error_text=	event=pixel	letter=2	test=False	user_agent=	user_ip=127.0.0.1\n",
        "tskv	tskv_format=fan-feedback-typed-log	timestamp=2022-07-19 00:00:00.000004	timezone=+0300	campaign=122	email=a@b.ru	error_code=	error_text=	event=pixel	letter=0	test=False	user_agent=	user_ip=127.0.0.1\n",
    ]


@pytest.fixture
def fan_feedback_logfile(fan_feedback_loglines):
    tmpfile = NamedTemporaryFile()
    with open(tmpfile.name, "w") as tf:
        tf.writelines(fan_feedback_loglines)
    yield tmpfile.name
