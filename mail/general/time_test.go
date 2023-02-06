package tutil

import (
	"testing"
	"time"
)

import . "a.yandex-team.ru/mail/iex/matchers"

func TestTimeUnmarshal_5s_5Seconds(t *testing.T) {
	var timeout Timeout
	_ = timeout.UnmarshalJSON([]byte("5s"))
	AssertThat(t, time.Duration(timeout), Is{V: time.Duration(5) * time.Second})
}

func TestTimeUnmarshal_5ms_5Milliseconds(t *testing.T) {
	var timeout Timeout
	_ = timeout.UnmarshalJSON([]byte("5ms"))
	AssertThat(t, time.Duration(timeout), Is{V: time.Duration(5) * time.Millisecond})
}

func TestTimeUnmarshal_5us_5Microseconds(t *testing.T) {
	var timeout Timeout
	_ = timeout.UnmarshalJSON([]byte("5us"))
	AssertThat(t, time.Duration(timeout), Is{V: time.Duration(5) * time.Microsecond})
}

func TestTimeUnmarshal_invalidDuration_givesError(t *testing.T) {
	var timeout Timeout
	e := timeout.UnmarshalJSON([]byte("zzz"))
	AssertThat(t, e, Not{V: Is{V: nil}})
}
