package models

import (
	"context"
	"database/sql"
	"fmt"
	"testing"
	"time"

	_ "github.com/mattn/go-sqlite3"
)

func testCreateLockTable(t testing.TB) {
	if _, err := testDB.Exec(
		`CREATE TABLE "lock"(` +
			`"id" integer PRIMARY KEY,` +
			`"name" varchar(32) NOT NULL,` +
			`"host_id" integer,` +
			`"ping_time" bigint NOT NULL)`,
	); err != nil {
		t.Fatal("Error:", err)
	}
	if _, err := testDB.Exec(
		`INSERT INTO "lock"("name","host_id","ping_time")`+
			`VALUES($1,$2,$3)`,
		"test_lock", nil, 0,
	); err != nil {
		t.Fatal("Error:", err)
	}
}

func TestLockManager(t *testing.T) {
	testSetup(t)
	defer testTeardown(t)
	testCreateLockTable(t)
	manager := NewLockStore(testDB, "lock", testLogger)
	if err := testWithTx(func(tx *sql.Tx) error {
		lock, err := manager.GetTx(tx, "test_lock")
		if err != nil {
			return err
		}
		if lock.Name != "test_lock" {
			t.Fatal("Invalid lock name")
		}
		return nil
	}); err != nil {
		t.Fatal("Error:", err)
	}
	lock, err := manager.acquire(context.Background(), "test_lock", 1)
	if err != nil {
		t.Fatal("Error:", err)
	}
	if err := manager.ping(context.Background(), &lock); err != nil {
		t.Fatal("Error:", err)
	}
	if _, err := manager.acquire(
		context.Background(), "test_lock", 1,
	); err != ErrLockAcquired {
		t.Fatal("Expected error")
	}
	if _, err := manager.acquire(
		context.Background(), "test_lock", 2,
	); err != ErrLockAcquired {
		t.Fatal("Expected error")
	}
	if err := manager.release(lock); err != nil {
		t.Fatal("Error:", err)
	}
	if _, err := manager.acquire(
		context.Background(), "test_lock", 1,
	); err != nil {
		t.Fatal("Error:", err)
	}
}

func TestLockManager_WithTx_Simple(t *testing.T) {
	testSetup(t)
	defer testTeardown(t)
	testCreateLockTable(t)
	manager := NewLockStore(testDB, "lock", testLogger)
	for i := 0; i < 10; i++ {
		if err := manager.WithLock(
			context.Background(), "test_lock", 1,
			func(ctx context.Context) error {
				return nil
			},
		); err != nil {
			t.Fatal("Error:", err)
		}
	}
}

func TestLockManager_WithTx_Error(t *testing.T) {
	testSetup(t)
	defer testTeardown(t)
	testCreateLockTable(t)
	manager := NewLockStore(testDB, "lock", testLogger)
	if err := manager.WithLock(
		context.Background(), "test_lock", 1,
		func(ctx context.Context) error {
			return fmt.Errorf("error")
		},
	); err == nil {
		t.Fatal("Expected error")
	}
}

func TestLockManager_WithTx_Sleep(t *testing.T) {
	testSetup(t)
	defer testTeardown(t)
	testCreateLockTable(t)
	manager := NewLockStore(testDB, "lock", testLogger)
	if err := manager.WithLock(
		context.Background(), "test_lock", 1,
		func(ctx context.Context) error {
			time.Sleep(1500 * time.Millisecond)
			return nil
		},
	); err != nil {
		t.Fatal("Error:", err)
	}
}
