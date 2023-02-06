package expresslite

import (
	"fmt"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	"a.yandex-team.ru/market/combinator/pkg/hardconfig"
	"a.yandex-team.ru/market/combinator/pkg/its"
	"a.yandex-team.ru/market/combinator/pkg/lite"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

const (
	intermediateReturnSCPartner  = 107
	intermediateReturnSCPoint    = 10000999
	intermediateReturnSCPointStr = "10000999"

	returnSCPartner = 108
	returnSCPoint   = 10000998
)

func TestReturnSC(t *testing.T) {
	hardconfig.GetHardConfig().PartnersExpList = map[string]map[int64]bool{
		"express_return_sc": {
			intermediateReturnSCPartner: true,
			returnSCPoint:               true,
		},
	}
	makeInterval := func(hourFrom, minuteFrom, hourTo, minuteTo uint32) *pb.DeliveryInterval {
		return &pb.DeliveryInterval{
			From: &pb.Time{Hour: hourFrom, Minute: minuteFrom},
			To:   &pb.Time{Hour: hourTo, Minute: minuteTo},
		}
	}
	scheduleFrom := &pb.Time{Hour: 10, Minute: 00}
	scheduleTo := &pb.Time{Hour: 20, Minute: 00}

	g := makeExpressReturnGraph(
		fmt.Sprintf("%02d:%02d:00", scheduleFrom.Hour, scheduleFrom.Minute),
		fmt.Sprintf("%02d:%02d:00", scheduleTo.Hour, scheduleTo.Minute),
	)

	generation := &bg.GenerationData{
		RegionMap:                regionMap,
		Graph:                    g,
		Express:                  makeExpress(),
		TariffsFinder:            makeTariffsFinder(),
		ExpressIntervalValidator: makeExpressIntervalValidator(),
	}
	generation.Express.Warehouses[expressWarehouseID].ReturnSCPartnerID = returnSCPartner
	returnSC := g.PartnerToSegments[returnSCPartner][0]
	intermediateReturnSC := g.PartnerToSegments[intermediateReturnSCPartner][0]
	backM := g.BackwardNeighbors[intermediateReturnSC.ID][0].ID

	settings, _ := its.NewStringSettingsHolder("{}")
	env, cancel := lite.NewEnv(t, generation, settings)
	defer cancel()
	startTime := time.Date(2022, 1, 27, 0, 0, 0, 0, lite.MskTZ)
	req := lite.MakeRequest(
		lite.RequestWithStartTime(startTime),
		lite.RequestWithDeliveryType(pb.DeliveryType_COURIER),
		lite.RequestWithPartner(expressWarehouseID),
		lite.RequestWithRegion(geobase.RegionMoscow),
		lite.RequestWithGpsCoords(userLatitude, userLongitude),
		lite.RequestWithTotalPrice(totalPrice),
		lite.RequestWithShopID(0),
	)

	resp, err := env.Client.GetCourierOptions(env.Ctx, req)
	require.NoError(t, err)
	require.Len(t, resp.GetOptions(), 22)

	// Заканчиваем сборку в 15:40, до закрытия мерча 4:20
	// значит в мета информации не должно быть информации о возвратном СЦ
	reqRoute := makeDeliveryRouteReq(
		startTime,
		makeInterval(16, 00, 16, 40),
		&pb.Date{
			Day:   uint32(startTime.Day()),
			Month: uint32(startTime.Month()),
			Year:  uint32(startTime.Year()),
		},
	)
	respRoute, err := env.Client.GetDeliveryRoute(env.Ctx, reqRoute)
	require.NoError(t, err)
	containsMeta(
		t,
		respRoute.Route.Points,
		enums.ServiceHanding,
		false,
		graph.ReturnSCTag,
		intermediateReturnSCPointStr,
	)

	// Заканчиваем сборку в 16:40, до закрытия мерча 3:20
	// значит в мета информации не должно быть информации о возвратном СЦ
	reqRoute = makeDeliveryRouteReq(
		startTime,
		makeInterval(17, 00, 17, 40),
		&pb.Date{
			Day:   uint32(startTime.Day()),
			Month: uint32(startTime.Month()),
			Year:  uint32(startTime.Year()),
		},
	)
	respRoute, err = env.Client.GetDeliveryRoute(env.Ctx, reqRoute)
	require.NoError(t, err)
	containsMeta(
		t,
		respRoute.Route.Points,
		enums.ServiceHanding,
		false,
		graph.ReturnSCTag,
		intermediateReturnSCPointStr,
	)

	// Заканчиваем сборку в 17:40, до закрытия мерча 2:20
	// значит в мета информации должна быть информации о возвратном СЦ
	reqRoute = makeDeliveryRouteReq(
		startTime,
		makeInterval(18, 00, 18, 40),
		&pb.Date{
			Day:   uint32(startTime.Day()),
			Month: uint32(startTime.Month()),
			Year:  uint32(startTime.Year()),
		},
	)
	respRoute, err = env.Client.GetDeliveryRoute(env.Ctx, reqRoute)
	require.NoError(t, err)
	containsMeta(
		t,
		respRoute.Route.Points,
		enums.ServiceHanding,
		true,
		graph.ReturnSCTag,
		intermediateReturnSCPointStr,
	)

	reqReturnRoute := &pb.ReturnRouteRequest{
		From: &pb.ReturnRouteRequest_ReturnRoutePoint{
			PartnerId: intermediateReturnSCPartner,
		},
		To: &pb.ReturnRouteRequest_ReturnRoutePoint{
			PartnerId: expressWarehouseID,
		},
	}

	respReturnRoute, err := env.Client.GetReturnRoute(env.Ctx, reqReturnRoute)
	require.NoError(t, err)
	require.Len(t, respReturnRoute.Points, 3)
	require.Equal(t, respReturnRoute.Points[0].SegmentId, uint64(intermediateReturnSC.ID))
	require.Equal(t, respReturnRoute.Points[1].SegmentId, uint64(backM))
	require.Equal(t, respReturnRoute.Points[2].SegmentId, uint64(returnSC.ID))

	reqReturnRoute = &pb.ReturnRouteRequest{
		From: &pb.ReturnRouteRequest_ReturnRoutePoint{
			PartnerId: returnSCPartner,
		},
		To: &pb.ReturnRouteRequest_ReturnRoutePoint{
			PartnerId: expressWarehouseID,
		},
	}

	respReturnRoute, err = env.Client.GetReturnRoute(env.Ctx, reqReturnRoute)
	require.NoError(t, err)
	require.Len(t, respReturnRoute.Points, 1)
	require.Equal(t, respReturnRoute.Points[0].SegmentId, uint64(returnSC.ID))
}

// Проверяем, что если отключили от возвратного СЦ, то возвратный путь должен еще жить.
func TestReturnSCWhenTurnOff(t *testing.T) {
	hardconfig.GetHardConfig().PartnersExpList = map[string]map[int64]bool{
		"express_return_sc": {
			intermediateReturnSCPartner: true,
			returnSCPoint:               true,
		},
	}
	scheduleFrom := &pb.Time{Hour: 10, Minute: 00}
	scheduleTo := &pb.Time{Hour: 20, Minute: 00}

	g := makeExpressReturnGraph(
		fmt.Sprintf("%02d:%02d:00", scheduleFrom.Hour, scheduleFrom.Minute),
		fmt.Sprintf("%02d:%02d:00", scheduleTo.Hour, scheduleTo.Minute),
	)

	generation := &bg.GenerationData{
		RegionMap:                regionMap,
		Graph:                    g,
		Express:                  makeExpress(),
		TariffsFinder:            makeTariffsFinder(),
		ExpressIntervalValidator: makeExpressIntervalValidator(),
	}
	returnSC := g.PartnerToSegments[returnSCPartner][0]
	intermediateReturnSC := g.PartnerToSegments[intermediateReturnSCPartner][0]
	backM := g.BackwardNeighbors[intermediateReturnSC.ID][0].ID

	settings, _ := its.NewStringSettingsHolder("{}")
	env, cancel := lite.NewEnv(t, generation, settings)
	defer cancel()

	reqReturnRoute := &pb.ReturnRouteRequest{
		From: &pb.ReturnRouteRequest_ReturnRoutePoint{
			PartnerId: intermediateReturnSCPartner,
		},
		To: &pb.ReturnRouteRequest_ReturnRoutePoint{
			PartnerId: expressWarehouseID,
		},
	}

	respReturnRoute, err := env.Client.GetReturnRoute(env.Ctx, reqReturnRoute)
	require.NoError(t, err)
	require.Len(t, respReturnRoute.Points, 3)
	require.Equal(t, respReturnRoute.Points[0].SegmentId, uint64(intermediateReturnSC.ID))
	require.Equal(t, respReturnRoute.Points[1].SegmentId, uint64(backM))
	require.Equal(t, respReturnRoute.Points[2].SegmentId, uint64(returnSC.ID))
}

func makeDeliveryRouteReq(startTime time.Time, interval *pb.DeliveryInterval, date *pb.Date, opts ...lite.MakeRequestOption) *pb.DeliveryRequest {
	return lite.MakeRequest(
		append(
			[]lite.MakeRequestOption{
				lite.RequestWithStartTime(startTime),
				lite.RequestWithDeliveryType(pb.DeliveryType_COURIER),
				lite.RequestWithDeliverySubtype(pb.DeliverySubtype_ORDINARY),
				lite.RequestWithPartner(expressWarehouseID),
				lite.RequestWithRegion(geobase.RegionMoscow),
				lite.RequestWithGpsCoords(userLatitude, userLongitude),
				lite.RequestWithTotalPrice(totalPrice),
				lite.RequestWithDeliveryOption(date, date),
				lite.RequestWithInterval(interval),
			}, opts...,
		)...,
	)
}

func containsMeta(
	t *testing.T,
	points []*pb.Route_Point,
	service enums.LmsServiceCode,
	needContains bool,
	key, value string,
) {
	var contains bool
	for _, r := range points {
		for _, s := range r.GetServices() {
			if s.Code == service.String() {
				for _, m := range s.ServiceMeta {
					if m.Key == key && m.Value == value {
						contains = true
					}
				}
			}
		}
	}
	require.Equal(t, needContains, contains)
}
