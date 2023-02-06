package rsserver_test

import (
	"a.yandex-team.ru/crypta/lib/go/yt/kv"
	rsserver "a.yandex-team.ru/crypta/utils/rtmr_resource_service/bin/server/lib"
	"a.yandex-team.ru/yt/go/migrate"
	"a.yandex-team.ru/yt/go/yttest"
	"context"
	"github.com/stretchr/testify/require"
	"testing"
	"time"
)

func TestCrossDcClient(t *testing.T) {
	ytEnv, cancel := yttest.NewEnv(t)
	defer cancel()

	ctx, cancel := context.WithTimeout(ytEnv.Ctx, time.Second*15)
	defer cancel()

	testTable := ytEnv.TmpPath().Child("table")
	require.NoError(t, migrate.Create(ctx, ytEnv.YT, testTable, kv.GetSchema()))
	require.NoError(t, migrate.MountAndWait(ctx, ytEnv.YT, testTable))

	client := kv.NewClient(ytEnv.YT, testTable)
	retryTimeout := time.Millisecond * 100
	crossDcClient := kv.NewCrossDcClient(client, []kv.Client{}, retryTimeout)
	readOnlyDBClient := rsserver.NewReadOnlyDBClient(crossDcClient)
	dbClient := rsserver.NewDBClient(client)

	dc := "dc"
	refEnv := "env"
	resource := "resource"
	refVersion := uint64(10)
	refEnv2 := "env2"

	refPublicResources := []rsserver.ResourceID{
		{
			Name:        resource,
			ReleaseType: refEnv,
			Version:     refVersion,
		},
		{
			Name:        resource,
			ReleaseType: refEnv2,
			Version:     refVersion,
		},
	}

	t.Run("Env", func(t *testing.T) {
		env, err := readOnlyDBClient.GetEnv(ctx, dc)
		require.NoError(t, err)
		require.Nil(t, env)

		require.NoError(t, dbClient.SetEnv(ctx, dc, refEnv))

		env, err = readOnlyDBClient.GetEnv(ctx, dc)
		require.NoError(t, err)
		require.Equal(t, &refEnv, env)
	})

	t.Run("LatestResourceVersion", func(t *testing.T) {
		version, err := readOnlyDBClient.GetLatestResourceVersion(ctx, refEnv, resource)
		require.NoError(t, err)
		require.Nil(t, version)

		require.NoError(t, dbClient.SetLatestResourceVersion(ctx, refEnv, resource, refVersion))

		version, err = readOnlyDBClient.GetLatestResourceVersion(ctx, refEnv, resource)
		require.NoError(t, err)
		require.Equal(t, &refVersion, version)
	})

	t.Run("PublicResources", func(t *testing.T) {
		publicResources, err := readOnlyDBClient.GetPublicResources(ctx)
		require.NoError(t, err)
		require.Empty(t, publicResources)

		require.NoError(t, dbClient.SetPublicResources(ctx, refPublicResources))

		publicResources, err = dbClient.GetPublicResources(ctx)
		require.NoError(t, err)
		require.Equal(t, refPublicResources, publicResources)
	})
}
