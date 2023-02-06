package config

import (
	"fmt"
	"os"
	"os/exec"
	"testing"
)

import . "a.yandex-team.ru/mail/iex/matchers"

func helpCreateFile(str string) *os.File {
	file, err := os.Create("./test")
	if err != nil {
		panic(fmt.Sprintf("[taksa] test fatal error while creating file [%v]", err))
	}

	if _, err := file.WriteString(str); err != nil {
		panic(fmt.Sprintf("[taksa] test fatal error while writing file [%v]", err))
	}
	if err := file.Close(); err != nil {
		panic(fmt.Sprintf("[taksa] test fatal error while closing file [%v]", err))
	}

	file, err = os.Open("./test")
	if err != nil {
		panic(fmt.Sprintf("[taksa] test fatal error while opening file [%v]", err))
	}

	return file
}

func TestGet_givenFile_closesFile(t *testing.T) {
	defer os.Remove("./test")

	file := helpCreateFile("{}")

	err := file.Close()
	if err != nil {
		panic(fmt.Sprintf("[taksa] test fatal error while closing file [%v]", err))
	}

	Get("./test")

	lsof, _ := exec.Command("lsof", "./test").Output()
	AssertThat(t, len(lsof), Is{V: 0})
}
