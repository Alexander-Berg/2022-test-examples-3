package integration

import (
	"context"
	"os"
	"testing"

	"github.com/stretchr/testify/require"

	app "a.yandex-team.ru/market/combinator/pkg/combinator-app"
	"a.yandex-team.ru/market/combinator/pkg/dsbs"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/yt/go/ypath"
	"a.yandex-team.ru/yt/go/yt"
	"a.yandex-team.ru/yt/go/yttest"
)

func TestReadCourierTariffFromYt(t *testing.T) {
	err := os.Setenv("YT_TOKEN", "no_token")
	require.NoError(t, err)

	env, cancel := yttest.NewEnv(t)
	defer cancel()

	cfg := app.NewConfigDev().YtConfig
	cfg.Proxy = ""

	genDir := ypath.Path("//tmp/dsbs_courier")

	daysFrom := uint32(2)
	daysTo := uint32(4)
	row := &dsbs.ShopCourierTariffYT{
		MarketPickupEnabled: true,
		CommonTariffYT: dsbs.CommonTariffYT{
			DaysFrom: &daysFrom,
			DaysTo:   &daysTo,
			ShopID:   23432,
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

	regionMap := make(geobase.RegionMap)

	tariffs, DSBSToOutletShops, err := dsbs.ReadShopCourierTariffsFromYt(&cfg, string(genDir), &regionMap, nil)

	require.NoError(t, err)
	require.Equal(t, 1, len(tariffs))
	require.Equal(t, 1, len(DSBSToOutletShops))
	require.Contains(t, DSBSToOutletShops, uint64(23432))
}
