package updater

import (
	"context"
	"sync"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/testutils"
)

func TestUpdateAll(t *testing.T) {
	upd := newTestScheduler(t)

	loadCount := 0
	fn := func() error {
		loadCount++
		return nil
	}

	period := 100 * time.Microsecond
	upd.AddUpdateRule("test-rule1", period, fn)
	upd.AddUpdateRule("test-rule2", period, fn)

	require.NoError(t, upd.UpdateAll(context.Background()))
	require.Equal(t, 2, loadCount)
}

func TestContinueUpdatingAfterSuccess(t *testing.T) {
	upd := newTestScheduler(t)

	runCount := 0
	loadCount := 3

	wg := sync.WaitGroup{}
	wg.Add(loadCount)

	fn := func() error {
		runCount++
		if runCount%2 == 1 && loadCount > 0 {
			loadCount--
			wg.Done()
			return nil
		}
		return AlreadyUpdated
	}

	period := 100 * time.Microsecond
	upd.AddUpdateRule("test-rule1", period, fn)
	upd.AddUpdateRule("test-rule2", period, fn)

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()
	upd.RunUpdating(ctx)
	wg.Wait()

	require.True(t, runCount > 3)
}

func newTestScheduler(t *testing.T) *Updater {
	logger := testutils.NewLogger(t)
	upd := NewUpdater("test", logger)
	return upd
}
