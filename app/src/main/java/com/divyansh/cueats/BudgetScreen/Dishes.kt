package com.divyansh.cueats.BudgetScreen

object DishRepository {
    fun getAllDishes(): List<Dish> {
        return listOf(
            Dish(1, "Aloo Tikki Burger", "CATCH UP CAFE", 40.0, true, 4.4f, "Boys Hostel", "A popular potato patty burger with various sauces and toppings.", "https://img.freepik.com/free-photo/top-view-tasty-chicken-sandwich-with-green-salad-vegetables-french-fries-rustic-grey-surface_140725-44245.jpg?t=st=1740913206~exp=1740916806~hmac=417c53f72125b408cf736e1ef8bfc82f09db0be79acd59690e2ddd788070efc9&w=1380",
                seller1
            ),
            Dish(2, "Veg Burger", "CATCH UP CAFE", 50.0, true, 4.3f, "Boys Hostel", "A classic vegetarian burger with a mix of vegetables and condiments.", "https://img.freepik.com/premium-photo/veggie-carrot-oats-burger-with-cucumber-onion-tomato_214995-6742.jpg?w=1380",
                seller1
            ),
            Dish(3, "Corn Sandwich", "CATCH UP CAFE", 40.0, true, 4.2f, "Boys Hostel", "A sandwich filled with sweet corn and other flavorful ingredients.", "https://img.freepik.com/premium-photo/sweet-corn-grilled-sandwich-served-with-ketchup-isolated-rustic-wooden-background-selective-focus_726363-1340.jpg?w=1380",
                seller1
            ),
            Dish(4, "Paneer Sandwich", "CATCH UP CAFE", 50.0, true, 4.7f, "Boys Hostel", "A hearty sandwich with paneer (Indian cottage cheese) and spices.", "https://img.freepik.com/premium-photo/grilled-panini-sandwich-wire-rack_879692-19.jpg?w=1380",
                seller1
            ),
            Dish(5, "Veg Grill Sandwich", "CATCH UP CAFE", 60.0, true, 4.6f, "Boys Hostel", "A grilled sandwich with a variety of vegetables and cheese.", "https://img.freepik.com/free-photo/high-angle-triangle-sandwiches-with-tomatoes_23-2148640141.jpg?t=st=1740913021~exp=1740916621~hmac=755f41f00b03be59843c059963f13d1e7e0922333ae9fbec5cea538640837ff7&w=740",
                seller1
            ),
            Dish(6, "White Sauce Pasta", "CATCH UP CAFE", 80.0, true, 4.9f, "Boys Hostel", "Pasta cooked in a creamy white sauce with vegetables.", "https://img.freepik.com/free-photo/penne-carbonara-pasta-with-salmon_74190-2784.jpg?t=st=1740912417~exp=1740916017~hmac=adb7bbbeb1b437c0bbb83750ceacec4f66f0168054bdd5b63840de0c227eec23&w=1380",
                seller1
            ),
            Dish(7, "Makhani Pasta", "CATCH UP CAFE", 80.0, true, 4.5f, "Boys Hostel", "Pasta in a rich, tomato-based makhani sauce with Indian spices.", "https://img.freepik.com/free-photo/side-view-penne-pasta-with-tomato-sauce-greens-plate_141793-5043.jpg?t=st=1740913119~exp=1740916719~hmac=0318a1e938dfce5bdf4510a7178f30c8e8663e7bebb00a82cffaf1bbc396da04&w=1380",
                seller1
            ),
            Dish(8, "Chicken White Sauce Pasta", "CATCH UP CAFE", 100.0, false, 4.8f, "Boys Hostel", "Pasta with chicken and a creamy white sauce.", "https://img.freepik.com/free-photo/carbonara_1203-2930.jpg?t=st=1740912512~exp=1740916112~hmac=18a6433d39ac77fa1df55277ad47130fce5ee9aba9e51f23ac71a4f0290322b6&w=1380",
                seller1
            ),
            Dish(9, "Paneer Noodles", "CATCH UP CAFE", 80.0, true, 4.5f, "Boys Hostel", "Noodles with paneer and a blend of Asian-inspired flavors.", "https://img.freepik.com/premium-photo/schezwan-hakka-noodles-with-paneer-cottage-cheese-served-bowl-selective-focus_466689-32656.jpg?w=1380",
                seller1
            ),
            Dish(10, "Peri Peri Fries", "CATCH UP CAFE", 70.0, true, 4.2f, "Boys Hostel", "Crispy fries seasoned with peri peri spices.", "https://img.freepik.com/free-photo/top-view-chips-with-sauses-bowls-black-stone_176474-1209.jpg?t=st=1740913356~exp=1740916956~hmac=b903cdb95e1d6cf42ea23706fa4f257f1ef210e5610c32e3fe3e06b7e577f7af&w=1380",
                seller1
            ),
            Dish(11, "Masala Sweet Corn", "CATCH UP CAFE", 40.0, true, 4.3f, "Boys Hostel", "Sweet corn kernels tossed with spices and herbs.", "https://img.freepik.com/free-photo/tasty-esquites-with-spices-cups_23-2149891146.jpg?t=st=1740913404~exp=1740917004~hmac=214e7876b219992467a26e66858f42c30398658a8f74d15ae1eea705d0a4fef3&w=1380",
                seller1
            ),
            Dish(12, "Paneer Fried Rice", "CATCH UP CAFE", 80.0, true, 4.5f, "Boys Hostel", "Fried rice with paneer and a mix of vegetables.", "https://img.freepik.com/premium-photo/vegetarian-paneer-biryani-panir-pulav-popular-indian-food_466689-2077.jpg?w=1380",
                seller1
            ),
            Dish(13, "Special Tea", "CATCH UP CAFE", 20.0, true, 4.6f, "Boys Hostel", "A unique blend of tea with special ingredients.", "https://img.freepik.com/free-photo/steaming-cup-coffee-with-cinnamon-sticks-star-anise-coffee-beans-dark-background_9975-124681.jpg?t=st=1740913489~exp=1740917089~hmac=6be645e4d14beaf8120e54063e79f775888394f32eacf5b20ab4838a588529af&w=1800",
                seller1
            ),
            Dish(14, "Veg Burger", "PUNJABI RASOI", 50.0, true, 4.3f, "Boys Hostel", "A classic vegetarian burger with a Punjabi twist.", "https://img.freepik.com/premium-photo/vegan-burger-wooden-board_171081-1058.jpg?w=1380",
                seller2
            ),
            Dish(15, "Grilled Sandwich", "PUNJABI RASOI", 70.0, true, 4.3f, "Boys Hostel", "A grilled sandwich with a variety of vegetables and spices.", "https://img.freepik.com/free-photo/top-view-delicious-ham-sandwiches-inside-plate-dark-surface_179666-35066.jpg?t=st=1740913660~exp=1740917260~hmac=fce92a814b5ab7db507e2f52612d42eeb278defec5b81adfc660ef06fd481607&w=1380",
                seller2
            ),
            Dish(16, "Paneer Sandwich", "PUNJABI RASOI", 90.0, true, 4.5f, "Boys Hostel", "A hearty sandwich filled with paneer and flavorful sauces.", "https://img.freepik.com/free-photo/front-view-tasty-ham-sandwiches-with-french-fries-dark-surface_179666-34644.jpg?t=st=1740913701~exp=1740917301~hmac=1d03152f28666777315e831166bc228863737827fe447056caaa6ad590344ba3&w=1380",
                seller2
            ),
            Dish(17, "Noodles Kathi Roll", "PUNJABI RASOI", 45.0, true, 4.4f, "Boys Hostel", "A roll filled with noodles and a spicy filling.", "https://img.freepik.com/premium-photo/peri-peri-paneer-chapati-frankie-wrap-roll-selective-focus_466689-39306.jpg?w=1380",
                seller2
            ),
            Dish(18, "Amritsari Kulcha", "PUNJABI RASOI", 110.0, true, 4.6f, "Boys Hostel", "Two pieces of Amritsari kulcha, a popular Punjabi bread.", "https://img.freepik.com/free-photo/fresh-homemade-gourmet-meal-bread-meat-vegetable-dessert-snack-generated-by-artificial-intelligence_188544-126597.jpg?t=st=1740913612~exp=1740917212~hmac=05da8b26efef3c99f0a3e86d81a6c8a0737b0892ead4852f40db21653c095a21&w=1800",
                seller2
            ),
            Dish(19, "Chana Samosa", "PUNJABI RASOI", 60.0, true, 4.3f, "Boys Hostel", "Samosas filled with chana (chickpeas) and spices.", "https://img.freepik.com/premium-photo/samosa-chaat-indian-special-traditional-street-food-served-rustic-wooden-background-selective-focus_726363-1078.jpg?w=1380",
                seller2
            ),
            Dish(20, "Paneer Fried Rice", "PUNJABI RASOI", 100.0, true, 4.3f, "Boys Hostel", "Fried rice with paneer and a blend of Asian-inspired flavors.", "https://img.freepik.com/premium-photo/healthy-paneer-pulav-pilaf-using-basmati-rice-served-bowl-plate-indian-food_466689-72600.jpg?w=740",
                seller2
            ),
            Dish(21, "White Sauce Pasta", "PUNJABI RASOI", 110.0, true, 4.5f, "Boys Hostel", "Pasta cooked in a creamy white sauce with vegetables and Punjabi spices.", "https://img.freepik.com/free-photo/penne-carbonara-pasta-with-salmon_74190-2784.jpg?t=st=1740912417~exp=1740916017~hmac=adb7bbbeb1b437c0bbb83750ceacec4f66f0168054bdd5b63840de0c227eec23&w=1380",
                seller2
            ),
            Dish(22, "Choley Bhature", "PUNJABI RASOI", 80.0, true, 4.6f, "Boys Hostel", "A classic Punjabi dish of chole (chickpeas) and bhature (fried bread).", "https://img.freepik.com/premium-photo/chole-bhature-is-north-indian-food-dish-combination-chana-masala-bhatura-puri_1184104-1335.jpg?w=1380",
                seller2
            ),
            Dish(23, "Rara Paneer Combo", "PUNJABI RASOI", 130.0, true, 4.6f, "Boys Hostel", "A combo meal with Rara Paneer and accompaniments.", "https://img.freepik.com/free-photo/pakistani-dish-arrangement-view_23-2148825114.jpg?t=st=1740913923~exp=1740917523~hmac=7eb3c07475e9d3868b686489c90a635962eeb17d5c971ae0d522379a82111b52&w=1380",
                seller2
            ),
            Dish(24, "Shahi Paneer Combo", "PUNJABI RASOI", 130.0, true, 4.5f, "Boys Hostel", "A combo meal with Shahi Paneer and accompaniments.", "https://img.freepik.com/premium-photo/paneer-khus-khus-curry-cottage-cheese-posto-masala-made-using-poppy-seeds-indian-recipe_1093310-2496.jpg?w=1380",
                seller2
            )
        )
    }
}
val seller1 = Seller(
    id = 102,
    name = "CATCH UP CAFE",
    phoneNumber = " NOT AVAILABLE",
    whatsappNumber = " NOT AVAILABLE "
)

val seller2 = Seller(
    id = 103,
    name = "PUNJABI RASOI",
    phoneNumber = " NOT AVAILABLE ",
    whatsappNumber = " NOT AVAILABLE "
)