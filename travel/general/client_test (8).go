package travelersclient

import (
	"context"
	"net/http"
	"testing"
	"time"

	"github.com/jarcoal/httpmock"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/core/log/nop"
	nopmetrics "a.yandex-team.ru/library/go/core/metrics/nop"
	"a.yandex-team.ru/library/go/core/xerrors"
)

func buildClient() *HTTPClient {
	serviceTicketGetter := func(ctx context.Context) (string, error) { return "", nil }
	userTicketGetter := func(ctx context.Context) (string, error) { return "", nil }
	config := DefaultTravelersConfig
	config.BaseURL = "https://example.com/v1"
	return NewHTTPClient(&nop.Logger{}, &config, userTicketGetter, serviceTicketGetter, false, nopmetrics.Registry{})
}

var brokenServiceTicketErr = xerrors.NewSentinel("broken service ticket")
var brokenUserTicketErr = xerrors.NewSentinel("broken service ticket")

func buildClientWithBrokenTickets() *HTTPClient {
	serviceTicketGetter := func(ctx context.Context) (string, error) { return "", brokenServiceTicketErr }
	userTicketGetter := func(ctx context.Context) (string, error) { return "", brokenUserTicketErr }
	config := DefaultTravelersConfig
	config.BaseURL = "https://example.com/v1"
	return NewHTTPClient(&nop.Logger{}, &config, userTicketGetter, serviceTicketGetter, false, nopmetrics.Registry{})
}

func TestListDocumentTypesSuccess(t *testing.T) {
	client := buildClient()

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("GET", "https://example.com/v1/document_types",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, `
{
	"passport": {
		"unused": ["unused_field"],
		"required": ["required_field"],
		"re_validations": {
			"number": "^\\d{10}$"
		}
	}
}`)))

	actual, err := client.ListDocumentTypes(context.Background())
	require.NoError(t, err)
	expected := &DocumentTypes{
		"passport": {
			Unused:   []string{"unused_field"},
			Required: []string{"required_field"},
			ReValidations: map[string]string{
				"number": "^\\d{10}$",
			},
		},
	}
	require.Equal(t, expected, actual)
}

func TestListDocumentTypes404(t *testing.T) {
	client := buildClient()

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("GET", "https://example.com/v1/document_types",
		httpmock.NewStringResponder(404, `<html><title>404: Document not found</title><body>404: Document not found</body></html>`))
	var expected StatusError
	_, err := client.ListDocumentTypes(context.Background())
	if assert.ErrorAs(t, err, &expected) {
		require.Equal(t, 404, expected.Status)
	}
}

func TestGetTraveler(t *testing.T) {
	client := buildClient()

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("GET", "https://example.com/v1/travelers/42",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, `
{
	"email": "aaa@example.com",
	"phone": "+79876543210",
	"phone_additional": "+79012345678",
	"agree": true,
	"created_at": "2021-10-19 14:00:00",
	"updated_at": "2021-10-27 18:33:39"
}`)))

	actual, err := client.GetTraveler(context.Background(), "42")
	require.NoError(t, err)
	expected := &Traveler{
		Email:           "aaa@example.com",
		Phone:           "+79876543210",
		PhoneAdditional: "+79012345678",
		Agree:           true,
		CreatedAt:       &Timestamp{time.Date(2021, 10, 19, 14, 0, 0, 0, moscowLocation)},
		UpdatedAt:       &Timestamp{time.Date(2021, 10, 27, 18, 33, 39, 0, moscowLocation)},
	}
	require.Equal(t, expected, actual)
}

func TestCreateOrUpdateTraveler(t *testing.T) {
	client := buildClient()

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("POST", "https://example.com/v1/travelers/42",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, `
{
	"email": "aaa@example.com",
	"phone": "+79876543210",
	"phone_additional": "+79012345678",
	"agree": true,
	"created_at": "2021-10-19 14:00:00",
	"updated_at": "2021-10-27 18:33:39"
}`)))

	actual, err := client.CreateOrUpdateTraveler(context.Background(), "42", &EditableTraveler{
		Email:           "aaa@example.com",
		Phone:           "+79876543210",
		PhoneAdditional: "+79012345678",
		Agree:           true,
	})
	require.NoError(t, err)
	expected := &Traveler{
		Email:           "aaa@example.com",
		Phone:           "+79876543210",
		PhoneAdditional: "+79012345678",
		Agree:           true,
		CreatedAt:       &Timestamp{time.Date(2021, 10, 19, 14, 0, 0, 0, moscowLocation)},
		UpdatedAt:       &Timestamp{time.Date(2021, 10, 27, 18, 33, 39, 0, moscowLocation)},
	}
	require.Equal(t, expected, actual)
}

