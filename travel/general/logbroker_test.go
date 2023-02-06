package logbroker

import (
	"context"
	"sync"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"go.uber.org/goleak"
	"go.uber.org/zap"
	"go.uber.org/zap/zaptest"

	"a.yandex-team.ru/kikimr/public/sdk/go/persqueue/recipe"
	yzap "a.yandex-team.ru/library/go/core/log/zap"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
)

func TestLogbroker(t *testing.T) {

	const (
		topic            = "topic"
		sampleSize int32 = 10000
		timeout          = 30 * time.Second
	)
	var clusters = []string{"cluster1_", "cluster2_"}
	assert.Equal(t, 0, int(sampleSize%2))
	assert.NotEmpty(t, clusters)

	runProducer := func(ctx context.Context, t *testing.T, cluster string) *Producer {
		env := recipe.New(t)
		logger := &yzap.Logger{L: zaptest.NewLogger(t, zaptest.Level(zap.ErrorLevel))}
		producer := NewProducerWithRecipe(env, cluster, topic, logger)
		err := producer.Run(ctx)
		assert.NoError(t, err)
		return producer
	}

	runProducers := func(ctx context.Context, t *testing.T) {

		var producers []*Producer
		for _, cluster := range clusters {
			producer := runProducer(ctx, t, cluster)
			producers = append(producers, producer)
		}
		var i int32
		for i = 0; i < sampleSize; i++ {
			msg := &rasp.TCountry{
				Id: i + 1,
			}
			err := producers[int(i)%len(producers)].Write(msg)
			assert.NoError(t, err, "failed to write")
		}
		t.Logf("Published %d messages", sampleSize)
	}

	runConsumerAndCheck := func(ctx context.Context, t *testing.T) {
		env := recipe.New(t)

		logger := &yzap.Logger{L: zaptest.NewLogger(t, zaptest.Level(zap.ErrorLevel))}
		c, err := NewConsumerWithRecipe(env, clusters, topic, logger)
		if !assert.NoError(t, err) {
			return
		}

		err = c.Run(ctx)
		if !assert.NoError(t, err) {
			return
		}

		channel := c.NewChannel()
		defer func() {
			err := c.CloseChannel(channel)
			assert.NoError(t, err)
		}()

		s := make(map[int32]struct{})
		var i int32
		msg := &rasp.TCountry{}
		for len(s) < int(sampleSize) {
			err := channel.Read(msg)
			if !assert.NoError(t, err) {
				return
			}
			s[msg.Id] = struct{}{}

			if i%10 == 0 {
				go func() {
					duplicateMsg := &rasp.TCountry{}
					deadline := time.Now().Add(time.Second)
					tmpChannel := c.NewChannel()
					_ = tmpChannel.ReadWithDeadline(duplicateMsg, deadline)
					err = c.CloseChannel(tmpChannel)
					assert.NoError(t, err)
				}()
			}
		}
		assert.Len(t, s, int(sampleSize))
	}

	t.Run("Multicluster Read-Write", func(t *testing.T) {

		ctx, cancel := context.WithDeadline(context.Background(), time.Now().Add(timeout))

		var wg sync.WaitGroup
		wg.Add(2)
		go func() {
			runProducers(ctx, t)
			wg.Done()
		}()
		go func() {
			runConsumerAndCheck(ctx, t)
			wg.Done()
		}()

		wg.Wait()
		cancel()
		goleak.VerifyNone(t)
	})
}
