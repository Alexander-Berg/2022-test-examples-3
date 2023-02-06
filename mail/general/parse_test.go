package config

import (
	"os"
	"testing"

	. "a.yandex-team.ru/mail/iex/matchers"
)

func TestParse(t *testing.T) {
	defer os.Remove("./test")
	file := helpCreateFile(`{"a": 1, "b": [1, 2], "c": {"x": true, "y": false}}`)

	_, e := parse(file)
	AssertThat(t, e, Is{V: (*ConfigError)(nil)})

	file = helpCreateFile(`{"log": "value", "server": {"port": 1234}}`)
	cfg, e := parse(file)
	AssertThat(t, e, Is{V: (*ConfigError)(nil)})
	AssertThat(t, cfg.LogFile, EqualTo{V: "value"})
	AssertThat(t, cfg.Server.Port, EqualTo{V: 1234})
}
