package testutils

import (
	"testing"

	"go.uber.org/zap"
	"go.uber.org/zap/zaptest"

	yzap "a.yandex-team.ru/library/go/core/log/zap"
)

func NewLogger(t *testing.T) *yzap.Logger {
	return &yzap.Logger{L: zaptest.NewLogger(t, zaptest.Level(zap.DebugLevel))}
}
