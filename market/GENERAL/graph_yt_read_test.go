package graph

import (
	"bytes"
	"context"
	"fmt"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/its"
	"a.yandex-team.ru/market/combinator/pkg/settings"
	"a.yandex-team.ru/market/combinator/pkg/timex"
	"a.yandex-team.ru/market/combinator/pkg/ytutil"
	"a.yandex-team.ru/yt/go/yt"
)

func TestReadLogServices(t *testing.T) {
	// TransferManager: int прилетает как double
	// https://st.yandex-team.ru/TM-542
	//
	// Как получить данные:
	// yt read --format '<format=text>yson' '//home/market/testing/indexer/combinator/graph/recent/yt_logistics_services[:#1]'
	yson := `{"lms_id"=34727;"segment_lms_id"=17104;"status"="active";"code"="HANDING";"type"="outbound";"duration"=0;"price"=0;"working_schedule"=[{"to"="18:00:00";"day"=5.;"from"="09:00:00";};];"holiday_dates"={"dates"=["2020-06-12";"2020-06-13";"2020-06-14";];"endDate"="2020-07-19";"startDate"="2020-05-10";};"cargo_types"=#;"tags"={"meta"={"key"="value"};};"delivery_type"=#;};`
	buf := bytes.NewBufferString(yson)
	reader, err := ytutil.NewFileReader2(buf)
	require.NoError(t, err)

	g := NewGraphWithHintsV3(MakeHintMap(TestHints()))
	var node Node
	g.NodesSlice = append(g.NodesSlice, node)
	g.Nodes[17104] = NodeIndex(len(g.NodesSlice) - 1)

	err = g.readLogServices(settings.FromContext(context.Background()), reader)
	require.NoError(t, err)

	for _, services := range [][]*LogisticService{g.GetNodeByID(17104).CourierServices, g.GetNodeByID(17104).PickupServices} {
		require.Equal(t, 1, len(services))

		ws := services[0].Schedule
		window := ws.Windows[5]
		for _, w := range window {
			require.Equal(t, timex.DayTime{Hour: 9, Minute: 0, Second: 0}, w.From)
			require.Equal(t, timex.DayTime{Hour: 18, Minute: 0, Second: 0}, w.To)
		}
		require.Equal(t, false, ws.IsAroundTheClock)
		require.Equal(t, false, ws.IsDaily)

		require.Equal(t, []string{"key", "value"}, g.GetServiceMeta(services[0].ID))
	}
}

// TestReadServiceAroundTheClockAndDaily проверяем вычисление флагов ежедневной работы и круглосуточной работы
func TestReadServiceAroundTheClockAndDaily(t *testing.T) {
	yson := `{"lms_id"=34727;"segment_lms_id"=17104;"status"="active";"code"="HANDING";"type"="outbound";"duration"=0;"price"=0;"working_schedule"=[`
	for i := 1; i < 8; i++ {
		yson += fmt.Sprintf(`{"to"="00:00:00";"day"=%d.;"from"="00:00:00";};`, i)
	}
	yson += `];"cargo_types"=#;"tags"={"meta"={"key"="value"};};"delivery_type"=#;};`
	buf := bytes.NewBufferString(yson)
	reader, err := ytutil.NewFileReader2(buf)
	require.NoError(t, err)

	g := NewGraphWithHintsV3(MakeHintMap(TestHints()))
	var node Node
	g.NodesSlice = append(g.NodesSlice, node)
	g.Nodes[17104] = NodeIndex(len(g.NodesSlice) - 1)

	err = g.readLogServices(settings.FromContext(context.Background()), reader)
	require.NoError(t, err)

	for _, services := range [][]*LogisticService{g.GetNodeByID(17104).CourierServices, g.GetNodeByID(17104).PickupServices} {
		require.Equal(t, 1, len(services))

		ws := services[0].Schedule
		for _, window := range ws.Windows {
			for _, w := range window {
				require.Equal(t, timex.DayTime{Hour: 0, Minute: 0, Second: 0}, w.From)
				require.Equal(t, timex.DayTime{Hour: 23, Minute: 59, Second: 59}, w.To)
			}
		}
		require.Equal(t, true, ws.IsAroundTheClock)
		require.Equal(t, true, ws.IsDaily)
	}
}

func TestReadLogServicesOrder(t *testing.T) {
	yson := `{"lms_id"=34727;"segment_lms_id"=17104;"status"="active";"code"="INBOUND";"type"="inbound";"duration"=0;"price"=0;"cargo_types"=#;"tags"=#;"delivery_type"=#;};
{"lms_id"=34728;"segment_lms_id"=17104;"status"="active";"code"="SHIPMENT";"type"="outbound";"duration"=0;"price"=0;"cargo_types"=#;"tags"=#;"delivery_type"=#;};
{"lms_id"=34729;"segment_lms_id"=17104;"status"="active";"code"="PROCESSING";"type"="internal";"duration"=0;"price"=0;"cargo_types"=#;"tags"=#;"delivery_type"=#;};
{"lms_id"=34730;"segment_lms_id"=17104;"status"="active";"code"="CUTOFF";"type"="internal";"duration"=0;"price"=0;"cargo_types"=#;"tags"=#;"delivery_type"=#;};`

	buf := bytes.NewBufferString(yson)
	reader, err := ytutil.NewFileReader2(buf)
	require.NoError(t, err)

	g := NewGraphWithHintsV3(MakeHintMap(TestHints()))
	var node Node
	g.NodesSlice = append(g.NodesSlice, node)
	g.Nodes[17104] = NodeIndex(len(g.NodesSlice) - 1)

	err = g.readLogServices(settings.FromContext(context.Background()), reader)
	require.NoError(t, err)

	for _, services := range [][]*LogisticService{g.GetNodeByID(17104).CourierServices, g.GetNodeByID(17104).PickupServices} {
		require.Equal(t, 4, len(services))
		require.Equal(t, enums.ServiceInbound, services[0].Code)
		require.Equal(t, enums.ServiceCutoff, services[1].Code)
		require.Equal(t, enums.ServiceProcessing, services[2].Code)
		require.Equal(t, enums.ServiceShipment, services[3].Code)
	}
}

