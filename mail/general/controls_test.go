package avia

import (
	"a.yandex-team.ru/mail/iex/taksa/client"
	"a.yandex-team.ru/mail/iex/taksa/currency"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/request"
	"a.yandex-team.ru/mail/iex/taksa/tanker"
	"a.yandex-team.ru/mail/iex/taksa/weather"
	"testing"
)

import . "a.yandex-team.ru/mail/iex/matchers"
import . "a.yandex-team.ru/mail/iex/taksa/widgets/common"

const (
	epoch string = "1970-01-01T03:00:01+03:00"
)

type emptyError struct {
}

func (e emptyError) Error() string {
	return ""
}

func makeClass() Class {
	return makeClassWithConfig(Config{})
}

func makeClassWithConfig(cfg Config) Class {
	return Class{Cfg: cfg, Logger: logger.Mock{}, Client: client.Mock{}, Request: request.RequestMock{}, Tanker: tanker.Mock{}}
}

func TestGetRoute_withNoDepartureCity_returnsError(t *testing.T) {
	class := makeClass()
	_, err := class.getRoute(Ticket{data: TicketData{IexDict{"date_dep_rfc": epoch, "city_arr": "b"}}})
	AssertThat(t, err, Not{V: nil})
}

func TestGetRoute_withNoArriveCity_returnsError(t *testing.T) {
	class := makeClass()
	_, err := class.getRoute(Ticket{data: TicketData{IexDict{"date_dep_rfc": epoch, "city_dep": "a"}}})
	AssertThat(t, err, Not{V: nil})
}

func TestGetRoute_withNoDepartureDate_returnsError(t *testing.T) {
	class := makeClass()
	_, err := class.getRoute(Ticket{data: TicketData{IexDict{"city_dep": "a", "city_arr": "b"}}})
	AssertThat(t, err, Not{V: nil})
}

func TestGetRoute_withBothCitiesAndDepartureAndArrivalDates_returnsControlWithRoute(t *testing.T) {
	class := makeClass()
	c, err := class.getRoute(Ticket{data: TicketData{IexDict{"date_dep_rfc": epoch, "date_arr_rfc": epoch, "city_dep": "a", "city_arr": "b"}}})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, c.Attributes["label"], Is{V: "Jan 1 03:00 a &plane; b 03:00"})
}

func TestGetRoute_mobileWithBothCities_returnsControlWithRoute(t *testing.T) {
	class := makeClass()
	class.Request = request.RequestMock{Mobile: true}
	c, err := class.getRoute(Ticket{data: TicketData{IexDict{"date_dep_rfc": epoch, "date_arr_rfc": epoch, "city_dep": "a", "city_arr": "b"}}})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, c.Attributes["label"], Is{V: "a &ndash; b"})
}

func TestGetRouteAux_mobileWithDepartureAndArrivalDates_returnsControlWithRoute(t *testing.T) {
	class := makeClass()
	class.Request = request.RequestMock{Mobile: true}
	c, err := class.getRouteAux(Ticket{data: TicketData{IexDict{"date_dep_rfc": epoch, "date_arr_rfc": epoch, "city_dep": "a", "city_arr": "b"}}})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, c.Attributes["label"], Is{V: "Jan 1 03:00 &plane; Jan 1 03:00"})
}

func TestGetShortRoute_withNoDepartureCity_returnsError(t *testing.T) {
	class := makeClass()
	_, err := class.getShortRoute(Ticket{data: TicketData{IexDict{"date_dep_rfc": epoch, "city_arr": "b"}}})
	AssertThat(t, err, Not{V: nil})
}

func TestGetShortRoute_withNoArriveCity_returnsError(t *testing.T) {
	class := makeClass()
	_, err := class.getShortRoute(Ticket{data: TicketData{IexDict{"date_dep_rfc": epoch, "city_dep": "a"}}})
	AssertThat(t, err, Not{V: nil})
}

func TestGetShortRoute_withNoDepartureDate_returnsError(t *testing.T) {
	class := makeClass()
	_, err := class.getShortRoute(Ticket{data: TicketData{IexDict{"city_dep": "a", "city_arr": "b"}}})
	AssertThat(t, err, Not{V: nil})
}

func TestGetShortRoute_withBothCitiesAndDepartureDate_returnsControlWithRoute(t *testing.T) {
	class := makeClass()
	c, err := class.getShortRoute(Ticket{data: TicketData{IexDict{"date_dep_rfc": epoch, "city_dep": "a", "city_dep_back": "b"}}})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, c.Attributes["label"], Is{V: "Jan 1 03:00 a &plane; b"})
}

func TestGetBooking_haveDateArr_linkWhenArr(t *testing.T) {
	class := makeClassWithConfig(Config{TravelLink: "travel.yandex"})
	class.Request = &request.RequestMock{Tld: "ru"}
	ticket := Ticket{data: TicketData{IexDict{"city_arr_geoid": "1", "date_arr_rfc": epoch, "date_dep_rfc": epoch}}}
	c, _ := class.getBooking(ticket)
	AssertThat(t, c.Attributes["value"], Is{V: "travel.yandex.ru/search/rooms?to=1&when=1970-01-01"})
}

