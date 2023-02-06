package wiki

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"strings"
)

const (
	WIKI_API_URL = "https://wiki-api.yandex-team.ru/_api/frontend/%s/.grid"
)

func fetchGridRows(data WikiResponseData) (table []map[string]interface{}) {
	for _, row := range data.Rows {
		jsonRow := make(map[string]interface{})
		for i, cell := range row {
			if cell["raw"] != nil && cell["raw"] != "" {
				fields := data.Structure["fields"].([]interface{})[i].(map[string]interface{})
				jsonRow[fields["title"].(string)] = cell["raw"]
			}
		}
		table = append(table, jsonRow)
	}
	return
}

type WikiResponseData struct {
	Rows      [][]map[string]interface{} `json:"rows"`
	Structure map[string]interface{}     `json:"structure"`
	Version   string                     `json:"version"`
}

type WikiResponse struct {
	Debug map[string]interface{} `json:"debug"`
	Data  WikiResponseData       `json:"data"`
	User  map[string]interface{} `json:"user"`
}

func ReadGrid(path string, token string) ([]map[string]interface{}, error) {
	req, _ := http.NewRequest("GET", fmt.Sprintf(WIKI_API_URL, path), nil)
	req.Header.Add("Content-Type", "application/json")
	req.Header.Add("Accept-Charset", "UTF-8")

	if !strings.HasPrefix(token, "OAuth") {
		req.Header.Add("Authorization", fmt.Sprintf("OAuth %s", token))
	} else {
		req.Header.Add("Authorization", token)
	}

	resp, _ := http.DefaultClient.Do(req)
	if resp.StatusCode == http.StatusOK {
		bodyBytes, _ := ioutil.ReadAll(resp.Body)
		var grid WikiResponse
		_ = json.Unmarshal([]byte(bodyBytes), &grid)

		return fetchGridRows(grid.Data), nil
	}
	err := fmt.Errorf("could not read wiki grid, resp status: %d", resp.StatusCode)
	return make([]map[string]interface{}, 0), err
}
