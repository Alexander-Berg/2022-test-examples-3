package config

import (
	"fmt"
	"os"
	"testing"
)

import . "a.yandex-team.ru/mail/iex/matchers"

func TestCheckMandatoryFields_configWithoutLog_givesError(t *testing.T) {
	defer os.Remove("./test")
	file := helpCreateFile("{}")
	if e := file.Close(); e != nil {
		panic(fmt.Sprintf("[taksa] test fatal error while closing file [%v]", e))
	}
	_, e := Get("./test")
	AssertThat(t, e, Is{V: Not{V: nil}})
}
