package message

import (
	"errors"
	"testing"
	"time"

	"rex/common/model"
	"rex/match"
)

var MessageTextTests = []struct {
	M Universal
	S string
}{
	{
		Universal{
			Result: &match.Result{
				Time: time.Date(2018, time.June, 1, 12, 30, 0, 0, time.UTC),
				Err:  errors.New("http error"),
				Rule: &model.Rule{
					HTTP: &model.HTTP{
						Request: model.HTTPRequest{URL: "https://yandex.ru"},
					},
				},
			},
		},
		"[desktop] https://yandex.ru: http error at 12:30:00 UTC",
	},
	{
		Universal{
			Result: &match.Result{
				Time: time.Date(2018, time.June, 1, 12, 30, 0, 0, time.UTC),
				Err:  errors.New("dns error"),
				Rule: &model.Rule{
					DNS: &model.DNS{Host: "yandex.ru"},
				},
			},
		},
		"yandex.ru is not resolved: dns error at 12:30:00 UTC",
	},
}

func TestMessage_Text(t *testing.T) {
	for _, tt := range MessageTextTests {
		if s := tt.M.Text(); s != tt.S {
			t.Errorf("want %q, got %q", tt.S, s)
		}
	}
}
