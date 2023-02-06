package tarifficator

import (
	"bytes"
	"fmt"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/deliverydelay"
	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
)

const testCourierXML = `
<tariff
	id="6313"
	name="БЕРУ - Магазин"
	label="БЕРУ - Магазин"
	type="COURIER"
	currency="RUB"
	carrier-id="227"
	carrier-name="VestovoySPB"
	delivery-method="COURIER"
	is-for-customer="0"
	is-for-shop="1"
>
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
      <option cost="233" delta-cost="1" days-min="1" days-max="2" scale="1000"/>
     </offer-rule>
     <offer-rule weight-min="10000" weight-max="30000">
      <option cost="233" delta-cost="20" days-min="2" days-max="2" scale="1000"/>
     </offer-rule>
    </location-rule>
   </location-rule>
  </offer-rule>
 </rules>
</tariff>
`

const testPickupXML = `
<?xml version="1.0" encoding="UTF-8"?>
<tariff
	id="7025"
	name="БЕРУ - Магазин"
	label="БЕРУ - Магазин"
	type="PO_BOX"
	currency="RUB"
	carrier-id="1004957"
	carrier-name="Вестовой Постаматы"
	delivery-method="PICKUP"
	is-for-customer="0"
	is-for-shop="1"
>
 <parameters>
  <m3weight>0</m3weight>
  <programs>
   <program name-key="MARKET_DELIVERY"/>
  </programs>
 </parameters>
 <rules>
  <offer-rule weight-max="20000" width-max="42" height-max="40" length-max="45" dim-sum-max="180">
   <location-rule from="213">
    <location-rule to="213">
     <offer-rule weight-max="4000">
      <option cost="170" delta-cost="0" days-min="0" days-max="0" scale="1000"/>
     </offer-rule>
     <offer-rule weight-min="4000" weight-max="9000">
      <option cost="195" delta-cost="0" days-min="0" days-max="0" scale="1000"/>
     </offer-rule>
     <offer-rule weight-min="9000" weight-max="20000">
      <option cost="195" delta-cost="20" days-min="0" days-max="1" scale="1000"/>
     </offer-rule>
     <offer-rule weight-max="20000" width-max="454" height-max="405" length-max="425" dim-sum-max="1284">
      <pickuppoint id="2899511" code="1004"></pickuppoint>
      <pickuppoint id="2899514" code="1013"></pickuppoint>
     </offer-rule>
     <offer-rule weight-max="5000" width-max="425" height-max="405" length-max="425" dim-sum-max="1284">
      <pickuppoint id="2899515" code="1015"></pickuppoint>
     </offer-rule>
     <offer-rule weight-max="20000" width-max="425" height-max="405" length-max="425" dim-sum-max="1284">
      <pickuppoint id="2899517" code="1018"></pickuppoint>
     </offer-rule>
    </location-rule>
   </location-rule>
  </offer-rule>
 </rules>
</tariff>
`

const badXML = `
<some-random-xml>
	<body>
		<element/>
	</body>
</some-random-xml>
`

func TestReadHeader(t *testing.T) {
	tests := []string{
		testCourierXML,
		testPickupXML,
	}
	for _, testXML := range tests {
		buf := bytes.NewBufferString(testXML)
		header, err := ReadHeader(buf)
		if err != nil {
			t.Fail()
			return
		}
		fmt.Printf(
			"%d %s %s\n",
			header.ID,
			header.Type,
			strings.Join(header.Programs, ","),
		)
	}
}

func TestReadBadHeaderXml(t *testing.T) {
	_, err := ReadHeader(bytes.NewBufferString(badXML))
	if err == nil {
		t.Fail()
		return
	}
}

type SimpleCase struct {
	src    geobase.RegionID
	dst    geobase.RegionID
	weight uint32
}

