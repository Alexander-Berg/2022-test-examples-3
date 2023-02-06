# -*- coding: utf-8 -*-

import os
import shutil
import unittest

from getter import geobaselib
import StringIO


class Test(unittest.TestCase):

    def test_istree_ok(self):
        '''
             1
            /  \
           2    3
          /
         4
        '''
        c2p = {
            4: 2,
            2: 1,
            3: 1,
        }
        self.assertTrue(geobaselib.findroot(c2p) is not None)

    def test_istree_fail(self):
        r'''
             1        4
            /  \     /
           2    3   5
        '''
        c2p = {
            2: 1,
            3: 1,
            5: 4,
        }
        self.assertRaises(geobaselib.Error, geobaselib.findroot, c2p)


class TestAll(unittest.TestCase):
    rootdir = 'tmp'
    geobase_xml = '''
<regions>
  <region id="10000" parent="0" name="ЗЕМЛЯ" en_name="Earth" syn="" type="0" tz_offset="0" />
  <region id="10001" parent="10000" name="Евразия" en_name="" syn="" type="1" tz_offset="0" />
  <region id="225" parent="10001" name="Россия" en_name="Russia" syn="" type="3" tz_offset="0" /> <!-- en name check -->
  <region id="3" parent="225" name="Центр" syn="" en_name="" type="4" tz_offset="10800" />
  <region id="1" parent="3" name="Москва и Московская область" en_name="" syn="" type="5" tz_offset="10800" />
  <region id="213" parent="1" name="Москва" en_name="" syn="" type="6" chief_region="1" tz_offset="10800" />
  <region id="2" parent="1" name="Петербург" en_name="" syn="" type="6" chief_region="1" tz_offset="10800" />
  <region id="163" parent="1" name="Астана" en_name="" syn="" type="6" chief_region="1" tz_offset="21600" />
  <region id="139" parent="3" name="Люберецкий район" en_name="" syn="" type="-1" tz_offset="10800" />
  <region id="215" parent="3" name="Люберцы" en_name="" syn="" type="6" chief_region="139" tz_offset="10800" />
  <region id="29385" parent="10001" name="Тайвань" en_name="" syn="Тайвань,Duplicate,Россия" type="3" chief_region="10592" tz_offset="28800"/> <!-- dedup by syn check -->
  <region id="322" parent="1" name="Объединенные Арабские Эмираты" en_name="" syn="ОАЭ, АУЕ, الإمارات العربية المتحدة" type="3" chief_region="10592" tz_offset="28800"/> <!-- synonyms and utf8 check-->
  <region id="10002" parent="10001" name="Duplicate" en_name="" syn="" type="3" tz_offset="0" /> <!-- duplicates check -->
  <region id="10003" parent="10001" name="Duplicate" en_name="" syn="" type="3" tz_offset="0" /> <!-- duplicates check -->
  <region id="10004" parent="10001" name="КНР" en_name="" syn="" type="3" tz_offset="0" /> <!-- duplicates in additional check -->
  <region id="10005" parent="10001" name="Китай" en_name="" syn="Гонконг" type="3" tz_offset="0" /> <!-- syn duplicates in additional check -->
  <region id="21564" parent="10002" name="Гваделупа" en_name="Guadeloupe" syn="" type="12" tz_offset="-14400" />
</regions>
'''

    expected_countries = '''Россия\t225
Russia\t225
Тайвань\t29385
Объединенные Арабские Эмираты\t322
ОАЭ\t322
АУЕ\t322
Китай\t10005
الإمارات العربية المتحدة\t322
Гваделупа\t21564
Guadeloupe\t21564'''

    @property
    def geobase_path(self):
        return os.path.join(self.rootdir, 'geobase_original.xml')

    def setUp(self):
        shutil.rmtree(self.rootdir, ignore_errors=True)
        os.makedirs(self.rootdir)
        with open(self.geobase_path, 'w') as fobj:
            fobj.write(self.geobase_xml)

    def tearDown(self):
        shutil.rmtree(self.rootdir, ignore_errors=True)

    def test_countries(self):
        geobaselib.gen_extra_files(self.geobase_path, self.rootdir)
        with open(os.path.join(self.rootdir, 'countries_utf8.c2n'), 'r') as f:
            content = set(f.read().splitlines())
            self.assertSetEqual(content, set(self.expected_countries.splitlines()))

    def test_additional_countries(self):
        geobaselib.gen_extra_files(self.geobase_path, self.rootdir)
        with open(os.path.join(self.rootdir, 'additional_countries_utf8.c2n'), 'r') as f:
            content = f.read()
            self.assertFalse('Тайвань' in content)    # проверяем, что Тайвань не попал в файл
            self.assertFalse('Сальвадор' in content)  # и Сальвадор тоже

    def test_removed_wrong_chief_region(self):
        output = StringIO.StringIO()
        geobaselib.geobase_convert_to_flat_view(self.geobase_path, output)
        content = output.getvalue()
        output.close()

        # output good region
        self.assertIn('id="215"', content)
        # do not output problem region
        self.assertNotIn('id="139"', content)
        # do output attrib chief_region = good region
        self.assertIn('chief_region="1"', content)
        # do not output attrib chief_region = problem region
        self.assertNotIn('chief_region="139"', content)


if __name__ == '__main__':
    unittest.main()
