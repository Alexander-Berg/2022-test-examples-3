package testdata

import (
	"a.yandex-team.ru/travel/app/backend/internal/lib/travelapiclient/models"
)

var GetHotelImagesResponse = models.GetHotelImagesResponse{
	TotalImageCount: 306,
	Images: []models.Image{
		{
			ID:          "urn:yandex:sprav:photo:6221595-2a0000018042a24fa7b7f218389b79796adb",
			URLTemplate: "https://avatars.mds.yandex.net/get-altay/6221595/2a0000018042a24fa7b7f218389b79796adb/%s",
			Sizes: []models.Size{
				{
					Identifier: "XS",
					Width:      100,
					Height:     66,
				},
				{
					Identifier: "orig",
					Width:      2048,
					Height:     1351,
				},
			},
			Tags: []string{},
		},
		{
			ID:          "urn:yandex:sprav:photo:2378041-2a0000017526d61fdc3279ce161af20d2cea",
			URLTemplate: "https://avatars.mds.yandex.net/get-altay/2378041/2a0000017526d61fdc3279ce161af20d2cea/%s",
			Sizes: []models.Size{
				{
					Identifier: "XL",
					Width:      800,
					Height:     534,
				},
			},
			Tags: []string{},
		},
	},
}
