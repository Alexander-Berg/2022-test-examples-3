# -*- coding: utf-8 -*-

from test_utils import TestParser


class TestYandexCollectionsFeedJSONParser(TestParser):

    def test_load_items_feed(self):
        components = self.parse_file("feed.json")["components"]
        assert 9 == len(components)

    def test_load_items_feed2(self):
        components = self.parse_file("feed2.json")["components"]
        assert 20 == len(components)

    def test_all_items_contains_card_ids(self):
        components = self.parse_file("feed2.json")["components"]
        assert all(["text.cardId" in x for x in components])
        assert all([len(x.get("text.cardId", "")) > 0 for x in components])

    def test_collections_items_has_type(self):
        components = self.parse_file("feed2.json")["components"]
        assert all(["text.cardType" in x for x in components])
        assert all([x["text.cardType"] == "collections" for x in components])

    def test_all_items_contains_image_url(self):
        components = self.parse_file("feed2.json")["components"]
        assert all(["url.imageUrl" in x for x in components])
        assert all(["https://" in x.get("url.imageUrl", "") for x in components])
        assert all(["get-pdb" in x.get("url.imageUrl", "") for x in components])

    def test_all_items_contains_image_sizes(self):
        components = self.parse_file("feed2.json")["components"]
        assert all(["dimension.IMAGE_DIMENSION" in x for x in components])
        assert all([x.get("dimension.IMAGE_DIMENSION", {}).get("w") > 0 for x in components])
        assert all([x.get("dimension.IMAGE_DIMENSION", {}).get("h") > 0 for x in components])

    def test_any_items_has_owner(self):
        components = self.parse_file("feed2.json")["components"]
        assert any(["json.owner" in x for x in components])

    def test_anonymous_items_has_no_owner(self):
        components = self.parse_file("feed_anonymous.json")["components"]
        assert not any(["json.owner" in x for x in components])

    def test_all_owner_uids_are_str(self):
        components = self.parse_file("feed2.json")["components"]
        assert all([isinstance(x.get("json.owner").get("text.uid"), str) for x in components if "json.owner" in x])

    def test_all_owner_has_avatars(self):
        components = self.parse_file("feed2.json")["components"]
        assert all(["avatars.mds.yandex.net" in x.get("json.owner").get("url.avatar") for x in components if "json.owner" in x])

    def test_some_collections_items_has_page_urls(self):
        components = self.parse_file("feed3.json")["components"]
        assert not all(["componentUrl" in x for x in components])
        assert any(["componentUrl" in x for x in components])

    def test_some_collections_items_hasnt_image_url(self):
        components = self.parse_file("feed3.json")["components"]
        assert "candidates" in components[9]["imageadd"]
        assert "candidates" not in components[10]["imageadd"]

    def test_some_collections_items_hasnt_title(self):
        components = self.parse_file("web_feed_static_dynamic_hp_from_toloka_components.json")["components"]
        collections_component = components[5]
        assert collections_component["text.cardType"] == "collections"
        assert "text.title" not in collections_component

    # Video feed:

    def test_parse_video_feed(self):
        components = self.parse_file("video_feed.json")["components"]
        assert 10 == len(components)

    def test_video_items_has_type(self):
        components = self.parse_file("video_feed.json")["components"]
        assert all(["text.cardType" in x for x in components])
        assert all([x["text.cardType"] == "video" for x in components])

    def test_all_video_items_has_thumbs(self):
        components = self.parse_file("video_feed.json")["components"]
        assert all(["url.videoThumbHref" in x for x in components])
        assert all(["https://" in x.get("url.videoThumbHref", "") for x in components])
        assert all(["avatars.mds.yandex.net" in x.get("url.videoThumbHref", "") for x in components])
        assert all(["get-vthumb" in x.get("url.videoThumbHref", "") for x in components])

    def test_video_thumbs_at_thumbadd(self):
        components = self.parse_file("video_feed.json")["components"]
        assert all(["url.videoThumbHref" in x for x in components])
        assert all(["thumbadd" in x for x in components])
        assert all([x.get("url.videoThumbHref") == x["thumbadd"]["urls"][0]
                    for x in components])
        assert all([x.get("url.videoThumbHref") == x.get("url.imageUrl")
                    for x in components])

    def test_video_items_has_players(self):
        components = self.parse_file("video_feed.json")["components"]
        assert all(["videoPlayerHtml" in x.get("thumbadd", {}) for x in components])
        assert all(["<iframe" in x["thumbadd"]["videoPlayerHtml"] for x in components])

    def test_format_video_duration(self):
        parser = self._get_parser_class()()
        formatter = parser._format_video_duration

        assert formatter(44) == "0:44"
        assert formatter(70) == "1:10"
        assert formatter(543) == "9:03"
        assert formatter(777) == "12:57"
        assert formatter(3600) == "1:00:00"
        assert formatter(3601) == "1:00:01"
        assert formatter(3691) == "1:01:31"
        assert formatter(65*60) == "1:05:00"
        assert formatter(60*60*10) == "10:00:00"
        assert formatter(23*60*60 + 59*60 + 59) == "23:59:59"
        assert formatter(23*60*60 + 59*60 + 59 + 1) == "0:00:00"
        assert formatter(23*60*60 + 59*60 + 59 + 2) == "0:00:01"

    def test_video_items_has_definition(self):
        components = self.parse_file("video_feed.json")["components"]
        assert all(["long.videoHdFlag" in x for x in components])
        # first item sd, others hd
        assert components[0]["long.videoHdFlag"] == 0
        assert all([x["long.videoHdFlag"] == 1 for x in components[1:]])

    # Images feed:

    def test_parse_images_feed(self):
        components = self.parse_file("images_feed.json")["components"]
        assert 10 == len(components)

    def test_images_items_has_type(self):
        components = self.parse_file("images_feed.json")["components"]
        assert all(["text.cardType" in x for x in components])
        assert all([x["text.cardType"] == "image" for x in components])

    def test_images_items_has_ids(self):
        components = self.parse_file("images_feed.json")["components"]
        assert all(["text.cardId" in x for x in components])
        assert all(["text.imageDocumentId" in x for x in components])
        assert all(["text.thumbId" in x for x in components[:9]])
        assert "text.thumbId" not in components[9]

        assert all([len(x["text.cardId"]) > 0 for x in components[1:]])
        assert all([len(x["text.imageDocumentId"]) > 0 for x in components[1:]])
        assert all([len(x["text.thumbId"]) > 0 for x in components[1:9]])

    def test_images_items_contains_thumb_sizes(self):
        components = self.parse_file("images_feed.json")["components"]
        assert all(["dimension.IMAGE_DIMENSION" in x for x in components])
        assert all([x.get("dimension.IMAGE_DIMENSION", {}).get("w") > 0 for x in components])
        assert all([x.get("dimension.IMAGE_DIMENSION", {}).get("h") > 0 for x in components])

    def test_images_items_has_image_url(self):
        components = self.parse_file("images_feed.json")["components"]
        assert all(["url.imageUrl" in x for x in components])
        assert all([len(x["url.imageUrl"]) > 0 for x in components[1:]])

    # News feed:

    def test_parse_news_feed(self):
        components = self.parse_file("news_feed.json")["components"]
        assert 9 == len(components)

    def test_news_items_has_type(self):
        components = self.parse_file("news_feed.json")["components"]
        assert all(["text.cardType" in x for x in components])
        assert all([x["text.cardType"] == "news" for x in components])

    def test_news_items_has_ids(self):
        components = self.parse_file("news_feed.json")["components"]
        assert all(["text.cardId" in x for x in components])
        assert all([len(x["text.cardId"]) > 0 for x in components])

    def test_news_items_has_news_urls(self):
        components = self.parse_file("news_feed.json")["components"]
        assert all(["url.newsUrl" in x for x in components])
        assert all([len(x["url.newsUrl"]) > 0 for x in components])

    def test_news_items_has_image_urls(self):
        components = self.parse_file("news_feed.json")["components"]
        assert all(["url.imageUrl" in x for x in components])
        assert all([len(x["url.imageUrl"]) > 0 for x in components])

    def test_news_items_contains_thumb_sizes(self):
        components = self.parse_file("news_feed.json")["components"]
        assert all(["dimension.IMAGE_DIMENSION" in x for x in components])
        assert all([x.get("dimension.IMAGE_DIMENSION", {}).get("w") > 0 for x in components])
        assert all([x.get("dimension.IMAGE_DIMENSION", {}).get("h") > 0 for x in components])

    # Web feed:

    def test_parse_web_feed(self):
        components = self.parse_file("web_feed.json")["components"]
        assert 20 == len(components)

    def test_web_items_has_type(self):
        components = self.parse_file("web_feed.json")["components"]
        assert all(["text.cardType" in x for x in components])
        assert all([x["text.cardType"] == "link" for x in components])

    def test_web_items_has_ids(self):
        components = self.parse_file("web_feed.json")["components"]
        assert all(["text.cardId" in x for x in components])
        assert all([len(x["text.cardId"]) > 0 for x in components])

    def test_web_items_has_image_urls(self):
        components = self.parse_file("web_feed.json")["components"]
        assert all(["url.imageUrl" in x for x in components])
        assert all([len(x["url.imageUrl"]) > 0 for x in components])

    def test_web_items_contains_thumb_sizes(self):
        components = self.parse_file("web_feed.json")["components"]
        assert all(["dimension.IMAGE_DIMENSION" in x for x in components])
        assert all([x.get("dimension.IMAGE_DIMENSION", {}).get("w") > 0 for x in components])
        assert all([x.get("dimension.IMAGE_DIMENSION", {}).get("h") > 0 for x in components])

    def test_web_has_turbo_url(self):
        components = self.parse_file("web_feed_turbo.json")["components"]
        assert 2 == len(components)
        assert components[0]["tags.isTurbo"] is True
        assert components[0]["url.originalUrl"] != components[0]["componentUrl"]["pageUrl"]
        assert 'yandex.ru/turbo' in components[0]["componentUrl"]["pageUrl"]
        assert 'yandex.ru/turbo' not in components[0]["url.originalUrl"]
        assert 'http://' in components[0]["url.originalUrl"] or 'https://' in components[0]["url.originalUrl"]

        assert components[1]["tags.isTurbo"] is False
        assert components[1]["url.originalUrl"] == components[1]["componentUrl"]["pageUrl"]

    def test_web_cards_has_thumbs(self):
        components = self.parse_file("web_feed_from_collections.json")["components"]
        assert all(["url.imageUrl" in x for x in components])
        assert all([len(x["url.imageUrl"]) > 0 for x in components])
        assert all(["https://" in x["url.imageUrl"] for x in components])

        # тумбы от рекомендаций веба и коллекционные тумбы
        assert "im2-tub-com.yandex.net" in components[0]["url.imageUrl"]
        assert "avatars.mds.yandex.net/get-pdb" in components[1]["url.imageUrl"]
        assert "avatars.mds.yandex.net/get-shinyserp" in components[2]["url.imageUrl"]

    # Toloka json_output card parse components

    def test_parse_toloka_json_component(self):
        components = self.parse_file("web_feed_static_dynamic_hp_from_toloka_components.json")["components"]
        assert 7 == len(components)

    def test_parse_toloka_json_web_component(self):
        components = self.parse_file("web_feed_static_dynamic_hp_from_toloka_components.json")["components"]
        assert all([x["text.cardType"] == "link" for x in components[:3]])
        assert all([len(x["componentUrl"]["pageUrl"]) > 0 for x in components[:3]])

    def test_parse_toloka_json_image_component(self):
        components = self.parse_file("web_feed_static_dynamic_hp_from_toloka_components.json")["components"]

        assert components[3]["text.cardType"] == "image"
        assert components[4]["text.cardType"] == "image"
        assert components[5]["text.cardType"] == "collections"
        assert components[6]["text.cardType"] == "image"
        assert all([len(x["url.imageUrl"]) > 0 for x in components[3:5]])

    # Preparer

    def test_preparer_hosts(self):
        preparer = self._get_parser_class()()
        assert preparer._prepare_url({"text": "12345"}, "yandex.ru") == "https://yandex.ru/collections/api/user/feed" + \
            "?puid=12345&utm_source=scraper-collections-feed&rec_flags=force_source_responses%3Dtrue%3Bupdate_history%3Dfalse%3Bpuid%3D12345"
        assert preparer._prepare_url({"text": "12345"}, "priemka-quality.collections.test.yandex.ru") == "https://priemka-quality.collections.test.yandex.ru/collections/api/user/feed" + \
            "?puid=12345&utm_source=scraper-collections-feed&rec_flags=force_source_responses%3Dtrue%3Bupdate_history%3Dfalse%3Bpuid%3D12345"

    def test_preparer_query_uid(self):
        preparer = self._get_parser_class()()
        assert preparer._prepare_url({"text": "12345", "uid": "y777"}, "yandex.ru") == "https://yandex.ru/collections/api/user/feed" + \
            "?puid=12345&utm_source=scraper-collections-feed&rec_flags=force_source_responses%3Dtrue%3Bupdate_history%3Dfalse%3Bpuid%3D12345%3Byandexuid%3D777%3Bicookie%3D777"
        assert preparer._prepare_url({"text": "12345", "uid": "y888"}, "yandex.ru") == "https://yandex.ru/collections/api/user/feed" + \
            "?puid=12345&utm_source=scraper-collections-feed&rec_flags=force_source_responses%3Dtrue%3Bupdate_history%3Dfalse%3Bpuid%3D12345%3Byandexuid%3D888%3Bicookie%3D888"

    def test_preparer_no_query(self):
        preparer = self._get_parser_class()()
        assert preparer._prepare_url({"uid": "y777"}, "yandex.ru") == "https://yandex.ru/collections/api/user/feed" + \
            "?utm_source=scraper-collections-feed&rec_flags=force_source_responses%3Dtrue%3Bupdate_history%3Dfalse%3Byandexuid%3D777%3Bicookie%3D777"
        assert preparer._prepare_url({"cgiParams": "&rec_flags=a=b=c"}, "yandex.ru") == "https://yandex.ru/collections/api/user/feed" + \
            "?utm_source=scraper-collections-feed&rec_flags=force_source_responses%3Dtrue%3Bupdate_history%3Dfalse%3Ba%3Db%3Dc"

    def test_preparer_query_uid_from_params(self):
        preparer = self._get_parser_class()()
        assert preparer._prepare_url({"text": "12345", "cgiParams": "&yandexuid=777&icookie=777"}, "yandex.ru") == "https://yandex.ru/collections/api/user/feed" + \
            "?puid=12345&utm_source=scraper-collections-feed&rec_flags=force_source_responses%3Dtrue%3Bupdate_history%3Dfalse%3Bpuid%3D12345%3Byandexuid%3D777%3Bicookie%3D777"
        assert preparer._prepare_url({"text": "12345", "cgiParams": "&rnd1=111&yandexuid=888&rnd2=222"}, "yandex.ru") == "https://yandex.ru/collections/api/user/feed" + \
            "?puid=12345&utm_source=scraper-collections-feed&rec_flags=force_source_responses%3Dtrue%3Bupdate_history%3Dfalse%3Bpuid%3D12345%3Byandexuid%3D888%3Bicookie%3D888&rnd1=111&rnd2=222"

    def test_preparer_query_rec_flags(self):
        preparer = self._get_parser_class()()
        assert preparer._prepare_url({"text": "12345", "cgiParams": "&rec_flags=a=b=c"}, "yandex.ru") == "https://yandex.ru/collections/api/user/feed" + \
            "?puid=12345&utm_source=scraper-collections-feed&rec_flags=force_source_responses%3Dtrue%3Bupdate_history%3Dfalse%3Bpuid%3D12345%3Ba%3Db%3Dc"
        assert preparer._prepare_url({"text": "12345", "cgiParams": "&rnd1=111&rec_flags=flag1=value1;flag2=value2&rnd2=222"}, "yandex.ru") == "https://yandex.ru/collections/api/user/feed" + \
            "?puid=12345&utm_source=scraper-collections-feed&rec_flags=force_source_responses%3Dtrue%3Bupdate_history%3Dfalse%3Bpuid%3D12345%3Bflag1%3Dvalue1%3Bflag2%3Dvalue2&rnd1=111&rnd2=222"
        assert preparer._prepare_url({"text": "12345", "cgiParams": "&rnd1=111&rec_flags=flag2=value2;flag1=value1&rnd2=222"}, "yandex.ru") == "https://yandex.ru/collections/api/user/feed" + \
            "?puid=12345&utm_source=scraper-collections-feed&rec_flags=force_source_responses%3Dtrue%3Bupdate_history%3Dfalse%3Bpuid%3D12345%3Bflag2%3Dvalue2%3Bflag1%3Dvalue1&rnd1=111&rnd2=222"

    def test_preparer_cron_rec_flags(self):
        preparer = self._get_parser_class()()
        assert preparer._prepare_url({"text": "12345"}, "yandex.ru", {"cgi": "&rec_flags=a=b=c"}) == "https://yandex.ru/collections/api/user/feed" + \
            "?puid=12345&utm_source=scraper-collections-feed&rec_flags=force_source_responses%3Dtrue%3Bupdate_history%3Dfalse%3Bpuid%3D12345%3Ba%3Db%3Dc"
        assert preparer._prepare_url({"text": "12345"}, "yandex.ru", {"cgi": "&rnd1=111&rec_flags=a=b=c&rnd2=222"}) == "https://yandex.ru/collections/api/user/feed" + \
            "?puid=12345&utm_source=scraper-collections-feed&rec_flags=force_source_responses%3Dtrue%3Bupdate_history%3Dfalse%3Bpuid%3D12345%3Ba%3Db%3Dc&rnd1=111&rnd2=222"

    def test_preparer_merge_rec_flags(self):
        preparer = self._get_parser_class()()
        assert preparer._prepare_url({"text": "12345"}, "yandex.ru", {"cgi": "&rec_flags=update_history=true"}) == "https://yandex.ru/collections/api/user/feed" + \
            "?puid=12345&utm_source=scraper-collections-feed&rec_flags=force_source_responses%3Dtrue%3Bupdate_history%3Dtrue%3Bpuid%3D12345"
        assert preparer._prepare_url({"text": "12345", "cgiParams": "&rec_flags=a=b=c"}, "yandex.ru", {"cgi": "&rec_flags=c=d"}) == "https://yandex.ru/collections/api/user/feed" + \
            "?puid=12345&utm_source=scraper-collections-feed&rec_flags=force_source_responses%3Dtrue%3Bupdate_history%3Dfalse%3Bpuid%3D12345%3Ba%3Db%3Dc%3Bc%3Dd"
        assert preparer._prepare_url({"text": "12345", "cgiParams": "&rec_flags=a=1"}, "yandex.ru", {"cgi": "&rec_flags=a=2"}) == "https://yandex.ru/collections/api/user/feed" + \
            "?puid=12345&utm_source=scraper-collections-feed&rec_flags=force_source_responses%3Dtrue%3Bupdate_history%3Dfalse%3Bpuid%3D12345%3Ba%3D2"
