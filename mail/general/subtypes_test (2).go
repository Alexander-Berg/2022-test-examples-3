package bounce

import (
	"a.yandex-team.ru/mail/iex/taksa/iex"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"a.yandex-team.ru/mail/iex/taksa/request"
	"a.yandex-team.ru/mail/iex/taksa/tanker"
	"testing"
	"time"
)

import . "a.yandex-team.ru/mail/iex/taksa/widgets/common"
import . "a.yandex-team.ru/mail/iex/matchers"

func prepareTestData() (class Class, bounce Bounce) {
	class = Class{
		Cfg: Config{
			LogoSuccess: LogoCfg{
				Zubchiki:         false,
				LogoColorDefault: "logo_color",
				LogoIconRegular:  "gray",
				LogoIconBright:   "green",
			},
			LogoFaiure: LogoCfg{
				Zubchiki:         false,
				LogoColorDefault: "logo_color",
				LogoIconRegular:  "gray",
				LogoIconBright:   "red",
			},
			FaqLinkMask: "faqlink.%v",
		},
		Tanker:  tanker.Mock{},
		Request: request.RequestMock{Tld: "tld"},
		Fact: iex.Fact{
			Envelope: meta.Envelope{
				Mid:         "mid",
				FromAddress: "a@b",
				Subject:     "subj",
				Firstline:   "fl",
			},
		},
	}
	bounce = Bounce{IexDict{
		"bounce_type":     "1",
		"final_recipient": []interface{}{"rcpt"},
		"original_from":   "origfrom",
		"original_mid":    "origmid",
	}}
	return
}

func checkCommonStuff(w Widget, t *testing.T) {
	AssertThat(t, w.Type(), Is{V: "bounce"})
	AssertThat(t, w.Valid(), Is{V: true})
	AssertThat(t, w.Double(), Is{V: false})
	AssertThat(t, w.Mid(), Is{V: "mid"})
	AssertThat(t, w.ExpireDate(), Is{V: (*time.Time)(nil)})
	AssertThat(t, w.Controls(), HasLogo{})
	AssertThat(t, w.Controls(), LogoLabelIs{Value: "mx b"})
	AssertThat(t, w.Controls(), LogoColorIs{Value: "logo_color"})
	AssertThat(t, w.Controls(), ZubchikiIs{})
	AssertThat(t, w.Controls(), HasDelete{Role: Action1.String()})
}

func TestMakeSuccessWidget(t *testing.T) {
	class, bounce := prepareTestData()
	w, _ := class.makeSuccessWidget(bounce)
	checkCommonStuff(w, t)
	AssertThat(t, w.SubType(), Is{V: "success"})
	AssertThat(t, w.Controls(), LogoIconIs{Value: "green-light"})
	AssertThat(t, w.Controls(), TitleIs{Value: "success bounce to [rcpt]"})
	AssertThat(t, w.Controls(), Description1Is{Value: ""})
}

func TestMakeIpBlockedWidget(t *testing.T) {
	class, bounce := prepareTestData()
	w, _ := class.makeIPBlockedWidget(bounce)
	checkCommonStuff(w, t)
	AssertThat(t, w.SubType(), Is{V: "ip_blocked"})
	AssertThat(t, w.Controls(), LogoIconIs{Value: "red-light"})
	AssertThat(t, w.Controls(), TitleContains{Value: "ip_blocked bounce to [rcpt]"})
	AssertThat(t, w.Controls(), TitleContains{Value: "rcpt"})
	AssertThat(t, w.Controls(), Description1Is{Value: ""})
}

func TestMakeDmarcWidget(t *testing.T) {
	class, bounce := prepareTestData()
	w, _ := class.makeDmarcWidget(bounce)
	checkCommonStuff(w, t)
	AssertThat(t, w.SubType(), Is{V: "dmarc"})
	AssertThat(t, w.Controls(), LogoIconIs{Value: "red-light"})
	AssertThat(t, w.Controls(), TitleIs{Value: "dmarc from origfrom"})
	AssertThat(t, w.Controls(), Description1Is{Value: ""})
	AssertThat(t, w.Controls(), HasLink{Role: Action2.String(), Label: "_Faq_", Value: "faqlink.tld"})
}

func TestMakeOtherWidget(t *testing.T) {
	class, bounce := prepareTestData()
	w, _ := class.makeOtherWidget(bounce)
	checkCommonStuff(w, t)
	AssertThat(t, w.SubType(), Is{V: "other"})
	AssertThat(t, w.Controls(), LogoIconIs{Value: "red-light"})
	AssertThat(t, w.Controls(), TitleContains{Value: "type1 bounce to [rcpt]"})
	AssertThat(t, w.Controls(), TitleContains{Value: "rcpt"})
	AssertThat(t, w.Controls(), Description1Is{Value: ""})
	AssertThat(t, w.Controls(), HasCompose{Role: Action2.String()})
}

func TestMakeTrivialWidget(t *testing.T) {
	class, bounce := prepareTestData()
	w, _ := class.makeTrivialWidget(bounce)
	checkCommonStuff(w, t)
	AssertThat(t, w.SubType(), Is{V: "trivial"})
	AssertThat(t, w.Controls(), LogoIconIs{Value: "gray-light"})
	AssertThat(t, w.Controls(), TitleIs{Value: "subj"})
	AssertThat(t, w.Controls(), Description1Is{Value: "fl"})
}
