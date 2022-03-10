## Working with Terraform

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
./terraform-run.sh
```

The script also configures your repository's GitHub Environment so that workflows can consume the resources.

### Consuming Terraform resources

In development, run this script to download a `terraform_outputs.json` file:

```bash
./terraform-fetch.sh
```

This downloads a `terraform_outputs.json` file, which is read by cloud integration tests.
