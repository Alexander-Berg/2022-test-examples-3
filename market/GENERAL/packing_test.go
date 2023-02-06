package packing

import (
	"os"
	"testing"

	"a.yandex-team.ru/market/logistics/wms-load/go/model"
	"github.com/stretchr/testify/require"
)

var webUser = "load_1"
var webPass, _ = os.LookupEnv("WMS")

func DisabledTestFilterTables(t *testing.T) {
	params := map[string]string{"loc": "UPACK"}
	sourceTables := []model.PackTable{
		{Loc: "UPACK", AreaKey: "testArea1"},
		{Loc: "UPACK2", AreaKey: "testArea2"},
		{Loc: "UPACK3", AreaKey: "testArea3"},
	}

	filterTables := FilterTables(params, sourceTables)
	require.Equal(t, params["loc"], filterTables[0].Loc)
	require.Equal(t, 1, len(filterTables))
}
