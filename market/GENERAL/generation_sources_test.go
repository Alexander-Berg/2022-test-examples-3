package expresslite

import (
	"context"
	"fmt"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/express"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	tr "a.yandex-team.ru/market/combinator/pkg/tarifficator"
	"a.yandex-team.ru/market/combinator/pkg/units"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

const (
	expressWarehouseID       = 666
	expressDeliveryPartnerID = 777

	warehouseWithCapacityCheckID        = 888
	shopWithCapacityCheckID      uint32 = 1353648

	userLatitude  = 55.724987
	userLongitude = 37.568081

	totalPrice = 4500
)

var (
	regionMap geobase.RegionMap
)

func init() {
	regionMap = geobase.NewExample()
}

func makeExpressGraphTemplate(
	whProcessingFrom string,
	whProcessingTo string,
	whShipmentFrom string,
	whShipmentTo string,
	mvMovementFrom string,
	mvMovementTo string,
	mvShipmentFrom string,
	mvShipmentTo string,
) *graph.Graph {
	pb := makeExpressGraphPathBuilder(
		whProcessingFrom, whProcessingTo,
		whShipmentFrom, whShipmentTo,
		mvMovementFrom, mvMovementTo,
		mvShipmentFrom, mvShipmentTo,
	)

	return pb.GetGraph()
}

func makeExpressGraphPathBuilder(
	whProcessingFrom string,
	whProcessingTo string,
	whShipmentFrom string,
	whShipmentTo string,
	mvMovementFrom string,
	mvMovementTo string,
	mvShipmentFrom string,
	mvShipmentTo string,
) *graph.PathBuilder {
	pb := graph.NewPathBuilder()
	expressWarehouse := pb.AddExpressWarehouse(
		expressWarehouseID,
		geobase.RegionMoscow,
		whProcessingFrom, whProcessingTo,
		whShipmentFrom, whShipmentTo,
	)
	expressMovement := pb.AddExpressMovement(
		expressDeliveryPartnerID,
		mvMovementFrom, mvMovementTo,
		mvShipmentFrom, mvShipmentTo,
	)
	expressLinehaul := pb.AddExpressLinehaul(
		expressDeliveryPartnerID,
		geobase.RegionMoscow,
	)
	pb.AddExpressHanding(
		expressDeliveryPartnerID,
		geobase.RegionMoscow,
	)
	pb.AddEdge(expressWarehouse, expressMovement)
	pb.AddEdge(expressMovement, expressLinehaul)
	pb.GetGraph().Finish(context.Background())

	return pb
}

func makeExpressReturnGraph(from, to string) *graph.Graph {
	pathBuilder := makeExpressGraphPathBuilder(
		from, // warehouse.PROCESSING from
		to,   // warehouse.PROCESSING to
		from, // warehouse.SHIPMENT from
		to,   // warehouse.SHIPMENT to
		from, // movement.MOVEMENT from
		to,   // movement.MOVEMENT to
		from, // movement.SHIPMENT from
		to,   // movement.SHIPMENT to
	)

	intermediateReturnSC := pathBuilder.AddWarehouse(
		pathBuilder.WithPartnerTypeSortingCenter(),
		pathBuilder.MakeProcessingService(),
		pathBuilder.WithPartnerLmsID(intermediateReturnSCPartner),
		pathBuilder.WithPointLmsID(intermediateReturnSCPoint),
	)
	intermediateReturnSC2 := pathBuilder.AddWarehouse(
		pathBuilder.WithPartnerTypeSortingCenter(),
		pathBuilder.MakeProcessingService(),
	)
	backM := pathBuilder.AddBackwardMovement()
	backM2 := pathBuilder.AddBackwardMovement()
	returnSC := pathBuilder.AddWarehouse(
		pathBuilder.WithPartnerTypeSortingCenter(),
		pathBuilder.MakeProcessingService(),
		pathBuilder.WithPartnerLmsID(returnSCPartner),
		pathBuilder.WithPointLmsID(returnSCPoint),
	)
	returnSC2 := pathBuilder.AddWarehouse(
		pathBuilder.WithPartnerTypeSortingCenter(),
		pathBuilder.MakeProcessingService(),
	)
	backMForExpress := pathBuilder.AddBackwardMovement(
		pathBuilder.WithPartnerLmsID(expressWarehouseID),
	)
	expressWarehouse := pathBuilder.GetGraph().PartnerToSegments[expressWarehouseID][0]
	pathBuilder.AddBEdge(intermediateReturnSC, backM)
	pathBuilder.AddBEdge(intermediateReturnSC2, backM2)
	pathBuilder.AddBEdge(backM, returnSC)
	pathBuilder.AddBEdge(backM2, returnSC2)
	pathBuilder.AddBEdge(returnSC, backMForExpress)
	pathBuilder.AddBEdge(backMForExpress, expressWarehouse.ID)
	g := pathBuilder.GetGraph()
	g.NodesGeoStorage.AddLocation(intermediateReturnSC, units.GpsCoords{Latitude: userLatitude + 0.0002, Longitude: userLongitude + 0.0002})
	g.NodesGeoStorage.AddLocation(intermediateReturnSC2, units.GpsCoords{Latitude: userLatitude + 0.0001, Longitude: userLongitude + 0.0001})
	g.Finish(context.Background())

	return g
}

func makeExpressGraph(from, to string) *graph.Graph {
	return makeExpressGraphTemplate(
		from, // warehouse.PROCESSING from
		to,   // warehouse.PROCESSING to
		from, // warehouse.SHIPMENT from
		to,   // warehouse.SHIPMENT to
		from, // movement.MOVEMENT from
		to,   // movement.MOVEMENT to
		from, // movement.SHIPMENT from
		to,   // movement.SHIPMENT to
	)
}

// yt express generation mock
func makeExpress() *express.Express {
	var expressWarehouseSrc string
	for i, warehouseID := range []int{expressWarehouseID, warehouseWithCapacityCheckID} {
		if i > 0 {
			expressWarehouseSrc = fmt.Sprintf("%s\n", expressWarehouseSrc)
		}
		expressWarehouseSrc = fmt.Sprintf(`%s{"warehouse_id":%d,"latitude":%f,"longitude":%f,"location_id":%d,"ready_to_ship_time":40,"radial_zones":[{"delivery_duration":20.0,"radius":5000.0,"zone_id":1227.0},{"zone_id":1262.0,"delivery_duration":33.0,"radius":10000.0},{"radius":15000.0,"zone_id":1313.0,"delivery_duration":44.0},{"radius":20000.0,"zone_id":1363.0,"delivery_duration":55.0},{"delivery_duration":65.0,"radius":25000.0,"zone_id":1412.0},{"radius":8000.0,"zone_id":4450.0,"delivery_duration":28.0},{"radius":11000.0,"zone_id":4451.0,"delivery_duration":35.0},{"radius":14000.0,"zone_id":4452.0,"delivery_duration":42.0},{"zone_id":4453.0,"delivery_duration":80.0,"radius":36000.0},{"radius":50000.0,"zone_id":4454.0,"delivery_duration":92.0}],"business_id":2483876,"enable_express_outlets":false}`,
			expressWarehouseSrc,
			warehouseID,
			userLatitude,
			userLongitude,
			geobase.RegionMoscow,
		)
	}
	warehouses, err := express.ReadFromString(expressWarehouseSrc, &regionMap)
	if err != nil {
		return nil
	}
	return warehouses
}

// yt express orders mock
func makeExpressIntervalValidator() (vc express.ValidatorsContainer) {
	expressRegionIntervalValidatorStr := `{"count":2,"region":213,"xtime":"2022-01-26T00"}
{"count":3,"region":213,"xtime":"2022-01-26T01"}
{"count":1,"region":213,"xtime":"2022-01-26T02"}
{"count":1,"region":213,"xtime":"2022-01-26T03"}
{"count":1,"region":213,"xtime":"2022-01-26T04"}
{"count":2,"region":213,"xtime":"2022-01-26T05"}
{"count":6,"region":213,"xtime":"2022-01-26T06"}
{"count":14,"region":213,"xtime":"2022-01-26T07"}
{"count":72,"region":213,"xtime":"2022-01-26T08"}
{"count":345,"region":213,"xtime":"2022-01-26T09"}
{"count":557,"region":213,"xtime":"2022-01-26T10"}
{"count":651,"region":213,"xtime":"2022-01-26T11"}
{"count":508,"region":213,"xtime":"2022-01-26T12"}
{"count":851,"region":213,"xtime":"2022-01-26T13"}
{"count":354,"region":213,"xtime":"2022-01-26T14"}
{"count":373,"region":213,"xtime":"2022-01-26T15"}
{"count":184,"region":213,"xtime":"2022-01-26T16"}
{"count":95,"region":213,"xtime":"2022-01-26T17"}
{"count":54,"region":213,"xtime":"2022-01-26T18"}
{"count":20,"region":213,"xtime":"2022-01-26T19"}
{"count":11,"region":213,"xtime":"2022-01-26T20"}
{"count":1,"region":213,"xtime":"2022-01-26T21"}
{"count":700,"region":213,"xtime":"2022-02-24T10"}
{"count":700,"region":213,"xtime":"2022-02-24T11"}
{"count":700,"region":213,"xtime":"2022-02-24T12"}
{"count":99999,"region":213,"xtime":"2022-02-24T13"}
{"count":99999,"region":213,"xtime":"2022-02-24T14"}
{"count":700,"region":213,"xtime":"2022-02-26T10"}
{"count":700,"region":213,"xtime":"2022-02-27T10"}`
	expressPartnerIntervalValidatorStr := `{"count":10,"partner":274612,"xtime":"2022-01-26T09"}
{"count":15,"partner":274612,"xtime":"2022-01-26T10"}
{"count":20,"partner":274612,"xtime":"2022-01-26T11"}`

	regionValidator, err := express.ReadRegionIntervalValidatorFromString(expressRegionIntervalValidatorStr)
	if err != nil {
		return
	}

	partnerValidator, err := express.ReadPartnerIntervalValidatorFromString(expressPartnerIntervalValidatorStr)
	if err != nil {
		return
	}

	vc.RegionIntervalValidator = regionValidator
	vc.PartnerIntervalValidator = partnerValidator

	return
}

// tariff doesn't matter for express tests so far. Here is any applyable
func makeTariffsFinder() *tr.TariffFinderSet {
	tariff := tr.TariffRT{
		ID:                1,
		DeliveryServiceID: expressDeliveryPartnerID,
		DeliveryMethod:    enums.DeliveryMethodCourier,
		ProgramTypeList:   tr.ProgramTypeList(tr.ProgramBeruCrossdock | tr.ProgramMarketDelivery),
		Option: tr.Option{
			Cost:    200,
			DaysMin: 0,
			DaysMax: 0,
		},
		RuleAttrs: tr.RuleAttrs{
			WeightMax: 30000,
			HeightMax: 0,
			LengthMax: 0,
			WidthMax:  0,
			DimSumMax: 0,
		},
		FromToRegions: tr.FromToRegions{
			From: 213,
			To:   213,
		},
		Type: tr.RuleTypePayment,
	}
	tariffsFinder := tr.NewTariffsFinder()
	tariffsFinder.Add(&tariff)
	return &tr.TariffFinderSet{
		Common:     tariffsFinder,
		B2B:        tariffsFinder,
		Nordstream: tariffsFinder,
	}
}

func compareDates(t *testing.T, date1, date2 *pb.Date) {
	require.Equal(t, date1.GetDay(), date2.GetDay())
	require.Equal(t, date1.GetMonth(), date2.GetMonth())
	require.Equal(t, date1.GetYear(), date2.GetYear())
}

func compareTimes(t *testing.T, time1, time2 *pb.Time) {
	require.Equal(t, time1.GetHour(), time2.GetHour())
	require.Equal(t, time1.GetMinute(), time2.GetMinute())
}

func compareIntervals(t *testing.T, interval1, interval2 *pb.DeliveryInterval) {
	compareTimes(t, interval1.GetFrom(), interval2.GetFrom())
	compareTimes(t, interval1.GetTo(), interval2.GetTo())
}
