package testdata

import (
	"time"

	"a.yandex-team.ru/library/go/ptr"
	"a.yandex-team.ru/travel/app/backend/internal/lib/travelapiclient/models"
)

var checkin = models.Date{
	Time: time.Date(2022, 7, 10, 0, 0, 0, 0, time.UTC),
}
var checkout = models.Date{
	Time: time.Date(2022, 7, 11, 0, 0, 0, 0, time.UTC),
}
var date2 = time.Date(2022, 7, 9, 11, 0, 0, 0, time.UTC)

var permalink = "1686701236"

var GetHotelInfoResponse = models.GetHotelInfoResponse{
	SearchParams: &models.OfferSearchParams{
		CheckinDate:  checkin,
		CheckoutDate: checkout,
		Adults:       1,
		ChildrenAges: []int{},
	},
	Breadcrumbs: models.Breadcrumbs{
		GeoRegions: []models.GeoRegions{
			{
				GeoID: 213,
				Slug:  "moscow",
				Linguistics: models.Linguistics{
					AblativeCase:      "",
					AccusativeCase:    "Москву",
					DativeCase:        "Москве",
					DirectionalCase:   "",
					GenitiveCase:      "Москвы",
					InstrumentalCase:  "Москвой",
					LocativeCase:      "",
					NominativeCase:    "Москва",
					Preposition:       "в",
					PrepositionalCase: "Москве",
				},
				Type: 6,
			},
		},
		Items: []models.Items{
			{
				BreadcrumbType: "",
				GeoRegions: models.GeoRegions{
					GeoID: 213,
					Slug:  "moscow",
					Linguistics: models.Linguistics{
						AblativeCase:      "",
						AccusativeCase:    "Москву",
						DativeCase:        "Москве",
						DirectionalCase:   "",
						GenitiveCase:      "Москвы",
						InstrumentalCase:  "Москвой",
						LocativeCase:      "",
						NominativeCase:    "Москва",
						Preposition:       "в",
						PrepositionalCase: "Москве",
					},
					Type: 6,
				},
			},
		},
	},
	ExtraVisitAndUserParams: models.ExtraVisitAndUserParams{
		VisitParams: models.VisitParams{
			Hotels: models.VisitHotels{},
		},
	},
	Hotel: models.Hotel{
		HotelBase: models.HotelBase{
			Permalink: permalink,
			Slug:      "moscow/beta-izmailovo",
			Name:      "Бета Измайлово",
			Coordinates: models.Coordinates{
				Latitude:  55.789562,
				Longitude: 37.74772,
			},
			Address:       "Россия, Москва, Измайловское шоссе, 71, корп. 2Б",
			Stars:         ptr.Int(3),
			Rating:        4.7,
			IsYandexHotel: false,
		},
		Rubric:          models.Rubric{ID: "hotels", Name: "Гостиница"},
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
		FeaturesGroups: []models.FeaturesGroup{
			{
				Feature: models.Feature{
					ID:   "Internet",
					Name: "Интернет",
				},
				Features: []models.Feature{
					{
						ID:   "wired_internet",
						Name: "Интернет",
					},
				},
			},
		},
		GeoInfo: &models.GeoInfo{
			Name: "8,8 км до центра",
			Icon: "city-center",
		},
		NearestStations: []models.TransportStation{
			{
				ID:           "1727553521",
				Type:         "METRO",
				Name:         "Измайлово",
				DistanceText: "257 м",
			},
		},
		DistanceMeters:       0,
		DistanceText:         "",
		TotalTextReviewCount: 3451,
		IsFavorite:           false,
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
		HotelURL: "/hotels/moscow/beta-izmailovo/?adults=0&checkinDate=&checkoutDate=&childrenAges=&searchPagePollingId=&seed=app-search",
	},
	HotelDescription: &models.HotelDescription{
		Text: "абвгд",
	},
	OffersInfo: models.OffersInfo{
		DefaultOffer: models.OfferInfo{
			ID:   "33ac3466-81db-40e2-8460-455307ca5028",
			Name: "[T!] Эконом с широкой кроватью • WiFi в номере",
			Price: models.Price{
				Value:    2400,
				Currency: "RUB",
			},
			PansionInfo: &models.PansionInfo{
				ID:   "RO",
				Name: "Без питания",
			},
			CancellationInfo: &models.CancellationInfo{
				HasFreeCancellation: true,
				RefundRules: []models.RefundRules{
					{
						EndsAt:   &date2,
						Penalty:  nil,
						StartsAt: nil,
						Type:     "FULLY_REFUNDABLE",
					},
					{
						EndsAt:   nil,
						Penalty:  nil,
						StartsAt: &date2,
						Type:     "NON_REFUNDABLE",
					},
				},
				RefundType: "FULLY_REFUNDABLE",
			},
			YandexPlusInfo: &models.YandexPlus{
				Eligible: false,
				Points:   240,
			},
			Badges: []models.HotelBadge{
				{
					ID:   "yandex_plus",
					Text: "240 баллов",
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
			DiscountInfo: nil,
			RoomID:       "8fa9de6dc854b5d20141fe32f146524c",
			OperatorID:   "44",
			LandingURL:   "https://xredirect-test.yandex.ru/redir?OfferId=33ac3466-81db-40e2-8460-455307ca5028&OfferIdHash=4226045642&ProtoLabel=OgMxMjNILGDgEnDkpOuVBni0iaSkBoIBJDMzYWMzNDY2LTgxZGItNDBlMi04NDYwLTQ1NTMwN2NhNTAyOKIBAzExObgBDcIBCjIwMjItMDctMTDIAQHSAQEx6AH-__________8B-gENdHJhdmVsLnBvcnRhbIICIHRyYXZlbC1wb3J0YWwtaG90ZWwtcGFnZS1kZXNrdG9wkAIJsgIvMTY1NjQxMDc1Mjc2Nzg0MS03MDg4MzY1MDAtYWRkcnMtdXBwZXItc3RhYmxlLTS6AiNkODUzMjQwMy1mYTIxMjc5MS1mZjJkZDI0Zi01MTFjOGE2ZvoCFXRyYXZlbC1wb3J0YWwtZGVza3RvcIIDFXRyYXZlbC1wb3J0YWwtZGVza3RvcAw",
			Token:        "",
			YandexOffer:  true,
		},
		AggregatedOfferInfo: models.AggregatedOfferInfo{
			CancellationInfoAggregate: ptr.String("FULLY_REFUNDABLE_AVAILABLE"),
			MaxPrice: models.MaxPrice{
				Price: models.Price{
					Value:    9675,
					Currency: "RUB",
				},
			},
			MinPrice: models.MinPrice{
				Price: models.Price{
					Value:    2400,
					Currency: "RUB",
				},
			},
			PansionAggregate: ptr.String("PANSION_AVAILABLE"),
		},
		BannerType: "NONE",
		GroupBy:    "ROOMS",
		MainOffers: []models.OfferInfo{
			{
				ID:   "33ac3466-81db-40e2-8460-455307ca5028",
				Name: "[T!] Эконом с широкой кроватью • WiFi в номере",
				Price: models.Price{
					Value:    2400,
					Currency: "RUB",
				},
				PansionInfo: &models.PansionInfo{
					ID:   "RO",
					Name: "Без питания",
				},
				CancellationInfo: &models.CancellationInfo{
					HasFreeCancellation: true,
					RefundRules: []models.RefundRules{
						{
							EndsAt:   &date2,
							Penalty:  nil,
							StartsAt: nil,
							Type:     "FULLY_REFUNDABLE",
						},
					},
					RefundType: "FULLY_REFUNDABLE",
				},
				YandexPlusInfo: &models.YandexPlus{
					Eligible: false,
					Points:   240,
				},
				Badges: []models.HotelBadge{
					{
						ID:   "yandex_plus",
						Text: "240 баллов",
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
				DiscountInfo: nil,
				RoomID:       "8fa9de6dc854b5d20141fe32f146524c",
				OperatorID:   "44",
				LandingURL:   "https://xredirect-test.yandex.ru/redir?OfferId=33ac3466-81db-40e2-8460-455307ca5028&OfferIdHash=4226045642&ProtoLabel=OgMxMjNILGDgEnDkpOuVBni0iaSkBoIBJDMzYWMzNDY2LTgxZGItNDBlMi04NDYwLTQ1NTMwN2NhNTAyOKIBAzExObgBDcIBCjIwMjItMDctMTDIAQHSAQEx6AH-__________8B-gENdHJhdmVsLnBvcnRhbIICIHRyYXZlbC1wb3J0YWwtaG90ZWwtcGFnZS1kZXNrdG9wkAIJsgIvMTY1NjQxMDc1Mjc2Nzg0MS03MDg4MzY1MDAtYWRkcnMtdXBwZXItc3RhYmxlLTS6AiNkODUzMjQwMy1mYTIxMjc5MS1mZjJkZDI0Zi01MTFjOGE2ZvoCFXRyYXZlbC1wb3J0YWwtZGVza3RvcIIDFXRyYXZlbC1wb3J0YWwtZGVza3RvcAw",
				Token:        "",
				YandexOffer:  true,
			},
		},
		NextPollingRequestDelayMs: 0,
		OfferCount:                5,
		OfferSearchProgress: models.OfferSearchProgress{
			Finished: true,
			FinishedPartners: []string{
				"PI_HOTELSCOMBINED",
				"PI_OSTROVOK",
				"PI_EXPEDIA",
				"PI_BNOVO",
				"PI_BRONEVIK",
				"PI_BOOKING",
				"PI_TRAVELLINE",
				"PI_HOTELS101",
			},
			PartnersTotal:    8,
			PartnersComplete: 8,
			PendingPartners:  []string{},
		},
		OperatorByID: models.OperatorByID{
			"44": {
				ID:           "44",
				Name:         "Яндекс.Путешествия",
				GreenURL:     "travel.yandex.ru",
				IconURL:      "https://yastatic.net/s3/travel-indexer/icons/travel.svg",
				BookOnYandex: true,
			},
		},
		OperatorCount: 2,
		PartnerOffers: []models.PartnerOffers{
			{
				CancellationInfoAggregate: nil,
				DefaultOffer: models.OfferInfo{
					ID:   "e1a74416-bcee-423b-ac34-6f6ed5cde556",
					Name: "[T!] Случайно-сгенерированный стандартный двухместный номер (2 отдельные кровати)",
					Price: models.Price{
						Value:    3699,
						Currency: "RUB",
					},
					PansionInfo: &models.PansionInfo{
						ID:   "BB",
						Name: "Завтрак включён",
					},
					CancellationInfo: &models.CancellationInfo{
						HasFreeCancellation: false,
						RefundRules:         nil,
						RefundType:          "NON_REFUNDABLE",
					},
					YandexPlusInfo: nil,
					Badges:         nil,
					DiscountInfo:   nil,
					RoomID:         "",
					OperatorID:     "4",
					LandingURL:     "https://xredirect-test.yandex.ru/redir?OfferId=e1a74416-bcee-423b-ac34-6f6ed5cde556&OfferIdHash=971193549&ProtoLabel=OgMxMjNIBGDzHHDmpOuVBni0iaSkBoIBJGUxYTc0NDE2LWJjZWUtNDIzYi1hYzM0LTZmNmVkNWNkZTU1NqIBBzg2ODUzODK4AR7CAQoyMDIyLTA3LTEwyAEB0gEBMegB_v__________AfoBDXRyYXZlbC5wb3J0YWyCAiB0cmF2ZWwtcG9ydGFsLWhvdGVsLXBhZ2UtZGVza3RvcJACCbICLzE2NTY0MTA3NTI3Njc4NDEtNzA4ODM2NTAwLWFkZHJzLXVwcGVyLXN0YWJsZS00ugIjNDRhYzUwYjktNWNlYjRkMzktODk4ODI5N2MtNTVjZmIxYTP6AhV0cmF2ZWwtcG9ydGFsLWRlc2t0b3CCAxV0cmF2ZWwtcG9ydGFsLWRlc2t0b3DD",
					Token:          "",
					YandexOffer:    false,
				},
				DefaultOfferCancellationInfo: nil,
				DefaultOfferPansion: &models.PansionInfo{
					ID:   "BB",
					Name: "Завтрак включён",
				},
				OperatorID:       "4",
				PansionAggregate: ptr.String("PANSION_AVAILABLE"),
			},
		},
		Rooms: []models.Room{
			{
				AmenityGroups: []models.FeaturesGroup{
					{
						Features: []models.Feature{
							{
								ID:   "phone:true",
								Name: "Телефон",
								Icon: ptr.String("phone"),
							},
						},
						Feature: models.Feature{
							Icon: ptr.String("internet-telephony"),
							Name: "Интернет и телефония",
							ID:   "internet-telephony",
						},
					},
					{
						Features: []models.Feature{
							{
								Icon: ptr.String("beds-types"),
								ID:   "beds-types:bunk_beds",
								Name: "Двухъярусные кровати",
							},
							{
								Icon: ptr.String("beds-types"),
								ID:   "beds-types:eight_single_beds",
								Name: "Восемь односпальных кроватей",
							},
						},
						Feature: models.Feature{
							Icon: ptr.String("sleep"),
							ID:   "sleep",
							Name: "Сон",
						},
					},
				},
				Area: models.Area{
					Unit:  "SQUARE_METERS",
					Value: 22,
				},
				Badges: []models.HotelBadge{},
				BedGroups: []models.BedGroups{
					{
						ID: "973f0bef0be07f58f23701d299c8b34d",
						Configuration: []models.Configuration{
							{
								Icon:              "single_bed",
								ID:                "single_bed",
								NameInflectedForm: "односпальные кровати",
								NameInitialForm:   "односпальная кровать",
								Quantity:          2,
							},
						},
					},
				},
				CancellationInfoAggregate: "FULLY_REFUNDABLE_AVAILABLE",
				Description:               "Номер с двумя раздельными кроватями, оснащенный отдельной ванной комнатой. Общая площадь номера составляет  22 кв.м.",
				ID:                        "55891996b657a4cec5a953673eafcc58",
				Images: []models.Image{
					{
						URLTemplate: "https://avatars.mds.yandex.net/get-travel-rooms/3595101/2a0000017338fdcbeaf7fdfb4a3e798ac087/%s",
						Sizes: []models.Size{
							{
								Identifier: "orig",
								Width:      900,
								Height:     600,
							},
						},
						ID:         "3595101-2a0000017338fdcbeaf7fdfb4a3e798ac087",
						Moderation: nil,
						Tags:       nil,
					},
				},
				MainAmenities: []models.Feature{
					{
						Icon: ptr.String("wifi"),
						ID:   "wifi:free",
						Name: "Бесплатный Wi‑Fi",
					},
				},
				Name:             "Эконом с двумя раздельными кроватями",
				PansionAggregate: "PANSION_AVAILABLE",
			},
		},
	},
	ParentRequestID: "adf1492e-1f6d-4441-a276-0ffcb5619aa1",
	RatingsInfo: &models.RatingsInfo{
		Teaser: "100% гостей понравилось питание",
		FeatureRatings: []models.FeatureRatings{
			{
				ID:              "survey_food",
				Name:            "Питание",
				PositivePercent: 100,
			},
		},
	},
	ReviewsInfo: &models.ReviewsInfo{
		TotalTextReviewCount: 3451,
		TotalKeyPhraseCount:  10,
		HasMore:              true,
		TextReviews: []models.TextReview{
			{
				Author: models.Author{
					Name:              "Дмитрий Аввакумов",
					AvatarURLTemplate: "https://avatars.mds.yandex.net/get-yapic/15298/enc-922ab6ca0a767bd2e12332ee27f071e8/{size}",
					Level:             "Знаток города 7 уровня",
					ProfileURL:        "https://reviews.yandex.ru/user/0gwkz2j6mzxnwtjfyyphaxa8a8",
				},
				BusinessComment: "",
				CommentCount:    1,
				ID:              "5lQsa6r6kSkv6uyhufS5BUP01aNV-1g",
				Images: []models.Image{
					{
						URLTemplate: "https://avatars.mds.yandex.net/get-altay/5482016/2a0000017e3e2cb38551e583bcf33d12c374/{size}",
						Sizes:       nil,
						ID:          "A6WyBexdv1B8hlD5YB1dHUXeYfa7Ef",
						Moderation: &models.Moderation{
							Status: "ACCEPTED",
						},
						Tags: nil,
					},
				},
				KeyPhraseMatch:    nil,
				Moderation:        nil,
				Rating:            3,
				Snippet:           "Гостиница по местоположению очень удобная, здесь метро Партизанская и МЦК Измайлово буквально в 50 метрах...",
				Text:              "Гостиница по местоположению очень удобная, здесь метро Партизанская и МЦК Измайлово буквально в 50 метрах, рядом есть блошиный рынок Измайловского Кремля, много точек быстрого питания и крупный торговый центр\n\nВнутри же гостиница тянет на 2* не более\nОбязательно всем иметь паспорт!\nИначе не заселят\nДизайн холла в стиле 70х годов ХХ века\nНомера исключительно с одноместными кроватями\nРай для командировочных \nОтдохнуть здесь не получится, лучше взять Альфу или Гамму! \nПитание на 3 из 10 баллов, лучше сходить в Якиторию или грузинский ресторанчик неподалёку, очень много молодых пьяных людей вечером здесь, не рекомендую данный корпус, советую Гамму или Дельту для спокойного отдыха, однако не пользуйтесь баней, там грязно! Всем добра!",
				TotalDislikeCount: 2,
				TotalLikeCount:    5,
				UpdatedAt:         "",
				UserReaction:      "NONE",
			},
		},
		KeyPhrases: []models.KeyPhrases{
			{
				Name:        "хорошая гостиница",
				ReviewCount: 677,
			},
		},
	},
	SeoInfo: models.SeoInfo{
		Title:       "Гостиница Бета Измайлово в Москве  — кешбэк баллами на Яндекс.Путешествиях",
		Description: "Бета Измайлово: бронируйте проживание на Яндекс.Путешествиях и получайте 10% кешбэка баллами Плюса за каждую поездку. Сравните стоимость размещения и выберите лучшее предложение на ваши даты. Расположение на карте, отзывы посетителей, фото номеров и правила проживания.",
		OpenGraph: models.OpenGraph{
			Title:       "Гостиница Бета Измайлово в Москве  — кешбэк баллами на Яндекс.Путешествиях",
			Description: "Бета Измайлово: бронируйте проживание на Яндекс.Путешествиях и получайте 10% кешбэка баллами Плюса за каждую поездку. Сравните стоимость размещения и выберите лучшее предложение на ваши даты. Расположение на карте, отзывы посетителей, фото номеров и правила проживания.",
			Image:       "https://avatars.mds.yandex.net/get-altay/879259/2a0000016156f0d392e7d8da7592698bd77d/orig",
		},
		SchemaOrg: models.SchemaOrg{
			Name:        "Бета Измайлово",
			Image:       "https://avatars.mds.yandex.net/get-altay/879259/2a0000016156f0d392e7d8da7592698bd77d/orig",
			PriceRange:  "2520р. - 10803р.",
			Address:     "Россия, Москва, Измайловское шоссе, 71, корп. 2Б",
			RatingValue: 4.7,
			ReviewCount: 3451,
		},
	},
	SimilarHotelsInfo: models.SimilarHotelsInfo{
		OfferSearchProgress: models.OfferSearchProgress{
			Finished:         true,
			PartnersTotal:    8,
			PendingPartners:  []string{},
			PartnersComplete: 8,
			FinishedPartners: []string{
				"PI_HOTELSCOMBINED",
				"PI_OSTROVOK",
				"PI_EXPEDIA",
				"PI_BNOVO",
				"PI_BRONEVIK",
				"PI_BOOKING",
				"PI_TRAVELLINE",
				"PI_HOTELS101",
			},
		},
		Hotels: []models.HotelWithOffers{
			{
				Hotel: models.Hotel{
					HotelBase: models.HotelBase{
						Permalink: "159702281672",
						Slug:      "moscow/otel-randevu-avtozavodskaia",
						Name:      "Отель Рандеву Автозаводская",
						Coordinates: models.Coordinates{
							Latitude:  55.703294,
							Longitude: 37.678819,
						},
						Address:       "ул. Трофимова, 25, корп. 1, Москва, Россия",
						Stars:         nil,
						Rating:        4.3,
						IsYandexHotel: false,
					},
					Rubric: models.Rubric{
						ID:   "",
						Name: "Гостиница",
					},
					TotalImageCount: 0,
					Features: []models.Feature{
						{
							ID:   "air_conditioning",
							Name: "Кондиционер в номере",
						},
						{
							ID:   "payment_by_credit_card",
							Name: "Оплата картой",
						},
					},
					FeaturesGroups:       nil,
					GeoInfo:              nil,
					NearestStations:      nil,
					DistanceMeters:       0,
					DistanceText:         "",
					TotalTextReviewCount: 42,
					IsFavorite:           false,
					Images: []models.Image{
						{
							URLTemplate: "https://avatars.mds.yandex.net/get-altay/176734/2a00000163e9488ca3d23441b9b2191dccc4/%s",
							ID:          "",
							Moderation:  nil,
							Sizes:       nil,
							Tags:        nil,
						},
					},
					HotelURL: "",
				},
				OffersInfo:      []models.OfferInfo{},
				PollingFinished: false,
			},
		},
	},
	SeoBreadcrumbs: models.SeoBreadcrumbs{
		Breadcrumbs: models.Breadcrumbs{
			GeoRegions: nil,
			Items: []models.Items{
				{
					BreadcrumbType: "",
					GeoRegions: models.GeoRegions{
						GeoID: 213,
						Slug:  "moscow",
						Linguistics: models.Linguistics{
							AblativeCase:      "",
							AccusativeCase:    "Москву",
							DativeCase:        "Москве",
							DirectionalCase:   "",
							GenitiveCase:      "Москвы",
							InstrumentalCase:  "Москвой",
							LocativeCase:      "",
							NominativeCase:    "Москва",
							Preposition:       "в",
							PrepositionalCase: "Москве",
						},
						Type: 6,
					},
				},
			},
		},
	},
}
