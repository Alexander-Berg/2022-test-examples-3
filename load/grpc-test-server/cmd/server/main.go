package main

import (
	"a.yandex-team.ru/load/tests/pandora/grpc-test-server/pkg/adder"
	"a.yandex-team.ru/load/tests/pandora/grpc-test-server/pkg/api"
	"log"
	"net"

	"google.golang.org/grpc"
	"google.golang.org/grpc/reflection"
)

func main() {
	s := grpc.NewServer()
	srv := &adder.GRPCServer{}
	api.RegisterAdderServer(s, srv)

	reflection.Register(s)
	l, err := net.Listen("tcp", ":8080")
	if err != nil {
		log.Fatal(err)
	}

	err = s.Serve(l)
	if err != nil {
		log.Fatal(err)
	}
}
