import pytest


class Args():
    def __init__(self):
        self.juggler_api = 'http://juggler-api.search.yandex.net'
        self.oauth_token = None
        self.cleanup_tag = '_market_cleanup_tag_'
        self.mandatory_tags = ['market', '_market_']
        self.verbose = False
        self.dry_run = True


@pytest.fixture(scope='module')
def args():
    return Args()
