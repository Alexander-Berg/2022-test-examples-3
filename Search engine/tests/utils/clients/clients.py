from search.mon.warden.src import config

from search.mon.warden.src.clients import TickenatorClientMock
from search.mon.warden.tests.utils.clients import StClientMock, JugglerClientMock, AbcClientMock


class Clients(object):
    __slots__ = (
        'abc',
        'juggler',
        'startrek',
        'warden_startrek',
        'config',
        'tickenator',
    )

    def __init__(self):

        self.abc = AbcClientMock()
        self.juggler = JugglerClientMock()
        self.startrek = StClientMock()
        self.warden_startrek = StClientMock()
        self.config = config.Config().config
        self.tickenator = TickenatorClientMock()
