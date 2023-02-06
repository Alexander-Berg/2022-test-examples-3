package ondemandlite

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/its"
	"a.yandex-team.ru/market/combinator/pkg/lite"
	"a.yandex-team.ru/market/combinator/pkg/ondemand"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

func TestAccessLog(t *testing.T) {
	generationData := bg.GenerationData{
		RegionMap:     geobase.NewExample(),
		TariffsFinder: lite.NewFinderSet(MakeTariffsFinder()),
		Graph:         MakeOnDemandGraph(),
	}
	settings, _ := its.NewStringSettingsHolder("{}")
	env, cancel := lite.NewEnv(t, &generationData, settings)
	defer cancel()

	PrepareHTTPClient(env)

	makeCourierOptionsRequest := func(warehouseID int64, latDelta float64) *pb.DeliveryRequest {
		return lite.MakeRequest(
			lite.RequestWithStartTime(time.Date(2021, 3, 25, 12, 0, 0, 0, lite.MskTZ)),
			lite.RequestWithDeliveryType(pb.DeliveryType_COURIER),
			lite.RequestWithPartner(warehouseID),
			lite.RequestWithRegion(geobase.RegionMoscow),
			lite.RequestWithGpsCoords(userLatitude+latDelta, userLongitude),
			lite.RequestWithUserInfo(true),
		)
	}

	makeDeliveryRouteRequest := func(warehouseID int64, latDelta float64) *pb.DeliveryRequest {
		return lite.MakeRequest(
			lite.RequestWithStartTime(time.Date(2021, 3, 25, 12, 0, 0, 0, lite.MskTZ)),
			lite.RequestWithDeliveryType(pb.DeliveryType_COURIER),
			lite.RequestWithDeliverySubtype(pb.DeliverySubtype_ON_DEMAND),
			lite.RequestWithPartner(warehouseID),
			lite.RequestWithRegion(geobase.RegionMoscow),
			lite.RequestWithGpsCoords(userLatitude+latDelta, userLongitude),
			lite.RequestWithUserInfo(true),
			lite.RequestWithDeliveryOption(
				&pb.Date{Year: 2021, Month: 3, Day: 29},
				&pb.Date{Year: 2021, Month: 3, Day: 29},
			),
			lite.RequestWithInterval(&pb.DeliveryInterval{
				From: &pb.Time{Hour: 10},
				To:   &pb.Time{Hour: 18},
			}),
		)
	}

	checkLastMessage := func(t *testing.T, env *lite.Env, code ondemand.ErrorCode) {
		logs := env.AccessLog.All()
		lastMessageFields := logs[len(logs)-1].ContextMap()
		require.Equal(t, string(code), lastMessageFields["on_demand_error_code"])
	}

	// GetCourierOptions
	{
		const courierOptionCount = 5
		onDemandOptionsCount := len(yTaxiAvailableOptions)

		req := makeCourierOptionsRequest(nonOnDemandWarehouseID, 0)
		resp, err := env.Client.GetCourierOptions(env.Ctx, req)
		require.NoError(t, err)
		require.NotNil(t, resp)
		checkLastMessage(t, env, ondemand.NoOptionsFound)

		req = makeCourierOptionsRequest(ffWarehouseID, 1)
		resp, err = env.Client.GetCourierOptions(env.Ctx, req)
		require.NoError(t, err)
		require.NotNil(t, resp)
		require.Len(t, resp.Options, courierOptionCount)
		checkLastMessage(t, env, ondemand.TaxiRequestTimeout)

		req = makeCourierOptionsRequest(ffWarehouseID, 0)
		resp, err = env.Client.GetCourierOptions(env.Ctx, req)
		require.NoError(t, err)
		require.NotNil(t, resp)
		require.Len(t, resp.Options, courierOptionCount+onDemandOptionsCount)
		checkLastMessage(t, env, ondemand.OK)
	}

	// GetDeliveryRoute
	{
		req := makeDeliveryRouteRequest(nonOnDemandWarehouseID, 0)
		_, err := env.Client.GetDeliveryRoute(env.Ctx, req)
		require.Error(t, err, ondemand.ErrNoOptionsFound)
		checkLastMessage(t, env, ondemand.NoOptionsFound)

		req = makeDeliveryRouteRequest(ffWarehouseID, 1)
		_, err = env.Client.GetDeliveryRoute(env.Ctx, req)
		require.Error(t, err, ondemand.ErrTaxiRequestTimeout)
		checkLastMessage(t, env, ondemand.TaxiRequestTimeout)

		req = makeDeliveryRouteRequest(ffWarehouseID, 0)
		resp, err := env.Client.GetDeliveryRoute(env.Ctx, req)
		require.NoError(t, err)
		require.NotNil(t, resp.Route)
		checkLastMessage(t, env, ondemand.OK)
	}
}
