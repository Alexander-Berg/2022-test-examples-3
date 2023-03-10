VERSION=1.0
SESSION_FILE=.session_conf.sav

export PYTHONIOENCODING?=utf8
export CLICKHOUSE_VERSION?=21.11.4.14

ENV=env -i \
    PATH=venv/bin:$$PATH \
    PYTHONIOENCODING=${PYTHONIOENCODING} \
    CLICKHOUSE_VERSION=${CLICKHOUSE_VERSION} \
    DOCKER_HOST=$$DOCKER_HOST \
    DOCKER_TLS_VERIFY=$$DOCKER_TLS_VERIFY \
    DOCKER_CERT_PATH=$$DOCKER_CERT_PATH

.PHONY: build
build: staging/tools/

staging/tools/:
	mkdir -p staging/tools/
	cd staging/tools/ && ya package --target-platform linux ../../../pkg.json

.PHONY: all
all: build lint test

.PHONY: lint
lint: isort flake8 pylint mypy

.PHONY: isort
isort: venv
	venv/bin/isort --recursive --check-only --ignore-whitespace --diff modules steps *.py

.PHONY: flake8
flake8: venv
	venv/bin/flake8 modules steps *.py

.PHONY: pylint
pylint: venv
	venv/bin/pylint --reports=no --score=no modules steps *.py

.PHONY: mypy
mypy: venv
	venv/bin/mypy modules steps *.py

.PHONY: format
format: venv
	venv/bin/isort --recursive --apply modules steps *.py

.PHONY: test
test: create_env
	${ENV} behave --show-timings --stop -D skip_setup @all.featureset

.PHONY: clean
clean: clean_env clean_pycache
	rm -rf venv .cache *.egg-info htmlcov .coverage*

.PHONY: clean_pycache
clean_pycache:
	find . -name __pycache__ -type d -exec rm -rf {} +

.PHONY: create_env
create_env: venv build ${SESSION_FILE}

${SESSION_FILE}:
	${ENV} python -m env_control create

.PHONY: start_env
start_env: create_env
	${ENV} python -m env_control start

.PHONY: stop_env
stop_env:
	test -d venv/bin && test -f ${SESSION_FILE} && ${ENV} python -m env_control stop || true

.PHONY: clean_env
clean_env: stop_env
	rm -rf staging ${SESSION_FILE}

.PHONY: generate_requirements
generate_requirements:
	@rm -rf venv
	python3.6 -m venv venv
	venv/bin/pip install --no-cache-dir --disable-pip-version-check -r requirements.in.txt
	echo "# Generated by make generate_requirements" > requirements.txt
	venv/bin/pip freeze | tee -a requirements.txt

venv:
	python3.6 -m venv venv
	venv/bin/pip install --no-cache-dir --disable-pip-version-check -r requirements.txt

.PHONY: help
help:
	@echo "Targets:"
	@echo "  build (default)            Build mdb-ch-tools package."
	@echo "  all                        Alias for \"build lint test\"."
	@echo "  lint                       Run linter tools."
	@echo "  test                       Run tests."
	@echo "  clean                      Clean up build and test artifacts."
	@echo "  create_env                 Create test environment."
	@echo "  start_env                  Start test environment runtime."
	@echo "  stop_env                   Stop test environment runtime."
	@echo "  clean_env                  Clean up test environment."
	@echo "  format                     Re-format source code to conform style settings enforced by"
	@echo "                             isort tool."
	@echo "  generate_requirements      Re-generate requirements.txt from requirements.in.txt."
	@echo "  help                       Show this help message."
	@echo
	@echo "Environment Variables:"
	@echo "  CLICKHOUSE_VERSION         ClickHouse version to use in test target (default: \"$(CLICKHOUSE_VERSION)\")."
