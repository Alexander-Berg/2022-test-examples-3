from datetime import date
from fan.delivery.status import DeliveryStatus
from fan.models import DeliveryErrorStats, StatsCampaign
from fan.message.letter import load_letter
from fan.testutils.letter import load_test_letter
from fan.utils.cached_getters import cache_clear


def add_test_letter_to_campaign(campaign, from_email):
    letter_file = load_test_letter("letter.html")
    letter = campaign.default_letter
    letter.description = "index.html"
    letter.subject = "Subject: {{ title }}"
    letter.from_name = "Me Robot"
    letter.from_email = from_email
    load_letter(letter=letter, html_body_file=letter_file)
    cache_clear()


def set_campaign_stats(campaign, stats):
    stat, _ = StatsCampaign.objects.get_or_create(campaign=campaign)
    stat.reads = stats["views"]
    stat.unsubscribes = stats["unsubscribed_after"]
    stat.save()


def set_delivery_stats(campaign, stats):
    DeliveryErrorStats.objects.create(
        stat_date=date.today(),
        campaign=campaign,
        letter=campaign.default_letter,
        status=DeliveryStatus.EMAIL_UPLOADED,
        count=stats["uploaded"],
    ).save()
    DeliveryErrorStats.objects.create(
        stat_date=date.today(),
        campaign=campaign,
        letter=campaign.default_letter,
        status=DeliveryStatus.EMAIL_UNSUBSCRIBED,
        count=stats["unsubscribed_before"],
    ).save()
    DeliveryErrorStats.objects.create(
        stat_date=date.today(),
        campaign=campaign,
        letter=campaign.default_letter,
        status=DeliveryStatus.EMAIL_DUPLICATED,
        count=stats["duplicated"],
    ).save()
    DeliveryErrorStats.objects.create(
        stat_date=date.today(),
        campaign=campaign,
        letter=campaign.default_letter,
        status=DeliveryStatus.EMAIL_INVALID,
        count=stats["invalid"],
    ).save()
