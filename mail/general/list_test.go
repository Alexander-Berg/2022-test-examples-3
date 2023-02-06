package list

import (
	"a.yandex-team.ru/mail/iex/taksa/client"
	"a.yandex-team.ru/mail/iex/taksa/errs"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/request"
	"a.yandex-team.ru/mail/iex/taksa/widgets"
	"testing"
)

import . "a.yandex-team.ru/mail/iex/matchers"

func TestList_noWidgetableEnvelopes_returnsImmediately(t *testing.T) {
	body := `{"envelopes":[{"mid":"1","types":[]}]}`
	req := request.Mock{Params: map[string]string{"uid": "123", "version": "hound"}, Body: []byte(body)}
	actual, err := Method{Config{}, widgets.Config{}}.Do(req, logger.Mock{}, client.Mock{})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, actual, Is{V: "{\n\t\"widgets\": []\n}"})
}

func TestList_normalInput_noErrorExpectedResponse(t *testing.T) {
	cfg := Config{IexPort: 10306}
	body := `{"envelopes":[{"mid":"1","subject":"a","types":[2,3]},{"mid":"4","subject":"b","types":[5,6]}]}`
	req := request.Mock{Params: map[string]string{"uid": "123", "version": "hound"}, Body: []byte(body)}
	cli := client.Mock{Data: `{"bodies":[{"transformerResult":{"textTransformerResult":{"facts":"f"}}}]}`}
	_, err := Method{cfg, widgets.Config{}}.Do(req, logger.Mock{}, cli)
	AssertThat(t, err, Is{V: nil})
}

func TestList_noUid_failWithErrorBadRequest(t *testing.T) {
	cfg := Config{IexPort: 10306}
	req := request.Mock{Params: map[string]string{}, Body: []byte(`{"a":1}`)}
	_, err := Method{cfg, widgets.Config{}}.Do(req, logger.Mock{}, client.Mock{})
	AssertThat(t, err, TypeOf{V: errs.BadRequest{}})
}

func TestList_noVersion_failWithErrorBadRequest(t *testing.T) {
	cfg := Config{IexPort: 10306}
	req := request.Mock{Params: map[string]string{"uid": "123"}, Body: []byte(`{"a":1}`)}
	_, err := Method{cfg, widgets.Config{}}.Do(req, logger.Mock{}, client.Mock{})
	AssertThat(t, err, TypeOf{V: errs.BadRequest{}})
}
