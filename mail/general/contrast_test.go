package common

import (
	"strconv"
	"testing"
)

import . "a.yandex-team.ru/mail/iex/matchers"

type ColorIs struct {
	Value Color
}

func (color ColorIs) Match(i interface{}) bool {
	other, _ := i.(Color)
	return color.Value.R == other.R &&
		color.Value.G == other.G &&
		color.Value.B == other.B
}

func (color ColorIs) String() string {
	return "ColorIs R=" + strconv.Itoa(int(color.Value.R)) +
		" G=" + strconv.Itoa(int(color.Value.G)) +
		" B=" + strconv.Itoa(int(color.Value.B))
}

func TestParse_emptyString_returnsZeros(t *testing.T) {
	AssertThat(t, parse(""), Is{V: Color{0, 0, 0}})
}

func TestParse_onlyHashSign_returnsZeros(t *testing.T) {
	AssertThat(t, parse("#"), Is{V: Color{0, 0, 0}})
}

func TestParse_twoBytes_returnsZeros(t *testing.T) {
	AssertThat(t, parse("#ffff"), Is{V: Color{0, 0, 0}})
}

func TestParse_fourBytes_returnsZeros(t *testing.T) {
	AssertThat(t, parse("#ffffffff"), Is{V: Color{0, 0, 0}})
}

func TestParse_goodString_returnsBytes(t *testing.T) {
	AssertThat(t, parse("#12ab34"), Is{V: Color{18, 171, 52}})
}

func TestWhiteOrBlack_whiteLogo_blackText(t *testing.T) {
	AssertThat(t, whiteOrBlack("#ffffff"), Is{V: "#000000"})
}

func TestWhiteOrBlack_blackLogo_whiteText(t *testing.T) {
	AssertThat(t, whiteOrBlack("#000000"), Is{V: "#ffffff"})
}

func TestWhiteOrBlack_liteLogo_blackText(t *testing.T) {
	AssertThat(t, whiteOrBlack("#bbbbbb"), Is{V: "#000000"})
}

func TestWhiteOrBlack_darkLogo_whiteText(t *testing.T) {
	AssertThat(t, whiteOrBlack("#55555"), Is{V: "#ffffff"})
}

func TestWhiteOrBlack_scaredNymphThighLogo_blackText(t *testing.T) {
	AssertThat(t, whiteOrBlack("#faeedd"), Is{V: "#000000"})
}

func TestWhiteOrBlack_brownDeerLogo_whiteText(t *testing.T) {
	AssertThat(t, whiteOrBlack("#55555"), Is{V: "#ffffff"})
}

func TestWhiteOrBlack_transparentLogo_blackText(t *testing.T) {
	AssertThat(t, whiteOrBlack("transparent"), Is{V: "#000000"})
}

func TestDarkOrLight_noIcon_dontModifyIcon(t *testing.T) {
	AssertThat(t, darkOrLight("#ffffff", ""), Is{V: ""})
}

func TestDarkOrLight_whiteText_lightIcon(t *testing.T) {
	AssertThat(t, darkOrLight("#ffffff", "icon"), Is{V: "icon-light"})
}

func TestDarkOrLight_blackText_darkIcon(t *testing.T) {
	AssertThat(t, darkOrLight("#000000", "icon"), Is{V: "icon-dark"})
}
