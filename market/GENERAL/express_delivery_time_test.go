package express

import (
	"bytes"
	"fmt"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/ytutil"
	"a.yandex-team.ru/yt/go/yt"
)

var (
	region213 = geobase.RegionChain{
		{
			ID: geobase.RegionMoscow,
		},
	}
	region1 = geobase.RegionChain{
		{
			ID: geobase.RegionMoscowAndObl,
		},
	}
	region2 = geobase.RegionChain{
		{
			ID: geobase.RegionSaintPetersburg,
		},
	}
	region10174 = geobase.RegionChain{
		{
			ID: geobase.RegionSaintPetersburgAndObl,
		},
	}
)

func GetExampleRegionMap() *geobase.RegionMap {
	return &geobase.RegionMap{
		geobase.RegionMoscow: geobase.RegionChain{
			{
				ID: geobase.RegionMoscow,
			},
			{
				ID: geobase.RegionMoscowAndObl,
			},
		},
		geobase.RegionSaintPetersburg: geobase.RegionChain{
			{
				ID: geobase.RegionSaintPetersburg,
			},
			{
				ID: geobase.RegionSaintPetersburgAndObl,
			},
		},
	}
}

func GetExampleDeliveryTimes() []*deliveryTimeRaw {
	return []*deliveryTimeRaw{
		{
			Weekday:           "weekend",
			City:              "Москва",
			RegionID:          213,
			Distance:          "00-05",
			DistanceFrom:      0,
			DistanceTo:        5,
			TimeIntervalStart: 0,
			TimeIntervalEnd:   8,
			Quant15TimeMnt:    createFloat64(5),
			Quant95TimeMnt:    createFloat64(10),
			Quant15CCMnt:      createFloat64(10),
			Quant95CCMnt:      createFloat64(15),
		},
		{
			Weekday:           "weekend",
			City:              "Москва",
			RegionID:          213,
			Distance:          "05-10",
			DistanceFrom:      5,
			DistanceTo:        10,
			TimeIntervalStart: 0,
			TimeIntervalEnd:   8,
			Quant15TimeMnt:    createFloat64(10),
			Quant95TimeMnt:    createFloat64(20),
			Quant15CCMnt:      createFloat64(15),
			Quant95CCMnt:      createFloat64(25),
		},
		{
			Weekday:           "workday",
			City:              "Москва",
			RegionID:          213,
			Distance:          "05-10",
			DistanceFrom:      5,
			DistanceTo:        10,
			TimeIntervalStart: 0,
			TimeIntervalEnd:   8,
			Quant15TimeMnt:    createFloat64(30),
			Quant95TimeMnt:    createFloat64(40),
			Quant15CCMnt:      createFloat64(35),
			Quant95CCMnt:      createFloat64(45),
		},
		{
			Weekday:           "weekend",
			City:              "Москва",
			RegionID:          213,
			Distance:          "10-15",
			DistanceFrom:      10,
			DistanceTo:        15,
			TimeIntervalStart: 0,
			TimeIntervalEnd:   8,
			Quant15TimeMnt:    createFloat64(40),
			Quant95TimeMnt:    createFloat64(50),
			Quant15CCMnt:      createFloat64(45),
			Quant95CCMnt:      createFloat64(55),
		},
		{
			Weekday:           "workday",
			City:              "Москва",
			RegionID:          213,
			Distance:          "10-15",
			DistanceFrom:      10,
			DistanceTo:        15,
			TimeIntervalStart: 0,
			TimeIntervalEnd:   8,
			Quant15TimeMnt:    createFloat64(60),
			Quant95TimeMnt:    createFloat64(70),
			Quant15CCMnt:      createFloat64(65),
			Quant95CCMnt:      createFloat64(75),
		},
		{
			Weekday:           "weekend",
			City:              "Москва",
			RegionID:          2,
			Distance:          "05-10",
			DistanceFrom:      5,
			DistanceTo:        10,
			TimeIntervalStart: 0,
			TimeIntervalEnd:   8,
			Quant15TimeMnt:    createFloat64(80),
			Quant95TimeMnt:    createFloat64(90),
			Quant15CCMnt:      createFloat64(85),
			Quant95CCMnt:      createFloat64(95),
		},
		{
			Weekday:           "workday",
			City:              "Москва",
			RegionID:          2,
			Distance:          "05-10",
			DistanceFrom:      5,
			DistanceTo:        10,
			TimeIntervalStart: 0,
			TimeIntervalEnd:   8,
			Quant15TimeMnt:    createFloat64(100),
			Quant95TimeMnt:    createFloat64(110),
			Quant15CCMnt:      createFloat64(105),
			Quant95CCMnt:      createFloat64(115),
		},
		{
			Weekday:           "weekend",
			City:              "Москва",
			RegionID:          2,
			Distance:          "10-15",
			DistanceFrom:      10,
			DistanceTo:        15,
			TimeIntervalStart: 0,
			TimeIntervalEnd:   8,
			Quant15TimeMnt:    createFloat64(120),
			Quant95TimeMnt:    createFloat64(130),
			Quant15CCMnt:      createFloat64(125),
			Quant95CCMnt:      createFloat64(135),
		},
		{
			Weekday:           "workday",
			City:              "Москва",
			RegionID:          2,
			Distance:          "10-15",
			DistanceFrom:      10,
			DistanceTo:        15,
			TimeIntervalStart: 0,
			TimeIntervalEnd:   8,
			Quant15TimeMnt:    createFloat64(140),
			Quant95TimeMnt:    createFloat64(150),
			Quant15CCMnt:      createFloat64(145),
			Quant95CCMnt:      createFloat64(155),
		},
		{
			Weekday:           "weekend",
			City:              "Москва",
			RegionID:          213,
			Distance:          "05-10",
			DistanceFrom:      5,
			DistanceTo:        10,
			TimeIntervalStart: 8,
			TimeIntervalEnd:   24,
			Quant15TimeMnt:    createFloat64(160),
			Quant95TimeMnt:    createFloat64(170),
			Quant15CCMnt:      createFloat64(165),
			Quant95CCMnt:      createFloat64(175),
		},
		{
			Weekday:           "workday",
			City:              "Москва",
			RegionID:          213,
			Distance:          "05-10",
			DistanceFrom:      5,
			DistanceTo:        10,
			TimeIntervalStart: 8,
			TimeIntervalEnd:   24,
			Quant15TimeMnt:    createFloat64(180),
			Quant95TimeMnt:    createFloat64(190),
			Quant15CCMnt:      createFloat64(185),
			Quant95CCMnt:      createFloat64(195),
		},
		{
			Weekday:           "weekend",
			City:              "Москва",
			RegionID:          213,
			Distance:          "10-15",
			DistanceFrom:      10,
			DistanceTo:        15,
			TimeIntervalStart: 8,
			TimeIntervalEnd:   24,
			Quant15TimeMnt:    createFloat64(200),
			Quant95TimeMnt:    createFloat64(210),
		},
		{
			Weekday:           "workday",
			City:              "Москва",
			RegionID:          213,
			Distance:          "10-15",
			DistanceFrom:      10,
			DistanceTo:        15,
			TimeIntervalStart: 8,
			TimeIntervalEnd:   24,
			Quant15TimeMnt:    createFloat64(220),
			Quant95TimeMnt:    createFloat64(230),
		},
		{
			Weekday:           "weekend",
			City:              "Москва",
			RegionID:          2,
			Distance:          "05-10",
			DistanceFrom:      5,
			DistanceTo:        10,
			TimeIntervalStart: 8,
			TimeIntervalEnd:   24,
			Quant15TimeMnt:    createFloat64(240),
			Quant95TimeMnt:    createFloat64(250),
		},
		{
			Weekday:           "workday",
			City:              "Москва",
			RegionID:          2,
			Distance:          "05-10",
			DistanceFrom:      5,
			DistanceTo:        10,
			TimeIntervalStart: 8,
			TimeIntervalEnd:   24,
			Quant15TimeMnt:    createFloat64(260),
			Quant95TimeMnt:    createFloat64(270),
		},
		{
			Weekday:           "weekend",
			City:              "Москва",
			RegionID:          2,
			Distance:          "10-15",
			DistanceFrom:      10,
			DistanceTo:        15,
			TimeIntervalStart: 8,
			TimeIntervalEnd:   24,
			Quant15TimeMnt:    createFloat64(280),
			Quant95TimeMnt:    createFloat64(290),
		},
		{
			Weekday:           "workday",
			City:              "Москва",
			RegionID:          2,
			Distance:          "10-15",
			DistanceFrom:      10,
			DistanceTo:        15,
			TimeIntervalStart: 8,
			TimeIntervalEnd:   24,
			Quant15TimeMnt:    createFloat64(300),
			Quant95TimeMnt:    createFloat64(310),
		},
		{
			Weekday:           "weekend",
			City:              "Москва",
			RegionID:          213,
			Distance:          "45+",
			DistanceFrom:      45,
			DistanceTo:        2000,
			TimeIntervalStart: 0,
			TimeIntervalEnd:   8,
			Quant15TimeMnt:    createFloat64(310),
			Quant95TimeMnt:    createFloat64(320),
		},
		{
			Weekday:           "weekend",
			City:              "Москва",
			RegionID:          213,
			Distance:          "00-05",
			DistanceFrom:      0,
			DistanceTo:        5,
			TimeIntervalStart: 22,
			TimeIntervalEnd:   24,
			Quant15TimeMnt:    createFloat64(320),
			Quant95TimeMnt:    createFloat64(330),
		},
		{
			Weekday:           "workday",
			City:              "Москва",
			RegionID:          213,
			Distance:          "00-05",
			DistanceFrom:      0,
			DistanceTo:        5,
			TimeIntervalStart: 22,
			TimeIntervalEnd:   24,
			Quant15TimeMnt:    nil,
			Quant95TimeMnt:    nil,
		},
	}
}

