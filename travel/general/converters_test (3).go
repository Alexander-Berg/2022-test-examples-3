package blacklist

import (
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/buses/backend/internal/api/filters"
	pb "a.yandex-team.ru/travel/buses/backend/proto"
)

func TestSupplierConverter(t *testing.T) {
	var (
		converter = supplierConverter{}
		value     comparableValue
		err       error
	)

	_, err = converter.fromRuleValue(1)
	assert.EqualError(t, err, "expected string, got int")

	_, err = converter.fromRuleValue("invalidName")
	assert.EqualError(t, err, "no such supplier with name = invalidName")

	value, err = converter.fromRuleValue("ok")
	if assert.NoError(t, err) {
		assert.Equal(t, value, comparableInteger(12))
	}

	assert.Equal(t, comparableInteger(1), converter.fromSearchRide(&filters.SearchInfo{}, &pb.TRide{SupplierId: 1}))
}

func TestFoundSuppliersConverter(t *testing.T) {
	var (
		converter = foundSuppliersConverter{}
		value     comparableValue
		err       error
	)

	_, err = converter.fromRuleValue(1)
	assert.EqualError(t, err, "expected string, got int")

	_, err = converter.fromRuleValue("invalidName")
	assert.EqualError(t, err, "no such supplier with name = invalidName")

	value, err = converter.fromRuleValue("ok")
	if assert.NoError(t, err) {
		assert.Equal(t, value, comparableInteger(12))
	}

	assert.Equal(t,
		comparableIntegers{10: true, 15: true},
		converter.fromSearchRide(&filters.SearchInfo{FoundSupplierIds: map[uint32]bool{10: true, 15: true}}, nil),
	)
}

func TestCarrierNameConverter(t *testing.T) {
	var (
		converter = carrierNameConverter{}
		value     comparableValue
		err       error
	)

	_, err = converter.fromRuleValue(1)
	assert.EqualError(t, err, "expected string, got int")

	value, err = converter.fromRuleValue("carrier name")
	if assert.NoError(t, err) {
		assert.Equal(t, value, comparableString("carrier name"))
	}

	assert.Equal(t, comparableString("carrier name"), converter.fromSearchRide(&filters.SearchInfo{}, &pb.TRide{CarrierName: "carrier name"}))
}

func TestPointKeyConverter(t *testing.T) {
	var (
		converters = []converter{
			rideDeparturePointKeyConverter{},
			rideArrivalPointKeyConverter{},
			searchDeparturePointKeyConverter{},
			searchArrivalPointKeyConverter{},
		}
		value comparableValue
		err   error
		pk    = &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_SETTLEMENT, Id: 1}
	)

	for _, converter := range converters {
		_, err = converter.fromRuleValue(1)
		assert.EqualError(t, err, "expected string, got int")

		_, err = converter.fromRuleValue("invalidPK")
		if assert.Error(t, err) {
			assert.True(t, strings.HasPrefix(err.Error(), "can't parse PointKey invalidPK: "))
		}

		value, err = converter.fromRuleValue("c1")
		if assert.NoError(t, err) {
			assert.Equal(t, value, comparablePointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_SETTLEMENT, ID: 1})
		}
	}

	assert.Equal(t,
		comparablePointKey{Type: pk.Type, ID: pk.Id},
		rideDeparturePointKeyConverter{}.fromSearchRide(&filters.SearchInfo{}, &pb.TRide{From: pk}),
	)
	assert.Equal(t,
		comparablePointKey{Type: pk.Type, ID: pk.Id},
		rideArrivalPointKeyConverter{}.fromSearchRide(&filters.SearchInfo{}, &pb.TRide{To: pk}),
	)
	assert.Equal(t,
		comparablePointKey{Type: pk.Type, ID: pk.Id},
		searchDeparturePointKeyConverter{}.fromSearchRide(&filters.SearchInfo{DeparturePK: pk}, nil),
	)
	assert.Equal(t,
		comparablePointKey{Type: pk.Type, ID: pk.Id},
		searchArrivalPointKeyConverter{}.fromSearchRide(&filters.SearchInfo{ArrivalPK: pk}, nil),
	)
}
