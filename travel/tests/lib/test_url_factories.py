# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import date, datetime
from unittest import TestCase

from mock import Mock

from common.models.geo import Settlement
from travel.rasp.wizards.train_wizard_api.lib.url_factories import (
    FactorySelector, HostProvider, OrderFactory, ProxyHostProvider,
    ProxyOrderFactory, ProxySearchUrlFactory, RaspSearchFactory, TrainSearchFactory
)


class TestRaspSearchFactory(TestCase):
    def setUp(self):
        self._factory = RaspSearchFactory({
            'ru': "localhost.ru",
            'ua': "localhost.ua"
        })

        self._from_settlement = Settlement(id=1, title_ru="Москва")
        self._to_settlement = Settlement(id=2, title_ru="Питер")
        self._date = date(2017, 9, 1)

    def test_ru_national_version(self):
        actual = self._factory.format_url(
            from_point=self._from_settlement,
            to_point=self._to_settlement,
            when=self._date,
            tld='ru',
            empty_variants=False,
            transport_type='train'
        )

        assert actual == (
            'https://localhost.ru/search/train/?'
            'fromName=%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0&fromId=c1&'
            'toName=%D0%9F%D0%B8%D1%82%D0%B5%D1%80&toId=c2&when=2017-09-01'
        )

    def test_ua_national_version(self):
        actual = self._factory.format_url(
            from_point=self._from_settlement,
            to_point=self._to_settlement,
            when=self._date,
            tld='ua',
            empty_variants=False,
            transport_type='train'
        )

        assert actual == (
            'https://localhost.ua/search/train/?'
            'fromName=%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0&fromId=c1&'
            'toName=%D0%9F%D0%B8%D1%82%D0%B5%D1%80&toId=c2&when=2017-09-01'
        )

    def test_unknown_national_version(self):
        actual = self._factory.format_url(
            from_point=self._from_settlement,
            to_point=self._to_settlement,
            when=self._date,
            tld='unknown',
            empty_variants=False,
            transport_type='train'
        )

        assert actual == (
            'https://localhost.ru/search/train/?'
            'fromName=%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0&fromId=c1&'
            'toName=%D0%9F%D0%B8%D1%82%D0%B5%D1%80&toId=c2&when=2017-09-01'
        )

    def test_suburban(self):
        actual = self._factory.format_url(
            from_point=self._from_settlement,
            to_point=self._to_settlement,
            when=self._date,
            tld='ru',
            empty_variants=False,
            transport_type='suburban'
        )

        assert actual == (
            'https://localhost.ru/search/suburban/?'
            'fromName=%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0&fromId=c1&'
            'toName=%D0%9F%D0%B8%D1%82%D0%B5%D1%80&toId=c2&when=2017-09-01'
        )


class TestTrainSearchFactory(TestCase):
    def setUp(self):
        self._factory = TrainSearchFactory({
            'ru': "localhost.ru",
            'ua': "localhost.ua"
        })

        self._from_settlement = Settlement(id=1, slug="moskow")
        self._to_settlement = Settlement(id=2, slug="piter")
        self._date = date(2017, 9, 1)

    def test_ru_national_version(self):
        actual = self._factory.format_url(
            from_point=self._from_settlement,
            to_point=self._to_settlement,
            when=self._date,
            tld='ru',
            empty_variants=False,
            transport_type='train'
        )

        assert actual == (
            'https://localhost.ru/moskow--piter/?when=2017-09-01'
        )

    def test_ua_national_version(self):
        actual = self._factory.format_url(
            from_point=self._from_settlement,
            to_point=self._to_settlement,
            when=self._date,
            tld='ua',
            empty_variants=False,
            transport_type='train'
        )

        assert actual == (
            'https://localhost.ua/moskow--piter/?when=2017-09-01'
        )

    def test_unknown_national_version(self):
        actual = self._factory.format_url(
            from_point=self._from_settlement,
            to_point=self._to_settlement,
            when=self._date,
            tld='unknown',
            empty_variants=False,
            transport_type='train'
        )

        assert actual == (
            'https://localhost.ru/moskow--piter/?when=2017-09-01'
        )

    def test_bus(self):
        with self.assertRaises(AssertionError):
            self._factory.format_url(
                from_point=self._from_settlement,
                to_point=self._to_settlement,
                when=self._date,
                tld='ru',
                empty_variants=False,
                transport_type='bus'
            )

    def test_empty_variants(self):
        actual = self._factory.format_url(
            from_point=self._from_settlement,
            to_point=self._to_settlement,
            when=self._date,
            tld='unknown',
            empty_variants=True,
            transport_type='train'
        )

        assert actual == (
            'https://localhost.ru/'
        )



