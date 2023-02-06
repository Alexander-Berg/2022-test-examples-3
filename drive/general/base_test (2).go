package models

import (
	"database/sql"
	"testing"

	"a.yandex-team.ru/drive/library/go/gosql"
	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"

	_ "github.com/mattn/go-sqlite3"
)

var (
	testDB     *gosql.DB
	testLogger log.Logger
)

func testSetup(t testing.TB) {
	var err error
	db, err := sql.Open("sqlite3", "file::memory:?cache=shared")
	if err != nil {
		t.Fatal("Error:", err)
	}
	testDB = &gosql.DB{
		DB:      db,
		RO:      db,
		Builder: gosql.NewBuilder(gosql.SQLiteDriver),
		Driver:  gosql.SQLiteDriver,
	}
	testLogger, err = zap.New(zap.ConsoleConfig(log.FatalLevel))
	if err != nil {
		t.Fatal("Error:", err)
	}
}

func testTeardown(t testing.TB) {
	if err := testDB.Close(); err != nil {
		t.Fatal("Error:", err)
	}
}

func testWithTx(fn func(*sql.Tx) error) error {
	tx, err := testDB.Begin()
	if err != nil {
		return err
	}
	if err := fn(tx); err != nil {
		_ = tx.Rollback()
		return err
	}
	return tx.Commit()
}
