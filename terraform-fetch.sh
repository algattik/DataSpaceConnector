#!/bin/bash

set -euxo pipefail

terraform init

terraform output -json > cloud_settings.json
