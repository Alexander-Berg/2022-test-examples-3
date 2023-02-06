package mocks

import (
	"github.com/stretchr/testify/mock"

	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/domain/models"
)

type TranslatedTitleRepositoryMock struct {
	mock.Mock
}

func (t *TranslatedTitleRepositoryMock) GetByID(id int) (title *models.TranslatedTitle, found bool) {
	panic("implement me")
}

func (t *TranslatedTitleRepositoryMock) GetTitleTranslation(
	titleID int,
	lang models.Lang,
	grammaticalCase models.GrammaticalCase,
) (string, error) {
	args := t.Called(titleID, lang, grammaticalCase)
	return args.String(0), args.Error(1)
}

func (t *TranslatedTitleRepositoryMock) GetOldTitleTranslation(model interface{}, lang models.Lang) (string, error) {
	args := t.Called(model, lang)
	return args.String(0), args.Error(1)
}
