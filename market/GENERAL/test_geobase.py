# coding: utf-8

import six

import market.pylibrary.geobaselib as geobaselib


source = u"""\
<regions>
<region id="10000" parent="0" name="Земля" type="0"/>
<region id="10001" parent="10000" name="Евразия" type="1"/>
<region id="225" parent="10001" name="Россия" type="3"/>
<region id="3" parent="225" name="Центральный федеральный округ" type="4"/>
<region id="1" parent="3" name="Москва и Московская область" type="5"/>
<region id="213" parent="1" name="Москва" type="6"/>
</regions>
"""


def test():
    geobase = geobaselib.load(six.BytesIO(source.encode('utf8')))

    # iter
    regions = [region for region in geobase]
    assert len(regions) == 6

    # is_city
    assert geobase.find_region(213).is_city
    assert geobase.find_region(225).is_country

    # find_region
    assert geobase.find_region(213).name == u'Москва'

    # find_path
    path = geobase.find_path(213)
    assert path[0].name == u'Москва'
    assert path[-1].name == u'Земля'

    # find_country
    assert geobase.find_country(213).name == u'Россия'
