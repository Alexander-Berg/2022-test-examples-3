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

func testCourierRoute(t *testing.T, env *lite.Env) {
	startTime := time.Date(2021, 3, 25, 12, 0, 0, 0, lite.MskTZ)
	request := lite.MakeRequest(
		lite.RequestWithStartTime(startTime),
		lite.RequestWithDeliveryType(pb.DeliveryType_COURIER),
		lite.RequestWithDeliverySubtype(pb.DeliverySubtype_ORDINARY),
		lite.RequestWithPartner(ffWarehouseID),
		lite.RequestWithRegion(geobase.RegionMoscow),
		lite.RequestWithGpsCoords(userLatitude, userLongitude),
		lite.RequestWithUserInfo(true),
		lite.RequestWithDeliveryOption(
			&pb.Date{Year: 2021, Month: 3, Day: 28},
			&pb.Date{Year: 2021, Month: 3, Day: 28},
		),
		lite.RequestWithInterval(&pb.DeliveryInterval{From: &pb.Time{Hour: 16}, To: &pb.Time{Hour: 22}}),
	)

	response, err := env.Client.GetDeliveryRoute(env.Ctx, request)
	require.NoError(t, err)
	require.Equal(t, pb.DeliverySubtype_ORDINARY, response.DeliverySubtype)
	require.NotNil(t, response.Route)

	courierRouteNodes := []int64{
		ffWarehouseSegmentID,
		lastMileMovementSegmentID,
		lastMileLinehaulSegmentID,
		lastMileHandingSegmentID,
	}
	courierRouteLength := len(courierRouteNodes)
	courierRouteEdges := make([][]int64, 0)
	for i := 1; i < courierRouteLength; i++ {
		courierRouteEdges = append(courierRouteEdges, []int64{int64(i - 1), int64(i)})
	}

	route := response.Route
	require.Equal(t, courierRouteLength, len(route.Points))
	require.Equal(t, courierRouteLength-1, len(route.Paths))
	for _, n := range route.Points {
		require.Contains(t, courierRouteNodes, int64(n.SegmentId))
	}
	for _, e := range route.Paths {
		require.Contains(t, courierRouteEdges, []int64{int64(e.PointFrom), int64(e.PointTo)})
	}
	requirepb.Equal(t, &pb.Date{Year: 2021, Month: 3, Day: 28}, route.DateFrom)
	requirepb.Equal(t, &pb.Date{Year: 2021, Month: 3, Day: 28}, route.DateTo)
}

