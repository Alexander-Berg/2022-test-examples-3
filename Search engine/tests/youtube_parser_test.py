# -*- coding: utf-8 -*-

import pytest
import datetime

from test_utils import TestParser, ComponentTypes, Alignments, WizardTypes


class TestYoutubeParser(TestParser):

    @pytest.fixture
    def fake_get_current_date(self):

        def fake_get_current_date():
            return datetime.datetime(2018, 10, 26, 2, 0, 0, 0)

        parser = self.get_parser()
        original_get_current_date = parser._get_current_date

        parser._get_current_date = fake_get_current_date
        yield
        parser._get_current_date = original_get_current_date

    def test_component_count(self):
        components = self.read_components('search_results.html')
        assert 19 == len(components)

    def test_component(self, fake_get_current_date):
        components = self.read_components('search_results.html')
        component = components[0]
        assert component['thumbadd']['urls'][0] == 'https://i.ytimg.com/vi/whTjYy464cY/hq720.jpg?sqp=-oaymwEcCOgCEMoBSFXyq4qpAw4IARUAAIhCGAFwAcABBg==&rs=AOn4CLCcsfN0Ua9Mj0u46vUDl3ErndcESg'
        assert component['componentInfo'] == {
            'type': ComponentTypes.SEARCH_RESULT,
            'alignment': Alignments.LEFT,
            'rank': 1,
        }
        assert component['componentUrl']['pageUrl'] == 'https://www.youtube.com/watch?v=whTjYy464cY'
        assert component['componentUrl']['viewUrl'] == 'www.youtube.com'
        assert component['text.title'] == '1+1 / Смотреть весь фильм'
        assert component['text.videoDuration'] == '1:52:03'
        assert component['text.playerId'] == 'youtube'
        assert component['text.SERVER_DESCR'] == 'VIDEO'
        assert component['webadd']['relevance'] == -1.0
        assert component['webadd']['isFoundByLink'] is False
        assert component['webadd']['isFastRobotSrc'] is False
        assert component['webadd']['hasVideoPlayer'] is True
        assert component['text.publishedDate'] == '2015-10-27T02:00:00+0000'
        assert component['long.viewCount'] == 20726271
        assert component['text.snippet'] == 'Пострадав в результате несчастного случая, богатый аристократ Филипп нанимает в помощники человека, который менее ...'
        assert component['json.serpData'] == {
            'iframePreviewPlayer': '<iframe src="https://www.youtube.com/embed/whTjYy464cY" width="260" height="146" frameborder="0" scrolling="no" allowfullscreen></iframe>',
        }
        assert component['long.serpRank'] == 1
        assert component['type'] == 'COMPONENT'

    def test_channel_component(self):
        components = self.read_components('search_results_with_channel.html')
        component = components[0]
        assert component['thumbadd']['urls'] == [
            '//yt3.ggpht.com/ytc/AKedOLRCpXDeP7nK8rFX-tHHRVkpRtQHk0fpwKx5SRCaLA=s88-c-k-c0x00ffffff-no-rj-mo',
            '//yt3.ggpht.com/ytc/AKedOLRCpXDeP7nK8rFX-tHHRVkpRtQHk0fpwKx5SRCaLA=s176-c-k-c0x00ffffff-no-rj-mo',
        ]
        assert component['componentInfo'] == {
            'type': ComponentTypes.WIZARD,
            'alignment': Alignments.LEFT,
            'rank': 1,
            'wizardType': WizardTypes.WIZARD_YOUTUBE_CHANNEL,
        }
        assert component['componentUrl']['pageUrl'] == 'https://www.youtube.com/user/Wylsacom'
        assert component['componentUrl']['viewUrl'] == 'www.youtube.com'
        assert component['text.title'] == 'Wylsacom'
        assert component['long.videoCount'] == 2745
        assert component['long.subscribersCount'] == 9910000
        assert component['text.snippet'] == 'Привет, тут у нас просто самый популярный канал в России о технологиях и жизни в ногу со временем. Вливайся! Сайт о ...'
        assert component['long.serpRank'] == 1
        assert component['type'] == 'COMPONENT'

    def test_rank(self):
        components = self.read_components('search_results.html')
        for index, component in enumerate(components, 1):
            assert component['componentInfo']['rank'] == index

    def test_get_published_date(self, fake_get_current_date):
        parser = self.get_parser()
        assert parser._get_published_date('2 года назад') == '2016-10-26T02:00:00+0000'
        assert parser._get_published_date('5 месяцаў таму') == '2018-05-29T02:00:00+0000'
        assert parser._get_published_date('1 week ago') == '2018-10-19T02:00:00+0000'
        assert parser._get_published_date('5 днів назад') == '2018-10-21T02:00:00+0000'
        assert parser._get_published_date('1 час назад') == '2018-10-26T01:00:00+0000'
        assert parser._get_published_date('25 минут назад') == '2018-10-26T01:35:00+0000'

    def test_get_subscribers_count(self):
        parser = self.get_parser()
        assert parser._get_subscribers_count('345K views') == 345000
        assert parser._get_subscribers_count('1,2M views') == 1200000
        assert parser._get_subscribers_count('1,2B views') == 1200000000
        assert parser._get_subscribers_count('1,2 K de visualizaciones') == 1200
        assert parser._get_subscribers_count('1,2 M de visualizaciones') == 1200000
        assert parser._get_subscribers_count('345.678 baxış') == 345678
        assert parser._get_subscribers_count('123 тыс. просмотров') == 123000
        assert parser._get_subscribers_count('1,2тыс. просмотров') == 1200
        assert parser._get_subscribers_count('1 млн. просмотров') == 1000000
        assert parser._get_subscribers_count('9745 просмотров') == 9745

    @pytest.mark.parametrize('input_filename,expected_output_filename', [
        ('input.json', 'expected_output.json')
    ])
    def test_preparer(self, input_filename, expected_output_filename):
        self.compare_preparer_output(input_filename, expected_output_filename, host="www.youtube.com")
