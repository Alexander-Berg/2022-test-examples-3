package tutil

import (
	"testing"
)

import . "a.yandex-team.ru/mail/iex/matchers"

func err(_ string, e error) error  { return e }
func res(s string, _ error) string { return s }

func TestDomainFromEmail_emptyEmail_givesError(t *testing.T) {
	AssertThat(t, err(DomainFromEmail("")), Not{V: nil})
}

func TestDomainFromEmail_badEmailNoAt_givesError(t *testing.T) {
	AssertThat(t, err(DomainFromEmail("aaa")), Not{V: nil})
}

func TestDomainFromEmail_badEmailManyAt_givesError(t *testing.T) {
	AssertThat(t, err(DomainFromEmail("a@a@a")), Not{V: nil})
}

func TestDomainFromEmail_goodEmail_givesError(t *testing.T) {
	AssertThat(t, err(DomainFromEmail("a@b")), Is{V: nil})
	AssertThat(t, res(DomainFromEmail("a@b")), Is{V: "b"})
}
