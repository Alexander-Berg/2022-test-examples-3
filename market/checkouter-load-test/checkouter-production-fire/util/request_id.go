package util

import (
	"encoding/hex"
	"fmt"
	"math/rand"
	"time"
)

type uuid struct {
	uuid chan string
}

func createGenerator() uuid {
	r := rand.New(rand.NewSource(time.Now().UnixNano()))
	u := uuid{uuid: make(chan string)}
	go func() {
		for {
			b := make([]byte, 16)
			read, err := r.Read(b)
			if err != nil || read < 16 {
				continue
			}
			c := make([]byte, 32)
			count := hex.Encode(c, b)
			if count < 32 {
				continue
			}
			s := string(c)
			u.uuid <- s
		}
	}()
	return u
}

var u = createGenerator()

func GetRequestID() (id *string, ok bool) {
	str, ok := <-u.uuid
	if !ok {
		return nil, ok
	}
	requestID := fmt.Sprintf("%d/%s", time.Now().UnixMilli(), str)
	return &requestID, ok

}
