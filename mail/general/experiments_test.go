package experiments

import "testing"

import . "a.yandex-team.ru/mail/iex/matchers"

func TestParseHeader_empty(t *testing.T) {
	exps, err := ParseHeader("")
	AssertThat(t, exps, EqualTo{V: Experiments(nil)})
	AssertThat(t, err, Is{V: nil})
}

func TestParseHeader_comma(t *testing.T) {
	exps, err := ParseHeader("105,356,865")
	AssertThat(t, exps, EqualTo{V: Experiments([]string{"105"})})
	AssertThat(t, err, Is{V: nil})
}

func TestParseHeader_comma_semicolon(t *testing.T) {
	exps, err := ParseHeader("105,0,865;12,0,80;15,0,23")
	AssertThat(t, exps, EqualTo{V: Experiments([]string{"105", "12", "15"})})
	AssertThat(t, err, Is{V: nil})
}

func TestParseHeader_spaces(t *testing.T) {
	exps, err := ParseHeader("  ; 51678, 0,80 ; 37819, 0 , 78 ; 91038 , 0 , 56 ;  34617 ,0 , 123;")
	AssertThat(t, exps, EqualTo{V: Experiments([]string{"51678", "37819", "91038", "34617"})})
	AssertThat(t, err, Is{V: nil})
}

func TestParseHeader_extra_chars(t *testing.T) {
	exps, err := ParseHeader(";51678, 0, 80;;37819,0,78abc;;;")
	AssertThat(t, exps, EqualTo{V: Experiments([]string{"51678", "37819"})})
	AssertThat(t, err, Is{V: nil})
}

func TestParseHeader_malformed_triple(t *testing.T) {
	exps, err := ParseHeader("83711,0,70;51678,80;37819,0,78;")
	AssertThat(t, exps, EqualTo{V: Experiments(nil)})
	AssertThat(t, err, Is{V: Not{V: nil}})
}

func TestContainsOneOf_nil(t *testing.T) {
	result := Experiments(nil).ContainsOneOf(Experiments(nil))
	AssertThat(t, result, EqualTo{V: false})
}

func TestContainsOneOf_empty(t *testing.T) {
	result := Experiments([]string{}).ContainsOneOf(Experiments([]string{}))
	AssertThat(t, result, EqualTo{V: false})
}

func TestContainsOneOf_self_nil(t *testing.T) {
	result := Experiments(nil).ContainsOneOf(Experiments([]string{"34572", "20998"}))
	AssertThat(t, result, EqualTo{V: false})
}

func TestContainsOneOf_self_empty(t *testing.T) {
	result := Experiments([]string{}).ContainsOneOf(Experiments([]string{"34572", "20998"}))
	AssertThat(t, result, EqualTo{V: false})
}

func TestContainsOneOf_exps_nil(t *testing.T) {
	result := Experiments([]string{"34572", "20998"}).ContainsOneOf(Experiments(nil))
	AssertThat(t, result, EqualTo{V: false})
}

func TestContainsOneOf_exps_empty(t *testing.T) {
	result := Experiments([]string{"34572", "20998"}).ContainsOneOf(Experiments([]string{}))
	AssertThat(t, result, EqualTo{V: false})
}

func TestContainsOneOf_contains_one(t *testing.T) {
	result := Experiments([]string{"34572", "20998", "72873"}).ContainsOneOf(Experiments([]string{"38911", "20998"}))
	AssertThat(t, result, EqualTo{V: true})
}
