package errs

import (
	"testing"
)

import . "a.yandex-team.ru/mail/iex/matchers"

func TestBadRequestError(t *testing.T) {
	AssertThat(t, BadRequest{Err: "test"}.Error(), Is{V: "{\n\t\"error\": \"test\"\n}"})
}

func TestInternalErrorError(t *testing.T) {
	AssertThat(t, InternalError{Err: "test"}.Error(), Is{V: "{\n\t\"error\": \"test\"\n}"})
}
