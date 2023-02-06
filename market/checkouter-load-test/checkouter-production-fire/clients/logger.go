package clients

import (
	"a.yandex-team.ru/library/go/core/log/zap"
	"a.yandex-team.ru/library/go/core/log/zap/encoders"
	uberzap "go.uber.org/zap"
	"go.uber.org/zap/zapcore"
	"gopkg.in/natefinch/lumberjack.v2"
	"io"
	"os"
	"path/filepath"
)

var RequestLogger = CreateRequestLogger()
var RequestIndexLogger = CreateRequestIndexLogger()
var StocksLogger = CreateStocksLogger()

func GetRequestLogger() *uberzap.Logger {
	return RequestLogger
}

func GetRequestIndexLogger() *uberzap.Logger {
	return RequestIndexLogger
}

func GetStocksLogger() *uberzap.Logger {
	return StocksLogger
}

func CreateStocksLogger() *uberzap.Logger {
	return CreateLumberjackLogger(&lumberjack.Logger{
		Filename:   GetCurrentDir() + "/stocks.log",
		MaxSize:    500, // megabytes
		MaxBackups: 0,
		MaxAge:     1, // days
		Compress:   false,
	})
}

func CreateRequestLogger() *uberzap.Logger {
	return CreateLumberjackLogger(&lumberjack.Logger{
		Filename:   GetCurrentDir() + "/requests.log",
		MaxSize:    500, // megabytes
		MaxBackups: 0,
		MaxAge:     1, // days
		Compress:   true,
	})
}

func CreateRequestIndexLogger() *uberzap.Logger {
	return CreateLumberjackLogger(&lumberjack.Logger{
		Filename:   GetCurrentDir() + "/requests-index.log",
		MaxSize:    1024, // megabytes
		MaxBackups: 0,
		MaxAge:     1, // days
		Compress:   false,
	})
}

func GetCurrentDir() string {
	executable, exErr := os.Executable()
	if exErr != nil {
		panic(exErr)
	}
	return filepath.Dir(executable)
}

func CreateLumberjackLogger(lumberjackLogger io.Writer) *uberzap.Logger {
	w := zapcore.AddSync(lumberjackLogger)
	encoder, err := encoders.NewTSKVEncoder(zap.NewDeployEncoderConfig())
	if err != nil {
		panic(err)
	}
	core := zapcore.NewCore(
		encoder,
		w,
		zapcore.InfoLevel)
	return uberzap.New(core)
}
