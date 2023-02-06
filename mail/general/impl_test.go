package currency

import (
	"a.yandex-team.ru/mail/iex/taksa/client"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"testing"
)

import . "a.yandex-team.ru/mail/iex/matchers"

type emptyError struct {
}

func (e emptyError) Error() string {
	return ""
}

func MockData(value string, numerator string, denominator string) string {
	data :=
		`<stock id="40052">
		<numerator_scale>` + numerator + `</numerator_scale>
		<denominator_scale>` + denominator + `</denominator_scale>
		<sdt date="2020-12-23" time="21:31">
			<value>` + value + `</value>
		</sdt>
		<sdt date="2020-12-22" time="21:31">
			<value>8.6386</value>
		</sdt>
		<sdt date="2020-12-21" time="21:31">
			<value>9.6386</value>
		</sdt>
	</stock>`
	return data
}

func TestGet_zeroGeoid_returnsError(t *testing.T) {
	impl := Impl{Log: logger.Mock{}, Cli: client.Mock{}}
	_, err := impl.Get(0, 1)
	AssertThat(t, err, Is{V: Error("invalid geoid")})
	_, err = impl.Get(1, 0)
	AssertThat(t, err, Is{V: Error("invalid geoid")})
}

func TestGet_equalGeoids_returnsError(t *testing.T) {
	impl := Impl{Log: logger.Mock{}, Cli: client.Mock{}}
	_, err := impl.Get(83, 83)
	AssertThat(t, err, Is{V: Error("no currency exchange rates for domestic flights")})
}

func TestGet_noGeoid_returnsError(t *testing.T) {
	impl := Impl{Log: logger.Mock{}, Cli: client.Mock{}}
	_, err := impl.Get(4, 83)
	AssertThat(t, err, Is{V: Error("currency not found for 83")})
}

func TestGet_noQuoteId_returnsError(t *testing.T) {
	impl := Impl{Log: logger.Mock{}, Cli: client.Mock{}}
	_, err := impl.Get(21339, 21344)
	AssertThat(t, err, Is{V: Error("quote id not found for SRD/XOF")})
}

func TestGet_httpError_returnsError(t *testing.T) {
	impl := Impl{Log: logger.Mock{}, Cli: client.Mock{Err: emptyError{}}}
	_, err := impl.Get(84, 118)
	AssertThat(t, err, Is{V: emptyError{}})
}

func TestGet_xmlParseError_returnsError(t *testing.T) {
	data := MockData("1", "abc", "1")
	impl := Impl{Log: logger.Mock{}, Cli: client.Mock{Data: data}}
	_, err := impl.Get(84, 118)
	AssertThat(t, err, Not{V: Is{V: nil}})
}

func TestGet_emptySdts_returnsError(t *testing.T) {
	data :=
		`<stock id="40052">
		<numerator_scale>1</numerator_scale>
		<denominator_scale>1</denominator_scale>
	</stock>`
	impl := Impl{Log: logger.Mock{}, Cli: client.Mock{Data: data}}
	_, err := impl.Get(84, 118)
	AssertThat(t, err, Is{V: Error("sdts are empty")})
}

func TestGet_noValue_returnsError(t *testing.T) {
	data :=
		`<stock id="40052">
		<numerator_scale>1</numerator_scale>
		<denominator_scale>1</denominator_scale>
		<sdt date="2020-12-23" time="21:31">
			<value2>6.34</value2>
		</sdt>
	</stock>`
	impl := Impl{Log: logger.Mock{}, Cli: client.Mock{Data: data}}
	_, err := impl.Get(84, 118)
	AssertThat(t, err, Is{V: Error("no value")})
}

func TestGet_ok_returnsRate(t *testing.T) {
	data := MockData("7.16842", "1", "1")
	impl := Impl{Log: logger.Mock{}, Cli: client.Mock{Data: data}}
	rate, err := impl.Get(118, 84)
	AssertThat(t, rate, Is{V: "USD=7.17 EUR"})
	AssertThat(t, err, Is{V: nil})
}

func TestGet_numeratorScale_returnsRate(t *testing.T) {
	data := MockData("7.16342", "70", "1")
	impl := Impl{Log: logger.Mock{}, Cli: client.Mock{Data: data}}
	rate, err := impl.Get(118, 84)
	AssertThat(t, rate, Is{V: "USD=0.10 EUR"})
	AssertThat(t, err, Is{V: nil})
}

func TestGet_denominatorScale_returnsRate(t *testing.T) {
	data := MockData("7.16342", "100", "900")
	impl := Impl{Log: logger.Mock{}, Cli: client.Mock{Data: data}}
	rate, err := impl.Get(118, 84)
	AssertThat(t, rate, Is{V: "USD=64.47 EUR"})
	AssertThat(t, err, Is{V: nil})
}

func TestGet_smallValue_returnsError(t *testing.T) {
	data := MockData("7.16342", "1000", "1")
	impl := Impl{Log: logger.Mock{}, Cli: client.Mock{Data: data}}
	_, err := impl.Get(118, 84)
	AssertThat(t, err, Is{V: Error("rate is very small")})
}

func TestGet_noNumeratorDenominator_returnsRate(t *testing.T) {
	data :=
		`<stock id="40052">
		<sdt date="2020-12-23" time="21:31">
			<value>6.34</value>
		</sdt>
	</stock>`
	impl := Impl{Log: logger.Mock{}, Cli: client.Mock{Data: data}}
	rate, err := impl.Get(118, 84)
	AssertThat(t, rate, Is{V: "USD=6.34 EUR"})
	AssertThat(t, err, Is{V: nil})
}
