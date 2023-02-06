package mocks

import (
	"github.com/stretchr/testify/mock"

	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/domain/models"
	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/repositories"
)

type CompanyRepositoryMock struct {
	repositories.Company
	mock.Mock
}

func (companyRepositoryMock *CompanyRepositoryMock) GetByID(id int) (*models.Company, bool) {
	args := companyRepositoryMock.Called(id)
	return args.Get(0).(*models.Company), args.Bool(1)
}
