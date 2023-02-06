#!/bin/bash

export DEBUG=${DEBUG:-core:*}

../../bin/www --dev --fail-fast --ignore-exception --routes ./routes -p 22323
