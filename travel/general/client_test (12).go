package pathfinder

import (
	"bytes"
	"context"
	"io/ioutil"
	"net/http"
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"
	"a.yandex-team.ru/travel/library/go/httputil/client"
	"a.yandex-team.ru/travel/trains/library/go/httputil/clients/pathfinder/models"
)

const IsolatedRun = true

func TestClient_Search(t *testing.T) {

	logger, _ := zap.New(zap.ConsoleConfig(log.InfoLevel))
	ctx := context.Background()
	cfg := Config{
		BaseURL: "https://testing.pathfinder-core.rasp.yandex.net",
		Timeout: DefaultConfig.Timeout,
	}

	t.Run("success call", func(t *testing.T) {

		request := models.SearchRequest{
			FromType: models.PointTypeSettlement,
			FromID:   10891,
			ToType:   models.PointTypeStation,
			ToID:     9817844,
			Date:     "2021-06-05",
			TType:    models.TransportTypeTrain,
			Mode:     models.SearchModeCommon,
		}

		var transport http.RoundTripper = nil
		if IsolatedRun {
			transport = client.TransportMock(func(req *http.Request) (*http.Response, error) {
				assert.Equal(t,
					"date=2021-06-05&from_id=10891&from_type=settlement&mode=1&to_id=9817844&to_type=station&ttype=1",
					req.URL.RawQuery)

				respBody, err := ioutil.ReadFile("gotest/search_response.xml")
				assert.NoError(t, err)

				return &http.Response{
					StatusCode: http.StatusOK,
					Body:       ioutil.NopCloser(bytes.NewBuffer(respBody)),
					Header:     make(http.Header),
				}, nil
			})
		}

		client, err := NewPathfinderClientWithTransport(&cfg, logger, transport)
		assert.NoError(t, err)

		response, err := client.Search(ctx, &request)
		assert.NoError(t, err)
		assert.NotEmpty(t, response.Variants[6].Routes[2].ThreadID)
	})
}