func TestFindOkCourier(t *testing.T) {
	buf := bytes.NewBufferString(testCourierXML)
	tariff, err := Read(buf)
	assert.NotNil(t, tariff)
	assert.NoError(t, err)

	// VGH
	assert.Equal(t, 100, int(tariff.RuleAttrs.WidthMax))
	assert.Equal(t, 100, int(tariff.RuleAttrs.HeightMax))
	assert.Equal(t, 100, int(tariff.RuleAttrs.LengthMax))
	assert.Equal(t, 180, int(tariff.RuleAttrs.DimSumMax))
	assert.Equal(t, [3]uint32{100, 100, 100}, tariff.RuleAttrs.SortedDimLimits)
	for _, rules := range tariff.Rules {
		for _, rule := range rules {
			assert.Equal(t, 0, int(rule.WidthMax))
			assert.Equal(t, 0, int(rule.HeightMax))
			assert.Equal(t, 0, int(rule.LengthMax))
			assert.Equal(t, 0, int(rule.DimSumMax))
			assert.Equal(t, [3]uint32{0, 0, 0}, rule.SortedDimLimits)
		}
	}

	tests := []struct {
		SimpleCase
		wantCost    uint32
		wantDaysMin uint32
		wantDaysMax uint32
	}{
		{
			SimpleCase: SimpleCase{
				src:    213,
				dst:    2,
				weight: 4000,
			},
			wantCost:    203,
			wantDaysMax: 1,
			wantDaysMin: 1,
		}, {
			SimpleCase: SimpleCase{
				src:    213,
				dst:    2,
				weight: 5000,
			},
			wantCost:    203,
			wantDaysMax: 1,
			wantDaysMin: 1,
		}, {
			SimpleCase: SimpleCase{
				src:    213,
				dst:    2,
				weight: 25000,
			},
			wantCost:    233 + 20*(15-1),
			wantDaysMax: 2,
			wantDaysMin: 2,
		},
	}
	for _, tt := range tests {
		result, err := tariff.FindRule(tt.src, tt.dst, tt.weight, nil)
		assert.Equal(t, err, nil)
		assert.Equal(t, result.TotalCost, tt.wantCost)
		assert.Equal(t, result.DaysMin, tt.wantDaysMin)
		assert.Equal(t, result.DaysMax, tt.wantDaysMax)
		assert.Equal(t, result.DeliveryMethod, "COURIER")
	}
}

func TestFindOkPickup(t *testing.T) {
	buf := bytes.NewBufferString(testPickupXML)
	tariff, err := Read(buf)
	assert.NotNil(t, tariff)
	assert.NoError(t, err)

	tests := []struct {
		SimpleCase
		wantCost    uint32
		wantDaysMin uint32
		wantDaysMax uint32
		wantPoints  []Pickuppoint
	}{
		{
			SimpleCase: SimpleCase{
				src:    213,
				dst:    213,
				weight: 4000,
			},
			wantCost:    170,
			wantDaysMax: 0,
			wantDaysMin: 0,
			wantPoints: []Pickuppoint{
				{
					ID: 2899511,
				},
				{
					ID: 2899514,
				},
				{
					ID: 2899515,
				},
				{
					ID: 2899517,
				},
			},
		}, {
			SimpleCase: SimpleCase{
				src:    213,
				dst:    213,
				weight: 7000,
			},
			wantCost:    195,
			wantDaysMax: 0,
			wantDaysMin: 0,
			wantPoints: []Pickuppoint{
				{
					ID: 2899511,
				},
				{
					ID: 2899514,
				},
				{
					ID: 2899517,
				},
			},
		}, {
			SimpleCase: SimpleCase{
				src:    213,
				dst:    213,
				weight: 13000,
			},
			wantCost:    195 + 20*(4-1),
			wantDaysMax: 1,
			wantDaysMin: 0,
			wantPoints: []Pickuppoint{
				{
					ID: 2899511,
				},
				{
					ID: 2899514,
				},
				{
					ID: 2899517,
				},
			},
		},
	}
	for _, tt := range tests {
		result, err := tariff.FindRule(tt.src, tt.dst, tt.weight, nil)
		assert.Equal(t, err, nil)
		assert.Equal(t, result.TotalCost, tt.wantCost)
		assert.Equal(t, result.DaysMin, tt.wantDaysMin)
		assert.Equal(t, result.DaysMax, tt.wantDaysMax)
		assert.ElementsMatch(t, result.Points, tt.wantPoints)
		assert.Equal(t, result.DeliveryMethod, "PICKUP")
	}
}

