package eshop

import (
	"a.yandex-team.ru/mail/iex/taksa/iex"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"a.yandex-team.ru/mail/iex/taksa/tanker"
	"testing"
	"time"
)

import . "a.yandex-team.ru/mail/iex/taksa/widgets/common"
import . "a.yandex-team.ru/mail/iex/matchers"

func prepareTestData(item string) (class Class, order Order) {
	class = Class{
		Cfg: Config{
			Logo: LogoCfg{
				Zubchiki:         false,
				LogoColorDefault: "color",
				LogoColorYa:      "gray",
				LogoColor:        map[string]string{"domain": "color_by_domain"},
				LogoIconRegular:  "icon",
				LogoIconBright:   "",
			},
		},
		Tanker: tanker.Mock{},
		Fact: iex.Fact{
			Envelope: meta.Envelope{
				Mid:             "mid",
				FromAddress:     "a@domain",
				FromDisplayName: "shop_name",
				Subject:         "subj",
				Firstline:       "fl",
			},
		},
	}
	order = Order{IexDict{
		"url":   "link",
		"price": "$1",
		"order": item,
	}}
	return
}

func checkCommonStuff(w Widget, t *testing.T) {
	AssertThat(t, w.Type(), Is{V: "eshop"})
	AssertThat(t, w.Double(), Is{V: false})
	AssertThat(t, w.Mid(), Is{V: "mid"})
	AssertThat(t, w.ExpireDate(), Is{V: (*time.Time)(nil)})
	AssertThat(t, w.Controls(), HasLogo{})
	AssertThat(t, w.Controls(), LogoLabelIs{Value: "shop_name"})
	AssertThat(t, w.Controls(), LogoIconIs{Value: "icon-light"})
	AssertThat(t, w.Controls(), ZubchikiIs{})
	AssertThat(t, w.Controls(), HasLink{Role: Action1.String(), Label: "_GoToOrder_", Value: "link"})
	AssertThat(t, w.Controls(), TitleIs{Value: "subj"})
	AssertThat(t, w.Controls(), Description2Is{Value: "$1"})
}

func TestMakeMainWidget(t *testing.T) {
	class, bounce := prepareTestData("")
	w, _ := class.makeMainWidget(bounce)
	checkCommonStuff(w, t)
	AssertThat(t, w.Valid(), Is{V: true})
	AssertThat(t, w.SubType(), Is{V: "order"})
	AssertThat(t, w.Controls(), LogoColorIs{Value: "color_by_domain"})
	AssertThat(t, w.Controls(), Description1Is{Value: "fl"})
}

func TestMakeMainWidgetWithOrder(t *testing.T) {
	class, bounce := prepareTestData("xxx")
	w, _ := class.makeMainWidget(bounce)
	checkCommonStuff(w, t)
	AssertThat(t, w.Valid(), Is{V: true})
	AssertThat(t, w.SubType(), Is{V: "order"})
	AssertThat(t, w.Controls(), LogoColorIs{Value: "color_by_domain"})
	AssertThat(t, w.Controls(), Description1Is{Value: "xxx", HasHTMLEntities: true})
}

func TestMakeOutdatedWidget(t *testing.T) {
	class, bounce := prepareTestData("")
	w, _ := class.makeOutdatedWidget(bounce)
	checkCommonStuff(w, t)
	AssertThat(t, w.Valid(), Is{V: false})
	AssertThat(t, w.SubType(), Is{V: "outdated"})
	AssertThat(t, w.Controls(), LogoColorIs{Value: "gray"})
	AssertThat(t, w.Controls(), Description1Is{Value: "fl"})
}