class FactorySelectorTest(TestCase):
    def setUp(self):
        self._fake_rasp_desktop_url_factory = Mock()
        self._fake_rasp_mobile_url_factory = Mock()
        self._fake_travel_url_factory = Mock()

        self._selector = FactorySelector(
            rasp_desktop_url_factory=self._fake_rasp_desktop_url_factory,
            rasp_mobile_url_factory=self._fake_rasp_mobile_url_factory,
            travel_url_factory=self._fake_travel_url_factory,
        )

    def test_travel(self):
        assert self._selector.select_factory(
            is_mobile=False,
            transport_type='train'
        ) == self._fake_travel_url_factory

        assert self._selector.select_factory(
            is_mobile=True,
            transport_type='train'
        ) == self._fake_travel_url_factory

    def test_not_train(self):
        assert self._selector.select_factory(
            is_mobile=False,
            transport_type='bus'
        ) == self._fake_rasp_desktop_url_factory

        assert self._selector.select_factory(
            is_mobile=False,
            transport_type='suburban'
        ) == self._fake_rasp_desktop_url_factory

    def test_not_train_and_mobile(self):
        assert self._selector.select_factory(
            is_mobile=True,
            transport_type='bus'
        ) == self._fake_rasp_mobile_url_factory

        assert self._selector.select_factory(
            is_mobile=True,
            transport_type='suburban'
        ) == self._fake_rasp_mobile_url_factory


class ProxySearchUrlFactoryTest(TestCase):
    def setUp(self):
        self._selector = ProxySearchUrlFactory(
            factory_selector=FactorySelector(
                rasp_desktop_url_factory=RaspSearchFactory({
                    'ru': 'rasp_desktop.ru'
                }),
                rasp_mobile_url_factory=RaspSearchFactory({
                    'ru': 'rasp_mobile.ru'
                }),
                travel_url_factory=TrainSearchFactory(
                    {'ru': 'travel.ru'},
                    'trains'
                ),
            )
        )

        self._from_settlement = Settlement(id=1, title_ru="Москва", slug='moskow')
        self._to_settlement = Settlement(id=2, title_ru="Питер", slug='piter')
        self._date = date(2017, 9, 1)

    def test_some_rasp_mobile(self):
        assert self._selector.format_url(
            is_mobile=True,
            from_point=self._from_settlement,
            to_point=self._to_settlement,
            when=self._date,
            tld='ru',
            empty_variants=False,
            transport_type='bus',
        ) == (
            'https://rasp_mobile.ru/search/bus/?'
            'fromName=%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0&fromId=c1&'
            'toName=%D0%9F%D0%B8%D1%82%D0%B5%D1%80&toId=c2&when=2017-09-01'
        )

    def test_some_rasp_desktop(self):
        assert self._selector.format_url(
            is_mobile=False,
            from_point=self._from_settlement,
            to_point=self._to_settlement,
            when=self._date,
            tld='ru',
            empty_variants=False,
            transport_type='bus',
        ) == (
            'https://rasp_desktop.ru/search/bus/?'
            'fromName=%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0&fromId=c1&'
            'toName=%D0%9F%D0%B8%D1%82%D0%B5%D1%80&toId=c2&when=2017-09-01'
        )

    def test_some_train_url(self):
        assert self._selector.format_url(
            is_mobile=False,
            from_point=self._from_settlement,
            to_point=self._to_settlement,
            when=self._date,
            tld='ru',
            empty_variants=False,
            transport_type='train',
        ) == (
            'https://travel.ru/trains/moskow--piter/?when=2017-09-01'
        )

    def test_train_empty_variants(self):
        assert self._selector.format_url(
            is_mobile=False,
            from_point=self._from_settlement,
            to_point=self._to_settlement,
            when=self._date,
            tld='ru',
            empty_variants=True,
            transport_type='train',
        ) == (
            'https://travel.ru/trains/'
        )