func TestReadLogServicesWithKorobyteRestrictions(t *testing.T) {
	yson := `{"lms_id"=34727;"segment_lms_id"=17104;"status"="active";"code"="HANDING";"type"="outbound";"duration"=0;"price"=0;"working_schedule"=[{"to"="18:00:00";"day"=5.;"from"="09:00:00";};];"holiday_dates"={"dates"=["2020-06-12";"2020-06-13";"2020-06-14";];"endDate"="2020-07-19";"startDate"="2020-05-10";};"cargo_types"=#;"tags"={"meta"={"key"="value"};};"delivery_type"=#;"korobyte_restrictions"=#;};
{"lms_id"=34728;"segment_lms_id"=17105;"status"="active";"code"="HANDING";"type"="outbound";"duration"=0;"price"=0;"working_schedule"=[{"to"="18:00:00";"day"=5.;"from"="09:00:00";};];"holiday_dates"={"dates"=["2020-06-12";"2020-06-13";"2020-06-14";];"endDate"="2020-07-19";"startDate"="2020-05-10";};"cargo_types"=#;"tags"={"meta"={"key"="value"};};"delivery_type"=#;"korobyte_restrictions"={"minimum_size_cm"=[10;15;20;];"maximum_size_cm"=[25;30;35;];"minimum_weight_g"=1024;"maximum_weight_g"=2048;"minimum_dimension_sum_cm"=40;"maximum_dimension_sum_cm"=200;};};`
	buf := bytes.NewBufferString(yson)
	reader, err := ytutil.NewFileReader2(buf)
	require.NoError(t, err)

	g := NewGraphWithHintsV3(MakeHintMap(TestHints()))
	var node17104, node17105 Node
	g.NodesSlice = append(g.NodesSlice, node17104, node17105)
	g.Nodes[17104] = NodeIndex(0)
	g.Nodes[17105] = NodeIndex(1)

	err = g.readLogServices(settings.FromContext(context.Background()), reader)
	require.NoError(t, err)

	for _, services := range [][]*LogisticService{g.GetNodeByID(17104).CourierServices, g.GetNodeByID(17104).PickupServices} {
		for _, s := range services {
			require.Equal(t, -1, s.KorobyteIndex)
			res, ok := g.KorobyteRestrictionsRepository.Get(s.KorobyteIndex)
			require.Equal(t, KorobyteRestriction{}, res)
			require.False(t, ok)
		}
	}
	for _, services := range [][]*LogisticService{g.GetNodeByID(17105).CourierServices, g.GetNodeByID(17105).PickupServices} {
		for _, s := range services {
			require.Equal(t, 0, s.KorobyteIndex)
			res, ok := g.KorobyteRestrictionsRepository.Get(s.KorobyteIndex)
			want := KorobyteRestriction{
				MinimumSizeCm:         [3]uint32{10, 15, 20},
				MaximumSizeCm:         [3]uint32{25, 30, 35},
				MinimumWeightG:        1024,
				MaximumWeightG:        2048,
				MinimumDimensionSumCm: 40,
				MaximumDimensionSumCm: 200,
			}
			require.Equal(t, want, res)
			require.True(t, ok)
		}
	}
}

// TODO: Remove this after testing table data become normal
func TestReadBadLogServices(t *testing.T) {
	yson := `{"lms_id"=28330;"segment_lms_id"=40308;"status"="active";"code"="DELIVERY";"type"="internal";"duration"=0;"price"=0;"working_schedule"=[{"to"=#;"day"=1.;"from"=#;"schedule_id"=9000499.;"id"=35368025.;};{"id"=35368026.;"to"=#;"day"=2.;"from"=#;"schedule_id"=9000499.;};{"to"=#;"day"=3.;"from"=#;"schedule_id"=9000499.;"id"=35368027.;};{"schedule_id"=9000499.;"id"=35368028.;"to"=#;"day"=4.;"from"=#;};{"day"=5.;"from"=#;"schedule_id"=9000499.;"id"=35368029.;"to"=#;};{"to"=#;"day"=6.;"from"=#;"schedule_id"=9000499.;"id"=35368030.;};{"id"=35368031.;"to"=#;"day"=7.;"from"=#;"schedule_id"=9000499.;};];"holiday_dates"={"dates"=["2020-05-15";"2020-05-16";"2020-05-22";"2020-05-23";"2020-05-29";"2020-05-30";"2020-06-05";"2020-06-06";"2020-06-12";"2020-06-13";"2020-06-19";"2020-06-20";"2020-06-26";"2020-06-27";"2020-07-03";"2020-07-04";"2020-07-10";"2020-07-11";];"endDate"="2020-07-14";"startDate"="2020-05-15";};"cargo_types"=#;"tags"=#;"delivery_type"=#;};`

	buf := bytes.NewBufferString(yson)
	reader, err := ytutil.NewFileReader2(buf)
	require.NoError(t, err)

	g := NewGraphWithHintsV3(MakeHintMap(TestHints()))
	var node Node
	g.NodesSlice = append(g.NodesSlice, node)
	g.Nodes[40308] = NodeIndex(len(g.NodesSlice) - 1)

	err = g.readLogServices(settings.FromContext(context.Background()), reader)
	require.NoError(t, err)
	for _, services := range [][]*LogisticService{g.GetNodeByID(40308).CourierServices, g.GetNodeByID(40308).PickupServices} {
		require.Equal(t, 1, len(services))

		ws := services[0].Schedule
		require.Equal(t, true, ws.Trivial)
	}
}

