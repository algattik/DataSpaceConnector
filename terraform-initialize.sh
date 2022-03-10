#!/bin/bash

set -euxo pipefail

. .env

# Create resource group
az group create --name "$TERRAFORM_STATE_STORAGE_RESOURCE_GROUP" --location "$TERRAFORM_STATE_STORAGE_LOCATION"

# Create storage account
az storage account create --resource-group "$TERRAFORM_STATE_STORAGE_RESOURCE_GROUP" --name "$TERRAFORM_STATE_STORAGE_ACCOUNT"

# Create blob container
az storage container create --name "$TERRAFORM_STATE_STORAGE_CONTAINER" --account-name "$TERRAFORM_STATE_STORAGE_ACCOUNT"