func TestListPassengers(t *testing.T) {
	client := buildClient()

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("GET", "https://example.com/v1/travelers/42/passengers",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, `[{
	"gender": "female",
	"phone": "+71111111111",
	"id": "99ee495f-5f08-41c1-8dcd-e9d74372e1f7",
	"created_at": "2021-10-21 16:46:39",
	"updated_at": "2021-10-21 19:41:51",
	"phone_additional": null,
	"email": "ada@adaic.com",
	"itn": "424242424242",
	"title": "Лавлейс Ада",
	"birth_date": "1852-11-27",
	"train_notifications_enabled": false
}]`)))

	res, err := client.ListPassengers(context.Background(), "42", false, false)
	require.NoError(t, err)
	require.Equal(t, []Passenger{expectedPassenger()}, res)
}

func TestListDocuments(t *testing.T) {
	client := buildClient()

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("GET", "https://example.com/v1/travelers/42/passengers/1/documents",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, `
[
    {
        "type": "other",
        "first_name_en": "Ada",
        "id": "1b8a067d-f990-4f65-ad47-647aab7b45b5",
        "number": "123456789",
        "created_at": "2021-10-21 16:46:39",
        "middle_name": "Августа",
        "middle_name_en": "Augusta",
        "expiration_date": null,
        "updated_at": "2021-10-21 16:47:48",
        "issue_date": null,
        "citizenship": 102,
        "last_name_en": "Lovelace",
        "last_name": "Лавлейс",
        "title": null,
        "passenger_id": "99ee495f-5f08-41c1-8dcd-e9d74372e1f7",
        "first_name": "Ада"
    }
]`)))
	res, err := client.ListDocuments(context.Background(), "42", "1")
	require.NoError(t, err)
	require.Equal(t, []Document{expectedDocument()}, res)
}

func TestGetDocument(t *testing.T) {
	client := buildClient()

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("GET", "https://example.com/v1/travelers/42/passengers/1/documents/2",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, `{
    "type": "other",
    "first_name_en": "Ada",
    "id": "1b8a067d-f990-4f65-ad47-647aab7b45b5",
    "number": "123456789",
    "created_at": "2021-10-21 16:46:39",
    "middle_name": "Августа",
    "middle_name_en": "Augusta",
    "expiration_date": null,
    "updated_at": "2021-10-21 16:47:48",
    "issue_date": null,
    "citizenship": 102,
    "last_name_en": "Lovelace",
    "last_name": "Лавлейс",
    "title": null,
    "passenger_id": "99ee495f-5f08-41c1-8dcd-e9d74372e1f7",
    "first_name": "Ада"
}`)))
	res, err := client.GetDocument(context.Background(), "42", "1", "2")
	require.NoError(t, err)
	require.Equal(t, expectedDocument(), *res)
}

func TestCreateDocument(t *testing.T) {
	client := buildClient()

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("POST", "https://example.com/v1/travelers/42/passengers/1/documents",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, `{
    "type": "other",
    "first_name_en": "Ada",
    "id": "1b8a067d-f990-4f65-ad47-647aab7b45b5",
    "number": "123456789",
    "created_at": "2021-10-21 16:46:39",
    "middle_name": "Августа",
    "middle_name_en": "Augusta",
    "expiration_date": null,
    "updated_at": "2021-10-21 16:47:48",
    "issue_date": null,
    "citizenship": 102,
    "last_name_en": "Lovelace",
    "last_name": "Лавлейс",
    "title": null,
    "passenger_id": "99ee495f-5f08-41c1-8dcd-e9d74372e1f7",
    "first_name": "Ада"
}`)))

	res, err := client.CreateDocument(context.Background(), "42", "1", &CreateOrUpdateDocumentRequest{})
	require.NoError(t, err)
	require.Equal(t, expectedDocument(), *res)
}

