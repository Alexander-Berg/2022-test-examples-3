package testdata

import (
	"time"

	"a.yandex-team.ru/library/go/ptr"
	"a.yandex-team.ru/travel/app/backend/internal/lib/travelapiclient/models"
)

var GetHotelOrderByTokenResponse = models.GetOrderByTokenResponse{
	HotelOrderInfo: models.HotelOrderInfo{
		TravelToken: "iROzF42qxNp-2oKOBN_ioAOFWbTn58uD4k2uE8KfWhfK8as_73ZzSThNk9E9-6hxE_YL6pyjcDQCt2oZLUzQMAl_oGMiuum7HzSo9MeTSspB4zUIsuJEXW3aCpfiVpa1b7-Y0KnhsgLyVEARR1OadFWq2uXhxfubXRLmbqwH",
		Label:       "sPVc-xDLul05-1p30Jaz1YHlM6_VrRiU4X1vmWA",
		Checksum:    "qlr816pACLpv2_NamBiIkQ==",
		SessionKey:  "5268a42a-6ff3-459a-866a-2f8406e92ecb",
		BasicHotelInfo: models.HotelCheckout{
			HotelBase: models.HotelBase{
				Permalink:     "1376154547",
				Slug:          "moscow/kosmos",
				Name:          "Космос",
				Coordinates:   models.Coordinates{},
				Address:       "Россия, Москва, проспект Мира, 150",
				Stars:         ptr.Int(3),
				Rating:        4.3,
				IsYandexHotel: false,
			},
			Breadcrumbs: models.Breadcrumbs{
				GeoRegions: []models.GeoRegions{
					{
						GeoID: 213,
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
						Slug: "moscow",
						Type: 6,
					},
				},
				Items: []models.Items{
					{
						BreadcrumbType: "",
						GeoRegions: models.GeoRegions{
							GeoID: 213,
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
							Slug: "moscow",
							Type: 6,
						},
					},
				},
			},
			ImageURLTemplate: "https://avatars.mds.yandex.net/get-altay/4824927/2a00000180b7cf56ef3af4ee71c861c09cb7/%s",
			LocationType:     "MOSCOW",
			Phone:            "+7 (495) 234-12-06",
			WorkingHours:     "ежедневно, круглосуточно",
			LegalInfo: models.LegalInfo{
				Inn:     "",
				Ogrn:    "1027700007037",
				Address: "129366, Россия, Москва, проспект Мира, 150",
			},
		},
		PartnerHotelInfo: models.HotelPartner{
			Address: models.Address{
				City:        "г. Москва",
				CountryCode: "129366",
				Line1:       "пр-т Мира д. 150",
				Line2:       "",
			},
			Amenities: map[string]models.Feature{},
			Checkin: models.Checkin{
				BeginTime:           "15:00",
				EndTime:             "",
				Instructions:        "",
				SpecialInstructions: "",
			},
			Checkout: models.Checkout{
				Time: "12:00",
			},
			Descriptions: models.Descriptions{},
			Fees: models.Fees{
				Mandatory: "<p>В стоимость включены следующие услуги:<ul><li><p><b>Спецпредложения от партнеров</b><br/>Наши гости получают специальные предложения (скидки, промокоды) в welcome – письме, которое приходит на электронную почту за 3 дня до заезда.</p></li><li><p><b>Онлайн-регистрация</b><br/>Процедура онлайн-регистрации проста и безопасна: она состоит из нескольких несложных шагов, а ваши данные будут надежно защищены.<br/>После бронирования номера за 2-3 дня до заезда Вам придет письмо со ссылкой на check-in, и если вам подходит такой формат, вы можете зарегистрироваться в режиме онлайн.</p></li></ul></p>",
				Optional:  "",
			},
			Images: []models.ImagesLinks{
				{
					Links: map[string]models.ImageLink{
						"350px": {
							Href:   "https://www.travelline.ru/resource/images/p/100/635086166167993546-8f4c8fcb-cd55-4899-8429-b57d4bba6cef",
							Method: "GET",
						},
					},
				},
			},
			Location:   models.Location{},
			Name:       "Cosmos Moscow Hotel",
			Phone:      "+7 (495) 234-12-06",
			Policies:   models.Policies{},
			PropertyID: "100",
			Ratings: models.Ratings{
				Property: models.Property{
					Rating: "3.0",
					Type:   "Star",
				},
			},
		},
		PartnerRoomInfo: models.RoomPartner{
			Amenities: map[string]models.FeaturePartner{
				"0": {
					Name: "ванна",
				},
			},
			Descriptions: models.Descriptions{
				Overview: "<p>Уютный классический однокомнатный номер, в котором созданы условия для работы и отдыха, с бесплатным Wi-Fi и прекрасными видами на ВДНХ, Останкинскую башню и городские пейзажи Москвы. </p><p>Просим обратить внимание, что предпочитаемый тип кроватей – две односпальные или большая двуспальная - не гарантируется. Ваши пожелания могут быть учтены при наличии номеров с указанным типом кровати на момент заселения.</p>",
			},
			Images: []models.ImagesLinks{
				{
					Links: map[string]models.ImageLink{
						"350px": {
							Href:   "https://www.travelline.ru/resource/images/rt/896/637630890916783107-909c6994-5e7e-4165-a520-846804c5d30f",
							Method: "GET",
						},
					},
				},
			},
			Name: "Стандартный [38266] 'Тариф на проживание без завтрака'",
		},
		BedGroups: []models.BedGroupsCheckout{
			{
				Description: "две односпальные или большая двуспальная кровать",
				ID:          "0",
			},
		},
		RequestInfo: models.RequestInfo{
			SearchParams: models.SearchParams{
				Adults:       2,
				CheckinDate:  "2022-07-20",
				CheckoutDate: "2022-07-21",
				ChildrenAges: []int{},
			},
			NumAdults:             2,
			SelectedBedGroupIndex: 0,
		},
		RateInfo: models.RateInfo{
			ExtraCharges: []models.ExtraCharges{},
			HotelCharges: models.HotelCharges{
				Daily: []models.HotelChargesBase{},
				Nightly: []models.HotelChargesBase{
					{
						Base: models.Amount{
							Amount:   "3360.00",
							Currency: "RUB",
						},
						TaxesAndFees: nil,
					},
				},
				Totals: models.Totals{
					Base: models.Amount{
						Amount:   "3360",
						Currency: "RUB",
					},
					Discount: models.Amount{},
					Grand: models.Amount{
						Amount:   "3360",
						Currency: "RUB",
					},
					PriceAfterPlusWithdraw: models.AmountValue{},
					StrikeThrough:          models.Amount{},
					TaxesAndFees:           nil,
					TaxesAndFeesSum:        models.Amount{},
				},
			},
		},
		PansionInfo: models.PansionInfo{
			ID:   "PT_RO",
			Name: "Без питания",
		},
		CancellationInfo: models.CancellationInfoPartner{
			Refundable: true,
			Penalties: []models.Penalty{
				{
					StartsAt: nil,
					EndsAt:   ptr.String("2022-07-19T12:00:00"),
					Type:     "NO_PENALTY",
					Amount:   nil,
					Currency: nil,
				},
				{
					StartsAt: ptr.String("2022-07-19T12:00:00"),
					EndsAt:   nil,
					Type:     "FULL_PRICE",
					Amount:   nil,
					Currency: nil,
				},
			},
			Highlighted: false,
		},
		CancellationInfoUnfiltered: models.CancellationInfoPartner{},
		LegalInfoGroup:             models.LegalInfoGroup{},
		PartnerID:                  "PI_TRAVELLINE",
		DirectPartner:              true,
		PromoCampaigns:             models.PromoCampaigns{},
	},
	AllGuestsRequired: false,
	DeferredPaymentSchedule: models.DeferredPaymentSchedule{
		DeferredPayments: []models.HotelPayment{
			{
				Amount: models.AmountValue{
					Currency: "RUB",
					Value:    3360,
				},
				PaymentEndsAt: date,
				PenaltyIfUnpaid: models.AmountValue{
					Currency: "RUB",
					Value:    0,
				},
				Percentage: 0,
			},
		},
		InitialPayment: models.HotelPayment{
			Amount: models.AmountValue{
				Currency: "RUB",
				Value:    0,
			},
			PaymentEndsAt:   time.Time{},
			PenaltyIfUnpaid: models.AmountValue{},
			Percentage:      0,
		},
		ZeroFirstPayment: false,
	},
	ExtraVisitAndUserParams: models.ExtraVisitAndUserParams{},
	RefundInfo: models.CancellationInfo{
		HasFreeCancellation: true,
		RefundRules: []models.RefundRules{
			{
				EndsAt:   &date,
				Penalty:  nil,
				StartsAt: nil,
				Type:     "FULLY_REFUNDABLE",
			},
			{
				EndsAt:   nil,
				Penalty:  nil,
				StartsAt: &date,
				Type:     "NON_REFUNDABLE",
			},
		},
		RefundType: "FULLY_REFUNDABLE",
	},
}
