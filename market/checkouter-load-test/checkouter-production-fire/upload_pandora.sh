#!/bin/sh
ya make --target-platform=linux

upload() {
    file=$1
    echo "upload $file"
    ya upload "$file" --ttl=2 --json-output | jq '.resource_id' 2>&1
}

upload app/app
upload cb_categories.json
upload noncb_categories.json
