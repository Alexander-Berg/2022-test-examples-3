package handler

import (
	"context"
	"fmt"
	"net/http"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"google.golang.org/genproto/googleapis/rpc/errdetails"
	"google.golang.org/genproto/googleapis/type/date"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/metadata"
	"google.golang.org/grpc/status"
	"google.golang.org/protobuf/types/known/timestamppb"

	"a.yandex-team.ru/library/go/core/log/nop"
	"a.yandex-team.ru/library/go/yandex/blackbox"
	travelersAPI "a.yandex-team.ru/travel/app/backend/api/travelers/v1"
	"a.yandex-team.ru/travel/app/backend/internal/common"
	"a.yandex-team.ru/travel/app/backend/internal/l10n"
	"a.yandex-team.ru/travel/app/backend/internal/lib/travelersclient"
	"a.yandex-team.ru/travel/app/backend/internal/travelers"
)

type travelersClientMock struct {
	mock.Mock
}

func (c *travelersClientMock) GetVersion(ctx context.Context) (string, error) {
	panic("implement me")
}

func (c *travelersClientMock) ListDocumentTypes(ctx context.Context) (*travelersclient.DocumentTypes, error) {
	args := c.Called(ctx)
	return args.Get(0).(*travelersclient.DocumentTypes), args.Error(1)
}

func (c *travelersClientMock) GetTraveler(ctx context.Context, uid string) (*travelersclient.Traveler, error) {
	args := c.Called(ctx, uid)
	return args.Get(0).(*travelersclient.Traveler), args.Error(1)
}

func (c *travelersClientMock) CreateOrUpdateTraveler(ctx context.Context, uid string, traveler *travelersclient.EditableTraveler) (*travelersclient.Traveler, error) {
	args := c.Called(ctx, uid, traveler)
	return args.Get(0).(*travelersclient.Traveler), args.Error(1)
}

func (c *travelersClientMock) ListPassengers(ctx context.Context, uid string, includeDocuments bool, includeCards bool) ([]travelersclient.Passenger, error) {
	args := c.Called(ctx, uid, includeDocuments, includeCards)
	return args.Get(0).([]travelersclient.Passenger), args.Error(1)
}

func (c *travelersClientMock) ListDocuments(ctx context.Context, uid string, passengerID string) ([]travelersclient.Document, error) {
	panic("implement me")
}

func (c *travelersClientMock) GetDocument(ctx context.Context, uid string, passengerID string, documentID string) (*travelersclient.Document, error) {
	args := c.Called(ctx, uid, passengerID, documentID)
	return args.Get(0).(*travelersclient.Document), args.Error(1)
}

func (c *travelersClientMock) CreateDocument(ctx context.Context, uid string, passengerID string, document *travelersclient.CreateOrUpdateDocumentRequest) (*travelersclient.Document, error) {
	args := c.Called(ctx, uid, passengerID, document)
	return args.Get(0).(*travelersclient.Document), args.Error(1)
}

func (c *travelersClientMock) UpdateDocument(ctx context.Context, uid string, passengerID string, documentID string, document *travelersclient.CreateOrUpdateDocumentRequest) (*travelersclient.Document, error) {
	panic("implement me")
}

func (c *travelersClientMock) DeleteDocument(ctx context.Context, uid string, passengerID string, documentID string) error {
	panic("implement me")
}

func (c *travelersClientMock) GetPassenger(ctx context.Context, uid string, id string, includeDocuments bool, includeCards bool) (*travelersclient.Passenger, error) {
	panic("implement me")
}

func (c *travelersClientMock) CreatePassenger(ctx context.Context, uid string, passenger *travelersclient.CreateOrUpdatePassengerRequest) (*travelersclient.Passenger, error) {
	args := c.Called(ctx, uid, passenger)
	return args.Get(0).(*travelersclient.Passenger), args.Error(1)
}

func (c *travelersClientMock) UpdatePassenger(ctx context.Context, uid string, id string, passenger *travelersclient.CreateOrUpdatePassengerRequest) (*travelersclient.Passenger, error) {
	panic("implement me")
}

func (c *travelersClientMock) DeletePassenger(ctx context.Context, uid string, id string) error {
	panic("implement me")
}

func (c *travelersClientMock) ListBonusCards(ctx context.Context, uid, passengerID string) ([]travelersclient.BonusCard, error) {
	args := c.Called(ctx, uid, passengerID)
	return args.Get(0).([]travelersclient.BonusCard), args.Error(1)
}

func (c *travelersClientMock) GetBonusCard(ctx context.Context, uid, passengerID, bonusCardID string) (*travelersclient.BonusCard, error) {
	args := c.Called(ctx, uid, passengerID, bonusCardID)
	return args.Get(0).(*travelersclient.BonusCard), args.Error(1)
}

func (c *travelersClientMock) CreateBonusCard(ctx context.Context, uid, passengerID string, bonusCard *travelersclient.EditableBonusCard) (*travelersclient.BonusCard, error) {
	args := c.Called(ctx, uid, passengerID, bonusCard)
	return args.Get(0).(*travelersclient.BonusCard), args.Error(1)
}

func (c *travelersClientMock) UpdateBonusCard(ctx context.Context, uid, passengerID, bonusCardID string, bonusCard *travelersclient.EditableBonusCard) (*travelersclient.BonusCard, error) {
	args := c.Called(ctx, uid, passengerID, bonusCardID, bonusCard)
	return args.Get(0).(*travelersclient.BonusCard), args.Error(1)
}

func (c *travelersClientMock) DeleteBonusCard(ctx context.Context, uid, passengerID, bonusCardID string) error {
	args := c.Called(ctx, uid, passengerID, bonusCardID)
	return args.Error(0)
}

type travelersCacheMock struct {
	mock.Mock
}

func (c *travelersCacheMock) GetFieldsData() travelers.FieldsData {
	args := c.Called()
	return args.Get(0).(travelers.FieldsData)
}

func (c *travelersCacheMock) RunUpdater() {
	c.Called()
}

// travelersAdminCacheMock нужен для тестов, которые проверяют работу
// генерации описания полей пассажира, документов, бонусных карт.
// Для остальных тестов лучше пользоваться тестовой реализацией интерфейса AdminCache,
// приближенным к реальным данным - adminCacheForTests
type travelersAdminCacheMock struct {
	mock.Mock
}

func (a *travelersAdminCacheMock) GetTag() string {
	args := a.Called()
	return args.Get(0).(string)
}

func (a *travelersAdminCacheMock) GetVisualPassenger() travelers.VisualEntity {
	args := a.Called()
	return args.Get(0).(travelers.VisualEntity)
}

func (a *travelersAdminCacheMock) GetVisualDocumentsByType() map[string]travelers.VisualEntity {
	args := a.Called()
	return args.Get(0).(map[string]travelers.VisualEntity)
}

func (a *travelersAdminCacheMock) GetVisualBonusCardsByType() map[string]travelers.VisualEntity {
	args := a.Called()
	return args.Get(0).(map[string]travelers.VisualEntity)
}

func (a *travelersAdminCacheMock) GetTankerKeys() []string {
	return []string{}
}

// Реализация интерфейса AdminCache для тестов.
// Здесь прописываем приближенные к реальности данные.
// Для большинства тестов этого будет достаточно, потому что это настройки представления.
// Отличается от TravelersAdminCache тем, что данные не надо получать из админки.
type adminCacheForTests struct {
	visualPassenger        travelers.VisualEntity
	visualDocumentsByType  map[string]travelers.VisualEntity
	visualBonusCardsByType map[string]travelers.VisualEntity
	tag                    string
}

