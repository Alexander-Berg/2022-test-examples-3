package travelapiclient

import (
	"context"
	"regexp"
	"testing"
	"time"

	"github.com/jarcoal/httpmock"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/ptr"
	"a.yandex-team.ru/travel/app/backend/internal/lib/travelapiclient/models"
	"a.yandex-team.ru/travel/app/backend/internal/lib/travelapiclient/testdata"
)

const exampleResponse = `{
  "groups": [
    {
      "name": "Отели к вашему путешествию",
      "items": [
        {
          "id": "personalized-order-146-ru",
          "name": "Симферополь",
          "description": "Республика Крым, Россия",
          "redirect_params": {
            "type": "cross_sale",
            "geo_id": 146,
            "permalink": null,
            "hotel_slug": null,
            "selected_sort_id": null,
            "sort_origin": null,
            "bbox": null,
            "offer_search_params": {
              "checkin_date": "2021-12-12",
              "checkout_date": "2021-12-13",
              "adults": 1,
              "children_ages": []
            }
          }
        }
      ]
    },
    {
      "name": "Вы искали",
      "items": [
        {
          "id": "personalized-search-22-ru",
          "name": "Калининград",
          "description": "Калининградская область, Россия",
          "redirect_params": {
            "type": "history",
            "geo_id": 22,
            "permalink": null,
            "hotel_slug": null,
            "selected_sort_id": null,
            "sort_origin": null,
            "bbox": null,
            "offer_search_params": {
              "checkin_date": "2021-12-23",
              "checkout_date": "2021-12-24",
              "adults": 1,
              "children_ages": []
            }
          }
        }
      ]
    },
	{
      "name": "Отели рядом",
      "items": [
        {
          "id": "hotels-nearby-ru",
          "name": "Отели рядом",
          "description": null,
          "redirect_params": {
            "type": "hotels_nearby",
            "geo_id": null,
            "permalink": null,
            "hotel_slug": null,
            "selected_sort_id": "near-first",
            "sort_origin": null,
            "bbox": null,
            "offer_search_params": null
          }
        }
      ]
    },
    {
      "name": "Регион",
      "items": [
        {
          "id": "region-213-ru",
          "name": "Москва",
          "description": "Россия",
          "redirect_params": {
            "type": "region",
            "geo_id": 213,
            "permalink": null,
            "hotel_slug": null,
            "selected_sort_id": null,
            "sort_origin": null,
            "bbox": null,
            "offer_search_params": null
          }
        },
        {
          "id": "region-2-ru",
          "name": "Санкт-Петербург",
          "description": "Россия, Санкт-Петербург и Ленинградская область",
          "redirect_params": {
            "type": "region",
            "geo_id": 2,
            "permalink": null,
            "hotel_slug": null,
            "selected_sort_id": null,
            "sort_origin": null,
            "bbox": null,
            "offer_search_params": null
          }
        }
      ]
    },
	{
      "name": "Гостиница",
      "items": [
        {
          "id": "hotel-19703708637-ru",
          "name": "Сочи Парк Отель",
          "description": "Гостиница · Россия, Краснодарский край, Сочи, Континентальный проспект, 6",
          "redirect_params": {
            "type": "hotel",
            "geo_id": null,
            "permalink": "19703708637",
            "hotel_slug": "sochi/sochi-park-otel",
            "selected_sort_id": null,
            "sort_origin": null,
            "bbox": null,
            "offer_search_params": null
          }
        },
        {
          "id": "hotel-1046552916-ru",
          "name": "ФГБУ Объединенный санаторий Сочи Управления делами Президента Российской Федерации",
          "description": "Санаторий · Россия, Краснодарский край, Сочи, Виноградная улица, 27",
          "redirect_params": {
            "type": "hotel",
            "geo_id": null,
            "permalink": "1046552916",
            "hotel_slug": "sochi/fgbu-ob-edinennyi-sanatorii-sochi-upravleniia-delami-prezidenta-rossiiskoi-federatsii",
            "selected_sort_id": null,
            "sort_origin": null,
            "bbox": null,
            "offer_search_params": null
          }
        }
      ]
    }
  ]
}`

var ps = func(str string) *string {
	return &str
}

