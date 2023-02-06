import shapely.geometry
from django.test import TestCase

from ..core.parking_area_checker import ParkingAreaChecker
from ..models.car_location import CarLocation


class ParkingAreaCheckerTestCase(TestCase):

    def test_effective_area_larger_than_visible(self):
        effective_area = shapely.geometry.Polygon([
            [1, 1],
            [1, -1],
            [-1, -1],
            [-1, 1],
            [1, 1],
        ])
        visible_area = shapely.geometry.Polygon([
            [1, 1],
            [1, 0.5],
            [0.5, 0.5],
            [0.5, 1],
            [1, 1],
        ])
        checker = ParkingAreaChecker(
            parking_area_effective=effective_area,
            parking_area_visible=visible_area,
        )

        location1 = CarLocation(lat=0.75, lon=0.75)
        checker.check(location1)

        location2 = CarLocation(lat=0, lon=0)
        checker.check(location2)

        location3 = CarLocation(lat=2, lon=2)
        with self.assertRaises(checker.ForbiddenLocationError):
            checker.check(location3)
