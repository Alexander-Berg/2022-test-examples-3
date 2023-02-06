package plugins

import (
	"a.yandex-team.ru/market/sre/tools/prostarter/internal/env"
	"a.yandex-team.ru/market/sre/tools/prostarter/internal/loader"
	"github.com/stretchr/testify/assert"
	"os"
	"testing"
)

func fullEnv(value string) string {
	return env.Prefix + "_" + value
}

func TestSystem(t *testing.T) {
	clearEnv := func() {
		_ = os.Unsetenv(fullEnv("STARTER"))
		_ = os.Unsetenv(fullEnv("MEMORY_GUARANTEE"))
		_ = os.Unsetenv(fullEnv("CPUMILS_GUARANTEE"))
		_ = os.Unsetenv(fullEnv("CPUCORES_GUARANTEE"))
		_ = os.Unsetenv(fullEnv("MEMORY_LIMIT"))
		_ = os.Unsetenv(fullEnv("CPUMILS_LIMIT"))
		_ = os.Unsetenv(fullEnv("CPUCORES_LIMIT"))
		_ = os.Unsetenv(fullEnv("CONFIG_PATH"))
		_ = os.Unsetenv(fullEnv("LOG_PATH"))
		_ = os.Unsetenv(fullEnv("LOG_FILE"))
		_ = os.Unsetenv(fullEnv("ROOT_PATH"))
		_ = os.Unsetenv(fullEnv("PERSISTENTDATA_PATH"))
		_ = os.Unsetenv(fullEnv("TEMP_PATH"))
		_ = os.Unsetenv(fullEnv("DATAGETTER_PATH"))
		_ = os.Unsetenv(fullEnv("DC"))
		_ = os.Unsetenv(fullEnv("ENV"))
		_ = os.Unsetenv(fullEnv("LISTEN_HOST"))
		_ = os.Unsetenv(fullEnv("LISTEN_WEB_PORT"))
		_ = os.Unsetenv(fullEnv("LISTEN_WEB_SSL_PORT"))
		_ = os.Unsetenv(fullEnv("LISTEN_APP_PORT"))
		_ = os.Unsetenv(fullEnv("LISTEN_GRPC_PORT"))
		_ = os.Unsetenv(fullEnv("LISTEN_GRPC_SSL_PORT"))
		_ = os.Unsetenv(fullEnv("LISTEN_GRPC_APP_PORT"))
		_ = os.Unsetenv(fullEnv("LISTEN_DEBUG_PORT"))
		_ = os.Unsetenv(fullEnv("VOLUMES"))
	}
	clearEnv()
	defer clearEnv()

	values := loader.Values{
		MemoryBytesGuarantee: 1,
		CPUMilsGuarantee:     2,
		CPUCoresGuarantee:    3,
		MemoryBytesLimit:     4,
		CPUMilsLimit:         5,
		CPUCoresLimit:        6,
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
		ListenWebPort:        7,
		ListenWebSSLPort:     8,
		ListenAppPort:        9,
		ListenGRPCPort:       10,
		ListenGRPCSSLPort:    11,
		ListenGRPCAppPort:    12,
		ListenDebugPort:      13,
		Volumes:              map[string]loader.Volume{"/": {MountPoint: "/", Quota: 14}, "/test": {MountPoint: "/test", Quota: 15}},
	}

	err := System(values)
	assert.NoError(t, err)
	assert.Equal(t, "true", os.Getenv(fullEnv("IS_STARTER")))
	assert.Equal(t, "1", os.Getenv(fullEnv("MEMORY_GUARANTEE")))
	assert.Equal(t, "2", os.Getenv(fullEnv("CPUMILS_GUARANTEE")))
	assert.Equal(t, "3", os.Getenv(fullEnv("CPUCORES_GUARANTEE")))
	assert.Equal(t, "4", os.Getenv(fullEnv("MEMORY_LIMIT")))
	assert.Equal(t, "5", os.Getenv(fullEnv("CPUMILS_LIMIT")))
	assert.Equal(t, "6", os.Getenv(fullEnv("CPUCORES_LIMIT")))
	assert.Equal(t, "/c", os.Getenv(fullEnv("CONFIG_PATH")))
	assert.Equal(t, "/l", os.Getenv(fullEnv("LOG_PATH")))
	assert.Equal(t, "/l/f.l", os.Getenv(fullEnv("LOG_FILE")))
	assert.Equal(t, "/r", os.Getenv(fullEnv("ROOT_PATH")))
	assert.Equal(t, "/pd", os.Getenv(fullEnv("PERSISTENTDATA_PATH")))
	assert.Equal(t, "/tmp", os.Getenv(fullEnv("TEMP_PATH")))
	assert.Equal(t, "/dg", os.Getenv(fullEnv("DATAGETTER_PATH")))
	assert.Equal(t, "dc", os.Getenv(fullEnv("DC")))
	assert.Equal(t, "env", os.Getenv(fullEnv("ENV")))
	assert.Equal(t, "::", os.Getenv(fullEnv("LISTEN_HOST")))
	assert.Equal(t, "7", os.Getenv(fullEnv("LISTEN_WEB_PORT")))
	assert.Equal(t, "8", os.Getenv(fullEnv("LISTEN_WEB_SSL_PORT")))
	assert.Equal(t, "9", os.Getenv(fullEnv("LISTEN_APP_PORT")))
	assert.Equal(t, "10", os.Getenv(fullEnv("LISTEN_GRPC_PORT")))
	assert.Equal(t, "11", os.Getenv(fullEnv("LISTEN_GRPC_SSL_PORT")))
	assert.Equal(t, "12", os.Getenv(fullEnv("LISTEN_GRPC_APP_PORT")))
	assert.Equal(t, "13", os.Getenv(fullEnv("LISTEN_DEBUG_PORT")))
	assert.Equal(t, "{\"/\":{\"mountPoint\":\"/\",\"quota\":14},\"/test\":{\"mountPoint\":\"/test\",\"quota\":15}}", os.Getenv(fullEnv("VOLUMES")))
}
