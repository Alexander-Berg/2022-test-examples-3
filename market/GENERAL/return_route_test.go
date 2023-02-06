package lite

import (
	"context"
	"testing"
	"time"

	"github.com/golang/protobuf/ptypes/timestamp"
	"github.com/stretchr/testify/require"

	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	"a.yandex-team.ru/market/combinator/pkg/combinator-app/routes"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	tr "a.yandex-team.ru/market/combinator/pkg/tarifficator"
	combinator "a.yandex-team.ru/market/combinator/proto/grpc"
)

func TestReturnRoute(t *testing.T) {
	example := graph.NewBackwardGraphExample()
	regionMap := geobase.NewExample()
	tariffsFinder := tr.NewTariffsFinder()
	tariffsFinder.Finish(&regionMap)
	genData := bg.GenerationData{
		RegionMap:     regionMap,
		Graph:         example.G,
		TariffsFinder: NewFinderSet(tariffsFinder),
	}

	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()

	utc, _ := time.LoadLocation("UTC")
	startTime := time.Date(2022, 6, 20, 9, 0, 0, 0, utc)
	makeRequest := func(toPartnerId uint64, sbgForReturnRoute bool) *combinator.ReturnRouteRequest {
		req := &combinator.ReturnRouteRequest{
			From: &combinator.ReturnRouteRequest_ReturnRoutePoint{
				PartnerId: 1006601,
			},
			To: &combinator.ReturnRouteRequest_ReturnRoutePoint{
				PartnerId: toPartnerId,
			},
			StartTime: &timestamp.Timestamp{
				Seconds: startTime.Unix(),
			},
			RearrFactors: "sbg_for_return_route=0",
		}
		if sbgForReturnRoute {
			req.RearrFactors = "sbg_for_return_route=1"
		}
		return req
	}

	{
		resp, err := env.Client.GetReturnRoute(
			context.Background(),
			makeRequest(100500, false),
		)
		require.NoError(t, err)
		require.Len(t, resp.GetPoints(), 3)
		require.Equal(t, "warehouse", resp.Points[0].GetSegmentType())
		require.Equal(t, "backward_movement", resp.Points[1].GetSegmentType())
		require.Equal(t, "warehouse", resp.Points[2].GetSegmentType())
		require.Empty(t, resp.Points[0].Services)
		require.Empty(t, resp.Points[1].Services)
		require.Empty(t, resp.Points[2].Services)
	}
	{
		_, err := env.Client.GetReturnRoute(
			context.Background(),
			makeRequest(200600, false),
		)
		require.Error(t, err, "rpc error: code = Unknown desc = return route to (partnerID 200600, pointLmsID 0, segmentID 0) not found from (partnerID 1006601, pointLmsID 0, segmentID 0)")
	}
	{
		resp, err := env.Client.GetReturnRoute(
			context.Background(),
			makeRequest(100500, true),
		)
		require.NoError(t, err)
		require.Len(t, resp.GetPoints(), 7)
		require.Equal(t, "backward_warehouse", resp.Points[0].GetSegmentType())
		require.EqualValues(t, 970, resp.Points[0].GetIds().GetRegionId())
		require.EqualValues(t, 10800, resp.Points[0].GetTzOffset())
		require.EqualValues(t, 1006601, resp.Points[0].GetIds().GetPartnerId())
		require.Len(t, resp.Points[0].GetServices(), 2)
		require.Equal(t,
			startTime,
			routes.ProtoToTimeLoc(resp.Points[0].Services[0].GetStartTime(), utc),
		)
		require.Equal(t, "backward_movement", resp.Points[1].SegmentType)
		require.Len(t, resp.Points[1].Services, 1)
		require.Equal(t, "backward_warehouse", resp.Points[4].SegmentType)
		require.EqualValues(t, 147, resp.Points[4].GetIds().GetPartnerId())
		require.Len(t, resp.Points[4].Services, 1)
		require.Equal(t,
			time.Date(2022, 7, 1, 15, 0, 0, 0, utc),
			routes.ProtoToTimeLoc(resp.Points[4].Services[0].GetEndTime(), utc),
		)
		// Путь через сегмент 1000001 запрещен поскольку в нем нет сервисов
		require.EqualValues(t, resp.Points[5].SegmentId, 2000002)
		require.Len(t, resp.Points[5].Services, 1)
		// Финальный сегмент пути не содержит сервисов
		require.Len(t, resp.Points[6].Services, 0)
	}
	{
		_, err := env.Client.GetReturnRoute(
			context.Background(),
			makeRequest(200600, true),
		)
		// Путь через сегмент 4000004 запрещен и другого нет
		require.Error(t, err, "rpc error: code = Unknown desc = return route to (partnerID 200600, pointLmsID 0, segmentID 0) not found from (partnerID 1006601, pointLmsID 0, segmentID 0)")
	}
}
