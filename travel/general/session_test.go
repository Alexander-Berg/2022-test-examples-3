package db

import (
	"context"
	"fmt"
	"math/rand"
	"testing"
	"time"

	"github.com/gofrs/uuid"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"golang.yandex/hasql"
	"google.golang.org/protobuf/proto"

	"a.yandex-team.ru/library/go/units"
	"a.yandex-team.ru/travel/komod/trips/internal/comparators"
	"a.yandex-team.ru/travel/komod/trips/internal/extractors"
	"a.yandex-team.ru/travel/komod/trips/internal/helpers"
	"a.yandex-team.ru/travel/komod/trips/internal/models"
	"a.yandex-team.ru/travel/komod/trips/internal/orders"
	"a.yandex-team.ru/travel/komod/trips/internal/point"
	"a.yandex-team.ru/travel/komod/trips/internal/references"
	"a.yandex-team.ru/travel/komod/trips/internal/testutils"
	"a.yandex-team.ru/travel/komod/trips/internal/trips"
	tripsmodels "a.yandex-team.ru/travel/komod/trips/internal/trips/models"
	"a.yandex-team.ru/travel/library/go/geobase"
	"a.yandex-team.ru/travel/library/go/syncutil"
	"a.yandex-team.ru/travel/library/go/testutil"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
)

func Test_LockUser(t *testing.T) {
	testPgClient := testutils.GetTestPgClient()
	dbTestTimeout := testutils.GetDBTestTimeout()
	withEmptyDB := testutils.NewDBCleaner(testPgClient, AllModels...)
	registry := buildRegistry()

	pointFactory := point.NewFactory(
		extractors.NewStationIDToSettlementIDMapper(registry),
		helpers.NewCachedLocationRepository(),
		geobase.StubGeobase{},
		registry,
	)
	storage := NewTripsStorage(TransactionOptions{}, testPgClient, pointFactory)

	tests := []struct {
		name       string
		createUser bool
	}{
		{
			name:       "user already exist",
			createUser: true,
		},
		{
			name:       "lock new user",
			createUser: false,
		},
	}

	for _, tt := range tests {
		t.Run(
			fmt.Sprintf("Update/locks row for update/second parallel transaction gets error: %s", tt.name),
			withEmptyDB(
				func(t *testing.T) {
					ctx, cancelFunc := context.WithTimeout(context.Background(), dbTestTimeout)
					defer cancelFunc()

					db, err := testPgClient.GetDB(hasql.Primary)
					require.NoError(t, err)
					db = db.Debug()

					user := generateUser()
					newTrip := generateTrip(user.PassportID, pointFactory, registry)

					if tt.createUser {
						require.NoError(t, db.Create(user).Error)
					}

					transactionsFlow := make(chan struct{})
					wg := &syncutil.WaitGroup{}
					wg.Go(
						func() {
							require.NoError(
								t,
								storage.ExecuteInTransaction(
									func(tx trips.StorageTxSession) error {
										assert.NoError(t, tx.LockUser(ctx, user.PassportID))
										transactionsFlow <- struct{}{}
										assert.NoError(t, tx.UpsertTrips(ctx, newTrip))
										<-transactionsFlow // wait for second transaction
										return nil
									},
								),
							)
						},
					)

					wg.Go(
						func() {
							require.Error(
								t,
								storage.ExecuteInTransaction(
									func(tx trips.StorageTxSession) error {
										<-transactionsFlow // wait for lock in first transaction
										err := tx.LockUser(ctx, user.PassportID)
										assert.ErrorIs(t, err, ErrUnableToLockUser)
										transactionsFlow <- struct{}{}
										return err
									},
								),
							)
						},
					)
					wait(t, wg)

					trip := &Trip{}
					require.NoError(t, db.Take(trip, "passport_id = ?", user.PassportID).Error)
					require.Equal(t, trip.PassportID, user.PassportID)

				},
			),
		)
	}
}

