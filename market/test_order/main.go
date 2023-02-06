package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	"os/signal"
	"runtime"
	"strconv"
	"strings"
	"syscall"
	"test_order/pkg/abo"
	"test_order/pkg/wiki"
	"time"

	"github.com/robfig/cron/v3"
)

const (
	STARTREK_URL = "https://st-api.yandex-team.ru/v2"
)

func GetStartrekTickets(query string, token string) (map[int]map[string]string, error) {
	req, _ := http.NewRequest("GET", fmt.Sprintf("%s/issues", STARTREK_URL), nil)

	if !strings.HasPrefix(token, "OAuth") {
		req.Header.Add("Authorization", fmt.Sprintf("OAuth %s", token))
	} else {
		req.Header.Add("Authorization", token)
	}

	urlQuery := req.URL.Query()
	urlQuery.Add("query", query)
	urlQuery.Add("perPage", "1000")
	req.URL.RawQuery = urlQuery.Encode()

	resp, _ := http.DefaultClient.Do(req)
	tickets := make(map[int]map[string]string)

	if resp.StatusCode == http.StatusOK {
		bodyBytes, _ := ioutil.ReadAll(resp.Body)
		var issues []map[string]interface{}
		_ = json.Unmarshal([]byte(bodyBytes), &issues)

		for _, issue := range issues {
			orders := ""
			if issue["customerOrderNumber"] != nil {
				orders = issue["customerOrderNumber"].(string)
			}
			assignee := "alc0valeva"
			if issue["assignee"] != nil {
				assignee = issue["assignee"].(map[string]interface{})["id"].(string)
			}

			if issue["idMagazina"] != nil {
				tickets[int(issue["idMagazina"].(float64))] = map[string]string{
					"key": issue["key"].(string), "orders": orders, "assignee": assignee,
				}
			}
		}
		return tickets, nil
	}
	return tickets, fmt.Errorf("empty issues list, resp code: %d", resp.StatusCode)
}

func CreateIssue(issue Issue, token string) {
	bytesRep, _ := json.Marshal(issue)
	req, _ := http.NewRequest("POST", fmt.Sprintf("%s/issues", STARTREK_URL), bytes.NewBuffer(bytesRep))

	if !strings.HasPrefix(token, "OAuth") {
		req.Header.Add("Authorization", fmt.Sprintf("OAuth %s", token))
	} else {
		req.Header.Add("Authorization", token)
	}

	client := &http.Client{}
	resp, _ := client.Do(req)

	if resp.StatusCode != http.StatusOK && resp.StatusCode != 201 {
		log.Fatalln(resp.Status)
	}
}

type Issue struct {
	Queue       string `json:"queue"`
	Summary     string `json:"summary"`
	Description string `json:"description"`
	IdMagazina  int    `json:"idMagazina"`
	BusinessIds string `json:"businessIds"`
	Components  []int  `json:"components"`
	Type        int    `json:"type"`
}

func RecheckTicketCaseHandler() {
	token := os.Getenv("ST_TOKEN")
	grid, err := wiki.ReadGrid("users/hitinap/test-orders-for-major-partners", token)
	CheckError(err)

	businessIDs := make([]string, 0)
	for _, row := range grid {
		businessIDs = append(businessIDs, row["BussinesID"].(string))
	}

	recheck := abo.GetRecheckTickets(businessIDs, "aboreader", os.Getenv("ABO_PASSWORD"))
	tickets, err := GetStartrekTickets(
		fmt.Sprintf(
			"Queue: BERUPREMOD Components: 113593 Created: >=\"%s\"",
			time.Now().Format("2006-01-02"),
		),
		token,
	)
	CheckError(err)

	for _, ticket := range recheck {
		if _, ok := tickets[ticket.ShopID]; !ok {
			summary := fmt.Sprintf("Тестирование заказа после премодерации (ID: %d)", ticket.ShopID)
			description := fmt.Sprintf(
				`Тикет проверки ассортимента: https://abo.market.yandex-team.ru/recheck/lite/%d
				Страница партнера в Або: https://abo.market.yandex-team.ru/partner/%d

				Все предложения магазина: http://market.yandex.ru/search?fesh=%d&free=1&not-collapse=1
				Ссылка на Единое окно: https://ow.market.yandex-team.ru/
				Флаги: %%%%&rearr-factors=market_blue_ignore_supplier_filter=1%%%%
				`, ticket.TicketID, ticket.ShopID, ticket.ShopID,
			)

			CreateIssue(
				Issue{
					Queue:       "BERUPREMOD",
					Summary:     summary,
					Description: description,
					IdMagazina:  ticket.ShopID,
					BusinessIds: strconv.Itoa(ticket.BusinessID),
					Components:  []int{113593},
					Type:        2,
				},
				token,
			)
		}
	}
}

func main() {
	runtime.GOMAXPROCS(2)
	moscowTime, _ := time.LoadLocation("Europe/Moscow")
	scheduler := cron.New(cron.WithLocation(moscowTime))

	scheduler.AddFunc("*/15 * * * *", RecheckTicketCaseHandler)
	scheduler.AddFunc("*/5 * * * *", StatusTestOrderHandler)
	go scheduler.Start()

	sig := make(chan os.Signal, 1)
	signal.Notify(sig, syscall.SIGINT, syscall.SIGTERM)
	<-sig
}
