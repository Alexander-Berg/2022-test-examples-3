package logger

import (
	"testing"
)

import . "a.yandex-team.ru/mail/iex/matchers"

func TestFilter_emptyString_returnsEmptyString(t *testing.T) {
	AssertThat(t, filter(""), Is{V: ""})
}

func TestFilter_oneByteString_returnsAsIs(t *testing.T) {
	AssertThat(t, filter("abc"), Is{V: "abc"})
}

func TestFilter_unicodePrintableCharacters_returnsAsIs(t *testing.T) {
	AssertThat(t, filter("測試"), Is{V: "測試"})
}

func TestFilter_spaceSymbols_returnsEscapedString(t *testing.T) {
	AssertThat(t, filter("\r\t\n"), Is{V: "\\r\\t\\n"})
}

func TestFilter_oneByteNonPrintableCharacters_returnsEscapedString(t *testing.T) {
	AssertThat(t, filter("\x01\x02\x03"), Is{V: "\\x01\\x02\\x03"})
}

func TestFilter_twoBytesNonPrintableCharacters_returnsEscapedString(t *testing.T) {
	AssertThat(t, filter("\ufeff"), Is{V: "\\ufeff"})
}

func TestFilter_fourBytesNonPrintableCharacters_returnsEscapedString(t *testing.T) {
	AssertThat(t, filter("\U000338af"), Is{V: "\\U000338af"})
}

func TestFilter_mixedNonPrintableCharacters_returnsEscapedString(t *testing.T) {
	AssertThat(t, filter("\x01\ufeff\U000338af"), Is{V: "\\x01\\ufeff\\U000338af"})
}

func TestFilter_backslash_returnsEscapedBackslash(t *testing.T) {
	AssertThat(t, filter("\\"), Is{V: "\\\\"})
}

func TestString_eventWithEmptyReason_givesEmptyString(t *testing.T) {
	AssertThat(t, TSKV{}.String(), Is{V: ""})
}
