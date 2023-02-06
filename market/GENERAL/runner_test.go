package runner

import (
	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"
	"a.yandex-team.ru/market/sre/services/remon/internal/flags"
	"context"
	"github.com/heetch/confita"
	"github.com/heetch/confita/backend/env"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"go.uber.org/zap/zapcore"
	"io/ioutil"
	"os"
	"path"
	"testing"
	"time"
)

func setupLogger() log.Logger {
	logger, _ := zap.New(zap.ConsoleConfig(log.InfoLevel))
	return logger
}

func TestNewRunner(t *testing.T) {
	ctx := context.Background()

	err := os.Setenv("YT_TOKEN", "test")
	require.NoError(t, err)
	err = os.Setenv("YT_LOCK_PATH", "//test")
	require.NoError(t, err)

	loader := confita.NewLoader(env.NewBackend())
	f := new(flags.Flags)

	r, err := NewRunner(ctx, f, loader)
	assert.NoError(t, err)
	assert.NotNil(t, r.Logger)
	assert.NotNil(t, r.RestyClient)
	assert.NotNil(t, r.Context)
	assert.NotNil(t, r.Config)
	assert.NotNil(t, r.YTClient)
	assert.Equal(t, 1, len(r.Collectors))
}

func TestRunnerCollectAll(t *testing.T) {
	logger := setupLogger()

	collected := false

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	r := Runner{}
	r.Context = ctx
	r.Config = DefaultConfig()
	r.Logger = logger
	r.Collectors = []Collector{
		(&TestCollector{}).SetCollectFunc(func(ctx context.Context, pool *PoolGroup) {
			collected = true
		}),
	}

	r.CollectAll()
	assert.Equal(t, true, collected)

	collected = false
	r.Config.Collector.CollectTimeout = time.Millisecond * 200
	r.Collectors = []Collector{
		(&TestCollector{}).SetCollectFunc(func(ctx context.Context, pool *PoolGroup) {
			select {
			case <-time.After(time.Millisecond * 400):
				collected = true
			case <-ctx.Done():
			}
		}),
	}

	r.CollectAll()
	assert.Equal(t, false, collected)

	collected = false
	r.Config.Collector.CollectTimeout = time.Second * 2

	go r.CollectAll()
	time.Sleep(time.Millisecond * 200)
	cancel()

	assert.Equal(t, false, collected)
}

func TestSetupLogger(t *testing.T) {
	c := DefaultConfig()

	logger, err := SetupLogger(c)
	assert.NoError(t, err)
	zapLogger := logger.(*zap.Logger)
	assert.Equal(t, true, zapLogger.L.Core().Enabled(zapcore.InfoLevel))
	assert.Equal(t, false, zapLogger.L.Core().Enabled(zapcore.DebugLevel))

	dir, err := ioutil.TempDir("", "log")
	require.NoError(t, err)
	defer os.RemoveAll(dir)

	c.LOG.Level = "ERROR"
	c.LOG.Path = path.Join(dir, "test.log")
	_, err = os.Stat(c.LOG.Path)
	assert.Error(t, err)

	logger, err = SetupLogger(c)
	zapLogger = logger.(*zap.Logger)
	assert.NoError(t, err)

	logFile, err := os.Stat(c.LOG.Path)
	assert.NoError(t, err)
	assert.Equal(t, int64(0), logFile.Size())
	assert.Equal(t, true, zapLogger.L.Core().Enabled(zapcore.ErrorLevel))
	assert.Equal(t, false, zapLogger.L.Core().Enabled(zapcore.InfoLevel))
	logger.Error("test")
	logFile, err = os.Stat(c.LOG.Path)
	assert.NoError(t, err)
	assert.NotEqual(t, int64(0), logFile.Size())
}

func TestPoolGroup(t *testing.T) {
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()
	pool := NewPoolGroup(ctx, 1)
	assert.NotNil(t, pool)

	do := pool.AddToPool()
	assert.Equal(t, true, do)

	added := false
	go func() {
		pool.AddToPool()
		defer pool.PoolDone()
		time.Sleep(time.Millisecond * 100)
		added = true
	}()

	time.Sleep(time.Millisecond * 50)
	assert.Equal(t, false, added)
	pool.PoolDone()

	time.Sleep(time.Millisecond * 50)
	assert.Equal(t, false, added)

	pool.PoolWait()
	assert.Equal(t, true, added)

	cancel()
	time.Sleep(time.Millisecond * 50)

	do = pool.AddToPool()
	assert.Equal(t, false, do)
}
