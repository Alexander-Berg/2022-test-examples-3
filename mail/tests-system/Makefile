
arcroot := $(shell ya dump root)
export ARCADIA_SOURCE_ROOT := $(arcroot)
export ARCADIA_BUILD_ROOT := $(arcroot)

service := xivaserver

shell:
	docker-compose exec $(service) /bin/bash

tests: up
	docker-compose exec -T $(service) /bin/bash /usr/bin/run_tests.sh $(extra)

retest:
	docker-compose exec -T $(service) /bin/bash /usr/bin/run_tests.sh $(extra)

up: app
	docker-compose up -d --force-recreate $(service)

app:
	ya make --checkout -j10 -t ../

stop:
	docker-compose stop $(service)

clean:
	docker-compose down --rmi local -v
	sudo chown --quiet -R `whoami` ../var || true
	rm -rf ../var
