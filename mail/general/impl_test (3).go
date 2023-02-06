package weather

import (
	"a.yandex-team.ru/mail/iex/taksa/client"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"testing"
	"time"
)

import . "a.yandex-team.ru/mail/iex/matchers"

type emptyError struct {
}

func (e emptyError) Error() string {
	return ""
}

func TestGet_zeroGeo_returnsError(t *testing.T) {
	impl := Impl{Log: logger.Mock{}, Cli: client.Mock{}}
	_, err := impl.Get(0, time.Time{})
	AssertThat(t, err, Not{V: nil})
}

func TestGet_httpError_returnsError(t *testing.T) {
	impl := Impl{Log: logger.Mock{}, Cli: client.Mock{Err: emptyError{}}}
	_, err := impl.Get(1, time.Time{})
	AssertThat(t, err, Not{V: nil})
}

func TestGet_badJson_returnsError(t *testing.T) {
	impl := Impl{Log: logger.Mock{}, Cli: client.Mock{Data: "fwkuey74w"}}
	_, err := impl.Get(1, time.Time{})
	AssertThat(t, err, Not{V: nil})
}

func TestGet_timeNotFound_returnsError(t *testing.T) {
	resp := `{"forecasts":[{"hours":[{"hour_ts": 1444597200, "temp": 2}]}]}`
	impl := Impl{Log: logger.Mock{}, Cli: client.Mock{Data: resp}}
	_, err := impl.Get(1, time.Unix(1544597200, 0))
	AssertThat(t, err, Not{V: nil})
}

func TestGet_aboveZero_returnsTempWithPlusSign(t *testing.T) {
	resp := `{"forecasts":[{"hours":[{"hour_ts": 1444597200, "temp": 2}]}]}`
	impl := Impl{Log: logger.Mock{}, Cli: client.Mock{Data: resp}}
	temp, err := impl.Get(1, time.Unix(1444597201, 0))
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, temp, Is{V: "+2°C"})
}

func TestGet_belowZero_returnsTempWithMinusSign(t *testing.T) {
	resp := `{"forecasts":[{"hours":[{"hour_ts": 1444597200, "temp": -2}]}]}`
	impl := Impl{Log: logger.Mock{}, Cli: client.Mock{Data: resp}}
	temp, err := impl.Get(1, time.Unix(1444597201, 0))
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, temp, Is{V: "-2°C"})
}
