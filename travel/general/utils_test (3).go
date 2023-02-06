package database

import (
	"testing"

	"github.com/jackc/pgx/v4"
	"github.com/jackc/pgx/v4/stdlib"
	"github.com/stretchr/testify/require"
)

func TestUtils(t *testing.T) {
	t.Run(
		"getNodeAddr/works with pgx db", func(t *testing.T) {
			connConfig, _ := pgx.ParseConfig("host=localhost port=1234 user=user password=password dbname=dbname")

			db := stdlib.OpenDB(*connConfig)
			require.Equal(t, "localhost", getNodeAddr(db))
		},
	)
}
