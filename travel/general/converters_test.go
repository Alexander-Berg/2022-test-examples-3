package hotels

import (
	"strings"
	"testing"

	"github.com/stretchr/testify/require"
	"google.golang.org/genproto/googleapis/type/date"
	"google.golang.org/protobuf/types/known/wrapperspb"

	hotelsAPI "a.yandex-team.ru/travel/app/backend/api/hotels/v1"
	"a.yandex-team.ru/travel/app/backend/internal/lib/travelapiclient/models"
)

func TestConvertToGetCountersReq_EmptyQuery_Error(t *testing.T) {
	testcases := []struct {
		req *hotelsAPI.GetHotelCountersReq
		err string
	}{
		{
			req: nil,
			err: "queryData is require",
		},
		{
			req: &hotelsAPI.GetHotelCountersReq{},
			err: "queryData is require",
		},
		{
			req: &hotelsAPI.GetHotelCountersReq{
				QueryData: &hotelsAPI.CountersQueryData{
					CheckinDate:  &date.Date{},
					CheckoutDate: &date.Date{},
					Bbox:         nil,
				},
			},
			err: "bbox is require",
		},
		{
			req: &hotelsAPI.GetHotelCountersReq{
				QueryData: &hotelsAPI.CountersQueryData{
					CheckinDate: nil,
				},
			},
			err: "checkin is require",
		},
		{
			req: &hotelsAPI.GetHotelCountersReq{
				QueryData: &hotelsAPI.CountersQueryData{
					CheckinDate:  &date.Date{},
					CheckoutDate: nil,
				},
			},
			err: "checkout is require",
		},
	}

	for _, tc := range testcases {
		_, err := convertToGetCountersReq(tc.req)

		require.Error(t, err)
		require.True(t, strings.Contains(err.Error(), tc.err))
	}
}

func TestConvertToGetCountersReq_Simple(t *testing.T) {
	testcases := []struct {
		req      *hotelsAPI.GetHotelCountersReq
		expected *models.GetCountersRequest
	}{
		{
			req: &hotelsAPI.GetHotelCountersReq{
				QueryData: &hotelsAPI.CountersQueryData{
					CheckinDate: &date.Date{
						Year:  2022,
						Month: 1,
						Day:   1,
					},
					CheckoutDate: &date.Date{
						Year:  2022,
						Month: 1,
						Day:   2,
					},
					Adults:       1,
					ChildrenAges: []uint32{1},
					Bbox: &hotelsAPI.BoundingBox{
						LeftDown: &hotelsAPI.Coordinates{
							Latitude:  0,
							Longitude: 0,
						},
						UpRight: &hotelsAPI.Coordinates{
							Latitude:  1,
							Longitude: 1,
						},
					},
					FilterAtoms:         nil,
					SearchPagePollingId: "",
					Price: &hotelsAPI.PriceInterval{
						From:     &wrapperspb.UInt32Value{Value: 100},
						To:       &wrapperspb.UInt32Value{Value: 1000},
						Currency: "",
					},
				},
				AnalyticsData: nil,
			},
			expected: &models.GetCountersRequest{
				QueryData: models.GetHotelsCountersMainParams{
					CheckinDate:         "2022-01-01",
					CheckoutDate:        "2022-01-02",
					Adults:              1,
					ChildrenAge:         []int{1},
					SearchPagePollingID: "",
					Atoms:               nil,
					Bbox: &models.BoundingBox{
						Coordinates: []models.Coordinates{
							{
								Latitude:  0,
								Longitude: 0,
							},
							{
								Latitude:  1,
								Longitude: 1,
							},
						},
					},
					PriceFrom: 100,
					PriceTo:   1000,
				},
				AnalyticsParams: nil,
			},
		},
	}

	for _, tc := range testcases {
		res, err := convertToGetCountersReq(tc.req)

		require.NoError(t, err)
		require.Equal(t, tc.expected, res)
	}
}
