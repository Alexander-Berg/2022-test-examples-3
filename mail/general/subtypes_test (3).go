package calendar

import (
	"a.yandex-team.ru/mail/iex/taksa/iex"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"a.yandex-team.ru/mail/iex/taksa/tanker"
	"testing"
	"time"
)

import . "a.yandex-team.ru/mail/iex/taksa/widgets/common"
import . "a.yandex-team.ru/mail/iex/matchers"

func prepareTestData() (class Class, event Event) {
	class = Class{
		Cfg: Config{
			Logo: LogoCfg{
				Zubchiki:         false,
				LogoColorDefault: "gray",
			},
		},
		Tanker: tanker.Mock{},
		Fact: iex.Fact{
			Envelope: meta.Envelope{
				Mid:             "mid",
				FromAddress:     "a@domain",
				FromDisplayName: "display_name",
				Subject:         "subj",
				Firstline:       "fl",
			},
		},
	}
	event = Event{IexDict{
		"title":          "t",
		"location":       "l",
		"start_date_rfc": "2017-12-05T14:00:00+04:00",
		"end_date_rfc":   "2017-12-05T14:30:00+04:00",
	}}
	return
}

func checkCommonStuff(w Widget, t *testing.T) {
	AssertThat(t, w.Type(), Is{V: "calendar"})
	AssertThat(t, w.Double(), Is{V: false})
	AssertThat(t, w.Mid(), Is{V: "mid"})
	AssertThat(t, w.ExpireDate(), Is{V: (*time.Time)(nil)})
	AssertThat(t, w.Controls(), HasLogo{})
	AssertThat(t, w.Controls(), LogoLabelIs{Value: "display_name"})
	AssertThat(t, w.Controls(), ZubchikiIs{})
}

func TestMakeNewWidget(t *testing.T) {
	class, event := prepareTestData()
	w, _ := class.makeNewWidget(event)
	checkCommonStuff(w, t)
	AssertThat(t, w.Valid(), Is{V: true})
	AssertThat(t, w.SubType(), Is{V: "new"})
	AssertThat(t, w.Controls(), Description1Is{Value: "Dec 5, Tue, from 14:00 till 14:30, l", HasHTMLEntities: true})
	AssertThat(t, w.Controls(), HasDrop{Role: "action-1", Label: "&man;&man;", HasHTMLEntities: true})
	AssertThat(t, w.Controls(), TitleIs{Value: "t"})
	AssertThat(t, w.Controls(), ButtonsIs{Role: "action-2"})
}

func TestMakeUpdatedWidget(t *testing.T) {
	class, event := prepareTestData()
	w, _ := class.makeUpdatedWidget(event)
	checkCommonStuff(w, t)
	AssertThat(t, w.Valid(), Is{V: true})
	AssertThat(t, w.SubType(), Is{V: "updated"})
	AssertThat(t, w.Controls(), Description1Is{Value: "Dec 5, Tue, from 14:00 till 14:30, l", HasHTMLEntities: true})
	AssertThat(t, w.Controls(), HasDrop{Role: "action-1", Label: "&man;&man;", HasHTMLEntities: true})
	AssertThat(t, w.Controls(), TitleIs{Value: "_Updated_: t"})
	AssertThat(t, w.Controls(), ButtonsIs{Role: "action-2"})
}

func TestMakeDeclinedWidget(t *testing.T) {
	class, event := prepareTestData()
	w, _ := class.makeDeclinedWidget(event)
	AssertThat(t, w.Type(), Is{V: "calendar"})
	AssertThat(t, w.Double(), Is{V: false})
	AssertThat(t, w.Mid(), Is{V: "mid"})
	AssertThat(t, w.Valid(), Is{V: false})
	AssertThat(t, w.SubType(), Is{V: "declined"})
	AssertThat(t, w.ExpireDate(), Is{V: (*time.Time)(nil)})
	AssertThat(t, w.Controls(), HasLogo{})
	AssertThat(t, w.Controls(), LogoLabelIs{Value: "display_name"})
	AssertThat(t, w.Controls(), ZubchikiIs{})
}

func TestMakeCancelledWidget(t *testing.T) {
	class, event := prepareTestData()
	w, _ := class.makeCancelledWidget(event)
	checkCommonStuff(w, t)
	AssertThat(t, w.Valid(), Is{V: false})
	AssertThat(t, w.SubType(), Is{V: "cancelled"})
	AssertThat(t, w.Controls(), TitleIs{Value: "Event \"t\" cancelled"})
	AssertThat(t, w.Controls(), Description1Is{Value: "_FreeTime_Dec 5, Tue, from 14:00 till 14:30, l", HasHTMLEntities: true})
}

func TestMakePastWidget(t *testing.T) {
	class, event := prepareTestData()
	w, _ := class.makePastWidget(event)
	checkCommonStuff(w, t)
	AssertThat(t, w.Valid(), Is{V: false})
	AssertThat(t, w.SubType(), Is{V: "past"})
	AssertThat(t, w.Controls(), TitleIs{Value: "t"})
	AssertThat(t, w.Controls(), Description1Is{Value: "Dec 5, Tue, from 14:00 till 14:30, l", HasHTMLEntities: true})
}
