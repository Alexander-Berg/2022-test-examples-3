package express

import (
	"bytes"
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/settings"
	"a.yandex-team.ru/market/combinator/pkg/ytutil"
)

func newIntervalValidator(json string, shopsToCheckCapacity ShopsToCheckCapacity, regionSlotCapacityMap RegionSlotCapacityMap, regionsFarAllShops RegionsWithCapacityForAllShops) (IntervalValidator, error) {
	buf := bytes.NewBufferString(json)
	reader, err := ytutil.NewFileReader2(buf)
	if err != nil {
		return nil, err
	}
	defer reader.Close()

	slotCountMap, err := readRegionSlotCount(reader)
	if err != nil {
		return nil, err
	}

	return &SimpleIntervalValidator{
		ShopsToCheckCapacity:           shopsToCheckCapacity,
		RegionSlotCapacityMap:          regionSlotCapacityMap,
		SlotCountMap:                   slotCountMap,
		RegionsWithCapacityForAllShops: regionsFarAllShops,
	}, nil
}

func TestIsValidInterval(t *testing.T) {
	data := `{"region":213,"xtime":"2021-11-11T09","count":89}
{"region":213,"xtime":"2021-11-11T10","count":100}
{"region":10758,"xtime":"2021-11-11T09","count":89}
{"region":10758,"xtime":"2021-11-11T10","count":100}
`
	var shopNoCapacity uint32 = 1
	var shopWithCapacity uint32 = 42
	shopsToCheckCapacity := ShopsToCheckCapacity{
		shopWithCapacity: true,
	}
	// На 9 часов есть капасити, на 10 - нет
	regionSlotCapacityMap := RegionSlotCapacityMap{
		geobase.RegionMoscow: map[int]int{
			9:  90,
			10: 100,
		},
		geobase.RegionHimki: map[int]int{
			9:  90,
			10: 100,
		},
	}
	regionsWithCapacityForAllShops := RegionsWithCapacityForAllShops{
		geobase.RegionHimki: true,
	}
	validator, err := newIntervalValidator(data, shopsToCheckCapacity, regionSlotCapacityMap, regionsWithCapacityForAllShops)
	require.NoError(t, err)

	time09 := time.Date(2021, 11, 11, 9, 30, 0, 0, time.Local)
	time10 := time.Date(2021, 11, 11, 10, 30, 0, 0, time.Local)

	localSettings := &settings.Settings{}
	ctx := settings.ContextWithSettings(context.Background(), localSettings)

	require.True(t, validator.IsValidInterval(ctx, shopNoCapacity, int64(geobase.RegionMoscow), time09))
	require.True(t, validator.IsValidInterval(ctx, shopWithCapacity, int64(geobase.RegionMoscow), time09))
	require.True(t, validator.IsValidInterval(ctx, shopNoCapacity, int64(geobase.RegionMoscow), time10))
	require.False(t, validator.IsValidInterval(ctx, shopWithCapacity, int64(geobase.RegionMoscow), time10))

	{
		// empty partners
		validator, err := newIntervalValidator(data, nil, regionSlotCapacityMap, regionsWithCapacityForAllShops)
		require.NoError(t, err)
		require.True(t, validator.IsValidInterval(ctx, shopNoCapacity, int64(geobase.RegionMoscow), time09))
		require.True(t, validator.IsValidInterval(ctx, shopWithCapacity, int64(geobase.RegionMoscow), time09))
		require.False(t, validator.IsValidInterval(ctx, shopNoCapacity, int64(geobase.RegionMoscow), time10))
		require.False(t, validator.IsValidInterval(ctx, shopWithCapacity, int64(geobase.RegionMoscow), time10))
	}

	// COMBINATOR-3350 Тестируем для разных регионов с флагом и без
	for _, regionFlag := range []bool{false, true} {
		localSettings.ExpressCapacityForAllPartnersInRegion = regionFlag

		for _, regionID := range []geobase.RegionID{geobase.RegionMoscow, geobase.RegionHimki} {
			himki := (regionID == geobase.RegionHimki)

			// 9 часов, капасити есть везде
			require.Equal(t, true, validator.IsValidInterval(ctx, shopNoCapacity, int64(regionID), time09))
			require.Equal(t, true, validator.IsValidInterval(ctx, shopWithCapacity, int64(regionID), time09))

			// 10 часов, для магазина NoCapacity капасити нет только в Химках при установленном флаге
			require.Equal(t, !(himki && regionFlag), validator.IsValidInterval(ctx, shopNoCapacity, int64(regionID), time10))
			// 10 часов, для магазина WithCapacity капасити есть в Химках, если снят флаг
			require.Equal(t, himki && !regionFlag, validator.IsValidInterval(ctx, shopWithCapacity, int64(regionID), time10))

			{
				// Тесты с пустым списком магазинов
				validator, err := newIntervalValidator(data, nil, regionSlotCapacityMap, regionsWithCapacityForAllShops)
				require.NoError(t, err)
				// 9 часов, капасити есть везде
				require.Equal(t, true, validator.IsValidInterval(ctx, shopNoCapacity, int64(regionID), time09))
				require.Equal(t, true, validator.IsValidInterval(ctx, shopWithCapacity, int64(regionID), time09))

				// 10 часов, для обоих магазинов капасити есть только в Химках с опущенным флагом
				require.Equal(t, himki && !regionFlag, validator.IsValidInterval(ctx, shopNoCapacity, int64(regionID), time10))
				require.Equal(t, himki && !regionFlag, validator.IsValidInterval(ctx, shopWithCapacity, int64(regionID), time10))
			}

		}
	}
}

func TestPartnerIntervalValidator_IsValidInterval(t *testing.T) {
	data := `{"partner":276119,"xtime":"2021-11-11T09","count":9}
{"partner":276119,"xtime":"2021-11-11T10","count":19}
{"partner":275963,"xtime":"2021-11-11T09","count":13}
{"partner":275963,"xtime":"2021-11-11T10","count":25}
`
	var wh1, wh2, wh3 int64 = 276119, 275963, 275301
	partnerSlotCapacityMap := PartnerSlotCapacityMap{
		wh1: {
			9:  20,
			10: 20,
		},
		wh2: {
			9:  25,
			10: 25,
		},
	}

	validator, err := ReadPartnerIntervalValidatorFromString(data)
	require.NoError(t, err)
	validator.(*PartnerIntervalValidator).PartnerSlotCapacityMap = partnerSlotCapacityMap

	time09 := time.Date(2021, 11, 11, 9, 30, 0, 0, time.Local)
	time10 := time.Date(2021, 11, 11, 10, 30, 0, 0, time.Local)
	time11 := time.Date(2021, 11, 11, 11, 30, 0, 0, time.Local)

	localSettings := &settings.Settings{ExpressCapacityForPartners: true}
	ctx := settings.ContextWithSettings(context.Background(), localSettings)

	require.True(t, validator.IsValidInterval(ctx, 0, wh1, time09))
	require.True(t, validator.IsValidInterval(ctx, 0, wh1, time10))
	require.True(t, validator.IsValidInterval(ctx, 0, wh2, time09))
	require.False(t, validator.IsValidInterval(ctx, 0, wh2, time10))

	require.True(t, validator.IsValidInterval(ctx, 0, wh3, time09)) // unknown wh
	require.True(t, validator.IsValidInterval(ctx, 0, wh1, time11)) // unknown time
}
