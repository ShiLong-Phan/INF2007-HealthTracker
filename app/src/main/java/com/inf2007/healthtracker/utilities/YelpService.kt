package com.inf2007.healthtracker.utilities

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface YelpService {
    @GET("v3/businesses/search")
    fun searchRestaurants(
        @Header("Authorization") authHeader: String,
        @Query("term") term: String,
        @Query("location") location: String,
        @Query("limit") limit: Int
    ): Call<YelpResponse>
}

fun getYelpSearchTerm(meal: String?): String {
    return when {
        meal.isNullOrEmpty() -> "Healthy restaurants"

        // ✅ Breakfast & Brunch
        meal.contains("oatmeal", ignoreCase = true) -> "Healthy breakfast"
        meal.contains("berries", ignoreCase = true) -> "Smoothie shop"
        meal.contains("chia seeds", ignoreCase = true) -> "Vegan cafe"
        meal.contains("avocado toast", ignoreCase = true) -> "Brunch spot"
        meal.contains("banana", ignoreCase = true) -> "Breakfast cafe"
        meal.contains("yogurt", ignoreCase = true) -> "Frozen yogurt shop"
        meal.contains("almond milk", ignoreCase = true) -> "Plant-based cafe"
        meal.contains("pancakes", ignoreCase = true) -> "Breakfast restaurant"
        meal.contains("waffles", ignoreCase = true) -> "Brunch restaurant"
        meal.contains("bagel", ignoreCase = true) -> "Bagel shop"
        meal.contains("muffin", ignoreCase = true) -> "Bakery"
        meal.contains("granola", ignoreCase = true) -> "Health food cafe"
        meal.contains("smoothie", ignoreCase = true) -> "Smoothie bar"
        meal.contains("protein shake", ignoreCase = true) -> "Fitness cafe"
        meal.contains("acai bowl", ignoreCase = true) -> "Superfood cafe"
        meal.contains("scrambled eggs", ignoreCase = true) -> "Brunch cafe"
        meal.contains("french toast", ignoreCase = true) -> "Brunch restaurant"
        meal.contains("cereal", ignoreCase = true) -> "Organic grocery store"
        meal.contains("energy bar", ignoreCase = true) -> "Health food store"
        meal.contains("turmeric latte", ignoreCase = true) -> "Wellness cafe"
        meal.contains("green smoothie", ignoreCase = true) -> "Juice bar"
        meal.contains("cappuccino", ignoreCase = true) -> "Coffee shop"
        meal.contains("latte", ignoreCase = true) -> "Coffee house"
        meal.contains("croissant", ignoreCase = true) -> "French bakery"
        meal.contains("eggs benedict", ignoreCase = true) -> "Brunch spot"
        meal.contains("matcha latte", ignoreCase = true) -> "Japanese cafe"
        meal.contains("vegan pancakes", ignoreCase = true) -> "Vegan brunch cafe"

        // ✅ Protein-Rich Meals
        meal.contains("salmon", ignoreCase = true) -> "Seafood restaurant"
        meal.contains("grilled chicken", ignoreCase = true) -> "Grilled chicken restaurant"
        meal.contains("tofu", ignoreCase = true) -> "Vegan restaurant"
        meal.contains("steak", ignoreCase = true) -> "Steakhouse"
        meal.contains("bison", ignoreCase = true) -> "Wild game restaurant"
        meal.contains("duck", ignoreCase = true) -> "French restaurant"
        meal.contains("shrimp", ignoreCase = true) -> "Seafood restaurant"
        meal.contains("lobster", ignoreCase = true) -> "Seafood shack"
        meal.contains("prawns", ignoreCase = true) -> "Mediterranean seafood"
        meal.contains("turkey", ignoreCase = true) -> "Deli restaurant"
        meal.contains("quinoa", ignoreCase = true) -> "Organic restaurant"
        meal.contains("cottage cheese", ignoreCase = true) -> "Health-focused cafe"
        meal.contains("lentils", ignoreCase = true) -> "Indian cuisine"
        meal.contains("chickpeas", ignoreCase = true) -> "Mediterranean restaurant"
        meal.contains("bacon", ignoreCase = true) -> "American diner"
        meal.contains("venison", ignoreCase = true) -> "Game meat restaurant"

        // ✅ Vegetables & Vegetarian Options
        meal.contains("broccoli", ignoreCase = true) -> "Vegetarian restaurant"
        meal.contains("kale", ignoreCase = true) -> "Health food store"
        meal.contains("spinach", ignoreCase = true) -> "Organic salad bar"
        meal.contains("mushroom", ignoreCase = true) -> "Vegetarian restaurant"
        meal.contains("sweet potato", ignoreCase = true) -> "Vegetarian restaurant"
        meal.contains("lentils", ignoreCase = true) -> "Indian cuisine"
        meal.contains("chickpeas", ignoreCase = true) -> "Mediterranean restaurant"
        meal.contains("hummus", ignoreCase = true) -> "Middle Eastern cuisine"
        meal.contains("beets", ignoreCase = true) -> "Farm-to-table restaurant"
        meal.contains("cauliflower", ignoreCase = true) -> "Vegan restaurant"
        meal.contains("carrots", ignoreCase = true) -> "Juice bar"
        meal.contains("brussels sprouts", ignoreCase = true) -> "Farm-to-table restaurant"
        meal.contains("zucchini", ignoreCase = true) -> "Vegan restaurant"
        meal.contains("artichoke", ignoreCase = true) -> "Mediterranean cuisine"
        meal.contains("jackfruit", ignoreCase = true) -> "Vegan restaurant"
        meal.contains("arugula", ignoreCase = true) -> "Organic salad bar"
        meal.contains("vegan burger", ignoreCase = true) -> "Vegan fast food"
        meal.contains("falafel", ignoreCase = true) -> "Mediterranean cuisine"

        // ✅ Carbohydrates & Grains
        meal.contains("brown rice", ignoreCase = true) -> "Asian cuisine"
        meal.contains("wild rice", ignoreCase = true) -> "Organic restaurant"
        meal.contains("whole wheat", ignoreCase = true) -> "Whole foods restaurant"
        meal.contains("sourdough", ignoreCase = true) -> "Artisan bakery"
        meal.contains("pasta", ignoreCase = true) -> "Italian restaurant"
        meal.contains("spaghetti", ignoreCase = true) -> "Italian restaurant"
        meal.contains("lasagna", ignoreCase = true) -> "Italian trattoria"
        meal.contains("ravioli", ignoreCase = true) -> "Italian bistro"
        meal.contains("focaccia", ignoreCase = true) -> "Mediterranean cafe"
        meal.contains("cornbread", ignoreCase = true) -> "Southern restaurant"
        meal.contains("baguette", ignoreCase = true) -> "French bakery"
        meal.contains("noodles", ignoreCase = true) -> "Asian noodle bar"
        meal.contains("ramen", ignoreCase = true) -> "Japanese ramen shop"
        meal.contains("soba", ignoreCase = true) -> "Japanese restaurant"
        meal.contains("udon", ignoreCase = true) -> "Japanese noodle house"

        // ✅ Ethnic & International Cuisine
        meal.contains("sushi", ignoreCase = true) -> "Japanese restaurant"
        meal.contains("dim sum", ignoreCase = true) -> "Chinese restaurant"
        meal.contains("biryani", ignoreCase = true) -> "Indian restaurant"
        meal.contains("tacos", ignoreCase = true) -> "Mexican restaurant"
        meal.contains("burrito", ignoreCase = true) -> "Mexican fast food"
        meal.contains("enchilada", ignoreCase = true) -> "Authentic Mexican food"
        meal.contains("paella", ignoreCase = true) -> "Spanish restaurant"
        meal.contains("gyro", ignoreCase = true) -> "Greek restaurant"
        meal.contains("shawarma", ignoreCase = true) -> "Middle Eastern restaurant"
        meal.contains("falafel", ignoreCase = true) -> "Vegetarian Mediterranean"
        meal.contains("curry", ignoreCase = true) -> "Indian or Thai restaurant"
        meal.contains("naan", ignoreCase = true) -> "Indian restaurant"
        meal.contains("tamale", ignoreCase = true) -> "Mexican street food"
        meal.contains("ceviche", ignoreCase = true) -> "Peruvian restaurant"
        meal.contains("empanada", ignoreCase = true) -> "Argentinian bakery"
        meal.contains("pho", ignoreCase = true) -> "Vietnamese noodle bar"
        meal.contains("ramen", ignoreCase = true) -> "Japanese ramen shop"
        meal.contains("bibimbap", ignoreCase = true) -> "Korean restaurant"

        // ✅ Snacks & Light Meals
        meal.contains("wrap", ignoreCase = true) -> "Healthy wraps"
        meal.contains("sandwich", ignoreCase = true) -> "Deli restaurant"
        meal.contains("bowl", ignoreCase = true) -> "Poke bowl shop"
        meal.contains("green beans", ignoreCase = true) -> "Vegetarian restaurant"
        meal.contains("tapas", ignoreCase = true) -> "Spanish tapas bar"
        meal.contains("crostini", ignoreCase = true) -> "Italian wine bar"
        meal.contains("cheese plate", ignoreCase = true) -> "Wine and cheese bar"

        // ✅ Desserts & Sweets
        meal.contains("chocolate", ignoreCase = true) -> "Chocolate shop"
        meal.contains("ice cream", ignoreCase = true) -> "Ice cream parlor"
        meal.contains("cupcake", ignoreCase = true) -> "Cupcake bakery"
        meal.contains("cheesecake", ignoreCase = true) -> "Dessert cafe"
        meal.contains("macaron", ignoreCase = true) -> "French patisserie"
        meal.contains("tiramisu", ignoreCase = true) -> "Italian dessert shop"
        meal.contains("cannoli", ignoreCase = true) -> "Italian pastry shop"
        meal.contains("gelato", ignoreCase = true) -> "Italian gelateria"
        meal.contains("donut", ignoreCase = true) -> "Donut shop"
        meal.contains("croissant", ignoreCase = true) -> "French bakery"
        meal.contains("pudding", ignoreCase = true) -> "British dessert cafe"
        meal.contains("apple pie", ignoreCase = true) -> "American bakery"
        meal.contains("mochi", ignoreCase = true) -> "Japanese dessert shop"
        meal.contains("baklava", ignoreCase = true) -> "Middle Eastern bakery"

        // ✅ Beverages
        meal.contains("coffee", ignoreCase = true) -> "Coffee shop"
        meal.contains("green tea", ignoreCase = true) -> "Tea house"
        meal.contains("matcha", ignoreCase = true) -> "Japanese cafe"
        meal.contains("bubble tea", ignoreCase = true) -> "Bubble tea shop"
        meal.contains("kombucha", ignoreCase = true) -> "Fermented drinks bar"
        meal.contains("herbal tea", ignoreCase = true) -> "Tea house"
        meal.contains("protein shake", ignoreCase = true) -> "Smoothie bar"
        meal.contains("almond milk latte", ignoreCase = true) -> "Vegan coffee shop"

        // ✅ Catch-All Fallbacks
        else -> "Healthy food"
    }
}
