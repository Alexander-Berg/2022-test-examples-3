package spam

import (
	"a.yandex-team.ru/mail/iex/taksa/iex"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"a.yandex-team.ru/mail/iex/taksa/tanker"
	"testing"
)

import . "a.yandex-team.ru/mail/iex/taksa/widgets/common"
import . "a.yandex-team.ru/mail/iex/matchers"

func extend() (Widget, error) {
	facts := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}, Subject: "You won 1000$"}, IEX: []interface{}{}}
	return Class{
		Cfg:    Config{Logo: LogoCfg{Zubchiki: false, LogoColorDefault: "#cc0d00"}},
		Fact:   facts,
		Logger: logger.Mock{},
		Tanker: tanker.Mock{}}.Extend()
}

func TestExtend_spamWidget(t *testing.T) {
	w, _ := extend()
	AssertThat(t, w, TypeOf{V: &SpamWidget{}})
	AssertThat(t, w.Type(), Is{V: "spam"})
	AssertThat(t, w.Valid(), Is{V: true})
	AssertThat(t, w.Double(), Is{V: false})
	AssertThat(t, w.Mid(), Is{V: "1"})
	AssertThat(t, w.Controls(), HasLogo{})
	AssertThat(t, w.Controls(), LogoColorIs{Value: "#cc0d00"})
	AssertThat(t, w.Controls(), ZubchikiIs{Value: false})
	title := "<b>_Spam_</b><span style=\"opacity:0.6;font-weight:normal;\">You won 1000$</span>"
	AssertThat(t, w.Controls(), TitleIs{Value: title, HasHTMLEntities: true})
}
