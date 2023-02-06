package testdata

import (
	"time"

	"a.yandex-team.ru/library/go/ptr"
	"a.yandex-team.ru/travel/app/backend/internal/lib/travelapiclient/models"
)

var date = time.Date(2022, 5, 10, 11, 0, 0, 0, time.UTC)

var GetHotelFavoritesRsp = models.GetHotelFavoritesRsp{
	Categories: []models.Categories{
		{
			Category: models.Category{
				ID:   "all",
				Name: "Все",
			},
			HotelCount: 1,
		}, {
			Category: models.Category{
				ID:   "213",
				Name: "Москва",
			},
			HotelCount: 1,
		},
	},
	Hotels: []models.HotelWithOffers{
		{
			Hotel: models.Hotel{
				HotelURL: "/hotels/moscow/beta-izmailovo/?",
				HotelBase: models.HotelBase{
					Permalink: permalink,
					Name:      "Бета Измайлово",
					Coordinates: models.Coordinates{
						Latitude:  55.789562,
						Longitude: 37.74772,
					},
					Address: "Россия, Москва, Измайловское шоссе, 71, корп. 2Б",
					Stars:   ptr.Int(3),
					Rating:  4.7,

					IsYandexHotel: false,

					Slug: "moscow/beta-izmailovo",
					//TODO(adurnev) нет в ответе от отелей HotelURL: "/hotels/moscow/beta-izmailovo/?adults=0&checkinDate=&checkoutDate=&childrenAges=&searchPagePollingId=&seed=app-search",
				},
				TotalImageCount: 265,
				Features: []models.Feature{
					{
						ID:   "wi_fi",
						Name: "Wi-Fi",
					},
					{
						ID:   "car_park",
						Name: "Парковка",
					},
					{
						ID:   "air_conditioning",
						Name: "Кондиционер в номере",
					},
					{
						ID:   "payment_by_credit_card",
						Name: "Оплата картой",
					},
				},
				Rubric:               models.Rubric{ID: "hotels", Name: "Гостиница"},
				DistanceMeters:       0,
				DistanceText:         "",
				TotalTextReviewCount: 3451,
				IsFavorite:           true,
				Images: []models.Image{
					{
						ID:          "urn:yandex:sprav:photo:69585875",
						URLTemplate: "https://avatars.mds.yandex.net/get-altay/879259/2a0000016156f0d392e7d8da7592698bd77d/%s",
						Sizes: []models.Size{
							{
								Identifier: "XXXS",
								Width:      50,
								Height:     33,
							},
						},
						Tags: []string{"Exterior"},
					},
				},
				GeoInfo: &models.GeoInfo{
					Name: "8,8 км до центра",
					Icon: "city-center",
				},
				FeaturesGroups: []models.FeaturesGroup{
					{
						Feature: models.Feature{
							ID:   "wi_fi",
							Name: "Wi-Fi",
						},
						Features: []models.Feature{
							{
								ID:   "wi_fi",
								Name: "Wi-Fi",
							},
						},
					},
				},
			},
			OffersInfo: []models.OfferInfo{
				{
					ID:             "479cb643-03fa-49b3-a2f2-7e55f2959c4b",
					RoomID:         "479cb643-03fa-49b3-a2f2-7e55f2959c4",
					Name:           "[T!] Эконом с двумя раздельными кроватями • WiFi в номере",
					OperatorID:     "44",
					LandingURL:     "https://xredirect-test.yandex.ru/redir?OfferId=479cb643-03fa-49b3-a2f2-7e55f2959c4b&OfferIdHash=2627619024&ProtoLabel=OgMxMjNILGDEE3DV-eeTBni0iaSkBoIBJDQ3OWNiNjQzLTAzZmEtNDliMy1hMmYyLTdlNTVmMjk1OWM0YqIBAzExObgBDcIBCjIwMjItMDUtMTHIAQHSAQEy6AH-__________8B-gENdHJhdmVsLnBvcnRhbIICJHRyYXZlbC1wb3J0YWwtZmF2b3JpdGVzLXBhZ2UtZGVza3RvcJACCrICMDE2NTIxNjIyODg0NDM5NDUtMzc2OTU2NTQyOC1hZGRycy11cHBlci1zdGFibGUtMboCIzQ3YzRkOWY1LTI4MGVmODliLTdmMGI4MjU4LTRmNzk5MmU1-gIVdHJhdmVsLXBvcnRhbC1kZXNrdG9wggMNcHJpY2UtY2hlY2tlcpk",
					Price:          models.Price{Value: 2500, Currency: "RUB"},
					YandexOffer:    true,
					YandexPlusInfo: &models.YandexPlus{Points: 250},
					PansionInfo: &models.PansionInfo{
						ID:   "RO",
						Name: "Без питания",
					},
					CancellationInfo: &models.CancellationInfo{
						RefundType:          "FULLY_REFUNDABLE",
						HasFreeCancellation: true,
						RefundRules: []models.RefundRules{
							{
								Type:   "FULLY_REFUNDABLE",
								EndsAt: &date,
							},
							{
								Type:     "NON_REFUNDABLE",
								StartsAt: &date,
							},
						},
					},
					Badges: []models.HotelBadge{
						{
							ID:   "yandex_plus",
							Text: "250 баллов",
							AdditionalPromoInfo: models.AdditionalPromoInfo{
								Text:  "Оформите подписку на Яндекс Плюс перед бронированием и мы начислим вам кешбэк баллами плюса в течение 5 дней после выезда из отеля, при условии оплаты бронирования на Яндекс Путешествиях. Баллы можно тратить на Яндекс Путешествиях и в других сервисах Яндекса.",
								Title: "Кешбэк от Яндекс Плюс",
								Link: models.Link{
									Text: "Подробнее о Яндекс Плюсе",
									URL:  "https://plus.yandex.ru/?utm_source=travel&utm_term=src_travel",
								},
							},
						},
					},
				},
			},
			PollingFinished: true,
			Badges: []models.HotelBadge{
				{
					ID:   "yandex_plus",
					Text: "250 баллов",
					AdditionalPromoInfo: models.AdditionalPromoInfo{
						Link: models.Link{
							Text: "Подробнее о Яндекс Плюсе",
							URL:  "https://plus.yandex.ru/?utm_source=travel&utm_term=src_travel",
						},
						Text:  "Оформите подписку на Яндекс Плюс перед бронированием и мы начислим вам кешбэк баллами плюса в течение 5 дней после выезда из отеля, при условии оплаты бронирования на Яндекс Путешествиях. Баллы можно тратить на Яндекс Путешествиях и в других сервисах Яндекса.",
						Title: "Кешбэк от Яндекс Плюс",
					},
				},
			},
		},
	},
	SelectedCategoryID: "all",
	TotalHotelCount:    1,
	OfferSearchParams: &models.SearchParams{
		CheckinDate:  "2022-05-11",
		CheckoutDate: "2022-05-12",
		Adults:       2,
		ChildrenAges: []int{},
	},
}
