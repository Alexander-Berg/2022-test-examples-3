package pkpass

import (
	"a.yandex-team.ru/mail/iex/taksa/tanker"
	"testing"
)

import . "a.yandex-team.ru/mail/iex/matchers"

func TestMakeRows_WithIexDataContainsRowsAndSeats_ReturnsSeatsGroupedByRows(t *testing.T) {
	seat := func(row, seat float64) map[string]interface{} {
		return map[string]interface{}{"row": row, "seat": seat}
	}
	iexData := []interface{}{seat(1, 2), seat(1, 3), seat(2, 4)}
	rows := makeRows(iexData)
	AssertThat(t, rows, ElementsAre{Row{1, Seats{2, 3}}, Row{2, Seats{4}}})
}

func TestMakeRows_WithEmptyIexData_ReturnsNoRows(t *testing.T) {
	iexData := []interface{}{}
	rows := makeRows(iexData)
	AssertThat(t, rows, ElementsAre{})
}

func TestMakeRows_WithIexBadData_ReturnsNoRows(t *testing.T) {
	seat := func(row, seat string) map[string]interface{} {
		return map[string]interface{}{"row": row, "seat": seat}
	}
	iexData := []interface{}{seat("a", "b"), seat("c", "d"), seat("e", "f")}
	rows := makeRows(iexData)
	AssertThat(t, rows, ElementsAre{})
}

func TestPrintRows_WithEmptyRows_ReturnEmptyString(t *testing.T) {
	s := printRows(Rows{}, tanker.Mock{})
	AssertThat(t, s, Is{V: ""})
}

func TestPrintRows_WithRowContainsOneSeat_ReturnsRowAndOneSeat(t *testing.T) {
	s := printRows(Rows{Row{4, Seats{5}}}, tanker.Mock{})
	AssertThat(t, s, Is{V: "_Row_ 4, _Seat_ 5"})
}

func TestPrintRows_WithRowContainsTwoSeats_ReturnsRowAndTwoSeatsWithAnd(t *testing.T) {
	s := printRows(Rows{Row{1, Seats{2, 3}}}, tanker.Mock{})
	AssertThat(t, s, Is{V: "_Row_ 1, _Seat_ 2 _And_ 3"})
}

func TestPrintRows_WithRowContainsThreeSeats_ReturnsRowAndFirstSeatsWithCommaAndLastSeatWithAnd(t *testing.T) {
	s := printRows(Rows{Row{1, Seats{1, 2, 3}}}, tanker.Mock{})
	AssertThat(t, s, Is{V: "_Row_ 1, _Seat_ 1, 2 _And_ 3"})
}

func TestPrintRows_WithRows_ReturnsRowsWithSeatsDelimitedBySemicolon(t *testing.T) {
	s := printRows(Rows{Row{1, Seats{2}}, Row{4, Seats{5}}}, tanker.Mock{})
	AssertThat(t, s, Is{V: "_Row_ 1, _Seat_ 2; _Row_ 4, _Seat_ 5"})
}
