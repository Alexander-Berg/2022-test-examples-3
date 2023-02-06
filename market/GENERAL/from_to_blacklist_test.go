package hardconfig

import (
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
)

func TestDecodeFromToBlacklist(t *testing.T) {
	blacklist, err := NewFromToBlackListMap("from_to_blacklist_test.yaml")
	require.NoError(t, err, "decode from to blacklist err")
	require.NotNil(t, blacklist, "blacklist is empty")

	pickup := "PICKUP"
	needValues := []FromToBlacklistItem{
		{
			PartnerID:    1003937,
			From:         213,
			To:           []geobase.RegionID{2, 213},
			DeliveryType: &pickup,
			deliveryType: enums.DeliveryMethodPickup,
			to: map[geobase.RegionID]struct{}{
				geobase.RegionID(2):   {},
				geobase.RegionID(213): {},
			},
		},
		{
			PartnerID: 1003937,
			From:      213,
			To:        []geobase.RegionID{3, 214},
			to: map[geobase.RegionID]struct{}{
				geobase.RegionID(3):   {},
				geobase.RegionID(214): {},
			},
		},
	}

	require.Contains(t, blacklist, uint64(1003937))

	for i, blacklistItem := range blacklist[1003937] {
		if i >= 2 {
			continue
		}

		require.Equal(t, blacklistItem, needValues[i])
	}
}

func TestFromToBlacklist(t *testing.T) {
	blacklist, _ := NewFromToBlackListMap("from_to_blacklist_test.yaml")
	require.NotNil(t, blacklist, "blacklist is empty")

	f := HardConfig{
		FromToBlackList: blacklist,
	}

	result := f.FromToBlackList.Filter(
		1003937,
		geobase.RegionChain{
			geobase.Region{ID: geobase.RegionMoscow},
			geobase.Region{ID: geobase.RegionMoscowAndObl},
		},
		geobase.RegionChain{
			geobase.Region{ID: geobase.RegionSaintPetersburg}},
		enums.DeliveryMethodPickup|enums.DeliveryMethodCourier|enums.DeliveryMethodPost,
	)

	require.Equal(t, result, enums.DeliveryMethodCourier|enums.DeliveryMethodPost)

	result = f.FromToBlackList.Filter(
		1003937,
		geobase.RegionChain{
			geobase.Region{ID: geobase.RegionMoscow},
			geobase.Region{ID: geobase.RegionMoscowAndObl},
		},
		geobase.RegionChain{
			geobase.Region{ID: geobase.RegionHamovniki}},
		enums.DeliveryMethodPickup|enums.DeliveryMethodCourier|enums.DeliveryMethodPost,
	)

	require.Equal(t, result,
		enums.DeliveryMethodPickup|enums.DeliveryMethodCourier|enums.DeliveryMethodPost)

	result = f.FromToBlackList.Filter(
		1003938,
		geobase.RegionChain{
			geobase.Region{ID: geobase.RegionMoscow},
			geobase.Region{ID: geobase.RegionMoscowAndObl},
		},
		geobase.RegionChain{
			geobase.Region{ID: 2}},
		enums.DeliveryMethodCourier,
	)

	require.Equal(t, result, enums.DeliveryMethodUnknown)

	result = f.FromToBlackList.Filter(
		1003937,
		geobase.RegionChain{
			geobase.Region{ID: geobase.RegionMoscow},
			geobase.Region{ID: geobase.RegionMoscowAndObl},
		},
		geobase.RegionChain{
			geobase.Region{ID: geobase.RegionEkaterinburg}},
		enums.DeliveryMethodPickup|enums.DeliveryMethodCourier|enums.DeliveryMethodPost,
	)

	require.Equal(t, result,
		enums.DeliveryMethodPickup|enums.DeliveryMethodPost)
}
