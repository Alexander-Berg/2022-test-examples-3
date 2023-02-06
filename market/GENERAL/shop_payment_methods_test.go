package dsbs

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
)

const exampleFile = `[
  {
    "id": 1048603,
    "groups": [
      {
        "paymentTypes": [
          "PREPAYMENT_CARD"
        ],
        "includedRegions": [
          1,
          149,
          159
        ],
        "excludedRegions": [
          213
        ]
      },
      {
        "paymentTypes": [
          "COURIER_CASH",
          "COURIER_CARD",
          "PREPAYMENT_CARD"
        ],
        "includedRegions": [
          213
        ]
      }
    ]
  }
]`

func TestParseShopPaymentFile(t *testing.T) {
	rfs, err := parsePaymentMethodFile()
	require.NoError(t, err)

	require.Len(t, rfs, 105450, fmt.Sprintf("real size is %d", len(rfs)))
}

func TestParsing(t *testing.T) {
	rfs, err := parseFromBuffer([]byte(exampleFile))
	require.NoError(t, err)

	require.Len(t, rfs, 1)

	pmm := convertRawData(rfs)
	require.Len(t, pmm, 1)

	shopGroups, ok := pmm[1048603]
	require.True(t, ok)
	require.Len(t, shopGroups, 2)

	g1 := shopGroups[0]
	require.Len(t, g1.IncludedRegions, 3)
	require.Len(t, g1.ExcludedRegions, 1)
	require.Equal(t, enums.MethodPrepayAllowed, g1.PaymentMethods)

	g2 := shopGroups[1]
	require.Len(t, g2.IncludedRegions, 1)
	require.Nil(t, g2.ExcludedRegions)
	require.Equal(t, enums.AllPaymentMethods, g2.PaymentMethods)

	// Map to Moscow rule
	chainMoscow := geobase.RegionChain{
		{ID: geobase.RegionHamovniki},
		{ID: geobase.RegionMoscow},
		{ID: geobase.RegionMoscowAndObl},
	}
	pm := doCheckPaymentMethods(1048603, chainMoscow, pmm, DefaultPaymentMethods)
	require.Equal(t, enums.AllPaymentMethods, pm)

	// Map to MoscowObl rule
	chainMosObl := geobase.RegionChain{
		{ID: geobase.RegionSofyno},
		{ID: geobase.RegionMoscowAndObl},
	}
	pm = doCheckPaymentMethods(1048603, chainMosObl, pmm, DefaultPaymentMethods)
	require.Equal(t, enums.MethodPrepayAllowed, pm)

	// No rule for Spb, so map to default
	chainSpb := geobase.RegionChain{
		{ID: geobase.RegionSaintPetersburg},
		{ID: geobase.RegionSaintPetersburgAndObl},
		{ID: geobase.RegionRussia},
	}
	pm = doCheckPaymentMethods(1048603, chainSpb, pmm, DefaultPaymentMethods)
	require.Equal(t, DefaultPaymentMethods, pm)

	// No rule for this shop
	pm = doCheckPaymentMethods(322, chainMoscow, pmm, DefaultPaymentMethods)
	require.Equal(t, DefaultPaymentMethods, pm)
}
