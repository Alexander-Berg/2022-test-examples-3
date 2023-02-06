#!/bin/sh
SCRIPT_DIR="$(dirname "$0")"
"$SCRIPT_DIR/run-sql.sh" "$SCRIPT_DIR/testing-pgaas.sh" 'testing_operator_window' "$@"
