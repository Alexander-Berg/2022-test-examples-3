package zephyrlib

import zephyr "a.yandex-team.ru/search/zephyr/proto"

func GetInstanceAlpha() *zephyr.Instance {
	return &zephyr.Instance{
		Fqdn:    "alpha",
		Port:    80,
		Project: "test",
		Stage:   "production",
		Methods: map[string]*zephyr.Method{
			"/test.Service/sayHello": {
				Input:   "input-alpha",
				Output:  "output-alpha",
				Timeout: 10,
				Retries: 3,
				Aliases: []string{"/hello"},
			},
			"/test.Service/sayHelloQuickly": {
				Input:   "input-alpha",
				Output:  "output-alpha",
				Timeout: 1,
				Retries: 3,
			},
			"/test.Service/sayHelloOnce": {
				Input:   "input-alpha",
				Output:  "output-alpha",
				Timeout: 10,
				Retries: 1,
			},
			"/test.Service/ping": {
				Input:   "input-bravo",
				Output:  "output-bravo",
				Timeout: 1,
				Retries: 1,
				Aliases: []string{"/ping"},
			},
		},
		ReportedAt: 1,
		Birth:      1,
	}
}
