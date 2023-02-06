package lite

import (
	"testing"

	"github.com/stretchr/testify/require"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"

	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	tr "a.yandex-team.ru/market/combinator/pkg/tarifficator"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

func TestEmptyRequest(t *testing.T) {
	gb := graph.NewGraphBuilder()
	graphEx := graph.BuildExample1(gb)
	tariffsBuilder := tr.NewTariffsBuilder()
	genData := bg.GenerationData{
		RegionMap:     geobase.NewExample(),
		TariffsFinder: tariffsBuilder.TariffsFinder,
		Graph:         graphEx.Graph,
	}
	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()

	checkLastMessage := func(expectedText string, expectedCode string) {
		logs := env.AccessLog.All()
		lastMessageFields := logs[len(logs)-1].ContextMap()
		require.Equal(t, lastMessageFields["error"], expectedText)
		require.Equal(t, lastMessageFields["error_code"], expectedCode)
	}

	check := func(resp interface{}, err error) {
		require.Error(t, err)
		st, ok := status.FromError(err)
		require.True(t, ok)
		require.Equal(t, codes.Unknown, st.Code())
		expectedErrorText := "bad destination 0|0|0"
		require.Equal(t, expectedErrorText, st.Message())
		checkLastMessage(expectedErrorText, "BAD_DESTINATION")
	}
	check(env.Client.GetDeliveryOptions(env.Ctx, &pb.DeliveryRequest{}))
	check(env.Client.GetOffersDeliveryStats(env.Ctx, &pb.OffersDeliveryRequest{}))
	check(env.Client.GetCourierOptions(env.Ctx, &pb.DeliveryRequest{}))
	check(env.Client.DebugFindPaths(env.Ctx, &pb.DeliveryRequest{}))
	{
		_, err := env.Client.GetDeliveryRoute(env.Ctx, &pb.DeliveryRequest{})
		require.Error(t, err)
		st, ok := status.FromError(err)
		require.True(t, ok)
		expectedErrorText := "wrong request: no Option"
		require.Equal(t, expectedErrorText, st.Message())
		checkLastMessage(expectedErrorText, "WRONG_REQUEST")
	}
	{
		resp, err := env.Client.GetPickupPointsGrouped(env.Ctx, &pb.PickupPointsRequest{})
		require.NoError(t, err)
		require.Empty(t, resp.Groups)
	}
	{
		resp, err := env.Client.SplitOrders(env.Ctx, &pb.SplitRequest{})
		require.NoError(t, err)
		require.Empty(t, resp.Orders)
	}
}
