package main

import (
	"fmt"
	"io/ioutil"
	"math/rand"
	"net/http"
	"strconv"
	"strings"
	"time"

	"a.yandex-team.ru/load/projects/pandora/cli"
	"a.yandex-team.ru/load/projects/pandora/core"
	"a.yandex-team.ru/load/projects/pandora/core/aggregator/netsample"
	coreimport "a.yandex-team.ru/load/projects/pandora/core/import"
	"a.yandex-team.ru/load/projects/pandora/core/register"
)

type Ammo struct {
	Tag        string
	URL        string
	HostHeader string
}

type GunConfig struct {
	Target          string `validate:"required"`
	IdleConnTimeout int    `config:"idle-conn-timeout"`
	ClientTimeout   int    `config:"client-timeout"`
}

type Gun struct {
	client http.Client
	conf   GunConfig
	aggr   core.Aggregator
	core.GunDeps
}

func ExampleGun(conf GunConfig) *Gun {
	return &Gun{conf: conf}
}

func (g *Gun) Bind(aggr core.Aggregator, deps core.GunDeps) error {
	tr := &http.Transport{
		MaxIdleConns:       1,
		IdleConnTimeout:    time.Duration(g.conf.IdleConnTimeout) * time.Second,
		DisableCompression: true,
	}
	g.client = http.Client{
		Transport: tr,
		Timeout:   time.Duration(g.conf.ClientTimeout) * time.Second,
	}
	g.aggr = aggr
	g.GunDeps = deps
	return nil
}

func (g *Gun) Shoot(ammo core.Ammo) {
	customAmmo := ammo.(*Ammo)
	g.shoot(customAmmo)
}

func (g *Gun) shoot(ammo *Ammo) {
	SleepTime := 100 + rand.Intn(12000)
	req, _ := http.NewRequest("GET", strings.Join([]string{"http://", g.conf.Target, ammo.URL, strconv.Itoa(SleepTime)}, ""), nil)
	req.Header.Add("Connection", "keep-alive")
	req.Header.Add("Host", ammo.HostHeader)

	for i := 0; i < 3; i++ {
		code := 0
		sample := netsample.Acquire(ammo.Tag)
		rs, err := g.client.Do(req)
		if err == nil {
			code = rs.StatusCode
			respBody, _ := ioutil.ReadAll(rs.Body)
			_ = rs.Body.Close()
			fmt.Println(string(respBody))
		} else {
			sample.SetErr(err)
		}
		sample.SetProtoCode(code)
		g.aggr.Report(sample)
	}
}

func main() {
	fs := coreimport.GetFs()
	coreimport.Import(fs)
	coreimport.RegisterCustomJSONProvider("example_provider", func() core.Ammo { return &Ammo{} })
	register.Gun("example", ExampleGun, func() GunConfig {
		return GunConfig{
			Target: "default target",
		}
	})
	cli.Run()
}
