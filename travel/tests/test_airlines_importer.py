# coding=utf-8
from __future__ import unicode_literals

import six

from travel.avia.shared_flights.tasks.sirena_parser.airlines_importer import AirlinesImporter

input_string = '''<?xml version="1.0" encoding="UTF-8"?>
<sirena>
  <answer pult="ЯНРСП1" msgid="1" time="08:58:07 20.12.2019" instance="ГРУ">
    <describe data="aircompany">
      <data>
        <code xml:lang="ru">ЙН</code>
        <code xml:lang="en">JH</code>
        <name xml:lang="ru">ТОО ЙОНГЕ ЭЙРЛАЙНС</name>
        <name xml:lang="en">IONGE RUSSIA LTD</name>
        <account-code>777</account-code>
        <latin_code_in_ref>true</latin_code_in_ref>
        <latin_registration>false</latin_registration>
        <no_raid_info>false</no_raid_info>
      </data>
    </describe>
  </answer>
</sirena>
'''


class TestAirlinesImporter:

    def test_parse_airlines(self):
        importer = AirlinesImporter()
        airlines = importer.parse(six.ensure_str(input_string))

        assert len(airlines) == 1
        assert airlines[0].SirenaCode == six.ensure_str('ЙН')
        assert airlines[0].IataCode == 'JH'
        assert airlines[0].Title == six.ensure_str('ТОО ЙОНГЕ ЭЙРЛАЙНС')
        assert airlines[0].TitleEn == 'IONGE RUSSIA LTD'
        assert airlines[0].AccountCode == '777'