func TestCorrectUpsertGet(t *testing.T) {
	testPgClient := testutils.GetTestPgClient()
	dbTestTimeout := testutils.GetDBTestTimeout()
	withEmptyDB := testutils.NewDBCleaner(testPgClient, AllModels...)
	registry := buildRegistry()

	visitFactory := point.NewFactory(
		extractors.NewStationIDToSettlementIDMapper(registry),
		helpers.NewCachedLocationRepository(),
		geobase.StubGeobase{},
		registry,
	)
	storage := NewTripsStorage(TransactionOptions{}, testPgClient, visitFactory)
	tripComparator := comparators.TripComparator{}

	t.Run(
		"Check getting trips if user has no orders",
		withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbTestTimeout)
				defer cancelFunc()

				result, err := storage.GetSession().GetTrips(ctx, "fakePassportID")
				require.NoError(t, err)
				require.Len(t, result, 0)
			},
		),
	)
	t.Run(
		"Check getting trip if user has no orders",
		withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbTestTimeout)
				defer cancelFunc()

				result, err := storage.GetSession().GetTrip(ctx, "fakeTripID")
				require.NoError(t, err)
				require.Nil(t, result)
			},
		),
	)
	t.Run(
		"Check getting trip by invalid id",
		withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbTestTimeout)
				defer cancelFunc()

				db, err := testPgClient.GetDB(hasql.Primary)
				require.NoError(t, err)
				db = db.Debug()

				user := generateUser()
				require.NoError(t, db.Create(user).Error)
				require.NoError(
					t,
					storage.ExecuteInTransaction(
						func(tx trips.StorageTxSession) error {
							err := tx.UpsertTrips(ctx, generateTrip(user.PassportID, visitFactory, registry))
							if err != nil {
								return err
							}
							result, err := tx.GetTrip(ctx, "fakeTripID")
							if err != nil {
								return err
							}
							assert.Nil(t, result)
							return nil
						},
					),
				)

				result, err := storage.GetSession().GetTrips(ctx, "fakePassportID")
				require.NoError(t, err)
				require.Len(t, result, 0)
			},
		),
	)
	t.Run(
		"Check correctness of upserting and getting trips",
		withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbTestTimeout)
				defer cancelFunc()

				db, err := testPgClient.GetDB(hasql.Primary)
				require.NoError(t, err)
				db = db.Debug()

				user := generateUser()
				require.NoError(t, db.Create(user).Error)

				firstTrip := generateTrip(user.PassportID, visitFactory, registry)
				secondTrip := generateTrip(user.PassportID, visitFactory, registry)

				require.NoError(
					t,
					storage.ExecuteInTransaction(
						func(tx trips.StorageTxSession) error {
							err := tx.UpsertTrips(ctx, firstTrip, secondTrip)
							if err != nil {
								return err
							}
							compareTripsWithDBState(t, ctx, tx, user.PassportID, firstTrip, secondTrip)
							return nil
						},
					),
				)
			},
		),
	)
	t.Run(
		"Check correctness of upserting and getting trips one by one",
		withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbTestTimeout)
				defer cancelFunc()

				db, err := testPgClient.GetDB(hasql.Primary)
				require.NoError(t, err)
				db = db.Debug()

				user := generateUser()
				require.NoError(t, db.Create(user).Error)

				firstTrip := generateTrip(user.PassportID, visitFactory, registry)
				secondTrip := generateTrip(user.PassportID, visitFactory, registry)

				require.NoError(
					t,
					storage.ExecuteInTransaction(
						func(tx trips.StorageTxSession) error {
							err := tx.UpsertTrips(ctx, firstTrip, secondTrip)
							if err != nil {
								return err
							}
							for _, tripObject := range []*tripsmodels.Trip{firstTrip, secondTrip} {
								fetchedTrip, err := tx.GetTrip(ctx, tripObject.ID)
								if err != nil {
									return err
								}
								assert.True(
									t,
									tripComparator.Compare(
										fetchedTrip,
										tripObject,
									),
									"trips should be equal: %+v %+v",
									fetchedTrip,
									tripObject,
								)
							}
							return nil
						},
					),
				)
			},
		),
	)
}

