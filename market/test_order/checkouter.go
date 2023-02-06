package main

import (
	"database/sql"
	"fmt"
	"io/ioutil"
	"log"
	"strings"

	_ "github.com/lib/pq"
)

const (
	host                 = "vla-ds7cmyz7xfdqie9t.db.yandex.net"
	port                 = 6432
	dbname               = "market_checkouter_prod"
	target_session_attrs = "read"
	sslmode              = "require"
)

func CheckError(err error) {
	if err != nil {
		log.Fatalln(err)
	}
}

type CheckouterOrder struct {
	OrderID   int    `json:"order_id"`
	EventDate string `json:"event_date"`
	ShopID    int    `json:"shop_id"`
}

func GetCheckouterOrders(shopIds []string, user string, password string) []CheckouterOrder {
	fileContent, _ := ioutil.ReadFile("resources/history.sql")
	query := string(fileContent)

	connStr := fmt.Sprintf("host=%s port=%d dbname=%s user=%s password=%s target_session_attrs=%s sslmode=%s",
		host, port, dbname, user, password, target_session_attrs, sslmode)
	conn, err := sql.Open("postgres", connStr)
	CheckError(err)
	defer conn.Close()

	query = fmt.Sprintf(query, strings.Join(shopIds[:], ","))
	rows, err := conn.Query(query)
	CheckError(err)
	defer rows.Close()

	orders := []CheckouterOrder{}
	for rows.Next() {
		order := CheckouterOrder{}
		err := rows.Scan(&order.OrderID,
			&order.EventDate,
			&order.ShopID)
		CheckError(err)
		orders = append(orders, order)
	}

	return orders
}
