package travelapiclient

import (
	"context"
	"testing"

	"github.com/go-resty/resty/v2"
	"github.com/jarcoal/httpmock"
	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/app/backend/internal/lib/travelapiclient/models"
	"a.yandex-team.ru/travel/app/backend/internal/lib/travelapiclient/testdata"
)

func TestGetReviews(t *testing.T) {
	client := buildClient()

	resp, err := testData.ReadFile("testdata/get_hotel_reviews_rsp.json")
	assert.NoError(t, err)

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder(resty.MethodGet, "https://example.com/hotels_portal/v1/get_hotel_reviews",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, string(resp))))

	request := models.GetHotelReviewsReq{}
	res, err := client.GetHotelReviews(context.Background(), &request)
	assert.NoError(t, err)

	assert.Equal(t, &testdata.GetHotelReviewsRsp, res)
}
