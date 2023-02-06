package ytlocker

import (
	"testing"

	"a.yandex-team.ru/yt/go/ypath"
	"a.yandex-team.ru/yt/go/yttest"

	"github.com/stretchr/testify/require"

	"github.com/tus/tusd/pkg/handler"
)

func TestYtLockConflict(t *testing.T) {
	env, cancel := yttest.NewEnv(t)
	defer cancel()

	locker := New(env.YT, ypath.Path("//tmp"))

	lock, _ := locker.NewLock("a")
	require.NoError(t, lock.Lock())

	lock2, _ := locker.NewLock("a")
	require.Equal(t, handler.ErrFileLocked, lock2.Lock())

	require.NoError(t, lock.Unlock())
}

func TestYtLockTwice(t *testing.T) {
	env, cancel := yttest.NewEnv(t)
	defer cancel()

	locker := New(env.YT, ypath.Path("//tmp"))
	lock, _ := locker.NewLock("a")

	require.NoError(t, lock.Lock())
	require.NoError(t, lock.Unlock())
	require.NoError(t, lock.Lock())
	require.NoError(t, lock.Unlock())
}
