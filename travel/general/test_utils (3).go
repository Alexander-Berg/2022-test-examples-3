package database

import (
	"context"
	"os"
	"strconv"
	"strings"
	"testing"
	"time"

	"golang.yandex/hasql"
	"gorm.io/gorm"

	"a.yandex-team.ru/travel/notifier/internal/models"
	"a.yandex-team.ru/travel/notifier/internal/pgclient"
)

func getPgEnv(key, defaultValue string) string {
	if value := os.Getenv("PG_LOCAL_" + strings.ToUpper(key)); value == "" {
		return defaultValue
	} else {
		return value
	}
}

func getTestPgClient() *pgclient.PGClient {
	port, err := strconv.Atoi(getPgEnv("port", "5432"))
	if err != nil {
		panic(err)
	}
	client, err := pgclient.NewPGClient(
		[]string{"localhost"},
		port,
		getPgEnv("database", "postgres"),
		getPgEnv("user", "postgres"),
		getPgEnv("password", "postgres"),
		pgclient.DefaultInitTimeout,
		[]pgclient.ClientOption{},
		append(pgclient.DefaultClusterOptions, hasql.WithUpdateTimeout(2*time.Second)),
	)
	if err != nil {
		panic(err)
	}
	return client
}

func cleanUp(testPgClient *pgclient.PGClient) {
	ctx, cancelFunc := context.WithTimeout(context.Background(), getDBTestTimeout())
	defer cancelFunc()
	err := testPgClient.ExecuteInTransaction(
		hasql.Primary, func(db *gorm.DB) error {
			db = db.Session(&gorm.Session{Context: ctx, AllowGlobalUpdate: true})
			if err := db.Delete(&models.Notification{}).Error; err != nil {
				return err
			}
			if err := db.Delete(&models.BetterPriceSubscription{}).Error; err != nil {
				return err
			}
			if err := db.Delete(&models.Variant{}).Error; err != nil {
				return err
			}
			if err := db.Delete(&models.Recipient{}).Error; err != nil {
				return err
			}
			if err := db.Delete(&models.User{}).Error; err != nil {
				return err
			}
			return db.Delete(&models.Order{}).Error
		},
	)
	if err != nil {
		panic(err)
	}
}

func newDBCleaner(testPgClient *pgclient.PGClient) func(testFunc func(*testing.T)) func(*testing.T) {
	return func(testFunc func(*testing.T)) func(*testing.T) {
		return func(t *testing.T) {
			if err := models.DropAll(testPgClient); err != nil {
				panic(err)
			}
			if err := models.MigrateAndInit(testPgClient); err != nil {
				panic(err)
			}
			cleanUp(testPgClient)
			t.Cleanup(func() { cleanUp(testPgClient) })
			testFunc(t)
		}
	}
}

func getDBTestTimeout() time.Duration {
	if yaTest := os.Getenv("YA_TEST_RUNNER"); yaTest != "" {
		return 10 * time.Second
	}
	return 5 * time.Minute
}
