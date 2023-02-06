package cache

import (
	"context"
	"fmt"
	"sync"
	"time"

	"github.com/golang/protobuf/proto"

	ipb "a.yandex-team.ru/travel/buses/backend/internal/common/proto"
)

type ResponseContent struct {
	Response string `json:"response"`
}

type TestContextStorage struct {
	mutex   sync.RWMutex
	records map[string]*map[string]*ipb.TTestContextCacheRecord
}

func NewTestContextStorage() *TestContextStorage {
	testContextStorage := &TestContextStorage{
		mutex:   sync.RWMutex{},
		records: make(map[string]*map[string]*ipb.TTestContextCacheRecord),
	}
	return testContextStorage
}

func (tcs *TestContextStorage) makeUniqToken() string {
	ts := time.Now().UnixNano()
	for {
		token := fmt.Sprintf("%x", ts)
		if _, ok := tcs.records[token]; ok {
			ts += 1
			continue
		}
		return token
	}
}

func (tcs *TestContextStorage) set(token string, name string, payloadBytes []byte) {
	record, ok := tcs.records[token]
	if !ok {
		r := make(map[string]*ipb.TTestContextCacheRecord)
		tcs.records[token] = &r
		record = &r
	}
	(*record)[name] = &ipb.TTestContextCacheRecord{
		Token:        token,
		Name:         name,
		PayloadBytes: payloadBytes,
	}
}

func (tcs *TestContextStorage) Set(token string, payload proto.Message) (string, error) {
	tcs.mutex.Lock()
	defer tcs.mutex.Unlock()

	payloadBytes, err := proto.Marshal(payload)
	if err != nil {
		return "", fmt.Errorf("TestContextStorage.Set: %w", err)
	}
	if token == "" {
		token = tcs.makeUniqToken()
	}
	name := string(proto.MessageReflect(payload).Descriptor().Name())
	tcs.set(token, name, payloadBytes)
	return token, nil
}

func (tcs *TestContextStorage) Get(token string, payload proto.Message) (bool, error) {
	tcs.mutex.RLock()
	defer tcs.mutex.RUnlock()

	record, exists := tcs.records[token]
	if !exists {
		return false, nil
	}
	name := string(proto.MessageReflect(payload).Descriptor().Name())
	message, exists := (*record)[name]
	if !exists {
		return false, nil
	}

	if err := proto.Unmarshal(message.PayloadBytes, payload); err != nil {
		return true, fmt.Errorf("TestContextStorage.Get: %w", err)
	}
	return true, nil
}

func (tcs *TestContextStorage) Len() int {
	return len(tcs.records)
}

func (tcs *TestContextStorage) Iter(ctx context.Context) <-chan proto.Message {
	ch := make(chan proto.Message)
	go func() {
		tcs.mutex.RLock()
		defer tcs.mutex.RUnlock()

		defer close(ch)
		for _, record := range tcs.records {
			for _, message := range *record {
				select {
				case ch <- message:
					continue
				case <-ctx.Done():
					return
				}
			}
		}
	}()
	return ch
}

func (tcs *TestContextStorage) Add(message proto.Message) {
	tcs.mutex.Lock()
	defer tcs.mutex.Unlock()

	record := message.(*ipb.TTestContextCacheRecord)
	tcs.set(record.Token, record.Name, record.PayloadBytes)
}
