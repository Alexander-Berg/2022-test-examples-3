PROJECT = "Sovetnik Redir"

NPM_PATH ?= npm

build:
	$(NPM_PATH) install
	$(NPM_PATH) run test:ci

clean:
	rm -f -r node_modules

.PHONY: clean build