func TestCreateDocumentValidationFailed(t *testing.T) {
	client := buildClient()

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("POST", "https://example.com/v1/travelers/42/passengers/1/documents",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(400, `{"_schema": ["Wrong field"]}`)))

	_, err := client.CreateDocument(context.Background(), "42", "1", &CreateOrUpdateDocumentRequest{})
	var expected ValidationError
	if assert.ErrorAs(t, err, &expected) {
		require.Equal(t, "Wrong field", expected.FieldErrors["_schema"])
	}
}

func TestErrorInServiceTicket(t *testing.T) {
	client := buildClientWithBrokenTickets()
	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	_, err := client.ListDocumentTypes(context.Background())
	assert.ErrorIs(t, err, NoServiceTicketError)
	assert.ErrorIs(t, err, brokenServiceTicketErr)
}

func TestUpdateDocument(t *testing.T) {
	client := buildClient()

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("PUT", "https://example.com/v1/travelers/42/passengers/1/documents/2",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, `{
    "type": "other",
    "first_name_en": "Ada",
    "id": "1b8a067d-f990-4f65-ad47-647aab7b45b5",
    "number": "123456789",
    "created_at": "2021-10-21 16:46:39",
    "middle_name": "Августа",
    "middle_name_en": "Augusta",
    "expiration_date": null,
    "updated_at": "2021-10-21 16:47:48",
    "issue_date": null,
    "citizenship": 102,
    "last_name_en": "Lovelace",
    "last_name": "Лавлейс",
    "title": null,
    "passenger_id": "99ee495f-5f08-41c1-8dcd-e9d74372e1f7",
    "first_name": "Ада"
}`)))

	res, err := client.UpdateDocument(context.Background(), "42", "1", "2", &CreateOrUpdateDocumentRequest{})
	require.NoError(t, err)
	require.Equal(t, expectedDocument(), *res)
}

func TestListBonusCards(t *testing.T) {
	client := buildClient()

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("GET", "https://example.com/v1/travelers/42/passengers/1/bonus-cards",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, `
[
	{
		"id": "7",
		"passenger_id": "1",
		"title": "РЖД Бонус",
		"type": "rzd_bonus",
		"number": "9001234567890",
		"created_at": "2021-10-24 15:00:00",
		"updated_at": "2021-10-24 15:18:56"
}
]`)))

	actual, err := client.ListBonusCards(context.Background(), "42", "1")
	require.NoError(t, err)
	expected := []BonusCard{
		{
			ID:          "7",
			PassengerID: "1",
			Type:        "rzd_bonus",
			CreatedAt:   &Timestamp{time.Date(2021, 10, 24, 15, 0, 0, 0, moscowLocation)},
			UpdatedAt:   &Timestamp{time.Date(2021, 10, 24, 15, 18, 56, 0, moscowLocation)},
			Fields: map[string]interface{}{
				"title":  "РЖД Бонус",
				"number": "9001234567890",
			},
		},
	}
	require.Equal(t, expected, actual)
}

func TestGetBonusCard(t *testing.T) {
	client := buildClient()

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("GET", "https://example.com/v1/travelers/42/passengers/1/bonus-cards/7",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, `
{
	"id": "7",
	"passenger_id": "1",
	"title": "РЖД Бонус",
	"type": "rzd_bonus",
	"number": "9001234567890",
	"created_at": "2021-10-24 15:00:00",
	"updated_at": "2021-10-24 15:18:56"
}`)))

	actual, err := client.GetBonusCard(context.Background(), "42", "1", "7")
	require.NoError(t, err)
	expected := &BonusCard{
		ID:          "7",
		PassengerID: "1",
		Type:        "rzd_bonus",
		CreatedAt:   &Timestamp{time.Date(2021, 10, 24, 15, 0, 0, 0, moscowLocation)},
		UpdatedAt:   &Timestamp{time.Date(2021, 10, 24, 15, 18, 56, 0, moscowLocation)},
		Fields: map[string]interface{}{
			"title":  "РЖД Бонус",
			"number": "9001234567890",
		},
	}
	require.Equal(t, expected, actual)
}

