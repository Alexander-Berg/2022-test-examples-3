package app

import (
	"context"

	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"

	tcpb "a.yandex-team.ru/travel/buses/backend/proto/testcontext"
)

func (a *App) SetBookParams(
	_ context.Context, request *tcpb.TTestContextSetBookParamsRequest,
) (*tcpb.TTestContextSetResponse, error) {
	token, err := a.testContextStorage.Set(request.GetTestContextToken(), request.GetPayload())
	if err != nil {
		return nil, err
	}
	return &tcpb.TTestContextSetResponse{TestContextToken: token}, err
}

func (a *App) GetBookParams(
	_ context.Context, request *tcpb.TTestContextGetRequest,
) (*tcpb.TTestContextGetBookParamsResponse, error) {
	payload := &tcpb.TTestContextBookParamsPayload{}
	ok, err := a.testContextStorage.Get(request.GetTestContextToken(), payload)
	if !ok {
		return &tcpb.TTestContextGetBookParamsResponse{}, status.Errorf(
			codes.NotFound, "token %s not found", request.GetTestContextToken())
	}
	if err != nil {
		return &tcpb.TTestContextGetBookParamsResponse{}, status.Errorf(codes.Internal, err.Error())
	}
	return &tcpb.TTestContextGetBookParamsResponse{Payload: payload}, nil
}
