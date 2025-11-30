package com.divyansh.cueats.ShopsScreen

import android.content.Context
import android.util.Log
import androidx.compose.runtime.remember
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.random.Random

class ShopViewModel : ViewModel() {
    private val _shops = MutableLiveData<List<Shop>>()
    val shops: LiveData<List<Shop>> = _shops

    init {
        fetchShops()
    }

    private fun fetchShops() {
        viewModelScope.launch {
            try {
                _shops.postValue(getShops())
            } catch (e: Exception) {
                Log.e("ShopViewModel", "Error fetching shops: ${e.message}")
                _shops.postValue(emptyList())
            }
        }
    }

    // Main function to get filtered and recommended shops
    fun getFilteredAndRecommendedShops(
        shops: List<Shop>,
        favoriteShops: Set<String>,
        selectedFilter: String,
        selectedAddressFilter: String,
        searchQuery: String,
        showFavoritesOnly: Boolean
    ): List<Shop> {
        // First apply filters
        val baseFiltered = shops.filter { shop ->
            val matchesCategory = if (selectedFilter == "All") {
                true
            } else {
                shop.menuItems.any { menuItem ->
                    menuItem.category.equals(selectedFilter, ignoreCase = true)
                }
            }

            val matchesAddress = if (selectedAddressFilter == "All") {
                true
            } else {
                shop.address.contains(selectedAddressFilter, ignoreCase = true) ||
                        shop.location.contains(selectedAddressFilter, ignoreCase = true)
            }

            val matchesSearch = searchQuery.isEmpty() ||
                    shop.name.contains(searchQuery, ignoreCase = true) ||
                    shop.menuItems.any { menuItem ->
                        menuItem.name.contains(searchQuery, ignoreCase = true)
                    }

            val matchesFavorites = if (showFavoritesOnly) {
                favoriteShops.contains(shop.id)
            } else {
                true
            }

            matchesCategory && matchesSearch && matchesAddress && matchesFavorites
        }

        // Then apply recommendation algorithm
        return getRecommendedShops(
            shops = baseFiltered,
            favoriteShops = favoriteShops,
            userLocation = selectedAddressFilter.takeIf { it != "All" },
            currentTimeHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        )
    }

    fun getRecommendedShops(
        shops: List<Shop>,
        favoriteShops: Set<String>,
        userLocation: String? = null,
        currentTimeHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    ): List<Shop> {
        return shops.map { shop ->
            val score = calculateRecommendationScore(
                shop = shop,
                favoriteShops = favoriteShops,
                userLocation = userLocation,
                currentTimeHour = currentTimeHour
            )
            shop to score
        }
            .sortedByDescending { it.second } // Sort by score (highest first)
            .map { it.first } // Return only the shops
    }

    private fun calculateRecommendationScore(
        shop: Shop,
        favoriteShops: Set<String>,
        userLocation: String?,
        currentTimeHour: Int
    ): Double {
        var score = 0.0

        // 1. Time-based recommendations get TOP priority (35% weight)
        // This ensures relevant food appears at the right time
        score += getTimeBasedScore(shop, currentTimeHour) * 0.35

        // 2. Location-based scoring (25% weight)
        userLocation?.let { location ->
            if (shop.address.contains(location, ignoreCase = true) ||
                shop.location.contains(location, ignoreCase = true)) {
                score += 25.0
            }
            val nearbyKeywords = listOf("near", "close", "vicinity")
            if (nearbyKeywords.any { shop.address.contains(it, ignoreCase = true) }) {
                score += 5.0
            }
        }

        // 3. Exploration boost for NON-favorites (20% weight)
        // This is the key change - reward shops user hasn't tried
        if (!favoriteShops.contains(shop.id)) {
            score += 20.0 // Boost unexplored shops
        } else {
            score += 8.0  // Small boost for favorites, but not dominant
        }

        // 4. Popular items and quality indicators (10% weight)
        val popularItemsCount = shop.menuItems.count { it.isPopular }
        score += (popularItemsCount * 2.0).coerceAtMost(10.0)

        // 5. Menu variety scoring (8% weight)
        val categoryCount = shop.menuItems.map { it.category }.distinct().size
        score += (categoryCount * 1.2).coerceAtMost(8.0)

        // 6. Price range diversity bonus (7% weight)
        val priceVariety = calculatePriceVariety(shop.menuItems)
        score += priceVariety * 0.07

        // 7. Discovery randomization (3% weight)
        // Higher randomization to promote serendipitous discovery
        score += Random.nextDouble(0.0, 5.0)

        // 8. Rating-based boost (2% weight)
        // Reward highly rated shops slightly
        score += shop.rating * 0.4

        return score
    }

    private fun getTimeBasedScore(shop: Shop, currentHour: Int): Double {
        val mealTimeBoosts = mapOf(
            // Early morning (6-7 AM) - Light options
            6 to mapOf("Beverages" to 20.0, "Healthy" to 15.0, "Bakery" to 12.0),

            // Breakfast time (7-10 AM)
            7 to mapOf("South Indian" to 25.0, "Healthy" to 20.0, "Beverages" to 15.0, "Bakery" to 12.0),
            8 to mapOf("South Indian" to 25.0, "Healthy" to 20.0, "Beverages" to 15.0, "Bakery" to 12.0),
            9 to mapOf("South Indian" to 20.0, "Healthy" to 15.0, "Beverages" to 12.0, "Street Food" to 8.0),
            10 to mapOf("South Indian" to 15.0, "Healthy" to 10.0, "Beverages" to 8.0),

            // Pre-lunch (11 AM)
            11 to mapOf("Street Food" to 15.0, "Sandwiches" to 12.0, "Beverages" to 10.0),

            // Lunch time (12-3 PM)
            12 to mapOf("Thali" to 30.0, "North Indian" to 25.0, "Biryani" to 25.0, "South Indian" to 15.0),
            13 to mapOf("Thali" to 30.0, "North Indian" to 25.0, "Biryani" to 25.0, "South Indian" to 15.0),
            14 to mapOf("Thali" to 25.0, "North Indian" to 20.0, "Biryani" to 20.0, "Chinese" to 15.0),
            15 to mapOf("Thali" to 15.0, "North Indian" to 12.0, "Chinese" to 12.0),

            // Evening snacks (4-6 PM)
            16 to mapOf("Street Food" to 25.0, "Momos" to 20.0, "Sandwiches" to 15.0, "Beverages" to 12.0),
            17 to mapOf("Street Food" to 25.0, "Momos" to 20.0, "Sandwiches" to 15.0, "Beverages" to 12.0),
            18 to mapOf("Street Food" to 20.0, "Momos" to 15.0, "Beverages" to 15.0, "Pizza" to 10.0),

            // Dinner time (7-10 PM)
            19 to mapOf("Pizza" to 25.0, "Chinese" to 20.0, "Chicken" to 20.0, "Paneer" to 15.0, "North Indian" to 12.0),
            20 to mapOf("Pizza" to 25.0, "Chinese" to 20.0, "Chicken" to 20.0, "Paneer" to 15.0, "Burger" to 12.0),
            21 to mapOf("Pizza" to 20.0, "Chinese" to 15.0, "Chicken" to 15.0, "Burger" to 12.0),
            22 to mapOf("Pizza" to 15.0, "Chinese" to 10.0, "Burger" to 10.0),

            // Late night (11 PM - 2 AM)
            23 to mapOf("Pizza" to 20.0, "Burger" to 15.0, "Rolls" to 15.0, "Street Food" to 10.0),
            0 to mapOf("Pizza" to 15.0, "Burger" to 12.0, "Rolls" to 12.0, "Chinese" to 8.0),
            1 to mapOf("Pizza" to 12.0, "Burger" to 10.0, "Rolls" to 8.0),
            2 to mapOf("Pizza" to 8.0, "Burger" to 6.0),

            // Very late/early (3-5 AM) - Minimal options
            3 to mapOf("Pizza" to 5.0, "Burger" to 3.0),
            4 to mapOf("Pizza" to 3.0),
            5 to mapOf("Beverages" to 5.0, "Bakery" to 3.0)
        )

        val timeBoosts = mealTimeBoosts[currentHour] ?: emptyMap()

        return shop.menuItems.map { it.category }
            .distinct()
            .sumOf { category -> timeBoosts[category] ?: 0.0 }
    }

    private fun calculatePriceVariety(menuItems: List<MenuItem>): Double {
        if (menuItems.isEmpty()) return 0.0

        val prices = menuItems.map { it.price }
        val avgPrice = prices.average()
        val priceRange = (prices.maxOrNull() ?: 0.0) - (prices.minOrNull() ?: 0.0)

        return when {
            avgPrice in 40.0..180.0 && priceRange > 30.0 -> 100.0 // Great variety in good range
            avgPrice in 30.0..220.0 && priceRange > 20.0 -> 80.0  // Good variety
            avgPrice in 50.0..200.0 -> 60.0                       // Good price range
            avgPrice < 25.0 -> 30.0                               // Too cheap
            avgPrice > 300.0 -> 20.0                              // Too expensive
            else -> 40.0                                          // Average
        }
    }

    // Add a function to get shops specifically for discovery
    fun getDiscoveryShops(
        shops: List<Shop>,
        favoriteShops: Set<String>,
        limit: Int = 3
    ): List<Shop> {
        return shops
            .filter { !favoriteShops.contains(it.id) } // Only non-favorites
            .filter { it.rating >= 4.0 } // Only well-rated shops
            .shuffled() // Random order for true discovery
            .take(limit)
    }

    // Optional: Get trending shops (shops with many popular items)
    fun getTrendingShops(shops: List<Shop>, limit: Int = 5): List<Shop> {
        return shops
            .sortedByDescending { shop ->
                val popularItemsCount = shop.menuItems.count { it.isPopular }
                val categoryVariety = shop.menuItems.map { it.category }.distinct().size
                popularItemsCount * 2 + categoryVariety + shop.rating
            }
            .take(limit)
    }

    fun getShopById(shopId: String): Shop? {
        return _shops.value?.find { it.id == shopId }
    }
    // https://res.cloudinary.com/dv5f6ctbx/image/upload/w_1000,h_1000,f_webp,q_auto:good/
    fun getShops(): List<Shop> {
        return listOf(
            Shop(
                id = "2",
                name = "Punjabi Rasoi",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/f_webp,q_auto:good/v1748772877/IMG20250528125019_q1xqly.jpg",
                cuisine = "North Indian",
                address = "Boys Hostel, Near NC-1",
                location = "NC Boys",
                rating = 4.1,
                deliveryTime = "15-25 min",
                popularItems = listOf(""),
                menuItems = getPunjabiRasoiMenuItems()
            ),
            Shop(
                id = "1",
                name = "Catch Up Cafe",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/f_webp,q_auto:good/v1748772875/IMG20250528124943_y2xwdw.jpg",
                cuisine = "Cafe",
                address = "Boys Hostel, Near NC-1",
                location = "NC Boys",
                rating = 4.5,
                deliveryTime = "10-20 min",
                popularItems = listOf(""),
                menuItems = getCatchUpCafeMenuItems(),

            ),
            Shop(
                id = "3",
                name = "Chatori Chaat And Kulcha Corner",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/f_webp,q_auto:good/v1748772876/IMG20250528124955_gy9wzx.jpg",
                cuisine = "Street Food",
                address = "Boys Hostel, Near NC-1",
                location = "NC Boys",
                rating = 4.3,
                deliveryTime = "15-30 min",
                popularItems = listOf("Pani Puri", "Chole Kulcha", "Bhel Puri"),
                menuItems = getChatoryChaatMenuItems(),
                hasDelivery = true, // NEW: Has delivery
                contactNumber = "9876543212" // NEW: Contact number for delivery
            ),
            Shop(
                id = "4",
                name = "Paratha House",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/f_webp,q_auto:good/v1748772888/IMG20250528125036_yvc6gw.jpg",
                cuisine = "North Indian",
                address = "Boys Hostel, Near NC-1",
                location = "NC Boys",
                rating = 4.4,
                deliveryTime = "12-20 min",
                popularItems = listOf("Aloo Paratha", "Paneer Paratha", "Lassi"),
                menuItems = getParathaHouseMenuItems()
            ),
            Shop(
                id = "5",
                name = "Baker'z Hub",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/f_webp,q_auto:good/bakershub_bbcinu.png",
                cuisine = "Bakery",
                address = "Boys Hostel, in NC-2",
                location = "NC Boys",
                rating = 4.1,
                deliveryTime = "20-30 min",
                popularItems = listOf("Black Forest Cake", "Garlic Bread", "Cookies"),
                menuItems = getBakerzHubMenuItems(),
                contactNumber = "8947000006"
            ),
            Shop(
                id = "6",
                name= "Fresh Juice Bar & Fruit Shop",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/w_1000,h_1000,f_webp,q_auto:good/v1754172829/freshFriute_jn7wog.jpg",
                cuisine = "Juice Bar",
                address = "Near B3",
                location = "North Campus",
                rating = 4.3,
                deliveryTime = "10 min",
                popularItems = listOf(""),
                menuItems = getJuiceBarMenuItems()
            ),
            Shop(
                id = "7",
                name= "Big Million Cafe",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/f_webp,q_auto:good/v1748772865/IMG20250528122528_lzjhpw.jpg",
                cuisine = "Cafe",
                address = "Near B3",
                location = "North Campus",
                rating = 4.0,
                deliveryTime = "10 min",
                popularItems = listOf(""),
                menuItems = getBigMillionCafeMenuItems(),
                hasDelivery = true,
                contactNumber = "9877131643"
            ),
            Shop(
                id = "8",
                name= "SINGH'S The Bakery Food Court",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/f_webp,q_auto:good/v1748772881/IMG20250528122720_ty000j.jpg",
                cuisine = "Street Food",
                address = "Near C3",
                location = "North Campus",
                rating = 4.2,
                deliveryTime = "10 min",
                popularItems = listOf(""),
                menuItems = getSinghBakeryMenuItems()
            ),
            Shop(
                id = "9",
                name= "Bunker's Coffee Corner",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/f_webp,q_auto:good/v1748772852/IMG20250528122735_nsd2y7.jpg",
                cuisine = "Cafe",
                address = "Near C3",
                location = "North Campus",
                rating = 4.1,
                deliveryTime = "10 min",
                popularItems = listOf(""),
                menuItems = getBunkerCoffeeMenuItems(),
                hasDelivery = true,
                contactNumber = "9872428001"

            ),
            Shop(
                id = "10",
                name= "MHC Food Court",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/f_webp,q_auto:good/v1748772867/IMG20250528122814_a2wukx.jpg",
                cuisine = "Street Food",
                address = "Near C3",
                location = "North Campus",
                rating = 3.9,
                deliveryTime = "10 min",
                popularItems = listOf(""),
                menuItems = getMHCMenuItems()
            ),
            Shop(
                id = "11",
                name= "THE SAMOSA EXPRESS",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/f_webp,q_auto:good/v1748772869/IMG20250528123024_iqlyta.jpg",
                cuisine = "Street Food",
                address = "Near C1, Near Basketball Court",
                location = "North Campus",
                rating = 4.2,
                deliveryTime = "10 min",
                popularItems = listOf(""),
                menuItems = getSamosaExpressMenuItems()
            ),
            Shop(
                id = "12",
                name= "Taste Of Italy",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/f_webp,q_auto:good/v1748772859/IMG20250528123324_x1ip9f.jpg",
                cuisine = "Pizza",
                address = "Food Republic, In C1 ",
                location = "North Campus",
                rating = 4.2,
                deliveryTime = "10 min",
                popularItems = listOf(""),
                menuItems = getTasteOfItalyMenuItems()
            ),
            Shop(
                id = "13",
                name= "Venky's",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/f_webp,q_auto:good/v1748772863/IMG20250528123424_k4rqi9.jpg",
                cuisine = "Fast Food",
                address = "Food Republic, In C1 ",
                location = "North Campus",
                rating = 4.0,
                deliveryTime = "10 min",
                popularItems = listOf(""),
                menuItems = getVenkyMenuItems()
            ),
            Shop(
                id = "14",
                name= "Chai Sutta Bar",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/f_webp,q_auto:good/v1748772853/IMG20250528123640_onywmx.jpg",
                cuisine = "Cafe",
                address = "Food Republic, In C1 ",
                location = "North Campus",
                rating = 4.4,
                deliveryTime = "10 min",
                popularItems = listOf(""),
                menuItems = getChaiSuttaBarMenuItems()
            ),
            Shop(
                id = "15",
                name= "Major Chang's",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/f_webp,q_auto:good/v1748772861/IMG20250528123754_d5ahwq.jpg",
                cuisine = "Fast Food",
                address = "Food Republic, In C1 ",
                location = "North Campus",
                rating = 4.2,
                deliveryTime = "10 min",
                popularItems = listOf(""),
                menuItems = getMajorChangMenuItems()
            ),
            Shop(
                id = "16",
                name= "Golden Fork",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/f_webp,q_auto:good/v1748772862/IMG20250528123900_ugck1n.jpg",
                cuisine = "Fast Food",
                address = "Food Republic, In C1 ",
                location = "North Campus",
                rating = 4.1,
                deliveryTime = "10 min",
                popularItems = listOf(""),
                menuItems = getGoldenForkMenuItems()
            ),
            Shop(
                id = "17",
                name = "Zaika",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/f_webp,q_auto:good/v1748772855/IMG20250528130850_ekmfmj.jpg",
                cuisine = "North Indian",
                address = "Boys Hostel, in NC-4",
                location = "NC Boys",
                rating = 4.4,
                deliveryTime = "20-30 min",
                popularItems = listOf(""),
                menuItems = getZaikaMenuItems(),
                contactNumber = "8360292356"
            ),
            Shop(
                id = "18",
                name = "X Burgers",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/f_webp,q_auto:good/v1749126475/WhatsApp_Image_2025-06-04_at_17.13.27_eb2428c8_rchicl.jpg",
                cuisine = "Cafe",
                address = "Near A1",
                location = "North Campus",
                rating = 4.2,
                deliveryTime = "20-30 min",
                popularItems = listOf(""),
                menuItems = getXBurgersMenuItems(),
                contactNumber = "9779888899"
            ),
            Shop(
                id = "19",
                name = "Chef's On Fire",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/f_webp,q_auto:good/v1749126474/WhatsApp_Image_2025-06-04_at_17.13.27_186fa2d8_k2tcnu.jpg",
                cuisine = "Cafe",
                address = "Near A1",
                location = "North Campus",
                rating = 4.2,
                deliveryTime = "20-30 min",
                popularItems = listOf(""),
                menuItems = getChefOnFireMenuItems(),
                contactNumber = "8585957788"

            ),
            Shop(
                id = "20",
                name = "Singh Bakers",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/f_webp,q_auto:good/v1749126474/WhatsApp_Image_2025-06-04_at_17.13.26_46e89a13_ccq8cy.jpg",
                cuisine = "North Indian",
                address = "Near A1",
                location = "North Campus",
                rating = 4.2,
                deliveryTime = "20-30 min",
                popularItems = listOf(""),
                menuItems = getSinghBakersMenuItems(),
                hasDelivery = true,
                contactNumber = "9779150172"
            ),
            Shop(
                id = "21",
                name = "Mummy Di Roti",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/f_webp,q_auto:good/v1749126474/WhatsApp_Image_2025-06-04_at_17.13.29_e959e205_rj745c.jpg",
                cuisine = "North Indian",
                address = "Near A1",
                location = "North Campus",
                rating = 4.2,
                deliveryTime = "20-30 min",
                popularItems = listOf(""),
                menuItems = getMummyDiRotiMenuItems(),
                hasDelivery = true,
                contactNumber = "7717281525"
            ),

            Shop(
                id = "22",
                name = "Hot & Cold",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/v1754172857/H_Cmain_uhqb6v.jpg",
                cuisine = "Cafe",
                address = "Near B3",
                location = "North Campus",
                rating = 4.2,
                deliveryTime = "20-30 min",
                popularItems = listOf(""),
                menuItems =  getHcHotAndColdMenuItems(),
                contactNumber = "9914282812"
            ),

            Shop(
                id = "23",
                name = "HR Cafe",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/v1754172861/HRMain_fojkwq.jpg",
                cuisine = "Cafe",
                address = "Near B3",
                location = "North Campus",
                rating = 4.2,
                deliveryTime = "20-30 min",
                popularItems = listOf(""),
                menuItems = getHRCafeMenuItems()
            ),

            Shop(
                id = "24",
                name = "Hunger Zone",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/f_webp,q_auto:good/v1754172866/HungerMain_sk7kyx.jpg",
                cuisine = "Fast Food",
                address = "Near B3",
                location = "North Campus",
                rating = 4.2,
                deliveryTime = "20-30 min",
                popularItems = listOf(""),
                menuItems = getHungerZoneMenuItems()
            ),
            Shop(
                id = "25",
                name = "Trending Food",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/v1754172874/TrendingMain_manbvr.jpg",
                cuisine = "North Indian",
                address = "Near B3",
                location = "North Campus",
                rating = 4.2,
                deliveryTime = "20-30 min",
                popularItems = listOf(""),
                menuItems = getTrendingMenuItems(),
                hasDelivery = true,
                contactNumber = "8901585585"
            ),

            Shop(
                id = "27",
                name = "PD Tibet Kitchen",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/f_webp,q_auto:good/v1754172868/PDTibetKitchen_liozx6.jpg",
                cuisine = "Fast Food",
                address = "Near B3",
                location = "North Campus",
                rating = 4.2,
                deliveryTime = "20-30 min",
                popularItems = listOf(""),
                menuItems = getTibetKitchenMenuItems(),
                contactNumber = "6280697283"
            ),

            Shop(
                id = "28",
                name = "Unique Foods",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/f_webp,q_auto:good/v1754172876/UniqueFoods_e5wnfk.jpg",
                cuisine = "Fast Food",
                address = "Near B3",
                location = "North Campus",
                rating = 4.2,
                deliveryTime = "20-30 min",
                popularItems = listOf(""),
                menuItems = getUniqueFoodsMenuItems(),
                contactNumber = "8453737379"
            ),

        Shop(
            id = "29",
            name = "Vikas Confectionery",
            imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/v1754215160/VikasConfectionery_brw6eu.jpg",
            cuisine = "Fast Food",
            address = "Near A3",
            location = "North Campus",
            rating = 4.2,
            deliveryTime = "20-30 min",
            popularItems = listOf(""),
            menuItems = getVikasConfectioneryMenuItems(),
            hasDelivery = true,
            contactNumber = "9316170925"
        ),

            Shop(
                id = "30",
                name = "Lords Food",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/v1754215219/lordsFood_cxjopq.jpg",
                cuisine = "Fast Food",
                address = "Near A3",
                location = "North Campus",
                rating = 4.2,
                deliveryTime = "20-30 min",
                popularItems = listOf(""),
                menuItems = getLordsFoodMenuItems(),
                hasDelivery = true,
                contactNumber = "8427308675"
            ),

            Shop(
                id = "31",
                name = "Creative Foods",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/v1754215291/creativeFood_grbka6.jpg",
                cuisine = "Fast Food",
                address = "Near A3",
                location = "North Campus",
                rating = 4.2,
                deliveryTime = "20-30 min",
                popularItems = listOf(""),
                menuItems = getCreativeCafeMenuItems()
            ),

            Shop(
                id = "32",
                name = "Chai Thekha",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/v1754215339/ChaiThekha_a0hxaa.jpg",
                cuisine = "Cafe",
                address = "Near A3",
                location = "North Campus",
                rating = 4.2,
                deliveryTime = "20-30 min",
                popularItems = listOf(""),
                menuItems = getChaiThekhaMenuItems()
            ),

            Shop(
                id = "32",
                name = "Buddies Multi Cuisine",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/v1754215399/buddies_czg4l5.jpg",
                cuisine = "North Indian",
                address = "Near A3",
                location = "North Campus",
                rating = 4.2,
                deliveryTime = "20-30 min",
                popularItems = listOf(""),
                menuItems = getBuddiesMenuItems()
            ),

            Shop(
                id = "33",
                name = "Crunchy Bite",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/v1754215538/crunchyBite_e3kkkq.jpg",
                cuisine = "North Indian",
                address = "Near A3",
                location = "North Campus",
                rating = 4.2,
                deliveryTime = "20-30 min",
                popularItems = listOf(""),
                menuItems = getCrunchyMenuItems(),
                hasDelivery = true,
                contactNumber = "8146751320"
            ),

            Shop(
                id = "34",
                name = "Eat & Smile",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/v1754656873/eatmain_mhf476.jpg",
                cuisine = "North Indian",
                address = "Boys Hostel, in NC-6",
                location = "NC Boys",
                rating = 4.5,
                deliveryTime = "10-20 min",
                popularItems = listOf(""),
                menuItems = getEatAndSmileMenuItems(),
                hasDelivery = true, // NEW: No delivery
                contactNumber = "7988511867" // NEW: But has contact number for other inquiries
            ),

            Shop(
                id = "35",
                name = "Barkat Food",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/v1754656909/barkatFood_n5vkyk.jpg",
                cuisine = "North Indian",
                address = "Boys Hostel, in Zakir-C",
                location = "Zakir Boys",
                rating = 4.5,
                deliveryTime = "10-20 min",
                popularItems = listOf(""),
                menuItems = getBarkatFoodMenuItems(),
                hasDelivery = false, // NEW: No delivery
                contactNumber = "7056853131" // NEW: But has contact number for other inquiries
            ),


            Shop(
                id = "36",
                name = "Food Castel",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/v1754656932/foodcastelmain_gfoz3x.jpg",
                cuisine = "North Indian",
                address = "Boys Hostel, in NC-5",
                location = "NC Boys",
                rating = 4.5,
                deliveryTime = "10-20 min",
                popularItems = listOf(""),
                menuItems = getFoodCastelMenuItems(),
                hasDelivery = false, // NEW: No delivery
                contactNumber = "9988619177" // NEW: But has contact number for other inquiries
            ),
            Shop(
                id = "37",
                name = "Food Junction",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/v1754656950/foodjunctionMain_phphhg.jpg",
                cuisine = "North Indian",
                address = "Boys Hostel, in Zakir-A",
                location = "Zakir Boys",
                rating = 4.5,
                deliveryTime = "10-20 min",
                popularItems = listOf(""),
                menuItems = getFoodJunctionMenuItems()
            ),
            Shop(
                id = "38",
                name = "King Cafe",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/v1754656986/kingmain_l2vubi.jpg",
                cuisine = "North Indian",
                address = "Boys Hostel, in Zakir-B",
                location = "Zakir Boys",
                rating = 4.5,
                deliveryTime = "10-20 min",
                popularItems = listOf(""),
                menuItems = getKingCafeMenuItems(),
                hasDelivery = false, // NEW: No delivery
                contactNumber = "7903199299" // NEW: But has contact number for other inquiries
            ),

            Shop(
                id = "39",
                name = "Handi Biryani",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/v1754657015/handimain_abbwah.jpg",
                cuisine = "North Indian",
                address = "Boys Hostel, in Zakir-D",
                location = "Zakir Boys",
                rating = 4.5,
                deliveryTime = "10-20 min",
                popularItems = listOf(""),
                menuItems = getHandiBiryaniMenuItems()
            ),

            Shop(
                id = "40",
                name = "Insta Food",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/v1754657034/instafood_sue63l.jpg",
                cuisine = "North Indian",
                address = "Boys Hostel, near NC-1",
                location = "NC Boys",
                rating = 4.5,
                deliveryTime = "10-20 min",
                popularItems = listOf(""),
                menuItems = getInstaFoodMenuItems(),
                contactNumber = "9855424863"
            ),

            Shop(
                id = "41",
                name = "Campus Cafe",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/v1754657041/IMG-20250727-WA0145_bjfywl.jpg",
                cuisine = "North Indian",
                address = "near Gate 2 , Fountain",
                location = "Near Fountain",
                rating = 4.5,
                deliveryTime = "10-20 min",
                popularItems = listOf(""),
                menuItems = getCampusCafeMenuItems()
            ),

            Shop(
                id = "42",
                name = "Horse Shoe Cafe",
                imageUrl = "https://res.cloudinary.com/dv5f6ctbx/image/upload/v1754657073/IMG-20250727-WA0143_evwi3x.jpg",
                cuisine = "North Indian",
                address = "near Gate 2 , Fountain",
                location = "Near Fountain",
                rating = 4.5,
                deliveryTime = "10-20 min",
                popularItems = listOf(""),
                menuItems = getHorseShoeCafeMenuItems()
            ),



        )
    }

    private fun getHorseShoeCafeMenuItems(): List<MenuItem> {
        return listOf(
            // --- Burgers ---
            MenuItem("hs1", "Veg Burger + Fries", "Vegetarian burger served with fries", 75.0, "", "Burger", true),
            MenuItem("hs2", "Chicken Burger + Fries", "Chicken burger served with fries", 95.0, "", "Burger", false),
            MenuItem("hs3", "Las Vegas Burger + Fries", "Special Las Vegas style burger with fries", 110.0, "", "Burger", true),
            MenuItem("hs4", "Cheese Slice Add On (Burger)", "Add cheese slice to burger", 20.0, "", "Burger", true),

            // --- Sandwich ---
            MenuItem("hs5", "Veg Cole Slaw Jumbo", "Jumbo sandwich with veg coleslaw", 80.0, "", "Sandwich", true),
            MenuItem("hs6", "Chicken Sandwich", "Chicken filled sandwich", 80.0, "", "Sandwich", false),
            MenuItem("hs7", "Egg Sandwich", "Egg filled sandwich", 70.0, "", "Sandwich", false),
            MenuItem("hs8", "Corn Sandwich", "Sweet corn filled sandwich", 70.0, "", "Sandwich", true),
            MenuItem("hs9", "Paneer Sandwich", "Paneer stuffed sandwich", 80.0, "", "Sandwich", true),
            MenuItem("hs10", "Special Sandwich", "Specialty sandwich", 100.0, "", "Sandwich", true),
            MenuItem("hs11", "Cheese Slice Add On (Sandwich)", "Add cheese slice to sandwich", 20.0, "", "Sandwich", true),

            // --- Wraps ---
            MenuItem("hs12", "Veg Wrap", "Vegetable stuffed wrap", 80.0, "", "Wraps", true),
            MenuItem("hs13", "Chicken Wrap", "Chicken stuffed wrap", 100.0, "", "Wraps", false),
            MenuItem("hs14", "Egg Wrap", "Egg stuffed wrap", 80.0, "", "Wraps", false),
            MenuItem("hs15", "Paneer Wrap", "Paneer stuffed wrap", 90.0, "", "Wraps", true),
            MenuItem("hs16", "Special Wrap", "Specialty wrap", 110.0, "", "Wraps", true),

            // --- Fries ---
            MenuItem("hs17", "French Fries", "Crispy french fries", 75.0, "", "Fries", true),
            MenuItem("hs18", "Peri Peri Fries", "Spicy peri peri seasoned fries", 100.0, "", "Fries", true),
            MenuItem("hs19", "Cream Fries", "Fries topped with cream", 100.0, "", "Fries", true),
            MenuItem("hs20", "Classic Fries", "Classic style fries", 85.0, "", "Fries", true),

            // --- Smoothie ---
            MenuItem("hs21", "Berry with Banana", "Berry and banana combo smoothie", 75.0, "", "Smoothie", true),
            MenuItem("hs22", "Strawberry with Banana", "Strawberry and banana smoothie", 95.0, "", "Smoothie", true),

            // --- Ice Cream Shakes ---
            MenuItem("hs23", "Oreo Shake", "Oreo cookie milkshake", 90.0, "", "Ice Cream Shakes", true),
            MenuItem("hs24", "Kitkat Shake", "Kitkat chocolate shake", 90.0, "", "Ice Cream Shakes", true),
            MenuItem("hs25", "Tuti-Fruity Shake", "Tuti fruity ice cream shake", 90.0, "", "Ice Cream Shakes", true),
            MenuItem("hs26", "Mango Shake", "Mango flavored shake", 90.0, "", "Ice Cream Shakes", true),
            MenuItem("hs27", "Strawberry Banana", "Strawberry-banana shake", 90.0, "", "Ice Cream Shakes", true),
            MenuItem("hs28", "Black Currant Shake", "Black currant flavored shake", 90.0, "", "Ice Cream Shakes", true),
            MenuItem("hs29", "Pineapple Chocolate Shake", "Pineapple-chocolate shake", 90.0, "", "Ice Cream Shakes", true),
            MenuItem("hs30", "Choco Crunch Shake", "Chocolate crunchy shake", 90.0, "", "Ice Cream Shakes", true),
            MenuItem("hs31", "Kesar-Elaichi-Gulkand Shake", "Shake with saffron, cardamom, gulkand", 90.0, "", "Ice Cream Shakes", true),
            MenuItem("hs32", "Brownie Shake", "Chocolate brownie blended shake", 90.0, "", "Ice Cream Shakes", true),

            // --- Hot Coffee ---
            MenuItem("hs33", "Spl. Espresso", "Special espresso coffee", 30.0, "", "Hot Coffee", true),
            MenuItem("hs34", "Cappuccino", "Frothy cappuccino", 35.0, "", "Hot Coffee", true),
            MenuItem("hs35", "American Latte", "Smooth latte coffee", 35.0, "", "Hot Coffee", true),
            MenuItem("hs36", "Cafe Mocha Coffee", "Mocha flavored coffee", 50.0, "", "Hot Coffee", true),

            // --- Cold Coffee ---
            MenuItem("hs37", "Cold Coffee", "Classic cold coffee", 70.0, "", "Cold Coffee", true),
            MenuItem("hs38", "Americano Black Cold Coffee", "Black cold Americano coffee", 70.0, "", "Cold Coffee", true),
            MenuItem("hs39", "Brownie Cold Coffee", "Brownie blended cold coffee", 100.0, "", "Cold Coffee", true),
            MenuItem("hs40", "Hazelnut Cold Coffee", "Hazelnut flavored cold coffee", 100.0, "", "Cold Coffee", true),

            // --- Mocktails ---
            MenuItem("hs41", "Blue Lagoon", "Refreshing lemony blue mocktail", 70.0, "", "Mocktails", true),
            MenuItem("hs42", "Virgin Mojito", "Classic lime mint mocktail", 70.0, "", "Mocktails", true),
            MenuItem("hs43", "Passion Fruit, Grenadine", "Passion fruit & grenadine mix mocktail", 90.0, "", "Mocktails", true),
            MenuItem("hs44", "Water Melon, Mango Mirch", "Watermelon or mango chili mocktail", 90.0, "", "Mocktails", true),
            MenuItem("hs45", "Orange Masala, Rose, Kesar", "Orange masala, rose, kesar mocktail", 90.0, "", "Mocktails", true),
            MenuItem("hs46", "Elaichi, Jaljeera, Bubble Gum", "Cardamom, jaljeera, or bubblegum mocktail", 90.0, "", "Mocktails", true),
            MenuItem("hs47", "Virgin Pina, Strawberry", "Virgin pina colada or strawberry", 90.0, "", "Mocktails", true),
            MenuItem("hs48", "Black Berry, Rose", "Blackberry or rose mocktail", 90.0, "", "Mocktails", true),

            // --- Ice Cream Cup/Cones ---
            MenuItem("hs49", "Vanilla (1 Scoop)", "Single scoop vanilla ice cream", 30.0, "", "Ice Cream Cup/Cones", true),
            MenuItem("hs50", "Chocolate (1 Scoop)", "Single scoop chocolate ice cream", 30.0, "", "Ice Cream Cup/Cones", true),
            MenuItem("hs51", "Ice Cream Made With Syrup (All Flavours)", "Vanilla ice cream with syrup", 60.0, "", "Ice Cream Cup/Cones", true),

            // --- Ice Tea ---
            MenuItem("hs52", "Lemon Ice Tea", "Chilled lemon flavor ice tea", 45.0, "", "Ice Tea", true),
            MenuItem("hs53", "Peach Ice Tea", "Chilled peach flavor ice tea", 44.0, "", "Ice Tea", true),
            MenuItem("hs54", "Masala Ice Tea", "Spiced masala iced tea", 44.0, "", "Ice Tea", true),
            MenuItem("hs55", "Fruit Beer", "Non-alcoholic fruit flavored beer", 60.0, "", "Ice Tea", true),

            // --- Momos ---
            MenuItem("hs56", "Veg Steam Momos", "Steamed vegetarian momos", 80.0, "", "Momos", true),
            MenuItem("hs57", "Veg Fried Momos", "Fried vegetarian momos", 90.0, "", "Momos", true),
            MenuItem("hs58", "Veg Momos in Red/White Sauce", "Veg momos tossed in sauce", 100.0, "", "Momos", true),
            MenuItem("hs59", "Chicken Steam Momos", "Steamed chicken momos", 100.0, "", "Momos", false),
            MenuItem("hs60", "Chicken Fried Momos", "Fried chicken momos", 110.0, "", "Momos", false),
            MenuItem("hs61", "Chicken Momos in Red/White Sauce", "Chicken momos in sauce", 120.0, "", "Momos", false),

            // --- Chinese ---
            MenuItem("hs62", "Veg Chowmein", "Vegetarian stir fry noodles", 80.0, "", "Chinese", true),
            MenuItem("hs63", "Chicken Chowmein", "Chicken stir fry noodles", 100.0, "", "Chinese", false),
            MenuItem("hs64", "Veg Hakka Noodles", "Hakka style veg noodles", 80.0, "", "Chinese", true),
            MenuItem("hs65", "Chicken Hakka Noodles", "Hakka style chicken noodles", 100.0, "", "Chinese", false),
            MenuItem("hs66", "Veg Fried Rice", "Vegetarian fried rice", 90.0, "", "Chinese", true),
            MenuItem("hs67", "Chicken Fried Rice", "Chicken fried rice", 100.0, "", "Chinese", false),
            MenuItem("hs68", "Egg Fried Rice", "Rice tossed with egg", 90.0, "", "Chinese", false),
            MenuItem("hs69", "Honey Chilly Potato", "Crispy potato in sweet spicy sauce", 90.0, "", "Chinese", true),
            MenuItem("hs70", "Honey Chilly Cauliflower", "Sweet chili cauliflower starter", 90.0, "", "Chinese", true),
            MenuItem("hs71", "Chilly Paneer", "Paneer in chili sauce", 100.0, "", "Chinese", true),
            MenuItem("hs72", "Chinese Corn", "Spicy Chinese-style corn", 90.0, "", "Chinese", true),

            // --- Maggi ---
            MenuItem("hs73", "Masala Maggi", "Classic masala Maggi", 80.0, "", "Maggi", true),
            MenuItem("hs74", "Veg Maggi", "Vegetable Maggi noodles", 75.0, "", "Maggi", true),
            MenuItem("hs75", "Chicken Maggi", "Chicken Maggi noodles", 95.0, "", "Maggi", false),
            MenuItem("hs76", "Cheese Maggi", "Cheese Maggi noodles", 100.0, "", "Maggi", true),
            MenuItem("hs77", "Egg Maggi", "Maggi noodles mixed with egg", 90.0, "", "Maggi", false),
            MenuItem("hs78", "Classic Maggi", "Classic plain Maggi", 70.0, "", "Maggi", true),

            // --- Salads ---
            MenuItem("hs79", "Veg Salad", "Fresh vegetable salad", 100.0, "", "Salads", true),
            MenuItem("hs80", "Chicken Salad", "Chicken salad with veggies", 120.0, "", "Salads", false),
            MenuItem("hs81", "Paneer Salad", "Paneer and veg salad", 120.0, "", "Salads", true),
            MenuItem("hs82", "Corn Salad", "Sweet corn salad", 120.0, "", "Salads", true),

            // --- Soup Seasonal ---
            MenuItem("hs83", "Manchow Soup", "Spicy Indo-Chinese soup", 70.0, "", "Soup Seasonal", true),
            MenuItem("hs84", "Sweet Corn Soup", "Sweet corn thick soup", 75.0, "", "Soup Seasonal", true),
            MenuItem("hs85", "Tomato Sour Soup", "Tangy tomato based soup", 75.0, "", "Soup Seasonal", true),
            MenuItem("hs86", "Chicken Clear Soup", "Light chicken broth soup", 90.0, "", "Soup Seasonal", false),
            MenuItem("hs87", "WHEA Available", "Seasonal soup (WHEA)", 80.0, "", "Soup Seasonal", true),

            // --- Pizza (Made Fresh on Order) ---
            MenuItem("hs88", "Margarita Pizza", "Classic cheese pizza", 120.0, "", "Pizza", true),
            MenuItem("hs89", "Paneer Pizza", "Pizza with paneer toppings", 120.0, "", "Pizza", true),
            MenuItem("hs90", "Paneer Spicy Pizza", "Spicy paneer pizza", 130.0, "", "Pizza", true),
            MenuItem("hs91", "Paneer Tikka Pizza", "Tandoori paneer pizza", 130.0, "", "Pizza", true),
            MenuItem("hs92", "Chicken Salami Pizza", "Chicken salami topped pizza", 140.0, "", "Pizza", false),
            MenuItem("hs93", "Chicken Patty Pizza", "Chicken patty on pizza", 140.0, "", "Pizza", false),

            // --- Pasta ---
            MenuItem("hs94", "Red Sauce Pasta", "Pasta with red sauce", 80.0, "", "Pasta", true),
            MenuItem("hs95", "White Sauce Pasta", "Pasta with creamy white sauce", 80.0, "", "Pasta", true),
            MenuItem("hs96", "Remix Pasta", "Mixed sauce pasta", 90.0, "", "Pasta", true),

            // --- Lassi ---
            MenuItem("hs97", "Sweet/Namkeen Lassi", "Sweet or salty thick lassi", 60.0, "", "Lassi", true),
            MenuItem("hs98", "Flavoured Lassi", "Fruit flavored lassi", 70.0, "", "Lassi", true),

            // --- Tea ---
            MenuItem("hs99", "Masala Chai", "Spiced Indian tea", 20.0, "", "Tea", true),

            // --- Breakfast ---
            MenuItem("hs100", "Bread Omelette", "Toasted bread with egg omelette", 50.0, "", "Breakfast", false),
            MenuItem("hs101", "Parantha Bhurji", "Parantha served with scrambled eggs", 75.0, "", "Breakfast", false),
            MenuItem("hs102", "Stuffed Parantha", "Parantha stuffed with filling", 60.0, "", "Breakfast", true),
            MenuItem("hs103", "Curd Add On", "Add curd to breakfast", 20.0, "", "Breakfast", true),

            // --- Indian ---
            MenuItem("hs104", "Aloo Parantha", "Potato stuffed paratha", 65.0, "", "Indian", true),
            MenuItem("hs105", "Mixed Parantha (Seasonal)", "Mixed vegetable paratha", 75.0, "", "Indian", true),
            MenuItem("hs106", "Jeera Chawal", "Rice with cumin seeds", 50.0, "", "Indian", true),
            MenuItem("hs107", "Rajma Chawal", "Kidney beans curry with rice", 80.0, "", "Indian", true),
            MenuItem("hs108", "Dal Chawal", "Lentil curry with rice", 75.0, "", "Indian", true),
            MenuItem("hs109", "Roti Chole", "Roti bread with chole curry", 80.0, "", "Indian", true),
            MenuItem("hs110", "Stuffed Paneer Paratha", "Paratha stuffed with paneer", 85.0, "", "Indian", true),
            MenuItem("hs111", "Curd Add On (Indian)", "Add curd to Indian dish", 20.0, "", "Indian", true),

            // --- Special Sundae ---
            MenuItem("hs112", "Brownie", "Chocolate brownie sundae", 70.0, "", "Special Sundae", true),
            MenuItem("hs113", "Brownie with Chocolate Sauce", "Brownie with rich chocolate sauce", 80.0, "", "Special Sundae", true),
            MenuItem("hs114", "Fruit Sundae", "Fruit based sundae", 60.0, "", "Special Sundae", true),
            MenuItem("hs115", "Muffin", "Plain muffin", 60.0, "", "Special Sundae", true),
            MenuItem("hs116", "Muffin with Hot Chocolate Sauce", "Muffin served with chocolate sauce", 70.0, "", "Special Sundae", true),
            MenuItem("hs117", "Muffin with Ice Cream", "Muffin served with ice cream", 80.0, "", "Special Sundae", true),

            // --- Misc. ---
            MenuItem("hs118", "Water Bottle", "Packaged water bottle", 0.0, "", "Misc.", true), // MRP
            MenuItem("hs119", "Cold Drinks", "Various cold beverages", 0.0, "", "Misc.", true), // MRP
            MenuItem("hs120", "Fruit Beer", "Fruit flavored non-alcoholic beer", 60.0, "", "Misc.", true),
            MenuItem("hs121", "Cheese Toast", "Grilled cheese toast", 60.0, "", "Misc.", true)
        )
    }



    private fun getCampusCafeMenuItems(): List<MenuItem> {
        return listOf(
            // --- Hot Favourites ---
            MenuItem("cc1", "Nescafe Regular", "Classic instant coffee", 20.0, "", "Hot Favourites", true),
            MenuItem("cc2", "Cappuchine", "Frothy cappuccino", 30.0, "", "Hot Favourites", true),
            MenuItem("cc3", "Cafe Latte", "Espresso with steamed milk", 30.0, "", "Hot Favourites", true),
            MenuItem("cc4", "Mochaccino", "Coffee with chocolate flavor", 40.0, "", "Hot Favourites", true),
            MenuItem("cc5", "Black Coffee", "Classic black coffee", 20.0, "", "Hot Favourites", true),
            MenuItem("cc6", "Hot Chocolate", "Rich hot chocolate beverage", 30.0, "", "Hot Favourites", true),
            MenuItem("cc7", "Elaichi Tea", "Cardamom flavoured tea", 15.0, "", "Hot Favourites", true),
            MenuItem("cc8", "Lemon Tea", "Lemon flavoured tea", 20.0, "", "Hot Favourites", true),
            MenuItem("cc9", "Tea (Tea Bag)", "Tea prepared with tea bag", 15.0, "", "Hot Favourites", true),
            MenuItem("cc10", "Green Tea", "Green tea beverage", 20.0, "", "Hot Favourites", true),

            // --- Cool Refreshers ---
            MenuItem("cc11", "Frappe (Cold Coffee) (S)", "Small size cold coffee frappe", 50.0, "", "Cool Refreshers", true),
            MenuItem("cc12", "Frappe (Cold Coffee) (L)", "Large size cold coffee frappe", 80.0, "", "Cool Refreshers", true),
            MenuItem("cc13", "Lemon Ice Tea (S)", "Small lemon ice tea", 40.0, "", "Cool Refreshers", true),
            MenuItem("cc14", "Lemon Ice Tea (L)", "Large lemon ice tea", 50.0, "", "Cool Refreshers", true),
            MenuItem("cc15", "Masala Lemon Ice Tea", "Spiced lemon ice tea", 50.0, "", "Cool Refreshers", true),
            MenuItem("cc16", "Mojito Ice Tea", "Mojito flavoured iced tea", 60.0, "", "Cool Refreshers", true),
            MenuItem("cc17", "Chocolate Shake (S)", "Small chocolate milkshake", 60.0, "", "Cool Refreshers", true),
            MenuItem("cc18", "Chocolate Shake (L)", "Large chocolate milkshake", 90.0, "", "Cool Refreshers", true),
            MenuItem("cc19", "Choco Frappe", "Chocolate cold coffee frappe", 50.0, "", "Cool Refreshers", true),
            MenuItem("cc20", "Kit Kat Shake (S)", "Small Kit Kat milkshake", 70.0, "", "Cool Refreshers", true),
            MenuItem("cc21", "Kit Kat Shake (L)", "Large Kit Kat milkshake", 110.0, "", "Cool Refreshers", true),
            MenuItem("cc22", "Ice Cream Frappe (S)", "Small frappe blended with ice cream", 80.0, "", "Cool Refreshers", true),
            MenuItem("cc23", "Ice Cream Frappe (L)", "Large frappe blended with ice cream", 120.0, "", "Cool Refreshers", true),

            // --- Special Roasted Beans Coffee ---
            MenuItem("cc24", "Beans Cappuccino", "Cappuccino made from fresh beans", 50.0, "", "Special Roasted Beans Coffee", true),
            MenuItem("cc25", "Beans Cappuccino Strong", "Strong bean cappuccino", 65.0, "", "Special Roasted Beans Coffee", true),
            MenuItem("cc26", "Beans Tapri Coffee", "Street-style beans coffee", 50.0, "", "Special Roasted Beans Coffee", true),
            MenuItem("cc27", "Beans Latte", "Latte with espresso beans", 50.0, "", "Special Roasted Beans Coffee", true),
            MenuItem("cc28", "Beans Chococino", "Chocolate cappuccino with beans", 50.0, "", "Special Roasted Beans Coffee", true),
            MenuItem("cc29", "Beans Cafe Mocha", "Mocha coffee made from beans", 50.0, "", "Special Roasted Beans Coffee", true),
            MenuItem("cc30", "Beans Hot Chocolate", "Beans style hot chocolate", 50.0, "", "Special Roasted Beans Coffee", true),
            MenuItem("cc31", "Beans Americano", "Americano beans coffee", 50.0, "", "Special Roasted Beans Coffee", true),
            MenuItem("cc32", "Beans Espresso", "Espresso shot from beans", 30.0, "", "Special Roasted Beans Coffee", true),
            MenuItem("cc33", "Beans Espresso Mojito", "Mojito-style beans espresso", 60.0, "", "Special Roasted Beans Coffee", true),
            MenuItem("cc34", "Beans Flat White", "Flat white coffee with beans", 60.0, "", "Special Roasted Beans Coffee", true),

            // --- Desserts ---
            MenuItem("cc35", "Brownie with Ice Cream", "Chocolate brownie served with ice cream", 80.0, "", "Desserts", true),
            MenuItem("cc36", "Choco Pudding", "Chocolate pudding dessert", 50.0, "", "Desserts", true),
            MenuItem("cc37", "Choco Lava", "Chocolate molten lava cake", 60.0, "", "Desserts", true),
            MenuItem("cc38", "Heart Brownie", "Heart shaped chocolate brownie", 60.0, "", "Desserts", true),
            MenuItem("cc39", "Donuts", "Assorted donuts", 50.0, "", "Desserts", true),
            MenuItem("cc40", "Brownie", "Classic chocolate brownie", 50.0, "", "Desserts", true),
            MenuItem("cc41", "Truffle", "Chocolate truffle dessert", 60.0, "", "Desserts", true),

            // --- Range of Maggi ---
            MenuItem("cc42", "Maggi Masala", "Classic masala maggi noodles", 35.0, "", "Range of Maggi", true),
            MenuItem("cc43", "Veggi Delight", "Vegetable maggi delight", 55.0, "", "Range of Maggi", true),
            MenuItem("cc44", "Student Corn", "Corn maggi noodles", 60.0, "", "Range of Maggi", true),
            MenuItem("cc45", "Chesse Delight", "Cheese maggi noodles", 70.0, "", "Range of Maggi", true),
            MenuItem("cc46", "Atta Maggi", "Whole wheat maggi noodles", 55.0, "", "Range of Maggi", true),
            MenuItem("cc47", "Schezwam Maggi", "Spicy schezwan maggi noodles", 70.0, "", "Range of Maggi", true),
            MenuItem("cc48", "Punjabi Tadka Maggi", "Punjabi style spiced maggi", 70.0, "", "Range of Maggi", true),
            MenuItem("cc49", "Chilly Garlic Butter Maggi", "Maggi with chilli, garlic, butter", 70.0, "", "Range of Maggi", true),
            MenuItem("cc50", "Tandoori Masala Maggi", "Tandoori spiced maggi noodles", 70.0, "", "Range of Maggi", true),
            MenuItem("cc51", "Shahi Paneer Maggi", "Paneer maggi with creamy gravy", 90.0, "", "Range of Maggi", true),
            MenuItem("cc52", "Red Sauce Pasta", "Pasta in tangy red sauce", 80.0, "", "Range of Maggi", true),
            MenuItem("cc53", "White Sauce Pasta", "Pasta in creamy white sauce", 80.0, "", "Range of Maggi", true),

            // --- Sandwiches / Fast Food ---
            MenuItem("cc54", "Corn Sandwich", "Sandwich stuffed with sweet corn", 40.0, "", "Sandwiches Fast Food", true),
            MenuItem("cc55", "Paneer Sandwich", "Paneer filled sandwich", 40.0, "", "Sandwiches Fast Food", true),
            MenuItem("cc56", "Veg Sandwich", "Vegetable sandwich", 40.0, "", "Sandwiches Fast Food", true),
            MenuItem("cc57", "Grilled Sandwich", "Grilled bread sandwich with vegetables", 60.0, "", "Sandwiches Fast Food", true),
            MenuItem("cc58", "Paneer Grilled Sandwich", "Grilled sandwich with paneer", 70.0, "", "Sandwiches Fast Food", true),
            MenuItem("cc59", "Panini Sandwich", "Vegetable panini sandwich", 70.0, "", "Sandwiches Fast Food", true),
            MenuItem("cc60", "Pizza Pocket (Two Piece)", "Two pieces of pizza pocket", 70.0, "", "Sandwiches Fast Food", true),
            MenuItem("cc61", "Veg Nuggets", "Deep fried vegetarian nuggets", 60.0, "", "Sandwiches Fast Food", true),
            MenuItem("cc62", "Chilli Potato Shots", "Spicy fried potato shots", 70.0, "", "Sandwiches Fast Food", true),
            MenuItem("cc63", "Smilies", "Potato smiley faces", 60.0, "", "Sandwiches Fast Food", true),
            MenuItem("cc64", "French Fries", "Crispy French fries", 60.0, "", "Sandwiches Fast Food", true),
            MenuItem("cc65", "Burger (Veg)", "Vegetarian burger", 40.0, "", "Sandwiches Fast Food", true),
            MenuItem("cc66", "Burger (Cheese)", "Veg burger with cheese", 50.0, "", "Sandwiches Fast Food", true),
            MenuItem("cc67", "Veg Rolls", "Vegetable roll wrap", 70.0, "", "Sandwiches Fast Food", true),
            MenuItem("cc68", "Paneer Rolls", "Roll stuffed with paneer", 60.0, "", "Sandwiches Fast Food", true),
            MenuItem("cc69", "Pasta Rolls", "Roll stuffed with pasta", 60.0, "", "Sandwiches Fast Food", true),
            MenuItem("cc70", "Cuppa Maggi", "Maggi served in a cup", 50.0, "", "Sandwiches Fast Food", true),
            MenuItem("cc71", "Chocolates", "Assorted chocolates", 50.0, "", "Sandwiches Fast Food", true),
            // 'Ready to Drinks (RTD)' and 'Water Bottles' show only 'MRP' for price
            // If you want to include them, set price as 0.0 or handle as special MRP items:
            // MenuItem("cc72", "Ready to Drinks (RTD)", "Ready to drink beverages", 0.0, "", "Sandwiches Fast Food", true),
            // MenuItem("cc73", "Water Bottles", "Packaged drinking water", 0.0, "", "Sandwiches Fast Food", true)
        )
    }


    private fun getInstaFoodMenuItems(): List<MenuItem> {
        return listOf(
            // JUICE (two rates for two sizes in image; using 70/80, 60/80, etc.)
            MenuItem("if1", "Mix Juice", "", 70.0, "", "Juice", true),
            MenuItem("if2", "Pineapple Juice", "", 70.0, "", "Juice", true),
            MenuItem("if3", "Mausami Juice", "", 70.0, "", "Juice", true),
            MenuItem("if4", "Orange Juice", "", 70.0, "", "Juice", true),
            MenuItem("if5", "Beet Root Juice", "", 80.0, "", "Juice", true),  // Corrected price (was 70, now 80)
            MenuItem("if6", "Watermelon Juice", "", 60.0, "", "Juice", true),
            MenuItem("if7", "Carrot Juice", "", 70.0, "", "Juice", true),

            // LASSI
            MenuItem("if8", "Sweet Lassi", "", 60.0, "", "Lassi", true),
            MenuItem("if9", "Mango Lassi", "", 70.0, "", "Lassi", true),
            MenuItem("if10", "Strawberry Lassi", "", 80.0, "", "Lassi", true), // Corrected price (was 70, now 80)
            MenuItem("if11", "Namkeen Lassi", "", 60.0, "", "Lassi", true),
            MenuItem("if12", "Chocolate Lassi", "", 80.0, "", "Lassi", true), // Corrected price (was 70, now 80)
            MenuItem("if13", "Rose Lassi", "", 80.0, "", "Lassi", true),      // Corrected price (was 70, now 80)

            // SHAKES (some have size/price options in image, use lowest rate for main menu; add if needed)
            MenuItem("if14", "Banana Shake", "", 50.0, "", "Shake", true),
            MenuItem("if15", "Papaya Shake", "", 60.0, "", "Shake", true),     // Corrected 'Papya'→'Papaya'
            MenuItem("if16", "Mango Shake", "", 60.0, "", "Shake", true),      // Corrected price (was 70)
            MenuItem("if17", "Oreo Shake", "", 70.0, "", "Shake", true),
            MenuItem("if18", "Kit-Kat Shake", "", 80.0, "", "Shake", true),
            MenuItem("if19", "Strawberry Shake", "", 60.0, "", "Shake", true), // Corrected price (was 70)
            MenuItem("if20", "Black Currant Shake", "", 70.0, "", "Shake", true),
            MenuItem("if21", "Blue Lagoon Shake", "", 70.0, "", "Shake", true), // Corrected capitalization
            MenuItem("if22", "Apple Shake", "", 60.0, "", "Shake", true),
            MenuItem("if23", "Guava Shake", "", 60.0, "", "Shake", true),     // Corrected price (was 70)
            MenuItem("if24", "Brownie Shake", "", 80.0, "", "Shake", true),
            MenuItem("if25", "Chocolate Shake", "", 60.0, "", "Shake", true), // Corrected price (was 70)
            MenuItem("if26", "Butterscotch Shake", "", 70.0, "", "Shake", true),
            MenuItem("if27", "Kesar Shake", "", 80.0, "", "Shake", true),
            MenuItem("if28", "Coffee Shake", "", 60.0, "", "Shake", true),    // Corrected price (was 70)
            MenuItem("if29", "Dry Fruit Shake", "", 100.0, "", "Shake", true),
            MenuItem("if30", "Badam Shake", "", 80.0, "", "Shake", true),
            MenuItem("if31", "Protein Shake", "", 100.0, "", "Shake", true),
            MenuItem("if32", "Apple Banana Shake", "", 80.0, "", "Shake", true),
            MenuItem("if33", "Special Shake", "", 80.0, "", "Shake", true),   // Added from image
            MenuItem("if34", "Chikoo Shake", "", 60.0, "", "Shake", true),   // Added from image

            // MAGGI
            MenuItem("if35", "Masala Maggi", "", 50.0, "", "Maggi", true),
            MenuItem("if36", "Veg Maggi", "", 60.0, "", "Maggi", true),
            MenuItem("if37", "Cheese Maggi", "", 70.0, "", "Maggi", true),
            MenuItem("if38", "Egg Maggi", "", 70.0, "", "Maggi", false),
            MenuItem("if39", "Paneer Maggi", "", 80.0, "", "Maggi", true),
            MenuItem("if40", "Chilli Maggi", "", 70.0, "", "Maggi", true),    // Added from image
            MenuItem("if41", "Mix Maggi", "", 80.0, "", "Maggi", true),       // Added from image

            // PASTA (added categories with correct prices)
            MenuItem("if42", "White Sauce Pasta", "", 70.0, "", "Pasta", true),
            MenuItem("if43", "Red Sauce Pasta", "", 70.0, "", "Pasta", true),
            MenuItem("if44", "Makhani Pasta", "", 70.0, "", "Pasta", true),
            MenuItem("if45", "Mix Sauce Pasta", "", 70.0, "", "Pasta", true),
            MenuItem("if46", "Chicken Pasta", "", 90.0, "", "Pasta", false),         // Added from image
            MenuItem("if47", "Paneer Pasta", "", 80.0, "", "Pasta", true),           // Added from image

            // MOJITO
            MenuItem("if48", "Green Apple Mojito", "", 50.0, "", "Mojito", true),
            MenuItem("if49", "Blue Curacao Mojito", "", 50.0, "", "Mojito", true),
            MenuItem("if50", "Watermelon Mojito", "", 50.0, "", "Mojito", true),
            MenuItem("if51", "Kala Khatta Mojito", "", 50.0, "", "Mojito", true),
            MenuItem("if52", "Virgin Mojito", "", 50.0, "", "Mojito", true),
            MenuItem("if53", "Mint Mojito", "", 50.0, "", "Mojito", true),
            MenuItem("if54", "Jeera Shikanji", "", 30.0, "", "Mojito", true),
            MenuItem("if55", "Nimbu Pani", "", 30.0, "", "Mojito", true),
            MenuItem("if56", "Fresh Lime Soda", "", 30.0, "", "Mojito", true),

            // BURGER
            MenuItem("if57", "Aloo Tikki Burger", "", 50.0, "", "Burger", true),
            MenuItem("if58", "Veg Burger", "", 60.0, "", "Burger", true),
            MenuItem("if59", "Veg Cheese Burger", "", 70.0, "", "Burger", true),
            MenuItem("if60", "Paneer Tikka Burger", "", 70.0, "", "Burger", true),
            MenuItem("if61", "Noodles Burger", "", 60.0, "", "Burger", true),
            MenuItem("if62", "Paneer Burger", "", 70.0, "", "Burger", true),     // Added from image
            MenuItem("if63", "Chicken Burger", "", 80.0, "", "Burger", false),
            MenuItem("if64", "Chicken Cheese Burger", "", 90.0, "", "Burger", false),
            MenuItem("if65", "Chicken Seekh Burger", "", 90.0, "", "Burger", false), // Added from image
            MenuItem("if66", "Egg Burger", "", 60.0, "", "Burger", false),
            MenuItem("if67", "Egg Cheese Burger", "", 70.0, "", "Burger", false),

            // SANDWICH
            MenuItem("if68", "Veg Sandwich", "", 50.0, "", "Sandwich", true),
            MenuItem("if69", "Paneer Sandwich", "", 60.0, "", "Sandwich", true),
            MenuItem("if70", "Veg Grilled Sandwich", "", 70.0, "", "Sandwich", true),
            MenuItem("if71", "Veg Cheese Sandwich", "", 70.0, "", "Sandwich", true),
            MenuItem("if72", "Aloo Sandwich", "", 60.0, "", "Sandwich", true),
            MenuItem("if73", "Paneer Grilled Sandwich", "", 70.0, "", "Sandwich", true),
            MenuItem("if74", "Egg Sandwich", "", 70.0, "", "Sandwich", false),
            MenuItem("if75", "Chicken Sandwich", "", 80.0, "", "Sandwich", false),
            MenuItem("if76", "Egg Cheese Sandwich", "", 80.0, "", "Sandwich", false),
            MenuItem("if77", "Chicken Cheese Sandwich", "", 90.0, "", "Sandwich", false),
            MenuItem("if78", "Paneer Cheese Sandwich", "", 80.0, "", "Sandwich", true),
            MenuItem("if79", "Club Sandwich", "", 100.0, "", "Sandwich", false),   // Added from image

            // WRAP
            MenuItem("if80", "Aloo Tikki Wrap", "", 50.0, "", "Wrap", true),
            MenuItem("if81", "Veg Wrap", "", 60.0, "", "Wrap", true),
            MenuItem("if82", "Paneer Wrap", "", 70.0, "", "Wrap", true),
            MenuItem("if83", "Chicken Wrap", "", 80.0, "", "Wrap", false),
            MenuItem("if84", "Egg Wrap", "", 60.0, "", "Wrap", false),                // Added

            // MOMOS
            MenuItem("if85", "Steam Momos", "", 50.0, "", "Momos", true),
            MenuItem("if86", "Veg Fried Momos", "", 60.0, "", "Momos", true),
            MenuItem("if87", "Kurkure Momos", "", 80.0, "", "Momos", true),
            MenuItem("if88", "Chicken Momos", "", 70.0, "", "Momos", false),
            MenuItem("if89", "Chicken Fried Momos", "", 80.0, "", "Momos", false),
            MenuItem("if90", "Chicken Kurkure Momos", "", 100.0, "", "Momos", false),
            MenuItem("if91", "Paneer Momos", "", 90.0, "", "Momos", true),
            MenuItem("if92", "Veg Momos", "", 50.0, "", "Momos", true),       // Added, for consistency with roll section

            // NOODLES
            MenuItem("if93", "Veg Noodles", "", 50.0, "", "Noodles", true),
            MenuItem("if94", "Paneer Noodles", "", 60.0, "", "Noodles", true),
            MenuItem("if95", "Egg Noodles", "", 60.0, "", "Noodles", false),
            MenuItem("if96", "Chilly Garlic Noodles", "", 60.0, "", "Noodles", true),
            MenuItem("if97", "Schezwan Noodles", "", 70.0, "", "Noodles", true),
            MenuItem("if98", "Hakka Noodles", "", 60.0, "", "Noodles", true),
            MenuItem("if99", "Mushroom Noodles", "", 80.0, "", "Noodles", true),
            MenuItem("if100", "Chicken Noodles", "", 80.0, "", "Noodles", false),
            MenuItem("if101", "Soya Chaap Noodles", "", 70.0, "", "Noodles", true), // Corrected spelling to Chaap
            MenuItem("if102", "Egg Chicken Noodles", "", 80.0, "", "Noodles", false), // Added from image

            // FRIES
            MenuItem("if103", "Salty Fries", "", 60.0, "", "Fries", true),
            MenuItem("if104", "Masala Fries", "", 70.0, "", "Fries", true),
            MenuItem("if105", "Peri Peri Fries", "", 80.0, "", "Fries", true),
            MenuItem("if106", "Cheesy Fries", "", 80.0, "", "Fries", true),
            MenuItem("if107", "Chilly Potato", "", 70.0, "", "Fries", true),
            MenuItem("if108", "Peri Peri Potato", "", 80.0, "", "Fries", true),
            MenuItem("if109", "Peri Peri Twister", "", 90.0, "", "Fries", true),

            // OMELETTE / EGGS (non-veg)
            MenuItem("if110", "Single Egg Omelette", "", 30.0, "", "Egg", false),
            MenuItem("if111", "Double Egg Omelette", "", 40.0, "", "Egg", false),
            MenuItem("if112", "Cheese Bread Omelette", "", 50.0, "", "Egg", false),
            MenuItem("if113", "Paneer Bread Omelette", "", 70.0, "", "Egg", false),
            MenuItem("if114", "Dry Fry", "", 50.0, "", "Egg", false),
            MenuItem("if115", "Egg Bhurji", "", 50.0, "", "Egg", false),    // Corrected price (was 40, now 50)
            MenuItem("if116", "2 Boiled Eggs", "", 30.0, "", "Egg", false),

            // ROLL
            MenuItem("if117", "Spring Roll", "", 50.0, "", "Roll", true),
            MenuItem("if118", "Single Egg Roll", "", 50.0, "", "Roll", false),
            MenuItem("if119", "Double Egg Roll", "", 60.0, "", "Roll", false),
            MenuItem("if120", "Veg Roll", "", 40.0, "", "Roll", true),
            MenuItem("if121", "Paneer Roll", "", 60.0, "", "Roll", true),
            MenuItem("if122", "Chicken Roll", "", 70.0, "", "Roll", false),
            MenuItem("if123", "Cheese Noodles Roll", "", 60.0, "", "Roll", true),
            MenuItem("if124", "Soya Chaap Roll", "", 60.0, "", "Roll", true),
            MenuItem("if125", "Egg Chicken Roll", "", 80.0, "", "Roll", false),
            MenuItem("if126", "Paneer Chicken Roll", "", 90.0, "", "Roll", false),
            MenuItem("if127", "Double Egg Double Chicken Roll", "", 100.0, "", "Roll", false),
            MenuItem("if128", "Chicken Cheese Roll", "", 80.0, "", "Roll", false),
            MenuItem("if129", "Paneer Kathi Roll", "", 90.0, "", "Roll", true),
            MenuItem("if130", "Cheese Noodles Roll", "", 60.0, "", "Roll", true), // (repeat; ensure ID uniqueness if needed)

            // SALAD
            MenuItem("if131", "Fruit Chaat", "", 70.0, "", "Salad", true),
            MenuItem("if132", "Banana Chaat", "", 70.0, "", "Salad", true),
            MenuItem("if133", "Papaya Chaat", "", 70.0, "", "Salad", true),
            MenuItem("if134", "Green Salad", "", 30.0, "", "Salad", true),
            MenuItem("if135", "Mix Vegetable Salad", "", 30.0, "", "Salad", true),

            // SOUP
            MenuItem("if136", "Veg Soup", "", 50.0, "", "Soup", true),
            MenuItem("if137", "Tomato Soup", "", 50.0, "", "Soup", true),
            MenuItem("if138", "Sweet Corn Soup", "", 60.0, "", "Soup", true),
            MenuItem("if139", "Chicken Soup", "", 80.0, "", "Soup", false),

            // CORNS
            MenuItem("if140", "Masala Sweet Corn", "", 50.0, "", "Corn", true),
            MenuItem("if141", "Chilli Garlic Corn", "", 60.0, "", "Corn", true),

            // MEALS (all bowls and mains)
            MenuItem("if142", "Rajma Rice", "", 60.0, "", "Meal", true),
            MenuItem("if143", "Fried Rice", "", 60.0, "", "Meal", true),
            MenuItem("if144", "Chilly Garlic Rice", "", 70.0, "", "Meal", true),
            MenuItem("if145", "Paneer Fried Rice", "", 80.0, "", "Meal", true), // Corrected price (was 70)
            MenuItem("if146", "Egg Rice", "", 70.0, "", "Meal", false),
            MenuItem("if147", "Chicken Fried Rice", "", 90.0, "", "Meal", false), // Corrected price (was 80)
            MenuItem("if148", "Chicken Seekh Kabab", "", 110.0, "", "Meal", false),
            MenuItem("if149", "Chicken Lollypop", "", 90.0, "", "Meal", false),
            MenuItem("if150", "Mutton Seekh Kabab", "", 110.0, "", "Meal", false),
            MenuItem("if151", "Veg Bullet", "", 50.0, "", "Meal", true),
            MenuItem("if152", "Chicken Bullet", "", 100.0, "", "Meal", false),
            MenuItem("if153", "Veg Manchurian Rice", "", 80.0, "", "Meal", true),
            MenuItem("if154", "Chicken Manchurian Rice", "", 100.0, "", "Meal", false),
            MenuItem("if155", "Paneer Manchurian Rice", "", 100.0, "", "Meal", true), // Corrected title (was Paneer Manchurian)
            MenuItem("if156", "Chicken Manchurian", "", 110.0, "", "Meal", false),

            // PATTIES
            MenuItem("if157", "Aloo Patties", "", 30.0, "", "Patty", true),
            MenuItem("if158", "Aloo Vada Cheese", "", 50.0, "", "Patty", true),
            MenuItem("if159", "Pasta Patties", "", 50.0, "", "Patty", true),
            MenuItem("if160", "Veg Patties", "", 30.0, "", "Patty", true),
            MenuItem("if161", "Egg Cheese Patties", "", 50.0, "", "Patty", false),
            MenuItem("if162", "Egg Patties", "", 50.0, "", "Patty", false),
            MenuItem("if163", "Chicken Patties", "", 60.0, "", "Patty", false),

            // TEA & COFFEE
            MenuItem("if164", "Tea", "", 15.0, "", "Tea & Coffee", true),
            MenuItem("if165", "Spl. Tea", "", 20.0, "", "Tea & Coffee", true),
            MenuItem("if166", "Lemon Tea", "", 20.0, "", "Tea & Coffee", true),
            MenuItem("if167", "Black Coffee", "", 20.0, "", "Tea & Coffee", true),   // Corrected price (was 25)
            MenuItem("if168", "Hot Coffee", "", 30.0, "", "Tea & Coffee", true),     // Added from image
            MenuItem("if169", "Hot Chocolate", "", 30.0, "", "Tea & Coffee", true),
            MenuItem("if170", "Bourn Vita", "", 35.0, "", "Tea & Coffee", true)
        )
    }


    private fun getBarkatFoodMenuItems(): List<MenuItem> {
        return listOf(
            // BREAKFAST
            MenuItem("bk1", "Aloo Parantha (2pcs)", "Potato stuffed paratha, 2 pieces", 70.0, "", "Breakfast", true),
            MenuItem("bk2", "Mix Paratha (2pcs)", "Mixed veg paratha, 2 pieces", 80.0, "", "Breakfast", true),
            MenuItem("bk3", "Paneer Parantha (2pcs)", "Paneer stuffed paratha, 2 pieces", 90.0, "", "Breakfast", true),
            MenuItem("bk4", "Chole Bhature", "Chole with fried bhature", 70.0, "", "Breakfast", true),
            MenuItem("bk5", "Samosa", "Classic fried potato samosa", 15.0, "", "Breakfast", true),
            MenuItem("bk6", "Samosa With Chole", "Samosa with chole", 25.0, "", "Breakfast", true),
            MenuItem("bk7", "Bread Omelette", "Egg omelette with bread", 50.0, "", "Breakfast", false),

            // PATTY
            MenuItem("bk8", "Aloo Patty", "", 20.0, "", "Patty", true),
            MenuItem("bk9", "Cheese Patty", "", 25.0, "", "Patty", true),
            MenuItem("bk10", "Cheese Corn", "", 30.0, "", "Patty", true),
            MenuItem("bk11", "Paneer Korma", "", 40.0, "", "Patty", true),
            MenuItem("bk12", "Tandoori Patty", "", 50.0, "", "Patty", true),

            // BURGER
            MenuItem("bk13", "Aloo Tikki", "", 50.0, "", "Burger", true),
            MenuItem("bk14", "Cheese Burger", "", 60.0, "", "Burger", true),
            MenuItem("bk15", "Paneer Burger", "", 70.0, "", "Burger", true),
            MenuItem("bk16", "Chicken Burger", "", 80.0, "", "Burger", false),
            MenuItem("bk17", "Mexican Burger", "", 70.0, "", "Burger", true),
            MenuItem("bk18", "Egg Burger", "", 80.0, "", "Burger", false),

            // PASTA
            MenuItem("bk19", "Red Sauce Pasta", "", 100.0, "", "Pasta", true),
            MenuItem("bk20", "White Sauce Pasta", "", 100.0, "", "Pasta", true),
            MenuItem("bk21", "Mix Sauce Pasta", "", 100.0, "", "Pasta", true),
            MenuItem("bk22", "Makhani Pasta", "", 110.0, "", "Pasta", true),
            MenuItem("bk23", "Chicken Pasta", "", 150.0, "", "Pasta", false),

            // NOODLES (VEG / EGG / CHICKEN / PANEER, etc.)
            MenuItem("bk24", "Veg Noodles", "", 80.0, "", "Noodles", true),
            MenuItem("bk25", "Chilli Garlic Noodles", "", 90.0, "", "Noodles", true),
            MenuItem("bk26", "Egg Noodles", "", 100.0, "", "Noodles", false),
            MenuItem("bk27", "Chicken Noodles", "", 110.0, "", "Noodles", false),
            MenuItem("bk28", "Paneer Noodles", "", 110.0, "", "Noodles", true),
            MenuItem("bk29", "Singapuri Noodles", "", 90.0, "", "Noodles", true),
            MenuItem("bk30", "Manchurian Noodles", "", 120.0, "", "Noodles", true),
            MenuItem("bk31", "Hakka Noodles", "", 120.0, "", "Noodles", true),

            // FRIED RICE
            MenuItem("bk32", "Veg Fried Rice", "", 80.0, "", "Fried Rice", true),
            MenuItem("bk33", "Garlic Fried Rice", "", 90.0, "", "Fried Rice", true),
            MenuItem("bk34", "Egg Fried Rice", "", 100.0, "", "Fried Rice", false),
            MenuItem("bk35", "Paneer Fried Rice", "", 110.0, "", "Fried Rice", true),
            MenuItem("bk36", "Chicken Fried Rice", "", 120.0, "", "Fried Rice", false),
            MenuItem("bk37", "Chinese Fried Rice", "", 120.0, "", "Fried Rice", true),

            // FRIES
            MenuItem("bk38", "Golden Fries", "", 80.0, "", "Fries", true),
            MenuItem("bk39", "Masala Fries", "", 90.0, "", "Fries", true),
            MenuItem("bk40", "Peri Peri Fries", "", 100.0, "", "Fries", true),
            MenuItem("bk41", "Cheese Fries", "", 110.0, "", "Fries", true),
            MenuItem("bk42", "Honey Chilli Potato", "", 120.0, "", "Fries", true),

            // CHINESE SNACKS
            MenuItem("bk43", "Veg Momos", "", 80.0, "", "Chinese", true),
            MenuItem("bk44", "KurKure Momos", "", 100.0, "", "Chinese", true),
            MenuItem("bk45", "Chilli Momos", "", 120.0, "", "Chinese", true),
            MenuItem("bk46", "Chilly Paneer", "", 150.0, "", "Chinese", true),
            MenuItem("bk47", "Chilli Potato", "", 100.0, "", "Chinese", true),
            MenuItem("bk48", "Honey Chilly Potato", "", 130.0, "", "Chinese", true),
            MenuItem("bk49", "Spring Roll", "", 80.0, "", "Chinese", true),
            MenuItem("bk50", "Chilli Chicken", "", 150.0, "", "Chinese", false),
            MenuItem("bk51", "Fried Chicken", "Boneless 8 pc", 230.0, "", "Chinese", false),

            // INDIAN MAIN COURSE (Half/Full)
            MenuItem("bk52h", "Dal Makhni (Half)", "", 100.0, "", "Indian Main Course", true),
            MenuItem("bk52f", "Dal Makhni (Full)", "", 150.0, "", "Indian Main Course", true),
            MenuItem("bk53h", "Channa Masala (Half)", "", 100.0, "", "Indian Main Course", true),
            MenuItem("bk53f", "Channa Masala (Full)", "", 150.0, "", "Indian Main Course", true),
            MenuItem("bk54h", "Shahi Paneer (Half)", "", 130.0, "", "Indian Main Course", true),
            MenuItem("bk54f", "Shahi Paneer (Full)", "", 210.0, "", "Indian Main Course", true),
            MenuItem("bk55h", "Kadhai Paneer (Half)", "", 130.0, "", "Indian Main Course", true),
            MenuItem("bk55f", "Kadhai Paneer (Full)", "", 210.0, "", "Indian Main Course", true),
            MenuItem("bk56h", "Paneer Do Pyaza (Half)", "", 140.0, "", "Indian Main Course", true),
            MenuItem("bk56f", "Paneer Do Pyaza (Full)", "", 220.0, "", "Indian Main Course", true),
            MenuItem("bk57h", "Butter Chicken (Half)", "", 200.0, "", "Indian Main Course", false),
            MenuItem("bk57f", "Butter Chicken (Full)", "", 290.0, "", "Indian Main Course", false),
            MenuItem("bk58h", "Kadhai Chicken (Half)", "", 200.0, "", "Indian Main Course", false),
            MenuItem("bk58f", "Kadhai Chicken (Full)", "", 290.0, "", "Indian Main Course", false),
            MenuItem("bk59h", "Chicken Lababdar (Half)", "", 230.0, "", "Indian Main Course", false),
            MenuItem("bk59f", "Chicken Lababdar (Full)", "", 390.0, "", "Indian Main Course", false),

            // ROTI & NAAN
            MenuItem("bk60", "Tawa Roti", "", 10.0, "", "Bread", true),
            MenuItem("bk61", "Tandoori Roti", "", 15.0, "", "Bread", true),
            MenuItem("bk62", "Naan", "", 20.0, "", "Bread", true),
            MenuItem("bk63", "Laccha Parantha", "", 35.0, "", "Bread", true),

            // THALI & COMBO
            MenuItem("bk64", "Paneer Thali", "", 160.0, "", "Thali & Combo", true),
            MenuItem("bk65", "Chicken Thali", "", 160.0, "", "Thali & Combo", false),
            MenuItem("bk66", "Cheese Naan With Gravy", "", 130.0, "", "Thali & Combo", true),
            MenuItem("bk67", "Chur Chur Naan", "", 110.0, "", "Thali & Combo", true),
            MenuItem("bk68", "Amritsari Naan", "", 110.0, "", "Thali & Combo", true),

            // BEVERAGE
            MenuItem("bk69", "Tea", "", 15.0, "", "Beverage", true),
            MenuItem("bk70", "Coffee", "", 15.0, "", "Beverage", true),
            MenuItem("bk71", "Banana Shake", "", 60.0, "", "Beverage", true),
            MenuItem("bk72", "Banana Shake (Big)", "", 80.0, "", "Beverage", true),
            MenuItem("bk73", "Cold Coffee", "", 60.0, "", "Beverage", true),
            MenuItem("bk74", "Cold Coffee (Big)", "", 80.0, "", "Beverage", true),
            MenuItem("bk75", "Strawberry Shake", "", 60.0, "", "Beverage", true),
            MenuItem("bk76", "Strawberry Shake (Big)", "", 90.0, "", "Beverage", true),
            MenuItem("bk77", "Mango Shake", "", 60.0, "", "Beverage", true),
            MenuItem("bk78", "Mango Shake (Big)", "", 90.0, "", "Beverage", true),
            MenuItem("bk79", "Butter Scotch Shake", "", 60.0, "", "Beverage", true),
            MenuItem("bk80", "Butter Scotch Shake (Big)", "", 90.0, "", "Beverage", true),
            MenuItem("bk81", "Oreo Shake", "", 70.0, "", "Beverage", true),
            MenuItem("bk82", "Oreo Shake (Big)", "", 90.0, "", "Beverage", true),
            MenuItem("bk83", "Black Current Shake", "", 70.0, "", "Beverage", true),
            MenuItem("bk84", "Black Current Shake (Big)", "", 90.0, "", "Beverage", true),
            MenuItem("bk85", "Chocolate Shake", "", 70.0, "", "Beverage", true),
            MenuItem("bk86", "Chocolate Shake (Big)", "", 90.0, "", "Beverage", true),
            MenuItem("bk87", "Banana Chocnut Shake", "", 60.0, "", "Beverage", true),
            MenuItem("bk88", "Banana Chocnut Shake (Big)", "", 90.0, "", "Beverage", true),
            MenuItem("bk89", "Rose Shake", "", 60.0, "", "Beverage", true),
            MenuItem("bk90", "Lassi", "", 50.0, "", "Beverage", true),

            // RICE & BIRYANI
            MenuItem("bk91", "Chole Rice", "", 70.0, "", "Rice & Biryani", true),
            MenuItem("bk92", "Paneer Rice", "", 100.0, "", "Rice & Biryani", true),
            MenuItem("bk93", "Veg Biryani", "", 80.0, "", "Rice & Biryani", true),
            MenuItem("bk94", "Veg Biryani (Full)", "", 100.0, "", "Rice & Biryani", true),
            MenuItem("bk95", "Chicken Biryani", "", 100.0, "", "Rice & Biryani", false),
            MenuItem("bk96", "Chicken Biryani (Full)", "", 150.0, "", "Rice & Biryani", false),

            // DESSERT
            MenuItem("bk97", "Kheer", "", 40.0, "", "Dessert", true),
            MenuItem("bk98", "Gulab Jamun", "", 40.0, "", "Dessert", true),
            MenuItem("bk99", "Cake & Pastries", "", 50.0, "", "Dessert", true),
            MenuItem("bk100", "Brownie with Ice Cream", "", 60.0, "", "Dessert", true),

            // MAJITO's (MOJITO section)
            MenuItem("bk101", "Lime Soda", "", 40.0, "", "Mojito", true),
            MenuItem("bk102", "Lime Water", "", 30.0, "", "Mojito", true),
            MenuItem("bk103", "Ice Blue", "", 50.0, "", "Mojito", true),
            MenuItem("bk104", "Green Apple", "", 60.0, "", "Mojito", true),
            MenuItem("bk105", "Water Melon", "", 60.0, "", "Mojito", true),
            MenuItem("bk106", "Virgin Mojito", "", 60.0, "", "Mojito", true),
            MenuItem("bk107", "Kala Khatta", "", 60.0, "", "Mojito", true),
            MenuItem("bk108", "Ice Tea", "", 50.0, "", "Mojito", true),

            // SANDWICH
            MenuItem("bk109", "Veg Grilled Sandwich", "", 70.0, "", "Sandwich", true),
            MenuItem("bk110", "Cheese Sandwich", "", 80.0, "", "Sandwich", true),
            MenuItem("bk111", "Aloo Tikki Sandwich", "", 70.0, "", "Sandwich", true),
            MenuItem("bk112", "Paneer Tikka Sandwich", "", 100.0, "", "Sandwich", true),
            MenuItem("bk113", "Chicken Sandwich", "", 100.0, "", "Sandwich", false),
            MenuItem("bk114", "BBQ Chicken Sandwich", "", 120.0, "", "Sandwich", false),

            // SPL. GYM DIET
            MenuItem("bk115", "Boil Chicken", "", 140.0, "", "Spl. Gym Diet", false),
            MenuItem("bk116", "Grill Chicken Salad", "", 160.0, "", "Spl. Gym Diet", false),
            MenuItem("bk117", "Grilled Fish", "", 160.0, "", "Spl. Gym Diet", false)
        )
    }


    private fun getEatAndSmileMenuItems(): List<MenuItem> {
        return listOf(
            // --- Breakfast ---
            MenuItem("es1", "Aloo Prantha", "Potato stuffed paratha", 70.0, "", "Breakfast", true),
            MenuItem("es2", "Mix Prantha", "Mixed veg stuffed paratha", 80.0, "", "Breakfast", true),
            MenuItem("es3", "Paneer Prantha", "Paneer stuffed paratha", 90.0, "", "Breakfast", true),
            MenuItem("es4", "Bread Omelette", "Egg omelette with bread", 50.0, "", "Breakfast", false),
            MenuItem("es5", "Maggi", "Plain maggi noodles", 50.0, "", "Breakfast", true),
            MenuItem("es6", "Veg Maggi", "Vegetable maggi noodles", 50.0, "", "Breakfast", true),

            // --- Patties ---
            MenuItem("es7", "Aloo Pattie", "Potato patty", 20.0, "", "Patties", true),
            MenuItem("es8", "Cheese Pattie", "Cheese patty", 25.0, "", "Patties", true),
            MenuItem("es9", "Cheese Corn Pattie", "Cheese and corn patty", 30.0, "", "Patties", true),
            MenuItem("es10", "Korma Pattie", "Korma flavored patty", 40.0, "", "Patties", true),

            // --- Rice Section ---
            MenuItem("es11", "Veg Fried Rice", "Vegetable fried rice", 70.0, "", "Rice", true),
            MenuItem("es12", "Paneer Rice", "Paneer fried rice", 90.0, "", "Rice", true),
            MenuItem("es13", "Egg Fried Rice", "Egg fried rice", 90.0, "", "Rice", false),
            MenuItem("es14", "Mushroom Fried Rice", "Mushroom fried rice", 90.0, "", "Rice", true),
            MenuItem("es15", "Chicken Fried Rice", "Chicken fried rice", 110.0, "", "Rice", false),

            // --- Pasta ---
            MenuItem("es16", "Red Sauce Pasta", "Pasta in red tomato sauce", 90.0, "", "Pasta", true),
            MenuItem("es17", "White Sauce Pasta", "Pasta in creamy white sauce", 100.0, "", "Pasta", true),
            MenuItem("es18", "Mix Sauce Pasta", "Pasta in mix red + white sauce", 100.0, "", "Pasta", true),
            MenuItem("es19", "Chicken Pasta", "Chicken pasta", 130.0, "", "Pasta", false),

            // --- Noodles ---
            MenuItem("es20", "Veg Noodles", "Veg stir fry noodles", 70.0, "", "Noodles", true),
            MenuItem("es21", "Chilli Garlic Noodles", "Spicy noodle with garlic and chili", 80.0, "", "Noodles", true),
            MenuItem("es22", "Hakka Noodles", "Hakka style noodles", 90.0, "", "Noodles", true),
            MenuItem("es23", "Paneer Noodles", "Noodles with paneer", 90.0, "", "Noodles", true),
            MenuItem("es24", "Egg Noodles", "Egg stir fry noodles", 90.0, "", "Noodles", false),
            MenuItem("es25", "Chicken Noodles", "Chicken noodles", 100.0, "", "Noodles", false),

            // --- Burger ---
            MenuItem("es26", "Aloo Tikki Burger", "Aloo tikki burger", 40.0, "", "Burger", true),
            MenuItem("es27", "Cheese Burger", "Cheese burger", 50.0, "", "Burger", true),
            MenuItem("es28", "Paneer Burger", "Paneer patty burger", 60.0, "", "Burger", true),
            MenuItem("es29", "Chicken Burger", "Chicken patty burger", 60.0, "", "Burger", false),
            MenuItem("es30", "King Burger", "Big size king burger", 80.0, "", "Burger", true),
            MenuItem("es31", "Mexican Burger", "Spicy Mexican-style burger", 80.0, "", "Burger", true),

            // --- Sandwich ---
            MenuItem("es32", "Veg Grilled Sandwich", "Grilled vegetable sandwich", 70.0, "", "Sandwich", true),
            MenuItem("es33", "Aloo Tikka Sandwich", "Aloo tikki sandwich", 70.0, "", "Sandwich", true),
            MenuItem("es34", "Cheese Corn Sandwich", "Stuffed cheese corn sandwich", 70.0, "", "Sandwich", true),
            MenuItem("es35", "Paneer Tikka Sandwich", "Paneer tikka sandwich", 90.0, "", "Sandwich", true),
            MenuItem("es36", "Mexican Sandwich", "Spicy Mexican style sandwich", 90.0, "", "Sandwich", true),
            MenuItem("es37", "Paneer Korma Sandwich", "Paneer korma sandwich", 90.0, "", "Sandwich", true),
            MenuItem("es38", "Chicken Sandwich", "Chicken sandwich", 100.0, "", "Sandwich", false),

            // --- Pav Bhaji ---
            MenuItem("es39", "Pav Bhaji", "Pav with spicy mashed bhaji (butter)", 70.0, "", "Street Food", true),

            // --- Rolls ---
            MenuItem("es40", "Spring Roll", "Veg spring roll", 60.0, "", "Roll", true),
            MenuItem("es41", "Veg Roll", "Veg roll wrap", 50.0, "", "Roll", true),
            MenuItem("es42", "Egg Roll", "Egg roll wrap", 60.0, "", "Roll", false),
            MenuItem("es43", "Paneer Roll", "Paneer stuffed roll", 70.0, "", "Roll", true),
            MenuItem("es44", "Soya Chaap Roll", "Soya chaap roll", 70.0, "", "Roll", true),
            MenuItem("es45", "Cheese Corn Roll", "Cheese corn roll", 70.0, "", "Roll", true),
            MenuItem("es46", "Chicken Roll", "Chicken roll wrap", 100.0, "", "Roll", false),
            MenuItem("es47", "Cheese Finger", "Cheesy fried finger roll", 100.0, "", "Roll", true),

            // --- Chinese ---
            MenuItem("es48", "Veg Manchurian", "Veg manchurian with gravy", 80.0, "", "Chinese", true),
            MenuItem("es49", "Steam Momos", "Steamed veg momos", 60.0, "", "Chinese", true),
            MenuItem("es50", "Fried Momos", "Fried vegetable momos", 70.0, "", "Chinese", true),
            MenuItem("es51", "Chilly Chicken", "Spicy chicken with chili and gravy", 150.0, "", "Chinese", false),
            MenuItem("es52", "Chilly Paneer", "Paneer tossed with chili sauce", 110.0, "", "Chinese", true),
            MenuItem("es53", "Mushroom Chilli", "Mushroom tossed in chili sauce", 100.0, "", "Chinese", true),

            // --- Fries ---
            MenuItem("es54", "Golden Fries", "French fries", 70.0, "", "Fries", true),
            MenuItem("es55", "Chilli Potato", "Chilli potato fries", 90.0, "", "Fries", true),
            MenuItem("es56", "Honey Chilli", "Honey tossed chili potato", 90.0, "", "Fries", true),

            // --- Beverages: Tea, Lassi, Shakes, Mojitos ---
            MenuItem("es57", "Chai", "Hot tea", 15.0, "", "Beverages", true),
            MenuItem("es58", "Hot Coffee", "Fresh hot coffee", 25.0, "", "Beverages", true),
            MenuItem("es59", "Sweet Lassi (M/L)", "Sweet lassi", 40.0, "", "Beverages", true),
            MenuItem("es59b", "Sweet Lassi (L)", "Large sweet lassi", 80.0, "", "Beverages", true),
            MenuItem("es60", "Mango Lassi (M/L)", "Mango lassi", 60.0, "", "Beverages", true),
            MenuItem("es60b", "Mango Lassi (L)", "Large mango lassi", 90.0, "", "Beverages", true),
            MenuItem("es61", "Cold Coffee (M/L)", "Chilled cold coffee", 60.0, "", "Beverages", true),
            MenuItem("es61b", "Cold Coffee (L)", "Large cold coffee", 90.0, "", "Beverages", true),
            MenuItem("es62", "Banana Shake (M/L)", "Banana shake", 60.0, "", "Beverages", true),
            MenuItem("es62b", "Banana Shake (L)", "Large banana shake", 90.0, "", "Beverages", true),
            MenuItem("es63", "Butter Scotch Shake (M/L)", "Butterscotch shake", 60.0, "", "Beverages", true),
            MenuItem("es63b", "Butter Scotch Shake (L)", "Large butterscotch shake", 90.0, "", "Beverages", true),
            MenuItem("es64", "Kesar Pista Shake (M/L)", "Kesar pista shake", 60.0, "", "Beverages", true),
            MenuItem("es64b", "Kesar Pista Shake (L)", "Large kesar pista shake", 90.0, "", "Beverages", true),
            MenuItem("es65", "Vanilla Shake (M/L)", "Vanilla shake", 60.0, "", "Beverages", true),
            MenuItem("es65b", "Vanilla Shake (L)", "Large vanilla shake", 90.0, "", "Beverages", true),
            MenuItem("es66", "Strawberry Shake (M/L)", "Strawberry shake", 60.0, "", "Beverages", true),
            MenuItem("es66b", "Strawberry Shake (L)", "Large strawberry shake", 90.0, "", "Beverages", true),
            MenuItem("es67", "Black Current Shake (M/L)", "Black currant shake", 60.0, "", "Beverages", true),
            MenuItem("es67b", "Black Current Shake (L)", "Large black currant shake", 90.0, "", "Beverages", true),
            MenuItem("es68", "Chocolate Shake (M/L)", "Chocolate shake", 60.0, "", "Beverages", true),
            MenuItem("es68b", "Chocolate Shake (L)", "Large chocolate shake", 90.0, "", "Beverages", true),
            MenuItem("es69", "Oreo Shake (M/L)", "Oreo shake", 60.0, "", "Beverages", true),
            MenuItem("es69b", "Oreo Shake (L)", "Large oreo shake", 90.0, "", "Beverages", true),

            // --- Mojitos ---
            MenuItem("es70", "Nimbu Paani", "Nimbu pani / lemon water", 30.0, "", "Mojito", true),
            MenuItem("es71", "Lime Soda", "Lime soda", 40.0, "", "Mojito", true),
            MenuItem("es72", "Kala Khata", "Kala khatta mojito", 50.0, "", "Mojito", true),
            MenuItem("es73", "Virgin Mojito", "Classic virgin mojito", 50.0, "", "Mojito", true),
            MenuItem("es74", "Green Apple", "Green apple mojito", 50.0, "", "Mojito", true),
            MenuItem("es75", "Ice Blue", "Ice blue mojito", 50.0, "", "Mojito", true),

            // --- Special Items & Combos ---
            MenuItem("es76", "Chole Bhature", "Chole Bhature special (with lassi 90/-)", 60.0, "", "Special", true),
            MenuItem("es77", "Special Gym Diet (250 gm)", "Special Gym Diet Plate", 60.0, "", "Special", false),
            MenuItem("es78", "Boil Chicken", "Boiled chicken", 130.0, "", "Special", false),
            MenuItem("es79", "Grill Chicken", "Grilled chicken", 140.0, "", "Special", false),
            MenuItem("es80", "Stuffed Grill Chicken", "Stuffed grilled chicken", 160.0, "", "Special", false),
            MenuItem("es81", "Special Meal", "Special thali/meal", 180.0, "", "Special", true),

            // --- Halal Items (from Red Board) ---
            MenuItem("esh1", "Halal Chicken Burger", "Halal-certified chicken burger", 70.0, "", "Halal", false),
            MenuItem("esh2", "Halal Chicken Sandwich", "Halal-certified chicken sandwich", 100.0, "", "Halal", false),
            MenuItem("esh3", "Halal Chicken Roll", "Halal-certified chicken roll", 100.0, "", "Halal", false),
            MenuItem("esh4", "Halal Chicken Noodles", "Halal chicken noodles", 110.0, "", "Halal", false),
            MenuItem("esh5", "Halal Chicken Rice", "Halal chicken rice", 120.0, "", "Halal", false),
            MenuItem("esh6", "Halal Chicken Pasta", "Halal chicken pasta", 130.0, "", "Halal", false),
            MenuItem("esh7", "Halal Chilly Chicken", "Halal chilly chicken", 250.0, "", "Halal", false),
            MenuItem("esh8", "Halal Fried Chicken", "Halal fried chicken", 250.0, "", "Halal", false),
            MenuItem("esh9", "Halal Chicken Biryani", "Halal chicken biryani", 200.0, "", "Halal", false)
        )
    }



    private fun getFoodCastelMenuItems(): List<MenuItem> {
        return listOf(
            // From foodcastel1.jpg (the street menu board)
            MenuItem("fc1", "Veg Bullet", "Crispy fried veg bullet", 50.0, "", "Snacks", true),
            MenuItem("fc2", "Soya Chaap Rice", "Soya chaap curry with rice", 90.0, "", "Snacks", true),
            MenuItem("fc3", "Soya Chaap Noodles", "Soya chaap with noodles", 90.0, "", "Snacks", true),
            MenuItem("fc4", "Paneer Kulcha", "Paneer stuffed kulcha", 80.0, "", "Snacks", true),
            MenuItem("fc5", "Kurkure Momos", "Crispy fried momos", 90.0, "", "Momos", true),
            MenuItem("fc6", "Chilli Chicken", "Spicy chili chicken starter", 120.0, "", "Chinese", false),
            MenuItem("fc7", "Chicken Noodles", "Chicken and noodles", 100.0, "", "Chinese", false),
            MenuItem("fc8", "Chicken Rice", "Chicken fried rice", 90.0, "", "Chinese", false),
            MenuItem("fc9", "Chicken Burger", "Chicken patty burger", 70.0, "", "Burger", false),
            MenuItem("fc10", "Chicken Roll", "Chicken filled roll", 70.0, "", "Roll", false),
            MenuItem("fc11", "Chicken Sandwich", "Chicken sandwich", 80.0, "", "Sandwich", false),
            MenuItem("fc12", "Chilli Momos", "Fried chili-flavored veg momos", 90.0, "", "Momos", true),
            MenuItem("fc13", "Pav Bhaji", "Pav served with spicy mashed bhaji", 70.0, "", "Snacks", true),

            // From foodcastel4.jpg (Large wall menu with most categories)
            // HOT BEVERAGES
            MenuItem("fc14", "Tea", "Classic hot tea", 20.0, "", "Beverages", true),
            MenuItem("fc15", "Milk Tea", "Milk-based tea", 30.0, "", "Beverages", true),
            MenuItem("fc16", "Espresso Coffee", "Classic espresso", 40.0, "", "Beverages", true),
            MenuItem("fc17", "Samosa Chaat", "Samosa with tangy chaat", 40.0, "", "Snacks", true),
            MenuItem("fc18", "Cream Chai", "Creamy milk chai", 50.0, "", "Beverages", true),
            MenuItem("fc19", "Vegan Roll", "Vegan stuffed roll", 50.0, "", "Roll", true),
            MenuItem("fc20", "Hot Badam Milk", "Hot almond-flavored milk drink", 50.0, "", "Beverages", true),

            // DESSERT
            MenuItem("fc21", "Brownie", "Chocolate brownie", 40.0, "", "Dessert", true),
            MenuItem("fc22", "Brownie with Hot Chocolate", "Brownie topped with hot chocolate", 50.0, "", "Dessert", true),

            // AMERICAN CORN
            MenuItem("fc23", "American Corn (Small)", "American corn (small)", 40.0, "", "Corn", true),
            MenuItem("fc24", "American Corn (Large)", "American corn (large)", 80.0, "", "Corn", true),

            // HEALTHY EXTRAS
            MenuItem("fc25", "Brown Sandwich", "Whole wheat brown bread sandwich", 30.0, "", "Healthy", true),
            MenuItem("fc26", "Fruit Salad", "Assorted fruit salad", 50.0, "", "Healthy", true),
            MenuItem("fc27", "Pasta Salad", "Pasta salad", 50.0, "", "Healthy", true),

            // KATHI ROLLS
            MenuItem("fc28", "Kathi Roll", "Classic kathi veg roll", 70.0, "", "Roll", true),
            MenuItem("fc29", "Soya Chaap Roll", "Soya chaap roll", 70.0, "", "Roll", true),
            MenuItem("fc30", "Chilly Paneer Roll", "Chilly paneer kathi roll", 90.0, "", "Roll", true),
            MenuItem("fc31", "Pasta Roll", "Pasta stuffed roll", 70.0, "", "Roll", true),
            MenuItem("fc32", "Egg Noodles Roll", "Egg and noodles roll", 90.0, "", "Roll", false),
            MenuItem("fc33", "Egg Paneer Roll", "Egg and paneer roll", 90.0, "", "Roll", false),
            MenuItem("fc34", "Egg Cheese Roll", "Egg and cheese roll", 90.0, "", "Roll", false),

            // CHINESE SNACKS COMBO
            MenuItem("fc35", "Noodle-Manchurian", "Noodles with manchurian", 100.0, "", "Chinese Combo", true),
            MenuItem("fc36", "Rice-Manchurian", "Fried rice with manchurian", 100.0, "", "Chinese Combo", true),
            MenuItem("fc37", "Noodle-Cheese Chilly", "Noodles with cheese chilly", 120.0, "", "Chinese Combo", true),
            MenuItem("fc38", "Rice-Cheese Chilly", "Fried rice with cheese chilly", 120.0, "", "Chinese Combo", true),

            // LOADED DRINKS
            MenuItem("fc39", "Oreo Chocolate Shake", "Oreo chocolate shake", 60.0, "", "Shakes", true),
            MenuItem("fc40", "Kitkat Blast", "Kitkat chocolate shake", 60.0, "", "Shakes", true),
            MenuItem("fc41", "Snicker Blast", "Snicker bar shake", 60.0, "", "Shakes", true),
            MenuItem("fc42", "Badam Milk", "Almond milk shake", 50.0, "", "Shakes", true),
            MenuItem("fc43", "Badam Thandai", "Saffron almond cool milk", 50.0, "", "Shakes", true),
            MenuItem("fc44", "Caramel Chocolate", "Caramel chocolate shake", 60.0, "", "Shakes", true),

            // FRUIT SHAKES
            MenuItem("fc45", "Banana Shake", "Banana shake", 60.0, "", "Shakes", true),
            MenuItem("fc46", "Mango Shake", "Mango shake", 60.0, "", "Shakes", true),
            MenuItem("fc47", "Papaya Shake", "Papaya shake", 60.0, "", "Shakes", true),
            MenuItem("fc48", "Strawberry Shake", "Strawberry shake", 60.0, "", "Shakes", true),

            // MILK SHAKES / SMOOTHIES
            MenuItem("fc49", "Oreo Chocolate Milkshake", "Oreo flavored milkshake", 60.0, "", "Shakes", true),
            MenuItem("fc50", "KitKat Blast Milkshake", "Kitkat flavored milkshake", 60.0, "", "Shakes", true),
            MenuItem("fc51", "Snicker Blast Milkshake", "Snicker bar milkshake", 60.0, "", "Shakes", true),
            MenuItem("fc52", "Vanilla Shake", "Vanilla flavored shake", 60.0, "", "Shakes", true),
            MenuItem("fc53", "Chocolate Shake", "Chocolate flavored shake", 60.0, "", "Shakes", true),
            MenuItem("fc54", "Butterscotch Shake", "Butterscotch flavored shake", 60.0, "", "Shakes", true),

            // LASSI
            MenuItem("fc55", "Punjabi Sweet Lassi", "Classic sweet lassi", 50.0, "", "Lassi", true),
            MenuItem("fc56", "Salty Lassi", "Salty buttermilk lassi", 50.0, "", "Lassi", true),

            // NOODLES
            MenuItem("fc57", "Veg Noodles", "Vegetarian noodles", 80.0, "", "Noodles", true),
            MenuItem("fc58", "Chilly Garlic Noodles", "Chilly garlic infused veg noodles", 80.0, "", "Noodles", true),
            MenuItem("fc59", "Cheese Noodles", "Cheese noodles", 100.0, "", "Noodles", true),
            MenuItem("fc60", "Paneer Noodles", "Paneer noodles", 90.0, "", "Noodles", true),
            MenuItem("fc61", "Butter Noodles", "Butter tossed noodles", 90.0, "", "Noodles", true),
            MenuItem("fc62", "Hakka Noodles", "Hakka style noodles", 80.0, "", "Noodles", true),
            MenuItem("fc63", "Egg Noodles", "Egg noodles", 90.0, "", "Noodles", false),

            // RICE
            MenuItem("fc64", "Veg Fried Rice", "Vegetable fried rice", 80.0, "", "Rice", true),
            MenuItem("fc65", "Cheese Fried Rice", "Cheese fried rice", 90.0, "", "Rice", true),
            MenuItem("fc66", "Egg Fried Rice", "Fried rice with egg", 90.0, "", "Rice", false),
            MenuItem("fc67", "Rice Mushroom", "Mushroom fried rice", 90.0, "", "Rice", true),

            // CHINESE SNACKS
            MenuItem("fc68", "Spring Roll", "Vegetable spring roll", 70.0, "", "Snacks", true),
            MenuItem("fc69", "Veg Manchurian", "Vegetable manchurian balls", 90.0, "", "Snacks", true),
            MenuItem("fc70", "Honey Chilly Potato", "Sweet spicy fried potato", 70.0, "", "Snacks", true),
            MenuItem("fc71", "Chilly Mushroom", "Chili tossed mushroom", 90.0, "", "Snacks", true),
            MenuItem("fc72", "Paneer Chilly", "Chilly paneer starter", 100.0, "", "Snacks", true),

            // From foodcastel.jpg (right-side full burger/sandwich menu)
            // Grilled Patty and Fried
            MenuItem("fc73", "Aloo Patty", "Aloo patty", 20.0, "", "Snacks", true),
            MenuItem("fc74", "Cheese Patty", "Cheese patty", 30.0, "", "Snacks", true),
            MenuItem("fc75", "Paneer Corn Patty", "Paneer and corn patty", 40.0, "", "Snacks", true),
            MenuItem("fc76", "Cheese Corn Patty", "Corn and cheese patty", 40.0, "", "Snacks", true),
            MenuItem("fc77", "Pasta Patty", "Pasta stuffed patty", 40.0, "", "Snacks", true),
            MenuItem("fc78", "Golden Fries", "Classic French fries", 70.0, "", "Snacks", true),
            MenuItem("fc79", "Masala Fries", "Spiced French fries", 80.0, "", "Snacks", true),
            MenuItem("fc80", "Finger Fries", "French fries sticks", 100.0, "", "Snacks", true),
            MenuItem("fc81", "Cheese Finger Fries", "Cheese loaded fingers", 100.0, "", "Snacks", true),

            // Burger
            MenuItem("fc82", "Aloo Tikki Burger", "Aloo tikki veg burger", 40.0, "", "Burger", true),
            MenuItem("fc83", "Veg Cheese Burger", "Veg cheese burger", 50.0, "", "Burger", true),
            MenuItem("fc84", "Spicy Paneer Burger", "Spicy paneer burger", 60.0, "", "Burger", true),
            MenuItem("fc85", "Mexican Burger", "Mexican style burger", 60.0, "", "Burger", true),
            MenuItem("fc86", "Paneer Tikka Burger", "Paneer tikka burger", 70.0, "", "Burger", true),
            MenuItem("fc87", "Hot King Burger", "Special hot king burger", 80.0, "", "Burger", true),

            // Grilled Sandwiches
            MenuItem("fc88", "Veg Sandwich", "Grilled vegetarian sandwich", 60.0, "", "Sandwich", true),
            MenuItem("fc89", "Cheese Burst Sandwich", "Sandwich with extra cheese", 80.0, "", "Sandwich", true),
            MenuItem("fc90", "Butter Sandwich", "Sandwich with butter", 60.0, "", "Sandwich", true),
            MenuItem("fc91", "Mushroom Corn Sandwich", "Mushroom and corn sandwich", 80.0, "", "Sandwich", true),
            MenuItem("fc92", "Cheese Corn Sandwich", "Corn and cheese sandwich", 80.0, "", "Sandwich", true),
            MenuItem("fc93", "Paneer Corn Sandwich", "Paneer and corn sandwich", 80.0, "", "Sandwich", true),
            MenuItem("fc94", "Potato Tikka Sandwich", "Potato tikka sandwich", 80.0, "", "Sandwich", true),

            // Pasta/ Garlic Bread
            MenuItem("fc95", "Tomato Penne", "Tomato sauce penne pasta", 90.0, "", "Pasta", true),
            MenuItem("fc96", "Creamy White Sauce Pasta", "White sauce pasta", 100.0, "", "Pasta", true),
            MenuItem("fc97", "Mushroom Corn Pasta", "Mushroom & corn pasta", 110.0, "", "Pasta", true),
            MenuItem("fc98", "Makhani Pasta", "Makhani sauce pasta", 110.0, "", "Pasta", true),
            MenuItem("fc99", "Mix Pasta", "Mixed sauce pasta", 110.0, "", "Pasta", true),

            MenuItem("fc100", "Garlic Bread (Plain Cheese)", "Plain cheese garlic bread", 80.0, "", "Garlic Bread", true),
            MenuItem("fc101", "Garlic Bread (Corn Cheese)", "Corn cheese garlic bread", 100.0, "", "Garlic Bread", true),
            MenuItem("fc102", "Garlic Bread (Vegetarian)", "Veg garlic bread", 120.0, "", "Garlic Bread", true),
            MenuItem("fc103", "Garlic Bread (Mexican)", "Mexican garlic bread", 120.0, "", "Garlic Bread", true),

            // From foodcastel5.jpg (pizza/bakery)
            MenuItem("fc104", "Cheese Pizza", "Classic cheese pizza", 100.0, "", "Pizza", true),
            MenuItem("fc105", "Paneer Makhani Pizza", "Paneer makhani pizza", 140.0, "", "Pizza", true),
            MenuItem("fc106", "Farm House Pizza", "Farm house pizza", 130.0, "", "Pizza", true),
            MenuItem("fc107", "Cheese Corn Pizza", "Cheese corn pizza", 130.0, "", "Pizza", true),
            MenuItem("fc108", "Veggie Penta Pizza", "Veggie penta pizza", 130.0, "", "Pizza", true),
            MenuItem("fc109", "Mexican Wave Pizza", "Mexican wave pizza", 140.0, "", "Pizza", true),
            MenuItem("fc110", "Special Pizza", "Special pizza", 150.0, "", "Pizza", true),

            MenuItem("fc111", "Garlic Bread (Plain Cheese)", "Plain cheese garlic bread", 80.0, "", "Garlic Bread", true),
            MenuItem("fc112", "Garlic Bread (Corn Cheese)", "Corn cheese garlic bread", 100.0, "", "Garlic Bread", true),
            MenuItem("fc113", "Garlic Bread (Vegetarian)", "Vegetarian garlic bread", 90.0, "", "Garlic Bread", true),
            MenuItem("fc114", "Garlic Bread (Paneer Cheese)", "Paneer cheese garlic bread", 120.0, "", "Garlic Bread", true),

            // Bakery Products
            MenuItem("fc115", "Bakery Biscuit", "Assorted bakery biscuits", 50.0, "", "Bakery", true),
            MenuItem("fc116", "Cake", "Full bakery cake", 300.0, "", "Bakery", true),
            MenuItem("fc117", "Swiss Roll", "Bakery swiss roll", 40.0, "", "Bakery", true),
            MenuItem("fc118", "Pastry", "Bakery pastry", 40.0, "", "Bakery", true),
            MenuItem("fc119", "Pudding", "Bakery pudding", 40.0, "", "Bakery", true),
            MenuItem("fc120", "Brownie", "Chocolate brownie", 40.0, "", "Bakery", true),
            MenuItem("fc121", "Brownie with Hot Chocolate", "Brownie served hot chocolate", 50.0, "", "Bakery", true)
        )
    }


    private fun getFoodJunctionMenuItems(): List<MenuItem> {
        return listOf(
            // --- Breakfast ---
            MenuItem("fj1", "Aloo Prantha Tawa Wala", "Potato stuffed prantha, tawa style", 90.0, "", "Breakfast", true),
            MenuItem("fj2", "Mix Prantha", "Mixed vegetables prantha", 90.0, "", "Breakfast", true),
            MenuItem("fj3", "Paneer Prantha", "Paneer stuffed prantha", 90.0, "", "Breakfast", true),
            MenuItem("fj4", "Aloo Prantha Tandoor Wala", "Potato prantha, tandoor style", 100.0, "", "Breakfast", true),
            MenuItem("fj5", "Mix Prantha Tandoor Wala", "Mixed prantha, tandoor style", 100.0, "", "Breakfast", true),
            MenuItem("fj6", "Paneer Prantha Tandoor Wala", "Paneer prantha, tandoor style", 110.0, "", "Breakfast", true),
            MenuItem("fj7", "Maggi", "Classic Maggi noodles", 40.0, "", "Breakfast", true),
            MenuItem("fj8", "Veg Maggi", "Vegetable Maggi noodles", 50.0, "", "Breakfast", true),

            // --- Patties / Samosa ---
            MenuItem("fj9", "Samosa", "Simple samosa", 15.0, "", "Snacks", true),
            MenuItem("fj10", "Single Chana Samosa", "Chana with one samosa", 40.0, "", "Snacks", true),
            MenuItem("fj11", "Double Chana Samosa", "Chana with two samosas", 60.0, "", "Snacks", true),
            MenuItem("fj12", "Chole Bhature", "Chickpeas curry with fried bread", 60.0, "", "Snacks", true),
            MenuItem("fj13", "Aloo Pattie", "Potato patty", 20.0, "", "Snacks", true),
            MenuItem("fj14", "Cheese Pattie", "Cheese patty", 30.0, "", "Snacks", true),
            MenuItem("fj15", "Cheese Corn Pattie", "Cheese corn patty", 30.0, "", "Snacks", true),
            MenuItem("fj16", "Korma Pattie", "Korma patty", 40.0, "", "Snacks", true),

            // --- Rice/Fried Rice ---
            MenuItem("fj17", "Veg Fried Rice", "Vegetable fried rice", 80.0, "", "Rice", true),
            MenuItem("fj18", "Paneer Rice", "Paneer rice", 90.0, "", "Rice", true),
            MenuItem("fj19h", "Chana Rice (Half)", "Chana with rice (half)", 60.0, "", "Rice", true),
            MenuItem("fj19f", "Chana Rice (Full)", "Chana with rice (full)", 80.0, "", "Rice", true),
            MenuItem("fj20h", "Rajma Rice (Half)", "Rajma with rice (half)", 60.0, "", "Rice", true),
            MenuItem("fj20f", "Rajma Rice (Full)", "Rajma with rice (full)", 80.0, "", "Rice", true),
            MenuItem("fj21", "Mushroom Fried Rice", "Mushroom fried rice", 90.0, "", "Rice", true),

            // --- Pasta ---
            MenuItem("fj22", "Red Sauce Pasta", "Pasta in red sauce", 100.0, "", "Pasta", true),
            MenuItem("fj23", "White Sauce Pasta", "Pasta in white sauce", 100.0, "", "Pasta", true),
            MenuItem("fj24", "Mix Sauce Pasta", "Pasta in mix sauce", 100.0, "", "Pasta", true),

            // --- Noodles ---
            MenuItem("fj25", "Veg Noodles", "Veg noodles", 80.0, "", "Noodles", true),
            MenuItem("fj26", "Chilli Garlic Noodles", "Chilli garlic noodles", 90.0, "", "Noodles", true),
            MenuItem("fj27", "Hakka Noodles", "Hakka style noodles", 100.0, "", "Noodles", true),
            MenuItem("fj28", "Paneer Noodles", "Paneer noodles", 100.0, "", "Noodles", true),

            // --- Main Course (Half/Full where applicable) ---
            MenuItem("fj29h", "Shahi Paneer (Half)", "Shahi paneer (half)", 140.0, "", "Main Course", true),
            MenuItem("fj29f", "Shahi Paneer (Full)", "Shahi paneer (full)", 200.0, "", "Main Course", true),
            MenuItem("fj30h", "Kadhai Paneer (Half)", "Kadhai paneer (half)", 140.0, "", "Main Course", true),
            MenuItem("fj30f", "Kadhai Paneer (Full)", "Kadhai paneer (full)", 200.0, "", "Main Course", true),
            MenuItem("fj31h", "Paneer Butter Masala (Half)", "Paneer butter masala (half)", 150.0, "", "Main Course", true),
            MenuItem("fj31f", "Paneer Butter Masala (Full)", "Paneer butter masala (full)", 200.0, "", "Main Course", true),
            MenuItem("fj32h", "Paneer Bhurji (Half)", "Paneer bhurji (half)", 150.0, "", "Main Course", true),
            MenuItem("fj32f", "Paneer Bhurji (Full)", "Paneer bhurji (full)", 200.0, "", "Main Course", true),
            MenuItem("fj33h", "Paneer Do Pyja (Half)", "Paneer do pyja (half)", 150.0, "", "Main Course", true),
            MenuItem("fj33f", "Paneer Do Pyja (Full)", "Paneer do pyja (full)", 200.0, "", "Main Course", true),
            MenuItem("fj34h", "Rahra Paneer (Half)", "Rahra paneer (half)", 150.0, "", "Main Course", true),
            MenuItem("fj34f", "Rahra Paneer (Full)", "Rahra paneer (full)", 200.0, "", "Main Course", true),
            MenuItem("fj35h", "Masala Chaap Gravy (Half)", "Masala chaap gravy (half)", 150.0, "", "Main Course", true),
            MenuItem("fj35f", "Masala Chaap Gravy (Full)", "Masala chaap gravy (full)", 200.0, "", "Main Course", true),
            MenuItem("fj36h", "Malai Chaap Gravy (Half)", "Malai chaap gravy (half)", 150.0, "", "Main Course", true),
            MenuItem("fj36f", "Malai Chaap Gravy (Full)", "Malai chaap gravy (full)", 200.0, "", "Main Course", true),
            MenuItem("fj37", "Dal Makhani", "Dal makhani", 120.0, "", "Main Course", true),
            MenuItem("fj38", "Channa Masala", "Channa masala", 150.0, "", "Main Course", true),

            // --- Breads ---
            MenuItem("fj39", "Tandoori Roti", "Tandoori roti", 10.0, "", "Breads", true),
            MenuItem("fj40", "Butter Naan", "Butter naan", 20.0, "", "Breads", true),
            MenuItem("fj41", "Missi Roti", "Missi roti", 25.0, "", "Breads", true),
            MenuItem("fj42", "Lachha Prantha", "Lachha parantha", 25.0, "", "Breads", true),
            MenuItem("fj43", "Amritsari Naan Chole", "Amritsari naan with chole", 100.0, "", "Breads", true),

            // --- Rolls ---
            MenuItem("fj44", "Spring Roll", "Veg Spring roll", 60.0, "", "Roll", true),
            MenuItem("fj45", "Veg Roll", "Veg Roll", 60.0, "", "Roll", true),
            MenuItem("fj46", "Paneer Roll", "Paneer roll", 80.0, "", "Roll", true),
            MenuItem("fj47", "Soya Chaap Roll", "Soya chaap roll", 90.0, "", "Roll", true),
            MenuItem("fj48", "Cheese Corn Roll", "Cheese corn roll", 90.0, "", "Roll", true),
            MenuItem("fj49", "Cheese Finger", "Cheese finger", 80.0, "", "Roll", true),
            MenuItem("fj50", "Kathi Roll", "Kathi roll", 60.0, "", "Roll", true),

            // --- Chinese ---
            MenuItem("fj51", "Veg Manchurian", "Veg Manchurian", 90.0, "", "Chinese", true),
            MenuItem("fj52", "Steam Momos", "Veg Steam Momos", 60.0, "", "Chinese", true),
            MenuItem("fj53", "Fried Momos", "Veg Fried Momos", 70.0, "", "Chinese", true),
            MenuItem("fj54", "Chilly Paneer", "Chilly Paneer", 100.0, "", "Chinese", true),
            MenuItem("fj55", "Mushroom Chilli", "Mushroom Chilli", 100.0, "", "Chinese", true),

            // --- Burgers ---
            MenuItem("fj56", "Aloo Tikki Burger", "Aloo Tikki Burger", 50.0, "", "Burger", true),
            MenuItem("fj57", "Cheese Burger", "Cheese Burger", 70.0, "", "Burger", true),
            MenuItem("fj58", "Paneer Burger", "Paneer Burger", 80.0, "", "Burger", true),
            MenuItem("fj59", "King Burger", "King Burger", 90.0, "", "Burger", true),
            MenuItem("fj60", "Mexican Burger", "Mexican Burger", 90.0, "", "Burger", true),

            // --- Sandwich ---
            MenuItem("fj61", "Veg Grilled Sandwich", "Veg grilled sandwich", 70.0, "", "Sandwich", true),
            MenuItem("fj62", "Aloo Tikka Sandwich", "Veg Aloo tikka sandwich", 80.0, "", "Sandwich", true),
            MenuItem("fj63", "Cheese Corn Sandwich", "Cheese corn sandwich", 80.0, "", "Sandwich", true),
            MenuItem("fj64", "Paneer Tikka Sandwich", "Paneer tikka sandwich", 90.0, "", "Sandwich", true),
            MenuItem("fj65", "Mexican Sandwich", "Mexican sandwich", 90.0, "", "Sandwich", true),
            MenuItem("fj66", "Paneer Korma Sandwich", "Paneer korma sandwich", 100.0, "", "Sandwich", true),

            // --- Fries ---
            MenuItem("fj67", "Golden Fries", "Classic fries", 70.0, "", "Fries", true),
            MenuItem("fj68", "Chilli Potato", "Chilli potato fries", 90.0, "", "Fries", true),
            MenuItem("fj69", "Honey Chilli", "Honey Chilli fries", 90.0, "", "Fries", true),

            // --- Beverages ---
            MenuItem("fj70", "Chai", "Tea", 15.0, "", "Beverages", true),
            MenuItem("fj71", "Kuladh Chai", "Tea in clay cup", 20.0, "", "Beverages", true),
            MenuItem("fj72", "Hot Coffee", "Hot coffee", 20.0, "", "Beverages", true),
            MenuItem("fj73", "Sweet Lassi (S)", "Sweet lassi (small)", 40.0, "", "Beverages", true),
            MenuItem("fj74", "Sweet Lassi (L)", "Sweet lassi (large)", 80.0, "", "Beverages", true),
            MenuItem("fj75", "Mango Lassi", "Mango lassi", 60.0, "", "Beverages", true),
            MenuItem("fj76", "Cold Coffee", "Cold coffee", 60.0, "", "Beverages", true),
            MenuItem("fj77", "Banana Shake", "Banana shake", 60.0, "", "Beverages", true),
            MenuItem("fj78", "Butterscotch Shake", "Butterscotch shake", 60.0, "", "Beverages", true),
            MenuItem("fj79", "Kesar Pista Shake", "Kesar pista shake", 60.0, "", "Beverages", true),
            MenuItem("fj80", "Vanilla Shake", "Vanilla shake", 60.0, "", "Beverages", true),
            MenuItem("fj81", "Strawberry Shake", "Strawberry shake", 60.0, "", "Beverages", true),
            MenuItem("fj82", "Black Current Shake", "Black currant shake", 60.0, "", "Beverages", true),
            MenuItem("fj83", "Chocolate Shake", "Chocolate shake", 60.0, "", "Beverages", true),
            MenuItem("fj84", "Oreo Shake", "Oreo shake", 60.0, "", "Beverages", true),

            // --- Mojito ---
            MenuItem("fj85", "Nimbu Panni", "Lime water", 30.0, "", "Mojito", true),
            MenuItem("fj86", "Lime Soda", "Lime soda", 40.0, "", "Mojito", true),
            MenuItem("fj87", "Kala Khata", "Kala khatta mojito", 60.0, "", "Mojito", true),
            MenuItem("fj88", "Virgin Mojito", "Classic virgin mojito", 60.0, "", "Mojito", true),
            MenuItem("fj89", "Green Apple", "Green apple mojito", 60.0, "", "Mojito", true),
            MenuItem("fj90", "Ice Blue", "Ice blue mojito", 60.0, "", "Mojito", true),

            // --- Indian Thali ---
            MenuItem("fj91", "Veg Thali", "Veg Indian thali", 110.0, "", "Thali", true),
            MenuItem("fj92", "Deluxe Thali", "Deluxe thali", 150.0, "", "Thali", true),

            // --- Combo ---
            MenuItem("fj93", "Kadhai Paneer + 2 Butter Naan", "Kadhai paneer and two butter naan combo", 150.0, "", "Combo", true),
            MenuItem("fj94", "Soya Chaap Gravy + 2 Butter Naan", "Soya chaap gravy and two butter naan combo", 150.0, "", "Combo", true),
            MenuItem("fj95", "Shahi Paneer + 2 Butter Naan", "Shahi paneer and two butter naan combo", 150.0, "", "Combo", true),
            MenuItem("fj96", "Butter Masala + 2 Butter Naan", "Butter masala and two butter naan combo", 150.0, "", "Combo", true),

            // --- Cake / Pastry (as per image; available, rate not listed) ---
            // Not included in this list as no price, but available in-store
        )
    }



    private fun getHandiBiryaniMenuItems(): List<MenuItem> {
        return listOf(
            // HYD. DUM BIRYANI VEG (Half/Full)
            MenuItem("hb1", "Subj Biryani (Half)", "Hyderabadi Dum Veg Biryani (Half)", 229.0, "", "Biryani Veg", true),
            MenuItem("hb2", "Subj Biryani (Full)", "Hyderabadi Dum Veg Biryani (Full)", 339.0, "", "Biryani Veg", true),
            MenuItem("hb3", "Soya Tikka Biryani (Half)", "Soya Tikka Dum Biryani (Half)", 239.0, "", "Biryani Veg", true),
            MenuItem("hb4", "Soya Tikka Biryani (Full)", "Soya Tikka Dum Biryani (Full)", 349.0, "", "Biryani Veg", true),
            MenuItem("hb5", "Paneer Tikka Biryani (Half)", "Paneer Tikka Dum Biryani (Half)", 249.0, "", "Biryani Veg", true),
            MenuItem("hb6", "Paneer Tikka Biryani (Full)", "Paneer Tikka Dum Biryani (Full)", 359.0, "", "Biryani Veg", true),
            MenuItem("hb7", "Pindi Paneer Biryani (Half)", "Pindi Paneer Dum Biryani (Half)", 249.0, "", "Biryani Veg", true),
            MenuItem("hb8", "Pindi Paneer Biryani (Full)", "Pindi Paneer Dum Biryani (Full)", 359.0, "", "Biryani Veg", true),

            // HYD. DUM BIRYANI NON-VEG (Half/Full)
            MenuItem("hb9",  "Chicken Biryani (Half)", "Hyderabadi Dum Chicken Biryani (Half)", 259.0, "", "Biryani NonVeg", false),
            MenuItem("hb10", "Chicken Biryani (Full)", "Hyderabadi Dum Chicken Biryani (Full)", 369.0, "", "Biryani NonVeg", false),
            MenuItem("hb11", "Boneless Chicken Biryani (Half)", "Dum Boneless Chicken Biryani (Half)", 279.0, "", "Biryani NonVeg", false),
            MenuItem("hb12", "Boneless Chicken Biryani (Full)", "Dum Boneless Chicken Biryani (Full)", 379.0, "", "Biryani NonVeg", false),
            MenuItem("hb13", "Chicken Tikka Biryani (Half)", "Chicken Tikka Dum Biryani (Half)", 289.0, "", "Biryani NonVeg", false),
            MenuItem("hb14", "Chicken Tikka Biryani (Full)", "Chicken Tikka Dum Biryani (Full)", 389.0, "", "Biryani NonVeg", false),
            MenuItem("hb15", "Pindi Chicken Biryani (Half)", "Pindi Style Chicken Biryani (Half)", 269.0, "", "Biryani NonVeg", false),
            MenuItem("hb16", "Pindi Chicken Biryani (Full)", "Pindi Style Chicken Biryani (Full)", 369.0, "", "Biryani NonVeg", false),
            MenuItem("hb17", "Mutton Biryani (Half)", "Hyderabadi Dum Mutton Biryani (Half)", 299.0, "", "Biryani NonVeg", false),
            MenuItem("hb18", "Mutton Biryani (Full)", "Hyderabadi Dum Mutton Biryani (Full)", 429.0, "", "Biryani NonVeg", false),

            // VEG CHINESE
            MenuItem("hb19", "Veg. Noodles", "Veg Noodles", 80.0, "", "Veg Chinese", true),
            MenuItem("hb20", "Chilli Garlic Noodles", "Chilli Garlic Veg Noodles", 90.0, "", "Veg Chinese", true),
            MenuItem("hb21", "Hakka Noodles", "Hakka Veg Noodles", 90.0, "", "Veg Chinese", true),
            MenuItem("hb22", "Cheese Noodles", "Cheese Veg Noodles", 110.0, "", "Veg Chinese", true),
            MenuItem("hb23", "Veg Fried Rice", "Vegetable Fried Rice", 90.0, "", "Veg Chinese", true),
            MenuItem("hb24", "Chilli Garlic Fried Rice", "Chilli Garlic Fried Rice", 100.0, "", "Veg Chinese", true),
            MenuItem("hb25", "Schezwan Fries Rice", "Schezwan Fried Rice Veg", 100.0, "", "Veg Chinese", true),
            MenuItem("hb26", "Chilli Paneer", "Chilli Paneer Dry", 110.0, "", "Veg Chinese", true),
            MenuItem("hb27", "Veg Manchurian", "Dry/Gravy", 90.0, "", "Veg Chinese", true),
            MenuItem("hb28", "Honey Chilli Potato", "Crispy potato tossed with honey & chilli", 90.0, "", "Veg Chinese", true),
            MenuItem("hb29", "Honey Chilli Cauliflower", "Crispy gobhi with honey chilli sauce", 90.0, "", "Veg Chinese", true),
            MenuItem("hb30", "Spring Roll", "Veg Spring Roll", 60.0, "", "Veg Chinese", true),

            // NON-VEG CHINESE
            MenuItem("hb31", "Chicken Noodles", "Chicken Noodles", 80.0, "", "NonVeg Chinese", false),
            MenuItem("hb32", "Egg Noodles", "Egg Noodles", 90.0, "", "NonVeg Chinese", false),
            MenuItem("hb33", "Chicken Fried Rice", "Chicken Fried Rice", 110.0, "", "NonVeg Chinese", false),
            MenuItem("hb34", "Egg Fried Rice", "Egg Fried Rice", 100.0, "", "NonVeg Chinese", false),
            MenuItem("hb35", "Schezwan Chicken Fried Rice", "Schezwan Chicken Fried Rice", 120.0, "", "NonVeg Chinese", false),
            MenuItem("hb36", "Chilli Chicken Boneless", "Chilli Chicken Boneless", 120.0, "", "NonVeg Chinese", false),

            // BURGER & PATTIES
            MenuItem("hb37", "Aloo Tikki Burger", "Aloo Tikki Burger", 40.0, "", "Burger & Patties", true),
            MenuItem("hb38", "Aloo Tikki Cheese Burger", "Aloo Tikki Cheese Burger", 50.0, "", "Burger & Patties", true),
            MenuItem("hb39", "Chicken Burger", "Chicken Burger", 70.0, "", "Burger & Patties", false),
            MenuItem("hb40", "Chicken Cheese Burger", "Chicken Cheese Burger", 80.0, "", "Burger & Patties", false),
            MenuItem("hb41", "Cheese Corn Sandwich", "Cheese Corn Sandwich", 40.0, "", "Burger & Patties", true),
            MenuItem("hb42", "Paneer Tikka Sandwich", "Paneer Tikka Sandwich", 60.0, "", "Burger & Patties", true),
            MenuItem("hb43", "Aloo Patty", "Aloo Patty", 30.0, "", "Burger & Patties", true),
            MenuItem("hb44", "Cheese Corn Patty", "Cheese Corn Patty", 35.0, "", "Burger & Patties", true),
            MenuItem("hb45", "French Fries", "French Fries", 40.0, "", "Burger & Patties", true),
            MenuItem("hb46", "Masala Fries", "Masala Fries", 50.0, "", "Burger & Patties", true)
        )
    }


    private fun getKingCafeMenuItems(): List<MenuItem> {
        return listOf(
            // SPECIALS
            MenuItem("kg1", "Chicken Biryani", "A royal blend of marinated chicken and spiced rice, cooked to perfection", 150.0, "", "Special", false),

            // COMBO MEALS from image front
            MenuItem("kg2", "Veg Burger + Fries + Cake + Coke", "Veg burger with fries, cake, and coke at a deal price", 120.0, "", "Special", true),
            // Incomplete cake/ice cream deal items: price not provided in image
            MenuItem("kg3", "Honey Cake & Waffle Cake (Combo)", "2 honey cakes with waffle cake at a deal price", 0.0, "", "Special", true),
            MenuItem("kg4", "Mango Ice Cream + Hot Chocolate (Combo)", "2 mango ice creams with hot chocolate at a deal price", 0.0, "", "Special", true),

            // BREAKFAST
            MenuItem("kg5", "Samosa", "Classic fried potato samosa", 15.0, "", "Breakfast", true),
            MenuItem("kg6", "Chana Samosa", "Samosa with chana", 60.0, "", "Breakfast", true),
            MenuItem("kg7", "Aloo Paratha", "Potato stuffed paratha", 80.0, "", "Breakfast", true),
            MenuItem("kg8", "Paneer Paratha", "Paneer stuffed paratha", 90.0, "", "Breakfast", true),
            MenuItem("kg9", "Mix Paratha", "Mixed vegetable stuffed paratha", 90.0, "", "Breakfast", true),
            MenuItem("kg10", "Chole Bhature", "Chickpeas curry with fried bread", 70.0, "", "Breakfast", true),
            MenuItem("kg11", "Paneer Kulcha", "Paneer stuffed kulcha", 100.0, "", "Breakfast", true),
            MenuItem("kg12", "Aloo Kulcha", "Potato stuffed kulcha", 80.0, "", "Breakfast", true),

            // BURGER / FRIES
            MenuItem("kg13", "Aloo Tikki Burger", "Aloo tikki patty burger", 50.0, "", "Burger", true),
            MenuItem("kg14", "Cheese Burger", "Cheese veg burger", 60.0, "", "Burger", true),
            MenuItem("kg15", "Paneer Burger", "Paneer patty burger", 70.0, "", "Burger", true),
            MenuItem("kg16", "Chicken Burger", "Chicken patty burger", 80.0, "", "Burger", false),
            MenuItem("kg17", "Peri Peri Fries", "Peri peri spiced fries", 80.0, "", "Fries", true),
            MenuItem("kg18", "French Fries", "Classic French fries", 60.0, "", "Fries", true),
            MenuItem("kg19", "Chilli Potato", "Chilli tossed potato fries", 90.0, "", "Fries", true),
            MenuItem("kg20", "Honey Chilli Potato", "Sweet-spicy honey chilli potatoes", 90.0, "", "Fries", true),

            // CHINESE
            MenuItem("kg21", "Veg Momos", "Steamed vegetarian momos", 70.0, "", "Chinese", true),
            MenuItem("kg22", "Fried Momos", "Fried vegetarian momos", 80.0, "", "Chinese", true),
            MenuItem("kg23", "Paneer Momos", "Paneer stuffed momos", 100.0, "", "Chinese", true),
            MenuItem("kg24", "Paneer Fried Momos", "Fried paneer momos", 100.0, "", "Chinese", true),
            MenuItem("kg25", "Veg Noodles", "Vegetarian noodles", 80.0, "", "Chinese", true),
            MenuItem("kg26", "Chilli Garlic Noodles", "Chilli garlic noodles", 90.0, "", "Chinese", true),
            MenuItem("kg27", "Paneer Noodles", "Paneer noodles", 100.0, "", "Chinese", true),
            MenuItem("kg28", "Chicken Noodles", "Chicken noodles", 110.0, "", "Chinese", false),

            // THALI / MEALS
            MenuItem("kg29", "Veg Thali", "Full plate veg thali", 90.0, "", "Meal", true),
            MenuItem("kg30", "Paneer Thali", "Paneer curry thali", 120.0, "", "Meal", true),
            MenuItem("kg31", "Chicken Thali", "Chicken curry thali", 150.0, "", "Meal", false),
            MenuItem("kg32", "Burger + Fries + Coke", "Veg burger with fries & coke combo", 120.0, "", "Meal", true),

            // KATHI ROLL
            MenuItem("kg33", "Veg Roll", "Veg kathi roll", 50.0, "", "Kathi Roll", true),
            MenuItem("kg34", "2x Eggs Roll", "Double egg roll", 60.0, "", "Kathi Roll", false),
            MenuItem("kg35", "Paneer Roll", "Paneer kathi roll", 80.0, "", "Kathi Roll", true),
            MenuItem("kg36", "Chicken Roll", "Chicken kathi roll", 100.0, "", "Kathi Roll", false),

            // PASTA
            MenuItem("kg37", "Red Sauce Pasta", "Red sauce pasta", 100.0, "", "Pasta", true),
            MenuItem("kg38", "White Sauce Pasta", "White sauce pasta", 100.0, "", "Pasta", true),
            MenuItem("kg39", "Mix Sauce Pasta", "Mix sauce pasta", 110.0, "", "Pasta", true),

            // INDIAN MAIN COURSE (Half/Full where listed)
            MenuItem("kg40", "Chole (Half)", "Chickpea curry half", 80.0, "", "Main Course", true),
            MenuItem("kg41", "Chole (Full)", "Chickpea curry full", 150.0, "", "Main Course", true),
            MenuItem("kg42", "Shahi Paneer (Half)", "Shahi paneer half", 150.0, "", "Main Course", true),
            MenuItem("kg43", "Shahi Paneer (Full)", "Shahi paneer full", 240.0, "", "Main Course", true),
            MenuItem("kg44", "Kadhai Paneer (Half)", "Kadhai paneer half", 150.0, "", "Main Course", true),
            MenuItem("kg45", "Kadhai Paneer (Full)", "Kadhai paneer full", 240.0, "", "Main Course", true),
            MenuItem("kg46", "Paneer Butter Masala (Half)", "Paneer butter masala half", 150.0, "", "Main Course", true),
            MenuItem("kg47", "Paneer Butter Masala (Full)", "Paneer butter masala full", 240.0, "", "Main Course", true),
            MenuItem("kg48", "Kadhai Chicken (Half)", "Kadhai chicken half", 240.0, "", "Main Course", false),
            MenuItem("kg49", "Kadhai Chicken (Full)", "Kadhai chicken full", 390.0, "", "Main Course", false),
            MenuItem("kg50", "Rara Chicken (Half)", "Rara chicken half", 240.0, "", "Main Course", false),
            MenuItem("kg51", "Rara Chicken (Full)", "Rara chicken full", 390.0, "", "Main Course", false),
            MenuItem("kg52", "Butter Chicken (Half)", "Butter chicken half", 240.0, "", "Main Course", false),
            MenuItem("kg53", "Butter Chicken (Full)", "Butter chicken full", 390.0, "", "Main Course", false),

            // ROTI / NAAN
            MenuItem("kg54", "Roti", "Tawa roti", 12.0, "", "Bread", true),
            MenuItem("kg55", "Butter Roti", "Butter tawa roti", 15.0, "", "Bread", true),
            MenuItem("kg56", "Naan", "Tandoor naan", 25.0, "", "Bread", true),

            // RICE
            MenuItem("kg57", "Fried Rice", "Veg fried rice", 90.0, "", "Rice", true),
            MenuItem("kg58", "Chicken Fried Rice", "Chicken fried rice", 100.0, "", "Rice", false),
            MenuItem("kg59", "Paneer Fried Rice", "Paneer fried rice", 100.0, "", "Rice", true),
            MenuItem("kg60", "Egg Fried Rice", "Egg fried rice", 100.0, "", "Rice", false),

            // BEVERAGES (all rates as on menu)
            MenuItem("kg61", "Spl. Tea", "Special tea", 15.0, "", "Beverages", true),
            MenuItem("kg62", "Coffee", "Hot coffee", 60.0, "", "Beverages", true),
            MenuItem("kg63", "Cold Coffee", "Cold coffee", 60.0, "", "Beverages", true),
            MenuItem("kg64", "Banana Shake", "Banana shake", 60.0, "", "Beverages", true),
            MenuItem("kg65", "Strawberry Shake", "Strawberry shake", 60.0, "", "Beverages", true),
            MenuItem("kg66", "Mango Shake", "Mango shake", 60.0, "", "Beverages", true),
            MenuItem("kg67", "Oreo Shake", "Oreo shake", 60.0, "", "Beverages", true)
        )
    }


    private fun getBuddiesMenuItems(): List<MenuItem> {
        return listOf(
            // Veg Combo's
            MenuItem("bd1", "Rajmah + Rice (Half)", "Rajma with rice, half portion", 60.0, "", "Veg Combo", true),
            MenuItem("bd2", "Rajmah + Rice (Full)", "Rajma with rice, full portion", 80.0, "", "Veg Combo", true),
            MenuItem("bd3", "Channa + Rice (Half)", "Channa with rice, half portion", 60.0, "", "Veg Combo", true),
            MenuItem("bd4", "Channa + Rice (Full)", "Channa with rice, full portion", 80.0, "", "Veg Combo", true),
            MenuItem("bd6", "Dal Makhni + Rice (Full)", "Dal mahkni with rice, full portion", 100.0, "", "Veg Combo", true),
            MenuItem("bd7", "Paneer + Rice (Half)", "Paneer curry with rice, half portion", 80.0, "", "Veg Combo", true),
            MenuItem("bd8", "Paneer + Rice (Full)", "Paneer curry with rice, full portion", 120.0, "", "Veg Combo", true),
            MenuItem("bd9", "Veg Biryani", "Vegetable biryani", 130.0, "", "Veg Combo", true),
            MenuItem("bd10", "Paneer Biryani", "Paneer biryani", 150.0, "", "Veg Combo", true),

            // Veg Thali
            MenuItem("bd11", "Dal Makhni Thali", "Dal makhni based veg thali", 100.0, "", "Veg Thali", true),
            MenuItem("bd12", "Spl. Veg Thali", "Special vegetarian thali", 110.0, "", "Veg Thali", true),
            MenuItem("bd13", "Kadhai Paneer Thali", "Kadhai paneer based thali", 140.0, "", "Veg Thali", true),
            MenuItem("bd14", "Paneer B. Masala Thali", "Paneer butter masala thali", 140.0, "", "Veg Thali", true),

            // Non-Veg Thali
            MenuItem("bd15", "Kadhai Chicken Thali", "Kadhai chicken thali", 170.0, "", "Non-Veg Thali", false),
            MenuItem("bd16", "Butter Chicken Thali", "Butter chicken thali", 170.0, "", "Non-Veg Thali", false),
            MenuItem("bd17", "Masala Chicken Thali", "Masala chicken thali", 170.0, "", "Non-Veg Thali", false),
            MenuItem("bd18", "Tawa Chicken Thali", "Tawa chicken thali", 170.0, "", "Non-Veg Thali", false),
            MenuItem("bd19", "Chicken Biryani", "Chicken biryani", 170.0, "", "Non-Veg Thali", false),
            MenuItem("bd20", "Chicken Curry Rice", "Chicken curry with rice", 120.0, "", "Non-Veg Thali", false),
            MenuItem("bd21", "Egg Curry Rice", "Egg curry with rice", 120.0, "", "Non-Veg Thali", false),

            // Veg Main Course (Half/Full split)
            MenuItem("bd22", "Dal Makhni (Half)", "Dal makhni, half", 120.0, "", "Veg Main Course", true),
            MenuItem("bd23", "Dal Makhni (Full)", "Dal makhni, full", 180.0, "", "Veg Main Course", true),
            MenuItem("bd24", "Mix Veg", "Mixed vegetable curry", 110.0, "", "Veg Main Course", true),
            MenuItem("bd25", "Channa Masala", "Channa masala curry", 110.0, "", "Veg Main Course", true),
            MenuItem("bd26", "Rajmah", "Rajmah curry", 110.0, "", "Veg Main Course", true),
            MenuItem("bd27", "Kadhai Paneer", "Kadhai paneer", 150.0, "", "Veg Main Course", true),
            MenuItem("bd28", "Shahi Paneer (Half)", "Shahi paneer, half", 150.0, "", "Veg Main Course", true),
            MenuItem("bd29", "Shahi Paneer (Full)", "Shahi paneer, full", 240.0, "", "Veg Main Course", true),
            MenuItem("bd30", "Paneer B. Masala (Half)", "Paneer butter masala, half", 150.0, "", "Veg Main Course", true),
            MenuItem("bd31", "Paneer B. Masala (Full)", "Paneer butter masala, full", 240.0, "", "Veg Main Course", true),
            MenuItem("bd32", "Paneer Tikka B.M (Half)", "Paneer tikka butter masala, half", 150.0, "", "Veg Main Course", true),
            MenuItem("bd33", "Paneer Tikka B.M (Full)", "Paneer tikka butter masala, full", 240.0, "", "Veg Main Course", true),
            MenuItem("bd34", "Soya Tikka B.M (Half)", "Soya tikka butter masala, half", 150.0, "", "Veg Main Course", true),
            MenuItem("bd35", "Soya Tikka B.M (Full)", "Soya tikka butter masala, full", 240.0, "", "Veg Main Course", true),

            // Breads
            MenuItem("bd36", "Tandoori Roti", "Classic tandoori roti", 10.0, "", "Breads", true),
            MenuItem("bd37", "Butter Naan", "Butter glazed naan", 35.0, "", "Breads", true),
            MenuItem("bd38", "Garlic Naan", "Garlic flavored naan", 40.0, "", "Breads", true),
            MenuItem("bd39", "Lachha Parantha", "Multi-layered lachha parantha", 35.0, "", "Breads", true),
            MenuItem("bd40", "Chole Bhature", "Chole with bhature", 70.0, "", "Breads", true),
            MenuItem("bd41", "Aloo Kulcha Amritsari", "Aloo stuffed kulcha Amritsari", 100.0, "", "Breads", true),
            MenuItem("bd42", "Paneer Kulcha Amritsari", "Paneer stuffed kulcha Amritsari", 120.0, "", "Breads", true),
            MenuItem("bd43", "Cheese Naan with Gravy", "Cheese naan served with gravy", 140.0, "", "Breads", true),
            MenuItem("bd44", "Aloo Parantha", "Aloo stuffed parantha", 50.0, "", "Breads", true),
            MenuItem("bd45", "Paneer Parantha", "Paneer stuffed parantha", 60.0, "", "Breads", true),

            // Non-Veg Main Course (Half/Full split)
            MenuItem("bd46", "Tandoori Roti Chicken (Half)", "Tandoori roti with chicken, half", 250.0, "", "Non-Veg Main Course", false),
            MenuItem("bd47", "Tandoori Roti Chicken (Full)", "Tandoori roti with chicken, full", 430.0, "", "Non-Veg Main Course", false),
            MenuItem("bd48", "Butter Chicken (Half)", "Butter chicken, half", 250.0, "", "Non-Veg Main Course", false),
            MenuItem("bd49", "Butter Chicken (Full)", "Butter chicken, full", 430.0, "", "Non-Veg Main Course", false),
            MenuItem("bd50", "Masala Chicken (Half)", "Masala chicken, half", 250.0, "", "Non-Veg Main Course", false),
            MenuItem("bd51", "Masala Chicken (Full)", "Masala chicken, full", 430.0, "", "Non-Veg Main Course", false),
            MenuItem("bd52", "Chicken Curry (Half)", "Chicken curry, half", 180.0, "", "Non-Veg Main Course", false),
            MenuItem("bd53", "Chicken Curry (Full)", "Chicken curry, full", 320.0, "", "Non-Veg Main Course", false),
            MenuItem("bd54", "Afghani Chicken (Half)", "Afghani chicken, half", 250.0, "", "Non-Veg Main Course", false),
            MenuItem("bd55", "Afghani Chicken (Full)", "Afghani chicken, full", 450.0, "", "Non-Veg Main Course", false),
            MenuItem("bd56", "Chicken Biryani", "Chicken biryani", 170.0, "", "Non-Veg Main Course", false),
            MenuItem("bd57", "Egg Curry Rice", "Egg curry with rice", 120.0, "", "Non-Veg Main Course", false),

            // Veg Snacks
            MenuItem("bd58", "Tandoori Paneer Tikka", "Tandoori paneer tikka", 150.0, "", "Veg Snacks", true),
            MenuItem("bd59", "Malai Soya Tikka", "Malai soya tikka", 150.0, "", "Veg Snacks", true),
            MenuItem("bd60", "Masala Soya Tikka", "Masala soya tikka", 150.0, "", "Veg Snacks", true),
            MenuItem("bd61", "Chilly Paneer (Dry)", "Dry chili paneer", 170.0, "", "Veg Snacks", true),
            MenuItem("bd62", "Chilly Paneer (Gravy)", "Gravy chili paneer", 180.0, "", "Veg Snacks", true),
            MenuItem("bd63", "Veg Manchurian (Dry)", "Dry veg manchurian", 170.0, "", "Veg Snacks", true),
            MenuItem("bd64", "Veg Manchurian (Gravy)", "Gravy veg manchurian", 180.0, "", "Veg Snacks", true),

            // Non-Veg Snacks
            MenuItem("bd65", "Tandoori Chicken Tikka", "Tandoori chicken tikka", 250.0, "", "Non-Veg Snacks", false),
            MenuItem("bd66", "Butter Mala Tikka", "Butter mala chicken tikka", 280.0, "", "Non-Veg Snacks", false),
            MenuItem("bd67", "Malai Tikka", "Malai chicken tikka", 250.0, "", "Non-Veg Snacks", false),
            MenuItem("bd68", "Chicken Seekh Tikka", "Chicken seekh kebab", 250.0, "", "Non-Veg Snacks", false),
            MenuItem("bd69", "Chilly Chicken (Dry)", "Dry chili chicken", 270.0, "", "Non-Veg Snacks", false),
            MenuItem("bd70", "Chilly Chicken (Gravy)", "Gravy chili chicken", 280.0, "", "Non-Veg Snacks", false)
        )
    }


    private fun getLordsFoodMenuItems(): List<MenuItem> {
        return listOf(
            // SAMOSA & PATTY
            MenuItem("lf1", "Aloo Samosa", "Potato stuffed fried samosa", 15.0, "", "Samosa & Patty", true),
            MenuItem("lf2", "Chole Samosa", "Samosa served with chole", 30.0, "", "Samosa & Patty", true),
            MenuItem("lf3", "Double Chole Samosa", "Double samosa with chole", 50.0, "", "Samosa & Patty", true),
            MenuItem("lf4", "Veg Patty", "Vegetable patty", 25.0, "", "Samosa & Patty", true),
            MenuItem("lf5", "Cheese Patty", "Cheese filled patty", 30.0, "", "Samosa & Patty", true),
            MenuItem("lf6", "Cheese Corn Patty", "Cheese and corn patty", 35.0, "", "Samosa & Patty", true),

            // BURGER
            MenuItem("lf7", "Veg Burger", "Classic veg burger", 50.0, "", "Burger", true),
            MenuItem("lf8", "Samosa Burger", "Burger with samosa patty", 50.0, "", "Burger", true),
            MenuItem("lf9", "Noodles Burger", "Burger with noodle stuffing", 60.0, "", "Burger", true),
            MenuItem("lf10", "Cheese Burger", "Burger with cheese slice", 60.0, "", "Burger", true),
            MenuItem("lf11", "Spice Paneer Burger", "Spicy paneer burger", 60.0, "", "Burger", true),

            // PRANTHA
            MenuItem("lf12", "Aloo Prantha", "Potato stuffed prantha", 40.0, "", "Prantha", true),
            MenuItem("lf13", "Gobhi Prantha", "Cauliflower stuffed prantha", 40.0, "", "Prantha", true),
            MenuItem("lf14", "Mix Prantha", "Mixed vegetable prantha", 40.0, "", "Prantha", true),
            MenuItem("lf15", "Paneer Prantha", "Paneer stuffed prantha", 45.0, "", "Prantha", true),

            // NOODLES
            MenuItem("lf16", "Veg Noodles", "Vegetarian noodles", 80.0, "", "Noodles", true),
            MenuItem("lf17", "Chilli Garlic Noodles", "Noodles with chilli and garlic", 90.0, "", "Noodles", true),
            MenuItem("lf18", "Hakka Noodles", "Hakka style veg noodles", 90.0, "", "Noodles", true),
            MenuItem("lf19", "Paneer Noodles", "Paneer tossed noodles", 100.0, "", "Noodles", true),
            MenuItem("lf20", "Veg Chaap Noodles", "Noodles with veg chaap", 100.0, "", "Noodles", true),

            // MOMOS
            MenuItem("lf21", "Veg Momos", "Steamed veg momos", 60.0, "", "Momos", true),
            MenuItem("lf22", "Cheese Momos", "Steamed cheese momos", 90.0, "", "Momos", true),
            MenuItem("lf23", "Steam Momos", "Steamed vegetarian momos", 60.0, "", "Momos", true),
            MenuItem("lf24", "Pav Bhaji (4pcs)", "Pav bhaji with 4 pav", 80.0, "", "Momos", true),

            // RICE & BIRYANI
            MenuItem("lf25", "Veg Biryani", "Vegetarian biryani", 100.0, "", "Rice & Biryani", true),
            MenuItem("lf26", "Cheese Biryani", "Biryani with cheese", 120.0, "", "Rice & Biryani", true),
            MenuItem("lf27", "Rajma Rice", "Rajma curry with rice", 80.0, "", "Rice & Biryani", true),
            MenuItem("lf28", "Channa Rice (Half)", "Channa with rice - half", 60.0, "", "Rice & Biryani", true),
            MenuItem("lf29", "Channa Rice (Full)", "Channa with rice - full", 80.0, "", "Rice & Biryani", true),
            MenuItem("lf30", "Fried Rice (Half)", "Fried rice - half plate", 70.0, "", "Rice & Biryani", true),
            MenuItem("lf31", "Fried Rice (Full)", "Fried rice - full plate", 100.0, "", "Rice & Biryani", true),
            MenuItem("lf32", "Veg Fried Rice", "Vegetable fried rice", 100.0, "", "Rice & Biryani", true),

            // SANDWICH
            MenuItem("lf33", "Veggi Grilled Sandwich", "Vegetable grilled sandwich", 80.0, "", "Sandwich", true),
            MenuItem("lf34", "Paneer Tikka Sandwich", "Sandwich with paneer tikka", 90.0, "", "Sandwich", true),
            MenuItem("lf35", "Cheese Grilled Sandwich", "Grilled cheese sandwich", 90.0, "", "Sandwich", true),
            MenuItem("lf36", "French Fries", "Classic French fries", 60.0, "", "Sandwich", true),
            MenuItem("lf37", "Masala French Fries", "French fries with masala", 70.0, "", "Sandwich", true),
            MenuItem("lf38", "Peri Peri Fries", "Spicy peri peri fries", 80.0, "", "Sandwich", true),

            // KULCHA
            MenuItem("lf39", "Cheese Kulcha", "Cheese stuffed kulcha", 100.0, "", "Kulcha", true),
            MenuItem("lf40", "Chole Kulcha", "Kulcha with chole", 90.0, "", "Kulcha", true),
            MenuItem("lf41", "Chaap Kulcha", "Soya chaap stuffed kulcha", 110.0, "", "Kulcha", true),
            MenuItem("lf42", "Chole Bhature", "Chole bhature", 90.0, "", "Kulcha", true),

            // SNACKS
            MenuItem("lf43", "Spring Roll", "Vegetarian spring roll", 60.0, "", "Snacks", true),
            MenuItem("lf44", "Honey Chilly Potato", "Sweet spicy potato fries", 80.0, "", "Snacks", true),
            MenuItem("lf45", "Honey Chilly Cauliflower", "Honey chili gobhi fry", 90.0, "", "Snacks", true),
            MenuItem("lf46", "Veg Dry Chaap", "Soya chaap dry", 120.0, "", "Snacks", true),

            // COMBO+KULCHA
            MenuItem("lf47", "Kadai Paneer (2 Tawa Roti)", "Kadai paneer combo with 2 tawa roti", 110.0, "", "Combo+Kulcha", true),
            MenuItem("lf48", "Shahi Paneer (2 Tawa Roti)", "Shahi paneer combo with 2 tawa roti", 110.0, "", "Combo+Kulcha", true),
            MenuItem("lf49", "Gravy Paneer", "Paneer with gravy", 100.0, "", "Combo+Kulcha", true),
            MenuItem("lf50", "Paneer Bhurji", "Scrambled paneer", 110.0, "", "Combo+Kulcha", true),

            // SHAKES (all veg)
            MenuItem("lf51", "Strawberry Shake", "Strawberry flavored shake", 60.0, "", "Shake", true),
            MenuItem("lf52", "Mango Shake", "Mango shake", 60.0, "", "Shake", true),
            MenuItem("lf53", "Banana Shake", "Banana shake", 60.0, "", "Shake", true),
            MenuItem("lf54", "Oreo Shake", "Oreo biscuit shake", 70.0, "", "Shake", true),
            MenuItem("lf55", "Kitkat Shake", "Kitkat chocolate shake", 70.0, "", "Shake", true),
            MenuItem("lf56", "Chocolate Shake", "Chocolate shake", 70.0, "", "Shake", true),
            MenuItem("lf57", "Butter Scotch Shake", "Butterscotch shake", 70.0, "", "Shake", true),
            MenuItem("lf58", "Papaya Shake", "Papaya shake", 70.0, "", "Shake", true),
            MenuItem("lf59", "Cold Coffee", "Cold coffee", 70.0, "", "Shake", true),
            MenuItem("lf60", "Virgin Mojito", "Virgin mint mojito", 70.0, "", "Shake", true),
            MenuItem("lf61", "Blue Lagoon Mojito", "Blue lagoon flavor mojito", 70.0, "", "Shake", true),
            MenuItem("lf62", "Mint Mojito", "Mint flavored mojito", 70.0, "", "Shake", true),
            MenuItem("lf63", "Green Apple Mojito", "Green apple mojito", 70.0, "", "Shake", true),

            // TEA/COFFEE/DRINKS
            MenuItem("lf64", "Tea", "Classic hot tea", 15.0, "", "Beverages", true),
            MenuItem("lf65", "Hot Coffee", "Hot coffee", 30.0, "", "Beverages", true),
            MenuItem("lf66", "Nimbu Pani", "Lemonade", 30.0, "", "Beverages", true),
            MenuItem("lf67", "Nimbu Lemon Soda", "Lemon soda", 35.0, "", "Beverages", true),
            MenuItem("lf68", "Roohafza Water", "Roohafza flavored drink", 35.0, "", "Beverages", true),
            MenuItem("lf69", "Roohafza Milk", "Roohafza milk drink", 50.0, "", "Beverages", true),
            MenuItem("lf70", "Sweet Lassi", "Sweet lassi/yogurt drink", 50.0, "", "Beverages", true),
            MenuItem("lf71", "Namkeen Lassi", "Salted lassi/yogurt drink", 50.0, "", "Beverages", true)
        )
    }


    private fun getCrunchyMenuItems(): List<MenuItem> {
        return listOf(
            // VEG ROLL
            MenuItem("cru1", "Veg Roll", "Vegetable roll", 50.0, "", "Roll", true),
            MenuItem("cru2", "Cheese Roll (Paneer)", "Cheese (paneer) roll", 70.0, "", "Roll", true),
            MenuItem("cru3", "Veg Cheese Roll", "Vegetable roll with cheese", 70.0, "", "Roll", true),
            MenuItem("cru4", "Veg Noodles Roll", "Vegetable noodles stuffed roll", 70.0, "", "Roll", true),
            MenuItem("cru5", "Soya Chaap Roll", "Soya chaap stuffed roll", 70.0, "", "Roll", true),
            MenuItem("cru6", "Soya Noodles Roll", "Soya with noodles stuffed roll", 70.0, "", "Roll", true),
            MenuItem("cru7", "Cheese Noodless Roll", "Cheesy noodles stuffed roll", 80.0, "", "Roll", true),
            MenuItem("cru8", "Double Cheese Roll", "Double cheese stuffed roll", 80.0, "", "Roll", true),
            MenuItem("cru9", "Double Cheese Soya Roll", "Double cheese with soya roll", 80.0, "", "Roll", true),
            MenuItem("cru10", "Cheese Chilli Roll", "Cheese roll with chilli", 80.0, "", "Roll", true),

            // EGG ROLL
            MenuItem("cru11", "Egg Roll", "Egg stuffed roll", 60.0, "", "Roll", false),
            MenuItem("cru12", "Double Egg Roll", "Double egg stuffed roll", 80.0, "", "Roll", false),
            MenuItem("cru13", "Egg Cheese Roll", "Egg and cheese stuffed roll", 70.0, "", "Roll", false),
            MenuItem("cru14", "Double Egg Cheese Roll", "Double egg and cheese stuffed roll", 90.0, "", "Roll", false),
            MenuItem("cru15", "Egg Noodles Roll", "Egg and noodles stuffed roll", 70.0, "", "Roll", false),
            MenuItem("cru16", "Egg Veg Roll", "Egg and vegetable roll", 70.0, "", "Roll", false),
            MenuItem("cru17", "Double Egg Veg Roll", "Double egg and vegetable roll", 80.0, "", "Roll", false),
            MenuItem("cru18", "Double Egg Soya Chaap Roll", "Double egg and soya chaap roll", 80.0, "", "Roll", false),
            MenuItem("cru19", "Double Egg Veg Cheese Roll", "Double egg, veg, and cheese roll", 90.0, "", "Roll", false),

            // CHICKEN ROLL
            MenuItem("cru20", "Chicken Roll", "Chicken stuffed roll", 90.0, "", "Roll", false),
            MenuItem("cru21", "Chicken Cheese Roll", "Chicken and cheese roll", 100.0, "", "Roll", false),
            MenuItem("cru22", "Chicken Noodles Roll", "Chicken noodles roll", 100.0, "", "Roll", false),
            MenuItem("cru23", "Egg Chicken Roll", "Egg and chicken roll", 100.0, "", "Roll", false),
            MenuItem("cru24", "Egg Chicken Cheese Roll", "Egg, chicken, and cheese roll", 110.0, "", "Roll", false),
            MenuItem("cru25", "Double Egg Chicken Roll", "Double egg and chicken roll", 110.0, "", "Roll", false),
            MenuItem("cru26", "Double Egg Chicken Cheese Roll", "Double egg, chicken, and cheese roll", 120.0, "", "Roll", false),
            MenuItem("cru27", "Double Chicken Roll", "Double chicken roll", 110.0, "", "Roll", false),
            MenuItem("cru28", "Double Chicken Cheese Roll", "Double chicken and cheese roll", 120.0, "", "Roll", false),
            MenuItem("cru29", "Chilli Chicken Roll", "Chilli chicken stuffed roll", 110.0, "", "Roll", false),
            MenuItem("cru30", "Double Egg Double Chicken Cheese Roll", "Double egg, double chicken, and cheese roll", 130.0, "", "Roll", false),

            // CRUNCHY SPECIAL
            MenuItem("cru31", "Kulcha Tawa Chicken/Lachha Parantha", "Kulcha or lachha parantha with tawa chicken", 100.0, "", "Crunchy Special", false),
            MenuItem("cru32", "Kulcha Chaap Masala/Lachha Parantha", "Kulcha or lachha parantha with chaap masala", 100.0, "", "Crunchy Special", true),

            // RICE
            MenuItem("cru33", "Veg Fried Rice", "Vegetarian fried rice", 70.0, "", "Rice", true),
            MenuItem("cru34", "Egg Fried Rice", "Egg fried rice", 90.0, "", "Rice", false),
            MenuItem("cru35", "Soya Chaap Fried Rice", "Soya chaap fried rice", 100.0, "", "Rice", true),
            MenuItem("cru36", "Cheese Fried Rice", "Cheese fried rice", 100.0, "", "Rice", true),
            MenuItem("cru37", "Chicken Fried Rice", "Chicken fried rice", 120.0, "", "Rice", false),

            // RICE WITH GRAVY
            MenuItem("cru38", "Veg Rice with Gravy", "Vegetable rice with gravy", 90.0, "", "Rice with Gravy", true),
            MenuItem("cru39", "Chicken Rice with Gravy", "Chicken rice with gravy", 130.0, "", "Rice with Gravy", false),

            // CHINESE
            MenuItem("cru40", "Chilli Chicken (Boneless)", "Boneless chilli chicken", 200.0, "", "Chinese", false),
            MenuItem("cru41", "Chilli Chicken (Boneless)", "Boneless chilli chicken (full)", 300.0, "", "Chinese", false),
            MenuItem("cru42", "Garlic Chicken (Boneless)", "Boneless garlic chicken", 230.0, "", "Chinese", false),
            MenuItem("cru43", "Garlic Chicken (Boneless)", "Boneless garlic chicken (full)", 330.0, "", "Chinese", false),
            MenuItem("cru44", "Cheese Chilli", "Cheese chilli", 180.0, "", "Chinese", true),
            MenuItem("cru45", "Soya Chilli", "Soya chilli", 180.0, "", "Chinese", true),

            // NOODLES
            MenuItem("cru46", "Veg Noodles", "Vegetable noodles", 90.0, "", "Noodles", true),
            MenuItem("cru47", "Hakka Noodles", "Hakka style noodles", 90.0, "", "Noodles", true),
            MenuItem("cru48", "Chilly Garlic Noodles", "Chilli and garlic noodles", 90.0, "", "Noodles", true),
            MenuItem("cru49", "Soya Chaap Noodles", "Soya chaap noodles", 100.0, "", "Noodles", true),
            MenuItem("cru50", "Egg Noodles", "Egg noodles", 100.0, "", "Noodles", false),
            MenuItem("cru51", "Cheese Noodles", "Cheese noodles", 110.0, "", "Noodles", true),
            MenuItem("cru52", "Chicken Noodles", "Chicken noodles", 120.0, "", "Noodles", false)
            // Cold Drinks on MRP not included, as price not on menu.
        )
    }


    private fun getCreativeCafeMenuItems(): List<MenuItem> {
        return listOf(
            // Juices (with sizes)
            MenuItem("cr1", "Mix Juice (Small)", "Mixed fruit juice - small", 50.0, "", "Juices", true),
            MenuItem("cr2", "Mix Juice (Medium)", "Mixed fruit juice - medium", 80.0, "", "Juices", true),
            MenuItem("cr3", "Mix Juice (Large)", "Mixed fruit juice - large", 100.0, "", "Juices", true),

            MenuItem("cr4", "Pineapple Juice (Small)", "Fresh pineapple juice - small", 60.0, "", "Juices", true),
            MenuItem("cr5", "Pineapple Juice (Medium)", "Fresh pineapple juice - medium", 90.0, "", "Juices", true),
            MenuItem("cr6", "Pineapple Juice (Large)", "Fresh pineapple juice - large", 110.0, "", "Juices", true),

            MenuItem("cr7", "Musami Juice (Small)", "Musami fruit juice - small", 60.0, "", "Juices", true),
            MenuItem("cr8", "Musami Juice (Medium)", "Musami fruit juice - medium", 90.0, "", "Juices", true),
            MenuItem("cr9", "Musami Juice (Large)", "Musami fruit juice - large", 110.0, "", "Juices", true),

            MenuItem("cr10", "Anaar Juice (Small)", "Pomegranate juice - small", 150.0, "", "Juices", true),
            MenuItem("cr11", "Anaar Juice (Medium)", "Pomegranate juice - medium", 230.0, "", "Juices", true),
            MenuItem("cr12", "Anaar Juice (Large)", "Pomegranate juice - large", 280.0, "", "Juices", true),

            MenuItem("cr13", "Orange Juice (Small)", "Orange juice - small (seasonal)", 50.0, "", "Juices", true),
            MenuItem("cr14", "Orange Juice (Medium)", "Orange juice - medium (seasonal)", 80.0, "", "Juices", true),
            MenuItem("cr15", "Orange Juice (Large)", "Orange juice - large (seasonal)", 100.0, "", "Juices", true),

            MenuItem("cr16", "Carrot Juice (Small)", "Carrot juice - small", 40.0, "", "Juices", true),
            MenuItem("cr17", "Carrot Juice (Medium)", "Carrot juice - medium", 60.0, "", "Juices", true),
            MenuItem("cr18", "Carrot Juice (Large)", "Carrot juice - large", 80.0, "", "Juices", true),

            // Mojitos (all one size)
            MenuItem("cr19", "Green Apple Mojito", "Refreshing green apple mojito", 60.0, "", "Mojito", true),
            MenuItem("cr20", "Mint Mojito", "Classic mint mojito", 60.0, "", "Mojito", true),
            MenuItem("cr21", "Icey Blue", "Blue mojito", 60.0, "", "Mojito", true),
            MenuItem("cr22", "Tango Mango", "Mango mojito", 60.0, "", "Mojito", true),
            MenuItem("cr23", "Black Lime Fusion", "Black lime fusion mojito", 60.0, "", "Mojito", true),
            MenuItem("cr24", "Watermelon Mojito", "Fresh watermelon mojito", 60.0, "", "Mojito", true),
            MenuItem("cr25", "Malt King", "Malt king mojito", 60.0, "", "Mojito", true),

            // Milkshakes
            MenuItem("cr26", "Banana Shake", "Banana milkshake", 50.0, "", "Milkshake", true),
            MenuItem("cr27", "Papaya Shake", "Papaya milkshake", 50.0, "", "Milkshake", true),
            MenuItem("cr28", "Mango Shake", "Mango milkshake", 50.0, "", "Milkshake", true),
            MenuItem("cr29", "Cold Coffee", "Chilled coffee shake", 50.0, "", "Milkshake", true),
            MenuItem("cr30", "Strawberry Shake", "Strawberry milkshake", 60.0, "", "Milkshake", true),
            MenuItem("cr31", "Chocolate Shake", "Chocolate milkshake", 60.0, "", "Milkshake", true),
            MenuItem("cr32", "Butter Scotch Shake", "Butterscotch flavored milkshake", 60.0, "", "Milkshake", true),
            MenuItem("cr33", "Red Velvet Shake", "Red velvet flavored milkshake", 70.0, "", "Milkshake", true),
            MenuItem("cr34", "Oreo Shake", "Oreo flavored milkshake", 70.0, "", "Milkshake", true),
            MenuItem("cr35", "Blueberry Shake", "Blueberry milkshake", 70.0, "", "Milkshake", true),

            // Grilled Sandwich
            MenuItem("cr36", "Corn Cheese Sandwich", "Corn and cheese grilled sandwich", 80.0, "", "Grilled Sandwich", true),
            MenuItem("cr37", "Paneer Tikka Sandwich", "Paneer tikka grilled sandwich", 80.0, "", "Grilled Sandwich", true),

            // SNACKS
            MenuItem("cr38", "Spring Rolls", "Veg spring rolls", 70.0, "", "Snacks", true),
            MenuItem("cr39", "Momos Steamed/Fried", "Vegetarian momos (steamed or fried)", 70.0, "", "Snacks", true),
            MenuItem("cr40", "Paneer Kurkure", "Crispy paneer snack", 110.0, "", "Snacks", true),
            MenuItem("cr41", "Paneer Momos", "Paneer stuffed momos", 100.0, "", "Snacks", true),
            MenuItem("cr42", "Veg Kurkure Momos", "Veg kurkure momos", 100.0, "", "Snacks", true),
            MenuItem("cr43", "Cheese Finger", "Cheesy finger snack", 110.0, "", "Snacks", true),
            MenuItem("cr44", "Cheesy Jalapino", "Cheesy jalapeno snack", 90.0, "", "Snacks", true),
            MenuItem("cr45", "Mushroom Duplex", "Mushroom duplex snack", 110.0, "", "Snacks", true),
            MenuItem("cr46", "Honey Chilli Potato", "Honey chilli tossed potato", 100.0, "", "Snacks", true),
            MenuItem("cr47", "Chilli Potato", "Chilli tossed potato", 100.0, "", "Snacks", true),
            MenuItem("cr48", "Mexican Fries", "Mexican-style fries", 90.0, "", "Snacks", true),
            MenuItem("cr49", "Cheese Fries", "Cheese loaded fries", 90.0, "", "Snacks", true),
            MenuItem("cr50", "Peri Peri Fries", "Peri peri spiced fries", 90.0, "", "Snacks", true),

            // BULLETS
            MenuItem("cr51", "Veg Bullet", "Vegetarian bullet snack", 60.0, "", "Bullets", true),
            MenuItem("cr52", "Cheese Bullet", "Cheesy bullet snack", 80.0, "", "Bullets", true),
            MenuItem("cr53", "Peri Peri Bullets", "Peri peri flavored bullets", 90.0, "", "Bullets", true),
            MenuItem("cr54", "Mexican Bullet", "Mexican-style bullet snack", 90.0, "", "Bullets", true),
            MenuItem("cr55", "Garlic Cheese Bullet", "Garlic cheese filled bullet snack", 90.0, "", "Bullets", true),

            // NOODLES
            MenuItem("cr56", "Veg Noodles", "Stir fried vegetarian noodles", 80.0, "", "Noodles", true),
            MenuItem("cr57", "Chilli Garlic Noodles", "Spicy chilli garlic noodles", 90.0, "", "Noodles", true),
            MenuItem("cr58", "Cheese Noodles", "Cheese flavored noodles", 100.0, "", "Noodles", true),
            MenuItem("cr59", "Hakka Noodles", "Hakka style stir fried noodles", 100.0, "", "Noodles", true),

            // PASTA
            MenuItem("cr60", "Pasta in Red Sauce", "Pasta in tangy red sauce", 90.0, "", "Pasta", true),
            MenuItem("cr61", "Pasta in White Sauce", "Pasta in creamy white sauce", 90.0, "", "Pasta", true),
            MenuItem("cr62", "Pasta in Mix Sauce", "Pasta in mixed sauces", 100.0, "", "Pasta", true)
        )
    }

    private fun getChaiThekhaMenuItems(): List<MenuItem> {
        return listOf(
            // Chai & Hot Drinks - All Sizes
            MenuItem("ct1s", "Desi Thara (Small)", "Classic desi tea in small kulhad", 17.0, "", "Tea", true),
            MenuItem("ct1m", "Desi Thara (Medium)", "Classic desi tea in medium kulhad", 23.0, "", "Tea", true),
            MenuItem("ct1l", "Desi Thara (Large/Kulhad)", "Classic desi tea in large kulhad", 29.0, "", "Tea", true),

            MenuItem("ct2s", "Classic Tea (Small)", "Classic milk tea, small", 11.0, "", "Tea", true),
            MenuItem("ct2m", "Classic Tea (Medium)", "Classic milk tea, medium", 14.0, "", "Tea", true),
            MenuItem("ct2l", "Classic Tea (Large/Regular)", "Classic milk tea, large", 19.0, "", "Tea", true),

            MenuItem("ct3s", "Masala Tea (Small)", "Masala flavored tea, small", 13.0, "", "Tea", true),
            MenuItem("ct3m", "Masala Tea (Medium)", "Masala flavored tea, medium", 16.0, "", "Tea", true),
            MenuItem("ct3l", "Masala Tea (Large/Regular)", "Masala tea, large", 21.0, "", "Tea", true),

            MenuItem("ct4s", "Mint Tea (Small)", "Mint flavored tea, small", 13.0, "", "Tea", true),
            MenuItem("ct4m", "Mint Tea (Medium)", "Mint flavored tea, medium", 16.0, "", "Tea", true),
            MenuItem("ct4l", "Mint Tea (Large/Regular)", "Mint tea, large", 22.0, "", "Tea", true),

            MenuItem("ct5s", "Ginger Tea (Small)", "Ginger flavored tea, small", 13.0, "", "Tea", true),
            MenuItem("ct5m", "Ginger Tea (Medium)", "Ginger flavored tea, medium", 16.0, "", "Tea", true),
            MenuItem("ct5l", "Ginger Tea (Large/Regular)", "Ginger tea, large", 22.0, "", "Tea", true),

            MenuItem("ct6s", "Lemon Tea (Small)", "Lemon flavored tea, small", 13.0, "", "Tea", true),
            MenuItem("ct6m", "Lemon Tea (Medium)", "Lemon flavored tea, medium", 16.0, "", "Tea", true),
            MenuItem("ct6l", "Lemon Tea (Large/Regular)", "Lemon tea, large", 22.0, "", "Tea", true),

            MenuItem("ct7s", "Elaichi Tea (Small)", "Cardamom flavored tea, small", 15.0, "", "Tea", true),
            MenuItem("ct7m", "Elaichi Tea (Medium)", "Cardamom flavored tea, medium", 18.0, "", "Tea", true),
            MenuItem("ct7l", "Elaichi Tea (Large/Regular)", "Cardamom tea, large", 24.0, "", "Tea", true),

            // Dessert
            MenuItem("ct8", "Choco Lava", "Chocolate lava cake", 59.0, "", "Dessert", true),
            MenuItem("ct9", "Chocolava with Ice Cream", "Lava cake with scoop of ice cream", 89.0, "", "Dessert", true),

            // Bun Maska & Snacks
            MenuItem("ct10", "Bun Maska", "Buttered bun", 37.0, "", "Snacks", true),
            MenuItem("ct11", "Desi Chaska Maska", "Bun Maska with desi chaska", 77.0, "", "Snacks", true),
            MenuItem("ct12", "French Fries", "Classic French fries", 72.0, "", "Snacks", true),

            // Sandwiches & Subs
            MenuItem("ct13", "Veggie Sub Sandwich", "Vegetable sub sandwich", 99.0, "", "Sandwich", true),
            MenuItem("ct14", "Cheese Corn Sub", "Corn and cheese sub sandwich", 109.0, "", "Sandwich", true),
            MenuItem("ct15", "Paneer Tikka Sub", "Paneer tikka sub sandwich", 129.0, "", "Sandwich", true),
            MenuItem("ct16", "Theka Spicy Paneer Sub", "Spicy paneer sub sandwich", 129.0, "", "Sandwich", true),

            // Brown Bread Sandwich
            MenuItem("ct17", "Corn Cheese", "Corn and cheese sandwich", 119.0, "", "Sandwich", true),
            MenuItem("ct18", "Cheese", "Cheese sandwich", 109.0, "", "Sandwich", true),
            MenuItem("ct19", "Paneer", "Paneer sandwich", 129.0, "", "Sandwich", true),
            MenuItem("ct20", "Veggie Sub", "Veg sub sandwich", 99.0, "", "Sandwich", true),

            // Burgers
            MenuItem("ct21", "Theka Burger", "Classic Theka veggie burger", 62.0, "", "Burger", true),
            MenuItem("ct22", "Cheese Burger", "Veg burger with cheese", 72.0, "", "Burger", true),
            MenuItem("ct23", "Paneer Tikka Burger", "Paneer tikka burger", 89.0, "", "Burger", true),
            MenuItem("ct24", "Pushpraj Burger (No Jhatka Burger)", "Veg burger (special)", 99.0, "", "Burger", true),

            // Pizza (Stone Baked)
            MenuItem("ct25", "Cheese Margherita", "Classic cheese pizza", 179.0, "", "Pizza", true),
            MenuItem("ct26", "Farm House", "Veggie topped pizza", 189.0, "", "Pizza", true),
            MenuItem("ct27", "Veggie Supreme", "Premium veg pizza", 199.0, "", "Pizza", true),
            MenuItem("ct28", "Paneer Tikka", "Paneer tikka pizza", 199.0, "", "Pizza", true),

            // Garlic Bread (3pcs)
            MenuItem("ct29", "Plain Garlic Bread", "Plain garlic bread", 79.0, "", "Garlic Bread", true),
            MenuItem("ct30", "Paneer & Garlic", "Paneer and garlic bread", 99.0, "", "Garlic Bread", true),
            MenuItem("ct31", "Veggie & Bread", "Veggie and garlic bread", 99.0, "", "Garlic Bread", true),

            // Pasta
            MenuItem("ct32", "Red Sauce Pasta", "Pasta with tangy red sauce", 119.0, "", "Pasta", true),
            MenuItem("ct33", "White Sauce Pasta", "Pasta with creamy white sauce", 129.0, "", "Pasta", true),
            MenuItem("ct34", "Mix Sauce Pasta", "Pasta in mixed sauces", 139.0, "", "Pasta", true),
            MenuItem("ct35", "Alfredo", "Classic Alfredo white sauce pasta", 129.0, "", "Pasta", true),

            // Kulhad Pizza
            MenuItem("ct36", "Veggies Kulhad Pizza", "Vegetable kulhad based pizza", 129.0, "", "Pizza", true),
            MenuItem("ct37", "Kulhad Tandoori Paneer", "Paneer based kulhad pizza", 149.0, "", "Pizza", true),

            // Combos
            MenuItem("ct38", "Theka Party Platter", "Two pizzas, 1 burger, 4 slices garlic bread, 2 pastas, 2 cones, French fries, softy", 299.0, "", "Combo", true),

            // Maggi
            MenuItem("ct39", "Regular Maggi", "Classic plain Maggi noodles", 41.0, "", "Maggi", true),
            MenuItem("ct40", "Masala Maggi", "Spicy masala Maggi", 47.0, "", "Maggi", true),
            MenuItem("ct41", "Butter Veggie Maggi", "Butter sautéed veggie Maggi", 61.0, "", "Maggi", true),
            MenuItem("ct42", "Cheese Veggie Maggi", "Cheese added veggie Maggi", 69.0, "", "Maggi", true),

            // Summer Coolers
            MenuItem("ct43", "Nimbu Pani", "Lemon fresh drink", 49.0, "", "Cooler", true),
            MenuItem("ct44", "Blue Lagoon", "Blue mocktail", 59.0, "", "Cooler", true),
            MenuItem("ct45", "Green Apple", "Green apple cooler", 59.0, "", "Cooler", true),
            MenuItem("ct46", "Mango Mule", "Mango mocktail", 59.0, "", "Cooler", true),

            // Mocktail
            MenuItem("ct47", "Mint Mojito", "Minty refreshing mocktail", 59.0, "", "Mocktail", true),
            MenuItem("ct48", "Strawberry Mojito", "Strawberry flavored mocktail", 69.0, "", "Mocktail", true),
            MenuItem("ct49", "Lemon Ice Tea", "Ice tea with lemon", 39.0, "", "Mocktail", true),

            // Softy
            MenuItem("ct50", "Softy", "Vanilla/strawberry/chocolate cone", 31.0, "", "Softy", true),

            // Cool Dude (Cold Coffee)
            MenuItem("ct51", "Cafe Coffee (regular)", "Classic cold coffee", 52.0, "", "Cold Coffee", true),
            MenuItem("ct52", "Cold Coffee with Ice Cream", "Cold coffee topped with ice cream", 68.0, "", "Cold Coffee", true),
            MenuItem("ct53", "Cold Dude", "Special desi cold drink", 69.0, "", "Cold Coffee", true),

            // The Shake We Desi (Shakes)
            MenuItem("ct54", "Red Velvet", "Red velvet flavored shake", 88.0, "", "Shake", true),
            MenuItem("ct55", "Butterscotch", "Butterscotch flavored shake", 88.0, "", "Shake", true),
            MenuItem("ct56", "Kitkat", "Kitkat flavored shake", 92.0, "", "Shake", true),
            MenuItem("ct57", "Oreo", "Oreo cookie shake", 92.0, "", "Shake", true),
            MenuItem("ct58", "Strawberry", "Strawberry shake", 88.0, "", "Shake", true),
            MenuItem("ct59", "Kesar Pista", "Saffron pista shake", 88.0, "", "Shake", true),

            // Chocolate Shakes
            MenuItem("ct60", "Rich Chocolate", "Rich chocolate shake", 69.0, "", "Shake", true),
            MenuItem("ct61", "Kitkat Shake", "Kitkat chocolate shake", 92.0, "", "Shake", true),
            MenuItem("ct62", "Caramel Delight", "Caramel flavored chocolate shake", 92.0, "", "Shake", true)
        )
    }

    private fun getVikasConfectioneryMenuItems(): List<MenuItem> {
        return listOf(
            // Indian Food / Rice
            MenuItem("vc1", "Rajma Rice", "Rajma curry with rice", 60.0, "", "Indian Food", true),
            MenuItem("vc2", "Paneer Rice", "Paneer curry with rice", 80.0, "", "Indian Food", true),
            MenuItem("vc3", "Veg Fried Rice", "Vegetable fried rice", 80.0, "", "Indian Food", true),
            MenuItem("vc4", "Egg Fried Rice", "Egg fried rice", 90.0, "", "Indian Food", false),
            MenuItem("vc5", "Cheese Fried Rice", "Cheese fried rice", 90.0, "", "Indian Food", true),
            MenuItem("vc6", "Aloo Parantha with Butter", "Potato stuffed parantha with butter", 100.0, "", "Indian Food", true),

            // Snacks - Noodles & Momos
            MenuItem("vc7", "Veg Noodles", "Stir-fried vegetarian noodles", 60.0, "", "Snacks", true),
            MenuItem("vc8", "Egg Noodles", "Egg noodles", 70.0, "", "Snacks", false),
            MenuItem("vc9", "Cheese Noodles", "Cheese tossed noodles", 80.0, "", "Snacks", true),
            MenuItem("vc10", "Veg Burger", "Classic vegetarian burger", 50.0, "", "Snacks", true),
            MenuItem("vc11", "Cheese Burger", "Cheese burger", 60.0, "", "Snacks", true),
            MenuItem("vc12", "Chicken Burger", "Chicken burger", 80.0, "", "Snacks", false),
            MenuItem("vc13", "Paneer Tikki Burger", "Paneer tikki patty burger", 60.0, "", "Snacks", true),
            MenuItem("vc14", "Veg Bullets", "Fried veg bullet snack", 60.0, "", "Snacks", true),
            MenuItem("vc15", "Paneer Bullets (8 pcs)", "Paneer-filled crispy bullets (8 pieces)", 90.0, "", "Snacks", true),
            MenuItem("vc16", "Spring Roll", "Vegetable spring roll", 60.0, "", "Snacks", true),
            MenuItem("vc17", "Fried/Steamed Momos", "Choice of fried or steamed veg momos", 60.0, "", "Snacks", true),

            // Pasta
            MenuItem("vc18", "Red Sauce Pasta", "Pasta in red sauce", 80.0, "", "Snacks", true),
            MenuItem("vc19", "White Sauce Pasta", "Pasta in creamy white sauce", 80.0, "", "Snacks", true),
            MenuItem("vc20", "Mix Sauce Pasta", "Pasta in mixed sauces", 100.0, "", "Snacks", true),
            MenuItem("vc21", "French Fries", "Golden French fries", 50.0, "", "Snacks", true),
            MenuItem("vc22", "Chilli Potato", "Chilli tossed potato fingers", 70.0, "", "Snacks", true),
            MenuItem("vc23", "Cold Sandwich", "Classic cold sandwich", 50.0, "", "Snacks", true),
            MenuItem("vc24", "Veg Grilled Sandwich", "Grilled veg sandwich", 60.0, "", "Snacks", true),
            MenuItem("vc25", "Paneer Korma Sandwich", "Sandwich filled with paneer korma", 70.0, "", "Snacks", true),
            MenuItem("vc26", "Corn Sandwich", "Sandwich filled with sweet corn", 60.0, "", "Snacks", true),
            MenuItem("vc27", "Cheese Sandwich", "Cheese grilled sandwich", 70.0, "", "Snacks", true),
            MenuItem("vc28", "Egg Bhurji (2 eggs)", "Scrambled egg bhurji (2 eggs)", 50.0, "", "Snacks", false),
            MenuItem("vc29", "Veg Maggi", "Vegetable Maggi noodles", 50.0, "", "Snacks", true),
            MenuItem("vc30", "Egg Omelet (2 eggs)", "Egg omelet (2 eggs)", 40.0, "", "Snacks", false),
            MenuItem("vc31", "Bread Omelet", "Egg omelet with bread", 50.0, "", "Snacks", false),

            // Tea & Coffee
            MenuItem("vc32", "Tea", "Classic hot tea", 15.0, "", "Tea/Coffee", true),
            MenuItem("vc33", "Hot Coffee", "Hot brewed coffee", 30.0, "", "Tea/Coffee", true),

            // Patties
            MenuItem("vc34", "Aloo Patty", "Potato stuffed patty", 20.0, "", "Patties", true),
            MenuItem("vc35", "Cheese Patty", "Cheese stuffed patty", 30.0, "", "Patties", true),
            MenuItem("vc36", "Corn Patty", "Corn stuffed patty", 40.0, "", "Patties", true),
            MenuItem("vc37", "Pasta Patty", "Pasta stuffed patty", 30.0, "", "Patties", true),
            MenuItem("vc38", "Paneer Tikka Patty", "Paneer tikka patty", 40.0, "", "Patties", true),

            // Shakes (add +10 for ice cream)
            MenuItem("vc39", "Kitkat Shake", "Kitkat flavored shake", 60.0, "", "Shake", true),
            MenuItem("vc40", "Oreo Shake", "Oreo flavored shake", 70.0, "", "Shake", true),
            MenuItem("vc41", "Banana Shake", "Banana milkshake", 60.0, "", "Shake", true),
            MenuItem("vc42", "Mango Shake", "Mango milkshake", 60.0, "", "Shake", true),
            MenuItem("vc43", "Chocolate Shake", "Chocolate shake", 60.0, "", "Shake", true),
            MenuItem("vc44", "Butter Scotch Shake", "Butterscotch flavored shake", 60.0, "", "Shake", true),
            MenuItem("vc45", "Black Current Shake", "Blackcurrant flavored shake", 60.0, "", "Shake", true),
            MenuItem("vc46", "Strawberry Shake", "Strawberry flavored shake", 60.0, "", "Shake", true),
            MenuItem("vc47", "Vanilla Shake", "Vanilla milkshake", 50.0, "", "Shake", true),
            MenuItem("vc48", "Sweet/Salty Lassi", "Sweet or salty yogurt lassi", 50.0, "", "Shake", true),
            MenuItem("vc49", "Lemon Soda", "Refreshing lemon soda", 30.0, "", "Shake", true),
            MenuItem("vc50", "Masala Soda", "Spiced masala soda", 40.0, "", "Shake", true),
            MenuItem("vc51", "Cold Coffee", "Chilled cold coffee", 50.0, "", "Shake", true),
            MenuItem("vc52", "Virgin Mojito", "Virgin mint mojito cooler", 60.0, "", "Shake", true),
            MenuItem("vc53", "Ice Blue Mojito", "Blue mint mojito cooler", 60.0, "", "Shake", true),

            // Ice Cream (see board - display price or MRP for packed items)
            MenuItem("vc54", "Ice Cream", "Assorted ice cream (as per selection)", 40.0, "", "Ice Cream", true),
            // Cold Drinks (assume sold at MRP)
            MenuItem("vc55", "Cold Drinks", "Bottled soft drink (MRP)", 0.0, "", "Cold Drinks", true)
        )
    }

    private fun getUniqueFoodsMenuItems(): List<MenuItem> {
        return listOf(
            // BREAKFAST
            MenuItem("uf1", "Aloo Prantha", "Potato stuffed paratha", 40.0, "", "Breakfast", true),
            MenuItem("uf2", "Mix Prantha", "Mixed vegetable paratha", 50.0, "", "Breakfast", true),
            MenuItem("uf3", "Gobhi Prantha", "Cauliflower stuffed paratha", 40.0, "", "Breakfast", true),
            MenuItem("uf4", "Mulli Prantha", "Radish stuffed paratha", 50.0, "", "Breakfast", true),
            MenuItem("uf5", "Paneer Prantha", "Paneer stuffed paratha", 60.0, "", "Breakfast", true),

            // BEVERAGES
            MenuItem("uf6", "Tea", "Classic hot tea", 15.0, "", "Beverages", true),
            MenuItem("uf7", "Hot Coffee", "Hot brewed coffee", 30.0, "", "Beverages", true),
            MenuItem("uf8", "Cold Drinks", "Bottled cold drink (MRP)", 0.0, "", "Beverages", true),
            MenuItem("uf9", "Water Bottle", "Packaged drinking water (MRP)", 0.0, "", "Beverages", true),
            MenuItem("uf10", "Lassi", "Sweet or salted yogurt drink (MRP)", 0.0, "", "Beverages", true),
            MenuItem("uf11", "Fruit Beer", "Non-alcoholic fruit beer (MRP)", 0.0, "", "Beverages", true),

            // COMBO
            MenuItem("uf12", "Rajmah Rice", "Rajma curry with rice", 70.0, "", "Combo", true),
            MenuItem("uf13", "Kadhi Rice", "Kadhi with rice", 70.0, "", "Combo", true),
            MenuItem("uf14", "Chana Rice", "Chole with rice", 70.0, "", "Combo", true),
            MenuItem("uf15", "Panner Rice", "Paneer curry with rice", 100.0, "", "Combo", true),
            MenuItem("uf16", "Veg Biryani with Raita", "Veg biryani with raita", 100.0, "", "Combo", true),
            MenuItem("uf17", "Channa Bhatura", "Chana masala with bhatura", 70.0, "", "Combo", true),
            MenuItem("uf18", "Chana Kulcha", "Chana with kulcha", 70.0, "", "Combo", true),

            // SHAKES
            MenuItem("uf19", "Vanilla Shake", "Vanilla flavored milkshake", 60.0, "", "Shake", true),
            MenuItem("uf20", "Strawberry Shake", "Strawberry flavored milkshake", 70.0, "", "Shake", true),
            MenuItem("uf21", "Butter Scotch Shake", "Butterscotch flavored milkshake", 70.0, "", "Shake", true),
            MenuItem("uf22", "Oreo Shake", "Oreo biscuit milkshake", 80.0, "", "Shake", true),
            MenuItem("uf23", "Kit Kat Shake", "Kit Kat chocolate shake", 80.0, "", "Shake", true),
            MenuItem("uf24", "Chocolate Shake", "Chocolate milkshake", 80.0, "", "Shake", true),

            // LASSI
            MenuItem("uf25", "Sweet Lassi", "Sweet yogurt drink", 60.0, "", "Lassi", true),
            MenuItem("uf26", "Namkeen Lassi", "Salted yogurt drink", 50.0, "", "Lassi", true),

            // ROLLS
            MenuItem("uf27", "Veg Roll", "Vegetable roll", 50.0, "", "Rolls", true),
            MenuItem("uf28", "Paneer Roll", "Paneer stuffed roll", 70.0, "", "Rolls", true),
            MenuItem("uf29", "Manchurian Roll", "Vegetable manchurian roll", 80.0, "", "Rolls", true),
            MenuItem("uf30", "Chaap Roll", "Soya chaap roll", 70.0, "", "Rolls", true),
            MenuItem("uf31", "Paneer Malai Tikka Roll", "Paneer malai tikka roll", 80.0, "", "Rolls", true),
            MenuItem("uf32", "Malai Chaap Roll", "Malai soya chaap roll", 80.0, "", "Rolls", true),

            // THALI
            MenuItem("uf33", "Thali", "Regular vegetarian thali", 100.0, "", "Thali", true),
            MenuItem("uf34", "Special Thali", "Special vegetarian thali", 140.0, "", "Thali", true),

            // SNACKS
            MenuItem("uf35", "Spring Roll", "Vegetable spring roll", 50.0, "", "Snacks", true),
            MenuItem("uf36", "Veg Manchurian", "Vegetable manchurian", 80.0, "", "Snacks", true),
            MenuItem("uf37", "Cheese Chilli", "Cheese chilli stir fry", 100.0, "", "Snacks", true),
            MenuItem("uf38", "Red Sauce Pasta", "Pasta in red sauce", 100.0, "", "Snacks", true),
            MenuItem("uf39", "White Sauce Pasta", "Pasta in white sauce", 110.0, "", "Snacks", true),
            MenuItem("uf40", "Mix Sauce Pasta", "Pasta in mixed sauces", 110.0, "", "Snacks", true),
            MenuItem("uf41", "Grilled Sandwich", "Grilled sandwich", 70.0, "", "Snacks", true),
            MenuItem("uf42", "Veg Burger", "Vegetarian burger", 60.0, "", "Snacks", true),
            MenuItem("uf43", "Veg Momos", "Steamed vegetable momos", 60.0, "", "Snacks", true),
            MenuItem("uf44", "Paneer Momos", "Steamed paneer momos", 80.0, "", "Snacks", true),
            MenuItem("uf45", "Honey Chilli Potato", "Honey chilli potato fingers", 70.0, "", "Snacks", true),
            MenuItem("uf46", "Honey Chilli Cauliflower", "Honey chilli gobhi", 80.0, "", "Snacks", true),
            MenuItem("uf47", "Veg Noodle", "Vegetarian noodles", 90.0, "", "Snacks", true),
            MenuItem("uf48", "Paneer Noodle", "Paneer tossed noodles", 110.0, "", "Snacks", true),
            MenuItem("uf49", "Veg Patties", "Vegetable patties", 20.0, "", "Snacks", true),
            MenuItem("uf50", "Paneer Patty", "Paneer stuffed patty", 40.0, "", "Snacks", true),
            MenuItem("uf51", "Veg Fried Rice", "Vegetable fried rice", 60.0, "", "Snacks", true),
            MenuItem("uf52", "French Fries", "French fries", 60.0, "", "Snacks", true),
            MenuItem("uf53", "Masala Fries", "Spicy masala fries", 70.0, "", "Snacks", true),
            MenuItem("uf54", "Paneer Fried Rice", "Paneer fried rice", 80.0, "", "Snacks", true)
        )
    }

    private fun getTrendingMenuItems(): List<MenuItem> {
        return listOf(
            // Tandoori Paranthas
            MenuItem("tr1", "Aloo Parantha", "Tandoori potato stuffed parantha", 40.0, "", "Tandoori Parantha", true),
            MenuItem("tr2", "Aloo Pyaz Parantha", "Tandoori potato & onion stuffed parantha", 50.0, "", "Tandoori Parantha", true),
            MenuItem("tr3", "Pyaz Parantha", "Tandoori onion stuffed parantha", 50.0, "", "Tandoori Parantha", true),
            MenuItem("tr4", "Gobhi Parantha", "Tandoori cauliflower stuffed parantha", 50.0, "", "Tandoori Parantha", true),
            MenuItem("tr5", "Paneer Parantha", "Tandoori paneer stuffed parantha", 70.0, "", "Tandoori Parantha", true),

            // Indian Combo
            MenuItem("tr6", "Rajma Rice", "Rajma curry with rice", 70.0, "", "Indian Combo", true),
            MenuItem("tr7", "Channa Rice", "Chole curry with rice", 70.0, "", "Indian Combo", true),
            MenuItem("tr8", "Yellow Dal Rice", "Yellow dal with rice", 70.0, "", "Indian Combo", true),
            MenuItem("tr9", "Dal Makhni Rice", "Dal makhani with rice", 80.0, "", "Indian Combo", true),
            MenuItem("tr10", "Paneer Rice", "Paneer curry with rice", 120.0, "", "Indian Combo", true),
            MenuItem("tr11", "Chicken Curry Rice", "Chicken curry with rice", 150.0, "", "Indian Combo", false),

            // Snacks
            MenuItem("tr12", "Bullet", "Veg bullet snack", 70.0, "", "Snacks", true),
            MenuItem("tr13", "Cheesy Bullet", "Cheese filled veg bullet snack", 90.0, "", "Snacks", true),
            MenuItem("tr14", "Chilli Potato", "Spicy fried potato", 80.0, "", "Snacks", true),
            MenuItem("tr15", "Honey Chilli Potato", "Sweet and spicy fried potato", 90.0, "", "Snacks", true),
            MenuItem("tr16", "Momos", "Vegetarian momos", 70.0, "", "Snacks", true),
            MenuItem("tr17", "Veg Spring Roll", "Vegetable spring roll", 70.0, "", "Snacks", true),
            MenuItem("tr18", "Manchurian Dry/Gravy", "Dry or gravy veg manchurian", 130.0, "", "Snacks", true),

            // Burgers (all visible are veg unless stated)
            MenuItem("tr19", "Aloo Tikki Burger", "Aloo tikki patty burger", 50.0, "", "Burger", true),
            MenuItem("tr20", "Veggie Blast Burger", "Loaded vegetable burger", 60.0, "", "Burger", true),
            MenuItem("tr21", "Spicy Grilled Burger", "Spicy grilled veg burger", 70.0, "", "Burger", true),
            MenuItem("tr22", "Cheesy Grilled Burger", "Cheesy grilled veg burger", 80.0, "", "Burger", true),
            MenuItem("tr23", "Load Plus Burger", "Special loaded burger", 90.0, "", "Burger", true),
            MenuItem("tr24", "Chicken Burger", "Classic chicken burger", 90.0, "", "Burger", false),
            MenuItem("tr25", "Spicy Chicken Burger", "Spicy chicken burger", 100.0, "", "Burger", false),
            MenuItem("tr26", "Chicken Grilled Burger", "Grilled chicken burger", 100.0, "", "Burger", false),
            MenuItem("tr27", "Chicken Crispy Roll", "Crispy chicken roll", 100.0, "", "Burger", false),

            // Sandwiches
            MenuItem("tr28", "Veg Grilled Sandwich", "Vegetarian grilled sandwich", 100.0, "", "Sandwich", true),
            MenuItem("tr29", "Corn Sandwich", "Sweet corn sandwich", 120.0, "", "Sandwich", true),
            MenuItem("tr30", "Paneer Sandwich", "Paneer stuffed sandwich", 120.0, "", "Sandwich", true),
            MenuItem("tr31", "Mushroom Sandwich", "Mushroom stuffed sandwich", 120.0, "", "Sandwich", true),
            MenuItem("tr32", "Chicken Grilled Sandwich", "Chicken filled grilled sandwich", 120.0, "", "Sandwich", false),

            // French Fries
            MenuItem("tr33", "Golden Fries", "Classic potato fries", 70.0, "", "French Fries", true),
            MenuItem("tr34", "Peri Peri Fries", "Spicy peri peri fries", 90.0, "", "French Fries", true),
            MenuItem("tr35", "Cheesy Fries", "Fries with cheese sauce", 100.0, "", "French Fries", true),

            // Beverages
            MenuItem("tr36", "Cold Coffee", "Chilled coffee", 70.0, "", "Beverages", true),
            MenuItem("tr37", "Chocolate Shake", "Rich chocolate shake", 70.0, "", "Beverages", true),
            MenuItem("tr38", "Chocolate Oreo Shake", "Oreo chocolate shake", 80.0, "", "Beverages", true),
            MenuItem("tr39", "Strawberry Shake", "Fresh strawberry shake", 70.0, "", "Beverages", true),
            MenuItem("tr40", "Black Currant Shake", "Black currant shake", 70.0, "", "Beverages", true),
            MenuItem("tr41", "Butter Scotch Shake", "Butterscotch flavored shake", 70.0, "", "Beverages", true),
            MenuItem("tr42", "Virgin Mojito", "Mint virgin mojito", 70.0, "", "Beverages", true),
            MenuItem("tr43", "Green Apple", "Green apple cooler", 70.0, "", "Beverages", true),
            MenuItem("tr44", "Icey Blue", "Blue mocktail", 80.0, "", "Beverages", true),
            MenuItem("tr45", "Spicy Mango", "Spicy mango drink", 80.0, "", "Beverages", true),

            // Kulhad Items
            MenuItem("tr46", "Kulhad Tea", "Masala tea served in clay cup", 20.0, "", "Beverages", true),
            MenuItem("tr47", "Kulhad Coffee", "Coffee served in clay cup", 30.0, "", "Beverages", true),

            // Noodles (Half/Full split)
            MenuItem("tr48", "Veg Noodles (Half)", "Veg noodles half portion", 70.0, "", "Noodles", true),
            MenuItem("tr49", "Veg Noodles (Full)", "Veg noodles full portion", 120.0, "", "Noodles", true),
            MenuItem("tr50", "Chilli Garlic Noodle (Half)", "Chilli garlic noodles half portion", 80.0, "", "Noodles", true),
            MenuItem("tr51", "Chilli Garlic Noodle (Full)", "Chilli garlic noodles full portion", 130.0, "", "Noodles", true),
            MenuItem("tr52", "Paneer Noodles (Half)", "Paneer noodles half portion", 90.0, "", "Noodles", true),
            MenuItem("tr53", "Paneer Noodles (Full)", "Paneer noodles full portion", 150.0, "", "Noodles", true),
            MenuItem("tr54", "Mushroom Noodle (Half)", "Mushroom noodles half portion", 90.0, "", "Noodles", true),
            MenuItem("tr55", "Mushroom Noodle (Full)", "Mushroom noodles full portion", 150.0, "", "Noodles", true),
            MenuItem("tr56", "Hakka Noodle (Half)", "Hakka noodles half portion", 70.0, "", "Noodles", true),
            MenuItem("tr57", "Hakka Noodle (Full)", "Hakka noodles full portion", 120.0, "", "Noodles", true),
            MenuItem("tr58", "Special Noodles (Half)", "Special noodles half portion", 90.0, "", "Noodles", true),
            MenuItem("tr59", "Special Noodles (Full)", "Special noodles full portion", 150.0, "", "Noodles", true),
            MenuItem("tr60", "Egg Noodles (Half)", "Egg noodles half portion", 80.0, "", "Noodles", false),
            MenuItem("tr61", "Egg Noodles (Full)", "Egg noodles full portion", 130.0, "", "Noodles", false),
            MenuItem("tr62", "Chicken Noodles (Half)", "Chicken noodles half portion", 100.0, "", "Noodles", false),
            MenuItem("tr63", "Chicken Noodles (Full)", "Chicken noodles full portion", 180.0, "", "Noodles", false),

            // Tandoori Veg
            MenuItem("tr64", "Masala Chaap", "Tandoori masala soya chaap", 150.0, "", "Tandoori Veg", true),
            MenuItem("tr65", "Malai Chaap", "Tandoori malai soya chaap", 150.0, "", "Tandoori Veg", true),
            MenuItem("tr66", "Achari Chaap", "Tandoori achari soya chaap", 150.0, "", "Tandoori Veg", true),
            MenuItem("tr67", "Paneer Tikka", "Tandoori paneer tikka", 250.0, "", "Tandoori Veg", true),

            // Tandoori Snacks
            MenuItem("tr68", "Tandoori Chicken (Half)", "Tandoori chicken half", 250.0, "", "Tandoori Snacks", false),
            MenuItem("tr69", "Tandoori Chicken (Full)", "Tandoori chicken full", 430.0, "", "Tandoori Snacks", false),
            MenuItem("tr70", "Afghani Chicken (Half)", "Afghani style chicken half", 280.0, "", "Tandoori Snacks", false),
            MenuItem("tr71", "Afghani Chicken (Full)", "Afghani style chicken full", 450.0, "", "Tandoori Snacks", false),

            // Chinese Combos
            MenuItem("tr72", "Noodle + Manchurian", "Noodles served with manchurian", 130.0, "", "Chinese Combos", true),
            MenuItem("tr73", "Fried Rice + Manchurian", "Fried rice served with manchurian", 130.0, "", "Chinese Combos", true),

            // Veg Thali
            MenuItem("tr74", "Veg Thali", "Standard veg thali", 90.0, "", "Veg Thali", true),
            MenuItem("tr75", "Dal Makhani Thali", "Dal makhani thali", 120.0, "", "Veg Thali", true),
            MenuItem("tr76", "Deluxe Paneer Thali", "Paneer based deluxe thali", 130.0, "", "Veg Thali", true),

            // Special Combo
            MenuItem("tr77", "Amritsari Kulcha Chana", "Amritsari kulcha with chana", 100.0, "", "Special Combo", true),
            MenuItem("tr78", "Cheese Naan With Gravy", "Cheese naan & gravy combo", 130.0, "", "Special Combo", true),
            MenuItem("tr79", "Cholle Bhature", "Cholle with bhature", 70.0, "", "Special Combo", true),

            // Non Veg Thali
            MenuItem("tr80", "Masala Chicken Thali", "Masala chicken thali", 170.0, "", "Non Veg Thali", false),
            MenuItem("tr81", "Kadhai Chicken Thali", "Kadhai chicken thali", 170.0, "", "Non Veg Thali", false),
            MenuItem("tr82", "Butter Chicken Thali", "Butter chicken thali", 170.0, "", "Non Veg Thali", false),

            // Veg Fried Rice (Half/Full)
            MenuItem("tr83", "Veg Fried Rice (Half)", "Vegetable fried rice half", 50.0, "", "Rice", true),
            MenuItem("tr84", "Veg Fried Rice (Full)", "Vegetable fried rice full", 90.0, "", "Rice", true),
            MenuItem("tr85", "Chilli Garlic Fried Rice (Half)", "Chilli garlic fried rice half", 70.0, "", "Rice", true),
            MenuItem("tr86", "Chilli Garlic Fried Rice (Full)", "Chilli garlic fried rice full", 120.0, "", "Rice", true),
            MenuItem("tr87", "Mushroom Fried Rice (Half)", "Mushroom fried rice half", 80.0, "", "Rice", true),
            MenuItem("tr88", "Mushroom Fried Rice (Full)", "Mushroom fried rice full", 130.0, "", "Rice", true),
            MenuItem("tr89", "Special Fried Rice (Half)", "Special fried rice half", 80.0, "", "Rice", true),
            MenuItem("tr90", "Special Fried Rice (Full)", "Special fried rice full", 130.0, "", "Rice", true),

            // Non-Veg Fried Rice
            MenuItem("tr91", "Egg Fried Rice (Half)", "Egg fried rice half", 80.0, "", "Rice", false),
            MenuItem("tr92", "Egg Fried Rice (Full)", "Egg fried rice full", 130.0, "", "Rice", false),
            MenuItem("tr93", "Chicken Fried Rice (Half)", "Chicken fried rice half", 100.0, "", "Rice", false),
            MenuItem("tr94", "Chicken Fried Rice (Full)", "Chicken fried rice full", 180.0, "", "Rice", false),

            // Pasta
            MenuItem("tr95", "Red Sauce Pasta", "Pasta with red sauce", 100.0, "", "Pasta", true),
            MenuItem("tr96", "White Sauce Pasta", "Pasta with white sauce", 130.0, "", "Pasta", true),
            MenuItem("tr97", "Mix Sauce Pasta", "Pasta with mix red and white sauce", 150.0, "", "Pasta", true),

            // Indian Main Course
            MenuItem("tr98", "Yellow Dal Tadka", "Yellow dal tadka", 140.0, "", "Indian Main Course", true),
            MenuItem("tr99", "Dal Makhani", "Dal makhani", 170.0, "", "Indian Main Course", true),
            MenuItem("tr100", "Rajma Raseela", "Raseela rajma curry", 170.0, "", "Indian Main Course", true),
            MenuItem("tr101", "Chana Masala", "Chana masala curry", 170.0, "", "Indian Main Course", true),
            MenuItem("tr102", "Mix Veg", "Mix veg curry", 170.0, "", "Indian Main Course", true),
            MenuItem("tr103", "Jeera Aloo", "Jeera aloo dry", 170.0, "", "Indian Main Course", true),
            MenuItem("tr104", "Masala Chaap Gravy", "Soya chaap masala gravy", 200.0, "", "Indian Main Course", true),
            MenuItem("tr105", "Shahi Paneer", "Rich shahi paneer", 250.0, "", "Indian Main Course", true),
            MenuItem("tr106", "Kadai Paneer", "Kadai paneer curry", 250.0, "", "Indian Main Course", true),
            MenuItem("tr107", "Paneer Lababdar", "Paneer lababdar curry", 250.0, "", "Indian Main Course", true),
            MenuItem("tr108", "Paneer 2 Pyazza", "Paneer do pyazza curry", 250.0, "", "Indian Main Course", true),

            // Tandoori Bread
            MenuItem("tr109", "Tandoori Butter Roti", "Butter tandoori roti", 10.0, "", "Tandoori Bread", true),
            MenuItem("tr110", "Lachha Parantha", "Tandoori lachha parantha", 30.0, "", "Tandoori Bread", true),
            MenuItem("tr111", "Plain Naan", "Classic plain naan", 30.0, "", "Tandoori Bread", true),
            MenuItem("tr112", "Butter Naan", "Butter glazed naan", 40.0, "", "Tandoori Bread", true),
            MenuItem("tr113", "Garlic Naan", "Garlic flavored naan", 50.0, "", "Tandoori Bread", true),
            MenuItem("tr114", "Aloo Naan", "Aloo stuffed naan", 60.0, "", "Tandoori Bread", true),
            MenuItem("tr115", "Paneer Naan", "Paneer stuffed naan", 80.0, "", "Tandoori Bread", true),

            // Rice & Biryani
            MenuItem("tr116", "Steamed Rice", "Steamed plain rice", 60.0, "", "Rice & Biryani", true),
            MenuItem("tr117", "Jeera Rice", "Cumin flavored jeera rice", 70.0, "", "Rice & Biryani", true),
            MenuItem("tr118", "Matar Pulao", "Peas pulao", 80.0, "", "Rice & Biryani", true),
            MenuItem("tr119", "Veg Pulao", "Vegetable pulao", 100.0, "", "Rice & Biryani", true),
            MenuItem("tr120", "Veg Biryani", "Vegetable biryani", 160.0, "", "Rice & Biryani", true),
            MenuItem("tr121", "Chicken Biryani", "Chicken biryani", 200.0, "", "Rice & Biryani", false),

            // Raita
            MenuItem("tr122", "Plain Curd", "Plain yogurt", 60.0, "", "Raita", true),
            MenuItem("tr123", "Boondi Raita", "Boondi in yogurt", 60.0, "", "Raita", true),
            MenuItem("tr124", "Mix Raita", "Assorted raita", 70.0, "", "Raita", true),
            MenuItem("tr125", "Green Salad", "Fresh green salad", 60.0, "", "Raita", true)
        )
    }

    private fun getTibetKitchenMenuItems(): List<MenuItem> {
        return listOf(
            // Breakfast
            MenuItem("tk1", "Plain Pan Cake", "Simple plain pancake", 80.0, "", "Breakfast", true),
            MenuItem("tk2", "Banana Pan Cake", "Pancake topped with banana", 100.0, "", "Breakfast", true),
            MenuItem("tk3", "Honey Pan Cake", "Pancake sweetened with honey", 100.0, "", "Breakfast", true),
            MenuItem("tk4", "Butter Pan Cake", "Pancake with butter", 100.0, "", "Breakfast", true),
            MenuItem("tk5", "Aloo Khasta Puri (2pc)", "Crispy puri with spiced potatoes (2 pieces)", 120.0, "", "Breakfast", true),
            MenuItem("tk6", "Tingmo", "Tibetan steamed bun", 40.0, "", "Breakfast", true),

            // Main Course
            MenuItem("tk7", "Mutton Shapta (Dry)", "Sautéed spicy mutton (dry)", 170.0, "", "Main Course", false),
            MenuItem("tk8", "Mutton Shapta (Gravy)", "Sautéed spicy mutton (gravy)", 180.0, "", "Main Course", false),
            MenuItem("tk9", "Chicken Shapta (Dry)", "Sautéed spicy chicken (dry)", 150.0, "", "Main Course", false),
            MenuItem("tk10", "Chicken Shapta (Gravy)", "Sautéed spicy chicken (gravy)", 160.0, "", "Main Course", false),
            MenuItem("tk11", "Chicken Chilly (Dry)", "Spicy chicken chilly (dry)", 180.0, "", "Main Course", false),
            MenuItem("tk12", "Chicken Chilly (Gravy)", "Spicy chicken chilly (gravy)", 180.0, "", "Main Course", false),
            MenuItem("tk13", "Paneer Chilly (Dry)", "Spicy paneer chilly (dry)", 150.0, "", "Main Course", true),
            MenuItem("tk14", "Paneer Chilly (Gravy)", "Spicy paneer chilly (gravy)", 160.0, "", "Main Course", true),
            MenuItem("tk15", "Paneer Manchurian (8pc Dry)", "8 pieces of dry paneer manchurian", 170.0, "", "Main Course", true),
            MenuItem("tk16", "Paneer Manchurian (8pc Gravy)", "8 pieces of paneer manchurian in gravy", 180.0, "", "Main Course", true),
            MenuItem("tk17", "Egg Chilly (Dry)", "Spicy egg chilly (dry)", 150.0, "", "Main Course", false), // corrected to non-veg
            MenuItem("tk18", "Egg Chilly (Gravy)", "Spicy egg chilly (gravy)", 150.0, "", "Main Course", false), // corrected to non-veg
            MenuItem("tk19", "Mutton Shabalay (2pc Dry)", "Tibetan mutton empanada (dry, 2 pieces)", 180.0, "", "Main Course", false),
            MenuItem("tk20", "Mutton Shabalay (2pc Gravy)", "Tibetan mutton empanada (gravy, 2 pieces)", 230.0, "", "Main Course", false),
            MenuItem("tk21", "Chicken Shabalay (2pc Dry)", "Tibetan chicken empanada (dry, 2 pieces)", 160.0, "", "Main Course", false),
            MenuItem("tk22", "Chicken Shabalay (2pc Gravy)", "Tibetan chicken empanada (gravy, 2 pieces)", 210.0, "", "Main Course", false),
            MenuItem("tk23", "Veg Shabalay (2pc Dry)", "Tibetan veg empanada (dry, 2 pieces)", 140.0, "", "Main Course", true),
            MenuItem("tk24", "Veg Shabalay (2pc Gravy)", "Tibetan veg empanada (gravy, 2 pieces)", 180.0, "", "Main Course", true),
            MenuItem("tk25", "Crispy Honey Chilly Potato", "Fried potato with sweet & spicy sauce", 150.0, "", "Snacks", true),

            // Momos (Veg)
            MenuItem("tk26", "Veg Momos (6pc)", "Steamed vegetable momos", 80.0, "", "Momos", true),
            MenuItem("tk27", "Potato Momos", "Steamed potato momos", 80.0, "", "Momos", true),
            MenuItem("tk28", "Fried Veg Momos", "Fried vegetable momos", 100.0, "", "Momos", true),
            MenuItem("tk29", "Paneer Momos", "Steamed paneer momos", 110.0, "", "Momos", true),
            MenuItem("tk30", "Chilli Garlic Momos", "Momos tossed in chili-garlic sauce", 120.0, "", "Momos", true),
            MenuItem("tk31", "Chilli Potato Momos", "Momos with spicy potato filling", 120.0, "", "Momos", true),
            MenuItem("tk32", "Cheese Steam Momos", "Steamed cheese momos", 120.0, "", "Momos", true),
            MenuItem("tk33", "Cheese Fried Momos", "Fried cheese momos", 130.0, "", "Momos", true),
            MenuItem("tk34", "Cheese Chilli Momos", "Cheese momos with chili", 130.0, "", "Momos", true),
            MenuItem("tk35", "Potato Pan Fry Momos", "Potato pan fried momos", 130.0, "", "Momos", true),
            MenuItem("tk36", "Chilli Momos", "Momos tossed in chili sauce", 120.0, "", "Momos", true),

            // Momos (Non-Veg)
            MenuItem("tk37", "Mutton Steam Momos (6pc)", "Steamed mutton momos", 150.0, "", "Momos", false),
            MenuItem("tk38", "Mutton Fried Momos (6pc)", "Fried mutton momos", 160.0, "", "Momos", false),
            MenuItem("tk39", "Egg Steam Momos (6pc)", "Steamed egg momos", 120.0, "", "Momos", false), // corrected to non-veg
            MenuItem("tk40", "Egg Fried Momos (6pc)", "Fried egg momos", 130.0, "", "Momos", false),   // corrected to non-veg
            MenuItem("tk41", "Chicken Steam Momos (6pc)", "Steamed chicken momos", 120.0, "", "Momos", false),
            MenuItem("tk42", "Chicken Fried Momos (6pc)", "Fried chicken momos", 130.0, "", "Momos", false),
            MenuItem("tk43", "Chicken Chilli Momos", "Chicken chili momos", 150.0, "", "Momos", false),
            MenuItem("tk44", "Cheese Chicken Momos", "Cheese and chicken momos", 180.0, "", "Momos", false),

            // Rice
            MenuItem("tk45", "Veg Fried Rice", "Vegetable fried rice", 90.0, "", "Rice", true),
            MenuItem("tk46", "Egg Fried Rice", "Egg fried rice", 110.0, "", "Rice", false), // corrected to non-veg
            MenuItem("tk47", "Mutton Fried Rice", "Mutton fried rice", 120.0, "", "Rice", false),
            MenuItem("tk48", "Chicken Fried Rice", "Chicken fried rice", 110.0, "", "Rice", false),
            MenuItem("tk49", "Schezwan Fried Rice", "Spicy schezwan fried rice", 110.0, "", "Rice", true),
            MenuItem("tk50", "Cheese Rice", "Rice with cheese", 100.0, "", "Rice", true),
            MenuItem("tk51", "Plain Rice", "Steamed plain rice", 50.0, "", "Rice", true),

            // Rice Noodles
            MenuItem("tk52", "Veg Rice Noodle", "Veg rice noodles", 130.0, "", "Rice Noodles", true),
            MenuItem("tk53", "Egg Rice Noodle", "Egg rice noodles", 140.0, "", "Rice Noodles", false), // corrected to non-veg
            MenuItem("tk54", "Mutton Rice Noodle", "Mutton rice noodles", 150.0, "", "Rice Noodles", false),
            MenuItem("tk55", "Chicken Rice Noodle", "Chicken rice noodles", 150.0, "", "Rice Noodles", false),

            // Glass Noodles (Phing)
            MenuItem("tk56", "Aloo Mushroom Phing", "Glass noodle with potato & mushroom", 150.0, "", "Glass Noodles", true),
            MenuItem("tk57", "Egg Mushroom Phing", "Glass noodles with egg & mushroom", 160.0, "", "Glass Noodles", false), // corrected to non-veg
            MenuItem("tk58", "Chicken Phing", "Chicken glass noodles", 180.0, "", "Glass Noodles", false),
            MenuItem("tk59", "Aloo Paneer Phing", "Aloo and paneer with glass noodles", 150.0, "", "Glass Noodles", true),

            // Noodles (Chow Mein)
            MenuItem("tk60", "Chicken Chow Mein", "Chicken fried noodles", 140.0, "", "Noodles", false),
            MenuItem("tk61", "Mutton Chow Mein", "Mutton fried noodles", 160.0, "", "Noodles", false),
            MenuItem("tk62", "Paneer Chow Mein", "Paneer fried noodles", 140.0, "", "Noodles", true),
            MenuItem("tk63", "Veg Chow Mein", "Vegetable fried noodles", 130.0, "", "Noodles", true),
            MenuItem("tk64", "Egg Chow Mein", "Egg fried noodles", 140.0, "", "Noodles", false), // corrected to non-veg
            MenuItem("tk65", "Mutton Chilli Garlic Chow Mein", "Spicy garlic mutton noodles", 170.0, "", "Noodles", false),
            MenuItem("tk66", "Chicken Chilli Garlic Chow Mein", "Spicy garlic chicken noodles", 170.0, "", "Noodles", false),
            MenuItem("tk67", "Veg Chilli Garlic Chow Mein", "Veg chili garlic noodles", 120.0, "", "Noodles", true),

            // Thukpa
            MenuItem("tk68", "Veg Thukpa", "Tibetan veg noodle soup", 130.0, "", "Thukpa", true),
            MenuItem("tk69", "Egg Thukpa", "Egg noodle soup", 140.0, "", "Thukpa", false), // corrected to non-veg
            MenuItem("tk70", "Mushroom Thukpa", "Mushroom noodle soup", 160.0, "", "Thukpa", true),
            MenuItem("tk71", "Paneer Thukpa", "Paneer noodle soup", 170.0, "", "Thukpa", true),
            MenuItem("tk72", "Chicken Thukpa", "Chicken noodle soup", 180.0, "", "Thukpa", false),
            MenuItem("tk73", "Mutton Thukpa", "Mutton noodle soup", 190.0, "", "Thukpa", false),
            MenuItem("tk74", "Tibetan Thukpa Special (Veg)", "Special Tibetan veg soup", 190.0, "", "Thukpa", true),
            MenuItem("tk75", "Tibetan Thukpa Special (Mutton)", "Special Tibetan mutton soup", 230.0, "", "Thukpa", false),
            MenuItem("tk76", "Veg Tibetan Mix Thukpa with Momos", "Veg Tibetan thukpa soup with momos", 210.0, "", "Thukpa", true),
            MenuItem("tk77", "Chicken Tibetan Mix Thukpa with Momos", "Chicken Tibetan thukpa soup with momos", 250.0, "", "Thukpa", false),
            MenuItem("tk78", "Mutton Tibetan Mix Thukpa with Momos", "Mutton Tibetan thukpa soup with momos", 250.0, "", "Thukpa", false),

            // Korean Dishes
            MenuItem("tk79", "Egg Ramen", "Egg noodle ramen", 150.0, "", "Korean", false), // corrected to non-veg
            MenuItem("tk80", "Veg Ramen", "Vegetarian ramen soup", 150.0, "", "Korean", true),
            MenuItem("tk81", "Paneer Kim Bab", "Paneer Korean sushi roll", 180.0, "", "Korean", true),
            MenuItem("tk82", "Egg Kim Bab", "Egg Korean sushi roll", 170.0, "", "Korean", false), // corrected to non-veg
            MenuItem("tk83", "Veg Kim Bab", "Vegetarian Korean sushi roll", 150.0, "", "Korean", true),
            MenuItem("tk84", "Chicken Kim Bab", "Chicken Korean sushi roll", 180.0, "", "Korean", false),
            MenuItem("tk85", "Chicken Bibimbab", "Korean chicken rice bowl", 190.0, "", "Korean", false),
            MenuItem("tk86", "Mutton Bibimbab", "Korean mutton rice bowl", 210.0, "", "Korean", false),

            // Laphin
            MenuItem("tk87", "Yellow Laphin", "Yellow mung bean noodle", 80.0, "", "Laphin", true),
            MenuItem("tk88", "White Laphin", "White mung bean noodle", 70.0, "", "Laphin", true),
            MenuItem("tk89", "Wai Wai Laphin", "Laphin with Wai Wai", 90.0, "", "Laphin", true),

            // Spring Rolls (2pcs)
            MenuItem("tk90", "Veg Spring Roll (2pc)", "2 vegetable spring rolls", 80.0, "", "Spring Roll", true),
            MenuItem("tk91", "Paneer Spring Roll (2pc)", "2 paneer spring rolls", 110.0, "", "Spring Roll", true),
            MenuItem("tk92", "Mutton Spring Roll (2pc)", "2 mutton spring rolls", 140.0, "", "Spring Roll", false),
            MenuItem("tk93", "Chicken Spring Roll (2pc)", "2 chicken spring rolls", 130.0, "", "Spring Roll", false),

            // Northeast Thali
            MenuItem("tk94", "Chicken Thali", "Manipuri style chicken thali", 180.0, "", "Northeast Thali", false),
            MenuItem("tk95", "Fish Thali", "Manipuri style fish thali", 180.0, "", "Northeast Thali", false),
            MenuItem("tk96", "Shingha Thali", "Manipuri shingha (veg) thali", 150.0, "", "Northeast Thali", true),
            MenuItem("tk97", "Tibetan Naga Veg Thali", "Tibetan Naga vegetarian thali", 150.0, "", "Northeast Thali", true),
            MenuItem("tk98", "Tibetan Naga Non-Veg Thali", "Tibetan Naga non-veg thali", 180.0, "", "Northeast Thali", false),
            MenuItem("tk99", "Bhutan Ema Datshi Thali", "Bhutan style veg thali", 150.0, "", "Northeast Thali", true),
            MenuItem("tk100", "Bhutan Non-Veg Thali", "Bhutan style non-veg thali", 180.0, "", "Northeast Thali", false),

            // Re-Chotse Momos in Soup (6pc)
            MenuItem("tk101", "Veg Re-Chotse (Soup, 6pc)", "Veg momos in spicy soup (6)", 120.0, "", "Soup Momos", true),
            MenuItem("tk102", "Paneer Re-Chotse (Soup, 6pc)", "Paneer momos in spicy soup (6)", 160.0, "", "Soup Momos", true),
            MenuItem("tk103", "Mushroom Re-Chotse (Soup, 6pc)", "Mushroom momos in soup (6)", 160.0, "", "Soup Momos", true),
            MenuItem("tk104", "Egg Re-Chotse (Soup, 6pc)", "Egg momos in spicy soup (6)", 150.0, "", "Soup Momos", false), // corrected to non-veg
            MenuItem("tk105", "Chicken Re-Chotse (Soup, 6pc)", "Chicken momos in spicy soup (6)", 180.0, "", "Soup Momos", false),

            // Korean Corn Dog & Waffle
            MenuItem("tk106", "Chicken Sausage Corn Dog", "Korean style chicken sausage corn dog", 120.0, "", "Corn Dog", false),
            MenuItem("tk107", "Cheese Corn Dog", "Korean style cheese corn dog", 120.0, "", "Corn Dog", true),
            MenuItem("tk108", "Chicken Sausage Stick Waffle", "Stick waffle with chicken sausage", 120.0, "", "Waffle", false),
            MenuItem("tk109", "Cheese Corn Dog Stick Waffle", "Stick waffle with cheese corn dog", 120.0, "", "Waffle", true)
        )
    }


    private fun getHungerZoneMenuItems(): List<MenuItem> {
        return listOf(
            // South Indian Counter
            MenuItem("hz1", "Plain Dosa", "Classic South Indian rice crepe", 50.0, "", "South Indian", true),
            MenuItem("hz2", "Masala Dosa", "Dosa filled with spiced potatoes", 70.0, "", "South Indian", true),
            MenuItem("hz3", "Onion Masala Dosa", "Dosa with onions and spicy potatoes", 70.0, "", "South Indian", true),
            MenuItem("hz4", "Paneer Masala Dosa", "Dosa stuffed with paneer masala", 90.0, "", "South Indian", true),
            MenuItem("hz5", "Idli Sambar", "Steamed rice cakes with sambar", 70.0, "", "South Indian", true),
            MenuItem("hz6", "Uttapam Mix", "Mixed veggie savory pancake", 70.0, "", "South Indian", true),
            MenuItem("hz7", "Pav Bhaji", "Spicy mashed veggies with buttered bread", 70.0, "", "Indian Street Food", true),
            MenuItem("hz8", "Vada Sambhar", "South Indian vada with lentil soup", 70.0, "", "South Indian", true),

            // Chat Papdi & Chaat
            MenuItem("hz9", "Golgappa (Paani Puri)", "Crispy balls with flavored water", 40.0, "", "Chaat", true),
            MenuItem("hz10", "Stuffed Golgappa", "Golgappa with potatoes and peas", 60.0, "", "Chaat", true),
            MenuItem("hz11", "Aloo Tikki", "Pan-fried spiced potato patties", 60.0, "", "Chaat", true),
            MenuItem("hz12", "Bhalla Papdi Chaat", "Crispy papdi, dahi bhalla, chutneys", 50.0, "", "Chaat", true),

            // Combos
            MenuItem("hz13", "Rajmah Rice", "Red kidney beans curry with rice", 70.0, "", "Combo", true),
            MenuItem("hz14", "Kadhi Rice", "Gram flour curry and rice", 70.0, "", "Combo", true),
            MenuItem("hz15", "Chana Rice", "Chickpeas curry with rice", 70.0, "", "Combo", true),
            MenuItem("hz16", "Channa Bhatura", "Chickpeas curry with fried Indian bread", 70.0, "", "Combo", true),
            MenuItem("hz17", "Veg Biryani with Raita", "Spiced rice and vegetable biryani served with raita", 100.0, "", "Combo", true),

            // Prantha Wali Gali
            MenuItem("hz18", "Aloo Prantha", "Potato stuffed paratha", 40.0, "", "Prantha", true),
            MenuItem("hz19", "Pyaaz Prantha", "Onion stuffed paratha", 40.0, "", "Prantha", true),
            MenuItem("hz20", "Paneer Prantha", "Paneer stuffed paratha", 60.0, "", "Prantha", true),
            MenuItem("hz21", "Gobhi Prantha", "Cauliflower stuffed paratha", 40.0, "", "Prantha", true),

            // Snacks/Quick Bites
            MenuItem("hz22", "Spring Roll", "Vegetable spring roll", 50.0, "", "Snacks", true),
            MenuItem("hz23", "Manchurian", "Vegetable manchurian", 80.0, "", "Snacks", true),
            MenuItem("hz24", "Burger", "Classic vegetarian burger", 50.0, "", "Snacks", true),
            MenuItem("hz25", "Veg Bullets", "Spicy fried veg bullets", 60.0, "", "Snacks", true),
            MenuItem("hz26", "White Sauce Pasta", "Pasta tossed in creamy white sauce", 100.0, "", "Pasta", true),
            MenuItem("hz27", "Red Sauce Pasta", "Pasta in zesty red sauce", 100.0, "", "Pasta", true),
            MenuItem("hz28", "Sandwich", "Vegetarian sandwich", 50.0, "", "Snacks", true),
            MenuItem("hz29", "Patty", "Flaky baked patty", 20.0, "", "Snacks", true),
            MenuItem("hz30", "Cheese Patty", "Patty stuffed with cheese", 20.0, "", "Snacks", true),
            MenuItem("hz31", "Noodles", "Stir-fried veg noodles", 80.0, "", "Chinese", true),
            MenuItem("hz32", "Paneer Fry", "Crisp fried paneer", 80.0, "", "Snacks", true),
            MenuItem("hz33", "French Fries", "Potato fries", 60.0, "", "Snacks", true),
            MenuItem("hz34", "Samosa", "Spiced potato samosa", 30.0, "", "Snacks", true),
            MenuItem("hz35", "Channa Samosa", "Samosa served with channa", 50.0, "", "Snacks", true),
            MenuItem("hz36", "Bread Omelette", "Egg omelette with bread", 60.0, "", "Snacks", false),
            MenuItem("hz37", "Fried Rice", "Veg fried rice with veggies", 80.0, "", "Chinese", true),

            // Momos
            MenuItem("hz38", "Veg Momos", "Steamed vegetable dumplings", 60.0, "", "Momos", true),
            MenuItem("hz39", "Paneer Momos", "Steamed paneer momos", 100.0, "", "Momos", true),
            MenuItem("hz40", "Fried Momos", "Fried vegetable momos", 80.0, "", "Momos", true),
            MenuItem("hz41", "Kurkure Momos", "Crispy fried momos", 120.0, "", "Momos", true),

            // Wraps & Rolls
            MenuItem("hz42", "Veg Rolls", "Vegetarian wrap", 100.0, "", "Rolls", true),
            MenuItem("hz43", "Paneer Roll", "Paneer wrap", 100.0, "", "Rolls", true),
            MenuItem("hz44", "Aloo Tikki Roll", "Wrap stuffed with aloo tikki", 80.0, "", "Rolls", true),
            MenuItem("hz45", "Chaap Roll", "Soya chaap in wrap", 100.0, "", "Rolls", true),
            MenuItem("hz46", "Egg Roll", "Egg filled roll", 70.0, "", "Rolls", false),
            MenuItem("hz47", "Egg Roll (Double Egg)", "Double egg filled roll", 90.0, "", "Rolls", false),

            // Pizza
            MenuItem("hz48", "Farm House Pizza", "Pizza with farmhouse veggies", 120.0, "", "Pizza", true),
            MenuItem("hz49", "Onion & Capsicum Pizza", "Pizza with onion and capsicum", 100.0, "", "Pizza", true),
            MenuItem("hz50", "Sweet Corn Pizza", "Pizza topped with sweet corn", 120.0, "", "Pizza", true),

            // Fries
            MenuItem("hz51", "Classic Fries", "Classic french fries", 60.0, "", "Fries", true),
            MenuItem("hz52", "Cheesy Fries", "Fries with cheese sauce", 100.0, "", "Fries", true),
            MenuItem("hz53", "Peri-Peri Fries", "Spiced peri-peri fries", 80.0, "", "Fries", true),

            // Shakes
            MenuItem("hz54", "Vanilla Shake", "Vanilla flavored shake", 60.0, "", "Shake", true),
            MenuItem("hz55", "Strawberry Shake", "Strawberry flavored shake", 60.0, "", "Shake", true),
            MenuItem("hz56", "Butter Scotch Shake", "Butterscotch shake", 80.0, "", "Shake", true),
            MenuItem("hz57", "Chocolate Shake", "Chocolate flavored shake", 80.0, "", "Shake", true),
            MenuItem("hz58", "Oreo Shake", "Oreo milkshake", 80.0, "", "Shake", true),
            MenuItem("hz59", "Kit Kat Shake", "Kit Kat blended milkshake", 80.0, "", "Shake", true),

            // Lassi
            MenuItem("hz60", "Sweet Lassi", "Traditional sweet lassi", 50.0, "", "Lassi", true),
            MenuItem("hz61", "Namkeen Lassi", "Salted lassi", 50.0, "", "Lassi", true),
            MenuItem("hz62", "Mango Lassi", "Mango flavored lassi", 60.0, "", "Lassi", true),
            MenuItem("hz63", "Rose Lassi", "Rose flavored lassi", 60.0, "", "Lassi", true),

            // Coffee & Tea
            MenuItem("hz64", "Hot Coffee", "Freshly brewed hot coffee", 50.0, "", "Coffee", true),
            MenuItem("hz65", "Cold Coffee", "Chilled cold coffee", 60.0, "", "Coffee", true),
            MenuItem("hz66", "Cold Coffee with Ice Cream", "Cold coffee topped with ice cream", 80.0, "", "Coffee", true),
            MenuItem("hz67", "Tea", "Classic Indian tea", 20.0, "", "Tea", true),

            // Beverages & Coolers
            MenuItem("hz68", "Masala Mojito", "Spiced mojito", 60.0, "", "Beverage", true),
            MenuItem("hz69", "Virgin Mojito", "Mint virgin mojito", 60.0, "", "Beverage", true),
            MenuItem("hz70", "Ice Blue Lagoon", "Blue lagoon mocktail", 70.0, "", "Beverage", true),
            MenuItem("hz71", "Juice", "Assorted juices (as per availability)", 0.0, "", "Beverage", true), // On MRP, i.e., as per actual price
            MenuItem("hz72", "Cold Drink", "Packaged cold drink (MRP)", 0.0, "", "Beverage", true), // On MRP
            MenuItem("hz73", "Water Bottle", "Packaged drinking water (MRP)", 0.0, "", "Beverage", true) // On MRP
        )
    }


    private fun getHRCafeMenuItems(): List<MenuItem> {
        return listOf(
            // Main Course from Panel
            MenuItem("hr1", "Pav Bhaji", "Spiced mixed vegetable curry with buttered pav", 70.0, "", "Main Course", true),
            MenuItem("hr2", "Chana Rice (Half)", "Chickpeas curry with rice (half portion)", 50.0, "", "Main Course", true),
            MenuItem("hr3", "Chana Rice (Full)", "Chickpeas curry with rice (full portion)", 70.0, "", "Main Course", true),
            MenuItem("hr4", "Rajma Rice (Half)", "Kidney beans curry with rice (half portion)", 50.0, "", "Main Course", true),
            MenuItem("hr5", "Rajma Rice (Full)", "Kidney beans curry with rice (full portion)", 70.0, "", "Main Course", true),
            MenuItem("hr6", "Paneer Rice (Half)", "Paneer curry with rice (half portion)", 70.0, "", "Main Course", true),
            MenuItem("hr7", "Paneer Rice (Full)", "Paneer curry with rice (full portion)", 100.0, "", "Main Course", true),
            MenuItem("hr8", "Channa Samosa", "Spiced chickpeas with samosa", 50.0, "", "Snack", true),
            MenuItem("hr9", "Chhole Bhature", "Spiced chickpeas served with fried bread", 70.0, "", "Main Course", true),

            // Beverages
            MenuItem("hr10", "Home Ginger Tea", "Fresh ginger brewed tea", 20.0, "", "Beverages", true),
            MenuItem("hr11", "Hot Expresso", "Classic hot expresso", 35.0, "", "Beverages", true),
            MenuItem("hr12", "Hot Chocolate", "Creamy hot chocolate drink", 40.0, "", "Beverages", true),
            MenuItem("hr13", "Lemon Tea", "Refreshing lemon tea", 30.0, "", "Beverages", true),
            MenuItem("hr14", "Peach Tea", "Peach flavored tea", 50.0, "", "Beverages", true),
            MenuItem("hr15", "Thandai Beer", "Thandai flavored (non-alcoholic)", 60.0, "", "Beverages", true),

            // Breakfast
            MenuItem("hr16", "Butter Toast", "Buttered bread toast", 30.0, "", "Breakfast", true),
            MenuItem("hr17", "Cheese Butter Toast", "Toast topped with cheese and butter", 50.0, "", "Breakfast", true),
            MenuItem("hr18", "Chilli Garlic Toast", "Toast with chili and garlic", 50.0, "", "Breakfast", true),
            MenuItem("hr19", "Aaloo Prantha (1 Pc) Butter+Chole+Curd", "Aloo paratha with butter, chole & curd", 60.0, "", "Breakfast", true),
            MenuItem("hr20", "Paneer Prantha (1 Pc) Butter+Chole+Curd", "Paneer paratha with butter, chole & curd", 70.0, "", "Breakfast", true),
            MenuItem("hr21", "Mix Prantha (1 Pc) Butter+Chole+Curd", "Mixed vegetable paratha with butter, chole & curd", 60.0, "", "Breakfast", true),
            MenuItem("hr22", "Bread Omelette", "Egg omelette sandwich", 50.0, "", "Breakfast", false),

            // Grilled & Fried Patty
            MenuItem("hr23", "Aaloo Patty", "Fried spiced potato patty", 30.0, "", "Snacks", true),
            MenuItem("hr24", "Cheese Patty", "Cheese stuffed patty", 50.0, "", "Snacks", true),
            MenuItem("hr25", "Paneer Korma Patty", "Paneer korma stuffed patty", 50.0, "", "Snacks", true),
            MenuItem("hr26", "Cheese Corn Patty", "Cheesy corn filled patty", 40.0, "", "Snacks", true),

            // Pasta
            MenuItem("hr27", "Red Sauce Pasta", "Pasta in tangy red sauce", 90.0, "", "Pasta", true),
            MenuItem("hr28", "Cream White Pasta", "Pasta in creamy white sauce", 100.0, "", "Pasta", true),
            MenuItem("hr29", "Mix Sauce Pasta", "Pasta in red & white sauce mix", 110.0, "", "Pasta", true),
            MenuItem("hr30", "Mushroom Corn Pasta", "Pasta with mushrooms and corn", 120.0, "", "Pasta", true),
            MenuItem("hr31", "Makhani Pasta", "Pasta with makhani (buttery) sauce", 110.0, "", "Pasta", true),
            MenuItem("hr32", "Tandoori Pasta", "Tandoori flavored pasta", 120.0, "", "Pasta", true),

            // Shakes
            MenuItem("hr33", "Butterscotch Shake", "Butterscotch flavored shake", 60.0, "", "Shake", true),
            MenuItem("hr34", "Cold Coffee", "Chilled coffee shake", 60.0, "", "Shake", true),
            MenuItem("hr35", "Hazelnut/Chocolate/Caramel Coffee", "Coffee shake with flavor", 70.0, "", "Shake", true),
            MenuItem("hr36", "Banana Shake", "Banana flavored shake", 50.0, "", "Shake", true),
            MenuItem("hr37", "Oreo Shake", "Oreo biscuit shake", 70.0, "", "Shake", true),
            MenuItem("hr38", "Chocolate Shake", "Classic chocolate shake", 70.0, "", "Shake", true),
            MenuItem("hr39", "Strawberry Shake", "Strawberry flavored shake", 70.0, "", "Shake", true),
            MenuItem("hr40", "Butter Scotch Shake", "Butterscotch flavored shake", 70.0, "", "Shake", true),
            MenuItem("hr41", "Nutella Shake", "Nutella chocolate shake", 70.0, "", "Shake", true),
            MenuItem("hr42", "Vanilla Shake", "Vanilla flavored shake", 60.0, "", "Shake", true),
            MenuItem("hr43", "Mango Shake", "Mango flavored shake", 70.0, "", "Shake", true),
            MenuItem("hr44", "Watermelon Shake", "Watermelon flavored shake", 60.0, "", "Shake", true),
            MenuItem("hr45", "Muskmelon Shake", "Muskmelon flavored shake", 60.0, "", "Shake", true),
            MenuItem("hr46", "Pineapple Shake", "Pineapple flavored shake", 60.0, "", "Shake", true),
            MenuItem("hr47", "Grape Shake", "Grape flavored shake", 60.0, "", "Shake", true),
            MenuItem("hr48", "Fresh Fruit Shake", "Shake with fresh fruit", 70.0, "", "Shake", true),
            MenuItem("hr49", "Mango Juice", "Fresh mango juice", 50.0, "", "Shake", true),
            MenuItem("hr50", "Orange Juice", "Fresh orange juice", 60.0, "", "Shake", true),

            // Lassi
            MenuItem("hr51", "Sweet Lassi", "Sweetened Punjabi lassi", 50.0, "", "Lassi", true),
            MenuItem("hr52", "Masala Lassi", "Lassi with masala", 50.0, "", "Lassi", true),
            MenuItem("hr53", "Mango Lassi", "Lassi flavored with mango", 60.0, "", "Lassi", true),
            MenuItem("hr54", "Chocolate Lassi", "Chocolate flavored lassi", 60.0, "", "Lassi", true),
            MenuItem("hr55", "Strawberry Lassi", "Strawberry flavored lassi", 60.0, "", "Lassi", true),
            MenuItem("hr56", "Plain Lassi", "Plain lassi", 50.0, "", "Lassi", true),
            MenuItem("hr57", "Thandai Lassi", "Lassi with thandai", 60.0, "", "Lassi", true),
            MenuItem("hr58", "Rose Lassi", "Rose flavored lassi", 60.0, "", "Lassi", true),

            // Mocktails
            MenuItem("hr59", "Fresh Lime Water", "Refreshing fresh lime drink", 40.0, "", "Mocktail", true),
            MenuItem("hr60", "Fresh Lime Soda", "Lime soda", 50.0, "", "Mocktail", true),
            MenuItem("hr61", "Kala Khatta", "Kala khatta flavored drink", 50.0, "", "Mocktail", true),
            MenuItem("hr62", "Peach Tango", "Peach flavored mocktail", 60.0, "", "Mocktail", true),
            MenuItem("hr63", "Virgin Apple Mojito", "Apple flavored virgin mojito", 70.0, "", "Mocktail", true),
            MenuItem("hr64", "Orange Mojito", "Orange flavored virgin mojito", 70.0, "", "Mocktail", true),
            MenuItem("hr65", "Watermelon Mojito", "Watermelon flavored mojito", 70.0, "", "Mocktail", true),
            MenuItem("hr66", "Mango Spicy Mocktail", "Spicy mango mocktail", 70.0, "", "Mocktail", true),
            MenuItem("hr67", "Hazelnut Mocktail", "Hazelnut mocktail", 70.0, "", "Mocktail", true),

            // Sandwiches
            MenuItem("hr68", "Veg Grilled Sandwich", "Vegetarian grilled sandwich", 70.0, "", "Sandwich", true),
            MenuItem("hr69", "Mushroom Corn Sandwich", "Sandwich with mushroom & corn", 80.0, "", "Sandwich", true),
            MenuItem("hr70", "Cheese Burst Sandwich", "Extra cheesy sandwich", 80.0, "", "Sandwich", true),
            MenuItem("hr71", "Cheese Corn Sandwich", "Corn and cheese sandwich", 80.0, "", "Sandwich", true),
            MenuItem("hr72", "Potato Tikka Sandwich", "Potato tikka sandwich", 80.0, "", "Sandwich", true),
            MenuItem("hr73", "Paneer Tikka Sandwich", "Paneer tikka sandwich", 90.0, "", "Sandwich", true),
            MenuItem("hr74", "Paneer Korma Sandwich", "Sandwich with paneer korma", 90.0, "", "Sandwich", true),
            MenuItem("hr75", "Mexican Sandwich", "Mexican style vegetarian sandwich", 90.0, "", "Sandwich", true),
            MenuItem("hr76", "Chicken Sandwich", "Sandwich with chicken", 90.0, "", "Sandwich", false),
            MenuItem("hr77", "Chicken Cheese Sandwich", "Sandwich with chicken and cheese", 100.0, "", "Sandwich", false),

            // Chinese
            MenuItem("hr78", "Plain Maggie", "Simple masala Maggie noodles", 40.0, "", "Chinese", true),
            MenuItem("hr79", "Veg Maggie", "Vegetable Maggie noodles", 50.0, "", "Chinese", true),
            MenuItem("hr80", "Veg Manchurian (dry)", "Fried vegetable manchurian (dry)", 80.0, "", "Chinese", true),
            MenuItem("hr81", "Veg Manchurian (gravy)", "Vegetable manchurian in gravy", 90.0, "", "Chinese", true),
            MenuItem("hr82", "Veg Momos (steam)", "Steamed vegetarian momos", 70.0, "", "Chinese", true),
            MenuItem("hr83", "Veg Momos (fry)", "Fried vegetarian momos", 80.0, "", "Chinese", true),
            MenuItem("hr84", "Chilly Potato", "Spicy fried potatoes", 70.0, "", "Chinese", true),
            MenuItem("hr85", "Honey Chilly Potato", "Sweet & spicy fried potato", 80.0, "", "Chinese", true),
            MenuItem("hr86", "Chicken Chilli", "Spicy chicken in chili sauce", 100.0, "", "Chinese", false),
            MenuItem("hr87", "Cheese Chilli", "Paneer or cheese in chili sauce", 100.0, "", "Chinese", true),
            MenuItem("hr88", "Mushroom Chilli", "Mushroom in chili sauce", 90.0, "", "Chinese", true),

            // Burger
            MenuItem("hr89", "Aaloo Tikki Burger", "Burger with aaloo tikki patty", 50.0, "", "Burger", true),
            MenuItem("hr90", "Veg Cheese Burger", "Vegetable burger with cheese", 70.0, "", "Burger", true),
            MenuItem("hr91", "Spicy Paneer Burger", "Spicy paneer patty burger", 80.0, "", "Burger", true),
            MenuItem("hr92", "Mexican Burger", "Mexican style burger", 80.0, "", "Burger", true),
            MenuItem("hr93", "Paneer Tikka Burger", "Paneer tikka burger", 90.0, "", "Burger", true),
            MenuItem("hr94", "Chicken Burger", "Chicken filled burger", 100.0, "", "Burger", false),
            MenuItem("hr95", "Chicken Cheese Burger", "Chicken burger with cheese", 110.0, "", "Burger", false),

            // Noodles
            MenuItem("hr96", "Veg Noodles", "Stir fried vegetarian noodles", 90.0, "", "Noodles", true),
            MenuItem("hr97", "Chilly Garlic Noodles", "Noodles with chili garlic flavor", 110.0, "", "Noodles", true),
            MenuItem("hr98", "Hakka Noodles", "Classic Hakka noodles", 110.0, "", "Noodles", true),
            MenuItem("hr99", "Cheese Noodles", "Cheesy vegetarian noodles", 120.0, "", "Noodles", true),
            MenuItem("hr100", "Egg Noodles", "Egg stir fried noodles", 120.0, "", "Noodles", false),
            MenuItem("hr101", "Chicken Noodles", "Chicken stir fried noodles", 130.0, "", "Noodles", false),
            MenuItem("hr102", "Schezwan Noodles", "Spicy schezwan noodles", 130.0, "", "Noodles", true),
            MenuItem("hr103", "Singapore Noodles", "Singapore style stir fried noodles", 130.0, "", "Noodles", true),

            // Rolls
            MenuItem("hr104", "Spring Roll", "Vegetarian spring roll", 60.0, "", "Rolls", true),
            MenuItem("hr105", "Veg Kathi Roll", "Vegetarian kathi roll", 70.0, "", "Rolls", true),
            MenuItem("hr106", "Double Egg Roll", "Egg roll with double filling", 80.0, "", "Rolls", false),
            MenuItem("hr107", "Crispy Spring Roll", "Extra crispy spring roll", 80.0, "", "Rolls", true),
            MenuItem("hr108", "Cheese Corn Roll", "Cheesy corn filled roll", 90.0, "", "Rolls", true),
            MenuItem("hr109", "Paneer Tikka Roll", "Paneer tikka roll", 90.0, "", "Rolls", true),
            MenuItem("hr110", "Soya Chaap Roll", "Soya chaap roll", 100.0, "", "Rolls", true),
            MenuItem("hr111", "Chicken Roll", "Chicken filled roll", 100.0, "", "Rolls", false),
            MenuItem("hr112", "Tandoori Paneer Tikka Roll", "Paneer tikka roll with tandoori flavor", 110.0, "", "Rolls", true),
            MenuItem("hr113", "Egg + Chicken Roll", "Egg and chicken filled roll", 110.0, "", "Rolls", false),

            // Fries
            MenuItem("hr114", "French Fries", "Classic French fries", 40.0, "", "Fries", true),
            MenuItem("hr115", "Masala Fries", "French fries with masala", 60.0, "", "Fries", true),
            MenuItem("hr116", "Peri Peri Fries", "Spicy peri peri fries", 70.0, "", "Fries", true),
            MenuItem("hr117", "Lemon Chilly Fries", "Fries with lemon and chili", 60.0, "", "Fries", true),
            MenuItem("hr118", "Cheezy Fries", "French fries with melted cheese", 70.0, "", "Fries", true),
            MenuItem("hr119", "Mexican Fries", "French fries with Mexican spices", 80.0, "", "Fries", true),
            MenuItem("hr120", "Paneer Fries", "Paneer topped fries", 120.0, "", "Fries", true),
            MenuItem("hr121", "Veg Bullets", "Fried vegetarian bullet snacks", 100.0, "", "Fries", true),

            // Rice
            MenuItem("hr122", "Veg Fried Rice", "Vegetable fried rice", 90.0, "", "Rice", true),
            MenuItem("hr123", "Chilli Garlic Fried Rice", "Fried rice with chili & garlic", 100.0, "", "Rice", true),
            MenuItem("hr124", "Chicken Fried Rice", "Chicken fried rice", 100.0, "", "Rice", false),
            MenuItem("hr125", "Egg Fried Rice", "Fried rice with egg", 100.0, "", "Rice", false),
            MenuItem("hr126", "Schezwan Fried Rice", "Schezwan style fried rice", 100.0, "", "Rice", true),
            MenuItem("hr127", "Paneer Fried Rice", "Paneer fried rice", 100.0, "", "Rice", true),
            MenuItem("hr128", "Mushroom Fried Rice", "Mushroom fried rice", 100.0, "", "Rice", true),

            // Corn
            MenuItem("hr129", "Crispy Corn", "Crispy corn snack", 90.0, "", "Corn", true),
            MenuItem("hr130", "Masala Corn", "Masala flavored corn", 60.0, "", "Corn", true),

            // Samosa
            MenuItem("hr131", "Samosa (2pcs)", "Spiced potato stuffed pastry (2 pieces)", 30.0, "", "Snack", true),
            MenuItem("hr132", "Chana Samosa", "Samosa with chickpeas", 50.0, "", "Snack", true)
        )
    }


    private fun getHcHotAndColdMenuItems(): List<MenuItem> {
        return listOf(
            // Sandwiches
            MenuItem("hc1", "Club Sandwich", "Classic club sandwich", 40.0, "", "Sandwich", true),
            MenuItem("hc2", "Veggie Grill Sandwich", "Grilled vegetarian sandwich", 70.0, "", "Sandwich", true),
            MenuItem("hc3", "Tandoori Sandwich", "Tandoori flavored sandwich", 80.0, "", "Sandwich", true),
            MenuItem("hc4", "Paneer Tikka Sandwich", "Sandwich with paneer tikka", 100.0, "", "Sandwich", true),
            MenuItem("hc5", "Chicken Grill Sandwich", "Grilled chicken sandwich", 100.0, "", "Sandwich", false),

            // Pasta
            MenuItem("hc6", "Red Pasta", "Pasta in red sauce", 90.0, "", "Pasta", true),
            MenuItem("hc7", "White Pasta", "Pasta in white sauce", 110.0, "", "Pasta", true),
            MenuItem("hc8", "Mix Sauce Pasta", "Pasta with mix of red and white sauce", 110.0, "", "Pasta", true),
            MenuItem("hc9", "Cheese Baked Pasta", "Oven baked cheesy pasta", 120.0, "", "Pasta", true),

            // Maggi
            MenuItem("hc10", "Plain Maggi", "Classic plain Maggi noodles", 40.0, "", "Maggi", true),
            MenuItem("hc11", "Vegetable Maggi", "Maggi with vegetables", 50.0, "", "Maggi", true),
            MenuItem("hc12", "Double Masala Maggi", "Double masala spicy Maggi", 70.0, "", "Maggi", true),
            MenuItem("hc13", "Tandoori Maggi", "Tandoori flavored Maggi noodles", 70.0, "", "Maggi", true),
            MenuItem("hc14", "Cheese Butter Maggi", "Maggi with cheese and butter", 80.0, "", "Maggi", true),
            MenuItem("hc15", "Chicken Maggi", "Maggi with chicken", 120.0, "", "Maggi", false),

            // Garlic Bread
            MenuItem("hc16", "Plain Garlic Bread", "Classic garlic bread", 70.0, "", "Garlic Bread", true),
            MenuItem("hc17", "Cheese Garlic Bread", "Garlic bread with cheese", 90.0, "", "Garlic Bread", true),
            MenuItem("hc18", "Tandoori Garlic Bread", "Tandoori style garlic bread", 120.0, "", "Garlic Bread", true),

            // Wraps
            MenuItem("hc19", "Veggie Wrap", "Vegetarian wrap", 70.0, "", "Wraps", true),
            MenuItem("hc20", "Crispy Paneer Wrap", "Wrap with crispy paneer", 90.0, "", "Wraps", true),
            MenuItem("hc21", "Cheese Potato Wrap", "Wrap with cheese and potato", 80.0, "", "Wraps", true),
            MenuItem("hc22", "Chicken Pesto Wrap", "Wrap with chicken pesto", 100.0, "", "Wraps", false),
            MenuItem("hc23", "Chicken Crunch Supreme", "Crunchy chicken wrap", 120.0, "", "Wraps", false),
            MenuItem("hc24", "Chicken Loaded Grill", "Grilled wrap with loaded chicken", 130.0, "", "Wraps", false),

            // Shakes
            MenuItem("hc25", "Strawberry Shake", "Strawberry flavored milkshake", 70.0, "", "Shakes", true),
            MenuItem("hc26", "Black Currant Shake", "Black currant flavored shake", 70.0, "", "Shakes", true),
            MenuItem("hc27", "Chocolate Shake", "Classic chocolate shake", 70.0, "", "Shakes", true),
            MenuItem("hc28", "Oreo Shake", "Oreo biscuit shake", 70.0, "", "Shakes", true),
            MenuItem("hc29", "Butterscotch Shake", "Butterscotch flavored shake", 70.0, "", "Shakes", true),
            MenuItem("hc30", "Blueberry Shake", "Blueberry shake", 70.0, "", "Shakes", true),
            MenuItem("hc31", "Brownie Shake", "Chocolate brownie shake", 90.0, "", "Shakes", true),
            MenuItem("hc32", "Oreo Brownie Shake", "Oreo and brownie shake", 90.0, "", "Shakes", true),
            MenuItem("hc33", "Vanilla Shake", "Classic vanilla shake", 70.0, "", "Shakes", true),

            // Cold Coffee
            MenuItem("hc34", "Cold Coffee", "Chilled coffee", 70.0, "", "Cold Coffee", true),
            MenuItem("hc35", "Choco Cold Coffee", "Chocolate cold coffee", 80.0, "", "Cold Coffee", true),
            MenuItem("hc36", "Hazelnut Cold Coffee", "Hazelnut flavored cold coffee", 90.0, "", "Cold Coffee", true),
            MenuItem("hc37", "Cold Coffee with Ice Cream", "Cold coffee topped with ice cream", 100.0, "", "Cold Coffee", true),

            // Tortilla
            MenuItem("hc38", "Veg. Tortilla", "Vegetarian tortilla wrap", 80.0, "", "Tortilla", true),
            MenuItem("hc39", "Paneer Tortilla", "Tortilla stuffed with paneer", 90.0, "", "Tortilla", true),
            MenuItem("hc40", "Chicken Tortilla", "Chicken stuffed tortilla", 120.0, "", "Tortilla", false),

            // Hot Beverages
            MenuItem("hc41", "Hot Coffee", "Hot brewed coffee", 40.0, "", "Hot Beverage", true),
            MenuItem("hc42", "Hazelnut Coffee", "Hazelnut flavored hot coffee", 50.0, "", "Hot Beverage", true),
            MenuItem("hc43", "Hot Chocolate", "Hot chocolate drink", 50.0, "", "Hot Beverage", true),
            MenuItem("hc44", "Tea", "Classic brewed tea", 20.0, "", "Hot Beverage", true),

            // Beverages (Crushers & Mojitos)
            MenuItem("hc45", "Strawberry Mary", "Strawberry flavored beverage", 70.0, "", "Beverages", true),
            MenuItem("hc46", "Mango Mule Mojito", "Mango mojito", 70.0, "", "Beverages", true),
            MenuItem("hc47", "Strawberry Ice Crusher", "Strawberry ice crusher drink", 70.0, "", "Beverages", true),
            MenuItem("hc48", "Kiwi Ice Crusher", "Kiwi ice crusher drink", 70.0, "", "Beverages", true),
            MenuItem("hc49", "Mango Ice Crusher", "Mango ice crusher drink", 70.0, "", "Beverages", true),
            MenuItem("hc50", "Blueberry Ice Crusher", "Blueberry ice crusher", 70.0, "", "Beverages", true),

            // Mocktails
            MenuItem("hc51", "Fresh Lemon Soda", "Fresh lemon soda", 60.0, "", "Mocktail", true),
            MenuItem("hc52", "Mint Mojito", "Mint flavored mojito", 65.0, "", "Mocktail", true),
            MenuItem("hc53", "Blue Hawaiian", "Blue Hawaiian mocktail", 65.0, "", "Mocktail", true),
            MenuItem("hc54", "Green Cooler", "Green colored cooler drink", 65.0, "", "Mocktail", true),
            MenuItem("hc55", "Watermelon Spritzer", "Refreshing watermelon spritzer", 65.0, "", "Mocktail", true),
            MenuItem("hc56", "Black Currant", "Black currant mocktail", 65.0, "", "Mocktail", true),

            // Friendship Bucket
            MenuItem("hc57", "Friendship Bucket", "Assorted fried snack bucket", 319.0, "", "Snacks", false)
        )
    }


    fun getPunjabiRasoiMenuItems(): List<MenuItem> {
        return listOf(
            // BURGER
            MenuItem("p1", "Veg. Burger", "Classic vegetarian burger", 50.0, "", "Burger", true),
            MenuItem("p2", "Delight Burger", "Delightful veg burger", 60.0, "", "Burger", true),
            MenuItem("p3", "Spicy Paneer Burger", "Paneer burger with spicy twist", 70.0, "", "Burger", true),
            MenuItem("p4", "Noodle Burger", "Burger with noodle stuffing", 60.0, "", "Burger", true),
            MenuItem("p5", "Cheese Burger", "Veg burger with cheese", 70.0, "", "Burger", true),
            MenuItem("p6", "Special Burger", "Special house burger", 70.0, "", "Burger", true),

            // SANDWICH
            MenuItem("p7", "Veg Sandwich", "Vegetarian sandwich", 60.0, "", "Sandwich", true),
            MenuItem("p8", "Spicy Grilled Sandwich", "Spicy grilled sandwich", 60.0, "", "Sandwich", true),
            MenuItem("p9", "Mushroom Grilled Sandwich", "Grilled mushroom sandwich", 70.0, "", "Sandwich", true),
            MenuItem("p10", "Special Paneer Bust", "Special paneer 'bust' sandwich", 80.0, "", "Sandwich", true),
            MenuItem("p11", "Cheese Bust", "Cheesy sandwich special", 70.0, "", "Sandwich", true),

            // CAKE & PASTRY
            MenuItem("p12", "Half Kg Cake", "Half kg celebration cake", 300.0, "", "Cake & Pastry", true),
            MenuItem("p13", "Brownie", "Chocolate brownie", 60.0, "", "Cake & Pastry", true),
            MenuItem("p14", "Choco Lava", "Choco lava cake", 50.0, "", "Cake & Pastry", true),
            MenuItem("p15", "Truffle Pastry", "Rich chocolate truffle pastry", 50.0, "", "Cake & Pastry", true),
            MenuItem("p16", "Pudding Cup", "Cup of sweet pudding", 40.0, "", "Cake & Pastry", true),

            // WRAPS
            MenuItem("p17", "Veg Wrap", "Vegetarian wrap", 70.0, "", "Wraps", true),
            MenuItem("p18", "Cheese Wrap", "Cheese stuffed wrap", 80.0, "", "Wraps", true),
            MenuItem("p19", "Paneer Wrap", "Paneer stuffed wrap", 90.0, "", "Wraps", true),

            // NOODLES
            MenuItem("p20", "Veg Noodles", "Vegetable noodles", 70.0, "", "Noodles", true),
            MenuItem("p21", "Chilli Garlic Noodles", "Chilli garlic flavored noodles", 80.0, "", "Noodles", true),
            MenuItem("p22", "Singapuri Noodles", "Spicy Singapuri noodles", 80.0, "", "Noodles", true),
            MenuItem("p23", "Hakka Noodles", "Classic Hakka-style noodles", 80.0, "", "Noodles", true),
            MenuItem("p24", "Paneer Noodles", "Paneer tossed noodles", 80.0, "", "Noodles", true),

            // ROLLS
            MenuItem("p25", "Noodles Kathi", "Roll stuffed with noodles", 45.0, "", "Rolls", true),
            MenuItem("p26", "Soya Kathi", "Soya roll", 50.0, "", "Rolls", true),
            MenuItem("p27", "Paneer Kathi", "Paneer roll", 80.0, "", "Rolls", true),

            // MAGGIE
            MenuItem("p28", "Veg Maggie", "Vegetarian Maggi", 60.0, "", "Maggie", true),
            MenuItem("p29", "Cheese Maggie", "Maggi with cheese", 70.0, "", "Maggie", true),

            // SNACKS
            MenuItem("p30", "Veg Bullets", "Fried vegetarian bullets", 60.0, "", "Snacks", true),
            MenuItem("p31", "Spring Rolls", "Vegetarian spring roll", 70.0, "", "Snacks", true),
            MenuItem("p32", "Veg Manchurian", "Vegetable manchurian (dry/gravy)", 100.0, "", "Snacks", true),
            MenuItem("p33", "Chilli Paneer", "Chilli Paneer (Honey/Chilli/Hot)", 180.0, "", "Snacks", true),

            // MOMOS
            MenuItem("p34", "Veg Steam Momos", "Steamed veg momos", 50.0, "", "Momos", true),
            MenuItem("p35", "Veg Fried Momos", "Fried veg momos", 60.0, "", "Momos", true),

            // KULCHA & SAMOSA
            MenuItem("p36", "Chana Kulcha", "Chana masala with kulcha", 60.0, "", "Kulcha & Samosa", true),
            MenuItem("p37", "Paneer Kulcha", "Paneer stuffed kulcha", 70.0, "", "Kulcha & Samosa", true),
            MenuItem("p38", "Amritsari Kulcha (1pc)", "Amritsari kulcha (single)", 60.0, "", "Kulcha & Samosa", true),
            MenuItem("p39", "Amritsari Kulcha (2pc)", "Amritsari kulcha (double)", 110.0, "", "Kulcha & Samosa", true),
            MenuItem("p41", "Chana Samosa (Single)", "Chana with single samosa", 40.0, "", "Kulcha & Samosa", true),
            MenuItem("p42", "Chana Samosa (Double)", "Chana with double samosa", 60.0, "", "Kulcha & Samosa", true),
            MenuItem("p43", "Paneer Samosa", "Paneer stuffed samosa", 40.0, "", "Kulcha & Samosa", true),
            MenuItem("p44", "Soya Chaap Kulcha", "Soya chaap stuffed kulcha", 70.0, "", "Kulcha & Samosa", true),
            MenuItem("p45", "Samosa", "Samosa (2 pcs)", 30.0, "", "Kulcha & Samosa", true),

            // PATTIES
            MenuItem("p46", "Aloo Patty", "Potato stuffed patty", 20.0, "", "Patties", true),
            MenuItem("p47", "Cheese Patty", "Cheese stuffed patty", 30.0, "", "Patties", true),
            MenuItem("p48", "Paneer Patty", "Paneer stuffed patty", 25.0, "", "Patties", true),

            // SHAKES
            MenuItem("p49", "Kit Kat Shake", "Chocolate Kit Kat shake", 60.0, "", "Shakes", true),
            MenuItem("p50", "Oreo Shake", "Creamy Oreo shake", 60.0, "", "Shakes", true),
            MenuItem("p51", "Vanilla Shake", "Vanilla flavored shake", 50.0, "", "Shakes", true),
            MenuItem("p52", "Chocolate Shake", "Classic chocolate shake", 50.0, "", "Shakes", true),
            MenuItem("p53", "Black Current Shake", "Fruity black current shake", 50.0, "", "Shakes", true),
            MenuItem("p54", "Butter Scotch Shake", "Butterscotch flavored shake", 50.0, "", "Shakes", true),
            MenuItem("p55", "Banana Shake", "Banana blended shake", 50.0, "", "Shakes", true),
            MenuItem("p56", "Mango Shake", "Mango flavored shake", 50.0, "", "Shakes", true),
            MenuItem("p57", "Kiwi Shake", "Kiwi infused shake", 60.0, "", "Shakes", true),

            // COLD COFFEE
            MenuItem("p58", "Cold Coffee", "Chilled coffee shake", 50.0, "", "Cold Coffee", true),
            MenuItem("p59", "Cold Coffee (with Ice Cream)", "Chilled coffee shake with ice cream", 60.0, "", "Cold Coffee", true),

            // RICE
            MenuItem("p60", "Rajma Rice", "Rajma masala with rice", 50.0, "", "Rice", true),
            MenuItem("p61", "Chana Rice", "Chana masala with rice", 50.0, "", "Rice", true),
            MenuItem("p62", "Kadi Rice", "Kadi pakora with rice", 60.0, "", "Rice", true),
            MenuItem("p63", "Dal Makhani Rice", "Dal makhani with rice", 80.0, "", "Rice", true),
            MenuItem("p64", "Veg Fried Rice", "Chinese style fried rice", 60.0, "", "Rice", true),
            MenuItem("p65", "Paneer Fried Rice", "Fried rice with paneer", 80.0, "", "Rice", true),
            MenuItem("p66", "Paneer Gravy Rice", "Paneer curry with rice", 90.0, "", "Rice", true),
            MenuItem("p67", "Veg Biryani", "Vegies biryani", 80.0, "", "Rice", true),
            MenuItem("p68", "Jeera Rice", "Rice with cumin", 50.0, "", "Rice", true),

            // PASTA
            MenuItem("p69", "Red Sauce Pasta", "Pasta in red sauce", 70.0, "", "Pasta", true),
            MenuItem("p70", "White Sauce Pasta", "Pasta in white sauce", 80.0, "", "Pasta", true),
            MenuItem("p71", "Makhni Pasta", "Makhni (buttery) pasta", 80.0, "", "Pasta", true),
            MenuItem("p72", "Mix Sauce Pasta", "Pasta in mixed sauces", 80.0, "", "Pasta", true),

            // LASSI
            MenuItem("p73", "Sweet Lassi (Small)", "Sweetened yogurt drink (small)", 40.0, "", "Lassi", true),
            MenuItem("p74", "Sweet Lassi (Medium)", "Sweetened yogurt drink (medium)", 50.0, "", "Lassi", true),
            MenuItem("p75", "Sweet Lassi (Large)", "Sweetened yogurt drink (large)", 70.0, "", "Lassi", true),
            MenuItem("p76", "Namkeen Lassi", "Salted yogurt drink", 40.0, "", "Lassi", true),

            // COOLERS & MOJITOS
            MenuItem("p77", "Nimbu Pani", "Refreshing lemon water", 30.0, "", "Coolers & Mojitos", true),
            MenuItem("p78", "Fresh Lime Soda", "Lemon soda", 40.0, "", "Coolers & Mojitos", true),
            MenuItem("p79", "Fresh Lime Masala Soda", "Masala flavored lemon soda", 50.0, "", "Coolers & Mojitos", true),
            MenuItem("p80", "Virgin Mojito", "Minty mojito", 60.0, "", "Coolers & Mojitos", true),
            MenuItem("p81", "Blue Mojito", "Blue colored mojito", 60.0, "", "Coolers & Mojitos", true),
            MenuItem("p82", "Green Apple Mojito", "Green apple mojito", 60.0, "", "Coolers & Mojitos", true),
            MenuItem("p83", "Kala Khatta Mojito", "Kala khatta flavored mojito", 60.0, "", "Coolers & Mojitos", true),

            // PIZZA
            MenuItem("p84", "Onion & Corn Pizza", "Onion and corn pizza", 110.0, "", "Pizza", true),
            MenuItem("p85", "Onion & Capsicum Pizza", "Onion and capsicum pizza", 120.0, "", "Pizza", true),
            MenuItem("p86", "Paneer & Onion Pizza", "Paneer and onion pizza", 150.0, "", "Pizza", true),
            MenuItem("p87", "Paneer & Capsicum Pizza", "Paneer and capsicum pizza", 150.0, "", "Pizza", true),
            MenuItem("p88", "Margherita Pizza", "Classic margherita pizza", 150.0, "", "Pizza", true),
            MenuItem("p89", "Cheese & Corn Pizza", "Cheese and corn pizza", 140.0, "", "Pizza", true),
            MenuItem("p90", "Mix Veg Pizza", "Mixed vegetable pizza", 170.0, "", "Pizza", true),

            // FRIES
            MenuItem("p91", "Simple Fries", "Classic fries", 70.0, "", "Fries", true),
            MenuItem("p92", "Masala Fries", "Spiced fries", 80.0, "", "Fries", true),
            MenuItem("p93", "Cheese Fries", "Cheesy fries", 110.0, "", "Fries", true),
            MenuItem("p94", "Peri-Peri Fries", "Spicy peri-peri fries", 90.0, "", "Fries", true),

            // TEA & COFFEE
            MenuItem("p95", "Ginger Tea", "Freshly brewed ginger tea", 15.0, "", "Tea & Coffee", true),
            MenuItem("p96", "Special Tea", "Special masala tea", 20.0, "", "Tea & Coffee", true),
            MenuItem("p97", "Hot Coffee", "Classic hot coffee", 25.0, "", "Tea & Coffee", true),

            // ROTI & PARATHA
            MenuItem("p98", "Tawa Roti", "Classic tawa roti", 10.0, "", "Roti", true),
            MenuItem("p99", "Tandoori Roti", "Tandoori roti", 15.0, "", "Roti", true),
            MenuItem("p100", "Lachha Paratha", "Layered paratha", 45.0, "", "Roti", true),
            MenuItem("p101", "Butter Naan", "Soft naan with butter", 30.0, "", "Roti", true),

            // STUFFED NAAN
            MenuItem("p102", "Stuffed Naan - Aloo", "Aloo stuffed naan", 50.0, "", "Stuffed Naan", true),
            MenuItem("p103", "Stuffed Naan - Gobhi", "Gobhi stuffed naan", 60.0, "", "Stuffed Naan", true),
            MenuItem("p104", "Stuffed Naan - Paneer", "Paneer stuffed naan", 70.0, "", "Stuffed Naan", true),

            // TANDOORI PARATHA
            MenuItem("p105", "Tandoori Paratha - Aloo", "Tandoori aloo paratha", 40.0, "", "Tandoori Paratha", true),
            MenuItem("p106", "Tandoori Paratha - Gobhi", "Tandoori gobhi paratha", 45.0, "", "Tandoori Paratha", true),
            MenuItem("p107", "Tandoori Paratha - Paneer", "Tandoori paneer paratha", 50.0, "", "Tandoori Paratha", true),

            // CHOLEY BHATURE
            MenuItem("p108", "Choley Bhature", "Classic choley bhature", 80.0, "", "Choley Bhature", true),
            MenuItem("p109", "Extra Bhature", "Extra serving of bhature", 25.0, "", "Choley Bhature", true),
            MenuItem("p110", "Aloo Puri", "Aloo sabzi with puri", 80.0, "", "Choley Bhature", true),
            MenuItem("p111", "Extra Puri", "Extra serving of puri", 15.0, "", "Choley Bhature", true),

            // INDIAN MAIN COURSE (Half / Full)
            MenuItem("p112", "Dal Makhni (Half)", "Creamy dal makhni (half)", 100.0, "", "Indian Main Course", true),
            MenuItem("p113", "Dal Makhni (Full)", "Creamy dal makhni (full)", 150.0, "", "Indian Main Course", true),
            MenuItem("p114", "Rajma (Half)", "Rajma (half portion)", 100.0, "", "Indian Main Course", true),
            MenuItem("p115", "Rajma (Full)", "Rajma (full portion)", 150.0, "", "Indian Main Course", true),
            MenuItem("p116", "Shahi Paneer (Half)", "Royal style paneer curry (half)", 150.0, "", "Indian Main Course", true),
            MenuItem("p117", "Shahi Paneer (Full)", "Royal style paneer curry (full)", 200.0, "", "Indian Main Course", true),
            MenuItem("p118", "Kadai Paneer (Half)", "Spicy kadai paneer (half)", 150.0, "", "Indian Main Course", true),
            MenuItem("p119", "Kadai Paneer (Full)", "Spicy kadai paneer (full)", 200.0, "", "Indian Main Course", true),
            MenuItem("p126", "Rara Paneer (Half)", "Kara Paneer (half)", 190.0, "", "Indian Main Course", true),
            MenuItem("p127", "Rara Paneer (Full)", "Kara Paneer (full)", 230.0, "", "Indian Main Course", true),
            MenuItem("p128", "Paneer Butter Masala (Half)", "Paneer butter masala (half)", 190.0, "", "Indian Main Course", true),
            MenuItem("p129", "Paneer Butter Masala (Full)", "Paneer butter masala (full)", 230.0, "", "Indian Main Course", true),
            MenuItem("p130", "Paneer Bhurji (Half)", "Scrambled paneer (half)", 150.0, "", "Indian Main Course", true),
            MenuItem("p131", "Paneer Bhurji (Full)", "Scrambled paneer (full)", 200.0, "", "Indian Main Course", true),
            MenuItem("p132", "Gravy Chaap (Half)", "Gravy chaap (half)", 180.0, "", "Indian Main Course", true),
            MenuItem("p133", "Gravy Chaap (Full)", "Gravy chaap (full)", 220.0, "", "Indian Main Course", true),

            // COMBO DISHES
            MenuItem("p134", "Dal Makhni + 2 Butter Naan", "Dal makhni and 2 butter naan combo", 100.0, "", "Combo", true),
            MenuItem("p135", "Kadai Paneer + 2 Butter Naan", "Kadai paneer and 2 butter naan", 130.0, "", "Combo", true),
            MenuItem("p136", "Shahi Paneer + 2 Butter Naan", "Shahi Paneer and 2 butter naan", 130.0, "", "Combo", true),
            MenuItem("p137", "Paneer Butter Masala + 2 Butter Naan", "Paneer butter masala and 2 butter naan", 130.0, "", "Combo", true),
            MenuItem("p138", "Paneer Bhurji Combo", "Paneer bhurji combo", 130.0, "", "Combo", true),
            MenuItem("p139", "Mushroom Masala + 2 Naan", "Mushroom masala and 2 naan", 130.0, "", "Combo", true),
            MenuItem("p140", "Kara Paneer Combo", "Kara paneer combo", 130.0, "", "Combo", true),
            MenuItem("p141", "Paneer Combo 2 Butter Naan", "Paneer combo with 2 butter naan", 130.0, "", "Combo", true)
        )
    }



    fun getCatchUpCafeMenuItems(): List<MenuItem> = listOf(
        // ------------- BURGER -------------
        MenuItem("b1", "Aloo Tikki Burger", "Burger with potato patty", 40.0, "", "Burger", true),
        MenuItem("b2", "Veg Burger", "Vegetable burger", 50.0, "", "Burger", true),
        MenuItem("b3", "Veg Cheese Burger", "Vegetable burger with cheese", 60.0, "", "Burger", true),
        MenuItem("b4", "Paneer Burger", "Burger with paneer patty", 70.0, "", "Burger", true),
        MenuItem("b5", "Makhani Burger", "Burger with makhani sauce", 80.0, "", "Burger", true),
        MenuItem("b6", "Double Decker Burger", "Double patty burger", 100.0, "", "Burger", true),

        // ----------- SANDWICH -------------
        MenuItem("s1", "Veg Sandwich", "Vegetable sandwich", 30.0, "", "Sandwich", true),
        MenuItem("s2", "Corn Sandwich", "Corn stuffed sandwich", 35.0, "", "Sandwich", true),
        MenuItem("s3", "Veg Cheese Sandwich", "Veg cheese sandwich", 45.0, "", "Sandwich", true),
        MenuItem("s4", "Veg Grill Sandwich", "Grilled vegetable sandwich", 50.0, "", "Sandwich", true),
        MenuItem("s5", "Mozzarella Grill Sandwich", "Grilled sandwich with mozzarella", 60.0, "", "Sandwich", true),
        MenuItem("s6", "Mexican Nachos Sandwich", "Mexican sandwich with nachos", 80.0, "", "Sandwich", true),

        // ------------- PASTA ---------------
        MenuItem("p1", "White Sauce Pasta", "Pasta in creamy white sauce", 90.0, "", "Pasta", true),
        MenuItem("p2", "Makhani Pasta", "Pasta in makhani sauce", 90.0, "", "Pasta", true),
        MenuItem("p3", "Red Sauce Pasta", "Pasta in red tomato sauce", 100.0, "", "Pasta", true),
        MenuItem("p4", "Spicy Lava Pasta", "Spicy pasta specialty", 110.0, "", "Pasta", true),
        MenuItem("p5", "Mix Sauce Pasta", "Pasta with mix of sauces", 110.0, "", "Pasta", true),

        // ----------- WRAP ------------------
        MenuItem("w1", "Aloo Tikki Wrap", "Wrap with potato tikki", 60.0, "", "Wrap", true),
        MenuItem("w2", "Veg Wrap", "Vegetable wrap", 60.0, "", "Wrap", true),
        MenuItem("w3", "Soya Wrap", "Wrap with soya filling", 70.0, "", "Wrap", true),
        MenuItem("w4", "Paneer Wrap", "Paneer stuffed wrap", 80.0, "", "Wrap", true),

        // ------------ MOMOS ----------------
        MenuItem("m1", "Steam Momos", "Steamed vegetable momos", 60.0, "", "Momos", true),
        MenuItem("m2", "Fried Momos", "Fried vegetable momos", 70.0, "", "Momos", true),
        MenuItem("m3", "Afghani Momos", "Momos in Afghani style sauce", 80.0, "", "Momos", true),
        MenuItem("m4", "Gravy Momos", "Momos in gravy", 80.0, "", "Momos", true),
        MenuItem("m5", "Kurkure Momos", "Crispy Kurkure style momos", 90.0, "", "Momos", true),
        MenuItem("m6", "Cheese Momos", "Momos stuffed with cheese", 90.0, "", "Momos", true),
        MenuItem("m7", "Makhani Momos", "Momos in makhani sauce", 90.0, "", "Momos", true),

        // ------------ NOODLES --------------
        MenuItem("n1", "Veg Noodles", "Vegetable noodles", 60.0, "", "Noodles", true),
        MenuItem("n2", "Chilly Garlic Noodles", "Chilli garlic flavored noodles", 70.0, "", "Noodles", true),
        MenuItem("n3", "Schewan Noodles", "Spicy Schezwan noodles", 70.0, "", "Noodles", true),
        MenuItem("n4", "Soya Noodles", "Noodles with soya", 70.0, "", "Noodles", true),
        MenuItem("n5", "Hakka Noodles", "Hakka style noodles", 80.0, "", "Noodles", true),
        MenuItem("n6", "Paneer Noodles", "Paneer tossed noodles", 90.0, "", "Noodles", true),

        // ------------- SNACKS --------------
        MenuItem("sn1", "Spring Roll", "Vegetarian spring roll", 60.0, "", "Snacks", true),
        MenuItem("sn2", "Veg. Nuggets", "Veg nuggets", 70.0, "", "Snacks", true),
        MenuItem("sn3", "Kurkure Spring Roll", "Crunchy spring roll", 80.0, "", "Snacks", true),
        MenuItem("sn4", "Veggies Sticks", "Vegetable sticks", 80.0, "", "Snacks", true),
        MenuItem("sn5", "Cheese Corn Roll", "Cheese and corn roll", 100.0, "", "Snacks", true),

        // -------------- FRIES --------------
        MenuItem("f1", "Salted Fries", "Classic salted fries", 60.0, "", "Fries", true),
        MenuItem("f2", "Peri Peri Fries", "Peri peri spiced fries", 80.0, "", "Fries", true),
        MenuItem("f3", "Cheesy Fries", "Fries topped with cheese", 100.0, "", "Fries", true),

        // ------------- CORNS ---------------
        MenuItem("c1", "Masala Sweet Corn", "Masala flavored sweet corn", 50.0, "", "Corns", true),
        MenuItem("c2", "Chilly Garlic Corn", "Chilli garlic flavored corn", 60.0, "", "Corns", true),
        MenuItem("c3", "Korean Cream Cheese Corn", "Korean style creamy cheese corn", 90.0, "", "Corns", true),

        // ----------- MAGGI -----------------
        MenuItem("mg1", "Regular Maggi", "Classic Maggi noodles", 35.0, "", "Maggi", true),
        MenuItem("mg2", "Veg Maggi", "Veg Maggi noodles", 50.0, "", "Maggi", true),
        MenuItem("mg3", "Spicy Maggi", "Spicy flavor Maggi noodles", 60.0, "", "Maggi", true),
        MenuItem("mg4", "Tandoori Maggi", "Tandoori flavor Maggi noodles", 70.0, "", "Maggi", true),
        MenuItem("mg5", "Cheese Maggi", "Cheesy Maggi noodles", 80.0, "", "Maggi", true),
        MenuItem("mg6", "Schewan Maggi", "Schezwan flavored Maggi", 80.0, "", "Maggi", true),

        // ----------- MOCKTAILS -------------
        MenuItem("mo1", "Green Apple", "Green Apple mocktail", 60.0, "", "Mocktails", true),
        MenuItem("mo2", "Watermelon", "Watermelon mocktail", 60.0, "", "Mocktails", true),
        MenuItem("mo3", "Blue Curacao", "Blue curacao mocktail", 60.0, "", "Mocktails", true),
        MenuItem("mo4", "Mint Mojito", "Mint mojito", 60.0, "", "Mocktails", true),
        MenuItem("mo5", "Fresh Lime Soda", "Fresh lime soda", 40.0, "", "Mocktails", true),
        MenuItem("mo6", "Blueberry", "Blueberry mocktail", 70.0, "", "Mocktails", true),
        MenuItem("mo7", "Ice Tea", "Iced tea", 35.0, "", "Mocktails", true),

        // -------------- LASSI --------------
        MenuItem("l1", "Regular Lassi", "Classic lassi", 50.0, "", "Lassi", true),
        MenuItem("l2", "Mango Lassi", "Mango flavored lassi", 60.0, "", "Lassi", true),
        MenuItem("l3", "Rose Lassi", "Rose flavored lassi", 60.0, "", "Lassi", true),

        // -------------- SHAKES -------------
        MenuItem("sh1", "Any Shake", "With ice cream: ₹10 extra", 0.0, "", "Shakes", true), // instruction only
        MenuItem("sh2", "Cold Coffee (Frappe)", "Chilled frappe style coffee shake", 60.0, "", "Shakes", true),
        MenuItem("sh3", "Chocolate Shake", "Chocolate milkshake", 60.0, "", "Shakes", true),
        MenuItem("sh4", "Pineapple Shake", "Pineapple milkshake", 60.0, "", "Shakes", true),
        MenuItem("sh5", "Strawberry Shake", "Strawberry milkshake", 60.0, "", "Shakes", true),
        MenuItem("sh6", "Blue Berry Shake", "Blueberry milkshake", 70.0, "", "Shakes", true),
        MenuItem("sh7", "Black Current Shake", "Black currant milkshake", 70.0, "", "Shakes", true),
        MenuItem("sh8", "Vanilla Shake", "Vanilla milkshake", 60.0, "", "Shakes", true),
        MenuItem("sh9", "Banana Shake", "Banana milkshake", 60.0, "", "Shakes", true),
        MenuItem("sh10", "Butter Scotch Shake", "Butterscotch milkshake", 60.0, "", "Shakes", true),
        MenuItem("sh11", "Kit Kat Shake", "KitKat chocolate milkshake", 70.0, "", "Shakes", true),
        MenuItem("sh12", "Oreo Shake", "Oreo cookie milkshake", 70.0, "", "Shakes", true),
        MenuItem("sh13", "Choco Pie Shake", "Choco Pie blended milkshake", 80.0, "", "Shakes", true),
        MenuItem("sh14", "Choco Caramel Shake", "Chocolate caramel shake", 80.0, "", "Shakes", true),
        MenuItem("sh15", "Munch Shake", "Munch chocolate shake", 80.0, "", "Shakes", true),
        MenuItem("sh16", "Cadbury Chocolate Shake", "Cadbury chocolate shake", 80.0, "", "Shakes", true),
        MenuItem("sh17", "Brownie Shake", "Brownie blended milkshake", 80.0, "", "Shakes", true),

        // ------------ HOT DRINKS -----------
        MenuItem("hd1", "Regular Tea", "Regular hot tea", 15.0, "", "Hot Drinks", true),
        MenuItem("hd2", "Special Tea", "Special masala tea", 20.0, "", "Hot Drinks", true),
        MenuItem("hd3", "Black Coffee", "Black hot coffee", 20.0, "", "Hot Drinks", true),
        MenuItem("hd4", "Milk Coffee", "Milk coffee", 20.0, "", "Hot Drinks", true),
        MenuItem("hd5", "Cappuccino", "Hot cappuccino", 30.0, "", "Hot Drinks", true),
        MenuItem("hd6", "Latte", "Hot latte", 30.0, "", "Hot Drinks", true),
        MenuItem("hd7", "Hot Chocolate", "Hot chocolate drink", 35.0, "", "Hot Drinks", true),

        // ------------ DESERT ---------------
        MenuItem("d1", "Brownie", "Chocolate brownie", 60.0, "", "Desert", true),
        MenuItem("d2", "Brownie with Ice Cream", "Brownie served with ice cream", 70.0, "", "Desert", true),

        // ------------- MEALS ---------------
        MenuItem("me1", "Rajma Rice (Half)", "Rajma Curry with rice - Half", 50.0, "", "Meals", true),
        MenuItem("me2", "Rajma Rice (Full)", "Rajma Curry with rice - Full", 70.0, "", "Meals", true),
        MenuItem("me3", "Fried Rice", "Fried rice", 60.0, "", "Meals", true),
        MenuItem("me4", "Chilli Garlic Rice", "Chilli garlic flavored rice", 70.0, "", "Meals", true),
        MenuItem("me5", "Hakka Rice", "Hakka style rice", 80.0, "", "Meals", true),
        MenuItem("me6", "Soya Rice", "Soya flavored rice", 80.0, "", "Meals", true),
        MenuItem("me7", "Paneer Fried Rice", "Paneer tossed fried rice", 90.0, "", "Meals", true),
        MenuItem("me8", "Paneer Rice", "Fried rice with paneer", 90.0, "", "Meals", true),

        // ----------- NON VEG SECTION -------
        MenuItem("nv1", "Chicken Wrap", "Wrap stuffed with chicken", 90.0, "", "Non Veg", false),
        MenuItem("nv2", "Chicken Burger", "Chicken patty burger", 90.0, "", "Non Veg", false),
        MenuItem("nv3", "Chicken Cheese Burger", "Chicken burger with cheese", 100.0, "", "Non Veg", false),
        MenuItem("nv4", "Egg Noodles", "Egg tossed noodles", 70.0, "", "Non Veg", false),
        MenuItem("nv5", "Egg Fried Rice", "Fried rice with egg", 80.0, "", "Non Veg", false),
        MenuItem("nv6", "Chicken Maggie", "Chicken maggi noodles", 80.0, "", "Non Veg", false),
        MenuItem("nv7", "Chicken Sandwich", "Chicken sandwich", 70.0, "", "Non Veg", false),
        MenuItem("nv8", "Crispy Chicken Burger", "Crispy chicken patty burger", 100.0, "", "Non Veg", false),
        MenuItem("nv9", "Crispy Chicken Wrap", "Crispy chicken wrap", 120.0, "", "Non Veg", false),
        MenuItem("nv10", "Chicken Seekh Kabab", "Seekh kebab style chicken", 100.0, "", "Non Veg", false),
        MenuItem("nv11", "Chicken Noodles", "Chicken tossed noodles", 100.0, "", "Non Veg", false),
        MenuItem("nv12", "Crispy Chicken Sandwich", "Crispy chicken sandwich", 110.0, "", "Non Veg", false),
        MenuItem("nv13", "Chicken Nuggets", "Deep fried chicken nuggets", 100.0, "", "Non Veg", false),
        MenuItem("nv14", "Chicken Lollipop", "Fried chicken lollipop", 110.0, "", "Non Veg", false),
        MenuItem("nv15", "Chicken Pasta", "Pasta with chicken", 130.0, "", "Non Veg", false),
        MenuItem("nv16", "Chicken Strips (KFC)", "Crispy chicken strips KFC style", 200.0, "", "Non Veg", false)
    )


    fun getChatoryChaatMenuItems(): List<MenuItem> = listOf(
        // ------- INDIAN MASALA / PARANTHAS --------
        MenuItem("IM01", "Aloo Paratha", "", 25.0, "", "Paranthas", true),
        MenuItem("IM02", "Pyaz Paratha", "", 25.0, "", "Paranthas", true),
        MenuItem("IM03", "Aloo Pyaz Paratha", "", 35.0, "", "Paranthas", true),
        MenuItem("IM04", "Gobhi Paratha", "", 35.0, "", "Paranthas", true),
        MenuItem("IM05", "Paneer Paratha", "", 40.0, "", "Paranthas", true),
        MenuItem("IM06", "2 Aloo Paratha + Dahi", "", 45.0, "", "Paranthas", true),

        // ------------ CHOWMIEN DELIGHT / NOODLES -----------
        MenuItem("N01", "Veg Chowmien", "", 40.0, "", "Noodles", true),
        MenuItem("N02", "Chilli Garlic Chowmien", "", 45.0, "", "Noodles", true),
        MenuItem("N03", "Singapuri Noodles", "", 45.0, "", "Noodles", true),
        MenuItem("N04", "Paneer Noodles", "", 50.0, "", "Noodles", true),
        MenuItem("N05", "Schezwan Noodles", "", 50.0, "", "Noodles", true),
        MenuItem("N06", "Paneer + Schezwan", "", 60.0, "", "Noodles", true),
        MenuItem("N07", "Hakka Noodles", "", 60.0, "", "Noodles", true),
        MenuItem("N08", "Chicken Noodles", "", 80.0, "", "Noodles", false),

        // ----------------- ROLLS -----------------
        MenuItem("R01", "Noodles Rolls", "", 40.0, "", "Rolls", true),
        MenuItem("R02", "Egg Rolls", "", 50.0, "", "Rolls", false),
        MenuItem("R03", "Aloo Rolls", "", 30.0, "", "Rolls", true),
        MenuItem("R04", "Veg Rolls", "", 35.0, "", "Rolls", true),
        MenuItem("R05", "Paneer Rolls", "", 50.0, "", "Rolls", true),
        MenuItem("R06", "KFC Paneer Rolls", "", 70.0, "", "Rolls", true),
        MenuItem("R07", "Chicken Rolls", "", 80.0, "", "Rolls", false),
        MenuItem("R08", "Kathi Rolls", "", 60.0, "", "Rolls", true),
        MenuItem("R09", "Spl. KFC Chicken Rolls", "", 90.0, "", "Rolls", false),

        // ----------------- WRAPS SWAPS -----------------
        MenuItem("W01", "Egg Wrap", "", 40.0, "", "Wraps", false),
        MenuItem("W02", "Aloo Wrap", "", 40.0, "", "Wraps", true),
        MenuItem("W03", "Chicken Wrap", "", 90.0, "", "Wraps", false),
        MenuItem("W04", "Veg Wrap", "", 50.0, "", "Wraps", true),
        MenuItem("W05", "Paneer Wrap", "", 60.0, "", "Wraps", true),
        MenuItem("W06", "Paneer Chunks Wrap", "", 80.0, "", "Wraps", true),

        // ----------------- ITALIAN / PASTA -----------------
        MenuItem("I01", "White Sauce Pasta", "", 90.0, "", "Pasta", true),
        MenuItem("I02", "Red Sauce Pasta", "", 90.0, "", "Pasta", true),
        MenuItem("I03", "Makhni Pasta", "", 100.0, "", "Pasta", true),
        MenuItem("I04", "Schezwan Pasta", "", 100.0, "", "Pasta", true),
        MenuItem("I05", "Mix Sauce Pasta", "", 100.0, "", "Pasta", true),
        MenuItem("I06", "Chicken Pasta", "", 120.0, "", "Pasta", false),

        // ----------------- SOUP -----------------
        MenuItem("S01", "Veg Soup", "", 40.0, "", "Soup", true),
        MenuItem("S02", "Tomato Soup", "", 40.0, "", "Soup", true),
        MenuItem("S03", "Chicken Soup", "", 50.0, "", "Soup", false),

        // ----------------- MAGGI -----------------
        MenuItem("M01", "Simple Maggi", "", 30.0, "", "Maggi", true),
        MenuItem("M02", "Masala Maggi", "", 35.0, "", "Maggi", true),
        MenuItem("M03", "Veggie Maggi", "", 35.0, "", "Maggi", true),
        MenuItem("M04", "Schezwan Maggi", "", 40.0, "", "Maggi", true),
        MenuItem("M05", "Special Maggi", "", 40.0, "", "Maggi", true),
        MenuItem("M06", "Paneer Maggi", "", 50.0, "", "Maggi", true),
        MenuItem("M07", "Paneer Maggi (large)", "", 70.0, "", "Maggi", true),

        // ----------------- SNACKS -----------------
        MenuItem("SN01", "Spring Rolls", "", 60.0, "", "Snacks", true),
        MenuItem("SN02", "Veg Bullets", "", 60.0, "", "Snacks", true),
        MenuItem("SN03", "Manchurian Dry", "", 70.0, "", "Snacks", true),
        MenuItem("SN04", "Honey Chilli Potato", "", 60.0, "", "Snacks", true),
        MenuItem("SN05", "Honey Chilli Cauliflower", "", 80.0, "", "Snacks", true),
        MenuItem("SN06", "Manchurian Gravy", "", 90.0, "", "Snacks", true),
        MenuItem("SN07", "Gobhi Manchurian", "", 80.0, "", "Snacks", true),
        MenuItem("SN08", "Chicken Lollipop", "", 100.0, "", "Snacks", false),
        MenuItem("SN09", "Chilli Paneer", "", 120.0, "", "Snacks", true),
        MenuItem("SN10", "Chicken Chilli", "", 150.0, "", "Snacks", false),

        // ----------------- MOMOS / STEAMED SENSATION -----------------
        MenuItem("MO01", "Sweet Corn Salted", "", 40.0, "", "Steamed", true),
        MenuItem("MO02", "Masala Sweet Corn", "", 50.0, "", "Steamed", true),
        MenuItem("MO03", "Cheesy Sweet Corn", "", 60.0, "", "Steamed", true),
        MenuItem("MO04", "Veg Momos", "", 60.0, "", "Steamed", true),
        MenuItem("MO05", "Kurkure Momos", "", 70.0, "", "Steamed", true),
        MenuItem("MO06", "Afghani Momos", "", 80.0, "", "Steamed", true),
        MenuItem("MO07", "Chicken Momos", "", 80.0, "", "Steamed", false),
        MenuItem("MO08", "Fried Chicken Momos", "", 90.0, "", "Steamed", false),

        // ----------------- BOMBAY BITES -----------------
        MenuItem("BB01", "Onion Vada Pav", "", 35.0, "", "Bombay Bites", true),
        MenuItem("BB02", "Schezwan Vada Pav", "", 40.0, "", "Bombay Bites", true),
        MenuItem("BB03", "Samosa", "", 20.0, "", "Bombay Bites", true),
        MenuItem("BB04", "Kachori with Sabzi", "", 40.0, "", "Bombay Bites", true),
        MenuItem("BB05", "Chole Bhature", "", 80.0, "", "Bombay Bites", true),
        MenuItem("BB06", "Pao Bhaji", "", 60.0, "", "Bombay Bites", true),

        // ----------------- CHAAT -----------------
        MenuItem("CH01", "Pani Puri (6pc)", "", 40.0, "", "Chaat", true),
        MenuItem("CH02", "Stuffed Golgappe (5pc)", "", 40.0, "", "Chaat", true),
        MenuItem("CH03", "Bhalla Papdi Chaat", "", 50.0, "", "Chaat", true),
        MenuItem("CH04", "Dahi Bhalla Chaat", "", 50.0, "", "Chaat", true),

        // ----------------- FRIES FRENZY -----------------
        MenuItem("FF01", "Salted Fries", "", 60.0, "", "Fries", true),
        MenuItem("FF02", "Indian Masala Fries", "", 70.0, "", "Fries", true),
        MenuItem("FF03", "Peri Peri Fries", "", 80.0, "", "Fries", true),
        MenuItem("FF04", "Cheesy Fries", "", 100.0, "", "Fries", true),
        MenuItem("FF05", "Makhni Fries", "", 90.0, "", "Fries", true),
        MenuItem("FF06", "Honey Chilli Fries", "", 100.0, "", "Fries", true),
        MenuItem("FF07", "Mexican Fries", "", 110.0, "", "Fries", true),

        // ----------------- ZENWICH ZONE -----------------
        MenuItem("Z01", "Aloo Sandwich", "", 50.0, "", "Sandwich", true),
        MenuItem("Z02", "Veg Sandwich", "", 50.0, "", "Sandwich", true),
        MenuItem("Z03", "Spicy Makhni Twisti", "", 55.0, "", "Sandwich", true),
        MenuItem("Z04", "Paneer Grilled", "", 80.0, "", "Sandwich", true),
        MenuItem("Z05", "Double Grilled Sandwich", "", 90.0, "", "Sandwich", true),
        MenuItem("Z06", "Chicken Sandwich", "", 80.0, "", "Sandwich", false),

        // ----------------- CRISPY KULCHA -----------------
        MenuItem("CK01", "Desi Punjabi Kulcha", "", 50.0, "", "Kulcha", true),
        MenuItem("CK02", "Onion Kulcha", "", 50.0, "", "Kulcha", true),
        MenuItem("CK03", "Nutri Keema Kulcha", "", 60.0, "", "Kulcha", true),
        MenuItem("CK04", "Paneer Kulcha", "", 60.0, "", "Kulcha", true),
        MenuItem("CK05", "Egg Kulcha With Cheese", "", 70.0, "", "Kulcha", false),
        MenuItem("CK06", "Chicken Keema Kulcha", "", 80.0, "", "Kulcha", false),

        // ----------------- RICE MANIA -----------------
        MenuItem("RM01", "Veg Fried Rice", "", 70.0, "", "Rice", true),
        MenuItem("RM02", "Paneer Fried Rice", "", 80.0, "", "Rice", true),
        MenuItem("RM03", "Mattar Rice Pulao", "", 70.0, "", "Rice", true),
        MenuItem("RM04", "Egg Fried Rice", "", 80.0, "", "Rice", false),
        MenuItem("RM05", "Veg Biryani", "", 110.0, "", "Rice", true),
        MenuItem("RM06", "Chicken Fried Rice", "", 120.0, "", "Rice", false),
        MenuItem("RM07", "Chicken Biryani", "", 150.0, "", "Rice", false),

        // ----------------- RICE COMBO -----------------
        MenuItem("RC01", "Rajma Chawal", "", 55.0, "", "Rice Combo", true),
        MenuItem("RC02", "Cholle Chawal", "", 55.0, "", "Rice Combo", true),
        MenuItem("RC03", "Kadhi Chawal", "", 55.0, "", "Rice Combo", true),
        MenuItem("RC04", "Dal Chawal", "", 60.0, "", "Rice Combo", true),
        MenuItem("RC05", "Paneer Chawal", "", 80.0, "", "Rice Combo", true),

        // ---------- INDIAN COMBO / THALI ----------
        MenuItem("TH01", "2 Butter Roti + Rajma", "", 70.0, "", "Indian Combo", true),
        MenuItem("TH02", "2 Butter Roti + Chole", "", 70.0, "", "Indian Combo", true),
        MenuItem("TH03", "2 Butter Roti + Dal Makhni", "", 70.0, "", "Indian Combo", true),
        MenuItem("TH04", "2 Butter Roti + Paneer Gravy", "", 80.0, "", "Indian Combo", true),
        MenuItem("TH05", "Dal Makhni + Nutri + Roti (3 Pc)", "", 100.0, "", "Indian Combo", true),
        MenuItem("TH06", "Paneer Thali", "Paneer Gravy + Dal + Nutri + Rice + 3 Chapati", 120.0, "", "Indian Combo", true),
        MenuItem("TH07", "Paneer Thali (Paneer Gravy + Dal Makhni + Nutri + Rice + 3 Chapati)", "", 130.0, "", "Indian Combo", true),

        // ---------- STUDY SAVER / BURGER & COMBO ----------
        MenuItem("COM01", "Sandwich + Coke + Fries", "", 70.0, "", "Combo", true),
        MenuItem("COM02", "Burger + Coke + Fries", "", 80.0, "", "Combo", true),
        MenuItem("COM03", "2 Burger + Coke + Fries", "", 100.0, "", "Combo", true),
        MenuItem("COM04", "Fried Rice + Manchurian + Coke", "", 130.0, "", "Combo", true),

        // ----------------- BEVERAGES / SHAKES / COFFEE -----------------
        MenuItem("B01", "Milk/Bournvita (small)", "", 10.0, "", "Beverages", true),
        MenuItem("B02", "Milk/Bournvita (large)", "", 15.0, "", "Beverages", true),
        MenuItem("B03", "Cappuccino", "", 25.0, "", "Beverages", true),
        MenuItem("B04", "Cappuccino (large)", "", 30.0, "", "Beverages", true),
        MenuItem("B05", "Masala Lime Soda", "", 40.0, "", "Beverages", true),
        MenuItem("B06", "Cold Coffee", "", 40.0, "", "Beverages", true),
        MenuItem("B07", "Hot Coffee", "", 30.0, "", "Beverages", true),
        MenuItem("B08", "Bournvita Shake", "", 50.0, "", "Beverages", true),
        MenuItem("B09", "Oreo Shake", "", 60.0, "", "Beverages", true),
        MenuItem("B10", "Chocolate Shake", "", 60.0, "", "Beverages", true),
        MenuItem("B11", "KitKat Shake", "", 60.0, "", "Beverages", true),
        MenuItem("B12", "Hazelnut Coffee", "", 60.0, "", "Beverages", true),
        MenuItem("B13", "Mango Shake", "", 60.0, "", "Beverages", true),
        MenuItem("B14", "Caramel Coffee", "", 60.0, "", "Beverages", true),

        // ---------- OTHERS (MRP items) ----------
        // Aerated Water, Cold Drinks, Chips, Chocolates, Soft Drink: All MRP (handle as special case if needed)

        // ---------- OMELETTE ----------
        MenuItem("OM01", "Boiled Egg", "", 15.0, "", "Omelette", false),
        MenuItem("OM02", "Bread Omelette", "", 20.0, "", "Omelette", false),
        MenuItem("OM03", "Bread Omelette With Cheese", "", 30.0, "", "Omelette", false),

        // ---------- PATTIES / SWEETS ----------
        MenuItem("PT01", "Veg Pakora", "", 20.0, "", "Patties", true),
        MenuItem("PT02", "Egg Patties", "", 20.0, "", "Patties", false),
        MenuItem("PT03", "Paneer Patties", "", 25.0, "", "Patties", true),
        MenuItem("PT04", "Brownie", "", 60.0, "", "Patties", true),
        MenuItem("PT05", "Brownie With Ice Cream", "", 80.0, "", "Patties", true),
        MenuItem("PT06", "Sizzling Brownie", "", 100.0, "", "Patties", true)
    )



    private fun getParathaHouseMenuItems(): List<MenuItem> {
        return listOf(
            // PARANTHA'S
            MenuItem("ph1", "Aloo Paratha (2 Pcs.)", "Stuffed potato paratha", 40.0, "", "Parantha", true, true),
            MenuItem("ph2", "Paneer Paratha (2 Pcs.)", "Paneer stuffed paratha", 50.0, "", "Parantha", true, true),
            MenuItem("ph3", "Gobhi Paratha (2 Pcs.)", "Cauliflower stuffed paratha", 50.0, "", "Parantha", true, false),
            MenuItem("ph4", "Methi Paratha (2 Pcs.)", "Fenugreek stuffed paratha", 50.0, "", "Parantha", true, false), // Added
            MenuItem("ph5", "Chilli Cheese Paratha (2 Pcs.)", "Spicy cheese stuffed paratha", 50.0, "", "Parantha", true, false), // Spelling fixed
            MenuItem("ph6", "Mix Veg Paratha (2 Pcs.)", "Mixed vegetables stuffed paratha", 50.0, "", "Parantha", true, false),
            MenuItem("ph7", "Plain Paratha (2 Pcs.)", "Classic plain paratha", 40.0, "", "Parantha", true, false),
            MenuItem("ph8", "Ajwain Paratha (2 Pcs.)", "Carom seed flavored paratha", 40.0, "", "Parantha", true, false),
            MenuItem("ph9", "Double Egg Paratha (2 Pcs.)", "Egg stuffed paratha", 60.0, "", "Parantha", false, false),
            MenuItem("ph10", "Mughlai Paratha (2 Pcs.)", "Mughlai style paratha", 50.0, "", "Parantha", true, false), // Added
            MenuItem("ph11", "Sugar Paratha (2 Pcs.)", "Sweet sugar paratha", 40.0, "", "Parantha", true, false),
            MenuItem("ph12", "Masala Paratha (2 Pcs.)", "Spiced paratha", 50.0, "", "Parantha", true, false),
            MenuItem("ph13", "Chicken Paratha (2 Pcs.)", "Chicken stuffed paratha", 60.0, "", "Parantha", false, false),
            MenuItem("ph14", "Egg Paratha (2 Pcs.)", "Egg stuffed paratha", 50.0, "", "Parantha", false, false),
            MenuItem("ph15", "Jeera Paratha (2 Pcs.)", "Cumin flavored paratha", 40.0, "", "Parantha", true, false),
            MenuItem("ph16", "Malabar Paratha (2 Pcs.)", "Flaky layered paratha", 50.0, "", "Parantha", true, false),
            MenuItem("ph17", "Dry Fruit Paratha (1 Pc.)", "Dry fruit stuffed paratha", 80.0, "", "Parantha", true, false),

            // DRINK'S & SHAKES
            MenuItem("ph18", "Hot Coffee", "Freshly brewed hot coffee", 30.0, "", "Drinks & Shakes", true, true),
            MenuItem("ph19", "Cold Coffee", "Chilled coffee", 30.0, "", "Drinks & Shakes", true, true),
            MenuItem("ph20", "Packed Juice", "Assorted packed juice", 0.0, "", "Drinks & Shakes", true, false), // MRP
            MenuItem("ph21", "Cold Drink", "Assorted cold drinks", 0.0, "", "Drinks & Shakes", true, false), // MRP
            MenuItem("ph22", "Plain Curd", "Homemade curd", 30.0, "", "Drinks & Shakes", true, false),
            MenuItem("ph23", "Raita", "Spiced yogurt", 40.0, "", "Drinks & Shakes", true, false),
            MenuItem("ph24", "Lime Soda (M)", "Medium lime soda", 20.0, "", "Drinks & Shakes", true, false),
            MenuItem("ph25", "Lime Soda (L)", "Large lime soda", 30.0, "", "Drinks & Shakes", true, false),
            MenuItem("ph26", "Jaljeera", "Spiced cumin drink", 30.0, "", "Drinks & Shakes", true, false),
            MenuItem("ph27", "Banana Shake (M)", "Medium banana shake", 40.0, "", "Drinks & Shakes", true, false),
            MenuItem("ph28", "Banana Shake (L)", "Large banana shake", 50.0, "", "Drinks & Shakes", true, false),
            MenuItem("ph29", "Oreo Shake (M)", "Medium Oreo shake", 40.0, "", "Drinks & Shakes", true, false),
            MenuItem("ph30", "Oreo Shake (L)", "Large Oreo shake", 50.0, "", "Drinks & Shakes", true, false),

            // SNACKS (expanded and corrected)
            MenuItem("ph31", "Egg Chaat", "Egg-based chaat", 30.0, "", "Snacks", false, false),
            MenuItem("ph32", "Egg Bhurji", "Spicy scrambled eggs", 35.0, "", "Snacks", false, false),
            MenuItem("ph33", "Half Fry (2 Eggs)", "Fried eggs sunny side up", 35.0, "", "Snacks", false, false),
            MenuItem("ph34", "Egg Omelette (3 Pcs.)", "Classic omelette", 35.0, "", "Snacks", false, false),
            MenuItem("ph35", "Egg Sandwich", "Sandwich with egg filling", 35.0, "", "Snacks", false, false),
            MenuItem("ph36", "Veg Cheese Sandwich", "Vegetable cheese sandwich", 40.0, "", "Snacks", true, false),
            MenuItem("ph37", "Veg Sandwich", "Vegetable sandwich", 30.0, "", "Snacks", true, false),
            MenuItem("ph38", "Paneer Sandwich", "Paneer sandwich", 40.0, "", "Snacks", true, false),
            MenuItem("ph39", "Tummy Yummy Chilli Eggs", "Spicy eggs", 50.0, "", "Snacks", false, false),
            MenuItem("ph40", "Egg Onion Fry", "Egg and onion stir fry", 40.0, "", "Snacks", false, false),
            MenuItem("ph41", "Egg Capsicum Fry (2 Pcs.)", "Egg and capsicum stir fry", 40.0, "", "Snacks", false, false),
            MenuItem("ph42", "Boiled Egg (2 Pcs.)", "Boiled eggs", 20.0, "", "Snacks", false, false),
            MenuItem("ph43", "Egg Momos", "Egg stuffed momos", 50.0, "", "Snacks", false, false),
            MenuItem("ph44", "Egg Crispy Fry", "Crispy fried eggs", 55.0, "", "Snacks", false, false), // Added from image
            MenuItem("ph45", "Egg Crispy Paratha", "Crispy egg paratha", 50.0, "", "Snacks", false, false),
            MenuItem("ph46", "Veg Momos", "Veg stuffed momos", 40.0, "", "Snacks", true, false),
            MenuItem("ph47", "Paneer Noodles", "Noodles with paneer", 60.0, "", "Snacks", true, false),
            MenuItem("ph48", "Garlic Noodles", "Garlic flavored noodles", 60.0, "", "Snacks", true, false),
            MenuItem("ph49", "Veg Manchurian", "Vegetable manchurian", 60.0, "", "Snacks", true, false), // Added
            MenuItem("ph50", "Egg Manchurian", "Egg in Manchurian sauce", 60.0, "", "Snacks", false, false),
            MenuItem("ph51", "Veg Pasta", "Vegetable pasta", 60.0, "", "Snacks", true, false),
            MenuItem("ph52", "Chicken Pasta", "Chicken pasta", 80.0, "", "Snacks", false, false),
            MenuItem("ph53", "Egg Kulcha", "Egg stuffed kulcha", 50.0, "", "Snacks", false, false),
            MenuItem("ph54", "Chicken Kulcha", "Chicken stuffed kulcha", 70.0, "", "Snacks", false, false),
            MenuItem("ph55", "Egg Patties", "Egg patties", 30.0, "", "Snacks", false, false),
            MenuItem("ph56", "Paneer Patties", "Paneer patties", 40.0, "", "Snacks", true, false),
            MenuItem("ph57", "Chilli Cheese Patties", "Spicy cheese patties", 40.0, "", "Snacks", true, false),
            MenuItem("ph58", "Corn Patties", "Corn patties", 40.0, "", "Snacks", true, false),
            MenuItem("ph59", "Chicken Patties", "Chicken patties", 50.0, "", "Snacks", false, false),

            // Burgers & Omelette
            MenuItem("ph60", "Egg Cheese Burger", "Egg and cheese burger", 40.0, "", "Snacks", false, false),
            MenuItem("ph61", "Chicken Cheese Burger", "Chicken and cheese burger", 50.0, "", "Snacks", false, false),
            MenuItem("ph62", "Veg Cheese Burger", "Vegetable and cheese burger", 40.0, "", "Snacks", true, false),
            MenuItem("ph63", "Handi Omelette", "Handi omelette", 50.0, "", "Snacks", false, false),

            // SOUP
            MenuItem("ph64", "Veg Soup (M)", "Medium vegetable soup", 20.0, "", "Soup", true, false),
            MenuItem("ph65", "Veg Soup (L)", "Large vegetable soup", 40.0, "", "Soup", true, false),
            MenuItem("ph66", "Chicken Soup (M)", "Medium chicken soup", 30.0, "", "Soup", false, false),
            MenuItem("ph67", "Chicken Soup (L)", "Large chicken soup", 50.0, "", "Soup", false, false),
            MenuItem("ph68", "Egg Soup (M)", "Medium egg soup", 20.0, "", "Soup", false, false),
            MenuItem("ph69", "Egg Soup (L)", "Large egg soup", 40.0, "", "Soup", false, false),

            // CONTINENTAL FOOD
            MenuItem("ph70", "Veg Hot Dog", "Vegetarian hot dog", 30.0, "", "Continental Food", true, false),
            MenuItem("ph71", "Egg Hot Dog", "Egg hot dog", 40.0, "", "Continental Food", false, false),
            MenuItem("ph72", "Chicken Hot Dog", "Chicken hot dog", 50.0, "", "Continental Food", false, false),
            MenuItem("ph73", "Chicken Rice", "Chicken rice", 70.0, "", "Continental Food", false, false),
            MenuItem("ph74", "Chicken Noodles", "Chicken noodles", 70.0, "", "Continental Food", false, false),
            MenuItem("ph75", "Egg Maggie", "Egg maggi noodles", 40.0, "", "Continental Food", false, false),
            MenuItem("ph76", "Chicken Maggie", "Chicken maggi noodles", 50.0, "", "Continental Food", false, false),
            MenuItem("ph77", "French Fries", "French fries", 40.0, "", "Continental Food", true, false),
            MenuItem("ph78", "Masala Fries", "Spicy fries", 50.0, "", "Continental Food", true, false),
            MenuItem("ph79", "Peri Peri Fries", "Peri peri fries", 50.0, "", "Continental Food", true, false),
            MenuItem("ph80", "Veg Spring Roll", "Vegetable spring roll", 40.0, "", "Continental Food", true, false),
            MenuItem("ph81", "Egg Spring Roll", "Egg spring roll", 50.0, "", "Continental Food", false, false),
            MenuItem("ph82", "Chicken Spring Roll", "Chicken spring roll", 70.0, "", "Continental Food", false, false),
            MenuItem("ph83", "Fried Momos", "Fried momos", 50.0, "", "Continental Food", true, false),
            MenuItem("ph84", "Chicken Fried Momos", "Chicken fried momos", 70.0, "", "Continental Food", false, false),
            MenuItem("ph85", "Paneer Burji", "Paneer bhurji", 100.0, "", "Continental Food", true, false),
            MenuItem("ph86", "Chilli Mushroom", "Chilli mushroom", 100.0, "", "Continental Food", true, false),
            MenuItem("ph87", "Crispy Corn", "Crispy corn", 70.0, "", "Continental Food", true, false),

            // ROLL
            MenuItem("ph88", "Double Egg Roll", "Double egg roll", 60.0, "", "Roll", false, false),
            MenuItem("ph89", "Egg Noodles Roll", "Egg noodles roll", 70.0, "", "Roll", false, false),
            MenuItem("ph90", "Chicken Double Egg Roll", "Chicken double egg roll", 90.0, "", "Roll", false, false),
            MenuItem("ph91", "Chicken Noodles Roll", "Chicken noodles roll", 90.0, "", "Roll", false, false),
            MenuItem("ph92", "Veg Roll", "Vegetarian roll", 50.0, "", "Roll", true, false),
            MenuItem("ph93", "Paneer Roll", "Paneer roll", 70.0, "", "Roll", true, false),

            // LASSI
            MenuItem("ph94", "Sweet Lassi", "Sweet lassi", 30.0, "", "Lassi", true, true),
            MenuItem("ph95", "Jeera Lassi", "Cumin flavored lassi", 30.0, "", "Lassi", true, false),
            MenuItem("ph96", "Masala Lassi", "Spiced lassi", 30.0, "", "Lassi", true, false),
            MenuItem("ph97", "Mango Lassi", "Mango lassi", 40.0, "", "Lassi", true, false),
            MenuItem("ph98", "Rose Lassi", "Rose lassi", 40.0, "", "Lassi", true, false),
            MenuItem("ph99", "Elachi Lassi", "Cardamom lassi", 40.0, "", "Lassi", true, false),
            MenuItem("ph100", "Kesar Badam Lassi", "Saffron almond lassi", 40.0, "", "Lassi", true, false),
            MenuItem("ph101", "Chocolate Lassi", "Chocolate lassi", 40.0, "", "Lassi", true, false),

            // POPCORNS
            MenuItem("ph102", "Salted Pop Corns (M)", "Medium salted popcorn", 20.0, "", "Popcorn", true, false),
            MenuItem("ph103", "Salted Pop Corns (L)", "Large salted popcorn", 40.0, "", "Popcorn", true, false),
            MenuItem("ph104", "Butter Pop Corns", "Butter popcorn", 40.0, "", "Popcorn", true, false),
            MenuItem("ph105", "Cheese Pop Corns", "Cheese popcorn", 50.0, "", "Popcorn", true, false),

            // JUICES (all sizes and corrections)
            MenuItem("ph106", "Orange Juice (M)", "Medium orange juice", 30.0, "", "Juice", true, false),
            MenuItem("ph107", "Orange Juice (L)", "Large orange juice", 50.0, "", "Juice", true, false),
            MenuItem("ph108", "Mix Juice (M)", "Medium mixed fruit juice", 30.0, "", "Juice", true, false),
            MenuItem("ph109", "Mix Juice (L)", "Large mixed fruit juice", 50.0, "", "Juice", true, false),
            MenuItem("ph110", "Carrot Juice (M)", "Medium carrot juice", 30.0, "", "Juice", true, false),
            MenuItem("ph111", "Carrot Juice (L)", "Large carrot juice", 50.0, "", "Juice", true, false),
            MenuItem("ph112", "Mosambi Juice (M)", "Medium sweet lime juice", 30.0, "", "Juice", true, false),
            MenuItem("ph113", "Mosambi Juice (L)", "Large sweet lime juice", 50.0, "", "Juice", true, false),
            MenuItem("ph114", "Pine Apple Juice (M)", "Medium pineapple juice", 30.0, "", "Juice", true, false),
            MenuItem("ph115", "Pine Apple Juice (L)", "Large pineapple juice", 50.0, "", "Juice", true, false),
            MenuItem("ph116", "Pomegranate Juice (M)", "Medium pomegranate juice", 50.0, "", "Juice", true, false),
            MenuItem("ph117", "Pomegranate Juice (L)", "Large pomegranate juice", 80.0, "", "Juice", true, false),

            // THE CHICKEN KITCHEN
            MenuItem("ph118", "Chicken Curry", "Chicken curry", 120.0, "", "Chicken Kitchen", false, false),
            MenuItem("ph119", "Chicken Curry (Large)", "Chicken curry large", 200.0, "", "Chicken Kitchen", false, false),
            MenuItem("ph120", "Chicken Masala", "Chicken masala", 120.0, "", "Chicken Kitchen", false, false),
            MenuItem("ph121", "Chicken Masala (Large)", "Chicken masala large", 200.0, "", "Chicken Kitchen", false, false),
            MenuItem("ph122", "Butter Chicken", "Butter chicken", 150.0, "", "Chicken Kitchen", false, false),
            MenuItem("ph123", "Butter Chicken (Large)", "Butter chicken large", 200.0, "", "Chicken Kitchen", false, false), // Added
            MenuItem("ph124", "Chicken Biryani + Raita", "Chicken biryani with raita", 150.0, "", "Chicken Kitchen", false, false),

            // FROM THE TANDOORI
            MenuItem("ph125", "Chicken Tandoori", "Chicken tandoori", 150.0, "", "Tandoori", false, false),
            MenuItem("ph126", "Chicken Tandoori (Full)", "Full chicken tandoori", 250.0, "", "Tandoori", false, false),
            MenuItem("ph127", "Chicken Sheek Kabab", "Chicken sheek kabab", 150.0, "", "Tandoori", false, false),
            MenuItem("ph128", "Chicken Tandoori Chat", "Chicken tandoori chat", 100.0, "", "Tandoori", false, false),
            MenuItem("ph129", "Chicken Cheese Shots", "Chicken cheese shots", 150.0, "", "Tandoori", false, false),
            MenuItem("ph130", "Chicken Cheese Shots (Full)", "Full chicken cheese shots", 200.0, "", "Tandoori", false, false),
            MenuItem("ph131", "Chicken Salami", "Chicken salami", 150.0, "", "Tandoori", false, false),

            // THE CURRY HOUSE
            MenuItem("ph132", "Egg Masala (2 Pcs.)", "Egg masala", 50.0, "", "Curry House", false, false),
            MenuItem("ph133", "Egg Korma (2 Pcs.)", "Egg korma", 60.0, "", "Curry House", false, false),
            MenuItem("ph134", "Egg Curry (2 Pcs.)", "Egg curry", 50.0, "", "Curry House", false, false),
            MenuItem("ph135", "Egg Paneer Burji", "Egg paneer bhurji", 60.0, "", "Curry House", false, false),
            MenuItem("ph136", "Punjabi Egg Curry", "Punjabi style egg curry", 60.0, "", "Curry House", false, false),
            MenuItem("ph137", "Egg Butter Masala", "Egg butter masala", 70.0, "", "Curry House", false, false),

            // CHAI KI CHUSKI
            MenuItem("ph138", "Masala Chai", "Masala tea", 15.0, "", "Chai", true, false),
            MenuItem("ph139", "Tandoori Chai", "Tandoori tea", 20.0, "", "Chai", true, false),
            MenuItem("ph140", "Rajwadi Chai", "Royal tea", 20.0, "", "Chai", true, false),
            MenuItem("ph141", "Ice Tea", "Iced tea", 20.0, "", "Chai", true, false),
            MenuItem("ph142", "Green Tea", "Green tea", 15.0, "", "Chai", true, false),

            // COMBO MOGAMBO
            MenuItem("ph143", "Egg Rice + Manchurian", "Egg fried rice with manchurian", 80.0, "", "Combo Mogambo", false, false),
            MenuItem("ph144", "Egg Noodles + Manchurian", "Egg noodles with manchurian", 80.0, "", "Combo Mogambo", false, false),
            MenuItem("ph145", "Egg Biryani + Raita", "Egg biryani with raita", 80.0, "", "Combo Mogambo", false, false),

            // THALI
            MenuItem("ph146", "Non-Veg Thali", "Non-vegetarian thali", 140.0, "", "Thali", false, false),
            MenuItem("ph147", "Spl. Non-Veg Thali", "Special non-vegetarian thali", 170.0, "", "Thali", false, false),
            MenuItem("ph148", "Egg Curry + 2 Lacha Paratha + Rice", "Egg curry, paratha, rice", 70.0, "", "Thali", false, false),
            MenuItem("ph149", "Egg Butter Masala + 2 Lacha Paratha", "Egg butter masala, paratha", 80.0, "", "Thali", false, false),
            MenuItem("ph150", "Dal Makhni + 2 Lacha Paratha", "Dal makhni, paratha", 70.0, "", "Thali", true, false),
            MenuItem("ph151", "Aloo/Poha (4 Pcs.) + Raita + Sabzi/Chole", "Aloo/poha, raita, sabzi/chole", 70.0, "", "Thali", true, false),

            // DESI SNACKS
            MenuItem("ph152", "Sabji Kachori", "Vegetable kachori", 20.0, "", "Desi Snacks", true, false),
            MenuItem("ph153", "Bread Pakoda", "Bread pakoda", 25.0, "", "Desi Snacks", true, false),
            MenuItem("ph154", "Vada Pav", "Vada pav", 20.0, "", "Desi Snacks", true, false),
            MenuItem("ph155", "Samosa", "Samosa", 15.0, "", "Desi Snacks", true, false),

            // BOW BOWLS
            MenuItem("ph156", "Chole Rice", "Chole with rice", 60.0, "", "Bow Bowls", true, false),
            MenuItem("ph157", "Rajmah Rice", "Rajma with rice", 60.0, "", "Bow Bowls", true, false),
            MenuItem("ph158", "Kadhi Rice", "Kadhi with rice", 60.0, "", "Bow Bowls", true, false),
            MenuItem("ph159", "Soya Chaap Rice", "Soya chaap with rice", 70.0, "", "Bow Bowls", true, false),
            MenuItem("ph160", "Paneer Rice", "Paneer with rice", 70.0, "", "Bow Bowls", true, false),
            MenuItem("ph161", "Chilly Paneer Rice", "Chilli paneer with rice", 70.0, "", "Bow Bowls", true, false),
            MenuItem("ph162", "Dal Makhni Rice", "Dal makhni with rice", 70.0, "", "Bow Bowls", true, false),
            MenuItem("ph163", "Egg Curry Rice", "Egg curry with rice", 60.0, "", "Bow Bowls", false, false),
            MenuItem("ph164", "Chicken Rice", "Chicken with rice", 80.0, "", "Bow Bowls", false, false),
            MenuItem("ph165", "Lemon Rice", "Lemon flavored rice", 60.0, "", "Bow Bowls", true, false),

            // FLAVOUR MILK
            MenuItem("ph166", "Bournvita Milk", "Bournvita flavored milk", 40.0, "", "Flavour Milk", true, false),
            MenuItem("ph167", "Chocolate Milk", "Chocolate flavored milk", 40.0, "", "Flavour Milk", true, false),
            MenuItem("ph168", "Kesar Milk", "Saffron flavored milk", 40.0, "", "Flavour Milk", true, false),
            MenuItem("ph169", "Mango Milk", "Mango flavored milk", 40.0, "", "Flavour Milk", true, false),
            MenuItem("ph170", "Almond Milk", "Almond flavored milk", 40.0, "", "Flavour Milk", true, false),
            MenuItem("ph171", "Rose Milk", "Rose flavored milk", 40.0, "", "Flavour Milk", true, false),
            MenuItem("ph172", "Strawberry Milk", "Strawberry flavored milk", 40.0, "", "Flavour Milk", true, false),
            MenuItem("ph173", "Badam Milk", "Badam flavored milk", 40.0, "", "Flavour Milk", true, false),
            // ... Packed items (Fruit Beer, Cold Drinks, etc.) - All have 'MRP' instead of price, so skip/keep as is
        )
    }

    private fun getBakerzHubMenuItems(): List<MenuItem> {
        return listOf(
            // BAKER'Z HUB MEALS
            MenuItem("bh1", "Veg. Burger + Fries + Coke (250ml)", "", 110.0, "", "Meal", true),
            MenuItem("bh2", "Chicken Burger + Fries + Coke (250ml)", "", 130.0, "", "Meal", false),
            MenuItem("bh3", "Chicken Biryani (Half)", "", 120.0, "", "Meal", false),
            MenuItem("bh4", "Chicken Biryani (Full)", "", 230.0, "", "Meal", false),

            // MOJITO
            MenuItem("bh5", "Masala Coke", "", 59.0, "", "Mojito", true),
            MenuItem("bh6", "Mousami Soda", "", 59.0, "", "Mojito", true),
            MenuItem("bh7", "Lemon Mint (LMG)", "", 59.0, "", "Mojito", true),          // as per image
            MenuItem("bh8", "Virgin Mojito", "", 69.0, "", "Mojito", true),
            MenuItem("bh9", "Green Apple", "", 69.0, "", "Mojito", true),
            MenuItem("bh10", "Icey Blue", "", 69.0, "", "Mojito", true),
            MenuItem("bh11", "Lemon Iced Tea", "", 69.0, "", "Mojito", true),

            // SHAKES (add with-icecream option)
            MenuItem("bh12", "Banana Shake", "With ice cream +20", 59.0, "", "Shake", true),
            MenuItem("bh13", "Chocolate Shake", "With ice cream +20", 59.0, "", "Shake", true),
            MenuItem("bh14", "Chocolate Oreo Shake", "With ice cream +20", 79.0, "", "Shake", true),
            MenuItem("bh15", "Strawberry Shake", "With ice cream +20", 69.0, "", "Shake", true),

            // COLD COFFEE (frappe based)
            MenuItem("bh16", "Classic Cold Coffee", "Frappe based", 69.0, "", "Cold Coffee", true),
            MenuItem("bh17", "Irish Cold Coffee", "Frappe based", 89.0, "", "Cold Coffee", true),
            MenuItem("bh18", "Hazelnut Cold Coffee", "Frappe based", 89.0, "", "Cold Coffee", true),
            MenuItem("bh19", "Tiramisu Cold Coffee", "Frappe based", 99.0, "", "Cold Coffee", true),

            // NOODLES
            MenuItem("bh20", "Veg. Noodle", "", 80.0, "", "Noodles", true),
            MenuItem("bh21", "Paneer Noodle", "", 120.0, "", "Noodles", true),
            MenuItem("bh22", "Hakka Noodle", "", 130.0, "", "Noodles", true),
            MenuItem("bh23", "Egg Noodle", "", 110.0, "", "Noodles", false),
            MenuItem("bh24", "Chicken Noodle", "", 120.0, "", "Noodles", false),

            // FRIED RICE
            MenuItem("bh25", "Veg. Fried Rice", "", 80.0, "", "Fried Rice", true),
            MenuItem("bh26", "Paneer Fried Rice", "", 120.0, "", "Fried Rice", true),
            MenuItem("bh27", "Egg Fried Rice", "", 110.0, "", "Fried Rice", false),
            MenuItem("bh28", "Chicken Fried Rice", "", 120.0, "", "Fried Rice", false),

            // CHINESE SNACKS
            MenuItem("bh29", "Spring Roll", "", 80.0, "", "Chinese Snack", true),
            MenuItem("bh30", "Chilly Paneer (6PC)", "", 130.0, "", "Chinese Snack", true),
            MenuItem("bh31", "Chilly Potato", "", 100.0, "", "Chinese Snack", true),
            MenuItem("bh32", "Honey Chilly Potato", "", 110.0, "", "Chinese Snack", true),
            MenuItem("bh33", "Crispy Fried Chaap", "", 120.0, "", "Chinese Snack", true),
            MenuItem("bh34", "Chilly Chicken", "", 120.0, "", "Chinese Snack", false),
            MenuItem("bh35", "Fried Chicken (Boneless 8PC)", "", 230.0, "", "Chinese Snack", false),

            // BURGER
            MenuItem("bh36", "Veggie Burger", "", 50.0, "", "Burger", true),
            MenuItem("bh37", "Punjabi Masala Burger", "", 60.0, "", "Burger", true),
            MenuItem("bh38", "Mexican Cheese Burger", "", 80.0, "", "Burger", true),
            MenuItem("bh39", "Crispy Chaap Burger", "", 80.0, "", "Burger", true),
            MenuItem("bh40", "Chicken Burger", "", 90.0, "", "Burger", false),
            MenuItem("bh41", "Hot & Crispy Chicken Burger", "", 130.0, "", "Burger", false),

            // SANDWICH
            MenuItem("bh42", "Veg. Grilled S/D", "", 90.0, "", "Sandwich", true),
            MenuItem("bh43", "Corn Grilled S/D", "", 95.0, "", "Sandwich", true),
            MenuItem("bh44", "Paneer Korma S/D", "", 130.0, "", "Sandwich", true),
            MenuItem("bh45", "Top Loaded Pizza S/D", "", 130.0, "", "Sandwich", true),
            MenuItem("bh46", "Chicken Korma S/D", "", 130.0, "", "Sandwich", false),
            MenuItem("bh47", "Chicken Makhani S/D", "", 150.0, "", "Sandwich", false),
            MenuItem("bh48", "Top Loaded Chicken Pizza S/D", "", 160.0, "", "Sandwich", false),

            // PATTY
            MenuItem("bh49", "Aloo Patty", "", 25.0, "", "Patty", true),
            MenuItem("bh50", "Cheese & Corn Patty", "", 30.0, "", "Patty", true),
            MenuItem("bh51", "Pizza Patty", "", 50.0, "", "Patty", true),
            MenuItem("bh52", "Paneer Achari Patty", "", 60.0, "", "Patty", true),
            MenuItem("bh53", "Chicken Patty", "", 70.0, "", "Patty", false),

            // MOMOS (STEAM/FRIED)
            MenuItem("bh54", "Veg. Momos (Steam/Fried)", "", 75.0, "", "Momos", true),
            MenuItem("bh55", "Corn Momos (Steam/Fried)", "", 95.0, "", "Momos", true),
            MenuItem("bh56", "Paneer Momos (Steam/Fried)", "", 115.0, "", "Momos", true),
            MenuItem("bh57", "Chicken Momos (Steam/Fried)", "", 115.0, "", "Momos", false),
            // KURKURE
            MenuItem("bh58", "Veg. Momos (Kurkure)", "", 95.0, "", "Momos", true),
            MenuItem("bh59", "Corn Momos (Kurkure)", "", 95.0, "", "Momos", true),
            MenuItem("bh60", "Paneer Momos (Kurkure)", "", 115.0, "", "Momos", true),
            MenuItem("bh61", "Chicken Momos (Kurkure)", "", 115.0, "", "Momos", false),
            // ACHARI
            MenuItem("bh62", "Veg. Momos (Achari)", "", 115.0, "", "Momos", true),
            MenuItem("bh63", "Corn Momos (Achari)", "", 115.0, "", "Momos", true),
            MenuItem("bh64", "Paneer Momos (Achari)", "", 135.0, "", "Momos", true),
            MenuItem("bh65", "Chicken Momos (Achari)", "", 135.0, "", "Momos", false),

            // WRAP
            MenuItem("bh66", "Crispy Veg. Wrap", "", 80.0, "", "Wrap", true),
            MenuItem("bh67", "Veg. Zinger (Chaap) Wrap", "", 90.0, "", "Wrap", true),
            MenuItem("bh68", "Mexican Wrap", "", 100.0, "", "Wrap", true),
            MenuItem("bh69", "Paneer Wrap", "", 110.0, "", "Wrap", true),
            MenuItem("bh70", "Chicken Wrap", "", 130.0, "", "Wrap", false),
            MenuItem("bh71", "Chicken Hot & Crispy Wrap", "", 160.0, "", "Wrap", false),

            // PIZZA (Veg)
            MenuItem("bh72", "Cheese & Corn Pizza", "", 110.0, "", "Pizza", true),
            MenuItem("bh73", "Garden Fresh Pizza", "Capsicum, Onion, Tomato", 120.0, "", "Pizza", true), // Description per image corrected
            MenuItem("bh74", "Paneer Onion Pizza", "Capsicum, Onion, Tomato, Jalapeno", 140.0, "", "Pizza", true),
            MenuItem("bh75", "Mexican Wave Pizza", "Mexican Spices", 160.0, "", "Pizza", true),
            MenuItem("bh76", "Farm House Pizza", "Capsicum, Onion, Mushroom", 160.0, "", "Pizza", true), // Description per image corrected
            MenuItem("bh77", "Cheese Tandoori Pizza", "Capsicum, Onion, Tomato, Jalapeno, Tandoori Shot", 170.0, "", "Pizza", true),
            MenuItem("bh78", "Baker'z Hub Spl. Pizza", "Fully loaded top veg with cheese", 210.0, "", "Pizza", true),

            // PIZZA (Non-Veg)
            MenuItem("bh79", "Chicken Tikka Pizza", "", 200.0, "", "Non-Veg Pizza", false),
            MenuItem("bh80", "Chicken Golden Delight Pizza", "", 210.0, "", "Non-Veg Pizza", false),
            MenuItem("bh81", "Smoked Chicken Pizza", "", 230.0, "", "Non-Veg Pizza", false),
            MenuItem("bh82", "Baker'z Hub Chicken Spl. Pizza", "Fully loaded, with cheese", 270.0, "", "Non-Veg Pizza", false),

            // AMERICAN FRIES
            MenuItem("bh83", "Golden Fries", "", 80.0, "", "Fries", true),
            MenuItem("bh84", "Masala Fries", "", 90.0, "", "Fries", true),
            MenuItem("bh85", "Cheezy Fries", "", 100.0, "", "Fries", true),
            MenuItem("bh86", "Peri Peri Fries", "", 100.0, "", "Fries", true),
            MenuItem("bh87", "Loaded Chicken Crispy Fries", "", 180.0, "", "Fries", false),

            // PASTA (Veg/Non-Veg)
            MenuItem("bh88", "White/Red Sauce Pasta", "Veg. & Non Veg.", 120.0, "", "Pasta", true),
            MenuItem("bh89", "Mix Sauce Pasta", "Veg. & Non Veg.", 130.0, "", "Pasta", true),
            MenuItem("bh90", "Tandoori Pasta", "Veg. & Non Veg.", 150.0, "", "Pasta", true),
            MenuItem("bh91", "Achari Pasta", "Veg. & Non Veg.", 150.0, "", "Pasta", true),

            // CAKE & PASTRY (as in image, half kg rate)
            MenuItem("bh92", "Cake & Pastry (Half KG)", "", 350.0, "", "Cake", true)
        )
    }


    private fun getJuiceBarMenuItems(): List<MenuItem> {
        return listOf(
            // JUICE
            MenuItem("j1", "Mix Juice (M)", "Fresh mixed fruit juice, medium size", 50.0, "", "Juice", true),
            MenuItem("j2", "Mix Juice (L)", "Fresh mixed fruit juice, large size", 90.0, "", "Juice", true),
            MenuItem("j3", "Pineapple Juice (M)", "Fresh pineapple juice, medium size", 50.0, "", "Juice", true),
            MenuItem("j4", "Pineapple Juice (L)", "Fresh pineapple juice, large size", 90.0, "", "Juice", true),
            MenuItem("j5", "Mosambi Juice (M)", "Fresh mosambi juice, medium size", 50.0, "", "Juice", true),
            MenuItem("j6", "Mosambi Juice (L)", "Fresh mosambi juice, large size", 90.0, "", "Juice", true),
            MenuItem("j7", "Orange Juice (M)", "Fresh orange juice, medium size", 50.0, "", "Juice", true),
            MenuItem("j8", "Orange Juice (L)", "Fresh orange juice, large size", 90.0, "", "Juice", true),
            MenuItem("j9", "Anar Juice (M)", "Fresh pomegranate juice, medium size", 70.0, "", "Juice", true),
            MenuItem("j10", "Anar Juice (L)", "Fresh pomegranate juice, large size", 120.0, "", "Juice", true),
            MenuItem("j11", "Fruit Chaat (M)", "Mixed fruit chaat, medium size", 70.0, "", "Juice", true),
            MenuItem("j12", "Fruit Chaat (L)", "Mixed fruit chaat, large size", 120.0, "", "Juice", true),
            MenuItem("j13", "Carrot Juice (M)", "Fresh carrot juice, medium size", 50.0, "", "Juice", true),
            MenuItem("j14", "Carrot Juice (L)", "Fresh carrot juice, large size", 90.0, "", "Juice", true),
            MenuItem("j15", "Green Apple Juice (M)", "Fresh green apple juice, medium size", 70.0, "", "Juice", true),
            MenuItem("j16", "Green Apple Juice (L)", "Fresh green apple juice, large size", 120.0, "", "Juice", true),

            // SHAKES
            MenuItem("s1", "Banana Shake (M)", "Banana shake, medium size", 50.0, "", "Shakes", true),
            MenuItem("s2", "Banana Shake (L)", "Banana shake, large size", 90.0, "", "Shakes", true),
            MenuItem("s3", "Oats Banana Shake (M)", "Oats banana shake, medium size", 70.0, "", "Shakes", true),
            MenuItem("s4", "Oats Banana Shake (L)", "Oats banana shake, large size", 120.0, "", "Shakes", true),
            MenuItem("s5", "Papaya Shake (M)", "Papaya shake, medium size", 50.0, "", "Shakes", true),
            MenuItem("s6", "Papaya Shake (L)", "Papaya shake, large size", 90.0, "", "Shakes", true),
            MenuItem("s7", "Mango Shake (M)", "Mango shake, medium size", 70.0, "", "Shakes", true),
            MenuItem("s8", "Mango Shake (L)", "Mango shake, large size", 120.0, "", "Shakes", true),
            MenuItem("s9", "Chiku Shake (M)", "Chiku shake, medium size", 70.0, "", "Shakes", true),
            MenuItem("s10", "Chiku Shake (L)", "Chiku shake, large size", 120.0, "", "Shakes", true),
            MenuItem("s11", "Butter Scotch Shake (M)", "Butterscotch shake, medium size", 70.0, "", "Shakes", true),
            MenuItem("s12", "Butter Scotch Shake (L)", "Butterscotch shake, large size", 120.0, "", "Shakes", true),
            MenuItem("s13", "Chocolate Shake (M)", "Chocolate shake, medium size", 70.0, "", "Shakes", true),
            MenuItem("s14", "Chocolate Shake (L)", "Chocolate shake, large size", 120.0, "", "Shakes", true),
            MenuItem("s15", "Strawberry Shake (M)", "Strawberry shake, medium size", 70.0, "", "Shakes", true),
            MenuItem("s16", "Strawberry Shake (L)", "Strawberry shake, large size", 120.0, "", "Shakes", true),
            MenuItem("s17", "Guava Shake (M)", "Guava shake, medium size", 70.0, "", "Shakes", true),
            MenuItem("s18", "Guava Shake (L)", "Guava shake, large size", 120.0, "", "Shakes", true),
            MenuItem("s19", "Kiwi Shake (M)", "Kiwi shake, medium size", 70.0, "", "Shakes", true),
            MenuItem("s20", "Kiwi Shake (L)", "Kiwi shake, large size", 120.0, "", "Shakes", true),
            MenuItem("s21", "Kitkat Shake (M)", "Kitkat shake, medium size", 80.0, "", "Shakes", true),
            MenuItem("s22", "Kitkat Shake (L)", "Kitkat shake, large size", 140.0, "", "Shakes", true),
            MenuItem("s23", "Peanut Butter Shake (M)", "Peanut butter shake, medium size", 80.0, "", "Shakes", true),
            MenuItem("s24", "Peanut Butter Shake (L)", "Peanut butter shake, large size", 140.0, "", "Shakes", true),
            MenuItem("s25", "Choco-Nutella Shake (M)", "Choco-Nutella shake, medium size", 100.0, "", "Shakes", true),
            MenuItem("s26", "Choco-Nutella Shake (L)", "Choco-Nutella shake, large size", 180.0, "", "Shakes", true),
            MenuItem("s27", "Oreo Shake (M)", "Oreo shake, medium size", 80.0, "", "Shakes", true),
            MenuItem("s28", "Oreo Shake (L)", "Oreo shake, large size", 140.0, "", "Shakes", true),
            MenuItem("s29", "Black Berry Shake (M)", "Black berry shake, medium size", 80.0, "", "Shakes", true),
            MenuItem("s30", "Black Berry Shake (L)", "Black berry shake, large size", 140.0, "", "Shakes", true),
            MenuItem("s31", "Blue Berry Shake (M)", "Blue berry shake, medium size", 80.0, "", "Shakes", true),
            MenuItem("s32", "Blue Berry Shake (L)", "Blue berry shake, large size", 140.0, "", "Shakes", true),
            MenuItem("s33", "Paan Shake (M)", "Paan shake, medium size", 80.0, "", "Shakes", true),
            MenuItem("s34", "Paan Shake (L)", "Paan shake, large size", 140.0, "", "Shakes", true),
            MenuItem("s35", "Roasted Almond Shake (M)", "Roasted almond shake, medium size", 80.0, "", "Shakes", true),
            MenuItem("s36", "Roasted Almond Shake (L)", "Roasted almond shake, large size", 140.0, "", "Shakes", true),
            MenuItem("s37", "Mix Fruit Shake (M)", "Mix fruit shake, medium size", 70.0, "", "Shakes", true),
            MenuItem("s38", "Mix Fruit Shake (L)", "Mix fruit shake, large size", 120.0, "", "Shakes", true),
            MenuItem("s39", "Thandai (M)", "Thandai, medium size", 70.0, "", "Shakes", true),
            MenuItem("s40", "Thandai (L)", "Thandai, large size", 120.0, "", "Shakes", true),

            // ICE CREAM SHAKE
            MenuItem("ics1", "Oreo Shake with Ice Cream", "Oreo shake with ice cream", 80.0, "", "Ice Cream Shake", true),
            MenuItem("ics2", "Butter Scotch with Ice Cream", "Butterscotch shake with ice cream", 80.0, "", "Ice Cream Shake", true),
            MenuItem("ics3", "Black Current with Ice Cream", "Black current shake with ice cream", 80.0, "", "Ice Cream Shake", true),
            MenuItem("ics4", "Vanilla with Ice Cream", "Vanilla shake with ice cream", 80.0, "", "Ice Cream Shake", true),
            MenuItem("ics5", "Cold Coffee with Ice Cream", "Cold coffee with ice cream", 80.0, "", "Ice Cream Shake", true),

            // FRUIT CHAAT
            MenuItem("fc1", "Plain Fruit Chaat (M)", "Plain fruit chaat, medium size", 50.0, "", "Fruit Chaat", true),
            MenuItem("fc2", "Plain Fruit Chaat (L)", "Plain fruit chaat, large size", 90.0, "", "Fruit Chaat", true),
            MenuItem("fc3", "Cream Fruit Chaat (M)", "Cream fruit chaat, medium size", 70.0, "", "Fruit Chaat", true),
            MenuItem("fc4", "Cream Fruit Chaat (L)", "Cream fruit chaat, large size", 140.0, "", "Fruit Chaat", true),
            MenuItem("fc5", "Green Salad (M)", "Green salad, medium size", 50.0, "", "Fruit Chaat", true),
            MenuItem("fc6", "Green Salad (L)", "Green salad, large size", 90.0, "", "Fruit Chaat", true),
            MenuItem("fc7", "Watermelon Fruit Chaat (M)", "Watermelon fruit chaat, medium size", 50.0, "", "Fruit Chaat", true),
            MenuItem("fc8", "Watermelon Fruit Chaat (L)", "Watermelon fruit chaat, large size", 90.0, "", "Fruit Chaat", true),
            MenuItem("fc9", "Chatpata Sweet Corn (M)", "Chatpata sweet corn, medium size", 50.0, "", "Fruit Chaat", true),
            MenuItem("fc10", "Chatpata Sweet Corn (L)", "Chatpata sweet corn, large size", 90.0, "", "Fruit Chaat", true),
            MenuItem("fc11", "Pineapple Chaat (M)", "Pineapple chaat, medium size", 70.0, "", "Fruit Chaat", true),
            MenuItem("fc12", "Pineapple Chaat (L)", "Pineapple chaat, large size", 140.0, "", "Fruit Chaat", true),

            // JUICE (SPECIALS)
            MenuItem("js1", "Cucumber Juice", "Fresh cucumber juice", 50.0, "", "Juice", true),
            MenuItem("js2", "Bitter Guard Juice", "Fresh bitter guard juice", 50.0, "", "Juice", true),
            MenuItem("js3", "Bottle Guard Juice", "Fresh bottle guard juice", 50.0, "", "Juice", true),
            MenuItem("js4", "Beetroot Juice", "Fresh beetroot juice", 50.0, "", "Juice", true),
            MenuItem("js5", "Grapes Juice", "Fresh grapes juice", 50.0, "", "Juice", true),
            MenuItem("js6", "Watermelon Juice", "Fresh watermelon juice", 50.0, "", "Juice", true),
            MenuItem("js7", "Mix Vegetable Juice", "Mixed vegetable juice", 50.0, "", "Juice", true),

            // LASSI (THICK & BIG)
            MenuItem("l1", "Chocolate Lassi", "Thick chocolate lassi", 70.0, "", "Lassi", true),
            MenuItem("l2", "Mango Lassi", "Thick mango lassi", 70.0, "", "Lassi", true),
            MenuItem("l3", "Strawberry Lassi", "Thick strawberry lassi", 70.0, "", "Lassi", true),
            MenuItem("l4", "Sweet Lassi", "Thick sweet lassi", 60.0, "", "Lassi", true),
            MenuItem("l5", "Namkeen Lassi", "Thick namkeen lassi", 60.0, "", "Lassi", true),

            // MOJITO
            MenuItem("m1", "Mint Mojito", "Refreshing mint mojito", 60.0, "", "Mojito", true),
            MenuItem("m2", "Pan Mojito", "Pan flavored mojito", 60.0, "", "Mojito", true),
            MenuItem("m3", "Spicy Mango", "Spicy mango mojito", 60.0, "", "Mojito", true),
            MenuItem("m4", "Watermelon", "Watermelon mojito", 60.0, "", "Mojito", true),
            MenuItem("m5", "Blue Curaco", "Blue curacao mojito", 60.0, "", "Mojito", true),
            MenuItem("m6", "Green Apple", "Green apple mojito", 60.0, "", "Mojito", true),
            MenuItem("m7", "Aal Jeera", "Aal jeera mojito", 60.0, "", "Mojito", true),
            MenuItem("m8", "Ice Tea", "Iced tea", 60.0, "", "Mojito", true),
            MenuItem("m9", "Kala Khata", "Kala khata mojito", 60.0, "", "Mojito", true),
            MenuItem("m10", "Chilli Guava", "Chilli guava mojito", 60.0, "", "Mojito", true),
            MenuItem("m11", "Bubble Gum", "Bubble gum mojito", 60.0, "", "Mojito", true),

            // LEMON WATER
            MenuItem("lw1", "Plain Lemon Water", "Plain lemon water", 40.0, "", "Lemon Water", true),
            MenuItem("lw2", "Lemon Soda", "Lemon soda", 40.0, "", "Lemon Water", true),
            MenuItem("lw3", "Soda Lemon Water", "Soda lemon water", 50.0, "", "Lemon Water", true),
            MenuItem("lw4", "Lemon Mint", "Lemon mint water", 50.0, "", "Lemon Water", true),
            MenuItem("lw5", "Lemon Honey Mint", "Lemon honey mint water", 50.0, "", "Lemon Water", true),

            // SNACKS
            MenuItem("sn1", "Plain Veg Grilled Sandwich", "Plain veg grilled sandwich", 60.0, "", "Snacks", true),
            MenuItem("sn2", "Corn Sandwich", "Corn sandwich", 70.0, "", "Snacks", true),
            MenuItem("sn3", "Paneer Sandwich", "Paneer sandwich", 80.0, "", "Snacks", true),
            MenuItem("sn4", "Cold Sandwich", "Cold sandwich", 60.0, "", "Snacks", true),
            MenuItem("sn5", "Veg Loaded Extra Cheese", "Veg sandwich loaded with extra cheese", 100.0, "", "Snacks", true),
            MenuItem("sn6", "Hot Dog", "Hot dog", 60.0, "", "Snacks", true),
            MenuItem("sn7", "Paneer Kulcha", "Paneer kulcha", 80.0, "", "Snacks", true),
            MenuItem("sn8", "French Fries", "French fries", 60.0, "", "Snacks", true),
            MenuItem("sn9", "Coconut Water", "Fresh coconut water", 50.0, "", "Snacks", true),
            MenuItem("sn10", "Strawberry", "Fresh strawberry", 50.0, "", "Snacks", true),
            MenuItem("sn11", "Sweet Corn", "Sweet corn", 50.0, "", "Snacks", true)
        )
    }

    private fun getBigMillionCafeMenuItems(): List<MenuItem> {
        return listOf(
            // SHAKES
            MenuItem("shake1", "Mango Shake", "Mango flavored shake", 60.0, "", "Shakes", true),
            MenuItem("shake2", "Banana Shake", "Banana flavored shake", 60.0, "", "Shakes", true),
            MenuItem("shake3", "Papaya Shake", "Papaya flavored shake", 60.0, "", "Shakes", true),
            MenuItem("shake4", "Vanila Shake", "Vanilla flavored shake", 60.0, "", "Shakes", true),
            MenuItem("shake5", "Pineapple Shake", "Pineapple flavored shake", 60.0, "", "Shakes", true),
            MenuItem("shake6", "Strawberry Shake", "Strawberry flavored shake", 60.0, "", "Shakes", true),
            MenuItem("shake7", "Butter Scotch Shake", "Butterscotch flavored shake", 60.0, "", "Shakes", true),
            MenuItem("shake8", "Kiwi Shake", "Kiwi flavored shake", 70.0, "", "Shakes", true),
            MenuItem("shake9", "Oreo Shake", "Oreo flavored shake", 80.0, "", "Shakes", true),
            MenuItem("shake10", "Kitkat Shake", "Kitkat flavored shake", 80.0, "", "Shakes", true),
            MenuItem("shake11", "Choco Brownie Shake", "Chocolate brownie flavored shake", 80.0, "", "Shakes", true),

            // MOJITO
            MenuItem("mojito1", "Virgin Mojito", "Classic virgin mojito", 60.0, "", "Mojito", true),
            MenuItem("mojito2", "Green Apple Mojito", "Green apple flavored mojito", 60.0, "", "Mojito", true),
            MenuItem("mojito3", "Blue Lagoon Mojito", "Blue lagoon flavored mojito", 60.0, "", "Mojito", true),

            // COLD COFFEE
            MenuItem("coffee1", "Cold Coffee", "Chilled cold coffee", 70.0, "", "Cold Coffee", true),

            // LASSI
            MenuItem("lassi1", "Sweet Lassi", "Sweet lassi", 50.0, "", "Lassi", true),
            MenuItem("lassi2", "Salted Lassi", "Salted lassi", 50.0, "", "Lassi", true),
            MenuItem("lassi3", "Mango Lassi", "Mango flavored lassi", 70.0, "", "Lassi", true),
            MenuItem("lassi4", "Strawberry Lassi", "Strawberry flavored lassi", 70.0, "", "Lassi", true),
            MenuItem("lassi5", "Rose Lassi", "Rose flavored lassi", 70.0, "", "Lassi", true),

            // SOFT DRINKS
            MenuItem("soft1", "Fresh Lime Soda", "Fresh lime soda", 50.0, "", "Soft Drinks", true),
            MenuItem("soft2", "Roohafza Milk", "Roohafza flavored milk", 60.0, "", "Soft Drinks", true),

            // BURGERS
            MenuItem("burger1", "Veg Burger", "Vegetarian burger", 60.0, "", "Burger", true),
            MenuItem("burger2", "Veg Cheese Burger", "Vegetarian burger with cheese", 70.0, "", "Burger", true),
            MenuItem("burger3", "Paneer Burger", "Paneer burger", 80.0, "", "Burger", true),

            // SANDWICHES
            MenuItem("sandwich1", "Veg Sandwich", "Vegetarian sandwich", 60.0, "", "Sandwich", true),
            MenuItem("sandwich2", "Veg Cheese Sandwich", "Vegetarian cheese sandwich", 70.0, "", "Sandwich", true),
            MenuItem("sandwich3", "Cheese Corn Sandwich", "Cheese and corn sandwich", 80.0, "", "Sandwich", true),
            MenuItem("sandwich4", "Paneer Tikka Sandwich", "Paneer tikka sandwich", 90.0, "", "Sandwich", true),

            // MAGGI
            MenuItem("maggi1", "Masala Maggi", "Masala flavored maggi", 40.0, "", "Maggi", true),
            MenuItem("maggi2", "Veg Maggi", "Vegetable maggi", 50.0, "", "Maggi", true),

            // PIZZA
            MenuItem("pizza1", "Sweet Corn Pizza", "Sweet corn pizza", 120.0, "", "Pizza", true),
            MenuItem("pizza2", "Onion Capsicum Pizza", "Onion and capsicum pizza", 120.0, "", "Pizza", true),
            MenuItem("pizza3", "Paneer Pizza", "Paneer pizza", 150.0, "", "Pizza", true),

            // NOODLES
            MenuItem("noodle1", "Veg Noodles", "Vegetarian noodles", 70.0, "", "Noodles", true),
            MenuItem("noodle2", "Hakka Noodles", "Hakka style noodles", 80.0, "", "Noodles", true),
            MenuItem("noodle3", "Chilly Garlic Noodles", "Chilly garlic flavored noodles", 90.0, "", "Noodles", true),

            // PASTA
            MenuItem("pasta1", "White Sauce Pasta", "Pasta in white sauce", 100.0, "", "Pasta", true),
            MenuItem("pasta2", "Red Sauce Pasta", "Pasta in red sauce", 100.0, "", "Pasta", true),

            // FRIES
            MenuItem("fries1", "Golden Fries", "Golden crispy fries", 60.0, "", "Fries", true),
            MenuItem("fries2", "Peri Peri Fries", "Peri peri flavored fries", 70.0, "", "Fries", true),

            // ROLL/WRAPS
            MenuItem("roll1", "Masala Roll", "Masala roll", 60.0, "", "Roll/Wraps", true),
            MenuItem("roll2", "Cheesy Roll", "Cheesy roll", 70.0, "", "Roll/Wraps", true),
            MenuItem("roll3", "Veg Wraps", "Vegetarian wraps", 70.0, "", "Roll/Wraps", true),
            MenuItem("roll4", "Spring Roll", "Vegetarian spring roll", 70.0, "", "Roll/Wraps", true),
            MenuItem("roll5", "Paneer Roll", "Paneer roll", 80.0, "", "Roll/Wraps", true),
            MenuItem("roll6", "Egg Roll", "Egg roll", 70.0, "", "Roll/Wraps", false),
            MenuItem("roll7", "Egg Chicken Roll", "Egg chicken roll", 100.0, "", "Roll/Wraps", false),

            // MOMOS
            MenuItem("momo1", "Veg Momos", "Vegetarian momos", 70.0, "", "Momos", true),
            MenuItem("momo2", "Fried Momos", "Fried vegetarian momos", 80.0, "", "Momos", true),
            MenuItem("momo3", "Manchurian", "Veg manchurian", 100.0, "", "Momos", true),
            MenuItem("momo4", "Cheese Chilli", "Cheese chilli", 100.0, "", "Momos", true),
            MenuItem("momo5", "Honey Chilli Potato", "Honey chilli potato", 100.0, "", "Momos", true),
            MenuItem("momo6", "Veg Bullets", "Veg bullets", 70.0, "", "Momos", true),

            // PARATHA
            MenuItem("paratha1", "Aloo Paratha", "Aloo paratha", 50.0, "", "Paratha", true),
            MenuItem("paratha2", "Mix Paratha", "Mix paratha", 60.0, "", "Paratha", true),
            MenuItem("paratha3", "Paneer Paratha", "Paneer paratha", 70.0, "", "Paratha", true),
            MenuItem("paratha4", "Gobhi Paratha", "Gobhi paratha", 60.0, "", "Paratha", true),

            // THALI / RICE
            MenuItem("thali1", "Veg Thali", "Veg thali (served with Amul butter and achar)", 80.0, "", "Thali/Rice", true),
            MenuItem("thali2", "Rajma Rice", "Rajma rice", 60.0, "", "Thali/Rice", true),
            MenuItem("thali3", "Chole Rice", "Chole rice", 60.0, "", "Thali/Rice", true),
            MenuItem("thali4", "Fried Rice (Half)", "Fried rice (half)", 60.0, "", "Thali/Rice", true),
            MenuItem("thali5", "Fried Rice (Full)", "Fried rice (full)", 80.0, "", "Thali/Rice", true),
            MenuItem("thali6", "Paneer Fried Rice (Half)", "Paneer fried rice (half)", 70.0, "", "Thali/Rice", true),
            MenuItem("thali7", "Paneer Fried Rice (Full)", "Paneer fried rice (full)", 100.0, "", "Thali/Rice", true),
            MenuItem("thali8", "Paneer Bhurji Rice (Half)", "Paneer bhurji rice (half)", 70.0, "", "Thali/Rice", true),
            MenuItem("thali9", "Paneer Bhurji Rice (Full)", "Paneer bhurji rice (full)", 100.0, "", "Thali/Rice", true),
            MenuItem("thali10", "Samosa", "Samosa", 15.0, "", "Thali/Rice", true),
            MenuItem("thali11", "Channa Samosa", "Channa samosa", 25.0, "", "Thali/Rice", true),
            MenuItem("thali12", "Veg Biryani", "Veg biryani", 70.0, "", "Thali/Rice", true),

            // NON VEG
            MenuItem("nonveg1", "Bread Omelette", "Bread omelette", 40.0, "", "Non Veg", false),
            MenuItem("nonveg2", "Chicken Burger", "Chicken burger", 70.0, "", "Non Veg", false),
            MenuItem("nonveg3", "Chicken Sandwich", "Chicken sandwich", 70.0, "", "Non Veg", false),
            MenuItem("nonveg4", "Chicken Roll", "Chicken roll", 70.0, "", "Non Veg", false),
            MenuItem("nonveg5", "Chicken Fried Rice (Half)", "Chicken fried rice (half)", 70.0, "", "Non Veg", false),
            MenuItem("nonveg6", "Chicken Fried Rice (Full)", "Chicken fried rice (full)", 100.0, "", "Non Veg", false),
            MenuItem("nonveg7", "Egg Fried Rice (Half)", "Egg fried rice (half)", 60.0, "", "Non Veg", false),
            MenuItem("nonveg8", "Egg Fried Rice (Full)", "Egg fried rice (full)", 80.0, "", "Non Veg", false),
            MenuItem("nonveg9", "Egg Roll", "Egg roll", 70.0, "", "Non Veg", false),

            // HOT BEVERAGES
            MenuItem("hot1", "Hot Tea", "Hot tea", 20.0, "", "Hot Beverages", true),
            MenuItem("hot2", "Kulhad Tea", "Kulhad tea", 30.0, "", "Hot Beverages", true),
            MenuItem("hot3", "Hot Coffee", "Hot coffee", 40.0, "", "Hot Beverages", true),
            MenuItem("hot4", "Hot Chocolate", "Hot chocolate", 70.0, "", "Hot Beverages", true)
        )
    }

    private fun getSinghBakeryMenuItems(): List<MenuItem> {
        return listOf(
            // SANDWICH
            MenuItem("sandwich1", "Paneer Kulcha", "Paneer kulcha sandwich", 60.0, "", "Sandwich", true),
            MenuItem("sandwich2", "Veg Grill S/W", "Vegetable grilled sandwich", 60.0, "", "Sandwich", true),
            MenuItem("sandwich3", "Veg Schezwan S/W", "Vegetable schezwan sandwich", 70.0, "", "Sandwich", true),
            MenuItem("sandwich4", "Veg Makhni Grilled S/W", "Vegetable makhni grilled sandwich", 70.0, "", "Sandwich", true),
            MenuItem("sandwich5", "Cheese Corn S/W", "Cheese corn sandwich", 80.0, "", "Sandwich", true),
            MenuItem("sandwich6", "Peri Peri Corn S/W", "Peri peri corn sandwich", 80.0, "", "Sandwich", true),
            MenuItem("sandwich7", "Paneer Chilly S/W", "Paneer chilly sandwich", 90.0, "", "Sandwich", true),
            MenuItem("sandwich8", "Paneer Schezwan S/W", "Paneer schezwan sandwich", 90.0, "", "Sandwich", true),
            MenuItem("sandwich9", "Chicken S/W", "Chicken sandwich", 90.0, "", "Sandwich", false),

            // PUFFS/PATTIES
            MenuItem("puff1", "Allu Patties", "Aloo patties", 30.0, "", "Puffs/Patties", true),
            MenuItem("puff2", "Cheese Patties", "Cheese patties", 35.0, "", "Puffs/Patties", true),
            MenuItem("puff3", "Corn Cheese Patties", "Corn cheese patties", 35.0, "", "Puffs/Patties", true),
            MenuItem("puff4", "Paneer Patties", "Paneer patties", 35.0, "", "Puffs/Patties", true),
            MenuItem("puff5", "Paneer Makhni Patties", "Paneer makhni patties", 40.0, "", "Puffs/Patties", true),
            MenuItem("puff6", "Chicken Patties", "Chicken patties", 40.0, "", "Puffs/Patties", false),

            // FRIES
            MenuItem("fries1", "French Fries", "French fries", 50.0, "", "Fries", true),
            MenuItem("fries2", "Masala French Fries", "Masala french fries", 60.0, "", "Fries", true),
            MenuItem("fries3", "Cheese Fries", "Cheese fries", 70.0, "", "Fries", true),
            MenuItem("fries4", "Peri Peri Fries", "Peri peri fries", 70.0, "", "Fries", true),
            MenuItem("fries5", "Chicken Nugget (6pcs)", "Chicken nuggets (6 pieces)", 80.0, "", "Fries", false),

            // GRILLED WRAPS/ROLLS
            MenuItem("roll1", "Allu Tikka Wrap", "Aloo tikka wrap", 60.0, "", "Grilled Wraps/Rolls", true),
            MenuItem("roll2", "Veg Roll", "Vegetable roll", 60.0, "", "Grilled Wraps/Rolls", true),
            MenuItem("roll3", "Paneer Spicy Wrap", "Paneer spicy wrap", 80.0, "", "Grilled Wraps/Rolls", true),
            MenuItem("roll4", "Chicken Spicy Wrap", "Chicken spicy wrap", 100.0, "", "Grilled Wraps/Rolls", false),

            // PASTA
            MenuItem("pasta1", "Red Sauce Pasta", "Red sauce pasta", 130.0, "", "Pasta", true),
            MenuItem("pasta2", "White Sauce Pasta", "White sauce pasta", 130.0, "", "Pasta", true),
            MenuItem("pasta3", "Makhni Sauce Pasta", "Makhni sauce pasta", 130.0, "", "Pasta", true),
            MenuItem("pasta4", "Mix Sauce Pasta", "Mix sauce pasta", 130.0, "", "Pasta", true),
            MenuItem("pasta5", "Tandoori Sauce Pasta", "Tandoori sauce pasta", 130.0, "", "Pasta", true),

            // CHINESE
            MenuItem("chinese1", "Veg Bullets", "Vegetable bullets", 60.0, "", "Chinese", true),
            MenuItem("chinese2", "Veg Spring Roll", "Vegetable spring roll", 60.0, "", "Chinese", true),
            MenuItem("chinese3", "Veg Noodles", "Vegetable noodles", 70.0, "", "Chinese", true),
            MenuItem("chinese4", "Hakka Noodles", "Hakka noodles", 80.0, "", "Chinese", true),
            MenuItem("chinese5", "Paneer Noodles", "Paneer noodles", 100.0, "", "Chinese", true),
            MenuItem("chinese6", "Egg Noodles", "Egg noodles", 100.0, "", "Chinese", false),
            MenuItem("chinese7", "Chicken Noodles", "Chicken noodles", 120.0, "", "Chinese", false),
            MenuItem("chinese8", "Veg Fried Rice", "Vegetable fried rice", 70.0, "", "Chinese", true),
            MenuItem("chinese9", "Egg Fried Rice", "Egg fried rice", 100.0, "", "Chinese", false),
            MenuItem("chinese10", "Paneer Fried Rice", "Paneer fried rice", 100.0, "", "Chinese", true),
            MenuItem("chinese11", "Chicken Fried Rice", "Chicken fried rice", 120.0, "", "Chinese", false),
            MenuItem("chinese12", "Schezwan Fried Rice", "Schezwan fried rice", 100.0, "", "Chinese", true),
            MenuItem("chinese13", "Crispy Chilli Potato", "Crispy chilli potato", 70.0, "", "Chinese", true),
            MenuItem("chinese14", "Crispy Honey Chilli Potato", "Crispy honey chilli potato", 80.0, "", "Chinese", true),

            // BURGER
            MenuItem("burger1", "Veg Burger", "Vegetable burger", 60.0, "", "Burger", true),
            MenuItem("burger2", "Cheese Grill Burger", "Cheese grilled burger", 70.0, "", "Burger", true),
            MenuItem("burger3", "Mexican Burger", "Mexican burger", 80.0, "", "Burger", true),
            MenuItem("burger4", "Chicken Grill Burger", "Chicken grilled burger", 90.0, "", "Burger", false),
            MenuItem("burger5", "Piri-Piri Burger", "Piri-piri burger", 80.0, "", "Burger", true),

            // LUNCH
            MenuItem("lunch1", "Rajma Rice", "Rajma rice", 50.0, "", "Lunch", true),
            MenuItem("lunch2", "Chana Rice", "Chana rice", 50.0, "", "Lunch", true),
            MenuItem("lunch3", "Paneer Rice", "Paneer rice", 80.0, "", "Lunch", true),
            MenuItem("lunch4", "Chane Samose", "Chane samose", 40.0, "", "Lunch", true),
            MenuItem("lunch5", "Chicken Fried Rice", "Chicken fried rice", 100.0, "", "Lunch", false),

            // SHAKE (WITH ICE-CREAM RS.10/- EXTRA)
            MenuItem("shake1", "Banana Shake", "Banana shake", 50.0, "", "Shake", true),
            MenuItem("shake2", "Cold Coffee", "Cold coffee", 60.0, "", "Shake", true),
            MenuItem("shake3", "Chocolate Shake", "Chocolate shake", 60.0, "", "Shake", true),
            MenuItem("shake4", "Mango Shake", "Mango shake", 60.0, "", "Shake", true),
            MenuItem("shake5", "Butterscotch Shake", "Butterscotch shake", 60.0, "", "Shake", true),
            MenuItem("shake6", "Kesar Pista Shake", "Kesar pista shake", 60.0, "", "Shake", true),
            MenuItem("shake7", "Strawberry Shake", "Strawberry shake", 60.0, "", "Shake", true),
            MenuItem("shake8", "Black Current Shake", "Black currant shake", 60.0, "", "Shake", true),
            MenuItem("shake9", "Vanilla Shake", "Vanilla shake", 60.0, "", "Shake", true),
            MenuItem("shake10", "Blue Berry Shake", "Blueberry shake", 70.0, "", "Shake", true),
            MenuItem("shake11", "Kitkat Shake", "Kitkat shake", 70.0, "", "Shake", true),
            MenuItem("shake12", "Oreo Shake", "Oreo shake", 80.0, "", "Shake", true),
            MenuItem("shake13", "Brownie Shake", "Brownie shake", 80.0, "", "Shake", true),

            // BEVERAGE
            MenuItem("bev1", "Tea", "Tea", 20.0, "", "Beverage", true),
            MenuItem("bev2", "Hot Coffee", "Hot coffee", 30.0, "", "Beverage", true),
            MenuItem("bev3", "Mint Mojito Soda", "Mint mojito soda", 40.0, "", "Beverage", true),
            MenuItem("bev4", "Green Apple Mojito", "Green apple mojito", 40.0, "", "Beverage", true),
            MenuItem("bev5", "Watermelon Mojito", "Watermelon mojito", 40.0, "", "Beverage", true),
            MenuItem("bev6", "Mango Mojito", "Mango mojito", 40.0, "", "Beverage", true),
            MenuItem("bev7", "Nimbu Pani", "Nimbu pani", 30.0, "", "Beverage", true),
            MenuItem("bev8", "Nimbu Soda", "Nimbu soda", 30.0, "", "Beverage", true),
            MenuItem("bev9", "Lassi Sweet/Salt", "Lassi sweet or salted", 50.0, "", "Beverage", true),

            // MINI MEALS
            MenuItem("mini1", "Bread Omelet", "Bread omelet", 50.0, "", "Mini Meals", true),
            MenuItem("mini2", "Samosa (2pc)", "Samosa (2 pieces)", 30.0, "", "Mini Meals", true),
            MenuItem("mini3", "Plain Maggi", "Plain Maggi noodles", 35.0, "", "Mini Meals", true),
            MenuItem("mini4", "Veg Maggi", "Vegetable Maggi noodles", 45.0, "", "Mini Meals", true),
            MenuItem("mini5", "Tandoori Maggi", "Tandoori Maggi noodles", 60.0, "", "Mini Meals", true),

            // SPECIALS (from yellow/red banner)
            MenuItem("spl1", "SPL Paharganj Chole Bhature", "Special Paharganj chole bhature", 60.0, "", "Specials", true),
            MenuItem("spl2", "Amritsar Nutri Kulcha", "Amritsar nutri kulcha", 60.0, "", "Specials", true)
        )
    }

    private fun getBunkerCoffeeMenuItems(): List<MenuItem> {
        return listOf(
            // KULCHA / BHATURA
            MenuItem("kulcha1", "Channa Bhatura", "Channa bhatura", 70.0, "", "Kulcha/Bhatura", true),
            MenuItem("kulcha2", "Channa Kulcha", "Channa kulcha", 70.0, "", "Kulcha/Bhatura", true),
            MenuItem("kulcha3", "Nutri Kulcha", "Nutri kulcha", 80.0, "", "Kulcha/Bhatura", true),
            MenuItem("kulcha4", "Soya Chaap Kulcha", "Soya chaap kulcha", 90.0, "", "Kulcha/Bhatura", true),

            // FRIED SNACKS / BULLETS
            MenuItem("snack1", "Veg Bullets", "Veg bullets", 70.0, "", "Fried Snacks", true),
            MenuItem("snack2", "Mexican Bullets", "Mexican bullets", 80.0, "", "Fried Snacks", true),
            MenuItem("snack3", "Peri Peri Bullets", "Peri peri bullets", 90.0, "", "Fried Snacks", true),
            MenuItem("snack4", "Cheese Peri Peri", "Cheese peri peri bullets", 100.0, "", "Fried Snacks", true),
            MenuItem("snack5", "Paneer Bullets", "Paneer bullets", 90.0, "", "Fried Snacks", true),
            MenuItem("snack6", "Paneer Peri Peri", "Paneer peri peri bullets", 100.0, "", "Fried Snacks", true),
            MenuItem("snack7", "Cheese Corn Bullets", "Cheese corn bullets", 100.0, "", "Fried Snacks", true),
            MenuItem("snack8", "Paneer Cauliflower Bullets", "Paneer cauliflower bullets", 100.0, "", "Fried Snacks", true),

            // BURGER
            MenuItem("burger1", "Veg Burger", "Veg burger", 50.0, "", "Burger", true),
            MenuItem("burger2", "Cheese Burger", "Cheese burger", 60.0, "", "Burger", true),
            MenuItem("burger3", "Paneer Burger", "Paneer burger", 70.0, "", "Burger", true),

            // CHINESE STARTERS / NOODLES
            MenuItem("chinese1", "Veg Noodles", "Veg noodles", 70.0, "", "Chinese", true),
            MenuItem("chinese2", "Hakka Noodles", "Hakka noodles", 80.0, "", "Chinese", true),
            MenuItem("chinese3", "Paneer Noodles", "Paneer noodles", 100.0, "", "Chinese", true),
            MenuItem("chinese4", "Egg Noodles", "Egg noodles", 100.0, "", "Chinese", false),
            MenuItem("chinese5", "Chicken Noodles", "Chicken noodles", 120.0, "", "Chinese", false),
            MenuItem("chinese6", "Masala Noodles", "Masala noodles", 80.0, "", "Chinese", true),
            MenuItem("chinese7", "Schezwan Noodles", "Schezwan noodles", 90.0, "", "Chinese", true),
            MenuItem("chinese8", "Veg Fried Rice", "Veg fried rice", 70.0, "", "Chinese", true),
            MenuItem("chinese9", "Egg Fried Rice", "Egg fried rice", 100.0, "", "Chinese", false),
            MenuItem("chinese10", "Paneer Fried Rice", "Paneer fried rice", 100.0, "", "Chinese", true),
            MenuItem("chinese11", "Chicken Fried Rice", "Chicken fried rice", 120.0, "", "Chinese", false),
            MenuItem("chinese12", "Veg Manchurian", "Veg manchurian", 80.0, "", "Chinese", true),
            MenuItem("chinese13", "Paneer Chilly", "Paneer chilly", 120.0, "", "Chinese", true),
            MenuItem("chinese14", "Mushroom Chilly", "Mushroom chilly", 120.0, "", "Chinese", true),
            MenuItem("chinese15", "Egg Chilly", "Egg chilly", 120.0, "", "Chinese", false),
            MenuItem("chinese16", "Chicken Chilly", "Chicken chilly", 140.0, "", "Chinese", false),
            MenuItem("chinese17", "Veg Manchurian Gravy", "Veg manchurian gravy", 100.0, "", "Chinese", true),
            MenuItem("chinese18", "Paneer Manchurian Gravy", "Paneer manchurian gravy", 120.0, "", "Chinese", true),
            MenuItem("chinese19", "Chicken Manchurian Gravy", "Chicken manchurian gravy", 140.0, "", "Chinese", false),

            // SANDWICH / PASTA
            MenuItem("sandwich1", "Veg Grill S/W", "Veg grilled sandwich", 60.0, "", "Sandwich/Pasta", true),
            MenuItem("sandwich2", "Paneer Tikka Grilled S/W", "Paneer tikka grilled sandwich", 80.0, "", "Sandwich/Pasta", true),
            MenuItem("sandwich3", "Cheese Corn Grilled S/W", "Cheese corn grilled sandwich", 90.0, "", "Sandwich/Pasta", true),
            MenuItem("sandwich4", "Paneer Tikka Sandwich", "Paneer tikka sandwich", 90.0, "", "Sandwich/Pasta", true),
            MenuItem("pasta1", "Red Sauce Pasta", "Red sauce pasta", 100.0, "", "Sandwich/Pasta", true),
            MenuItem("pasta2", "White Sauce Pasta", "White sauce pasta", 100.0, "", "Sandwich/Pasta", true),
            MenuItem("pasta3", "Mix Sauce Pasta", "Mix sauce pasta", 110.0, "", "Sandwich/Pasta", true),

            // ROLLS
            MenuItem("roll1", "Veg Roll", "Veg roll", 50.0, "", "Rolls", true),
            MenuItem("roll2", "Noodle Roll", "Noodle roll", 60.0, "", "Rolls", true),
            MenuItem("roll3", "Veg Paneer Roll", "Veg paneer roll", 70.0, "", "Rolls", true),

            // PARANTHA'S
            MenuItem("paratha1", "Aloo Parantha + Curd or Butter", "Aloo parantha with curd or butter", 50.0, "", "Parantha", true),
            MenuItem("paratha2", "Mix Parantha", "Mix parantha", 60.0, "", "Parantha", true),
            MenuItem("paratha3", "Paneer Parantha", "Paneer parantha", 60.0, "", "Parantha", true),
            MenuItem("paratha4", "Green Chilli Parantha", "Green chilli parantha", 70.0, "", "Parantha", true),
            MenuItem("paratha5", "Methi Parantha", "Methi parantha", 70.0, "", "Parantha", true),
            MenuItem("paratha6", "Onion Parantha", "Onion parantha", 70.0, "", "Parantha", true),

            // RICE / BIRYANI
            MenuItem("rice1", "Rajma Rice (Half)", "Rajma rice (half)", 50.0, "", "Rice/Biryani", true),
            MenuItem("rice2", "Rajma Rice (Full)", "Rajma rice (full)", 80.0, "", "Rice/Biryani", true),
            MenuItem("rice3", "Chana Rice (Half)", "Chana rice (half)", 50.0, "", "Rice/Biryani", true),
            MenuItem("rice4", "Chana Rice (Full)", "Chana rice (full)", 80.0, "", "Rice/Biryani", true),
            MenuItem("rice5", "Paneer Gravy Rice (Half)", "Paneer gravy rice (half)", 60.0, "", "Rice/Biryani", true),
            MenuItem("rice6", "Paneer Gravy Rice (Full)", "Paneer gravy rice (full)", 100.0, "", "Rice/Biryani", true),
            MenuItem("rice7", "Fried Rice", "Fried rice", 70.0, "", "Rice/Biryani", true),
            MenuItem("rice8", "Egg Fried Rice", "Egg fried rice", 100.0, "", "Rice/Biryani", true),
            MenuItem("rice9", "Soya Chaap Rice", "Soya chaap rice", 80.0, "", "Rice/Biryani", true),
            MenuItem("rice10", "Matar Paneer Rice", "Matar paneer rice", 60.0, "", "Rice/Biryani", true),
            MenuItem("rice11", "Veg Biryani", "Veg biryani", 120.0, "", "Rice/Biryani", true),
            MenuItem("rice12", "Cheese Biryani", "Cheese biryani", 120.0, "", "Rice/Biryani", true),

            // SAMOSA / PATTIES
            MenuItem("samosa1", "Aloo Samosa (Small)", "Aloo samosa small", 15.0, "", "Samosa/Patties", true),
            MenuItem("samosa2", "Aloo Samosa (Large)", "Aloo samosa large", 30.0, "", "Samosa/Patties", true),
            MenuItem("samosa3", "Chana Samosa", "Chana samosa", 40.0, "", "Samosa/Patties", true),
            MenuItem("samosa4", "Chana Samosa (Large)", "Chana samosa large", 60.0, "", "Samosa/Patties", true),
            MenuItem("samosa5", "Veg Patties", "Veg patties", 25.0, "", "Samosa/Patties", true),
            MenuItem("samosa6", "Cheese Patties", "Cheese patties", 30.0, "", "Samosa/Patties", true),
            MenuItem("samosa7", "Cheese Corn Patties", "Cheese corn patties", 35.0, "", "Samosa/Patties", true),

            // MOMOS
            MenuItem("momo1", "Momos", "Momos", 70.0, "", "Momos", true),
            MenuItem("momo2", "Veg Fried Momos", "Veg fried momos", 90.0, "", "Momos", true),
            MenuItem("momo3", "Tandoori Momos", "Tandoori momos", 90.0, "", "Momos", true),
            MenuItem("momo4", "Kurkur Momos", "Kurkur momos", 100.0, "", "Momos", true),
            MenuItem("momo5", "Creamy Momos", "Creamy momos", 110.0, "", "Momos", true),
            MenuItem("momo6", "Gravy Momos", "Gravy momos", 110.0, "", "Momos", true),

            // TEA / COFFEE
            MenuItem("tea1", "Tea", "Tea", 15.0, "", "Tea/Coffee", true),
            MenuItem("tea2", "Hot Butter Scotch", "Hot butter scotch", 35.0, "", "Tea/Coffee", true),
            MenuItem("tea3", "Coffee", "Coffee", 20.0, "", "Tea/Coffee", true),
            MenuItem("tea4", "Hot Chocolate Milk", "Hot chocolate milk", 35.0, "", "Tea/Coffee", true),
            MenuItem("tea5", "Black Coffee", "Black coffee", 30.0, "", "Tea/Coffee", true),
            MenuItem("tea6", "Hot Vanilla Coffee", "Hot vanilla coffee", 35.0, "", "Tea/Coffee", true),
            MenuItem("tea7", "Hot Hazelnut Coffee", "Hot hazelnut coffee", 35.0, "", "Tea/Coffee", true),

            // SHAKES
            MenuItem("shake1", "Banana Shake", "Banana shake", 50.0, "", "Shakes", true),
            MenuItem("shake2", "Mango Shake", "Mango shake", 50.0, "", "Shakes", true),
            MenuItem("shake3", "Strawberry Shake", "Strawberry shake", 50.0, "", "Shakes", true),
            MenuItem("shake4", "Vanilla Shake", "Vanilla shake", 50.0, "", "Shakes", true),
            MenuItem("shake5", "Butter Scotch Shake", "Butterscotch shake", 60.0, "", "Shakes", true),
            MenuItem("shake6", "Black Current Shake", "Black currant shake", 60.0, "", "Shakes", true),
            MenuItem("shake7", "Blue Berry Shake", "Blueberry shake", 60.0, "", "Shakes", true),
            MenuItem("shake8", "Kitkat Shake", "Kitkat shake", 70.0, "", "Shakes", true),
            MenuItem("shake9", "Oreo Shake", "Oreo shake", 70.0, "", "Shakes", true),
            MenuItem("shake10", "Nutella Shake", "Nutella shake", 90.0, "", "Shakes", true),
            MenuItem("shake11", "Chocolate Shake", "Chocolate shake", 60.0, "", "Shakes", true),

            // COLD COFFEE
            MenuItem("cold1", "Cold Coffee", "Cold coffee", 50.0, "", "Cold Coffee", true),

            // COMBO
            MenuItem("combo1", "Veg Burger + Coke", "Veg burger and coke combo", 100.0, "", "Combo", true),
            MenuItem("combo2", "Veg Grill S/W + Cold Coffee", "Veg grilled sandwich and cold coffee combo", 110.0, "", "Combo", true),
            MenuItem("combo3", "Veg Noodles + Lime Soda", "Veg noodles and lime soda combo", 100.0, "", "Combo", true),
            MenuItem("combo4", "Veg Manchurian + Noodles", "Veg manchurian and noodles combo", 150.0, "", "Combo", true),
            MenuItem("combo5", "Fried Rice + Manchurian", "Fried rice and manchurian combo", 150.0, "", "Combo", true),

            // BEVERAGES
            MenuItem("bev1", "Sweet Lassi", "Sweet lassi", 50.0, "", "Beverages", true),
            MenuItem("bev2", "Salty Lassi", "Salty lassi", 50.0, "", "Beverages", true),
            MenuItem("bev3", "Mango Lassi", "Mango lassi", 60.0, "", "Beverages", true),
            MenuItem("bev4", "Strawberry Lassi", "Strawberry lassi", 60.0, "", "Beverages", true),
            MenuItem("bev5", "Lime Soda", "Lime soda", 40.0, "", "Beverages", true),
            MenuItem("bev6", "Mojito", "Mojito", 70.0, "", "Beverages", true)
        )
    }

    private fun getMHCMenuItems(): List<MenuItem> {
        return listOf(
            // NOODLES
            MenuItem("noodle1", "Veg Noodles", "Vegetarian noodles", 80.0, "", "Noodles", true),
            MenuItem("noodle2", "Chilly Garlic Noodle", "Spicy garlic noodles", 80.0, "", "Noodles", true),
            MenuItem("noodle3", "Cheese Noodle", "Cheese noodles", 90.0, "", "Noodles", true),
            MenuItem("noodle4", "Egg Noodle", "Egg noodles", 90.0, "", "Noodles", false),
            MenuItem("noodle5", "Mushroom Noodle", "Mushroom noodles", 90.0, "", "Noodles", true),
            MenuItem("noodle6", "Butter Noodle", "Butter noodles", 90.0, "", "Noodles", true),
            MenuItem("noodle7", "Hakka Noodle", "Hakka noodles", 100.0, "", "Noodles", true),

            // RICE
            MenuItem("rice1", "Veg Fried Rice", "Vegetarian fried rice", 80.0, "", "Rice", true),
            MenuItem("rice2", "Cheese Fried Rice", "Cheese fried rice", 90.0, "", "Rice", true),
            MenuItem("rice3", "Egg Rice", "Egg rice", 90.0, "", "Rice", false),
            MenuItem("rice4", "Mushroom Rice", "Mushroom rice", 90.0, "", "Rice", true),

            // CHINESE SNACKS
            MenuItem("chinese1", "Veg Spring Roll", "Vegetarian spring roll", 60.0, "", "Chinese Snacks", true),
            MenuItem("chinese2", "Veg Maggi", "Vegetarian Maggi noodles", 50.0, "", "Chinese Snacks", true),
            MenuItem("chinese3", "Veg Manchurian", "Vegetarian manchurian", 70.0, "", "Chinese Snacks", true),
            MenuItem("chinese4", "Veg Momos", "Vegetarian momos", 60.0, "", "Chinese Snacks", true),
            MenuItem("chinese5", "Chilly Potato", "Chilly potato", 70.0, "", "Chinese Snacks", true),
            MenuItem("chinese6", "Honey Chilly Potato", "Honey chilly potato", 90.0, "", "Chinese Snacks", true),
            MenuItem("chinese7", "Honey Chilly Cauliflower", "Honey chilly cauliflower", 90.0, "", "Chinese Snacks", true),
            MenuItem("chinese8", "Kurkure Momos", "Crispy kurkure momos", 90.0, "", "Chinese Snacks", true),
            MenuItem("chinese9", "Chilly Momos", "Spicy chilly momos", 70.0, "", "Chinese Snacks", true),
            MenuItem("chinese10", "Pav Bhaji", "Pav bhaji", 100.0, "", "Chinese Snacks", true),
            MenuItem("chinese11", "Cheese Finger", "Cheese fingers", 100.0, "", "Chinese Snacks", true),
            MenuItem("chinese12", "Cheese Chilly", "Cheese chilly", 100.0, "", "Chinese Snacks", true),
            MenuItem("chinese13", "Mushroom Chilly", "Mushroom chilly", 100.0, "", "Chinese Snacks", true),
            MenuItem("chinese14", "Cheese Manchurian", "Cheese manchurian", 100.0, "", "Chinese Snacks", true),

            // HOT BEVERAGES
            MenuItem("hot1", "Tea", "Tea", 15.0, "", "Hot Beverages", true),
            MenuItem("hot2", "Espresso Coffee", "Espresso coffee", 25.0, "", "Hot Beverages", true),
            MenuItem("hot3", "Samosa", "Samosa", 15.0, "", "Hot Beverages", true),
            MenuItem("hot4", "Samosa Chat", "Samosa chaat", 50.0, "", "Hot Beverages", true),
            MenuItem("hot5", "Rajma Rice", "Rajma rice", 60.0, "", "Hot Beverages", true),
            MenuItem("hot6", "Chana Rice", "Chana rice", 40.0, "", "Hot Beverages", true),
            MenuItem("hot7", "Hot Badam Milk", "Hot badam milk", 40.0, "", "Hot Beverages", true),

            // DESSERT
            MenuItem("dessert1", "Bakery Biscuits", "Bakery biscuits", 50.0, "", "Dessert", true),
            MenuItem("dessert2", "Cake", "Cake", 300.0, "", "Dessert", true),
            MenuItem("dessert3", "Swiss Roll", "Swiss roll", 40.0, "", "Dessert", true),
            MenuItem("dessert4", "Pastry", "Pastry", 40.0, "", "Dessert", true),
            MenuItem("dessert5", "Pudding", "Pudding", 40.0, "", "Dessert", true),
            MenuItem("dessert6", "Brownie", "Brownie", 40.0, "", "Dessert", true),
            MenuItem("dessert7", "Brownie with Hot Chocolate", "Brownie with hot chocolate", 60.0, "", "Dessert", true),

            // LASSI
            MenuItem("lassi1", "Punjabi Sweets Lassi", "Punjabi sweets lassi", 50.0, "", "Lassi", true),
            MenuItem("lassi2", "Salty Lassi", "Salty lassi", 60.0, "", "Lassi", true),
            MenuItem("lassi3", "Mango Lassi", "Mango lassi", 60.0, "", "Lassi", true),

            // EXTRA HEALTHY
            MenuItem("extra1", "Brown Sandwich", "Brown bread sandwich", 80.0, "", "Extra Healthy", true),
            MenuItem("extra2", "Fruit Salad", "Fruit salad", 60.0, "", "Extra Healthy", true),
            MenuItem("extra3", "Pasta Salad", "Pasta salad", 100.0, "", "Extra Healthy", true),

            // KATHI ROLL
            MenuItem("roll1", "Veg Roll", "Vegetarian roll", 50.0, "", "Kathi Roll", true),
            MenuItem("roll2", "Paneer Roll", "Paneer roll", 70.0, "", "Kathi Roll", true),
            MenuItem("roll3", "Egg Roll", "Egg roll", 70.0, "", "Kathi Roll", false),
            MenuItem("roll4", "Soya Chaap Roll", "Soya chaap roll", 70.0, "", "Kathi Roll", true),
            MenuItem("roll5", "Chilli Paneer Roll", "Chilli paneer roll", 70.0, "", "Kathi Roll", true),
            MenuItem("roll6", "Pasta Roll", "Pasta roll", 70.0, "", "Kathi Roll", true),
            MenuItem("roll7", "Egg Noodle Roll", "Egg noodle roll", 70.0, "", "Kathi Roll", false),
            MenuItem("roll8", "Egg Paneer Roll", "Egg paneer roll", 70.0, "", "Kathi Roll", false),
            MenuItem("roll9", "Egg Chinese Roll", "Egg chinese roll", 90.0, "", "Kathi Roll", false),

            // CHINESE SNACKS COMBO
            MenuItem("combo1", "Noodles + Manchurian", "Noodles with manchurian", 100.0, "", "Chinese Snacks Combo", true),
            MenuItem("combo2", "Rice + Manchurian", "Rice with manchurian", 100.0, "", "Chinese Snacks Combo", true),
            MenuItem("combo3", "Noodles + Cheese Chilly", "Noodles with cheese chilly", 120.0, "", "Chinese Snacks Combo", true),
            MenuItem("combo4", "Rice + Cheese Chilly", "Rice with cheese chilly", 120.0, "", "Chinese Snacks Combo", true),

            // LOADED DRINKS
            MenuItem("drink1", "Oreo Chocolate", "Oreo chocolate drink", 60.0, "", "Loaded Drinks", true),
            MenuItem("drink2", "Kitkat Blast", "Kitkat blast drink", 60.0, "", "Loaded Drinks", true),
            MenuItem("drink3", "Snicker Blast", "Snicker blast drink", 60.0, "", "Loaded Drinks", true),
            MenuItem("drink4", "Badam Milk", "Badam milk", 60.0, "", "Loaded Drinks", true),
            MenuItem("drink5", "Badam Thandai", "Badam thandai", 60.0, "", "Loaded Drinks", true),
            MenuItem("drink6", "Caramel Chocolate", "Caramel chocolate drink", 60.0, "", "Loaded Drinks", true),
            MenuItem("drink7", "Mango Smoothies", "Mango smoothie", 60.0, "", "Loaded Drinks", true),
            MenuItem("drink8", "Strawberry Smoothies", "Strawberry smoothie", 60.0, "", "Loaded Drinks", true),

            // GRILLED PATTY & FRIED
            MenuItem("patty1", "Allo Patty", "Aloo patty", 20.0, "", "Grilled Patty & Fried", true),
            MenuItem("patty2", "Cheese Patty", "Cheese patty", 30.0, "", "Grilled Patty & Fried", true),
            MenuItem("patty3", "Paneer Corma Patty", "Paneer corma patty", 40.0, "", "Grilled Patty & Fried", true),
            MenuItem("patty4", "Cheese Corn Patty", "Cheese corn patty", 40.0, "", "Grilled Patty & Fried", true),
            MenuItem("patty5", "Pasta Patty", "Pasta patty", 60.0, "", "Grilled Patty & Fried", true),
            MenuItem("patty6", "Golden Fries", "Golden fries", 80.0, "", "Grilled Patty & Fried", true),
            MenuItem("patty7", "Masala Fries", "Masala fries", 100.0, "", "Grilled Patty & Fried", true),
            MenuItem("patty8", "Cheese Finger", "Cheese finger", 100.0, "", "Grilled Patty & Fried", true),

            // BURGER
            MenuItem("burger1", "Allo Tikki Burger", "Aloo tikki burger", 40.0, "", "Burger", true),
            MenuItem("burger2", "Veg Cheese Burger", "Veg cheese burger", 50.0, "", "Burger", true),
            MenuItem("burger3", "Spicy Paneer Burger", "Spicy paneer burger", 60.0, "", "Burger", true),
            MenuItem("burger4", "Mexican Burger", "Mexican burger", 70.0, "", "Burger", true),
            MenuItem("burger5", "MHC King Burger", "MHC king burger", 70.0, "", "Burger", true),

            // GRILLED SANDWICH
            MenuItem("sandwich1", "Veg Sandwich", "Vegetarian sandwich", 70.0, "", "Grilled Sandwich", true),
            MenuItem("sandwich2", "Cheese Bust Sandwich", "Cheese burst sandwich", 80.0, "", "Grilled Sandwich", true),
            MenuItem("sandwich3", "Butter Sandwich", "Butter sandwich", 70.0, "", "Grilled Sandwich", true),
            MenuItem("sandwich4", "Mushroom Corn Sandwich", "Mushroom corn sandwich", 80.0, "", "Grilled Sandwich", true),
            MenuItem("sandwich5", "Potato Tikka Sandwich", "Potato tikka sandwich", 80.0, "", "Grilled Sandwich", true),
            MenuItem("sandwich6", "Paneer Tikka Sandwich", "Paneer tikka sandwich", 80.0, "", "Grilled Sandwich", true),
            MenuItem("sandwich7", "Pasta Sandwich", "Pasta sandwich", 80.0, "", "Grilled Sandwich", true),
            MenuItem("sandwich8", "Cheese Corn Sandwich", "Cheese corn sandwich", 80.0, "", "Grilled Sandwich", true),

            // PASTA
            MenuItem("pasta1", "Tomato Pine Pasta", "Tomato pine pasta", 90.0, "", "Pasta", true),
            MenuItem("pasta2", "Creamy White Pasta", "Creamy white pasta", 100.0, "", "Pasta", true),
            MenuItem("pasta3", "Mushroom Corn Pasta", "Mushroom corn pasta", 110.0, "", "Pasta", true),
            MenuItem("pasta4", "Mix Pasta", "Mix pasta", 110.0, "", "Pasta", true),

            // MOJITO & DRINKS
            MenuItem("mojito1", "Kala Khatta", "Kala khatta mojito", 60.0, "", "Mojito", true),
            MenuItem("mojito2", "Mango Tango", "Mango tango mojito", 60.0, "", "Mojito", true),
            MenuItem("mojito3", "Green Apple", "Green apple mojito", 60.0, "", "Mojito", true),
            MenuItem("mojito4", "Icey Blue", "Icey blue mojito", 60.0, "", "Mojito", true),
            MenuItem("mojito5", "Peach Mojito", "Peach mojito", 60.0, "", "Mojito", true),
            MenuItem("mojito6", "Virgin Mojito", "Virgin mojito", 60.0, "", "Mojito", true),
            MenuItem("mojito7", "Cold Coffee", "Cold coffee", 60.0, "", "Mojito", true),
            MenuItem("mojito8", "Light Drink", "Light drink", 40.0, "", "Mojito", true),
            MenuItem("mojito9", "Lime Water", "Lime water", 40.0, "", "Mojito", true),
            MenuItem("mojito10", "Lime Soda", "Lime soda", 40.0, "", "Mojito", true),
            MenuItem("mojito11", "Water Melon", "Watermelon drink", 50.0, "", "Mojito", true),
            MenuItem("mojito12", "Jal Jeera", "Jal jeera", 50.0, "", "Mojito", true),
            MenuItem("mojito13", "Strawberry Soda", "Strawberry soda", 50.0, "", "Mojito", true),
            MenuItem("mojito14", "Blue Italian", "Blue Italian drink", 50.0, "", "Mojito", true),

            // FRUIT SHAKES
            MenuItem("shake1", "Banana Shake", "Banana shake", 60.0, "", "Fruit Shakes", true),
            MenuItem("shake2", "Papaya Shake", "Papaya shake", 60.0, "", "Fruit Shakes", true),
            MenuItem("shake3", "Mango Shake", "Mango shake", 60.0, "", "Fruit Shakes", true),
            MenuItem("shake4", "Chiku Shake", "Chiku shake", 60.0, "", "Fruit Shakes", true),
            MenuItem("shake5", "Vanilla Shake", "Vanilla shake", 60.0, "", "Fruit Shakes", true),
            MenuItem("shake6", "Chocolate Shake", "Chocolate shake", 60.0, "", "Fruit Shakes", true),
            MenuItem("shake7", "Butter Scotch Shake", "Butterscotch shake", 60.0, "", "Fruit Shakes", true),
            MenuItem("shake8", "Black Current Shake", "Black currant shake", 60.0, "", "Fruit Shakes", true)
        )
    }

    private fun getSamosaExpressMenuItems(): List<MenuItem> {
        return listOf(
            // VEG SAMOSA
            MenuItem("samosa1", "Classic Aloo Samosa", "Classic potato samosa", 21.0, "", "Veg Samosa", true),
            MenuItem("samosa2", "Paneer Samosa", "Paneer stuffed samosa", 25.0, "", "Veg Samosa", true),
            MenuItem("samosa3", "Makhni Samosa", "Makhni flavored samosa", 25.0, "", "Veg Samosa", true),
            MenuItem("samosa4", "Cheese Samosa", "Cheese stuffed samosa", 25.0, "", "Veg Samosa", true),
            MenuItem("samosa5", "Paneer Cheese Samosa", "Paneer and cheese samosa", 25.0, "", "Veg Samosa", true),
            MenuItem("samosa6", "Nac. Cheese Samosa", "Nacho cheese samosa", 25.0, "", "Veg Samosa", true),

            // MINI SAMOSA BUCKET
            MenuItem("minibucket1", "Mini Samosa Bucket (6 pcs)", "Mini samosa bucket (6 pieces)", 30.0, "", "Mini Samosa Bucket", true),
            MenuItem("minibucket2", "Mini Samosa Bucket (12 pcs)", "Mini samosa bucket (12 pieces)", 60.0, "", "Mini Samosa Bucket", true),
            MenuItem("minibucket3", "Mini Samosa Bucket (18 pcs)", "Mini samosa bucket (18 pieces)", 90.0, "", "Mini Samosa Bucket", true),

            // VEG SNACK IN
            MenuItem("vegsnack1", "Mini Veg Bites (6 pcs)", "Mini veg bites (6 pieces)", 30.0, "", "Veg Snack In", true),
            MenuItem("vegsnack2", "Mini Veg Bites (12 pcs)", "Mini veg bites (12 pieces)", 60.0, "", "Veg Snack In", true),
            MenuItem("vegsnack3", "Mini Veg Bites (18 pcs)", "Mini veg bites (18 pieces)", 90.0, "", "Veg Snack In", true),

            // FRIES N NACHOS
            MenuItem("fries1", "French Fries", "French fries", 60.0, "", "Fries N Nachos", true),
            MenuItem("fries2", "Masala Fries", "Masala french fries", 70.0, "", "Fries N Nachos", true),
            MenuItem("fries3", "Cheese Fries", "Cheese fries", 80.0, "", "Fries N Nachos", true),
            MenuItem("fries4", "Nachos", "Nachos", 80.0, "", "Fries N Nachos", true),

            // CLUB SANDWICH
            MenuItem("club1", "Veg Grilled Sandwich", "Vegetarian grilled sandwich", 80.0, "", "Club Sandwich", true),
            MenuItem("club2", "Veg Club Sandwich", "Vegetarian club sandwich", 100.0, "", "Club Sandwich", true),

            // BURGERS
            MenuItem("burger1", "Veg Burger", "Vegetarian burger", 60.0, "", "Burgers", true),
            MenuItem("burger2", "Veg Cheese Burger", "Vegetarian cheese burger", 70.0, "", "Burgers", true),
            MenuItem("burger3", "Paneer Burger", "Paneer burger", 80.0, "", "Burgers", true),

            // SAMOSA EXPRESS SECRET
            MenuItem("secret1", "Samosa Express Single Piece", "Special express samosa (single)", 40.0, "", "Samosa Express Secret", true),
            MenuItem("secret2", "Samosa Express Double Piece", "Special express samosa (double)", 80.0, "", "Samosa Express Secret", true),

            // WRAPS N ROLL
            MenuItem("wrap1", "Veg Roll", "Vegetarian roll", 70.0, "", "Wraps N Roll", true),
            MenuItem("wrap2", "Paneer Roll", "Paneer roll", 80.0, "", "Wraps N Roll", true),
            MenuItem("wrap3", "Paneer Cheese Roll", "Paneer cheese roll", 90.0, "", "Wraps N Roll", true),
            MenuItem("wrap4", "Makhni Roll", "Makhni roll", 90.0, "", "Wraps N Roll", true),

            // PASTA
            MenuItem("pasta1", "Red Sauce Pasta", "Pasta in red sauce", 100.0, "", "Pasta", true),
            MenuItem("pasta2", "White Sauce Pasta", "Pasta in white sauce", 110.0, "", "Pasta", true),
            MenuItem("pasta3", "Mix Sauce Pasta", "Pasta in mix sauce", 120.0, "", "Pasta", true),

            // CHAI LOVER
            MenuItem("chai1", "Adrak Chai", "Ginger tea", 20.0, "", "Chai Lover", true),
            MenuItem("chai2", "Elaichi Chai", "Cardamom tea", 22.0, "", "Chai Lover", true),
            MenuItem("chai3", "Tandoori Chai", "Tandoori tea", 25.0, "", "Chai Lover", true),
            MenuItem("chai4", "Kulhad", "Tea in kulhad", 33.0, "", "Chai Lover", true),

            // MOCKTAILS
            MenuItem("mocktail1", "Mint Mojito", "Mint mojito", 60.0, "", "Mocktails", true),
            MenuItem("mocktail2", "Blue Lagoon", "Blue lagoon mocktail", 60.0, "", "Mocktails", true),
            MenuItem("mocktail3", "Green Apple", "Green apple mocktail", 60.0, "", "Mocktails", true),
            MenuItem("mocktail4", "Watermelon", "Watermelon mocktail", 60.0, "", "Mocktails", true),

            // COFFENESS
            MenuItem("coffee1", "Espresso", "Espresso coffee", 40.0, "", "Coffeness", true),
            MenuItem("coffee2", "Americano", "Americano coffee", 50.0, "", "Coffeness", true),
            MenuItem("coffee3", "Cappuccino", "Cappuccino coffee", 60.0, "", "Coffeness", true),
            MenuItem("coffee4", "Cafe Latte", "Cafe latte", 60.0, "", "Coffeness", true),
            MenuItem("coffee5", "Cafe Mocha", "Cafe mocha", 70.0, "", "Coffeness", true),
            MenuItem("coffee6", "Hazelnut Latte", "Hazelnut latte", 70.0, "", "Coffeness", true),
            MenuItem("coffee7", "Irish Latte", "Irish latte", 70.0, "", "Coffeness", true),
            MenuItem("coffee8", "Caramel Latte", "Caramel latte", 70.0, "", "Coffeness", true),
            MenuItem("coffee9", "Hot Chocolate", "Hot chocolate", 70.0, "", "Coffeness", true),

            // MILKSHAKES
            MenuItem("shake1", "Pineapple Shake", "Pineapple milkshake", 70.0, "", "Milkshakes", true),
            MenuItem("shake2", "Mango Shake", "Mango milkshake", 70.0, "", "Milkshakes", true),
            MenuItem("shake3", "Banana Shake", "Banana milkshake", 70.0, "", "Milkshakes", true),
            MenuItem("shake4", "Strawberry Shake", "Strawberry milkshake", 70.0, "", "Milkshakes", true),
            MenuItem("shake5", "Butterscotch Shake", "Butterscotch milkshake", 70.0, "", "Milkshakes", true),
            MenuItem("shake6", "Oreo Shake", "Oreo milkshake", 80.0, "", "Milkshakes", true),

            // BURGER (Non-Veg)
            MenuItem("nvburger1", "Hot N Spicy Burger", "Hot and spicy chicken burger", 80.0, "", "Burger", false),
            MenuItem("nvburger2", "Chicken Seekh Burger", "Chicken seekh burger", 90.0, "", "Burger", false),

            // NON VEG EXPRESS
            MenuItem("nv1", "Chicken Seekh Samosa", "Chicken seekh samosa", 40.0, "", "Non Veg Express", false),
            MenuItem("nv2", "Chicken Keema Samosa", "Chicken keema samosa", 40.0, "", "Non Veg Express", false),
            MenuItem("nv3", "Chicken Cheese Samosa", "Chicken cheese samosa", 50.0, "", "Non Veg Express", false),
            MenuItem("nv4", "Chicken Mini Samosa (6 pcs)", "Chicken mini samosa (6 pieces)", 40.0, "", "Non Veg Express", false),
            MenuItem("nv5", "Chicken Mini Samosa (12 pcs)", "Chicken mini samosa (12 pieces)", 80.0, "", "Non Veg Express", false),
            MenuItem("nv6", "Chicken Mini Samosa (18 pcs)", "Chicken mini samosa (18 pieces)", 120.0, "", "Non Veg Express", false),

            // NON VEG SNACK IN
            MenuItem("nvSnack1", "Chicken Popcorn (6 pcs)", "Chicken popcorn (6 pieces)", 40.0, "", "Non Veg Snack In", false),
            MenuItem("nvSnack2", "Chicken Popcorn (12 pcs)", "Chicken popcorn (12 pieces)", 80.0, "", "Non Veg Snack In", false),
            MenuItem("nvSnack3", "Chicken Popcorn (18 pcs)", "Chicken popcorn (18 pieces)", 120.0, "", "Non Veg Snack In", false),

            // WRAP N ROLLS (Non-Veg)
            MenuItem("nvwrap1", "Egg Roll", "Egg roll", 70.0, "", "Wrap N Rolls", false),
            MenuItem("nvwrap2", "Egg Chicken Roll", "Egg chicken roll", 100.0, "", "Wrap N Rolls", false),
            MenuItem("nvwrap3", "Chicken Seekh Roll", "Chicken seekh roll", 100.0, "", "Wrap N Rolls", true),
            MenuItem("nvwrap4", "Chicken Malai Tikka Roll", "Chicken malai tikka roll", 130.0, "", "Wrap N Rolls", false),
            MenuItem("nvwrap5", "Chicken Tikka Wrap", "Chicken tikka wrap", 130.0, "", "Wrap N Rolls", false),

            // CLUB SANDWICH (Non-Veg)
            MenuItem("nvclub1", "Chicken Grilled Sandwich", "Chicken grilled sandwich", 100.0, "", "Club Sandwich", false),
            MenuItem("nvclub2", "Chicken Tikka Grilled Sandwich", "Chicken tikka grilled sandwich", 120.0, "", "Club Sandwich", false),

            // COMBOS (BOTTOM BANNER)
            MenuItem("combo1", "Veg Burger + Coke + Fries", "Veg burger with coke and fries", 110.0, "", "Combos", true),
            MenuItem("combo2", "Veg Wrap + Coke + Fries", "Veg wrap with coke and fries", 120.0, "", "Combos", true),
            MenuItem("combo3", "Veg Sandwich + Coke + Fries", "Veg sandwich with coke and fries", 120.0, "", "Combos", true),
            MenuItem("combo4", "Chicken Burger + Coke + Fries", "Chicken burger with coke and fries", 120.0, "", "Combos", false),
            MenuItem("combo5", "Chicken Wrap + Coke + Fries", "Chicken wrap with coke and fries", 150.0, "", "Combos", false),
            MenuItem("combo6", "Chicken Sandwich + Coke + Fries", "Chicken sandwich with coke and fries", 150.0, "", "Combos", false)
        )
    }

    private fun getTasteOfItalyMenuItems(): List<MenuItem> {
        return listOf(
            // BURGER COMBOS
            MenuItem("combo_burger_veg", "Veg Burger Combo", "Veg burger + fries + coke", 99.0, "", "Combo", true),
            MenuItem("combo_burger_nonveg", "Non Veg Burger Combo", "Non veg burger + fries + coke", 129.0, "", "Combo", false),

            // PIZZA COMBOS
            MenuItem("combo_pizza_veg", "Veg Pizza Combo", "1 pan pizza + fries + coke", 159.0, "", "Combo", true),
            MenuItem("combo_pizza_nonveg", "Chicken Pizza Combo", "Chicken pizza + fries + coke", 249.0, "", "Combo", false),

            // SANDWICH COMBOS
            MenuItem("combo_sandwich_veg", "Veg Sandwich Combo", "Farm villa sandwich + fries + coke", 139.0, "", "Combo", true),
            MenuItem("combo_sandwich_nonveg", "Non Veg Sandwich Combo", "Chicken sandwich + fries + coke", 199.0, "", "Combo", false),

            // PASTA COMBOS
            MenuItem("combo_pasta_veg", "Veg Pasta Combo", "Veg pasta + fries + coke", 249.0, "", "Combo", true),
            MenuItem("combo_pasta_nonveg", "Non Veg Pasta Combo", "Chicken pasta + nuggets + fries + coke", 299.0, "", "Combo", false),

            // SUB ITALIAN
            MenuItem("sub_veg", "Italian Veg Sub", "Italian veg sub", 109.0, "", "Sub Italian", true),
            MenuItem("sub_makhni", "Makhni Paneer Sub", "Makhni paneer sub", 139.0, "", "Sub Italian", true),
            MenuItem("sub_chicken", "Chicken Sub", "Chicken sub", 149.0, "", "Sub Italian", false),
            MenuItem("sub_mutton", "Mutton Sub", "Mutton sub", 199.0, "", "Sub Italian", false),

            // BURGER (VEG)
            MenuItem("burger_garden", "Garden Special", "Garden special veg burger", 59.0, "", "Burger", true),
            MenuItem("burger_cheese", "Cheese Passion", "Cheese passion veg burger", 69.0, "", "Burger", true),
            MenuItem("burger_chips", "Chips Italiano", "Chips Italiano veg burger", 79.0, "", "Burger", true),
            MenuItem("burger_peri", "Peri Peri Chilli", "Peri peri chilli veg burger", 89.0, "", "Burger", true),
            MenuItem("burger_paneer", "Paneer Mac Deluxe", "Paneer mac deluxe veg burger", 99.0, "", "Burger", true),
            MenuItem("burger_king", "Burger King", "Burger king veg burger", 120.0, "", "Burger", true),

            // BURGER (NON-VEG)
            MenuItem("burger_mexican", "Chicken Mexicana", "Chicken Mexicana burger", 89.0, "", "Burger", false),
            MenuItem("burger_african", "African Poco Lover", "African Poco Lover burger", 109.0, "", "Burger", false),
            MenuItem("burger_american", "American Chicken", "American Chicken burger", 149.0, "", "Burger", false),

            // WRAP
            MenuItem("wrap_veg", "Veg Thick", "Veg thick wrap", 80.0, "", "Wrap", true),
            MenuItem("wrap_paneer", "Paneer Thick", "Paneer thick wrap", 99.0, "", "Wrap", true),
            MenuItem("wrap_chicken", "Chicken", "Chicken wrap", 139.0, "", "Wrap", false),
            MenuItem("wrap_dominate", "Dominator", "Dominator non veg wrap", 139.0, "", "Wrap", false),

            // SNACKS (VEG)
            MenuItem("snack_fries_single", "French Fries (Single)", "French fries single", 70.0, "", "Snacks", true),
            MenuItem("snack_fries_double", "French Fries (Double)", "French fries double", 140.0, "", "Snacks", true),
            MenuItem("snack_masala_single", "Masala Fries (Single)", "Masala fries single", 80.0, "", "Snacks", true),
            MenuItem("snack_masala_double", "Masala Fries (Double)", "Masala fries double", 160.0, "", "Snacks", true),
            MenuItem("snack_peri_single", "Peri Peri Fries (Single)", "Peri peri fries single", 90.0, "", "Snacks", true),
            MenuItem("snack_peri_double", "Peri Peri Fries (Double)", "Peri peri fries double", 180.0, "", "Snacks", true),
            MenuItem("snack_cheesy_single", "Cheesy Fries (Single)", "Cheesy fries single", 99.0, "", "Snacks", true),
            MenuItem("snack_cheesy_double", "Cheesy Fries (Double)", "Cheesy fries double", 160.0, "", "Snacks", true),
            MenuItem("snack_nuggets_single", "Veg Nuggets (Single)", "Veg nuggets single", 99.0, "", "Snacks", true),
            MenuItem("snack_nuggets_double", "Veg Nuggets (Double)", "Veg nuggets double", 160.0, "", "Snacks", true),

            // SNACKS (NON-VEG)
            MenuItem("snack_cknuggets_single", "Chicken Nuggets (Single)", "Chicken nuggets single", 109.0, "", "Snacks", false),
            MenuItem("snack_cknuggets_double", "Chicken Nuggets (Double)", "Chicken nuggets double", 189.0, "", "Snacks", false),
            MenuItem("snack_seekh_single", "Chicken Seekh Kabab (Single)", "Chicken seekh kabab single", 139.0, "", "Snacks", false),
            MenuItem("snack_seekh_double", "Chicken Seekh Kabab (Double)", "Chicken seekh kabab double", 229.0, "", "Snacks", false),
            MenuItem("snack_popcorn_single", "Chicken Pop Corn (Single)", "Chicken popcorn single", 129.0, "", "Snacks", false),
            MenuItem("snack_popcorn_double", "Chicken Pop Corn (Double)", "Chicken popcorn double", 199.0, "", "Snacks", false),
            MenuItem("snack_mutton_single", "Mutton Seekh Kabab (Single)", "Mutton seekh kabab single", 129.0, "", "Snacks", false),
            MenuItem("snack_mutton_double", "Mutton Seekh Kabab (Double)", "Mutton seekh kabab double", 229.0, "", "Snacks", false),

            // QUESADILLAS
            MenuItem("quesa_paneer", "Paneer & Corn Quesadilla", "Paneer & corn quesadilla", 99.0, "", "Quesadillas", true),
            MenuItem("quesa_mushroom", "Mushroom & Olives Quesadilla", "Mushroom & olives quesadilla", 119.0, "", "Quesadillas", true),
            MenuItem("quesa_bbq", "BBQ Chicken Quesadilla", "BBQ chicken quesadilla", 129.0, "", "Quesadillas", false),
            MenuItem("quesa_tikka", "Chicken Tikka & Red Paprika Quesadilla", "Chicken tikka & red paprika quesadilla", 139.0, "", "Quesadillas", false),

            // SANDWICHES
            MenuItem("sandwich_farm", "Farm Villa (Grilled)", "Farm villa grilled sandwich", 79.0, "", "Sandwich", true),
            MenuItem("sandwich_corn", "Corn Sandwich (Grilled)", "Corn grilled sandwich", 99.0, "", "Sandwich", true),
            MenuItem("sandwich_paneer", "Paneer Deluxe (Grilled)", "Paneer deluxe grilled sandwich", 119.0, "", "Sandwich", true),
            MenuItem("sandwich_makhni", "Makhni Paneer (Grilled)", "Makhni paneer grilled sandwich", 119.0, "", "Sandwich", true),
            MenuItem("sandwich_ckn", "Chicken Sandwich (Grilled)", "Chicken grilled sandwich", 139.0, "", "Sandwich", false),
            MenuItem("sandwich_mutton", "Mutton Sandwich (Grilled)", "Mutton grilled sandwich", 179.0, "", "Sandwich", false),
            MenuItem("sandwich_meat", "Meat Blast (Grilled)", "Meat blast grilled sandwich", 199.0, "", "Sandwich", false),

            // PIZZA CLASSIC VEG
            MenuItem("pizza_margherita_s", "Margherita (Small)", "Classic veg pizza", 99.0, "", "Pizza Classic Veg", true),
            MenuItem("pizza_margherita_m", "Margherita (Medium)", "Classic veg pizza", 179.0, "", "Pizza Classic Veg", true),
            MenuItem("pizza_margherita_l", "Margherita (Large)", "Classic veg pizza", 349.0, "", "Pizza Classic Veg", true),
            MenuItem("pizza_margherita_xl", "Margherita (XL)", "Classic veg pizza", 649.0, "", "Pizza Classic Veg", true),

            MenuItem("pizza_cheese_s", "Cheese & Corn (Small)", "Cheese & corn pizza", 109.0, "", "Pizza Classic Veg", true),
            MenuItem("pizza_cheese_m", "Cheese & Corn (Medium)", "Cheese & corn pizza", 189.0, "", "Pizza Classic Veg", true),
            MenuItem("pizza_cheese_l", "Cheese & Corn (Large)", "Cheese & corn pizza", 379.0, "", "Pizza Classic Veg", true),
            MenuItem("pizza_cheese_xl", "Cheese & Corn (XL)", "Cheese & corn pizza", 699.0, "", "Pizza Classic Veg", true),

            MenuItem("pizza_farm_s", "Farm Fresh (Small)", "Farm fresh pizza", 109.0, "", "Pizza Classic Veg", true),
            MenuItem("pizza_farm_m", "Farm Fresh (Medium)", "Farm fresh pizza", 189.0, "", "Pizza Classic Veg", true),
            MenuItem("pizza_farm_l", "Farm Fresh (Large)", "Farm fresh pizza", 379.0, "", "Pizza Classic Veg", true),
            MenuItem("pizza_farm_xl", "Farm Fresh (XL)", "Farm fresh pizza", 699.0, "", "Pizza Classic Veg", true),

            // PIZZA SIMPLY VEG
            MenuItem("pizza_tikka_s", "Paneer Tikka (Small)", "Paneer tikka pizza", 119.0, "", "Pizza Simply Veg", true),
            MenuItem("pizza_tikka_m", "Paneer Tikka (Medium)", "Paneer tikka pizza", 199.0, "", "Pizza Simply Veg", true),
            MenuItem("pizza_tikka_l", "Paneer Tikka (Large)", "Paneer tikka pizza", 399.0, "", "Pizza Simply Veg", true),
            MenuItem("pizza_tikka_xl", "Paneer Tikka (XL)", "Paneer tikka pizza", 749.0, "", "Pizza Simply Veg", true),

            MenuItem("pizza_peri_s", "Peri-Peri Chilli (Small)", "Peri-peri chilli pizza", 119.0, "", "Pizza Simply Veg", true),
            MenuItem("pizza_peri_m", "Peri-Peri Chilli (Medium)", "Peri-peri chilli pizza", 199.0, "", "Pizza Simply Veg", true),
            MenuItem("pizza_peri_l", "Peri-Peri Chilli (Large)", "Peri-peri chilli pizza", 399.0, "", "Pizza Simply Veg", true),
            MenuItem("pizza_peri_xl", "Peri-Peri Chilli (XL)", "Peri-peri chilli pizza", 749.0, "", "Pizza Simply Veg", true),

            MenuItem("pizza_mushroom_s", "Mushroom Deluxe (Small)", "Mushroom deluxe pizza", 119.0, "", "Pizza Simply Veg", true),
            MenuItem("pizza_mushroom_m", "Mushroom Deluxe (Medium)", "Mushroom deluxe pizza", 199.0, "", "Pizza Simply Veg", true),
            MenuItem("pizza_mushroom_l", "Mushroom Deluxe (Large)", "Mushroom deluxe pizza", 399.0, "", "Pizza Simply Veg", true),
            MenuItem("pizza_mushroom_xl", "Mushroom Deluxe (XL)", "Mushroom deluxe pizza", 749.0, "", "Pizza Simply Veg", true),

            MenuItem("pizza_doublecheese_s", "Double Cheese Magherita (Small)", "Double cheese magherita pizza", 143.0, "", "Pizza Simply Veg", true),
            MenuItem("pizza_doublecheese_m", "Double Cheese Magherita (Medium)", "Double cheese magherita pizza", 249.0, "", "Pizza Simply Veg", true),
            MenuItem("pizza_doublecheese_l", "Double Cheese Magherita (Large)", "Double cheese magherita pizza", 499.0, "", "Pizza Simply Veg", true),
            MenuItem("pizza_doublecheese_xl", "Double Cheese Magherita (XL)", "Double cheese magherita pizza", 849.0, "", "Pizza Simply Veg", true),

            // PIZZA PREMIUM VEG
            MenuItem("pizza_peppe_s", "Peppe Paneer (Small)", "Peppe paneer pizza", 149.0, "", "Pizza Premium Veg", true),
            MenuItem("pizza_peppe_m", "Peppe Paneer (Medium)", "Peppe paneer pizza", 249.0, "", "Pizza Premium Veg", true),
            MenuItem("pizza_peppe_l", "Peppe Paneer (Large)", "Peppe paneer pizza", 499.0, "", "Pizza Premium Veg", true),
            MenuItem("pizza_peppe_xl", "Peppe Paneer (XL)", "Peppe paneer pizza", 849.0, "", "Pizza Premium Veg", true),

            MenuItem("pizza_lovebite_s", "Love Bite (Small)", "Love bite pizza", 149.0, "", "Pizza Premium Veg", true),
            MenuItem("pizza_lovebite_m", "Love Bite (Medium)", "Love bite pizza", 249.0, "", "Pizza Premium Veg", true),
            MenuItem("pizza_lovebite_l", "Love Bite (Large)", "Love bite pizza", 499.0, "", "Pizza Premium Veg", true),
            MenuItem("pizza_lovebite_xl", "Love Bite (XL)", "Love bite pizza", 849.0, "", "Pizza Premium Veg", true),

            MenuItem("pizza_hawaiin_s", "Veg Hawaiin (Small)", "Veg Hawaiin pizza", 149.0, "", "Pizza Premium Veg", true),
            MenuItem("pizza_hawaiin_m", "Veg Hawaiin (Medium)", "Veg Hawaiin pizza", 249.0, "", "Pizza Premium Veg", true),
            MenuItem("pizza_hawaiin_l", "Veg Hawaiin (Large)", "Veg Hawaiin pizza", 499.0, "", "Pizza Premium Veg", true),
            MenuItem("pizza_hawaiin_xl", "Veg Hawaiin (XL)", "Veg Hawaiin pizza", 849.0, "", "Pizza Premium Veg", true),

            MenuItem("pizza_lasvegas_s", "Las Vegas Retreat (Small)", "Las Vegas Retreat pizza", 149.0, "", "Pizza Premium Veg", true),
            MenuItem("pizza_lasvegas_m", "Las Vegas Retreat (Medium)", "Las Vegas Retreat pizza", 249.0, "", "Pizza Premium Veg", true),
            MenuItem("pizza_lasvegas_l", "Las Vegas Retreat (Large)", "Las Vegas Retreat pizza", 499.0, "", "Pizza Premium Veg", true),
            MenuItem("pizza_lasvegas_xl", "Las Vegas Retreat (XL)", "Las Vegas Retreat pizza", 849.0, "", "Pizza Premium Veg", true),

            MenuItem("pizza_supreme_s", "Supreme Smokie (Small)", "Supreme Smokie pizza", 149.0, "", "Pizza Premium Veg", true),
            MenuItem("pizza_supreme_m", "Supreme Smokie (Medium)", "Supreme Smokie pizza", 249.0, "", "Pizza Premium Veg", true),
            MenuItem("pizza_supreme_l", "Supreme Smokie (Large)", "Supreme Smokie pizza", 499.0, "", "Pizza Premium Veg", true),
            MenuItem("pizza_supreme_xl", "Supreme Smokie (XL)", "Supreme Smokie pizza", 849.0, "", "Pizza Premium Veg", true),

            // PIZZA SPECIALITY VEG
            MenuItem("pizza_burn_s", "Burn To Hell (Small)", "Burn To Hell pizza", 179.0, "", "Pizza Speciality Veg", true),
            MenuItem("pizza_burn_m", "Burn To Hell (Medium)", "Burn To Hell pizza", 349.0, "", "Pizza Speciality Veg", true),
            MenuItem("pizza_burn_l", "Burn To Hell (Large)", "Burn To Hell pizza", 649.0, "", "Pizza Speciality Veg", true),
            MenuItem("pizza_burn_xl", "Burn To Hell (XL)", "Burn To Hell pizza", 1199.0, "", "Pizza Speciality Veg", true),

            MenuItem("pizza_pasta_s", "Pizza Pasta (Small)", "Pizza pasta pizza", 179.0, "", "Pizza Speciality Veg", true),
            MenuItem("pizza_pasta_m", "Pizza Pasta (Medium)", "Pizza pasta pizza", 349.0, "", "Pizza Speciality Veg", true),
            MenuItem("pizza_pasta_l", "Pizza Pasta (Large)", "Pizza pasta pizza", 649.0, "", "Pizza Speciality Veg", true),
            MenuItem("pizza_pasta_xl", "Pizza Pasta (XL)", "Pizza pasta pizza", 1199.0, "", "Pizza Speciality Veg", true),

            MenuItem("pizza_cheesy7_s", "Cheesy-7 (Small)", "Cheesy-7 pizza", 199.0, "", "Pizza Speciality Veg", true),
            MenuItem("pizza_cheesy7_m", "Cheesy-7 (Medium)", "Cheesy-7 pizza", 379.0, "", "Pizza Speciality Veg", true),
            MenuItem("pizza_cheesy7_l", "Cheesy-7 (Large)", "Cheesy-7 pizza", 699.0, "", "Pizza Speciality Veg", true),
            MenuItem("pizza_cheesy7_xl", "Cheesy-7 (XL)", "Cheesy-7 pizza", 1199.0, "", "Pizza Speciality Veg", true),

            MenuItem("pizza_tasteofitaly_s", "Spl. Taste Of Italy (Small)", "Special Taste Of Italy pizza", 199.0, "", "Pizza Speciality Veg", true),
            MenuItem("pizza_tasteofitaly_xl", "Spl. Taste Of Italy (XL)", "Special Taste Of Italy pizza", 1199.0, "", "Pizza Speciality Veg", true),

            // PIZZA CLASSIC NON-VEG
            MenuItem("pizza_ckndelight_s", "Chicken De-light (Small)", "Chicken De-light pizza", 149.0, "", "Pizza Classic Non-Veg", false),
            MenuItem("pizza_ckndelight_m", "Chicken De-light (Medium)", "Chicken De-light pizza", 279.0, "", "Pizza Classic Non-Veg", false),
            MenuItem("pizza_ckndelight_l", "Chicken De-light (Large)", "Chicken De-light pizza", 399.0, "", "Pizza Classic Non-Veg", false),
            MenuItem("pizza_ckndelight_xl", "Chicken De-light (XL)", "Chicken De-light pizza", 699.0, "", "Pizza Classic Non-Veg", false),

            MenuItem("pizza_poco_s", "Poco Chicken (Small)", "Poco Chicken pizza", 149.0, "", "Pizza Classic Non-Veg", false),
            MenuItem("pizza_poco_m", "Poco Chicken (Medium)", "Poco Chicken pizza", 279.0, "", "Pizza Classic Non-Veg", false),
            MenuItem("pizza_poco_l", "Poco Chicken (Large)", "Poco Chicken pizza", 399.0, "", "Pizza Classic Non-Veg", false),
            MenuItem("pizza_poco_xl", "Poco Chicken (XL)", "Poco Chicken pizza", 699.0, "", "Pizza Classic Non-Veg", false),

            MenuItem("pizza_keema_s", "Chicken Keema (Small)", "Chicken Keema pizza", 149.0, "", "Pizza Classic Non-Veg", false),
            MenuItem("pizza_keema_m", "Chicken Keema (Medium)", "Chicken Keema pizza", 279.0, "", "Pizza Classic Non-Veg", false),
            MenuItem("pizza_keema_l", "Chicken Keema (Large)", "Chicken Keema pizza", 399.0, "", "Pizza Classic Non-Veg", false),
            MenuItem("pizza_keema_xl", "Chicken Keema (XL)", "Chicken Keema pizza", 699.0, "", "Pizza Classic Non-Veg", false),

            // PIZZA SIMPLY NON-VEG
            MenuItem("pizza_mutton_s", "Mutton Twist (Small)", "Mutton twist pizza", 179.0, "", "Pizza Simply Non-Veg", false),
            MenuItem("pizza_mutton_m", "Mutton Twist (Medium)", "Mutton twist pizza", 299.0, "", "Pizza Simply Non-Veg", false),
            MenuItem("pizza_mutton_l", "Mutton Twist (Large)", "Mutton twist pizza", 549.0, "", "Pizza Simply Non-Veg", false),
            MenuItem("pizza_mutton_xl", "Mutton Twist (XL)", "Mutton twist pizza", 999.0, "", "Pizza Simply Non-Veg", false),

            MenuItem("pizza_perichicken_s", "Peri-Peri Chicken (Small)", "Peri-peri chicken pizza", 179.0, "", "Pizza Simply Non-Veg", false),
            MenuItem("pizza_perichicken_m", "Peri-Peri Chicken (Medium)", "Peri-peri chicken pizza", 299.0, "", "Pizza Simply Non-Veg", false),
            MenuItem("pizza_perichicken_l", "Peri-Peri Chicken (Large)", "Peri-peri chicken pizza", 549.0, "", "Pizza Simply Non-Veg", false),
            MenuItem("pizza_perichicken_xl", "Peri-Peri Chicken (XL)", "Peri-peri chicken pizza", 999.0, "", "Pizza Simply Non-Veg", false),

            MenuItem("pizza_indianchicken_s", "Indian Chicken (Small)", "Indian chicken pizza", 179.0, "", "Pizza Simply Non-Veg", false),
            MenuItem("pizza_indianchicken_m", "Indian Chicken (Medium)", "Indian chicken pizza", 299.0, "", "Pizza Simply Non-Veg", false),
            MenuItem("pizza_indianchicken_l", "Indian Chicken (Large)", "Indian chicken pizza", 549.0, "", "Pizza Simply Non-Veg", false),
            MenuItem("pizza_indianchicken_xl", "Indian Chicken (XL)", "Indian chicken pizza", 999.0, "", "Pizza Simply Non-Veg", false),

            // PIZZA INTERNATIONAL NON-VEG
            MenuItem("pizza_afghani_s", "Afghani Chicken (Small)", "Afghani chicken pizza", 149.0, "", "Pizza International Non-Veg", false),
            MenuItem("pizza_afghani_m", "Afghani Chicken (Medium)", "Afghani chicken pizza", 279.0, "", "Pizza International Non-Veg", false),
            MenuItem("pizza_afghani_l", "Afghani chicken (Large)", "Afghani chicken pizza", 399.0, "", "Pizza International Non-Veg", false),
            MenuItem("pizza_afghani_xl", "Afghani chicken (XL)", "Afghani chicken pizza", 699.0, "", "Pizza International Non-Veg", false),

            MenuItem("pizza_bhutan_s", "Bhutanese Ama Datsi (Small)", "Bhutanese Ama Datsi pizza", 149.0, "", "Pizza International Non-Veg", false),
            MenuItem("pizza_bhutan_m", "Bhutanese Ama Datsi (Medium)", "Bhutanese Ama Datsi pizza", 279.0, "", "Pizza International Non-Veg", false),
            MenuItem("pizza_bhutan_l", "Bhutanese Ama Datsi (Large)", "Bhutanese Ama Datsi pizza", 399.0, "", "Pizza International Non-Veg", false),
            MenuItem("pizza_bhutan_xl", "Bhutanese Ama Datsi (XL)", "Bhutanese Ama Datsi pizza", 699.0, "", "Pizza International Non-Veg", false),

            MenuItem("pizza_jamaica_s", "Jamaica Chicken (Small)", "Jamaica chicken pizza", 149.0, "", "Pizza International Non-Veg", false),
            MenuItem("pizza_jamaica_m", "Jamaica Chicken (Medium)", "Jamaica chicken pizza", 279.0, "", "Pizza International Non-Veg", false),
            MenuItem("pizza_jamaica_l", "Jamaica chicken (Large)", "Jamaica chicken pizza", 399.0, "", "Pizza International Non-Veg", false),
            MenuItem("pizza_jamaica_xl", "Jamaica chicken (XL)", "Jamaica chicken pizza", 699.0, "", "Pizza International Non-Veg", false),

            MenuItem("pizza_afrperi_s", "African Peri Peri (Small)", "African peri peri pizza", 169.0, "", "Pizza International Non-Veg", false),
            MenuItem("pizza_afrperi_m", "African Peri Peri (Medium)", "African peri peri pizza", 299.0, "", "Pizza International Non-Veg", false),
            MenuItem("pizza_afrperi_l", "African peri peri (Large)", "African peri peri pizza", 569.0, "", "Pizza International Non-Veg", false),
            MenuItem("pizza_afrperi_xl", "African peri peri (XL)", "African peri peri pizza", 999.0, "", "Pizza International Non-Veg", false),

            MenuItem("pizza_spanish_s", "Spanish Salami (Small)", "Spanish salami pizza", 169.0, "", "Pizza International Non-Veg", false),
            MenuItem("pizza_spanish_m", "Spanish Salami (Medium)", "Spanish salami pizza", 299.0, "", "Pizza International Non-Veg", false),
            MenuItem("pizza_spanish_l", "Spanish salami (Large)", "Spanish salami pizza", 569.0, "", "Pizza International Non-Veg", false),
            MenuItem("pizza_spanish_xl", "Spanish salami (XL)", "Spanish salami pizza", 999.0, "", "Pizza International Non-Veg", false),

            // PIZZA SPECIALITY NON-VEG
            MenuItem("pizza_fire_s", "Fire-C-Chicken (Small)", "Fire-C-Chicken pizza", 249.0, "", "Pizza Speciality Non-Veg", false),
            MenuItem("pizza_fire_m", "Fire-C-Chicken (Medium)", "Fire-C-Chicken pizza", 399.0, "", "Pizza Speciality Non-Veg", false),
            MenuItem("pizza_fire_l", "Fire-C-Chicken (Large)", "Fire-C-Chicken pizza", 699.0, "", "Pizza Speciality Non-Veg", false),
            MenuItem("pizza_fire_xl", "Fire-C-Chicken (XL)", "Fire-C-Chicken pizza", 1299.0, "", "Pizza Speciality Non-Veg", false),

            MenuItem("pizza_heaven_s", "Heaven of Chicken (Small)", "Heaven of Chicken pizza", 249.0, "", "Pizza Speciality Non-Veg", false),
            MenuItem("pizza_heaven_m", "Heaven of Chicken (Medium)", "Heaven of Chicken pizza", 399.0, "", "Pizza Speciality Non-Veg", false),
            MenuItem("pizza_heaven_l", "Heaven of Chicken (Large)", "Heaven of Chicken pizza", 699.0, "", "Pizza Speciality Non-Veg", false),
            MenuItem("pizza_heaven_xl", "Heaven of Chicken (XL)", "Heaven of Chicken pizza", 1299.0, "", "Pizza Speciality Non-Veg", false),

            // PIZZA MANIA
            MenuItem("mania_onion", "Onion", "Onion pizza mania", 89.0, "", "Pizza Mania", true),
            MenuItem("mania_capsicum", "Capsicum", "Capsicum pizza mania", 99.0, "", "Pizza Mania", true),
            MenuItem("mania_tomato", "Tomato", "Tomato pizza mania", 99.0, "", "Pizza Mania", true),
            MenuItem("mania_golden", "Golden Corn", "Golden corn pizza mania", 109.0, "", "Pizza Mania", true),
            MenuItem("mania_onioncapsicum", "Onion & Capsicum", "Onion & capsicum pizza mania", 99.0, "", "Pizza Mania", true),
            MenuItem("mania_onionpaneer", "Onion & Paneer", "Onion & paneer pizza mania", 109.0, "", "Pizza Mania", true),
            MenuItem("mania_capsicumpaneer", "Capsicum & Paneer", "Capsicum & paneer pizza mania", 109.0, "", "Pizza Mania", true),
            MenuItem("mania_paneercornjalapino", "Paneer Corn & Jalapino", "Paneer corn & jalapino pizza mania", 119.0, "", "Pizza Mania", true),
            // Non-Veg Mania
            MenuItem("mania_chickentikka", "Chicken Tikka", "Chicken tikka pizza mania", 129.0, "", "Pizza Mania", false),
            MenuItem("mania_bbqchicken", "BBQ Chicken", "BBQ chicken pizza mania", 129.0, "", "Pizza Mania", false),
            MenuItem("mania_sausage", "Sausage Chicken", "Sausage chicken pizza mania", 139.0, "", "Pizza Mania", false),
            MenuItem("mania_chickenseekh", "Chicken SEEKH", "Chicken seekh pizza mania", 139.0, "", "Pizza Mania", false),

            // GARLIC BREAD (VEG)
            MenuItem("gbread_plain", "Plain Cheesy", "Plain cheesy garlic bread", 99.0, "", "Garlic Bread", true),
            MenuItem("gbread_stuffed", "Stuffed", "Stuffed garlic bread", 139.0, "", "Garlic Bread", true),
            MenuItem("gbread_paneer", "Paneer Tikka Stuffed", "Paneer tikka stuffed garlic bread", 179.0, "", "Garlic Bread", true),

            // GARLIC BREAD (NON-VEG)
            MenuItem("gbread_chicken", "Chicken Stuffed Garlic Bread", "Chicken stuffed garlic bread", 179.0, "", "Garlic Bread", false),
            MenuItem("gbread_mutton", "Mutton Stuffed Garlic Bread", "Mutton stuffed garlic bread", 209.0, "", "Garlic Bread", false),

            // PASTA ITALIANO
            MenuItem("pasta_white", "White Sauce Pasta", "White sauce pasta", 149.0, "", "Pasta Italiano", true),
            MenuItem("pasta_red", "Red Sauce Pasta", "Red sauce pasta", 149.0, "", "Pasta Italiano", true),
            MenuItem("pasta_italiano", "Italiano Pasta", "Italiano pasta", 149.0, "", "Pasta Italiano", true),
            MenuItem("pasta_nonveg", "Non-Veg Pasta", "Non-veg pasta", 199.0, "", "Pasta Italiano", false),

            // TACOS
            MenuItem("taco_mexican", "Taco Mexican", "Taco Mexican veg", 149.0, "", "Taco", true),
            MenuItem("taco_paneer", "Paneer Butter Masala Taco", "Paneer butter masala taco", 149.0, "", "Taco", true),
            MenuItem("taco_african", "African Chicken Taco", "African chicken taco", 189.0, "", "Taco", false),
            MenuItem("taco_chicken", "Chicken Blast Taco", "Chicken blast taco", 189.0, "", "Taco", false),

            // DESSERTS
            MenuItem("dessert_brownie", "Hot Brownie with Chocolate", "Hot brownie with chocolate", 79.0, "", "Dessert", true),
            MenuItem("dessert_icecream", "Cup Ice Cream", "Cup ice cream", 59.0, "", "Dessert", true),
            MenuItem("dessert_brownieice", "Hot Brownie with Ice Cream", "Hot brownie with ice cream", 89.0, "", "Dessert", true),
            MenuItem("dessert_choco", "Choco Lava", "Choco lava", 89.0, "", "Dessert", true),

            // SHAKES
            MenuItem("shake_coldcoffee", "Cold Coffee", "Cold coffee shake", 59.0, "", "Shake", true),
            MenuItem("shake_blackcurrant", "Black Currant", "Black currant shake", 79.0, "", "Shake", true),
            MenuItem("shake_butterscotch", "Butterscotch", "Butterscotch shake", 79.0, "", "Shake", true),
            MenuItem("shake_strawberry", "Strawberry", "Strawberry shake", 79.0, "", "Shake", true),
            MenuItem("shake_oreo", "Oreo Shake", "Oreo shake", 79.0, "", "Shake", true),
            MenuItem("shake_icecoke", "Ice Coke", "Ice coke", 59.0, "", "Shake", true),

            // HOT DRINKS
            MenuItem("hot_hotcoffee", "Hot Coffee", "Hot coffee", 49.0, "", "Hot Drinks", true),
            MenuItem("hot_blackcoffee", "Black Coffee", "Black coffee", 39.0, "", "Hot Drinks", true),
            MenuItem("hot_greentea", "Green Tea", "Green tea", 39.0, "", "Hot Drinks", true),
            MenuItem("hot_hotchocolate", "Hot Chocolate", "Hot chocolate", 79.0, "", "Hot Drinks", true)
        )
    }

    private fun getVenkyMenuItems(): List<MenuItem> {
        return listOf(
            // BURGERS
            MenuItem("burger1", "Grilled Chicken Burger (Garlic Pepper/Tandoori)", "Grilled chicken burger with garlic pepper or tandoori flavor", 160.0, "", "Burger", false),
            MenuItem("burger2", "Chicken and Cheese Burger", "Chicken and cheese burger", 145.0, "", "Burger", false),
            MenuItem("burger3", "Crispy Chicken Burger", "Crispy chicken burger", 145.0, "", "Burger", false),
            MenuItem("burger4", "Spicy Chicken Burger", "Spicy chicken burger", 95.0, "", "Burger", false),
            MenuItem("burger5", "Chicken Smacker Burger", "Chicken smacker burger", 65.0, "", "Burger", false),
            MenuItem("burger6", "Chicken Siracha Burger", "Chicken siracha burger", 95.0, "", "Burger", false),

            // WRAP
            MenuItem("wrap1", "Grilled Chicken Wrap (Garlic Pepper/Tandoori)", "Grilled chicken wrap with garlic pepper or tandoori flavor", 165.0, "", "Wrap", false),
            MenuItem("wrap2", "Chicken Kheema Wrap", "Chicken kheema wrap", 165.0, "", "Wrap", false),
            MenuItem("wrap3", "Classic Chicken Sandwich", "Classic chicken sandwich", 120.0, "", "Wrap", false),

            // LITE BITE
            MenuItem("lite1", "Grilled Chicken Wings (Barbeque/Tandoori) 6 pcs", "Grilled chicken wings barbeque or tandoori, 6 pieces", 225.0, "", "Lite Bite", false),
            MenuItem("lite2", "Grilled Chicken Wings (Barbeque/Tandoori) 10 pcs", "Grilled chicken wings barbeque or tandoori, 10 pieces", 340.0, "", "Lite Bite", false),
            MenuItem("lite3", "Hot & Spicy Chicken Wings 6 pcs", "Hot & spicy chicken wings, 6 pieces", 230.0, "", "Lite Bite", false),
            MenuItem("lite4", "Hot & Spicy Chicken Wings 10 pcs", "Hot & spicy chicken wings, 10 pieces", 350.0, "", "Lite Bite", false),
            MenuItem("lite5", "Chicken Nuggets 6 pcs", "Chicken nuggets, 6 pieces", 120.0, "", "Lite Bite", false),
            MenuItem("lite6", "Chicken Nuggets 15 pcs", "Chicken nuggets, 15 pieces", 230.0, "", "Lite Bite", false),
            MenuItem("lite7", "Chicken & Cheese Nuggets 6 pcs", "Chicken & cheese nuggets, 6 pieces", 150.0, "", "Lite Bite", true),
            MenuItem("lite8", "Chicken & Cheese Nuggets 15 pcs", "Chicken & cheese nuggets, 15 pieces", 300.0, "", "Lite Bite", false),
            MenuItem("lite9", "Chicken Lollipops 4 pcs", "Chicken lollipops, 4 pieces", 160.0, "", "Lite Bite", false),
            MenuItem("lite10", "Chicken Lollipops 8 pcs", "Chicken lollipops, 8 pieces", 300.0, "", "Lite Bite", false),
            MenuItem("lite11", "Tender Grill Chicken 4 pcs", "Tender grill chicken, 4 pieces", 165.0, "", "Lite Bite", false),

            // MAIN COURSE
            MenuItem("main1", "Grilled Chicken (Garlic Pepper/Tandoori) Half", "Grilled chicken with garlic pepper or tandoori flavor, half", 310.0, "", "Main Course", false),
            MenuItem("main2", "Grilled Chicken (Garlic Pepper/Tandoori) Full", "Grilled chicken with garlic pepper or tandoori flavor, full", 585.0, "", "Main Course", false),
            MenuItem("main3", "Chicken Kheema with Paratha", "Chicken kheema served with paratha", 200.0, "", "Main Course", false),
            MenuItem("main4", "Chicken Masala with Paratha", "Chicken masala served with paratha", 200.0, "", "Main Course", false),

            // VEG
            MenuItem("veg1", "Veg Burger", "Vegetarian burger", 90.0, "", "Veg", true),
            MenuItem("veg2", "Aloo Tikki Burger", "Aloo tikki burger", 70.0, "", "Veg", true),
            MenuItem("veg3", "Aloo Tikki (3 pcs)", "Aloo tikki, 3 pieces", 60.0, "", "Veg", true),
            MenuItem("veg4", "French Fries", "French fries", 90.0, "", "Veg", true),
            MenuItem("veg5", "Malabar Paratha (1 Nos)", "Malabar paratha, 1 piece", 20.0, "", "Veg", true),
            MenuItem("veg6", "Choco Lava", "Choco lava cake", 90.0, "", "Veg", true),
            MenuItem("veg7", "Veg Nuggets", "Vegetarian nuggets", 60.0, "", "Veg", true),
            MenuItem("veg8", "Veg Crispers", "Vegetarian crispers", 100.0, "", "Veg", true),
            MenuItem("veg9", "Chilly Garlic Potato Bites (12 pcs)", "Chilly garlic potato bites, 12 pieces", 60.0, "", "Veg", true),
            MenuItem("veg10", "Potato Cheese Shots (8 pcs)", "Potato cheese shots, 8 pieces", 100.0, "", "Veg", true),

            // COMBOS
            MenuItem("combo1", "Combo (add to any burger/wrap)", "Convert any burger or wrap into a combo", 85.0, "", "Combo", true),

            // ADD ONS
            MenuItem("addon1", "Egg", "Egg add-on", 15.0, "", "Add Ons", false),
            MenuItem("addon2", "Mayo Dip", "Mayo dip add-on", 20.0, "", "Add Ons", true),
            MenuItem("addon3", "Cheese Slice", "Cheese slice add-on", 15.0, "", "Add Ons", true),
            MenuItem("addon4", "Coca Cola / Fanta / Sprite", "Soft drinks", 0.0, "", "Add Ons", true), // Price as per MRP
            MenuItem("addon5", "Thums Up", "Thums Up soft drink", 0.0, "", "Add Ons", true) // Price as per MRP
        )
    }

    private fun getChaiSuttaBarMenuItems(): List<MenuItem> {
        return listOf(
            // COMBO OFFERS
            MenuItem("combo1", "Maska Bun (2) + Tea (2)", "2 maska buns and 2 teas (A/R/C)", 80.0, "", "Combo", true),
            MenuItem("combo2", "French Fries + Tea (2)", "French fries and 2 teas (A/R/C)", 90.0, "", "Combo", true),
            MenuItem("combo3", "Veg Delight Pizza + Lemonade", "Veg delight pizza and lemonade", 180.0, "", "Combo", true),
            MenuItem("combo4", "Veg Burger + Fries + Mojito", "Veg burger, fries, and mojito", 190.0, "", "Combo", true),
            MenuItem("combo5", "Double Masala Maggie + Fries + Plain Cold Coffee", "Double masala maggie, fries, and plain cold coffee", 200.0, "", "Combo", true),

            // CHAI (S, M, L)
            MenuItem("chai_chocolate_s", "Chocolate Chai (S)", "Small chocolate chai", 20.0, "", "Chai", true),
            MenuItem("chai_chocolate_m", "Chocolate Chai (M)", "Medium chocolate chai", 30.0, "", "Chai", true),
            MenuItem("chai_chocolate_l", "Chocolate Chai (L)", "Large chocolate chai", 50.0, "", "Chai", true),
            MenuItem("chai_adrak_s", "Adrak Chai (S)", "Small adrak chai", 20.0, "", "Chai", true),
            MenuItem("chai_adrak_m", "Adrak Chai (M)", "Medium adrak chai", 30.0, "", "Chai", true),
            MenuItem("chai_adrak_l", "Adrak Chai (L)", "Large adrak chai", 50.0, "", "Chai", true),
            MenuItem("chai_rose_s", "Rose Chai (S)", "Small rose chai", 20.0, "", "Chai", true),
            MenuItem("chai_rose_m", "Rose Chai (M)", "Medium rose chai", 30.0, "", "Chai", true),
            MenuItem("chai_rose_l", "Rose Chai (L)", "Large rose chai", 60.0, "", "Chai", true),
            MenuItem("chai_paan_s", "Paan Chai (S)", "Small paan chai", 20.0, "", "Chai", true),
            MenuItem("chai_paan_m", "Paan Chai (M)", "Medium paan chai", 35.0, "", "Chai", true),
            MenuItem("chai_paan_l", "Paan Chai (L)", "Large paan chai", 60.0, "", "Chai", true),
            MenuItem("chai_elaichi_s", "Elaichi Chai (S)", "Small elaichi chai", 25.0, "", "Chai", true),
            MenuItem("chai_elaichi_m", "Elaichi Chai (M)", "Medium elaichi chai", 40.0, "", "Chai", true),
            MenuItem("chai_elaichi_l", "Elaichi Chai (L)", "Large elaichi chai", 70.0, "", "Chai", true),
            MenuItem("chai_kesar_s", "Kesar Chai (S)", "Small kesar chai", 25.0, "", "Chai", true),
            MenuItem("chai_kesar_m", "Kesar Chai (M)", "Medium kesar chai", 40.0, "", "Chai", true),
            MenuItem("chai_kesar_l", "Kesar Chai (L)", "Large kesar chai", 80.0, "", "Chai", true),
            MenuItem("chai_masala_s", "Masala Chai (S)", "Small masala chai", 25.0, "", "Chai", true),
            MenuItem("chai_masala_m", "Masala Chai (M)", "Medium masala chai", 40.0, "", "Chai", true),
            MenuItem("chai_masala_l", "Masala Chai (L)", "Large masala chai", 80.0, "", "Chai", true),
            MenuItem("chai_lemon_s", "Lemon Chai (S)", "Small lemon chai", 25.0, "", "Chai", true),
            MenuItem("chai_lemon_m", "Lemon Chai (M)", "Medium lemon chai", 40.0, "", "Chai", true),
            MenuItem("chai_lemon_l", "Lemon Chai (L)", "Large lemon chai", 80.0, "", "Chai", true),
            MenuItem("chai_tulsi_s", "Tulsi Chai (S)", "Small tulsi chai", 25.0, "", "Chai", true),
            MenuItem("chai_tulsi_m", "Tulsi Chai (M)", "Medium tulsi chai", 40.0, "", "Chai", true),
            MenuItem("chai_tulsi_l", "Tulsi Chai (L)", "Large tulsi chai", 80.0, "", "Chai", true),
            MenuItem("chai_gurh_s", "Gurh Chai (S)", "Small gurh chai", 25.0, "", "Chai", true),
            MenuItem("chai_gurh_m", "Gurh Chai (M)", "Medium gurh chai", 40.0, "", "Chai", true),
            MenuItem("chai_gurh_l", "Gurh Chai (L)", "Large gurh chai", 80.0, "", "Chai", true),

            // COLD COFFEE
            MenuItem("coldcoffee_plain", "Plain Cold Coffee", "Plain cold coffee", 80.0, "", "Cold Coffee", true),
            MenuItem("coldcoffee_choco", "Choco Cold Coffee", "Choco cold coffee", 90.0, "", "Cold Coffee", true),
            MenuItem("coldcoffee_strong", "Strong Cold Coffee", "Strong cold coffee", 90.0, "", "Cold Coffee", true),
            MenuItem("coldcoffee_brownie", "Brownie Cold Coffee", "Brownie cold coffee", 90.0, "", "Cold Coffee", true),
            MenuItem("coldcoffee_icecream", "Cold Coffee with Ice Cream", "Cold coffee with ice cream", 100.0, "", "Cold Coffee", true),
            MenuItem("coldcoffee_csb_special", "CSB Special Coffee", "CSB special cold coffee", 120.0, "", "Cold Coffee", true),
            MenuItem("coldcoffee_beer", "Beer Cold Coffee", "Beer cold coffee", 100.0, "", "Cold Coffee", true),
            MenuItem("coldcoffee_whiskey", "Whiskey Cold Coffee", "Whiskey cold coffee", 100.0, "", "Cold Coffee", true),
            MenuItem("coldcoffee_rum", "Rum Cold Coffee", "Rum cold coffee", 100.0, "", "Cold Coffee", true),
            MenuItem("coldcoffee_vodka", "Vodka Cold Coffee", "Vodka cold coffee", 100.0, "", "Cold Coffee", true),
            MenuItem("coldcoffee_brandy", "Brandy Cold Coffee", "Brandy cold coffee", 100.0, "", "Cold Coffee", true),
            MenuItem("coldcoffee_scotch", "Scotch Cold Coffee", "Scotch cold coffee", 100.0, "", "Cold Coffee", true),
            MenuItem("coldcoffee_gin", "Gin Cold Coffee", "Gin cold coffee", 100.0, "", "Cold Coffee", true),
            MenuItem("coldcoffee_irishcream", "Irish Cream Cold Coffee", "Irish cream cold coffee", 100.0, "", "Cold Coffee", true),
            MenuItem("coldcoffee_redwine", "Red Wine Cold Coffee", "Red wine cold coffee", 100.0, "", "Cold Coffee", true),
            MenuItem("coldcoffee_hazelnut", "Hazelnut Cold Coffee", "Hazelnut cold coffee", 100.0, "", "Cold Coffee", true),

            // HOT COFFEE (S, M, L)
            MenuItem("hotcoffee_s", "Hot Coffee (S)", "Small hot coffee", 30.0, "", "Hot Coffee", true),
            MenuItem("hotcoffee_m", "Hot Coffee (M)", "Medium hot coffee", 40.0, "", "Hot Coffee", true),
            MenuItem("hotcoffee_l", "Hot Coffee (L)", "Large hot coffee", 70.0, "", "Hot Coffee", true),
            MenuItem("blackcoffee_s", "Black Coffee (S)", "Small black coffee", 35.0, "", "Hot Coffee", true),
            MenuItem("blackcoffee_m", "Black Coffee (M)", "Medium black coffee", 40.0, "", "Hot Coffee", true),
            MenuItem("blackcoffee_l", "Black Coffee (L)", "Large black coffee", 90.0, "", "Hot Coffee", true),
            MenuItem("strongcoffee_s", "Strong Coffee (S)", "Small strong coffee", 35.0, "", "Hot Coffee", true),
            MenuItem("strongcoffee_m", "Strong Coffee (M)", "Medium strong coffee", 45.0, "", "Hot Coffee", true),
            MenuItem("strongcoffee_l", "Strong Coffee (L)", "Large strong coffee", 85.0, "", "Hot Coffee", true),
            MenuItem("chocolatecoffee_s", "Chocolate Coffee (S)", "Small chocolate coffee", 35.0, "", "Hot Coffee", true),
            MenuItem("chocolatecoffee_m", "Chocolate Coffee (M)", "Medium chocolate coffee", 45.0, "", "Hot Coffee", true),
            MenuItem("chocolatecoffee_l", "Chocolate Coffee (L)", "Large chocolate coffee", 85.0, "", "Hot Coffee", true),
            MenuItem("strongchococoffee_s", "Strong Choco Coffee (S)", "Small strong choco coffee", 40.0, "", "Hot Coffee", true),
            MenuItem("strongchococoffee_m", "Strong Choco Coffee (M)", "Medium strong choco coffee", 50.0, "", "Hot Coffee", true),
            MenuItem("strongchococoffee_l", "Strong Choco Coffee (L)", "Large strong choco coffee", 90.0, "", "Hot Coffee", true),
            MenuItem("beercoffee_s", "Beer Coffee (S)", "Small beer coffee", 40.0, "", "Hot Coffee", true),
            MenuItem("beercoffee_m", "Beer Coffee (M)", "Medium beer coffee", 50.0, "", "Hot Coffee", true),
            MenuItem("beercoffee_l", "Beer Coffee (L)", "Large beer coffee", 85.0, "", "Hot Coffee", true),
            MenuItem("whiskeycoffee_s", "Whiskey Coffee (S)", "Small whiskey coffee", 40.0, "", "Hot Coffee", true),
            MenuItem("whiskeycoffee_m", "Whiskey Coffee (M)", "Medium whiskey coffee", 50.0, "", "Hot Coffee", true),
            MenuItem("whiskeycoffee_l", "Whiskey Coffee (L)", "Large whiskey coffee", 85.0, "", "Hot Coffee", true),
            MenuItem("rumcoffee_s", "Rum Coffee (S)", "Small rum coffee", 40.0, "", "Hot Coffee", true),
            MenuItem("rumcoffee_m", "Rum Coffee (M)", "Medium rum coffee", 50.0, "", "Hot Coffee", true),
            MenuItem("rumcoffee_l", "Rum Coffee (L)", "Large rum coffee", 85.0, "", "Hot Coffee", true),
            MenuItem("vodkacoffee_s", "Vodka Coffee (S)", "Small vodka coffee", 40.0, "", "Hot Coffee", true),
            MenuItem("vodkacoffee_m", "Vodka Coffee (M)", "Medium vodka coffee", 50.0, "", "Hot Coffee", true),
            MenuItem("vodkacoffee_l", "Vodka Coffee (L)", "Large vodka coffee", 85.0, "", "Hot Coffee", true),
            MenuItem("redwinecoffee_s", "Red Wine Coffee (S)", "Small red wine coffee", 40.0, "", "Hot Coffee", true),
            MenuItem("redwinecoffee_m", "Red Wine Coffee (M)", "Medium red wine coffee", 50.0, "", "Hot Coffee", true),
            MenuItem("redwinecoffee_l", "Red Wine Coffee (L)", "Large red wine coffee", 85.0, "", "Hot Coffee", true),
            MenuItem("brandycoffee_s", "Brandy Coffee (S)", "Small brandy coffee", 40.0, "", "Hot Coffee", true),
            MenuItem("brandycoffee_m", "Brandy Coffee (M)", "Medium brandy coffee", 50.0, "", "Hot Coffee", true),
            MenuItem("brandycoffee_l", "Brandy Coffee (L)", "Large brandy coffee", 85.0, "", "Hot Coffee", true),

            // BURGERS
            MenuItem("burger_veg", "Veg Burger", "Vegetarian burger", 60.0, "", "Burger", true),
            MenuItem("burger_cheese", "Veg Cheese Burger", "Vegetarian cheese burger", 70.0, "", "Burger", true),
            MenuItem("burger_paneer", "Veg Paneer Burger", "Vegetarian paneer burger", 80.0, "", "Burger", true),
            MenuItem("burger_cheese_paneer", "Veg Cheese Paneer Burger", "Vegetarian cheese paneer burger", 90.0, "", "Burger", true),
            MenuItem("burger_mexican", "Veg Mexican Burger", "Vegetarian Mexican burger", 100.0, "", "Burger", true),
            MenuItem("burger_special", "CSB Special Burger", "CSB special burger", 120.0, "", "Burger", true),

            // PIZZA (S, M)
            MenuItem("pizza_margherita_s", "Margherita Pizza (S)", "Small margherita pizza", 120.0, "", "Pizza", true),
            MenuItem("pizza_margherita_m", "Margherita Pizza (M)", "Medium margherita pizza", 190.0, "", "Pizza", true),
            MenuItem("pizza_onion_s", "Onion Pizza (S)", "Small onion pizza", 130.0, "", "Pizza", true),
            MenuItem("pizza_onion_m", "Onion Pizza (M)", "Medium onion pizza", 200.0, "", "Pizza", true),
            MenuItem("pizza_corn_s", "Sweet Corn Pizza (S)", "Small sweet corn pizza", 140.0, "", "Pizza", true),
            MenuItem("pizza_corn_m", "Sweet Corn Pizza (M)", "Medium sweet corn pizza", 220.0, "", "Pizza", true),
            MenuItem("pizza_olives_s", "Black Olives Pizza (S)", "Small black olives pizza", 150.0, "", "Pizza", true),
            MenuItem("pizza_olives_m", "Black Olives Pizza (M)", "Medium black olives pizza", 250.0, "", "Pizza", true),
            MenuItem("pizza_pepperoni_s", "Red Pepperoni Pizza (S)", "Small red pepperoni pizza", 150.0, "", "Pizza", true),
            MenuItem("pizza_pepperoni_m", "Red Pepperoni Pizza (M)", "Medium red pepperoni pizza", 250.0, "", "Pizza", true),
            MenuItem("pizza_mushroom_s", "Mushroom Pizza (S)", "Small mushroom pizza", 150.0, "", "Pizza", true),
            MenuItem("pizza_mushroom_m", "Mushroom Pizza (M)", "Medium mushroom pizza", 250.0, "", "Pizza", true),
            MenuItem("pizza_jalapeno_s", "Jalapeno Pizza (S)", "Small jalapeno pizza", 150.0, "", "Pizza", true),
            MenuItem("pizza_jalapeno_m", "Jalapeno Pizza (M)", "Medium jalapeno pizza", 250.0, "", "Pizza", true),
            MenuItem("pizza_paneer_s", "Paneer Pizza (S)", "Small paneer pizza", 150.0, "", "Pizza", true),
            MenuItem("pizza_paneer_m", "Paneer Pizza (M)", "Medium paneer pizza", 250.0, "", "Pizza", true),
            MenuItem("pizza_farmfresh_s", "Farm Fresh Pizza (S)", "Small farm fresh pizza (onion, capsicum & 2 toppings of your choice)", 190.0, "", "Pizza", true),
            MenuItem("pizza_farmfresh_m", "Farm Fresh Pizza (M)", "Medium farm fresh pizza (onion, capsicum & 2 toppings of your choice)", 300.0, "", "Pizza", true),
            MenuItem("pizza_extracheese_s", "Extra Cheese Pizza (S)", "Small extra cheese pizza (onion + capsicum with extra cheese & 1 topping)", 190.0, "", "Pizza", true),
            MenuItem("pizza_extracheese_m", "Extra Cheese Pizza (M)", "Medium extra cheese pizza (onion + capsicum with extra cheese & 1 topping)", 300.0, "", "Pizza", true),
            MenuItem("pizza_csb_s", "CSB Pizza (S)", "Small CSB pizza (any 4 toppings)", 220.0, "", "Pizza", true),
            MenuItem("pizza_csb_m", "CSB Pizza (M)", "Medium CSB pizza (any 4 toppings)", 350.0, "", "Pizza", true),

            // PASTA
            MenuItem("pasta_red", "Red Sauce Pasta", "Red sauce pasta", 130.0, "", "Pasta", true),
            MenuItem("pasta_white", "White Sauce Pasta", "White sauce pasta", 140.0, "", "Pasta", true),
            MenuItem("pasta_mix", "Mix Sauce Pasta", "Mix sauce pasta", 150.0, "", "Pasta", true),

            // HEALTHY FEAST
            MenuItem("healthy_cornchaat", "Corn Chaat in Kulhad", "Corn chaat served in kulhad", 50.0, "", "Healthy Feast", true),

            // BITES
            MenuItem("bites_maska", "Maska Bun", "Maska bun", 30.0, "", "Bites", true),
            MenuItem("bites_garlic", "Garlic Bun", "Garlic bun", 40.0, "", "Bites", true),
            MenuItem("bites_garlicbread_plain", "Plain Garlic Bread", "Plain garlic bread", 70.0, "", "Bites", true),
            MenuItem("bites_fries", "French Fries", "French fries", 80.0, "", "Bites", true),
            MenuItem("bites_garlicshots", "Garlic Shots", "Garlic shots", 80.0, "", "Bites", true),
            MenuItem("bites_brownieice", "Brownie with Ice Cream", "Brownie with ice cream", 90.0, "", "Bites", true),
            MenuItem("bites_cheesegarlic", "Cheese Garlic Bread", "Cheese garlic bread", 90.0, "", "Bites", true),
            MenuItem("bites_masalafries", "Masala Fries", "Masala fries", 90.0, "", "Bites", true),
            MenuItem("bites_cheeseshots", "Cheese Shots", "Cheese shots", 90.0, "", "Bites", true),
            MenuItem("bites_periperi", "Peri Peri Fries", "Peri peri fries", 100.0, "", "Bites", true),
            MenuItem("bites_loadedfries", "Loaded Fries", "Loaded fries", 110.0, "", "Bites", true),

            // SANDWICHES
            MenuItem("sandwich_bombay", "Bombay Kaccha Sandwich", "Bombay kaccha sandwich", 60.0, "", "Sandwich", true),
            MenuItem("sandwich_chocolate", "Chocolate Sandwich", "Chocolate sandwich", 70.0, "", "Sandwich", true),
            MenuItem("sandwich_cornmasala", "Corn Masala Sandwich", "Corn masala sandwich", 70.0, "", "Sandwich", true),
            MenuItem("sandwich_chillichatpata", "Chilli Chatpata Sandwich", "Chilli chatpata sandwich", 80.0, "", "Sandwich", true),
            MenuItem("sandwich_veggiegrill", "Veggie Grill", "Veggie grill sandwich", 90.0, "", "Sandwich", true),
            MenuItem("sandwich_cheesechutney", "Cheese Chutney Sandwich", "Cheese chutney sandwich", 90.0, "", "Sandwich", true),
            MenuItem("sandwich_cornmayo", "Corn Mayo Sandwich", "Corn mayo sandwich", 90.0, "", "Sandwich", true),
            MenuItem("sandwich_paneertakatak", "Paneer Takatak Sandwich", "Paneer takatak sandwich", 100.0, "", "Sandwich", true),
            MenuItem("sandwich_tandoori", "Tandoori Sandwich", "Tandoori sandwich", 100.0, "", "Sandwich", true),
            MenuItem("sandwich_paneerspecial", "Paneer Special Sandwich", "Paneer special sandwich", 110.0, "", "Sandwich", true),

            // MAGGIE
            MenuItem("maggie_plain", "Plain Maggie", "Plain maggie", 50.0, "", "Maggie", true),
            MenuItem("maggie_doublemasala", "Double Masala Maggie", "Double masala maggie", 60.0, "", "Maggie", true),
            MenuItem("maggie_corncheese", "Corn Cheese Maggie", "Corn cheese maggie", 70.0, "", "Maggie", true),
            MenuItem("maggie_shezwan", "Shezwan Maggie", "Shezwan maggie", 75.0, "", "Maggie", true),
            MenuItem("maggie_vegetable", "Vegetable Maggie", "Vegetable maggie", 75.0, "", "Maggie", true),
            MenuItem("maggie_cheesebutter", "Cheese & Butter Maggie", "Cheese and butter maggie", 80.0, "", "Maggie", true),
            MenuItem("maggie_tandoori", "Tandoori Maggie", "Tandoori maggie", 80.0, "", "Maggie", true),
            MenuItem("maggie_paneer", "Paneer Maggie", "Paneer maggie", 90.0, "", "Maggie", true),
            MenuItem("maggie_special", "CSB Special Maggie", "CSB special maggie", 100.0, "", "Maggie", true),

            // MILK SHAKES
            MenuItem("shake_vanilla", "Vanilla Shake", "Vanilla milkshake", 80.0, "", "Milk Shakes", true),
            MenuItem("shake_strawberry", "Strawberry Shake", "Strawberry milkshake", 80.0, "", "Milk Shakes", true),
            MenuItem("shake_butterscotch", "Butterscotch Shake", "Butterscotch milkshake", 90.0, "", "Milk Shakes", true),
            MenuItem("shake_oreo", "Oreo Shake", "Oreo milkshake", 90.0, "", "Milk Shakes", true),
            MenuItem("shake_kitkat", "Kitkat Shake", "Kitkat milkshake", 90.0, "", "Milk Shakes", true),
            MenuItem("shake_dairymilk", "Dairy Milk Shake", "Dairy milk shake", 90.0, "", "Milk Shakes", true),
            MenuItem("shake_brownie", "Brownie Shake", "Brownie milkshake", 90.0, "", "Milk Shakes", true),
            MenuItem("shake_bubblegum", "Bubble Gum", "Bubble gum shake", 110.0, "", "Milk Shakes", true),
            MenuItem("shake_romancecandy", "Romance Candy", "Romance candy shake", 110.0, "", "Milk Shakes", true),
            MenuItem("shake_blackcurrent", "Black Current", "Black currant shake", 110.0, "", "Milk Shakes", true),

            // MOJITO
            MenuItem("mojito_classic", "Classic Mojito", "Classic mojito", 80.0, "", "Mojito", true),
            MenuItem("mojito_blackcobra", "Black Cobra", "Black cobra mojito", 80.0, "", "Mojito", true),
            MenuItem("mojito_raspberry", "Raspberry Mojito", "Raspberry mojito", 80.0, "", "Mojito", true),
            MenuItem("mojito_strawberry", "Strawberry Mojito", "Strawberry mojito", 70.0, "", "Mojito", true),
            MenuItem("mojito_greenmint", "Green Mint Mojito", "Green mint mojito", 70.0, "", "Mojito", true),
            MenuItem("mojito_kiwi", "Kiwi Mojito", "Kiwi mojito", 70.0, "", "Mojito", true),

            // MASALA LEMONADE
            MenuItem("lemonade_strawberry", "Strawberry Lemonade", "Strawberry masala lemonade", 70.0, "", "Masala Lemonade", true),
            MenuItem("lemonade_greenmint", "Green Mint Lemonade", "Green mint masala lemonade", 70.0, "", "Masala Lemonade", true),
            MenuItem("lemonade_kiwi", "Kiwi Lemonade", "Kiwi masala lemonade", 70.0, "", "Masala Lemonade", true),

            // ICE CRUSHER
            MenuItem("icecrusher_strawberry", "Strawberry Ice Crusher", "Strawberry ice crusher", 70.0, "", "Ice Crusher", true),
            MenuItem("icecrusher_kiwi", "Kiwi Ice Crusher", "Kiwi ice crusher", 70.0, "", "Ice Crusher", true),
            MenuItem("icecrusher_blueberry", "Blueberry Ice Crusher", "Blueberry ice crusher", 70.0, "", "Ice Crusher", true),

            // ICE TEA
            MenuItem("icetea_plain", "Plain Ice Tea", "Plain ice tea", 70.0, "", "Ice Tea", true),
            MenuItem("icetea_lemon", "Lemon Ice Tea", "Lemon ice tea", 75.0, "", "Ice Tea", true),
            MenuItem("icetea_classic", "Classic Ice Tea", "Classic ice tea", 80.0, "", "Ice Tea", true),

            // EXTRA ADD ON
            MenuItem("addon_oreo", "Extra Oreo", "Extra Oreo topping", 5.0, "", "Add On", true),
            MenuItem("addon_sugarfree", "Sugar Free", "Sugar free option", 5.0, "", "Add On", true),
            MenuItem("addon_teakulhad", "Extra Tea Kulhad", "Extra tea kulhad", 5.0, "", "Add On", true),
            MenuItem("addon_coldcoffeekulhad", "Extra Cold Coffee Kulhad", "Extra cold coffee kulhad", 10.0, "", "Add On", true),
            MenuItem("addon_kitkat", "Extra Kit Kat", "Extra Kit Kat", 10.0, "", "Add On", true),
            MenuItem("addon_cheese", "Extra Cheese", "Extra cheese", 15.0, "", "Add On", true),
            MenuItem("addon_brownbread", "With Brown Bread", "Brown bread option", 15.0, "", "Add On", true),
            MenuItem("addon_icecream", "Extra Ice Cream", "Extra ice cream", 15.0, "", "Add On", true)
        )
    }

    private fun getMajorChangMenuItems(): List<MenuItem> {
        return listOf(
            // NOODLES
            MenuItem("noodle1", "Veg Noodles", "Vegetarian noodles", 99.0, "", "Noodles", true),
            MenuItem("noodle2", "Chilli Garlic Noodles", "Spicy garlic noodles", 149.0, "", "Noodles", true),
            MenuItem("noodle3", "Schezwan Noodles", "Schezwan style noodles", 149.0, "", "Noodles", true),
            MenuItem("noodle4", "Paneer Noodles", "Paneer noodles", 169.0, "", "Noodles", true),
            MenuItem("noodle5", "Dan Dan Noodles", "Dan Dan noodles", 180.0, "", "Noodles", true),
            MenuItem("noodle6", "Hakka Noodles", "Hakka style noodles", 119.0, "", "Noodles", true),
            MenuItem("noodle7", "Chicken Noodles", "Chicken noodles", 169.0, "", "Noodles", false),
            MenuItem("noodle8", "Chopsey", "Chopsey noodles", 169.0, "", "Noodles", true),
            MenuItem("noodle9", "Mushroom Noodles", "Mushroom noodles", 169.0, "", "Noodles", true),
            MenuItem("noodle10", "Korean Black Bean Noodles", "Korean black bean noodles", 259.0, "", "Noodles", true),
            MenuItem("noodle11", "Singapore Noodles", "Singapore style noodles", 160.0, "", "Noodles", true),
            MenuItem("noodle12", "Red Chilli Noodles", "Red chilli noodles", 150.0, "", "Noodles", true),

            // FRENCH FRIES
            MenuItem("fries1", "French Fries", "Classic french fries", 119.0, "", "French Fries", true),
            MenuItem("fries2", "Cheese Fries", "Cheese french fries", 99.0, "", "French Fries", true),
            MenuItem("fries3", "Peri Peri Fries", "Peri peri french fries", 149.0, "", "French Fries", true),
            MenuItem("fries4", "Masala Fries", "Masala french fries", 140.0, "", "French Fries", true),
            MenuItem("fries5", "Chilli Potato", "Chilli potato", 149.0, "", "French Fries", true),
            MenuItem("fries6", "Honey Chilli Potato", "Honey chilli potato", 189.0, "", "French Fries", true),
            MenuItem("fries7", "Honey Chilli Cauliflower", "Honey chilli cauliflower", 189.0, "", "French Fries", true),
            MenuItem("fries8", "Loaded Fries", "Loaded french fries", 149.0, "", "French Fries", true),

            // ROLLS & WRAPS
            MenuItem("wrap1", "Spring Rolls", "Spring rolls", 99.0, "", "Rolls & Wraps", true),
            MenuItem("wrap2", "Veg Wrap", "Vegetarian wrap", 99.0, "", "Rolls & Wraps", true),
            MenuItem("wrap3", "Cheese Wrap", "Cheese wrap", 120.0, "", "Rolls & Wraps", true),
            MenuItem("wrap4", "Paneer Wrap", "Paneer wrap", 120.0, "", "Rolls & Wraps", true),
            MenuItem("wrap5", "Egg Wrap", "Egg wrap", 120.0, "", "Rolls & Wraps", false),
            MenuItem("wrap6", "Chicken Wrap", "Chicken wrap", 150.0, "", "Rolls & Wraps", false),
            MenuItem("wrap7", "Double Chicken Wrap", "Double chicken wrap", 199.0, "", "Rolls & Wraps", false),

            // EGG EXPRESS
            MenuItem("egg1", "Bread Omelette", "Bread omelette", 99.0, "", "Egg Express", false),
            MenuItem("egg2", "Chilli Omelette", "Chilli omelette", 120.0, "", "Egg Express", false),
            MenuItem("egg3", "Chicken Omelette", "Chicken omelette", 149.0, "", "Egg Express", false),
            MenuItem("egg4", "Cheese Omelette", "Cheese omelette", 120.0, "", "Egg Express", false),
            MenuItem("egg5", "Masala Omelette", "Masala omelette", 120.0, "", "Egg Express", false),
            MenuItem("egg6", "Vegetable Omelette", "Vegetable omelette", 120.0, "", "Egg Express", false),

            // PARANTHA
            MenuItem("paratha1", "Aloo Parantha", "Aloo parantha", 79.0, "", "Parantha", true),
            MenuItem("paratha2", "Mix Parantha", "Mix parantha", 99.0, "", "Parantha", true),
            MenuItem("paratha3", "Egg Parantha", "Egg parantha", 99.0, "", "Parantha", true),
            MenuItem("paratha4", "Paneer Parantha", "Paneer parantha", 129.0, "", "Parantha", true),
            MenuItem("paratha5", "Chicken Parantha", "Chicken parantha", 149.0, "", "Parantha", false),

            // RICE
            MenuItem("rice1", "Steam Rice", "Steamed rice", 79.0, "", "Rice", true),
            MenuItem("rice2", "Veg Fried Rice", "Vegetarian fried rice", 129.0, "", "Rice", true),
            MenuItem("rice3", "Chilli Garlic Fried Rice", "Chilli garlic fried rice", 129.0, "", "Rice", true),
            MenuItem("rice4", "Sezwan Fried Rice", "Schezwan fried rice", 149.0, "", "Rice", true),
            MenuItem("rice5", "Veg Biryani", "Vegetarian biryani", 129.0, "", "Rice", true),
            MenuItem("rice6", "Chicken Biryani", "Chicken biryani", 169.0, "", "Rice", false),
            MenuItem("rice7", "Double Chicken Fried Rice", "Double chicken fried rice", 199.0, "", "Rice", false),

            // MEALS
            MenuItem("meal1", "Tawa Roti", "Tawa roti", 12.0, "", "Meals", true),
            MenuItem("meal2", "Butter Roti", "Butter roti", 15.0, "", "Meals", true),
            MenuItem("meal3", "Veg Thali", "Vegetarian thali", 120.0, "", "Meals", true),
            MenuItem("meal4", "Deluxe", "Deluxe meal", 150.0, "", "Meals", true),
            MenuItem("meal5", "Chicken Thali", "Chicken thali", 250.0, "", "Meals", true),
            MenuItem("meal6", "Chole Bathure", "Chole bathure", 99.0, "", "Meals", true),
            MenuItem("meal7", "Puri Chole", "Puri chole", 99.0, "", "Meals", true),
            MenuItem("meal8", "Rajma Masala", "Rajma masala", 99.0, "", "Meals", true),
            MenuItem("meal9", "Shahi Paneer", "Shahi paneer", 150.0, "", "Meals", true),
            MenuItem("meal10", "Mix Veg", "Mix veg", 120.0, "", "Meals", true),
            MenuItem("meal11", "Rajma Rice", "Rajma rice", 99.0, "", "Meals", true),
            MenuItem("meal12", "Paneer Rice", "Paneer rice", 149.0, "", "Meals", true),
            MenuItem("meal13", "Channa Rice", "Channa rice", 99.0, "", "Meals", true),
            MenuItem("meal14", "Pav Bhaji", "Pav bhaji", 99.0, "", "Meals", true),
            MenuItem("meal15", "Egg Burji", "Egg burji", 99.0, "", "Meals", false),
            MenuItem("meal16", "Paneer Burji", "Paneer burji", 149.0, "", "Meals", true),
            MenuItem("meal17", "Paneer Butter Masala", "Paneer butter masala", 249.0, "", "Meals", true),
            MenuItem("meal18", "Channa Masala", "Channa masala", 99.0, "", "Meals", true),
            MenuItem("meal19", "Kadhi Paneer", "Kadhi paneer", 249.0, "", "Meals", true),
            MenuItem("meal20", "Egg Curry", "Egg curry", 199.0, "", "Meals", false),

            // COMBO'S
            MenuItem("combo1", "Noodles + Manchurian", "Noodles with manchurian", 149.0, "", "Combo", true),
            MenuItem("combo2", "Fried Rice + Manchurian", "Fried rice with manchurian", 149.0, "", "Combo", true),
            MenuItem("combo3", "Masala Chicken + Roti", "Masala chicken with roti", 249.0, "", "Combo", false),
            MenuItem("combo4", "Fried Rice + Chilly Chicken", "Fried rice with chilly chicken", 249.0, "", "Combo", false),
            MenuItem("combo5", "Home Style Chicken + Roti", "Home style chicken with roti", 249.0, "", "Combo", false),
            MenuItem("combo6", "Cheese Chilly + Fried Rice", "Cheese chilly with fried rice", 199.0, "", "Combo", true),

            // SALADS
            MenuItem("salad1", "Green Salad", "Green salad", 79.0, "", "Salads", true),
            MenuItem("salad2", "Chicken Salad", "Chicken salad", 129.0, "", "Salads", false),

            // DOSA EXPRESS
            MenuItem("dosa1", "Plain Dosa", "Plain dosa", 99.0, "", "Dosa Express", true),
            MenuItem("dosa2", "Masala Dosa", "Masala dosa", 120.0, "", "Dosa Express", true),
            MenuItem("dosa3", "Paneer Dosa", "Paneer dosa", 149.0, "", "Dosa Express", true),
            MenuItem("dosa4", "Chicken Dosa", "Chicken dosa", 150.0, "", "Dosa Express", false),
            MenuItem("dosa5", "Mix Veg Dosa", "Mix veg dosa", 120.0, "", "Dosa Express", true),

            // SWEET DISH
            MenuItem("sweet1", "Hot Brownie", "Hot brownie", 99.0, "", "Sweet Dish", true),
            MenuItem("sweet2", "Kheer", "Kheer", 89.0, "", "Sweet Dish", true),
            MenuItem("sweet3", "Gulab Jamun", "Gulab jamun", 99.0, "", "Sweet Dish", true),

            // ADD'ON
            MenuItem("addon1", "Cheese Dip", "Cheese dip", 20.0, "", "Add On", true),
            MenuItem("addon2", "Mayonnaise", "Mayonnaise", 20.0, "", "Add On", true),
            MenuItem("addon3", "Chicken Gravy", "Chicken gravy", 49.0, "", "Add On", false),
            MenuItem("addon4", "Egg", "Egg", 19.0, "", "Add On", false),

            // BEVERAGES
            MenuItem("bev1", "Tea", "Tea", 25.0, "", "Beverages", true),
            MenuItem("bev2", "Ginger Tea", "Ginger tea", 30.0, "", "Beverages", true),
            MenuItem("bev3", "Green Tea", "Green tea", 30.0, "", "Beverages", true),
            MenuItem("bev4", "Black Coffee", "Black coffee", 59.0, "", "Beverages", true),
            MenuItem("bev5", "Hot Coffee", "Hot coffee", 59.0, "", "Beverages", true),
            MenuItem("bev6", "Hot Chocolate", "Hot chocolate", 99.0, "", "Beverages", true),
            MenuItem("bev7", "Ice Coke", "Ice coke", 59.0, "", "Beverages", true),

            // SHAKES & MOJITO (sizes: Regular/Big)
            MenuItem("shake1", "Cold Coffee (R)", "Regular cold coffee", 99.0, "", "Shakes & Mojito", true),
            MenuItem("shake2", "Cold Coffee (B)", "Big cold coffee", 129.0, "", "Shakes & Mojito", true),
            MenuItem("shake3", "Hazelnut Cold Coffee (R)", "Regular hazelnut cold coffee", 109.0, "", "Shakes & Mojito", true),
            MenuItem("shake4", "Hazelnut Cold Coffee (B)", "Big hazelnut cold coffee", 149.0, "", "Shakes & Mojito", true),
            MenuItem("shake5", "Black Current Shake (R)", "Regular black current shake", 109.0, "", "Shakes & Mojito", true),
            MenuItem("shake6", "Black Current Shake (B)", "Big black current shake", 149.0, "", "Shakes & Mojito", true),
            MenuItem("shake7", "Kitkat Shake (R)", "Regular kitkat shake", 109.0, "", "Shakes & Mojito", true),
            MenuItem("shake8", "Kitkat Shake (B)", "Big kitkat shake", 149.0, "", "Shakes & Mojito", true),
            MenuItem("shake9", "Oreo Shake (R)", "Regular oreo shake", 109.0, "", "Shakes & Mojito", true),
            MenuItem("shake10", "Oreo Shake (B)", "Big oreo shake", 149.0, "", "Shakes & Mojito", true),
            MenuItem("shake11", "Sweet Lassi (R)", "Regular sweet lassi", 99.0, "", "Shakes & Mojito", true),
            MenuItem("shake12", "Sweet Lassi (B)", "Big sweet lassi", 129.0, "", "Shakes & Mojito", true),
            MenuItem("shake13", "Fresh Lime Water (R)", "Regular fresh lime water", 99.0, "", "Shakes & Mojito", true),
            MenuItem("shake14", "Fresh Lime Water (B)", "Big fresh lime water", 129.0, "", "Shakes & Mojito", true),
            MenuItem("shake15", "Chocolate Shake (R)", "Regular chocolate shake", 109.0, "", "Shakes & Mojito", true),
            MenuItem("shake16", "Chocolate Shake (B)", "Big chocolate shake", 149.0, "", "Shakes & Mojito", true),
            MenuItem("shake17", "Vanilla Shake (R)", "Regular vanilla shake", 109.0, "", "Shakes & Mojito", true),
            MenuItem("shake18", "Vanilla Shake (B)", "Big vanilla shake", 149.0, "", "Shakes & Mojito", true),
            MenuItem("shake19", "Butter Scotch Shake (R)", "Regular butterscotch shake", 109.0, "", "Shakes & Mojito", true),
            MenuItem("shake20", "Butter Scotch Shake (B)", "Big butterscotch shake", 149.0, "", "Shakes & Mojito", true),
            MenuItem("shake21", "Banana Shake (R)", "Regular banana shake", 109.0, "", "Shakes & Mojito", true),
            MenuItem("shake22", "Banana Shake (B)", "Big banana shake", 149.0, "", "Shakes & Mojito", true),
            MenuItem("shake23", "Cranberry Mojito (R)", "Regular cranberry mojito", 109.0, "", "Shakes & Mojito", true),
            MenuItem("shake24", "Cranberry Mojito (B)", "Big cranberry mojito", 149.0, "", "Shakes & Mojito", true),
            MenuItem("shake25", "Blue Berry Mojito (R)", "Regular blueberry mojito", 109.0, "", "Shakes & Mojito", true),
            MenuItem("shake26", "Blue Berry Mojito (B)", "Big blueberry mojito", 149.0, "", "Shakes & Mojito", true),
            MenuItem("shake27", "Green Apple Mojito (R)", "Regular green apple mojito", 109.0, "", "Shakes & Mojito", true),
            MenuItem("shake28", "Green Apple Mojito (B)", "Big green apple mojito", 149.0, "", "Shakes & Mojito", true),
            MenuItem("shake29", "Mango Shake (R)", "Regular mango shake", 109.0, "", "Shakes & Mojito", true),
            MenuItem("shake30", "Mango Shake (B)", "Big mango shake", 149.0, "", "Shakes & Mojito", true),

            // SOUPS
            MenuItem("soup1", "Veg Soup", "Vegetarian soup", 60.0, "", "Soups", true),
            MenuItem("soup2", "Manchow Soup", "Manchow soup", 69.0, "", "Soups", true),
            MenuItem("soup3", "Chicken Soup", "Chicken soup", 99.0, "", "Soups", false),
            MenuItem("soup4", "Chicken Noodles Soup", "Chicken noodles soup", 159.0, "", "Soups", false),
            MenuItem("soup5", "Veg Noodles Soup", "Vegetarian noodles soup", 99.0, "", "Soups", true),

            // BURGER
            MenuItem("burger1", "Veg Burger", "Vegetarian burger", 79.0, "", "Burger", true),
            MenuItem("burger2", "Cheese Burger", "Cheese burger", 99.0, "", "Burger", true),
            MenuItem("burger3", "Paneer Burger", "Paneer burger", 120.0, "", "Burger", true),
            MenuItem("burger4", "Egg Burger", "Egg burger", 99.0, "", "Burger", false),
            MenuItem("burger5", "Chicken Burger", "Chicken burger", 120.0, "", "Burger", false),

            // CHICKEN
            MenuItem("chicken1", "Chilli Chicken", "Chilli chicken", 189.0, "", "Chicken", false),
            MenuItem("chicken2", "Chicken Wings", "Chicken wings", 189.0, "", "Chicken", false),
            MenuItem("chicken3", "KFC Style Chicken Wings", "KFC style chicken wings", 179.0, "", "Chicken", false),
            MenuItem("chicken4", "Chicken Nuggets", "Chicken nuggets", 120.0, "", "Chicken", false),
            MenuItem("chicken5", "Garlic Fried Chicken", "Garlic fried chicken", 189.0, "", "Chicken", false),
            MenuItem("chicken6", "KFC Style Chicken", "KFC style chicken", 299.0, "", "Chicken", false),
            MenuItem("chicken7", "Chicken Popcorn", "Chicken popcorn", 159.0, "", "Chicken", false),
            MenuItem("chicken8", "Mexican Chicken", "Mexican chicken", 189.0, "", "Chicken", false),
            MenuItem("chicken9", "Steamed Chicken", "Steamed chicken", 159.0, "", "Chicken", false),
            MenuItem("chicken10", "Home Style Chicken", "Home style chicken", 249.0, "", "Chicken", false),
            MenuItem("chicken11", "Chicken Loli Pop", "Chicken lollipop", 299.0, "", "Chicken", false),
            MenuItem("chicken12", "Masala Chicken", "Masala chicken", 299.0, "", "Chicken", false),

            // MANCHURIAN
            MenuItem("manchurian1", "Dry Manchurian", "Dry manchurian", 99.0, "", "Manchurian", true),
            MenuItem("manchurian2", "Curry Manchurian", "Curry manchurian", 120.0, "", "Manchurian", true),
            MenuItem("manchurian3", "Chicken Manchurian", "Chicken manchurian", 149.0, "", "Manchurian", false),
            MenuItem("manchurian4", "Cheese Chilli", "Cheese chilli", 159.0, "", "Manchurian", true),
            MenuItem("manchurian5", "Chilli Mushroom", "Chilli mushroom", 159.0, "", "Manchurian", true),

            // SANDWICH
            MenuItem("sandwich1", "Veg Sandwich", "Vegetarian sandwich", 69.0, "", "Sandwich", true),
            MenuItem("sandwich2", "Veg Grilled Sandwich", "Vegetarian grilled sandwich", 99.0, "", "Sandwich", true),
            MenuItem("sandwich3", "Grilled Cheese Sandwich", "Grilled cheese sandwich", 120.0, "", "Sandwich", true),
            MenuItem("sandwich4", "Chicken Sandwich", "Chicken sandwich", 159.0, "", "Sandwich", false),
            MenuItem("sandwich5", "Double Chicken Sandwich", "Double chicken sandwich", 199.0, "", "Sandwich", false),
            MenuItem("sandwich6", "Egg Sandwich", "Egg sandwich", 99.0, "", "Sandwich", false),

            // MOMOS
            MenuItem("momo1", "Veg Momos", "Vegetarian momos", 99.0, "", "Momos", true),
            MenuItem("momo2", "Fried Momos", "Fried momos", 99.0, "", "Momos", true),
            MenuItem("momo3", "Afghani Momos", "Afghani momos", 169.0, "", "Momos", true),
            MenuItem("momo4", "Chilli Momos", "Chilli momos", 169.0, "", "Momos", true),
            MenuItem("momo5", "Tandoori Momos", "Tandoori momos", 169.0, "", "Momos", true),
            MenuItem("momo6", "KFC Style Momos", "KFC style momos", 159.0, "", "Momos", false),
            MenuItem("momo7", "Kurkure Momos", "Kurkure momos", 159.0, "", "Momos", true),
            MenuItem("momo8", "Chicken Momos", "Chicken momos", 169.0, "", "Momos", false),

            // CHAAP
            MenuItem("chaap1", "Masala Chaap", "Masala chaap", 169.0, "", "Chaap", true),
            MenuItem("chaap2", "Chilli Chaap", "Chilli chaap", 169.0, "", "Chaap", true),
            MenuItem("chaap3", "Malai Chaap", "Malai chaap", 169.0, "", "Chaap", true),

            // SNACKS
            MenuItem("snack1", "Bread Pakora", "Bread pakora", 35.0, "", "Snacks", true),
            MenuItem("snack2", "Paneer Pakora", "Paneer pakora", 89.0, "", "Snacks", true),
            MenuItem("snack3", "Chicken Pakora", "Chicken pakora", 119.0, "", "Snacks", true),
            MenuItem("snack4", "Mix Pakora", "Mix pakora", 169.0, "", "Snacks", true),

            // PASTA & MAGGI
            MenuItem("pasta1", "Plain Maggi", "Plain maggi", 50.0, "", "Pasta & Maggi", true),
            MenuItem("pasta2", "Veg Maggi", "Vegetarian maggi", 69.0, "", "Pasta & Maggi", true),
            MenuItem("pasta3", "Cheese Maggi", "Cheese maggi", 99.0, "", "Pasta & Maggi", true),
            MenuItem("pasta4", "White Sauce Pasta", "White sauce pasta", 129.0, "", "Pasta & Maggi", true),
            MenuItem("pasta5", "Red Sauce Pasta", "Red sauce pasta", 129.0, "", "Pasta & Maggi", true),
            MenuItem("pasta6", "Mix Sauce Pasta", "Mix sauce pasta", 150.0, "", "Pasta & Maggi", true),
            MenuItem("pasta7", "Pink Sauce Pasta", "Pink sauce pasta", 150.0, "", "Pasta & Maggi", true),
            MenuItem("pasta8", "Chicken Pasta", "Chicken pasta", 179.0, "", "Pasta & Maggi", true),
            MenuItem("pasta9", "Alfredo Pasta", "Alfredo pasta", 199.0, "", "Pasta & Maggi", true),
            MenuItem("pasta10", "Mushroom Pasta", "Mushroom pasta", 179.0, "", "Pasta & Maggi", true),

            // CHAAT
            MenuItem("chaat1", "Allo Tikki Chaat", "Aloo tikki chaat", 99.0, "", "Chaat", true),
            MenuItem("chaat2", "Katori Chaat", "Katori chaat", 99.0, "", "Chaat", true),
            MenuItem("chaat3", "Papri Chaat", "Papri chaat", 89.0, "", "Chaat", true),
            MenuItem("chaat4", "Dahi Bhalla Chaat", "Dahi bhalla chaat", 99.0, "", "Chaat", true),
            MenuItem("chaat5", "Spicy Blast Chaat", "Spicy blast chaat", 150.0, "", "Chaat", true),
            MenuItem("chaat6", "Bhel Puri Chaat", "Bhel puri chaat", 99.0, "", "Chaat", true)
        )
    }

    private fun getGoldenForkMenuItems(): List<MenuItem> {
        return listOf(
            // Burgers
            MenuItem("burger1", "Aloo Tikki Burger", "Aloo tikki burger", 50.0, "", "Burger", true),
            MenuItem("burger2", "Tikki Tummy Burger", "Tikki tummy burger", 60.0, "", "Burger", true),
            MenuItem("burger3", "Cheese Tikki Burger", "Cheese tikki burger", 70.0, "", "Burger", true),
            MenuItem("burger4", "Crispy Paneer Burger", "Crispy paneer burger", 80.0, "", "Burger", true),
            MenuItem("burger5", "Classic Mexican Burger", "Classic Mexican burger", 100.0, "", "Burger", true),

            // Fries
            MenuItem("fries1", "Salted Fries", "Salted fries", 90.0, "", "Fries", true),
            MenuItem("fries2", "Masala Fries", "Masala fries", 100.0, "", "Fries", true),
            MenuItem("fries3", "Peri Peri Fries", "Peri peri fries", 120.0, "", "Fries", true),
            MenuItem("fries4", "Cheesy Fries", "Cheesy fries", 120.0, "", "Fries", true),
            MenuItem("fries5", "Veg Nuggets", "Veg nuggets", 100.0, "", "Fries", true),
            MenuItem("fries6", "Paneer Fingers", "Paneer fingers", 110.0, "", "Fries", true),

            // Sandwich
            MenuItem("sandwich1", "Veg Grilled Sandwich", "Veg grilled sandwich", 70.0, "", "Sandwich", true),
            MenuItem("sandwich2", "Tandoori Sandwich", "Tandoori sandwich", 80.0, "", "Sandwich", true),
            MenuItem("sandwich3", "Cheese Corn Sandwich", "Cheese corn sandwich", 90.0, "", "Sandwich", true),
            MenuItem("sandwich4", "Paneer Sandwich", "Paneer sandwich", 100.0, "", "Sandwich", true),
            MenuItem("sandwich5", "Dominator Sandwich", "Dominator sandwich", 120.0, "", "Sandwich", true),

            // Wraps
            MenuItem("wrap1", "Aloo Thick Wrap", "Aloo thick wrap", 70.0, "", "Wrap", true),
            MenuItem("wrap2", "Veg Wrap", "Veg wrap", 80.0, "", "Wrap", true),
            MenuItem("wrap3", "Paneer Thick Wrap", "Paneer thick wrap", 100.0, "", "Wrap", true),
            MenuItem("wrap4", "Mexican Wrap", "Mexican wrap", 100.0, "", "Wrap", true),
            MenuItem("wrap5", "Tandoori Wrap", "Tandoori wrap", 120.0, "", "Wrap", true),

            // Shakes
            MenuItem("shake1", "Black Current Shake", "Black current shake", 80.0, "", "Shake", true),
            MenuItem("shake2", "Vanilla Shake", "Vanilla shake", 80.0, "", "Shake", true),
            MenuItem("shake3", "Strawberry Shake", "Strawberry shake", 80.0, "", "Shake", true),
            MenuItem("shake4", "Butter Scotch Shake", "Butter scotch shake", 80.0, "", "Shake", true),
            MenuItem("shake5", "Oreo Shake", "Oreo shake", 80.0, "", "Shake", true),

            // Coolers
            MenuItem("cooler1", "Fresh Lime Soda", "Fresh lime soda", 70.0, "", "Cooler", true),
            MenuItem("cooler2", "Peach Ice Tea", "Peach ice tea", 80.0, "", "Cooler", true),
            MenuItem("cooler3", "Lemon Ice Tea", "Lemon ice tea", 80.0, "", "Cooler", true),
            MenuItem("cooler4", "Aam Panna", "Aam panna", 70.0, "", "Cooler", true),
            MenuItem("cooler5", "Masala Lemonade", "Masala lemonade", 70.0, "", "Cooler", true),

            // Cold Coffee
            MenuItem("coldcoffee1", "Classic Cold Coffee", "Classic cold coffee", 90.0, "", "Cold Coffee", true),
            MenuItem("coldcoffee2", "Chocolate Cold Coffee", "Chocolate cold coffee", 90.0, "", "Cold Coffee", true),
            MenuItem("coldcoffee3", "Hazelnut Cold Coffee", "Hazelnut cold coffee", 100.0, "", "Cold Coffee", true),
            MenuItem("coldcoffee4", "Caramel Cold Coffee", "Caramel cold coffee", 100.0, "", "Cold Coffee", true),

            // Tea / Coffee
            MenuItem("tea1", "Normal Tea", "Normal tea", 25.0, "", "Tea/Coffee", true),
            MenuItem("tea2", "Elaichi Tea", "Elaichi tea", 30.0, "", "Tea/Coffee", true),
            MenuItem("tea3", "Adrak Tea", "Adrak tea", 30.0, "", "Tea/Coffee", true),
            MenuItem("tea4", "Masala Tea", "Masala tea", 30.0, "", "Tea/Coffee", true),
            MenuItem("tea5", "Hot Coffee", "Hot coffee", 40.0, "", "Tea/Coffee", true),

            // Chaat Section
            MenuItem("chaat1", "Aloo Tikki", "Aloo tikki", 70.0, "", "Chaat", true),
            MenuItem("chaat2", "Dahi Bhalla", "Dahi bhalla", 60.0, "", "Chaat", true),
            MenuItem("chaat3", "Dahi Bhalla Papri", "Dahi bhalla papri", 70.0, "", "Chaat", true),
            MenuItem("chaat4", "Papri Chaat", "Papri chaat", 60.0, "", "Chaat", true),
            MenuItem("chaat5", "Bhelpuri", "Bhelpuri", 50.0, "", "Chaat", true),
            MenuItem("chaat6", "Stuffed Golgappa (6 pcs)", "Stuffed golgappa (6 pieces)", 60.0, "", "Chaat", true),
            MenuItem("chaat7", "Mix Pakora", "Mix pakora", 80.0, "", "Chaat", true),
            MenuItem("chaat8", "Pav Bhaji", "Pav bhaji", 80.0, "", "Chaat", true),

            // Chinese
            MenuItem("chinese1", "Veg Noodles", "Veg noodles", 90.0, "", "Chinese", true),
            MenuItem("chinese2", "Chilli Garlic Noodles", "Chilli garlic noodles", 120.0, "", "Chinese", true),
            MenuItem("chinese3", "Paneer Noodles", "Paneer noodles", 130.0, "", "Chinese", true),
            MenuItem("chinese4", "Manchurian (Dry)", "Manchurian dry", 100.0, "", "Chinese", true),
            MenuItem("chinese5", "Manchurian (Gravy)", "Manchurian gravy", 120.0, "", "Chinese", true),
            MenuItem("chinese6", "Honey Chilli Potato", "Honey chilli potato", 150.0, "", "Chinese", true),
            MenuItem("chinese7", "Honey Chilli Cauliflower", "Honey chilli cauliflower", 180.0, "", "Chinese", true),
            MenuItem("chinese8", "Chilli Paneer", "Chilli paneer", 180.0, "", "Chinese", true),
            MenuItem("chinese9", "Veg Momos", "Veg momos", 100.0, "", "Chinese", true),
            MenuItem("chinese10", "Kurkure Momos (Veg)", "Kurkure momos (veg)", 150.0, "", "Chinese", true),
            MenuItem("chinese11", "Kurkure Momos (Paneer)", "Kurkure momos (paneer)", 150.0, "", "Chinese", true),
            MenuItem("chinese12", "Spring Roll", "Spring roll", 80.0, "", "Chinese", true),
            MenuItem("chinese13", "Fried Rice", "Fried rice", 90.0, "", "Chinese", true),
            MenuItem("chinese14", "Paneer Fried Rice", "Paneer fried rice", 120.0, "", "Chinese", true),

            // South Indian
            MenuItem("south1", "Plain Dosa", "Plain dosa", 80.0, "", "South Indian", true),
            MenuItem("south2", "Masala Dosa", "Masala dosa", 100.0, "", "South Indian", true),
            MenuItem("south3", "Onion Dosa", "Onion dosa", 100.0, "", "South Indian", true),
            MenuItem("south4", "Paneer Dosa", "Paneer dosa", 120.0, "", "South Indian", true),
            MenuItem("south5", "Idli Sambar", "Idli sambar", 50.0, "", "South Indian", true),
            MenuItem("south6", "Onion Uttapam", "Onion uttapam", 90.0, "", "South Indian", true),
            MenuItem("south7", "Paneer Uttapam", "Paneer uttapam", 120.0, "", "South Indian", true),
            MenuItem("south8", "Mix Uttapam", "Mix uttapam", 130.0, "", "South Indian", true),

            // North Indian
            MenuItem("north1", "Rajmah Rice", "Rajmah rice", 70.0, "", "North Indian", true),
            MenuItem("north2", "Kadhi Rice", "Kadhi rice", 70.0, "", "North Indian", true),
            MenuItem("north3", "Channa Rice", "Channa rice", 70.0, "", "North Indian", true),
            MenuItem("north4", "Paneer Rice", "Paneer rice", 90.0, "", "North Indian", true),
            MenuItem("north5", "Veg Thali", "Veg thali", 130.0, "", "North Indian", true),
            MenuItem("north6", "Delux Thali", "Delux thali", 150.0, "", "North Indian", true),
            MenuItem("north7", "Channa Bhatura", "Channa bhatura", 90.0, "", "North Indian", true),
            MenuItem("north8", "Butter Kulcha with Channa", "Butter kulcha with channa", 90.0, "", "North Indian", true),
            MenuItem("north9", "Kadhai Paneer", "Kadhai paneer", 150.0, "", "North Indian", true),
            MenuItem("north10", "Paneer Butter Masala", "Paneer butter masala", 150.0, "", "North Indian", true),
            MenuItem("north11", "Shahi Paneer", "Shahi paneer", 150.0, "", "North Indian", true),
            MenuItem("north12", "Roti", "Roti", 10.0, "", "North Indian", true),
            MenuItem("north13", "Curd", "Curd", 20.0, "", "North Indian", true),

            // Lassi
            MenuItem("lassi1", "Sweet Lassi", "Sweet lassi", 60.0, "", "Lassi", true),
            MenuItem("lassi2", "Masala Lassi", "Masala lassi", 60.0, "", "Lassi", true)
        )
    }

    private fun getZaikaMenuItems(): List<MenuItem> {
        return listOf(
            // VEG THALI
            MenuItem("z1", "Rajma Thali", "Rajma + Rice + 2 Roti", 80.0, "", "Veg Thali", true),
            MenuItem("z2", "Chole Thali", "Chole + Rice + 2 Roti", 80.0, "", "Veg Thali", true),
            MenuItem("z3", "Dal Makhni Thali", "Dal Makhni + Rice + 2 Roti", 90.0, "", "Veg Thali", true),
            MenuItem("z4", "Spl. Veg Thali", "2 Sabzi + Dal Makhni + Rice + 2 Roti", 110.0, "", "Veg Thali", true),
            MenuItem("z5", "Kadhai Paneer Thali", "Kadhai Paneer + 2 Roti + Rice", 140.0, "", "Veg Thali", true),
            MenuItem("z6", "Paneer Butter Masala Thali", "Paneer Butter Masala + 2 Roti + Rice", 140.0, "", "Veg Thali", true),

            // VEG MAIN COURSE
            MenuItem("z7", "Dal Makhni", "", 90.0, "", "Veg Main Course", true),
            MenuItem("z8", "Mix Veg", "", 100.0, "", "Veg Main Course", true),
            MenuItem("z9", "Channa Masala", "", 90.0, "", "Veg Main Course", true),
            MenuItem("z10", "Cholle White", "", 80.0, "", "Veg Main Course", true),
            MenuItem("z11", "Kadhai Paneer", "", 150.0, "", "Veg Main Course", true),
            MenuItem("z12", "Shahi Paneer", "", 150.0, "", "Veg Main Course", true),
            MenuItem("z13", "Paneer Butter Masala", "", 150.0, "", "Veg Main Course", true),
            MenuItem("z14", "Paneer Do Pyaza", "", 150.0, "", "Veg Main Course", true),
            MenuItem("z15", "Soya Butter Masala", "", 150.0, "", "Veg Main Course", true),
            MenuItem("z16", "Kadhai Soya", "", 150.0, "", "Veg Main Course", true),

            // COMBOS (VEG)
            MenuItem("z17", "Rajmah + Rice", "", 60.0, "", "Veg Combo", true),
            MenuItem("z18", "Cholle + Rice", "", 60.0, "", "Veg Combo", true),
            MenuItem("z19", "Dal Makhni + Rice", "", 80.0, "", "Veg Combo", true),
            MenuItem("z20", "Paneer + Rice", "", 130.0, "", "Veg Combo", true),

            // PARANTHA
            MenuItem("z21", "Aloo Parantha", "", 50.0, "", "Parantha", true),
            MenuItem("z22", "Paneer Parantha", "", 75.0, "", "Parantha", true),

            // VEG SNACKS
            MenuItem("z23", "Paneer Tikka", "", 220.0, "", "Veg Snacks", true),
            MenuItem("z24", "Paneer Malai Tikka", "", 240.0, "", "Veg Snacks", true),
            MenuItem("z25", "Soya Malai Chaap", "", 200.0, "", "Veg Snacks", true),
            MenuItem("z26", "Soya Achari Tikka", "", 200.0, "", "Veg Snacks", true),

            // NON-VEG THALI
            MenuItem("z27", "Kadhai Chicken Thali", "Kadhai Chicken + 2 Roti + Rice", 160.0, "", "Non-Veg Thali", false),
            MenuItem("z28", "Butter Chicken Thali", "Butter Chicken + 2 Roti + Rice", 170.0, "", "Non-Veg Thali", false),
            MenuItem("z29", "Tawa Chicken Thali", "Tawa Chicken + 2 Roti + Rice", 160.0, "", "Non-Veg Thali", false),
            MenuItem("z30", "Masala Chicken", "Masala Chicken + 2 Roti + Rice", 160.0, "", "Non-Veg Thali", false),

            // NON-VEG MAIN COURSE (Half / Full)
            MenuItem("z31", "Butter Chicken", "", 250.0, "", "Non-Veg Main Course", false),
            MenuItem("z32", "Butter Chicken", "", 430.0, "", "Non-Veg Main Course", false),
            MenuItem("z33", "Kadhai Chicken", "", 250.0, "", "Non-Veg Main Course", false),
            MenuItem("z34", "Kadhai Chicken", "", 430.0, "", "Non-Veg Main Course", false),
            MenuItem("z35", "Masala Chicken", "", 250.0, "", "Non-Veg Main Course", false),
            MenuItem("z36", "Masala Chicken", "", 430.0, "", "Non-Veg Main Course", false),
            MenuItem("z37", "Tawa Chicken", "", 250.0, "", "Non-Veg Main Course", false),
            MenuItem("z38", "Tawa Chicken", "", 430.0, "", "Non-Veg Main Course", false),
            MenuItem("z39", "Chicken Do Pyaza", "", 250.0, "", "Non-Veg Main Course", false),
            MenuItem("z40", "Chicken Curry (Home Style)", "", 250.0, "", "Non-Veg Main Course", false),
            MenuItem("z41", "Chicken Curry (Home Style)", "", 420.0, "", "Non-Veg Main Course", false),

            // COMBOS (NON-VEG)
            MenuItem("z42", "Chicken Curry + Rice", "", 160.0, "", "Non-Veg Combo", false),
            MenuItem("z43", "Butter Chicken + Rice", "", 160.0, "", "Non-Veg Combo", false),

            // NON-VEG SNACKS
            MenuItem("z44", "Tandoori Chicken", "", 250.0, "", "Non-Veg Snacks", false),
            MenuItem("z45", "Tandoori Chicken", "", 420.0, "", "Non-Veg Snacks", false),
            MenuItem("z46", "Afghani Chicken", "", 270.0, "", "Non-Veg Snacks", false),
            MenuItem("z47", "Afghani Chicken", "", 450.0, "", "Non-Veg Snacks", false),

            // BIRYANI
            MenuItem("z48", "Veg Biryani", "", 130.0, "", "Biryani", true),
            MenuItem("z49", "Chicken Biryani", "", 160.0, "", "Biryani", false),

            // BREADS / KULCHA
            MenuItem("z50", "Tandoori Roti", "", 20.0, "", "Bread", true),
            MenuItem("z51", "Butter Naan", "", 40.0, "", "Bread", true),
            MenuItem("z52", "Garlic Naan", "", 50.0, "", "Bread", true),
            MenuItem("z53", "Lacha Paratha", "", 40.0, "", "Bread", true),
            MenuItem("z54", "Amritsari Kulcha (Aloo/Pyaza)", "", 80.0, "", "Bread", true),
            MenuItem("z55", "Amritsari Kulcha (Paneer)", "", 80.0, "", "Bread", true),
            MenuItem("z56", "Cheese Naan with Gravy", "", 110.0, "", "Bread", true)
        )
    }

    private fun getXBurgersMenuItems(): List<MenuItem> {
        return listOf(
            // VEG BURGERS
            MenuItem("vegbrgr1", "Miniox Burger", "Veg burger", 50.0, "", "Veg Burger", true),
            MenuItem("vegbrgr2", "Aloo Tikki Burger", "Aloo tikki burger", 50.0, "", "Veg Burger", true),
            MenuItem("vegbrgr3", "Aloo Tikki Cheese Burger", "Aloo tikki cheese burger", 60.0, "", "Veg Burger", true),
            MenuItem("vegbrgr4", "Aloo Tikki Crunch Burger", "Aloo tikki crunch burger", 60.0, "", "Veg Burger", true),
            MenuItem("vegbrgr5", "Aloo Tikki Cheese Crunch Burger", "Aloo tikki cheese crunch burger", 80.0, "", "Veg Burger", true),
            MenuItem("vegbrgr6", "Veggi Burger", "Veggi burger", 60.0, "", "Veg Burger", true),
            MenuItem("vegbrgr7", "Veggi Spicy Burger", "Veggi spicy burger", 70.0, "", "Veg Burger", true),
            MenuItem("vegbrgr8", "Veggi Crunch Burger", "Veggi crunch burger", 80.0, "", "Veg Burger", true),
            MenuItem("vegbrgr9", "Veggi Cheese Burger", "Veggi cheese burger", 80.0, "", "Veg Burger", true),
            MenuItem("vegbrgr10", "Veggi Cheese Chilli Burger", "Veggi cheese chilli burger", 90.0, "", "Veg Burger", true),
            MenuItem("vegbrgr11", "Paneer Burger", "Paneer burger", 90.0, "", "Veg Burger", true),
            MenuItem("vegbrgr12", "Maharaja Burger", "Maharaja burger", 110.0, "", "Veg Burger", true),

            // NON VEG BURGERS
            MenuItem("nonvegbrgr1", "Egg Burger", "Egg burger", 60.0, "", "Non Veg Burger", false),
            MenuItem("nonvegbrgr2", "Chicken Burger", "Chicken burger", 80.0, "", "Non Veg Burger", false),
            MenuItem("nonvegbrgr3", "Chicken Crunch Burger", "Chicken crunch burger", 90.0, "", "Non Veg Burger", false),
            MenuItem("nonvegbrgr4", "Chicken Cheese Burger", "Chicken cheese burger", 90.0, "", "Non Veg Burger", false),
            MenuItem("nonvegbrgr5", "Chicken Maharaja Burger", "Chicken maharaja burger", 110.0, "", "Non Veg Burger", false),

            // WRAPS
            MenuItem("wrap1", "Veg Wrap", "Veg wrap", 70.0, "", "Wrap", true),
            MenuItem("wrap2", "Veg Crunch Wrap", "Veg crunch wrap", 80.0, "", "Wrap", true),
            MenuItem("wrap3", "Soya Crunch Wrap", "Soya crunch wrap", 80.0, "", "Wrap", true),
            MenuItem("wrap4", "Paneer Wrap", "Paneer wrap", 90.0, "", "Wrap", true),
            MenuItem("wrap5", "Egg Wrap", "Egg wrap", 80.0, "", "Wrap", false),
            MenuItem("wrap6", "Chicken Wrap", "Chicken wrap", 90.0, "", "Wrap", false),

            // RICE BOWL
            MenuItem("ricebowl1", "Plain Rice Bowl", "Plain rice bowl", 40.0, "", "Rice Bowl", true),
            MenuItem("ricebowl2", "Veg Rice Bowl", "Veg rice bowl", 50.0, "", "Rice Bowl", true),
            MenuItem("ricebowl3", "Paneer Rice Bowl", "Paneer rice bowl", 70.0, "", "Rice Bowl", true),
            MenuItem("ricebowl4", "Egg Rice Bowl", "Egg rice bowl", 70.0, "", "Rice Bowl", false),
            MenuItem("ricebowl5", "Chicken Rice Bowl", "Chicken rice bowl", 90.0, "", "Rice Bowl", false),

            // SANDWICH
            MenuItem("sandwich1", "Cold Sandwich (White Bread)", "Cold sandwich (white bread)", 30.0, "", "Sandwich", true),
            MenuItem("sandwich2", "Cold Sandwich (Brown Bread)", "Cold sandwich (brown bread)", 40.0, "", "Sandwich", true),
            MenuItem("sandwich3", "Veggi Grilled Sandwich", "Veggi grilled sandwich", 70.0, "", "Sandwich", true),
            MenuItem("sandwich4", "Corn Grilled Sandwich", "Corn grilled sandwich", 80.0, "", "Sandwich", true),
            MenuItem("sandwich5", "Italiopiz Grilled Sandwich", "Italiopiz grilled sandwich", 80.0, "", "Sandwich", true),
            MenuItem("sandwich6", "Soya Sandwich", "Soya sandwich", 80.0, "", "Sandwich", true),
            MenuItem("sandwich7", "Paneer Grilled Sandwich", "Paneer grilled sandwich", 80.0, "", "Sandwich", true),
            MenuItem("sandwich8", "Egg Grilled Sandwich", "Egg grilled sandwich", 80.0, "", "Sandwich", false),
            MenuItem("sandwich9", "Chicken Grilled Sandwich", "Chicken grilled sandwich", 90.0, "", "Sandwich", false),

            // FRIES
            MenuItem("fries1", "French Fries", "French fries", 70.0, "", "Fries", true),
            MenuItem("fries2", "Masala Fries", "Masala fries", 80.0, "", "Fries", true),
            MenuItem("fries3", "Cheesy Masala Fries", "Cheesy masala fries", 90.0, "", "Fries", true),
            MenuItem("fries4", "Canadian Fries", "Canadian fries", 90.0, "", "Fries", true),

            // SNACKS
            MenuItem("snack1", "Tandoori Pasta", "Tandoori pasta", 80.0, "", "Snacks", true),
            MenuItem("snack2", "Veg Nuggets", "Veg nuggets", 80.0, "", "Snacks", true),
            MenuItem("snack3", "Masala Corn", "Masala corn", 50.0, "", "Snacks", true),
            MenuItem("snack4", "Soya Malai Chaap", "Soya malai chaap", 80.0, "", "Snacks", true),

            // DESSERTS
            MenuItem("dessert1", "Simple Brownie", "Simple brownie", 40.0, "", "Dessert", true),
            MenuItem("dessert2", "Chocolate Brownie", "Chocolate brownie", 60.0, "", "Dessert", true),
            MenuItem("dessert3", "Brownie with Chocolate", "Brownie with chocolate", 70.0, "", "Dessert", true)
        )
    }
    private fun getChefOnFireMenuItems(): List<MenuItem> {
        return listOf(
            // VEG MOMOS
            MenuItem("vegmomo1", "Steam Momos", "Veg steam momos", 80.0, "", "Veg Momos", true),
            MenuItem("vegmomo2", "Fried Momos", "Veg fried momos", 100.0, "", "Veg Momos", true),
            MenuItem("vegmomo3", "Tandoori Momos", "Veg tandoori momos", 130.0, "", "Veg Momos", true),
            MenuItem("vegmomo4", "Crunchy Momos", "Veg crunchy momos", 130.0, "", "Veg Momos", true),
            MenuItem("vegmomo5", "Afghani Momos", "Veg afghani momos", 150.0, "", "Veg Momos", true),
            MenuItem("vegmomo6", "Makhani Momos", "Veg makhani momos", 150.0, "", "Veg Momos", true),
            MenuItem("vegmomo7", "Schezwan Momos", "Veg schezwan momos", 150.0, "", "Veg Momos", true),

            // PANEER MOMOS
            MenuItem("paneermomo1", "Steam Momos", "Paneer steam momos", 90.0, "", "Paneer Momos", true),
            MenuItem("paneermomo2", "Fried Momos", "Paneer fried momos", 100.0, "", "Paneer Momos", true),
            MenuItem("paneermomo3", "Tandoori Momos", "Paneer tandoori momos", 150.0, "", "Paneer Momos", true),
            MenuItem("paneermomo4", "Crunchy Momos", "Paneer crunchy momos", 150.0, "", "Paneer Momos", true),
            MenuItem("paneermomo5", "Afghani Momos", "Paneer afghani momos", 180.0, "", "Paneer Momos", true),
            MenuItem("paneermomo6", "Makhani Momos", "Paneer makhani momos", 170.0, "", "Paneer Momos", true),
            MenuItem("paneermomo7", "Schezwan Momos", "Paneer schezwan momos", 170.0, "", "Paneer Momos", true),

            // NON-VEG MOMOS
            MenuItem("nonvegmomo1", "Steam Momos", "Non-veg steam momos", 90.0, "", "Non-Veg Momos", false),
            MenuItem("nonvegmomo2", "Fried Momos", "Non-veg fried momos", 110.0, "", "Non-Veg Momos", false),
            MenuItem("nonvegmomo3", "Tandoori Momos", "Non-veg tandoori momos", 150.0, "", "Non-Veg Momos", false),
            MenuItem("nonvegmomo4", "Crunchy Momos", "Non-veg crunchy momos", 180.0, "", "Non-Veg Momos", false),
            MenuItem("nonvegmomo5", "Afghani Momos", "Non-veg afghani momos", 170.0, "", "Non-Veg Momos", false),
            MenuItem("nonvegmomo6", "Makhani Momos", "Non-veg makhani momos", 170.0, "", "Non-Veg Momos", false),
            MenuItem("nonvegmomo7", "Schezwan Momos", "Non-veg schezwan momos", 170.0, "", "Non-Veg Momos", false),

            // MOJITOS N COOLERS
            MenuItem("mojito1", "Classic Mint", "Classic mint mojito", 70.0, "", "Mojitos n Coolers", true),
            MenuItem("mojito2", "Star Ice Tea", "Star ice tea", 60.0, "", "Mojitos n Coolers", true),
            MenuItem("mojito3", "Kiwi", "Kiwi mojito", 70.0, "", "Mojitos n Coolers", true),
            MenuItem("mojito4", "Peach", "Peach mojito", 70.0, "", "Mojitos n Coolers", true),
            MenuItem("mojito5", "Green Apple", "Green apple mojito", 70.0, "", "Mojitos n Coolers", true),
            MenuItem("mojito6", "Strawberry", "Strawberry mojito", 70.0, "", "Mojitos n Coolers", true),
            MenuItem("mojito7", "Passion Fruit", "Passion fruit mojito", 80.0, "", "Mojitos n Coolers", true),
            MenuItem("mojito8", "Black Currant", "Black currant mojito", 80.0, "", "Mojitos n Coolers", true),
            MenuItem("mojito9", "Green Ice Tea", "Green ice tea", 60.0, "", "Mojitos n Coolers", true),
            MenuItem("mojito10", "Kiwi Ice Tea", "Kiwi ice tea", 70.0, "", "Mojitos n Coolers", true),
            MenuItem("mojito11", "Peach Ice Tea", "Peach ice tea", 70.0, "", "Mojitos n Coolers", true),
            MenuItem("mojito12", "Kala Khatha", "Kala khatha", 70.0, "", "Mojitos n Coolers", true),
            MenuItem("mojito13", "Lemon Soda", "Lemon soda", 50.0, "", "Mojitos n Coolers", true),
            MenuItem("mojito14", "Strawberry Soda", "Strawberry soda", 80.0, "", "Mojitos n Coolers", true),
            MenuItem("mojito15", "Orange & Lemon", "Orange & lemon mojito", 80.0, "", "Mojitos n Coolers", true),
            MenuItem("mojito16", "Green Ice Tea", "Green ice tea", 60.0, "", "Mojitos n Coolers", true),
            MenuItem("mojito17", "Kiwi Crush", "Kiwi crush", 80.0, "", "Mojitos n Coolers", true),

            // SHAKES N COFFEE
            MenuItem("shake1", "Mango Shake", "Mango shake", 70.0, "", "Shakes n Coffee", true),
            MenuItem("shake2",

                "Banana Shake", "Banana shake", 70.0, "", "Shakes n Coffee", true),
            MenuItem("shake3", "Chocolate Shake", "Chocolate shake", 70.0, "", "Shakes n Coffee", true),
            MenuItem("shake4", "Kiwi Shake", "Kiwi shake", 70.0, "", "Shakes n Coffee", true),
            MenuItem("shake5", "Pineapple Shake", "Pineapple shake", 70.0, "", "Shakes n Coffee", true),
            MenuItem("shake6", "Butter Scotch Shake", "Butter scotch shake", 70.0, "", "Shakes n Coffee", true),
            MenuItem("shake7", "Strawberry Shake", "Strawberry shake", 70.0, "", "Shakes n Coffee", true),
            MenuItem("shake8", "Vanilla Shake", "Vanilla shake", 70.0, "", "Shakes n Coffee", true),
            MenuItem("shake9", "Black Currant Shake", "Black currant shake", 80.0, "", "Shakes n Coffee", true),
            MenuItem("shake10", "Raspberry Shake", "Raspberry shake", 80.0, "", "Shakes n Coffee", true),
            MenuItem("shake11", "Ice Latte", "Ice latte", 70.0, "", "Shakes n Coffee", true),
            MenuItem("shake12", "Ice Mocha", "Ice mocha", 70.0, "", "Shakes n Coffee", true),
            MenuItem("shake13", "Cold Coffee", "Cold coffee", 70.0, "", "Shakes n Coffee", true),
            MenuItem("shake14", "Hazel Nut Coffee", "Hazel nut coffee", 90.0, "", "Shakes n Coffee", true),
            MenuItem("shake15", "Caramel Frappe", "Caramel frappe", 90.0, "", "Shakes n Coffee", true),
            MenuItem("shake16", "Vanilla Frappe", "Vanilla frappe", 90.0, "", "Shakes n Coffee", true),
            MenuItem("shake17", "Irish Frappe", "Irish frappe", 90.0, "", "Shakes n Coffee", true),

            // EXOTIC SHAKES
            MenuItem("exoticshake1", "Strawberry Banana", "Strawberry banana shake", 80.0, "", "Exotic Shakes", true),
            MenuItem("exoticshake2", "Mix 'E' Shake", "Mix 'E' shake", 90.0, "", "Exotic Shakes", true),
            MenuItem("exoticshake3", "Oreo Shake", "Oreo shake", 90.0, "", "Exotic Shakes", true),
            MenuItem("exoticshake4", "Caramel Crunch", "Caramel crunch shake", 90.0, "", "Exotic Shakes", true),
            MenuItem("exoticshake5", "Brownie Shake", "Brownie shake", 90.0, "", "Exotic Shakes", true),
            MenuItem("exoticshake6", "Ferrero Rocher", "Ferrero rocher shake", 90.0, "", "Exotic Shakes", true),
            MenuItem("exoticshake7", "Choco Frappe", "Choco frappe", 90.0, "", "Exotic Shakes", true),
            MenuItem("exoticshake8", "Kitkat Shake", "Kitkat shake", 90.0, "", "Exotic Shakes", true),
            MenuItem("exoticshake9", "Coffee Shake", "Coffee shake", 90.0, "", "Exotic Shakes", true),
            MenuItem("exoticshake10", "Hazel Nut Shake", "Hazel nut shake", 90.0, "", "Exotic Shakes", true),
            MenuItem("exoticshake11", "Mint Oreo", "Mint oreo shake", 90.0, "", "Exotic Shakes", true),

            // VEG KATHI ROLLS
            MenuItem("vegroll1", "Single Roll", "Veg single roll", 40.0, "", "Veg Kathi Rolls", true),
            MenuItem("vegroll2", "Paneer Roll", "Paneer roll", 60.0, "", "Veg Kathi Rolls", true),
            MenuItem("vegroll3", "Veggie Roll", "Veggie roll", 60.0, "", "Veg Kathi Rolls", true),
            MenuItem("vegroll4", "Cheese Roll", "Cheese roll", 70.0, "", "Veg Kathi Rolls", true),
            MenuItem("vegroll5", "Paneer Bhurji Roll", "Paneer bhurji roll", 80.0, "", "Veg Kathi Rolls", true),
            MenuItem("vegroll6", "Paneer Tikka Roll", "Paneer tikka roll", 110.0, "", "Veg Kathi Rolls", true),
            MenuItem("vegroll7", "Soya Roll", "Soya roll", 70.0, "", "Veg Kathi Rolls", true),
            MenuItem("vegroll8", "Veggie Mushroom Roll", "Veggie mushroom roll", 100.0, "", "Veg Kathi Rolls", true),
            MenuItem("vegroll9", "Paneer Mushroom Roll", "Paneer mushroom roll", 120.0, "", "Veg Kathi Rolls", true),
            MenuItem("vegroll10", "Paneer Cheese Roll", "Paneer cheese roll", 130.0, "", "Veg Kathi Rolls", true),

            // NON-VEG KATHI ROLLS
            MenuItem("nonvegroll1", "Egg Roll", "Egg roll", 50.0, "", "Non-Veg Kathi Rolls", false),
            MenuItem("nonvegroll2", "Double Egg Roll", "Double egg roll", 70.0, "", "Non-Veg Kathi Rolls", false),
            MenuItem("nonvegroll3", "Chicken Roll", "Chicken roll", 100.0, "", "Non-Veg Kathi Rolls", false),
            MenuItem("nonvegroll4", "Chicken Seekh Roll", "Chicken seekh roll", 100.0, "", "Non-Veg Kathi Rolls", false),
            MenuItem("nonvegroll5", "Chicken Egg Roll", "Chicken egg roll", 110.0, "", "Non-Veg Kathi Rolls", false),
            MenuItem("nonvegroll6", "Chicken Bhurji Roll", "Chicken bhurji roll", 110.0, "", "Non-Veg Kathi Rolls", false),
            MenuItem("nonvegroll7", "Chicken Cheese Roll", "Chicken cheese roll", 120.0, "", "Non-Veg Kathi Rolls", false),
            MenuItem("nonvegroll8", "Chicken Mushroom Roll", "Chicken mushroom roll", 130.0, "", "Non-Veg Kathi Rolls", false),
            MenuItem("nonvegroll9", "Chicken Paneer Roll", "Chicken paneer roll", 130.0, "", "Non-Veg Kathi Rolls", false),
            MenuItem("nonvegroll10", "Chicken Tikka Roll", "Chicken tikka roll", 130.0, "", "Non-Veg Kathi Rolls", false),
            MenuItem("nonvegroll11", "Chicken Malai Roll", "Chicken malai roll", 130.0, "", "Non-Veg Kathi Rolls", false),
            MenuItem("nonvegroll12", "Tandoori Chilly Chicken", "Tandoori chilly chicken roll", 130.0, "", "Non-Veg Kathi Rolls", false),

            // DESSERTS
            MenuItem("dessert1", "Hot Chocolate Fudge", "Hot chocolate fudge", 80.0, "", "Dessert", true),
            MenuItem("dessert2", "Browne Fudge", "Browne fudge", 100.0, "", "Dessert", true)
        )
    }

    private fun getSinghBakersMenuItems(): List<MenuItem> {
        return listOf(
            // VADA PAV
            MenuItem("vadapav1", "Vada Pav", "Vada pav", 40.0, "", "Vada Pav", true),
            MenuItem("vadapav2", "Tandoori Vada Pav", "Tandoori vada pav", 50.0, "", "Vada Pav", true),
            MenuItem("vadapav3", "Masala Vada Pav", "Masala vada pav", 50.0, "", "Vada Pav", true),
            MenuItem("vadapav4", "Cheese Vada Pav", "Cheese vada pav", 50.0, "", "Vada Pav", true),

            // PATTIES
            MenuItem("patty1", "Veg Patty", "Veg patty", 35.0, "", "Patties", true),
            MenuItem("patty2", "Paneer Patty / Cheese Patty", "Paneer patty or cheese patty", 40.0, "", "Patties", true),
            MenuItem("patty3", "Cheese Corn / Pizza Patty", "Cheese corn or pizza patty", 40.0, "", "Patties", true),
            MenuItem("patty4", "Pasta Patty", "Pasta patty", 40.0, "", "Patties", true),
            MenuItem("patty5", "Mexican Patty", "Mexican patty", 70.0, "", "Patties", true),
            MenuItem("patty6", "Chicken Patty", "Chicken patty", 70.0, "", "Patties", false),

            // SANDWICH VEG/NON VEG
            MenuItem("sandwich1", "Cold Sandwich (White/Brown)", "Cold sandwich (white/brown)", 30.0, "", "Sandwich", true),
            MenuItem("sandwich2", "Cold Sandwich (White/Brown)", "Cold sandwich (white/brown)", 40.0, "", "Sandwich", true),
            MenuItem("sandwich3", "Veg Grilled Sandwich", "Veg grilled sandwich", 70.0, "", "Sandwich", true),
            MenuItem("sandwich4", "Aloo Grilled Sandwich", "Aloo grilled sandwich", 70.0, "", "Sandwich", true),
            MenuItem("sandwich5", "Cheese Corn Grilled Sandwich", "Cheese corn grilled sandwich", 80.0, "", "Sandwich", true),
            MenuItem("sandwich6", "Mexican Grilled Sandwich", "Mexican grilled sandwich", 80.0, "", "Sandwich", true),
            MenuItem("sandwich7", "Italian Cheese Grilled Sandwich", "Italian cheese grilled sandwich", 80.0, "", "Sandwich", true),
            MenuItem("sandwich8", "Tandoori Grilled Sandwich", "Tandoori grilled sandwich", 80.0, "", "Sandwich", true),
            MenuItem("sandwich9", "Cheese Chilly Grilled Sandwich", "Cheese chilly grilled sandwich", 90.0, "", "Sandwich", true),
            MenuItem("sandwich10", "Paneer Grilled Sandwich", "Paneer grilled sandwich", 90.0, "", "Sandwich", true),
            MenuItem("sandwich11", "Chicken Grilled Sandwich", "Chicken grilled sandwich", 100.0, "", "Sandwich", false),
            MenuItem("sandwich12", "Chicken Tandoori Grilled Sandwich", "Chicken tandoori grilled sandwich", 100.0, "", "Sandwich", false),

            // BURGER VEG/NON VEG
            MenuItem("burger1", "Aloo Tikki Burger", "Aloo tikki burger", 50.0, "", "Burger", true),
            MenuItem("burger2", "Veggie Classic Burger", "Veggie classic burger", 70.0, "", "Burger", true),
            MenuItem("burger3", "Veggie Classic Spicy Burger", "Veggie classic spicy burger", 80.0, "", "Burger", true),
            MenuItem("burger4", "Paneer Grilled Burger", "Paneer grilled burger", 80.0, "", "Burger", true),
            MenuItem("burger5", "Chicken Burger", "Chicken burger", 70.0, "", "Burger", false),
            MenuItem("burger6", "Chicken Spicy Grilled Burger", "Chicken spicy grilled burger", 80.0, "", "Burger", false),
            MenuItem("burger7", "Chicken Tandoori Grilled Burger", "Chicken tandoori grilled burger", 100.0, "", "Burger", false),

            // FRIES
            MenuItem("fries1", "French Fries", "French fries", 80.0, "", "Fries", true),
            MenuItem("fries2", "Cheesy Fries", "Cheesy fries", 90.0, "", "Fries", true),
            MenuItem("fries3", "Pizza Fries", "Pizza fries", 110.0, "", "Fries", true),
            MenuItem("fries4", "Peri Peri Fries", "Peri peri fries", 110.0, "", "Fries", true),

            // PASTA
            MenuItem("pasta1", "Red/White Sauce Pasta", "Red/white sauce pasta", 90.0, "", "Pasta", true),
            MenuItem("pasta2", "Mix Sauce Pasta", "Mix sauce pasta", 100.0, "", "Pasta", true),

            // MINI MEALS
            MenuItem("mini1", "Bread Omlette", "Bread omlette", 50.0, "", "Mini Meals", false),
            MenuItem("mini2", "Aloo Chaat", "Aloo chaat", 50.0, "", "Mini Meals", true),
            MenuItem("mini3", "Bhelpuri", "Bhelpuri", 60.0, "", "Mini Meals", true),
            MenuItem("mini4", "Spring Roll", "Spring roll", 70.0, "", "Mini Meals", true),
            MenuItem("mini5", "Bullets", "Bullets", 70.0, "", "Mini Meals", true),
            MenuItem("mini6", "Paneer Cutlets", "Paneer cutlets", 110.0, "", "Mini Meals", true),
            MenuItem("mini7", "Chicken Nuggets", "Chicken nuggets", 110.0, "", "Mini Meals", false),

            // TACOS
            MenuItem("taco1", "Crispy Aloo Tikki Tacos", "Crispy aloo tikki tacos", 100.0, "", "Tacos", true),
            MenuItem("taco2", "Cheese Chilly Tacos", "Cheese chilly tacos", 120.0, "", "Tacos", true),
            MenuItem("taco3", "Chicken Tandoori Tacos", "Chicken tandoori tacos", 130.0, "", "Tacos", false),

            // MAGGI
            MenuItem("maggi1", "Maggi", "Maggi", 40.0, "", "Maggi", true),
            MenuItem("maggi2", "Veg. Maggi", "Veg. maggi", 50.0, "", "Maggi", true),
            MenuItem("maggi3", "Cheese Maggi", "Cheese maggi", 60.0, "", "Maggi", true),
            MenuItem("maggi4", "Egg Maggi", "Egg maggi", 60.0, "", "Maggi", false),

            // BROWNIE
            MenuItem("brownie1", "Brownie", "Brownie", 60.0, "", "Brownie", true),
            MenuItem("brownie2", "Hot Brownie with Chocolate Sauce", "Hot brownie with chocolate sauce", 90.0, "", "Brownie", true),
            MenuItem("brownie3", "Hot Brownie with Vanilla Ice Cream", "Hot brownie with vanilla ice cream", 100.0, "", "Brownie", true),

            // SHAKES
            MenuItem("shake1", "Banana Shake", "Banana shake", 70.0, "", "Shakes", true),
            MenuItem("shake2", "Mango Shake", "Mango shake", 70.0, "", "Shakes", true),
            MenuItem("shake3", "Chocolate Shake", "Chocolate shake", 70.0, "", "Shakes", true),
            MenuItem("shake4", "Butterscotch Shake", "Butterscotch shake", 70.0, "", "Shakes", true),
            MenuItem("shake5", "Pineapple Shake", "Pineapple shake", 70.0, "", "Shakes", true),
            MenuItem("shake6", "Vanilla Shake", "Vanilla shake", 70.0, "", "Shakes", true),
            MenuItem("shake7", "Strawberry Shake", "Strawberry shake", 70.0, "", "Shakes", true),
            MenuItem("shake8", "Kiwi Shake", "Kiwi shake", 70.0, "", "Shakes", true),
            MenuItem("shake9", "Strawberry Banana Shake", "Strawberry banana shake", 80.0, "", "Shakes", true),
            MenuItem("shake10", "Chocolate Strawberry Shake", "Chocolate strawberry shake", 80.0, "", "Shakes", true),
            MenuItem("shake11", "Blue Berry Shake", "Blue berry shake", 80.0, "", "Shakes", true),
            MenuItem("shake12", "Black Currant Shake", "Black currant shake", 80.0, "", "Shakes", true),

            // SHAKES AND FRAPPE
            MenuItem("frappe1", "Oreo Shake", "Oreo shake", 90.0, "", "Shakes & Frappe", true),
            MenuItem("frappe2", "Kitkat Shake", "Kitkat shake", 90.0, "", "Shakes & Frappe", true),
            MenuItem("frappe3", "Brownie Shake", "Brownie shake", 100.0, "", "Shakes & Frappe", true),
            MenuItem("frappe4", "Caramel Crunch Shake", "Caramel crunch shake", 100.0, "", "Shakes & Frappe", true),
            MenuItem("frappe5", "Naughty Nutella Shake", "Naughty nutella shake", 100.0, "", "Shakes & Frappe", true),
            MenuItem("frappe6", "Bubblegum Shake", "Bubblegum shake", 100.0, "", "Shakes & Frappe", true),

            // COFFEE
            MenuItem("coffee1", "Cold Coffee", "Cold coffee", 80.0, "", "Coffee", true),
            MenuItem("coffee2", "Chocolate Cold Coffee", "Chocolate cold coffee", 90.0, "", "Coffee", true),

            // MOJITOS
            MenuItem("mojito1", "Blue Lagoon", "Blue lagoon", 60.0, "", "Mojitos", true),
            MenuItem("mojito2", "Classic Mint", "Classic mint", 70.0, "", "Mojitos", true),
            MenuItem("mojito3", "Kala Khatta", "Kala khatta", 80.0, "", "Mojitos", true),
            MenuItem("mojito4", "Fizzy Aam Panna", "Fizzy aam panna", 80.0, "", "Mojitos", true),
            MenuItem("mojito5", "Green Apple", "Green apple", 80.0, "", "Mojitos", true),
            MenuItem("mojito6", "Green Mint", "Green mint", 80.0, "", "Mojitos", true),
            MenuItem("mojito7", "Passion Fruit", "Passion fruit", 80.0, "", "Mojitos", true),
            MenuItem("mojito8", "Water Melon", "Water melon", 80.0, "", "Mojitos", true),
            MenuItem("mojito9", "Black Currant", "Black currant", 80.0, "", "Mojitos", true),
            MenuItem("mojito10", "Strawberry", "Strawberry", 80.0, "", "Mojitos", true),

            // COOLERS
            MenuItem("cooler1", "Nimboo Pani", "Nimboo pani", 40.0, "", "Coolers", true),
            MenuItem("cooler2", "Lemon Ice Tea", "Lemon ice tea", 60.0, "", "Coolers", true),
            MenuItem("cooler3", "Peach Ice Tea", "Peach ice tea", 60.0, "", "Coolers", true),
            MenuItem("cooler4", "Masala Lemon Soda", "Masala lemon soda", 60.0, "", "Coolers", true),

            // HOT BEVERAGES
            MenuItem("hotbeverage1", "Chai", "Chai", 15.0, "", "Hot Beverages", true),
            MenuItem("hotbeverage2", "Coffee", "Coffee", 30.0, "", "Hot Beverages", true),
            MenuItem("hotbeverage3", "Hot Chocolate", "Hot chocolate", 60.0, "", "Hot Beverages", true),
            MenuItem("hotbeverage4", "Veg. Soup", "Veg. soup", 60.0, "", "Hot Beverages", true),

            // WAFFLE
            MenuItem("waffle1", "Kitkat Waffle", "Kitkat waffle", 80.0, "", "Waffle", true),
            MenuItem("waffle2", "Oreo Waffle", "Oreo waffle", 80.0, "", "Waffle", true),
            MenuItem("waffle3", "Nutella Waffle", "Nutella waffle", 90.0, "", "Waffle", true),
            MenuItem("waffle4", "Brownie Waffle", "Brownie waffle", 90.0, "", "Waffle", true)
        )
    }

    private fun getMummyDiRotiMenuItems(): List<MenuItem> {
        return listOf(
            // MAIN ITEMS
            MenuItem("cholebhature", "Chole Bhature", "Chole bhature", 70.0, "", "Main", true),
            MenuItem("paubhaji", "Pau Bhaji", "Pau bhaji", 80.0, "", "Main", true),
            MenuItem("cholekulcha", "Chole Kulcha", "Chole kulcha", 70.0, "", "Main", true),
            MenuItem("nutrikulcha", "Nutri Kulcha", "Nutri kulcha", 70.0, "", "Main", true),

            // BEVERAGES
            MenuItem("sweetlassi", "Sweet Lassi", "Sweet lassi", 40.0, "", "Beverages", true),
            MenuItem("namkeenlassi", "Namkeen Lassi", "Namkeen lassi", 40.0, "", "Beverages", true),
            MenuItem("roselassi", "Rose Lassi", "Rose lassi", 50.0, "", "Beverages", true),
            MenuItem("coldcoffee", "Cold Coffee", "Cold coffee", 50.0, "", "Beverages", true),
            MenuItem("icetea", "Ice Tea", "Ice tea", 60.0, "", "Beverages", true),
            MenuItem("bananashake", "Banana Shake", "Banana shake", 60.0, "", "Beverages", true),

            // THALI
            MenuItem("vegthali", "Veg Thali", "3 rothi + rice + dal + sabji + salad", 80.0, "", "Thali", true),
            MenuItem("paneerthali", "Paneer Thali", "3 rothi + rice + paneer sabji + dal + salad", 110.0, "", "Thali", true),
            MenuItem("chickenthali", "Chicken Thali", "3 rothi + rice + chicken + salad", 130.0, "", "Thali", false),

            // BIRYANI
            MenuItem("paneerbiryani_half", "Paneer Biryani (Medium)", "Paneer biryani (medium)", 80.0, "", "Biryani", true),
            MenuItem("paneerbiryani_full", "Paneer Biryani (Full)", "Paneer biryani (full)", 130.0, "", "Biryani", true),
            MenuItem("chickenbiryani_half", "Chicken Biryani (Medium)", "Chicken biryani (medium)", 90.0, "", "Biryani", false),
            MenuItem("chickenbiryani_full", "Chicken Biryani (Full)", "Chicken biryani (full)", 140.0, "", "Biryani", false),

            // CHINESE COMBO
            MenuItem("friedricecombo_half", "Fried Rice + (Medium)", "Fried rice + (medium)", 70.0, "", "Chinese Combo", true),
            MenuItem("friedricecombo_full", "Fried Rice + (Full)", "Fried rice + (full)", 120.0, "", "Chinese Combo", true),
            MenuItem("noodlemanchurian_half", "Noodle Manchurian (Medium)", "Noodle manchurian (medium)", 80.0, "", "Chinese Combo", true),
            MenuItem("noodlemanchurian_full", "Noodle Manchurian (Full)", "Noodle manchurian (full)", 130.0, "", "Chinese Combo", true),
            MenuItem("friedricecheesechilli_half", "Fried Rice Cheese Chilli (Medium)", "Fried rice cheese chilli (medium)", 80.0, "", "Chinese Combo", true),
            MenuItem("friedricecheesechilli_full", "Fried Rice Cheese Chilli (Full)", "Fried rice cheese chilli (full)", 130.0, "", "Chinese Combo", true),
            MenuItem("noodlescheesechilli_half", "Noodles Cheese Chilli (Medium)", "Noodles cheese chilli (medium)", 80.0, "", "Chinese Combo", true),
            MenuItem("noodlescheesechilli_full", "Noodles Cheese Chilli (Full)", "Noodles cheese chilli (full)", 130.0, "", "Chinese Combo", true),

            // PARANTHA
            MenuItem("alooparantha", "Aloo Parantha", "Aloo parantha", 40.0, "", "Parantha", true),
            MenuItem("mixparantha", "Mix Parantha", "Mix parantha", 50.0, "", "Parantha", true),
            MenuItem("paneerparantha", "Paneer Parantha", "Paneer parantha", 60.0, "", "Parantha", true),
            MenuItem("eggparantha", "Egg Parantha", "Egg parantha", 70.0, "", "Parantha", false),
            MenuItem("chickenparantha", "Chicken Parantha", "Chicken parantha", 70.0, "", "Parantha", false),

            // CHINESE (NOODLES & RICE)
            MenuItem("vegnoodles_half", "Veg Noodles (Medium)", "Veg noodles (medium)", 50.0, "", "Chinese", true),
            MenuItem("vegnoodles_full", "Veg Noodles (Full)", "Veg noodles (full)", 80.0, "", "Chinese", true),
            MenuItem("paneernoodles_half", "Paneer Noodles (Medium)", "Paneer noodles (medium)", 70.0, "", "Chinese", true),
            MenuItem("paneernoodles_full", "Paneer Noodles (Full)", "Paneer noodles (full)", 110.0, "", "Chinese", true),
            MenuItem("eggnoodles_half", "Egg Noodles (Medium)", "Egg noodles (medium)", 70.0, "", "Chinese", false),
            MenuItem("eggnoodles_full", "Egg Noodles (Full)", "Egg noodles (full)", 110.0, "", "Chinese", false),
            MenuItem("chickennoodles_half", "Chicken Noodles (Medium)", "Chicken noodles (medium)", 80.0, "", "Chinese", false),
            MenuItem("chickennoodles_full", "Chicken Noodles (Full)", "Chicken noodles (full)", 130.0, "", "Chinese", false),
            MenuItem("garlicnoodles_half", "Garlic Noodles (Medium)", "Garlic noodles (medium)", 60.0, "", "Chinese", true),
            MenuItem("garlicnoodles_full", "Garlic Noodles (Full)", "Garlic noodles (full)", 80.0, "", "Chinese", true),
            MenuItem("vegfriedrice_half", "Veg Fried Rice (Medium)", "Veg fried rice (medium)", 50.0, "", "Chinese", true),
            MenuItem("vegfriedrice_full", "Veg Fried Rice (Full)", "Veg fried rice (full)", 80.0, "", "Chinese", true),
            MenuItem("paneerfriedrice_half", "Paneer Fried Rice (Medium)", "Paneer fried rice (medium)", 70.0, "", "Chinese", true),
            MenuItem("paneerfriedrice_full", "Paneer Fried Rice (Full)", "Paneer fried rice (full)", 110.0, "", "Chinese", true),
            MenuItem("eggfriedrice_half", "Egg Fried Rice (Medium)", "Egg fried rice (medium)", 70.0, "", "Chinese", false),
            MenuItem("eggfriedrice_full", "Egg Fried Rice (Full)", "Egg fried rice (full)", 110.0, "", "Chinese", false),
            MenuItem("chickenfriedrice_half", "Chicken Fried Rice (Medium)", "Chicken fried rice (medium)", 80.0, "", "Chinese", false),
            MenuItem("chickenfriedrice_full", "Chicken Fried Rice (Full)", "Chicken fried rice (full)", 130.0, "", "Chinese", false),
            MenuItem("chillypotato_half", "Chilly Potato (Medium)", "Chilly potato (medium)", 60.0, "", "Chinese", true),
            MenuItem("chillypotato_full", "Chilly Potato (Full)", "Chilly potato (full)", 80.0, "", "Chinese", true),
            MenuItem("honeychillypotato_half", "Honey Chilly Potato (Medium)", "Honey chilly potato (medium)", 70.0, "", "Chinese", true),
            MenuItem("honeychillypotato_full", "Honey Chilly Potato (Full)", "Honey chilly potato (full)", 90.0, "", "Chinese", true),

            // DRY/GRAVY CHINESE
            MenuItem("vegmanchurian_dry", "Veg Manchurian 13pc (Dry)", "Veg manchurian 13pc (dry)", 100.0, "", "Chinese Dry/Gravy", true),
            MenuItem("vegmanchurian_gravy", "Veg Manchurian 13pc (Gravy)", "Veg manchurian 13pc (gravy)", 120.0, "", "Chinese Dry/Gravy", true),
            MenuItem("cheesechilly_dry", "Cheese Chilly 12pc (Dry)", "Cheese chilly 12pc (dry)", 100.0, "", "Chinese Dry/Gravy", true),
            MenuItem("cheesechilly_gravy", "Cheese Chilly 12pc (Gravy)", "Cheese chilly 12pc (gravy)", 120.0, "", "Chinese Dry/Gravy", true),
            MenuItem("chickenchilly_dry", "Chicken Chilly 7pc (Dry)", "Chicken chilly 7pc (dry)", 250.0, "", "Chinese Dry/Gravy", false),
            MenuItem("chickenchilly_gravy", "Chicken Chilly 7pc (Gravy)", "Chicken chilly 7pc (gravy)", 270.0, "", "Chinese Dry/Gravy", false),

            // RICE COMBO
            MenuItem("rajmharice", "Rajma Rice", "Rajma rice", 50.0, "", "Rice Combo", true),
            MenuItem("channarice", "Channa Rice", "Channa rice", 50.0, "", "Rice Combo", true),
            MenuItem("paneerrice", "Paneer Rice", "Paneer rice", 70.0, "", "Rice Combo", true),
            MenuItem("kadhirice", "Kadhi Rice", "Kadhi rice", 50.0, "", "Rice Combo", true),
            MenuItem("dalrice", "Dal Rice", "Dal rice", 50.0, "", "Rice Combo", true),
            MenuItem("nutririce", "Nutri Rice", "Nutri rice", 70.0, "", "Rice Combo", true),
            MenuItem("butterchickenrice", "Butter Chicken Rice", "Butter chicken rice", 130.0, "", "Rice Combo", false),

            // CHAAT
            MenuItem("golgappa_atta", "Golgappa (Atta) 6pc", "Golgappa (atta) 6pc", 30.0, "", "Chaat", true),
            MenuItem("golgappa_suji", "Golgappa (Suji) 6pc", "Golgappa (suji) 6pc", 30.0, "", "Chaat", true),
            MenuItem("dahigolgappa", "Dahi Golgappa 6pc", "Dahi golgappa 6pc", 40.0, "", "Chaat", true),
            MenuItem("dahibhala", "Dahi Bhala", "Dahi bhala", 60.0, "", "Chaat", true),
            MenuItem("bhalapaprichat", "Bhala Papri Chat", "Bhala papri chat", 60.0, "", "Chaat", true),
            MenuItem("aloochat", "Aloo Chat", "Aloo chat", 60.0, "", "Chaat", true),
            MenuItem("rajkachori", "Raj Kachori", "Raj kachori", 70.0, "", "Chaat", true),
            MenuItem("channatikki", "Channa Tikki", "Channa tikki", 60.0, "", "Chaat", true),
            MenuItem("tikkichat", "Tikki Chat", "Tikki chat", 60.0, "", "Chaat", true)
        )
    }
}


