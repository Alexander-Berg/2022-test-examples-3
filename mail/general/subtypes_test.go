package avia

import (
	"a.yandex-team.ru/mail/iex/taksa/currency"
	"a.yandex-team.ru/mail/iex/taksa/iex"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"a.yandex-team.ru/mail/iex/taksa/request"
	"a.yandex-team.ru/mail/iex/taksa/tanker"
	"a.yandex-team.ru/mail/iex/taksa/weather"
	"testing"
)

import . "a.yandex-team.ru/mail/iex/taksa/widgets/common"
import . "a.yandex-team.ru/mail/iex/matchers"

func prepareTestData() (class Class, ticket Ticket) {
	class = Class{
		Cfg: Config{
			Logo: LogoCfg{
				Zubchiki:         true,
				LogoColorDefault: "logo_color_default",
				LogoColorYa:      "logo_color_ya",
				LogoIconRegular:  "gray",
				LogoIconBright:   "white",
			},
			TravelLink:      "https://travel.yandex",
			AeroexpressLink: "aex",
			YTaxiLink:       "ytaxi",
		},
		Logger:   logger.Mock{},
		Tanker:   tanker.Mock{},
		Request:  request.RequestMock{Tld: "tld"},
		Weather:  weather.Mock{Data: "+25"},
		Currency: currency.Mock{Data: "dorogo"},
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
	ticket.Init(IexDict{
		"date_arr_rfc":       "2016-12-11T10:20:00+00:00",
		"date_dep_rfc":       "2016-12-10T21:10:00+00:00",
		"airport_dep":        "Шереметьево",
		"date_arr_back_rfc":  "2016-12-25T18:05:00+00:00",
		"date_dep_back_rfc":  "2016-12-25T11:55:00+00:00",
		"city_arr":           "Пхукет",
		"airport_arr":        "Пхукет",
		"transfer":           "aeroexpress",
		"checkin_url":        "url",
		"from_country_geoid": "225",
		"to_country_geoid":   "995",
		"widget_subtype":     "eticket",
		"city_dep_geoid":     213,
		"city_dep":           "Москва",
		"city_arr_geoid":     10622,
		"city_dep_back":      "Пхукет",
		"airline_iata":       "SU",
		"url":                "link",
		"print_parts":        []interface{}{"1.1"},
	})
	return
}

func checkCommonStuff(w Widget, t *testing.T) {
	AssertThat(t, w.Type(), Is{V: "avia"})
	AssertThat(t, w.Mid(), Is{V: "mid"})
	AssertThat(t, w.ExpireDate().Unix(), Is{V: int64(1482689100)})
	AssertThat(t, w.Controls(), HasLogo{})
	AssertThat(t, w.Controls(), LogoLabelIs{Value: "dn"})
	AssertThat(t, w.Controls(), ZubchikiIs{Value: true})
}

func TestMakeTicketWidget(t *testing.T) {
	class, hotel := prepareTestData()
	w, _ := class.makeTicketWidget(hotel)
	checkCommonStuff(w, t)
	AssertThat(t, w.Valid(), Is{V: true})
	AssertThat(t, w.Double(), Is{V: true})
	AssertThat(t, w.SubType(), Is{V: "ticket"})
	AssertThat(t, w.Controls(), LogoIconIs{Value: "gray-light"})
	AssertThat(t, w.Controls(), LogoColorIs{Value: "logo_color_default"})
	AssertThat(t, w.Controls(), TitleIs{Value: "subj"})
	AssertThat(t, w.Controls(), Description1Is{Value: "Dec 10 21:10 Москва &planes; Пхукет 11:55 Dec 25", HasHTMLEntities: true})
	AssertThat(t, w.Controls(), Description1AuxIs{HasHTMLEntities: true})
	//AssertThat(t, w.Controls(), HasLink{"action-1", "_Booking_", "https://travel.yandex.tld/search/rooms?to=10622&when=2016-12-11", false})
	AssertThat(t, w.Controls(), HasPrint{Role: "action-2", Parts: "1.1"})
}

func TestMakeTicketWidget_mobile(t *testing.T) {
	class, hotel := prepareTestData()
	class.Request = request.RequestMock{Mobile: true}
	w, _ := class.makeTicketWidget(hotel)
	AssertThat(t, w.Controls(), Description1Is{Value: "Москва &ndash; Пхукет", HasHTMLEntities: true})
	AssertThat(t, w.Controls(), Description1AuxIs{Value: "Dec 10 21:10 &planes; Dec 25 11:55", HasHTMLEntities: true})
}

func TestMakeReminderWidget(t *testing.T) {
	class, hotel := prepareTestData()
	w, _ := class.makeReminderWidget(hotel)
	checkCommonStuff(w, t)
	AssertThat(t, w.Valid(), Is{V: true})
	AssertThat(t, w.Double(), Is{V: true})
	AssertThat(t, w.SubType(), Is{V: "reminder"})
	AssertThat(t, w.Controls(), LogoIconIs{Value: "gray-light"})
	AssertThat(t, w.Controls(), LogoColorIs{Value: "logo_color_default"})
	AssertThat(t, w.Controls(), TitleIs{Value: "subj"})
	AssertThat(t, w.Controls(), Description1Is{Value: "Dec 10 21:10 Москва (Шереметьево) &plane; Пхукет (Пхукет) 10:20 Dec 11", HasHTMLEntities: true})
	AssertThat(t, w.Controls(), Description1AuxIs{HasHTMLEntities: true})
	AssertThat(t, w.Controls(), HasLink{Role: "action-3", Label: "_Checkin_", Value: "url"})
	AssertThat(t, w.Controls(), HasLink{Role: "action-4", Label: "_Aeroexpress_", Value: "aex"})
	AssertThat(t, w.Controls(), Description2Is{Value: "+25"})
	AssertThat(t, w.Controls(), Description3Is{Value: "dorogo"})
}

func TestMakeReminderWidget_mobile(t *testing.T) {
	class, hotel := prepareTestData()
	class.Request = request.RequestMock{Mobile: true}
	w, _ := class.makeReminderWidget(hotel)
	AssertThat(t, w.Controls(), Description1Is{Value: "Москва &ndash; Пхукет", HasHTMLEntities: true})
	AssertThat(t, w.Controls(), Description1AuxIs{Value: "Dec 10 21:10 &plane; Dec 11 10:20", HasHTMLEntities: true})
}

func TestMakeBoardingPassWidget(t *testing.T) {
	class, hotel := prepareTestData()
	w, _ := class.makeBoardingPassWidget(hotel)
	checkCommonStuff(w, t)
	AssertThat(t, w.Valid(), Is{V: true})
	AssertThat(t, w.Double(), Is{V: true})
	AssertThat(t, w.SubType(), Is{V: "boardingpass"})
	AssertThat(t, w.Controls(), LogoIconIs{Value: "gray-light"})
	AssertThat(t, w.Controls(), LogoColorIs{Value: "logo_color_default"})
	AssertThat(t, w.Controls(), TitleIs{Value: "subj"})
	AssertThat(t, w.Controls(), Description1Is{Value: "Dec 10 21:10 Москва (Шереметьево) &plane; Пхукет (Пхукет) 10:20 Dec 11", HasHTMLEntities: true})
	AssertThat(t, w.Controls(), Description1AuxIs{HasHTMLEntities: true})
	AssertThat(t, w.Controls(), HasPrint{Role: "action-2", Parts: "1.1"})
	AssertThat(t, w.Controls(), HasLink{Role: "action-4", Label: "_Aeroexpress_", Value: "aex"})
	AssertThat(t, w.Controls(), Description2Is{Value: "+25"})
}

func TestMakeBoardingPassWidget_mobile(t *testing.T) {
	class, hotel := prepareTestData()
	class.Request = request.RequestMock{Mobile: true}
	w, _ := class.makeBoardingPassWidget(hotel)
	AssertThat(t, w.Controls(), Description1Is{Value: "Москва &ndash; Пхукет", HasHTMLEntities: true})
	AssertThat(t, w.Controls(), Description1AuxIs{Value: "Dec 10 21:10 &plane; Dec 11 10:20", HasHTMLEntities: true})
}

func TestMakeTomorrowWidget(t *testing.T) {
	class, hotel := prepareTestData()
	w, _ := class.makeTomorrowWidget(hotel)
	checkCommonStuff(w, t)
	AssertThat(t, w.Valid(), Is{V: true})
	AssertThat(t, w.Double(), Is{V: true})
	AssertThat(t, w.SubType(), Is{V: "tomorrow"})
	AssertThat(t, w.Controls(), LogoIconIs{Value: "gray-light"})
	AssertThat(t, w.Controls(), TitleIs{Value: "subj"})
	AssertThat(t, w.Controls(), Description1Is{Value: "Dec 10 21:10 Москва (Шереметьево) &plane; Пхукет (Пхукет) 10:20 Dec 11", HasHTMLEntities: true})
	AssertThat(t, w.Controls(), Description1AuxIs{HasHTMLEntities: true})
	AssertThat(t, w.Controls(), HasLink{Role: "action-3", Label: "_Checkin_", Value: "url"})
	AssertThat(t, w.Controls(), HasLink{Role: "action-4", Label: "_Aeroexpress_", Value: "aex"})
	AssertThat(t, w.Controls(), Description2Is{Value: "+25"})
	AssertThat(t, w.Controls(), Description3Is{Value: "dorogo"})
}

func TestMakeTomorrowWidget_mobile(t *testing.T) {
	class, hotel := prepareTestData()
	class.Request = request.RequestMock{Mobile: true}
	w, _ := class.makeTomorrowWidget(hotel)
	AssertThat(t, w.Controls(), Description1Is{Value: "Москва &ndash; Пхукет", HasHTMLEntities: true})
	AssertThat(t, w.Controls(), Description1AuxIs{Value: "Dec 10 21:10 &plane; Dec 11 10:20", HasHTMLEntities: true})
}

func TestMakeExpiredWidget(t *testing.T) {
	class, hotel := prepareTestData()
	w, _ := class.makeExpiredWidget(hotel)
	checkCommonStuff(w, t)
	AssertThat(t, w.Valid(), Is{V: false})
	AssertThat(t, w.Double(), Is{V: false})
	AssertThat(t, w.SubType(), Is{V: "ticket"})
	AssertThat(t, w.Controls(), LogoIconIs{Value: "gray-light"})
	AssertThat(t, w.Controls(), TitleIs{Value: "subj"})
	AssertThat(t, w.Controls(), Description1Is{Value: "Dec 10 21:10 Москва &planes; Пхукет 11:55 Dec 25", HasHTMLEntities: true})
	AssertThat(t, w.Controls(), Description1AuxIs{HasHTMLEntities: true})
}

func TestMakeExpiredWidget_mobile(t *testing.T) {
	class, hotel := prepareTestData()
	class.Request = request.RequestMock{Mobile: true}
	w, _ := class.makeExpiredWidget(hotel)
	AssertThat(t, w.Controls(), Description1Is{Value: "Москва &ndash; Пхукет", HasHTMLEntities: true})
	AssertThat(t, w.Controls(), Description1AuxIs{Value: "Dec 10 21:10 &planes; Dec 25 11:55", HasHTMLEntities: true})
}

func TestMakeExpiredBoardingPassWidget(t *testing.T) {
	class, ticket := prepareTestData()
	w, _ := class.makeExpiredBoardingPassWidget(ticket)
	checkCommonStuff(w, t)
	AssertThat(t, w.Valid(), Is{V: false})
	AssertThat(t, w.Double(), Is{V: false})
	AssertThat(t, w.SubType(), Is{V: "boardingpass"})
	AssertThat(t, w.Controls(), LogoIconIs{Value: "gray-light"})
	AssertThat(t, w.Controls(), TitleIs{Value: "_BoardingPass_"})
	AssertThat(t, w.Controls(), Description1Is{Value: "Dec 10 21:10 Москва (Шереметьево) &plane; Пхукет (Пхукет) 10:20 Dec 11", HasHTMLEntities: true})
	AssertThat(t, w.Controls(), Description1AuxIs{HasHTMLEntities: true})
}

func TestMakeExpiredBoardingPassWidget_mobile(t *testing.T) {
	class, ticket := prepareTestData()
	class.Request = request.RequestMock{Mobile: true}
	w, _ := class.makeExpiredBoardingPassWidget(ticket)
	AssertThat(t, w.Controls(), Description1Is{Value: "Москва &ndash; Пхукет", HasHTMLEntities: true})
	AssertThat(t, w.Controls(), Description1AuxIs{Value: "Dec 10 21:10 &plane; Dec 11 10:20", HasHTMLEntities: true})
}

func TestMakeFactlessWidget(t *testing.T) {
	class, hotel := prepareTestData()
	w, _ := class.makeFactlessWidget(hotel)
	checkCommonStuff(w, t)
	AssertThat(t, w.Valid(), Is{V: true})
	AssertThat(t, w.Double(), Is{V: false})
	AssertThat(t, w.SubType(), Is{V: "factless"})
	AssertThat(t, w.Controls(), LogoIconIs{Value: "gray-light"})
	AssertThat(t, w.Controls(), TitleIs{Value: "subj"})
	AssertThat(t, w.Controls(), HasLink{Role: "action-2", Label: "_GoToOrder_", Value: "link"})
}
