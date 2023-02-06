package graph

import (
	"context"
	"fmt"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/its"
	"a.yandex-team.ru/market/combinator/pkg/settings"
)

func TestDSBSPickupDeduplication(t *testing.T) {
	stx := settings.New(its.GetSettingsHolder().GetSettings(), "")
	versions := []int{2, 3}
	s1 := CreateWeekdaySchedule()
	s2 := CreateFullWeekSchedule()
	s3 := CreateScheduleManyIntervals()
	regionMap := geobase.NewExample()
	params := []NodeParams{
		NodeParams{
			PointLmsID: 100200300,
			Schedule:   s3,
		},
		NodeParams{
			PointLmsID: 100200400,
			Schedule:   s1,
		},
		NodeParams{
			PointLmsID: 100200500,
			Schedule:   s2,
		},
		NodeParams{
			PointLmsID: 100200600,
			Schedule:   s3,
		},
		NodeParams{
			PointLmsID: 100200700,
			Schedule:   s2,
		},
		NodeParams{
			PointLmsID: 100200800,
			Schedule:   s3,
		},
	}
	for _, version := range versions {
		fmt.Printf("version %d\n", version)
		g := createGraphByHintsVersion(version, nil)
		err := g.readEnrichedSegments(stx, NewEnrichedTableReader(params), &regionMap, nil)
		require.NoError(t, err)

		g.Finish(context.Background())

		require.Len(t, g.DSBSPickupCanonicals, 3)
		switch version {
		case 2:
			require.Len(t, g.DSBSPickupFeatures, 3)
			require.Len(t, g.DSBSPickupFeaturesSlice, 0)
		case 3:
			require.Len(t, g.DSBSPickupFeatures, 0)
			require.Len(t, g.DSBSPickupFeaturesSlice, 3)
		}
		for i, param := range params {
			fmt.Printf("  pointLmsID=%d\n", param.PointLmsID)
			node := g.FindPickupNode(param.PointLmsID)
			require.NotNil(t, node)
			require.EqualValues(t, SegmentTypePickup, node.Type)
			require.EqualValues(t, geobase.RegionMoscow, node.LocationID)
			require.EqualValues(t, param.PointLmsID, node.PointLmsID)
			require.EqualValues(t, 2*(i+1), node.ID)
			require.Len(t, node.PickupServices, 1)
			ps, _ := NewSchedule(param.Schedule, false)
			require.Equal(t, ps.CalcRepresentation(), node.PickupServices[0].GetSchedule().CalcRepresentation())
		}
	}
}

type NodeParams struct {
	PointLmsID int64
	Schedule   []DaySchedule
}

type EnrichedTableReader struct {
	nodes    []NodeParams
	position int
}

func NewEnrichedTableReader(nodes []NodeParams) *EnrichedTableReader {
	return &EnrichedTableReader{
		nodes:    nodes,
		position: 0,
	}
}

func (r *EnrichedTableReader) Scan(value interface{}) error {
	v := value.(*SegmentServicesYT)

	node := r.nodes[r.position]
	r.position++

	regionID := int64(geobase.RegionMoscow)
	res, err := createSegmentServicesYT(
		&LogisticSegmentYT{
			ID:           int64(2 * r.position),
			PartnerLmsID: 100500,
			PointLmsID:   &node.PointLmsID,
			LocationID:   &regionID,
			Type:         "pickup",
			PartnerType:  "DROPSHIP_BY_SELLER",
			PartnerName:  "Рога и копыта",
		},
		[]*LogisticServiceYT{
			&LogisticServiceYT{
				ID:              int64(2*r.position + 1),
				SegmentLmsID:    int64(2 * r.position),
				Status:          "active",
				Code:            "HANDING",
				Type:            "outbound",
				WorkingSchedule: node.Schedule,
			},
		},
	)
	*v = *res
	return err
}

func (r *EnrichedTableReader) Next() bool {
	return r.position < len(r.nodes)
}

func (r *EnrichedTableReader) Err() error {
	return nil
}

func (r *EnrichedTableReader) Close() error {
	return nil
}
