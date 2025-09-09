package com.pizzamania.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(
        entities = { CartItem.class },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract CartDao cartDao();
}