func TestNoQuery(t *testing.T) {
	client := buildClient()

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("GET", "https://example.com/hotels_portal/v1/suggest",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, exampleResponse)))

	res, err := client.Suggest(context.Background(), "", 5, "ru", "ru", "", 0)
	require.NoError(t, err)
	expected := &models.SuggestResponse{Groups: []models.SuggestGroup{
		{
			Name: "Отели к вашему путешествию",
			Items: []models.SuggestItem{
				{
					ID:          "personalized-order-146-ru",
					Name:        "Симферополь",
					Description: "Республика Крым, Россия",
					RedirectParams: &models.RedirectParam{
						BBox:      "",
						GeoID:     intP(146),
						HotelSlug: "",
						OfferSearchParams: &models.OfferSearchParams{
							Adults:       1,
							CheckinDate:  models.Date{Time: time.Date(2021, 12, 12, 0, 0, 0, 0, time.UTC)},
							CheckoutDate: models.Date{Time: time.Date(2021, 12, 13, 0, 0, 0, 0, time.UTC)},
							ChildrenAges: []int{},
						},
						Permalink:      nil,
						SelectedSortID: "",
						SortOrigin:     "",
						Type:           models.SuggestTypeCrossSale,
					},
				},
			},
		},
		{
			Name: "Вы искали",
			Items: []models.SuggestItem{
				{
					ID:          "personalized-search-22-ru",
					Name:        "Калининград",
					Description: "Калининградская область, Россия",
					RedirectParams: &models.RedirectParam{
						BBox:      "",
						GeoID:     intP(22),
						HotelSlug: "",
						OfferSearchParams: &models.OfferSearchParams{
							Adults:       1,
							CheckinDate:  models.Date{Time: time.Date(2021, 12, 23, 0, 0, 0, 0, time.UTC)},
							CheckoutDate: models.Date{Time: time.Date(2021, 12, 24, 0, 0, 0, 0, time.UTC)},
							ChildrenAges: []int{},
						},
						Permalink:      nil,
						SelectedSortID: "",
						SortOrigin:     "",
						Type:           models.SuggestTypeHistory,
					},
				},
			},
		},
		{
			Name: "Отели рядом",
			Items: []models.SuggestItem{
				{
					ID:          "hotels-nearby-ru",
					Name:        "Отели рядом",
					Description: "",
					RedirectParams: &models.RedirectParam{
						BBox:              "",
						GeoID:             nil,
						HotelSlug:         "",
						OfferSearchParams: nil,
						Permalink:         nil,
						SelectedSortID:    "near-first",
						SortOrigin:        "",
						Type:              models.SuggestTypeHotelsNearby,
					},
				},
			},
		},
		{
			Name: "Регион",
			Items: []models.SuggestItem{
				{
					ID:          "region-213-ru",
					Name:        "Москва",
					Description: "Россия",
					RedirectParams: &models.RedirectParam{
						BBox:              "",
						GeoID:             intP(213),
						HotelSlug:         "",
						OfferSearchParams: nil,
						Permalink:         nil,
						SelectedSortID:    "",
						SortOrigin:        "",
						Type:              models.SuggestTypeRegion,
					},
				},
				{
					ID:          "region-2-ru",
					Name:        "Санкт-Петербург",
					Description: "Россия, Санкт-Петербург и Ленинградская область",
					RedirectParams: &models.RedirectParam{
						BBox:              "",
						GeoID:             intP(2),
						HotelSlug:         "",
						OfferSearchParams: nil,
						Permalink:         nil,
						SelectedSortID:    "",
						SortOrigin:        "",
						Type:              models.SuggestTypeRegion,
					},
				},
			},
		},
		{
			Name: "Гостиница",
			Items: []models.SuggestItem{
				{
					ID:          "hotel-19703708637-ru",
					Name:        "Сочи Парк Отель",
					Description: "Гостиница · Россия, Краснодарский край, Сочи, Континентальный проспект, 6",
					RedirectParams: &models.RedirectParam{
						BBox:              "",
						GeoID:             nil,
						HotelSlug:         "sochi/sochi-park-otel",
						OfferSearchParams: nil,
						Permalink:         uintP(19703708637),
						SelectedSortID:    "",
						SortOrigin:        "",
						Type:              models.SuggestTypeHotel,
					},
				},
				{
					ID:          "hotel-1046552916-ru",
					Name:        "ФГБУ Объединенный санаторий Сочи Управления делами Президента Российской Федерации",
					Description: "Санаторий · Россия, Краснодарский край, Сочи, Виноградная улица, 27",
					RedirectParams: &models.RedirectParam{
						BBox:              "",
						GeoID:             nil,
						HotelSlug:         "sochi/fgbu-ob-edinennyi-sanatorii-sochi-upravleniia-delami-prezidenta-rossiiskoi-federatsii",
						OfferSearchParams: nil,
						Permalink:         uintP(1046552916),
						SelectedSortID:    "",
						SortOrigin:        "",
						Type:              models.SuggestTypeHotel,
					},
				},
			},
		},
	}}

	assert.Equal(t, expected, res)
}

