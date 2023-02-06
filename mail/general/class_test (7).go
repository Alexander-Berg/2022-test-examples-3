package onelink

import (
	"a.yandex-team.ru/mail/iex/taksa/iex"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"a.yandex-team.ru/mail/iex/taksa/tanker"
	"testing"
	"time"
)

import . "a.yandex-team.ru/mail/iex/taksa/widgets/common"
import . "a.yandex-team.ru/mail/iex/matchers"

func TestExtend(t *testing.T) {
	info := map[string]interface{}{
		"taksa_widget_type_1234543456546": "action",
		"widget_subtype":                  "view_action",
		"name":                            "n",
		"url":                             "u",
	}
	iexFactsArray := []interface{}{info}
	fact := iex.Fact{
		Envelope: meta.Envelope{Mid: "mid", FromAddress: "addr@goodDomain"},
		IEX:      iexFactsArray,
	}
	w, e := Class{
		Cfg:    Config{MicroTrustedDomains: []string{"goodDomain"}},
		Fact:   fact,
		Logger: logger.Mock{},
		Tanker: tanker.Mock{},
	}.Extend()
	AssertThat(t, e, Is{V: nil})
	AssertThat(t, w, TypeOf{V: &OneLinkWidget{}})
	AssertThat(t, w.Type(), Is{V: "onelink"})
	AssertThat(t, w.Valid(), Is{V: true})
	AssertThat(t, w.Double(), Is{V: false})
	AssertThat(t, w.Mid(), Is{V: "mid"})
	AssertThat(t, w.ExpireDate(), Is{V: (*time.Time)(nil)})
	AssertThat(t, w.Controls(), Not{V: HasLogo{}})
	AssertThat(t, w.Controls(), HasLink{Role: Action1.String(), Label: "n", Value: "u"})
}

func TestExtend_untrustedDomain_givesNoWidget(t *testing.T) {
	info := map[string]interface{}{
		"widget_subtype": "view_action",
		"name":           "n",
		"url":            "u",
	}
	iexFactsArray := []interface{}{info}
	fact := iex.Fact{
		Envelope: meta.Envelope{Mid: "mid", FromAddress: "bad"},
		IEX:      iexFactsArray,
	}
	w, _ := Class{
		Cfg:    Config{MicroTrustedDomains: []string{"good"}},
		Fact:   fact,
		Logger: logger.Mock{},
		Tanker: tanker.Mock{},
	}.Extend()
	AssertThat(t, w, Is{V: nil})
}
