package mocks

import (
	"github.com/stretchr/testify/mock"

	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/domain/models"
	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/repositories"
)

type SettlementRepositoryMock struct {
	repositories.Settlement
	mock.Mock
}

func (settlementRepositoryMock *SettlementRepositoryMock) GetByID(id int) (*models.Settlement, bool) {
	args := settlementRepositoryMock.Called(id)
	return args.Get(0).(*models.Settlement), args.Bool(1)
}