func TestSearch(t *testing.T) {
	client := buildClient()

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	b, err := testData.ReadFile("testdata/search_resp.json")
	require.NoError(t, err)

	resp := string(b)
	r := regexp.MustCompile(`^[\w*:/.?=&-]+(search_hotels)+[\w*:/.?=&-]+`)
	response := httpmock.ResponderFromResponse(mockResponseFromJSONString(200, resp))
	httpmock.RegisterRegexpResponder("GET", r, response)

	geoID := 213
	request := models.SearchHotelsRequest{
		QueryData: models.HotelsSearchMainParams{
			PollingData: &models.PollingData{},
			SearchParams: models.SearchParams{
				CheckinDate:  "2022-03-15",
				CheckoutDate: "2022-03-17",
				Adults:       2,
			},
			GeoID:                  &geoID,
			HotelSearchStartReason: models.StartSearchReasonMount,
			TotalHotelsLimit:       50,
			PageHotelCount:         2,
		},
	}
	res, err := client.SearchHotels(context.Background(), request)
	require.NoError(t, err)

	expected := &models.SearchHotelsResponse{
		SearchContext: "9d9839c1bd8c6cd5dab9eb45d78179a-0-newsearch-0~Ch85ZDk4MzljMWJkOGM2Y2Q1ZGFiOWViNDVkNzgxNzlhEAAYACCSkPKlnbK15L0BKAgyADgB",
		Bbox: models.BoundingBoxRsp{
			models.Coordinates{Latitude: 55.60479790806535, Longitude: 37.40455300000002},
			models.Coordinates{Latitude: 55.85604490037295, Longitude: 37.78211399999998},
		},
		NavigationTokens: models.NavigationTokens{NextPage: "50"},
		OfferSearchProgress: models.OfferSearchProgress{
			Finished:         true,
			PartnersTotal:    3,
			PartnersComplete: 3,
		},
		NextPollingRequestDelayMs: 1000,
		FoundHotelsCount:          6608,
		Hotels: []models.HotelWithOffers{
			{Hotel: models.Hotel{
				HotelBase: models.HotelBase{
					Permalink: "107375353234",
					Slug:      "moscow/metamoskva",
					Name:      "МетаМосква",
					Coordinates: models.Coordinates{
						Latitude:  55.74923,
						Longitude: 37.645222,
					},
					Address:       "Россия, Москва, Яузская улица, 5",
					Stars:         ptr.Int(3),
					Rating:        4.4,
					IsYandexHotel: true,
				},
				Rubric:          models.Rubric{ID: "184106414", Name: "Гостиница"},
				TotalImageCount: 109,
				Features: []models.Feature{
					{
						ID:   "wi_fi",
						Name: "Wi-Fi",
					},
					{
						ID:   "air_conditioning",
						Name: "Кондиционер в номере",
					},
				},
				GeoInfo:              &models.GeoInfo{Name: "1,5 км до центра", Icon: "city-center"},
				TotalTextReviewCount: 187,
				IsFavorite:           false,
				Images: []models.Image{
					{
						ID:          "urn:yandex:sprav:photo:DPwyO6sjGf4yFErKO_xuABhh4OiK0_vz",
						URLTemplate: "https://avatars.mds.yandex.net/get-altay/5236021/2a0000017b72e177af1b6b38199b35175189/%s",
						Sizes: []models.Size{
							{Identifier: "XXXS", Width: 0x32, Height: 0x21},
							{Identifier: "XXS", Width: 0x4b, Height: 0x32},
						},
						Tags: []string{"Interior"},
					},
					{
						ID:          "urn:yandex:sprav:photo:lCPxUw0sSpkWS65BLjzQdQ2RRNfxmGJ_t",
						URLTemplate: "https://avatars.mds.yandex.net/get-altay/1870294/2a0000016fa57044a339925a65f0ed889d37/%s",
						Sizes: []models.Size{
							{Identifier: "XXXS", Width: 0x32, Height: 0x21},
							{Identifier: "XXS", Width: 0x4b, Height: 0x32},
						},
						Tags: []string{"Food"}},
				},
				HotelURL: "/hotels/moscow/metamoskva/?adults=2&checkinDate=2022-03-15&checkoutDate=2022-03-17&childrenAges=&searchPagePollingId=9d9839c1bd8c6cd5dab9eb45d78179a-0-newsearch&seed=app-search"},
				PollingFinished: true,
				OffersInfo: []models.OfferInfo{
					{
						Price:          models.Price{Value: 8553, Currency: "RUB"},
						YandexPlusInfo: &models.YandexPlus{Points: 855},
						Badges: []models.HotelBadge{
							{
								ID:   "ads",
								Text: "Реклама",
							},
							{
								ID:   "mir_cashback",
								Text: "Возврат 20%",
							},
						},
					},
				},
				Badges: []models.HotelBadge{
					{
						ID:   "ads",
						Text: "Реклама",
					},
					{
						ID:   "mir_cashback",
						Text: "Возврат 20%",
					},
				},
			},
			{Hotel: models.Hotel{
				HotelBase: models.HotelBase{
					Permalink:     "1686701236",
					Slug:          "moscow/beta-izmailovo",
					Name:          "Бета Измайлово",
					Coordinates:   models.Coordinates{Latitude: 55.789562, Longitude: 37.74772},
					Address:       "Россия, Москва, Измайловское шоссе, 71, корп. 2Б",
					Stars:         ptr.Int(3),
					Rating:        4.7,
					IsYandexHotel: false,
				},
				Rubric:          models.Rubric{ID: "184106414", Name: "Гостиница"},
				TotalImageCount: 0x128,
				Features: []models.Feature{
					{
						ID:   "wi_fi",
						Name: "Wi-Fi",
					},
					{
						ID:   "car_park",
						Name: "Парковка",
					},
				},
				GeoInfo:              &models.GeoInfo{Name: "8,8 км до центра", Icon: "city-center"},
				DistanceText:         "asdasd",
				TotalTextReviewCount: 3369,
				IsFavorite:           false,
				Images: []models.Image{
					{
						ID:          "urn:yandex:sprav:photo:69585875",
						URLTemplate: "https://avatars.mds.yandex.net/get-altay/879259/2a0000016156f0d392e7d8da7592698bd77d/%s",
						Sizes: []models.Size{
							{Identifier: "XXXS", Width: 0x32, Height: 0x21},
							{Identifier: "XXS", Width: 0x4b, Height: 0x32},
						},
						Tags: []string{"Exterior"},
					},
					{
						ID:          "urn:yandex:sprav:photo:69585867",
						URLTemplate: "https://avatars.mds.yandex.net/get-altay/941278/2a0000016156f0a59a6509c7e500386f7f0b/%s",
						Sizes: []models.Size{
							{Identifier: "XXXS", Width: 0x32, Height: 0x21},
							{Identifier: "XXS", Width: 0x4b, Height: 0x32},
						},
						Tags: []string{"Exterior"},
					},
				},
				HotelURL: "/hotels/moscow/beta-izmailovo/?adults=2&checkinDate=2022-03-15&checkoutDate=2022-03-17&childrenAges=&searchPagePollingId=9d9839c1bd8c6cd5dab9eb45d78179a-0-newsearch&seed=app-search"},
				PollingFinished: true,
				OffersInfo: []models.OfferInfo{
					{
						Price: models.Price{Value: 1151, Currency: "RUB"},
						PansionInfo: &models.PansionInfo{
							ID:   "PT_BB",
							Name: "Завтрак включён",
						},
						Badges: []models.HotelBadge{},
					},
				},
				Badges: []models.HotelBadge{},
			},
		},
		FilterInfo: models.FilterInfo{
			QuickFilters: []models.QuickFilters{
				{
					ID:       "yandex-plus-quick-desktop",
					Name:     "Кешбэк Плюса",
					Hint:     "",
					Effect:   "yandex-plus",
					Enabled:  true,
					AtomsOn:  []string{"FAKE-ID-yandex-offers:1"},
					AtomsOff: []string{},
				},
				{
					ID:       "mir-offers-quick",
					Name:     "Возврат 20%",
					Hint:     "Бронируйте сейчас от 2 ночей на даты с 01.10.2021 по 24.12.2021 и получите возврат 20% по карте «Мир»",
					Effect:   "mir-cashback",
					Enabled:  true,
					AtomsOn:  []string{"FAKE-ID-mir-offers:1"},
					AtomsOff: []string{},
				},
			},
			DetailedFilter: []models.FilterAggregation{
				{Type: "PRICE", DetailedFilter: nil},
				{Type: "GROUP", DetailedFilter: &models.Filter{
					ID:   "pansion",
					Name: "Питание",
					Type: "MULTI",
					Items: []models.DetailedFilter{
						{
							ID:      "breakfast_included",
							Name:    "Завтрак",
							Hint:    "639",
							Effect:  "",
							Enabled: true,
							Atoms: []string{
								"hotel_pansion_with_offerdata:hotel_pansion_breakfast_included",
							},
						},
						{ID: "breakfast_dinner_included",
							Name:    "Полупансион",
							Hint:    "107",
							Effect:  "",
							Enabled: true,
							Atoms: []string{
								"hotel_pansion_with_offerdata:hotel_pansion_breakfast_dinner_included",
							},
						},
						{
							ID:      "breakfast_lunch_dinner_included",
							Name:    "Завтрак, обед и ужин",
							Hint:    "59",
							Effect:  "",
							Enabled: true,
							Atoms: []string{
								"hotel_pansion_with_offerdata:hotel_pansion_breakfast_lunch_dinner_included",
							},
						},
						{
							ID:      "all_inclusive",
							Name:    "Всё включено",
							Hint:    "15",
							Effect:  "",
							Enabled: true,
							Atoms: []string{
								"hotel_pansion_with_offerdata:hotel_pansion_all_inclusive",
							},
						},
						{
							ID:      "no_pansion_included",
							Name:    "Без питания",
							Hint:    "4586",
							Effect:  "",
							Enabled: true,
							Atoms: []string{
								"hotel_pansion_with_offerdata:hotel_pansion_no_pansion_included",
							},
						},
					},
				}},
				{Type: "GROUP", DetailedFilter: &models.Filter{
					ID:   "stars",
					Name: "Звёздность",
					Type: "MULTI",
					Items: []models.DetailedFilter{
						{
							ID:      "stars-no",
							Name:    "Без звёзд",
							Hint:    "5207",
							Effect:  "",
							Enabled: true,
							Atoms: []string{
								"star:unrated",
							},
						},
						{
							ID:      "stars-1",
							Name:    "1*",
							Hint:    "137",
							Effect:  "",
							Enabled: true,
							Atoms: []string{
								"star:one",
							},
						},
						{
							ID:      "stars-2",
							Name:    "2*",
							Hint:    "214",
							Effect:  "",
							Enabled: true,
							Atoms: []string{
								"star:two",
							},
						},
						{
							ID:      "stars-3",
							Name:    "3*",
							Hint:    "647",
							Effect:  "",
							Enabled: true,
							Atoms: []string{
								"star:three",
							},
						},
						{
							ID:      "stars-4",
							Name:    "4*",
							Hint:    "314",
							Effect:  "",
							Enabled: true,
							Atoms: []string{
								"star:four",
							},
						},
						{
							ID:      "stars-5",
							Name:    "5*",
							Hint:    "64",
							Effect:  "",
							Enabled: true,
							Atoms: []string{
								"star:five",
							},
						},
					},
				}},
			},
			DetailedFilterBatches: []models.FilterAggregationBatches{
				{Filters: []models.FilterAggregation{
					{Type: "PRICE", DetailedFilter: nil},
					{Type: "GROUP",

						DetailedFilter: &models.Filter{
							ID:   "pansion",
							Name: "Питание",
							Type: "MULTI",
							Items: []models.DetailedFilter{
								{
									ID:      "breakfast_included",
									Name:    "Завтрак",
									Hint:    "639",
									Effect:  "",
									Enabled: true,
									Atoms: []string{
										"hotel_pansion_with_offerdata:hotel_pansion_breakfast_included",
									},
								},
								{
									ID:      "breakfast_dinner_included",
									Name:    "Полупансион",
									Hint:    "107",
									Effect:  "",
									Enabled: true,
									Atoms: []string{
										"hotel_pansion_with_offerdata:hotel_pansion_breakfast_dinner_included",
									},
								},
							},
						}},
				}}},
			PriceFilter: models.PriceFilter{
				Currency:         "RUB",
				MinPriceEstimate: 0,
				MaxPriceEstimate: 20000,
				HistogramBounds: []int{
					0,
					1000,
					2000,
					3000,
					4000,
					5000,
					6000,
					7000,
					8000,
					9000,
					10000,
					11000,
					12000,
					13000,
					14000,
					15000,
					16000,
					17000,
					18000,
					19000,
				},
				HistogramCounts: []int{
					134,
					324,
					346,
					385,
					600,
					989,
					1345,
					1639,
					1540,
					1428,
					1274,
					1142,
					1003,
					918,
					849,
					763,
					723,
					658,
					596,
					769,
				},
			}},
		SortInfo: models.SortInfo{
			SelectedSortID: "relevant-first",
			QuickSorterGroup: []models.QuickSorterGroup{
				{
					SortTypes: []models.QuickSorter{
						{ID: "relevant-first", Name: "Сначала популярные"},
					},
				},
				{
					SortTypes: []models.QuickSorter{
						{ID: "cheap-first", Name: "Сначала дешевые"},
					},
				},
				{
					SortTypes: []models.QuickSorter{
						{ID: "expensive-first", Name: "Сначала дорогие"},
					},
				},
				{
					SortTypes: []models.QuickSorter{
						{ID: "high-rating-first", Name: "Сначала с высоким рейтингом"},
					},
				},
			},
		},
		PollEpoch:       0,
		PollingSearchID: "9d9839c1bd8c6cd5dab9eb45d78179a-0-newsearch",
	}
	assert.Equal(t, expected, res)
}