func newAdminCacheForTests() *adminCacheForTests {
	return &adminCacheForTests{
		tag: "some tag",
		visualPassenger: travelers.VisualEntity{
			VisualFieldDefinitions: map[string]travelers.VisualFieldDefinition{
				"birth_date":                  {Type: "date"},
				"email":                       {Type: "string"},
				"gender":                      {Type: "gender"},
				"itn":                         {Type: "string"},
				"phone":                       {Type: "string"},
				"phone_additional":            {Type: "string"},
				"title":                       {Type: "string"},
				"train_notifications_enabled": {Type: "bool"},
			},
			CreationGroupDefinitions: []travelers.GroupDefinition{
				{
					FieldNames: []string{
						"gender",
						"birth_date",
					},
				},
				{
					TitleTankerKey:    "passenger__creation_group__contacts__title",
					SubtitleTankerKey: "passenger__creation_group__contacts__subtitle",
					FieldNames: []string{
						"phone",
						"email",
					},
				},
			},
			EditGroupDefinitions: []travelers.GroupDefinition{
				{
					FieldNames: []string{
						"title",
						"gender",
						"birth_date",
						"itn",
						"train_notifications_enabled",
					},
				},
				{
					TitleTankerKey:    "passenger__edit_group__contacts__title",
					SubtitleTankerKey: "passenger__edit_group__contacts__subtitle",
					FieldNames: []string{
						"phone",
						"phone_additional",
						"email",
					},
				},
			},
		},
		visualDocumentsByType: map[string]travelers.VisualEntity{
			"other": {
				VisualFieldDefinitions: map[string]travelers.VisualFieldDefinition{
					"citizenship_geo_id": {Type: "country"},
					"expiration_date":    {Type: "date"},
					"first_name":         {Type: "string"},
					"first_name_en":      {Type: "string"},
					"issue_date":         {Type: "date"},
					"last_name":          {Type: "string"},
					"last_name_en":       {Type: "string"},
					"middle_name":        {Type: "string"},
					"middle_name_en":     {Type: "string"},
					"number":             {Type: "string"},
					"title":              {Type: "string"},
				},
				CreationGroupDefinitions: []travelers.GroupDefinition{
					{
						FieldNames: []string{
							"first_name",
							"middle_name",
							"last_name",
							"number",
							"citizenship_geo_id",
							"issue_date",
						},
					},
				},
				EditGroupDefinitions: []travelers.GroupDefinition{
					{
						FieldNames: []string{
							"title",
							"first_name",
							"middle_name",
							"last_name",
							"number",
							"citizenship_geo_id",
							"issue_date",
							"first_name_en",
							"middle_name_en",
							"last_name_en",
							"expiration_date",
						},
					},
				},
			},
			"ru_birth_certificate": {
				BackendFieldDefinitions: map[string]travelers.BackendFieldDefinition{
					"citizenship_geo_id": {Type: "country", DefaultValue: "225"},
				},
				VisualFieldDefinitions: map[string]travelers.VisualFieldDefinition{
					"first_name":     {Type: "string"},
					"first_name_en":  {Type: "string"},
					"issue_date":     {Type: "date"},
					"last_name":      {Type: "string"},
					"last_name_en":   {Type: "string"},
					"middle_name":    {Type: "string"},
					"middle_name_en": {Type: "string"},
					"number":         {Type: "string"},
					"title":          {Type: "string"},
				},
				CreationGroupDefinitions: []travelers.GroupDefinition{
					{
						FieldNames: []string{
							"last_name",
							"first_name",
							"middle_name",
							"number",
						},
					},
				},
				EditGroupDefinitions: []travelers.GroupDefinition{
					{
						FieldNames: []string{
							"title",
							"last_name",
							"first_name",
							"middle_name",
							"number",
							"last_name_en",
							"first_name_en",
							"middle_name_en",
							"issue_date",
						},
					},
				},
			},
			"ru_foreign_passport": {
				BackendFieldDefinitions: map[string]travelers.BackendFieldDefinition{
					"citizenship_geo_id": {Type: "country", DefaultValue: "225"},
				},
				VisualFieldDefinitions: map[string]travelers.VisualFieldDefinition{
					"expiration_date": {Type: "date"},
					"first_name_en":   {Type: "string"},
					"issue_date":      {Type: "date"},
					"last_name_en":    {Type: "string"},
					"middle_name_en":  {Type: "string"},
					"number":          {Type: "string", InputMask: "xx xxxxxxx"},
					"title":           {Type: "string"},
				},
				CreationGroupDefinitions: []travelers.GroupDefinition{
					{
						FieldNames: []string{
							"last_name_en",
							"first_name_en",
							"middle_name_en",
							"number",
						},
					},
				},
				EditGroupDefinitions: []travelers.GroupDefinition{
					{
						FieldNames: []string{
							"title",
							"last_name_en",
							"first_name_en",
							"middle_name_en",
							"number",
							"issue_date",
							"expiration_date",
						},
					},
				},
			},
			"ru_national_passport": {
				BackendFieldDefinitions: map[string]travelers.BackendFieldDefinition{
					"citizenship_geo_id": {Type: "country", DefaultValue: "225"},
				},
				VisualFieldDefinitions: map[string]travelers.VisualFieldDefinition{
					"first_name":     {Type: "string"},
					"first_name_en":  {Type: "string"},
					"issue_date":     {Type: "date"},
					"last_name":      {Type: "string"},
					"last_name_en":   {Type: "string"},
					"middle_name":    {Type: "string"},
					"middle_name_en": {Type: "string"},
					"number":         {Type: "string", InputMask: "xxxx xxxxxx"},
					"title":          {Type: "string"},
				},
				CreationGroupDefinitions: []travelers.GroupDefinition{
					{
						FieldNames: []string{
							"last_name",
							"first_name",
							"middle_name",
							"number",
						},
					},
				},
				EditGroupDefinitions: []travelers.GroupDefinition{
					{
						FieldNames: []string{
							"title",
							"last_name",
							"first_name",
							"middle_name",
							"last_name_en",
							"first_name_en",
							"middle_name_en",
							"number",
							"issue_date",
						},
					},
				},
			},
		},
		visualBonusCardsByType: map[string]travelers.VisualEntity{
			"rzd_bonus": {
				VisualFieldDefinitions: map[string]travelers.VisualFieldDefinition{
					"number": {
						Type: "string",
					},
					"title": {
						Type: "string",
					},
				},
			},
		},
	}
}

func (a *adminCacheForTests) GetTag() string {
	return a.tag
}

func (a *adminCacheForTests) GetVisualPassenger() travelers.VisualEntity {
	return a.visualPassenger
}

func (a *adminCacheForTests) GetVisualDocumentsByType() map[string]travelers.VisualEntity {
	return a.visualDocumentsByType
}

func (a *adminCacheForTests) GetVisualBonusCardsByType() map[string]travelers.VisualEntity {
	return a.visualBonusCardsByType
}

func (a *adminCacheForTests) GetTankerKeys() []string {
	return []string{}
}

type l10nServiceForTests struct{}

func newL10nServiceForTests() *l10nServiceForTests {
	return &l10nServiceForTests{}
}

func (*l10nServiceForTests) Get(keyset string, language string) (*l10n.Keyset, error) {
	if keyset != l10nKeyset || language != "ru" {
		panic("implement me")
	}
	return &l10n.Keyset{
		Name:     l10nKeyset,
		Tag:      "l10n-tag",
		Language: "ru",
		Keys: map[string]string{
			"document__other":                                "другой документ",
			"document__other__citizenship_geo_id":            "страна выдачи",
			"document__other__expiration_date":               "срок действия",
			"document__other__first_name":                    "имя",
			"document__other__first_name_en":                 "имя латиницей",
			"document__other__issue_date":                    "дата выдачи",
			"document__other__last_name":                     "фамилия",
			"document__other__last_name_en":                  "фамилия латиницей",
			"document__other__middle_name":                   "отчество",
			"document__other__middle_name_en":                "отчество латиницей",
			"document__other__number":                        "номер",
			"document__other__title":                         "название",
			"document__ru_birth_certificate":                 "свидетельство о рождении",
			"document__ru_birth_certificate__first_name":     "имя",
			"document__ru_birth_certificate__first_name_en":  "имя латиницей",
			"document__ru_birth_certificate__issue_date":     "дата выдачи",
			"document__ru_birth_certificate__last_name":      "фамилия",
			"document__ru_birth_certificate__last_name_en":   "фамилия латиницей",
			"document__ru_birth_certificate__middle_name":    "отчество",
			"document__ru_birth_certificate__middle_name_en": "отчество латиницей",
			"document__ru_birth_certificate__number":         "номер",
			"document__ru_birth_certificate__title":          "название",
			"document__ru_foreign_passport":                  "заграничный паспорт",
			"document__ru_foreign_passport__expiration_date": "срок действия",
			"document__ru_foreign_passport__first_name_en":   "имя",
			"document__ru_foreign_passport__issue_date":      "дата выдачи",
			"document__ru_foreign_passport__last_name_en":    "фамилия",
			"document__ru_foreign_passport__middle_name_en":  "отчество",
			"document__ru_foreign_passport__number":          "номер",
			"document__ru_foreign_passport__title":           "название",
			"document__ru_national_passport":                 "паспорт",
			"document__ru_national_passport__first_name":     "имя",
			"document__ru_national_passport__first_name_en":  "имя латиницей",
			"document__ru_national_passport__issue_date":     "дата выдачи",
			"document__ru_national_passport__last_name":      "фамилия",
			"document__ru_national_passport__last_name_en":   "фамилия латиницей",
			"document__ru_national_passport__middle_name":    "отчество",
			"document__ru_national_passport__middle_name_en": "отчество латиницей",
			"document__ru_national_passport__number":         "номер",
			"document__ru_national_passport__title":          "название",
			"bonus_card__avia_company_loyalty":               "бонусная карта авиакомпании",
			"bonus_card__avia_company_loyalty__number":       "номер",
			"bonus_card__avia_company_loyalty__title":        "название",
			"bonus_card__rzd_bonus":                          "РЖД бонус",
			"bonus_card__rzd_bonus__number":                  "номер",
			"bonus_card__rzd_bonus__title":                   "название",
			"bonus_card__universal_road":                     "дорожная карта",
			"bonus_card__universal_road__number":             "номер",
			"bonus_card__universal_road__title":              "название",
			"passenger__birth_date":                          "дата рождения",
			"passenger__email":                               "email",
			"passenger__gender":                              "пол",
			"passenger__itn":                                 "ИНН",
			"passenger__phone":                               "телефон",
			"passenger__phone_additional":                    "дополнительный телефон",
			"passenger__title":                               "название",
			"passenger__train_notifications_enabled":         "присылать уведомления от РЖД",
		},
	}, nil
}

func prepareAuthContext(ctx context.Context, uid uint64) context.Context {
	return context.WithValue(ctx, common.AuthMarker, common.AuthInfo{
		IP:            "1.1.1.1",
		Authenticated: true,
		User:          &blackbox.User{ID: uid},
		OAuthToken:    "some-token",
		UserTicket:    "some-user-ticket",
	})
}

func prepareLanguageContext(ctx context.Context, acceptLanguageValue string) context.Context {
	md := metadata.MD{
		"grpcgateway-accept-language": []string{acceptLanguageValue},
	}
	return metadata.NewIncomingContext(ctx, md)
}

func TestListFields(t *testing.T) {
	ctx := prepareLanguageContext(context.Background(), "ru-KZ")
	tcMock := new(travelersClientMock)
	tCacheMock := new(travelersCacheMock)
	tCacheMock.On("GetFieldsData").Return(travelers.FieldsData{
		Tag: "travelers-cache-tag",
		PassengerFields: map[string]travelers.FieldSettings{
			"title": {
				Required: true,
				Regex:    ".*",
				Unused:   false,
			},
		},
	})
	tAdminCacheMock := new(travelersAdminCacheMock)
	tAdminCacheMock.On("GetTag").Return("admin-cache-tag")
	tAdminCacheMock.On("GetVisualPassenger").Return(travelers.VisualEntity{
		VisualFieldDefinitions: map[string]travelers.VisualFieldDefinition{
			"title": {
				TankerKey: "passenger__title",
				Type:      "string",
			},
		},
		CreationGroupDefinitions: []travelers.GroupDefinition{{FieldNames: []string{"title"}}},
		EditGroupDefinitions:     []travelers.GroupDefinition{{FieldNames: []string{"title"}}},
	})
	tAdminCacheMock.On("GetVisualDocumentsByType").Return(map[string]travelers.VisualEntity{})
	tAdminCacheMock.On("GetVisualBonusCardsByType").Return(map[string]travelers.VisualEntity{})
	handler := NewGRPCTravelersHandler(&nop.Logger{}, tcMock, tCacheMock, tAdminCacheMock, newL10nServiceForTests())
	req := travelersAPI.ListFieldsReq{
		Tag: "old tag",
	}
	rsp, err := handler.ListFields(ctx, &req)

	require.NoError(t, err)
	require.Equal(t, "travelers-cache-tag_admin-cache-tag_l10n-tag", rsp.Tag)
	rspPassenger := rsp.GetPassengerGroups()
	expectedFields := travelersAPI.Field{
		Name:          "title",
		Type:          travelersAPI.FieldType_FIELD_TYPE_STRING,
		Title:         "название",
		Required:      true,
		OptionalRegex: &travelersAPI.Field_Regex{Regex: ".*"},
	}
	require.Len(t, rspPassenger.CreationGroups, 1)
	require.Len(t, rspPassenger.CreationGroups[0].Fields, 1)
	require.Equal(t, &expectedFields, rspPassenger.CreationGroups[0].Fields[0])
	require.Len(t, rspPassenger.EditGroups, 1)
	require.Len(t, rspPassenger.EditGroups[0].Fields, 1)
	require.Equal(t, &expectedFields, rspPassenger.EditGroups[0].Fields[0])
	tcMock.AssertExpectations(t)
	tCacheMock.AssertExpectations(t)
	tAdminCacheMock.AssertExpectations(t)
}

