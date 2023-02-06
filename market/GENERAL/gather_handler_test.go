package agent

import (
	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"
	"a.yandex-team.ru/market/sre/library/proto/juggler_pb"
	"a.yandex-team.ru/market/sre/library/proto/remon_pb"
	"encoding/json"
	"github.com/labstack/echo/v4"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

func TestGatherJugglerEvents(t *testing.T) {
	eventsResponse := remon_pb.JugglerEventsResponse{
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
	response, err := json.Marshal(&eventsResponse)
	require.NoError(t, err)

	a := new(Agent)
	a.Echo = echo.New()
	a.Logger, _ = zap.New(zap.ConsoleConfig(log.DebugLevel))
	for _, event := range eventsResponse.Events {
		err := a.JugglerEvents.PutEvent(event)
		assert.NoError(t, err)
	}

	req := httptest.NewRequest(http.MethodGet, "/api/v1/gather/juggler", nil)
	rec := httptest.NewRecorder()
	c := a.Echo.NewContext(req, rec)

	assert.NoError(t, GatherJugglerEvents(a)(c))
	assert.Equal(t, http.StatusOK, rec.Code)
	assert.Equal(t, string(response), strings.TrimSpace(rec.Body.String()))
}