// On courier XML because at this point all XML's are the same
func TestFindErrors(t *testing.T) {
	buf := bytes.NewBufferString(testCourierXML)
	tariff, err := Read(buf)
	assert.NotNil(t, tariff)
	assert.NoError(t, err)

	testsTooHeavy := []SimpleCase{
		{
			src:    213,
			dst:    2,
			weight: 40000,
		},
	}
	for _, tt := range testsTooHeavy {
		_, err := tariff.FindRule(tt.src, tt.dst, tt.weight, nil)
		assert.Equal(t, ErrRuleTooHeavy, err)
	}

	testsNoRegion := []SimpleCase{
		{
			src:    213,
			dst:    3,
			weight: 20000,
		},
	}
	for _, tt := range testsNoRegion {
		_, err := tariff.FindRule(tt.src, tt.dst, tt.weight, nil)
		assert.Equal(t, ErrRuleNoRegion, err)
	}

	testsTooLarge := []struct {
		SimpleCase
		DeliverySize
	}{
		{ // Width > 100
			SimpleCase: SimpleCase{
				src:    213,
				dst:    2,
				weight: 20000,
			},
			DeliverySize: DeliverySize{
				Width:  101,
				Height: 10,
				Length: 10,
			},
		}, { // Height > 100
			SimpleCase: SimpleCase{
				src:    213,
				dst:    2,
				weight: 20000,
			},
			DeliverySize: DeliverySize{
				Width:  10,
				Height: 101,
				Length: 10,
			},
		}, { // Length > 100
			SimpleCase: SimpleCase{
				src:    213,
				dst:    2,
				weight: 20000,
			},
			DeliverySize: DeliverySize{
				Width:  10,
				Height: 10,
				Length: 101,
			},
		}, { // sumDim > 180
			SimpleCase: SimpleCase{
				src:    213,
				dst:    2,
				weight: 20000,
			},
			DeliverySize: DeliverySize{
				Width:  90,
				Height: 90,
				Length: 90,
			},
		},
	}
	for _, tt := range testsTooLarge {
		_, err := tariff.FindRule(tt.src, tt.dst, tt.weight, &tt.DeliverySize)
		assert.Equal(t, ErrRuleTooLarge, err)
	}
}

