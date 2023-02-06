package avia

import (
	"a.yandex-team.ru/mail/iex/taksa/tanker"
	"a.yandex-team.ru/mail/iex/taksa/tutil"
	"testing"
	"time"
)

import . "a.yandex-team.ru/mail/iex/matchers"

func TestCanPrint_hasErrorInWhen_false(t *testing.T) {
	r := Route{When: makeTimeAndErr(time.Unix(1, 0), emptyError{})}
	AssertThat(t, r.canPrint(), Is{V: false})
}

func TestCanPrint_hasErrorInFrom_false(t *testing.T) {
	r := Route{From: makeStringAndErr("", emptyError{})}
	AssertThat(t, r.canPrint(), Is{V: false})
}

func TestCanPrint_hasErrorInWhere_false(t *testing.T) {
	r := Route{Where: makeStringAndErr("", emptyError{})}
	AssertThat(t, r.canPrint(), Is{V: false})
}

func TestCanPrint_hasNoErrors_true(t *testing.T) {
	r := Route{}
	AssertThat(t, r.canPrint(), Is{V: true})
}

func TestPrintWholeTripInOneLine_isReturn(t *testing.T) {
	r := Route{
		When:     makeTimeAndErr(tutil.GetTimeFromRfcString(epoch)),
		Back:     makeTimeAndErr(tutil.GetTimeFromRfcString(epoch)),
		From:     makeStringAndErr("src", nil),
		Where:    makeStringAndErr("dst", nil),
		IsReturn: true,
		Tanker:   tanker.Mock{},
	}
	AssertThat(t, r.printWholeTripInOneLine(), Is{V: "Jan 1 03:00 src &planes; dst 03:00 Jan 1"})
}

func TestPrintWholeTripInOneLine_isNotReturnAndArrivesSameDay(t *testing.T) {
	r := Route{
		When:     makeTimeAndErr(tutil.GetTimeFromRfcString(epoch)),
		WhenArr:  makeTimeAndErr(tutil.GetTimeFromRfcString(epoch)),
		From:     makeStringAndErr("src", nil),
		Where:    makeStringAndErr("dst", nil),
		IsReturn: false,
		Tanker:   tanker.Mock{},
	}
	AssertThat(t, r.printWholeTripInOneLine(), Is{V: "Jan 1 03:00 src &plane; dst 03:00"})
}

func TestPrintWholeTripInOneLine_isNotReturnAndArrivesNextDay(t *testing.T) {
	when, err := tutil.GetTimeFromRfcString(epoch)
	r := Route{
		When:     makeTimeAndErr(when, err),
		WhenArr:  makeTimeAndErr(when.AddDate(0, 0, 1), nil),
		From:     makeStringAndErr("src", nil),
		Where:    makeStringAndErr("dst", nil),
		IsReturn: false,
		Tanker:   tanker.Mock{},
	}
	AssertThat(t, r.printWholeTripInOneLine(), Is{V: "Jan 1 03:00 src &plane; dst 03:00 Jan 2"})
}

func TestPrintWholeTripFirstLine(t *testing.T) {
	r := Route{
		From:   makeStringAndErr("src", nil),
		Where:  makeStringAndErr("dst", nil),
		Tanker: tanker.Mock{},
	}
	AssertThat(t, r.printWholeTripFirstLine(), Is{V: "src &ndash; dst"})
}

func TestPrintWholeTripSecondLine_isReturn(t *testing.T) {
	r := Route{
		When:     makeTimeAndErr(tutil.GetTimeFromRfcString(epoch)),
		Back:     makeTimeAndErr(tutil.GetTimeFromRfcString(epoch)),
		IsReturn: true,
		Tanker:   tanker.Mock{},
	}
	AssertThat(t, r.printWholeTripSecondLine(), Is{V: "Jan 1 03:00 &planes; Jan 1 03:00"})
}

