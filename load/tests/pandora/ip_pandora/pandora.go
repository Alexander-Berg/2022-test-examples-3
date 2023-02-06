package main

import (
	"fmt"
	"io/ioutil"
	"math/rand"
	"net"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/yandex/pandora/cli"
	"github.com/yandex/pandora/core"
	"github.com/yandex/pandora/core/aggregator/netsample"
	coreimport "github.com/yandex/pandora/core/import"
	"github.com/yandex/pandora/core/register"
)

type Ammo struct {
	Tag        string
	URLPart    string
	HostHeader string
}

type GunConfig struct {
	Target  string `validate:"required"`
	Ips     []string
	Verbose bool
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
	var tr *http.Transport
	if g.conf.Ips != nil {
		rand.Seed(time.Now().UnixNano())
		localAddr, err := net.ResolveIPAddr("ip", g.conf.Ips[rand.Intn(len(g.conf.Ips))])
		if err != nil {
			panic(err)
		}

		localTCPAddr := net.TCPAddr{
			IP: localAddr.IP,
		}

		d := net.Dialer{
			LocalAddr: &localTCPAddr,
			Timeout:   30 * time.Second,
			KeepAlive: 30 * time.Second,
		}

		tr = &http.Transport{
			Dial:               d.Dial,
			MaxIdleConns:       1,
			IdleConnTimeout:    time.Duration(60) * time.Second,
			DisableCompression: true,
		}
	} else {
		tr = &http.Transport{
			MaxIdleConns:       1,
			IdleConnTimeout:    time.Duration(10) * time.Second,
			DisableCompression: true,
		}
	}
	g.client = http.Client{Transport: tr}
	g.aggr = aggr
	g.GunDeps = deps
	return nil
}

func (g *Gun) Shoot(ammo core.Ammo) {
	customAmmo := ammo.(*Ammo)
	g.shoot(customAmmo)
}

func (g *Gun) shoot(ammo *Ammo) {
	code := 0
	SleepTime := 10 + rand.Intn(100)
	req, _ := http.NewRequest("GET", strings.Join([]string{"http://", g.conf.Target, ammo.Tag, strconv.Itoa(SleepTime), ammo.URLPart}, ""), nil)
	req.Header.Add("Connection", "keep-alive")
	req.Host = ammo.HostHeader

	sample := netsample.Acquire(ammo.Tag)
	rs, err := g.client.Do(req)
	if err == nil {
		code = rs.StatusCode
		respBody, _ := ioutil.ReadAll(rs.Body)
		_ = rs.Body.Close()
		if g.conf.Verbose {
			fmt.Println(string(respBody))
		}
	} else {
		fmt.Println(err)
		sample.SetErr(err)
	}

	defer func() {
		sample.SetProtoCode(code)
		g.aggr.Report(sample)
	}()
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
