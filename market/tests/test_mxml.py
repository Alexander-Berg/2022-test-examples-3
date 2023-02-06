# -*- coding: utf-8 -*-
import six
import unittest

import market.idx.pylibrary.mxml.mxml as mxml


def u(string):
    return six.ensure_text(string)


MBI_PARAMS_101 = """<?xml version="1.0" encoding="utf-8"?>
<map>
  <is_offline>true</is_offline>
  <shop_id>77232</shop_id>
  <shop_grades_count>79</shop_grades_count>
  <home_region>225</home_region>
  <show_premium>true</show_premium>
  <local_delivery_cost>35000</local_delivery_cost>
  <is_online>true</is_online>
  <tariff>CLICKS</tariff>
  <shipping_full_text>∎ доставка за пределы МКАД -кол-во км. от МКАД * 20 руб. + 350 руб.</shipping_full_text>
  <cpa>NO</cpa>
  <url>http://robot4home.ru/index.php?option=com_vmymlexport&amp;view=yml&amp;no_html=1</url>
  <shop_cluster_id>69605112</shop_cluster_id>
  <priority_regions>213</priority_regions>
  <is_enabled>true</is_enabled>
  <delivery_src>WEB</delivery_src>
  <use_open_stat>true</use_open_stat>
  <shop_currency>RUR</shop_currency>
  <datafeed_id>101</datafeed_id>
  <phone>+7 495 799-53-32</phone>
  <regions>213;</regions>
  <shopname>Роботвдом.рф</shopname>
  <price_scheme>10;9=5;</price_scheme>
  <urlforlog>robot4home.ru</urlforlog>
  <quality_rating>5</quality_rating>
  <delivery_services>2=1;99=1;</delivery_services>
</map>
"""

MBI_PARAMS_102 = """<?xml version="1.0" encoding="utf-8"?>
<map>
  <is_offline>true</is_offline>
  <autobroker_enabled>true</autobroker_enabled>
  <shop_id>50411</shop_id>
  <shop_grades_count>161</shop_grades_count>
  <home_region>225</home_region>
  <show_premium>true</show_premium>
  <local_delivery_cost>30000</local_delivery_cost>
  <is_online>true</is_online>
  <tariff>CLICKS</tariff>
  <shipping_full_text>- на МКАД и ближайшие районы - 500 руб.&lt;br&gt;- Подмосковье от 600 руб.</shipping_full_text>
  <cpa>NO</cpa>
  <url>http://www.99tonn.ru/ya.xml/</url>
  <priority_regions>213</priority_regions>
  <is_enabled>true</is_enabled>
  <delivery_src>WEB</delivery_src>
  <use_open_stat>true</use_open_stat>
  <shop_currency>RUR</shop_currency>
  <datafeed_id>102</datafeed_id>
  <phone>+7 (495) 2335601</phone>
  <regions>225;213;</regions>
  <shopname>99tonn</shopname>
  <price_scheme>10;9=5;</price_scheme>
  <urlforlog>99tonn.ru</urlforlog>
  <quality_rating>5</quality_rating>
  <delivery_services>2=1;11=1;6=1;4=1;</delivery_services>
</map>
"""

INVALID_XML = """<?xml version="1.0" encoding="utf-8"?>
<map>
    <220-Please-visit-http>sourceforgenetprojectsfilezilla</220-Please-visit-http>
    <Status>226</Status>
</map>"""


class TestXmlUtil(unittest.TestCase):
    def test_xmlutil_rus(self):
        '''sax parser recognize encoding="utf-8", and do not recognize "utf8"
        '''
        d = mxml.xml2map(MBI_PARAMS_101)
        self.assertEqual(u('true'), d['is_offline'])
        self.assertEqual(u('∎ доставка за пределы МКАД -кол-во км. от МКАД * 20 руб. + 350 руб.'),
                         d['shipping_full_text'])
        self.assertEqual(u('Роботвдом.рф'), d['shopname'])

    def test_xmlutil_rus_braces(self):
        d = mxml.xml2map(MBI_PARAMS_102)
        self.assertEqual(u('225'), d['home_region'])
        self.assertEqual(u('- на МКАД и ближайшие районы - 500 руб.<br>- Подмосковье от 600 руб.'),
                         d['shipping_full_text'])

    def test_map_to_xml(self):
        result = u('<?xml version="1.0" encoding="utf-8"?>\n<map><10>20</10><tag>value</tag><рус_тэг>рус_валью</рус_тэг></map>')
        self.assertEqual(result, mxml.map2xml({'tag': 'value', 'рус_тэг': 'рус_валью', 10: 20}).decode('utf-8'))

    def test_invalid_xml(self):
        d = mxml.xml2map(INVALID_XML)
        self.assertEqual({}, d)


if '__main__' == __name__:
    unittest.main()
