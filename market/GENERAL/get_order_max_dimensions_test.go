package lite

import (
	"testing"

	"github.com/stretchr/testify/require"

	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

func TestGetOrderMaxDimensions(t *testing.T) {
	graphEx := graph.NewExample()
	// This tariff couldn't handle such a heavy items
	genData := bg.GenerationData{
		RegionMap:     geobase.NewExample(),
		TariffsFinder: nil,
		Graph:         graphEx.G,
	}
	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()

	// Тестируем, что при попытке получить несуществующий заказ мы не валимся, а возвращаем ошибку
	req := pb.OrderMaxDimensionsRequest{OrderId: 0}
	_, err := env.Client.GetOrderMaxDimensions(env.Ctx, &req)
	require.Error(t, err)
}
