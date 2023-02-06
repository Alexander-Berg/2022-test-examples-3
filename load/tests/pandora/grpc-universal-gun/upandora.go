package main

import (
	"context"
	"encoding/json"
	"log"
	"time"

	"a.yandex-team.ru/load/projects/pandora/cli"
	"a.yandex-team.ru/load/projects/pandora/core"
	"a.yandex-team.ru/load/projects/pandora/core/aggregator/netsample"
	coreimport "a.yandex-team.ru/load/projects/pandora/core/import"
	"a.yandex-team.ru/load/projects/pandora/core/register"
	"github.com/jhump/protoreflect/desc"
	"github.com/jhump/protoreflect/dynamic"
	"github.com/jhump/protoreflect/dynamic/grpcdynamic"
	"github.com/jhump/protoreflect/grpcreflect"
	"github.com/spf13/afero"
	"google.golang.org/grpc"
	"google.golang.org/grpc/metadata"
	reflectpb "google.golang.org/grpc/reflection/grpc_reflection_v1alpha"
)

type Ammo struct {
	Tag      string                 `json:"tag"`
	Call     string                 `json:"call"`
	Metadata map[string]string      `json:"metadata"`
	Payload  map[string]interface{} `json:"payload"`
}

type Sample struct {
	URL              string
	ShootTimeSeconds float64
}

type GunConfig struct {
	Target string `validate:"required"`
}

type Gun struct {
	client *grpc.ClientConn
	conf   GunConfig
	aggr   core.Aggregator
	core.GunDeps

	stub     grpcdynamic.Stub
	services map[string]desc.MethodDescriptor
}

func NewGun(conf GunConfig) *Gun {
	return &Gun{conf: conf}
}

func (g *Gun) Bind(aggr core.Aggregator, deps core.GunDeps) error {
	conn, err := grpc.Dial(
		g.conf.Target,
		grpc.WithInsecure(),
		grpc.WithTimeout(time.Second),
		grpc.WithUserAgent("load test, pandora universal grpc shooter"))
	if err != nil {
		log.Fatalf("FATAL: grpc.Dial failed\n %s\n", err)
	}
	g.client = conn
	g.aggr = aggr
	g.GunDeps = deps
	g.stub = grpcdynamic.NewStub(conn)

	meta := make(metadata.MD)
	refCtx := metadata.NewOutgoingContext(context.Background(), meta)
	refClient := grpcreflect.NewClient(refCtx, reflectpb.NewServerReflectionClient(conn))
	listServices, err := refClient.ListServices()
	if err != nil {
		log.Fatalf("Fatal: failed to get services list\n %s\n", err)
	}
	g.services = make(map[string]desc.MethodDescriptor)
	for _, s := range listServices {
		service, err := refClient.ResolveService(s)
		if err != nil {
			log.Fatalf("FATAL ResolveService: %s", err)
		}
		listMethods := service.GetMethods()
		for _, m := range listMethods {
			g.services[m.GetFullyQualifiedName()] = *m
		}
	}

	return nil
}

func (g *Gun) Shoot(ammo core.Ammo) {
	customAmmo := ammo.(*Ammo)
	g.shoot(customAmmo)
}

func (g *Gun) shoot(ammo *Ammo) {

	code := 0
	sample := netsample.Acquire(ammo.Tag)
	defer func() {
		sample.SetProtoCode(code)
		g.aggr.Report(sample)
	}()

	method, ok := g.services[ammo.Call]
	if !ok {
		log.Fatalf("Fatal: No such method %s\n", ammo.Call)
		return
	}

	log.Printf("MethodDescriptor: %s", method.GetInputType())
	payloadJSON, err := json.Marshal(ammo.Payload)
	if err != nil {
		log.Fatalf("FATAL: Payload parsing error %s\n", err)
		return
	}

	md := method.GetInputType()
	message := dynamic.NewMessage(md)
	err = message.UnmarshalJSON(payloadJSON)
	if err != nil {
		code = 400
		log.Printf("BAD REQUEST: %s", err)
		return
	}

	meta := make(metadata.MD)
	if ammo.Metadata != nil && len(ammo.Metadata) > 0 {
		for key, value := range ammo.Metadata {
			meta = metadata.Pairs(key, value)
		}
	}

	ctx := metadata.NewOutgoingContext(context.Background(), meta)

	out, err := g.stub.InvokeRpc(ctx, &method, message)
	if err != nil {
		code = 0
		log.Printf("BAD REQUEST: %s\n", err)
	}
	if out != nil {
		code = 200
	} else {
		code = 400
	}

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
