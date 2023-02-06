test:
	npm ci
	make temp-fix
	cp -n config/secrets.example.json config/secrets.json
	npm run test

.PHONY: test
