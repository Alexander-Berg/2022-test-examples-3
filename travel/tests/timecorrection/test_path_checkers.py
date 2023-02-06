# -*- coding: utf-8 -*-

import pytest

from common.models.geo import Station
from travel.rasp.admin.timecorrection.path_checkers import GeometryCheck


class TestGeometryCheck(object):
    station_a = Station(latitude=55.715251431239594, longitude=37.6275861640625)  # Москва
    station_b = Station(latitude=54.17068792209795, longitude=37.66315182476469)  # Тула
    station_c = Station(latitude=52.60788480678189, longitude=38.49156461244351)  # Елец
    station_d = Station(latitude=51.68544685486367, longitude=39.162569733771484)  # Воронеж
    station_e = Station(latitude=53.182031605461795, longitude=50.16952168412026)  # Саратов

    bad_station = Station(longitude=None, latitude=55.734071)

    good_path = (station_a, station_b, station_c, station_d, station_e)
    bad_path = (station_a, station_c, station_b, station_d, station_e)
    bad_path_with_none = (station_a, station_b, bad_station, station_c, station_d, station_e)

    check_path = GeometryCheck()

    def test_check_coord_not_none(self):
        """проверка отсутсвия вывода ошибки при корректном пути"""
        try:
            GeometryCheck.check_coord_not_none(self.good_path)
        except GeometryCheck.GeometryCheckError:
            pytest.fail('Unexpected error')

    def test_check_coord_not_none_with_error(self):
        """проверка вывода ошибки при отсутсвии координат у станции"""
        with pytest.raises(GeometryCheck.GeometryCheckError):
            GeometryCheck.check_coord_not_none(self.bad_path_with_none)

    def test_check_angles_between_stations(self):
        """Проверка check_angles_between_stations для хорошего пути"""
        try:
            self.check_path.check_angles_between_stations(self.good_path)
        except GeometryCheck.GeometryCheckError:
            pytest.fail('Unexpected error')

    def test_get_angle_between_station(self):
        """Тест get_angle_between_station"""
        angle = self.check_path.get_angle_between_station(self.station_a, self.station_b, self.station_c)
        assert 162.87 == round(angle, 2)

    def test_get_angle_between_station_with_error(self):
        """Тест angle_between_station для повторяющейся станции"""
        with pytest.raises(self.check_path.GeometryCheckError):
            self.check_path.get_angle_between_station(self.station_a, self.station_a, self.station_b)

    def test_three_sides_given(self):
        """Тест three_sides_given. """
        from common.utils.geo import great_circle_angular_distance

        Moscow_Voronezh_len = great_circle_angular_distance(self.station_a, self.station_d)
        Voronezh_Samara_len = great_circle_angular_distance(self.station_d, self.station_e)
        Moscow_Samara_len = great_circle_angular_distance(self.station_a, self.station_e)

        angle_rad = round(
            self.check_path.three_sides_given(Moscow_Voronezh_len, Voronezh_Samara_len, Moscow_Samara_len), 2)
        assert 1.49 == angle_rad  # 85.37 градуса

    def test_is_path_correct(self):
        """Тестируем логику is_path_correct"""
        assert self.check_path.is_path_correct(self.good_path)

    @pytest.mark.parametrize('path', [bad_path, bad_path_with_none])
    def test_is_path_correct_with_error(self, path):
        """Тестируем логику is_path_correct для плохого пути"""
        assert not self.check_path.is_path_correct(path)
