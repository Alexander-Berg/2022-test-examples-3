from django.conf import settings
from django.test import TestCase
from fan.message.get import get_message_by_letter_id
from fan.models import Account, Campaign
from fan.testutils.utils import rndstr


def create_test_object(main_cls, kw_func=None, **kw):
    """
    Хитрый хэлпер для создания цепочки классов
    """
    for key, func in kw_func or []:
        subobj = kw.get(key, {})
        if isinstance(subobj, dict):
            kw[key] = func(**subobj)
    obj = main_cls(**kw)
    obj.save()
    return obj


def create_test_account(**kw):
    kw.setdefault("name", rndstr())
    return create_test_object(Account, **kw)


def create_test_campaign(**kw):
    return create_test_object(Campaign, [("account", create_test_account)], **kw)


def create_letter(**kw):
    campaign = create_test_campaign(**kw.pop("campaign", {}))
    kw.setdefault("from_email", settings.RETURN_RECEIPT_DEFAULT)
    l = campaign.create_letter(**kw)
    return l


class LetterCreateMixin:
    def create_letter(self, **kw):
        return create_letter(**kw)


class TestMessageRenderer(TestCase, LetterCreateMixin):
    def test_MessageRenderer(self):
        # TODO: воспроизвести https://st.yandex-team.ru/SENDER-193
        spec = {
            "subject": "",  # Штука для снятия стрессов
            "from_email": "",  # 'devnull@yandex.ru',
            "from_name": "",  # 'alsun (Я.ру)',
            "html_body": "",
        }  # 'A={{ A }}. Unsubscribe: <a href="{{ unsubscribe_link }}">x</a>'}
        l = self.create_letter(**spec)
        m = get_message_by_letter_id(l.id)
