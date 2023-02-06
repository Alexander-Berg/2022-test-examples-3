import json
import pytest

from test_utils import TestParser, cleanup_falsy
from base_parsers import SerpParser

ComponentTypes = SerpParser.MetricsMagicNumbers.ComponentTypes


class TestYandexVideoIslandsJSONParser(TestParser):
    def test_search_result_first_component(self):
        components = self.read_components('spiderman.json')
        assert components[0]['componentInfo']['type'] == ComponentTypes.SEARCH_RESULT

    def test_player_in_noautoplay(self):
        components = self.read_components('player_in_noautoplay.json')
        assert components[2]['thumbadd']['videoPlayerHtml'] == ('<iframe scrolling="no" frameborder="0" '
                                                                'src="http://www.kure.tv/VideoEmbed?ID=142949" '
                                                                'allowfullscreen="1"></iframe>')
        assert components[3]['thumbadd']['videoPlayerHtml'] == ('<iframe scrolling="no" frameborder="0" '
                                                                'src="http://www.kure.tv/VideoEmbed?ID=142909" '
                                                                'allowfullscreen="1"></iframe>')

    def test_video_player_id(self):
        parsed = self.parse_file('video_player_id.json')
        assert len(parsed['components']) == 10
        self._check_video_player_id(parsed, 0, 2, 'myvi')
        self._check_video_player_id(parsed, 2, 9, 'youtube')
        self._check_video_player_id(parsed, 9, 10, 'myvi')

    def test_video_player_id_2(self):
        parsed = self.parse_file('player_id.json')
        assert len(parsed['components']) == 30
        self._check_video_player_id(parsed, 0, 1, 'myvi')
        self._check_video_player_id(parsed, 1, 2, '__raw_opengraph_iframe__')
        self._check_video_player_id(parsed, 2, 3, 'myvi')
        self._check_video_player_id(parsed, 3, 4, '__raw_opengraph_iframe__')
        self._check_video_player_id(parsed, 4, 5, 'myvi')
        self._check_video_player_id(parsed, 5, 6, '__raw_opengraph_iframe__')
        self._check_video_player_id(parsed, 6, 7, 'myvi')
        self._check_video_player_id(parsed, 7, 8, 'rutube')
        self._check_video_player_id(parsed, 8, 10, '__raw_opengraph_iframe__')
        self._check_video_player_id(parsed, 10, 12, 'hdgo')
        self._check_video_player_id(parsed, 12, 13, 'uakino')
        self._check_video_player_id(parsed, 13, 14, 'kinoluvr')
        self._check_video_player_id(parsed, 14, 17, 'hdgo')
        self._check_video_player_id(parsed, 17, 18, 'youtube')
        self._check_video_player_id(parsed, 18, 19, 'kinoluvr')
        self._check_video_player_id(parsed, 19, 20, 'moonwalk')
        self._check_video_player_id(parsed, 20, 21, '__raw_opengraph_embed_url__')
        self._check_video_player_id(parsed, 21, 23, 'ok')
        self._check_video_player_id(parsed, 23, 24, 'myvi')
        self._check_video_player_id(parsed, 24, 25, 'hdgo')
        self._check_video_player_id(parsed, 25, 26, 'moonwalk')
        self._check_video_player_id(parsed, 26, 28, 'hdgo')
        self._check_video_player_id(parsed, 28, 29, 'moonwalk')
        self._check_video_player_id(parsed, 29, 30, 'mailru')

    def test_player_with_parameterized_width_and_height(self):
        components = self.read_components('player_with_parameterized_size.json')
        assert components[0]['thumbadd']['videoPlayerHtml'] == (
            '<iframe src="http://embed.publicvideohost.org/v.php?f=ff6600&amp;'
            'l=3366cc&amp;b=000000&amp;v=235774&amp;w=450&amp;h=334" '
            'width="450" height="334" frameborder="0" scrolling="no" '
            'allowfullscreen="1"></iframe>'
        )
        for component in components:
            html = component['thumbadd']['videoPlayerHtml']
            if html:
                assert '%{' not in html

    def test_video_navigator(self):
        parsed = self.parse_file('series_navigator.json')
        navigator = parsed['seriesNavigator.SERIES_NAVIGATOR']
        assert navigator
        assert navigator['id'] == 3909547314
        assert navigator['title'] == 'Симпсоны'
        assert len(navigator['seasons']) == 27
        assert navigator['hasSeasons']

    def test_video_navigator_without_seasons(self):
        parsed = self.parse_file('series_navigator_without_seasons.json')
        navigator = parsed['seriesNavigator.SERIES_NAVIGATOR']
        assert navigator
        assert navigator['id'] == 3991064927
        assert navigator['title'] == 'Ну Погоди'
        assert len(navigator['seasons']) == 1
        season = navigator['seasons'][0]
        assert season['name'] == '0'
        assert len(season['episodes']) == 20
        assert not navigator['hasSeasons']

    def test_polyboost(self):
        parsed = self.parse_file('new_searchdata.json')
        assert parsed['components'][0]['double.CLICK_ADD'] == 0.3495291312
        assert parsed['components'][1]['double.CLICK_ADD'] == 0.2992108134
        assert parsed['components'][2]['double.CLICK_ADD'] == 0.5416042611

    def test_debug_related(self):
        components = self.read_components('debug_related.json')
        expected = {'no_cnt': '1',
                    'parent-reqid': '1492494722795544-55059214305981488131963-ws36-870-V',
                    'related': '{\"rvb\":\"CiQIi-V6EAAYACALKAswCDgPQAZICFAJWA9gAmhYcAF40dOXwwUSZwoSMTI2NDg0MTc1NTQwODU1M' +
                               'DA1ChI3OTgzNTIxMTEwOTMwMDk3MDUKEzQ2MzQ1Njk0NTE0ODYyNzU2MDUKEzQ2OTcwODEwMzIwMzEyMTA0MDUKE' +
                               'zM3NTk4OTYxMTg3OTUyODk2MDUaGgoSMTI2NDg0MTc1NTQwODU1MDA1EP8BGP8B\",\"src\":\"serp\",' +
                               '\"url\":\"http://www.youtube.com/watch?v=j0w6yBRf8Is\",\"porno\":null,\"vfp\":1}',
                    'relatedVideo': 'yes',
                    'related_porno': 'null',
                    'related_src': 'serp',
                    'related_url': 'http://www.youtube.com/watch?v=j0w6yBRf8Is',
                    'related_vfp': '1',
                    'text': 'qqbhhlrfwbfxhgggqq'}
        assert len(components) == 30
        for component in components:
            assert component['text.videoRelatedProperties']
        assert json.loads(components[0]['text.videoRelatedProperties']) == expected

    def test_frames_urls(self):
        components = self.read_components('frames_urls.json')
        assert len(components) == 10
        with_thumbs = [component for component in components if component['textLists.FRAMES_THUMBS']]
        assert len(with_thumbs) == 4
        for component in with_thumbs:
            thumbs = component['textLists.FRAMES_THUMBS']
            assert len(thumbs) > 3
            assert all(x.startswith('https://avatars.mds.yandex.net/get-video_frame/') for x in thumbs)

    def test_kg_object(self):
        components = self.read_components('kg_object.json')
        assert len(components) == 11
        kg = components[0]
        assert kg['componentInfo']['type'] == 2
        assert kg['componentInfo']['wizardType'] == 104

    def test_kg_list(self):
        components = self.read_components('kg_list.json')
        assert len(components) == 11
        kg = components[0]
        assert kg['componentInfo']['type'] == 2
        assert kg['componentInfo']['wizardType'] == 105

    def test_video_commercial(self):
        components = self.read_components('vc.json')
        assert len(components) == 31
        for idx, component in enumerate(components):
            if idx in [1, 2, 3, 4, 5]:
                assert component['text.videoCommercial'] == 'inserted.rsya'
            elif idx in [8, 9, 10, 18, 23]:
                assert component['text.videoCommercial'] == 'organicrsya'

    def test_internals(self):
        components = self.read_components('internals.json')
        assert len(components) == 20
        component = components[0]
        assert component['componentInfo']['type'] == 1
        assert component['componentUrl']['pageUrl'] == 'http://clipiki.ru/video/304460'
        assert component['text.title'] == 'Штамм / The Strain 4 сезон 6 серия (2017)'
        assert component['text.videoDuration'] == '43:41'
        assert component['long.viewCount'] == 5470
        assert component['text.publishedDate'] == '2017-08-22T07:00:00+0000'
        assert component['thumbadd']['videoPlayerHtml'] == ('<iframe src="//clipiki.ru/embed/304460" '
                                                            'frameborder="0" scrolling="no" '
                                                            'allowfullscreen="1" aria-label="Video"/>')
        assert component['text.SERVER_DESCR'] == 'VIDEOP'

    def test_object_answer_regression(self):
        parsed = cleanup_falsy(self.parse_file('object_answer_input.json'))
        component = parsed['components'][0]
        assert component['componentInfo']['type'] == 2
        assert component['componentInfo']['wizardType'] == 120

    def test_single_object_is_knowledge_graph(self):
        components = self.read_components('knowledge_graph.json')
        component = components[0]
        assert component['componentInfo']['type'] == 2
        assert component['componentInfo']['wizardType'] == 89

    def test_parse_flags_from_root(self):
        parsed = self.parse_file('flags.json')
        assert parsed['tags.isKidsQuery'] == 1

    def test_has_previews(self):
        components = self.read_components('previews.json')
        assert len(components) == 1
        assert components[0]['json.videoPreviews']

    def test_video_navigator_one_season(self):
        parsed = self.parse_file('video_navigator.json')
        assert parsed['seriesNavigator.SERIES_NAVIGATOR']
        assert len(parsed['seriesNavigator.SERIES_NAVIGATOR']['seasons']) == 1

    def test_serp_data(self):
        components = self.read_components('serp_data.json')
        assert len(components) == 10
        for component in components:
            assert component['json.serpData']

    def test_extract_search_properties(self):
        parsed = cleanup_falsy(self.parse_file('search_props_input.json'))
        expected = cleanup_falsy(json.loads(self._read_file('search_props_output.json')))
        assert parsed['debug'] == expected

    def _check_video_player_id(self, serp, begin, end, player_id):
        for component in serp['components'][begin:end]:
            assert 'text.playerId' in component
            assert component['text.playerId'] == player_id

    def test_right_wizard(self):
        parsed = self.read_components('right_wizard.json')
        assert len(parsed) == 22
        assert parsed[5]['textLists.tags'] == []
        assert parsed[4]['json.serpData']['preview'] == 'https://video-preview.s3.yandex.net/-5JDtwAAAAA.mp4'

    def test_knowledge_graph_wizard(self):
        parsed = self.parse_file('knowledge_graph_wizard.json')
        components = parsed['components']
        assert parsed['long.docsFound'] == 9188
        assert len(components) == 23

    def test_quantity_keypoints(self):
        components = self.read_components('insights_keypoints.json')
        insights = components[0]['json.insights_keypoints']
        assert len(insights) == 8

    def test_prepare_uri(self):
        query = {
            "text": "text",
            "region": {
                "id": 114
            }
        }
        prepare = self.prepare(query=query)
        assert prepare['uri'] == 'https://yandex.ru/video/search?text=text&lr=114&no-tests=1' \
                                 + '&json_dump=searchdata&json_dump=wizplaces&json_dump=flags&yandexuid=' \
                                 + '&gta=VideoObjectsJson&gta=clickadd&gta=is_est&gta=is_tvod&json_dump=search_props' \
                                 + '&exp_flags=show_insight_in_snippets'

    @pytest.mark.parametrize("country, tld", [
        ("DE", "com"),
        ("RU", "ru"),
    ])
    def test_tld_mapping(self, country, tld):
        tlds = self.get_parser().get_tlds()
        assert tlds.get(country) == tld

    def test_prepare_uri_with_numdoc(self):
        query = {
            "text": "text",
            "region": {
                "id": 114
            }
        }
        additional_parameters = {
            "numdoc": 137
        }
        prepare = self.prepare(query=query,
                               additional_parameters=additional_parameters)
        assert "numdoc=137" in prepare["uri"]

    def component_ontodata(self):
        parsed = self.parse_file('new_searchdata.json')
        assert parsed['components'][1]['text.videoOntoId'] == 'ruw4463106'

    def search_props_test(self):
        parsed = self.parse_file('query_object_id.json')
        assert parsed['text.object_id'] == "ruw4463106"
        assert parsed['text.object_type'] == "Film"
        assert parsed['text.QueryFreshintent'] == "0.21147"

    def test_right_alignment(self):
        parsed = self.parse_file('right_alignment.json')
        assert parsed['components'][0]["componentInfo"]["alignment"] == 4

    def test_vod(self):
        parsed = self.parse_file('new_searchdata.json')
        assert parsed['components'][0]['tags.isSVOD']
        assert not parsed['components'][0]['tags.isTVOD']
        assert not parsed['components'][0]['tags.isEST']

    def test_serpdata(self):
        parsed = self.parse_file('new_searchdata.json')
        serpData = parsed['components'][0]['json.serpData']
        previews = serpData['previews']

        assert json.loads(previews)[0]['url'] == 'https://video-preview.s3.yandex.net/vh/4109376042821690886_vmaf-preview-536.mp4'
        assert serpData['preview'] == "https://video-preview.s3.yandex.net/vh/4109376042821690886_vmaf-preview-536.mp4"

    def test_oo_collections(self):
        parsed = self.parse_file('oo_collections.json')

        assert parsed["text.ParentCollection_show"] == "1"
        assert parsed["text.ParentCollection_id"] == "lst.rec"
