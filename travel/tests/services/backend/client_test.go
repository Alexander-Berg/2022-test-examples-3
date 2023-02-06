package backend

import (
	"a.yandex-team.ru/library/go/core/log"
	aviaLogging "a.yandex-team.ru/travel/avia/library/go/logging"
	"a.yandex-team.ru/travel/avia/library/go/services/backend"
	backendPartners "a.yandex-team.ru/travel/avia/library/go/services/backend/partners"
	"encoding/json"
	"github.com/stretchr/testify/assert"
	"net/http"
	"net/http/httptest"
	"testing"
)

var (
	logger, _ = aviaLogging.NewLogger(&aviaLogging.Config{
		Level: log.InfoLevel,
	})
)

func getServerWithResponse(response *backend.WizardPartnersResponse) *httptest.Server {
	return httptest.NewServer(http.HandlerFunc(func(res http.ResponseWriter, req *http.Request) {
		res.WriteHeader(http.StatusOK)
		_ = json.NewEncoder(res).Encode(response)
	}))
}

func TestGetPartners_CorrectBackendAnswer_ShouldReturnsPartners(t *testing.T) {
	backendResponse := &backend.WizardPartnersResponse{
		Status: backend.SuccessResponseStatus,
		Data: [][]*backendPartners.WizardPartner{{{
			ID:              1,
			Code:            "partner_code",
			Enabled:         true,
			EnabledInWizard: map[string]bool{"nv": true},
			IsAviacompany:   false,
			SiteURL:         "http://some-partner-url",
			LogosSvg:        nil,
			Titles:          nil,
		}}},
	}
	server := getServerWithResponse(backendResponse)
	defer server.Close()

	backendClient := backend.NewHTTPClient(server.URL, server.Client())

	partners, err := backendClient.GetPartners()

	assert.NoError(t, err)
	assert.Equal(t, backendResponse.Data[0], partners)
}

func TestGetPartners_ErrorBackendAnswer_ShouldReturnsError(t *testing.T) {
	backendResponse := &backend.WizardPartnersResponse{
		Status: backend.ErrorResponseStatus,
		Reason: "Backend internal error",
	}
	server := getServerWithResponse(backendResponse)
	defer server.Close()

	backendClient := backend.NewHTTPClient(server.URL, server.Client())

	partners, err := backendClient.GetPartners()

	assert.Error(t, err)
	assert.Contains(t, err.Error(), backendResponse.Reason)
	assert.Equal(t, []*backendPartners.WizardPartner(nil), partners)
}
