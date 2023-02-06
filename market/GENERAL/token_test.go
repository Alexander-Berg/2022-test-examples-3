package oauthlib

import (
	"fmt"
	"golang.org/x/sys/unix"
	"os"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/market/sre/library/golang/logger"
)

func init() {
	_ = os.Setenv("LOG_LEVEL", "ERROR")
	logger.Setup()
	logger.ProceedFlags()
}

func memfile(name string, b []byte) (int, error) {
	fd, err := unix.MemfdCreate(name, 0)
	if err != nil {
		return 0, fmt.Errorf("MemfdCreate: %v", err)
	}

	err = unix.Ftruncate(fd, int64(len(b)))
	if err != nil {
		return 0, fmt.Errorf("Ftruncate: %v", err)
	}

	data, err := unix.Mmap(fd, 0, len(b), unix.PROT_READ|unix.PROT_WRITE, unix.MAP_SHARED)
	if err != nil {
		return 0, fmt.Errorf("Mmap: %v", err)
	}

	copy(data, b)

	err = unix.Munmap(data)
	if err != nil {
		return 0, fmt.Errorf("Munmap: %v", err)
	}

	return fd, nil
}

func Test_CheckUpdateTokenByFile(t *testing.T) {
	fd, err := memfile("testLoadSnapshot", []byte("TestPASS"))
	if err != nil {
		t.Fatalf("memfile: %v", err)
	}
	fp := fmt.Sprintf("/proc/self/fd/%d", fd)
	f := os.NewFile(uintptr(fd), fp)
	if f == nil {
		t.Fatalf("error create file %s", fp)
	}
	defer func() {
		_ = f.Close()
	}()

	token := NewToken(f.Name())
	time.Sleep(30 * time.Second)
	assert.Contains(t, "TestPASS", token.String())
	//update test
	_, _ = f.Seek(0, 0)
	_, _ = f.Write([]byte("TestPASS2\n"))
	_ = f.Sync()
	time.Sleep(30 * time.Second)
	assert.Contains(t, "TestPASS2", token.String())
	assert.NotContains(t, "TestPASS", token.String())
	//test
}

func Test_CheckOutputOAuth(t *testing.T) {
	fd, err := memfile("testLoadSnapshot", []byte("TestPASS"))
	if err != nil {
		t.Fatalf("memfile: %v", err)
	}
	fp := fmt.Sprintf("/proc/self/fd/%d", fd)
	f := os.NewFile(uintptr(fd), fp)
	if f == nil {
		t.Fatalf("error create file %s", fp)
	}
	defer func() {
		_ = f.Close()
	}()
	_, _ = f.WriteString("TestPASS\n")
	_ = f.Sync()
	token := NewToken(f.Name())
	time.Sleep(30 * time.Second)
	assert.Contains(t, "OAuth TestPASS", token.String())
}
