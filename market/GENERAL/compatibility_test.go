package plugins

import (
	"a.yandex-team.ru/market/sre/tools/prostarter/internal/env"
	"a.yandex-team.ru/market/sre/tools/prostarter/internal/loader"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"os"
	"testing"
)

func TestCompatibility(t *testing.T) {
	clearEnv := func() {
		_ = os.Unsetenv(env.Compatibility)
	}
	clearEnv()
	defer clearEnv()

	values := loader.Values{
		MemoryBytesGuarantee: 1,
		CPUMilsGuarantee:     2,
		CPUCoresGuarantee:    3,
		ConfigPath:           "/c",
		LogPath:              "/l",
		LogFile:              "/l/f.l",
		RootPath:             "/r",
		PersistentDataPath:   "/pd",
		TmpPath:              "/tmp",
		DataGetterPath:       "/dg",
		DC:                   "dc",
		ENV:                  "env",
		ListenHost:           "::",
		ListenAppPort:        4,
		ListenDebugPort:      5,
	}
	args := Compatibility(values)
	var empty []string
	assert.Equal(t, empty, args)

	err := os.Setenv(env.Compatibility, "JaVa")
	require.NoError(t, err)

	args = Compatibility(values)
	assert.Equal(t, []string{
		"--logdir=/l",
		"--httpport=4",
		"--debugport=5",
		"--tmpdir=/tmp",
		"--datadir=/pd",
		"--extdatadir=/dg",
		"--environment=env",
		"--dc=dc",
		"--cpu-count=3",
	}, args)

	err = os.Setenv(env.Compatibility, "CPP")
	require.NoError(t, err)

	args = Compatibility(values)
	assert.Equal(t, []string{
		"--config=/c",
		"--host=::",
		"--port=4",
		"--log-path=/l/f.l",
		"--dc=dc",
		"--env=env",
		"--root-path=/r",
		"--listen-threads=3",
	}, args)

	err = os.Setenv(env.Compatibility, "pYTHON")
	require.NoError(t, err)

	args = Compatibility(values)
	assert.Equal(t, []string{
		"--config=/c",
		"--host=::",
		"--port=4",
		"--log-path=/l/f.l",
		"--dc=dc",
		"--env=env",
		"--root-path=/r",
		"--listen-threads=3",
	}, args)
}
