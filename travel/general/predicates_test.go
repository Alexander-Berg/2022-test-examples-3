package blacklist

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/buses/backend/internal/api/filters"
	pb "a.yandex-team.ru/travel/buses/backend/proto"
)

func TestPredicateEquals(t *testing.T) {
	var predicate, _ = predicateEquals("good carrier", carrierNameConverter{})

	assert.True(t, predicate(&filters.SearchInfo{}, &pb.TRide{CarrierName: "good carrier"}))
	assert.False(t, predicate(&filters.SearchInfo{}, &pb.TRide{CarrierName: "bad carrier"}))
}

func TestPredicateFoundSuppliers(t *testing.T) {
	var predicate, _ = predicateFoundSuppliers("ok", foundSuppliersConverter{})

	assert.True(t, predicate(&filters.SearchInfo{FoundSupplierIds: map[uint32]bool{1: true, 12: true}}, nil))
	assert.False(t, predicate(&filters.SearchInfo{FoundSupplierIds: map[uint32]bool{1: true, 2: true}}, nil))
}

func TestMakeContainedPredicate(t *testing.T) {
	var values = []interface{}{"carrier 1", "carrier 2"}

	for _, expected := range []bool{true, false} {
		var (
			predicateFactory = makeContainedPredicate(expected)
			predicate        predicate
			err              error
		)

		_, err = predicateFactory(1, carrierNameConverter{})
		assert.EqualError(t, err, "operator argument is int instead of array")

		predicate, err = predicateFactory(values, carrierNameConverter{})
		if assert.NoError(t, err) {
			for _, value := range values {
				assert.Equal(t, expected, predicate(&filters.SearchInfo{}, &pb.TRide{CarrierName: value.(string)}))
			}
			assert.Equal(t, !expected, predicate(&filters.SearchInfo{}, &pb.TRide{CarrierName: "bad carrier"}))
		}
	}
}
