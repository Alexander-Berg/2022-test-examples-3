package hotels

import (
	"a.yandex-team.ru/mail/iex/taksa/tanker"
	"testing"
	"time"
)

import . "a.yandex-team.ru/mail/iex/matchers"

func TestPrintDetailsInOneLine_noDetails_empty(t *testing.T) {
	d := Details{Tanker: tanker.Mock{}}
	AssertThat(t, d.printDetailsInOneLine(), Is{V: ""})
}

func TestPrintDetailsInOneLine_hasSinceAndNoTill_empty(t *testing.T) {
	d := Details{Since: time.Unix(1, 0), Tanker: tanker.Mock{}}
	AssertThat(t, d.printDetailsInOneLine(), Is{V: ""})
}

func TestPrintDetailsInOneLine_hasTillAndNoSince_empty(t *testing.T) {
	d := Details{Till: time.Unix(1, 0), Tanker: tanker.Mock{}}
	AssertThat(t, d.printDetailsInOneLine(), Is{V: ""})
}

func TestPrintDetailsInOneLine_hasTillAndSince_timeBounds(t *testing.T) {
	d := Details{Since: time.Unix(1, 0), Till: time.Unix(1, 0), Tanker: tanker.Mock{}}
	AssertThat(t, d.printDetailsInOneLine(), Is{V: "Jan 1 &ndash; Jan 1"})
}

func TestPrintDetailsInOneLine_hasCity_city(t *testing.T) {
	d := Details{City: "c", Tanker: tanker.Mock{}}
	AssertThat(t, d.printDetailsInOneLine(), Is{V: "c"})
}

func TestPrintDetailsInOneLine_hasCityAndSinceAndTill_cityAndTimeBounds(t *testing.T) {
	d := Details{City: "c", Since: time.Unix(1, 0), Till: time.Unix(1, 0), Tanker: tanker.Mock{}}
	AssertThat(t, d.printDetailsInOneLine(), Is{V: "c, Jan 1 &ndash; Jan 1"})
}

func TestPrintDetailsInOneLine_hasCityAndTillButNoSince_city(t *testing.T) {
	d := Details{City: "c", Till: time.Unix(1, 0), Tanker: tanker.Mock{}}
	AssertThat(t, d.printDetailsInOneLine(), Is{V: "c"})
}

func TestPrintDetailsInOneLine_hasNights_nights(t *testing.T) {
	d := Details{Nights: 3, Tanker: tanker.Mock{}}
	AssertThat(t, d.printDetailsInOneLine(), Is{V: "(3 _Nights_)"})
}

func TestPrintDetailsInOneLine_hasNightsAndCity_cityAndNights(t *testing.T) {
	d := Details{City: "c", Nights: 3, Tanker: tanker.Mock{}}
	AssertThat(t, d.printDetailsInOneLine(), Is{V: "c (3 _Nights_)"})
}

func TestPrintDetailsInOneLine_hasNightsAndSinceAndTill_timeBoundsAndNights(t *testing.T) {
	d := Details{Since: time.Unix(1, 0), Till: time.Unix(1, 0), Nights: 3, Tanker: tanker.Mock{}}
	AssertThat(t, d.printDetailsInOneLine(), Is{V: "Jan 1 &ndash; Jan 1 (3 _Nights_)"})
}

func TestPrintDetailsInOneLine_fullHouse_poem(t *testing.T) {
	d := Details{City: "c", Since: time.Unix(1, 0), Till: time.Unix(1, 0), Nights: 3, Tanker: tanker.Mock{}}
	AssertThat(t, d.printDetailsInOneLine(), Is{V: "c, Jan 1 &ndash; Jan 1 (3 _Nights_)"})
}

func TestPrintDetailsFirstLine_noCity_empty(t *testing.T) {
	d := Details{Tanker: tanker.Mock{}}
	AssertThat(t, d.printDetailsFirstLine(), Is{V: ""})
}

func TestPrintDetailsFirstLine_noCountry_city(t *testing.T) {
	d := Details{City: "c", Tanker: tanker.Mock{}}
	AssertThat(t, d.printDetailsFirstLine(), Is{V: "c"})
}

func TestPrintDetailsFirstLine_cityAndCountry_cityAndCountry(t *testing.T) {
	d := Details{City: "c", Country: "rf", Tanker: tanker.Mock{}}
	AssertThat(t, d.printDetailsFirstLine(), Is{V: "c, rf"})
}

func TestPrintDetailsSecondLine_noSince_empty(t *testing.T) {
	d := Details{Till: time.Unix(1, 0), Tanker: tanker.Mock{}}
	AssertThat(t, d.printDetailsSecondLine(), Is{V: ""})
}

func TestPrintDetailsSecondLine_noTill_empty(t *testing.T) {
	d := Details{Since: time.Unix(1, 0), Tanker: tanker.Mock{}}
	AssertThat(t, d.printDetailsSecondLine(), Is{V: ""})
}

func TestPrintDetailsSecondLine_haveTimeBounds_timeBounds(t *testing.T) {
	d := Details{Since: time.Unix(1, 0), Till: time.Unix(1, 0), Tanker: tanker.Mock{}}
	AssertThat(t, d.printDetailsSecondLine(), Is{V: "Jan. 1 &ndash; Jan. 1"})
}
