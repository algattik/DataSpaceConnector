#!/bin/bash

set -euxo pipefail

output=terraform_outputs.json

. terraform-init.sh

terraform output -json > $output

echo Created $output
