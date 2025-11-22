package com.divyansh.cueats.ShopsScreen

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

class OfferViewModel : ViewModel() {

    private val _offers = MutableLiveData<List<Offer>>()
    val offers: LiveData<List<Offer>> = _offers

    private val firestore = FirebaseFirestore.getInstance()
    private var refreshTimer: Timer? = null

    init {
        fetchOffers()
        startAutoRefresh()
    }

    private fun fetchOffers() {
        Log.d("OfferViewModel", "Starting fetchOffers...")
        firestore.collection("offers")
            .addSnapshotListener { snapshot, error ->
                Log.d("OfferViewModel", "Snapshot listener triggered")

                if (error != null) {
                    Log.e("OfferViewModel", "Error fetching offers", error)
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    Log.w("OfferViewModel", "Snapshot is null")
                    return@addSnapshotListener
                }

                Log.d("OfferViewModel", "Documents found: ${snapshot.documents.size}")

                val currentDate = Date()
                Log.d("OfferViewModel", "Current date: $currentDate")

                val offersList = snapshot.documents.mapNotNull { doc ->
                    try {
                        Log.d("OfferViewModel", "Processing document: ${doc.id}")
                        Log.d("OfferViewModel", "Document data: ${doc.data}")

                        val isActive = doc.getBoolean("isActive") ?: true
                        Log.d("OfferViewModel", "Offer '${doc.getString("title")}' isActive: $isActive")

                        // First check if the offer is active
                        if (!isActive) {
                            Log.d("OfferViewModel", "Offer '${doc.getString("title")}' is inactive, filtering out")
                            return@mapNotNull null
                        }

                        val validUntilString = doc.getString("validUntil") ?: ""
                        Log.d("OfferViewModel", "ValidUntil string: '$validUntilString'")

                        val isOfferValid = if (validUntilString.isNotEmpty()) {
                            try {
                                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val expiryDate = formatter.parse(validUntilString)

                                if (expiryDate != null) {
                                    val calendar = Calendar.getInstance()
                                    calendar.time = expiryDate
                                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                                    calendar.set(Calendar.MINUTE, 59)
                                    calendar.set(Calendar.SECOND, 59)
                                    val expiryEndOfDay = calendar.time

                                    val isValid = expiryEndOfDay.after(currentDate)
                                    Log.d("OfferViewModel", "Offer '${doc.getString("title")}' expires on: $expiryEndOfDay, Current: $currentDate, Valid: $isValid")
                                    isValid
                                } else {
                                    Log.w("OfferViewModel", "Could not parse date: $validUntilString")
                                    false
                                }
                            } catch (e: Exception) {
                                Log.e("OfferViewModel", "Error parsing date: $validUntilString", e)
                                false
                            }
                        } else {
                            Log.d("OfferViewModel", "No valid until date found for offer: ${doc.getString("title")}, considering it valid")
                            true
                        }

                        if (isOfferValid) {
                            val offer = Offer(
                                id = doc.id,
                                title = doc.getString("title") ?: "",
                                description = doc.getString("description") ?: "",
                                shopName = doc.getString("shopName") ?: "",
                                discount = doc.getString("discount") ?: "",
                                validUntil = null, // Keep this as null for now
                                validUntilString = validUntilString,
                                imageUrl = doc.getString("imageUrl"),
                                isActive = isActive
                            )

                            Log.d("OfferViewModel", "Valid offer added: ${offer.title}")
                            offer
                        } else {
                            Log.d("OfferViewModel", "Expired offer filtered out: ${doc.getString("title")}")
                            null
                        }

                    } catch (e: Exception) {
                        Log.e("OfferViewModel", "Error parsing offer: ${doc.id}", e)
                        null
                    }
                }

                Log.d("OfferViewModel", "Total offers processed: ${offersList.size}")
                _offers.value = offersList
            }
    }

    private fun startAutoRefresh() {
        refreshTimer = Timer()
        refreshTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                Log.d("OfferViewModel", "Auto-refreshing offers...")
                // Don't call fetchOffers() here as it will add another listener
                // The snapshot listener will handle real-time updates
            }
        }, 60000, 60000) // Refresh every 1 minute
    }

    override fun onCleared() {
        super.onCleared()
        refreshTimer?.cancel()
        refreshTimer = null
    }

    fun addOffer(offer: Offer) {
        val offerMap = hashMapOf(
            "title" to offer.title,
            "description" to offer.description,
            "shopName" to offer.shopName,
            "discount" to offer.discount,
            "validUntil" to offer.validUntilString,
            "imageUrl" to offer.imageUrl,
            "isActive" to offer.isActive,
            "createdAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("offers")
            .add(offerMap)
            .addOnSuccessListener { documentReference ->
                Log.d("OfferViewModel", "Offer added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w("OfferViewModel", "Error adding offer", e)
            }
    }

    // Add this function to manually refresh offers for debugging
    fun refreshOffers() {
        Log.d("OfferViewModel", "Manual refresh triggered")
        fetchOffers()
    }
}