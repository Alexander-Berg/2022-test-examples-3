class Config(object):
    def __init__(self):
        self.host_limit = 100
        self.source_types = ['opengraph', 'regexps', 'json.ld', 'json.semantic.schema', 'wildplayer', 'og.twitter']
        self.player_types = ['iframe', 'embed_url']
        self.max_input_rows = 10000
        self.sample_size = 100
        self.saved_sample_size = 5
        self.max_loss_rate = 0.2
        self.max_vdp_rate = 0.1
        self.max_popup_rate = 0.1
        self.allowed_popup_rate = 0.05
        self.max_player_dup_rate = 0.5
        self.min_major_host_rate = 0.75
        self.max_known_host_rate = 0.3
        self.max_scrolling_rate = 0.2
        self.max_known_player_rate = 0.3
        self.known_hosts = set(['m.youtube.com', 'youtu.be', 'youtube.com', 'www.youtube.com', 'vk.com', 'ok.ru', 'my.mail.ru', 'rutube.ru', 'vimeo.com'])
        self.banned_hosts = set(['more.tv'])
        self.filtered_hosts = set(['bandcamp.com', 'video.az'])
        self.platform = 'Desktop'
        self.version = '2.0'
        self.probing_size = 10
        self.min_moving_area = 0.4
        self.digging_days = 90
        self.digging_size = 1000
