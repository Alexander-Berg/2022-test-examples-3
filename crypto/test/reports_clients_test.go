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

func TestReportsClient(t *testing.T) {
	ytEnv, cancel := yttest.NewEnv(t)
	defer cancel()

	ctx, cancel := context.WithTimeout(ytEnv.Ctx, time.Second*15)
	defer cancel()

	testTable := ytEnv.TmpPath().Child("table")
	require.NoError(t, migrate.Create(ctx, ytEnv.YT, testTable, kv.GetSchema()))
	require.NoError(t, migrate.MountAndWait(ctx, ytEnv.YT, testTable))

	client := kv.NewClient(ytEnv.YT, testTable)
	reportsClient := rsserver.NewReportsClient(client, time.Second)

	const (
		env       = "env"
		resource  = "resource"
		version   = uint64(10)
		clientID1 = "clientID1"
		clientID2 = "clientID2"
	)

	require.NoError(t, reportsClient.ReportVersion(ctx, env, resource, version, clientID1))
	require.NoError(t, reportsClient.ReportVersion(ctx, env, resource, version, clientID2))
	require.NoError(t, reportsClient.ReportOk(ctx, env, resource, version, clientID1))
	require.NoError(t, reportsClient.ReportOk(ctx, env, resource, version, clientID2))
	require.NoError(t, reportsClient.ReportVersion(ctx, env, resource, version, clientID1))

	resource1Reports, err := reportsClient.GetReportCounts(ctx, env, resource, version)
	require.NoError(t, err)
	require.Equal(
		t,
		rsserver.ReportCounts{
			OkCount:      2,
			VersionCount: 2,
		},
		resource1Reports,
	)
}
