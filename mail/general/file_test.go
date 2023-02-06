package config

import (
	"fmt"
	"os"
	"testing"
)

import . "a.yandex-team.ru/mail/iex/matchers"

func openFileError(s string) *ConfigError {
	_, e := openFile(s)
	return e
}

func TestOpenFile_nonexistentFile_givesError(t *testing.T) {
	AssertThat(t, openFileError("./test"), Is{V: Not{V: nil}})
}

func TestOpenFile_fileWithBadPermssions_givesError(t *testing.T) {
	file, e := os.Create("./test")
	if e != nil {
		panic(fmt.Sprintf("[taksa] test fatal error while creating file [%v]", e))
	}
	defer os.Remove("./test")
	if e := file.Chmod(0200); e != nil {
		panic(fmt.Sprintf("[taksa] test fatal error while chmodding file [%v]", e))
	}
	AssertThat(t, openFileError("./test"), Is{V: Not{V: nil}})
}
