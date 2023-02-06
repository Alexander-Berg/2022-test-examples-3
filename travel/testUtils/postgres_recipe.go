package testutils

import (
	"context"
	"fmt"
	"os"
	"strconv"
	"strings"
	"testing"
	"time"

	"golang.yandex/hasql"
	"gorm.io/gorm"

	"a.yandex-team.ru/travel/komod/trips/internal/pgclient"
)

func GetDBTestTimeout() time.Duration {
	if yaTest := os.Getenv("YA_TEST_RUNNER"); yaTest != "" {
		return 10 * time.Second
	}
	return 5 * time.Minute
}

func GetTestPgClient() *pgclient.Client {
	port, err := strconv.Atoi(getPgEnv("port", "5432"))
	if err != nil {
		panic(err)
	}
	client, err := pgclient.NewClientBuilder(
		[]string{"localhost"},
		port,
		getPgEnv("database", "postgres"),
		getPgEnv("user", "postgres"),
		getPgEnv("password", "postgres"),
	).WithClusterOptions(hasql.WithUpdateTimeout(2 * time.Second)).Build()
	if err != nil {
		panic(err)
	}
	return client
}

func NewDBCleaner(testPgClient *pgclient.Client, models ...interface{}) func(testFunc func(*testing.T)) func(*testing.T) {
	return func(testFunc func(*testing.T)) func(*testing.T) {
		return func(t *testing.T) {
			if err := dropAll(testPgClient, models); err != nil {
				panic(err)
			}
			if err := migrateAll(testPgClient, models); err != nil {
				panic(err)
			}
			cleanUp(testPgClient, models)
			t.Cleanup(func() { cleanUp(testPgClient, models) })
			testFunc(t)
		}
	}
}

func dropAll(pgClient *pgclient.Client, models []interface{}) error {
	rwDB, err := pgClient.GetDB(hasql.Primary)
	if err != nil {
		return err
	}
	err = rwDB.Migrator().DropTable(models...)
	if err != nil {
		return fmt.Errorf("failed to drop tables: %w", err)

	}
	return nil
}

func migrateAll(pgClient *pgclient.Client, models []interface{}) error {
	rwDB, err := pgClient.GetDB(hasql.Primary)
	if err != nil {
		return err
	}
	err = rwDB.AutoMigrate(models...)
	if err != nil {
		return fmt.Errorf("failed to migrate tables: %w", err)

	}
	return nil
}

func cleanUp(testPgClient *pgclient.Client, models []interface{}) {
	ctx, cancelFunc := context.WithTimeout(context.Background(), GetDBTestTimeout())
	defer cancelFunc()
	err := testPgClient.ExecuteInTransaction(
		hasql.Primary, func(db *gorm.DB) error {
			db = db.Session(&gorm.Session{Context: ctx, AllowGlobalUpdate: true})
			for i := len(models) - 1; i >= 0; i-- {
				if err := db.Delete(models[i]).Error; err != nil {
					return err
				}
			}
			return nil
		},
	)
	if err != nil {
		panic(err)
	}
}

func getPgEnv(key, defaultValue string) string {
	if value := os.Getenv("PG_LOCAL_" + strings.ToUpper(key)); value == "" {
		return defaultValue
	} else {
		return value
	}
}
