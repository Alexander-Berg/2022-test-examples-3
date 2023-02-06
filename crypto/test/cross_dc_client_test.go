package kv_test

import (
	"a.yandex-team.ru/crypta/lib/go/yt/kv"
	"a.yandex-team.ru/yt/go/migrate"
	"a.yandex-team.ru/yt/go/ypath"
	"a.yandex-team.ru/yt/go/yt"
	"a.yandex-team.ru/yt/go/yt/ythttp"
	"a.yandex-team.ru/yt/go/yttest"
	"context"
	"github.com/stretchr/testify/require"
	"testing"
	"time"
)

func TestCrossDcClient(t *testing.T) {
	env, cancel := yttest.NewEnv(t)
	defer cancel()

	ctx, cancel := context.WithTimeout(env.Ctx, time.Second*15)
	defer cancel()

	retryTimeout := time.Millisecond * 100

	createTable := func(t *testing.T) ypath.Path {
		testTable := env.TmpPath().Child(t.Name())
		require.NoError(t, migrate.Create(ctx, env.YT, testTable, kv.GetSchema()))
		require.NoError(t, migrate.MountAndWait(ctx, env.YT, testTable))
		return testTable
	}

	setup := func(t *testing.T) (kv.Client, kv.ReadOnlyClient) {
		client := kv.NewClient(env.YT, createTable(t))

		return client, kv.NewCrossDcClient(client, []kv.Client{client}, retryTimeout)
	}

	const (
		key  = "key"
		key2 = "key2"
	)
	referenceValue := "value"
	referenceValue2 := "value2"
	referenceRecords := map[string]string{key: referenceValue, key2: referenceValue2}
	keys := []string{key, key2}

	t.Parallel()

	t.Run("SimpleOps", func(t *testing.T) {
		client, crossDcClient := setup(t)

		value, err := crossDcClient.Lookup(ctx, key)
		require.NoError(t, err)
		require.Nil(t, value)

		require.NoError(t, client.Write(ctx, key, referenceValue))

		value, err = crossDcClient.Lookup(ctx, key)
		require.NoError(t, err)
		require.Equal(t, &referenceValue, value)

		require.NoError(t, client.Delete(ctx, key))

		value, err = crossDcClient.Lookup(ctx, key)
		require.NoError(t, err)
		require.Nil(t, value)
	})

	t.Run("MultiOps", func(t *testing.T) {
		client, crossDcClient := setup(t)

		records, err := crossDcClient.LookupMany(ctx, keys)
		require.NoError(t, err)
		require.Empty(t, records)

		require.NoError(t, client.WriteMany(ctx, referenceRecords))

		records, err = crossDcClient.LookupMany(ctx, keys)
		require.NoError(t, err)
		require.Equal(t, referenceRecords, records)

		require.NoError(t, client.DeleteMany(ctx, keys))

		records, err = crossDcClient.LookupMany(ctx, keys)
		require.NoError(t, err)
		require.Empty(t, records)
	})

	t.Run("Select", func(t *testing.T) {
		client, crossDcClient := setup(t)

		require.NoError(t, client.WriteMany(ctx, referenceRecords))

		records, err := crossDcClient.Select(ctx, "WHERE is_substr(\"2\", key)")
		require.NoError(t, err)
		require.Equal(t, map[string]string{key2: referenceValue2}, records)
	})

	t.Run("SelectWithBrokenMain", func(t *testing.T) {
		testTable := createTable(t)
		client := kv.NewClient(env.YT, testTable)

		brokenYtClient, err := ythttp.NewClient(&yt.Config{Proxy: "broken.proxy"})
		require.NoError(t, err)

		brokenClient := kv.NewClient(brokenYtClient, testTable)
		crossDcClientWithBrokenMain := kv.NewCrossDcClient(brokenClient, []kv.Client{client}, retryTimeout)

		require.NoError(t, client.WriteMany(ctx, referenceRecords))

		records, err := crossDcClientWithBrokenMain.Select(ctx, "WHERE is_substr(\"2\", key)")
		require.NoError(t, err)
		require.Equal(t, map[string]string{key2: referenceValue2}, records)
	})
}
