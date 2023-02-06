package extractors

import (
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/travel/notifier/internal/orders"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
)

func TestRoutePointsExtractor(t *testing.T) {
	newStationsRepository := func(stations ...*rasp.TStation) *fakeStationsRepository {
		byID := make(map[int]*rasp.TStation, len(stations))
		for _, station := range stations {
			byID[int(station.Id)] = station
		}

		return &fakeStationsRepository{byID: byID}
	}
	newSettlementsRepository := func(settlements ...*rasp.TSettlement) *fakeSettlementsRepository {
		byGeoID := make(map[int]*rasp.TSettlement, len(settlements))
		byCode := make(map[string]*rasp.TSettlement, len(settlements))
		for _, settlement := range settlements {
			byGeoID[int(settlement.GeoId)] = settlement
			byCode[settlement.Iata] = settlement
			byCode[settlement.SirenaId] = settlement
		}

		return &fakeSettlementsRepository{byGeoID: byGeoID, byCode: byCode}
	}
	newStationCodesRepository := func(stationCodes ...*rasp.TStationCode) *fakeStationCodesRepository {
		byCode := make(map[string]int32, len(stationCodes))
		byExpress := make(map[string]int32, len(stationCodes))
		for _, stationCode := range stationCodes {
			if stationCode.SystemId == rasp.ECodeSystem_CODE_SYSTEM_EXPRESS {
				byExpress[stationCode.Code] = stationCode.StationId
			} else {
				byCode[stationCode.Code] = stationCode.StationId
			}
		}
		return &fakeStationCodesRepository{byCode: byCode, byExpress: byExpress}
	}
	newStationIDToSettlementIDMapper := func(stationToSettlements ...*rasp.TStation2Settlement) *fakeStationIDToSettlementIDMapper {
		settlementIDByStationID := make(map[int32]int32, len(stationToSettlements))
		for _, stationToSettlement := range stationToSettlements {
			settlementIDByStationID[stationToSettlement.StationId] = stationToSettlement.SettlementId
		}

		return &fakeStationIDToSettlementIDMapper{settlementIDByStationID: settlementIDByStationID}
	}
	t.Run(
		"ExtractArrivalSettlementID/avia order/correct order", func(t *testing.T) {
			stationCodesRepository := newStationCodesRepository(&rasp.TStationCode{StationId: 9600213})
			settlementsRepository := newSettlementsRepository(&rasp.TSettlement{Id: 213, Iata: "MOW"})
			extractor := NewRoutePointsFromOrderExtractor(
				newStationsRepository(),
				settlementsRepository,
				stationCodesRepository,
				newStationIDToSettlementIDMapper(),
			)

			settlementID, err := extractor.ExtractArrivalSettlementID(
				&orders.OrderInfo{
					Type: orders.OrderTypeAvia,
					AviaOrderItems: []*orders.AviaOrderItem{
						{
							OriginDestinations: []*orders.AviaOriginDestination{
								{
									ArrivalStation: "MOW",
								},
							},
						},
					},
				},
			)

			require.NoError(t, err)
			require.EqualValues(t, 213, settlementID)
		},
	)
	t.Run(
		"ExtractArrivalSettlementID/avia order/correct order with unknown settlement", func(t *testing.T) {
			stationCodesRepository := newStationCodesRepository(&rasp.TStationCode{StationId: 9600213, Code: "SVO"})
			settlementsRepository := newSettlementsRepository(&rasp.TSettlement{Id: 213})
			stationIDToSettlementIDMapper := newStationIDToSettlementIDMapper(
				&rasp.TStation2Settlement{
					StationId:    9600213,
					SettlementId: 213,
				},
			)
			extractor := NewRoutePointsFromOrderExtractor(
				newStationsRepository(),
				settlementsRepository,
				stationCodesRepository,
				stationIDToSettlementIDMapper,
			)

			settlementID, err := extractor.ExtractArrivalSettlementID(
				&orders.OrderInfo{
					Type: orders.OrderTypeAvia,
					AviaOrderItems: []*orders.AviaOrderItem{
						{
							OriginDestinations: []*orders.AviaOriginDestination{
								{
									ArrivalStation: "MOW",
									Segments:       []*orders.AviaSegment{{ArrivalStation: "SVO"}},
								},
							},
						},
					},
				},
			)

			require.NoError(t, err)
			require.EqualValues(t, 213, settlementID)
		},
	)

	t.Run(
		"ExtractArrivalSettlementID/avia order/same points", func(t *testing.T) {
			stationCodesRepository := newStationCodesRepository(&rasp.TStationCode{StationId: 9600213, Code: "SVO"})
			settlementsRepository := newSettlementsRepository(&rasp.TSettlement{Id: 213})
			stationIDToSettlementIDMapper := newStationIDToSettlementIDMapper(
				&rasp.TStation2Settlement{
					StationId:    9600213,
					SettlementId: 213,
				},
			)
			extractor := NewRoutePointsFromOrderExtractor(
				newStationsRepository(),
				settlementsRepository,
				stationCodesRepository,
				stationIDToSettlementIDMapper,
			)

			_, err := extractor.ExtractArrivalSettlementID(
				&orders.OrderInfo{
					Type: orders.OrderTypeAvia,
					AviaOrderItems: []*orders.AviaOrderItem{
						{
							OriginDestinations: []*orders.AviaOriginDestination{
								{
									ArrivalStation: "MOW",
									Segments:       []*orders.AviaSegment{{DepartureStation: "SVO"}, {ArrivalStation: "SVO"}},
								},
							},
						},
					},
				},
			)

			require.Error(t, err)
			require.IsType(t, &ErrSameOriginDestination{}, err)
		},
	)

	t.Run(
		"ExtractArrivalSettlementID/avia order/no settlement code, no segments", func(t *testing.T) {
			extractor := NewRoutePointsFromOrderExtractor(
				newStationsRepository(),
				newSettlementsRepository(),
				newStationCodesRepository(),
				newStationIDToSettlementIDMapper(),
			)

			_, err := extractor.ExtractArrivalSettlementID(
				&orders.OrderInfo{
					ID:   "1",
					Type: orders.OrderTypeAvia,
					AviaOrderItems: []*orders.AviaOrderItem{
						{OriginDestinations: []*orders.AviaOriginDestination{{ArrivalStation: ""}}},
					},
				},
			)

			require.Error(t, err)
			require.IsType(t, &ErrNoDestination{}, err)
		},
	)

	t.Run(
		"ExtractArrivalSettlementID/avia order/no settlement code, invalid station code", func(t *testing.T) {
			extractor := NewRoutePointsFromOrderExtractor(
				newStationsRepository(),
				newSettlementsRepository(),
				newStationCodesRepository(),
				newStationIDToSettlementIDMapper(),
			)

			_, err := extractor.ExtractArrivalSettlementID(
				&orders.OrderInfo{
					ID:   "1",
					Type: orders.OrderTypeAvia,
					AviaOrderItems: []*orders.AviaOrderItem{
						{
							OriginDestinations: []*orders.AviaOriginDestination{
								{
									ArrivalStation: "",
									Segments:       []*orders.AviaSegment{{ArrivalStation: ""}},
								},
							},
						},
					},
				},
			)

			require.Error(t, err)
			require.IsType(t, &ErrInvalidDestination{}, err)
		},
	)

	t.Run(
		"ExtractArrivalSettlementID/avia order/unknown settlement code unknown station code", func(t *testing.T) {
			extractor := NewRoutePointsFromOrderExtractor(
				newStationsRepository(),
				newSettlementsRepository(),
				newStationCodesRepository(),
				newStationIDToSettlementIDMapper(),
			)

			_, err := extractor.ExtractArrivalSettlementID(
				&orders.OrderInfo{
					ID:   "1",
					Type: orders.OrderTypeAvia,
					AviaOrderItems: []*orders.AviaOrderItem{
						{
							OriginDestinations: []*orders.AviaOriginDestination{
								{
									ArrivalStation: "LOL",
									Segments:       []*orders.AviaSegment{{ArrivalStation: "STA"}},
								},
							},
						},
					},
				},
			)

			require.Error(t, err)
			require.IsType(t, &ErrUnknownDestination{}, err)
		},
	)

	t.Run(
		"ExtractArrivalSettlementID/avia order/station without settlement", func(t *testing.T) {
			stationCodesRepository := newStationCodesRepository(&rasp.TStationCode{StationId: 9600213, Code: "SVO"})
			stationsRepository := newStationsRepository(&rasp.TStation{Id: 9600213})
			extractor := NewRoutePointsFromOrderExtractor(
				stationsRepository,
				newSettlementsRepository(),
				stationCodesRepository,
				newStationIDToSettlementIDMapper(),
			)

			_, err := extractor.ExtractArrivalSettlementID(
				&orders.OrderInfo{
					ID:   "1",
					Type: orders.OrderTypeAvia,
					AviaOrderItems: []*orders.AviaOrderItem{
						{
							OriginDestinations: []*orders.AviaOriginDestination{
								{
									ArrivalStation: "MOW",
									Segments:       []*orders.AviaSegment{{ArrivalStation: "SVO"}},
								},
							},
						},
					},
				},
			)

			require.Error(t, err)
			require.IsType(t, &ErrDestinationWithoutSettlement{}, err)
		},
	)

	t.Run(
		"ExtractArrivalSettlementID/avia order/originDestination item is nil", func(t *testing.T) {
			extractor := NewRoutePointsFromOrderExtractor(
				newStationsRepository(),
				newSettlementsRepository(),
				newStationCodesRepository(),
				newStationIDToSettlementIDMapper(),
			)

			_, err := extractor.ExtractArrivalSettlementID(
				&orders.OrderInfo{
					ID:   "1",
					Type: orders.OrderTypeAvia,
					AviaOrderItems: []*orders.AviaOrderItem{
						{OriginDestinations: []*orders.AviaOriginDestination{nil}},
					},
				},
			)

			require.Error(t, err)
			require.IsType(t, &ErrNoDestination{}, err)
		},
	)

	t.Run(
		"ExtractArrivalSettlementID/avia order/originDestinations list is empty", func(t *testing.T) {
			extractor := NewRoutePointsFromOrderExtractor(
				newStationsRepository(),
				newSettlementsRepository(),
				newStationCodesRepository(),
				newStationIDToSettlementIDMapper(),
			)

			_, err := extractor.ExtractArrivalSettlementID(
				&orders.OrderInfo{
					ID:   "1",
					Type: orders.OrderTypeAvia,
					AviaOrderItems: []*orders.AviaOrderItem{
						{OriginDestinations: []*orders.AviaOriginDestination{}},
					},
				},
			)

			require.Error(t, err)
			require.IsType(t, &ErrNoDestination{}, err)
		},
	)

	t.Run(
		"ExtractArrivalSettlementID/avia order/originDestinations list is nil", func(t *testing.T) {
			extractor := NewRoutePointsFromOrderExtractor(
				newStationsRepository(),
				newSettlementsRepository(),
				newStationCodesRepository(),
				newStationIDToSettlementIDMapper(),
			)

			_, err := extractor.ExtractArrivalSettlementID(
				&orders.OrderInfo{
					ID:   "1",
					Type: orders.OrderTypeAvia,
					AviaOrderItems: []*orders.AviaOrderItem{
						{OriginDestinations: nil},
					},
				},
			)

			require.Error(t, err)
			require.IsType(t, &ErrNoDestination{}, err)
		},
	)

	t.Run(
		"ExtractArrivalSettlementID/avia order/order item is nil", func(t *testing.T) {
			extractor := NewRoutePointsFromOrderExtractor(
				newStationsRepository(),
				newSettlementsRepository(),
				newStationCodesRepository(),
				newStationIDToSettlementIDMapper(),
			)

			_, err := extractor.ExtractArrivalSettlementID(
				&orders.OrderInfo{
					ID:             "1",
					Type:           orders.OrderTypeAvia,
					AviaOrderItems: []*orders.AviaOrderItem{nil},
				},
			)

			require.Error(t, err)
			require.IsType(t, &ErrNoOrderItems{}, err)
		},
	)

	t.Run(
		"ExtractArrivalSettlementID/avia order/order items list is nil", func(t *testing.T) {
			extractor := NewRoutePointsFromOrderExtractor(
				newStationsRepository(),
				newSettlementsRepository(),
				newStationCodesRepository(),
				newStationIDToSettlementIDMapper(),
			)

			_, err := extractor.ExtractArrivalSettlementID(
				&orders.OrderInfo{
					ID:             "1",
					Type:           orders.OrderTypeAvia,
					AviaOrderItems: nil,
				},
			)

			require.Error(t, err)
			require.IsType(t, &ErrNoOrderItems{}, err)
		},
	)

	t.Run(
		"ExtractArrivalSettlementID/train order/correct order", func(t *testing.T) {
			stationsRepository := newStationsRepository(&rasp.TStation{Id: 9600213})
			stationIDToSettlementIDMapper := newStationIDToSettlementIDMapper(
				&rasp.TStation2Settlement{
					StationId:    9600213,
					SettlementId: 213,
				},
			)
			extractor := NewRoutePointsFromOrderExtractor(
				stationsRepository,
				newSettlementsRepository(),
				newStationCodesRepository(),
				stationIDToSettlementIDMapper,
			)

			settlementID, err := extractor.ExtractArrivalSettlementID(
				&orders.OrderInfo{
					Type: orders.OrderTypeTrain,
					TrainOrderItems: []*orders.TrainOrderItem{
						{
							ArrivalStation: "9600213",
						},
					},
				},
			)

			require.NoError(t, err)
			require.EqualValues(t, 213, settlementID)
		},
	)

	t.Run(
		"ExtractArrivalSettlementID/train order/correct order with express code", func(t *testing.T) {
			stationsRepository := newStationsRepository(&rasp.TStation{Id: 9600213})
			stationIDToSettlementIDMapper := newStationIDToSettlementIDMapper(
				&rasp.TStation2Settlement{
					StationId:    9600213,
					SettlementId: 213,
				},
			)
			stationCodesRepository := newStationCodesRepository(
				&rasp.TStationCode{
					Code:      "CODE",
					SystemId:  rasp.ECodeSystem_CODE_SYSTEM_EXPRESS,
					StationId: 9600213,
				},
			)
			extractor := NewRoutePointsFromOrderExtractor(
				stationsRepository,
				newSettlementsRepository(),
				stationCodesRepository,
				stationIDToSettlementIDMapper,
			)

			settlementID, err := extractor.ExtractArrivalSettlementID(
				&orders.OrderInfo{
					Type: orders.OrderTypeTrain,
					TrainOrderItems: []*orders.TrainOrderItem{
						{
							ArrivalStation: "CODE",
						},
					},
				},
			)

			require.NoError(t, err)
			require.EqualValues(t, 213, settlementID)
		},
	)

	t.Run(
		"ExtractArrivalSettlementID/train order/invalid station point key", func(t *testing.T) {
			extractor := NewRoutePointsFromOrderExtractor(
				newStationsRepository(),
				newSettlementsRepository(),
				newStationCodesRepository(),
				newStationIDToSettlementIDMapper(),
			)

			_, err := extractor.ExtractArrivalSettlementID(
				&orders.OrderInfo{
					ID:              "1",
					Type:            orders.OrderTypeTrain,
					TrainOrderItems: []*orders.TrainOrderItem{{ArrivalStation: ""}},
				},
			)

			require.Error(t, err)
			require.IsType(t, &ErrInvalidDestination{}, err)
		},
	)

	t.Run(
		"ExtractArrivalSettlementID/train order/unknown station", func(t *testing.T) {
			extractor := NewRoutePointsFromOrderExtractor(
				newStationsRepository(),
				newSettlementsRepository(),
				newStationCodesRepository(),
				newStationIDToSettlementIDMapper(),
			)

			_, err := extractor.ExtractArrivalSettlementID(
				&orders.OrderInfo{
					ID:              "1",
					Type:            orders.OrderTypeTrain,
					TrainOrderItems: []*orders.TrainOrderItem{{ArrivalStation: "9600213"}},
				},
			)

			require.Error(t, err)
			require.IsType(t, &ErrUnknownDestination{}, err)
		},
	)

	t.Run(
		"ExtractArrivalSettlementID/train order/station without settlement", func(t *testing.T) {
			stationsRepository := newStationsRepository(&rasp.TStation{Id: 9600213})
			extractor := NewRoutePointsFromOrderExtractor(
				stationsRepository,
				newSettlementsRepository(),
				newStationCodesRepository(),
				newStationIDToSettlementIDMapper(),
			)

			_, err := extractor.ExtractArrivalSettlementID(
				&orders.OrderInfo{
					ID:              "1",
					Type:            orders.OrderTypeTrain,
					TrainOrderItems: []*orders.TrainOrderItem{{ArrivalStation: "9600213"}},
				},
			)

			require.Error(t, err)
			require.IsType(t, &ErrDestinationWithoutSettlement{}, err)
		},
	)

	t.Run(
		"ExtractArrivalSettlementID/train order/order item is nil", func(t *testing.T) {
			extractor := NewRoutePointsFromOrderExtractor(
				newStationsRepository(),
				newSettlementsRepository(),
				newStationCodesRepository(),
				newStationIDToSettlementIDMapper(),
			)

			_, err := extractor.ExtractArrivalSettlementID(
				&orders.OrderInfo{
					ID:              "1",
					Type:            orders.OrderTypeTrain,
					TrainOrderItems: []*orders.TrainOrderItem{nil},
				},
			)

			require.Error(t, err)
			require.IsType(t, &ErrNoOrderItems{}, err)
		},
	)

	t.Run(
		"ExtractArrivalSettlementID/train order/order items list is nil", func(t *testing.T) {
			extractor := NewRoutePointsFromOrderExtractor(
				newStationsRepository(),
				newSettlementsRepository(),
				newStationCodesRepository(),
				newStationIDToSettlementIDMapper(),
			)

			_, err := extractor.ExtractArrivalSettlementID(
				&orders.OrderInfo{
					ID:              "1",
					Type:            orders.OrderTypeTrain,
					TrainOrderItems: nil,
				},
			)

			require.Error(t, err)
			require.IsType(t, &ErrNoOrderItems{}, err)
		},
	)

	t.Run(
		"ExtractArrivalSettlementID/hotel order/correct order", func(t *testing.T) {
			settlementsRepository := newSettlementsRepository(&rasp.TSettlement{Id: 213, GeoId: 213})
			extractor := NewRoutePointsFromOrderExtractor(
				newStationsRepository(),
				settlementsRepository,
				newStationCodesRepository(),
				newStationIDToSettlementIDMapper(),
			)

			settlementID, err := extractor.ExtractArrivalSettlementID(
				&orders.OrderInfo{
					Type: orders.OrderTypeHotel,
					HotelOrderItems: []*orders.HotelOrderItem{
						{
							GeoRegions: []*orders.GeoRegion{
								{
									GeoID: 213,
									Type:  cityGeoIDType,
								},
							},
						},
					},
				},
			)

			require.NoError(t, err)
			require.EqualValues(t, 213, settlementID)
		},
	)

	t.Run(
		"ExtractArrivalSettlementID/hotel order/correct order with village", func(t *testing.T) {
			settlementsRepository := newSettlementsRepository(&rasp.TSettlement{Id: 10994, GeoId: 10994})
			extractor := NewRoutePointsFromOrderExtractor(
				newStationsRepository(),
				settlementsRepository,
				newStationCodesRepository(),
				newStationIDToSettlementIDMapper(),
			)

			settlementID, err := extractor.ExtractArrivalSettlementID(
				&orders.OrderInfo{
					Type: orders.OrderTypeHotel,
					HotelOrderItems: []*orders.HotelOrderItem{
						{
							GeoRegions: []*orders.GeoRegion{
								{
									GeoID: 10994,
									Type:  villageGeoIDType,
								},
								{
									GeoID: 213,
									Type:  cityGeoIDType,
								},
							},
						},
					},
				},
			)

			require.NoError(t, err)
			require.EqualValues(t, 10994, settlementID)
		},
	)

	t.Run(
		"ExtractArrivalSettlementID/hotel order/order item is nil", func(t *testing.T) {
			extractor := NewRoutePointsFromOrderExtractor(
				newStationsRepository(),
				newSettlementsRepository(),
				newStationCodesRepository(),
				newStationIDToSettlementIDMapper(),
			)

			_, err := extractor.ExtractArrivalSettlementID(
				&orders.OrderInfo{
					ID:              "1",
					Type:            orders.OrderTypeHotel,
					HotelOrderItems: []*orders.HotelOrderItem{nil},
				},
			)

			require.Error(t, err)
			require.IsType(t, &ErrNoOrderItems{}, err)
		},
	)

	t.Run(
		"ExtractArrivalSettlementID/hotel order/order items list is nil", func(t *testing.T) {
			extractor := NewRoutePointsFromOrderExtractor(
				newStationsRepository(),
				newSettlementsRepository(),
				newStationCodesRepository(),
				newStationIDToSettlementIDMapper(),
			)

			_, err := extractor.ExtractArrivalSettlementID(
				&orders.OrderInfo{
					ID:              "1",
					Type:            orders.OrderTypeHotel,
					HotelOrderItems: nil,
				},
			)

			require.Error(t, err)
			require.IsType(t, &ErrNoOrderItems{}, err)
		},
	)

	t.Run(
		"ExtractArrivalSettlementID/hotel order/geo-regions list is nil", func(t *testing.T) {
			extractor := NewRoutePointsFromOrderExtractor(
				newStationsRepository(),
				newSettlementsRepository(),
				newStationCodesRepository(),
				newStationIDToSettlementIDMapper(),
			)

			_, err := extractor.ExtractArrivalSettlementID(
				&orders.OrderInfo{
					ID:              "1",
					Type:            orders.OrderTypeHotel,
					HotelOrderItems: []*orders.HotelOrderItem{{GeoRegions: nil}},
				},
			)

			require.Error(t, err)
			require.IsType(t, &ErrNoDestination{}, err)
		},
	)

	t.Run(
		"ExtractArrivalSettlementID/hotel order/geo-regions list is empty", func(t *testing.T) {
			extractor := NewRoutePointsFromOrderExtractor(
				newStationsRepository(),
				newSettlementsRepository(),
				newStationCodesRepository(),
				newStationIDToSettlementIDMapper(),
			)

			_, err := extractor.ExtractArrivalSettlementID(
				&orders.OrderInfo{
					ID:              "1",
					Type:            orders.OrderTypeHotel,
					HotelOrderItems: []*orders.HotelOrderItem{{GeoRegions: []*orders.GeoRegion{}}},
				},
			)

			require.Error(t, err)
			require.IsType(t, &ErrNoDestination{}, err)
		},
	)

	t.Run(
		"ExtractArrivalSettlementID/hotel order/geo-regions list doesn't contain settlement geoID", func(t *testing.T) {
			extractor := NewRoutePointsFromOrderExtractor(
				newStationsRepository(),
				newSettlementsRepository(),
				newStationCodesRepository(),
				newStationIDToSettlementIDMapper(),
			)

			_, err := extractor.ExtractArrivalSettlementID(
				&orders.OrderInfo{
					ID:   "1",
					Type: orders.OrderTypeHotel,
					HotelOrderItems: []*orders.HotelOrderItem{
						{
							GeoRegions: []*orders.GeoRegion{{GeoID: 123, Type: 1}},
						},
					},
				},
			)

			require.Error(t, err)
			require.IsType(t, &ErrHotelOrderWithoutSettlementGeoID{}, err)
		},
	)

	t.Run(
		"ExtractArrivalSettlementID/hotel order/unknown settlement geoID", func(t *testing.T) {
			extractor := NewRoutePointsFromOrderExtractor(
				newStationsRepository(),
				newSettlementsRepository(),
				newStationCodesRepository(),
				newStationIDToSettlementIDMapper(),
			)

			_, err := extractor.ExtractArrivalSettlementID(
				&orders.OrderInfo{
					ID:   "1",
					Type: orders.OrderTypeHotel,
					HotelOrderItems: []*orders.HotelOrderItem{
						{
							GeoRegions: []*orders.GeoRegion{{GeoID: 123, Type: cityGeoIDType}},
						},
					},
				},
			)

			require.Error(t, err)
			require.IsType(t, &ErrUnknownGeoID{}, err)
		},
	)

	t.Run(
		"ExtractArrivalStationID/train order/correct order", func(t *testing.T) {
			stationsRepository := newStationsRepository(&rasp.TStation{Id: 9600213})
			stationIDToSettlementIDMapper := newStationIDToSettlementIDMapper()
			extractor := NewRoutePointsFromOrderExtractor(
				stationsRepository,
				newSettlementsRepository(),
				newStationCodesRepository(),
				stationIDToSettlementIDMapper,
			)

			stationID, err := extractor.ExtractArrivalStationID(
				&orders.OrderInfo{
					Type: orders.OrderTypeTrain,
					TrainOrderItems: []*orders.TrainOrderItem{
						{
							ArrivalStation: "9600213",
						},
					},
				},
			)

			require.NoError(t, err)
			require.EqualValues(t, 9600213, stationID)
		},
	)
	t.Run(
		"ExtractDepartureStationID/train order/correct order", func(t *testing.T) {
			stationsRepository := newStationsRepository(&rasp.TStation{Id: 9600213})
			stationIDToSettlementIDMapper := newStationIDToSettlementIDMapper()
			extractor := NewRoutePointsFromOrderExtractor(
				stationsRepository,
				newSettlementsRepository(),
				newStationCodesRepository(),
				stationIDToSettlementIDMapper,
			)

			stationID, err := extractor.ExtractDepartureStationID(
				&orders.OrderInfo{
					Type: orders.OrderTypeTrain,
					TrainOrderItems: []*orders.TrainOrderItem{
						{
							DepartureStation: "9600213",
						},
					},
				},
			)

			require.NoError(t, err)
			require.EqualValues(t, 9600213, stationID)
		},
	)
}

