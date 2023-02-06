package integration

import (
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/ytutil"
	"a.yandex-team.ru/yt/go/ypath"
	"a.yandex-team.ru/yt/go/yt"
	"a.yandex-team.ru/yt/go/yttest"
)

func TestResolveGeneration(t *testing.T) {
	env, cancel := yttest.NewEnv(t)
	defer cancel()

	genDir := ypath.Path("//tmp/combinator/foo")
	genPath := genDir.Child("g1")
	_, _ = env.YT.CreateNode(env.Ctx, genPath, yt.NodeTable, &yt.CreateNodeOptions{Recursive: true})
	_, _ = env.YT.LinkNode(env.Ctx, genPath, genDir.Child("recent"), nil)

	{
		gen, err := ytutil.ResolveRecentGeneration(env.YT, genDir)
		require.NoError(t, err)
		require.Equal(t, "g1", gen.Version)
		require.Equal(t, "foo", gen.Service)
		require.Equal(t, genPath.String(), gen.DirPath)
	}
	{
		gen, err := ytutil.ResolveGeneration(env.YT, genDir.Child("g1"))
		require.NoError(t, err)
		require.NotEqual(t, "g1", gen.Version)
		require.Equal(t, "foo", gen.Service)
		require.Equal(t, genPath.String(), gen.DirPath)
	}
	{
		_, err := ytutil.ResolveGeneration(env.YT, genDir.Child("no_such_generation"))
		require.Error(t, err)
	}
}
