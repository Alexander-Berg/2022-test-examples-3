package integration

import (
	"context"
	"sort"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/ytutil"
	"a.yandex-team.ru/market/combinator/pkg/ytutil/ytwriter"
	"a.yandex-team.ru/yt/go/ypath"
	"a.yandex-team.ru/yt/go/yt"
	"a.yandex-team.ru/yt/go/yttest"
)

type Row struct {
	A string
}

type TableWriter struct {
}

func (tw *TableWriter) WriteTable(p *ytwriter.Params) (ytwriter.Meta, error) {
	ctx := context.Background()
	if _, err := yt.CreateTable(ctx, p.Tx, p.DstPath); err != nil {
		return nil, err
	}
	w, err := p.Tx.WriteTable(ctx, p.DstPath, nil)
	if err != nil {
		return nil, err
	}
	row := Row{"foo"}
	if err := w.Write(row); err != nil {
		return nil, err
	}
	return nil, w.Commit()
}

func TestGenerationMaker(t *testing.T) {
	env, cancel := yttest.NewEnv(t)
	defer cancel()

	tableWriter := &TableWriter{}
	spec := ytwriter.Spec{
		YT:          env.YT,
		DirPath:     ypath.Path("//tmp/foo"),
		NumKeepGens: 1,
	}

	uploader := ytwriter.NewGenerationMaker(&spec, tableWriter)
	for i := 0; i < 2; i++ {
		var options ytwriter.Options
		if i != 0 {
			options.Force = true
			time.Sleep(1001 * time.Millisecond)
		}
		result, err := uploader.MakeGeneration(options)
		require.NoError(t, err)

		exists, err := env.YT.NodeExists(env.Ctx, ypath.Path(result.TablePath), nil)
		require.NoError(t, err)
		require.True(t, exists)
		{
			gen, err := ytutil.ResolveRecentGeneration(env.YT, spec.DirPath)
			require.NoError(t, err)
			require.Equal(t, result.TablePath, gen.DirPath)
		}
		{
			var nodes []string
			err := env.YT.ListNode(env.Ctx, spec.DirPath, &nodes, nil)
			sort.Strings(nodes)
			require.NoError(t, err)
			require.Len(t, nodes, 3)
			require.Equal(t, "lock", nodes[1])
			require.Equal(t, "recent", nodes[2])
		}
	}

	{
		spec := ytwriter.Spec{
			YT:              env.YT,
			DirPath:         ypath.Path("//tmp/bar"),
			SnapshotDirPath: ypath.Path("//tmp/snapshot"),
			NumKeepGens:     1,
		}
		_, err := env.YT.CreateNode(env.Ctx, spec.SnapshotDirPath, yt.NodeMap, nil)
		require.NoError(t, err)
		res, err := ytwriter.RunOnce(&spec, tableWriter, ytwriter.Options{})
		require.NoError(t, err)
		require.Equal(t, true, res.Updated)

		var nodes []string
		err = env.YT.ListNode(env.Ctx, spec.DirPath, &nodes, nil)
		require.NoError(t, err)
		sort.Strings(nodes)
		require.Len(t, nodes, 3)
		require.Equal(t, "lock", nodes[1])
		require.Equal(t, "recent", nodes[2])
	}
}
