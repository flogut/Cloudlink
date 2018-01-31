package de.hgv.model

import io.javalin.security.Role

enum class MyRole: Role {
    READ, WRITE
}