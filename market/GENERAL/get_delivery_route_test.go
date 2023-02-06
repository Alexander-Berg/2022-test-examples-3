package deferredcourierlite

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/test/requirepb"
	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	"a.yandex-team.ru/market/combinator/pkg/its"
	"a.yandex-team.ru/market/combinator/pkg/lite"
	"a.yandex-team.ru/market/combinator/pkg/outlets"
	"a.yandex-team.ru/market/combinator/pkg/units"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

func TestGetDeferredCourierDeliveryRoute(t *testing.T) {
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
	{ //Подтверждаем опцию и отдаем маршрут. Проверка даркстора
		startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)
		req := lite.MakeRequest(
			lite.RequestWithStartTime(startTime),
			lite.RequestWithDeliveryType(pb.DeliveryType_COURIER),
			lite.RequestWithDeliverySubtype(pb.DeliverySubtype_DEFERRED_COURIER),
			lite.RequestWithDeliveryOption(
				&pb.Date{Day: uint32(startTime.Day() + 2),
					Month: uint32(startTime.Month()),
					Year:  uint32(startTime.Year())},
				&pb.Date{Day: uint32(startTime.Day() + 2),
					Month: uint32(startTime.Month()),
					Year:  uint32(startTime.Year())}),
			lite.RequestWithInterval(&pb.DeliveryInterval{
				From: &pb.Time{
					Hour:   12,
					Minute: 0,
				},
				To: &pb.Time{
					Hour:   13,
					Minute: 0,
				},
			}),
			lite.RequestWithGpsCoords(55.122613, 37.855378), //Пользователь в радиусе даркстора
			lite.RequestWithPartner(ffWarehouseID),
			lite.RequestWithRegion(geobase.RegionMoscow),
			lite.RequestWithRearrFactors("market_combinator_deferred_courier_options=1;use_yandex_go_in_deferred_courier=0"),
			lite.RequestWithUserInfo(true),
		)
		env, cancel := lite.NewEnv(t, &generationData, nil)
		defer cancel()
		PrepareHTTPClient(env)

		wantDateFrom := startTime.Add(time.Hour * 2 * 24)
		wantDateTo := startTime.Add(time.Hour * 2 * 24)

		resp, err := env.Client.GetDeliveryRoute(env.Ctx, req)

		require.NoError(t, err)
		require.NotNil(t, resp.Route)
		require.Equal(t, pb.DeliverySubtype_DEFERRED_COURIER, resp.DeliverySubtype)
		require.Equal(t, 50, int(resp.Route.CostForShop))
		requirepb.Equal(t, lite.ToProtoDate(wantDateFrom), resp.Route.DateFrom)
		requirepb.Equal(t, lite.ToProtoDate(wantDateTo), resp.Route.DateTo)

		deferredCourierRouteNodes := []int64{
			ffWarehouseSegmentID,
			lastMileMovementSegmentID,
			lastMileLinehaulSegmentID,
			lastMilePickupDarkSegmentID,
			goPlatformDarkSegmentID,
		}
		deferredCourierRouteLength := len(deferredCourierRouteNodes)
		deferredCourierRouteEdges := make([][]int64, 0)
		for i := 1; i < deferredCourierRouteLength; i++ {
			deferredCourierRouteEdges = append(deferredCourierRouteEdges, []int64{int64(i - 1), int64(i)})
		}

		require.Equal(t, deferredCourierRouteLength, len(resp.Route.Points))
		require.Equal(t, deferredCourierRouteLength-1, len(resp.Route.Paths))
		for _, n := range resp.Route.Points {
			require.Contains(t, deferredCourierRouteNodes, int64(n.SegmentId))
		}
		for _, e := range resp.Route.Paths {
			require.Contains(t, deferredCourierRouteEdges, []int64{int64(e.PointFrom), int64(e.PointTo)})
		}
	}
	{ //Подтверждаем опцию и отдаем маршрут. Проверка лавки
		startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)
		req := lite.MakeRequest(
			lite.RequestWithStartTime(startTime),
			lite.RequestWithDeliveryType(pb.DeliveryType_COURIER),
			lite.RequestWithDeliverySubtype(pb.DeliverySubtype_DEFERRED_COURIER),
			lite.RequestWithDeliveryOption(
				&pb.Date{Day: uint32(startTime.Day() + 2),
					Month: uint32(startTime.Month()),
					Year:  uint32(startTime.Year())},
				&pb.Date{Day: uint32(startTime.Day() + 2),
					Month: uint32(startTime.Month()),
					Year:  uint32(startTime.Year())}),
			lite.RequestWithInterval(&pb.DeliveryInterval{
				From: &pb.Time{
					Hour:   12,
					Minute: 0,
				},
				To: &pb.Time{
					Hour:   13,
					Minute: 0,
				},
			}),
			lite.RequestWithGpsCoords(55.722613, 37.555378), //Пользователь в радиусе лавки
			lite.RequestWithPartner(ffWarehouseID),
			lite.RequestWithRegion(geobase.RegionMoscow),
			lite.RequestWithRearrFactors("market_combinator_deferred_courier_options=1;use_yandex_go_in_deferred_courier=1;"),
			lite.RequestWithUserInfo(true),
		)

		env, cancel := lite.NewEnv(t, &generationData, nil)
		defer cancel()
		PrepareHTTPClient(env)

		wantDateFrom := startTime.Add(time.Hour * 2 * 24)
		wantDateTo := startTime.Add(time.Hour * 2 * 24)

		resp, err := env.Client.GetDeliveryRoute(env.Ctx, req)

		require.NoError(t, err)
		require.NotNil(t, resp.Route)
		require.Equal(t, pb.DeliverySubtype_DEFERRED_COURIER, resp.DeliverySubtype)
		require.Equal(t, 50, int(resp.Route.CostForShop))
		requirepb.Equal(t, lite.ToProtoDate(wantDateFrom), resp.Route.DateFrom)
		requirepb.Equal(t, lite.ToProtoDate(wantDateTo), resp.Route.DateTo)

		deferredCourierRouteNodes := []int64{
			ffWarehouseSegmentID,
			lastMileMovementSegmentID,
			lastMileLinehaulSegmentID,
			lastMilePickupSegmentID,
			goPlatformSegmentID,
		}
		deferredCourierRouteLength := len(deferredCourierRouteNodes)
		deferredCourierRouteEdges := make([][]int64, 0)
		for i := 1; i < deferredCourierRouteLength; i++ {
			deferredCourierRouteEdges = append(deferredCourierRouteEdges, []int64{int64(i - 1), int64(i)})
		}

		require.Equal(t, deferredCourierRouteLength, len(resp.Route.Points))
		require.Equal(t, deferredCourierRouteLength-1, len(resp.Route.Paths))
		for _, n := range resp.Route.Points {
			require.Contains(t, deferredCourierRouteNodes, int64(n.SegmentId))
		}
		for _, e := range resp.Route.Paths {
			require.Contains(t, deferredCourierRouteEdges, []int64{int64(e.PointFrom), int64(e.PointTo)})
		}
	}
	{ //Выключен rear флаг. Отдаем ошибку
		startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)
		req := lite.MakeRequest(
			lite.RequestWithStartTime(startTime),
			lite.RequestWithDeliveryType(pb.DeliveryType_COURIER),
			lite.RequestWithDeliverySubtype(pb.DeliverySubtype_DEFERRED_COURIER),
			lite.RequestWithDeliveryOption(
				&pb.Date{Day: uint32(startTime.Day() + 1),
					Month: uint32(startTime.Month()),
					Year:  uint32(startTime.Year())},
				&pb.Date{Day: uint32(startTime.Day() + 1),
					Month: uint32(startTime.Month()),
					Year:  uint32(startTime.Year())}),
			lite.RequestWithInterval(&pb.DeliveryInterval{
				From: &pb.Time{
					Hour:   12,
					Minute: 0,
				},
				To: &pb.Time{
					Hour:   13,
					Minute: 0,
				},
			}),
			lite.RequestWithGpsCoords(55.122613, 37.855378), //Пользователь в радиусе даркстора
			lite.RequestWithPartner(ffWarehouseID),
			lite.RequestWithRegion(geobase.RegionMoscow),
			lite.RequestWithRearrFactors("market_combinator_deferred_courier_options=0"),
			lite.RequestWithUserInfo(true),
		)

		env, cancel := lite.NewEnv(t, &generationData, nil)
		defer cancel()

		_, err := env.Client.GetDeliveryRoute(env.Ctx, req)
		require.Error(t, err)
		require.Equal(t, "rpc error: code = Unknown desc = DEFERRED_COURIER_DISABLED", err.Error())
	}
	{ //Не нашлось запрашиваемого интервала доставки
		startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)
		req := lite.MakeRequest(
			lite.RequestWithStartTime(startTime),
			lite.RequestWithDeliveryType(pb.DeliveryType_COURIER),
			lite.RequestWithDeliverySubtype(pb.DeliverySubtype_DEFERRED_COURIER),
			lite.RequestWithDeliveryOption(
				&pb.Date{Day: uint32(startTime.Day() + 1),
					Month: uint32(startTime.Month()),
					Year:  uint32(startTime.Year())},
				&pb.Date{Day: uint32(startTime.Day() + 1),
					Month: uint32(startTime.Month()),
					Year:  uint32(startTime.Year())}),
			lite.RequestWithInterval(&pb.DeliveryInterval{
				From: &pb.Time{
					Hour:   9,
					Minute: 0,
				},
				To: &pb.Time{
					Hour:   10,
					Minute: 0,
				},
			}),
			lite.RequestWithGpsCoords(55.122613, 37.855378), //Пользователь в радиусе даркстора
			lite.RequestWithPartner(ffWarehouseID),
			lite.RequestWithRegion(geobase.RegionMoscow),
			lite.RequestWithRearrFactors("market_combinator_deferred_courier_options=1;use_yandex_go_in_deferred_courier=1"),
			lite.RequestWithUserInfo(true),
		)

		env, cancel := lite.NewEnv(t, &generationData, nil)
		defer cancel()

		_, err := env.Client.GetDeliveryRoute(env.Ctx, req)
		require.Error(t, err)
		require.Equal(t, "rpc error: code = Unknown desc = no courier route, t:40 i:0 ds:0", err.Error())
	}
	{ //Если в ПВЗ не настроены две волны, то ожидаем на handing сервисе pickup сегмента пустой слайс интервалов
		startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)
		req := lite.MakeRequest(
			lite.RequestWithStartTime(startTime),
			lite.RequestWithDeliveryType(pb.DeliveryType_COURIER),
			lite.RequestWithDeliverySubtype(pb.DeliverySubtype_DEFERRED_COURIER),
			lite.RequestWithDeliveryOption(
				&pb.Date{Day: uint32(startTime.Day() + 2),
					Month: uint32(startTime.Month()),
					Year:  uint32(startTime.Year())},
				&pb.Date{Day: uint32(startTime.Day() + 2),
					Month: uint32(startTime.Month()),
					Year:  uint32(startTime.Year())}),
			lite.RequestWithInterval(&pb.DeliveryInterval{
				From: &pb.Time{
					Hour:   13,
					Minute: 0,
				},
				To: &pb.Time{
					Hour:   14,
					Minute: 0,
				},
			}),
			lite.RequestWithGpsCoords(55.122613, 37.855378), //Пользователь в радиусе даркстора
			lite.RequestWithPartner(ffWarehouseID),
			lite.RequestWithRegion(geobase.RegionMoscow),
			lite.RequestWithRearrFactors("market_combinator_deferred_courier_options=1;use_yandex_go_in_deferred_courier=0;"),
			lite.RequestWithUserInfo(true),
		)

		env, cancel := lite.NewEnv(t, &generationData, nil)
		defer cancel()
		PrepareHTTPClient(env)

		resp, err := env.Client.GetDeliveryRoute(env.Ctx, req)

		require.NoError(t, err)
		require.NotNil(t, resp.Route)

		require.Equal(t, pb.DeliverySubtype_DEFERRED_COURIER, resp.DeliverySubtype)
		serviceWasChecked := 0
		for _, route := range resp.Route.Points {
			if route.SegmentType == graph.SegmentTypePickup.String() {
				for _, service := range route.Services {
					if service.Code == enums.ServiceHanding.String() {
						require.Len(t, service.DeliveryIntervals, 0)
						serviceWasChecked += 1
					}
				}
			}
			if route.SegmentType == graph.SegmentTypeGoPlatform.String() {
				for _, service := range route.Services {
					if service.Code == enums.ServiceHanding.String() {
						require.Len(t, service.DeliveryIntervals, 1)
						requirepb.Equal(t,
							req.Option.Interval,
							service.DeliveryIntervals[0],
						)
						serviceWasChecked += 1
					}
				}
			}
		}
		require.Equal(t, 2, serviceWasChecked)
	}
}
