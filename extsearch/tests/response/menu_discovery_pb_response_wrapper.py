# encoding: utf-8

from .base_response_wrapper import ResponseSpecificParameters, SearchResult

import yandex.maps.proto.menu.menu_pb2 as menu_pb


class MenuItem(object):
    def __init__(self, message):
        self.message = message


class MenuPbResult(SearchResult):
    @classmethod
    def get_specific_params(cls):
        return ResponseSpecificParameters(ms='menu_discovery_pb', gta=[])

    def __init__(self, serialized_message):
        self.message = menu_pb.MenuInfo()
        self.message.ParseFromString(serialized_message)
        err_list = []
        assert self.message.IsInitialized(err_list), 'Message is not initialized\n%s' % err_list
        self.menu_items = [MenuItem(obj) for obj in self.message.menu_item]

    def is_not_empty(self):
        return len(self.menu_items) > 0

    def bounded_by(self):
        bbox = self.message.bounded_by
        return '{0:f},{1:f} {2:f},{3:f}'.format(
            bbox.lower_corner.lon, bbox.lower_corner.lat, bbox.upper_corner.lon, bbox.upper_corner.lat
        )
