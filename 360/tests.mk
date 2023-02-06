test: node_modules
	@npm run test-ci

coverage: node_modules
	@npm run $@

.PHONY: test coverage
