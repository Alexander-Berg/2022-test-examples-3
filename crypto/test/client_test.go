package kv_test

import (
	"a.yandex-team.ru/crypta/lib/go/yt/kv"
	"a.yandex-team.ru/yt/go/migrate"
	"a.yandex-team.ru/yt/go/yttest"
	"context"
	"github.com/stretchr/testify/require"
	"testing"
	"time"
)

func TestClient(t *testing.T) {
	env, cancel := yttest.NewEnv(t)
	defer cancel()

	ctx, cancel := context.WithTimeout(env.Ctx, time.Second*15)
	defer cancel()

	setup := func(t *testing.T) kv.Client {
		testTable := env.TmpPath().Child(t.Name())
		require.NoError(t, migrate.Create(ctx, env.YT, testTable, kv.GetSchema()))
		require.NoError(t, migrate.MountAndWait(ctx, env.YT, testTable))

		return kv.NewClient(env.YT, testTable)
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
		kvClient := setup(t)

		value, err := kvClient.Lookup(ctx, key)
		require.NoError(t, err)
		require.Nil(t, value)

		require.NoError(t, kvClient.Write(ctx, key, referenceValue))

		value, err = kvClient.Lookup(ctx, key)
		require.NoError(t, err)
		require.Equal(t, &referenceValue, value)

		require.NoError(t, kvClient.Delete(ctx, key))

		value, err = kvClient.Lookup(ctx, key)
		require.NoError(t, err)
		require.Nil(t, value)
	})

	t.Run("MultiOps", func(t *testing.T) {
		kvClient := setup(t)

		records, err := kvClient.LookupMany(ctx, keys)
		require.NoError(t, err)
		require.Empty(t, records)

		require.NoError(t, kvClient.WriteMany(ctx, referenceRecords))

		records, err = kvClient.LookupMany(ctx, keys)
		require.NoError(t, err)
		require.Equal(t, referenceRecords, records)

		require.NoError(t, kvClient.DeleteMany(ctx, keys))

		records, err = kvClient.LookupMany(ctx, keys)
		require.NoError(t, err)
		require.Empty(t, records)
	})

	t.Run("Transaction", func(t *testing.T) {
		kvClient := setup(t)

		tx, err := kvClient.StartTx(ctx)
		require.NoError(t, err)

		require.NoError(t, tx.Write(ctx, key, referenceValue))
		require.NoError(t, tx.Commit())

		value, err := kvClient.Lookup(ctx, key)
		require.NoError(t, err)
		require.Equal(t, &referenceValue, value)

		tx, err = kvClient.StartTx(ctx)
		require.NoError(t, err)

		tx2, err := kvClient.StartTx(ctx)
		require.NoError(t, err)

		require.NoError(t, tx2.Write(ctx, key, referenceValue2))
		require.NoError(t, tx.Write(ctx, key, referenceValue))
		require.NoError(t, tx2.Commit())
		require.Error(t, tx.Commit())

		value, err = kvClient.Lookup(ctx, key)
		require.NoError(t, err)
		require.Equal(t, &referenceValue2, value)
	})

	t.Run("Select", func(t *testing.T) {
		kvClient := setup(t)

		require.NoError(t, kvClient.WriteMany(ctx, referenceRecords))

		records, err := kvClient.Select(ctx, "WHERE is_substr(\"2\", key)")
		require.NoError(t, err)
		require.Equal(t, map[string]string{key2: referenceValue2}, records)
	})
}
