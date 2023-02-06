from typing import Dict, List, Optional, Tuple


class MockGeoFormatManager:
    def __init__(
        self,
        connections: List[Tuple[str, int]],
        parents: Optional[Dict[str, List[str]]] = None,
        titles: Optional[Dict[str, str]] = None,
    ):
        self.connections = connections
        if parents is None:
            parents = {}
        self.parents = parents
        self.titles = titles

    def get_point_key_by_geo_id(self, geo_id: int) -> Optional[str]:
        for cur_point_key, cur_geo_id in self.connections:
            if cur_geo_id == geo_id:
                return cur_point_key
        return None

    def get_geo_id_by_point_key(self, point_key: str) -> Optional[int]:
        for cur_point_key, cur_geo_id in self.connections:
            if cur_point_key == point_key:
                return cur_geo_id
        return None

    def get_point_key_parents(self, point_key: str) -> List[str]:
        return self.parents.get(point_key, [])

    def get_point_ru_name_by_point_key(self, point_key: str) -> Optional[str]:
        return self.titles.get(point_key, None)

    def has_point_key_in_hierarchy(self, point_key: str, search_point_key: str) -> bool:
        if point_key == search_point_key:
            return True

        parents = self.get_point_key_parents(point_key)
        for parent in parents:
            if parent == search_point_key:
                return True

        return False


default_mock_geo_format_manager = MockGeoFormatManager(
    connections=[
        ('l1', 1),
        ('l2', 2),
        ('l3', 3),
        ('l4', 4),
        ('r1', 5),
        ('r2', 6),
        ('r3', 7),
        ('r4', 8),
        ('c1', 9),
        ('c2', 10),
        ('c3', 11),
        ('c4', 12),
    ],
    titles={
        'l1': 'Банания',
        'l2': 'Финикия',
    },
)


class MockIdGeoFormatManager:
    MOCK_POINT_KEY_PREFIX = 'p'

    def get_point_key_by_geo_id(self, geo_id: int) -> Optional[str]:
        return self.MOCK_POINT_KEY_PREFIX + str(geo_id)

    def get_geo_id_by_point_key(self, point_key: str) -> Optional[int]:
        prefix = point_key[0]
        if prefix == self.MOCK_POINT_KEY_PREFIX:
            return int(point_key[1:])
        return None
