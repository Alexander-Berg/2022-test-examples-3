package testdata

import "a.yandex-team.ru/travel/app/backend/internal/lib/travelapiclient/models"

var GetHotelReviewsRsp = models.GetHotelReviewsRsp{
	ReviewsInfo: models.ReviewsInfo{
		TextReviews: []models.TextReview{
			{
				ID:        "vD6OlCW0oRzZiYWw6eFS4nvjEcUaJ0_o6",
				Text:      "Расположение гостиницы супер, рядом метро и парк ВДНХ.",
				UpdatedAt: "2019-09-01T12:29:03.126Z",
				Author: models.Author{
					Name:              "Вася Пупкин",
					Level:             "Знаток города 9 уровня",
					AvatarURLTemplate: "https://avatars.mds.yandex.net/get-yapic/61207/NeAYqTNIs8ajmGdWu0cNRgnCjFk-1/{size}",
					ProfileURL:        "https://reviews.yandex.ru/user/w6zt2gp7fazyhx6xpgk1en0nhw",
				},
				Rating:       2,
				UserReaction: "NONE",
				Moderation:   nil,
				Images: []models.Image{
					{
						ID:          "XPtEMAUYejfAPvEv8Q_qbe8Qt9gTM2",
						URLTemplate: "https://avatars.mds.yandex.net/get-altay/5585693/2a0000017e263945b6b9384aa2c481869407/{size}",
						Moderation: &models.Moderation{
							Status: "ACCEPTED",
						},
						Tags:  nil,
						Sizes: nil,
					},
				},
				KeyPhraseMatch: &models.KeyPhraseMatch{
					Fragments: []models.Fragments{
						{
							Position: 217,
							Size:     7,
						},
					},
				},
				CommentCount:      0,
				BusinessComment:   "",
				TotalLikeCount:    0,
				TotalDislikeCount: 0,
				Snippet:           "Расположение гостиницы супер, рядом метро и парк ВДНХ. Бронировал номер с помощью букинга, выбрал номер...",
			},
		},
		UserTextReview: &models.TextReview{
			ID:        "vD6OlCW0oRzZiYWw6eFS4nvjEcUaJ0_o6",
			Text:      "Расположение гостиницы супер, рядом метро и парк ВДНХ.",
			UpdatedAt: "2019-09-01T12:29:03.126Z",
			Author: models.Author{
				Name:              "Вася Пупкин",
				Level:             "Знаток города 9 уровня",
				AvatarURLTemplate: "https://avatars.mds.yandex.net/get-yapic/61207/NeAYqTNIs8ajmGdWu0cNRgnCjFk-1/{size}",
				ProfileURL:        "https://reviews.yandex.ru/user/w6zt2gp7fazyhx6xpgk1en0nhw",
			},
			Rating:       2,
			UserReaction: "NONE",
			Moderation:   nil,
			Images: []models.Image{
				{
					ID:          "XPtEMAUYejfAPvEv8Q_qbe8Qt9gTM2",
					URLTemplate: "https://avatars.mds.yandex.net/get-altay/5585693/2a0000017e263945b6b9384aa2c481869407/{size}",
					Moderation: &models.Moderation{
						Status: "ACCEPTED",
					},
					Tags:  nil,
					Sizes: nil,
				},
			},
			KeyPhraseMatch: &models.KeyPhraseMatch{
				Fragments: []models.Fragments{
					{
						Position: 217,
						Size:     7,
					},
				},
			},
			CommentCount:      0,
			BusinessComment:   "",
			TotalLikeCount:    0,
			TotalDislikeCount: 0,
			Snippet:           "Расположение гостиницы супер, рядом метро и парк ВДНХ. Бронировал номер с помощью букинга, выбрал номер...",
		},
		KeyPhrases: []models.KeyPhrases{
			{
				Name:        "вднх",
				ReviewCount: 875,
			},
		},
		TotalTextReviewCount: 310,
		TotalKeyPhraseCount:  10,
		HasMore:              true,
	},
}
