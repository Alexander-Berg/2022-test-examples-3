// Copyright (c) 2017 Yandex LLC. All rights reserved.
// Use of this source code is governed by a MPL 2.0
// license that can be found in the LICENSE file.

package main

import (
	"io/ioutil"
	"net/http"
	"regexp"
	"time"

	"a.yandex-team.ru/load/projects/pandora/cli"
	"a.yandex-team.ru/load/projects/pandora/core"
	"a.yandex-team.ru/load/projects/pandora/core/aggregator/netsample"
	coreimport "a.yandex-team.ru/load/projects/pandora/core/import"
	"a.yandex-team.ru/load/projects/pandora/core/register"
)

// Ammo is the structure of the meta information for requests
type Ammo struct {
	Tag string
	BID string
	PID string
	SID string
}

// Payload is the structure of the requests
type Payload struct {
	Method string
	URI    string
	Assert string
	Cotype string
}

//GunConfig is structure for the Gun
type GunConfig struct {
	Target string `validate:"required"` // Configuration will fail, without target defined
}

// Gun is structure for the load generator
type Gun struct {
	// Configured on construction.
	client http.Client
	expire time.Time
	conf   GunConfig
	// Configured on Bind, before shooting
	aggr core.Aggregator // May be your custom Aggregator.
	core.GunDeps
}

// NewGun is the constructor for the loadf generator
func NewGun(conf GunConfig) *Gun {
	return &Gun{conf: conf}
}

// Bind is the configurator for the load generator
func (g *Gun) Bind(aggr core.Aggregator, deps core.GunDeps) error {
	tr := &http.Transport{
		MaxIdleConns:       1,
		IdleConnTimeout:    600 * time.Second,
		DisableCompression: false,
	}
	g.client = http.Client{Transport: tr} //keep-alive shooting
	g.expire = time.Now().AddDate(0, 0, 1)
	g.aggr = aggr
	g.GunDeps = deps
	return nil
}

// Shoot is the launcher for firing
func (g *Gun) Shoot(ammo core.Ammo) {
	customAmmo := ammo.(*Ammo) // Shoot will panic on unexpected ammo type. Panic cancels shooting.
	g.shoot(customAmmo)
}

func (g *Gun) shoot(ammo *Ammo) {
	code := 0
	payload := g.genPayload(ammo)
	req := g.makeReq(payload)
	sample := netsample.Acquire(ammo.Tag)
	rs, err := g.client.Do(req)
	if err != nil {
		code = 0
	} else {
		code = rs.StatusCode
		if code == 200 {

			respBody, _ := ioutil.ReadAll(rs.Body)
			re := regexp.MustCompile(payload.Assert)

			if payload.Assert == "" || len(re.Find(respBody)) == 0 {
				code = rs.StatusCode
			} else {
				code = 314
			}
		}
		_ = rs.Body.Close()
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
	// May not be imported, if you don't need http guns and etc.

	// Custom imports. Integrate your custom types into configuration system.
	coreimport.RegisterCustomJSONProvider("meduza_provider", func() core.Ammo { return &Ammo{} })

	register.Gun("meduza", NewGun, func() GunConfig {
		return GunConfig{
			Target: "default target",
		}
	})

	cli.Run()
}