func TestGetOnDemandRoute(t *testing.T) {
	generationData := bg.GenerationData{
		RegionMap:     geobase.NewExample(),
		TariffsFinder: lite.NewFinderSet(MakeTariffsFinder()),
		Graph:         MakeOnDemandGraph(),
	}
	settings, _ := its.NewStringSettingsHolder("{}")
	env, cancel := lite.NewEnv(t, &generationData, settings)
	defer cancel()

	PrepareHTTPClient(env)

	startTime := time.Date(2021, 3, 25, 12, 0, 0, 0, lite.MskTZ)

	// Delivery option confirmed by Yandex.Go
	{
		request := lite.MakeRequest(
			lite.RequestWithStartTime(startTime),
			lite.RequestWithDeliveryType(pb.DeliveryType_COURIER),
			lite.RequestWithDeliverySubtype(pb.DeliverySubtype_ON_DEMAND),
			lite.RequestWithPartner(ffWarehouseID),
			lite.RequestWithRegion(geobase.RegionMoscow),
			lite.RequestWithGpsCoords(userLatitude, userLongitude),
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
		response, err := env.Client.GetDeliveryRoute(env.Ctx, request)
		require.NoError(t, err)
		require.Equal(t, pb.DeliverySubtype_ON_DEMAND, response.DeliverySubtype)
		require.NotNil(t, response.Route)

		onDemandRouteNodes := []int64{
			ffWarehouseSegmentID,
			middleMileServiceSegmentID,
			yTaxiWarehouseSegmentID,
			yTaxiMovementSegmentID,
			yTaxiLinehaulSegmentID,
			yTaxiHandingSegmentID,
		}
		onDemandRouteLength := len(onDemandRouteNodes)
		onDemandRouteEdges := make([][]int64, 0)
		for i := 1; i < onDemandRouteLength; i++ {
			onDemandRouteEdges = append(onDemandRouteEdges, []int64{int64(i - 1), int64(i)})
		}

		route := response.Route
		require.Equal(t, onDemandRouteLength, len(route.Points))
		require.Equal(t, onDemandRouteLength-1, len(route.Paths))
		for _, n := range route.Points {
			require.Contains(t, onDemandRouteNodes, int64(n.SegmentId))
		}
		for _, e := range route.Paths {
			require.Contains(t, onDemandRouteEdges, []int64{int64(e.PointFrom), int64(e.PointTo)})
		}
		requirepb.Equal(t, &pb.Date{Year: 2021, Month: 3, Day: 29}, route.DateFrom)
		requirepb.Equal(t, &pb.Date{Year: 2021, Month: 3, Day: 29}, route.DateTo)
	}

	// Delivery option not confirmed by Yandex.Go
	{
		request := lite.MakeRequest(
			lite.RequestWithStartTime(startTime),
			lite.RequestWithDeliveryType(pb.DeliveryType_COURIER),
			lite.RequestWithDeliverySubtype(pb.DeliverySubtype_ON_DEMAND),
			lite.RequestWithPartner(ffWarehouseID),
			lite.RequestWithRegion(geobase.RegionMoscow),
			lite.RequestWithGpsCoords(userLatitude, userLongitude),
			lite.RequestWithUserInfo(true),
			lite.RequestWithDeliveryOption(
				&pb.Date{Year: 2021, Month: 3, Day: 28},
				&pb.Date{Year: 2021, Month: 3, Day: 28},
			),
			lite.RequestWithInterval(&pb.DeliveryInterval{From: &pb.Time{Hour: 10}, To: &pb.Time{Hour: 18}}),
		)
		_, err := env.Client.GetDeliveryRoute(env.Ctx, request)
		require.Error(t, err, "no courier route, t:%d i:0 ds: 0 c:0", len(yTaxiAvailableOptions))
	}

	// Courier delivery option
	testCourierRoute(t, env)
}

func requireEqual(t *testing.T, expected, actual *pb.SupplierDeliveryDates) {
	require.NotNil(t, expected)
	require.NotNil(t, actual)

	requirepb.Equal(t, expected.GetWarehouseId(), actual.GetWarehouseId())
	requirepb.Equal(t, expected.GetProcessingStartTime(), actual.GetProcessingStartTime())
	requirepb.Equal(t, expected.GetShipmentBySupplier(), actual.GetShipmentBySupplier())
	requirepb.Equal(t, expected.GetReceptionByWarehouse(), actual.GetReceptionByWarehouse())
}

func TestDeliveryDates(t *testing.T) {
	generationData := bg.GenerationData{
		RegionMap:     geobase.NewExample(),
		TariffsFinder: lite.NewFinderSet(MakeTariffsFinder()),
		Graph:         MakeOnDemandGraph(),
	}
	settings, _ := its.NewStringSettingsHolder("{}")
	env, cancel := lite.NewEnv(t, &generationData, settings)
	defer cancel()

	PrepareHTTPClient(env)

	startTime := time.Date(2021, 3, 25, 12, 0, 0, 0, lite.MskTZ)
	makeRequest := func(subtype pb.DeliverySubtype, interval *pb.DeliveryInterval) *pb.DeliveryRequest {
		return lite.MakeRequest(
			lite.RequestWithStartTime(startTime),
			lite.RequestWithDeliveryType(pb.DeliveryType_COURIER),
			lite.RequestWithPartner(cdWarehouseID),
			lite.RequestWithRegion(geobase.RegionMoscow),
			lite.RequestWithGpsCoords(userLatitude, userLongitude),
			lite.RequestWithUserInfo(true),
			lite.RequestWithDeliveryOption(
				&pb.Date{Year: 2021, Month: 3, Day: 26},
				&pb.Date{Year: 2021, Month: 3, Day: 26},
			),
			lite.RequestWithDeliverySubtype(subtype),
			lite.RequestWithInterval(interval),
		)
	}

	req := makeRequest(pb.DeliverySubtype_ORDINARY, &pb.DeliveryInterval{From: &pb.Time{Hour: 16}, To: &pb.Time{Hour: 22}})
	resp, err := env.Client.GetDeliveryRoute(env.Ctx, req)
	require.NoError(t, err)
	require.Equal(t, pb.DeliverySubtype_ORDINARY, resp.DeliverySubtype)
	require.NotNil(t, resp.DeliveryDates)
	courierResponse := resp.GetDeliveryDates()

	req = makeRequest(pb.DeliverySubtype_ON_DEMAND, &pb.DeliveryInterval{From: &pb.Time{Hour: 10}, To: &pb.Time{Hour: 18}})
	resp, err = env.Client.GetDeliveryRoute(env.Ctx, req)
	require.NoError(t, err)
	require.Equal(t, pb.DeliverySubtype_ON_DEMAND, resp.DeliverySubtype)
	require.NotNil(t, resp.DeliveryDates)
	onDemandResponse := resp.GetDeliveryDates()

	requirepb.Equal(t, courierResponse.GetShipmentDate(), onDemandResponse.GetShipmentDate())
	requirepb.Equal(t, courierResponse.GetShipmentDay(), onDemandResponse.GetShipmentDay())
	requirepb.Equal(t, courierResponse.GetPackagingTime(), onDemandResponse.GetPackagingTime())
	requirepb.Equal(t, courierResponse.GetShipmentBySupplier(), onDemandResponse.GetShipmentBySupplier())
	requirepb.Equal(t, courierResponse.GetReceptionByWarehouse(), onDemandResponse.GetReceptionByWarehouse())

	courierSDL := courierResponse.GetSupplierDeliveryList()
	onDemandSDL := onDemandResponse.GetSupplierDeliveryList()
	require.Equal(t, len(courierSDL), len(onDemandSDL))
	for i, sd := range courierSDL {
		requireEqual(t, sd, onDemandSDL[i])
	}

	courierLWO := courierResponse.GetLastWarehouseOffset()
	onDemandLWO := onDemandResponse.GetLastWarehouseOffset()
	require.NotNil(t, courierLWO)
	require.NotNil(t, onDemandLWO)
	requirepb.Equal(t, courierLWO.GetWarehousePosition(), onDemandLWO.GetWarehousePosition())
	requirepb.Equal(t, courierLWO.GetOffset(), onDemandLWO.GetOffset())
}
