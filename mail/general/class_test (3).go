package bounce

import (
	"a.yandex-team.ru/mail/iex/taksa/iex"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"a.yandex-team.ru/mail/iex/taksa/request"
	"a.yandex-team.ru/mail/iex/taksa/tanker"
	"testing"
)

import . "a.yandex-team.ru/mail/iex/taksa/widgets/common"
import . "a.yandex-team.ru/mail/iex/matchers"

func extend(iexBounceType, diagnosticCode string) Widget {
	bounce := map[string]interface{}{
		"bounce_type":     iexBounceType,
		"diagnostic_code": diagnosticCode,
	}
	iexFactsArray := []interface{}{bounce}
	fact := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: iexFactsArray}

	w, _ := Class{
		Cfg:     Config{},
		Request: request.RequestMock{},
		Fact:    fact,
		Logger:  logger.Mock{},
		Tanker:  tanker.Mock{}}.Extend()
	return w
}

func TestExtend_noCode_givesTrivialBounceWidget(t *testing.T) {
	AssertThat(t, extend("", ""), TypeOf{V: &TrivialBounceWidget{}})
}

func TestExtend_code1_givesOtherBounceWidget(t *testing.T) {
	AssertThat(t, extend("1", ""), TypeOf{V: &OtherBounceWidget{}})
}

func TestExtend_code2AndDiagnosticDmark_givesDmarcBounceWidget(t *testing.T) {
	AssertThat(t, extend("2", "aaa DMARC bbb"), TypeOf{V: &DmarcBounceWidget{}})
}

func TestExtend_code2AndDiagnostic535_givesDmarcBounceWidget(t *testing.T) {
	AssertThat(t, extend("2", "zzz 535 Authentication failed. yyy"), TypeOf{V: &DmarcBounceWidget{}})
}

func TestExtend_code2AndOtherDiagnostic_givesOtherBounceWidget(t *testing.T) {
	AssertThat(t, extend("2", "xxx"), TypeOf{V: &OtherBounceWidget{}})
}

func TestExtend_code3_givesOtherBounceWidget(t *testing.T) {
	AssertThat(t, extend("3", ""), TypeOf{V: &OtherBounceWidget{}})
}

func TestExtend_code4_givesOtherBounceWidget(t *testing.T) {
	AssertThat(t, extend("4", ""), TypeOf{V: &OtherBounceWidget{}})
}

func TestExtend_code5_givesOtherBounceWidget(t *testing.T) {
	AssertThat(t, extend("5", ""), TypeOf{V: &OtherBounceWidget{}})
}

func TestExtend_code6_givesOtherBounceWidget(t *testing.T) {
	AssertThat(t, extend("6", ""), TypeOf{V: &OtherBounceWidget{}})
}

func TestExtend_code7_givesIpBlockedBounceWidget(t *testing.T) {
	AssertThat(t, extend("7", ""), TypeOf{V: &IPBlockedBounceWidget{}})
}

func TestExtend_code8_givesOtherBounceWidget(t *testing.T) {
	AssertThat(t, extend("8", ""), TypeOf{V: &OtherBounceWidget{}})
}

func TestExtend_code9_givesSuccessBounceWidget(t *testing.T) {
	AssertThat(t, extend("9", ""), TypeOf{V: &SuccessBounceWidget{}})
}
