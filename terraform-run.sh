#!/bin/bash

set -euxo pipefail

REPO=agera-edc/DataSpaceConnector

terraform init

terraform apply --auto-approve

terraform output -json | gh secret set CLOUD_SETTINGS --repo $REPO --env Azure-dev 
terraform output -raw ci_client_id | gh secret set AZURE_CLIENT_ID --repo $REPO --env Azure-dev
terraform output -raw subscription_id | gh secret set AZURE_SUBSCRIPTION_ID --repo $REPO --env Azure-dev
terraform output -raw tenant_id | gh secret set AZURE_TENANT_ID --repo $REPO --env Azure-dev