func TestCorrectTripStateUpdate(t *testing.T) {
	testPgClient := testutils.GetTestPgClient()
	dbTestTimeout := testutils.GetDBTestTimeout()
	withEmptyDB := testutils.NewDBCleaner(testPgClient, AllModels...)
	registry := buildRegistry()

	visitFactory := point.NewFactory(
		extractors.NewStationIDToSettlementIDMapper(registry),
		helpers.NewCachedLocationRepository(),
		geobase.StubGeobase{},
		registry,
	)
	storage := NewTripsStorage(TransactionOptions{}, testPgClient, visitFactory)

	t.Run(
		"Add new order",
		withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbTestTimeout)
				defer cancelFunc()

				db, err := testPgClient.GetDB(hasql.Primary)
				require.NoError(t, err)
				db = db.Debug()

				user := generateUser()
				require.NoError(t, db.Create(user).Error)

				firstTrip := generateTrip(user.PassportID, visitFactory, registry)
				secondTrip := generateTrip(user.PassportID, visitFactory, registry)

				require.NoError(
					t,
					storage.ExecuteInTransaction(
						func(tx trips.StorageTxSession) error {
							err := tx.UpsertTrips(ctx, firstTrip, secondTrip)
							if err != nil {
								return err
							}
							compareTripsWithDBState(t, ctx, tx, user.PassportID, firstTrip, secondTrip)

							firstTrip.UpsertOrder(generateOrderInfo(registry, visitFactory))
							err = tx.UpsertTrips(ctx, firstTrip)
							if err != nil {
								return err
							}
							compareTripsWithDBState(t, ctx, tx, user.PassportID, firstTrip, secondTrip)
							return nil
						},
					),
				)
			},
		),
	)

	t.Run(
		"NewSpan trip",
		withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbTestTimeout)
				defer cancelFunc()

				db, err := testPgClient.GetDB(hasql.Primary)
				require.NoError(t, err)
				db = db.Debug()

				user := generateUser()
				require.NoError(t, db.Create(user).Error)

				firstTrip := generateTrip(user.PassportID, visitFactory, registry)

				require.NoError(
					t,
					storage.ExecuteInTransaction(
						func(tx trips.StorageTxSession) error {
							err := tx.UpsertTrips(ctx, firstTrip)
							if err != nil {
								return err
							}
							compareTripsWithDBState(t, ctx, tx, user.PassportID, firstTrip)

							secondTrip := generateTrip(user.PassportID, visitFactory, registry)
							err = tx.UpsertTrips(ctx, secondTrip)
							if err != nil {
								return err
							}
							compareTripsWithDBState(t, ctx, tx, user.PassportID, firstTrip, secondTrip)
							return nil
						},
					),
				)
			},
		),
	)

	t.Run(
		"Merge trips",
		withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbTestTimeout)
				defer cancelFunc()

				db, err := testPgClient.GetDB(hasql.Primary)
				require.NoError(t, err)
				db = db.Debug()

				user := generateUser()
				require.NoError(t, db.Create(user).Error)

				firstTrip := generateTrip(user.PassportID, visitFactory, registry)
				secondTrip := generateTrip(user.PassportID, visitFactory, registry)

				require.NoError(
					t,
					storage.ExecuteInTransaction(
						func(tx trips.StorageTxSession) error {
							err := tx.UpsertTrips(ctx, firstTrip, secondTrip)
							if err != nil {
								return err
							}
							compareTripsWithDBState(t, ctx, tx, user.PassportID, firstTrip, secondTrip)

							for _, info := range firstTrip.OrderInfos {
								secondTrip.UpsertOrder(info)
							}
							err = tx.UpsertTrips(ctx, secondTrip)
							if err != nil {
								return err
							}
							err = tx.RemoveTrips(ctx, firstTrip)
							if err != nil {
								return err
							}

							compareTripsWithDBState(t, ctx, tx, user.PassportID, secondTrip)
							return nil
						},
					),
				)
			},
		),
	)

	t.Run(
		"Remove trip order spans",
		withEmptyDB(
			func(t *testing.T) {
				ctx, cancelFunc := context.WithTimeout(context.Background(), dbTestTimeout)
				defer cancelFunc()

				db, err := testPgClient.GetDB(hasql.Primary)
				require.NoError(t, err)
				db = db.Debug()

				user := generateUser()
				require.NoError(t, db.Create(user).Error)

				firstTrip := generateTrip(user.PassportID, visitFactory, registry)
				secondTrip := generateTrip(user.PassportID, visitFactory, registry)

				require.NoError(
					t,
					storage.ExecuteInTransaction(
						func(tx trips.StorageTxSession) error {
							err := tx.UpsertTrips(ctx, firstTrip, secondTrip)
							if err != nil {
								return err
							}
							compareTripsWithDBState(t, ctx, tx, user.PassportID, firstTrip, secondTrip)

							err = tx.RemoveTripOrderSpans(ctx, firstTrip.ID)
							if err != nil {
								return err
							}
							err = tx.RemoveTrips(ctx, firstTrip)
							if err != nil {
								return err
							}

							compareTripsWithDBState(t, ctx, tx, user.PassportID, secondTrip)
							return nil
						},
					),
				)
			},
		),
	)
}

