package pkpass

import (
	"a.yandex-team.ru/mail/iex/taksa/iex"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"a.yandex-team.ru/mail/iex/taksa/tanker"
	"testing"
)

import . "a.yandex-team.ru/mail/iex/taksa/widgets/common"
import . "a.yandex-team.ru/mail/iex/matchers"

const (
	future  int    = 2468486280
	past    int    = 1468486280
	invalid string = "this is not a time string"
)

func extend(iexSubtype string, date int) (Widget, error) {
	pass := map[string]interface{}{
		"taksa_widget_type_1234543456546": "event-ticket",
		"title":                           "The Hound of the Baskervilles 2: The Rise of Taksa",
		"widget_subtype":                  iexSubtype,
		"start_date_ts":                   date,
	}
	iexFactsArray := []interface{}{pass}
	fact := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: iexFactsArray}

	return Class{
		Fact:   fact,
		Logger: logger.Mock{},
		Tanker: tanker.Mock{}}.Extend()
}

func err(_ Widget, e error) error     { return e }
func widget(w Widget, _ error) Widget { return w }

func TestExtend_invalidIexSubtype_givesError(t *testing.T) {
	AssertThat(t, err(extend("", future)), Not{V: nil})
}

func TestExtend_cinemaWithPastTime_givesNoWidget(t *testing.T) {
	AssertThat(t, widget(extend("cinema", past)), Is{V: nil})
}

func TestExtend_cinemaWithFutureTime_givesCinemaWidget(t *testing.T) {
	AssertThat(t, widget(extend("cinema", future)), TypeOf{V: &CinemaWidget{}})
}
