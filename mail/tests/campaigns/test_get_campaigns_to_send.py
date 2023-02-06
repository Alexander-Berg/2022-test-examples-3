from fan.campaigns.list import get_campaigns_to_send
import pytest


pytestmark = pytest.mark.django_db


def test_negative_count():
    with pytest.raises(RuntimeError):
        get_campaigns_to_send(-1)


def test_count_is_zero():
    with pytest.raises(RuntimeError):
        get_campaigns_to_send(0)


def test_no_campaigns():
    res = get_campaigns_to_send(1)
    assert len(res) == 0


def test_single_campaign(campaigns_to_send):
    res = get_campaigns_to_send(1)
    assert len(res) == 1
    assert res[0].slug == campaigns_to_send[-1].slug


def test_multiple_campaigns(campaigns_to_send):
    res = get_campaigns_to_send(2)
    assert len(res) == 2
    assert res[0].slug == campaigns_to_send[-1].slug
    assert res[1].slug == campaigns_to_send[-2].slug


def test_count_greater_than_number_of_campaigns(campaigns_to_send):
    res = get_campaigns_to_send(2 * len(campaigns_to_send))
    assert len(res) == len(campaigns_to_send)