func compareTripsWithDBState(
	t *testing.T,
	ctx context.Context,
	tx trips.StorageTxSession,
	passportID string,
	tripObjects ...*tripsmodels.Trip,
) {
	fetchedTrips, err := tx.GetTrips(ctx, passportID)
	require.NoError(t, err)
	require.True(
		t,
		comparators.TripComparator{}.CompareSlices(
			fetchedTrips,
			tripObjects,
		),
		"trips should be equal: %+v %+v",
		fetchedTrips,
		tripObjects,
	)
}

func generateUser() *User {
	return &User{PassportID: uuid.Must(uuid.NewV4()).String()}
}

func generateTrip(passportID string, pointFactory *point.Factory, registry references.References) *tripsmodels.Trip {
	newTrip := tripsmodels.NewTrip(uuid.Must(uuid.NewV4()).String(), passportID)

	ordersCount := rand.Intn(3) + 1
	for i := 0; i < ordersCount; i++ {
		newTrip.UpsertOrder(generateOrderInfo(registry, pointFactory))
	}
	return newTrip
}

func generateOrderInfo(registry references.References, factory *point.Factory) tripsmodels.OrderInfo {
	now := time.Now().Truncate(time.Second)
	first, found := registry.Settlements().Get(1)
	if !found {
		panic("not found first settlement")
	}
	second, found := registry.Settlements().Get(2)
	if !found {
		panic("not found second settlement")
	}

	return tripsmodels.OrderInfo{
		ID: orders.ID(uuid.Must(uuid.NewV4()).String()),
		Spans: []models.Span{
			models.NewSpan(
				models.NewVisit(factory.MakeBySettlement(first), now.Add(2*units.Day)),
				models.NewVisit(factory.MakeBySettlement(second), now.Add(3*units.Day)),
				true,
			),
			models.NewSpan(
				models.NewVisit(factory.MakeBySettlement(second), now.Add(4*units.Day)),
				models.NewVisit(factory.MakeBySettlement(first), now.Add(5*units.Day)),
				true,
			),
		},
	}
}

type fakeReferences struct {
	settlements *references.SettlementsRepository
}

func (f fakeReferences) Settlements() *references.SettlementsRepository {
	return f.settlements
}

func (f fakeReferences) Stations() *references.StationsRepository {
	//TODO implement me
	panic("implement me")
}

func (f fakeReferences) StationToSettlements() *references.StationToSettlementsRepository {
	//TODO implement me
	panic("implement me")
}

func (f fakeReferences) StationCodes() *references.StationCodesRepository {
	//TODO implement me
	panic("implement me")
}

func (f fakeReferences) Carriers() *references.CarrierRepository {
	//TODO implement me
	panic("implement me")
}

func (f fakeReferences) Regions() *references.RegionRepository {
	//TODO implement me
	panic("implement me")
}

func (f fakeReferences) Countries() *references.CountryRepository {
	//TODO implement me
	panic("implement me")
}

func buildRegistry() references.References {
	registry := &fakeReferences{
		settlements: references.NewSettlementsRepository(),
	}
	_, _ = registry.Settlements().Write(mustMarshal(&rasp.TSettlement{Id: 1, GeoId: 1}))
	_, _ = registry.Settlements().Write(mustMarshal(&rasp.TSettlement{Id: 2, GeoId: 2}))

	return registry
}

func mustMarshal(m proto.Message) []byte {
	data, err := proto.Marshal(m)
	if err != nil {
		panic(err)
	}
	return data
}

func wait(t *testing.T, wg *syncutil.WaitGroup) {
	require.NoError(
		t,
		testutil.CallWithTimeout(
			func() {
				wg.Wait()
			},
			5*time.Second,
		),
	)
}