func TestListFields_SameTag(t *testing.T) {
	ctx := prepareLanguageContext(context.Background(), "ru-RU")
	tcMock := new(travelersClientMock)
	tCacheMock := new(travelersCacheMock)
	tCacheMock.On("GetFieldsData").Return(travelers.FieldsData{Tag: "travelers-cache-tag"})
	tAdminCacheMock := new(travelersAdminCacheMock)
	tAdminCacheMock.On("GetTag").Return("admin-cache-tag")
	handler := NewGRPCTravelersHandler(&nop.Logger{}, tcMock, tCacheMock, tAdminCacheMock, newL10nServiceForTests())
	req := travelersAPI.ListFieldsReq{
		Tag: "travelers-cache-tag_admin-cache-tag_l10n-tag",
	}
	rsp, err := handler.ListFields(ctx, &req)

	require.NoError(t, err)
	require.Equal(t, "travelers-cache-tag_admin-cache-tag_l10n-tag", rsp.Tag)
	require.Nil(t, rsp.PassengerGroups)
	require.Len(t, rsp.DocumentTypes, 0)
	require.Len(t, rsp.BonusCardTypes, 0)
	tcMock.AssertExpectations(t)
	tCacheMock.AssertExpectations(t)
	tAdminCacheMock.AssertExpectations(t)
}

func TestListPassengers(t *testing.T) {
	var uid uint64 = 100500
	uidStr := fmt.Sprintf("%d", uid)
	ctx := prepareAuthContext(context.Background(), uid)
	req := travelersAPI.ListPassengersReq{IncludeDetails: false}
	tcMock := new(travelersClientMock)
	tcMock.On("ListPassengers", ctx, uidStr, false, false).Return([]travelersclient.Passenger{
		{
			ID:        "1",
			CreatedAt: &travelersclient.Timestamp{Time: time.Unix(1635260000, 0)},
			UpdatedAt: &travelersclient.Timestamp{Time: time.Unix(1635262000, 0)},
			Fields: map[string]interface{}{
				"title":                       "А А А",
				"gender":                      "male",
				"birth_date":                  "1990-09-08",
				"itn":                         "123123123123",
				"phone":                       "+79041234567",
				"email":                       "a@example.com",
				"train_notifications_enabled": true,
			},
		},
	}, nil)
	tCacheMock := new(travelersCacheMock)
	handler := NewGRPCTravelersHandler(&nop.Logger{}, tcMock, tCacheMock, newAdminCacheForTests(), newL10nServiceForTests())

	rsp, err := handler.ListPassengers(ctx, &req)

	require.NoError(t, err)
	expectedPassenger := travelersAPI.Passenger{
		Id: "1",
		Fields: map[string]*travelersAPI.FieldValue{
			"title":                       {Value: &travelersAPI.FieldValue_StrValue{StrValue: "А А А"}},
			"gender":                      {Value: &travelersAPI.FieldValue_GenderValue{GenderValue: travelersAPI.Gender_GENDER_MALE}},
			"birth_date":                  {Value: &travelersAPI.FieldValue_DateValue{DateValue: &date.Date{Year: 1990, Month: 9, Day: 8}}},
			"phone":                       {Value: &travelersAPI.FieldValue_StrValue{StrValue: "+79041234567"}},
			"email":                       {Value: &travelersAPI.FieldValue_StrValue{StrValue: "a@example.com"}},
			"train_notifications_enabled": {Value: &travelersAPI.FieldValue_BoolValue{BoolValue: true}},
			"itn":                         {Value: &travelersAPI.FieldValue_StrValue{StrValue: "123123123123"}},
		},
		CreatedAt: &timestamppb.Timestamp{Seconds: 1635260000, Nanos: 0},
		UpdatedAt: &timestamppb.Timestamp{Seconds: 1635262000, Nanos: 0},
	}
	require.Len(t, rsp.Passengers, 1)
	require.Equal(t, &expectedPassenger, rsp.Passengers[0])
	tcMock.AssertExpectations(t)
}

func TestCreatePassengerWithDetails(t *testing.T) {
	ctx := prepareAuthContext(context.Background(), 100500)
	req := travelersAPI.CreatePassengerReq{
		Passenger: &travelersAPI.EditablePassenger{
			Fields: map[string]*travelersAPI.FieldValue{
				"title":                       {Value: &travelersAPI.FieldValue_StrValue{StrValue: "А А А"}},
				"gender":                      {Value: &travelersAPI.FieldValue_GenderValue{GenderValue: travelersAPI.Gender_GENDER_MALE}},
				"birth_date":                  {Value: &travelersAPI.FieldValue_DateValue{DateValue: &date.Date{Year: 1990, Month: 9, Day: 8}}},
				"phone":                       {Value: &travelersAPI.FieldValue_StrValue{StrValue: "+79041234567"}},
				"email":                       {Value: &travelersAPI.FieldValue_StrValue{StrValue: "a@example.com"}},
				"train_notifications_enabled": {Value: &travelersAPI.FieldValue_BoolValue{BoolValue: true}},
			},
			BonusCards: []*travelersAPI.EditableBonusCard{
				{
					Type: "rzd_bonus",
					Fields: map[string]*travelersAPI.FieldValue{
						"title":  {Value: &travelersAPI.FieldValue_StrValue{StrValue: "bc title"}},
						"number": {Value: &travelersAPI.FieldValue_StrValue{StrValue: "9001234567890"}},
					},
				},
			},
		},
	}
	uid := "100500"
	tcMock := new(travelersClientMock)
	tcMock.On("GetTraveler", ctx, uid).Return(&travelersclient.Traveler{
		Email:           "",
		Phone:           "",
		PhoneAdditional: "",
		Agree:           true,
		CreatedAt:       &travelersclient.Timestamp{Time: time.Unix(1635260000, 0)},
		UpdatedAt:       &travelersclient.Timestamp{Time: time.Unix(1635262000, 0)},
	}, nil)
	tcMock.On("CreatePassenger", ctx, uid, &travelersclient.CreateOrUpdatePassengerRequest{
		Fields: map[string]interface{}{
			"title":                       "А А А",
			"gender":                      "male",
			"birth_date":                  "1990-09-08",
			"phone":                       "+79041234567",
			"email":                       "a@example.com",
			"train_notifications_enabled": true,
		},
	}).Return(&travelersclient.Passenger{
		ID:        "1",
		CreatedAt: &travelersclient.Timestamp{Time: time.Unix(1635260000, 0)},
		UpdatedAt: &travelersclient.Timestamp{Time: time.Unix(1635262000, 0)},
		Fields: map[string]interface{}{
			"title":                       "А А А",
			"gender":                      "male",
			"birth_date":                  "1990-09-08",
			"phone":                       "+79041234567",
			"email":                       "a@example.com",
			"train_notifications_enabled": true,
		},
	}, nil)
	tcMock.On("CreateBonusCard", ctx, uid, "1", &travelersclient.EditableBonusCard{
		Type: "rzd_bonus",
		Fields: map[string]interface{}{
			"title":  "bc title",
			"number": "9001234567890",
		},
	}).Return(&travelersclient.BonusCard{
		ID:          "2",
		PassengerID: "1",
		Type:        "rzd_bonus",
		CreatedAt:   &travelersclient.Timestamp{Time: time.Unix(1635260000, 0)},
		UpdatedAt:   &travelersclient.Timestamp{Time: time.Unix(1635262000, 0)},
		Fields: map[string]interface{}{
			"title":  "bc title",
			"number": "9001234567890",
		},
	}, nil)
	tCacheMock := new(travelersCacheMock)
	handler := NewGRPCTravelersHandler(&nop.Logger{}, tcMock, tCacheMock, newAdminCacheForTests(), newL10nServiceForTests())

	rsp, err := handler.CreatePassenger(ctx, &req)

	require.NoError(t, err)
	expectedPassenger := travelersAPI.Passenger{
		Id: "1",
		Fields: map[string]*travelersAPI.FieldValue{
			"title":                       {Value: &travelersAPI.FieldValue_StrValue{StrValue: "А А А"}},
			"gender":                      {Value: &travelersAPI.FieldValue_GenderValue{GenderValue: travelersAPI.Gender_GENDER_MALE}},
			"birth_date":                  {Value: &travelersAPI.FieldValue_DateValue{DateValue: &date.Date{Year: 1990, Month: 9, Day: 8}}},
			"phone":                       {Value: &travelersAPI.FieldValue_StrValue{StrValue: "+79041234567"}},
			"email":                       {Value: &travelersAPI.FieldValue_StrValue{StrValue: "a@example.com"}},
			"train_notifications_enabled": {Value: &travelersAPI.FieldValue_BoolValue{BoolValue: true}},
		},
		CreatedAt: &timestamppb.Timestamp{Seconds: 1635260000, Nanos: 0},
		UpdatedAt: &timestamppb.Timestamp{Seconds: 1635262000, Nanos: 0},
		BonusCards: []*travelersAPI.BonusCard{
			{
				Id:          "2",
				PassengerId: "1",
				Type:        "rzd_bonus",
				Fields: map[string]*travelersAPI.FieldValue{
					"title":  {Value: &travelersAPI.FieldValue_StrValue{StrValue: "bc title"}},
					"number": {Value: &travelersAPI.FieldValue_StrValue{StrValue: "9001234567890"}},
				},
				CreatedAt: &timestamppb.Timestamp{Seconds: 1635260000, Nanos: 0},
				UpdatedAt: &timestamppb.Timestamp{Seconds: 1635262000, Nanos: 0},
			},
		},
	}
	require.Equal(t, &expectedPassenger, rsp.Passenger)
	require.Equal(t, uint32(0), rsp.NotSavedDocumentsCount)
	require.Equal(t, uint32(0), rsp.NotSavedBonusCardsCount)
	tcMock.AssertExpectations(t)
}

