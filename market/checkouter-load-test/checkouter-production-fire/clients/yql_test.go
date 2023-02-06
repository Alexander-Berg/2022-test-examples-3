package clients

import (
	"a.yandex-team.ru/library/go/slices"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/util"
	"context"
	"encoding/json"
	"fmt"
	"github.com/stretchr/testify/require"
	"testing"
)

var ctx = context.Background()
var token = util.ResolveSecret(prepareSecretConfig())

func prepareSecretConfig() util.SecretConfig {
	return util.SecretConfig{
		Type: "ENV",
		Name: "YQL_TOKEN",
	}
}

func TestYql(t *testing.T) {
	res, err := YQLClient().GetOperations(ctx, token)
	require.Empty(t, err)
	require.NotEmpty(t, res)
	fmt.Println(res)
}

func TestExecute(t *testing.T) {
	ctx := context.Background()
	result, _ := YQLClient().ExecuteQuery("select 1 as a union all select 2 as a", ctx, token)
	body, _ := json.Marshal(&result)
	fmt.Println(string(body))
}

func TestGetOperation(t *testing.T) {
	result, _ := YQLClient().GetOperation("615427bcae4e0fea5e1d11c1", ctx, token)
	body, _ := json.Marshal(&result)
	fmt.Println(string(body))
}

func TestGetOperationData(t *testing.T) {
	result, _ := YQLClient().GetOperationData("614b5cabd2b70ca7f85c73db", ctx, token)
	body, _ := json.Marshal(&result)
	fmt.Println(string(body))
}

func TestContains(t *testing.T) {
	runningStatuses := []string{"IDLE", "PENDING", "RUNNING"}
	found, _ := slices.Contains(runningStatuses, "IDLE")
	if found {
		fmt.Println("true")
	} else {
		fmt.Println("false")
	}
	require.True(t, found)
}
