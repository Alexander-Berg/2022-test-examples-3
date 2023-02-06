package licensechecks

import (
	"context"
	"database/sql"
	"testing"
	"time"

	"a.yandex-team.ru/drive/library/go/gosql"
	"a.yandex-team.ru/drive/library/go/gosql/stores"
	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"

	_ "github.com/mattn/go-sqlite3"
)

var (
	testDB     *gosql.DB
	testLogger log.Logger
)

func testCreateTaskTable(tb testing.TB) {
	if _, err := testDB.Exec(
		`CREATE TABLE "license_check_task"(` +
			`"id" integer PRIMARY KEY AUTOINCREMENT,` +
			`"license_number" varchar(255) NOT NULL,` +
			`"license_issue_date" datetime NOT NULL,` +
			`"result" varchar(255) NOT NULL,` +
			`"raw_result" varchar(255) NOT NULL,` +
			`"check_retry" integer NOT NULL,` +
			`"check_tries" integer NOT NULL,` +
			`"check_next_time" datetime NULL,` +
			`"notify_retry" integer NOT NULL,` +
			`"notify_next_time" datetime NULL,` +
			`"callback" varchar(255) NOT NULL,` +
			`"callback_data" text NOT NULL,` +
			`"callback_url" text NULL,` +
			`"result_time" datetime NULL,` +
			`"priority" integer NOT NULL)`,
	); err != nil {
		tb.Fatal("Error:", err)
	}
}

func testSetup(tb testing.TB) {
	var err error
	db, err := sql.Open("sqlite3", "file::memory:?cache=shared")
	if err != nil {
		tb.Fatal("Error:", err)
	}
	testDB = &gosql.DB{
		DB:      db,
		RO:      db,
		Builder: gosql.NewBuilder(gosql.SQLiteDriver),
		Driver:  gosql.SQLiteDriver,
	}
	testLogger, err = zap.New(zap.ConsoleConfig(log.FatalLevel))
	if err != nil {
		tb.Fatal("Error:", err)
	}
	testCreateTaskTable(tb)
}

func testTeardown(tb testing.TB) {
	if err := testDB.Close(); err != nil {
		tb.Fatal("Error:", err)
	}
}

func TestTaskStore(t *testing.T) {
	testSetup(t)
	defer testTeardown(t)
	store := NewTaskStore(testDB, testLogger)
	task := Task{
		LicenseNumber:    "123",
		LicenseIssueDate: time.Date(2020, 01, 02, 10, 11, 12, 13, time.UTC),
	}
	if err := store.Create(context.Background(), &task); err != nil {
		t.Fatal("Error:", err)
	}
	if task.ID == 0 {
		t.Fatal("Task has zero ID")
	}
	rows, err := store.objects.SelectObjects(context.Background())
	if err != nil {
		t.Fatal("Error:", err)
	}
	{
		var outTask Task
		if err := stores.ScanRow(rows, &outTask); err != nil {
			t.Fatal("Error:", err)
		}
		if outTask.ID == 0 {
			t.Fatal("Tash has zero ID")
		}
		if outTask.LicenseNumber != task.LicenseNumber {
			t.Fatalf("Expected %q, but got %q", task.LicenseNumber, outTask.LicenseNumber)
		}
		if day := outTask.LicenseIssueDate.Day(); day != 2 {
			t.Fatalf("Expected %d, but got %d", 2, day)
		}
	}
}