func TestGetHotelImages(t *testing.T) {
	client := buildClient()
	resp, err := testData.ReadFile("testdata/get_hotel_images_rsp.json")
	require.NoError(t, err)

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("GET", "https://example.com/hotels_portal/v1/get_hotel_images",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, string(resp))))
	response, err := client.GetHotelImages(context.Background(), &models.GetHotelImagesRequest{
		QueryData: &models.GetHotelInfoQueryData{
			Permalink: 1686701236,
		},
	})
	require.NoError(t, err)

	assert.Equal(t, testdata.GetHotelImagesResponse, *response)
}

func TestGetHotelOffers(t *testing.T) {
	client := buildClient()
	resp, err := testData.ReadFile("testdata/get_hotel_offers_rsp.json")
	require.NoError(t, err)

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("GET", "https://example.com/hotels_portal/v1/get_hotel_offers",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, string(resp))))
	response, err := client.GetHotelOffers(context.Background(), &models.GetHotelOffersRequest{
		QueryData: &models.GetHotelInfoQueryData{
			Permalink: 1686701236,
		},
	})
	require.NoError(t, err)

	assert.Equal(t, &testdata.GetHotelOffersResponse, response)
}

func TestGetSimilarHotels(t *testing.T) {
	client := buildClient()
	resp, err := testData.ReadFile("testdata/get_similar_hotels_rsp.json")
	require.NoError(t, err)

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("GET", "https://example.com/hotels_portal/v1/get_similar_hotels",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, string(resp))))
	response, err := client.GetSimilarHotels(context.Background(), &models.GetSimilarHotelsRequest{
		QueryData: &models.GetHotelInfoQueryData{
			Permalink: 1686701236,
		},
	})
	require.NoError(t, err)

	assert.Equal(t, models.GetSimilarHotelsResponse{
		SimilarHotelsInfo: models.SimilarHotelsInfo{
			Hotels: []models.HotelWithOffers{
				{
					Hotel: models.Hotel{
						HotelBase: models.HotelBase{
							Name:      "Петроградская",
							Permalink: "70131593855",
							Rating:    4.7,
							Address:   "Большой просп. Петроградской стороны, 27/1, Санкт-Петербург, Россия",
							Coordinates: models.Coordinates{
								Latitude:  59.957362,
								Longitude: 30.299567,
							},
							Slug: "saint-petersburg/petrogradskaia",
						},
						Rubric: models.Rubric{
							Name: "Гостиница",
						},
						Images: []models.Image{
							{
								URLTemplate: "https://avatars.mds.yandex.net/get-altay/1981910/2a0000016d6a0498972b017af41e91cc69ed/%s",
							},
						},
						Features: []models.Feature{
							{
								ID:   "payment_by_credit_card",
								Name: "Оплата картой",
							},
						},
						TotalTextReviewCount: 55,
					},
					OffersInfo: []models.OfferInfo{
						{
							CancellationInfo: &models.CancellationInfo{
								HasFreeCancellation: true,
							},
							ID: "8a84cf4e-bda0-4188-90e8-175d4869b2eb",
							PansionInfo: &models.PansionInfo{
								ID:   "RO",
								Name: "Без питания",
							},
							OperatorID: "44",
							Price: models.Price{
								Currency: "RUB",
								Value:    20000,
							},
							YandexOffer: true,
						},
					},
					PollingFinished: false,
				},
			},
			NextPollingRequestDelayMs: 4000,
			OfferSearchProgress: models.OfferSearchProgress{
				Finished: false,
				FinishedPartners: []string{
					"PI_TVIL",
					"PI_BOOKING",
					"PI_TRAVELLINE",
					"PI_BNOVO",
				},
				PartnersComplete: 4,
				PartnersTotal:    5,
				PendingPartners: []string{
					"PI_OSTROVOK",
				},
			},
			OperatorByID: models.OperatorByID{
				"44": models.OperatorInfo{
					BookOnYandex: true,
					GreenURL:     "travel.yandex.ru",
					IconURL:      "https://yastatic.net/s3/travel-indexer/icons/travel.svg",
					ID:           "44",
					Name:         "Яндекс.Путешествия",
				},
			},
		},
	}, *response)
}

