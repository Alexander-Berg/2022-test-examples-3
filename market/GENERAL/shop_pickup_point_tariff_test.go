package integration

import (
	"context"
	"os"
	"testing"

	"github.com/stretchr/testify/require"

	app "a.yandex-team.ru/market/combinator/pkg/combinator-app"
	"a.yandex-team.ru/market/combinator/pkg/dsbs"
	"a.yandex-team.ru/market/combinator/pkg/outlets"
	"a.yandex-team.ru/yt/go/ypath"
	"a.yandex-team.ru/yt/go/yt"
	"a.yandex-team.ru/yt/go/yttest"
)

func TestReadPickupPointTariffFromYt(t *testing.T) {
	err := os.Setenv("YT_TOKEN", "no_token")
	require.NoError(t, err)

	env, cancel := yttest.NewEnv(t)
	defer cancel()

	genDir := ypath.Path("//tmp/dsbs_pickup")
	cfg := app.NewConfigDev().YtConfig
	cfg.Proxy = ""

	daysFrom := uint32(2)
	daysTo := uint32(4)
	row := &dsbs.ShopPickupPointTariffYT{
		LogisticsPointID: 10001937398,
		CommonTariffYT: dsbs.CommonTariffYT{
			DaysFrom: &daysFrom,
			DaysTo:   &daysTo,
		},
	}

	ctx := context.Background()
	_, err = yt.CreateTable(ctx, env.YT, genDir)
	require.NoError(t, err)
	writer, err := env.YT.WriteTable(ctx, genDir, nil)
	require.NoError(t, err)
	err = writer.Write(row)
	require.NoError(t, err)
	err = writer.Commit()
	require.NoError(t, err)

	outlets := outlets.OutletStorage{
		OutletsSlice: make([]outlets.Outlet, 1),
		Outlets: map[int64]int{
			10001937398: 0,
		},
	}

	tariffs, err := dsbs.ReadShopPickupPointTariffsFromYt(&cfg, string(genDir), &outlets)

	require.NoError(t, err)
	require.Equal(t, 1, len(tariffs))
	require.Equal(t, int64(10001937398), tariffs[0][0].LogisticsPoints[0])
}
