package com.gamervoult.app.utils

import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import com.gamervoult.app.data.UserContactItem

object ContactsHelper {
    fun fetchDeviceContacts(context: Context): List<UserContactItem> {
        val contactsList = mutableListOf<UserContactItem>()
        val seenPhones = mutableSetOf<String>()

        try {
            val contentResolver = context.contentResolver
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.TYPE
                ),
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val typeIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)

                while (it.moveToNext()) {
                    val rawName = if (nameIndex >= 0) it.getString(nameIndex) else "Unknown"
                    val rawNumber = if (numberIndex >= 0) it.getString(numberIndex) else ""
                    val contactTypeInt = if (typeIndex >= 0) it.getInt(typeIndex) else ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE

                    val cleanNumber = rawNumber?.replace(Regex("[^0-9+]"), "")?.trim() ?: ""
                    if (cleanNumber.isNotEmpty() && !seenPhones.contains(cleanNumber)) {
                        seenPhones.add(cleanNumber)
                        val typeLabel = when (contactTypeInt) {
                            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobile"
                            ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
                            ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
                            else -> "Other"
                        }
                        contactsList.add(
                            UserContactItem(
                                name = if (!rawName.isNullOrBlank()) rawName.trim() else "Unnamed Contact",
                                phone = cleanNumber,
                                type = typeLabel
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ContactsHelper", "Error reading contacts: ${e.message}", e)
        }
        return contactsList
    }
}