func TestGetHotelInfo(t *testing.T) {
	client := buildClient()
	resp, err := testData.ReadFile("testdata/get_hotel_info_resp.json")
	require.NoError(t, err)

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("GET", "https://example.com/hotels_portal/v1/get_hotel_info",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, string(resp))))
	permalink := uint64(1686701236)
	response, err := client.GetHotelInfo(context.Background(), &models.GetHotelInfoRequest{
		QueryData: &models.GetHotelInfoQueryData{
			Permalink: permalink,
		},
	})
	require.NoError(t, err)

	assert.Equal(t, testdata.GetHotelInfoResponse, *response)
}

func TestGetHotelInfo_BuildURLParams_UsePermalink(t *testing.T) {
	request := models.GetHotelInfoRequest{
		QueryData: &models.GetHotelInfoQueryData{
			Permalink: 1686701236,
			HotelSlug: "moscow/beta-izmailovo",
		},
	}

	url := request.BuildURLParams()

	assert.Equal(t, "1686701236", url.Get("permalink"))
	assert.Equal(t, "", url.Get("hotel_slug"))
}

func TestGetHotelInfo_BuildURLParams_UseSlug(t *testing.T) {
	request := models.GetHotelInfoRequest{
		QueryData: &models.GetHotelInfoQueryData{
			HotelSlug: "moscow/beta-izmailovo",
		},
	}

	url := request.BuildURLParams()

	assert.Equal(t, "", url.Get("permalink"))
	assert.Equal(t, "moscow/beta-izmailovo", url.Get("hotel_slug"))
}

