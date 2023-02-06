package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"
	"strconv"
	"strings"
	"test_order/pkg/telegram"
)

func fetchTicketOrders(ticketOrders string) map[string]string {
	orders := make(map[string]string, 0)
	for _, orderId := range strings.Split(ticketOrders, ", ") {
		if orderId != "" {
			orders[orderId] = orderId
		}
	}
	return orders
}

func ordersMapToString(orders map[string]string) string {
	ordersList := make([]string, 0)
	for orderId := range orders {
		ordersList = append(ordersList, orderId)
	}

	return strings.Join(ordersList[:], ", ")
}

type Comment struct {
	Text      string   `json:"text"`
	Summonees []string `json:"summonees"`
}

func CreateComment(key string, order CheckouterOrder, assignee string, token string) {
	comment := fmt.Sprintf("[%s] Заказ №%d получил статус READY_TO_SHIP, тестовый заказ нужно отменить.",
		order.EventDate, order.OrderID)

	bytesRep, _ := json.Marshal(Comment{comment, []string{assignee}})
	req, _ := http.NewRequest(
		"POST", fmt.Sprintf("%s/issues/%s/comments", STARTREK_URL, key), bytes.NewBuffer(bytesRep),
	)

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

type IssueOrders struct {
	CustomerOrderNumber string `json:"customerOrderNumber"`
}

func UpdateIssue(key string, orders string, token string) {
	bytesRep, _ := json.Marshal(IssueOrders{orders})
	req, _ := http.NewRequest(
		"PATCH", fmt.Sprintf("%s/issues/%s", STARTREK_URL, key), bytes.NewBuffer(bytesRep),
	)

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

func StatusTestOrderHandler() {
	token := os.Getenv("ST_TOKEN")
	tickets, err := GetStartrekTickets(
		"Queue: BERUPREMOD Components: 113593 Resolution: empty()", token,
	)
	CheckError(err)

	shopIds := make([]string, 0)
	for shopID := range tickets {
		shopIds = append(shopIds, strconv.Itoa(shopID))
	}

	if len(shopIds) > 0 {
		user := os.Getenv("PG_USER")
		password := os.Getenv("PG_PASSWORD")
		checkouterOrders := GetCheckouterOrders(shopIds, user, password)

		for _, order := range checkouterOrders {
			orders := fetchTicketOrders(tickets[order.ShopID]["orders"])
			if _, ok := orders[strconv.Itoa(order.OrderID)]; !ok {
				ticketKey := tickets[order.ShopID]["key"]
				assignee := tickets[order.ShopID]["assignee"]

				go telegram.SendNotification(order.OrderID, ticketKey, assignee)
				CreateComment(ticketKey, order, assignee, token)

				orders[strconv.Itoa(order.OrderID)] = strconv.Itoa(order.OrderID)
				ordersString := ordersMapToString(orders)
				UpdateIssue(ticketKey, ordersString, token)
			}
		}
	}
}
