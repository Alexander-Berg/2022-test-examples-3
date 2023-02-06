package loader

import (
	"a.yandex-team.ru/library/go/yandex/deploy/podagent"
	"a.yandex-team.ru/market/sre/tools/prostarter/internal/env"
	"context"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"os"
	"path/filepath"
	"testing"
)

func TestCPUMilsToCores(t *testing.T) {
	assert.Equal(t, uint64(1), CPUMilsToCores(uint64(500)))
	assert.Equal(t, uint64(2), CPUMilsToCores(uint64(2134)))
}

func TestGetLoader(t *testing.T) {
	clearEnv := func() {
		_ = os.Unsetenv(env.DumpJSON)
		_ = os.Unsetenv(podagent.EnvPodIDKey)
	}
	clearEnv()
	defer clearEnv()

	ctx := context.Background()
	_, err := GetLoader(ctx, "test")
	assert.Error(t, err)

	dir, cancel, err := createTestDumpJSON()
	if cancel != nil {
		defer cancel()
	}
	require.NoError(t, err)
	err = os.Setenv(env.DumpJSON, filepath.Join(dir, DumpJSONName))
	require.NoError(t, err)
	loader, err := GetLoader(ctx, "test")
	assert.NoError(t, err)
	assert.Equal(t, NannyName, loader.LoaderName())

	err = os.Setenv(podagent.EnvPodIDKey, "1")
	require.NoError(t, err)
	loader, err = GetLoader(ctx, "test")
	assert.NoError(t, err)
	assert.Equal(t, DeployName, loader.LoaderName())
}