func TestCreatePassengerWithDetailsErrorCreatingPassenger(t *testing.T) {
	ctx := prepareAuthContext(context.Background(), 100500)
	req := travelersAPI.CreatePassengerReq{
		Passenger: &travelersAPI.EditablePassenger{
			Fields: map[string]*travelersAPI.FieldValue{
				"title":                       {Value: &travelersAPI.FieldValue_StrValue{StrValue: "А А А"}},
				"gender":                      {Value: &travelersAPI.FieldValue_GenderValue{GenderValue: travelersAPI.Gender_GENDER_MALE}},
				"birth_date":                  {Value: &travelersAPI.FieldValue_DateValue{DateValue: &date.Date{Year: 1990, Month: 9, Day: 8}}},
				"phone":                       {Value: &travelersAPI.FieldValue_StrValue{StrValue: "+79041234567"}},
				"email":                       {Value: &travelersAPI.FieldValue_StrValue{StrValue: "a@example.com"}},
				"train_notifications_enabled": {Value: &travelersAPI.FieldValue_BoolValue{BoolValue: true}},
			},
			BonusCards: []*travelersAPI.EditableBonusCard{
				{
					Type: "rzd_bonus",
					Fields: map[string]*travelersAPI.FieldValue{
						"title":  {Value: &travelersAPI.FieldValue_StrValue{StrValue: "bc title"}},
						"number": {Value: &travelersAPI.FieldValue_StrValue{StrValue: "9001234567890"}},
					},
				},
			},
		},
	}
	uid := "100500"
	tcMock := new(travelersClientMock)
	tcMock.On("GetTraveler", ctx, uid).Return(&travelersclient.Traveler{
		Email:           "",
		Phone:           "",
		PhoneAdditional: "",
		Agree:           true,
		CreatedAt:       &travelersclient.Timestamp{Time: time.Unix(1635260000, 0)},
		UpdatedAt:       &travelersclient.Timestamp{Time: time.Unix(1635262000, 0)},
	}, nil)
	tcMock.On("CreatePassenger", ctx, uid, &travelersclient.CreateOrUpdatePassengerRequest{
		Fields: map[string]interface{}{
			"title":                       "А А А",
			"gender":                      "male",
			"birth_date":                  "1990-09-08",
			"phone":                       "+79041234567",
			"email":                       "a@example.com",
			"train_notifications_enabled": true,
		},
	}).Return((*travelersclient.Passenger)(nil), travelersclient.StatusError{Status: http.StatusInternalServerError})
	tCacheMock := new(travelersCacheMock)
	handler := NewGRPCTravelersHandler(&nop.Logger{}, tcMock, tCacheMock, newAdminCacheForTests(), newL10nServiceForTests())

	_, err := handler.CreatePassenger(ctx, &req)

	var res interface{ GRPCStatus() *status.Status }
	if assert.ErrorAs(t, err, &res) {
		assert.Equal(t, codes.Unknown, res.GRPCStatus().Code())
	}
	tcMock.AssertExpectations(t)
}

func TestCreatePassengerWithDetailsErrorCreatingTraveler(t *testing.T) {
	ctx := prepareAuthContext(context.Background(), 100500)
	req := travelersAPI.CreatePassengerReq{
		Passenger: &travelersAPI.EditablePassenger{
			Fields: map[string]*travelersAPI.FieldValue{
				"title":                       {Value: &travelersAPI.FieldValue_StrValue{StrValue: "А А А"}},
				"gender":                      {Value: &travelersAPI.FieldValue_GenderValue{GenderValue: travelersAPI.Gender_GENDER_MALE}},
				"birth_date":                  {Value: &travelersAPI.FieldValue_DateValue{DateValue: &date.Date{Year: 1990, Month: 9, Day: 8}}},
				"phone":                       {Value: &travelersAPI.FieldValue_StrValue{StrValue: "+79041234567"}},
				"email":                       {Value: &travelersAPI.FieldValue_StrValue{StrValue: "a@example.com"}},
				"train_notifications_enabled": {Value: &travelersAPI.FieldValue_BoolValue{BoolValue: true}},
			},
			BonusCards: []*travelersAPI.EditableBonusCard{
				{
					Type: "rzd_bonus",
					Fields: map[string]*travelersAPI.FieldValue{
						"title":  {Value: &travelersAPI.FieldValue_StrValue{StrValue: "bc title"}},
						"number": {Value: &travelersAPI.FieldValue_StrValue{StrValue: "9001234567890"}},
					},
				},
			},
		},
	}
	uid := "100500"
	tcMock := new(travelersClientMock)
	tcMock.On("GetTraveler", ctx, uid).Return((*travelersclient.Traveler)(nil), travelersclient.StatusError{Status: http.StatusNotFound})
	tcMock.On("CreateOrUpdateTraveler", ctx, uid, &travelersclient.EditableTraveler{Agree: true}).Return((*travelersclient.Traveler)(nil), travelersclient.StatusError{Status: http.StatusInternalServerError})
	tCacheMock := new(travelersCacheMock)
	handler := NewGRPCTravelersHandler(&nop.Logger{}, tcMock, tCacheMock, newAdminCacheForTests(), newL10nServiceForTests())

	_, err := handler.CreatePassenger(ctx, &req)

	var res interface{ GRPCStatus() *status.Status }
	if assert.ErrorAs(t, err, &res) {
		assert.Equal(t, codes.Unknown, res.GRPCStatus().Code())
	}
	tcMock.AssertExpectations(t)
}

func TestCreatePassengerWithDetailsBonusCardUnexpectedError(t *testing.T) {
	ctx := prepareAuthContext(context.Background(), 100500)
	req := travelersAPI.CreatePassengerReq{
		Passenger: &travelersAPI.EditablePassenger{
			Fields: map[string]*travelersAPI.FieldValue{
				"title":                       {Value: &travelersAPI.FieldValue_StrValue{StrValue: "А А А"}},
				"gender":                      {Value: &travelersAPI.FieldValue_GenderValue{GenderValue: travelersAPI.Gender_GENDER_MALE}},
				"birth_date":                  {Value: &travelersAPI.FieldValue_DateValue{DateValue: &date.Date{Year: 1990, Month: 9, Day: 8}}},
				"phone":                       {Value: &travelersAPI.FieldValue_StrValue{StrValue: "+79041234567"}},
				"email":                       {Value: &travelersAPI.FieldValue_StrValue{StrValue: "a@example.com"}},
				"train_notifications_enabled": {Value: &travelersAPI.FieldValue_BoolValue{BoolValue: true}},
			},
			BonusCards: []*travelersAPI.EditableBonusCard{
				{
					Type: "rzd_bonus",
					Fields: map[string]*travelersAPI.FieldValue{
						"title":  {Value: &travelersAPI.FieldValue_StrValue{StrValue: "no error"}},
						"number": {Value: &travelersAPI.FieldValue_StrValue{StrValue: "9001234567890"}},
					},
				},
				{
					Type: "rzd_bonus",
					Fields: map[string]*travelersAPI.FieldValue{
						"title":  {Value: &travelersAPI.FieldValue_StrValue{StrValue: "error"}},
						"number": {Value: &travelersAPI.FieldValue_StrValue{StrValue: "9001111111111"}},
					},
				},
			},
		},
	}
	uid := "100500"
	tcMock := new(travelersClientMock)
	tcMock.On("GetTraveler", ctx, uid).Return(&travelersclient.Traveler{
		Email:           "",
		Phone:           "",
		PhoneAdditional: "",
		Agree:           true,
		CreatedAt:       &travelersclient.Timestamp{Time: time.Unix(1635260000, 0)},
		UpdatedAt:       &travelersclient.Timestamp{Time: time.Unix(1635262000, 0)},
	}, nil)
	tcMock.On("CreatePassenger", ctx, uid, &travelersclient.CreateOrUpdatePassengerRequest{
		Fields: map[string]interface{}{
			"title":                       "А А А",
			"gender":                      "male",
			"birth_date":                  "1990-09-08",
			"phone":                       "+79041234567",
			"email":                       "a@example.com",
			"train_notifications_enabled": true,
		},
	}).Return(&travelersclient.Passenger{
		ID:        "1",
		CreatedAt: &travelersclient.Timestamp{Time: time.Unix(1635260000, 0)},
		UpdatedAt: &travelersclient.Timestamp{Time: time.Unix(1635262000, 0)},
		Fields: map[string]interface{}{
			"title":                       "А А А",
			"gender":                      "male",
			"birth_date":                  "1990-09-08",
			"phone":                       "+79041234567",
			"email":                       "a@example.com",
			"train_notifications_enabled": true,
		},
	}, nil)
	tcMock.On("CreateBonusCard", ctx, uid, "1", &travelersclient.EditableBonusCard{
		Type: "rzd_bonus",
		Fields: map[string]interface{}{
			"title":  "no error",
			"number": "9001234567890",
		},
	}).Return(&travelersclient.BonusCard{
		ID:          "2",
		PassengerID: "1",
		Type:        "rzd_bonus",
		CreatedAt:   &travelersclient.Timestamp{Time: time.Unix(1635260000, 0)},
		UpdatedAt:   &travelersclient.Timestamp{Time: time.Unix(1635262000, 0)},
		Fields: map[string]interface{}{
			"title":  "no error",
			"number": "9001234567890",
		},
	}, nil)
	tcMock.On("CreateBonusCard", ctx, uid, "1", &travelersclient.EditableBonusCard{
		Type: "rzd_bonus",
		Fields: map[string]interface{}{
			"title":  "error",
			"number": "9001111111111",
		},
	}).Return((*travelersclient.BonusCard)(nil), travelersclient.StatusError{Status: http.StatusInternalServerError})
	tCacheMock := new(travelersCacheMock)
	handler := NewGRPCTravelersHandler(&nop.Logger{}, tcMock, tCacheMock, newAdminCacheForTests(), newL10nServiceForTests())

	rsp, err := handler.CreatePassenger(ctx, &req)

	require.NoError(t, err)
	expectedPassenger := travelersAPI.Passenger{
		Id: "1",
		Fields: map[string]*travelersAPI.FieldValue{
			"title":                       {Value: &travelersAPI.FieldValue_StrValue{StrValue: "А А А"}},
			"gender":                      {Value: &travelersAPI.FieldValue_GenderValue{GenderValue: travelersAPI.Gender_GENDER_MALE}},
			"birth_date":                  {Value: &travelersAPI.FieldValue_DateValue{DateValue: &date.Date{Year: 1990, Month: 9, Day: 8}}},
			"phone":                       {Value: &travelersAPI.FieldValue_StrValue{StrValue: "+79041234567"}},
			"email":                       {Value: &travelersAPI.FieldValue_StrValue{StrValue: "a@example.com"}},
			"train_notifications_enabled": {Value: &travelersAPI.FieldValue_BoolValue{BoolValue: true}},
		},
		CreatedAt: &timestamppb.Timestamp{Seconds: 1635260000, Nanos: 0},
		UpdatedAt: &timestamppb.Timestamp{Seconds: 1635262000, Nanos: 0},
		BonusCards: []*travelersAPI.BonusCard{
			{
				Id:          "2",
				PassengerId: "1",
				Type:        "rzd_bonus",
				Fields: map[string]*travelersAPI.FieldValue{
					"title":  {Value: &travelersAPI.FieldValue_StrValue{StrValue: "no error"}},
					"number": {Value: &travelersAPI.FieldValue_StrValue{StrValue: "9001234567890"}},
				},
				CreatedAt: &timestamppb.Timestamp{Seconds: 1635260000, Nanos: 0},
				UpdatedAt: &timestamppb.Timestamp{Seconds: 1635262000, Nanos: 0},
			},
		},
	}
	require.Equal(t, &expectedPassenger, rsp.Passenger)
	require.Equal(t, uint32(0), rsp.NotSavedDocumentsCount)
	require.Equal(t, uint32(1), rsp.NotSavedBonusCardsCount)
	require.Len(t, rsp.BonusCardsCreationErrors, 1)
	require.Equal(t, uint32(1), rsp.BonusCardsCreationErrors[0].Index)
	require.Len(t, rsp.BonusCardsCreationErrors[0].Details, 1)
	require.Equal(t, "type.googleapis.com/travel.app.backend.api.travelers.v1.OtherTravelersError", rsp.BonusCardsCreationErrors[0].Details[0].TypeUrl)
	detail := rsp.BonusCardsCreationErrors[0].Details[0]
	var otherTravelersError travelersAPI.OtherTravelersError
	require.NoError(t, detail.UnmarshalTo(&otherTravelersError))
	require.Equal(t, "unexpected http status: 500", otherTravelersError.ErrorMessage)
	tcMock.AssertExpectations(t)
}

