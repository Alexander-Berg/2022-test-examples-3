package adder

import (
	//GRPCServer ...
	"a.yandex-team.ru/load/tests/pandora/grpc-test-server/pkg/api"

	"context"
)

type GRPCServer struct{}

func (s *GRPCServer) Add(ctx context.Context, req *api.AddRequest) (*api.AddResponse, error) {
	return &api.AddResponse{Result: req.GetX() + req.GetY()}, nil
}
