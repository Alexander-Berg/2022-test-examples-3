# -*- coding: utf-8 -*-

import pytest

from cStringIO import StringIO

from cysix.base import safe_parse_xml
from cysix.filters.cysix_xml_validation import (
    CysixCheckError,
    validate_xml_against_xsd
)

cysix_data = """\
<?xml version='1.0' encoding='utf8'?>
<channel t_type="bus" carrier_code_system="local" version="1.0" station_code_system="vendor" timezone="start_station" vehicle_code_system="local">
  <group code="group1">
    <stations>
      <station code="1" title="Станция 1">
      </station>
      <station code="2" title="Станция 2">
      </station>
    </stations>
    <vehicles>
      <vehicle code="2" title="ПАЗ-3205"/>
    </vehicles>
    <carriers>
      <carrier code="3" title="Carrier-title"/>
    </carriers>
    <threads>
      <thread title="t-title" number="t-number" carrier_code="3" vehicle_code="2" is_transit="1">
        <stoppoints>
          <stoppoint station_title="Станция 1" station_code="1" departure_time="13:00:00"/>
          <stoppoint station_title="Станция 2" station_code="2" arrival_time="14:10:00"/>
        </stoppoints>
        <schedules>
          <schedule period_start_date="2013-03-29" period_end_date="2015-03-29" days="135" times="13:00:00"/>
        </schedules>
      </thread>
     </threads>
  </group>
</channel>
"""

def test_is_transit_ignore():
    xml_tree = safe_parse_xml(StringIO(cysix_data))

    try:
        validate_xml_against_xsd(xml_tree)
    except CysixCheckError:
        pytest.fail('thread with is_transit should validate')
