package handler

import (
	"context"
	"testing"

	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc/metadata"

	"a.yandex-team.ru/library/go/core/log/nop"
	hotelsApi "a.yandex-team.ru/travel/app/backend/api/hotels/v1"
	service "a.yandex-team.ru/travel/app/backend/internal/hotels"
	"a.yandex-team.ru/travel/app/backend/internal/lib/travelapiclient/models"
)

type travelAPIClientMock struct {
	mock.Mock
}

func (c *travelAPIClientMock) GetHotelReviews(ctx context.Context, req *models.GetHotelReviewsReq) (*models.GetHotelReviewsRsp, error) {
	args := c.Called(ctx, req)
	return args.Get(0).(*models.GetHotelReviewsRsp), args.Error(1)
}

func (c *travelAPIClientMock) AddHotelReview(ctx context.Context, request *models.AddHotelReviewReq) (*models.HotelReviewRsp, error) {
	panic("implement me")
}

func (c *travelAPIClientMock) EditHotelReview(ctx context.Context, request *models.EditHotelReviewReq) (*models.HotelReviewRsp, error) {
	panic("implement me")
}

func (c *travelAPIClientMock) DeleteHotelReview(ctx context.Context, request *models.DeleteHotelReviewReq) error {
	panic("implement me")
}

func (c *travelAPIClientMock) SetReactionHotelReview(ctx context.Context, request *models.SetReactionHotelReviewReq) error {
	panic("implement me")
}

func (c *travelAPIClientMock) UploadImageHotelReview(ctx context.Context, request *models.UploadImageHotelReviewReq) (*models.UploadImageHotelReviewRsp, error) {
	panic("implement me")
}

func (c *travelAPIClientMock) DeleteImagesHotelReview(ctx context.Context, request *models.DeleteImagesHotelReviewReq) error {
	panic("implement me")
}

type translationServiceForTests struct{}

func (t translationServiceForTests) GetKeysets() map[string][]string {
	return map[string][]string{}
}

func (translationServiceForTests) Get(ctx context.Context, key service.TranslationKey) string {
	return ""
}
func (translationServiceForTests) GetFunc(ctx context.Context) func(key service.TranslationKey) string {
	return func(key service.TranslationKey) string {
		return ""
	}
}

