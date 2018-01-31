package de.hgv.model

import io.javalin.security.Role

// Users:
// viewer, pphwtbl4100h
// writer, pphwtbj01s05
enum class MyRole: Role {
    READ, WRITE
}