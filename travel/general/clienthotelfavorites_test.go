package travelapiclient

import (
	"context"
	"testing"

	"github.com/jarcoal/httpmock"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/travel/app/backend/internal/lib/travelapiclient/models"
	"a.yandex-team.ru/travel/app/backend/internal/lib/travelapiclient/testdata"
)

func TestGetFavorites(t *testing.T) {
	client := buildClient()
	resp, err := testData.ReadFile("testdata/get_hotel_favorites_resp.json")
	require.NoError(t, err)

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("GET", "https://example.com/hotels_portal/v1/get_favorite_hotels",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, string(resp))))
	response, err := client.GetHotelFavorites(context.Background(), &models.GetHotelFavoritesReq{})
	require.NoError(t, err)

	assert.Equal(t, testdata.GetHotelFavoritesRsp, *response)
}

func TestGetFavorites_BuildURLParams(t *testing.T) {
	request := models.GetHotelFavoritesReq{
		PagingParams: &models.PagingParams{
			Offset: 0,
			Limit:  100,
		},
	}

	url := request.BuildURLParams()

	assert.Equal(t, "0", url.Get("offset"))
	assert.Equal(t, "100", url.Get("limit"))
}

func TestAddFavorites(t *testing.T) {
	client := buildClient()

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("POST", "https://example.com/hotels_portal/v1/add_favorite_hotel",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, "{}")))

	permalink := uint64(1686701236)
	err := client.AddHotelFavorites(context.Background(), &models.AddHotelFavoritesReq{
		Permalink: &permalink,
	})
	require.NoError(t, err)
}

func TestAddFavorites_BuildURLParams(t *testing.T) {
	permalink := uint64(1686701236)
	request := models.AddHotelFavoritesReq{
		Permalink: &permalink,
	}

	url := request.BuildURLParams()

	assert.Equal(t, "1686701236", url.Get("permalink"))
}

func TestRemoveFavorites(t *testing.T) {
	client := buildClient()

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("POST", "https://example.com/hotels_portal/v1/remove_favorite_hotels",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, "{}")))

	permalink := uint64(1686701236)
	err := client.RemoveHotelFavorites(context.Background(), &models.RemoveHotelFavoritesReq{
		Permalink: &permalink,
	})
	require.NoError(t, err)
}

func TestRemoveFavorites_BuildURLParams(t *testing.T) {
	permalink := uint64(1686701236)
	request := models.RemoveHotelFavoritesReq{
		Permalink: &permalink,
	}

	url := request.BuildURLParams()

	assert.Equal(t, "1686701236", url.Get("permalink"))
	assert.Equal(t, "", url.Get("category_id"))
}

func TestRemoveFavorites_BuildURLParams_WithCategory(t *testing.T) {
	categoryID := "categoryID"
	request := models.RemoveHotelFavoritesReq{
		CategoryID: &categoryID,
	}

	url := request.BuildURLParams()

	assert.Equal(t, "", url.Get("permalink"))
	assert.Equal(t, categoryID, url.Get("category_id"))
}