class HostProviderTest(TestCase):
    def setUp(self):
        self._provider = ProxyHostProvider(
            factory_selector=FactorySelector(
                rasp_desktop_url_factory=HostProvider({
                    'ru': 'rasp_desktop.ru'
                }),
                rasp_mobile_url_factory=HostProvider({
                    'ru': 'rasp_mobile.ru'
                }),
                travel_url_factory=HostProvider({
                    'ru': 'travel.ru'
                }),
            )
        )

        self._from_settlement = Settlement(id=1, title_ru="Москва", slug='moskow')
        self._to_settlement = Settlement(id=2, title_ru="Питер", slug='piter')
        self._date = date(2017, 9, 1)

    def test_some_rasp_mobile(self):
        assert self._provider.get_host(
            is_mobile=True,
            tld='ru',
            transport_type='bus',
        ) == 'rasp_mobile.ru'

    def test_some_rasp_desktop(self):
        assert self._provider.get_host(
            is_mobile=False,
            tld='ru',
            transport_type='bus',
        ) == 'rasp_desktop.ru'

    def test_some_travel_train_url(self):
        assert self._provider.get_host(
            is_mobile=False,
            tld='ru',
            transport_type='train',
        ) == (
            'travel.ru'
        )


class OrderFactoryTest(TestCase):
    def setUp(self):
        self._factory = OrderFactory({
            'ru': "localhost.ru",
            'ua': "localhost.ua"
        })

        self._from_settlement = Settlement(id=1, title_ru="Москва")
        self._to_settlement = Settlement(id=2, title_ru="Питер")
        self._date = date(2017, 9, 1)
        self._departure_date = datetime(2017, 9, 1, 10, 20)

    def test_ru(self):
        assert self._factory.build_context(
            from_point=self._from_settlement,
            to_point=self._to_settlement,
            when=self._date,
            tld='ru',
            transport_type='train'
        ).add_segment_info(
            number="А 56",
            departure_local_dt=self._departure_date,
            provider='',
        ).format_url() == (
            'https://localhost.ru/order/?'
            'fromName=%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0&fromId=c1&'
            'toName=%D0%9F%D0%B8%D1%82%D0%B5%D1%80&toId=c2&'
            'when=2017-09-01&transportType=train&number=%D0%90+56&time=10:20'
        )

        assert self._factory.build_context(
            from_point=self._from_settlement,
            to_point=self._to_settlement,
            when=self._date,
            tld='ru',
            transport_type='train'
        ).add_segment_info(
            number="А 56",
            departure_local_dt=self._departure_date,
            provider='P1',
        ).add_coach_info(
            'common'
        ).format_url() == (
            'https://localhost.ru/order/?'
            'fromName=%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0&fromId=c1&'
            'toName=%D0%9F%D0%B8%D1%82%D0%B5%D1%80&toId=c2&'
            'when=2017-09-01&transportType=train&number=%D0%90+56&time=10:20&'
            'provider=P1&'
            'coachType=common'
        )

    def test_ua(self):
        assert self._factory.build_context(
            from_point=self._from_settlement,
            to_point=self._to_settlement,
            when=self._date,
            tld='ua',
            transport_type='train'
        ).add_segment_info(
            number="А 56",
            departure_local_dt=self._departure_date,
            provider='P1',
        ).format_url() == (
            'https://localhost.ua/order/?'
            'fromName=%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0&fromId=c1&'
            'toName=%D0%9F%D0%B8%D1%82%D0%B5%D1%80&toId=c2&'
            'when=2017-09-01&transportType=train&number=%D0%90+56&time=10:20&'
            'provider=P1'
        )

        assert self._factory.build_context(
            from_point=self._from_settlement,
            to_point=self._to_settlement,
            when=self._date,
            tld='ua',
            transport_type='train'
        ).add_segment_info(
            number="А 56",
            departure_local_dt=self._departure_date,
            provider='',
        ).add_coach_info(
            'common'
        ).format_url() == (
            'https://localhost.ua/order/?'
            'fromName=%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0&fromId=c1&'
            'toName=%D0%9F%D0%B8%D1%82%D0%B5%D1%80&toId=c2&'
            'when=2017-09-01&transportType=train&number=%D0%90+56&time=10:20&coachType=common'
        )

    def test_unknown_national_version(self):
        assert self._factory.build_context(
            from_point=self._from_settlement,
            to_point=self._to_settlement,
            when=self._date,
            tld='unknown',
            transport_type='train'
        ).add_segment_info(
            number="А 56",
            departure_local_dt=self._departure_date,
            provider=None,
        ).format_url() == (
            'https://localhost.ru/order/?'
            'fromName=%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0&fromId=c1&'
            'toName=%D0%9F%D0%B8%D1%82%D0%B5%D1%80&toId=c2&'
            'when=2017-09-01&transportType=train&number=%D0%90+56&time=10:20'
        )

        assert self._factory.build_context(
            from_point=self._from_settlement,
            to_point=self._to_settlement,
            when=self._date,
            tld='unknown',
            transport_type='train'
        ).add_segment_info(
            number="А 56",
            departure_local_dt=self._departure_date,
            provider=None,
        ).add_coach_info(
            'common'
        ).format_url() == (
            'https://localhost.ru/order/?'
            'fromName=%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0&fromId=c1&'
            'toName=%D0%9F%D0%B8%D1%82%D0%B5%D1%80&toId=c2&'
            'when=2017-09-01&transportType=train&number=%D0%90+56&time=10:20&coachType=common'
        )