func TestBbox(t *testing.T) {
	main := models.HotelsSearchMainParams{
		Bbox: &models.BoundingBox{
			Coordinates: []models.Coordinates{
				{
					Longitude: 37.421752533039125,
					Latitude:  55.62746978839288,
				},
				{
					Longitude: 37.756977000000006,
					Latitude:  55.847358251133244,
				},
			},
		},
	}

	require.Equal(t, "37.421752533039125,55.62746978839288~37.756977000000006,55.847358251133244", main.Bbox.String())
}

func TestGetOrderStatus(t *testing.T) {
	client := buildClient()

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("GET", "https://example.com/booking_flow/v1/get_order_status",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, `
{
	"status":"CONFIRMED",
	"payment_url":"https://trust-test.yandex.ru/web/payment?purchase_token=2e2acb3553a4449a77fed5997e402559",
	"payment_error":null
}`)))
	orderID := "5b28b2a0-d632-4a55-93c4-ad3444474df9"
	paymentURL := "https://trust-test.yandex.ru/web/payment?purchase_token=2e2acb3553a4449a77fed5997e402559"
	response, err := client.GetOrderStatus(context.Background(), orderID)
	require.NoError(t, err)

	assert.Equal(t, models.GetOrderStatusRsp{
		Status:       models.OrderStatusConfirmed,
		PaymentURL:   &paymentURL,
		PaymentError: nil,
	}, *response)
}

