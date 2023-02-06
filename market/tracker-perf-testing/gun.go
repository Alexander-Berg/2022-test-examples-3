package main

import (
	"fmt"
	"io/ioutil"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"time"

	"github.com/yandex/pandora/cli"
	"github.com/yandex/pandora/core"
	"github.com/yandex/pandora/core/aggregator/netsample"
	"github.com/yandex/pandora/core/import"
	"github.com/yandex/pandora/core/register"
)

type Ammo struct {
	Tag               string
	DeliveryServiceID int
	StatusDelay       int
	HistoryDelay      int
	TrackType         string
	APIVersion        int
	NextRequestDate   string
}

type GunConfig struct {
	Target       string `validate:"required"` // Configuration will fail, without target defined
	FastSlowRate float64
	BaseOrderID  int
}

type Gun struct {
	client http.Client
	conf   GunConfig
	aggr   core.Aggregator
	core.GunDeps
}

func NewGun(conf GunConfig) *Gun {
	return &Gun{conf: conf}
}

func (g *Gun) Bind(aggr core.Aggregator, deps core.GunDeps) error {
	tr := &http.Transport{
		MaxIdleConns:       1,
		IdleConnTimeout:    0,
		DisableCompression: true,
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
	conf := &g.conf
	startTS := time.Now().Format(time.RFC3339)
	trackCode := strings.Join([]string{ // trackCode=A_2009-11-10T23%3A00%3A00Z_123_321_97_1
		"trackCode=" + ammo.TrackType,   // trackCode=[A|B] -> trackCode=A
		url.QueryEscape(startTS),        // 2009-11-10T23%3A00%3A00Z
		strconv.Itoa(ammo.StatusDelay),  // 123
		strconv.Itoa(ammo.HistoryDelay), // 321
		strconv.Itoa(g.InstanceID),      // 97
	}, "_")
	orderID := strings.Join([]string{
		"orderId=perf",
		strconv.Itoa(conf.BaseOrderID),
		strconv.Itoa(g.InstanceID),
	}, "_")

	dsID := "deliveryServiceId=" + strconv.Itoa(ammo.DeliveryServiceID)

	apiVersion := "ApiVersion=" + strconv.Itoa(ammo.APIVersion)

	queryParams := []string{trackCode, dsID, "consumerId=3", orderID, apiVersion}

	if ammo.NextRequestDate != "" {
		queryParams = append(queryParams, "nextRequestDate="+url.QueryEscape(ammo.NextRequestDate))
	}

	queryParamsStr := strings.Join(queryParams, "&")

	destURL := strings.Join([]string{"http://", conf.Target, "/track?", queryParamsStr}, "")

	// формируем запрос, параметризуем его
	code := 0 // выставляем код ответа по умолчанию
	req, _ := http.NewRequest("PUT", destURL, nil)
	req.Header.Add("Connection", "keep-alive")

	sample := netsample.Acquire(ammo.Tag) // стартуем замер. Это нужно, чтобы в отчёте можно было увидеть, сколько времени выполнялось действие.
	rs, err := g.client.Do(req)           // делаем сам запрос
	if err == nil {
		// обрабатываем результат. "Принты" уйдут в лог пандоры, если нужно дебажить ответы, можно их после стрельбы обработать
		code = rs.StatusCode
		respBody, _ := ioutil.ReadAll(rs.Body)
		_ = rs.Body.Close()
		fmt.Println(string(respBody))
	} else {
		fmt.Println(err)
	}
	// после всего закрываем замер и отправляем результат замера танку для дальнейшей агрегации
	// можно делать вложенные замеры, или несколько в одном shoot(). В этом случае rps на графиках будет отличаться от заданного расписания
	defer func() {
		sample.SetProtoCode(code)
		g.aggr.Report(sample)
	}()
}

func main() {
	fs := coreimport.GetFs()
	coreimport.Import(fs)
	// регистрируем провайдера патронов
	coreimport.RegisterCustomJSONProvider("delivery_tracker_ammo_provider", func() core.Ammo {
		return &Ammo{}
	})
	// регистрируем Gun
	register.Gun("delivery_tracker_gun", NewGun, func() GunConfig {
		return GunConfig{
			Target: "default target",
		}
	})
	cli.Run()
}
