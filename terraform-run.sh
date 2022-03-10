#!/bin/bash

set -euo pipefail

ENVIRONMENT=Azure-dev

echo "== Running terraform init =="
. terraform-init.sh

echo "== Checking GitHub contributor permissions =="
gh="gh --repo $GITHUB_REPO --env $ENVIRONMENT"
if ! $gh secret list > /dev/null
then
  echo "Cannot access repo $GITHUB_REPO"
  echo "Usage: $0 OWNER/REPO TerraformStateStorageAccountName"
  echo "OWNER/REPO must be a repository on which you have Contributor permissions."
  exit 1
fi

echo "== Running terraform apply =="
terraform apply

echo "== Collecting terraform outputs =="
terraform output -json | $gh secret set TERRAFORM_OUTPUTS

terraform output -raw ci_client_id | $gh secret set AZURE_CLIENT_ID
terraform output -raw EDC_AZURE_SUBSCRIPTIONID | $gh secret set AZURE_SUBSCRIPTION_ID
terraform output -raw EDC_AZURE_TENANTID | $gh secret set AZURE_TENANT_ID

