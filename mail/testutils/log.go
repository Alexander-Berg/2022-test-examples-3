package testutils

import (
	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"
	"go.uber.org/zap/zapcore"
)

func NewLogger() log.Logger {
	logConfig := zap.ConsoleConfig(log.DebugLevel)
	logConfig.EncoderConfig.EncodeLevel = zapcore.CapitalColorLevelEncoder
	logger, _ := zap.New(logConfig)
	return logger
}
