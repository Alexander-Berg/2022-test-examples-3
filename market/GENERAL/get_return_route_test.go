package lite

import (
	"context"
	"testing"

	"github.com/stretchr/testify/require"

	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	"a.yandex-team.ru/market/combinator/pkg/its"
	"a.yandex-team.ru/market/combinator/pkg/settings"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

func nodesToPath(nodes graph.Nodes) []*pb.ReturnRouteResponse_Point {
	wantPoints := make([]*pb.ReturnRouteResponse_Point, len(nodes))
	for i, node := range nodes {
		wantPoints[i] = &pb.ReturnRouteResponse_Point{
			SegmentType: node.Type.String(),
			SegmentId:   uint64(node.ID),
			PartnerType: node.PartnerType.String(),
			Ids: &pb.PointIds{
				PartnerId:       uint64(node.PartnerLmsID),
				LogisticPointId: uint64(node.PointLmsID),
				RegionId:        uint32(node.LocationID),
			},
		}
	}
	return wantPoints
}

func makeGraph1() (*graph.Graph, []*pb.ReturnRouteResponse_Point, []*pb.ReturnRouteResponse_Point) {
	// Единственный возможный путь из SC В FF
	// SC -> BMV -> FF
	pb := graph.NewPathBuilder()
	sc := pb.AddWarehouse(
		pb.WithPartnerTypeSortingCenter(),
		pb.WithPartnerLmsID(200),
		pb.WithPointLmsID(10000000003),
		pb.MakeSortService(),
	)
	bMovement := pb.AddBackwardMovement()
	fulfillment := pb.AddWarehouse(
		pb.WithPartnerTypeFulfillment(),
		pb.WithPartnerLmsID(172),
		pb.MakeProcessingService(),
	)
	pickup := pb.AddPickup(
		pb.WithPartnerLmsID(800),
		pb.WithPointLmsID(10000000008),
		pb.MakeHandingService(
			pb.WithSchedule(*CreateEveryDaySchedule("00:00:00", "23:59:59", false)),
		),
	)
	pb.AddBEdge(sc, bMovement)
	pb.AddBEdge(bMovement, fulfillment)
	backwardSettings, _ := its.NewStringSettingsHolder("{\"read_backward_graph\": true}")
	ctx := settings.ContextWithSettings(context.Background(), settings.New(backwardSettings.GetSettings(), ""))
	pb.GetGraph().Finish(ctx)
	path := nodesToPath(
		graph.Nodes{
			pb.GetGraph().GetNodeByID(pickup),
			pb.GetGraph().GetNodeByID(sc),
			pb.GetGraph().GetNodeByID(bMovement),
			pb.GetGraph().GetNodeByID(fulfillment),
		})

	return pb.GetGraph(), path[1:], path
}

func makeGraph2() (*graph.Graph, []*pb.ReturnRouteResponse_Point, []*pb.ReturnRouteResponse_Point) {
	// Кратчайший путь из SC1 в FF
	//           SC1
	//          /    \
	//         |      V
	//         |     BMV2
	//         |       |
	//         |       V
	//       BMV1     SC2
	//         |       |
	//         |       V
	//         |     BMV3
	//          \     /
	//           V   V
	//             FF
	// Результат SC1 -> BMV1 -> FF
	pb := graph.NewPathBuilder()
	sc1 := pb.AddWarehouse(
		pb.WithPartnerTypeSortingCenter(),
		pb.WithPartnerLmsID(200),
		pb.WithPointLmsID(10000000003),
		pb.MakeSortService(),
	)
	bMovement1 := pb.AddBackwardMovement()
	fulfillment := pb.AddWarehouse(
		pb.WithPartnerTypeFulfillment(),
		pb.WithPartnerLmsID(172),
		pb.MakeProcessingService(),
	)
	bMovement2 := pb.AddBackwardMovement()
	sc2 := pb.AddWarehouse(
		pb.WithPartnerTypeSortingCenter(),
		pb.WithPartnerLmsID(300),
		pb.WithPointLmsID(10000000005),
		pb.MakeSortService(),
	)
	bMovement3 := pb.AddBackwardMovement()
	pickup := pb.AddPickup(
		pb.WithPartnerLmsID(800),
		pb.WithPointLmsID(10000000008),
		pb.MakeHandingService(
			pb.WithSchedule(*CreateEveryDaySchedule("00:00:00", "23:59:59", false)),
		),
	)

	pb.AddBEdge(sc1, bMovement1)
	pb.AddBEdge(bMovement1, fulfillment)

	pb.AddBEdge(sc1, bMovement2)
	pb.AddBEdge(bMovement2, sc2)
	pb.AddBEdge(sc2, bMovement3)
	pb.AddBEdge(bMovement3, fulfillment)

	backwardSettings, _ := its.NewStringSettingsHolder("{\"read_backward_graph\": true}")
	ctx := settings.ContextWithSettings(context.Background(), settings.New(backwardSettings.GetSettings(), ""))
	pb.GetGraph().Finish(ctx)

	path := nodesToPath(
		graph.Nodes{
			pb.GetGraph().GetNodeByID(pickup),
			pb.GetGraph().GetNodeByID(sc1),
			pb.GetGraph().GetNodeByID(bMovement1),
			pb.GetGraph().GetNodeByID(fulfillment),
		})

	return pb.GetGraph(), path[1:], path
}

func makeGraph3() (*graph.Graph, []*pb.ReturnRouteResponse_Point, []*pb.ReturnRouteResponse_Point) {
	// Граф: SC1 -> BMV1 -> SC2 -> BMV2 -> FF
	// Искомый путь: SC1 -> BMV1 -> SC2
	pb := graph.NewPathBuilder()
	sc1 := pb.AddWarehouse(
		pb.WithPartnerTypeSortingCenter(),
		pb.WithPartnerLmsID(200),
		pb.WithPointLmsID(10000000003),
		pb.MakeSortService(),
	)
	bMovement1 := pb.AddBackwardMovement()
	sc2 := pb.AddWarehouse(
		pb.WithPartnerTypeSortingCenter(),
		pb.WithPartnerLmsID(300),
		pb.WithPointLmsID(10000000005),
		pb.MakeSortService(),
	)
	bMovement2 := pb.AddBackwardMovement()
	fulfillment := pb.AddWarehouse(
		pb.WithPartnerTypeFulfillment(),
		pb.WithPartnerLmsID(172),
		pb.MakeProcessingService(),
	)
	pickup := pb.AddPickup(
		pb.WithPartnerLmsID(800),
		pb.WithPointLmsID(10000000008),
		pb.MakeHandingService(
			pb.WithSchedule(*CreateEveryDaySchedule("00:00:00", "23:59:59", false)),
		),
	)
	pb.AddBEdge(sc1, bMovement1)
	pb.AddBEdge(bMovement1, sc2)
	pb.AddBEdge(sc2, bMovement2)
	pb.AddBEdge(bMovement2, fulfillment)
	backwardSettings, _ := its.NewStringSettingsHolder("{\"read_backward_graph\": true}")
	ctx := settings.ContextWithSettings(context.Background(), settings.New(backwardSettings.GetSettings(), ""))
	pb.GetGraph().Finish(ctx)

	path := nodesToPath(
		graph.Nodes{
			pb.GetGraph().GetNodeByID(pickup),
			pb.GetGraph().GetNodeByID(sc1),
			pb.GetGraph().GetNodeByID(bMovement1),
			pb.GetGraph().GetNodeByID(sc2),
		})

	return pb.GetGraph(), path[1:], path
}

func makeGraph4() (*graph.Graph, []*pb.ReturnRouteResponse_Point, []*pb.ReturnRouteResponse_Point) {
	// Граф: SC1 -> BMV1 -> SC2 -> BMV2 -> FF
	// Искомый путь: SC2 -> BMV2 -> FF
	pb := graph.NewPathBuilder()
	sc1 := pb.AddWarehouse(
		pb.WithPartnerTypeSortingCenter(),
		pb.WithPartnerLmsID(200),
		pb.WithPointLmsID(10000000003),
		pb.MakeSortService(),
	)
	bMovement1 := pb.AddBackwardMovement()
	sc2 := pb.AddWarehouse(
		pb.WithPartnerTypeSortingCenter(),
		pb.WithPartnerLmsID(300),
		pb.WithPointLmsID(10000000005),
		pb.MakeSortService(),
	)
	bMovement2 := pb.AddBackwardMovement()
	fulfillment := pb.AddWarehouse(
		pb.WithPartnerTypeFulfillment(),
		pb.WithPartnerLmsID(172),
		pb.MakeProcessingService(),
	)
	pickup := pb.AddPickup(
		pb.WithPartnerLmsID(800),
		pb.WithPointLmsID(10000000008),
		pb.MakeHandingService(
			pb.WithSchedule(*CreateEveryDaySchedule("00:00:00", "23:59:59", false)),
		),
	)
	pb.AddBEdge(sc1, bMovement1)
	pb.AddBEdge(bMovement1, sc2)
	pb.AddBEdge(sc2, bMovement2)
	pb.AddBEdge(bMovement2, fulfillment)
	backwardSettings, _ := its.NewStringSettingsHolder("{\"read_backward_graph\": true}")
	ctx := settings.ContextWithSettings(context.Background(), settings.New(backwardSettings.GetSettings(), ""))
	pb.GetGraph().Finish(ctx)

	path := nodesToPath(
		graph.Nodes{
			pb.GetGraph().GetNodeByID(pickup),
			pb.GetGraph().GetNodeByID(sc2),
			pb.GetGraph().GetNodeByID(bMovement2),
			pb.GetGraph().GetNodeByID(fulfillment),
		})

	return pb.GetGraph(), path[1:], path
}

func makeGraph5() (*graph.Graph, []*pb.ReturnRouteResponse_Point, []*pb.ReturnRouteResponse_Point) {
	// Граф: SC1 -> BMV1 -> SC2 -> BMV2 -> SC3 -> BMV3 -> FF
	// Искомый путь: SC2 -> BMV2 -> SC3
	pb := graph.NewPathBuilder()
	sc1 := pb.AddWarehouse(
		pb.WithPartnerTypeSortingCenter(),
		pb.WithPartnerLmsID(200),
		pb.WithPointLmsID(10000000003),
		pb.MakeSortService(),
	)
	bMovement1 := pb.AddBackwardMovement()
	sc2 := pb.AddWarehouse(
		pb.WithPartnerTypeSortingCenter(),
		pb.WithPartnerLmsID(300),
		pb.WithPointLmsID(10000000005),
		pb.MakeSortService(),
	)
	bMovement2 := pb.AddBackwardMovement()
	sc3 := pb.AddWarehouse(
		pb.WithPartnerTypeSortingCenter(),
		pb.WithPartnerLmsID(400),
		pb.WithPointLmsID(10000000006),
		pb.MakeSortService(),
	)
	bMovement3 := pb.AddBackwardMovement()
	fulfillment := pb.AddWarehouse(
		pb.WithPartnerTypeFulfillment(),
		pb.WithPartnerLmsID(172),
		pb.MakeProcessingService(),
	)
	pickup := pb.AddPickup(
		pb.WithPartnerLmsID(800),
		pb.WithPointLmsID(10000000008),
		pb.MakeHandingService(
			pb.WithSchedule(*CreateEveryDaySchedule("00:00:00", "23:59:59", false)),
		),
	)
	pb.AddBEdge(sc1, bMovement1)
	pb.AddBEdge(bMovement1, sc2)
	pb.AddBEdge(sc2, bMovement2)
	pb.AddBEdge(bMovement2, sc3)
	pb.AddBEdge(sc3, bMovement3)
	pb.AddBEdge(bMovement3, fulfillment)
	backwardSettings, _ := its.NewStringSettingsHolder("{\"read_backward_graph\": true}")
	ctx := settings.ContextWithSettings(context.Background(), settings.New(backwardSettings.GetSettings(), ""))
	pb.GetGraph().Finish(ctx)

	path := nodesToPath(
		graph.Nodes{
			pb.GetGraph().GetNodeByID(pickup),
			pb.GetGraph().GetNodeByID(sc2),
			pb.GetGraph().GetNodeByID(bMovement2),
			pb.GetGraph().GetNodeByID(sc3),
		})

	return pb.GetGraph(), path[1:], path
}

func makeGraph6() (*graph.Graph, []*pb.ReturnRouteResponse_Point, []*pb.ReturnRouteResponse_Point) {
	// Единственный возможный путь из SC В DS, но BMV имеет тот же partnerId, что и DS(самозабор).
	// SC -> BMV -> DS
	pb := graph.NewPathBuilder()
	sc := pb.AddWarehouse(
		pb.WithPartnerTypeSortingCenter(),
		pb.WithPartnerLmsID(200),
		pb.WithPointLmsID(10000000003),
		pb.MakeSortService(),
	)
	bMovement := pb.AddBackwardMovement(
		pb.WithPartnerLmsID(300),
	)
	dropship := pb.AddWarehouse(
		pb.WithPartnerTypeDropship(),
		pb.WithPartnerLmsID(300),
		pb.MakeProcessingService(),
	)
	pickup := pb.AddPickup(
		pb.WithPartnerLmsID(800),
		pb.WithPointLmsID(10000000008),
		pb.MakeHandingService(
			pb.WithSchedule(*CreateEveryDaySchedule("00:00:00", "23:59:59", false)),
		),
	)
	pb.AddBEdge(sc, bMovement)
	pb.AddBEdge(bMovement, dropship)
	backwardSettings, _ := its.NewStringSettingsHolder("{\"read_backward_graph\": true}")
	ctx := settings.ContextWithSettings(context.Background(), settings.New(backwardSettings.GetSettings(), ""))
	pb.GetGraph().Finish(ctx)

	path := nodesToPath(
		graph.Nodes{
			pb.GetGraph().GetNodeByID(pickup),
			pb.GetGraph().GetNodeByID(sc),
			pb.GetGraph().GetNodeByID(bMovement),
			pb.GetGraph().GetNodeByID(dropship),
		})

	return pb.GetGraph(), path[1:], path
}

func TestGetReturnRoute(t *testing.T) {
	comparePaths := func(want, response []*pb.ReturnRouteResponse_Point) {
		require.Len(t, response, len(want))
		for i, point := range response {
			require.Equal(t, want[i].SegmentType, point.SegmentType)
			require.Equal(t, want[i].SegmentId, point.SegmentId)
			require.Equal(t, want[i].PartnerType, point.PartnerType)
			require.Equal(t, want[i].PartnerName, point.PartnerName)
			require.Equal(t, want[i].Ids.PartnerId, point.Ids.PartnerId)
			require.Equal(t, want[i].Ids.LogisticPointId, point.Ids.LogisticPointId)
			require.Equal(t, want[i].Ids.RegionId, point.Ids.RegionId)
			require.Equal(t, want[i].Ids.PostCode, point.Ids.PostCode)
			require.Equal(t, want[i].Ids.GpsCoords, point.Ids.GpsCoords)
			require.Equal(t, want[i].Ids.DsbsPointId, point.Ids.DsbsPointId)
		}
	}

	backwardSettings, _ := its.NewStringSettingsHolder("{\"read_backward_graph\": true}")

	examples := []func() (*graph.Graph, []*pb.ReturnRouteResponse_Point, []*pb.ReturnRouteResponse_Point){
		makeGraph1,
		makeGraph2,
		makeGraph3,
		makeGraph4,
		makeGraph5,
		makeGraph6,
	}

	for _, example := range examples {
		bGraph, wantPoints, wantPointsWithPickup := example()
		genData := bg.GenerationData{
			Graph:         bGraph,
			TariffsFinder: NewFinderSet(nil),
		}
		env, cancel := NewEnv(t, &genData, backwardSettings)
		defer cancel()

		// https://wiki.yandex-team.ru/market/it-operations-development/texnicheskie-kartochki-proektov/vozvraty-v-dropship/final/backward-route/#combinator.logikarabotyruchkipoluchenijavozvratnogomarshruta
		for _, scenario := range []int{0, 1, 2} {
			fromSegmentID := int64(wantPoints[0].SegmentId) // Вариант 2 со странички Вики - указан сегмент СЦ
			switch scenario {
			case 1:
				fromSegmentID = 0 // Вариант 3 со странички Вики - не указан сегмент СЦ
			case 2:
				fromSegmentID = 1337 // COMBINATOR-3191 указан несуществующий сегмент СЦ, из которого нет маршрута
			}
			req := MakeReturnRouteRequest(
				RequestWithReturnFrom(graph.ReturnRoutePoint{
					PartnerID:  int64(wantPoints[0].Ids.PartnerId),
					PointLmsID: int64(wantPoints[0].Ids.LogisticPointId),
					SegmentID:  fromSegmentID,
				}),
				RequestWithReturnTo(graph.ReturnRoutePoint{
					PartnerID: int64(wantPoints[len(wantPoints)-1].Ids.PartnerId),
				}),
			)

			resp, err := env.Client.GetReturnRoute(env.Ctx, req)

			require.NoError(t, err)
			comparePaths(wantPoints, resp.GetPoints())
		}

		// Возврат с ПВЗ с указанием первого СЦ
		reqWithPickup := MakeReturnRouteRequest(
			RequestWithReturnFrom(graph.ReturnRoutePoint{
				PartnerID:  int64(wantPointsWithPickup[0].Ids.PartnerId),
				PointLmsID: int64(wantPointsWithPickup[0].Ids.LogisticPointId),
			}),
			RequestWithReturnFirstSC(graph.ReturnRoutePoint{
				PartnerID:  int64(wantPointsWithPickup[1].Ids.PartnerId),
				PointLmsID: int64(wantPointsWithPickup[1].Ids.LogisticPointId),
			}),
			RequestWithReturnTo(graph.ReturnRoutePoint{
				PartnerID: int64(wantPointsWithPickup[len(wantPointsWithPickup)-1].Ids.PartnerId),
			}),
		)

		respWithPickup, err := env.Client.GetReturnRoute(env.Ctx, reqWithPickup)

		require.NoError(t, err)
		comparePaths(wantPointsWithPickup, respWithPickup.GetPoints())
	}
}
