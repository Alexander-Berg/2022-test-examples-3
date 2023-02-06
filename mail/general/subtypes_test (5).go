package hotels

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

func prepareTestData() (class Class, hotel Hotel) {
	class = Class{
		Cfg: Config{
			Logo: LogoCfg{
				Zubchiki:         true,
				LogoColorDefault: "logo_color_default",
				LogoColorYa:      "logo_color_ya",
				LogoIconRegular:  "gray",
				LogoIconBright:   "white",
			},
			WeatherLink: "https://pogoda.yandex",
		},
		Logger:  logger.Mock{},
		Tanker:  tanker.Mock{},
		Request: request.RequestMock{Tld: "tld"},
		Fact: iex.Fact{
			Envelope: meta.Envelope{
				Mid:             "mid",
				FromAddress:     "a@b",
				FromDisplayName: "dn",
				Subject:         "subj",
				Firstline:       "fl",
			},
		},
	}
	hotel = Hotel{IexDict{
		"widget_subtype":       "booking",
		"city":                 "city",
		"city_geoid":           10622,
		"checkin_date_rfc":     "2016-12-11T15:00:00+07:00",
		"checkout_date_rfc":    "2016-12-25T12:00:00+07:00",
		"number_of_nights":     "14",
		"people":               "2",
		"cancellation_info":    "26.11.2016 23:59:00",
		"modifyReservationUrl": "url",
		"domain":               "booking.com",
		"hotel":                "hotel name",
		"price":                "68 070,60 THB",
	}}
	return
}

func checkCommonStuff(w Widget, t *testing.T) {
	AssertThat(t, w.Type(), Is{V: "hotels"})
	AssertThat(t, w.Mid(), Is{V: "mid"})
	AssertThat(t, w.ExpireDate().Unix(), Is{V: int64(1482642000)})
	AssertThat(t, w.Controls(), HasLogo{})
	AssertThat(t, w.Controls(), LogoLabelIs{Value: "dn"})
	AssertThat(t, w.Controls(), ZubchikiIs{Value: true})
}

func TestMakeBookingWidget(t *testing.T) {
	class, hotel := prepareTestData()
	w, _ := class.makeBookingWidget(hotel)
	checkCommonStuff(w, t)
	AssertThat(t, w.Valid(), Is{V: true})
	AssertThat(t, w.Double(), Is{V: true})
	AssertThat(t, w.SubType(), Is{V: "booking"})
	AssertThat(t, w.Controls(), LogoIconIs{Value: "gray-light"})
	AssertThat(t, w.Controls(), LogoColorIs{Value: "logo_color_default"})
	AssertThat(t, w.Controls(), TitleIs{Value: "hotel name &man;&man;", HasHTMLEntities: true})
	AssertThat(t, w.Controls(), Description1Is{Value: "city, Dec 11 &ndash; Dec 25 (14 _Nights_)", HasHTMLEntities: true})
	AssertThat(t, w.Controls(), Description1AuxIs{HasHTMLEntities: true})
	AssertThat(t, w.Controls(), Description2Is{Value: "68 070,60 THB"})
	AssertThat(t, w.Controls(), Description3Is{Value: "_FreeCancelDate_ Nov 26"})
	AssertThat(t, w.Controls(), HasLink{Role: "action-1", Label: "_ChangeBooking_", Value: "url"})
	AssertThat(t, w.Controls(), HasPrint{Role: "action-2"})
}

func TestMakeBookingWidget_mobile(t *testing.T) {
	class, hotel := prepareTestData()
	class.Request = request.RequestMock{Mobile: true}
	w, _ := class.makeBookingWidget(hotel)
	AssertThat(t, w.Controls(), Description1Is{Value: "city", HasHTMLEntities: true})
	AssertThat(t, w.Controls(), Description1AuxIs{Value: "Dec. 11 &ndash; Dec. 25", HasHTMLEntities: true})
	AssertThat(t, w.Controls(), Description3Is{Value: "_FreeCancelDateShort_ Nov. 26"})
}

