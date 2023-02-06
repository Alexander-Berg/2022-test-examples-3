package blacklist

import (
	"testing"

	"github.com/stretchr/testify/assert"
	uzap "go.uber.org/zap"

	"a.yandex-team.ru/travel/buses/backend/internal/api/filters"
	pb "a.yandex-team.ru/travel/buses/backend/proto"
)

func TestNewRules(t *testing.T) {
	var logger, logs = setupLogsCapture()

	t.Run("errors", func(t *testing.T) {
		var rr rules

		rr = newRules(
			parsedRules{
				[]parsedItem{
					{path: []string{"invalid", "path"}, value: 1},
					{path: []string{"carrier"}, value: "carrierName1"},
				},
				[]parsedItem{
					{path: []string{"carrier"}, value: "carrierName2"},
				},
			},
			logger,
		)

		if logEntries := logs.TakeAll(); assert.Equal(t, 1, len(logEntries)) {
			assert.Equal(t, uzap.ErrorLevel, logEntries[0].Level)
			assert.Equal(t, logEntries[0].Message, "blacklist.newRules: unknown field [invalid path]")
		}
		assert.Len(t, rr, 1)

		rr = newRules(
			parsedRules{
				[]parsedItem{
					{path: []string{"partner"}, operator: "$goodness", value: 1000},
				},
				[]parsedItem{
					{path: []string{"carrier"}, value: "carrierName"},
				},
			},
			logger,
		)

		if logEntries := logs.TakeAll(); assert.Equal(t, 1, len(logEntries)) {
			assert.Equal(t, uzap.ErrorLevel, logEntries[0].Level)
			assert.Equal(t, logEntries[0].Message, "blacklist.newRules: unknown operator $goodness")
		}
		assert.Len(t, rr, 1)
	})

	t.Run("operators", func(t *testing.T) {
		var rr = newRules(
			parsedRules{
				[]parsedItem{
					{path: []string{"carrier"}, value: "good carrier"},
					{path: []string{"carrier"}, operator: "$eq", value: "good carrier"},
					{path: []string{"carrier"}, operator: "$in", value: []interface{}{"good carrier", "best carrier"}},
					{path: []string{"carrier"}, operator: "$nin", value: []interface{}{"bad carrier", "worst carrier"}},
				},
			},
			logger,
		)

		if assert.Equal(t, logs.Len(), 0) && assert.Len(t, rr, 1) {
			for _, predicate := range rr[0] {
				assert.True(t, predicate(&filters.SearchInfo{}, &pb.TRide{CarrierName: "good carrier"}))
				assert.False(t, predicate(&filters.SearchInfo{}, &pb.TRide{CarrierName: "bad carrier"}))
			}
		}
	})

	t.Run("fields", func(t *testing.T) {
		var (
			departurePK = &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 100}
			arrivalPK   = &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 200}
			checkRule   = func(rule parsedItem, checker func(predicate predicate)) {
				if rr := newRules(parsedRules{[]parsedItem{rule}}, logger); assert.Equal(t, logs.Len(), 0) && assert.Len(t, rr, 1) {
					if rule := rr[0]; assert.Len(t, rule, 1) {
						checker(rule[0])
					}
				}
			}
		)

		checkRule(parsedItem{path: []string{"partner"}, value: "ok"}, func(predicate predicate) {
			assert.True(t, predicate(&filters.SearchInfo{}, &pb.TRide{SupplierId: 12}))
			assert.False(t, predicate(&filters.SearchInfo{}, &pb.TRide{SupplierId: 99}))
		})

		checkRule(parsedItem{path: []string{"carrier"}, value: "good carrier"}, func(predicate predicate) {
			assert.True(t, predicate(&filters.SearchInfo{}, &pb.TRide{CarrierName: "good carrier"}))
			assert.False(t, predicate(&filters.SearchInfo{}, &pb.TRide{CarrierName: "bad carrier"}))
		})

		checkRule(parsedItem{path: []string{"from.id"}, value: "s1"}, func(predicate predicate) {
			assert.True(t, predicate(
				&filters.SearchInfo{},
				&pb.TRide{
					From: &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 1},
					To:   &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 2},
				},
			))
			assert.False(t, predicate(&filters.SearchInfo{}, &pb.TRide{From: departurePK, To: arrivalPK}))
		})

		checkRule(parsedItem{path: []string{"from.serp_id"}, value: "c1"}, func(predicate predicate) {
			assert.True(t, predicate(
				&filters.SearchInfo{
					DeparturePK: &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_SETTLEMENT, Id: 1},
					ArrivalPK:   &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_SETTLEMENT, Id: 2},
				},
				&pb.TRide{From: departurePK, To: arrivalPK},
			))
			assert.False(t, predicate(
				&filters.SearchInfo{DeparturePK: departurePK, ArrivalPK: arrivalPK},
				&pb.TRide{From: departurePK, To: arrivalPK},
			))
		})

		checkRule(parsedItem{path: []string{"to.id"}, value: "s2"}, func(predicate predicate) {
			assert.True(t, predicate(
				&filters.SearchInfo{},
				&pb.TRide{
					From: &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 1},
					To:   &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 2},
				},
			))
			assert.False(t, predicate(&filters.SearchInfo{}, &pb.TRide{From: departurePK, To: arrivalPK}))
		})

		checkRule(parsedItem{path: []string{"to.serp_id"}, value: "c2"}, func(predicate predicate) {
			assert.True(t, predicate(
				&filters.SearchInfo{
					DeparturePK: &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_SETTLEMENT, Id: 1},
					ArrivalPK:   &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_SETTLEMENT, Id: 2},
				},
				&pb.TRide{From: departurePK, To: arrivalPK},
			))
			assert.False(t, predicate(
				&filters.SearchInfo{DeparturePK: departurePK, ArrivalPK: arrivalPK},
				&pb.TRide{From: departurePK, To: arrivalPK},
			))
		})

		checkRule(parsedItem{path: []string{"found_partners"}, value: "ok"}, func(predicate predicate) {
			assert.True(t, predicate(
				&filters.SearchInfo{FoundSupplierIds: map[uint32]bool{12: true, 20: true}},
				&pb.TRide{},
			))
			assert.False(t, predicate(
				&filters.SearchInfo{FoundSupplierIds: map[uint32]bool{20: true}},
				&pb.TRide{},
			))
		})
	})

	t.Run("handle rule path", func(t *testing.T) {
		var (
			expectedIds = []uint32{1, 2}
			rr          = newRules(
				parsedRules{
					[]parsedItem{
						{path: []string{"from", "id"}, value: "c1"},
					},
					[]parsedItem{
						{path: []string{"from.id"}, value: "c2"},
					},
				},
				logger,
			)
		)

		if assert.Equal(t, logs.Len(), 0) && assert.Len(t, rr, len(expectedIds)) {
			for i, rule := range rr {
				if assert.Len(t, rule, 1) {
					assert.True(t, rule[0](&filters.SearchInfo{}, &pb.TRide{
						From: &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_SETTLEMENT, Id: expectedIds[i]},
					}))
					assert.False(t, rule[0](&filters.SearchInfo{}, &pb.TRide{
						From: &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_SETTLEMENT, Id: 99},
					}))
				}
			}
		}
	})

	t.Run("handle empty rules", func(t *testing.T) {
		var rr = newRules(parsedRules{[]parsedItem{}}, logger)

		if assert.Equal(t, logs.Len(), 0) {
			assert.Len(t, rr, 0)
		}
	})
}
