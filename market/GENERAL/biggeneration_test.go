package integration

import (
	"errors"
	"testing"

	"github.com/stretchr/testify/require"

	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	"a.yandex-team.ru/yt/go/ypath"
	"a.yandex-team.ru/yt/go/yt"
	"a.yandex-team.ru/yt/go/yttest"
)

func TestOutputTableCreation(t *testing.T) {
	// logging.Setup(nil)
	env, cancel := yttest.NewEnv(t)
	defer cancel()

	rootDir := ypath.Path("//tmp/combinator")
	cleanup := func() {
		_ = env.YT.RemoveNode(env.Ctx, rootDir, &yt.RemoveNodeOptions{Recursive: true, Force: true})
	}
	// no generations
	{
		_, err := bg.GetRecentByCli(env.YT, rootDir, nil)
		require.Error(t, err)
		require.True(t, errors.Is(err, bg.ErrGenerationNotExist))

		_, err = bg.UpdateGeneration(env.YT, rootDir, &bg.UpdateGenerationOptions{GenName: "foo"})
		require.Error(t, err)
		require.Contains(t, err.Error(), "not exists //tmp")
	}

	cleanup()
	gname := "local"
	for _, sname := range bg.ServiceList {
		tablePath := rootDir.Child(sname).Child(gname)
		_, _ = env.YT.CreateNode(env.Ctx, tablePath, yt.NodeTable, &yt.CreateNodeOptions{Recursive: true})
		_, _ = env.YT.LinkNode(env.Ctx, tablePath, rootDir.Child(sname).Child("recent"), nil)
	}

	// first
	{
		updated, err := bg.UpdateGeneration(env.YT, rootDir, &bg.UpdateGenerationOptions{GenName: "foo"})
		require.NoError(t, err)
		require.True(t, updated)

		ok, err := env.YT.NodeExists(env.Ctx, rootDir.Child("generation").Child("foo"), nil)
		require.NoError(t, err)
		require.True(t, ok)

		gen, err := bg.GetRecentByCli(env.YT, rootDir, nil)
		require.NoError(t, err)
		require.Equal(t, gen.Version, "foo")
		require.Equal(t, gen.Generations[bg.ServiceGeobase].DirPath, string(rootDir.Child("geobase").Child(gname)))
		require.Equal(t, gen.Generations[bg.ServiceTariffs].DirPath, string(rootDir.Child("tariffs").Child(gname)))

		gen2, err := bg.ResolveGeneration(env.YT, rootDir.Child("generation").Child("foo"))
		require.NoError(t, err)
		require.Equal(t, gen, gen2)
	}

	// second, not updated
	{
		updated, err := bg.UpdateGeneration(env.YT, rootDir, &bg.UpdateGenerationOptions{GenName: "bar"})
		require.NoError(t, err)
		require.False(t, updated)

		gen, err := bg.GetRecentByCli(env.YT, rootDir, nil)
		require.NoError(t, err)
		require.Equal(t, gen.Version, "foo")
		require.Equal(t, gen.Generations[bg.ServiceGeobase].DirPath, string(rootDir.Child("geobase").Child(gname)))
		require.Equal(t, gen.Generations[bg.ServiceTariffs].DirPath, string(rootDir.Child("tariffs").Child(gname)))

		_, err = bg.ResolveGeneration(env.YT, rootDir.Child("generation").Child("bar"))
		require.Error(t, err)
	}
}