func TestGoPlatformSegments(t *testing.T) {
	createReader := func(tableYson string) (yt.TableReader, error) {
		buf := bytes.NewBufferString(tableYson)
		return ytutil.NewFileReader2(buf)
	}
	closeReader := func(t *testing.T, reader yt.TableReader) {
		err := reader.Close()
		require.NoError(t, err)
	}

	segmentsYson := `{"lms_id"=1;"partner_lms_id"=100;"logistics_point_lms_id"=10000000000;"location_id"=213;"type"="pickup";"partner_type"="DELIVERY";"partner_name"="СДЭК";};
{"lms_id"=2;"partner_lms_id"=200;"logistics_point_lms_id"=#;"location_id"=213;"type"="go_platform";"partner_type"="DELIVERY";"partner_name"="Яндекс.Go";};
{"lms_id"=3;"partner_lms_id"=300;"logistics_point_lms_id"=10000000001;"location_id"=213;"type"="pickup";"partner_type"="DELIVERY";"partner_name"="DPD";};
{"lms_id"=4;"partner_lms_id"=200;"logistics_point_lms_id"=#;"location_id"=213;"type"="go_platform";"partner_type"="DELIVERY";"partner_name"="Яндекс.Go";};
{"lms_id"=5;"partner_lms_id"=400;"logistics_point_lms_id"=10000000002;"location_id"=213;"type"="pickup";"partner_type"="DELIVERY";"partner_name"="МК Восток";};
{"lms_id"=6;"partner_lms_id"=200;"logistics_point_lms_id"=#;"location_id"=213;"type"="go_platform";"partner_type"="DELIVERY";"partner_name"="Яндекс.Go";};`

	edgesYson := `{"lms_id"=10;"from_segment_lms_id"=1;"to_segment_lms_id"=2;};
{"lms_id"=11;"from_segment_lms_id"=3;"to_segment_lms_id"=4;};`

	servicesYson := `{"lms_id"=100;"segment_lms_id"=1;"status"="active";"code"="HANDING";"type"="outbound";"duration"=0;"price"=0;"working_schedule"=[{"to"="18:00:00";"day"=5.;"from"="09:00:00";};];"holiday_dates"={"dates"=[];"endDate"="2020-07-19";"startDate"="2020-05-10";};"dayoff_dates"={"dates"=[];"endDate"="2020-07-19";"startDate"="2020-05-10";};"holiday_dates"={"dates"=[];"endDate"="2020-07-19";"startDate"="2020-05-10";};"cargo_types"=#;"tags"={"meta"={"key"="value"};};"delivery_type"=#;};
{"lms_id"=101;"segment_lms_id"=2;"status"="active";"code"="HANDING";"type"="outbound";"duration"=0;"price"=0;"working_schedule"=[{"to"="18:00:00";"day"=5.;"from"="09:00:00";};];"holiday_dates"={"dates"=[];"endDate"="2020-07-19";"startDate"="2020-05-10";};"dayoff_dates"={"dates"=[];"endDate"="2020-07-19";"startDate"="2020-05-10";};"holiday_dates"={"dates"=[];"endDate"="2020-07-19";"startDate"="2020-05-10";};"cargo_types"=#;"tags"={"meta"={"key"="value"};};"delivery_type"=#;};
{"lms_id"=102;"segment_lms_id"=3;"status"="active";"code"="HANDING";"type"="outbound";"duration"=0;"price"=0;"working_schedule"=[{"to"="18:00:00";"day"=5.;"from"="09:00:00";};];"holiday_dates"={"dates"=[];"endDate"="2020-07-19";"startDate"="2020-05-10";};"dayoff_dates"={"dates"=[];"endDate"="2020-07-19";"startDate"="2020-05-10";};"holiday_dates"={"dates"=[];"endDate"="2020-07-19";"startDate"="2020-05-10";};"cargo_types"=#;"tags"={"meta"={"key"="value"};};"delivery_type"=#;};
{"lms_id"=103;"segment_lms_id"=5;"status"="active";"code"="HANDING";"type"="outbound";"duration"=0;"price"=0;"working_schedule"=[{"to"="18:00:00";"day"=5.;"from"="09:00:00";};];"holiday_dates"={"dates"=[];"endDate"="2020-07-19";"startDate"="2020-05-10";};"dayoff_dates"={"dates"=[];"endDate"="2020-07-19";"startDate"="2020-05-10";};"holiday_dates"={"dates"=[];"endDate"="2020-07-19";"startDate"="2020-05-10";};"cargo_types"=#;"tags"={"meta"={"key"="value"};};"delivery_type"=#;};
{"lms_id"=104;"segment_lms_id"=6;"status"="active";"code"="HANDING";"type"="outbound";"duration"=0;"price"=0;"working_schedule"=[{"to"="18:00:00";"day"=5.;"from"="09:00:00";};];"holiday_dates"={"dates"=[];"endDate"="2020-07-19";"startDate"="2020-05-10";};"dayoff_dates"={"dates"=[];"endDate"="2020-07-19";"startDate"="2020-05-10";};"holiday_dates"={"dates"=[];"endDate"="2020-07-19";"startDate"="2020-05-10";};"cargo_types"=#;"tags"={"meta"={"key"="value"};};"delivery_type"="pickup";};
{"lms_id"=105;"segment_lms_id"=6;"status"="active";"code"="HANDING";"type"="outbound";"duration"=0;"price"=0;"working_schedule"=[{"to"="18:00:00";"day"=5.;"from"="09:00:00";};];"holiday_dates"={"dates"=[];"endDate"="2020-07-19";"startDate"="2020-05-10";};"dayoff_dates"={"dates"=[];"endDate"="2020-07-19";"startDate"="2020-05-10";};"holiday_dates"={"dates"=[];"endDate"="2020-07-19";"startDate"="2020-05-10";};"cargo_types"=#;"tags"={"meta"={"key"="value"};};"delivery_type"="courier";};`

	segmentsReader, err := createReader(segmentsYson)
	require.NoError(t, err)
	defer closeReader(t, segmentsReader)

	edgesReader, err := createReader(edgesYson)
	require.NoError(t, err)
	defer closeReader(t, edgesReader)

	servicesReader, err := createReader(servicesYson)
	require.NoError(t, err)
	defer closeReader(t, servicesReader)

	ctx := context.Background()
	g := NewGraphWithHintsV3(MakeHintMap(TestHints()))
	err = g.readLogSegments(settings.FromContext(ctx), segmentsReader, nil, nil)
	require.NoError(t, err)
	err = g.readLogEdges(ctx, edgesReader)
	require.NoError(t, err)
	err = g.readLogServices(settings.FromContext(ctx), servicesReader)
	require.NoError(t, err)
	g.Finish(ctx)

	expectedMap := map[int64]*Node{
		10000000000: {
			LogisticSegment: LogisticSegment{
				ID:   2,
				Type: SegmentTypeGoPlatform,
			},
			InEdges:  1,
			OutEdges: 0,
		},
	}

	require.Equal(t, len(expectedMap), len(g.GoPlatformMap))
	for pointLmsID, expected := range expectedMap {
		actual, ok := g.GoPlatformMap[pointLmsID]
		require.True(t, ok)
		require.NotNil(t, actual)
		require.Equal(t, expected.ID, actual.ID)
		require.Equal(t, expected.Type, actual.Type)
		require.Equal(t, expected.InEdges, actual.InEdges)
		require.Equal(t, expected.OutEdges, actual.OutEdges)
	}

	for _, node := range g.GoPlatformMap {
		require.Equal(t, node.CourierServices, node.PickupServices)
	}

	// one of the segments is discarded due to the incoming edge absence,
	// the other - for being inactive (segment with lms_id = 4 doesn't have any services)
	require.Equal(t, 0, g.numGoPlInvalid)

	// TODO(sergeykostrov): add test for go_platform segment with LocationID = nil
	require.Equal(t, 0, g.numGoPlNoLocationID)
}

