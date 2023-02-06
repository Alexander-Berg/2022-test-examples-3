# -*- coding: utf-8 -*-

import mock
import pytest

from common.models.geo import Station
from travel.rasp.admin.timecorrection.utils import Constants
from travel.rasp.admin.timecorrection.data_downloaders import MapsDataDownloader, GeoCoderDataDownloader

good_map_response = '''<?xml version="1.0" encoding="utf-8"?>
<ymaps:ymaps xmlns:ymaps="http://maps.yandex.ru/ymaps/1.x" xmlns:gml="http://www.opengis.net/gml" xmlns:r="http://maps.yandex.ru/router/1.x" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maps.yandex.ru/router/1.x http://maps.yandex.ru/schemas/router/1.x/router.xsd">
  <ymaps:GeoObjectCollection>
    <gml:metaDataProperty>
      <r:RouterRouteMetaData>
        <r:length>393.62</r:length>
        <r:humanLength unit="meters">393.00</r:humanLength>
        <r:Length>
          <r:value>393.62</r:value>
          <r:text>390 м</r:text>
        </r:Length>
        <r:time>198.72</r:time>
        <r:Time>
          <r:value>198.72</r:value>
          <r:text>3 мин</r:text>
        </r:Time>
        <r:jamsTime>367.19</r:jamsTime>
        <r:JamsTime>
          <r:value>367.19</r:value>
          <r:text>6 мин</r:text>
        </r:JamsTime>
      </r:RouterRouteMetaData>
    </gml:metaDataProperty>
    <gml:boundedBy>
      <gml:Envelope>
        <gml:lowerCorner>37.589401 55.732656</gml:lowerCorner>
        <gml:upperCorner>37.592801 55.734130</gml:upperCorner>
      </gml:Envelope>
    </gml:boundedBy>
    <gml:featureMembers>
      <ymaps:GeoObject>
        <gml:Point>
          <gml:pos>37.589401 55.734071</gml:pos>
        </gml:Point>
      </ymaps:GeoObject>
      <ymaps:GeoObjectCollection>
        <gml:metaDataProperty>
          <r:RouterRouteMetaData>
            <r:length>393.62</r:length>
            <r:humanLength unit="meters">393.00</r:humanLength>
            <r:Length>
              <r:value>393.62</r:value>
              <r:text>390 м</r:text>
            </r:Length>
            <r:time>198.72</r:time>
            <r:Time>
              <r:value>198.72</r:value>
              <r:text>3 мин</r:text>
            </r:Time>
            <r:jamsTime>367.19</r:jamsTime>
            <r:JamsTime>
              <r:value>367.19</r:value>
              <r:text>6 мин</r:text>
            </r:JamsTime>
          </r:RouterRouteMetaData>
        </gml:metaDataProperty>
        <gml:boundedBy>
          <gml:Envelope>
            <gml:lowerCorner>37.589541 55.732656</gml:lowerCorner>
            <gml:upperCorner>37.592801 55.734130</gml:upperCorner>
          </gml:Envelope>
        </gml:boundedBy>
        <gml:featureMembers>
          <ymaps:GeoObject>
            <gml:metaDataProperty>
              <r:RouterSegmentMetaData>
                <r:street>улица Тимура Фрунзе</r:street>
                <r:action>none</r:action>
                <r:Action>
                  <r:value>none</r:value>
                  <r:text>прямо</r:text>
                </r:Action>
                <r:angle>0</r:angle>
                <r:length>215.97</r:length>
                <r:humanLength unit="meters">215.00</r:humanLength>
                <r:Length>
                  <r:value>215.97</r:value>
                  <r:text>220 м</r:text>
                </r:Length>
                <r:time>43.19</r:time>
                <r:Time>
                  <r:value>43.19</r:value>
                  <r:text>1 мин</r:text>
                </r:Time>
                <r:jamsTime>42.44</r:jamsTime>
                <r:JamsTime>
                  <r:value>42.44</r:value>
                  <r:text>1 мин</r:text>
                </r:JamsTime>
                <r:text>улица Тимура Фрунзе</r:text>
              </r:RouterSegmentMetaData>
            </gml:metaDataProperty>
            <gml:boundedBy>
              <gml:Envelope>
                <gml:lowerCorner>37.589541 55.732656</gml:lowerCorner>
                <gml:upperCorner>37.591770 55.734130</gml:upperCorner>
              </gml:Envelope>
            </gml:boundedBy>
            <gml:MultiGeometry>
              <gml:geometryMembers>
                <gml:LineString>
                  <gml:posList>37.589541 55.734130 37.589840 55.733944 37.590761 55.733378 37.590816 55.733344 37.590863 55.733315 37.590955 55.733258 37.591062 55.733180 37.591204 55.733081 37.591364 55.732964 37.591553 55.732827 37.591770 55.732656</gml:posList>
                </gml:LineString>
              </gml:geometryMembers>
            </gml:MultiGeometry>
          </ymaps:GeoObject>
          <ymaps:GeoObject>
            <gml:metaDataProperty>
              <r:RouterSegmentMetaData>
                <r:street/>
                <r:action>left</r:action>
                <r:Action>
                  <r:value>left</r:value>
                  <r:text>налево</r:text>
                </r:Action>
                <r:angle>114.04</r:angle>
                <r:length>105.75</r:length>
                <r:humanLength unit="meters">105.00</r:humanLength>
                <r:Length>
                  <r:value>105.75</r:value>
                  <r:text>110 м</r:text>
                </r:Length>
                <r:time>61.15</r:time>
                <r:Time>
                  <r:value>61.15</r:value>
                  <r:text>1 мин</r:text>
                </r:Time>
                <r:jamsTime>113.75</r:jamsTime>
                <r:JamsTime>
                  <r:value>113.75</r:value>
                  <r:text>2 мин</r:text>
                </r:JamsTime>
                <r:text>Налево</r:text>
              </r:RouterSegmentMetaData>
            </gml:metaDataProperty>
            <gml:boundedBy>
              <gml:Envelope>
                <gml:lowerCorner>37.591770 55.732656</gml:lowerCorner>
                <gml:upperCorner>37.592294 55.733541</gml:upperCorner>
              </gml:Envelope>
            </gml:boundedBy>
            <gml:MultiGeometry>
              <gml:geometryMembers>
                <gml:LineString>
                  <gml:posList>37.591770 55.732656 37.591863 55.732745 37.591904 55.732796 37.592290 55.733459 37.592294 55.733478 37.592289 55.733497 37.592275 55.733514 37.592233 55.733541</gml:posList>
                </gml:LineString>
              </gml:geometryMembers>
            </gml:MultiGeometry>
          </ymaps:GeoObject>
          <ymaps:GeoObject>
            <gml:metaDataProperty>
              <r:RouterSegmentMetaData>
                <r:street/>
                <r:action>left</r:action>
                <r:Action>
                  <r:value>left</r:value>
                  <r:text>налево</r:text>
                </r:Action>
                <r:angle>18.50</r:angle>
                <r:length>9.05</r:length>
                <r:humanLength unit="meters">9.00</r:humanLength>
                <r:Length>
                  <r:value>9.05</r:value>
                  <r:text>9 м</r:text>
                </r:Length>
                <r:time>41.81</r:time>
                <r:Time>
                  <r:value>41.81</r:value>
                  <r:text>1 мин</r:text>
                </r:Time>
                <r:jamsTime>92.19</r:jamsTime>
                <r:JamsTime>
                  <r:value>92.19</r:value>
                  <r:text>2 мин</r:text>
                </r:JamsTime>
                <r:text>Налево</r:text>
              </r:RouterSegmentMetaData>
            </gml:metaDataProperty>
            <gml:boundedBy>
              <gml:Envelope>
                <gml:lowerCorner>37.592108 55.733541</gml:lowerCorner>
                <gml:upperCorner>37.592233 55.733581</gml:upperCorner>
              </gml:Envelope>
            </gml:boundedBy>
            <gml:MultiGeometry>
              <gml:geometryMembers>
                <gml:LineString>
                  <gml:posList>37.592233 55.733541 37.592108 55.733581</gml:posList>
                </gml:LineString>
              </gml:geometryMembers>
            </gml:MultiGeometry>
          </ymaps:GeoObject>
          <ymaps:GeoObject>
            <gml:metaDataProperty>
              <r:RouterSegmentMetaData>
                <r:street>Большой Чудов переулок</r:street>
                <r:action>right</r:action>
                <r:Action>
                  <r:value>right</r:value>
                  <r:text>направо</r:text>
                </r:Action>
                <r:angle>-86.97</r:angle>
                <r:length>62.85</r:length>
                <r:humanLength unit="meters">62.00</r:humanLength>
                <r:Length>
                  <r:value>62.85</r:value>
                  <r:text>62 м</r:text>
                </r:Length>
                <r:time>52.57</r:time>
                <r:Time>
                  <r:value>52.57</r:value>
                  <r:text>1 мин</r:text>
                </r:Time>
                <r:jamsTime>118.81</r:jamsTime>
                <r:JamsTime>
                  <r:value>118.81</r:value>
                  <r:text>2 мин</r:text>
                </r:JamsTime>
                <r:text>Направо, Большой Чудов переулок</r:text>
              </r:RouterSegmentMetaData>
            </gml:metaDataProperty>
            <gml:boundedBy>
              <gml:Envelope>
                <gml:lowerCorner>37.592108 55.733581</gml:lowerCorner>
                <gml:upperCorner>37.592801 55.733898</gml:upperCorner>
              </gml:Envelope>
            </gml:boundedBy>
            <gml:MultiGeometry>
              <gml:geometryMembers>
                <gml:LineString>
                  <gml:posList>37.592108 55.733581 37.592325 55.733825 37.592362 55.733848 37.592412 55.733869 37.592477 55.733884 37.592548 55.733894 37.592625 55.733898 37.592715 55.733897 37.592789 55.733890 37.592801 55.733889</gml:posList>
                </gml:LineString>
              </gml:geometryMembers>
            </gml:MultiGeometry>
          </ymaps:GeoObject>
        </gml:featureMembers>
      </ymaps:GeoObjectCollection>
      <ymaps:GeoObject>
        <gml:Point>
          <gml:pos>37.592780 55.733841</gml:pos>
        </gml:Point>
      </ymaps:GeoObject>
    </gml:featureMembers>
  </ymaps:GeoObjectCollection>
</ymaps:ymaps>'''
bad_map_response = '''<?xml version="1.0" encoding="utf-8"?>
<ymaps:ymaps xmlns:ymaps="http://maps.yandex.ru/ymaps/1.x" xmlns:gml="http://www.opengis.net/gml" xmlns:r="http://maps.yandex.ru/router/1.x" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maps.yandex.ru/router/1.x http://maps.yandex.ru/schemas/router/1.x/router.xsd">
  <ymaps:GeoObjectCollection>
    <gml:boundedBy>
      <gml:Envelope>
        <gml:lowerCorner>37.592780 55.733841</gml:lowerCorner>
        <gml:upperCorner>100.589401 55.734071</gml:upperCorner>
      </gml:Envelope>
    </gml:boundedBy>
    <gml:featureMembers>
      <ymaps:GeoObject>
        <gml:Point>
          <gml:pos>100.589401 55.734071</gml:pos>
        </gml:Point>
      </ymaps:GeoObject>
      <ymaps:GeoObject>
        <gml:Point>
          <gml:pos>37.592780 55.733841</gml:pos>
        </gml:Point>
      </ymaps:GeoObject>
    </gml:featureMembers>
  </ymaps:GeoObjectCollection>
</ymaps:ymaps>
'''
good_geocode_response = '''GeoObjectCollection><metaDataProperty><GeocoderResponseMetaData><request>Тверская 6
</request><found>100</found><results>10</results><InternalResponseInfo><accuracy>1</accuracy><mode>geocode
</mode><version>0.26.5-new-geosrc</version><boundedBy><Envelope><lowerCorner>37.603118 55.755601</lowerCorner>
<upperCorner>37.619576 55.764880</upperCorner></Envelope></boundedBy></InternalResponseInfo></GeocoderResponseMetaData>
</metaDataProperty><featureMember><GeoObject gml:id="1"><metaDataProperty><GeocoderMetaData><kind>house</kind>
<text>Россия, Москва, Тверская улица, 6с1</text><precision>exact</precision><AddressDetails><Country>
<AddressLine>Москва, Тверская улица, 6с1</AddressLine><CountryNameCode>RU</CountryNameCode>
<CountryName>Россия</CountryName><AdministrativeArea><AdministrativeAreaName>Москва</AdministrativeAreaName>
<Locality><LocalityName>Москва</LocalityName><Thoroughfare><ThoroughfareName>Тверская улица</ThoroughfareName>
<Premise><PremiseNumber>6с1</PremiseNumber></Premise></Thoroughfare></Locality></AdministrativeArea></Country>
</AddressDetails><InternalToponymInfo><geoid>213</geoid><houses>0</houses><Point><pos>37.611347 55.760241</pos>
</Point></InternalToponymInfo></GeocoderMetaData></metaDataProperty><description>Москва, Россия</description>
<name>Тверская улица, 6с1</name><boundedBy><Envelope><lowerCorner>37.603118 55.755601</lowerCorner>
<upperCorner>37.619576 55.76488</upperCorner></Envelope></boundedBy><Point><pos>37.611347 55.760241</pos>
</Point></GeoObject></featureMember><featureMember><GeoObject gml:id="2"><metaDataProperty><GeocoderMetaData>
<kind>house</kind><text>Россия, Санкт-Петербург, Колпино, Тверская улица, 6</text><precision>exact</precision>
<AddressDetails><Country><AddressLine>Санкт-Петербург, Колпино, Тверская улица, 6</AddressLine>
<CountryNameCode>RU</CountryNameCode><CountryName>Россия</CountryName><AdministrativeArea>
<AdministrativeAreaName>Санкт-Петербург</AdministrativeAreaName><SubAdministrativeArea>
<SubAdministrativeAreaName>Колпинский район</SubAdministrativeAreaName><Locality>
<LocalityName>Колпино</LocalityName><Thoroughfare><ThoroughfareName>Тверская улица</ThoroughfareName>
<Premise><PremiseNumber>6</PremiseNumber></Premise></Thoroughfare></Locality></SubAdministrativeArea>
</AdministrativeArea></Country></AddressDetails><InternalToponymInfo>
<geoid>26081</geoid><houses>0</houses><Point>'''


