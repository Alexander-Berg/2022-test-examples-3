package app

import (
	"strings"
	"testing"

	"github.com/stretchr/testify/require"

	httpclient "a.yandex-team.ru/market/combinator/pkg/http_client"
	"a.yandex-team.ru/market/combinator/pkg/util/envtype"
)

func TestParseFlagsProduction(t *testing.T) {
	cmdline := "bin/combinator-app -httpport 1000 -grpcport 1001 -tvmport 1002 -logdir logs/combinator -envtype production"
	args := strings.Split(cmdline, " ")
	config, err := ParseFlags(args[0], args[1:])

	require.NoError(t, err)
	require.Equal(t, 1000, config.HTTPPort)
	require.Equal(t, 1001, config.GRPCPort)
	require.Equal(t, 1002, config.TVMPort)
	require.Equal(t, envtype.Production, config.Env)
	require.Equal(t, hahn, config.YtConfig.Proxy)
	require.Equal(t, arnold, config.SecondaryYtConfig.Proxy)
	require.Equal(t, "//home/market/production/indexer/combinator", config.TablesDir)
	require.Equal(t, "logs/combinator", config.LogDir)
	require.Equal(t, "", config.LocalRootDir)
	require.Equal(t, 10, config.MaxProcs)
	require.Equal(t, httpclient.ClientTypeReal, config.TaxiClientType)
}

func TestParseFlagsTesting(t *testing.T) {
	cmdline := "bin/combinator-app -httpport 1000 -grpcport 1001 -tvmport 1002 -logdir logs/combinator -envtype testing"
	args := strings.Split(cmdline, " ")
	config, err := ParseFlags(args[0], args[1:])

	require.NoError(t, err)
	require.Equal(t, 1000, config.HTTPPort)
	require.Equal(t, 1001, config.GRPCPort)
	require.Equal(t, 1002, config.TVMPort)
	require.Equal(t, envtype.Testing, config.Env)
	require.Equal(t, hahn, config.YtConfig.Proxy)
	require.Equal(t, "//home/market/testing/indexer/combinator", config.TablesDir)
	require.Equal(t, "logs/combinator", config.LogDir)
	require.Equal(t, "", config.LocalRootDir)
	require.Equal(t, 2, config.MaxProcs)
	require.Equal(t, httpclient.ClientTypeReal, config.TaxiClientType)
}

func TestParseFlagsDev(t *testing.T) {
	{
		cmdline := "bin/combinator-app"
		args := strings.Split(cmdline, " ")
		config, err := ParseFlags(args[0], args[1:])

		require.NoError(t, err)
		require.Equal(t, 3000, config.HTTPPort)
		require.Equal(t, 3001, config.GRPCPort)
		require.Equal(t, 3223, config.TVMPort)
		require.Equal(t, envtype.Development, config.Env)
		require.Equal(t, hahn, config.YtConfig.Proxy)
		require.Equal(t, "//home/market/production/indexer/combinator", config.TablesDir)
		require.Equal(t, "root/logs/combinator", config.LogDir)
		require.Equal(t, "", config.LocalRootDir)
		require.Equal(t, 0, config.MaxProcs)
		require.Equal(t, httpclient.ClientTypeReal, config.TaxiClientType)
	}
	{
		cmdline := "bin/combinator-app -local-dir pdata -threads=2"
		args := strings.Split(cmdline, " ")
		config, err := ParseFlags(args[0], args[1:])

		require.NoError(t, err)
		require.Equal(t, 3000, config.HTTPPort)
		require.Equal(t, 3001, config.GRPCPort)
		require.Equal(t, 3223, config.TVMPort)
		require.Equal(t, envtype.Development, config.Env)
		require.Equal(t, hahn, config.YtConfig.Proxy)
		require.Equal(t, "//home/market/production/indexer/combinator", config.TablesDir)
		require.Equal(t, "root/logs/combinator", config.LogDir)
		require.Equal(t, "pdata", config.LocalRootDir)
		require.Equal(t, 2, config.MaxProcs)
		require.Equal(t, httpclient.ClientTypeReal, config.TaxiClientType)
	}
}

func TestParseFlagsLoadTesting(t *testing.T) {
	cmdline := "bin/combinator-app -httpport 1000 -grpcport 1001 -tvmport 1002 -envtype loadtesting"
	args := strings.Split(cmdline, " ")
	config, err := ParseFlags(args[0], args[1:])

	require.NoError(t, err)
	require.Equal(t, 1000, config.HTTPPort)
	require.Equal(t, 1001, config.GRPCPort)
	require.Equal(t, 1002, config.TVMPort)
	require.Equal(t, envtype.Production, config.Env)
	require.Equal(t, hahn, config.YtConfig.Proxy)
	require.Equal(t, "//home/market/production/indexer/combinator", config.TablesDir)
	require.Equal(t, "logs/combinator", config.LogDir)
	require.Equal(t, "", config.LocalRootDir)
	require.Equal(t, 10, config.MaxProcs)
	require.Equal(t, httpclient.ClientTypeDummy, config.TaxiClientType)
}
