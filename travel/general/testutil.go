package blocktests

import (
	"fmt"
	"net/http"
	"time"
)

var cache = make(map[string]error)

func testURL(url string) error {
	if len(url) == 0 {
		return fmt.Errorf("URL is empty")
	}

	cachedValue, ok := cache[url]
	if ok {
		return cachedValue
	}

	client := http.Client{
		Timeout: 5 * time.Second, // izi travel often fails with lesser timeout
	}
	resp, err := client.Get(url)
	if err != nil {
		return storeAndReturn(url, err)
	}

	if resp == nil {
		return storeAndReturn(url, fmt.Errorf("URL content is nil"))
	}

	if resp.ContentLength == 0 {
		return storeAndReturn(url, fmt.Errorf("URL content is empty"))
	}

	if resp.StatusCode != http.StatusOK {
		return storeAndReturn(url, fmt.Errorf("URL status is not OK: %v %v", resp.StatusCode, resp.Status))
	}

	return storeAndReturn(url, nil)
}

func storeAndReturn(url string, err error) error {
	cache[url] = err
	return err
}