func TestCreatePassengerWithDetailsBonusCardValidationError(t *testing.T) {
	ctx := prepareAuthContext(context.Background(), 100500)
	req := travelersAPI.CreatePassengerReq{
		Passenger: &travelersAPI.EditablePassenger{
			Fields: map[string]*travelersAPI.FieldValue{
				"title":                       {Value: &travelersAPI.FieldValue_StrValue{StrValue: "А А А"}},
				"gender":                      {Value: &travelersAPI.FieldValue_GenderValue{GenderValue: travelersAPI.Gender_GENDER_MALE}},
				"birth_date":                  {Value: &travelersAPI.FieldValue_DateValue{DateValue: &date.Date{Year: 1990, Month: 9, Day: 8}}},
				"phone":                       {Value: &travelersAPI.FieldValue_StrValue{StrValue: "+79041234567"}},
				"email":                       {Value: &travelersAPI.FieldValue_StrValue{StrValue: "a@example.com"}},
				"train_notifications_enabled": {Value: &travelersAPI.FieldValue_BoolValue{BoolValue: true}},
			},
			BonusCards: []*travelersAPI.EditableBonusCard{
				{
					Type: "rzd_bonus",
					Fields: map[string]*travelersAPI.FieldValue{
						"title":  {Value: &travelersAPI.FieldValue_StrValue{StrValue: "no error"}},
						"number": {Value: &travelersAPI.FieldValue_StrValue{StrValue: "9001234567890"}},
					},
				},
				{
					Type: "rzd_bonus",
					Fields: map[string]*travelersAPI.FieldValue{
						"title": {Value: &travelersAPI.FieldValue_StrValue{StrValue: "error"}},
					},
				},
			},
		},
	}
	uid := "100500"
	tcMock := new(travelersClientMock)
	tcMock.On("GetTraveler", ctx, uid).Return(&travelersclient.Traveler{
		Email:           "",
		Phone:           "",
		PhoneAdditional: "",
		Agree:           true,
		CreatedAt:       &travelersclient.Timestamp{Time: time.Unix(1635260000, 0)},
		UpdatedAt:       &travelersclient.Timestamp{Time: time.Unix(1635262000, 0)},
	}, nil)
	tcMock.On("CreatePassenger", ctx, uid, &travelersclient.CreateOrUpdatePassengerRequest{
		Fields: map[string]interface{}{
			"title":                       "А А А",
			"gender":                      "male",
			"birth_date":                  "1990-09-08",
			"phone":                       "+79041234567",
			"email":                       "a@example.com",
			"train_notifications_enabled": true,
		},
	}).Return(&travelersclient.Passenger{
		ID:        "1",
		CreatedAt: &travelersclient.Timestamp{Time: time.Unix(1635260000, 0)},
		UpdatedAt: &travelersclient.Timestamp{Time: time.Unix(1635262000, 0)},
		Fields: map[string]interface{}{
			"title":                       "А А А",
			"gender":                      "male",
			"birth_date":                  "1990-09-08",
			"phone":                       "+79041234567",
			"email":                       "a@example.com",
			"train_notifications_enabled": true,
		},
	}, nil)
	tcMock.On("CreateBonusCard", ctx, uid, "1", &travelersclient.EditableBonusCard{
		Type: "rzd_bonus",
		Fields: map[string]interface{}{
			"title":  "no error",
			"number": "9001234567890",
		},
	}).Return(&travelersclient.BonusCard{
		ID:          "2",
		PassengerID: "1",
		Type:        "rzd_bonus",
		CreatedAt:   &travelersclient.Timestamp{Time: time.Unix(1635260000, 0)},
		UpdatedAt:   &travelersclient.Timestamp{Time: time.Unix(1635262000, 0)},
		Fields: map[string]interface{}{
			"title":  "no error",
			"number": "9001234567890",
		},
	}, nil)
	tcMock.On("CreateBonusCard", ctx, uid, "1", &travelersclient.EditableBonusCard{
		Type: "rzd_bonus",
		Fields: map[string]interface{}{
			"title": "error",
		},
	}).Return((*travelersclient.BonusCard)(nil), travelersclient.ValidationError{FieldErrors: map[string]string{"number": "Required field"}})
	tCacheMock := new(travelersCacheMock)
	handler := NewGRPCTravelersHandler(&nop.Logger{}, tcMock, tCacheMock, newAdminCacheForTests(), newL10nServiceForTests())

	rsp, err := handler.CreatePassenger(ctx, &req)

	require.NoError(t, err)
	expectedPassenger := travelersAPI.Passenger{
		Id: "1",
		Fields: map[string]*travelersAPI.FieldValue{
			"title":                       {Value: &travelersAPI.FieldValue_StrValue{StrValue: "А А А"}},
			"gender":                      {Value: &travelersAPI.FieldValue_GenderValue{GenderValue: travelersAPI.Gender_GENDER_MALE}},
			"birth_date":                  {Value: &travelersAPI.FieldValue_DateValue{DateValue: &date.Date{Year: 1990, Month: 9, Day: 8}}},
			"phone":                       {Value: &travelersAPI.FieldValue_StrValue{StrValue: "+79041234567"}},
			"email":                       {Value: &travelersAPI.FieldValue_StrValue{StrValue: "a@example.com"}},
			"train_notifications_enabled": {Value: &travelersAPI.FieldValue_BoolValue{BoolValue: true}},
		},
		CreatedAt: &timestamppb.Timestamp{Seconds: 1635260000, Nanos: 0},
		UpdatedAt: &timestamppb.Timestamp{Seconds: 1635262000, Nanos: 0},
		BonusCards: []*travelersAPI.BonusCard{
			{
				Id:          "2",
				PassengerId: "1",
				Type:        "rzd_bonus",
				Fields: map[string]*travelersAPI.FieldValue{
					"title":  {Value: &travelersAPI.FieldValue_StrValue{StrValue: "no error"}},
					"number": {Value: &travelersAPI.FieldValue_StrValue{StrValue: "9001234567890"}},
				},
				CreatedAt: &timestamppb.Timestamp{Seconds: 1635260000, Nanos: 0},
				UpdatedAt: &timestamppb.Timestamp{Seconds: 1635262000, Nanos: 0},
			},
		},
	}
	require.Equal(t, &expectedPassenger, rsp.Passenger)
	require.Equal(t, uint32(0), rsp.NotSavedDocumentsCount)
	require.Equal(t, uint32(1), rsp.NotSavedBonusCardsCount)
	require.Len(t, rsp.BonusCardsCreationErrors, 1)
	require.Equal(t, uint32(1), rsp.BonusCardsCreationErrors[0].Index)
	require.Len(t, rsp.BonusCardsCreationErrors[0].Details, 1)
	require.Equal(t, "type.googleapis.com/google.rpc.BadRequest", rsp.BonusCardsCreationErrors[0].Details[0].TypeUrl)
	detail := rsp.BonusCardsCreationErrors[0].Details[0]
	var badRequestError errdetails.BadRequest
	require.NoError(t, detail.UnmarshalTo(&badRequestError))
	require.Len(t, badRequestError.FieldViolations, 1)
	require.Equal(t, "number", badRequestError.FieldViolations[0].Field)
	require.Equal(t, "Required field", badRequestError.FieldViolations[0].Description)
	tcMock.AssertExpectations(t)
}

