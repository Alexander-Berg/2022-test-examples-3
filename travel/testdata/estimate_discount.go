package testdata

import "a.yandex-team.ru/travel/app/backend/internal/lib/travelapiclient/models"

var EstimateDiscountResponse = models.EstimateDiscountResponse{
	CodeApplicationResults: []models.CodeApplicationResult{
		{
			Code: "SUCCESS",
			DiscountAmount: models.DiscountAmount{
				Currency: "RUB",
				Value:    500,
			},
			Type: "SUCCESS",
		},
	},
	OriginalAmount: models.DiscountAmount{
		Currency: "RUB",
		Value:    3360,
	},
	DiscountedAmount: models.DiscountAmount{
		Currency: "RUB",
		Value:    2860,
	},
	DiscountAmount: models.DiscountAmount{
		Currency: "RUB",
		Value:    500,
	},
}