func TestCreateBonusCard(t *testing.T) {
	client := buildClient()

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("POST", "https://example.com/v1/travelers/42/passengers/1/bonus-cards",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, `
{
	"id": "7",
	"passenger_id": "1",
	"title": "РЖД Бонус",
	"type": "rzd_bonus",
	"number": "9001234567890",
	"created_at": "2021-10-24 15:00:00",
	"updated_at": "2021-10-24 15:18:56"
}`)))

	actual, err := client.CreateBonusCard(context.Background(), "42", "1", &EditableBonusCard{
		Type: "rzd_bonus",
		Fields: map[string]interface{}{
			"title":  "РЖД Бонус",
			"number": "9001234567890",
		},
	})
	require.NoError(t, err)
	expected := &BonusCard{
		ID:          "7",
		PassengerID: "1",
		Type:        "rzd_bonus",
		CreatedAt:   &Timestamp{time.Date(2021, 10, 24, 15, 0, 0, 0, moscowLocation)},
		UpdatedAt:   &Timestamp{time.Date(2021, 10, 24, 15, 18, 56, 0, moscowLocation)},
		Fields: map[string]interface{}{
			"title":  "РЖД Бонус",
			"number": "9001234567890",
		},
	}
	require.Equal(t, expected, actual)
}

func TestUpdateBonusCard(t *testing.T) {
	client := buildClient()

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("PUT", "https://example.com/v1/travelers/42/passengers/1/bonus-cards/7",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, `
{
	"id": "7",
	"passenger_id": "1",
	"title": "РЖД Бонус",
	"type": "rzd_bonus",
	"number": "9001234567890",
	"created_at": "2021-10-24 15:00:00",
	"updated_at": "2021-10-24 15:18:56"
}`)))

	actual, err := client.UpdateBonusCard(context.Background(), "42", "1", "7", &EditableBonusCard{
		Type: "rzd_bonus",
		Fields: map[string]interface{}{
			"title":  "РЖД Бонус",
			"number": "9001234567890",
		},
	})
	require.NoError(t, err)
	expected := &BonusCard{
		ID:          "7",
		PassengerID: "1",
		Type:        "rzd_bonus",
		CreatedAt:   &Timestamp{time.Date(2021, 10, 24, 15, 0, 0, 0, moscowLocation)},
		UpdatedAt:   &Timestamp{time.Date(2021, 10, 24, 15, 18, 56, 0, moscowLocation)},
		Fields: map[string]interface{}{
			"title":  "РЖД Бонус",
			"number": "9001234567890",
		},
	}
	require.Equal(t, expected, actual)
}

func TestDeleteBonusCard(t *testing.T) {
	client := buildClient()

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("DELETE", "https://example.com/v1/travelers/42/passengers/1/bonus-cards/7",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, "{}")))

	err := client.DeleteBonusCard(context.Background(), "42", "1", "7")
	require.NoError(t, err)
}

func mockResponseFromJSONString(status int, response string) *http.Response {
	resp := httpmock.NewStringResponse(status, response)
	resp.Header[http.CanonicalHeaderKey("Content-Type")] = []string{"application/json"}
	return resp
}

func expectedPassenger() Passenger {
	return Passenger{
		ID:        "99ee495f-5f08-41c1-8dcd-e9d74372e1f7",
		CreatedAt: &Timestamp{time.Date(2021, 10, 21, 16, 46, 39, 0, moscowLocation)},
		UpdatedAt: &Timestamp{time.Date(2021, 10, 21, 19, 41, 51, 0, moscowLocation)},
		Fields: map[string]interface{}{
			"title":                       "Лавлейс Ада",
			"gender":                      "female",
			"birth_date":                  "1852-11-27",
			"itn":                         "424242424242",
			"phone":                       "+71111111111",
			"phone_additional":            nil,
			"email":                       "ada@adaic.com",
			"train_notifications_enabled": false,
		},
	}
}

func expectedDocument() Document {
	return Document{
		ID:          "1b8a067d-f990-4f65-ad47-647aab7b45b5",
		PassengerID: "99ee495f-5f08-41c1-8dcd-e9d74372e1f7",
		Type:        "other",
		CreatedAt:   &Timestamp{time.Date(2021, 10, 21, 16, 46, 39, 0, moscowLocation)},
		UpdatedAt:   &Timestamp{time.Date(2021, 10, 21, 16, 47, 48, 0, moscowLocation)},
		Fields: map[string]interface{}{
			"title":           nil,
			"number":          "123456789",
			"first_name":      "Ада",
			"middle_name":     "Августа",
			"last_name":       "Лавлейс",
			"first_name_en":   "Ada",
			"middle_name_en":  "Augusta",
			"last_name_en":    "Lovelace",
			"issue_date":      nil,
			"expiration_date": nil,
			"citizenship":     float64(102),
		},
	}
}
