# coding: utf-8

import six

import market.pylibrary.delivery_tariffs as delivery_tariffs  # noqa


source_courier = u"""\
<tariff id="6313" name="БЕРУ - Магазин" label="БЕРУ - Магазин" type="COURIER" currency="RUB" carrier-id="227" carrier-name="VestovoySPB" delivery-method="COURIER" is-for-customer="0" is-for-shop="1">
 <parameters>
  <m3weight>10</m3weight>
  <programs>
   <program name-key="MARKET_DELIVERY"/>
  </programs>
 </parameters>
 <rules>
  <offer-rule weight-max="30000" width-max="100" height-max="100" length-max="100" dim-sum-max="180">
   <location-rule from="213">
    <location-rule to="2">
     <offer-rule weight-max="5000">
      <option cost="203" delta-cost="0" days-min="1" days-max="1" scale="1000"/>
     </offer-rule>
     <offer-rule weight-min="5000" weight-max="10000">
      <option cost="233" delta-cost="0" days-min="1" days-max="1" scale="1000"/>
     </offer-rule>
     <offer-rule weight-min="10000" weight-max="30000">
      <option cost="233" delta-cost="20" days-min="1" days-max="1" scale="1000"/>
     </offer-rule>
    </location-rule>
   </location-rule>
  </offer-rule>
 </rules>
</tariff>
"""

source_pickup = u"""\
<tariff id="5996" name="Маркет - Магазин" label="Маркет - Магазин" type="PO_BOX" currency="RUB" carrier-id="198" carrier-name="Маркет ПВЗ" delivery-method="PICKUP" is-for-customer="0" is-for-shop="1">
 <parameters>
  <m3weight>200</m3weight>
  <programs>
   <program name-key="MARKET_DELIVERY"/>
  </programs>
 </parameters>
 <rules>
  <offer-rule weight-max="20000" width-max="180" height-max="180" length-max="180" dim-sum-max="400">
   <location-rule from="213">
    <location-rule to="37118">
     <offer-rule weight-max="20000">
      <option cost="0" delta-cost="0" days-min="1" days-max="1" scale="1000"/>
     </offer-rule>
    </location-rule>
    <location-rule to="2">
     <offer-rule weight-max="20000">
      <option cost="0" delta-cost="0" days-min="1" days-max="1" scale="1000"/>
     </offer-rule>
     <offer-rule weight-max="20000" width-max="180" height-max="180" length-max="180" dim-sum-max="400">
      <pickuppoint id="2303516" code="2303516"></pickuppoint>
      <pickuppoint id="2303517" code="2303517"></pickuppoint>
     </offer-rule>
    </location-rule>
   </location-rule>
  </offer-rule>
 </rules>
</tariff>
"""


def test_courier():
    tariff = delivery_tariffs.load_one(six.BytesIO(source_courier.encode('utf8')))

    assert tariff.find_rule(213, 2, 4000).cost == 203
    assert tariff.find_rule(213, 2, 5000).cost == 203
    assert tariff.find_rule(213, 2, 25000).cost == 233
    assert bool(tariff.find_rule(213, 2, 40000)) is False


def test_pickup():
    tariff = delivery_tariffs.load_one(six.BytesIO(source_pickup.encode('utf8')))
    weight, size = 1000, (10, 10, 10)

    rule = tariff.find_rule(213, 37118, weight, size)
    assert bool(rule) is False

    rule = tariff.find_rule(213, 2, weight, size)
    assert rule.cost == 0
    assert rule.points == [2303516, 2303517]


def test_many():
    tariff1 = delivery_tariffs.load_one(six.BytesIO(source_courier.encode('utf8')))
    tariff2 = delivery_tariffs.load_one(six.BytesIO(source_pickup.encode('utf8')))
    tariffs = delivery_tariffs.Tariffs([tariff1, tariff2])

    weight, size = 1000, (10, 10, 10)
    result = tariffs.find_rules(213, 2, weight, size)
    assert len(result.rules) == 2
    assert result.courier_rules[0].cost == 203
    assert result.pickup_rules[0].cost == 0

    result = tariffs.find_rules(213, 2, weight, (200, 200, 200))
    assert len(result.rules) == 0
    assert len(result.bads) == 2
    assert result.bads[0] is delivery_tariffs.RULE_TOO_LARGE
    assert result.bads[1] is delivery_tariffs.RULE_TOO_LARGE
