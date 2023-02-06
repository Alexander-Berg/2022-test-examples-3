package mocks

import (
	"github.com/stretchr/testify/mock"

	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/helpers"
)

type JobIDGeneratorMock struct {
	helpers.JobIDGenerator
	mock.Mock
}

func (jobIDGeneratorMock *JobIDGeneratorMock) Generate() string {
	args := jobIDGeneratorMock.Called()
	return args.String(0)
}
