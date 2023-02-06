package testing

import (
	"context"

	"a.yandex-team.ru/travel/komod/trips/internal/orders"
	"a.yandex-team.ru/travel/komod/trips/internal/trips"
	tripsmodels "a.yandex-team.ru/travel/komod/trips/internal/trips/models"
)

type fakeStorage struct {
	tripByID map[string]*tripsmodels.Trip
}

func NewFakeStorage() *fakeStorage {
	return &fakeStorage{tripByID: make(map[string]*tripsmodels.Trip, 0)}
}

func (s *fakeStorage) GetSession() trips.StorageSession {
	return &fakeSession{
		storage: s,
	}
}

func (s *fakeStorage) ExecuteInTransaction(txBody func(tx trips.StorageTxSession) error) error {
	return txBody(
		&fakeSession{
			storage: s,
		},
	)
}

type fakeSession struct {
	storage *fakeStorage
}

func (s *fakeSession) GetTrips(_ context.Context, _ string) (tripsmodels.Trips, error) {
	var resultTrips = make(tripsmodels.Trips, 0, len(s.storage.tripByID))
	for _, t := range s.storage.tripByID {
		resultTrips = append(resultTrips, t)
	}
	return resultTrips, nil
}

func (s *fakeSession) GetTrip(_ context.Context, tripID string) (*tripsmodels.Trip, error) {
	return s.storage.tripByID[tripID], nil
}

func (s *fakeSession) LockUser(_ context.Context, _ string) error {
	return nil
}

func (s *fakeSession) RemoveTrips(_ context.Context, items ...*tripsmodels.Trip) error {
	for _, trip := range items {
		delete(s.storage.tripByID, trip.ID)
	}
	return nil
}

func (s *fakeSession) RemoveTripOrderSpans(_ context.Context, tripID string) error {
	if t, ok := s.storage.tripByID[tripID]; ok {
		t.OrderInfos = make(map[orders.ID]tripsmodels.OrderInfo)
	}
	return nil
}

func (s fakeSession) UpsertTrips(_ context.Context, items ...*tripsmodels.Trip) error {
	for _, trip := range items {
		s.storage.tripByID[trip.ID] = trip
	}
	return nil
}
