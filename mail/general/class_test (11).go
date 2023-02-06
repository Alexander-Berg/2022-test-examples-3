package tracker

import (
	"a.yandex-team.ru/mail/iex/taksa/iex"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"testing"
	"time"
)

import . "a.yandex-team.ru/mail/iex/taksa/widgets/common"
import . "a.yandex-team.ru/mail/iex/matchers"

func TestExtend(t *testing.T) {
	info := map[string]interface{}{
		"taksa_widget_type_1234543456546": "action",
		"url":                             "link",
	}
	iexFactsArray := []interface{}{info}
	fact := iex.Fact{
		Envelope: meta.Envelope{Mid: "mid"},
		IEX:      iexFactsArray,
	}
	w, e := Class{
		Cfg:    Config{Img: "img"},
		Fact:   fact,
		Logger: logger.Mock{},
	}.Extend()
	AssertThat(t, e, Is{V: nil})
	AssertThat(t, w, TypeOf{V: &TrackerWidget{}})
	AssertThat(t, w.Type(), Is{V: "tracker"})
	AssertThat(t, w.Valid(), Is{V: true})
	AssertThat(t, w.Double(), Is{V: false})
	AssertThat(t, w.Mid(), Is{V: "mid"})
	AssertThat(t, w.ExpireDate(), Is{V: (*time.Time)(nil)})
	AssertThat(t, w.Controls(), Not{V: HasLogo{}})
	AssertThat(t, w.Controls(), HasImgLink{Role: Action1.String(), Label: "img", Value: "link"})
}
