package mordabackend

import (
	"bytes"
	"context"
	"encoding/json"
	"io/ioutil"
	"net/http"
	"testing"
	"time"

	"github.com/jonboulle/clockwork"
	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"
	"a.yandex-team.ru/travel/library/go/httputil/client"
	"a.yandex-team.ru/travel/trains/library/go/httputil/clients/mordabackend/models"
)

func TestClient_Search(t *testing.T) {

	logger, _ := zap.New(zap.ConsoleConfig(log.InfoLevel))
	ctx := context.Background()
	cfg := Config{
		BaseURL: "https://127.0.0.1:8080",
		Timeout: DefaultConfig.Timeout,
	}
	request := models.SearchRequest{
		PointFrom: "s123",
		Timezones: "Europe:Moscow",
		IsMobile:  true,
	}

	t.Run("success call", func(t *testing.T) {

		clock := clockwork.NewRealClock()
		response := models.SearchResponse{
			Result: models.SearchResult{
				Context: models.ResponseContext{
					TransportTypes: []string{"trains"},
					LatestDatetime: clock.Now().Round(time.Hour * 24).In(time.UTC),
				},
			},
		}

		client, err := NewMordaBackendClientWithTransport(
			&cfg,
			logger,
			client.TransportMock(func(req *http.Request) (*http.Response, error) {
				assert.Equal(t, http.MethodGet, req.Method)
				assert.Equal(t, "/ru/search/search/", req.URL.Path)
				assert.Equal(t, "127.0.0.1:8080", req.URL.Host)
				assert.Equal(t,
					"isMobile=true&pointFrom=s123&timezones=Europe%3AMoscow",
					req.URL.RawQuery)

				respBody, err := json.Marshal(response)
				assert.NoError(t, err)

				return &http.Response{
					StatusCode: http.StatusOK,
					Body:       ioutil.NopCloser(bytes.NewBuffer(respBody)),
					Header:     make(http.Header),
				}, nil
			}),
		)
		assert.NoError(t, err)

		gottenResponse, err := client.Search(ctx, &request)
		assert.NoError(t, err)
		assert.Equal(t, response.Result.Context.TransportTypes, gottenResponse.Result.Context.TransportTypes)
		assert.Equal(t, response.Result.Context.LatestDatetime, gottenResponse.Result.Context.LatestDatetime)
	})

	t.Run("bad format response", func(t *testing.T) {

		client, err := NewMordaBackendClientWithTransport(
			&cfg,
			logger,
			client.TransportMock(func(req *http.Request) (*http.Response, error) {
				return &http.Response{
					StatusCode: http.StatusOK,
					Body:       ioutil.NopCloser(bytes.NewBufferString("{")),
					Header:     make(http.Header),
				}, nil
			}),
		)
		assert.NoError(t, err)

		_, err = client.Search(ctx, &request)
		assert.Error(t, err)
	})

	t.Run("bad status code", func(t *testing.T) {

		client, err := NewMordaBackendClientWithTransport(
			&cfg,
			logger,
			client.TransportMock(func(req *http.Request) (*http.Response, error) {
				return &http.Response{
					StatusCode: http.StatusBadRequest,
					Body:       ioutil.NopCloser(bytes.NewBufferString(`{"errors": {"_schema": [{"point_to": "no_such_point"}]}, "result": {}}`)),
					Header:     make(http.Header),
				}, nil
			}),
		)
		assert.NoError(t, err)

		_, err = client.Search(ctx, &request)
		assert.Error(t, err)
	})
}
