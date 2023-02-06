package main

import (
	"encoding/json"
	"fmt"
	"os"
	"runtime/debug"
	"sort"
	"strings"
	"time"

	"a.yandex-team.ru/market/logistics/wms-load/go/model"
	"a.yandex-team.ru/market/logistics/wms-load/go/solomon"
	"a.yandex-team.ru/market/logistics/wms-load/go/sql"
	"a.yandex-team.ru/market/logistics/wms-load/go/util"
	"github.com/yandex/pandora/core"
)

type StopTestsDB struct {
	Target string
	Port   int
	Name   string
}

type StopTestsGunConfig struct {
	DB          StopTestsDB
	SaveStats   bool
	TestTimeout int
	ServiceUrls string
	Config      string
}

type StopTestsGun struct {
	conf          StopTestsGunConfig
	dbCredentials model.DBCredentials
	serviceUrls   map[string]string
	core.GunDeps
}

// StopTestsGunInit создает новую пушку Web IU из переданного конфига.
func StopTestsGunInit(conf StopTestsGunConfig) *StopTestsGun {
	logger := util.NewCtxLogger("STOP-TESTS")

	gun := &StopTestsGun{conf: conf}
	props := util.ParseProps(conf.Config, logger)

	var services map[string]string
	err := json.Unmarshal([]byte(conf.ServiceUrls), &services)
	util.CheckError(err)
	gun.serviceUrls = services

	if gun.conf.SaveStats {
		util.Metrics.Enable()
	}

	gun.dbCredentials = util.ParseDBCred(props, logger)

	util.CheckNotEmpty(gun.dbCredentials.User, "DB user")
	util.CheckNotEmpty(gun.dbCredentials.Password, "DB password")

	sql.ResetDBStateOnce(conf.Config, logger)

	return gun
}

func (gun *StopTestsGun) Shoot(ammo core.Ammo) {
	customAmmo := ammo.(*model.StopTestsAmmo)
	gun.shoot(customAmmo)
}

// shoot проводит стрельбу патроном ammo из пушки StopTests.
func (gun *StopTestsGun) shoot(ammo *model.StopTestsAmmo) {
	logger := util.NewCtxLogger("STOP-TESTS")
	defer func() {
		if r := recover(); r != nil {
			logger.Error("stop-tests failed. Ammo:\n%v\nRecovery data: '%+v'\nError stack:\n%v", ammo, r, string(debug.Stack()))
			os.Exit(1)
		}
	}()

	props := util.ParseProps(gun.conf.Config, logger)
	solomonToken := util.ParseSolomonToken(props, logger)
	statsCredentials := util.ParseStatsDBCred(props, logger)

	sql.CreateDBConnection(gun.conf.Config, logger)

	startedAt := time.Now()

	logger.Log("START", "Starting to monitor for %v orders to reach %v status in %v waves after: %v",
		ammo.TotalOrders, ammo.TargetStatus, ammo.TotalWaves, util.TimeLogFormat(startedAt))

	timeoutMin := gun.conf.TestTimeout
	var deadline time.Time
	if timeoutMin >= 1 {
		timeoutDur, _ := time.ParseDuration(fmt.Sprintf("%dm", timeoutMin))
		deadline = startedAt.Add(timeoutDur)
		logger.Log("START", "Tests deadline set to %v", deadline)
	} else {
		deadline = startedAt.Add(time.Hour * 999999)
	}

	prevOrdersInTargetStatus := 0
	for {
		ordersStatuses := sql.LastWavesOrdersStatuses(ammo.TotalWaves, startedAt)
		ordersMonitored := 0
		ordersInTargetStatus := 0
		msg := logger.Format("DEBUG") + " Orders statuses:\n"
		for _, e := range ordersStatuses {
			msg += fmt.Sprintf("    %v: %v orders", e.Status, e.Count)
			ordersMonitored += e.Count
			if e.Status == ammo.TargetStatus {
				ordersInTargetStatus = e.Count
				msg += fmt.Sprintf(" (+%d)", ordersInTargetStatus-prevOrdersInTargetStatus)
			}
			msg += "\n"
		}
		msg += fmt.Sprintf("\n  Created orders %v/%v\n", ordersMonitored, ammo.TotalOrders)
		fmt.Print(msg)

		logger.Debug("Accumulated stats:\n%v\n", statsToString(util.Metrics.GetStatistics(gun.makeEndpointMapper(), logger)))

		stop := false
		var result string
		if ordersInTargetStatus == ammo.TotalOrders {
			logger.Log("FINISH", "All orders reached target status, stopping")
			result = "OK"
			stop = true
		} else if time.Now().After(deadline) {
			logger.Log("FINISH", "Tests timed out")
			result = "TIMEOUT"
			stop = true
		}

		if stop {
			if gun.conf.SaveStats {
				stats := util.Metrics.GetStatistics(gun.makeEndpointMapper(), logger)
				connParams := sql.CHConnectionParams{
					Addrs:    statsCredentials.Addr,
					Username: statsCredentials.User,
					Password: statsCredentials.Password,
				}
				sql.SaveStats(connParams, startedAt, ammo.TotalWaves, stats, result)

				solomonClient := solomon.NewClient("market_wms", "Load_testing", "load_tester", solomonToken)
				solomonClient.Save(stats)
			}
			os.Exit(0)
		}

		prevOrdersInTargetStatus = ordersInTargetStatus
		util.Sleep(30, 30)
	}
}

func statsToString(stats map[string]map[string]util.Stats) string {
	result := ""
	endpoints := make([]string, 0)
	for endpoint := range stats {
		endpoints = append(endpoints, endpoint)
	}
	sort.Strings(endpoints)

	for _, endpoint := range endpoints {
		outcomesMap := stats[endpoint]

		outcomes := make([]string, 0)
		for outcome := range outcomesMap {
			outcomes = append(outcomes, outcome)
		}
		sort.Strings(outcomes)

		for _, outcome := range outcomes {
			result += fmt.Sprintf("%-70s %-10s %+v\n", endpoint, outcome, outcomesMap[outcome])
		}
	}
	return result
}

func (gun *StopTestsGun) makeEndpointMapper() func(string) string {
	return func(endpoint string) string {
		for serviceName, url := range gun.serviceUrls {
			newEndpoint := strings.Replace(endpoint, url, serviceName+"_", 1)
			if newEndpoint != endpoint {
				return normalizeURL(newEndpoint)
			}
		}
		return endpoint
	}
}

func normalizeURL(u string) string {
	prevEndpoint := u
	for {
		newEndpoint := fmt.Sprintf(prevEndpoint, "X") // Подставляем X вместо первого плейсхолдера
		if strings.Contains(newEndpoint, "%!") {      // останавливаемся, если плейсхолдеров не осталось
			break
		}
		prevEndpoint = newEndpoint
	}
	prevEndpoint = strings.Replace(prevEndpoint, " ", "", -1) // схлопываем пробелы
	return prevEndpoint
}
