package ycombo

import (
	"context"
	"fmt"
	"os"
	"testing"

	"github.com/stretchr/testify/require"
	"google.golang.org/grpc"

	httpclient "a.yandex-team.ru/market/combinator/pkg/http_client"
	"a.yandex-team.ru/market/combinator/pkg/util/envtype"
	cr "a.yandex-team.ru/market/combinator/proto/grpc"
)

func createDummyCombi(t *testing.T) cr.CombinatorClient {
	conn, err := grpc.Dial(
		"localhost:1001",
		grpc.WithUserAgent("test"),
		grpc.WithInsecure(),
	)
	require.NoError(t, err)
	return cr.NewCombinatorClient(conn)
}

func TestMakePrettyRoute_LomOrdersServiceUnavailable(t *testing.T) {
	envtype.SetEnvType(envtype.Testing)
	err := os.Setenv(httpclient.IsUnitTestRunEnvName, "true")
	require.NoError(t, err, fmt.Sprintf("failed to set %s", httpclient.IsUnitTestRunEnvName))

	srvLom := httpclient.GetLomSrvStubDefault(t)
	defer srvLom.Close()

	srvCh := httpclient.GetCheckouterSrvStubDefault(t)
	defer srvCh.Close()

	err = os.Setenv("LOM_ORDERS_URL", "invalid url")
	require.NoError(t, err, "failed to set LOM_ORDERS_URL")

	result := makePrettyRoute(context.Background(), createDummyCombi(t), httpclient.ExpectedTestOrderID, AUTO)
	require.Equal(t, "", result.CheckouterError)
	require.Equal(t, "", result.LOMRoutesFindOneError)
	require.Equal(
		t,
		`Put "/invalid%20url": unsupported protocol scheme ""`,
		result.LOMOrderError,
	)
}

func TestMakePrettyRoute_LomOrdersNotUsed(t *testing.T) {
	envtype.SetEnvType(envtype.Testing)
	err := os.Setenv(httpclient.IsUnitTestRunEnvName, "true")
	require.NoError(t, err, fmt.Sprintf("failed to set %s", httpclient.IsUnitTestRunEnvName))

	srvLom := httpclient.GetLomSrvStubDefault(t)
	defer srvLom.Close()

	srvCh := httpclient.GetCheckouterSrvStubDefault(t)
	defer srvCh.Close()

	err = os.Setenv("LOM_ORDERS_URL", "invalid url")
	require.NoError(t, err, "failed to set LOM_ORDERS_URL")

	result := makePrettyRoute(
		context.Background(),
		createDummyCombi(t),
		httpclient.ExpectedTestOrderID,
		httpclient.Checkouter,
	)
	require.Equal(t, "", result.CheckouterError)
	require.Equal(t, "", result.LOMRoutesFindOneError)
	require.Equal(t, "", result.LOMOrderError)
	require.Equal(
		t,
		"lastItem index is out of range",
		result.FormatError,
	)
}
