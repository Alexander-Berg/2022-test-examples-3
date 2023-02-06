package httpclient

import (
	"context"
	"fmt"
	"os"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/util/envtype"
)

func TestGetOrder_Lom(t *testing.T) {
	ctx := context.Background()
	envtype.SetEnvType(envtype.Testing)
	err := os.Setenv(IsUnitTestRunEnvName, "true")
	require.NoError(t, err, fmt.Sprintf("failed to set %s", IsUnitTestRunEnvName))

	srv := GetLomSrvStubDefault(t)
	defer srv.Close()

	data, status, err := GetOrder(ctx, ExpectedTestOrderID, LomRoutesFindOne, nil, false)
	VerifyLomDefaultResponse(t, data, status, err)
}

func TestGetOrder_Checkouter(t *testing.T) {
	ctx := context.Background()
	envtype.SetEnvType(envtype.Testing)
	err := os.Setenv(IsUnitTestRunEnvName, "true")
	require.NoError(t, err, fmt.Sprintf("failed to set %s", IsUnitTestRunEnvName))

	srv := GetCheckouterSrvStubDefault(t)
	defer srv.Close()

	data, status, err := GetOrder(ctx, ExpectedTestOrderID, Checkouter, nil, false)
	VerifyCheckouterDefaultResponse(t, data, status, err)
}

func TestGetOrderDefault_LomHasHigherPriority(t *testing.T) {
	ctx := context.Background()
	envtype.SetEnvType(envtype.Testing)
	err := os.Setenv(IsUnitTestRunEnvName, "true")
	require.NoError(t, err, fmt.Sprintf("failed to set %s", IsUnitTestRunEnvName))

	srvLom := GetLomSrvStubDefault(t)
	defer srvLom.Close()

	srvCh := GetCheckouterSrvStubDefault(t)
	defer srvCh.Close()

	source, data, status, err := GetOrderDefault(ctx, ExpectedTestOrderID, nil)
	require.Equal(t, source, LomRoutesFindOne)
	VerifyLomDefaultResponse(t, data, status, err)
}

func TestGetOrderDefault_NoDataInLom(t *testing.T) {
	ctx := context.Background()
	envtype.SetEnvType(envtype.Testing)
	err := os.Setenv(IsUnitTestRunEnvName, "true")
	require.NoError(t, err, fmt.Sprintf("failed to set %s", IsUnitTestRunEnvName))

	srvCh := GetCheckouterSrvStubDefault(t)
	defer srvCh.Close()

	// unable to reach LOM
	source, data, status, err := GetOrderDefault(ctx, ExpectedTestOrderID, nil)
	require.Equal(t, source, Checkouter)
	VerifyCheckouterDefaultResponse(t, data, status, err)

	srvLom := GetLomSrvStubDefaultWithCustomResponse(t, "")
	defer srvLom.Close()

	// lom returns empty result
	source, data, status, err = GetOrderDefault(ctx, ExpectedTestOrderID, nil)
	require.Equal(t, source, Checkouter)
	VerifyCheckouterDefaultResponse(t, data, status, err)
}

func TestGetOrderMerged_NoLom(t *testing.T) {
	ctx := context.Background()
	envtype.SetEnvType(envtype.Testing)
	err := os.Setenv(IsUnitTestRunEnvName, "true")
	require.NoError(t, err, fmt.Sprintf("failed to set %s", IsUnitTestRunEnvName))

	srvCh := GetCheckouterSrvStubDefault(t)
	defer srvCh.Close()

	source, data, status, err := GetOrderDefault(ctx, ExpectedTestOrderID, nil)
	require.Equal(t, source, Checkouter)
	VerifyCheckouterDefaultResponse(t, data, status, err)
}

func TestGetOrderMerged_NoCheckouter(t *testing.T) {
	ctx := context.Background()
	envtype.SetEnvType(envtype.Testing)
	err := os.Setenv(IsUnitTestRunEnvName, "true")
	require.NoError(t, err, fmt.Sprintf("failed to set %s", IsUnitTestRunEnvName))

	srvCh := GetLomSrvStubDefault(t)
	defer srvCh.Close()

	source, data, status, err := GetOrderMerged(ctx, ExpectedTestOrderID, nil)
	require.Equal(t, source, LomRoutesFindOne)
	VerifyLomDefaultResponse(t, data, status, err)
}

func TestGetOrderMerged_MergeIsActuallyDone(t *testing.T) {
	ctx := context.Background()
	envtype.SetEnvType(envtype.Testing)
	err := os.Setenv(IsUnitTestRunEnvName, "true")
	require.NoError(t, err, fmt.Sprintf("failed to set %s", IsUnitTestRunEnvName))

	LomResponseDefaultWithoutShipmentAndInfo := LomResponseDefault
	srvLom := GetLomSrvStubDefaultWithCustomResponse(t, LomResponseDefaultWithoutShipmentAndInfo)
	defer srvLom.Close()

	checkouterResponseWithShipmentAndInfo := CheckouterResponseDefaultStub
	srvCh := GetCheckouterSrvStubDefaultWithCustomResponse(t, checkouterResponseWithShipmentAndInfo)
	defer srvCh.Close()

	source, data, status, err := GetOrderMerged(ctx, ExpectedTestOrderID, nil)
	require.Equal(t, source, "lom+")
	VerifyStatusAndError(t, status, err)
	VerifyLomDefaultRoute(t, data.Route)
	VerifyCheckouterDefaultShipment(t, data.Shipment)
	VerifyCheckouterDefaultInfo(t, data.Info)
}
