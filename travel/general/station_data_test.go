package extractors

import (
	"testing"

	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/travel/proto/dicts/rasp"
)

type mockStationsRepository struct {
	mock.Mock
}

func (s *mockStationsRepository) Get(id int) (*rasp.TStation, bool) {
	args := s.Called(id)
	return args.Get(0).(*rasp.TStation), args.Bool(1)
}

type mockStationToSettlementRepository struct {
	mock.Mock
}

func (s *mockStationToSettlementRepository) Get(id int) (*rasp.TStation2Settlement, bool) {
	args := s.Called(id)
	arg0 := args.Get(0)
	if arg0 == nil {
		return nil, args.Bool(1)
	}
	return args.Get(0).(*rasp.TStation2Settlement), args.Bool(1)
}

type mockStationCodesRepository struct {
	mock.Mock
}

func (s *mockStationCodesRepository) GetStationIDByCode(code string) (int32, bool) {
	args := s.Called(code)
	return args.Get(0).(int32), args.Bool(1)
}

func (s *mockStationCodesRepository) GetStationIDByExpressCode(code string) (int32, bool) {
	args := s.Called(code)
	return args.Get(0).(int32), args.Bool(1)
}

type mockSettlementsRepository struct {
	mock.Mock
}

func (s *mockSettlementsRepository) GetByGeoID(id int) (*rasp.TSettlement, bool) {
	args := s.Called(id)
	return args.Get(0).(*rasp.TSettlement), args.Bool(1)
}

func (s *mockSettlementsRepository) GetByCode(code string) (*rasp.TSettlement, bool) {
	args := s.Called(code)
	return args.Get(0).(*rasp.TSettlement), args.Bool(1)
}

func (s *mockSettlementsRepository) Get(id int) (*rasp.TSettlement, bool) {
	args := s.Called(id)
	return args.Get(0).(*rasp.TSettlement), args.Bool(1)
}

func TestStationData(t *testing.T) {
	t.Run(
		"GetStationByID", func(t *testing.T) {

			mockStationsRepository := &mockStationsRepository{}

			mockStationsRepository.On("Get", 2000001).Return(
				&rasp.TStation{
					Id: 2000001,
				},
				true,
			)

			stationDataProvider := NewStationDataProvider(
				mockStationsRepository,
				&mockStationToSettlementRepository{},
				&mockSettlementsRepository{},
				&mockStationCodesRepository{},
			)

			station, found := stationDataProvider.GetStationByID(2000001)

			require.True(t, found)
			require.EqualValues(t, 2000001, station.Id)
		},
	)

	t.Run(
		"GetSettlementByStationID", func(t *testing.T) {

			mockStationToSettlementRepository := &mockStationToSettlementRepository{}
			mockSettlementsRepository := &mockSettlementsRepository{}
			mockStationsRepository := &mockStationsRepository{}
			mockStationCodesRepository := &mockStationCodesRepository{}

			mockStationToSettlementRepository.On("Get", 201).Return(
				&rasp.TStation2Settlement{
					SettlementId: 402,
				},
				true,
			)
			mockStationToSettlementRepository.On("Get", 301).Return(nil, false)

			mockStationsRepository.On("Get", 301).Return(
				&rasp.TStation{
					Id:           301,
					SettlementId: 602,
				},
				true,
			)

			mockSettlementsRepository.On("Get", 402).Return(
				&rasp.TSettlement{
					TitleDefault: "settlement-402",
				},
				true,
			)

			mockSettlementsRepository.On("Get", 602).Return(
				&rasp.TSettlement{
					TitleDefault: "settlement-602",
				},
				true,
			)

			stationDataProvider := NewStationDataProvider(
				mockStationsRepository,
				mockStationToSettlementRepository,
				mockSettlementsRepository,
				mockStationCodesRepository,
			)

			settlement201, found201 := stationDataProvider.GetSettlementByStationID(201)
			require.True(t, found201)
			require.EqualValues(t, "settlement-402", settlement201.TitleDefault)

			settlement301, found301 := stationDataProvider.GetSettlementByStationID(301)
			require.True(t, found301)
			require.EqualValues(t, "settlement-602", settlement301.TitleDefault)
		},
	)
}
