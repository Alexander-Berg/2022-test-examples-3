import pytest
from datetime import timedelta
from django.utils import timezone
from fan.accounts.organizations.limits import get_test_send_task_count_for_period
from fan.campaigns.create import create_campaign
from fan.models import TestSendTask, EditorialLog


pytestmark = pytest.mark.django_db


def test_no_campaigns(org_id):
    assert get_test_send_task_count_for_period(org_id, 1) == 0


def test_several_campaigns(org_id, campaign_with_letter, another_campaign):
    _schedule_at(campaign_with_letter, timezone.now())
    _schedule_at(another_campaign, timezone.now())
    assert get_test_send_task_count_for_period(org_id, 1) == 2


def test_no_tasks(org_id, campaign_with_letter):
    assert get_test_send_task_count_for_period(org_id, 1) == 0


def test_scheduled_now(org_id, campaign_with_letter):
    _schedule_at(campaign_with_letter, timezone.now())
    assert get_test_send_task_count_for_period(org_id, 1) == 1


def test_scheduled_outside_of_period(org_id, campaign_with_letter):
    _schedule_at(campaign_with_letter, timezone.now() - timedelta(hours=25))
    assert get_test_send_task_count_for_period(org_id, 1) == 0


def test_scheduled_inside_of_period(org_id, campaign_with_letter):
    _schedule_at(campaign_with_letter, timezone.now() - timedelta(hours=23))
    assert get_test_send_task_count_for_period(org_id, 1) == 1


@pytest.fixture
def another_campaign(project, account):
    return create_campaign(account=account, project=project)


def _schedule_at(campaign, dt):
    TestSendTask(campaign=campaign).save()
    log_msg = EditorialLog(
        object_type="campaign",
        object_id=campaign.id,
        component="test_send_task",
        action="schedule",
        description="Запланирована тестовая отправка",
    )
    log_msg.save()
    EditorialLog.objects.filter(id=log_msg.id).update(datetime=dt)
