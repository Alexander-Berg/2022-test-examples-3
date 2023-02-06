from django.test import Client
from django.conf import settings

import pytest

from fan.models import UnsubscribeListElement
from fan.testutils.pytest_fixtures import unsub_list

from .helpers import get_unsubscribe_link, init_fake_typed_log_handler

pytestmark = pytest.mark.django_db

other_unsub = unsub_list


class TestLinkClick:
    def test_unsubscribe_click(self, letter):
        """
        Простой тест отписки
        """
        c = Client()
        response = c.get(get_unsubscribe_link(letter=letter, email="a@b.ru"))
        assert response.status_code == 302
        assert response.url == settings.DEFAULT_UNSUBSCRIBE_LANDING_URL

    def test_unsubscribe_typed_log(self, letter):
        """
        При отписке пишет в typed лог
        """
        log = init_fake_typed_log_handler()
        response = Client().get(get_unsubscribe_link(letter=letter, email="a@b.ru"))
        assert len(log.messages) == 1

    def test_unsubscribe_typed_log_fields(self, letter):
        """
        При отписке пишет в typed лог поля с корректными значениями
        """
        log = init_fake_typed_log_handler()
        response = Client().get(get_unsubscribe_link(letter=letter, email="a@b.ru"))
        log_record = dict(log.messages[-1])
        assert all(k in log_record for k in ("event", "email", "letter", "campaign"))
        assert log_record["event"] == "unsubscribe"
        assert log_record["email"] == "a@b.ru"
        assert log_record["letter"] == letter.id
        assert log_record["campaign"] == letter.campaign.id

    def test_unsubscribe_incorrect_url(self, letter):
        """
        Отписка с неправильным секретом
        """
        c = Client()
        response = c.get("/unsubscribe/ZZZ")
        assert response.status_code == 302
        assert response.url == settings.DEFAULT_UNSUBSCRIBE_LANDING_URL

    def test_unsubscribe_click_lists(self, campaign, letter, unsub_list, other_unsub):
        """
        Проверяем добавление в списки отписки
        """
        campaign.unsubscribe_lists = [unsub_list, other_unsub]

        c = Client()
        response = c.get(get_unsubscribe_link(letter=letter, email="a@b.ru"))

        assert response.status_code == 302

        unsub_result = UnsubscribeListElement.objects.filter(
            email="a@b.ru", list__in=campaign.unsubscribe_lists.all()
        ).values_list("list_id", flat=True)

        assert set(unsub_result) == set([unsub_list.id, other_unsub.id])

    def test_global_unsubscribe_click_lists(
        self, campaign, letter, unsub_list, other_unsub, unsub_list_general
    ):
        """
        Проверяем добавление в глобальный список отписки
        """
        campaign.unsubscribe_lists = [unsub_list, other_unsub]

        c = Client()
        response = c.get(get_unsubscribe_link(letter=letter, email="a@b.ru") + "?type=global")

        assert response.status_code == 302

        unsub_result = UnsubscribeListElement.objects.filter(
            email="a@b.ru", list__in=list(campaign.unsubscribe_lists.all()) + [unsub_list_general]
        ).values_list("list_id", flat=True)
        assert set(unsub_result) == set([unsub_list.id, other_unsub.id, unsub_list_general.id])
