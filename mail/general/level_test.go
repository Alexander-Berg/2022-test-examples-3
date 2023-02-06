package logger

import (
	"testing"
)

import . "a.yandex-team.ru/mail/iex/matchers"

func TestLevelString(t *testing.T) {
	AssertThat(t, DebugLevel.String(), Is{V: "debug"})
	AssertThat(t, InfoLevel.String(), Is{V: "info"})
	AssertThat(t, ErrorLevel.String(), Is{V: "error"})
}

func TestLevelByName(t *testing.T) {
	AssertThat(t, levelByName("debug"), Is{V: DebugLevel})
	AssertThat(t, levelByName("info"), Is{V: InfoLevel})
	AssertThat(t, levelByName("error"), Is{V: ErrorLevel})
}
