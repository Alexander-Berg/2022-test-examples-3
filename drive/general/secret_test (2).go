package models

import (
	"database/sql"
	"reflect"
	"testing"

	"a.yandex-team.ru/drive/runner/config"
)

func testCreateSecretTable(t testing.TB) {
	if _, err := testDB.Exec(
		`CREATE TABLE "secret" (` +
			`"id" integer PRIMARY KEY,` +
			`"dir_id" integer,` +
			`"title" varchar(255) NOT NULL,` +
			`"type" varchar(32) NOT NULL,` +
			`"data" text NOT NULL` +
			`)`,
	); err != nil {
		t.Fatal("Error:", err)
	}
	if _, err := testDB.Exec(
		`CREATE TABLE "secret_event" (` +
			`"event_id" integer PRIMARY KEY,` +
			`"event_type" varchar(16) NOT NULL,` +
			`"event_time" bigint NOT NULL,` +
			`"event_user_id" bigint,` +
			`"event_task_id" bigint,` +
			`"id" integer NOT NULL,` +
			`"dir_id" integer,` +
			`"title" varchar(255) NOT NULL,` +
			`"type" varchar(32) NOT NULL,` +
			`"data" text NOT NULL` +
			`)`,
	); err != nil {
		t.Fatal("Error:", err)
	}
}

func TestSecretManager(t *testing.T) {
	testSetup(t)
	defer testTeardown(t)
	testCreateSecretTable(t)
	manager, err := NewSecretStore(
		testDB, "secret", "secret_event", config.Yav{},
		testLogger,
	)
	if err != nil {
		t.Fatal("Error:", err)
	}
	if err := manager.InitTx(testDB); err != nil {
		t.Fatal("Error:", err)
	}
	secret := Secret{
		DirID: 1,
		Title: "Secret1",
		Type:  YavSecret,
		Data:  []byte("{}"),
	}
	if err := manager.CreateTx(testDB, &secret); err != nil {
		t.Fatal("Error:", err)
	}
	if secret.ID == 0 {
		t.Fatal("Secret ID should be modified")
	}
	secret = Secret{
		ID:    secret.ID,
		DirID: 1,
		Title: "Secret2",
		Type:  "Unknown",
		Data:  []byte("[1, 2, 3]"),
	}
	if err := manager.UpdateTx(testDB, secret); err != nil {
		t.Fatal("Error:", err)
	}
	if err := manager.SyncTx(testDB); err != nil {
		t.Fatal("Error:", err)
	}
	{
		result, err := manager.Get(secret.ID)
		if err != nil {
			t.Fatal("Error:", err)
		}
		if !reflect.DeepEqual(result, secret) {
			t.Fatalf("Expected %v, got %v", secret, result)
		}
	}
	if err := manager.RemoveTx(testDB, secret.ID); err != nil {
		t.Fatal("Error:", err)
	}
	if err := manager.SyncTx(testDB); err != nil {
		t.Fatal("Error:", err)
	}
	if _, err := manager.Get(secret.ID); err != sql.ErrNoRows {
		t.Fatalf("Expected %v, got %v", sql.ErrNoRows, err)
	}
}
