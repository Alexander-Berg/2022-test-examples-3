package onelink

import (
	"a.yandex-team.ru/mail/iex/taksa/iex"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"a.yandex-team.ru/mail/iex/taksa/tanker"
	"testing"
)

import . "a.yandex-team.ru/mail/iex/taksa/widgets/common"
import . "a.yandex-team.ru/mail/iex/matchers"

func TestGetLinkInfo_emptyFactsArray_givesError(t *testing.T) {
	facts := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: []interface{}{}}
	class := Class{Fact: facts, Logger: logger.Mock{}}
	_, err := class.getLinkInfo()
	AssertThat(t, err, Not{V: nil})
}

func TestGetLinkInfo_wrongStructType_givesError(t *testing.T) {
	some := []int{42}
	facts := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: []interface{}{some}}
	class := Class{Fact: facts, Logger: logger.Mock{}}
	_, err := class.getLinkInfo()
	AssertThat(t, err, Not{V: nil})
}

func err(_ string, e error) error  { return e }
func res(s string, _ error) string { return s }

func TestGetText_subtypeIsViewAction_givesNameFromIex(t *testing.T) {
	info := LinkInfo{IexDict{"widget_subtype": "view_action", "name": "n"}}
	AssertThat(t, res(info.getText(tanker.Mock{})), Is{V: "n"})
}

func TestGetText_subtypeIsConfirmAction_givesNameFromIex(t *testing.T) {
	info := LinkInfo{IexDict{"widget_subtype": "confirm_action", "name": "n"}}
	AssertThat(t, res(info.getText(tanker.Mock{})), Is{V: "n"})
}

func TestGetText_subtypeIsSaveAction_givesNameFromIex(t *testing.T) {
	info := LinkInfo{IexDict{"widget_subtype": "save_action", "name": "n"}}
	AssertThat(t, res(info.getText(tanker.Mock{})), Is{V: "n"})
}

func TestGetText_subtypeIsConfirmEmail_givesNameFromTanker(t *testing.T) {
	info := LinkInfo{IexDict{"widget_subtype": "confirm_email", "name": "n"}}
	AssertThat(t, res(info.getText(tanker.Mock{})), Is{V: "_ConfirmEmail_"})
}

func TestGetText_subtypeIsRestorePassword_givesNameFromTanker(t *testing.T) {
	info := LinkInfo{IexDict{"widget_subtype": "restore_password", "name": "n"}}
	AssertThat(t, res(info.getText(tanker.Mock{})), Is{V: "_RestorePassword_"})
}

func TestGetText_subtypeIsUnsubscribe_givesNameFromTanker(t *testing.T) {
	info := LinkInfo{IexDict{"widget_subtype": "unsubscribe", "name": "n"}}
	//AssertThat(t, res(info.getText(tanker.Mock{})), Is{V: "_Unsubscribe_"})
	//Lida asked to temporarily disable unsubscribe widgets
	AssertThat(t, res(info.getText(tanker.Mock{})), Is{V: ""})
}

func TestGetText_subtypeIsNotSupported_givesError(t *testing.T) {
	info := LinkInfo{IexDict{"widget_subtype": "fhdjfgbnc", "name": "n"}}
	AssertThat(t, err(info.getText(tanker.Mock{})), Not{V: nil})
}
