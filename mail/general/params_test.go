package request

import (
	"testing"
)

import . "a.yandex-team.ru/mail/iex/matchers"

func TestRequestGetTld_reqHasTld_returnsTldFromReq(t *testing.T) {
	req := Mock{Params: map[string]string{"tld": "ua"}}
	impl := RequestImpl{Req: req, DefaultTLD: "ru"}
	AssertThat(t, impl.GetTld(), Is{V: "ua"})
}

func TestRequestGetTld_reqHasBadTld_returnsTldFromConfig(t *testing.T) {
	req := Mock{Params: map[string]string{"tld": "phishing.com"}}
	impl := RequestImpl{Req: req, DefaultTLD: "ru"}
	AssertThat(t, impl.GetTld(), Is{V: "ru"})
}

func TestRequestGetTld_reqHasNoTld_returnsTldFromConfig(t *testing.T) {
	impl := RequestImpl{Req: Mock{}, DefaultTLD: "ru"}
	AssertThat(t, impl.GetTld(), Is{V: "ru"})
}
