#!/bin/bash

set -euo pipefail

REPO=${1:-eclipse-dataspaceconnector/DataSpaceConnector}
ENVIRONMENT=Azure-dev

gh="gh --repo $REPO --env $ENVIRONMENT"

echo "== Checking GitHub contributor permissions =="
if ! $gh secret list > /dev/null
then
  echo "Cannot access repo $REPO"
  echo "Usage: $0 OWNER/REPO"
  echo "OWNER/REPO must be a repository on which you have Contributor permissions."
  exit 1
fi

echo "== Running terraform init =="
terraform init

echo "== Running terraform apply =="
terraform apply

echo "== Collecting terraform outputs =="
terraform output -json | $gh secret set TERRAFORM_OUTPUTS

terraform output -raw ci_client_id | $gh secret set AZURE_CLIENT_ID
terraform output -raw EDC_AZURE_SUBSCRIPTIONID | $gh secret set AZURE_SUBSCRIPTION_ID
terraform output -raw EDC_AZURE_TENANTID | $gh secret set AZURE_TENANT_ID

