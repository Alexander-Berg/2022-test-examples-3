package abo

import (
	"database/sql"
	"fmt"
	"io/ioutil"
	"log"
	"strings"

	_ "github.com/lib/pq"
)

const (
	host                 = "abo-market01f.db.yandex.net"
	port                 = 6432
	dbname               = "abodb"
	target_session_attrs = "read"
	sslmode              = "require"
)

func CheckError(err error) {
	if err != nil {
		log.Fatalln(err)
	}
}

type RecheckTicket struct {
	TicketID     int    `json:"ticket_id"`
	ShopID       int    `json:"shop_id"`
	BusinessID   int    `json:"business_id"`
	CreationTime string `json:"creation_time"`
}

func GetRecheckTickets(businessIDs []string, user string, password string) []RecheckTicket {
	fileContent, _ := ioutil.ReadFile("resources/recheck.sql")
	query := string(fileContent)

	connStr := fmt.Sprintf("host=%s port=%d dbname=%s user=%s password=%s target_session_attrs=%s sslmode=%s",
		host, port, dbname, user, password, target_session_attrs, sslmode)
	conn, err := sql.Open("postgres", connStr)
	CheckError(err)
	defer conn.Close()

	query = fmt.Sprintf(query, strings.Join(businessIDs[:], ","))
	rows, err := conn.Query(query)
	CheckError(err)
	defer rows.Close()

	tickets := []RecheckTicket{}
	for rows.Next() {
		ticket := RecheckTicket{}
		err := rows.Scan(&ticket.TicketID,
			&ticket.ShopID,
			&ticket.BusinessID,
			&ticket.CreationTime)
		CheckError(err)
		tickets = append(tickets, ticket)
	}

	return tickets
}
