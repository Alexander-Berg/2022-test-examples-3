package kraken

import (
	"fmt"
	"golang.org/x/sys/unix"
	"os"
	"testing"
)

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

func Test_LoadSnapshot(t *testing.T) {
	fd, err := memfile("testLoadSnapshot", []byte(testData))
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

	snapshot := NewKrakenSnapshot()
	if err := snapshot.Load(fp); err != nil {
		t.Errorf("error %s, trace %s", err, trace())
	}

	if snapshot.Mproxy.RPS != float32(325.0) {
		t.Errorf("error Mproxy.RPS != 325.0")
	}
}

func Test_SaveSnapshot(t *testing.T) {
}

var testData = `{
    "mproxy": {
        "rps": 325.0, #
        "rps_limit": 325.0, #
        "rps_limit_changed": true,
        "rps_limit_prev": 696.0
    },
    "sample1": {
        "errors": 0.0,
        "rps": 2271.8, #
        "rtime_0_80": 89.0, #
        "rtime_0_95": 214.0, #
        "rtime_0_99": 332.0, #
        "rtime_0_999": 518.0 #
    },
    "sample2": {
        "errors": 0.0,
        "rps": 2298.8166666666666,
        "rtime_0_80": 108.0,
        "rtime_0_95": 242.0,
        "rtime_0_99": 367.0,
        "rtime_0_999": 590.0
    },
    "target": {
        "errors": 0.0,
        "rps": 324.98333333333335,
        "rtime_0_80": 102.0,
        "rtime_0_95": 244.0,
        "rtime_0_99": 362.0,
        "rtime_0_999": 629.0
    },
    "time": "2021-11-24 20:17:00",
    "total": {
        "count": 60.0,
        "errors": 0.0, #
        "power": 86522.133038,
        "rps": 7074.633333333333, #
        "rtime_0_80": 101.0, #
        "rtime_0_95": 238.0, #
        "rtime_0_99": 353.0,
        "rtime_0_999": 556.0 #
    }
}`
