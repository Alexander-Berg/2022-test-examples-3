from exam import before
from fan.emails.message import BaseMessage
from fan.message.message import SendrMessage
from fan.message.template import Template as T
from fan.models import LetterUtm
from fan.testutils import TestCase


class MessageReplacerTestCase(TestCase):
    def test_mail_from_replace(self):
        """
        Проверяем, что mail_from подменяется.
        """

        # Проверим, что mail_from работает без замены
        original_mail_from = (self.letter.from_name, self.letter.from_email)
        m1 = SendrMessage(letter=self.letter)
        self.assertEqual(m1.mail_from, original_mail_from)

        # Проверим, что применяется mail_to из replace_from_message

        m1.replace_from_message = BaseMessage()
        self.assertEqual(m1.mail_from, original_mail_from)

        replaced_mail_from = ("Replaced Replace", "replaced@replace.tld")
        m1.replace_from_message = BaseMessage(mail_from=replaced_mail_from)
        self.assertEqual(m1.mail_from, replaced_mail_from)


class ReplacerFixContextTestCase(TestCase):
    """
    Тесты дополнительных преобразований контекста рендеринга сообщений
    """

    @before
    def make_new_campaign(self):
        self.campaign = self.create_campaign()
        self.campaign.magic_list_id = "some_magic"
        self.letter = self.create_letter(campaign=self.campaign)

    def _check_email_replacement(self, mail_to, context, body_want):
        """
        Тестирование рендеринга email в теле письма
        """
        m1 = SendrMessage(letter=self.letter)
        m1.mail_to = mail_to
        m1.html = T("{{ email }}")

        m1.render(**context)

        # Функция вызывается при формировании финального сообщения при отправке
        # но из-за наличия в нем логики преобразования контекста придется запускать руками
        m1.before_build(m1)
        self.assertEqual(m1.html_body, body_want)

    def test_mail_email_substitude_without_context(self):
        """
        Проверяем, что в теле письма подменяется поле {{ email }} даже если его нет в контексте.
        """
        mail_to = ("TEST", "test@example.com")
        self._check_email_replacement(mail_to, {}, mail_to[1])

    def test_mail_email_substitude_with_context(self):
        """
        Проверяем, что в теле письма подменяется поле {{ email }} если оно явно определено в контексте.
        """
        mail_to = ("TEST", "test@example.com")
        email = "other@example.net"
        self._check_email_replacement(mail_to, {"email": email}, email)

    def _check_mail_utm(self, want):
        """
        Тестирование рендеринга UTM меток
        """
        from urllib.parse import parse_qs

        m1 = SendrMessage(letter=self.letter)
        m1.render(email="test@example.com")
        m1.html = T("{{ __UTM__ }}|{{ __UTM_ONLY__ }}")

        m1.before_build(m1)
        result = m1.html_body
        utm_addition, utm_only = result.split("|")
        utm_only = utm_only[1:] if utm_only else utm_only

        parsed = [
            {k: v[0] for k, v in parse_qs(utm_addition).items()},
            {k: v[0] for k, v in parse_qs(utm_only).items()},
        ]

        self.assertEqual([want] * 2, parsed)

    def test_no_utm(self):
        """
        Тестирование рендеринга без подстановки UTM
        """
        self.campaign.use_utm = False
        self.campaign.save()

        self._check_mail_utm({})

    def test_default_utm(self):
        """
        Тестирование рендеринга без подстановки UTM
        """
        self.campaign.use_utm = True
        self.campaign.save()

        self._check_mail_utm(
            {
                "utm_source": "yandex",
                "utm_medium": "email",
                "utm_campaign": self.campaign.magic_list_id,
            }
        )

    def test_setted_utm(self):
        """
        Тестирование рендеринга из письма
        """
        self.campaign.use_utm = True
        self.campaign.save()

        utm, _ = LetterUtm.objects.get_or_create(letter=self.letter)
        utm.utm_source = "test_source"
        utm.utm_medium = "test_medium"
        utm.utm_campaign = "test_campaign"
        utm.utm_content = "test_content"
        utm.save()

        self._check_mail_utm(
            {
                "utm_source": "test_source",
                "utm_medium": "test_medium",
                "utm_campaign": "test_campaign",
                "utm_content": "test_content",
            }
        )
