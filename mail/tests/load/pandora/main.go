package main

import (
	"io/ioutil"
	"net/http"
	"strings"
	"time"

	"github.com/yandex/pandora/cli"
	"github.com/yandex/pandora/core"
	"github.com/yandex/pandora/core/aggregator/netsample"
	coreimport "github.com/yandex/pandora/core/import"
	"github.com/yandex/pandora/core/register"
)

type Ammo struct {
	Tag   string
	Delay int
}

type Sample struct {
	URL              string
	ShootTimeSeconds float64
}

type GunConfig struct {
	Target   string `validate:"required"` // Configuration will fail, without target defined
	Callback string `validate:"required"` // Configuration will fail, without target defined
}

type Gun struct {
	// Configured on construction.
	client http.Client
	conf   GunConfig
	// Configured on Bind, before shooting
	aggr core.Aggregator // May be your custom Aggregator.
	core.GunDeps
}

func httpGet(delay int, target string, callback string) *http.Request {
	remTime := time.Now().Local().Add(time.Second * time.Duration(delay)).Format("2006-01-02T15:04:05.123Z")
	req, _ := http.NewRequest("POST", target, strings.NewReader(`{"run_at": "`+remTime+`", "target": "`+callback+`", "context": {"message": "you are fool"}}`))
	req.Header.Add("Content-Type", "application/x-www-form-urlencoded")
	req.Header.Add("x-real-ip", "127.0.0.1")
	req.Header.Add("User-Agent", "Pandora")
	req.Header.Add("Host", "callmeback.mail.yandex.net")
	return req
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
	req := httpGet(ammo.Delay, g.conf.Target, g.conf.Callback)
	sample := netsample.Acquire(ammo.Tag)
	rs, err := g.client.Do(req)
	if err != nil {
		code = 0
		//fmt.Println(err)
	} else {
		code = rs.StatusCode
		if code == 200 {
			_, err := ioutil.ReadAll(rs.Body)
			if err != nil {
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

	// Custom imports. Integrate your custom types into configuration system.
	coreimport.RegisterCustomJSONProvider("callmeback_provider", func() core.Ammo { return &Ammo{} })

	register.Gun("callmeback", NewGun, func() GunConfig {
		return GunConfig{
			Target:   "default target",
			Callback: "default callback",
		}
	})

	cli.Run()
}
