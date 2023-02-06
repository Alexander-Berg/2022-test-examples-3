package testdata

import (
	"a.yandex-team.ru/library/go/ptr"
	"a.yandex-team.ru/travel/app/backend/internal/lib/travelapiclient/models"
)

var timeout = models.PaymentErrorCodePaymentTimeout
var GetHotelHappyPage = models.GetHotelHappyPageRsp{
	GetHappyPageRsp: models.GetHappyPageRsp{
		CrossSale: models.CrossSaleBlocks{
			Blocks: []models.CrossSaleBlock{
				{
					Order:     1,
					BlockType: "PROMO",
					UIPayload: models.UIPayload{
						AdFoxID: "1",
					},
				},
				{
					Order:     4,
					BlockType: "TRANSPORT_CROSS_SALE",
					UIPayload: models.UIPayload{
						AdFoxID: "",
					},
				},
			},
		},
		OrderType: "HOTEL",
	},
	Order: models.HotelOrder{
		ID:            "7dd0c1f5-bbb1-4105-b1fb-f5ebe2837d9f",
		YandexOrderID: "YA-5637-9529-2429",
		OrderInfo: models.HotelOrderInfo{
			TravelToken: "Y5GMIC2nIKgjCyeAo5veoIAbbWlywtxCWdaz0eQIFgI0UAPDyaFiLaOrdskIK7DGGRc5vdi1AGZsT45JX5i7NYyKDLDTdPbANCT944eG-ezAn9TXDPikvfTWCYb-gcHlW8G8R75ZbvxwsH5mVMLzaqWe4K3iuTW-6G7ehig=",
			Label:       "",
			Checksum:    "sbo8ZmGxn3fGtwEPXeXV9w==",
			SessionKey:  "5a5d9620-dd37-4bd0-9d26-c5e015e0d187",
			BasicHotelInfo: models.HotelCheckout{
				HotelBase: models.HotelBase{
					Permalink:     1.002445907e+09,
					Slug:          "moscow/vega-izmailovo",
					Name:          "Вега Измайлово",
					Coordinates:   models.Coordinates{},
					Address:       "Россия, Москва, Измайловское шоссе, 71, корп. 3В",
					Stars:         ptr.Int(4),
					Rating:        5,
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
				ImageURLTemplate: "https://avatars.mds.yandex.net/get-altay/758053/2a00000161571db25bde344add504bfc30ac/%s",
				LocationType:     "MOSCOW",
				Phone:            "8 (800) 550-41-85",
				WorkingHours:     "ежедневно, круглосуточно",
				LegalInfo: models.LegalInfo{
					Inn:     "",
					Ogrn:    "1027739012003",
					Address: "105613, МОСКВА ГОРОД, ШОССЕ ИЗМАЙЛОВСКОЕ, ДОМ 71, КОРПУС 3В, ЭТАЖ 3 ПОМЕЩЕНИЕ 26",
				},
			},
			PartnerHotelInfo: models.HotelPartner{
				Address: models.Address{
					City:        "г. Москва",
					CountryCode: "105613",
					Line1:       "Измайловское шоссе, д. 71 корп. 3В",
					Line2:       "",
				},
				Amenities: map[string]models.Feature{},
				Checkin: models.Checkin{
					BeginTime:           "14:00",
					EndTime:             "",
					Instructions:        "",
					SpecialInstructions: "",
				},
				Checkout: models.Checkout{
					Time: "12:00",
				},
				Descriptions: models.Descriptions{},
				Fees: models.Fees{
					Mandatory: "<p>В стоимость включены следующие услуги:<ul><li><p><b>Регистрация в программе лояльности</b><br/>Вы становитесь участником программы лояльности VEGA Клуб. Получить карту и  ознакомиться с условиями программы вы можете на ресепшене отеля.</p></li><li><p><b>Приветственный напиток</b><br/>Приветственный напиток–шампанское или  лимонад доступен для вас по вашему желанию в зоне VIP регистрации.</p></li><li><p><b>Business access</b><br/>Воспользуйтесь рабочим пространством с бесплатным интернетом и возможностью работы с документами (печать, сканирование, копирование).</p></li></ul></p>",
					Optional:  "",
				},
				Images: []models.ImagesLinks{
					{
						Links: map[string]models.ImageLink{
							"350px": {
								Href:   "https://www.travelline.ru/resource/images/p/742/636330532834385162-7e6254ba-da34-44f8-946e-4e82793a8ff7",
								Method: "GET",
							},
						},
					},
				},
				Location:   models.Location{},
				Name:       "\"Вега Измайлово\" Отель и Конгресс-Центр",
				Phone:      "+7 (800) 600-43-68",
				Policies:   models.Policies{},
				PropertyID: "742",
				Ratings: models.Ratings{
					Property: models.Property{
						Rating: "4.0",
						Type:   "Star",
					},
				},
			},
			PartnerRoomInfo: models.RoomPartner{
				Amenities: map[string]models.FeaturePartner{
					"0": {
						Name: "цифровое тв",
					},
					"44.0": {
						Name: "цифровое тв",
					},
				},
				Descriptions: models.Descriptions{
					Overview: "<p>Апартамент Смарт, выполненный в урбанистическом стиле и в серо-черной цветовой гамме с яркими красными акцентами, станет полноценной заменой дома в длительной поездке для вас и вашей семьи.</p><p>В вашем распоряжении: просторная прихожая, гостиная, спальня, зона для отдыха, оборудованная кухня, большая ванная комната с современной душевой и ванной.</p><p>Одна большая кровать 180 х 200 и диван трансформер.<br/>Проживание ребёнка на месте родителей до 7 лет - бесплатно.</p><p>Расположение: 24 и 27 этаж.</p><p>К Вашим услугам:<br/>• VIP-размещение<br/>• LCD – телевизор с функцией SMART-TV<br/>• Док-станция с функцией подзарядки мобильных устройств на Android и iOS и воспроизведения музыки, а так же smart - часы, помогающие соблюдать режим сна<br/>• Халаты в номере<br/>• Косметика в ванной комнате бренда Hotel collection<br/>• Бесплатный беспроводной интернет Wi-Fi <br/>• Бесплатное посещение фитнес-центра<br/>• Бесплатная чистка обуви <br/>• Услуги прачечной и химчистки по запросу<br/>• Заказ еды и напитков в номер по меню \"Рум-сервис\" круглосуточно <br/>• Кулер с горячей и холодной водой на этаже</p>",
				},
				Images: []models.ImagesLinks{
					{
						Links: map[string]models.ImageLink{
							"350px": {
								Href:   "https://www.travelline.ru/resource/images/rt/98996/636995893738150695-fea23d3f-2f4d-4449-aeb4-5d831fa0b00e",
								Method: "GET",
							},
						},
					},
				},
				Name: "Апартамент Смарт [229179] 'VEGA Клуб'",
			},
			BedGroups: []models.BedGroupsCheckout{
				{
					Description: "двуспальная кровать",
					ID:          "0",
				},
			},
			RequestInfo: models.RequestInfo{
				SearchParams: models.SearchParams{
					Adults:       2,
					CheckinDate:  "2022-04-06",
					CheckoutDate: "2022-04-08",
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
								Amount:   "8550.00",
								Currency: "RUB",
							},
							TaxesAndFees: nil,
						},
						{
							Base: models.Amount{
								Amount:   "8550.00",
								Currency: "RUB",
							},
							TaxesAndFees: nil,
						},
					},
					Totals: models.Totals{
						Base: models.Amount{
							Amount:   "17100",
							Currency: "RUB",
						},
						Discount: models.Amount{},
						Grand: models.Amount{
							Amount:   "17100",
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
						EndsAt:   ptr.String("2022-04-06T11:00:00"),
						Type:     "NO_PENALTY",
						Amount:   nil,
						Currency: nil,
					},
					{
						StartsAt: ptr.String("2022-04-06T11:00:00"),
						EndsAt:   nil,
						Type:     "FULL_PRICE",
						Amount:   nil,
						Currency: nil,
					},
				},
				Highlighted: true,
			},
			CancellationInfoUnfiltered: models.CancellationInfoPartner{
				Refundable: true,
				Penalties: []models.Penalty{
					{
						StartsAt: ptr.String("2022-04-04T19:30:00"),
						EndsAt:   ptr.String("2022-04-06T11:00:00"),
						Type:     "NO_PENALTY",
						Amount:   nil,
						Currency: nil,
					},
					{
						StartsAt: ptr.String("2022-04-06T11:00:00"),
						EndsAt:   ptr.String("2022-04-06T23:59:59"),
						Type:     "FULL_PRICE",
						Amount:   nil,
						Currency: nil,
					},
				},
				Highlighted: true,
			},
			LegalInfoGroup: models.LegalInfoGroup{
				Hotel: models.LegalInfoPartner{
					ActualAddress:  "Россия, Москва, Измайловское шоссе, 71, корп. 3В",
					LegalAddress:   "105613, МОСКВА ГОРОД, ШОССЕ ИЗМАЙЛОВСКОЕ, ДОМ 71, КОРПУС 3В, ЭТАЖ 3 ПОМЕЩЕНИЕ 26",
					Name:           "АО ТГК \"ВЕГА\"",
					Ogrn:           "1027739012003",
					RegistryNumber: "",
					WorkingHours:   "ежедневно, круглосуточно",
				},
				Partner: models.LegalInfoPartner{
					ActualAddress:  "",
					LegalAddress:   "119021, Россия, г. Москва, ул. Льва Толстого, д. 16",
					Name:           "Сервис предоставляет ООО «Яндекс»",
					Ogrn:           "1027700229193",
					RegistryNumber: "",
					WorkingHours:   "пн-пт: с 9:00 до 20:00 по местному времени, сб и вс: выходной",
				},
				Yandex: models.LegalInfoPartner{
					ActualAddress:  "",
					LegalAddress:   "119021, Россия, г. Москва, ул. Льва Толстого, д. 16",
					Name:           "Сервис предоставляет ООО «Яндекс»",
					Ogrn:           "1027700229193",
					RegistryNumber: "",
					WorkingHours:   "пн-пт: с 9:00 до 20:00 по местному времени, сб и вс: выходной",
				},
			},
			PartnerID:     "PI_TRAVELLINE",
			DirectPartner: true,
			PromoCampaigns: models.PromoCampaigns{
				Mir2020: models.Mir2020{
					CashbackAmount:       3420,
					CashbackAmountString: "3 420 ₽",
					Eligible:             true,
				},
				Taxi2020: models.Taxi2020{
					Eligible: false,
				},
				WhiteLabel: models.WhiteLabel{
					Eligible: false,
					Points: models.Points{
						Amount:     0,
						PointsName: "",
						PointsType: "",
					},
				},
				YandexEda: models.YandexEda{
					Data:     models.Data{},
					Eligible: false,
				},
				YandexPlus: models.YandexPlus{
					Eligible:       false,
					Points:         1710,
					WithdrawPoints: 0,
				},
			},
		},
		GuestInfo: models.HotelGuestInfo{
			CustomerEmail:      "s.khanin.test@yandex.ru",
			CustomerPhone:      "+79655127667",
			AllowsSubscription: false,
			Guests: []models.HotelGuest{
				{
					FirstName: "Khanin",
					LastName:  "Semen",
					Empty:     false,
				},
				{
					FirstName: "ккк",
					LastName:  "пппп",
					Empty:     false,
				},
			},
		},
		Status: "PAYMENT_FAILED",
		Payment: models.PaymentReceipts{
			Receipts:             []models.Receipt{},
			ErrorInfo:            &timeout,
			UsesDeferredPayments: false,
		},
		ConfirmationInfo:      models.ConfirmationInfo{},
		RefundInfo:            models.RefundInfo{},
		PromoCampaignsApplied: models.PromoCampaignsApplied{},
	},
}