func TestGetBooking_noDateArrhaveDateDep_linkWhenDep(t *testing.T) {
	class := makeClassWithConfig(Config{TravelLink: "travel.yandex"})
	class.Request = &request.RequestMock{Tld: "ru"}
	ticket := Ticket{data: TicketData{IexDict{"city_arr_geoid": "1", "date_dep_rfc": epoch}}}
	c, _ := class.getBooking(ticket)
	AssertThat(t, c.Attributes["value"], Is{V: "travel.yandex.ru/search/rooms?to=1&when=1970-01-01"})
}

func TestGetBooking_noDates_linkNoWhen(t *testing.T) {
	class := makeClassWithConfig(Config{TravelLink: "travel.yandex"})
	class.Request = &request.RequestMock{Tld: "ru"}
	ticket := Ticket{data: TicketData{IexDict{"city_arr_geoid": "1"}}}
	c, _ := class.getBooking(ticket)
	AssertThat(t, c.Attributes["value"], Is{V: "travel.yandex.ru/search/rooms?to=1"})
}

func TestGetWeather_badCity_returnsError(t *testing.T) {
	class := makeClass()
	_, err := class.getWeather(Ticket{data: TicketData{IexDict{"date_arr_rfc": epoch}}})
	AssertThat(t, err, Not{V: nil})
}

func TestGetWeather_badDate_returnsError(t *testing.T) {
	class := makeClass()
	_, err := class.getWeather(Ticket{data: TicketData{IexDict{"city_arr_geoid": 1}}})
	AssertThat(t, err, Not{V: nil})
}

func TestGetWeather_weatherError_returnsError(t *testing.T) {
	class := makeClass()
	class.Weather = &weather.Mock{Err: emptyError{}}
	_, err := class.getWeather(Ticket{data: TicketData{IexDict{"city_arr_geoid": 1, "date_arr_rfc": epoch}}})
	AssertThat(t, err, Not{V: nil})
}

func TestGetWeather_ok_returnsWeather(t *testing.T) {
	class := makeClass()
	class.Weather = &weather.Mock{Data: "w"}
	c, err := class.getWeather(Ticket{data: TicketData{IexDict{"city_arr_geoid": 1, "date_arr_rfc": epoch}}})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, c.Attributes["label"], Is{V: "w"})
}

func TestGetCurrency_badFrom_returnsError(t *testing.T) {
	class := makeClass()
	_, err := class.getCurrency(Ticket{data: TicketData{IexDict{"to_country_geoid": 1}}})
	AssertThat(t, err, Not{V: nil})
}

func TestGetCurrency_badTo_returnsError(t *testing.T) {
	class := makeClass()
	_, err := class.getCurrency(Ticket{data: TicketData{IexDict{"from_country_geoid": 1}}})
	AssertThat(t, err, Not{V: nil})
}

func TestGetCurrency_currencyError_returnsError(t *testing.T) {
	class := makeClass()
	class.Currency = &currency.Mock{Err: emptyError{}}
	ticket := Ticket{data: TicketData{IexDict{"from_country_geoid": 1, "to_country_geoid": 1}}}
	_, err := class.getCurrency(ticket)
	AssertThat(t, err, Not{V: nil})
}

func TestGetCurrency_ok_returnsCurrency(t *testing.T) {
	class := makeClass()
	class.Currency = &currency.Mock{Data: "c"}
	ticket := Ticket{data: TicketData{IexDict{"from_country_geoid": 1, "to_country_geoid": 1}}}
	c, err := class.getCurrency(ticket)
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, c.Attributes["label"], Is{V: "c"})
}

func TestGetTransfer_noTransfer_returnsError(t *testing.T) {
	class := makeClass()
	_, err := class.getTransfer(Ticket{data: TicketData{}})
	AssertThat(t, err, Not{V: nil})
}

func TestGetTransfer_unknownTransfer_returnsError(t *testing.T) {
	class := makeClass()
	_, err := class.getTransfer(Ticket{data: TicketData{IexDict{"transfer_type": "husky"}}})
	AssertThat(t, err, Not{V: nil})
}

func TestGetTransfer_okAeroexpress_returnsAeroexpressLabelAndLink(t *testing.T) {
	class := makeClassWithConfig(Config{AeroexpressLink: "link"})
	c, err := class.getTransfer(Ticket{data: TicketData{IexDict{"transfer": "aeroexpress"}}})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, c.Attributes["label"], Is{V: "_Aeroexpress_"})
	AssertThat(t, c.Attributes["value"], Is{V: "link"})
}

func TestGetTransfer_okYTaxi_returnsTaxiLabelLink(t *testing.T) {
	class := makeClassWithConfig(Config{YTaxiLink: "link"})
	c, err := class.getTransfer(Ticket{data: TicketData{IexDict{"transfer": "ytaxi"}}})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, c.Attributes["label"], Is{V: "_Taxi_"})
	AssertThat(t, c.Attributes["value"], Is{V: "link"})
}

