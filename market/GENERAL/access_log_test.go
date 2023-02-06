package deferredcourierlite

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	"a.yandex-team.ru/market/combinator/pkg/deferredcourier"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/its"
	"a.yandex-team.ru/market/combinator/pkg/lite"
	"a.yandex-team.ru/market/combinator/pkg/outlets"
	"a.yandex-team.ru/market/combinator/pkg/units"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

func TestAccessLog(t *testing.T) {
	settings, _ := its.NewStringSettingsHolder("{}")
	regionMap := geobase.NewExample()
	graphEx, disabledDates := makeDeferredCourierGraph(settings)
	generationData := bg.GenerationData{
		RegionMap:           regionMap,
		TariffsFinder:       makeTariffsFinder(),
		Graph:               graphEx,
		DisabledDatesHashed: disabledDates,
		Outlets: outlets.Make([]outlets.Outlet{
			{
				ID:                         goPlatformPointLmsID,
				Type:                       outlets.Depot,
				PostCode:                   123321,
				GpsCoords:                  units.GpsCoords{Latitude: 55.722613, Longitude: 37.555378},
				RegionID:                   geobase.RegionID(geobase.RegionMoscow),
				DeferredCourierRadius:      200,
				IsDeferredCourierAvailable: true,
			},
			{
				ID:                         goPlatformDarkPointLmsID,
				Type:                       outlets.Depot,
				PostCode:                   123321,
				RegionID:                   geobase.RegionID(geobase.RegionMoscow),
				GpsCoords:                  units.GpsCoords{Latitude: 55.122613, Longitude: 37.855378},
				DeferredCourierRadius:      200,
				IsDeferredCourierAvailable: true,
				IsDarkStore:                true,
			},
		}, &regionMap, nil),
	}
	generationData.TariffsFinder.Common.PointRegionMap = map[int64]geobase.RegionID{
		goPlatformDarkPointLmsID: geobase.RegionMoscow,
		goPlatformPointLmsID:     geobase.RegionMoscow,
	}
	env, cancel := lite.NewEnv(t, &generationData, settings)
	defer cancel()
	PrepareHTTPClient(env)

	makeCourierOptionsRequest := func(warehouseID int64, latDelta float64) *pb.DeliveryRequest {
		return lite.MakeRequest(
			lite.RequestWithStartTime(time.Date(2021, 6, 4, 12, 0, 0, 0, lite.MskTZ)),
			lite.RequestWithDeliveryType(pb.DeliveryType_COURIER),
			lite.RequestWithPartner(ffWarehouseID),
			lite.RequestWithRegion(geobase.RegionMoscow),
			lite.RequestWithRearrFactors("market_combinator_deferred_courier_options=1"),
			lite.RequestWithGpsCoords(55.722613+latDelta, 37.555378),
			lite.RequestWithUserInfo(true),
		)
	}

	makeDeliveryRouteRequest := func(warehouseID int64, latDelta float64) *pb.DeliveryRequest {
		return lite.MakeRequest(
			lite.RequestWithStartTime(time.Date(2021, 6, 4, 12, 0, 0, 0, lite.MskTZ)),
			lite.RequestWithDeliveryType(pb.DeliveryType_COURIER),
			lite.RequestWithDeliverySubtype(pb.DeliverySubtype_DEFERRED_COURIER),
			lite.RequestWithPartner(ffWarehouseID),
			lite.RequestWithRegion(geobase.RegionMoscow),
			lite.RequestWithRearrFactors("market_combinator_deferred_courier_options=1"),
			lite.RequestWithGpsCoords(55.722613+latDelta, 37.555378),
			lite.RequestWithDeliveryOption(
				&pb.Date{Year: 2021, Month: 6, Day: 6},
				&pb.Date{Year: 2021, Month: 6, Day: 6},
			),
			lite.RequestWithInterval(&pb.DeliveryInterval{
				From: &pb.Time{Hour: 13},
				To:   &pb.Time{Hour: 14},
			}),
			lite.RequestWithUserInfo(true),
		)
	}

	checkLastMessage := func(t *testing.T, env *lite.Env, code deferredcourier.ErrorCode) {
		logs := env.AccessLog.All()
		lastMessageFields := logs[len(logs)-1].ContextMap()
		require.Equal(t, string(code), lastMessageFields["deferred_courier_error_code"])
	}

	// GetCourierOptions
	{
		const courierOptionCount = 5
		const deferredCourierOptionCount = 40
		req := makeCourierOptionsRequest(ffWarehouseID, 1)
		resp, err := env.Client.GetCourierOptions(env.Ctx, req)
		require.NoError(t, err)
		require.NotNil(t, resp)
		checkLastMessage(t, env, deferredcourier.OutletsNotFound)

		req = makeCourierOptionsRequest(ffWarehouseID, 0)
		resp, err = env.Client.GetCourierOptions(env.Ctx, req)
		require.NoError(t, err)
		require.NotNil(t, resp)
		require.Len(t, resp.Options, courierOptionCount+deferredCourierOptionCount)
		checkLastMessage(t, env, deferredcourier.OK)
	}

	//GetDeliveryRoute
	{
		req := makeDeliveryRouteRequest(ffWarehouseID, 1)
		_, err := env.Client.GetDeliveryRoute(env.Ctx, req)
		require.Error(t, err, deferredcourier.ErrOutletsNotFound)
		checkLastMessage(t, env, deferredcourier.OutletsNotFound)

		req = makeDeliveryRouteRequest(ffWarehouseID, 0)
		resp, err := env.Client.GetDeliveryRoute(env.Ctx, req)
		require.NoError(t, err)
		require.NotNil(t, resp.Route)
		//checkLastMessage(t, env, deferredcourier.OK)
	}
}
