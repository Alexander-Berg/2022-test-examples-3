package main

import (
	"net/http"
	"time"

	"github.com/yandex/pandora/cli"
	"github.com/yandex/pandora/core"
	"github.com/yandex/pandora/core/aggregator/netsample"
	"github.com/yandex/pandora/core/import"
	"github.com/yandex/pandora/core/register"
)

type Sample struct {
	URL              string
	ShootTimeSeconds float64
}

type GunConfig struct {
	Target string `validate:"required"` // Configuration will fail, without target defined
	Param  string
}

type Gun struct {
	// Configured on construction.
	client http.Client
	conf   GunConfig
	// Configured on Bind, before shooting
	aggr core.Aggregator // May be your custom Aggregator.
	core.GunDeps
}

func NewGun(conf GunConfig) *Gun {
	return &Gun{conf: conf}
}

func (g *Gun) Bind(aggr core.Aggregator, deps core.GunDeps) error {
	tr := &http.Transport{
		MaxIdleConns:       1,
		IdleConnTimeout:    600 * time.Second,
		DisableCompression: true,
	}
	g.client = http.Client{Transport: tr} //keep-alive shooting
	g.aggr = aggr
	g.GunDeps = deps
	return nil
}

func (g *Gun) Shoot(ammo core.Ammo) {
	customAmmo := ammo.(*Ammo) // Shoot will panic on unexpected ammo type. Panic cancels shooting.
	g.shoot(customAmmo)
}

func (g *Gun) shoot(ammo *Ammo) {
	code := 0
	req := g.makeReq(ammo)
	sample := netsample.Acquire(ammo.Tag)

	rs, err := g.client.Do(req)
	if err != nil {
		code = 0
	} else {
		code = rs.StatusCode
		rs.Body.Close()
	}
	defer func() {
		sample.SetProtoCode(code)
		g.aggr.Report(sample)
	}()
}

func main() {
	// Standard imports.
	fs := coreimport.GetFs()
	coreimport.Import(fs)

	// Custom imports. Integrate your custom types into configuration system.
	coreimport.RegisterCustomJSONProvider("test_provider", func() core.Ammo { return &Ammo{} })

	register.Gun("test_gun", NewGun, func() GunConfig {
		return GunConfig{
			Target: "default target",
		}
	})

	cli.Run()
}
