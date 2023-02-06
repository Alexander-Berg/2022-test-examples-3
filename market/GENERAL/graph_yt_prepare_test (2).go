package integration

import (
	"context"
	"os"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/graph"
	"a.yandex-team.ru/yt/go/mapreduce"
	"a.yandex-team.ru/yt/go/migrate"
	"a.yandex-team.ru/yt/go/schema"
	"a.yandex-team.ru/yt/go/ypath"
	"a.yandex-team.ru/yt/go/yttest"
)

func TestMakeUpdate(t *testing.T) {
	env, cancel := yttest.NewEnv(t)
	defer cancel()

	srcDir := ypath.Path("//tmp/cdc")
	dstDir := ypath.Path("//tmp/combinator/graph")

	prepareTable := func(path ypath.Path, rows []interface{}) {
		schemax := schema.MustInfer(rows[0])
		require.NoError(t, migrate.Create(env.Ctx, env.YT, path, schemax))
		val := true
		require.NoError(t, env.YT.SetNode(env.Ctx, path.Attr("enable_dynamic_store_read"), &val, nil))
		require.NoError(t, migrate.MountAndWait(env.Ctx, env.YT, path))

		tx, err := env.YT.BeginTabletTx(env.Ctx, nil)
		require.NoError(t, err)

		require.NoError(t, tx.InsertRows(env.Ctx, path, rows, nil))
		require.NoError(t, tx.Commit())

		require.NoError(t, env.YT.FreezeTable(env.Ctx, path, nil))
	}

	nameRowsMap := map[string][]interface{}{
		graph.TableSegments: []interface{}{
			&graph.LogisticSegmentYT{
				ID: 1,
			},
		},
		graph.TableServices: []interface{}{
			&graph.LogisticServiceYT{
				ID:           1000 + 1,
				SegmentLmsID: 1000 + 1,
				Status:       "inactive",
				Code:         "HANDING",
			},
			// Эта запись отбросится в mapper.
			&graph.LogisticServiceYT{
				ID:           1000 + 2,
				SegmentLmsID: 1000 + 2,
				Status:       "inactive",
				Code:         "CASH_ALLOWED",
			},
		},
		graph.TableEdges: []interface{}{
			&graph.LogisticsEdges{
				ID: 1,
			},
		},
	}

	for name, rows := range nameRowsMap {
		prepareTable(srcDir.Child(name), rows)
	}
	ctx := context.Background()
	uploader := graph.NewUploader(ctx, env.YT, srcDir, dstDir, nil)
	result, err := uploader.MakeUpdate(nil, nil, &graph.UploaderOptions{
		SkipValidationAndHints: true,
	})
	require.NoError(t, err)
	require.True(t, result.Updated)
	{
		result, err := uploader.MakeUpdate(nil, nil, &graph.UploaderOptions{
			SkipValidationAndHints: true,
		})
		require.NoError(t, err)
		require.False(t, result.Updated)
	}
	for _, name := range graph.TableList {
		ok, err := env.YT.NodeExists(env.Ctx, dstDir.Child(result.Generation).Child(name), nil)
		require.NoError(t, err)
		require.True(t, ok)
		var rowCount int
		require.NoError(t, env.YT.GetNode(env.Ctx, dstDir.Child(result.Generation).Child(name).Attr("row_count"), &rowCount, nil))
		require.Equal(t, 1, rowCount)
	}
	var targetPath string
	require.NoError(t, env.YT.GetNode(env.Ctx, dstDir.Child("recent").SuppressSymlink().Attr("target_path"), &targetPath, nil))
	require.Equal(t, dstDir.Child(result.Generation).String(), targetPath)
}

func TestMain(m *testing.M) {
	if mapreduce.InsideJob() {
		os.Exit(mapreduce.JobMain())
	}

	os.Exit(m.Run())
}