func TestReturnSegments(t *testing.T) {
	createReader := func(tableYson string) (yt.TableReader, error) {
		buf := bytes.NewBufferString(tableYson)
		return ytutil.NewFileReader2(buf)
	}
	closeReader := func(t *testing.T, reader yt.TableReader) {
		err := reader.Close()
		require.NoError(t, err)
	}
	// Необходимость фиктивных сервисов на backward_movement для управления активностью
	// Необходимость собственных обратных сервисов на СЦ и ФФ со своим расписанием
	segmentsYson := `{"lms_id"=2;"partner_lms_id"=200;"logistics_point_lms_id"=#;"location_id"=213;"type"="backward_movement";"partner_type"="DELIVERY";"partner_name"="МК Тарный 2";};
{"lms_id"=3;"partner_lms_id"=100;"logistics_point_lms_id"=10000000001;"location_id"=213;"type"="warehouse";"partner_type"="SORTING_CENTER";"partner_name"="МК Тарный 1";};
{"lms_id"=4;"partner_lms_id"=200;"logistics_point_lms_id"=10000000003;"location_id"=213;"type"="warehouse";"partner_type"="SORTING_CENTER";"partner_name"="МК Тарный 2";};
{"lms_id"=5;"partner_lms_id"=300;"logistics_point_lms_id"=10000000002;"location_id"=213;"type"="warehouse";"partner_type"="SORTING_CENTER";"partner_name"="МК Восток";};
{"lms_id"=7;"partner_lms_id"=172;"logistics_point_lms_id"=10000000004;"location_id"=213;"type"="warehouse";"partner_type"="FULFILLMENT";"partner_name"="Яндекс.Маркет (Софьино)";};
{"lms_id"=8;"partner_lms_id"=172;"logistics_point_lms_id"=#;"location_id"=213;"type"="backward_movement";"partner_type"="FULFILLMENT";"partner_name"="Яндекс.Маркет (Софьино)";};
{"lms_id"=9;"partner_lms_id"=172;"logistics_point_lms_id"=#;"location_id"=213;"type"="backward_movement";"partner_type"="FULFILLMENT";"partner_name"="Яндекс.Маркет (Софьино)";};`

	edgesYson := `{"lms_id"=1000;"from_segment_lms_id"=3;"to_segment_lms_id"=2;};
{"lms_id"=1002;"from_segment_lms_id"=2;"to_segment_lms_id"=4;};
{"lms_id"=1003;"from_segment_lms_id"=4;"to_segment_lms_id"=8;};
{"lms_id"=1004;"from_segment_lms_id"=8;"to_segment_lms_id"=7;};
{"lms_id"=1005;"from_segment_lms_id"=5;"to_segment_lms_id"=9;};
{"lms_id"=1006;"from_segment_lms_id"=9;"to_segment_lms_id"=7;};`

	servicesYson := `{"lms_id":30001,"segment_lms_id":3,"status":"active","code":"INBOUND","type":"inbound","duration":0,"price":0,"working_schedule":[{"day":1.0,"from":"09:00:00","to":"20:00:00"},{"day":2.0,"from":"09:00:00","to":"20:00:00"},{"day":3.0,"from":"09:00:00","to":"20:00:00"},{"day":4.0,"from":"09:00:00","to":"20:00:00"},{"day":5.0,"from":"09:00:00","to":"20:00:00"},{"day":6.0,"from":"09:00:00","to":"20:00:00"},{"day":7.0,"from":"09:00:00","to":"20:00:00"}],"cargo_types":[],"delivery_type":"","tags":{"meta":{}},"holiday_dates":{"dates":[],"startDate":"","endDate":""},"dayoff_dates":{"dates":[],"startDate":"","endDate":""}}
{"lms_id":30002,"segment_lms_id":3,"status":"active","code":"SORT","type":"internal","duration":0,"price":0,"working_schedule":[{"day":1.0,"from":"09:00:00","to":"20:00:00"},{"day":2.0,"from":"09:00:00","to":"20:00:00"},{"day":3.0,"from":"09:00:00","to":"20:00:00"},{"day":4.0,"from":"09:00:00","to":"20:00:00"},{"day":5.0,"from":"09:00:00","to":"20:00:00"},{"day":6.0,"from":"09:00:00","to":"20:00:00"},{"day":7.0,"from":"09:00:00","to":"20:00:00"}],"cargo_types":[],"delivery_type":"","tags":{"meta":{}},"holiday_dates":{"dates":[],"startDate":"","endDate":""},"dayoff_dates":{"dates":[],"startDate":"","endDate":""}}
{"lms_id":30003,"segment_lms_id":3,"status":"active","code":"SHIPMENT","type":"outbound","duration":0,"price":0,"working_schedule":[{"day":1.0,"from":"09:00:00","to":"20:00:00"},{"day":2.0,"from":"09:00:00","to":"20:00:00"},{"day":3.0,"from":"09:00:00","to":"20:00:00"},{"day":4.0,"from":"09:00:00","to":"20:00:00"},{"day":5.0,"from":"09:00:00","to":"20:00:00"},{"day":6.0,"from":"09:00:00","to":"20:00:00"},{"day":7.0,"from":"09:00:00","to":"20:00:00"}],"cargo_types":[],"delivery_type":"","tags":{"meta":{}},"holiday_dates":{"dates":[],"startDate":"","endDate":""},"dayoff_dates":{"dates":[],"startDate":"","endDate":""}}
{"lms_id":40001,"segment_lms_id":4,"status":"active","code":"INBOUND","type":"inbound","duration":0,"price":0,"working_schedule":[{"day":1.0,"from":"09:00:00","to":"20:00:00"},{"day":2.0,"from":"09:00:00","to":"20:00:00"},{"day":3.0,"from":"09:00:00","to":"20:00:00"},{"day":4.0,"from":"09:00:00","to":"20:00:00"},{"day":5.0,"from":"09:00:00","to":"20:00:00"},{"day":6.0,"from":"09:00:00","to":"20:00:00"},{"day":7.0,"from":"09:00:00","to":"20:00:00"}],"cargo_types":[],"delivery_type":"","tags":{"meta":{}},"holiday_dates":{"dates":[],"startDate":"","endDate":""},"dayoff_dates":{"dates":[],"startDate":"","endDate":""}}
{"lms_id":40002,"segment_lms_id":4,"status":"active","code":"SORT","type":"internal","duration":0,"price":0,"working_schedule":[{"day":1.0,"from":"09:00:00","to":"20:00:00"},{"day":2.0,"from":"09:00:00","to":"20:00:00"},{"day":3.0,"from":"09:00:00","to":"20:00:00"},{"day":4.0,"from":"09:00:00","to":"20:00:00"},{"day":5.0,"from":"09:00:00","to":"20:00:00"},{"day":6.0,"from":"09:00:00","to":"20:00:00"},{"day":7.0,"from":"09:00:00","to":"20:00:00"}],"cargo_types":[],"delivery_type":"","tags":{"meta":{}},"holiday_dates":{"dates":[],"startDate":"","endDate":""},"dayoff_dates":{"dates":[],"startDate":"","endDate":""}}
{"lms_id":40003,"segment_lms_id":4,"status":"active","code":"SHIPMENT","type":"outbound","duration":0,"price":0,"working_schedule":[{"day":1.0,"from":"09:00:00","to":"20:00:00"},{"day":2.0,"from":"09:00:00","to":"20:00:00"},{"day":3.0,"from":"09:00:00","to":"20:00:00"},{"day":4.0,"from":"09:00:00","to":"20:00:00"},{"day":5.0,"from":"09:00:00","to":"20:00:00"},{"day":6.0,"from":"09:00:00","to":"20:00:00"},{"day":7.0,"from":"09:00:00","to":"20:00:00"}],"cargo_types":[],"delivery_type":"","tags":{"meta":{}},"holiday_dates":{"dates":[],"startDate":"","endDate":""},"dayoff_dates":{"dates":[],"startDate":"","endDate":""}}
{"lms_id":50001,"segment_lms_id":5,"status":"active","code":"INBOUND","type":"inbound","duration":0,"price":0,"working_schedule":[{"day":1.0,"from":"09:00:00","to":"20:00:00"},{"day":2.0,"from":"09:00:00","to":"20:00:00"},{"day":3.0,"from":"09:00:00","to":"20:00:00"},{"day":4.0,"from":"09:00:00","to":"20:00:00"},{"day":5.0,"from":"09:00:00","to":"20:00:00"},{"day":6.0,"from":"09:00:00","to":"20:00:00"},{"day":7.0,"from":"09:00:00","to":"20:00:00"}],"cargo_types":[],"delivery_type":"","tags":{"meta":{}},"holiday_dates":{"dates":[],"startDate":"","endDate":""},"dayoff_dates":{"dates":[],"startDate":"","endDate":""}}
{"lms_id":50002,"segment_lms_id":5,"status":"active","code":"SORT","type":"internal","duration":0,"price":0,"working_schedule":[{"day":1.0,"from":"09:00:00","to":"20:00:00"},{"day":2.0,"from":"09:00:00","to":"20:00:00"},{"day":3.0,"from":"09:00:00","to":"20:00:00"},{"day":4.0,"from":"09:00:00","to":"20:00:00"},{"day":5.0,"from":"09:00:00","to":"20:00:00"},{"day":6.0,"from":"09:00:00","to":"20:00:00"},{"day":7.0,"from":"09:00:00","to":"20:00:00"}],"cargo_types":[],"delivery_type":"","tags":{"meta":{}},"holiday_dates":{"dates":[],"startDate":"","endDate":""},"dayoff_dates":{"dates":[],"startDate":"","endDate":""}}
{"lms_id":50003,"segment_lms_id":5,"status":"active","code":"SHIPMENT","type":"outbound","duration":0,"price":0,"working_schedule":[{"day":1.0,"from":"09:00:00","to":"20:00:00"},{"day":2.0,"from":"09:00:00","to":"20:00:00"},{"day":3.0,"from":"09:00:00","to":"20:00:00"},{"day":4.0,"from":"09:00:00","to":"20:00:00"},{"day":5.0,"from":"09:00:00","to":"20:00:00"},{"day":6.0,"from":"09:00:00","to":"20:00:00"},{"day":7.0,"from":"09:00:00","to":"20:00:00"}],"cargo_types":[],"delivery_type":"","tags":{"meta":{}},"holiday_dates":{"dates":[],"startDate":"","endDate":""},"dayoff_dates":{"dates":[],"startDate":"","endDate":""}}
{"lms_id":70001,"segment_lms_id":7,"status":"active","code":"INBOUND","type":"inbound","duration":0,"price":0,"working_schedule":[{"day":1.0,"from":"09:00:00","to":"20:00:00"},{"day":2.0,"from":"09:00:00","to":"20:00:00"},{"day":3.0,"from":"09:00:00","to":"20:00:00"},{"day":4.0,"from":"09:00:00","to":"20:00:00"},{"day":5.0,"from":"09:00:00","to":"20:00:00"},{"day":6.0,"from":"09:00:00","to":"20:00:00"},{"day":7.0,"from":"09:00:00","to":"20:00:00"}],"cargo_types":[],"delivery_type":"","tags":{"meta":{}},"holiday_dates":{"dates":[],"startDate":"","endDate":""},"dayoff_dates":{"dates":[],"startDate":"","endDate":""}}
{"lms_id":70002,"segment_lms_id":7,"status":"active","code":"PROCESSING","type":"internal","duration":0,"price":0,"working_schedule":[{"day":1.0,"from":"09:00:00","to":"20:00:00"},{"day":2.0,"from":"09:00:00","to":"20:00:00"},{"day":3.0,"from":"09:00:00","to":"20:00:00"},{"day":4.0,"from":"09:00:00","to":"20:00:00"},{"day":5.0,"from":"09:00:00","to":"20:00:00"},{"day":6.0,"from":"09:00:00","to":"20:00:00"},{"day":7.0,"from":"09:00:00","to":"20:00:00"}],"cargo_types":[],"delivery_type":"","tags":{"meta":{}},"holiday_dates":{"dates":[],"startDate":"","endDate":""},"dayoff_dates":{"dates":[],"startDate":"","endDate":""}}`

	// SC(МК Тарный 1).lms_id=3
	//            |
	//            |
	//            V
	// BMV(МК Тарный 2).lms_id=2
	//            |
	//            |
	//            V
	// SC(МК Тарный 2).lms_id=4                 SC(МК Восток).lms_id=5
	//            |                                       |
	//            |                                       |
	//            V                                       V
	// BMV(Яндекс.Маркет (Софьино)).lms_id=8   BMV(Яндекс.Маркет (Софьино)).lms_id=9
	//                        \                          /
	//                         \                        /
	//                          V                      V
	//                    FF(Яндекс.Маркет (Софьино)).lms_id=7

	segmentsReader, err := createReader(segmentsYson)
	require.NoError(t, err)
	defer closeReader(t, segmentsReader)

	edgesReader, err := createReader(edgesYson)
	require.NoError(t, err)
	defer closeReader(t, edgesReader)

	servicesReader, err := createReader(servicesYson)
	require.NoError(t, err)
	defer closeReader(t, servicesReader)

	backwardSettings, _ := its.NewStringSettingsHolder("{\"read_backward_graph\": true}")
	stx := settings.New(backwardSettings.GetSettings(), "")
	ctx := settings.ContextWithSettings(context.Background(), settings.New(backwardSettings.GetSettings(), ""))
	g := NewGraphWithHintsV3(MakeHintMap(TestHints()))
	err = g.readLogSegments(stx, segmentsReader, nil, nil)
	require.NoError(t, err)
	err = g.readLogEdges(ctx, edgesReader)
	require.NoError(t, err)
	err = g.readLogServices(stx, servicesReader)
	require.NoError(t, err)
	g.Finish(ctx)

	sofinoNode := &Node{
		LogisticSegment: LogisticSegment{
			ID:           7,
			Type:         SegmentTypeWarehouse,
			PartnerLmsID: 172,
			PointLmsID:   10000000004,
			LocationID:   213,
		},
		InEdges:  1,
		OutEdges: 1,
	}

	tarnij2Node := &Node{
		LogisticSegment: LogisticSegment{
			ID:           4,
			Type:         SegmentTypeWarehouse,
			PartnerLmsID: 200,
			PointLmsID:   10000000003,
			LocationID:   213,
		},
		InEdges:  1,
		OutEdges: 1,
	}

	wantedBackwardMovementsWarehousesMap := map[int64][]*Node{
		2: {tarnij2Node},
		8: {sofinoNode},
		9: {sofinoNode},
	}

	require.Equal(t, len(wantedBackwardMovementsWarehousesMap), len(g.backwardMovementsWarehousesMap))
	for bMvID, whSegment := range g.backwardMovementsWarehousesMap {
		wantWhSegment := wantedBackwardMovementsWarehousesMap[bMvID]
		require.Equal(t, len(wantWhSegment), len(whSegment))
		require.Equal(t, wantWhSegment[0].LogisticSegment.ID, whSegment[0].LogisticSegment.ID)
		require.Equal(t, wantWhSegment[0].LogisticSegment.Type, whSegment[0].LogisticSegment.Type)
		require.Equal(t, wantWhSegment[0].LogisticSegment.PartnerLmsID, whSegment[0].LogisticSegment.PartnerLmsID)
		require.Equal(t, wantWhSegment[0].LogisticSegment.PointLmsID, whSegment[0].LogisticSegment.PointLmsID)
		require.Equal(t, wantWhSegment[0].LogisticSegment.LocationID, whSegment[0].LogisticSegment.LocationID)
	}

	wantedWarehouseBackwardMovementsMap := map[int64][]*Node{
		3: {
			&Node{
				LogisticSegment: LogisticSegment{
					ID:           2,
					Type:         SegmentTypeBackwardMovement,
					PartnerLmsID: 200,
					LocationID:   213,
				},
				InEdges:  1,
				OutEdges: 1,
			},
		},
		4: {
			&Node{
				LogisticSegment: LogisticSegment{
					ID:           8,
					Type:         SegmentTypeBackwardMovement,
					PartnerLmsID: 172,
					LocationID:   213,
				},
				InEdges:  1,
				OutEdges: 1,
			},
		},
		5: {
			&Node{
				LogisticSegment: LogisticSegment{
					ID:           9,
					Type:         SegmentTypeBackwardMovement,
					PartnerLmsID: 172,
					LocationID:   213,
				},
				InEdges:  1,
				OutEdges: 1,
			},
		},
	}

	require.Equal(t, len(wantedWarehouseBackwardMovementsMap), len(g.warehouseBackwardMovementsMap))
	for whID, bMvSegment := range g.warehouseBackwardMovementsMap {
		wantBMVSegment := wantedWarehouseBackwardMovementsMap[whID]
		require.Equal(t, len(wantBMVSegment), len(bMvSegment))
		require.Equal(t, wantBMVSegment[0].LogisticSegment.ID, bMvSegment[0].LogisticSegment.ID)
		require.Equal(t, wantBMVSegment[0].LogisticSegment.Type, bMvSegment[0].LogisticSegment.Type)
		require.Equal(t, wantBMVSegment[0].LogisticSegment.PartnerLmsID, bMvSegment[0].LogisticSegment.PartnerLmsID)
		require.Equal(t, wantBMVSegment[0].LogisticSegment.LocationID, bMvSegment[0].LogisticSegment.LocationID)
	}
}

