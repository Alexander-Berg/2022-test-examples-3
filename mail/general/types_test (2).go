package tutil

import (
	"testing"
)

import . "a.yandex-team.ru/mail/iex/matchers"

func TestGetTimeFromSomething_bool_error(t *testing.T) {
	_, err := GetTimeFromSomething(true)
	AssertThat(t, err, Not{V: nil})
}

func TestGetTimeFromSomething_BadString_error(t *testing.T) {
	_, err := GetTimeFromSomething("aaa")
	AssertThat(t, err, Not{V: nil})
}

func TestGetTimeFromSomething_GoodStringUnix_ok(t *testing.T) {
	ts, err := GetTimeFromSomething("1465568923")
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, ts.Unix(), Is{V: int64(1465568923)})
}

func TestGetTimeFromSomething_GoodStringHuman_ok(t *testing.T) {
	ts, err := GetTimeFromSomething("04.03.2016 08:40:00")
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, ts.Unix(), Is{V: int64(1457080800)})
}

func TestGetTimeFromSomething_GoodStringHuman2_ok(t *testing.T) {
	ts, err := GetTimeFromSomething("2016-03-0408:40")
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, ts.Unix(), Is{V: int64(1457080800)})
}

func TestGetTimeFromRfcString_notAString_error(t *testing.T) {
	_, err := GetTimeFromRfcString(false)
	AssertThat(t, err, Not{V: nil})
}

func TestGetTimeFromRfcString_notRfc_error(t *testing.T) {
	_, err := GetTimeFromRfcString("string")
	AssertThat(t, err, Not{V: nil})
}

func TestGetTimeFromRfcString_good_ok(t *testing.T) {
	ts, err := GetTimeFromRfcString("2016-03-04T12:40:00+04:00")
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, ts.Unix(), Is{V: int64(1457080800)})
}

func TestGetTimeFromSomething_int_ok(t *testing.T) {
	ts, err := GetTimeFromSomething(int(1465568923))
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, ts.Unix(), Is{V: int64(1465568923)})
}

func TestGetTimeFromSomething_int64_ok(t *testing.T) {
	ts, err := GetTimeFromSomething(int64(1465568923))
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, ts.Unix(), Is{V: int64(1465568923)})
}

func TestGetTimeFromSomething_float64_ok(t *testing.T) {
	ts, err := GetTimeFromSomething(float64(1465568923))
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, ts.Unix(), Is{V: int64(1465568923)})
}

func TestGetIntFromSomething_bool_error(t *testing.T) {
	_, err := GetIntFromSomething(true)
	AssertThat(t, err, Not{V: nil})
}

func TestGetIntFromSomething_BadString_error(t *testing.T) {
	_, err := GetIntFromSomething("aaa")
	AssertThat(t, err, Not{V: nil})
}

func TestGetIntFromSomething_EmptyString_error(t *testing.T) {
	_, err := GetIntFromSomething("")
	AssertThat(t, err, Not{V: nil})
}

func TestGetIntFromSomething_GoodString_ok(t *testing.T) {
	i, err := GetIntFromSomething("123")
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, i, Is{V: 123})
}

func TestGetIntFromSomething_int_ok(t *testing.T) {
	i, err := GetIntFromSomething(int(123))
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, i, Is{V: 123})
}

func TestGetIntFromSomething_int64_ok(t *testing.T) {
	i, err := GetIntFromSomething(int64(123))
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, i, Is{V: 123})
}

func TestGetIntFromSomething_float64_ok(t *testing.T) {
	i, err := GetIntFromSomething(float64(123))
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, i, Is{V: 123})
}