class TestMapsDataDownloader:
    def test_get_data(self):
        """Тестируем логику get_time_from_maps"""

        time_min = round(198.72 / Constants.SECONDS_IN_MINUTE, 2)
        length_km = round(393.62 / Constants.METERS_IN_KM, 2)

        station_a = Station(longitude=37.592780, latitude=55.734071)
        station_b = Station(longitude=37.589401, latitude=55.734071)
        maps_data_downloader = MapsDataDownloader()

        with mock.patch('requests.get') as m_requests_get:
            m_requests_get.return_value = m_response = mock.Mock()
            m_response.text = good_map_response

            maps_data = maps_data_downloader.get_data(station_a, station_b)
            assert time_min == maps_data.time
            assert length_km == maps_data.distance

    @pytest.mark.parametrize('response', ['', '<><>', 'asdfasdf', bad_map_response])
    def test_get_data_with_error(self, response):
        """Тестируем логику get_time_from_maps с плохим ответом"""
        station_a = Station(longitude=37.592780, latitude=55.734071)
        station_b = Station(longitude=37.589401, latitude=55.734071)
        maps_data_downloader = MapsDataDownloader()

        with mock.patch('requests.get') as m_requests_get:
            m_requests_get.return_value = m_response = mock.Mock()
            m_response.text = response
            with pytest.raises(MapsDataDownloader.MapsDataDownloaderError):
                maps_data_downloader.get_data(station_a, station_b)


class TestGeoCoderDataDownloader:
    def test_get_country_code(self):
        """Тестируем логику get_country_code"""
        station = Station(longitude=37.592780, latitude=55.734071)
        geo_coder = GeoCoderDataDownloader()
        with mock.patch('requests.get') as m_requests_get:
            m_requests_get.return_value = m_response = mock.Mock()
            m_response.text = good_geocode_response

            country_code = geo_coder.get_country_code(station=station)

            assert country_code == 'RU'

    def test_get_country_code_with_bad_response(self):
        """Тестируем логику get_country_code с плохим ответом ГеоКодера"""
        station = Station(longitude=37.592780, latitude=55.734071)
        geo_coder = GeoCoderDataDownloader()
        with mock.patch('requests.get') as m_requests_get:
            m_requests_get.return_value = m_response = mock.Mock()
            m_response.text = bad_map_response

            country_code = geo_coder.get_country_code(station=station)

            assert country_code is None