func TestReadDeliveryTime(t *testing.T) {
	data := `{"weekday":"weekend","distance":"5-10","region_id":213,"time_interval_start":0,"city":"Москва","distance_from":5,"distance_to":10,"time_interval_end":8,"orders_count":20,"quant_05_time_mnt":14.250000000000002,"quant_10_time_mnt":15,"quant_15_time_mnt":16.7,"quant_20_time_mnt":17.8,"quant_25_time_mnt":18.75,"quant_30_time_mnt":20.4,"quant_35_time_mnt":21.65,"quant_40_time_mnt":22,"quant_45_time_mnt":22.55,"quant_50_time_mnt":23,"quant_55_time_mnt":23.450000000000003,"quant_60_time_mnt":25.6,"quant_65_time_mnt":28.35,"quant_70_time_mnt":29.3,"quant_75_time_mnt":30.75,"quant_80_time_mnt":33.4,"quant_85_time_mnt":35.15,"quant_90_time_mnt":37.000000000000014,"quant_95_time_mnt":58.400000000000176,"quant_05_cc_mnt":14.250000000000002,"quant_10_cc_mnt":15,"quant_15_cc_mnt":16.7,"quant_20_cc_mnt":17.8,"quant_25_cc_mnt":18.75,"quant_30_cc_mnt":20.4,"quant_35_cc_mnt":21.65,"quant_40_cc_mnt":22,"quant_45_cc_mnt":22.55,"quant_50_cc_mnt":23,"quant_55_cc_mnt":23.450000000000003,"quant_60_cc_mnt":25.6,"quant_65_cc_mnt":28.35,"quant_70_cc_mnt":29.3,"quant_75_cc_mnt":30.75,"quant_80_cc_mnt":33.4,"quant_85_cc_mnt":35.15,"quant_90_cc_mnt":37.000000000000014,"quant_95_cc_mnt":58.400000000000176}`
	item := DeliveryTime{
		hourInterval: hourInterval{Start: 0, End: 8},
		From:         17,
		To:           58,
		FromCC:       17,
		ToCC:         58,
	}

	buf := bytes.NewBufferString(data)
	reader, err := ytutil.NewFileReader2(buf)
	require.NoError(t, err)
	defer func(reader yt.TableReader) {
		err := reader.Close()
		if err != nil {
			require.NoError(t, err)
		}
	}(reader)
	dtm, err := readDeliveryTimeAnalytics(reader, nil)
	require.NoError(t, err)
	require.Len(t, dtm.analyticalValues, 1)
	require.Equal(t, &item, dtm.analyticalValues[213]["weekend"]["5-10"][0])
}

