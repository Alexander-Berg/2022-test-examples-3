package config

import (
	"reflect"
	"testing"
	"time"

	"rex/common/config"
)

var expectedConfig = &Config{
	Log: config.LogConfig{
		Bytes: DefaultLogBytes,
		Count: DefaultLogCount,
	},
	Notify: NotifyConfig{
		SMS:      SMSConfig{Smsc: SmscConfig{Timeout: 5 * time.Second}},
		Telegram: TelegramConfig{Timeout: 5 * time.Second},
	},
	Vars: VarsConfig{
		File:  ".rex.vars",
		Set:   30 * time.Second,
		Write: 45 * time.Second,
	},
	Rules: RulesConfig{
		TasksConfig: TasksConfig{
			Global: TaskConfig{Interval: 30 * time.Second, Timeout: 5 * time.Second},
			HTTP:   TaskConfig{Interval: 120 * time.Second, Timeout: 5 * time.Second},
			DNS:    TaskConfig{Interval: 30 * time.Second, Timeout: 5 * time.Second},
		},
		Files: []string{
			"testdata/rules/outer.yaml",
			"testdata/rules/inner/inner.yaml",
		},
	},
}

func TestParseFile(t *testing.T) {
	c, err := ParseFile("testdata/valid.yml")
	if err != nil {
		t.Fatal(err)
	}
	if !reflect.DeepEqual(c, expectedConfig) {
		t.Fatalf("configs differ:\n%+v\n%+v", c, expectedConfig)
	}
}
