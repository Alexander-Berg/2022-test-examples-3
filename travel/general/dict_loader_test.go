package registry

import (
	"testing"
	"time"

	"github.com/golang/protobuf/proto"
	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/library/go/resourcestorage"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/clock"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/testutils"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/updater"
)

func TestLoadFirstlyAndOnlyOnce(t *testing.T) {
	callCount := 0
	fn := func(_ *resourcestorage.Loader) error {
		callCount++
		return nil
	}

	loader, _ := newTestDictLoader(t, fn)

	err := loader.Load()
	assert.NoError(t, err)
	err = loader.Load()
	assert.ErrorIs(t, err, updater.AlreadyUpdated)
	assert.Equal(t, callCount, 1)
}

func TestLoadUpdatedResource(t *testing.T) {
	callCount := 0
	fn := func(_ *resourcestorage.Loader) error {
		callCount++
		return nil
	}

	loader, testReader := newTestDictLoader(t, fn)

	err := loader.Load()
	assert.NoError(t, err)

	testReader.fakeTime = clock.Now()
	err = loader.Load()
	assert.NoError(t, err)
	assert.Equal(t, callCount, 2)
}

func newTestDictLoader(t *testing.T, fn DictLoadFn) (*DictLoader, *storageReader) {
	logger := testutils.NewLogger(t)
	reader := &storageReader{fakeTime: clock.Now().Add(-time.Hour)}
	return NewDictLoader(logger, reader, fn, "", nil), reader
}

type storageReader struct {
	fakeTime time.Time
}

func (r *storageReader) Open(_ string) error                      { return nil }
func (r *storageReader) Read(_ proto.Message) error               { return resourcestorage.ErrStopIteration }
func (r *storageReader) Close()                                   {}
func (r *storageReader) GetTimestamp(_ string) (time.Time, error) { return r.fakeTime, nil }
