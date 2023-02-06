package worker

import (
	"container/list"
	"io"
	"io/ioutil"
	"os"
	"testing"
	"time"
)

type stubFileReader struct {
	data []byte
	pos  int
}

func (s *stubFileReader) Read(p []byte) (n int, err error) {
	if n = copy(p, s.data[s.pos:]); n == 0 {
		err = io.EOF
	}
	s.pos += n
	return
}

func (s *stubFileReader) Close() error {
	return nil
}

func (s *stubFileReader) ExpireTime() time.Time {
	return time.Now().Add(time.Hour)
}

type stubFileProvider struct{}

func (p *stubFileProvider) ReadFile(url FileURL) (FileReader, error) {
	return &stubFileReader{data: []byte(url.Path)}, nil
}

func TestFileStore(t *testing.T) {
	tempDir := os.TempDir()
	defer func() {
		_ = os.RemoveAll(tempDir)
	}()
	store := &FileStore{
		dir: tempDir,
		providers: map[string]FileProvider{
			"test": &stubFileProvider{},
		},
		files: list.New(),
		index: map[FileURL]*list.Element{},
	}
	r, err := store.ReadFile(FileURL{"test", "Hello, World!"})
	if err != nil {
		t.Fatal("Error:", err)
	}
	defer func() {
		if err := r.Close(); err != nil {
			t.Fatal("Error:", err)
		}
	}()
	s, err := ioutil.ReadAll(r)
	if err != nil {
		t.Fatal("Error:", err)
	}
	if string(s) != "Hello, World!" {
		t.Fatal("Invalid value")
	}
}
