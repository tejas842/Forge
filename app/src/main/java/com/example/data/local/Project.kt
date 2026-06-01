package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val path: String,
    val createdAt: Long = System.currentTimeMillis()
)
