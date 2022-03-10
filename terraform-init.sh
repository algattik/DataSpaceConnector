#!/bin/bash

set -euo pipefail

set -a
. .env

cat > generated_backend.tf <<EOF
terraform {
  backend "azurerm" {
    resource_group_name  = "$TERRAFORM_STATE_STORAGE_RESOURCE_GROUP"
    storage_account_name = "$TERRAFORM_STATE_STORAGE_ACCOUNT"
    container_name       = "$TERRAFORM_STATE_STORAGE_CONTAINER"
    key                  = "dev.terraform.tfstate"
  }
}
EOF

terraform init

