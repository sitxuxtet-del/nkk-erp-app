package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nkk_erp_preferences")

data class BusinessSettings(
    val businessName: String = "NKK Store & POS",
    val sector: String = "RETAIL", // RETAIL, RESTAURANT, PHARMACY, FASHION, WHOLESALE, SERVICES
    val currencySymbol: String = "$",
    val taxRatePercent: Double = 5.0,
    val address: String = "100 Market Boulevard, Suite 10",
    val phone: String = "+1 800-555-0199",
    val receiptHeader: String = "NKK ERP Retail Point Of Sale",
    val receiptFooter: String = "Thank you for your business! Visit again.",
    val fastApiBaseUrl: String = "http://10.0.2.2:8000",
    val currentStaffName: String = "Alex Vance (Admin)",
    val currentStaffRole: String = "ADMIN",
    val currentStaffPin: String = "1234"
)

class BusinessPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val BUSINESS_NAME = stringPreferencesKey("business_name")
        val SECTOR = stringPreferencesKey("sector")
        val CURRENCY = stringPreferencesKey("currency")
        val TAX_RATE = doublePreferencesKey("tax_rate")
        val ADDRESS = stringPreferencesKey("address")
        val PHONE = stringPreferencesKey("phone")
        val RECEIPT_HEADER = stringPreferencesKey("receipt_header")
        val RECEIPT_FOOTER = stringPreferencesKey("receipt_footer")
        val FASTAPI_URL = stringPreferencesKey("fastapi_url")
        val STAFF_NAME = stringPreferencesKey("staff_name")
        val STAFF_ROLE = stringPreferencesKey("staff_role")
        val STAFF_PIN = stringPreferencesKey("staff_pin")
    }

    val settingsFlow: Flow<BusinessSettings> = context.dataStore.data.map { preferences ->
        BusinessSettings(
            businessName = preferences[PreferencesKeys.BUSINESS_NAME] ?: "NKK Store & POS",
            sector = preferences[PreferencesKeys.SECTOR] ?: "RETAIL",
            currencySymbol = preferences[PreferencesKeys.CURRENCY] ?: "$",
            taxRatePercent = preferences[PreferencesKeys.TAX_RATE] ?: 5.0,
            address = preferences[PreferencesKeys.ADDRESS] ?: "100 Market Boulevard, Suite 10",
            phone = preferences[PreferencesKeys.PHONE] ?: "+1 800-555-0199",
            receiptHeader = preferences[PreferencesKeys.RECEIPT_HEADER] ?: "NKK ERP Retail Point Of Sale",
            receiptFooter = preferences[PreferencesKeys.RECEIPT_FOOTER] ?: "Thank you for your business! Visit again.",
            fastApiBaseUrl = preferences[PreferencesKeys.FASTAPI_URL] ?: "http://10.0.2.2:8000",
            currentStaffName = preferences[PreferencesKeys.STAFF_NAME] ?: "Alex Vance (Admin)",
            currentStaffRole = preferences[PreferencesKeys.STAFF_ROLE] ?: "ADMIN",
            currentStaffPin = preferences[PreferencesKeys.STAFF_PIN] ?: "1234"
        )
    }

    suspend fun updateBusinessProfile(
        name: String,
        sector: String,
        currency: String,
        taxRate: Double,
        address: String,
        phone: String,
        header: String,
        footer: String,
        fastApiUrl: String
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BUSINESS_NAME] = name
            preferences[PreferencesKeys.SECTOR] = sector
            preferences[PreferencesKeys.CURRENCY] = currency
            preferences[PreferencesKeys.TAX_RATE] = taxRate
            preferences[PreferencesKeys.ADDRESS] = address
            preferences[PreferencesKeys.PHONE] = phone
            preferences[PreferencesKeys.RECEIPT_HEADER] = header
            preferences[PreferencesKeys.RECEIPT_FOOTER] = footer
            preferences[PreferencesKeys.FASTAPI_URL] = fastApiUrl
        }
    }

    suspend fun setActiveStaff(name: String, role: String, pin: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.STAFF_NAME] = name
            preferences[PreferencesKeys.STAFF_ROLE] = role
            preferences[PreferencesKeys.STAFF_PIN] = pin
        }
    }
}
