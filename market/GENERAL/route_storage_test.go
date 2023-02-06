package routestorage

import (
	"context"
	"os"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/core/log/nop"
	"a.yandex-team.ru/library/go/test/requirepb"
	"a.yandex-team.ru/market/combinator/pkg/its"
	routeytclient "a.yandex-team.ru/market/combinator/pkg/route_storage/route_yt_client"
	"a.yandex-team.ru/market/combinator/pkg/route_storage/service"
	cr "a.yandex-team.ru/market/combinator/proto/grpc"
)

func TestAddRoute(t *testing.T) {
	loger := &nop.Logger{}
	err := os.Setenv("YT_TOKEN", "no_token")
	require.NoError(t, err)
	cluster := os.Getenv("YT_PROXY")
	cfg := service.NewConfigLocalTest(cluster)
	settingsHolder := its.GetSettingsHolder()
	ytClient, err := routeytclient.NewYtClient(loger)
	require.NoError(t, err)
	require.NotNil(t, ytClient)
	ytController, err := routeytclient.CreateRoutesYtController(cfg.ReplicaPath, cfg.ReplicatedPath, cfg.ClusterReplicated, cfg.ClustersReplica)
	require.NoError(t, err)
	require.NotNil(t, ytController)
	serv := service.NewRouteStorageService(loger, settingsHolder, nil, nil, ytController, ytClient)
	err = serv.CreateTestTableYT()
	require.NoError(t, err)
	time.Sleep(10 * time.Second) // ждем mount таблички

	routeReq := &cr.DeliveryRoute{RouteId: "31c933aa-e256-11ec-b90e-465d9be8aa5v", Promise: "aaaaaaaa"}
	err = serv.AddRouteYT(context.Background(), "31c933aa-e256-11ec-b90e-465d9be8ab5v", routeReq)
	require.NoError(t, err)
	route, err := serv.FindRouteYT(context.Background(), "31c933aa-e256-11ec-b90e-465d9be8ab5v")
	require.NoError(t, err)
	requirepb.Equal(t, routeReq, route)
}
