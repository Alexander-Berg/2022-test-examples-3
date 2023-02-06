#!/usr/bin/env python
# -*- coding: UTF-8 -*-
import os
import pytest
import six
import xml.etree.ElementTree as ET
import yatest

from market.idx.yatf.resources.shops_outlet import ShopsOutlet


@pytest.fixture
def resource():
    telephone = ET.Element('Telephone')
    ET.SubElement(telephone, 'CountryCode').text = '2353'
    ET.SubElement(telephone, 'CityCode').text = '32131231'
    ET.SubElement(telephone, 'TelephoneNumber').text = '132-321312312312'
    ET.SubElement(telephone, 'TelephoneType').text = 'PHONE'

    worktime = ET.Element('WorkingTime')
    ET.SubElement(worktime, 'WorkingDaysFrom').text = '1'
    ET.SubElement(worktime, 'WorkingDaysTill').text = '7'
    ET.SubElement(worktime, 'WorkingHoursFrom').text = '8:00'
    ET.SubElement(worktime, 'WorkingHoursTill').text = '18:00'

    outlet = ET.Element('outlet')
    ET.SubElement(outlet, 'DeliveryServicePointId').text = 'MSK-2042'
    ET.SubElement(outlet, 'PointId').text = '123'
    ET.SubElement(outlet, 'ShopPointId').text = '123'
    ET.SubElement(outlet, 'PointName').text = six.u('Почтамат INSPOST 1')
    ET.SubElement(outlet, 'Type').text = 'depot'
    ET.SubElement(outlet, 'LocalityName').text = six.u('Санкт-Петербург')
    ET.SubElement(outlet, 'ThoroughfareName').text = six.u('Невский проспект')
    ET.SubElement(outlet, 'PremiseNumber').text = '68'
    ET.SubElement(outlet, 'GpsCoord').text = '30.345045,59.933347'
    ET.SubElement(outlet, 'RegionId').text = '2'
    outlet.append(telephone)
    outlet.append(worktime)

    delivery_service = ET.Element('delivery-service', attrib={'id': '103'})
    delivery_service.append(outlet)

    shop_info_outlet = ET.Element('outlet')
    ET.SubElement(outlet, 'PointId').text = '288899'
    ET.SubElement(outlet, 'ShopPointId').text = '288899'
    ET.SubElement(outlet, 'PointName').text = six.u('Кельвин')
    ET.SubElement(outlet, 'Type').text = 'retail'
    ET.SubElement(outlet, 'IsMain').text = 'true'
    ET.SubElement(outlet, 'LocalityName').text = six.u('Санкт-Петербург')
    ET.SubElement(outlet, 'ThoroughfareName').text = six.u('Невский проспект')
    ET.SubElement(outlet, 'PremiseNumber').text = '68'
    ET.SubElement(outlet, 'AddressAdd').text = six.u('Вход с 13-й линии')
    ET.SubElement(outlet, 'GpsCoord').text = '30.345045,59.933347'
    ET.SubElement(outlet, 'RegionId').text = '2'
    ET.SubElement(outlet, 'Email').text = 'winkel@kelwin.ru'
    shop_info_outlet.append(telephone)
    shop_info_outlet.append(worktime)

    shop_delivery_service_ref = ET.Element('outlet', attrib={
        'delivery-service-ref': '103',
        'delivery-service-outlet-ref': '288899'
    })

    shop_delivery_service = ET.Element('shop-delivery-service', attrib={
        'id': '103'
    })

    shop_info_outlethops = ET.Element('Shop', attrib={'id': '131069'})
    shop_info_outlethops.append(shop_info_outlet)
    shop_info_outlethops.append(shop_delivery_service_ref)
    shop_info_outlethops.append(shop_delivery_service)

    outlet_info = ET.Element('OutletInfo')
    delivery_services = ET.SubElement(outlet_info, 'delivery-services')
    delivery_services.append(delivery_service)
    shops = ET.SubElement(outlet_info, 'shops')
    shops.append(shop_info_outlethops)

    return ShopsOutlet(outlet_info)


def test_shops_outlet(resource):
    resource.write(yatest.common.test_output_path())
    assert os.path.exists(resource.path)
    assert resource.path.endswith(resource.filename)
