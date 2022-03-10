# Cloud testing

## Usage

### Overview

A cloud deployment is required to test the integration with resources that cannot be run in a local emulator, such as Azure Data Factory.

- A **GitHub environment** is used to encapsulate secrets.
- An **application** is created to represent the system when running test. In Azure Active Directory, a service principal for the application is configured in the cloud tenant, and configured to trust the GitHub environment using Federated Identity Credentials. For running tests locally, developers should be assigned equivalent or higher permissions.
- **Cloud resources** and **role assignments** are provisioned using Terraform. To simplify the configuration and reduce the risk of leakage of elevated credentials, Terraform is executed manually by privileged developers on their local machine (rather than in a GitHub workflow).
- **Configuration** for connecting to cloud resources is provided using a Terraform outputs JSON file. The file is uploaded to GitHub as an Environment secret. For running tests locally, a script is provided to download this file locally.

### Forks and pull requests

The cloud testing pipeline detects whether credentials for an Azure environment are configured. If not, the cloud testing pipeline is skipped.

When running pull requests across repositories (from a repository fork), the workflow doesn't have access to secrets for security reasons. Therefore, the cloud testing pipeline will only run after the PR is merged.

If the PR author has reason to expect that the PR may break cloud tests, they should configure credentials for an Azure environment on their fork and provision Azure resources. This will cause the cloud testing workflow to run on their fork (outside of PR checks). The author should attach evidence of the cloud testing workflow run to the PR.

Alternatively, the reviewer from the upstream repository may pull the PR into a temporary branch on the upstream repository in order to trigger the cloud testing workflow (outside of PR checks). This should only be done after careful inspection that the code is not leaking credentials.

## Deploying an Azure environment

### Planning your deployment

You will need:

- An Azure subscription
- At least one developer with the `Owner` role on the Azure subscription in order to deploy resources and assign roles
- A service principal (instructions below)
- For developers to be able to read configuration from Terraform outputs, grant them the `Storage Blob Data Reader` (or `Contributor`) role on the storage account holding the Terraform state file.
- For developers to be able to use their own identity to run tests, grant the the following roles at the Subscription level, or on the Resource Group created to hold Terraform resources.
  - `Storage Account Contributor` (or `Contributor`)
  - `Data Factory Contributor` (or `Contributor`)
  - `Key Vault Secrets Officer`
- Developers will need the following utilities installed locally:
  - [Azure CLI](https://docs.microsoft.com/cli/azure/install-azure-cli)
  - [Terraform CLI](https://learn.hashicorp.com/tutorials/terraform/install-cli)
  - [GitHub CLI](https://cli.github.com)

### Create a service identity for the GitHub environment

[Create and configure an application for your GitHub environment](https://docs.microsoft.com/azure/active-directory/develop/workload-identity-federation-create-trust-github).

- For **Entity Type**, select **Environment**.
- For **Environment Name**, type `Azure-dev`.
- For **Name**, type any name.

### Configure Terraform settings

The shell scripts that wrap Terraform commands take their configuration from a file named `.env` that should not be committed into the repository (though the file should be shared across developers in your fork). Copy and adapt the example settings file to your environment, following the comments in the file:

```bash
cp .env.example .env
```

### Deploying Terraform resources

The first time only, set up the state storage account by running this script:

```bash
./terraform-initialize.sh
```

After that, run this script to update cloud resources. Follow the prompt and enter `yes` if requested to apply any changes:

```bash
./terraform-apply.sh
```

The script also configures your repository's GitHub Environment so that workflows can consume the resources. The following secrets are provisioned in the Environment:

- `AZURE_TENANT_ID`, `AZURE_CLIENT_ID` , and `AZURE_SUBSCRIPTION_ID`, required to log in with the Federated Credential scenario.
- `TERRAFORM_OUTPUTS`, a JSON string containing resource identifiers and other settings needed to connect tests to the resources deployed with Terraform.

Note that these values do not actually contain any sensitive information.

That is sufficient to have the cloud testing workflow run in your fork on every Git push.

### Consuming Terraform resources locally

For running cloud tests in local development, run this script to download a `terraform_outputs.json` file:

```bash
./terraform-fetch.sh
```

This downloads a `terraform_outputs.json` file, which is read by cloud integration tests. This file should not be committed to the repository.
