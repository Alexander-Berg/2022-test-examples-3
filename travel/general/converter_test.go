package handler

import (
	"testing"

	"a.yandex-team.ru/library/go/test/assertpb"

	apipb "a.yandex-team.ru/travel/trains/search_api/api"
	commonModels "a.yandex-team.ru/travel/trains/search_api/internal/pkg/clients/common/models"
)

func Test_featuresToProto(t *testing.T) {
	for _, tt := range []struct {
		name     string
		segment  *commonModels.SegmentWithTariffs
		expected *apipb.Features
	}{
		{
			name: "ThroughTrain",
			segment: &commonModels.SegmentWithTariffs{
				IsThroughTrain: true,
			},
			expected: &apipb.Features{
				ThroughTrain: true,
			},
		},
		{
			name: "SubSegments",
			segment: &commonModels.SegmentWithTariffs{
				SubSegments: []commonModels.SegmentWithTariffs{{}, {}},
				MinArrival:  "2020-01-01T00:00:00",
				MaxArrival:  "2020-01-01T01:00:00",
			},
			expected: &apipb.Features{
				Subsegments: &apipb.FeaturesSubSegments{
					Arrival: &apipb.SubSegmentsArrival{
						Min: "2020-01-01T00:00:00",
						Max: "2020-01-01T01:00:00",
					},
				},
			},
		},
	} {
		t.Run(tt.name, func(t *testing.T) {
			protoFeatures := featuresToProto(tt.segment)
			assertpb.Equal(t, tt.expected, protoFeatures)
		})
	}
}
