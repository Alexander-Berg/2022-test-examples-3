package resourcestorage

import (
	"context"
	"errors"
	"fmt"
	"os"
	"path"
	"testing"

	"github.com/golang/protobuf/proto"

	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"
	ipb "a.yandex-team.ru/travel/proto/resourcestorage"
)

const (
	storageDir  = "local_storage"
	resourceKey = "resource_key"
	chunkCount  = 1000
)

var logger, _ = zap.NewQloudLogger(log.InfoLevel)

func newLocalStorageWriter() *LocalStorageWriter {
	rootPath := path.Join(os.TempDir(), storageDir)
	return NewLocalStorageWriter(rootPath)
}

func newLocalStorageReader() *LocalStorageReader {
	rootPath := path.Join(os.TempDir(), storageDir)
	return NewLocalStorageReader(rootPath)
}

func TestLocalStorage_CreateVersion(t *testing.T) {
	writer := newLocalStorageWriter()
	defer writer.Close()
	err := writer.CreateVersion(resourceKey)
	if err != nil {
		t.Errorf("Writer Start fails: %s", err.Error())
	}
	for i := 0; i < chunkCount; i++ {
		err = writer.Write(&ipb.ResourceMeta{
			Key: fmt.Sprintf("%X", i),
		})
		if err != nil {
			t.Errorf("Writer WriteChunk fails: %s", err.Error())
		}
	}
	err = writer.Commit()
	if err != nil {
		t.Errorf("Writer Commit fails: %s", err.Error())
	}
}

func TestLocalStorageWriter_CleanOldVersions(t *testing.T) {
	TestLocalStorage_CreateVersion(t)
	TestLocalStorage_CreateVersion(t)
	TestLocalStorage_CreateVersion(t)
	reader := newLocalStorageReader()
	versions, err := reader.getVersions(resourceKey)
	if err != nil {
		t.Errorf("Reader GetVersions fails: %s", err.Error())
		return
	}
	if len(versions) < 3 {
		t.Errorf("Reader CreateVersion fails: versions < 3")
		return
	}
	writer := newLocalStorageWriter()
	err = writer.CleanOldVersions(resourceKey, 1)
	if err != nil {
		t.Errorf("Writer CleanOldVersions fails: %s", err.Error())
		return
	}
	versions, err = reader.getVersions(resourceKey)
	if err != nil {
		t.Errorf("Reader GetVersions fails: %s", err.Error())
		return
	}
	if len(versions) != 1 {
		t.Errorf("Reader CleanOldVersions fails: versions != 1 ")
		return
	}
	err = reader.Open(resourceKey)
	if err != nil {
		t.Errorf("Reader Open fails: %s", err.Error())
		return
	}
	if reader.resourceMeta.Version != versions[0] {
		t.Errorf("Reader CleanOldVersions fails: leaved version is not last")
		return
	}
}

func TestLocalStorageWriter_ReadChunk(t *testing.T) {
	TestLocalStorage_CreateVersion(t)
	reader := newLocalStorageReader()
	err := reader.Open(resourceKey)
	if err != nil {
		t.Errorf("Reader Open fails: %s", err.Error())
		return
	}
	message := ipb.ResourceMeta{}
	cnt := 0
	for {
		err = reader.Read(&message)
		if err != nil {
			if errors.Is(err, ErrStopIteration) {
				break
			}
			t.Errorf("Reader ReadChunk fails: %s", err.Error())
			return
		}
		cnt++
	}
	if cnt != chunkCount {
		t.Errorf("Reader ReadChunk fails: wrong chunk count (%d != %d)", cnt, chunkCount)
	}
}

type testWriter struct {
	data []*ipb.ResourceMeta
}

func (w *testWriter) Write(raw []byte) (int, error) {
	message := &ipb.ResourceMeta{}
	err := proto.Unmarshal(raw, message)
	if err != nil {
		return 0, fmt.Errorf("testWriter.Write fails: %w", err)
	}
	w.data = append(w.data, message)
	return len(raw), nil
}

type Cont struct {
	Data []*ipb.ResourceMeta
}

func (c *Cont) Iter(ctx context.Context) <-chan proto.Message {
	ch := make(chan proto.Message)
	go func() {
		defer close(ch)
		for _, d := range c.Data {
			select {
			case ch <- d:
				continue
			case <-ctx.Done():
				return
			}
		}
	}()
	return ch
}

func (c *Cont) Add(message proto.Message) {
	c.Data = append(c.Data, message.(*ipb.ResourceMeta))
}

func TestDumper_DumpAndLoad(t *testing.T) {
	c1 := Cont{}
	c2 := Cont{}
	for i := 0; i < chunkCount; i++ {
		c1.Add(&ipb.ResourceMeta{
			Key: fmt.Sprintf("%X", i),
		})
	}
	dumper := NewDumper(&c1, resourceKey, newLocalStorageWriter(), 1, logger)
	n, err := dumper.Dump()
	if err != nil {
		t.Errorf("Dumping fails: %s", err.Error())
		return
	}
	if n != chunkCount {
		t.Errorf("Dumped %d != %d", n, chunkCount)
		return
	}
	loader := NewLoader(&ipb.ResourceMeta{}, resourceKey, newLocalStorageReader(), logger)
	n, err = loader.Load(&c2)
	if err != nil {
		t.Errorf("Loading fails: %s", err.Error())
		return
	}
	if n != chunkCount {
		t.Errorf("Loaded %d != %d", n, chunkCount)
		return
	}
	for i := 0; i < chunkCount; i++ {
		if !proto.Equal(c1.Data[i], c2.Data[i]) {
			t.Errorf("Restored data differs")
			return
		}
	}
}
