package testdata

import (
	"time"

	"a.yandex-team.ru/library/go/ptr"
	"a.yandex-team.ru/travel/app/backend/internal/lib/travelapiclient/models"
)

var refundTill = time.Date(2022, 7, 9, 8, 59, 0, 0, time.UTC)
var refund2 = time.Date(2022, 7, 9, 12, 0, 0, 0, time.UTC)
var cancellationInfoAggregate = "FULLY_REFUNDABLE_AVAILABLE"
var GetHotelOffersResponse = models.GetHotelOffersResponse{
	OffersInfo: models.OffersInfo{
		AggregatedOfferInfo: models.AggregatedOfferInfo{
			CancellationInfoAggregate: &cancellationInfoAggregate,
			PansionAggregate:          nil,
			MaxPrice: models.MaxPrice{
				Price: models.Price{Value: 55000, Currency: "RUB"},
			},
			MinPrice: models.MinPrice{
				Price: models.Price{Value: 55000, Currency: "RUB"},
			},
		},
		BannerType: "NONE",
		DefaultOffer: models.OfferInfo{
			ID:   "47cde497-5173-4b07-8605-54192d8fc84d",
			Name: "Кровать в общем номере (женский номер) (общая ванная комната) (8 кроватей, дополнительная кровать (без питания) включена)",
			CancellationInfo: &models.CancellationInfo{
				HasFreeCancellation: true,
				RefundType:          "FULLY_REFUNDABLE",
				RefundRules: []models.RefundRules{
					{
						Type:   "FULLY_REFUNDABLE",
						EndsAt: &refund2,
					},
				},
			},
			LandingURL: "https://travel.yandex.ru/redir?OfferId=47cde497-5173-4b07-8605-54192d8fc84d",
			PansionInfo: &models.PansionInfo{
				Name: "Без питания",
				ID:   "RO",
			},
			OperatorID: "4",
			Price:      models.Price{Value: 55000, Currency: "RUB"},
		},
		GroupBy: "ROOMS",
		MainOffers: []models.OfferInfo{
			{
				ID:   "1058d4f9-bf63-40df-ae98-4b92a74168b5",
				Name: "[T!] Стандартный • WiFi в номере • Онлайн-регистрация • Спецпредложения от партнеров",
				Price: models.Price{
					Value:    2780,
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
							EndsAt:   &refund2,
							Penalty:  nil,
							StartsAt: nil,
							Type:     "FULLY_REFUNDABLE",
						},
					},
					RefundType: "FULLY_REFUNDABLE",
				},
				YandexPlusInfo: &models.YandexPlus{
					Eligible: false,
					Points:   278,
				},
				Badges: []models.HotelBadge{
					{
						ID:   "yandex_plus",
						Text: "278 баллов",
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
				RoomID:       "3951579e2881b550e01f5ca33e562392",
				OperatorID:   "44",
				LandingURL:   "https://xredirect-test.yandex.ru/redir?OfferId=1058d4f9-bf63-40df-ae98-4b92a74168b5&OfferIdHash=349230869&ProtoLabel=OgMxMjNILGDcFXCBjI-WBniz55mQBYIBJDEwNThkNGY5LWJmNjMtNDBkZi1hZTk4LTRiOTJhNzQxNjhiNaIBAzEwMLgBDcIBCjIwMjItMDctMTDIAQHSAQEx6AH-__________8B-gENdHJhdmVsLnBvcnRhbIICIHRyYXZlbC1wb3J0YWwtaG90ZWwtcGFnZS1kZXNrdG9wkAIJsgIwMTY1NzAwMDkyNTc0MjU3OS0zNDA5NjAwNTYzLWFkZHJzLXVwcGVyLXN0YWJsZS00ugIiYWNiYWY4MzMtZTJmZGU0MmItNzZlOGQ1ZC00OGJlYTlhNvoCFXRyYXZlbC1wb3J0YWwtZGVza3RvcIIDFXRyYXZlbC1wb3J0YWwtZGVza3RvcGk",
				Token:        "",
				YandexOffer:  true,
			},
		},
		NextPollingRequestDelayMs: 0,
		OfferCount:                1,
		OfferSearchProgress: models.OfferSearchProgress{
			FinishedPartners: []string{
				"PI_BOOKING",
				"PI_OSTROVOK",
				"PI_TRAVELLINE",
			},
			Finished:         true,
			PartnersTotal:    3,
			PartnersComplete: 3,
			PendingPartners:  []string{},
		},
		OperatorByID: models.OperatorByID{
			"2": models.OperatorInfo{
				BookOnYandex: false,
				GreenURL:     "booking.com",
				IconURL:      "https://yastatic.net/s3/travel-indexer/icons/booking.svg",
				ID:           "2",
				Name:         "Booking.com",
			},
			"4": models.OperatorInfo{
				BookOnYandex: false,
				GreenURL:     "ostrovok.ru",
				IconURL:      "https://yastatic.net/s3/travel-indexer/icons/ostrovok.svg",
				ID:           "4",
				Name:         "Ostrovok.ru",
			},
		},
		OperatorCount: 1,
		PartnerOffers: []models.PartnerOffers{
			{
				CancellationInfoAggregate: &cancellationInfoAggregate,
				DefaultOffer: models.OfferInfo{
					CancellationInfo: &models.CancellationInfo{
						HasFreeCancellation: true,
						RefundRules: []models.RefundRules{
							{
								EndsAt: &refundTill,
								Type:   "FULLY_REFUNDABLE",
							},
						},
						RefundType: "FULLY_REFUNDABLE",
					},
					ID:         "47cde497-5173-4b07-8605-54192d8fc84d",
					Name:       "Кровать в общем номере (женский номер) (общая ванная комната) (8 кроватей, дополнительная кровать (без питания) включена)",
					LandingURL: "https://travel.yandex.ru/redir?DebugPortalHost=travel.yandex.ru&OfferId=47cde497-5173-4b07-8605-54192d8fc84d&OfferIdHash=2617783070&ProtoLabel=CgZ5YW5kZXgSBnNlYXJjaBo-Y246c3NhX2h0bF9rNTAuaG90ZWwuY2l0eS5sZG5nLmh0bC1tYWluLWt3X3J1X2FsbHxjaWQ6NzM0MDYyNjkikwFhaWQ6MTIwMjI3Njg4OTV8Ymk6MTIwMjI3Njg4OTV8Y3Q6dHlwZTF8Z2lkOjQ4ODY5MDM5ODB8cHN0OjF8cHN0OnByZW1pdW18ZHQ6ZGVza3RvcHxwbNGBOm5vbmV8cGxjdDpzZWFyY2h8Y2djaTowfHJuOtCc0L7RgdC60LLQsHxyaWQ6MjEzfHJ0aWQ6fG1haW4qNGt3OtC-0YLQtdC70Lgg0LrRgNCw0YHQvdC-0LPQvtGA0YHQunxraWQ6MzgxNzAwNjQ0NzE6EzQ1MjI5Mzg3NDE2Mzk2NDgxMTlIBGDYrQNwx4KglAZ419WdzYsDggEkNDdjZGU0OTctNTE3My00YjA3LTg2MDUtNTQxOTJkOGZjODRkogEHOTk5MTY3MKoBCjE0OTA4NTQ1MzG4AR7CAQoyMDIyLTA3LTEwyAEL0gEBNOgBAfIBEzQ1MjI5Mzg3NDE2Mzk2NDgxMTn6AQ10cmF2ZWwucG9ydGFsggIgdHJhdmVsLXBvcnRhbC1ob3RlbC1wYWdlLWRlc2t0b3CQAgmaAgdkZXNrdG9wqgIgNWU0NjAzNDdjN2MyOGMxZDZjNzQ2MzEzY2FlNTVkNjayAiBlOWQ4NWY5MDA4MDAwZjMyODU3ZDIxZWUzM2Q5OTkyN7oCIzY1NmE5NTY5LTU3NGFlMGNkLTNlYjc1YzNhLThkYWM4NmVh4gITMTYzOTY0ODEyNDE1NDk4NzM0MOoCG_OyIMqbIuucI7HjIqDAId6zHfyuHYbpH8OVI_ICSP___________wH___________8B____________Af___________wH___________8B____________Af___________wFfRvoCFXRyYXZlbC1wb3J0YWwtZGVza3RvcIIDFXRyYXZlbC1wb3J0YWwtZGVza3RvcJIDEzQ1MjI5Mzg3NDE2Mzk2NDgxMTnCAyoxYmYyODMxNTE5NTk5ZTlkZjE3MTBmNjc3OGMwOTQtMC1uZXdzZWFyY2gf",
					PansionInfo: &models.PansionInfo{
						ID:   "RO",
						Name: "Без питания",
					},
					OperatorID: "4",
					Price: models.Price{
						Currency: "RUB",
						Value:    55000,
					},
					YandexOffer: false,
				},
				DefaultOfferCancellationInfo: &models.CancellationInfo{
					HasFreeCancellation: true,
					RefundRules: []models.RefundRules{
						{
							EndsAt: &refundTill,
							Type:   "FULLY_REFUNDABLE",
						},
					},
					RefundType: "FULLY_REFUNDABLE",
				},
				DefaultOfferPansion: nil,
				OperatorID:          "4",
				PansionAggregate:    nil,
			},
		},
		Rooms: []models.Room{
			{
				AmenityGroups: []models.FeaturesGroup{
					{
						Features: []models.Feature{
							{
								Icon: ptr.String("maid-service"),
								ID:   "maid-service:true",
								Name: "Обслуживание номеров (услуги горничной)",
							},
						},
						Feature: models.Feature{
							Icon: ptr.String("other"),
							ID:   "other",
							Name: "Удобства",
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
					Value: 24,
				},
				Badges: []models.HotelBadge{},
				BedGroups: []models.BedGroups{
					{
						Configuration: []models.Configuration{
							{
								Icon:              "double_bed",
								ID:                "double_bed",
								NameInflectedForm: "двуспальная кровать",
								NameInitialForm:   "двуспальная кровать",
								Quantity:          1,
							},
						},
						ID: "c7093de994ab6be1d426c58e1a97efbc",
					},
					{
						Configuration: []models.Configuration{
							{
								Icon:              "single_bed",
								ID:                "single_bed",
								NameInflectedForm: "односпальные кровати",
								NameInitialForm:   "односпальная кровать",
								Quantity:          2,
							},
						},
						ID: "973f0bef0be07f58f23701d299c8b34d",
					},
				},
				CancellationInfoAggregate: "NON_REFUNDABLE_AVAILABLE",
				Description:               "Койко-место 80×190см в двухуровневой кровати в восьмиместном номере, выключатель у изголовья кровати, постельные принадлежности, 2 полотенца, стул для каждого, вешалки для одежды, розетки по количеству койко-мест, кухня обустроенная всей необходимой техникой  посудой и мебелью. На кухне телевизор. В хостеле есть wi-fi. Несколько санитарно- гигиенических помещений. Есть отдельная система хранения вещей.",
				ID:                        "afe68e4a2566d1954eb9cf02259aebc1",
				Images: []models.Image{
					{
						ID: "3613454-2a0000017f9f9b2f13246d613ab82d8728a8",
						Sizes: []models.Size{
							{
								Identifier: "orig",
								Width:      1440,
								Height:     1080,
							},
						},
						URLTemplate: "https://avatars.mds.yandex.net/get-travel-rooms/3613454/2a0000017f9f9b2f13246d613ab82d8728a8/%s",
					},
				},
				MainAmenities: []models.Feature{
					{
						Icon: ptr.String("view-court-view"),
						ID:   "view:court_view",
						Name: "Вид во двор",
					},
					{
						Icon: ptr.String("wifi"),
						ID:   "wifi:free",
						Name: "Бесплатный Wi‑Fi",
					},
				},
				Name:             "Койко-место в общем 8-местном номере",
				PansionAggregate: "PANSION_AVAILABLE",
			},
		},
	},
}
