package common

import (
	"a.yandex-team.ru/mail/iex/matchers"
	"testing"
)

func TestGetString_notString_givesError(t *testing.T) {
	_, err := IexDict{"key": false}.GetString("key")
	matchers.AssertThat(t, err, matchers.Is{V: matchers.Not{V: nil}})
}

func TestGetString_emptyString_givesEmptyString(t *testing.T) {
	res, err := IexDict{"key": ""}.GetString("key")
	matchers.AssertThat(t, err, matchers.Is{V: nil})
	matchers.AssertThat(t, res, matchers.Is{V: ""})
}

func TestGetString_nonEmptyString_givesThatString(t *testing.T) {
	res, err := IexDict{"key": "a"}.GetString("key")
	matchers.AssertThat(t, err, matchers.Is{V: nil})
	matchers.AssertThat(t, res, matchers.Is{V: "a"})
}

func TestGetNonEmptyString_emptyString_givesEmptyString(t *testing.T) {
	_, err := IexDict{"key": ""}.GetNonEmptyString("key")
	matchers.AssertThat(t, err, matchers.Is{V: matchers.Not{V: nil}})
}

func TestGetNonEmptyString_nonEmptyString_givesThatString(t *testing.T) {
	res, err := IexDict{"key": "a"}.GetNonEmptyString("key")
	matchers.AssertThat(t, err, matchers.Is{V: nil})
	matchers.AssertThat(t, res, matchers.Is{V: "a"})
}

func TestGetInterfaceSlice_notSlice_givesError(t *testing.T) {
	_, err := GetInterfaceSlice(true)
	matchers.AssertThat(t, err, matchers.Is{V: matchers.Not{V: nil}})
}

func TestGetInterfaceSlice_slice_givesNoErrorAndASlice(t *testing.T) {
	res, err := GetInterfaceSlice([]interface{}{1, 2})
	matchers.AssertThat(t, err, matchers.Is{V: nil})
	matchers.AssertThat(t, res, matchers.Is{V: []interface{}{1, 2}})
}

func TestGetStringSlice_notSlice_givesError(t *testing.T) {
	_, err := GetStringSlice(true)
	matchers.AssertThat(t, err, matchers.Is{V: matchers.Not{V: nil}})
}

func TestGetStringSlice_notStringSlice_givesNoErrorAndEmptySlice(t *testing.T) {
	res, err := GetStringSlice([]interface{}{1, 2})
	matchers.AssertThat(t, err, matchers.Is{V: nil})
	matchers.AssertThat(t, len(res), matchers.Is{V: 0})
}

func TestGetStringSlice_stringSlice_givesNoErrorASlice(t *testing.T) {
	res, err := GetStringSlice([]interface{}{"a", "b"})
	matchers.AssertThat(t, err, matchers.Is{V: nil})
	matchers.AssertThat(t, res, matchers.Is{V: []string{"a", "b"}})
}

func TestPrintParts_badSlice_givesEmptyArr(t *testing.T) {
	matchers.AssertThat(t, len(PrintParts("smth")), matchers.Is{V: 0})
}

func TestPrintParts_onePart_givesArrWithOneElement(t *testing.T) {
	matchers.AssertThat(t, PrintParts([]interface{}{"1"}), matchers.Is{V: []string{"1"}})
}

func TestPrintParts_manyParts_givesArrWithManyElements(t *testing.T) {
	matchers.AssertThat(t, PrintParts([]interface{}{"1", "2", "3"}), matchers.Is{V: []string{"1", "2", "3"}})
}