func TestCreateDocument_RuNationalPassport(t *testing.T) {
	var uid uint64 = 100500
	uidStr := fmt.Sprintf("%d", uid)
	ctx := prepareAuthContext(context.Background(), uid)
	req := travelersAPI.CreateDocumentReq{PassengerId: "1", Document: &travelersAPI.EditableDocument{
		Type: "ru_national_passport",
		Fields: map[string]*travelersAPI.FieldValue{
			"title":       {Value: &travelersAPI.FieldValue_StrValue{StrValue: "doc title"}},
			"number":      {Value: &travelersAPI.FieldValue_StrValue{StrValue: "6504123456"}},
			"first_name":  {Value: &travelersAPI.FieldValue_StrValue{StrValue: "Сидор"}},
			"middle_name": {Value: &travelersAPI.FieldValue_StrValue{StrValue: "Сидорович"}},
			"last_name":   {Value: &travelersAPI.FieldValue_StrValue{StrValue: "Сидоров"}},
			"issue_date":  {Value: &travelersAPI.FieldValue_DateValue{DateValue: &date.Date{Year: 2004, Month: 9, Day: 8}}},
		},
	}}
	tcMock := new(travelersClientMock)
	tcMock.On("CreateDocument", ctx, uidStr, req.PassengerId, &travelersclient.CreateOrUpdateDocumentRequest{
		Type: "ru_national_passport",
		Fields: map[string]interface{}{
			"title":       "doc title",
			"number":      "6504123456",
			"first_name":  "Сидор",
			"middle_name": "Сидорович",
			"last_name":   "Сидоров",
			"issue_date":  "2004-09-08",
			"citizenship": uint64(225),
		},
	}).Return(&travelersclient.Document{
		ID:          "2",
		PassengerID: "1",
		Type:        "ru_national_passport",
		CreatedAt:   &travelersclient.Timestamp{Time: time.Unix(1635260000, 0)},
		UpdatedAt:   &travelersclient.Timestamp{Time: time.Unix(1635262000, 0)},
		Fields: map[string]interface{}{
			"title":       "doc title",
			"number":      "6504123456",
			"first_name":  "Сидор",
			"middle_name": "Сидорович",
			"last_name":   "Сидоров",
			"issue_date":  "2004-09-08",
			"citizenship": uint64(225),
		},
	}, nil)
	tCacheMock := new(travelersCacheMock)
	handler := NewGRPCTravelersHandler(&nop.Logger{}, tcMock, tCacheMock, newAdminCacheForTests(), newL10nServiceForTests())

	rsp, err := handler.CreateDocument(ctx, &req)

	require.NoError(t, err)
	expectedDocument := travelersAPI.Document{
		Id:          "2",
		PassengerId: "1",
		Type:        "ru_national_passport",
		Fields: map[string]*travelersAPI.FieldValue{
			"title":       {Value: &travelersAPI.FieldValue_StrValue{StrValue: "doc title"}},
			"number":      {Value: &travelersAPI.FieldValue_StrValue{StrValue: "6504123456"}},
			"first_name":  {Value: &travelersAPI.FieldValue_StrValue{StrValue: "Сидор"}},
			"middle_name": {Value: &travelersAPI.FieldValue_StrValue{StrValue: "Сидорович"}},
			"last_name":   {Value: &travelersAPI.FieldValue_StrValue{StrValue: "Сидоров"}},
			"issue_date":  {Value: &travelersAPI.FieldValue_DateValue{DateValue: &date.Date{Year: 2004, Month: 9, Day: 8}}},
		},
		CreatedAt: &timestamppb.Timestamp{Seconds: 1635260000, Nanos: 0},
		UpdatedAt: &timestamppb.Timestamp{Seconds: 1635262000, Nanos: 0},
	}
	require.Equal(t, &expectedDocument, rsp.Document)
	tcMock.AssertExpectations(t)
}

func TestGetDocument_RuNationalPassport(t *testing.T) {
	var uid uint64 = 100500
	uidStr := fmt.Sprintf("%d", uid)
	ctx := prepareAuthContext(context.Background(), uid)
	req := travelersAPI.GetDocumentReq{PassengerId: "1", DocumentId: "7"}
	tcMock := new(travelersClientMock)
	tcMock.On("GetDocument", ctx, uidStr, req.PassengerId, req.DocumentId).Return(&travelersclient.Document{
		ID:          "7",
		PassengerID: "1",
		Type:        "ru_national_passport",
		CreatedAt:   &travelersclient.Timestamp{Time: time.Unix(1635260000, 0)},
		UpdatedAt:   &travelersclient.Timestamp{Time: time.Unix(1635262000, 0)},
		Fields: map[string]interface{}{
			"title":       "doc title",
			"number":      "6504123456",
			"first_name":  "Сидор",
			"middle_name": "Сидорович",
			"last_name":   "Сидоров",
			"issue_date":  "2004-09-08",
			"citizenship": uint64(225),
		},
	}, nil)
	tCacheMock := new(travelersCacheMock)
	handler := NewGRPCTravelersHandler(&nop.Logger{}, tcMock, tCacheMock, newAdminCacheForTests(), newL10nServiceForTests())

	rsp, err := handler.GetPassengerDocument(ctx, &req)

	require.NoError(t, err)
	expectedDocument := travelersAPI.Document{
		Id:          "7",
		PassengerId: "1",
		Type:        "ru_national_passport",
		Fields: map[string]*travelersAPI.FieldValue{
			"title":       {Value: &travelersAPI.FieldValue_StrValue{StrValue: "doc title"}},
			"number":      {Value: &travelersAPI.FieldValue_StrValue{StrValue: "6504123456"}},
			"first_name":  {Value: &travelersAPI.FieldValue_StrValue{StrValue: "Сидор"}},
			"middle_name": {Value: &travelersAPI.FieldValue_StrValue{StrValue: "Сидорович"}},
			"last_name":   {Value: &travelersAPI.FieldValue_StrValue{StrValue: "Сидоров"}},
			"issue_date":  {Value: &travelersAPI.FieldValue_DateValue{DateValue: &date.Date{Year: 2004, Month: 9, Day: 8}}},
		},
		CreatedAt: &timestamppb.Timestamp{Seconds: 1635260000, Nanos: 0},
		UpdatedAt: &timestamppb.Timestamp{Seconds: 1635262000, Nanos: 0},
	}
	require.Equal(t, &expectedDocument, rsp.Document)
	tcMock.AssertExpectations(t)
}

func TestGetDocument_Other(t *testing.T) {
	var uid uint64 = 100500
	uidStr := fmt.Sprintf("%d", uid)
	ctx := prepareAuthContext(context.Background(), uid)
	req := travelersAPI.GetDocumentReq{PassengerId: "1", DocumentId: "7"}
	tcMock := new(travelersClientMock)
	tcMock.On("GetDocument", ctx, uidStr, req.PassengerId, req.DocumentId).Return(&travelersclient.Document{
		ID:          "7",
		PassengerID: "1",
		Type:        "other",
		CreatedAt:   &travelersclient.Timestamp{Time: time.Unix(1635260000, 0)},
		UpdatedAt:   &travelersclient.Timestamp{Time: time.Unix(1635262000, 0)},
		Fields: map[string]interface{}{
			"title":       "doc title",
			"number":      "6504123456",
			"first_name":  "Сидор",
			"middle_name": "Сидорович",
			"last_name":   "Сидоров",
			"issue_date":  "2004-09-08",
			"citizenship": float64(111),
		},
	}, nil)
	tCacheMock := new(travelersCacheMock)
	handler := NewGRPCTravelersHandler(&nop.Logger{}, tcMock, tCacheMock, newAdminCacheForTests(), newL10nServiceForTests())

	rsp, err := handler.GetPassengerDocument(ctx, &req)

	require.NoError(t, err)
	expectedDocument := travelersAPI.Document{
		Id:          "7",
		PassengerId: "1",
		Type:        "other",
		Fields: map[string]*travelersAPI.FieldValue{
			"title":              {Value: &travelersAPI.FieldValue_StrValue{StrValue: "doc title"}},
			"number":             {Value: &travelersAPI.FieldValue_StrValue{StrValue: "6504123456"}},
			"first_name":         {Value: &travelersAPI.FieldValue_StrValue{StrValue: "Сидор"}},
			"middle_name":        {Value: &travelersAPI.FieldValue_StrValue{StrValue: "Сидорович"}},
			"last_name":          {Value: &travelersAPI.FieldValue_StrValue{StrValue: "Сидоров"}},
			"issue_date":         {Value: &travelersAPI.FieldValue_DateValue{DateValue: &date.Date{Year: 2004, Month: 9, Day: 8}}},
			"citizenship_geo_id": {Value: &travelersAPI.FieldValue_CountryGeoIdValue{CountryGeoIdValue: 111}},
		},
		CreatedAt: &timestamppb.Timestamp{Seconds: 1635260000, Nanos: 0},
		UpdatedAt: &timestamppb.Timestamp{Seconds: 1635262000, Nanos: 0},
	}
	require.Equal(t, &expectedDocument, rsp.Document)
	tcMock.AssertExpectations(t)
}

func TestGetDocument_NilIssueDate(t *testing.T) {
	var uid uint64 = 100500
	uidStr := fmt.Sprintf("%d", uid)
	ctx := prepareAuthContext(context.Background(), uid)
	req := travelersAPI.GetDocumentReq{PassengerId: "1", DocumentId: "7"}
	tcMock := new(travelersClientMock)
	tcMock.On("GetDocument", ctx, uidStr, req.PassengerId, req.DocumentId).Return(&travelersclient.Document{
		ID:          "7",
		PassengerID: "1",
		Type:        "ru_national_passport",
		CreatedAt:   &travelersclient.Timestamp{Time: time.Unix(1635260000, 0)},
		UpdatedAt:   &travelersclient.Timestamp{Time: time.Unix(1635262000, 0)},
		Fields: map[string]interface{}{
			"title":       "doc title",
			"number":      "6504123456",
			"first_name":  "Сидор",
			"middle_name": "Сидорович",
			"last_name":   "Сидоров",
			"citizenship": uint64(225),
		},
	}, nil)
	tCacheMock := new(travelersCacheMock)
	handler := NewGRPCTravelersHandler(&nop.Logger{}, tcMock, tCacheMock, newAdminCacheForTests(), newL10nServiceForTests())

	rsp, err := handler.GetPassengerDocument(ctx, &req)

	require.NoError(t, err)
	expectedDocument := travelersAPI.Document{
		Id:          "7",
		PassengerId: "1",
		Type:        "ru_national_passport",
		Fields: map[string]*travelersAPI.FieldValue{
			"title":       {Value: &travelersAPI.FieldValue_StrValue{StrValue: "doc title"}},
			"number":      {Value: &travelersAPI.FieldValue_StrValue{StrValue: "6504123456"}},
			"first_name":  {Value: &travelersAPI.FieldValue_StrValue{StrValue: "Сидор"}},
			"middle_name": {Value: &travelersAPI.FieldValue_StrValue{StrValue: "Сидорович"}},
			"last_name":   {Value: &travelersAPI.FieldValue_StrValue{StrValue: "Сидоров"}},
		},
		CreatedAt: &timestamppb.Timestamp{Seconds: 1635260000, Nanos: 0},
		UpdatedAt: &timestamppb.Timestamp{Seconds: 1635262000, Nanos: 0},
	}
	require.Equal(t, &expectedDocument, rsp.Document)
	tcMock.AssertExpectations(t)
}