func TestBuildAnalyticalValues(t *testing.T) {
	lines := GetExampleDeliveryTimes()
	var raw string
	for _, item := range lines {
		itemRaw, err := json.Marshal(item)
		require.NoError(t, err)
		raw += fmt.Sprintf("%s\n", itemRaw)
	}
	buf := bytes.NewBufferString(raw)
	reader, err := ytutil.NewFileReader2(buf)
	require.NoError(t, err)
	defer func(reader yt.TableReader) {
		err := reader.Close()
		if err != nil {
			require.NoError(t, err)
		}
	}(reader)
	dtm, err := readDeliveryTimeAnalytics(reader, GetExampleRegionMap())
	val := dtm.analyticalValues
	require.NoError(t, err)

	require.Contains(t, val, geobase.RegionMoscow)
	require.Contains(t, val, geobase.RegionMoscowAndObl)
	require.Contains(t, val, geobase.RegionSaintPetersburg)
	require.Contains(t, val, geobase.RegionSaintPetersburgAndObl)

	require.Contains(t, val[geobase.RegionMoscow], "workday")
	require.Contains(t, val[geobase.RegionMoscow], "weekend")
	require.Contains(t, val[geobase.RegionMoscowAndObl], "workday")
	require.Contains(t, val[geobase.RegionMoscowAndObl], "weekend")
	require.Contains(t, val[geobase.RegionSaintPetersburg], "workday")
	require.Contains(t, val[geobase.RegionSaintPetersburg], "weekend")
	require.Contains(t, val[geobase.RegionSaintPetersburgAndObl], "workday")
	require.Contains(t, val[geobase.RegionSaintPetersburgAndObl], "weekend")

	require.NotContains(t, val[geobase.RegionMoscow]["workday"], "00-05") // nil значения
	require.Contains(t, val[geobase.RegionMoscow]["workday"], "05-10")
	require.Contains(t, val[geobase.RegionMoscow]["workday"], "10-15")
	require.Contains(t, val[geobase.RegionMoscow]["weekend"], "05-10")
	require.Contains(t, val[geobase.RegionMoscow]["weekend"], "10-15")
	require.Contains(t, val[geobase.RegionMoscowAndObl]["workday"], "05-10")
	require.Contains(t, val[geobase.RegionMoscowAndObl]["workday"], "10-15")
	require.Contains(t, val[geobase.RegionMoscowAndObl]["weekend"], "05-10")
	require.Contains(t, val[geobase.RegionMoscowAndObl]["weekend"], "10-15")
	require.Contains(t, val[geobase.RegionSaintPetersburg]["workday"], "05-10")
	require.Contains(t, val[geobase.RegionSaintPetersburg]["workday"], "10-15")
	require.Contains(t, val[geobase.RegionSaintPetersburg]["weekend"], "05-10")
	require.Contains(t, val[geobase.RegionSaintPetersburg]["weekend"], "10-15")
	require.Contains(t, val[geobase.RegionSaintPetersburgAndObl]["workday"], "05-10")
	require.Contains(t, val[geobase.RegionSaintPetersburgAndObl]["workday"], "10-15")
	require.Contains(t, val[geobase.RegionSaintPetersburgAndObl]["weekend"], "05-10")
	require.Contains(t, val[geobase.RegionSaintPetersburgAndObl]["weekend"], "10-15")

	require.Len(t, val[geobase.RegionMoscow]["workday"]["05-10"], 2)
	require.Len(t, val[geobase.RegionMoscow]["workday"]["10-15"], 2)
	require.Len(t, val[geobase.RegionMoscow]["weekend"]["05-10"], 2)
	require.Len(t, val[geobase.RegionMoscow]["weekend"]["10-15"], 2)
	require.Len(t, val[geobase.RegionSaintPetersburg]["workday"]["05-10"], 2)
	require.Len(t, val[geobase.RegionSaintPetersburg]["workday"]["10-15"], 2)
	require.Len(t, val[geobase.RegionSaintPetersburg]["weekend"]["05-10"], 2)
	require.Len(t, val[geobase.RegionSaintPetersburg]["weekend"]["10-15"], 2)

	require.Len(t, val[geobase.RegionMoscowAndObl]["workday"]["05-10"], 2)
	require.Len(t, val[geobase.RegionMoscowAndObl]["workday"]["10-15"], 2)
	require.Len(t, val[geobase.RegionMoscowAndObl]["weekend"]["05-10"], 2)
	require.Len(t, val[geobase.RegionMoscowAndObl]["weekend"]["10-15"], 2)
	require.Len(t, val[geobase.RegionSaintPetersburgAndObl]["workday"]["05-10"], 2)
	require.Len(t, val[geobase.RegionSaintPetersburgAndObl]["workday"]["10-15"], 2)
	require.Len(t, val[geobase.RegionSaintPetersburgAndObl]["weekend"]["05-10"], 2)
	require.Len(t, val[geobase.RegionSaintPetersburgAndObl]["weekend"]["10-15"], 2)
}

