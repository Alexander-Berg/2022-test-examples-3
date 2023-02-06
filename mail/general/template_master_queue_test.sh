#!/bin/bash
set -exo pipefail

source template_master_queue_prod.sh

export CONFIG_TEMPLATE=queue-test.cfg.template

