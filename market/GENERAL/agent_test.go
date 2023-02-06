package agent

import (
	"a.yandex-team.ru/library/go/core/log"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"testing"
)

type LoggerMock struct {
	log.Logger
	mock.Mock
}

func (l *LoggerMock) Infof(format string, args ...interface{}) {
	l.Called(format, args)
}

func (l *LoggerMock) Fatalf(format string, args ...interface{}) {
	l.Called(format, args)
}

func (l *LoggerMock) Info(msg string, fields ...log.Field) {
	l.Called(msg, fields)
}

func TestAgent(t *testing.T) {
	logger := new(LoggerMock)
	a := NewAgent(logger)
	a.NewEcho()

	assert.NotNil(t, a.Logger)
	assert.NotNil(t, a.Echo)

	logger.On("Infof", mock.Anything, mock.Anything).Once()
	logger.On("Fatalf", mock.Anything, mock.Anything).Once()
	a.StartEcho("invalid:address")

	logger.AssertExpectations(t)
}
