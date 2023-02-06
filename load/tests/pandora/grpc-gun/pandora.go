package main

import (
	"context"
	"fmt"
	"log"
	"time"

	"a.yandex-team.ru/load/projects/pandora/cli"
	"a.yandex-team.ru/load/projects/pandora/core"
	"a.yandex-team.ru/load/projects/pandora/core/aggregator/netsample"
	coreimport "a.yandex-team.ru/load/projects/pandora/core/import"
	"a.yandex-team.ru/load/projects/pandora/core/register"
	"github.com/spf13/afero"
	"google.golang.org/grpc"

	pb "a.yandex-team.ru/load/tests/pandora/grpc-test-server/pkg/api"
)

type Ammo struct {
	Tag string
	X   int32
	Y   int32
}

type Sample struct {
	URL              string
	ShootTimeSeconds float64
}

type GunConfig struct {
	Target string `validate:"required"`
}

type Gun struct {
	client pb.AdderClient
	conf   GunConfig
	aggr   core.Aggregator
	core.GunDeps
}

func NewGun(conf GunConfig) *Gun {
	return &Gun{conf: conf}
}

func (g *Gun) Bind(aggr core.Aggregator, deps core.GunDeps) error {
	conn, err := grpc.Dial(
		g.conf.Target,
		grpc.WithInsecure(),
		//nolint:SA1019
		grpc.WithTimeout(time.Second),
		grpc.WithUserAgent("load test, pandora custom shooter"))
	if err != nil {
		log.Fatalf("FATAL: %s", err)
	}
	g.client = pb.NewAdderClient(conn)
	g.aggr = aggr
	g.GunDeps = deps
	return nil
}

func (g *Gun) Shoot(ammo core.Ammo) {
	customAmmo := ammo.(*Ammo)
	g.shoot(customAmmo)
}

func (g *Gun) Add(client pb.AdderClient, ammo *Ammo) int {
	code := 0

	request := pb.AddRequest{
		X: ammo.X,
		Y: ammo.Y,
	}
	out, err := client.Add(context.TODO(), &request)

	if err != nil {
		fmt.Printf("FATAL: %s\n", err)
		code = 500
	}

	if out != nil {
		code = 200
	}
	return code
}

func (g *Gun) shoot(ammo *Ammo) {
	code := 0
	sample := netsample.Acquire(ammo.Tag)

	switch ammo.Tag {
	case "/api.Adder/Add":
		code = g.Add(g.client, ammo)
	default:
		code = 404
	}

	defer func() {
		sample.SetProtoCode(code)
		g.aggr.Report(sample)
	}()
}

func main() {
	fs := afero.NewOsFs()
	coreimport.Import(fs)

	coreimport.RegisterCustomJSONProvider("custom_provider", func() core.Ammo { return &Ammo{} })

	register.Gun("grpc_gun", NewGun, func() GunConfig {
		return GunConfig{
			Target: "default target",
		}
	})

	cli.Run()
}