func TestGetBonusCard(t *testing.T) {
	ctx := prepareAuthContext(context.Background(), 100500)
	req := travelersAPI.GetBonusCardReq{PassengerId: "1", BonusCardId: "7"}
	uid := "100500"
	tcMock := new(travelersClientMock)
	tcMock.On("GetBonusCard", ctx, uid, req.PassengerId, req.BonusCardId).Return(&travelersclient.BonusCard{
		ID:          "7",
		PassengerID: "1",
		Type:        "rzd_bonus",
		CreatedAt:   &travelersclient.Timestamp{Time: time.Unix(1635260000, 0)},
		UpdatedAt:   &travelersclient.Timestamp{Time: time.Unix(1635262000, 0)},
		Fields: map[string]interface{}{
			"title":  "bc title",
			"number": "9001234567890",
		},
	}, nil)
	tCacheMock := new(travelersCacheMock)
	handler := NewGRPCTravelersHandler(&nop.Logger{}, tcMock, tCacheMock, newAdminCacheForTests(), newL10nServiceForTests())

	rsp, err := handler.GetBonusCard(ctx, &req)

	require.NoError(t, err)
	expectedBonusCard := travelersAPI.BonusCard{
		Id:          "7",
		PassengerId: "1",
		Fields: map[string]*travelersAPI.FieldValue{
			"title":  {Value: &travelersAPI.FieldValue_StrValue{StrValue: "bc title"}},
			"number": {Value: &travelersAPI.FieldValue_StrValue{StrValue: "9001234567890"}},
		},
		Type:      "rzd_bonus",
		CreatedAt: &timestamppb.Timestamp{Seconds: 1635260000, Nanos: 0},
		UpdatedAt: &timestamppb.Timestamp{Seconds: 1635262000, Nanos: 0},
	}
	require.Equal(t, &expectedBonusCard, rsp.BonusCard)
	tcMock.AssertExpectations(t)
}

func TestGetBonusCardError(t *testing.T) {
	// Проверяем, что не забыли про конвертацию ошибок
	travelersClientError := travelersclient.StatusError{Status: http.StatusNotFound}
	expectedGRPCStatus := codes.NotFound

	ctx := prepareAuthContext(context.Background(), 100500)
	req := travelersAPI.GetBonusCardReq{}
	uid := "100500"
	tcMock := new(travelersClientMock)
	tcMock.On("GetBonusCard", ctx, uid, req.PassengerId, req.BonusCardId).Return((*travelersclient.BonusCard)(nil), travelersClientError)
	tCacheMock := new(travelersCacheMock)
	handler := NewGRPCTravelersHandler(&nop.Logger{}, tcMock, tCacheMock, newAdminCacheForTests(), newL10nServiceForTests())

	_, err := handler.GetBonusCard(ctx, &req)

	var res interface{ GRPCStatus() *status.Status }
	if assert.ErrorAs(t, err, &res) {
		assert.Equal(t, expectedGRPCStatus, res.GRPCStatus().Code())
	}
	tcMock.AssertExpectations(t)
}

func TestListBonusCards(t *testing.T) {
	ctx := prepareAuthContext(context.Background(), 100500)
	req := travelersAPI.ListBonusCardsReq{PassengerId: "1"}
	uid := "100500"
	tcMock := new(travelersClientMock)
	tcMock.On("ListBonusCards", ctx, uid, req.PassengerId).Return([]travelersclient.BonusCard{{
		ID:          "2",
		PassengerID: "1",
		Type:        "rzd_bonus",
		CreatedAt:   &travelersclient.Timestamp{Time: time.Unix(1635260000, 0)},
		UpdatedAt:   &travelersclient.Timestamp{Time: time.Unix(1635262000, 0)},
		Fields: map[string]interface{}{
			"title":  "bc title",
			"number": "9001234567890",
		},
	}}, nil)
	tCacheMock := new(travelersCacheMock)
	handler := NewGRPCTravelersHandler(&nop.Logger{}, tcMock, tCacheMock, newAdminCacheForTests(), newL10nServiceForTests())

	rsp, err := handler.ListBonusCards(ctx, &req)

	require.NoError(t, err)
	expectedBonusCards := []*travelersAPI.BonusCard{
		{
			Id:          "2",
			PassengerId: "1",
			Fields: map[string]*travelersAPI.FieldValue{
				"title":  {Value: &travelersAPI.FieldValue_StrValue{StrValue: "bc title"}},
				"number": {Value: &travelersAPI.FieldValue_StrValue{StrValue: "9001234567890"}},
			},
			Type:      "rzd_bonus",
			CreatedAt: &timestamppb.Timestamp{Seconds: 1635260000, Nanos: 0},
			UpdatedAt: &timestamppb.Timestamp{Seconds: 1635262000, Nanos: 0},
		},
	}
	require.Equal(t, expectedBonusCards, rsp.BonusCards)
	tcMock.AssertExpectations(t)
}

func TestListBonusCardsError(t *testing.T) {
	// Проверяем, что не забыли про конвертацию ошибок
	travelersClientError := travelersclient.StatusError{Status: http.StatusNotFound}
	expectedGRPCStatus := codes.NotFound

	ctx := prepareAuthContext(context.Background(), 100500)
	req := travelersAPI.ListBonusCardsReq{PassengerId: "1"}
	uid := "100500"
	tcMock := new(travelersClientMock)
	tcMock.On("ListBonusCards", ctx, uid, req.PassengerId).Return(([]travelersclient.BonusCard)(nil), travelersClientError)
	tCacheMock := new(travelersCacheMock)
	handler := NewGRPCTravelersHandler(&nop.Logger{}, tcMock, tCacheMock, newAdminCacheForTests(), newL10nServiceForTests())

	_, err := handler.ListBonusCards(ctx, &req)

	var res interface{ GRPCStatus() *status.Status }
	if assert.ErrorAs(t, err, &res) {
		assert.Equal(t, expectedGRPCStatus, res.GRPCStatus().Code())
	}
	tcMock.AssertExpectations(t)
}

func TestCreateBonusCard(t *testing.T) {
	ctx := prepareAuthContext(context.Background(), 100500)
	req := travelersAPI.CreateBonusCardReq{PassengerId: "1", BonusCard: &travelersAPI.EditableBonusCard{
		Type: "rzd_bonus",
		Fields: map[string]*travelersAPI.FieldValue{
			"title":  {Value: &travelersAPI.FieldValue_StrValue{StrValue: "bc title"}},
			"number": {Value: &travelersAPI.FieldValue_StrValue{StrValue: "9001234567890"}},
		},
	}}
	uid := "100500"
	tcMock := new(travelersClientMock)
	tcMock.On("CreateBonusCard", ctx, uid, req.PassengerId, &travelersclient.EditableBonusCard{
		Type: "rzd_bonus",
		Fields: map[string]interface{}{
			"title":  "bc title",
			"number": "9001234567890",
		},
	}).Return(&travelersclient.BonusCard{
		ID:          "2",
		PassengerID: "1",
		Type:        "rzd_bonus",
		CreatedAt:   &travelersclient.Timestamp{Time: time.Unix(1635260000, 0)},
		UpdatedAt:   &travelersclient.Timestamp{Time: time.Unix(1635262000, 0)},
		Fields: map[string]interface{}{
			"title":  "bc title",
			"number": "9001234567890",
		},
	}, nil)
	tCacheMock := new(travelersCacheMock)
	handler := NewGRPCTravelersHandler(&nop.Logger{}, tcMock, tCacheMock, newAdminCacheForTests(), newL10nServiceForTests())

	rsp, err := handler.CreateBonusCard(ctx, &req)

	require.NoError(t, err)
	expectedBonusCard := travelersAPI.BonusCard{
		Id:          "2",
		PassengerId: "1",
		Type:        "rzd_bonus",
		Fields: map[string]*travelersAPI.FieldValue{
			"title":  {Value: &travelersAPI.FieldValue_StrValue{StrValue: "bc title"}},
			"number": {Value: &travelersAPI.FieldValue_StrValue{StrValue: "9001234567890"}},
		},
		CreatedAt: &timestamppb.Timestamp{Seconds: 1635260000, Nanos: 0},
		UpdatedAt: &timestamppb.Timestamp{Seconds: 1635262000, Nanos: 0},
	}
	require.Equal(t, &expectedBonusCard, rsp.BonusCard)
	tcMock.AssertExpectations(t)
}

func TestCreateBonusCard_NoTitle(t *testing.T) {
	ctx := prepareAuthContext(context.Background(), 100500)
	req := travelersAPI.CreateBonusCardReq{PassengerId: "1", BonusCard: &travelersAPI.EditableBonusCard{
		Type: "rzd_bonus",
		Fields: map[string]*travelersAPI.FieldValue{
			"number": {Value: &travelersAPI.FieldValue_StrValue{StrValue: "9001234567890"}},
		},
	}}
	uid := "100500"
	tcMock := new(travelersClientMock)
	tcMock.On("CreateBonusCard", ctx, uid, req.PassengerId, &travelersclient.EditableBonusCard{
		Type: "rzd_bonus",
		Fields: map[string]interface{}{
			"number": "9001234567890",
		},
	}).Return(&travelersclient.BonusCard{
		ID:          "2",
		PassengerID: "1",
		Type:        "rzd_bonus",
		CreatedAt:   &travelersclient.Timestamp{Time: time.Unix(1635260000, 0)},
		UpdatedAt:   &travelersclient.Timestamp{Time: time.Unix(1635262000, 0)},
		Fields: map[string]interface{}{
			"number": "9001234567890",
		},
	}, nil)
	tCacheMock := new(travelersCacheMock)
	handler := NewGRPCTravelersHandler(&nop.Logger{}, tcMock, tCacheMock, newAdminCacheForTests(), newL10nServiceForTests())

	rsp, err := handler.CreateBonusCard(ctx, &req)

	require.NoError(t, err)
	expectedBonusCard := travelersAPI.BonusCard{
		Id:          "2",
		PassengerId: "1",
		Type:        "rzd_bonus",
		Fields: map[string]*travelersAPI.FieldValue{
			"number": {Value: &travelersAPI.FieldValue_StrValue{StrValue: "9001234567890"}},
		},
		CreatedAt: &timestamppb.Timestamp{Seconds: 1635260000, Nanos: 0},
		UpdatedAt: &timestamppb.Timestamp{Seconds: 1635262000, Nanos: 0},
	}
	require.Equal(t, &expectedBonusCard, rsp.BonusCard)
	tcMock.AssertExpectations(t)
}