func TestFindOptions(t *testing.T) {
	regionKey := FromToRegions{From: 213, To: 213}
	t0 := TariffRT{
		ID:            1,
		FromToRegions: regionKey,
		RuleAttrs: RuleAttrs{
			WeightMax:       32222,
			HeightMax:       50,
			LengthMax:       40,
			WidthMax:        30,
			DimSumMax:       100,
			SortedDimLimits: [3]uint32{30, 40, 50},
		},
		Type: RuleTypeGlobal,
	}
	t1 := TariffRT{
		ID:            1,
		FromToRegions: regionKey,
		Option: Option{
			Cost:    42,
			DaysMin: 1,
			DaysMax: 2,
		},
		Type: RuleTypePayment,
	}
	t2 := TariffRT{
		ID:            1,
		FromToRegions: regionKey,
		Points: []Point{
			Point{ID: 1},
		},
		Type: RuleTypeForPoint,
	}
	t3 := TariffRT{
		ID:            1,
		FromToRegions: regionKey,
		Points: []Point{
			Point{ID: 22},
			Point{ID: 33},
			Point{ID: 44},
		},
		Type: RuleTypeForPoint,
	}
	regionMap := geobase.NewExample()
	var wd WeightAndDim
	{
		tariffsFinder := NewTariffsFinder()
		tariffsFinder.Add(&t2)
		tariffsFinder.Add(&t3)
		tariffsFinder.Add(&t1)
		tariffsFinder.Add(&t0)
		tariffsFinder.Finish(&regionMap)
		results := tariffsFinder.FindOptions([]WeightAndDim{wd}, regionKey, nil)
		require.Len(t, results, 1)
		assert.Equal(t, uint32(42), results[0].Cost)
		assert.Equal(t, uint32(1), results[0].DaysMin)
		assert.Equal(t, uint32(2), results[0].DaysMax)

		points, canc := results[0].GetPoints()
		defer canc()
		assert.Len(t, points, 4)
		assert.Equal(t, int64(1), points[0])
		assert.Equal(t, int64(22), points[1])
		assert.Equal(t, int64(33), points[2])
		assert.Equal(t, int64(44), points[3])

		// check points count for key
		assert.Equal(t, 4, tariffsFinder.tariffs[regionKey].TotalPointsNum)
	}
	// empty
	{
		tariffsFinder := NewTariffsFinder()
		tariffsFinder.Add(&t2)
		tariffsFinder.Add(&t3)
		tariffsFinder.Finish(&regionMap)
		results := tariffsFinder.FindOptions([]WeightAndDim{wd}, regionKey, nil)
		assert.Len(t, results, 0)
	}
	// too heavy
	{
		wd.Weight = 32223
		tariffsFinder := NewTariffsFinder()
		tariffsFinder.Add(&t2)
		tariffsFinder.Add(&t3)
		tariffsFinder.Add(&t1)
		tariffsFinder.Add(&t0)
		tariffsFinder.Finish(&regionMap)
		results := tariffsFinder.FindOptions([]WeightAndDim{wd}, regionKey, nil)
		assert.Len(t, results, 0)
	}
	// max dim
	{
		wd = NewWeightAndDim(0, 1, 1, 60) // dims are sorted in ascending order
		tariffsFinder := NewTariffsFinder()
		tariffsFinder.Add(&t0)
		tariffsFinder.Add(&t2)
		tariffsFinder.Add(&t3)
		tariffsFinder.Add(&t1)
		tariffsFinder.Finish(&regionMap)
		results := tariffsFinder.FindOptions([]WeightAndDim{wd}, regionKey, nil)
		assert.Len(t, results, 0)
	}
	// dim sum
	{
		wd = NewWeightAndDim(0, 25, 35, 45) // dims are sorted in ascending order
		tariffsFinder := NewTariffsFinder()
		tariffsFinder.Add(&t2)
		tariffsFinder.Add(&t3)
		tariffsFinder.Add(&t1)
		tariffsFinder.Add(&t0)
		tariffsFinder.Finish(&regionMap)
		results := tariffsFinder.FindOptions([]WeightAndDim{wd}, regionKey, nil)
		assert.Len(t, results, 0)
	}
	// direct post
	{
		directPost := getDirectFlowPostDeliveryServiceID()
		tDirectPost := t1
		tDirectPost.DeliveryServiceID = directPost
		tDirectPost.DeliveryMethod = enums.DeliveryMethodPost
		{
			tariffsFinder := NewTariffsFinder()
			tariffsFinder.Add(&tDirectPost)
			tariffsFinder.Finish(&regionMap)
			results := tariffsFinder.FindOptions([]WeightAndDim{wd}, regionKey, &OptionFilters{IsReturn: false, DeliveryServiceIDs: map[uint64]bool{directPost: true}})
			assert.Len(t, results, 1)
		}
		// context.IsReturn == true
		{
			tariffsFinder := NewTariffsFinder()
			tariffsFinder.Add(&tDirectPost)
			tariffsFinder.Finish(&regionMap)
			results := tariffsFinder.FindOptions([]WeightAndDim{wd}, regionKey, &OptionFilters{IsReturn: true, DeliveryServiceIDs: map[uint64]bool{directPost: true}})
			assert.Len(t, results, 0)
		}
	}
	// return post
	{
		returnPost := getReturnFlowPostDeliveryServiceID()
		tReturnPost := t1
		tReturnPost.DeliveryServiceID = returnPost
		tReturnPost.DeliveryMethod = enums.DeliveryMethodPost
		// context.IsReturn == false
		{
			tariffsFinder := NewTariffsFinder()
			tariffsFinder.Add(&tReturnPost)
			tariffsFinder.Finish(&regionMap)
			results := tariffsFinder.FindOptions([]WeightAndDim{wd}, regionKey, &OptionFilters{IsReturn: false, DeliveryServiceIDs: map[uint64]bool{returnPost: true}})
			assert.Len(t, results, 0)
		}
		// context.IsReturn == true
		{
			tariffsFinder := NewTariffsFinder()
			tariffsFinder.Add(&tReturnPost)
			tariffsFinder.Finish(&regionMap)
			results := tariffsFinder.FindOptions([]WeightAndDim{wd}, regionKey, &OptionFilters{IsReturn: true, DeliveryServiceIDs: map[uint64]bool{returnPost: true}})
			assert.Len(t, results, 1)
		}
	}
}

