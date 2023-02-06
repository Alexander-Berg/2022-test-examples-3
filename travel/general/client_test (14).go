package trainapi

import (
	"bytes"
	"context"
	"io/ioutil"
	"net/http"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"
	"a.yandex-team.ru/travel/library/go/httputil/client"
	"a.yandex-team.ru/travel/trains/library/go/httputil/clients/trainapi/models"
)

func TestClient(t *testing.T) {
	const isolatedRun = true

	logger, _ := zap.New(zap.ConsoleConfig(log.InfoLevel))
	ctx := context.Background()
	cfg := Config{
		BaseURL: "https://production.train-api.rasp.internal.yandex.net/ru/api/",
		Timeout: DefaultConfig.Timeout,
	}

	t.Run("ActivePartners", func(t *testing.T) {
		request := models.ActivePartnersRequest{}

		var transport http.RoundTripper = nil
		if isolatedRun {
			transport = client.TransportMock(func(req *http.Request) (*http.Response, error) {
				assert.Equal(t, "", req.URL.RawQuery)

				respBody, err := ioutil.ReadFile("gotest/active_partners_response.json")
				assert.NoError(t, err)

				return &http.Response{
					StatusCode: http.StatusOK,
					Body:       ioutil.NopCloser(bytes.NewBuffer(respBody)),
					Header:     make(http.Header),
				}, nil
			})
		}
		client, err := NewTrainAPIClientWithTransport(&cfg, logger, transport)
		assert.NoError(t, err)

		response, err := client.ActivePartners(ctx, &request)
		assert.NoError(t, err)
		assert.Len(t, response.PartnersCodes, 2)
		assert.Equal(t, "im", response.PartnersCodes[0])
	})

	t.Run("TariffsRequest", func(t *testing.T) {
		startTime, _ := time.Parse(time.RFC3339, "2021-03-05T05:26:00+00:00")
		endTime, _ := time.Parse(time.RFC3339, "2021-03-05T18:39:00+00:00")
		request := models.TrainTariffsRequest{
			PointFrom:       "s9603175",
			PointTo:         "s9603463",
			Experiment:      true,
			NationalVersion: "ru",
			IncludePriceFee: true,
			Partner:         "im",
			UseWizardSource: true,
			StartTime:       startTime,
			EndTime:         endTime,
		}

		var transport http.RoundTripper = nil
		if isolatedRun {
			transport = client.TransportMock(func(req *http.Request) (*http.Response, error) {
				assert.Equal(t,
					"endTime=2021-03-05T18%3A39%3A00Z&experiment=true&includePriceFee=true&national_version=ru&partner=im&pointFrom=s9603175&pointTo=s9603463&startTime=2021-03-05T05%3A26%3A00Z&useWizardSource=true",
					req.URL.RawQuery)

				respBody, err := ioutil.ReadFile("gotest/train_tariffs_response.json")
				assert.NoError(t, err)

				return &http.Response{
					StatusCode: http.StatusOK,
					Body:       ioutil.NopCloser(bytes.NewBuffer(respBody)),
					Header:     make(http.Header),
				}, nil
			})
		}

		client, err := NewTrainAPIClientWithTransport(&cfg, logger, transport)
		assert.NoError(t, err)

		response, err := client.TrainTariffs(ctx, &request, http.Header{})
		assert.NoError(t, err)
		assert.True(t, response.Querying)
		assert.Len(t, response.Segments, 2)
		assert.Equal(t, "RUB", response.Segments[0].Tariffs.Classes.Sitting.Price.Currency)
	})
}
