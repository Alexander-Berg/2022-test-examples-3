package protox

import (
	"testing"

	"github.com/stretchr/testify/require"
	"google.golang.org/protobuf/proto"

	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

func TestReduceDeliveryOptions(t *testing.T) {
	resp := &pb.DeliveryOptionsForUser{
		Options: []*pb.DeliveryOption{
			&pb.DeliveryOption{
				Cost: 100500,
				Customizers: []*pb.DeliveryOption_Customizer{
					&pb.DeliveryOption_Customizer{
						Key:  "key",
						Name: "name",
					},
				},
			},
		},
	}

	orig1, _ := proto.Marshal(resp)

	clone := proto.Clone(resp).(*pb.DeliveryOptionsForUser)
	reduceDeliveryOptions(clone)
	copy1, _ := proto.Marshal(clone)

	restore := ReduceDeliveryOptions(resp)
	copy2, _ := proto.Marshal(resp)

	restore()
	orig2, _ := proto.Marshal(resp)

	require.Equal(t, orig1, orig2)
	require.Equal(t, copy1, copy2)
	require.Len(t, orig1, 19)
	require.Len(t, copy1, 6)
}

func TestReduceGetPickupPointsGroupedReq(t *testing.T) {
	req := &pb.PickupPointsRequest{
		DestinationRegions: []uint32{1, 2, 3, 4, 5, 6, 7, 8, 9, 10},
	}

	orig1, _ := proto.Marshal(req)

	clone := proto.Clone(req).(*pb.PickupPointsRequest)
	ReduceGetPickupPointsGroupedReq(clone)
	copy1, _ := proto.Marshal(clone)

	restore := ReduceGetPickupPointsGroupedReq(req)
	copy2, _ := proto.Marshal(req)

	restore()
	orig2, _ := proto.Marshal(req)

	require.Equal(t, orig1, orig2)
	require.Equal(t, copy1, copy2)
	require.Len(t, orig1, 12)
	require.Len(t, copy1, 7)
}
