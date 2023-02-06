package testing

import (
	"context"
	"strings"
	"time"

	timeformats "cuelang.org/go/pkg/time"
	"google.golang.org/grpc"

	"a.yandex-team.ru/library/go/ptr"
	testing "a.yandex-team.ru/travel/komod/trips/api/testing/v1"
	tripsapi "a.yandex-team.ru/travel/komod/trips/api/trips/v1"
	"a.yandex-team.ru/travel/komod/trips/internal/components/api/trips"
	apimodels "a.yandex-team.ru/travel/komod/trips/internal/components/api/trips/models"
	"a.yandex-team.ru/travel/komod/trips/internal/models"
	"a.yandex-team.ru/travel/komod/trips/internal/orders"
	"a.yandex-team.ru/travel/komod/trips/internal/point"
	"a.yandex-team.ru/travel/komod/trips/internal/references"
	ordersproto "a.yandex-team.ru/travel/orders/proto"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
)

type Service struct {
	dictsRegistry    references.References
	tripsProvider    *trips.TestingTripsProvider
	spanPointFactory *point.Factory
}

func NewService(
	tripsProvider *trips.TestingTripsProvider,
	dictsRegistry references.References,
	spanPointFactory *point.Factory,
) *Service {
	return &Service{
		tripsProvider:    tripsProvider,
		dictsRegistry:    dictsRegistry,
		spanPointFactory: spanPointFactory,
	}
}

func (s *Service) Ping(ctx context.Context, in *testing.PingReq) (*testing.PingRsp, error) {
	return &testing.PingRsp{Status: "OK"}, nil
}

func (s *Service) BuildTrips(ctx context.Context, in *testing.BuildTripsReqV1) (*testing.BuildTripsRspV1, error) {
	ordersList := s.getOrders(in)
	tripsRsp, err := s.tripsProvider.GetTrips(ctx, ordersList, int(in.GeoId))
	if err != nil {
		return nil, err
	}
	return &testing.BuildTripsRspV1{
		Active: s.mapPaginatedTripsList(tripsRsp.Active),
		Past:   s.mapPaginatedTripsList(tripsRsp.Past),
	}, nil
}

func (s *Service) GetServiceRegisterer() func(*grpc.Server) {
	return func(server *grpc.Server) {
		testing.RegisterTestingServiceV1Server(server, s)
	}
}

func (s *Service) getOrders(in *testing.BuildTripsReqV1) []orders.Order {
	ordersList := make([]orders.Order, 0)
	for _, o := range in.Orders {
		switch t := o.GetData().(type) {
		case *testing.Order_Avia:
			ordersList = append(ordersList, &orders.AviaOrder{
				BaseOrder: orders.NewBaseOrder(orders.ID(o.Id), "fakePassportID", mapState(o.Status)),
				Route: orders.Route{
					FromSettlement:    s.getSettlementByID(t.Avia.FromSettlement),
					ToSettlement:      s.getSettlementByID(t.Avia.ToSettlement),
					ForwardDeparture:  parseDate(t.Avia.ForwardDepartureDate),
					BackwardDeparture: parseDateOptional(t.Avia.BackwardDepartureDate),
				},
				PNR:      "123",
				Carriers: s.mapAirline(26),
			})
		case *testing.Order_Train:
			ordersList = append(ordersList, &orders.TrainOrder{
				BaseOrder: orders.NewBaseOrder(orders.ID(o.Id), "fakePassportID", mapState(o.Status)),
				Route: orders.Route{
					FromSettlement:    s.getSettlementByID(t.Train.FromSettlement),
					ToSettlement:      s.getSettlementByID(t.Train.ToSettlement),
					ForwardDeparture:  parseDate(t.Train.ForwardDepartureDate),
					BackwardDeparture: parseDateOptional(t.Train.BackwardDepartureDate),
				},
				Trains: []*orders.Train{{
					TrainInfo: orders.TrainInfo{
						Direction:  orders.TrainDirectionForward,
						Number:     "123",
						BrandTitle: "ласточка",
					},
					FromSettlement: s.getSettlementByID(t.Train.FromSettlement),
					ToSettlement:   s.getSettlementByID(t.Train.ToSettlement),
				}},
				PrintURL: "",
			})

		case *testing.Order_Hotel:
			ordersList = append(ordersList, &orders.HotelOrder{
				BaseOrder:    orders.NewBaseOrder(orders.ID(o.Id), "fakePassportID", mapState(o.Status)),
				Point:        s.getPointByGeoID(t.Hotel.GeoId),
				CityGeoID:    int(t.Hotel.GeoId),
				Title:        "The Best Hotel",
				CheckinDate:  parseDate(t.Hotel.CheckinDate),
				CheckoutDate: parseDate(t.Hotel.CheckoutDate),
				Address:      "st. Pushkina",
				Coordinates:  nil,
			})
		}
	}
	return ordersList
}

func (s *Service) mapAirline(carrierID int) []*rasp.TCarrier {
	carrier, _ := s.dictsRegistry.Carriers().Get(carrierID)
	return []*rasp.TCarrier{carrier}
}

func parseDateOptional(date string) *time.Time {
	if date == "" {
		return nil
	}
	return ptr.Time(parseDate(date))
}

func parseDate(date string) time.Time {
	parsed, _ := time.Parse(timeformats.RFC3339Date, date)
	return parsed
}

func (s *Service) getSettlementByID(settlementID int32) *rasp.TSettlement {
	settlement, _ := s.dictsRegistry.Settlements().Get(int(settlementID))
	return settlement
}

func mapState(status string) ordersproto.EDisplayOrderState {
	if strings.ToUpper(status) == "CANCELLED" {
		return ordersproto.EDisplayOrderState_OS_CANCELLED
	}
	return ordersproto.EDisplayOrderState_OS_FULFILLED
}

func (s *Service) mapPaginatedTripsList(response *apimodels.PaginatedTripsListRsp) *tripsapi.PaginatedTripsList {
	return &tripsapi.PaginatedTripsList{
		Trips:             s.mapTripsListItem(response.Trips),
		ContinuationToken: response.ContinuationToken,
	}
}

func (s *Service) mapTripsListItem(items []apimodels.TripItemRsp) (result []*tripsapi.TripsListItem) {
	for _, item := range items {
		var resItem *tripsapi.TripsListItem
		switch value := item.(type) {
		case *apimodels.OrderTripItemRsp:
			resItem = &tripsapi.TripsListItem{
				Type:  tripsapi.TripsListItemType_ORDER_TYPE,
				State: value.State,
				Item: &tripsapi.TripsListItem_OrderItem{
					OrderItem: &tripsapi.OrderItem{
						OrderId:     value.OrderID,
						Title:       value.Title,
						Image:       value.Image,
						DisplayDate: value.DisplayDate,
					},
				},
			}
		case *apimodels.RealTripItemRsp:
			resItem = &tripsapi.TripsListItem{
				Type:  tripsapi.TripsListItemType_ORDER_TYPE,
				State: value.State,
				Item: &tripsapi.TripsListItem_RealItem{
					RealItem: &tripsapi.RealItem{
						Id:          value.ID,
						Title:       value.Title,
						Image:       value.Image,
						DisplayDate: value.DisplayDate,
						OrderIds:    value.OrderIDs,
					},
				},
			}
		}
		result = append(result, resItem)
	}
	return result
}

func (s *Service) getPointByGeoID(geoID int32) models.Point {
	point, _ := s.spanPointFactory.MakeByGeoID(int(geoID))
	return point
}
