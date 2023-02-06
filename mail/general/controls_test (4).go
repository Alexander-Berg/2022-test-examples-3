package common

import (
	"a.yandex-team.ru/mail/iex/matchers"
	"a.yandex-team.ru/mail/iex/taksa/tanker"
	"testing"
)

func TestGetPrint_anything_givesCorrectTypeRoleAndLabel(t *testing.T) {
	c, err := GetPrint(Action1, []string{}, tanker.Mock{})
	matchers.AssertThat(t, err, matchers.Is{V: nil})
	matchers.AssertThat(t, c.Type, matchers.Is{V: Print})
	matchers.AssertThat(t, c.Role, matchers.Is{V: Action1.String()})
	matchers.AssertThat(t, c.Attributes["label"], matchers.Is{V: "_Print_"})
}

func TestGetPrint_noParts_givesNoPartsNoError(t *testing.T) {
	c, err := GetPrint(Action1, []string{}, tanker.Mock{})
	matchers.AssertThat(t, err, matchers.Is{V: nil})
	matchers.AssertThat(t, c.Attributes["value"], matchers.Is{V: ""})
}

func TestGetPrint_onePart_givesParts(t *testing.T) {
	c, err := GetPrint(Action1, []string{"1.1"}, tanker.Mock{})
	matchers.AssertThat(t, err, matchers.Is{V: nil})
	matchers.AssertThat(t, c.Attributes["value"], matchers.Is{V: "1.1"})
}

func TestGetPrint_manyParts_givesParts(t *testing.T) {
	c, err := GetPrint(Action1, []string{"1.1", "1.2", "1.3"}, tanker.Mock{})
	matchers.AssertThat(t, err, matchers.Is{V: nil})
	matchers.AssertThat(t, c.Attributes["value"], matchers.Is{V: "1.1 1.2 1.3"})
}

func giveNonEmptyString() (string, error) {
	return "a", nil
}

func TestGetLink_anything_givesCorrectTypeRoleAndLabel(t *testing.T) {
	c, err := GetLink(Action1, giveNonEmptyString, "key", tanker.Mock{})
	matchers.AssertThat(t, err, matchers.Is{V: nil})
	matchers.AssertThat(t, c.Type, matchers.Is{V: Link})
	matchers.AssertThat(t, c.Role, matchers.Is{V: Action1.String()})
	matchers.AssertThat(t, c.Attributes["label"], matchers.Is{V: "key"})
}

func giveErr() (string, error) {
	return "", WidgetError("")
}

func TestGetLink_noLink_givesError(t *testing.T) {
	_, err := GetLink(Action1, giveErr, "key", tanker.Mock{})
	matchers.AssertThat(t, err, matchers.Is{V: matchers.Not{V: nil}})
}

func giveEmptyString() (string, error) {
	return "", nil
}

func TestGetLink_emptyLink_givesError(t *testing.T) {
	_, err := GetLink(Action1, giveEmptyString, "key", tanker.Mock{})
	matchers.AssertThat(t, err, matchers.Is{V: matchers.Not{V: nil}})
}

func TestGetLink_nonEmptyLink_givesLink(t *testing.T) {
	c, err := GetLink(Action1, giveNonEmptyString, "key", tanker.Mock{})
	matchers.AssertThat(t, err, matchers.Is{V: nil})
	matchers.AssertThat(t, c.Attributes["value"], matchers.Is{V: "a"})
}

func TestGetText_anything_givesCorrectTypeRoleAndLabel(t *testing.T) {
	c, err := GetText(Action1, giveNonEmptyString)
	matchers.AssertThat(t, err, matchers.Is{V: nil})
	matchers.AssertThat(t, c.Type, matchers.Is{V: Text})
	matchers.AssertThat(t, c.Role, matchers.Is{V: Action1.String()})
	matchers.AssertThat(t, c.Attributes["label"], matchers.Is{V: "a"})
}

func TestGetText_getterGivesError_givesError(t *testing.T) {
	_, err := GetText(Action1, giveErr)
	matchers.AssertThat(t, err, matchers.Is{V: matchers.Not{V: nil}})
}

func TestGetText_getterGivesString_givesString(t *testing.T) {
	c, err := GetText(Action1, giveNonEmptyString)
	matchers.AssertThat(t, err, matchers.Is{V: nil})
	matchers.AssertThat(t, c.Attributes["label"], matchers.Is{V: "a"})
}

func TestGetDelete_noParams_givesCorrectTypeRoleAndLabel(t *testing.T) {
	c, err := GetDelete(Action1, tanker.Mock{})
	matchers.AssertThat(t, err, matchers.Is{V: nil})
	matchers.AssertThat(t, c.Type, matchers.Is{V: Delete})
	matchers.AssertThat(t, c.Role, matchers.Is{V: Action1.String()})
	matchers.AssertThat(t, c.Attributes["label"], matchers.Is{V: "_Delete_"})
}