func TestPrintWholeTripSecondLine_isNotReturn(t *testing.T) {
	r := Route{
		When:     makeTimeAndErr(tutil.GetTimeFromRfcString(epoch)),
		WhenArr:  makeTimeAndErr(tutil.GetTimeFromRfcString(epoch)),
		IsReturn: false,
		Tanker:   tanker.Mock{},
	}
	AssertThat(t, r.printWholeTripSecondLine(), Is{V: "Jan 1 03:00 &plane; Jan 1 03:00"})
}

func TestPrintWholeTripSecondLine_isNotReturnAndNoDateArr(t *testing.T) {
	when, err := tutil.GetTimeFromRfcString(epoch)
	r := Route{
		When:     makeTimeAndErr(when, err),
		WhenArr:  makeTimeAndErr(when, emptyError{}),
		IsReturn: false,
		Tanker:   tanker.Mock{},
	}
	AssertThat(t, r.printWholeTripSecondLine(), Is{V: "Jan 1 03:00"})
}

func TestPrintNextFlightInOneLine_haveNoAirportsAndSameDayArrival(t *testing.T) {
	r := Route{
		When:    makeTimeAndErr(tutil.GetTimeFromRfcString(epoch)),
		WhenArr: makeTimeAndErr(tutil.GetTimeFromRfcString(epoch)),
		From:    makeStringAndErr("src", nil),
		Where:   makeStringAndErr("dst", nil),
		Tanker:  tanker.Mock{},
	}
	AssertThat(t, r.printNextFlightInOneLine(), Is{V: "Jan 1 03:00 src &plane; dst 03:00"})
}

func TestPrintNextFlightInOneLine_haveNoAirportsAndNextDayArrival(t *testing.T) {
	when, err := tutil.GetTimeFromRfcString(epoch)
	r := Route{
		When:    makeTimeAndErr(when, err),
		WhenArr: makeTimeAndErr(when.AddDate(0, 0, 1), nil),
		From:    makeStringAndErr("src", nil),
		Where:   makeStringAndErr("dst", nil),
		Tanker:  tanker.Mock{},
	}
	AssertThat(t, r.printNextFlightInOneLine(), Is{V: "Jan 1 03:00 src &plane; dst 03:00 Jan 2"})
}

func TestPrintNextFlightInOneLine_haveAirports(t *testing.T) {
	r := Route{
		When:         makeTimeAndErr(tutil.GetTimeFromRfcString(epoch)),
		WhenArr:      makeTimeAndErr(tutil.GetTimeFromRfcString(epoch)),
		From:         makeStringAndErr("src", nil),
		Where:        makeStringAndErr("dst", nil),
		FromAirport:  makeStringAndErr("a", nil),
		WhereAirport: makeStringAndErr("b", nil),
		Tanker:       tanker.Mock{},
	}
	AssertThat(t, r.printNextFlightInOneLine(), Is{V: "Jan 1 03:00 src (a) &plane; dst (b) 03:00"})
}

func TestPrintNextFlightFirstLine(t *testing.T) {
	r := Route{
		From:   makeStringAndErr("src", nil),
		Where:  makeStringAndErr("dst", nil),
		Tanker: tanker.Mock{},
	}
	AssertThat(t, r.printWholeTripFirstLine(), Is{V: "src &ndash; dst"})
}

func TestPrintNextFlightSecondLine_haveDateArr(t *testing.T) {
	r := Route{
		When:    makeTimeAndErr(tutil.GetTimeFromRfcString(epoch)),
		WhenArr: makeTimeAndErr(tutil.GetTimeFromRfcString(epoch)),
		Tanker:  tanker.Mock{},
	}
	AssertThat(t, r.printNextFlightSecondLine(), Is{V: "Jan 1 03:00 &plane; Jan 1 03:00"})
}

func TestPrintNextFlightSeconfLine_haveNoDateArr(t *testing.T) {
	when, err := tutil.GetTimeFromRfcString(epoch)
	r := Route{
		When:    makeTimeAndErr(when, err),
		WhenArr: makeTimeAndErr(when, emptyError{}),
		Tanker:  tanker.Mock{},
	}
	AssertThat(t, r.printNextFlightSecondLine(), Is{V: "Jan 1 03:00"})
}
