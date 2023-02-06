package storage

import (
	"testing"

	"github.com/stretchr/testify/require"

	farefamiliesstructs "a.yandex-team.ru/travel/avia/fare_families/internal/services/fare_families/data_structs/fare_families"
	"a.yandex-team.ru/travel/library/go/logging"
)

func TestDump(t *testing.T) {
	t.Run(
		"TestDump", func(t *testing.T) {
			logger, _ := logging.New(&logging.DefaultConfig)
			storage := NewEmptyStorage(logger).(*storageImpl)

			testCarrierID := int32(1)
			testFareFamily := farefamiliesstructs.FareFamily{
				Brand: "test",
				Key:   "ff_index=0;carrier=1",
			}
			err := storage.AddFareFamily(testCarrierID, testFareFamily)
			require.NoError(t, err)

			compiledTestFareFamily, err := InitFareFamily(testFareFamily)
			require.NoError(t, err)
			allFareFamilies := CarrierFareFamilies{
				CarrierID:    testCarrierID,
				FareFamilies: []farefamiliesstructs.CompiledFareFamily{*compiledTestFareFamily},
			}
			result, err := storage.GetDump(testCarrierID)
			require.NoError(t, err)
			require.EqualValues(t, allFareFamilies, *result)

			testMatchingFilter := func(fareFamily farefamiliesstructs.CompiledFareFamily) bool {
				return fareFamily.Brand == "test"
			}
			result, err = storage.GetDump(testCarrierID, testMatchingFilter)
			require.NoError(t, err)
			require.EqualValues(t, allFareFamilies, *result)

			testNonMatchingFilter := func(fareFamily farefamiliesstructs.CompiledFareFamily) bool {
				return fareFamily.Brand == "non-matching fantasy"
			}
			noFareFamilies := CarrierFareFamilies{
				CarrierID:    testCarrierID,
				FareFamilies: []farefamiliesstructs.CompiledFareFamily{},
			}
			result, err = storage.GetDump(testCarrierID, testNonMatchingFilter)
			require.NoError(t, err)
			require.EqualValues(t, noFareFamilies, *result)
		},
	)
}
