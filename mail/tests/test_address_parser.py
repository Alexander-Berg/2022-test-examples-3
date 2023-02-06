try:
    import address_parser
except:
    import mail.catdog.address_parser.cython.address_parser as address_parser

import unittest


class Base(unittest.TestCase):
    def check_recipient(self, recipients, size=1, at=0, **kwargs):
        self.assertEqual(len(recipients), size)
        rec = recipients[at]

        self.assertEqual(rec.display_name, kwargs['display_name'])
        self.assertEqual(rec.local, kwargs['local'])
        self.assertEqual(rec.domain, kwargs['domain'])
        self.assertEqual(rec.email, kwargs['email'])
        self.assertEqual(rec.valid, kwargs['valid'])


class CheckAddressParserDomainAndDisplayNameConditions(Base):
    def test_parse_single_address_without_display_name(self):
        self.check_recipient(address_parser.parse_recipients('a@yandex.ru'),
                             display_name='', local='a', domain='yandex.ru', email='a@yandex.ru', valid=True)

    def test_address_with_display_name(self):
        self.check_recipient(address_parser.parse_recipients('"Ололо, привет" a@yandex.ru'),
                             display_name='"Ололо, привет"', local='a', domain='yandex.ru',
                             email='"Ололо, привет" <a@yandex.ru>', valid=True)

    def test_parse_address_with_russian_domain(self):
        self.check_recipient(address_parser.parse_recipients('a@яндекс.рф'),
                             display_name='', local='a', domain='яндекс.рф', email='a@яндекс.рф', valid=True)

    def test_parse_address_with_punycode_domain(self):
        self.check_recipient(address_parser.parse_recipients('a@xn--d1acpjx3f.xn--p1ai'),
                             display_name='', local='a', domain='xn--d1acpjx3f.xn--p1ai',
                             email='a@xn--d1acpjx3f.xn--p1ai', valid=True)

    def test_local_and_domain_to_lower_case(self):
        self.check_recipient(address_parser.parse_recipients('A@Yandex.ru'),
                             display_name='', local='a', domain='yandex.ru', email='a@yandex.ru', valid=True)


class ValidAndInvalidCases(Base):
    def test_valid_email_with_display_name(self):
        self.check_recipient(address_parser.parse_recipients('"Hello, world" <a@yandex.ru>'),
                             display_name='"Hello, world"', local='a', domain='yandex.ru',
                             email='"Hello, world" <a@yandex.ru>', valid=True)

    def test_valid_email_with_display_name_without_angle_brackets(self):
        self.check_recipient(address_parser.parse_recipients('"Hello, world" a@yandex.ru'),
                             display_name='"Hello, world"', local='a', domain='yandex.ru',
                             email='"Hello, world" <a@yandex.ru>', valid=True)

    def test_invalid_email_with_empty_local(self):
        self.check_recipient(address_parser.parse_recipients('"Hello, world" @yandex.ru'),
                             display_name='', local='"hello, world"', domain='yandex.ru',
                             email='"hello, world"@yandex.ru', valid=False)

        self.check_recipient(address_parser.parse_recipients('"Hello, world" <@yandex.ru>'),
                             display_name='', local='', domain='', email='"Hello, world" <@yandex.ru>', valid=False)

    def test_invalid_email_with_empty_domain(self):
        s = '"Hello, world" a@'
        self.check_recipient(address_parser.parse_recipients(s),
                             display_name='', local='', domain='', email=s, valid=False)

    def test_invalid_yandex_email(self):
        self.check_recipient(address_parser.parse_recipients('_@ya.ru'),
                             display_name='', local='_', domain='ya.ru', email='_@ya.ru', valid=False)

        self.check_recipient(address_parser.parse_recipients('_@mail.ru'),
                             display_name='', local='_', domain='mail.ru', email='_@mail.ru', valid=True)

    def test_invalid_percent_hack_email(self):
        s = 'ololo a%b.ru@ya.ru'
        self.check_recipient(address_parser.parse_recipients(s),
                             display_name='ololo', local='a%b.ru', domain='ya.ru', email='ololo <a%b.ru@ya.ru>', valid=False)


class GroupParsingCases(Base):
    def test_valid_two_emails(self):
        a = address_parser.parse_recipients('"Hello, world" <a@yandex.ru>, "World, hello" <b@mail.ru>')
        self.check_recipient(a, size=2, at=0,
                             display_name='"Hello, world"', local='a', domain='yandex.ru',
                             email='"Hello, world" <a@yandex.ru>', valid=True)
        self.check_recipient(a, size=2, at=1,
                             display_name='"World, hello"', local='b', domain='mail.ru',
                             email='"World, hello" <b@mail.ru>', valid=True)

    def test_three_emails_the_second_is_invalid(self):
        a = address_parser.parse_recipients('"Hello, world" <a@yandex.ru>, "World, hello" <_@yandex.ru>, c@ya.ru')
        self.check_recipient(a, size=3, at=0,
                             display_name='"Hello, world"', local='a', domain='yandex.ru',
                             email='"Hello, world" <a@yandex.ru>', valid=True)
        self.check_recipient(a, size=3, at=1,
                             display_name='"World, hello"', local='_', domain='yandex.ru',
                             email='"World, hello" <_@yandex.ru>', valid=False)
        self.check_recipient(a, size=3, at=2,
                             display_name='', local='c', domain='ya.ru',
                             email='c@ya.ru', valid=True)

    def test_email_is_garbage(self):
        self.check_recipient(address_parser.parse_recipients('asdfasdfasd fasdfajsdf'),
                             display_name='', local='', domain='', email='asdfasdfasd fasdfajsdf', valid=False)

    def test_email_with_garbage(self):
        a = address_parser.parse_recipients('a@ya.ru, asdfasdfasd fasdfajsdf, c@ya.ru')
        self.check_recipient(a, size=2, at=0,
                             display_name='', local='a', domain='ya.ru', email='a@ya.ru', valid=True)

        self.check_recipient(a, size=2, at=1,
                             display_name='', local='', domain='', email='a@ya.ru, asdfasdfasd fasdfajsdf, c@ya.ru', valid=False)
