package deferredcourierlite

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/test/requirepb"
	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/its"
	"a.yandex-team.ru/market/combinator/pkg/lite"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

func TestTooExpensiveCart(t *testing.T) {
	settings, _ := its.NewStringSettingsHolder("{}")
	graphEx, disabledDates := makeDeferredCourierGraph(settings)
	generationData := bg.GenerationData{
		RegionMap:           geobase.NewExample(),
		TariffsFinder:       makeTariffsFinder(),
		Graph:               graphEx,
		DisabledDatesHashed: disabledDates,
	}
	env, cancel := lite.NewEnv(t, &generationData, settings)
	defer cancel()

	const totalPrice = 120000
	startTime := time.Date(2021, 6, 4, 12, 0, 0, 0, lite.MskTZ)

	// GetCourierOptions
	{
		const courierOptionCount = 5
		req := lite.MakeRequest(
			lite.RequestWithStartTime(startTime),
			lite.RequestWithDeliveryType(pb.DeliveryType_COURIER),
			lite.RequestWithPartner(ffWarehouseID),
			lite.RequestWithRegion(geobase.RegionMoscow),
			lite.RequestWithRearrFactors("market_combinator_deferred_courier=1"),
			lite.RequestWithGpsCoords(userLatitude, userLongitude),
			lite.RequestWithTotalPrice(totalPrice),
			lite.RequestWithUserInfo(true),
		)
		resp, err := env.Client.GetCourierOptions(env.Ctx, req)
		require.NoError(t, err)

		require.Len(t, resp.Options, courierOptionCount)
		for i := 0; i < courierOptionCount; i++ {
			option := resp.Options[i]
			require.Equal(t, uint32(lastMileDeliveryServiceID), option.DeliveryServiceId)
			requirepb.Equal(t, &pb.Date{Year: 2021, Month: 6, Day: uint32(5 + i)}, option.DateFrom)
			requirepb.Equal(t, &pb.Date{Year: 2021, Month: 6, Day: uint32(5 + i)}, option.DateTo)
			requirepb.Equal(t, &pb.Time{Hour: 16}, option.Interval.From)
			requirepb.Equal(t, &pb.Time{Hour: 22}, option.Interval.To)
		}

		dsResp, dsErr := env.Client.GetCourierDeliveryStatistics(env.Ctx, req)
		require.NoError(t, dsErr)
		require.Len(t, dsResp.DeliveryMethods, 1)
		require.Equal(t, pb.DeliveryMethod_DM_COURIER, dsResp.DeliveryMethods[0])
	}
}