func TestGetReviews_WithEmtyReq(t *testing.T) {
	ctx := prepareLanguageContext(context.Background(), "ru-RU")
	slug := "xxx"
	tcMock := new(travelAPIClientMock)
	req := &models.GetHotelReviewsReq{
		QueryData: models.GetHotelInfoQueryData{
			HotelSlug: slug,
		},
		PagingParams: &models.PagingParams{
			Offset: 0,
			Limit:  10,
		},
		Sort: "byRelevanceOrg",
	}
	rsp := models.GetHotelReviewsRsp{
		ReviewsInfo: models.ReviewsInfo{
			TextReviews: []models.TextReview{
				{
					ID:        "vD6OlCW0oRzZiYWw6eFS4nvjEcUaJ0_o6",
					Text:      "Расположение гостиницы супер, рядом метро и парк ВДНХ. Бронировал номер с помощью букинга, выбрал номер получше",
					UpdatedAt: "2019-09-01T12:29:03.126Z",
					Author: models.Author{
						Name:              "Вася Пупкин",
						Level:             "Знаток города 9 уровня",
						AvatarURLTemplate: "https://avatars.mds.yandex.net/get-yapic/61207/NeAYqTNIs8ajmGdWu0cNRgnCjFk-1/{size}",
						ProfileURL:        "https://reviews.yandex.ru/user/w6zt2gp7fazyhx6xpgk1en0nhw",
					},
					Rating:       2,
					UserReaction: "NONE",
					Moderation: &models.Moderation{
						Status: "ACCEPTED",
					},
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
					CommentCount:      3,
					BusinessComment:   "",
					TotalLikeCount:    1,
					TotalDislikeCount: 2,
					Snippet:           "Расположение гостиницы супер, рядом метро и парк ВДНХ. Бронировал номер с помощью букинга, выбрал номер...",
				},
			},
			UserTextReview: &models.TextReview{
				ID:        "vD6OlCW0oRzZiYWw6eFS4nvjEcUaJ0_o6",
				Text:      "Расположение гостиницы супер, рядом метро и парк ВДНХ. Бронировал номер с помощью букинга, выбрал номер получше",
				UpdatedAt: "2019-09-01T12:29:03.126Z",
				Author: models.Author{
					Name:              "Вася Пупкин",
					Level:             "Знаток города 9 уровня",
					AvatarURLTemplate: "https://avatars.mds.yandex.net/get-yapic/61207/NeAYqTNIs8ajmGdWu0cNRgnCjFk-1/{size}",
					ProfileURL:        "https://reviews.yandex.ru/user/w6zt2gp7fazyhx6xpgk1en0nhw",
				},
				Rating:       2,
				UserReaction: "NONE",
				Moderation: &models.Moderation{
					Status: "ACCEPTED",
				},
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
				CommentCount:      3,
				BusinessComment:   "",
				TotalLikeCount:    1,
				TotalDislikeCount: 2,
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
	tcMock.On("GetHotelReviews", ctx, req).Return(&rsp, nil)
	handler := NewGRPCHandler(&service.DefaultConfig.Reviews, &nop.Logger{}, tcMock, &translationServiceForTests{})

	request := hotelsApi.GetReviewsReq{
		HotelId: &hotelsApi.HotelID{
			Value: &hotelsApi.HotelID_HotelSlug{
				HotelSlug: slug,
			},
		},
	}
	response, err := handler.GetReviews(ctx, &request)
	require.NoError(t, err)

	require.Equal(t, &hotelsApi.ReviewsRsp{
		HasMore: true,
		Reviews: []*hotelsApi.ReviewRsp{
			{
				Id:                "vD6OlCW0oRzZiYWw6eFS4nvjEcUaJ0_o6",
				TotalLikeCount:    1,
				Snippet:           "Расположение гостиницы супер, рядом метро и парк ВДНХ. Бронировал номер с помощью букинга, выбрал номер...",
				Text:              "Расположение гостиницы супер, рядом метро и парк ВДНХ. Бронировал номер с помощью букинга, выбрал номер получше",
				TotalDislikeCount: 2,
				BusinessComment:   "",
				CommentCount:      3,
				Images: []*hotelsApi.ImageReview{
					{
						Id: "XPtEMAUYejfAPvEv8Q_qbe8Qt9gTM2",
						Image: &hotelsApi.Image{
							UrlTemplate: "https://avatars.mds.yandex.net/get-altay/5585693/2a0000017e263945b6b9384aa2c481869407/%s",
							SizeOrig:    nil,
						},
						Tags: nil,
						Moderation: &hotelsApi.Moderation{
							Status: hotelsApi.ModerationStatus_MODERATION_STATUS_ACCEPTED,
						},
					},
				},
				UserReaction: hotelsApi.ReviewReactionType_REVIEW_REACTION_NONE,
				Moderation: &hotelsApi.Moderation{
					Status: hotelsApi.ModerationStatus_MODERATION_STATUS_ACCEPTED,
				},
				Rating: 2,
				Author: &hotelsApi.Author{
					Name:       "Вася Пупкин",
					Level:      "Знаток города 9 уровня",
					ProfileUrl: "https://reviews.yandex.ru/user/w6zt2gp7fazyhx6xpgk1en0nhw",
					AvatarUrl:  "https://avatars.mds.yandex.net/get-yapic/61207/NeAYqTNIs8ajmGdWu0cNRgnCjFk-1/islands-75",
				},
				UpdatedAt: "2019-09-01T12:29:03.126Z",
				PhraseMatch: &hotelsApi.Keyphrasematch{
					Fragments: []*hotelsApi.Keyphrasematch_Fragments{
						{
							Position: 217,
							Size:     7,
						},
					},
				},
			},
		},
		Sort: &hotelsApi.ReviewQuickFilterSort{
			Selected: hotelsApi.ReviewSort_REVIEW_SORT_RELEVANT_FIRST,
			Available: []*hotelsApi.ReviewQuickSorter{
				{
					Id: hotelsApi.ReviewSort_REVIEW_SORT_RELEVANT_FIRST,
				},
				{
					Id: hotelsApi.ReviewSort_REVIEW_SORT_TIME_DESC,
				},
				{
					Id: hotelsApi.ReviewSort_REVIEW_SORT_RATING_ASC,
				},
				{
					Id: hotelsApi.ReviewSort_REVIEW_SORT_RATING_DESC,
				},
			},
		},
		TotalReviewCount: 310,
		UserReview: &hotelsApi.ReviewRsp{
			Id:                "vD6OlCW0oRzZiYWw6eFS4nvjEcUaJ0_o6",
			TotalLikeCount:    1,
			Snippet:           "Расположение гостиницы супер, рядом метро и парк ВДНХ. Бронировал номер с помощью букинга, выбрал номер...",
			Text:              "Расположение гостиницы супер, рядом метро и парк ВДНХ. Бронировал номер с помощью букинга, выбрал номер получше",
			TotalDislikeCount: 2,
			BusinessComment:   "",
			CommentCount:      3,
			Images: []*hotelsApi.ImageReview{
				{
					Id: "XPtEMAUYejfAPvEv8Q_qbe8Qt9gTM2",
					Image: &hotelsApi.Image{
						UrlTemplate: "https://avatars.mds.yandex.net/get-altay/5585693/2a0000017e263945b6b9384aa2c481869407/%s",
						SizeOrig:    nil,
					},
					Tags: nil,
					Moderation: &hotelsApi.Moderation{
						Status: hotelsApi.ModerationStatus_MODERATION_STATUS_ACCEPTED,
					},
				},
			},
			UserReaction: hotelsApi.ReviewReactionType_REVIEW_REACTION_NONE,
			Moderation: &hotelsApi.Moderation{
				Status: hotelsApi.ModerationStatus_MODERATION_STATUS_ACCEPTED,
			},
			Rating: 2,
			Author: &hotelsApi.Author{
				Name:       "Вася Пупкин",
				Level:      "Знаток города 9 уровня",
				ProfileUrl: "https://reviews.yandex.ru/user/w6zt2gp7fazyhx6xpgk1en0nhw",
				AvatarUrl:  "https://avatars.mds.yandex.net/get-yapic/61207/NeAYqTNIs8ajmGdWu0cNRgnCjFk-1/islands-75",
			},
			UpdatedAt: "2019-09-01T12:29:03.126Z",
			PhraseMatch: &hotelsApi.Keyphrasematch{
				Fragments: []*hotelsApi.Keyphrasematch_Fragments{
					{
						Position: 217,
						Size:     7,
					},
				},
			},
		},
		Phrases: &hotelsApi.ReviewQuickFilterPhrase{
			Available: []*hotelsApi.Keyphrase{
				{
					Name:        "вднх",
					ReviewCount: 875,
				},
			},
			Selected:   "",
			TotalCount: 10,
		},
	}, response)
}

func prepareLanguageContext(ctx context.Context, acceptLanguageValue string) context.Context {
	md := metadata.MD{
		"grpcgateway-accept-language": []string{acceptLanguageValue},
	}
	return metadata.NewIncomingContext(ctx, md)
}