// Тест что возвратный граф не подмешивается к прямому
func TestSeparatedNeighbours(t *testing.T) {
	createReader := func(tableYson string) (yt.TableReader, error) {
		buf := bytes.NewBufferString(tableYson)
		return ytutil.NewFileReader2(buf)
	}
	closeReader := func(t *testing.T, reader yt.TableReader) {
		err := reader.Close()
		require.NoError(t, err)
	}
	segmentsYson := `{"lms_id"=2;"partner_lms_id"=200;"logistics_point_lms_id"=#;"location_id"=213;"type"="backward_movement";"partner_type"="DELIVERY";"partner_name"="МК Тарный 2";};
{"lms_id"=3;"partner_lms_id"=100;"logistics_point_lms_id"=10000000001;"location_id"=213;"type"="warehouse";"partner_type"="SORTING_CENTER";"partner_name"="МК Тарный 1";};
{"lms_id"=4;"partner_lms_id"=200;"logistics_point_lms_id"=10000000003;"location_id"=213;"type"="warehouse";"partner_type"="SORTING_CENTER";"partner_name"="МК Тарный 2";};
{"lms_id"=7;"partner_lms_id"=172;"logistics_point_lms_id"=10000000004;"location_id"=213;"type"="warehouse";"partner_type"="FULFILLMENT";"partner_name"="Яндекс.Маркет (Софьино)";};
{"lms_id"=9;"partner_lms_id"=172;"logistics_point_lms_id"=#;"location_id"=213;"type"="movement";"partner_type"="FULFILLMENT";"partner_name"="Яндекс.Маркет (Софьино)";};`

	edgesYson := `{"lms_id"=1000;"from_segment_lms_id"=3;"to_segment_lms_id"=2;};
{"lms_id"=1002;"from_segment_lms_id"=2;"to_segment_lms_id"=4;};
{"lms_id"=1005;"from_segment_lms_id"=9;"to_segment_lms_id"=7;};`

	servicesYson := `{"lms_id":30001,"segment_lms_id":3,"status":"active","code":"INBOUND","type":"inbound","duration":0,"price":0,"working_schedule":[{"day":1.0,"from":"09:00:00","to":"20:00:00"},{"day":2.0,"from":"09:00:00","to":"20:00:00"},{"day":3.0,"from":"09:00:00","to":"20:00:00"},{"day":4.0,"from":"09:00:00","to":"20:00:00"},{"day":5.0,"from":"09:00:00","to":"20:00:00"},{"day":6.0,"from":"09:00:00","to":"20:00:00"},{"day":7.0,"from":"09:00:00","to":"20:00:00"}],"cargo_types":[],"delivery_type":"","tags":{"meta":{}},"holiday_dates":{"dates":[],"startDate":"","endDate":""},"dayoff_dates":{"dates":[],"startDate":"","endDate":""}}
{"lms_id":30002,"segment_lms_id":3,"status":"active","code":"SORT","type":"internal","duration":0,"price":0,"working_schedule":[{"day":1.0,"from":"09:00:00","to":"20:00:00"},{"day":2.0,"from":"09:00:00","to":"20:00:00"},{"day":3.0,"from":"09:00:00","to":"20:00:00"},{"day":4.0,"from":"09:00:00","to":"20:00:00"},{"day":5.0,"from":"09:00:00","to":"20:00:00"},{"day":6.0,"from":"09:00:00","to":"20:00:00"},{"day":7.0,"from":"09:00:00","to":"20:00:00"}],"cargo_types":[],"delivery_type":"","tags":{"meta":{}},"holiday_dates":{"dates":[],"startDate":"","endDate":""},"dayoff_dates":{"dates":[],"startDate":"","endDate":""}}
{"lms_id":30003,"segment_lms_id":3,"status":"active","code":"SHIPMENT","type":"outbound","duration":0,"price":0,"working_schedule":[{"day":1.0,"from":"09:00:00","to":"20:00:00"},{"day":2.0,"from":"09:00:00","to":"20:00:00"},{"day":3.0,"from":"09:00:00","to":"20:00:00"},{"day":4.0,"from":"09:00:00","to":"20:00:00"},{"day":5.0,"from":"09:00:00","to":"20:00:00"},{"day":6.0,"from":"09:00:00","to":"20:00:00"},{"day":7.0,"from":"09:00:00","to":"20:00:00"}],"cargo_types":[],"delivery_type":"","tags":{"meta":{}},"holiday_dates":{"dates":[],"startDate":"","endDate":""},"dayoff_dates":{"dates":[],"startDate":"","endDate":""}}
{"lms_id":40001,"segment_lms_id":4,"status":"active","code":"INBOUND","type":"inbound","duration":0,"price":0,"working_schedule":[{"day":1.0,"from":"09:00:00","to":"20:00:00"},{"day":2.0,"from":"09:00:00","to":"20:00:00"},{"day":3.0,"from":"09:00:00","to":"20:00:00"},{"day":4.0,"from":"09:00:00","to":"20:00:00"},{"day":5.0,"from":"09:00:00","to":"20:00:00"},{"day":6.0,"from":"09:00:00","to":"20:00:00"},{"day":7.0,"from":"09:00:00","to":"20:00:00"}],"cargo_types":[],"delivery_type":"","tags":{"meta":{}},"holiday_dates":{"dates":[],"startDate":"","endDate":""},"dayoff_dates":{"dates":[],"startDate":"","endDate":""}}
{"lms_id":40002,"segment_lms_id":4,"status":"active","code":"SORT","type":"internal","duration":0,"price":0,"working_schedule":[{"day":1.0,"from":"09:00:00","to":"20:00:00"},{"day":2.0,"from":"09:00:00","to":"20:00:00"},{"day":3.0,"from":"09:00:00","to":"20:00:00"},{"day":4.0,"from":"09:00:00","to":"20:00:00"},{"day":5.0,"from":"09:00:00","to":"20:00:00"},{"day":6.0,"from":"09:00:00","to":"20:00:00"},{"day":7.0,"from":"09:00:00","to":"20:00:00"}],"cargo_types":[],"delivery_type":"","tags":{"meta":{}},"holiday_dates":{"dates":[],"startDate":"","endDate":""},"dayoff_dates":{"dates":[],"startDate":"","endDate":""}}
{"lms_id":40003,"segment_lms_id":4,"status":"active","code":"SHIPMENT","type":"outbound","duration":0,"price":0,"working_schedule":[{"day":1.0,"from":"09:00:00","to":"20:00:00"},{"day":2.0,"from":"09:00:00","to":"20:00:00"},{"day":3.0,"from":"09:00:00","to":"20:00:00"},{"day":4.0,"from":"09:00:00","to":"20:00:00"},{"day":5.0,"from":"09:00:00","to":"20:00:00"},{"day":6.0,"from":"09:00:00","to":"20:00:00"},{"day":7.0,"from":"09:00:00","to":"20:00:00"}],"cargo_types":[],"delivery_type":"","tags":{"meta":{}},"holiday_dates":{"dates":[],"startDate":"","endDate":""},"dayoff_dates":{"dates":[],"startDate":"","endDate":""}}
{"lms_id":70001,"segment_lms_id":7,"status":"active","code":"INBOUND","type":"inbound","duration":0,"price":0,"working_schedule":[{"day":1.0,"from":"09:00:00","to":"20:00:00"},{"day":2.0,"from":"09:00:00","to":"20:00:00"},{"day":3.0,"from":"09:00:00","to":"20:00:00"},{"day":4.0,"from":"09:00:00","to":"20:00:00"},{"day":5.0,"from":"09:00:00","to":"20:00:00"},{"day":6.0,"from":"09:00:00","to":"20:00:00"},{"day":7.0,"from":"09:00:00","to":"20:00:00"}],"cargo_types":[],"delivery_type":"","tags":{"meta":{}},"holiday_dates":{"dates":[],"startDate":"","endDate":""},"dayoff_dates":{"dates":[],"startDate":"","endDate":""}}
{"lms_id":70002,"segment_lms_id":7,"status":"active","code":"PROCESSING","type":"internal","duration":0,"price":0,"working_schedule":[{"day":1.0,"from":"09:00:00","to":"20:00:00"},{"day":2.0,"from":"09:00:00","to":"20:00:00"},{"day":3.0,"from":"09:00:00","to":"20:00:00"},{"day":4.0,"from":"09:00:00","to":"20:00:00"},{"day":5.0,"from":"09:00:00","to":"20:00:00"},{"day":6.0,"from":"09:00:00","to":"20:00:00"},{"day":7.0,"from":"09:00:00","to":"20:00:00"}],"cargo_types":[],"delivery_type":"","tags":{"meta":{}},"holiday_dates":{"dates":[],"startDate":"","endDate":""},"dayoff_dates":{"dates":[],"startDate":"","endDate":""}}
{"lms_id":70001,"segment_lms_id":9,"status":"active","code":"MOVEMENT","type":"internal","duration":0,"price":0,"working_schedule":[{"day":1.0,"from":"09:00:00","to":"20:00:00"},{"day":2.0,"from":"09:00:00","to":"20:00:00"},{"day":3.0,"from":"09:00:00","to":"20:00:00"},{"day":4.0,"from":"09:00:00","to":"20:00:00"},{"day":5.0,"from":"09:00:00","to":"20:00:00"},{"day":6.0,"from":"09:00:00","to":"20:00:00"},{"day":7.0,"from":"09:00:00","to":"20:00:00"}],"cargo_types":[],"delivery_type":"","tags":{"meta":{}},"holiday_dates":{"dates":[],"startDate":"","endDate":""},"dayoff_dates":{"dates":[],"startDate":"","endDate":""}}
{"lms_id":70002,"segment_lms_id":9,"status":"active","code":"SHIPMENT","type":"outbound","duration":0,"price":0,"working_schedule":[{"day":1.0,"from":"09:00:00","to":"20:00:00"},{"day":2.0,"from":"09:00:00","to":"20:00:00"},{"day":3.0,"from":"09:00:00","to":"20:00:00"},{"day":4.0,"from":"09:00:00","to":"20:00:00"},{"day":5.0,"from":"09:00:00","to":"20:00:00"},{"day":6.0,"from":"09:00:00","to":"20:00:00"},{"day":7.0,"from":"09:00:00","to":"20:00:00"}],"cargo_types":[],"delivery_type":"","tags":{"meta":{}},"holiday_dates":{"dates":[],"startDate":"","endDate":""},"dayoff_dates":{"dates":[],"startDate":"","endDate":""}}`

	// обратный граф:
	// SC(МК Тарный 1).lms_id=3
	//            |
	//            |
	//            V
	// BMV(МК Тарный 2).lms_id=2
	//            |
	//            |
	//            V
	// SC(МК Тарный 2).lms_id=4
	//
	// прямой граф:
	// MV(Яндекс.Маркет (Софьино)).lms_id=9
	//            |
	//            |
	//            V
	// SC(МК Восток).lms_id=7

	segmentsReader, err := createReader(segmentsYson)
	require.NoError(t, err)
	defer closeReader(t, segmentsReader)

	edgesReader, err := createReader(edgesYson)
	require.NoError(t, err)
	defer closeReader(t, edgesReader)

	servicesReader, err := createReader(servicesYson)
	require.NoError(t, err)
	defer closeReader(t, servicesReader)

	ctx := context.Background()
	g := NewGraphWithHintsV3(MakeHintMap(TestHints()))
	err = g.readLogSegments(settings.FromContext(ctx), segmentsReader, nil, nil)
	require.NoError(t, err)
	err = g.readLogEdges(ctx, edgesReader)
	require.NoError(t, err)
	err = g.readLogServices(settings.FromContext(ctx), servicesReader)
	require.NoError(t, err)
	g.Finish(ctx)

	require.Len(t, g.Neighbors, 1)
	require.Len(t, g.BackwardNeighbors, 2)
	require.Len(t, g.backwardMovementsWarehousesMap, 1)
	require.Len(t, g.warehouseBackwardMovementsMap, 1)
	require.Len(t, g.MovementWarehousesMap, 1)
}
