package core

import (
	"a.yandex-team.ru/load/projects/pandora/components/grpc"
	"a.yandex-team.ru/load/projects/pandora/core/warmup"
	"testing"
)

func TestGrpcGunImplementsWarmedUp(t *testing.T) {
	_ = warmup.WarmedUp(&grpc.Gun{})
}
