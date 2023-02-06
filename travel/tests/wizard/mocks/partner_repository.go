package mocks

import (
	"github.com/stretchr/testify/mock"

	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/domain/models"
	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/lib/containers"
	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/repositories"
)

type PartnerRepositoryMock struct {
	repositories.Partner
	mock.Mock
}

func (partnerRepositoryMock *PartnerRepositoryMock) GetDisabledPartnersCodes(nationalVersion string) containers.SetOfString {
	args := partnerRepositoryMock.Called(nationalVersion)
	return args.Get(0).(containers.SetOfString)
}

func (partnerRepositoryMock *PartnerRepositoryMock) GetPartnerByCode(code string) (*models.Partner, bool) {
	args := partnerRepositoryMock.Called(code)
	return args.Get(0).(*models.Partner), args.Bool(1)
}

func (partnerRepositoryMock *PartnerRepositoryMock) IsEnabled(partnerCode, nationalVersion string) bool {
	args := partnerRepositoryMock.Called(partnerCode, nationalVersion)
	return args.Bool(0)
}
