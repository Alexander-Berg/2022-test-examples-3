from test_utils import TestParser


class TestYandexSearchMusicWizardBaobabHtmlParser(TestParser):

    def test_track_wizard(self):
        parsed_json = self.parse_file('track_wizard.html')
        assert len(parsed_json['components']) == 1
        wizard = parsed_json["components"][0]
        assert wizard["componentUrl"]["pageUrl"] == "https://music.yandex.ru/album/1878821/track/15672?from=serp"
        assert wizard["text.title"] == "<b>Fernando</b> — слушайте бесплатно онлайн"
        assert wizard["text.baobabWizardName"] == "musicplayer"
        assert wizard["text.baobabWizardSubtype"] == "track"

        wizard_elements = wizard["json.wizardElements"]
        self._assert_wizard_elements(
            wizard_elements,
            [
                ("play", "",
                 "https://music.yandex.ru/album/1878821/track/15672?play=1&from=serp_autoplay"),
                ("track", "Fernando  — ABBA",
                 "https://music.yandex.ru/album/1878821/track/15672?play=1&from=serp_autoplay"),
                ("more", "Ещё 123 трека исполнителя \"ABBA\"",
                 "https://music.yandex.ru/artist/9367?from=serp"),
            ],
        )

    def test_album_wizard(self):
        parsed_json = self.parse_file('album_wizard.html')

        assert len(parsed_json["components"]) == 1
        wizard = parsed_json["components"][0]
        assert wizard["componentUrl"]["pageUrl"] == "https://music.yandex.ru/album/37335?from=serp"
        assert wizard["text.title"] == "<b>Madonna — Madonna</b> на Яндекс.Музыке"
        assert wizard["text.baobabWizardName"] == "musicplayer"
        assert wizard["text.baobabWizardSubtype"] == "album"

        wizard_elements = wizard["json.wizardElements"]
        self._assert_wizard_elements(
            wizard_elements,
            [
                ("image", "",
                 "https://music.yandex.ru/album/37335?from=serp"),
                ("play", "",
                 "https://music.yandex.ru/album/37335/track/216197?playTrack=216197&from=serp_autoplay"),
                ("track", "Lucky Star",
                 "https://music.yandex.ru/album/37335/track/216197?playTrack=216197&from=serp_autoplay"),
                ("play", "",
                 "https://music.yandex.ru/album/37335/track/216198?playTrack=216198&from=serp_autoplay"),
                ("track", "Burning Up",
                 "https://music.yandex.ru/album/37335/track/216198?playTrack=216198&from=serp_autoplay"),
                ("play", "",
                 "https://music.yandex.ru/album/37335/track/216200?playTrack=216200&from=serp_autoplay"),
                ("track", "Borderline",
                 "https://music.yandex.ru/album/37335/track/216200?playTrack=216200&from=serp_autoplay"),
                ("more", "Ещё 5 треков альбома",
                 "https://music.yandex.ru/album/37335?from=serp"),
            ]
        )

    def test_artist_wizard(self):
        parsed_json = self.parse_file('artist_wizard.html')

        assert len(parsed_json["components"]) == 1
        wizard = parsed_json["components"][0]
        assert wizard["componentUrl"]["pageUrl"] == "https://music.yandex.ru/artist/1813?from=serp"
        assert wizard["text.title"] == "Слушайте бесплатно — <b>madonna</b>"
        assert wizard["text.baobabWizardName"] == "musicplayer"
        assert wizard["text.baobabWizardSubtype"] == "artist"

        wizard_elements = wizard["json.wizardElements"]
        self._assert_wizard_elements(
            wizard_elements,
            [
                ("image", "",
                 "https://music.yandex.ru/artist/1813?from=serp"),
                ("play", "",
                 "https://music.yandex.ru/artist/1813?playTrack=125949&from=serp_autoplay"),
                ("track", "The Power of Good-Bye",
                 "https://music.yandex.ru/artist/1813?playTrack=125949&from=serp_autoplay"),
                ("play", "", "https://music.yandex.ru/artist/1813?playTrack=133054&from=serp_autoplay"),
                ("track", "Hung Up Radio Version",
                 "https://music.yandex.ru/artist/1813?playTrack=133054&from=serp_autoplay"),
                ("play", "",
                 "https://music.yandex.ru/artist/1813?playTrack=133207&from=serp_autoplay"),
                ("track", "4 Minutes feat. Justin Timberlake and Timbaland",
                 "https://music.yandex.ru/artist/1813?playTrack=133207&from=serp_autoplay"),
                ("radio", "",
                 "https://music.yandex.ru/artist/1813?radio=play&from=serp"),
                ("similar", "Shakira",
                 "https://music.yandex.ru/artist/118883?from=serp"),
                ("similar", "Robbie Williams",
                 "https://music.yandex.ru/artist/14240?from=serp"),
                ("similar", "Lady Gaga",
                 "https://music.yandex.ru/artist/1438?from=serp"),
                ("similar", "Rihanna",
                 "https://music.yandex.ru/artist/3477?from=serp"),
                ("more", "Ещё 401 трек исполнителя",
                 "https://music.yandex.ru/artist/1813?from=serp"),
            ]
        )

    def test_playlist_wizard(self):
        parsed_json = self.parse_file('playlist_wizard.html')

        assert len(parsed_json["components"]) == 1
        wizard = parsed_json["components"][0]
        assert wizard["componentUrl"]["pageUrl"] == "https://music.yandex.ru/users/music-blog/playlists/2131?from=serp"
        assert wizard["text.title"] == "<b>Вечный рок</b> на Яндекс.Музыке"
        assert wizard["text.baobabWizardName"] == "musicplayer"
        assert wizard["text.baobabWizardSubtype"] == "playlist"

        wizard_elements = wizard["json.wizardElements"]
        self._assert_wizard_elements(
            wizard_elements,
            [
                ("play", "",
                 "https://music.yandex.ru/users/music-blog/playlists/2131?playTrack=10216&from=serp_autoplay"),
                ("track", "Wind Of Change  — Scorpions",
                 "https://music.yandex.ru/users/music-blog/playlists/2131?playTrack=10216&from=serp_autoplay"),
                ("play", "",
                 "https://music.yandex.ru/users/music-blog/playlists/2131?playTrack=137936&from=serp_autoplay"),
                ("track", "Rockin&apos; in the Free World  — Neil Young",
                 "https://music.yandex.ru/users/music-blog/playlists/2131?playTrack=137936&from=serp_autoplay"),
                ("play", "",
                 "https://music.yandex.ru/users/music-blog/playlists/2131?playTrack=14370992&from=serp_autoplay"),
                ("track", "Won&apos;t Get Fooled Again Single Edit  — The Who",
                 "https://music.yandex.ru/users/music-blog/playlists/2131?playTrack=14370992&from=serp_autoplay"),
                ("more", "Ещё 95 треков Вечный рок",
                 "https://music.yandex.ru/users/music-blog/playlists/2131?from=serp"),
            ]
        )

    def _assert_wizard_elements(self, elements, expected_elements):
        assert len(elements) == len(expected_elements)
        for element, (name, text, url) in zip(elements, expected_elements):
            self._assert_wizard_element(element, name, text, url)

    @staticmethod
    def _assert_wizard_element(element, name, text, url):
        assert element["name"] == name
        assert element["text"] == text
        assert element["url"] == url
