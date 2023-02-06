package hotels

import (
	"a.yandex-team.ru/mail/iex/taksa/client"
	"a.yandex-team.ru/mail/iex/taksa/iex"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"a.yandex-team.ru/mail/iex/taksa/request"
	"a.yandex-team.ru/mail/iex/taksa/tanker"
	"testing"
)

import . "a.yandex-team.ru/mail/iex/matchers"
import . "a.yandex-team.ru/mail/iex/taksa/widgets/common"

type emptyError struct {
}

func (e emptyError) Error() string {
	return ""
}

func TestPrintPersons(t *testing.T) {
	AssertThat(t, printPersons(0), Is{V: ""})
	AssertThat(t, printPersons(1), Is{V: "&man;"})
	AssertThat(t, printPersons(2), Is{V: "&man;&man;"})
	AssertThat(t, printPersons(3), Is{V: "&man;&man;&man;"})
	AssertThat(t, printPersons(4), Is{V: "&man;4"})
	AssertThat(t, printPersons(5), Is{V: "&man;5"})
}

func TestGetDetails_notMobile(t *testing.T) {
	class := Class{Tanker: tanker.Mock{}, Request: request.RequestMock{}}
	c, err := class.getDetails(Hotel{IexDict{"city": "c", "number_of_nights": "2", "checkin_date_rfc": "2016-12-11T15:00:00+07:00", "checkout_date_rfc": "2016-12-25T12:00:00+07:00"}})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, c.Attributes["label"], Is{V: "c, Dec 11 &ndash; Dec 25 (2 _Nights_)"})
}

func TestGetDetails_mobile(t *testing.T) {
	class := Class{Tanker: tanker.Mock{}, Request: request.RequestMock{Mobile: true}}
	c, err := class.getDetails(Hotel{IexDict{"city": "c", "number_of_nights": "2", "checkin_date_rfc": "2016-12-11T15:00:00+07:00", "checkout_date_rfc": "2016-12-25T12:00:00+07:00"}})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, c.Attributes["label"], Is{V: "c"})
}

func TestGetDetailsAux_notMobile(t *testing.T) {
	class := Class{Tanker: tanker.Mock{}, Request: request.RequestMock{}}
	c, err := class.getDetailsAux(Hotel{IexDict{"city": "c", "number_of_nights": "2", "checkin_date_rfc": "2016-12-11T15:00:00+07:00", "checkout_date_rfc": "2016-12-25T12:00:00+07:00"}})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, c.Attributes["label"], Is{V: ""})
}

func TestGetDetailsAux_mobile(t *testing.T) {
	class := Class{Tanker: tanker.Mock{}, Request: request.RequestMock{Mobile: true}}
	c, err := class.getDetailsAux(Hotel{IexDict{"city": "c", "number_of_nights": "2", "checkin_date_rfc": "2016-12-11T15:00:00+07:00", "checkout_date_rfc": "2016-12-25T12:00:00+07:00"}})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, c.Attributes["label"], Is{V: "Dec. 11 &ndash; Dec. 25"})
}

func TestGetTitleNice_hasHotel_givesNiceTitle(t *testing.T) {
	class := Class{Logger: logger.Mock{}, Client: client.Mock{}}
	c, err := class.getTitleNice(Hotel{IexDict{"hotel": "Plaza", "people": "2"}})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, c.Attributes["label"], Is{V: "Plaza &man;&man;"})
}

func TestGetTitleNice_hasNoHotel_fallsBackToSubject(t *testing.T) {
	fact := iex.Fact{Envelope: meta.Envelope{Subject: "subj"}}
	class := Class{Logger: logger.Mock{}, Client: client.Mock{}, Fact: fact}
	c, err := class.getTitleNice(Hotel{})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, c.Attributes["label"], Is{V: "subj"})
}

func TestGetFreeCancelDate_noDate_givesError(t *testing.T) {
	class := Class{Logger: logger.Mock{}, Client: client.Mock{}, Tanker: tanker.Mock{}, Request: request.RequestMock{}}
	_, err := class.getFreeCancelDate(Hotel{})
	AssertThat(t, err, Not{V: nil})
}

func TestGetFreeCancelDate_badDate_givesError(t *testing.T) {
	class := Class{Logger: logger.Mock{}, Client: client.Mock{}, Tanker: tanker.Mock{}, Request: request.RequestMock{}}
	_, err := class.getFreeCancelDate(Hotel{IexDict{"cancellation_info": "bad date"}})
	AssertThat(t, err, Not{V: nil})
}

func TestGetFreeCancelDate_haveDate_givesDate(t *testing.T) {
	class := Class{Logger: logger.Mock{}, Client: client.Mock{}, Tanker: tanker.Mock{}, Request: request.RequestMock{}}
	c, err := class.getFreeCancelDate(Hotel{IexDict{"cancellation_info": "10.09.2016 12:00:00"}})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, c.Attributes["label"], Is{V: "_FreeCancelDate_ Sep 10"})
}

func TestGetFreeCancelDate_haveDateAndMobile_givesDate(t *testing.T) {
	class := Class{Logger: logger.Mock{}, Client: client.Mock{}, Tanker: tanker.Mock{}, Request: request.RequestMock{Mobile: true}}
	c, err := class.getFreeCancelDate(Hotel{IexDict{"cancellation_info": "10.09.2016 12:00:00"}})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, c.Attributes["label"], Is{V: "_FreeCancelDateShort_ Sep. 10"})
}

func TestGetCheckWeather_tldPassed_linkWithPassedTld(t *testing.T) {
	config := Config{WeatherLink: "pogoda.yandex"}
	class := Class{Cfg: config,
		Logger:  logger.Mock{},
		Client:  client.Mock{},
		Request: request.RequestMock{Tld: "ua"},
		Tanker:  tanker.Mock{}}
	c, err := class.getCheckWeather(Hotel{IexDict{"city_geoid": "1"}})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, c.Attributes["value"], Is{V: "pogoda.yandex.ua/1"})
}
