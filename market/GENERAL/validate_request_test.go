package ondemandlite

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

func TestTooExpensiveBasket(t *testing.T) {
	generationData := bg.GenerationData{
		RegionMap:     geobase.NewExample(),
		TariffsFinder: lite.NewFinderSet(MakeTariffsFinder()),
		Graph:         MakeOnDemandGraph(),
	}
	settings, _ := its.NewStringSettingsHolder("{}")
	env, cancel := lite.NewEnv(t, &generationData, settings)
	defer cancel()

	PrepareHTTPClient(env)

	const totalPrice = 150000 + 1
	startTime := time.Date(2021, 3, 25, 12, 0, 0, 0, lite.MskTZ)

	// GetCourierOptions
	{
		request := lite.MakeRequest(
			lite.RequestWithStartTime(startTime),
			lite.RequestWithDeliveryType(pb.DeliveryType_COURIER),
			lite.RequestWithPartner(ffWarehouseID),
			lite.RequestWithRegion(geobase.RegionMoscow),
			lite.RequestWithGpsCoords(userLatitude, userLongitude),
			lite.RequestWithUserInfo(true),
			lite.RequestWithTotalPrice(totalPrice),
		)
		response, err := env.Client.GetCourierOptions(env.Ctx, request)
		require.NoError(t, err)

		const courierOptionCount = 5
		require.Len(t, response.Options, courierOptionCount)
		for i := 0; i < courierOptionCount; i++ {
			option := response.Options[i]
			requirepb.Equal(t, pb.DeliverySubtype_ORDINARY, option.DeliverySubtype)
			require.Equal(t, uint32(lastMileServiceID), option.DeliveryServiceId)
		}

		dsResp, dsErr := env.Client.GetCourierDeliveryStatistics(env.Ctx, request)
		require.NoError(t, dsErr)
		require.Len(t, dsResp.DeliveryMethods, 1)
		require.Equal(t, pb.DeliveryMethod_DM_COURIER, dsResp.DeliveryMethods[0])
	}

	// GetDeliveryRoute
	{
		request := lite.MakeRequest(
			lite.RequestWithStartTime(startTime),
			lite.RequestWithDeliveryType(pb.DeliveryType_COURIER),
			lite.RequestWithDeliverySubtype(pb.DeliverySubtype_ON_DEMAND),
			lite.RequestWithPartner(ffWarehouseID),
			lite.RequestWithRegion(geobase.RegionMoscow),
			lite.RequestWithGpsCoords(userLatitude, userLongitude),
			lite.RequestWithUserInfo(true),
			lite.RequestWithTotalPrice(totalPrice),
			lite.RequestWithDeliveryOption(
				&pb.Date{Year: 2021, Month: 3, Day: 30},
				&pb.Date{Year: 2021, Month: 3, Day: 30},
			),
			lite.RequestWithInterval(&pb.DeliveryInterval{From: &pb.Time{Hour: 10}, To: &pb.Time{Hour: 18}}),
		)
		_, err := env.Client.GetDeliveryRoute(env.Ctx, request)
		require.Error(t, err, "ON_DEMAND_PRICE_THRESHOLD")
	}
}

func TestDisabled(t *testing.T) {
	generationData := bg.GenerationData{
		RegionMap:     geobase.NewExample(),
		TariffsFinder: lite.NewFinderSet(MakeTariffsFinder()),
		Graph:         MakeOnDemandGraph(),
	}
	settings, _ := its.NewStringSettingsHolder(`{"enable_on_demand": false}`)
	env, cancel := lite.NewEnv(t, &generationData, settings)
	defer cancel()

	PrepareHTTPClient(env)

	const totalPrice = 5000
	startTime := time.Date(2021, 3, 25, 12, 0, 0, 0, lite.MskTZ)

	// GetCourierOptions
	{
		request := lite.MakeRequest(
			lite.RequestWithStartTime(startTime),
			lite.RequestWithDeliveryType(pb.DeliveryType_COURIER),
			lite.RequestWithPartner(ffWarehouseID),
			lite.RequestWithRegion(geobase.RegionMoscow),
			lite.RequestWithGpsCoords(userLatitude, userLongitude),
			lite.RequestWithUserInfo(true),
			lite.RequestWithTotalPrice(totalPrice),
		)
		response, err := env.Client.GetCourierOptions(env.Ctx, request)
		require.NoError(t, err)

		const courierOptionCount = 5
		require.Len(t, response.Options, courierOptionCount)
		for i := 0; i < courierOptionCount; i++ {
			option := response.Options[i]
			requirepb.Equal(t, pb.DeliverySubtype_ORDINARY, option.DeliverySubtype)
			require.Equal(t, uint32(lastMileServiceID), option.DeliveryServiceId)
		}
	}

	// GetDeliveryRoute
	{
		request := lite.MakeRequest(
			lite.RequestWithStartTime(startTime),
			lite.RequestWithDeliveryType(pb.DeliveryType_COURIER),
			lite.RequestWithDeliverySubtype(pb.DeliverySubtype_ON_DEMAND),
			lite.RequestWithPartner(ffWarehouseID),
			lite.RequestWithRegion(geobase.RegionMoscow),
			lite.RequestWithGpsCoords(userLatitude, userLongitude),
			lite.RequestWithUserInfo(true),
			lite.RequestWithTotalPrice(totalPrice),
			lite.RequestWithDeliveryOption(
				&pb.Date{Year: 2021, Month: 3, Day: 30},
				&pb.Date{Year: 2021, Month: 3, Day: 30},
			),
			lite.RequestWithInterval(&pb.DeliveryInterval{From: &pb.Time{Hour: 10}, To: &pb.Time{Hour: 18}}),
		)
		_, err := env.Client.GetDeliveryRoute(env.Ctx, request)
		require.Error(t, err, "ON_DEMAND_DISABLED")
	}
}
