## Working with Terraform

### Planning your deployment

You will need:

- An Azure subscription
- A service principal (instructions below)
- For developers to be able to use their own identity, grant them the following roles at the Subscription level, or on the Resource Group created to hold Terraform resources.
  - `Storage Account Contributor` (or `Contributor`)
  - `Data Factory Contributor` (or `Contributor`)
  - `Key Vault Secrets Officer`

### Create a service identity for CI

Create a service principal.

https://docs.microsoft.com/en-us/azure/active-directory/develop/workload-identity-federation-create-trust-github?tabs=azure-portal

Configure the application to trust your GitHub repository.

https://docs.microsoft.com/en-us/azure/active-directory/develop/workload-identity-federation-create-trust-github?tabs=azure-portal

For **Entity Type**, select **Environment**.

For **Environment Name**, type `Azure-dev`.

For **Name**, type any name.

### Configure Terraform scripts

Copy and adapt the settings file to your environment:

```bash
cp .env.example .env
```

### Deploying Terraform resources

The first time only, set up the state storage account by running this script:

```bash
./terraform-initialize.sh
```

After that, run this script to update cloud resources. Follow the prompt and enter `yes` to apply any changes:

```bash
./terraform-apply.sh
```

The script also configures your repository's GitHub Environment so that workflows can consume the resources.

### Consuming Terraform resources

In development, run this script to download a `terraform_outputs.json` file:

```bash
./terraform-fetch.sh
```

This downloads a `terraform_outputs.json` file, which is read by cloud integration tests.
