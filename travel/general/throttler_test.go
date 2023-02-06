package logthrottler

import (
	"a.yandex-team.ru/library/go/core/log"
	"github.com/stretchr/testify/mock"
	"testing"
	"time"
)

type mockLogger struct {
	mock.Mock
}

func (ml *mockLogger) mockLogFunction(msg string, fields ...log.Field) {
	ml.Mock.Called(msg, fields)
}

func TestLogWithThrottling(t *testing.T) {
	logger := &mockLogger{}
	logger.Mock.On("mockLogFunction", mock.AnythingOfType("string"), mock.AnythingOfType("[]log.Field"))
	LogWithThrottling(
		"my-descriptor",
		1*time.Second,
		logger.mockLogFunction,
		"Hello, world!",
	)

	logger.Mock.AssertNumberOfCalls(t, "mockLogFunction", 1)

	LogWithThrottling(
		"my-descriptor",
		1*time.Second,
		logger.mockLogFunction,
		"Hello, world!",
	)

	logger.Mock.AssertNumberOfCalls(t, "mockLogFunction", 1)
	time.Sleep(2 * time.Second)

	LogWithThrottling(
		"my-descriptor",
		1*time.Second,
		logger.mockLogFunction,
		"Hello, world!",
	)
	logger.Mock.AssertNumberOfCalls(t, "mockLogFunction", 2)

}

func TestLogWithThrottlingStructDescriptor(t *testing.T) {
	logger := &mockLogger{}
	logger.Mock.On("mockLogFunction", mock.AnythingOfType("string"), mock.AnythingOfType("[]log.Field"))
	type mydescriptor struct {
		v string
	}

	LogWithThrottling(
		mydescriptor{"my-descriptor"},
		1*time.Second,
		logger.mockLogFunction,
		"Hello, world!",
	)

	logger.Mock.AssertNumberOfCalls(t, "mockLogFunction", 1)

	LogWithThrottling(
		mydescriptor{"my-descriptor"},
		1*time.Second,
		logger.mockLogFunction,
		"Hello, world!",
	)

	logger.Mock.AssertNumberOfCalls(t, "mockLogFunction", 1)
	time.Sleep(2 * time.Second)

	LogWithThrottling(
		mydescriptor{"my-descriptor"},
		1*time.Second,
		logger.mockLogFunction,
		"Hello, world!",
	)
	logger.Mock.AssertNumberOfCalls(t, "mockLogFunction", 2)

}

func TestLogWithThrottlingDifferentTypesSameValue(t *testing.T) {
	logger := &mockLogger{}
	logger.Mock.On("mockLogFunction", mock.AnythingOfType("string"), mock.AnythingOfType("[]log.Field"))
	type mydescriptor string
	type otherdescriptor string

	LogWithThrottling(
		mydescriptor("my-descriptor"),
		1*time.Second,
		logger.mockLogFunction,
		"Hello, world!",
	)

	logger.Mock.AssertNumberOfCalls(t, "mockLogFunction", 1)

	LogWithThrottling(
		otherdescriptor("my-descriptor"),
		1*time.Second,
		logger.mockLogFunction,
		"Hello, world!",
	)

	logger.Mock.AssertNumberOfCalls(t, "mockLogFunction", 2)
}