func TestCreateBonusCardNotFoundError(t *testing.T) {
	// Проверяем, что не забыли про конвертацию ошибок
	travelersClientError := travelersclient.StatusError{Status: http.StatusNotFound}
	expectedGRPCStatus := codes.NotFound

	ctx := prepareAuthContext(context.Background(), 100500)
	req := travelersAPI.CreateBonusCardReq{PassengerId: "1", BonusCard: &travelersAPI.EditableBonusCard{}}
	uid := "100500"
	tcMock := new(travelersClientMock)
	tcMock.On(
		"CreateBonusCard", ctx, uid, req.PassengerId, &travelersclient.EditableBonusCard{},
	).Return((*travelersclient.BonusCard)(nil), travelersClientError)
	tCacheMock := new(travelersCacheMock)
	handler := NewGRPCTravelersHandler(&nop.Logger{}, tcMock, tCacheMock, newAdminCacheForTests(), newL10nServiceForTests())

	_, err := handler.CreateBonusCard(ctx, &req)

	var res interface{ GRPCStatus() *status.Status }
	if assert.ErrorAs(t, err, &res) {
		assert.Equal(t, expectedGRPCStatus, res.GRPCStatus().Code())
	}
	tcMock.AssertExpectations(t)
}

func TestCreateBonusCardValidationError(t *testing.T) {
	travelersClientError := travelersclient.ValidationError{FieldErrors: map[string]string{
		"number": "required field",
	}}
	expectedGRPCStatus := codes.InvalidArgument

	ctx := prepareAuthContext(context.Background(), 100500)
	req := travelersAPI.CreateBonusCardReq{PassengerId: "1", BonusCard: &travelersAPI.EditableBonusCard{
		Type: "rzd_bonus",
	}}
	uid := "100500"
	tcMock := new(travelersClientMock)
	tcMock.On(
		"CreateBonusCard", ctx, uid, req.PassengerId, &travelersclient.EditableBonusCard{Type: "rzd_bonus"},
	).Return((*travelersclient.BonusCard)(nil), travelersClientError)
	tCacheMock := new(travelersCacheMock)
	handler := NewGRPCTravelersHandler(&nop.Logger{}, tcMock, tCacheMock, newAdminCacheForTests(), newL10nServiceForTests())

	_, err := handler.CreateBonusCard(ctx, &req)

	var res interface{ GRPCStatus() *status.Status }
	if assert.ErrorAs(t, err, &res) {
		assert.Equal(t, expectedGRPCStatus, res.GRPCStatus().Code())
		assert.Equal(t, "CardValidationFailed", res.GRPCStatus().Message())
		assert.Len(t, res.GRPCStatus().Details(), 1)
		detail := res.GRPCStatus().Details()[0]
		var badRequest *errdetails.BadRequest
		if assert.IsType(t, badRequest, detail) {
			badRequest = detail.(*errdetails.BadRequest)
			assert.Len(t, badRequest.FieldViolations, 1)
			assert.Equal(t, "number", badRequest.FieldViolations[0].Field)
			assert.Equal(t, "required field", badRequest.FieldViolations[0].Description)
		}
	}
	tcMock.AssertExpectations(t)
}

func TestUpdateBonusCard(t *testing.T) {
	ctx := prepareAuthContext(context.Background(), 100500)
	req := travelersAPI.UpdateBonusCardReq{PassengerId: "1", BonusCardId: "3", BonusCard: &travelersAPI.EditableBonusCard{
		Type: "rzd_bonus",
		Fields: map[string]*travelersAPI.FieldValue{
			"title":  {Value: &travelersAPI.FieldValue_StrValue{StrValue: "bc title"}},
			"number": {Value: &travelersAPI.FieldValue_StrValue{StrValue: "9001234567890"}},
		},
	}}
	uid := "100500"
	tcMock := new(travelersClientMock)
	tcMock.On("UpdateBonusCard", ctx, uid, req.PassengerId, req.BonusCardId, &travelersclient.EditableBonusCard{
		Type: req.BonusCard.Type,
		Fields: map[string]interface{}{
			"title":  "bc title",
			"number": "9001234567890",
		},
	}).Return(&travelersclient.BonusCard{
		ID:          "3",
		PassengerID: "1",
		Type:        "rzd_bonus",
		CreatedAt:   &travelersclient.Timestamp{Time: time.Unix(1635260000, 0)},
		UpdatedAt:   &travelersclient.Timestamp{Time: time.Unix(1635262000, 0)},
		Fields: map[string]interface{}{
			"title":  "bc title",
			"number": "9001234567890",
		},
	}, nil)
	tCacheMock := new(travelersCacheMock)
	handler := NewGRPCTravelersHandler(&nop.Logger{}, tcMock, tCacheMock, newAdminCacheForTests(), newL10nServiceForTests())

	rsp, err := handler.UpdateBonusCard(ctx, &req)

	require.NoError(t, err)
	expectedBonusCard := travelersAPI.BonusCard{
		Id:          "3",
		PassengerId: "1",
		Type:        "rzd_bonus",
		Fields: map[string]*travelersAPI.FieldValue{
			"title":  {Value: &travelersAPI.FieldValue_StrValue{StrValue: "bc title"}},
			"number": {Value: &travelersAPI.FieldValue_StrValue{StrValue: "9001234567890"}},
		},
		CreatedAt: &timestamppb.Timestamp{Seconds: 1635260000, Nanos: 0},
		UpdatedAt: &timestamppb.Timestamp{Seconds: 1635262000, Nanos: 0},
	}
	require.Equal(t, &expectedBonusCard, rsp.BonusCard)
	tcMock.AssertExpectations(t)
}

func TestUpdateBonusCardNotFoundError(t *testing.T) {
	// Проверяем, что не забыли про конвертацию ошибок
	travelersClientError := travelersclient.StatusError{Status: http.StatusNotFound}
	expectedGRPCStatus := codes.NotFound

	ctx := prepareAuthContext(context.Background(), 100500)
	req := travelersAPI.UpdateBonusCardReq{PassengerId: "1", BonusCardId: "3", BonusCard: &travelersAPI.EditableBonusCard{}}
	uid := "100500"
	tcMock := new(travelersClientMock)
	tcMock.On(
		"UpdateBonusCard", ctx, uid, req.PassengerId, req.BonusCardId, &travelersclient.EditableBonusCard{},
	).Return((*travelersclient.BonusCard)(nil), travelersClientError)
	tCacheMock := new(travelersCacheMock)
	handler := NewGRPCTravelersHandler(&nop.Logger{}, tcMock, tCacheMock, newAdminCacheForTests(), newL10nServiceForTests())

	_, err := handler.UpdateBonusCard(ctx, &req)

	var res interface{ GRPCStatus() *status.Status }
	if assert.ErrorAs(t, err, &res) {
		assert.Equal(t, expectedGRPCStatus, res.GRPCStatus().Code())
	}
	tcMock.AssertExpectations(t)
}

func TestUpdateBonusCardValidationError(t *testing.T) {
	// Проверяем, что не забыли про конвертацию ошибок
	travelersClientError := travelersclient.ValidationError{FieldErrors: map[string]string{"number": "required field"}}
	expectedGRPCStatus := codes.InvalidArgument

	ctx := prepareAuthContext(context.Background(), 100500)
	req := travelersAPI.UpdateBonusCardReq{PassengerId: "1", BonusCardId: "3", BonusCard: &travelersAPI.EditableBonusCard{
		Type: "rzd_bonus",
	}}
	uid := "100500"
	tcMock := new(travelersClientMock)
	tcMock.On(
		"UpdateBonusCard", ctx, uid, req.PassengerId, req.BonusCardId, &travelersclient.EditableBonusCard{
			Type: "rzd_bonus",
		},
	).Return((*travelersclient.BonusCard)(nil), travelersClientError)
	tCacheMock := new(travelersCacheMock)
	handler := NewGRPCTravelersHandler(&nop.Logger{}, tcMock, tCacheMock, newAdminCacheForTests(), newL10nServiceForTests())

	_, err := handler.UpdateBonusCard(ctx, &req)

	var res interface{ GRPCStatus() *status.Status }
	if assert.ErrorAs(t, err, &res) {
		assert.Equal(t, expectedGRPCStatus, res.GRPCStatus().Code())
		assert.Equal(t, "CardValidationFailed", res.GRPCStatus().Message())
		assert.Len(t, res.GRPCStatus().Details(), 1)
		detail := res.GRPCStatus().Details()[0]
		var badRequest *errdetails.BadRequest
		if assert.IsType(t, badRequest, detail) {
			badRequest = detail.(*errdetails.BadRequest)
			assert.Len(t, badRequest.FieldViolations, 1)
			assert.Equal(t, "number", badRequest.FieldViolations[0].Field)
			assert.Equal(t, "required field", badRequest.FieldViolations[0].Description)
		}
	}
	tcMock.AssertExpectations(t)
}

func TestDeleteBonusCard(t *testing.T) {
	ctx := prepareAuthContext(context.Background(), 100500)
	req := travelersAPI.DeleteBonusCardReq{PassengerId: "1", BonusCardId: "1"}
	uid := "100500"
	tcMock := new(travelersClientMock)
	tcMock.On("DeleteBonusCard", ctx, uid, req.PassengerId, req.BonusCardId).Return(nil)
	tCacheMock := new(travelersCacheMock)
	handler := NewGRPCTravelersHandler(&nop.Logger{}, tcMock, tCacheMock, newAdminCacheForTests(), newL10nServiceForTests())

	_, err := handler.DeleteBonusCard(ctx, &req)

	require.NoError(t, err)
	tcMock.AssertExpectations(t)
}

func TestDeleteBonusCardError(t *testing.T) {
	// Проверяем, что не забыли про конвертацию ошибок
	travelersClientError := travelersclient.StatusError{Status: http.StatusNotFound}
	expectedGRPCStatus := codes.NotFound

	ctx := prepareAuthContext(context.Background(), 100500)
	req := travelersAPI.DeleteBonusCardReq{PassengerId: "1", BonusCardId: "1"}
	uid := "100500"
	tcMock := new(travelersClientMock)
	tcMock.On("DeleteBonusCard", ctx, uid, req.PassengerId, req.BonusCardId).Return(travelersClientError)
	tCacheMock := new(travelersCacheMock)
	handler := NewGRPCTravelersHandler(&nop.Logger{}, tcMock, tCacheMock, newAdminCacheForTests(), newL10nServiceForTests())

	_, err := handler.DeleteBonusCard(ctx, &req)

	var res interface{ GRPCStatus() *status.Status }
	if assert.ErrorAs(t, err, &res) {
		assert.Equal(t, expectedGRPCStatus, res.GRPCStatus().Code())
	}
	tcMock.AssertExpectations(t)
}