func TestGetOrder(t *testing.T) {
	client := buildClient()
	resp, err := testData.ReadFile("testdata/get_order.json")
	require.NoError(t, err)

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("GET", "https://example.com/booking_flow/v1/get_order",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, string(resp))))
	orderID := "5b28b2a0-d632-4a55-93c4-ad3444474df9"
	paymentURL := "https://trust-test.yandex.ru/web/payment?purchase_token=4635943983bf759135625550dc09ba12"
	response, err := client.GetOrder(context.Background(), orderID)
	require.NoError(t, err)

	assert.Equal(t, models.GetOrderRsp{
		Status: models.OrderStatusAwaitsPayment,
		Payment: &models.Payment{
			Current: &models.CurrentPayment{
				PaymentURL: &paymentURL,
			},
			AmountPaid: models.Price{
				Currency: "RUB",
				Value:    0,
			},
		},
	}, *response)
}

func TestStartPayment(t *testing.T) {
	client := buildClient()

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("POST", "https://example.com/booking_flow/v1/start_payment",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, "{}")))

	orderID := ""
	paymentTestContextToken := ""
	err := client.StartPayment(context.Background(), orderID, &paymentTestContextToken)
	require.NoError(t, err)
}

func TestGetOrderByToken(t *testing.T) {
	client := buildClient()
	resp, err := testData.ReadFile("testdata/get_hotel_order_by_token.json")
	require.NoError(t, err)

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("GET", "https://example.com/booking_flow/v1/get_order_info_by_token",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, string(resp))))

	request := models.GetOrderByTokenRequest{}
	response, err := client.GetOrderByToken(context.Background(), &request)
	require.NoError(t, err)

	assert.Equal(t, &testdata.GetHotelOrderByTokenResponse, response)
}

