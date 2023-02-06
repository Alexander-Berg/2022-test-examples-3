package testing

import (
	"a.yandex-team.ru/travel/library/go/grpcgateway"
)

type Config struct {
	Enabled     bool `yaml:"enabled"`
	GrpcAddr    string
	GrpcGateway grpcgateway.Config `yaml:"grpc_gateway"`
}

var DefaultConfig = Config{
	Enabled:  false,
	GrpcAddr: "[::]:9002",
	GrpcGateway: grpcgateway.Config{
		Enabled:       true,
		Address:       "[::]:80",
		EnableBinary:  true,
		EnableSwagger: true,
	},
}