func TestCalcDeliveryTime(t *testing.T) {
	lines := GetExampleDeliveryTimes()
	var raw string
	for _, item := range lines {
		itemRaw, err := json.Marshal(item)
		require.NoError(t, err)
		raw += fmt.Sprintf("%s\n", itemRaw)
	}
	buf := bytes.NewBufferString(raw)
	reader, err := ytutil.NewFileReader2(buf)
	require.NoError(t, err)
	defer func(reader yt.TableReader) {
		err := reader.Close()
		if err != nil {
			require.NoError(t, err)
		}
	}(reader)
	dtm, err := readDeliveryTimeAnalytics(reader, GetExampleRegionMap())
	require.NoError(t, err)
	time09workday := time.Date(2021, 11, 11, 9, 30, 0, 0, time.Local)
	time07workday := time.Date(2021, 11, 11, 7, 30, 0, 0, time.Local)

	result, err := dtm.CalcDeliveryTime(region2, 6000, time09workday)
	require.NoError(t, err)
	require.Equal(t, 260, result.From)
	require.Equal(t, 270, result.To)
	require.Equal(t, 0, result.FromCC)
	require.Equal(t, 0, result.ToCC)

	result, err = dtm.CalcDeliveryTime(region213, 6000, time09workday)
	require.NoError(t, err)
	require.Equal(t, 180, result.From)
	require.Equal(t, 190, result.To)
	require.Equal(t, 185, result.FromCC)
	require.Equal(t, 195, result.ToCC)

	result, err = dtm.CalcDeliveryTime(region2, 6000, time07workday)
	require.NoError(t, err)
	require.Equal(t, 100, result.From)
	require.Equal(t, 110, result.To)
	require.Equal(t, 105, result.FromCC)
	require.Equal(t, 115, result.ToCC)

	result, err = dtm.CalcDeliveryTime(region213, 6000, time07workday)
	require.NoError(t, err)
	require.Equal(t, 30, result.From)
	require.Equal(t, 40, result.To)
	require.Equal(t, 35, result.FromCC)
	require.Equal(t, 45, result.ToCC)

	result, err = dtm.CalcDeliveryTime(region10174, 6000, time09workday)
	require.NoError(t, err)
	require.Equal(t, 260, result.From)
	require.Equal(t, 270, result.To)
	require.Equal(t, 0, result.FromCC)
	require.Equal(t, 0, result.ToCC)

	result, err = dtm.CalcDeliveryTime(region1, 6000, time09workday)
	require.NoError(t, err)
	require.Equal(t, 180, result.From)
	require.Equal(t, 190, result.To)
	require.Equal(t, 185, result.FromCC)
	require.Equal(t, 195, result.ToCC)

	result, err = dtm.CalcDeliveryTime(region10174, 6000, time07workday)
	require.NoError(t, err)
	require.Equal(t, 100, result.From)
	require.Equal(t, 110, result.To)
	require.Equal(t, 105, result.FromCC)
	require.Equal(t, 115, result.ToCC)

	result, err = dtm.CalcDeliveryTime(region1, 6000, time07workday)
	require.NoError(t, err)
	require.Equal(t, 30, result.From)
	require.Equal(t, 40, result.To)
	require.Equal(t, 35, result.FromCC)
	require.Equal(t, 45, result.ToCC)

	time09weekend := time.Date(2021, 11, 13, 9, 30, 0, 0, time.Local)
	time07weekend := time.Date(2021, 11, 13, 7, 30, 0, 0, time.Local)

	result, err = dtm.CalcDeliveryTime(region2, 6000, time09weekend)
	require.NoError(t, err)
	require.Equal(t, 240, result.From)
	require.Equal(t, 250, result.To)

	result, err = dtm.CalcDeliveryTime(region213, 3000, time07weekend)
	require.NoError(t, err)
	require.Equal(t, 5, result.From)
	require.Equal(t, 10, result.To)

	result, err = dtm.CalcDeliveryTime(region213, 6000, time09weekend)
	require.NoError(t, err)
	require.Equal(t, 160, result.From)
	require.Equal(t, 170, result.To)

	result, err = dtm.CalcDeliveryTime(region2, 6000, time07weekend)
	require.NoError(t, err)
	require.Equal(t, 80, result.From)
	require.Equal(t, 90, result.To)

	result, err = dtm.CalcDeliveryTime(region213, 6000, time07weekend)
	require.NoError(t, err)
	require.Equal(t, 10, result.From)
	require.Equal(t, 20, result.To)

	result, err = dtm.CalcDeliveryTime(region2, 10000, time09workday)
	require.NoError(t, err)
	require.Equal(t, 260, result.From)
	require.Equal(t, 270, result.To)

	result, err = dtm.CalcDeliveryTime(region213, 10000, time09workday)
	require.NoError(t, err)
	require.Equal(t, 180, result.From)
	require.Equal(t, 190, result.To)

	result, err = dtm.CalcDeliveryTime(region2, 10000, time07workday)
	require.NoError(t, err)
	require.Equal(t, 100, result.From)
	require.Equal(t, 110, result.To)

	result, err = dtm.CalcDeliveryTime(region213, 10000, time07workday)
	require.NoError(t, err)
	require.Equal(t, 30, result.From)
	require.Equal(t, 40, result.To)

	result, err = dtm.CalcDeliveryTime(region2, 10000, time09weekend)
	require.NoError(t, err)
	require.Equal(t, 240, result.From)
	require.Equal(t, 250, result.To)

	result, err = dtm.CalcDeliveryTime(region213, 10000, time09weekend)
	require.NoError(t, err)
	require.Equal(t, 160, result.From)
	require.Equal(t, 170, result.To)

	result, err = dtm.CalcDeliveryTime(region2, 10000, time07weekend)
	require.NoError(t, err)
	require.Equal(t, 80, result.From)
	require.Equal(t, 90, result.To)

	result, err = dtm.CalcDeliveryTime(region213, 10000, time07weekend)
	require.NoError(t, err)
	require.Equal(t, 10, result.From)
	require.Equal(t, 20, result.To)

	result, err = dtm.CalcDeliveryTime(region2, 11000, time09weekend)
	require.NoError(t, err)
	require.Equal(t, 280, result.From)
	require.Equal(t, 290, result.To)

	result, err = dtm.CalcDeliveryTime(region213, 11000, time09weekend)
	require.NoError(t, err)
	require.Equal(t, 200, result.From)
	require.Equal(t, 210, result.To)

	result, err = dtm.CalcDeliveryTime(region2, 11000, time07weekend)
	require.NoError(t, err)
	require.Equal(t, 120, result.From)
	require.Equal(t, 130, result.To)

	result, err = dtm.CalcDeliveryTime(region213, 11000, time07weekend)
	require.NoError(t, err)
	require.Equal(t, 40, result.From)
	require.Equal(t, 50, result.To)

	result, err = dtm.CalcDeliveryTime(region213, 46000, time07weekend)
	require.NoError(t, err)
	require.Equal(t, 310, result.From)
	require.Equal(t, 320, result.To)

	result, err = dtm.CalcDeliveryTime(region213, 10, time07weekend)
	require.NoError(t, err)
	require.Equal(t, 5, result.From)
	require.Equal(t, 10, result.To)

	time00weekend := time.Date(2021, 11, 13, 0, 30, 0, 0, time.Local)

	result, err = dtm.CalcDeliveryTime(region213, 10, time00weekend)
	require.NoError(t, err)
	require.Equal(t, 5, result.From)
	require.Equal(t, 10, result.To)

	time23weekend := time.Date(2021, 11, 13, 23, 30, 0, 0, time.Local)
	time22weekend := time.Date(2021, 11, 13, 22, 30, 0, 0, time.Local)

	result, err = dtm.CalcDeliveryTime(region213, 10, time23weekend)
	require.NoError(t, err)
	require.Equal(t, 320, result.From)
	require.Equal(t, 330, result.To)

	result, err = dtm.CalcDeliveryTime(region213, 10, time22weekend)
	require.NoError(t, err)
	require.Equal(t, 320, result.From)
	require.Equal(t, 330, result.To)
}

func TestRound5(t *testing.T) {
	needValues := []int{0, 0, 0, 5, 5, 5, 5, 5, 10, 10, 10, 10, 10, 15, 15, 15, 15, 15, 20, 20, 20, 20, 20, 25, 25, 25, 25, 25, 30, 30}
	for i := 0; i < 30; i++ {
		require.Equal(t, needValues[i], roundTo5(i))
	}
}

func createFloat64(i float64) *float64 {
	return &i
}