func TestCreateOrder(t *testing.T) {
	client := buildClient()
	resp, err := testData.ReadFile("testdata/create_order.json")
	require.NoError(t, err)

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("POST", "https://example.com/booking_flow/v1/create_order",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, string(resp))))

	req := models.CreateOrderRequest{}
	rsp, err := client.CreateOrder(context.Background(), &req)
	require.NoError(t, err)

	require.Equal(t, &testdata.CreateOrderResponse, rsp)
}

func TestEstimateDiscount(t *testing.T) {
	client := buildClient()
	resp, err := testData.ReadFile("testdata/estimate_discount.json")
	require.NoError(t, err)

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("POST", "https://example.com/booking_flow/v1/estimate_discount",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, string(resp))))

	req := models.EstimateDiscountRequest{}
	rsp, err := client.EstimateDiscount(context.Background(), &req)
	require.NoError(t, err)

	require.Equal(t, &testdata.EstimateDiscountResponse, rsp)
}

func TestHotelHappyPage(t *testing.T) {
	client := buildClient()
	resp, err := testData.ReadFile("testdata/get_hotel_happy_page.json")
	require.NoError(t, err)

	httpmock.ActivateNonDefault(client.httpClient.GetClient())
	defer httpmock.DeactivateAndReset()

	httpmock.RegisterResponder("GET", "https://example.com/orders/v1/get_order_happy_page",
		httpmock.ResponderFromResponse(mockResponseFromJSONString(200, string(resp))))

	orderID := "7dd0c1f5-bbb1-4105-b1fb-f5ebe2837d9f"
	response, err := client.GetHotelHappyPage(context.Background(), orderID)
	require.NoError(t, err)

	assert.Equal(t, &testdata.GetHotelHappyPage, response)
}
