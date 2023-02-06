package logging

import (
	"os"
	"syscall"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/library/go/core/log"
)

func TestLogrotate(t *testing.T) {
	claim := assert.New(t)

	file, err := os.CreateTemp("", "log_")
	claim.NoError(err)

	l, err := NewYtFileLogger(log.DebugLevel, file.Name())
	claim.NoError(err)
	l.Info("Hello!")
	err = l.L.Sync()
	claim.NoError(err)
	l.Info("How are you?")

	actual, err := os.ReadFile(file.Name())
	claim.NoError(err)
	claim.Equal("Hello!\nHow are you?\n", string(actual))

	proc, err := os.FindProcess(os.Getpid())
	claim.NoError(err)
	err = os.Remove(file.Name())
	claim.NoError(err)
	err = proc.Signal(syscall.SIGUSR1)
	claim.NoError(err)
	time.Sleep(time.Second)

	l.Info("Hello!")

	actual, err = os.ReadFile(file.Name())
	claim.NoError(err)
	claim.Equal("Hello!\n", string(actual))
}
