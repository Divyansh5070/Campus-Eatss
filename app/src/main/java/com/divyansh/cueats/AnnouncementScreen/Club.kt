package com.divyansh.cueats.AnnouncementScreen

/**
 * Club profile data model (matches admin app structure)
 */
data class Club(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val logoUrl: String = "",
    val contactEmail: String = "",
    val contactPhone: String = "",
    val website: String = ""
)
