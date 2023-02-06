package partdel

import (
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/graph"
	"a.yandex-team.ru/market/combinator/pkg/its"
	"a.yandex-team.ru/market/combinator/pkg/settings"
)

func TestPartnerSegmentHasPartialReturnService(t *testing.T) {
	holder, err := its.NewStringSettingsHolder(`{ "switch_to_disable_partial_return_service": true }`)
	require.NoError(t, err)

	validPartnerID := int64(145)
	schedule, err := graph.CreateAroundTheClockSchedule(false)
	require.NoError(t, err)

	// invalid partnerID
	g := graph.Graph{PartnerToSegments: map[int64][]*graph.Node{validPartnerID: {graph.CreateWarehousePartDelNode(schedule, true)}}}
	res := PartnerSegmentHasPartialReturnService(settings.New(holder.GetSettings(), ""), &g, 1)
	require.False(t, res)

	// with disable partial delivery service
	g = graph.Graph{PartnerToSegments: map[int64][]*graph.Node{validPartnerID: {graph.CreateWarehousePartDelNode(schedule, false)}}}
	res = PartnerSegmentHasPartialReturnService(settings.New(holder.GetSettings(), ""), &g, validPartnerID)
	require.False(t, res)

	// without disable partial delivery service
	g = graph.Graph{PartnerToSegments: map[int64][]*graph.Node{validPartnerID: {graph.CreateWarehousePartDelNode(schedule, true)}}}
	res = PartnerSegmentHasPartialReturnService(settings.New(holder.GetSettings(), ""), &g, validPartnerID)
	require.True(t, res)
}