func TestFindOptionsDeliveryDelay(t *testing.T) {
	regions := geobase.NewExample()
	tf := NewTariffsFinder()
	{
		regionKey := FromToRegions{From: 213, To: 213}
		t0 := TariffRT{
			ID:                1,
			DeliveryServiceID: 11,
			DeliveryMethod:    enums.DeliveryMethodPickup,
			FromToRegions:     regionKey,
			RuleAttrs: RuleAttrs{
				WeightMax: 100,
			},
			Type: RuleTypeGlobal,
		}
		t1 := t0
		t1.Option = Option{
			DaysMin: 1,
			DaysMax: 2,
		}
		t1.Type = RuleTypePayment
		tf.Add(&t0)
		tf.Add(&t1)
	}
	tf.Finish(&regions)
	findOptions := func(
		ddn deliverydelay.DeliveryDelayMapNew,
		useDeliveryDelay, useExtendedDelayFormat bool,
	) []*OptionResult {
		region213 := regions[213]
		wd := WeightAndDim{
			Weight: 10,
		}
		tso := TariffSearchOption{
			OptionFilters: OptionFilters{
				Method:             enums.DeliveryMethodPickup,
				TariffFor:          TFShop,
				DeliveryServiceIDs: map[uint64]bool{11: true},
				UseDeliveryDelay:   useDeliveryDelay,
			},
		}
		tf.deliveryDelayMapNew = ddn
		return tf.FindOptionsFromTo(
			region213,
			region213,
			[]WeightAndDim{wd},
			tso,
		)
	}
	{
		createDDN := func(deliveryType int) deliverydelay.DeliveryDelayMapNew {
			itemsNew := []deliverydelay.ItemNew{
				deliverydelay.ItemNew{
					DelayInDays:  2,
					RegionID:     213,
					DserviceID:   11,
					WarehouseID:  -1,
					DeliveryType: deliveryType,
				},
				deliverydelay.ItemNew{
					DelayInDays:  3,
					RegionID:     213,
					DserviceID:   11,
					WarehouseID:  145,
					DeliveryType: deliveryType,
				},
			}
			return deliverydelay.NewDeliveryDelayMapNew(itemsNew)
		}
		ddn := createDDN(1)
		// Случай пикапа
		results := findOptions(ddn, true, true)
		require.Len(t, results, 1)
		require.Equal(t, uint32(1), results[0].DaysMin)
		require.Equal(t, uint32(2), results[0].DaysMax)
		require.Equal(t, 2, len(results[0].Delays))

		allDelay, ok := results[0].Delays[-1]
		require.True(t, ok)
		require.Equal(t, 2, allDelay)

		delay, ok := results[0].Delays[145]
		require.True(t, ok)
		require.Equal(t, 3, delay)
		// Случай курьерки
		ddn = createDDN(2)
		results = findOptions(ddn, true, true)
		require.Len(t, results, 1)
		require.Equal(t, uint32(1), results[0].DaysMin)
		require.Equal(t, uint32(2), results[0].DaysMax)
		require.Nil(t, results[0].Delays)
		// Случай почты
		ddn = createDDN(2)
		results = findOptions(ddn, true, true)
		require.Len(t, results, 1)
		require.Equal(t, uint32(1), results[0].DaysMin)
		require.Equal(t, uint32(2), results[0].DaysMax)
		require.Nil(t, results[0].Delays)
	}
}

func TestCalcEffectiveWeight(t *testing.T) {
	wad := NewWeightAndDim(1000, 10, 10, 10)                // 1000 грамм, 10x10x10 см^3
	require.Equal(t, 1000.0, wad.CalcEffectiveWeight(100))  // используем реальный вес
	require.Equal(t, 1000.0, wad.CalcEffectiveWeight(1000)) // реальный вес и вес по пороговой плотности одинаковый
	require.Equal(t, 2000.0, wad.CalcEffectiveWeight(2000)) // используем вес по пороговой плотности
}

