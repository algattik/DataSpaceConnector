######### provider.tf

#Set the terraform required version
terraform {
  required_version = ">= 1.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      # It is recommended to pin to a given version of the Provider
      version = "=2.98.0"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = "=2.15.0"
    }
  }
}


# Configure the Microsoft Azure Provider
provider "azurerm" {
  features {
    key_vault {
      # Do not retain Azure Key Vaults after destruction
      purge_soft_delete_on_destroy = true
    }
  }
}


# Data

# Make client_id, tenant_id, subscription_id and object_id variables available
data "azurerm_client_config" "current" {}

######### variables.tf

variable "ci_client_id" {
  type = string
}

variable "prefix" {
  type        = string
  description = "Application name. Use only lowercase letters and numbers"
}

variable "location" {
  type        = string
  description = "Azure region where to create resources."
  default     = "North Europe"
}

######### main.tf

## This configuration creates and configures the following resources:
## - Azure Key Vault (AKV) instance for managing data factory secrets
## - Azure Data Factory (ADF) instance connected to AKV
## - Storage accounts for integration tests, representing provider and consumer side

data "azuread_service_principal" "ci_client" {
  application_id = var.ci_client_id
}

resource "azurerm_resource_group" "main" {
  name     = "rg-${var.prefix}-main"
  location = var.location
}

## Create storage accounts for provider-side data (source) and consumer-side data (sink)
resource "azurerm_storage_account" "provider" {
  name                     = "sa${var.prefix}prov"
  resource_group_name      = azurerm_resource_group.main.name
  location                 = azurerm_resource_group.main.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
}

resource "azurerm_role_assignment" "provider_ci_client" {
  scope                = azurerm_storage_account.provider.id
  role_definition_name = "Storage Account Contributor"
  principal_id         = data.azuread_service_principal.ci_client.object_id
}

## Grant test access to storage keys
resource "azurerm_storage_account" "consumer" {
  name                     = "sa${var.prefix}cons"
  resource_group_name      = azurerm_resource_group.main.name
  location                 = azurerm_resource_group.main.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
}

resource "azurerm_role_assignment" "consumer_ci_client" {
  scope                = azurerm_storage_account.consumer.id
  role_definition_name = "Storage Account Contributor"
  principal_id         = data.azuread_service_principal.ci_client.object_id
}

## Create AKV
resource "azurerm_key_vault" "main" {
  name                = "kv${var.prefix}adf"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
  tenant_id           = data.azurerm_client_config.current.tenant_id
  sku_name            = "standard"
}

## Create ADF
resource "azurerm_data_factory" "main" {
  name                = "df-${var.prefix}-main"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
}

## Create ADF linked service to retrieve keys from AKV
resource "azurerm_data_factory_linked_service_key_vault" "example" {
  name                = "example"
  resource_group_name = azurerm_resource_group.main.name
  data_factory_id     = azurerm_data_factory.main.id
  key_vault_id        = azurerm_key_vault.main.id
}

## Grant ADF read access to AKV secrets
#resource "azurerm_role_assignment" "data_factory_key_vault" {
#  scope                = azurerm_key_vault.main.id
#  role_definition_name = "Key Vault Secrets User"
#  principal_id         = azurerm_data_factory.main.identity[0].principal_id
#}

## Grant DPF write access to AKV secrets
resource "azurerm_role_assignment" "ci_key_vault" {
  scope                = azurerm_key_vault.main.id
  role_definition_name = "Key Vault Secrets Officer"
  principal_id         = data.azuread_service_principal.ci_client.object_id
}

## Grant DPF access to ADF
resource "azurerm_role_assignment" "data_factory" {
  scope                = azurerm_data_factory.main.id
  role_definition_name = "Data Factory Contributor"
  principal_id         = data.azuread_service_principal.ci_client.object_id
}

######### outputs.tf


output "ci_client_id" {
  value = var.ci_client_id
}
output "EDC_AZURE_TENANTID" {
  value = data.azurerm_client_config.current.tenant_id
}
output "EDC_AZURE_SUBSCRIPTIONID" {
  value = data.azurerm_client_config.current.subscription_id
}
output "EDC_DATAFACTORY_RESOURCEID" {
  # value = azurerm_data_factory.main.id
  value = "subscriptions/9d236f09-93d9-4f41-88a6-20201a6a1abc/resourceGroups/adfspike-provider/providers/Microsoft.DataFactory/factories/edcageraspikeadf"
}
output "EDC_DATAFACTORY_KEYVAULT_RESOURCEID" {
  value = "subscriptions/9d236f09-93d9-4f41-88a6-20201a6a1abc/resourceGroups/adfspike-provider/providers/Microsoft.KeyVault/vaults/edcageraspikeadfvault"
}
output "edc_test_provider_storage_resourceid" {
  value = "subscriptions/9d236f09-93d9-4f41-88a6-20201a6a1abc/resourceGroups/adfspike-provider/providers/Microsoft.Storage/storageAccounts/edcproviderstore"
}
output "edc_test_consumer_storage_resourceid" {
  value = "subscriptions/9d236f09-93d9-4f41-88a6-20201a6a1abc/resourceGroups/adfspike-consumer/providers/Microsoft.Storage/storageAccounts/edcconsumerstore"
}
