package gc

import (
	"context"
	"fmt"
	"math"
	"math/rand"
	"testing"
	"time"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/core/log/nop"

	"a.yandex-team.ru/search/tools/perforator/internal/storage"
	storage_mock "a.yandex-team.ru/search/tools/perforator/internal/storage/mock"
)

const (
	day  = time.Hour * 24
	week = day * 7
)

func generateID() storage.ProfileID {
	return fmt.Sprintf("%016x", rand.Uint64())
}

func TestShouldRemove(t *testing.T) {
	rand.Seed(19)

	halflife := week

	now := time.Now()
	justBeforeNow := now.Add(-time.Second)
	justBeforeHalflife := now.Add(time.Second - halflife)
	afterHalfLife := now.Add(-halflife)
	afterTenEpochs := now.Add(-halflife * 5)

	ids := make([]storage.ProfileID, 10000)
	for i := range ids {
		ids[i] = generateID()
	}

	for _, test := range []struct {
		timestamp time.Time
		minAlive  float64
		maxAlive  float64
	}{{
		timestamp: justBeforeNow,
		minAlive:  1.0,
	}, {
		timestamp: justBeforeHalflife,
		minAlive:  1.0,
	}, {
		timestamp: afterHalfLife,
		minAlive:  0.4,
		maxAlive:  0.6,
	}, {
		timestamp: afterTenEpochs,
		minAlive:  0.8 * math.Pow(0.5, 5),
		maxAlive:  1.2 * math.Pow(0.5, 5),
	}} {
		t.Run(fmt.Sprintf("profile/%s", now.Sub(test.timestamp)), func(t *testing.T) {
			gc := GC{
				log:      &nop.Logger{},
				halflife: halflife,
			}
			numAlive := 0
			for _, id := range ids {
				shouldRemove := gc.shouldRemove(storage.ProfileMetaInfo{
					Timestamp: test.timestamp,
					ProfileID: id,
				}, now)
				if !shouldRemove {
					numAlive++
				}
			}
			alive := float64(numAlive) / float64(len(ids))

			if test.maxAlive != 0.0 {
				require.LessOrEqual(t, alive, test.maxAlive)
			}
			if test.minAlive != 0.0 {
				require.GreaterOrEqual(t, alive, test.minAlive)
			}
		})
	}
}

func TestGCRemoves(t *testing.T) {
	rand.Seed(42)

	ctrl := gomock.NewController(t)
	defer ctrl.Finish()

	s := storage_mock.NewMockStorage(ctrl)
	ctx := context.Background()

	now := time.Now()
	longTimeAgo := now.Add(-week * 300)
	removeProfileID := generateID()
	keepProfileID := generateID()

	gomock.InOrder(
		s.EXPECT().
			ListProfiles(ctx, storage.ProfileListRequest{
				Offset: 0,
				Limit:  profilesListLimit,
			}).
			Return(&storage.ProfileMetaInfoList{
				Values: []storage.ProfileMetaInfo{{
					ProfileID: keepProfileID,
					Timestamp: now.Add(week),
				}, {
					ProfileID: removeProfileID,
					Timestamp: longTimeAgo,
				}, {
					ProfileID: removeProfileID + "frozen",
					Timestamp: longTimeAgo,
					Frozen:    true,
				}},
				Paginated: storage.Paginated{
					Page:     1,
					NumPages: 1,
				}}, nil),

		s.EXPECT().
			RemoveProfiles(ctx, []storage.ProfileMetaInfo{{
				ProfileID: removeProfileID,
				Timestamp: longTimeAgo,
			}}).
			Return(nil),
	)

	gc := GC{
		log:       &nop.Logger{},
		halflife:  week,
		storage:   s,
		batchSize: 100,
	}

	err := gc.Collect(ctx, now)
	require.NoError(t, err)
}
