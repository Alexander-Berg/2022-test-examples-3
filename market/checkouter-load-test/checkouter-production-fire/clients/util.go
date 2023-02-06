package clients

import (
	"encoding/json"
	"io/ioutil"
	"strings"
)

const RequestID = "x-market-req-id"

func ExtractReqID(headers map[string][]string) string {
	if len(headers) == 0 {
		return ""
	}

	for k, v := range headers {
		if strings.ToLower(k) == RequestID {
			return strings.Join(v, ",")
		}
	}

	return ""
}

func ReadCashbackCategories(file string) map[int]bool {
	data, err := ioutil.ReadFile(file)
	if err != nil {
		panic(err)
	}
	var categories []int
	err = json.Unmarshal(data, &categories)
	if err != nil {
		panic(err)
	}
	result := map[int]bool{}
	for _, c := range categories {
		result[c] = true
	}
	return result
}

func CategoriesKeys(m map[int]bool) []int {
	var result []int
	for k := range m {
		result = append(result, k)
	}
	return result
}