type fakeSettlementsRepository struct {
	byGeoID map[int]*rasp.TSettlement
	byCode  map[string]*rasp.TSettlement
}

func (f *fakeSettlementsRepository) GetByGeoID(id int) (*rasp.TSettlement, bool) {
	station, ok := f.byGeoID[id]
	return station, ok
}

func (f *fakeSettlementsRepository) GetByCode(code string) (*rasp.TSettlement, bool) {
	station, ok := f.byCode[code]
	return station, ok
}

func (f *fakeSettlementsRepository) Get(id int) (*rasp.TSettlement, bool) {
	return nil, false
}

type fakeStationCodesRepository struct {
	byCode    map[string]int32
	byExpress map[string]int32
}

func (f *fakeStationCodesRepository) GetStationIDByCode(code string) (int32, bool) {
	station, ok := f.byCode[code]
	return station, ok
}

func (f *fakeStationCodesRepository) GetStationIDByExpressCode(code string) (int32, bool) {
	station, ok := f.byExpress[code]
	return station, ok
}

type fakeStationIDToSettlementIDMapper struct {
	settlementIDByStationID map[int32]int32
}

func (f *fakeStationIDToSettlementIDMapper) Map(stationID int32) (settlementID int32, found bool) {
	station, ok := f.settlementIDByStationID[stationID]
	return station, ok
}
