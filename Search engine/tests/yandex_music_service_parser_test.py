from test_utils import TestParser


class TestYandexMusicServiceParser(TestParser):

    def serp(self):
        return self.parse_file('test_serp.json')

    def test_serp(self):
        serp_info = self.serp()
        assert serp_info['documents-found'] == 901
        assert serp_info['components'][0]['page-url'] == 'https://music.yandex.ru/artist/1813'
