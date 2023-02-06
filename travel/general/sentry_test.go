package logging

import (
	"testing"

	"github.com/getsentry/sentry-go"
	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/xerrors"
	"a.yandex-team.ru/travel/library/go/errutil"
)

func Test_filterFrames(t *testing.T) {
	var err error
	var l log.Logger

	claim := assert.New(t)

	claim.NoError(err)
	var events []*sentry.Event
	l, err = NewDeploy(&Config{
		Level:       "debug",
		SentryDSN:   "http://user:password@localhost/123",
		SentryLevel: "error",
	})
	claim.NoError(err)
	sentry.AddGlobalEventProcessor(func(event *sentry.Event, hint *sentry.EventHint) *sentry.Event {
		events = append(events, event)
		return nil
	})
	a(l)

	claim.Len(events, 1)
	event := events[0]
	exceptions := event.Exception
	claim.Len(exceptions, 3)
	exception := exceptions[len(exceptions)-1]
	claim.Len(exception.Stacktrace.Frames, 3) // Test_filterFrames + a + b
	claim.Equal(exception.Stacktrace.Frames[2].Function, "b")
	claim.Equal(exception.Stacktrace.Frames[1].Function, "a")
	claim.Equal(exception.Stacktrace.Frames[0].Function, "Test_filterFrames")

	exception = exceptions[0]
	claim.Len(exception.Stacktrace.Frames, 1) // c from xerrors
	claim.Equal(exception.Stacktrace.Frames[0].Function, "d")

	exception = exceptions[1]
	claim.Len(exception.Stacktrace.Frames, 1) // c from xerrors
	claim.Equal(exception.Stacktrace.Frames[0].Function, "c")

	claim.Contains(event.Extra, "qqq")
	claim.Equal("zzz", event.Extra["qqq"])
}

func a(logger log.Logger) {
	b(logger)
}

func b(logger log.Logger) {
	logger.Error("Some error", log.Error(c()))
}

func c() error {
	return xerrors.Errorf("wrapped: %w", d())
}

func d() error {
	return errutil.NewTracedError(xerrors.Errorf("occured"), errutil.Field("qqq", "zzz"))
}