func TestGetTransfer_okTaxi_returnsTaxiLabelLink(t *testing.T) {
	class := makeClassWithConfig(Config{SearchLink: "ya"})
	class.Request = &request.RequestMock{Tld: "ru"}
	c, err := class.getTransfer(Ticket{data: TicketData{IexDict{"transfer": "taxi", "city_dep": "city with/url?unsafe&symbols"}}})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, c.Attributes["label"], Is{V: "_Taxi_"})
	AssertThat(t, c.Attributes["value"], Is{V: "ya.ru/search/?text=_Taxi_+city+with%2Furl%3Funsafe%26symbols"})
}

func TestGetPrintOrMoreTransfer_hasParts_returnsPrintControl(t *testing.T) {
	class := makeClass()
	c, err := class.getPrintOrMoreTransfer(Ticket{data: TicketData{IexDict{"print_parts": []interface{}{"1.1"}}}})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, c.Type, Is{V: Print})
	AssertThat(t, c.Role, Is{V: Action2.String()})
	AssertThat(t, c.Attributes["label"], Is{V: "_Print_"})
	AssertThat(t, c.Attributes["value"], Is{V: "1.1"})
}

func TestGetPrintOrMoreTransfer_hasNoPartsAndAeroexpressTransfer_returnsYTaxiControl(t *testing.T) {
	class := makeClassWithConfig(Config{YTaxiLink: "link"})
	c, err := class.getPrintOrMoreTransfer(Ticket{data: TicketData{IexDict{"transfer": "aeroexpress"}}})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, c.Type, Is{V: Link})
	AssertThat(t, c.Role, Is{V: Action2.String()})
	AssertThat(t, c.Attributes["label"], Is{V: "_Taxi_"})
	AssertThat(t, c.Attributes["value"], Is{V: "link"})
}

func TestGetPrintOrMoreTransfer_hasNoPartsAndYTaxiTransfer_returnsNoWidget(t *testing.T) {
	class := makeClass()
	_, err := class.getPrintOrMoreTransfer(Ticket{data: TicketData{IexDict{"transfer": "ytaxi"}}})
	AssertThat(t, err, Not{V: nil})
}

func TestGetPrintOrMoreTransfer_hasNoPartsAndTaxiTransfer_returnsNoWidget(t *testing.T) {
	class := makeClass()
	_, err := class.getPrintOrMoreTransfer(Ticket{data: TicketData{IexDict{"transfer": "taxi"}}})
	AssertThat(t, err, Not{V: nil})
}

func TestGetPrintOrLink_hasPrintParts_returnsPrintWithParts(t *testing.T) {
	class := makeClass()
	c, err := class.getPrintOrLink(Ticket{data: TicketData{IexDict{"print_parts": []interface{}{"1.1"}}}})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, c.Type, Is{V: Print})
	AssertThat(t, c.Role, Is{V: Action2.String()})
	AssertThat(t, c.Attributes["label"], Is{V: "_Print_"})
	AssertThat(t, c.Attributes["value"], Is{V: "1.1"})
}

func TestGetPrintOrLink_hasNoPrintPartsAndHasUrl_returnsLink(t *testing.T) {
	class := makeClass()
	c, err := class.getPrintOrLink(Ticket{data: TicketData{IexDict{"url": "link"}}})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, c.Type, Is{V: Link})
	AssertThat(t, c.Role, Is{V: Action2.String()})
	AssertThat(t, c.Attributes["label"], Is{V: "_GoToOrder_"})
	AssertThat(t, c.Attributes["value"], Is{V: "link"})
}

func TestGetPrintOrLink_hasNoPrintPartsAndNoLink_returnsEmptyPrint(t *testing.T) {
	class := makeClass()
	c, err := class.getPrintOrLink(Ticket{data: TicketData{IexDict{}}})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, c.Type, Is{V: Print})
	AssertThat(t, c.Role, Is{V: Action2.String()})
	AssertThat(t, c.Attributes["label"], Is{V: "_Print_"})
	AssertThat(t, c.Attributes["value"], Is{V: ""})
}

func TestGetGoToOrder_wrongSubtype_returnsError(t *testing.T) {
	class := makeClass()
	_, err := class.getGoToOrder(Ticket{data: TicketData{IexDict{"widget_subtype": "reminder"}}})
	AssertThat(t, err, Not{V: nil})
}

func TestGetGoToOrder_correctSubtypeButNoUrl_returnsError(t *testing.T) {
	class := makeClass()
	_, err := class.getGoToOrder(Ticket{data: TicketData{IexDict{"widget_subtype": "booking"}}})
	AssertThat(t, err, Not{V: nil})
}

func TestGetGoToOrder_correctSubtypeAndHasUrl_returnsLink(t *testing.T) {
	class := makeClass()
	c, err := class.getGoToOrder(Ticket{data: TicketData{IexDict{"widget_subtype": "booking", "url": "link"}}})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, c.Type, Is{V: Link})
	AssertThat(t, c.Role, Is{V: Action2.String()})
	AssertThat(t, c.Attributes["label"], Is{V: "_GoToOrder_"})
	AssertThat(t, c.Attributes["value"], Is{V: "link"})
}
