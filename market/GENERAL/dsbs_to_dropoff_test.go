package lite

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/its"
)

func TestDSBSToDropOff(t *testing.T) {
	settings, _ := its.NewStringSettingsHolder(`{"modify_requests": false}`)
	env, cancel := NewEnv(t, &GenDataDSBS, settings)
	defer cancel()
	{
		req := MakeRequest(
			RequestWithStartTime(time.Date(2021, 3, 25, 12, 0, 0, 0, MskTZ)),
			RequestWithPartner(GraphExampleDSBS.WarehouseDSBS.PartnerLmsID),
			RequestWithRegion(geobase.RegionSaintPetersburg),
			RequestWithShopID(322),
			RequestWithItemPrice(6789),
			RequestWithRearrFactors("enable_dsbs_to_dropoff=1"),
		)
		resp, err := env.Client.GetCourierOptions(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, resp.Options, 10)
		for _, o := range resp.Options {
			require.Equal(t, false, o.IsExternalLogistics)
		}
	}
	{
		req := MakeRequest(
			RequestWithStartTime(time.Date(2021, 3, 25, 12, 0, 0, 0, MskTZ)),
			RequestWithPartner(GraphExampleDSBS.WarehouseDSBS.PartnerLmsID),
			RequestWithRegion(geobase.RegionMoscow),
			RequestWithShopID(322),
			RequestWithItemPrice(6789),
			RequestWithRearrFactors("enable_dsbs_to_dropoff=1;disable_external_requests=1"),
			RequestWithGpsCoords(55.753274, 37.619402),
			RequestWithUserInfo(true),
		)
		resp, err := env.Client.GetCourierOptions(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, resp.Options, 12)
	}
}
