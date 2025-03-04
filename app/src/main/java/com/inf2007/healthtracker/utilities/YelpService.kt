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

        // ✅ Southeast Asian Cuisine
        meal.contains("lemongrass", ignoreCase = true) -> "Vietnamese or Thai restaurant"
        meal.contains("galangal", ignoreCase = true) -> "Thai restaurant"
        meal.contains("turmeric rice", ignoreCase = true) -> "Malaysian restaurant"
        meal.contains("coconut milk", ignoreCase = true) -> "Southeast Asian restaurant"
        meal.contains("shrimp paste", ignoreCase = true) -> "Malaysian or Indonesian restaurant"
        meal.contains("pandan", ignoreCase = true) -> "Southeast Asian bakery"
        meal.contains("fish sauce", ignoreCase = true) -> "Vietnamese restaurant"
        meal.contains("tamarind", ignoreCase = true) -> "Thai or Malaysian restaurant"
        meal.contains("kaffir lime", ignoreCase = true) -> "Thai restaurant"
        meal.contains("belacan", ignoreCase = true) -> "Malaysian restaurant"
        meal.contains("sambal", ignoreCase = true) -> "Indonesian or Malaysian restaurant"
        meal.contains("rendang", ignoreCase = true) -> "Indonesian restaurant"
        meal.contains("laksa", ignoreCase = true) -> "Singaporean or Malaysian restaurant"
        meal.contains("nasi lemak", ignoreCase = true) -> "Malaysian restaurant"
        meal.contains("satay", ignoreCase = true) -> "Malaysian or Indonesian restaurant"
        meal.contains("gado-gado", ignoreCase = true) -> "Indonesian restaurant"
        meal.contains("pho", ignoreCase = true) -> "Vietnamese restaurant"
        meal.contains("banh mi", ignoreCase = true) -> "Vietnamese sandwich shop"
        meal.contains("bun cha", ignoreCase = true) -> "Vietnamese restaurant"
        meal.contains("pad thai", ignoreCase = true) -> "Thai street food"
        meal.contains("tom yum", ignoreCase = true) -> "Thai restaurant"
        meal.contains("mango sticky rice", ignoreCase = true) -> "Thai dessert shop"
        meal.contains("hainanese chicken rice", ignoreCase = true) -> "Singaporean restaurant"
        meal.contains("char kway teow", ignoreCase = true) -> "Malaysian hawker food"
        meal.contains("roti canai", ignoreCase = true) -> "Malaysian mamak stall"
        meal.contains("bak kut teh", ignoreCase = true) -> "Singaporean/Malaysian herbal soup"
        meal.contains("otak-otak", ignoreCase = true) -> "Malay grilled fish cake"
        meal.contains("cendol", ignoreCase = true) -> "Southeast Asian dessert shop"
        meal.contains("kuih", ignoreCase = true) -> "Malaysian traditional cakes"
        meal.contains("sago", ignoreCase = true) -> "Southeast Asian dessert"
        meal.contains("gado gado", ignoreCase = true) -> "Indonesian salad"
        meal.contains("soto ayam", ignoreCase = true) -> "Indonesian soup restaurant"
        meal.contains("nasi goreng", ignoreCase = true) -> "Indonesian fried rice"
        meal.contains("mee rebus", ignoreCase = true) -> "Malay noodle dish"
        meal.contains("popiah", ignoreCase = true) -> "Singaporean fresh spring rolls"
        meal.contains("kaya toast", ignoreCase = true) -> "Singaporean kopitiam"

        // Thailand
        meal.contains("som tum", ignoreCase = true) -> "Thai papaya salad restaurant"
        meal.contains("khao soi", ignoreCase = true) -> "Northern Thai restaurant"
        meal.contains("tom kha gai", ignoreCase = true) -> "Thai coconut soup restaurant"
        meal.contains("moo ping", ignoreCase = true) -> "Thai street food"
        meal.contains("pad see ew", ignoreCase = true) -> "Thai stir-fried noodles"
        meal.contains("massaman curry", ignoreCase = true) -> "Thai curry restaurant"
        meal.contains("pla pao", ignoreCase = true) -> "Thai grilled fish"

        // Vietnam
        meal.contains("bánh xèo", ignoreCase = true) -> "Vietnamese pancake restaurant"
        meal.contains("bún chả", ignoreCase = true) -> "Vietnamese grilled pork restaurant"
        meal.contains("bánh cuốn", ignoreCase = true) -> "Vietnamese steamed rice rolls"
        meal.contains("cơm tấm", ignoreCase = true) -> "Vietnamese broken rice restaurant"
        meal.contains("bún đậu mắm tôm", ignoreCase = true) -> "Vietnamese fermented shrimp paste dish"

        // Malaysia/Singapore
        meal.contains("nasi kandar", ignoreCase = true) -> "Malaysian Indian-Muslim restaurant"
        meal.contains("ayam penyet", ignoreCase = true) -> "Indonesian/Malaysian smashed fried chicken"
        meal.contains("asam pedas", ignoreCase = true) -> "Malay sour and spicy stew"
        meal.contains("apam balik", ignoreCase = true) -> "Malaysian peanut pancake"
        meal.contains("roti jala", ignoreCase = true) -> "Malay net bread restaurant"
        meal.contains("chendol", ignoreCase = true) -> "Southeast Asian dessert stall"

        // Indonesia
        meal.contains("sate padang", ignoreCase = true) -> "Indonesian Padang satay"
        meal.contains("rawon", ignoreCase = true) -> "Javanese black beef soup"
        meal.contains("gudeg", ignoreCase = true) -> "Javanese jackfruit stew"
        meal.contains("pempek", ignoreCase = true) -> "Indonesian fishcake restaurant"
        meal.contains("soto betawi", ignoreCase = true) -> "Jakarta beef soup restaurant"

        // Philippines
        meal.contains("sinigang", ignoreCase = true) -> "Filipino sour soup restaurant"
        meal.contains("adobo", ignoreCase = true) -> "Filipino vinegar stew"
        meal.contains("lechon", ignoreCase = true) -> "Filipino roasted pig restaurant"
        meal.contains("kare-kare", ignoreCase = true) -> "Filipino peanut stew"
        meal.contains("halo-halo", ignoreCase = true) -> "Filipino dessert shop"

        // Myanmar (Burma)
        meal.contains("mohinga", ignoreCase = true) -> "Burmese fish noodle soup"
        meal.contains("laphet thoke", ignoreCase = true) -> "Burmese tea leaf salad"

        // Cambodia
        meal.contains("amok", ignoreCase = true) -> "Cambodian fish curry"
        meal.contains("num banh chok", ignoreCase = true) -> "Khmer rice noodle dish"

        // Laos
        meal.contains("laap", ignoreCase = true) -> "Lao minced meat salad"
        meal.contains("tam mak hoong", ignoreCase = true) -> "Lao papaya salad"

        // Regional Ingredients
        meal.contains("belacan", ignoreCase = true) -> "Malaysian shrimp paste dishes"
        meal.contains("daun kesum", ignoreCase = true) -> "Vietnamese/Malay laksa herb"
        meal.contains("candlenut", ignoreCase = true) -> "Indonesian/Malay rempah base"
        meal.contains("torch ginger", ignoreCase = true) -> "Nyonya/Malay cuisine"

        // Street Food & Snacks
        meal.contains("pisang goreng", ignoreCase = true) -> "Southeast Asian fried banana"
        meal.contains("kueh pie tee", ignoreCase = true) -> "Peranakan top hat pastry"
        meal.contains("onde-onde", ignoreCase = true) -> "Malaysian glutinous rice ball"
        meal.contains("putu piring", ignoreCase = true) -> "Singaporean steamed rice cake"

        // Beverages
        meal.contains("teh tarik", ignoreCase = true) -> "Malaysian pulled tea stall"
        meal.contains("kopi", ignoreCase = true) -> "Southeast Asian coffee shop"
        meal.contains("bandung", ignoreCase = true) -> "Malay rose syrup drink"

        // ✅ Catch-All Fallbacks
        else -> "Healthy food"
    }
}
