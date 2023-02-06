package agent

import (
	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"
	"a.yandex-team.ru/market/sre/library/proto/juggler_pb"
	"encoding/json"
	"github.com/golang/protobuf/proto"
	"github.com/google/go-cmp/cmp"
	"github.com/labstack/echo/v4"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

func TestJugglerEventsBulk(t *testing.T) {
	eventsRequest := juggler_pb.SendEventsRequest{
		Events: []*juggler_pb.Event{
			{
				Host:    "test",
				Status:  "ok",
				Service: "ssh",
			},
			{
				Host:    "test2",
				Status:  "ok",
				Service: "web",
			},
		},
	}
	request, err := json.Marshal(&eventsRequest)
	require.NoError(t, err)

	eventsResponse := juggler_pb.SendEventsResponse{
		Events: []*juggler_pb.EventResponse{
			{
				Code: 200,
			},
			{
				Code: 200,
			},
		},
	}
	response, err := json.Marshal(&eventsResponse)
	require.NoError(t, err)

	a := new(Agent)
	a.Echo = echo.New()
	a.Logger, _ = zap.New(zap.ConsoleConfig(log.DebugLevel))

	req := httptest.NewRequest(http.MethodPost, "/api/1/batch", strings.NewReader(string(request)))
	req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)
	rec := httptest.NewRecorder()
	c := a.Echo.NewContext(req, rec)

	assert.NoError(t, JugglerEventsBulk(a)(c))
	events, errors := a.JugglerEvents.GetAllEvents()
	assert.Equal(t, http.StatusOK, rec.Code)
	assert.Equal(t, string(response), strings.TrimSpace(rec.Body.String()))
	assert.True(t, cmp.Equal(eventsRequest.Events, events, cmp.Comparer(proto.Equal)))
	assert.Empty(t, errors)
}