class ProxyOrderFactoryTest(TestCase):
    def setUp(self):
        self._factory = ProxyOrderFactory(
            factory_selector=FactorySelector(
                rasp_desktop_url_factory=OrderFactory({
                    'ru': 'rasp_desktop.ru'
                }),
                rasp_mobile_url_factory=OrderFactory({
                    'ru': 'rasp_mobile.ru'
                }),
                travel_url_factory=OrderFactory(
                    {'ru': 'travel.ru'},
                    'trains'
                ),
            ))

        self._from_settlement = Settlement(id=1, title_ru="Москва")
        self._to_settlement = Settlement(id=2, title_ru="Питер")
        self._date = date(2017, 9, 1)
        self._departure_date = datetime(2017, 9, 1, 10, 20)

    def test_some_mobile_rasp_url(self):
        context = self._factory.build_context(
            is_mobile=True,
            from_point=self._from_settlement,
            to_point=self._to_settlement,
            when=self._date,
            tld='ru',
            transport_type='bus',
        )

        segment_context = context.add_segment_info(
            number="А 56",
            departure_local_dt=self._departure_date,
            provider='',
        )

        assert segment_context.format_url() == (
            'https://rasp_mobile.ru/order/?'
            'fromName=%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0&fromId=c1&'
            'toName=%D0%9F%D0%B8%D1%82%D0%B5%D1%80&toId=c2&'
            'when=2017-09-01&transportType=bus&number=%D0%90+56&time=10:20'
        )
        assert segment_context.add_coach_info(
            'common'
        ).format_url() == (
            'https://rasp_mobile.ru/order/?'
            'fromName=%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0&fromId=c1&'
            'toName=%D0%9F%D0%B8%D1%82%D0%B5%D1%80&toId=c2&'
            'when=2017-09-01&transportType=bus&number=%D0%90+56&time=10:20&coachType=common'
        )

    def test_some_desktop_rasp_url(self):
        context = self._factory.build_context(
            is_mobile=False,
            from_point=self._from_settlement,
            to_point=self._to_settlement,
            when=self._date,
            tld='ru',
            transport_type='bus',
        )

        segment_context = context.add_segment_info(
            number="А 56",
            departure_local_dt=self._departure_date,
            provider='',
        )

        assert segment_context.format_url() == (
            'https://rasp_desktop.ru/order/?'
            'fromName=%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0&fromId=c1&'
            'toName=%D0%9F%D0%B8%D1%82%D0%B5%D1%80&toId=c2&'
            'when=2017-09-01&transportType=bus&number=%D0%90+56&time=10:20'
        )
        assert segment_context.add_coach_info(
            'common'
        ).format_url() == (
            'https://rasp_desktop.ru/order/?'
            'fromName=%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0&fromId=c1&'
            'toName=%D0%9F%D0%B8%D1%82%D0%B5%D1%80&toId=c2&'
            'when=2017-09-01&transportType=bus&number=%D0%90+56&time=10:20&coachType=common'
        )

    def test_some_travel_train_url(self):
        context = self._factory.build_context(
            is_mobile=False,
            from_point=self._from_settlement,
            to_point=self._to_settlement,
            when=self._date,
            tld='ru',
            transport_type='train',
        )

        segment_context = context.add_segment_info(
            number="А 56",
            departure_local_dt=self._departure_date,
            provider='',
        )

        assert segment_context.format_url() == (
            'https://travel.ru/trains/order/?'
            'fromName=%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0&fromId=c1&'
            'toName=%D0%9F%D0%B8%D1%82%D0%B5%D1%80&toId=c2&'
            'when=2017-09-01&transportType=train&number=%D0%90+56&time=10:20'
        )
        assert segment_context.add_coach_info(
            'common'
        ).format_url() == (
            'https://travel.ru/trains/order/?'
            'fromName=%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0&fromId=c1&'
            'toName=%D0%9F%D0%B8%D1%82%D0%B5%D1%80&toId=c2&'
            'when=2017-09-01&transportType=train&number=%D0%90+56&time=10:20&coachType=common'
        )
