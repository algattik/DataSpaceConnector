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
  }
}


# Configure the Microsoft Azure Provider
provider "azurerm" {
  features {}
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


resource "azurerm_resource_group" "main" {
  name     = "rg-${var.prefix}-main"
  location = var.location
}

resource "azurerm_data_factory" "main" {
  name                = "df-${var.prefix}-main"
  location            = azurerm_resource_group.main.location
  resource_group_name = azurerm_resource_group.main.name
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
  value = azurerm_data_factory.main.id
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
