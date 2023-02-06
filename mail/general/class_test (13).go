package yandexpayment

import (
	"a.yandex-team.ru/mail/iex/taksa/iex"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"testing"
)

import . "a.yandex-team.ru/mail/iex/taksa/widgets/common"
import . "a.yandex-team.ru/mail/iex/matchers"

func extend() (Widget, error) {
	facts := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{74}, Subject: "Order payment"}, IEX: []interface{}{}}
	return Class{
		Cfg:    Config{Logo: LogoCfg{Zubchiki: false, LogoColorDefault: "#ffc65c"}},
		Fact:   facts,
		Logger: logger.Mock{}}.Extend()
}

func TestExtend_yandexPaymentWidget(t *testing.T) {
	w, _ := extend()
	AssertThat(t, w, TypeOf{V: &YandexPaymentWidget{}})
	AssertThat(t, w.Type(), Is{V: "yandex_payment"})
	AssertThat(t, w.Valid(), Is{V: true})
	AssertThat(t, w.Double(), Is{V: false})
	AssertThat(t, w.Mid(), Is{V: "1"})
	AssertThat(t, w.Controls(), HasLogo{})
	AssertThat(t, w.Controls(), LogoColorIs{Value: "#ffc65c"})
	AssertThat(t, w.Controls(), ZubchikiIs{Value: false})
}
