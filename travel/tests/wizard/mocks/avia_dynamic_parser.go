package mocks

import (
	"github.com/stretchr/testify/mock"

	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/domain/flags"
	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/domain/parameters/dynamic"
)

type AviaDynamicParserMock struct {
	mock.Mock
}

func (aviaDynamicParserMock *AviaDynamicParserMock) Parse(rawInput string) (*dynamic.AviaDynamic, error) {
	args := aviaDynamicParserMock.Called(rawInput)
	return args.Get(0).(*dynamic.AviaDynamic), args.Error(1)
}

type FlagsParserMock struct {
	mock.Mock
}

func (flagsParserMock *FlagsParserMock) Parse(rawInput string) (flags.Flags, error) {
	args := flagsParserMock.Called(rawInput)
	return args.Get(0).(flags.Flags), args.Error(1)
}
