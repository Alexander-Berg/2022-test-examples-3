package deliverydelay

import (
	"strings"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/geobase"
)

func TestResourcesNew(t *testing.T) {
	items, err := parseResourceNew()
	require.NoError(t, err)
	require.True(t, len(items) != 0)
}

const newCsv = "1\t11235\t1003937\t-1\t1\n" +
	"2\t11235\t1003937\t145\t1\n" +
	"3\t11235\t1003937\t172\t1\n" +
	"4\t11235\t1003937\t-1\t2\n" +
	"5\t11235\t1003937\t145\t2\n" +
	"6\t11235\t1003937\t172\t2\n" +
	"7\t213\t1003937\t111\t3\n"

func TestParseCSVNew(t *testing.T) {
	reader := strings.NewReader(newCsv)
	items, err := parseNewCSV(reader)
	require.NoError(t, err)
	expectedItems := []ItemNew{
		{
			DelayInDays:  1,
			RegionID:     11235,
			DserviceID:   1003937,
			WarehouseID:  -1,
			DeliveryType: 1,
		},
		{
			DelayInDays:  2,
			RegionID:     11235,
			DserviceID:   1003937,
			WarehouseID:  145,
			DeliveryType: 1,
		},
		{
			DelayInDays:  3,
			RegionID:     11235,
			DserviceID:   1003937,
			WarehouseID:  172,
			DeliveryType: 1,
		},
		{
			DelayInDays:  4,
			RegionID:     11235,
			DserviceID:   1003937,
			WarehouseID:  -1,
			DeliveryType: 2,
		},
		{
			DelayInDays:  5,
			RegionID:     11235,
			DserviceID:   1003937,
			WarehouseID:  145,
			DeliveryType: 2,
		},
		{
			DelayInDays:  6,
			RegionID:     11235,
			DserviceID:   1003937,
			WarehouseID:  172,
			DeliveryType: 2,
		},
		{
			DelayInDays:  7,
			RegionID:     213,
			DserviceID:   1003937,
			WarehouseID:  111,
			DeliveryType: 3,
		},
	}
	require.Len(t, items, 7)
	for i := range items {
		require.Equal(t, expectedItems[i], items[i])
	}
}

func TestFindNew(t *testing.T) {
	reader := strings.NewReader(newCsv)
	items, err := parseNewCSV(reader)
	require.NoError(t, err)

	ddn := NewDeliveryDelayMapNew(items)
	// Find pickup delivery delays
	delaysPickup := ddn.FindDelaysNew(1003937, []geobase.Region{geobase.Region{ID: 11235}}, 1)
	require.Equal(t, 3, len(delaysPickup))
	d, ok := ChooseDelay(delaysPickup, 145)
	require.True(t, ok)
	require.Equal(t, 2, d)

	d, ok = ChooseDelay(delaysPickup, 172)
	require.True(t, ok)
	require.Equal(t, 3, d)

	d, ok = ChooseDelay(delaysPickup, 322)
	require.True(t, ok)
	require.Equal(t, 1, d)

	// Find courier delivery delays
	delaysCourier := ddn.FindDelaysNew(1003937, []geobase.Region{geobase.Region{ID: 11235}}, 2)
	require.Equal(t, 3, len(delaysCourier))
	d, ok = ChooseDelay(delaysCourier, 145)
	require.True(t, ok)
	require.Equal(t, 5, d)

	d, ok = ChooseDelay(delaysCourier, 172)
	require.True(t, ok)
	require.Equal(t, 6, d)

	d, ok = ChooseDelay(delaysCourier, 322)
	require.True(t, ok)
	require.Equal(t, 4, d)

	// Find post delivery delays
	delaysPost := ddn.FindDelaysNew(1003937, []geobase.Region{geobase.Region{ID: 213}}, 3)
	require.Equal(t, 1, len(delaysPost))
	d, ok = ChooseDelay(delaysPost, 111)
	require.True(t, ok)
	require.Equal(t, 7, d)

	d, ok = ChooseDelay(delaysPost, 145)
	require.False(t, ok)
	require.Equal(t, 0, d)

	// No delays for this delivery service
	delaysWrongDS := ddn.FindDelaysNew(1003939, []geobase.Region{geobase.Region{ID: 11235}}, 2)
	require.Nil(t, delaysWrongDS)
}
