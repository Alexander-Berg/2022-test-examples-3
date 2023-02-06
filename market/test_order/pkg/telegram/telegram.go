package telegram

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	"strings"
)

const (
	STAFF_API_URL    = "https://staff-api.yandex-team.ru/v3/persons?_one=1&login=%s&_fields=accounts"
	TELEGRAM_API_URL = "https://api.telegram.org/bot"
	CHAT_ID          = "-1001511199505"
)

func GetTelegramLogin(assignee string) (string, error) {
	req, _ := http.NewRequest("GET", fmt.Sprintf(STAFF_API_URL, assignee), nil)
	token := os.Getenv("ST_TOKEN")

	if !strings.HasPrefix(token, "OAuth") {
		req.Header.Add("Authorization", fmt.Sprintf("OAuth %s", token))
	} else {
		req.Header.Add("Authorization", token)
	}

	resp, _ := http.DefaultClient.Do(req)
	if resp.StatusCode == http.StatusOK {
		bodyBytes, _ := ioutil.ReadAll(resp.Body)
		var accounts map[string]interface{}
		_ = json.Unmarshal([]byte(bodyBytes), &accounts)

		for _, contact := range accounts["accounts"].([]interface{}) {
			if contact.(map[string]interface{})["type"].(string) == "telegram" {
				return contact.(map[string]interface{})["value"].(string), nil
			}
		}
	}

	return "", errors.New("could not get telegram login")
}

type Message struct {
	ChatID    string `json:"chat_id"`
	Text      string `json:"text"`
	ParseMode string `json:"parse_mode"`
}

func SendNotification(orderID int, ticketKey string, assignee string) {
	login, _ := GetTelegramLogin(assignee)
	token := os.Getenv("TG_TOKEN")
	message := fmt.Sprintf(
		"@%s [%s](https://st.yandex-team.ru/%s): Заказ №%d получил статус READY\\_TO\\_SHIP.",
		login, ticketKey, ticketKey, orderID,
	)

	bytesRep, _ := json.Marshal(Message{CHAT_ID, message, "Markdown"})
	req, _ := http.NewRequest(
		"POST", fmt.Sprintf("%s%s/sendMessage", TELEGRAM_API_URL, token), bytes.NewBuffer(bytesRep),
	)

	req.Header.Add("Content-Type", "application/json")
	resp, _ := http.DefaultClient.Do(req)

	if resp.StatusCode != http.StatusOK {
		log.Println(resp.Status)
	}
}