func TestValidateOption(t *testing.T) {
	limit := RuleAttrs{
		WeightMax: 1000, // в граммах
	}
	var m3weight uint32 = 200 // кг/м**3, минимальная плотность
	{
		// плотность 1000 кг/м^3 (вода)
		wad := NewWeightAndDim(1000, 10, 10, 10)
		require.Equal(t, 1000.0, wad.CalcEffectiveWeight(m3weight))
		err := validatePaymentOption(&limit, wad.CalcEffectiveWeight(m3weight))
		require.NoError(t, err)
	}
	{
		// плотность 125 кг/м^3
		wad := NewWeightAndDim(1000, 20, 20, 20)
		require.Equal(t, 1600.0, wad.CalcEffectiveWeight(m3weight))
		err := validatePaymentOption(&limit, wad.CalcEffectiveWeight(m3weight))
		require.Equal(t, err, ErrRuleTooHeavy)
	}
	{
		// плотность 8000 кг/м^3
		wad := NewWeightAndDim(1000, 5, 5, 5)
		require.Equal(t, 1000.0, wad.CalcEffectiveWeight(m3weight))
		err := validatePaymentOption(&limit, wad.CalcEffectiveWeight(m3weight))
		require.NoError(t, err)
	}
}

func TestFindPointsNumForRegions(t *testing.T) {
	{
		tariffMap := make(TariffMap)
		key := FromToRegions{From: geobase.RegionMoscowAndObl, To: geobase.RegionMoscow}
		tariffMap[key] = RegionalTariffs{TotalPointsNum: 322}
		tf := TariffsFinder{
			tariffs: tariffMap,
		}
		singleMoscow := []geobase.RegionChain{
			{
				{ID: geobase.RegionMoscow},
			},
		}
		res := tf.FindPointsNumForRegions(singleMoscow, 1)
		require.Equal(t, 1, len(res))
		require.Equal(t, 1, res[0].PointsNum) // потому что только один регион пришел
	}
	{
		tariffMap := make(TariffMap)
		key := FromToRegions{From: geobase.RegionMoscowAndObl, To: geobase.RegionMoscow}
		tariffMap[key] = RegionalTariffs{TotalPointsNum: 322}
		tf := TariffsFinder{
			tariffs: tariffMap,
		}
		singleMoscow := []geobase.RegionChain{
			{
				geobase.Region{ID: geobase.RegionMoscow},
			},
			{
				geobase.Region{ID: geobase.RegionID(12345)}, // регион без тарифа
			},
		}
		res := tf.FindPointsNumForRegions(singleMoscow, 1)
		require.Equal(t, 1, len(res))
		require.Equal(t, 1, res[0].PointsNum)
	}
	{
		tariffMap := make(TariffMap)
		key := FromToRegions{From: geobase.RegionMoscowAndObl, To: geobase.RegionMoscow}
		tariffMap[key] = RegionalTariffs{TotalPointsNum: 12}
		key = FromToRegions{From: geobase.RegionMoscowAndObl, To: geobase.RegionHamovniki}
		tariffMap[key] = RegionalTariffs{TotalPointsNum: 13}
		key = FromToRegions{From: geobase.RegionMoscowAndObl, To: geobase.RegionKotelniki}
		tariffMap[key] = RegionalTariffs{TotalPointsNum: 60}
		tf := TariffsFinder{
			tariffs: tariffMap,
		}
		fourWithMoscow := []geobase.RegionChain{
			{
				geobase.Region{ID: geobase.RegionMoscow},
			},
			{
				geobase.Region{ID: geobase.RegionHamovniki},
			},
			{
				geobase.Region{ID: geobase.RegionKotelniki},
			},
			{
				geobase.Region{ID: geobase.RegionID(12345)}, // регион без тарифа
			},
		}
		res := tf.FindPointsNumForRegions(fourWithMoscow, 1)
		require.Equal(t, 3, len(res))
		require.Equal(t, 1, res[0].PointsNum)
		require.Equal(
			t,
			geobase.RegionChain{
				geobase.Region{ID: geobase.RegionMoscow},
			},
			res[0].Reg,
		)
		require.Equal(t, 60, res[1].PointsNum)
		require.Equal(
			t,
			geobase.RegionChain{
				geobase.Region{ID: geobase.RegionKotelniki},
			},
			res[1].Reg,
		)
		require.Equal(t, 13, res[2].PointsNum)
		require.Equal(
			t,
			geobase.RegionChain{
				geobase.Region{ID: geobase.RegionHamovniki},
			},
			res[2].Reg,
		)
	}
}

func TestToCustomerTariffID(t *testing.T) {
	specs := []struct {
		val, want uint64
	}{
		{100000100147, 100147},
		{10000100147, 10000100147},
		{100147, 100147},
	}
	for _, spec := range specs {
		require.Equal(t, spec.want, ToCustomerTariffID(spec.val))
	}
}