func TestMakeReminderWidget(t *testing.T) {
	class, hotel := prepareTestData()
	w, _ := class.makeReminderWidget(hotel)
	checkCommonStuff(w, t)
	AssertThat(t, w.Valid(), Is{V: true})
	AssertThat(t, w.Double(), Is{V: true})
	AssertThat(t, w.SubType(), Is{V: "reminder"})
	AssertThat(t, w.Controls(), LogoIconIs{Value: "white-light"})
	AssertThat(t, w.Controls(), LogoColorIs{Value: "logo_color_ya"})
	AssertThat(t, w.Controls(), TitleIs{Value: "subj"})
	AssertThat(t, w.Controls(), Description1Is{Value: "city, Dec 11 &ndash; Dec 25 (14 _Nights_)", HasHTMLEntities: true})
	AssertThat(t, w.Controls(), Description1AuxIs{HasHTMLEntities: true})
	AssertThat(t, w.Controls(), HasLink{Role: "action-3", Label: "_CheckWeather_", Value: "https://pogoda.yandex.tld/10622"})
	AssertThat(t, w.Controls(), HasPrint{Role: "action-2"})
}

func TestMakeReminderWidget_mobile(t *testing.T) {
	class, hotel := prepareTestData()
	class.Request = request.RequestMock{Mobile: true}
	w, _ := class.makeReminderWidget(hotel)
	AssertThat(t, w.Controls(), Description1Is{Value: "city", HasHTMLEntities: true})
	AssertThat(t, w.Controls(), Description1AuxIs{Value: "Dec. 11 &ndash; Dec. 25", HasHTMLEntities: true})
}

func TestMakeTomorrowWidget(t *testing.T) {
	class, hotel := prepareTestData()
	w, _ := class.makeTomorrowWidget(hotel)
	checkCommonStuff(w, t)
	AssertThat(t, w.Valid(), Is{V: true})
	AssertThat(t, w.Double(), Is{V: true})
	AssertThat(t, w.SubType(), Is{V: "tomorrow"})
	AssertThat(t, w.Controls(), LogoIconIs{Value: "gray-light"})
	AssertThat(t, w.Controls(), TitleIs{Value: "hotel name &man;&man;", HasHTMLEntities: true})
	AssertThat(t, w.Controls(), Description1Is{Value: "city, Dec 11 &ndash; Dec 25 (14 _Nights_)", HasHTMLEntities: true})
	AssertThat(t, w.Controls(), Description1AuxIs{HasHTMLEntities: true})
	AssertThat(t, w.Controls(), HasLink{Role: "action-3", Label: "_CheckWeather_", Value: "https://pogoda.yandex.tld/10622"})
	AssertThat(t, w.Controls(), HasPrint{Role: "action-2"})
}

func TestMakeTomorrowWidget_mobile(t *testing.T) {
	class, hotel := prepareTestData()
	class.Request = request.RequestMock{Mobile: true}
	w, _ := class.makeTomorrowWidget(hotel)
	AssertThat(t, w.Controls(), Description1Is{Value: "city", HasHTMLEntities: true})
	AssertThat(t, w.Controls(), Description1AuxIs{Value: "Dec. 11 &ndash; Dec. 25", HasHTMLEntities: true})
}

func TestMakeExpiredWidget(t *testing.T) {
	class, hotel := prepareTestData()
	w, _ := class.makeExpiredWidget(hotel)
	checkCommonStuff(w, t)
	AssertThat(t, w.Valid(), Is{V: false})
	AssertThat(t, w.Double(), Is{V: false})
	AssertThat(t, w.SubType(), Is{V: "booking"})
	AssertThat(t, w.Controls(), LogoIconIs{Value: "gray-light"})
	AssertThat(t, w.Controls(), TitleIs{Value: "subj"})
	AssertThat(t, w.Controls(), Description1Is{Value: "fl"})
}

func TestMakeCancelingWidget(t *testing.T) {
	class, hotel := prepareTestData()
	w, _ := class.makeCancelingWidget(hotel)
	checkCommonStuff(w, t)
	AssertThat(t, w.Valid(), Is{V: true})
	AssertThat(t, w.Double(), Is{V: false})
	AssertThat(t, w.SubType(), Is{V: "canceling"})
	AssertThat(t, w.Controls(), LogoIconIs{Value: "gray-light"})
	AssertThat(t, w.Controls(), TitleIs{Value: "subj"})
	AssertThat(t, w.Controls(), Description1Is{Value: "fl"})
}
